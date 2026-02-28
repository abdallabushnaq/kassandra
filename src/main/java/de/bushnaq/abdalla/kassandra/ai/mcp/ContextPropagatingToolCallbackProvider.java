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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Arrays;

/**
 * A {@link ToolCallbackProvider} decorator that transparently handles
 * {@link ToolContextHelper#setup(ToolContext)} and {@link ToolContextHelper#cleanup()}
 * around every {@code @Tool} method invocation.
 * <p>
 * This removes the need for any boilerplate in individual {@code @Tool} methods:
 * they no longer need a {@link ToolContext} parameter, nor explicit setup/cleanup calls.
 * The decorator wraps each {@link ToolCallback} returned by the delegate provider so that
 * the security context and activity context are installed on the executing thread before
 * the method body runs, and reliably cleaned up afterwards — even if the method throws.
 * <p>
 * Usage in {@code AiAssistantService.createToolCallbackProviders()}:
 * <pre>
 *     augmentedProviders.add(new ContextPropagatingToolCallbackProvider(augmentedProvider));
 * </pre>
 */
public class ContextPropagatingToolCallbackProvider implements ToolCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(ContextPropagatingToolCallbackProvider.class);

    private final ToolCallbackProvider delegate;

    public ContextPropagatingToolCallbackProvider(ToolCallbackProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return Arrays.stream(delegate.getToolCallbacks())
                .map(this::wrap)
                .toArray(ToolCallback[]::new);
    }

    private ToolCallback wrap(ToolCallback cb) {
        return new ToolCallback() {
            @Override
            public String call(String toolInput) {
                return call(toolInput, null);
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                ToolContextHelper.setup(toolContext);
                try {
                    String result = cb.call(toolInput, toolContext);
                    int    len    = result == null ? 0 : result.length();
                    log.debug("[tool-result] tool={} inputChars={} resultChars={} result={}",
                            cb.getToolDefinition().name(),
                            toolInput == null ? 0 : toolInput.length(),
                            len,
                            result);
                    return result;
                } finally {
                    ToolContextHelper.cleanup();
                }
            }

            @Override
            public ToolDefinition getToolDefinition() {
                return cb.getToolDefinition();
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return cb.getToolMetadata();
            }
        };
    }
}

