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
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI tool bean that exposes JavaScript syntax validation to the LLM.
 *
 * <p>The LLM calls {@link #validateJavaScript} with the function body it has just written.
 * The tool runs the code through {@link JavaScriptSyntaxValidator} and returns either
 * {@code "OK"} (the code is syntactically valid) or a human-readable error message
 * including the line number and engine error text.  The LLM is expected to correct its
 * output before returning the final answer whenever this tool reports an error.</p>
 */
@Component
@Slf4j
public class JavaScriptValidatorTools {

    private final JavaScriptSyntaxValidator syntaxValidator;

    public JavaScriptValidatorTools(JavaScriptSyntaxValidator syntaxValidator) {
        this.syntaxValidator = syntaxValidator;
    }

    /**
     * Validates the syntax of a JavaScript function body.
     *
     * @param functionBody The JavaScript code that goes inside
     *                     {@code function filterEntity(entity, now) { … }}.
     *                     Do NOT include the function signature — only the body.
     * @return {@code "OK"} when the code is syntactically valid; otherwise a
     * human-readable error message with line number and engine error text.
     */
    @Tool(description = "Validates the syntax of a JavaScript function body. " +
            "Pass ONLY the body (the code that goes inside 'function filterEntity(entity, now) { … }'), " +
            "not the function signature. " +
            "Returns \"OK\" if the code is syntactically valid, or an error message with line number otherwise. " +
            "Always call this tool before returning your final JavaScript answer.")
    public String validateJavaScript(
            @ToolParam(description = "The JavaScript function body to validate (no function signature).")
            String functionBody) {
        log.debug("LLM requested JS validation for body: {}", functionBody);
        String error = syntaxValidator.validate(functionBody);
        if (error == null) {
            log.debug("JS validation: OK");
            return "OK";
        }
        log.debug("JS validation error: {}", error);
        return error;
    }
}

