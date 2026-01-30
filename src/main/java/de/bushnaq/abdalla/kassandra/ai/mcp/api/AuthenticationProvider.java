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
 * Provides authentication for AI API calls.
 * Uses the current user's OIDC token from the SecurityContext.
 * The token is captured when captureCurrentUserToken() is called (typically at the start of an AI query)
 * and then used for all subsequent tool calls within that request.
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
            headers.setBearerAuth(token);
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
