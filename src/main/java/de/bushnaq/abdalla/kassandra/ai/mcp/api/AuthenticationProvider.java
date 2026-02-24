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
 * <p>
 * Token propagation uses Spring AI's {@link org.springframework.ai.chat.model.ToolContext}:
 * the OIDC token is captured on the Vaadin UI thread, passed into the ChatClient via
 * {@code .toolContext(map)}, and extracted in each @Tool method via
 * {@link de.bushnaq.abdalla.kassandra.ai.mcp.ToolContextHelper#setup}.
 * The helper sets a short-lived ThreadLocal that survives only for the synchronous
 * RestTemplate call within the same stack frame.
 * <p>
 * In unit tests (with @WithMockUser), falls back to basic authentication using TEST_PASSWORD.
 */
@Component
@Slf4j
public class AuthenticationProvider {

    /**
     * Short-lived ThreadLocal set by {@link de.bushnaq.abdalla.kassandra.ai.mcp.ToolContextHelper#setup}
     * at the top of each @Tool method and cleared in its finally block.
     * Only needs to survive the synchronous RestTemplate call within that method.
     */
    private static final ThreadLocal<String> currentUserToken = new ThreadLocal<>();

    @Autowired(required = false)
    private OAuth2AuthorizedClientService authorizedClientService;

    /**
     * Captures the OIDC token from the current SecurityContext (must be called on
     * the request/UI thread where the SecurityContext is available).
     *
     * @return the raw bearer token string, or null if not available (e.g. test mode)
     */
    public String captureCurrentUserToken() {
        String token = getTokenFromSecurityContext();
        if (token != null) {
            log.debug("Captured user token for AI tools");
            return token;
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof OAuth2AuthenticationToken)) {
                log.debug("No OAuth2 token available, will use basic auth fallback for user: {}", authentication.getName());
                return null;
            }
            log.warn("No token available to capture for AI tools");
            return null;
        }
    }

    /**
     * Clear the token from the current thread. Called by {@link de.bushnaq.abdalla.kassandra.ai.mcp.ToolContextHelper#cleanup}.
     */
    public static void clearCurrentToken() {
        currentUserToken.remove();
    }

    public HttpHeaders createAuthHeaders() {
        String      token   = currentUserToken.get();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (token != null) {
            headers.setBearerAuth(token);
            log.debug("Using captured OAuth2 token for authentication");
        } else {
            // Fallback to basic auth (test flow with @WithMockUser)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof OAuth2AuthenticationToken)) {
                String username    = authentication.getName();
                String password    = SecurityConfig.TEST_PASSWORD;
                String auth        = username + ":" + password;
                byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
                String authHeader  = "Basic " + new String(encodedAuth);
                headers.set("Authorization", authHeader);
            } else {
                log.warn("No authentication token available (neither OAuth2 nor basic)");
            }
        }

        return headers;
    }

    /**
     * Reads the OIDC token from the current SecurityContext.
     * Public so that callers (e.g. ChatAgentPanel) can capture the raw token string
     * on the UI thread before passing it into the AI pipeline via ToolContext.
     */
    public String getTokenFromSecurityContext() {
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
     * Set the token on the current thread. Called by {@link de.bushnaq.abdalla.kassandra.ai.mcp.ToolContextHelper#setup}.
     */
    public static void setCurrentToken(String token) {
        if (token != null && !token.isEmpty()) {
            currentUserToken.set(token);
        }
    }
}
