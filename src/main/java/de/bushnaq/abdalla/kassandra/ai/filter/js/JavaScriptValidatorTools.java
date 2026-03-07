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

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI tool bean that exposes JavaScript validation to the LLM.
 *
 * <p>A single {@link #validateJavaScript} tool performs both syntax checking
 * (via {@link JavaScriptSyntaxValidator}) and runtime execution checking
 * (via {@link JavaScriptExecutionValidator}) in one call.  The LLM does not
 * need to know about the two phases.</p>
 */
@Component
@Slf4j
public class JavaScriptValidatorTools {

    private final JavaScriptExecutionValidator executionValidator;
    private final JavaScriptSyntaxValidator    syntaxValidator;

    public JavaScriptValidatorTools(JavaScriptSyntaxValidator syntaxValidator,
                                    JavaScriptExecutionValidator executionValidator) {
        this.syntaxValidator    = syntaxValidator;
        this.executionValidator = executionValidator;
    }

    /**
     * Validates a JavaScript function body: first checks syntax, then executes it
     * against the real entity list to catch runtime errors.
     *
     * @param functionBody The JavaScript code that goes inside
     *                     {@code function filterEntity(entity, now) { … }}.
     *                     Do NOT include the function signature — only the body.
     * @return {@code "OK"} when the code is both syntactically valid and executes without
     * error against all entities; otherwise a human-readable error message.
     */
    @Tool(description = "Validates a JavaScript function body by checking its syntax and then " +
            "executing it against the real entity list to catch runtime errors " +
            "(wrong getter name, null-dereference, type mismatch, etc.). " +
            "Pass ONLY the body (the code that goes inside 'function filterEntity(entity, now) { … }'), " +
            "not the function signature. " +
            "Returns \"OK\" if the code is valid and executes without error, or an error message otherwise. " +
            "Always call this tool before returning your final JavaScript answer.")
    public String validateJavaScript(
            @ToolParam(description = "The JavaScript function body to validate (no function signature).")
            String functionBody,
            ToolContext toolContext) {
        log.debug("LLM requested JS validation, body: {}", functionBody);

        // Phase 1: syntax check
        String syntaxError = syntaxValidator.validate(functionBody);
        if (syntaxError != null) {
            log.debug("JS syntax error: {}", syntaxError);
            return syntaxError;
        }

        // Phase 2: execution check against the real entity list
        String executionError = executionValidator.validate(functionBody, toolContext);
        if (executionError != null) {
            log.debug("JS execution error: {}", executionError);
            return executionError;
        }

        log.debug("JS validation: OK");
        return "OK";
    }
}
