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

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.dao.OidcIdentityDAO;
import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.dao.SecurityConfigurationDAO;
import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.repository.OidcIdentityRepository;
import de.bushnaq.abdalla.kassandra.repository.OidcProviderRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resolves Kassandra roles from explicit OIDC issuer and subject links.
 */
@Service
public class OidcIdentityService {

    @Autowired
    private OidcIdentityRepository         oidcIdentityRepository;
    @Autowired
    private OidcProviderRepository         oidcProviderRepository;
    @Autowired
    private SecurityConfigurationService   securityConfigurationService;
    @Autowired
    private UserRepository                 userRepository;

    /**
     * Resolves local roles for a linked OIDC identity.
     * During protected initial setup only, the first browser identity becomes the first administrator.
     *
     * @param issuerUri validated OIDC issuer URI
     * @param subject immutable OIDC subject
     * @param email OIDC email claim, required only for the first administrator claim
     * @param displayName OIDC display name, used only when creating the first administrator
     * @param allowInitialAdministratorClaim true only for the browser OIDC authorization-code flow
     * @return local Kassandra roles
     * @throws IllegalStateException when the identity is not linked or its provider is not enabled
     */
    @Transactional
    public List<String> resolveRoles(String issuerUri, String subject, String email, String displayName,
                                     boolean allowInitialAdministratorClaim) {
        OidcProviderDAO provider = oidcProviderRepository.findByIssuerUriAndEnabledTrue(issuerUri)
                .orElseThrow(() -> new IllegalStateException("OIDC issuer is not enabled"));
        OidcIdentityDAO identity = oidcIdentityRepository.findByProviderAndSubject(provider, subject).orElse(null);
        if (identity != null) {
            return identity.getUser().getRoleList();
        }
        identity = claimInitialAdministrator(provider, subject, email, displayName, allowInitialAdministratorClaim);
        return identity.getUser().getRoleList();
    }

    /**
     * Explicitly links a provider subject to an existing Kassandra user.
     *
     * @param userId Kassandra user identifier
     * @param registrationId persistent OIDC provider registration identifier
     * @param subject immutable OIDC subject
     * @return local Kassandra roles for the linked user
     * @throws IllegalArgumentException when the user or provider does not exist
     * @throws IllegalStateException when the subject is already linked to another user
     */
    @Transactional
    public List<String> linkIdentity(UUID userId, String registrationId, String subject) {
        OidcProviderDAO provider = oidcProviderRepository.findByRegistrationId(registrationId)
                .filter(OidcProviderDAO::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("OIDC provider is not enabled"));
        UserDAO user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kassandra user does not exist"));
        OidcIdentityDAO existingIdentity = oidcIdentityRepository.findByProviderAndSubject(provider, subject)
                .orElse(null);
        if (existingIdentity != null) {
            if (!existingIdentity.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("OIDC identity is already linked to another Kassandra user");
            }
            return existingIdentity.getUser().getRoleList();
        }

        OidcIdentityDAO identity = new OidcIdentityDAO();
        identity.setProvider(provider);
        identity.setSubject(subject);
        identity.setUser(user);
        oidcIdentityRepository.save(identity);
        return user.getRoleList();
    }

    private OidcIdentityDAO claimInitialAdministrator(OidcProviderDAO provider, String subject, String email,
                                                       String displayName, boolean allowInitialAdministratorClaim) {
        if (!allowInitialAdministratorClaim
                || securityConfigurationService.getConfiguration().getSetupState()
                        != SecurityConfigurationDAO.SetupState.SETUP_IN_PROGRESS
                || oidcIdentityRepository.count() != 0) {
            throw new IllegalStateException("OIDC identity is not linked to a Kassandra user");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("The first administrator must provide an OIDC email claim");
        }

        UserDAO user = userRepository.findByEmail(email)
                .orElseGet(() -> createInitialAdministrator(email, displayName));
        user.addRole("ADMIN");
        user.addRole("USER");
        userRepository.save(user);

        OidcIdentityDAO identity = new OidcIdentityDAO();
        identity.setProvider(provider);
        identity.setSubject(subject);
        identity.setUser(user);
        identity = oidcIdentityRepository.save(identity);
        securityConfigurationService.completeSetup();
        return identity;
    }

    private UserDAO createInitialAdministrator(String email, String displayName) {
        UserDAO user = new UserDAO();
        user.setEmail(email);
        user.setName(displayName == null || displayName.isBlank() ? email : displayName);
        user.setColor(Color.BLUE);
        user.setFirstWorkingDay(LocalDate.now());
        user.setRoles("ADMIN,USER");
        return user;
    }
}
