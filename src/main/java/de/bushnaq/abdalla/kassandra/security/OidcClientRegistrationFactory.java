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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
            if (!discoveryUri.equals(provider.getIssuerUri())) {
                return createWithSeparateDiscoveryUri(provider, clientSecret, discoveryUri);
            }
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

    private ClientRegistration createWithSeparateDiscoveryUri(OidcProviderDAO provider, String clientSecret, String discoveryUri) {
        Map<String, Object> metadata = RestClient.create()
                .get()
                .uri(withoutTrailingSlash(discoveryUri) + "/.well-known/openid-configuration")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (metadata == null) {
            throw new IllegalStateException("OIDC discovery document is empty");
        }

        String publicIssuerUri   = withoutTrailingSlash(provider.getIssuerUri());
        String internalIssuerUri = withoutTrailingSlash(discoveryUri);
        Map<String, Object> providerMetadata = new HashMap<>(metadata);
        providerMetadata.put("issuer", publicIssuerUri);
        providerMetadata.put("token_endpoint", internalEndpoint(metadata, "token_endpoint", publicIssuerUri, internalIssuerUri));
        providerMetadata.put("jwks_uri", internalEndpoint(metadata, "jwks_uri", publicIssuerUri, internalIssuerUri));
        providerMetadata.put("userinfo_endpoint", internalEndpoint(metadata, "userinfo_endpoint", publicIssuerUri, internalIssuerUri));

        return ClientRegistration.withRegistrationId(provider.getRegistrationId())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId(provider.getClientId())
                .clientSecret(clientSecret)
                .scope(Arrays.stream(provider.getScopes().split(","))
                        .map(String::trim)
                        .filter(scope -> !scope.isEmpty())
                        .toList())
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri(requiredEndpoint(metadata, "authorization_endpoint"))
                .tokenUri((String) providerMetadata.get("token_endpoint"))
                .jwkSetUri((String) providerMetadata.get("jwks_uri"))
                .userInfoUri((String) providerMetadata.get("userinfo_endpoint"))
                .userNameAttributeName("sub")
                .issuerUri(publicIssuerUri)
                .providerConfigurationMetadata(providerMetadata)
                .build();
    }

    private String internalEndpoint(Map<String, Object> metadata, String name, String publicIssuerUri, String internalIssuerUri) {
        String endpoint = requiredEndpoint(metadata, name);
        if (!endpoint.startsWith(publicIssuerUri + "/")) {
            throw new IllegalStateException("OIDC " + name + " does not belong to the configured issuer");
        }
        return internalIssuerUri + endpoint.substring(publicIssuerUri.length());
    }

    private String requiredEndpoint(Map<String, Object> metadata, String name) {
        Object endpoint = metadata.get(name);
        if (!(endpoint instanceof String value) || value.isBlank()) {
            throw new IllegalStateException("OIDC discovery document does not contain " + name);
        }
        return value;
    }

    private String withoutTrailingSlash(String uri) {
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }
}
