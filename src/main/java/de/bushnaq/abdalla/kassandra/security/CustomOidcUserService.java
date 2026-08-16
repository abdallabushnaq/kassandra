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

import de.bushnaq.abdalla.kassandra.service.OidcIdentityService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads local roles for browser OIDC logins from trusted issuer and subject links.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final OidcIdentityService oidcIdentityService;

    /**
     * Creates the OIDC user service.
     *
     * @param oidcIdentityService OIDC identity resolver
     */
    public CustomOidcUserService(OidcIdentityService oidcIdentityService) {
        this.oidcIdentityService = oidcIdentityService;
    }

    /**
     * Loads the OIDC user and replaces provider roles with Kassandra-local roles.
     *
     * @param userRequest OIDC user-information request
     * @return authenticated OIDC user with local authorities
     * @throws OAuth2AuthenticationException when the provider or identity is not trusted
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Set<GrantedAuthority> authorities = new HashSet<>();
        try {
            List<String> roles = oidcIdentityService.resolveRoles(
                    oidcUser.getIdToken().getIssuer().toString(),
                    oidcUser.getSubject(),
                    oidcUser.getEmail(),
                    oidcUser.getFullName(),
                    true);
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        } catch (IllegalStateException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied"), e.getMessage(), e);
        }
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
