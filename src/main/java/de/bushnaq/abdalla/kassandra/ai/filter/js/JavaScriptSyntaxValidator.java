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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates JavaScript function bodies by attempting to parse them inside a
 * GraalVM context.  A parse-only eval is used so no code is actually executed.
 *
 * <p>This component is used by {@link JavaScriptAiFilterGenerator} to give the
 * LLM structured compilation feedback before the generated code is applied to
 * real entities.</p>
 */
@Component
public class JavaScriptSyntaxValidator {

    private static final Logger logger = LoggerFactory.getLogger(JavaScriptSyntaxValidator.class);

    private String buildFeedbackMessage(String jsFunctionBody, PolyglotException e) {
        StringBuilder sb = new StringBuilder();
        sb.append("The JavaScript function body failed syntax validation.\n");

        if (e.getSourceLocation() != null) {
            var loc = e.getSourceLocation();
            // The wrapper adds one header line, so subtract 1 to report the
            // line number relative to the function body the LLM produced.
            int reportedLine = loc.getStartLine() - 1;
            sb.append("Syntax error at line ").append(reportedLine).append(": ");
        } else {
            sb.append("Syntax error: ");
        }

        sb.append(e.getMessage()).append("\n");
        sb.append("\nFailed code:\n").append(jsFunctionBody);
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /**
     * Validates the syntax of a JavaScript function body.
     *
     * @param jsFunctionBody the raw function body (the part that goes inside
     *                       {@code function filterEntity(entity, now) { … }})
     * @return {@code null} when the code is syntactically valid; otherwise a
     * human-readable error message suitable for feeding back to the LLM
     */
    public String validate(String jsFunctionBody) {
        if (jsFunctionBody == null || jsFunctionBody.isBlank()) {
            return "Function body is empty.";
        }

        String wrapped = "function filterEntity(entity, now) {\n" + jsFunctionBody + "\n}";

        try (Context ctx = Context.newBuilder("js")
                .allowIO(IOAccess.NONE)
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowHostAccess(org.graalvm.polyglot.HostAccess.NONE)
                .allowEnvironmentAccess(org.graalvm.polyglot.EnvironmentAccess.NONE)
                .allowPolyglotAccess(org.graalvm.polyglot.PolyglotAccess.NONE)
                .option("engine.WarnInterpreterOnly", "false")
                // Parse-only: do not execute, just check syntax
                .build()) {

            ctx.parse("js", wrapped); // throws PolyglotException on syntax error
            return null; // valid

        } catch (PolyglotException e) {
            String msg = buildFeedbackMessage(jsFunctionBody, e);
            logger.debug("JavaScript syntax validation failed: {}", msg);
            return msg;
        } catch (Exception e) {
            String msg = "Unexpected validation error: " + e.getMessage();
            logger.warn(msg, e);
            return msg;
        }
    }
}

