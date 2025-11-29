/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

import java.time.Duration;

/**
 * Configuration for OAuth2 client with automatic token refresh support.
 * This enables the application to automatically refresh expired access tokens
 * using refresh tokens, preventing 401 errors in long-running operations.
 */
@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.keycloak.client-id")
public class OAuth2ClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientConfig.class);

    /**
     * Creates an OAuth2AuthorizedClientManager that automatically refreshes tokens.
     * This is the recommended approach for managing OAuth2 clients in Spring Security 5.2+.
     * <p>
     * The manager will:
     * - Automatically refresh access tokens when they're expired or about to expire
     * - Use refresh tokens if available
     * - Handle authorization code flow for new authentications
     *
     * @param clientRegistrationRepository Repository of OAuth2 client registrations
     * @param authorizedClientRepository   Repository for storing authorized clients
     * @return Configured OAuth2AuthorizedClientManager with refresh support
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        logger.info(">>> Configuring OAuth2AuthorizedClientManager with automatic token refresh");

        // Create provider that supports refresh tokens
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()     // Support authorization code flow (for web login)
                        .refreshToken(refreshToken -> refreshToken
                                .clockSkew(Duration.ofSeconds(30))  // Allow 30 seconds clock skew
                        )
                        .build();

        // Create the manager
        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientRepository);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        logger.info("OAuth2AuthorizedClientManager configured with refresh token support");
        logger.info("Tokens will be automatically refreshed when expired or within 30 seconds of expiry");

        return authorizedClientManager;
    }
}

