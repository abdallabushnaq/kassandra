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

package de.bushnaq.abdalla.kassandra.ai.mcp.api;

import de.bushnaq.abdalla.kassandra.security.SecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Provides authentication for AI API calls.
 * Uses the current user's OIDC token from the SecurityContext.
 * The token is captured when captureCurrentUserToken() is called (typically at the start of an AI query)
 * and then used for all subsequent tool calls within that request.
 * <p>
 * In production (with Keycloak), OAuth2 tokens are used.
 * In unit tests (with @WithMockUser), falls back to basic authentication using TEST_PASSWORD.
 */
@Component
@Slf4j
public class AuthenticationProvider {

    private static final ThreadLocal<String> currentUserToken = new ThreadLocal<>();

    @Autowired(required = false)
    private OAuth2AuthorizedClientService authorizedClientService;

    public String captureCurrentUserToken() {
        String token = getTokenFromSecurityContext();
        if (token != null) {
            currentUserToken.set(token);
            log.debug("Captured user token for AI tools");
            return token;
        } else {
            // Check if we have basic auth available as fallback
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof OAuth2AuthenticationToken)) {
                log.debug("No OAuth2 token available, will use basic auth fallback for user: {}", authentication.getName());
                return null; // null is OK, createAuthHeaders will handle it
            }
            log.warn("No token available to capture for AI tools");
            return null;
        }
    }

    public void clearCapturedToken() {
        currentUserToken.remove();
        log.debug("Cleared captured AI token");
    }

    public HttpHeaders createAuthHeaders() {
        String      token   = currentUserToken.get();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (token != null) {
            // Use captured OAuth2 token (production flow with Keycloak)
            headers.setBearerAuth(token);
            log.debug("Using captured OAuth2 token for authentication");
        } else {
            // Fallback to basic auth (test flow with @WithMockUser)
            // This ensures AI tools work in unit tests that use basic authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof OAuth2AuthenticationToken)) {
                // We have non-OAuth2 authentication (e.g., test context with @WithMockUser)
                String username    = authentication.getName();
                String password    = SecurityConfig.TEST_PASSWORD;
                String auth        = username + ":" + password;
                byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
                String authHeader  = "Basic " + new String(encodedAuth);
                headers.set("Authorization", authHeader);
//                log.debug("Using basic auth fallback for user: {}", username);
            } else {
                log.warn("No authentication token available (neither OAuth2 nor basic)");
            }
        }

        return headers;
    }

    private String getTokenFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
            OAuth2AuthorizedClient client = authorizedClientService != null ?
                    authorizedClientService.loadAuthorizedClient(clientRegistrationId, oauthToken.getName()) : null;
            if (client != null && client.getAccessToken() != null) {
                return client.getAccessToken().getTokenValue();
            }
        }
        return null;
    }

    /**
     * Sets a specific token for MCP authentication.
     * Useful for testing or when the token is obtained externally.
     */
    public void setToken(String token) {
        if (token != null && !token.isEmpty()) {
            currentUserToken.set(token);
            log.debug("Set explicit token for MCP tools");
        }
    }
}
