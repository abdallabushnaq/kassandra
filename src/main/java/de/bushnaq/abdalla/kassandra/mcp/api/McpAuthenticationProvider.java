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

package de.bushnaq.abdalla.kassandra.mcp.api;

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

/**
 * Provides authentication for MCP API calls.
 * Uses the current user's OIDC token from the SecurityContext.
 * The token is captured when captureCurrentUserToken() is called (typically at the start of an AI query)
 * and then used for all subsequent MCP tool calls within that request.
 */
@Component
@Slf4j
public class McpAuthenticationProvider {

    /**
     * Thread-local storage for the current user's access token.
     * This allows the token to be captured in the request thread and used in MCP tool execution.
     */
    private static final ThreadLocal<String> currentUserToken = new ThreadLocal<>();

    @Autowired(required = false)
    private OAuth2AuthorizedClientService authorizedClientService;

    /**
     * Captures the current user's token for use in MCP tool calls.
     * Call this at the start of an AI query processing while still in the request thread.
     *
     * @return the captured token value, or null if no token available
     */
    public String captureCurrentUserToken() {
        String token = getTokenFromSecurityContext();
        if (token != null) {
            currentUserToken.set(token);
            log.debug("Captured user token for MCP tools");
            return token;
        } else {
            log.warn("No token available to capture for MCP tools");
            return null;
        }
    }

    /**
     * Clears the captured token.
     * Call this after AI query processing is complete.
     */
    public void clearCapturedToken() {
        currentUserToken.remove();
        log.debug("Cleared captured MCP token");
    }

    /**
     * Creates HTTP headers with authentication for MCP API calls.
     * Uses the captured user token if available.
     */
    public HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        // First try to use the captured token
        String token = currentUserToken.get();
        if (token != null && !token.isEmpty()) {
            headers.setBearerAuth(token);
            log.debug("Using captured user token for MCP authentication");
            return headers;
        }

        // Try to get token from current SecurityContext (if we're still in the same thread)
        token = getTokenFromSecurityContext();
        if (token != null && !token.isEmpty()) {
            headers.setBearerAuth(token);
            log.debug("Using SecurityContext token for MCP authentication");
            return headers;
        }

        log.warn("No authentication token available for MCP - API calls may fail");
        return headers;
    }

    /**
     * Extracts the access token from the current SecurityContext.
     */
    private String getTokenFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            if (authorizedClientService != null) {
                try {
                    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                            oauth2Token.getAuthorizedClientRegistrationId(),
                            oauth2Token.getName());

                    if (client != null && client.getAccessToken() != null) {
                        return client.getAccessToken().getTokenValue();
                    }
                } catch (Exception e) {
                    log.warn("Failed to load authorized client: {}", e.getMessage());
                }
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
