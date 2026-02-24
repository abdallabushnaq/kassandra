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

package de.bushnaq.abdalla.kassandra.ai.mcp;

import de.bushnaq.abdalla.kassandra.ai.mcp.api.AuthenticationProvider;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for propagating per-request context (auth token, activity context) to @Tool methods
 * via Spring AI's built-in {@link ToolContext} mechanism.
 * <p>
 * Spring AI passes the {@link ToolContext} as an explicit method argument, which means it is
 * thread-safe regardless of which Reactor scheduler thread executes the tool call.
 * <p>
 * Each @Tool method should call {@link #setup(ToolContext)} at the top and
 * {@link #cleanup()} in a finally block. The ThreadLocals are scoped to the duration
 * of the synchronous RestTemplate call within a single tool method invocation.
 */
public class ToolContextHelper {

    public static final String ACTIVITY_CONTEXT_KEY = "activityContext";
    public static final String AUTH_TOKEN_KEY       = "authToken";

    /**
     * Build the toolContext map to pass to {@code ChatClient.prompt(...).toolContext(map)}.
     *
     * @param authToken       the OIDC bearer token (may be null for test/basic-auth mode)
     * @param activityContext the session activity context for streaming UI updates
     * @return an unmodifiable map suitable for {@link ToolContext}
     */
    public static Map<String, Object> buildContextMap(String authToken, ToolActivityContext activityContext) {
        Map<String, Object> ctx = new HashMap<>();
        if (authToken != null) {
            ctx.put(AUTH_TOKEN_KEY, authToken);
        }
        if (activityContext != null) {
            ctx.put(ACTIVITY_CONTEXT_KEY, activityContext);
        }
        return ctx;
    }

    /**
     * Clear the ThreadLocals set by {@link #setup(ToolContext)}.
     * Call this in a finally block in every @Tool method.
     */
    public static void cleanup() {
        AuthenticationProvider.clearCurrentToken();
        ToolActivityContextHolder.clear();
    }

    /**
     * Extract values from the {@link ToolContext} and set them on the current thread's
     * ThreadLocals. Call this at the very top of every @Tool method.
     * <p>
     * The ThreadLocals only need to survive the synchronous RestTemplate call that
     * follows within the same stack frame — they are cleared by {@link #cleanup()}.
     */
    public static void setup(ToolContext toolContext) {
        if (toolContext != null) {
            Object token = toolContext.getContext().get(AUTH_TOKEN_KEY);
            if (token instanceof String t) {
                AuthenticationProvider.setCurrentToken(t);
            }
            Object activityCtx = toolContext.getContext().get(ACTIVITY_CONTEXT_KEY);
            if (activityCtx instanceof ToolActivityContext ctx) {
                ToolActivityContextHolder.setContext(ctx);
            }
        }
    }
}

