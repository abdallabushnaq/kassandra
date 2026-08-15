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

import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.repository.OidcIdentityRepository;
import de.bushnaq.abdalla.kassandra.repository.OidcProviderRepository;
import de.bushnaq.abdalla.kassandra.security.DatabaseClientRegistrationRepository;
import de.bushnaq.abdalla.kassandra.security.OidcAuthenticationManagerResolver;
import de.bushnaq.abdalla.kassandra.security.OidcClientRegistrationFactory;
import de.bushnaq.abdalla.kassandra.security.SecuritySecretService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Manages OIDC providers and validates discovery before enabling them.
 */
@Service
public class OidcProviderService {

    @Autowired
    private DatabaseClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private OidcAuthenticationManagerResolver    oidcAuthenticationManagerResolver;
    @Autowired
    private OidcIdentityRepository              oidcIdentityRepository;
    @Autowired
    private OidcProviderRepository              oidcProviderRepository;
    @Autowired
    private OidcClientRegistrationFactory       oidcClientRegistrationFactory;
    @Autowired
    private SecuritySecretService                securitySecretService;

    /**
     * Creates and enables a provider after successful OIDC discovery validation.
     *
     * @param displayName display name shown on the login page
     * @param issuerUri issuer URI used for OIDC discovery
     * @param clientId OIDC client identifier
     * @param clientSecret OIDC client secret
     * @param scopes requested scopes
     * @return persisted enabled provider
     * @throws IllegalArgumentException when a required provider setting is blank
     */
    @Transactional
    public OidcProviderDAO createProvider(String displayName, String issuerUri, String clientId,
                                          String clientSecret, List<String> scopes) {
        requireText(displayName, "Provider display name");
        requireText(issuerUri, "OIDC issuer URI");
        requireText(clientId, "OIDC client ID");
        requireText(clientSecret, "OIDC client secret");
        if (scopes == null || scopes.isEmpty() || scopes.stream().anyMatch(scope -> scope == null || scope.isBlank())) {
            throw new IllegalArgumentException("At least one OIDC scope is required");
        }

        OidcProviderDAO provider = new OidcProviderDAO();
        provider.setDisplayName(displayName.trim());
        provider.setIssuerUri(issuerUri.trim());
        provider.setClientId(clientId.trim());
        provider.setClientSecretEncrypted(securitySecretService.encrypt(clientSecret));
        provider.setRegistrationId("oidc-" + UUID.randomUUID());
        provider.setScopes(String.join(",", scopes.stream().map(String::trim).toList()));
        oidcClientRegistrationFactory.create(provider);
        provider.setEnabled(true);
        provider = oidcProviderRepository.save(provider);
        clientRegistrationRepository.invalidate(provider.getRegistrationId());
        oidcAuthenticationManagerResolver.invalidate(provider.getIssuerUri());
        return provider;
    }

    /**
     * Disables a provider and immediately removes its registration from the cache.
     *
     * @param providerId provider identifier
     * @throws IllegalArgumentException when the provider does not exist
     */
    @Transactional
    public void disableProvider(UUID providerId) {
        OidcProviderDAO provider = getProvider(providerId);
        provider.setEnabled(false);
        oidcProviderRepository.save(provider);
        clientRegistrationRepository.invalidate(provider.getRegistrationId());
        oidcAuthenticationManagerResolver.invalidate(provider.getIssuerUri());
    }

    /**
     * Revalidates OIDC discovery and enables a provider.
     *
     * @param providerId provider identifier
     * @throws IllegalArgumentException when the provider does not exist
     * @throws IllegalStateException when OIDC discovery fails
     */
    @Transactional
    public void enableProvider(UUID providerId) {
        OidcProviderDAO provider = getProvider(providerId);
        oidcClientRegistrationFactory.create(provider);
        provider.setEnabled(true);
        oidcProviderRepository.save(provider);
        clientRegistrationRepository.invalidate(provider.getRegistrationId());
        oidcAuthenticationManagerResolver.invalidate(provider.getIssuerUri());
    }

    /**
     * Deletes a provider without identity links.
     *
     * @param providerId provider identifier
     * @throws IllegalStateException when identities still reference the provider
     */
    @Transactional
    public void deleteProvider(UUID providerId) {
        OidcProviderDAO provider = getProvider(providerId);
        if (oidcIdentityRepository.existsByProvider(provider)) {
            throw new IllegalStateException("OIDC provider cannot be deleted while identities are linked to it");
        }
        oidcProviderRepository.delete(provider);
        clientRegistrationRepository.invalidate(provider.getRegistrationId());
        oidcAuthenticationManagerResolver.invalidate(provider.getIssuerUri());
    }

    /**
     * Lists providers in display order.
     *
     * @return all providers
     */
    public List<OidcProviderDAO> getProviders() {
        return oidcProviderRepository.findAll().stream()
                .sorted((left, right) -> left.getDisplayName().compareToIgnoreCase(right.getDisplayName()))
                .toList();
    }

    private OidcProviderDAO getProvider(UUID providerId) {
        return oidcProviderRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("OIDC provider does not exist"));
    }

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}
