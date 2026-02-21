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

import de.bushnaq.abdalla.util.AnsiColorConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local holder for ToolActivityContext, so tools can access the current context.
 */
@Slf4j
public class ToolActivityContextHolder {
    private static final ThreadLocal<ToolActivityContext> contextHolder = new ThreadLocal<>();

    public static void clear() {
        contextHolder.remove();
    }

    public static ToolActivityContext getContext() {
        return contextHolder.get();
    }

    public static void reportActivity(String message) {
        ToolActivityContext context = getContext();
        if (context != null) context.reportActivity(message);
        log.info(AnsiColorConstants.ANSI_GRAY + message + AnsiColorConstants.ANSI_RESET);
    }

    public static void setContext(ToolActivityContext context) {
        contextHolder.set(context);
    }
}
