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

package de.bushnaq.abdalla.kassandra.ai.filter.js;

import de.bushnaq.abdalla.kassandra.ai.mcp.ToolContextHelper;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Validates a JavaScript function body by actually <em>executing</em> it against the real
 * entity list supplied via Spring AI's {@link ToolContext}.
 *
 * <p>The caller must pass the entity list and reference date in the {@link ToolContext} map
 * using {@link ToolContextHelper#FILTER_ENTITIES_KEY} and {@link ToolContextHelper#FILTER_NOW_KEY}.</p>
 */
@Component
public class JavaScriptExecutionValidator {

    private static final Logger logger = LoggerFactory.getLogger(JavaScriptExecutionValidator.class);

    private Context createContext() {
        HostAccess secureHostAccess = HostAccess.newBuilder(HostAccess.EXPLICIT)
                .allowPublicAccess(true)
                .build();
        ResourceLimits secureResourceLimits = ResourceLimits.newBuilder()
                .statementLimit(200, null)
                .build();
        return Context.newBuilder("js")
                .allowHostAccess(secureHostAccess)
                .allowIO(IOAccess.NONE)
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowHostClassLookup(className ->
                        className.equals("java.lang.String") ||
                                className.equals("java.lang.Integer") ||
                                className.equals("java.lang.Boolean") ||
                                className.startsWith("java.time.") ||
                                className.equals("de.bushnaq.abdalla.kassandra.dto.Status") ||
                                className.equals("de.bushnaq.abdalla.kassandra.dto.OffDayType"))
                .allowEnvironmentAccess(org.graalvm.polyglot.EnvironmentAccess.NONE)
                .allowPolyglotAccess(org.graalvm.polyglot.PolyglotAccess.NONE)
                .resourceLimits(secureResourceLimits)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
    }

    @SuppressWarnings("unchecked")
    public String validate(String jsFunctionBody, ToolContext toolContext) {
        if (jsFunctionBody == null || jsFunctionBody.isBlank()) {
            return "Function body is empty.";
        }
        if (toolContext == null) {
            logger.debug("No ToolContext provided – skipping execution validation");
            return null;
        }

        Object entitiesObj = toolContext.getContext().get(ToolContextHelper.FILTER_ENTITIES_KEY);
        Object nowObj      = toolContext.getContext().get(ToolContextHelper.FILTER_NOW_KEY);

        if (!(entitiesObj instanceof List<?> rawList)) {
            logger.debug("No entity list in ToolContext – skipping execution validation");
            return null;
        }
        List<Object> entities = (List<Object>) rawList;
        LocalDate    now      = (nowObj instanceof LocalDate ld) ? ld : LocalDate.now();

        if (entities.isEmpty()) {
            logger.debug("Entity list is empty – skipping execution validation");
            return null;
        }

        String completeFunction = "function filterEntity(entity, now) {\n" + jsFunctionBody + "\n}";

        try (Context jsContext = createContext()) {
            try {
                jsContext.eval("js", completeFunction);
            } catch (PolyglotException e) {
                return "JavaScript function definition failed during execution validation: "
                        + e.getMessage() + "\n\nFailed code:\n" + jsFunctionBody;
            }
            for (int i = 0; i < entities.size(); i++) {
                Object entity = entities.get(i);
                try {
                    jsContext.getBindings("js").putMember("currentEntity", entity);
                    jsContext.getBindings("js").putMember("currentDate", now);
                    Value result = jsContext.eval("js", "filterEntity(currentEntity, currentDate)");
                    if (result == null || result.isNull()) {
                        return "JavaScript runtime error on entity #" + (i + 1) + " (" + entity
                                + "): function returned null/undefined\n\nFailed code:\n" + jsFunctionBody;
                    }
                } catch (PolyglotException e) {
                    return "JavaScript runtime error on entity #" + (i + 1) + " (" + entity
                            + "): " + e.getMessage() + "\n\nFailed code:\n" + jsFunctionBody;
                }
                break; // only test the first entity to keep validation fast, we just want to catch syntax errors or basic runtime issues
            }
        } catch (Exception e) {
            logger.debug("Unexpected error during JS execution validation", e);
            return "Unexpected error during execution validation: " + e.getMessage();
        }
        return null; // all good
    }
}
