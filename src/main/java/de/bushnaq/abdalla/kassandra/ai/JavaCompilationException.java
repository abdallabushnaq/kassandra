/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.ai;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception thrown when Java compilation fails, containing detailed error information
 * that can be used to provide feedback to the AI for code correction.
 */
public class JavaCompilationException extends RuntimeException {
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics;
    private final String                                     failedCode;

    public JavaCompilationException(String message, String failedCode, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        super(message);
        this.failedCode  = failedCode;
        this.diagnostics = diagnostics;
    }

    /**
     * Get a formatted error message suitable for AI feedback.
     */
    public String getAiFeedbackMessage() {
        StringBuilder feedback = new StringBuilder();
        feedback.append("Compilation failed with the following errors:\n\n");

        if (diagnostics != null && !diagnostics.isEmpty()) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
                feedback.append(String.format("❌ Line %d, Column %d: %s\n",
                        diagnostic.getLineNumber(),
                        diagnostic.getColumnNumber(),
                        diagnostic.getMessage(null)));
            }
        } else {
            feedback.append("❌ ").append(getMessage()).append("\n");
        }

        return feedback.toString();
    }

    /**
     * Get a detailed error message including compilation diagnostics.
     */
    public String getDetailedErrorMessage() {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return getMessage();
        }

        String diagnosticMessages = diagnostics.stream()
                .map(diagnostic -> String.format("Line %d: %s",
                        diagnostic.getLineNumber(),
                        diagnostic.getMessage(null)))
                .collect(Collectors.joining("\n"));

        return getMessage() + "\n\nCompilation errors:\n" + diagnosticMessages;
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return diagnostics;
    }

    public String getFailedCode() {
        return failedCode;
    }
}
