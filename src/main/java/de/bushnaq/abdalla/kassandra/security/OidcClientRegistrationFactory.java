/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.security;

import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Creates client registrations from OIDC issuer discovery and persisted credentials.
 */
@Component
public class OidcClientRegistrationFactory {

    @Autowired
    private SecuritySecretService securitySecretService;

    /**
     * Discovers provider metadata and creates the matching authorization-code registration.
     *
     * @param provider persisted provider configuration
     * @return discovered client registration
     * @throws IllegalStateException when discovery or secret decryption fails
     */
    public ClientRegistration create(OidcProviderDAO provider) {
        String clientSecret = securitySecretService.decrypt(provider.getClientSecretEncrypted());
        String discoveryUri = provider.getDiscoveryUri();
        if (discoveryUri == null || discoveryUri.isBlank()) {
            discoveryUri = provider.getIssuerUri();
        }
        try {
            return ClientRegistrations.fromIssuerLocation(discoveryUri)
                    .registrationId(provider.getRegistrationId())
                    .clientId(provider.getClientId())
                    .clientSecret(clientSecret)
                    .scope(Arrays.stream(provider.getScopes().split(","))
                            .map(String::trim)
                            .filter(scope -> !scope.isEmpty())
                            .toList())
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .build();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Unable to discover OIDC provider " + provider.getDisplayName(), e);
        }
    }
}
