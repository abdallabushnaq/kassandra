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

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for propagating per-request context (security context, activity context) to @Tool methods
 * via Spring AI's built-in {@link ToolContext} mechanism.
 * <p>
 * The full Spring {@link SecurityContext} is captured on the Vaadin UI thread (or test thread)
 * and stored in the {@link ToolContext} map. When a @Tool method calls {@link #setup(ToolContext)},
 * the SecurityContext is restored on the tool-execution thread so that
 * {@code SecurityContextHolder.getContext().getAuthentication()} works naturally — enabling
 * {@code AbstractApi.createAuthHeaders()} to obtain the OIDC token without any adapter layer.
 * <p>
 * Each @Tool method should call {@link #setup(ToolContext)} at the top and
 * {@link #cleanup()} in a finally block. The ThreadLocals are scoped to the duration
 * of the synchronous RestTemplate call within a single tool method invocation.
 */
public class ToolContextHelper {

    public static final  String                       ACTIVITY_CONTEXT_KEY    = "activityContext";
    public static final  String                       SECURITY_CONTEXT_KEY    = "securityContext";
    /**
     * Saves the SecurityContext that was present on the thread before {@link #setup(ToolContext)}
     * replaced it. {@link #cleanup()} restores this context, so the thread is left exactly as it
     * was found — regardless of whether the tool ran on a fresh Reactor thread (previous context
     * is empty → restore is effectively a clear) or on the same thread as the caller
     * (e.g. a blocking test thread with @WithMockUser → the test's SecurityContext survives).
     */
    private static final ThreadLocal<SecurityContext> previousSecurityContext = new ThreadLocal<>();

    /**
     * Build the toolContext map to pass to {@code ChatClient.prompt(...).toolContext(map)}.
     *
     * @param securityContext the Spring SecurityContext captured on the UI/request thread (may be null for test/basic-auth mode)
     * @param activityContext the session activity context for streaming UI updates
     * @return a map suitable for {@link ToolContext}
     */
    public static Map<String, Object> buildContextMap(SecurityContext securityContext, ToolActivityContext activityContext) {
        Map<String, Object> ctx = new HashMap<>();
        if (securityContext != null) {
            ctx.put(SECURITY_CONTEXT_KEY, securityContext);
        }
        if (activityContext != null) {
            ctx.put(ACTIVITY_CONTEXT_KEY, activityContext);
        }
        return ctx;
    }

    /**
     * Restores the SecurityContext that was saved by {@link #setup(ToolContext)} and clears
     * the activity context ThreadLocal. Call this in a finally block in every @Tool method.
     * <p>
     * Restoring (rather than clearing) ensures that a blocking caller — such as a test running
     * on a thread that already has a {@code @WithMockUser} SecurityContext — finds its context
     * intact after the tool call returns.
     */
    public static void cleanup() {
        SecurityContext previous = previousSecurityContext.get();
        previousSecurityContext.remove();
        if (previous != null) {
            SecurityContextHolder.setContext(previous);
        } else {
            SecurityContextHolder.clearContext();
        }
        ToolActivityContextHolder.clear();
    }

    /**
     * Extract values from the {@link ToolContext} and set them on the current thread's
     * ThreadLocals. Call this at the very top of every @Tool method.
     * <p>
     * Saves the current SecurityContext first so that {@link #cleanup()} can restore it,
     * then installs the context that was captured on the UI/request thread.
     */
    public static void setup(ToolContext toolContext) {
        // Always save whatever is on the thread right now, even if we end up not replacing it.
        previousSecurityContext.set(SecurityContextHolder.getContext());

        if (toolContext != null) {
            Object secCtx = toolContext.getContext().get(SECURITY_CONTEXT_KEY);
            if (secCtx instanceof SecurityContext sc) {
                SecurityContextHolder.setContext(sc);
            }
            Object activityCtx = toolContext.getContext().get(ACTIVITY_CONTEXT_KEY);
            if (activityCtx instanceof ToolActivityContext ctx) {
                ToolActivityContextHolder.setContext(ctx);
            }
        }
    }
}

