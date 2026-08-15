/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.security;

import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import de.bushnaq.abdalla.kassandra.service.OidcIdentityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Coordinates an administrator-initiated OIDC authorization flow that links a provider subject to a user.
 */
@Service
public class OidcIdentityLinkService {

    private static final String ORIGINAL_AUTHENTICATION_ATTRIBUTE = OidcIdentityLinkService.class.getName()
            + ".originalAuthentication";
    private static final String PENDING_LINK_ATTRIBUTE            = OidcIdentityLinkService.class.getName()
            + ".pendingLink";

    @Autowired
    private OidcIdentityService oidcIdentityService;
    @Autowired
    private UserRepository      userRepository;

    /**
     * Lists Kassandra users that an administrator may link to an OIDC identity.
     *
     * @return users in deterministic name order
     */
    public List<UserDAO> getUsers() {
        return userRepository.findAll().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();
    }

    /**
     * Records the target user and provider before redirecting to OIDC authorization.
     *
     * @param userId Kassandra user to link
     * @param registrationId OIDC provider registration identifier
     * @throws AccessDeniedException when the current principal is not an administrator
     */
    public void startLink(UUID userId, String registrationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Only administrators can link OIDC identities");
        }
        HttpSession session = currentSession();
        session.setAttribute(PENDING_LINK_ATTRIBUTE, new PendingLink(userId, registrationId));
        session.setAttribute(ORIGINAL_AUTHENTICATION_ATTRIBUTE, authentication);
    }

    /**
     * Consumes a pending link request after the selected provider has authenticated an identity.
     *
     * @param userRequest OIDC request containing the selected client registration
     * @param oidcUser validated OIDC user
     * @return linked user roles, or null when this is a normal sign-in
     */
    public List<String> consumePendingLink(OidcUserRequest userRequest, OidcUser oidcUser) {
        HttpSession session = currentSession();
        Object attribute = session.getAttribute(PENDING_LINK_ATTRIBUTE);
        if (!(attribute instanceof PendingLink pendingLink)) {
            return null;
        }
        if (!pendingLink.registrationId().equals(userRequest.getClientRegistration().getRegistrationId())) {
            throw new IllegalStateException("OIDC provider does not match the pending identity link");
        }
        List<String> roles = oidcIdentityService.linkIdentity(
                pendingLink.userId(),
                pendingLink.registrationId(),
                oidcUser.getSubject());
        session.removeAttribute(PENDING_LINK_ATTRIBUTE);
        return roles;
    }

    /**
     * Restores the administrator authentication after a successful identity-link authorization flow.
     *
     * @return original administrator authentication, or null for a normal sign-in
     */
    public Authentication consumeOriginalAuthentication() {
        HttpSession session = currentSession();
        Object attribute = session.getAttribute(ORIGINAL_AUTHENTICATION_ATTRIBUTE);
        session.removeAttribute(ORIGINAL_AUTHENTICATION_ATTRIBUTE);
        return attribute instanceof Authentication authentication ? authentication : null;
    }

    private HttpSession currentSession() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest().getSession();
    }

    private record PendingLink(UUID userId, String registrationId) implements Serializable {
    }
}
