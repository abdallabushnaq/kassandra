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

package de.bushnaq.abdalla.kassandra.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ErrorResponse {
    private String       exceptionClass;
    private String       field;      // Field that caused the error (for unique constraint violations)
    private String       message;
    private List<String> stackTrace;
    private String       status; // Status as a string for better serialization/deserialization
    private Object       value;      // Value that caused the error (for unique constraint violations)

    public ErrorResponse() {
        this.stackTrace = new ArrayList<>();
    }

    /**
     * Constructor that accepts HttpStatus
     *
     * @param status  the HTTP status
     * @param message the error message
     * @param e       the exception that caused this error
     */
    public ErrorResponse(HttpStatus status, String message, Exception e) {
        this();
        this.status  = status != null ? status.toString() : null;
        this.message = message;
        populateExceptionDetails(e);
    }

    /**
     * Constructor that accepts HttpStatusCode (for ResponseStatusException)
     *
     * @param statusCode the HTTP status code
     * @param message    the error message
     * @param e          the exception that caused this error
     */
    public ErrorResponse(HttpStatusCode statusCode, String message, Exception e) {
        this();
        this.status  = statusCode != null ? statusCode.toString() : null;
        this.message = message;
        populateExceptionDetails(e);
    }

    /**
     * Get the HTTP status as an enum
     *
     * @return the HttpStatus enum value, or null if it can't be parsed
     */
    @JsonIgnore
    public HttpStatus getHttpStatus() {
        if (status == null) {
            return null;
        }

        try {
            // Try to parse as a status code number
            if (status.matches("\\d+")) {
                return HttpStatus.valueOf(Integer.parseInt(status));
            }

            // If status contains numbers and text like "409 CONFLICT"
            if (status.matches("\\d+\\s+.*")) {
                String[] parts = status.split("\\s+", 2);
                return HttpStatus.valueOf(Integer.parseInt(parts[0]));
            }

            // Try to parse as a status name (like "CONFLICT")
            return HttpStatus.valueOf(status);
        } catch (Exception e) {
            // If parsing fails, return null
            return null;
        }
    }

    /**
     * Populates exception details like class name and stack trace
     *
     * @param e the exception to extract details from
     */
    private void populateExceptionDetails(Exception e) {
        if (e != null) {
            this.exceptionClass = e.getClass().getName();

            // Capture stack trace as strings to avoid circular references
            if (e.getStackTrace() != null) {
                for (StackTraceElement element : e.getStackTrace()) {
                    this.stackTrace.add(element.toString());
                }
            }

            // Include cause information if available
            Throwable cause = e.getCause();
            if (cause != null) {
                this.stackTrace.add("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                if (cause.getStackTrace() != null) {
                    for (StackTraceElement element : cause.getStackTrace()) {
                        this.stackTrace.add("    at " + element.toString());
                    }
                }
            }
        }
    }

    /**
     * Reconstructs an exception from the stored information.
     * This is used client-side to recreate an exception for logging purposes.
     *
     * @return a new Exception with the stored information and properly formatted stack trace
     */
    public Exception reconstructException() {
        if (exceptionClass == null || stackTrace == null || stackTrace.isEmpty()) {
            return null;
        }

        // Create an exception with the original message
        Exception reconstructedException = new Exception(message + " (Original exception: " + exceptionClass + ")");

        try {
            // Parse the stack trace strings and create StackTraceElement objects
            List<StackTraceElement> stackTraceElements = new ArrayList<>();
            boolean                 inCause            = false;
            Exception               currentException   = reconstructedException;

            for (String line : stackTrace) {
                // Check if this is a "Caused by:" line indicating a nested exception
                if (line.startsWith("Caused by:")) {
                    // Create a cause exception
                    String    causeInfo = line.substring("Caused by:".length()).trim();
                    Exception cause     = new Exception(causeInfo);
                    currentException.initCause(cause);
                    currentException = cause;
                    inCause          = true;
                    continue;
                }

                // Process normal stack trace lines
                if (line.startsWith("    at ")) {
                    line = line.substring(6); // Remove the "    at " prefix
                }

                // Parse the stack trace line into a StackTraceElement
                try {
                    // Example format: package.Class.method(File.java:123)
                    int openParenIndex  = line.lastIndexOf('(');
                    int closeParenIndex = line.lastIndexOf(')');

                    if (openParenIndex > 0 && closeParenIndex > openParenIndex) {
                        String methodPart = line.substring(0, openParenIndex);
                        String filePart   = line.substring(openParenIndex + 1, closeParenIndex);

                        // Split the method part into declaring class and method name
                        int    lastDotIndex = methodPart.lastIndexOf('.');
                        String className    = methodPart.substring(0, lastDotIndex);
                        String methodName   = methodPart.substring(lastDotIndex + 1);

                        // Split the file part into filename and line number
                        String fileName;
                        int    lineNumber = -1;

                        int colonIndex = filePart.lastIndexOf(':');
                        if (colonIndex > 0) {
                            fileName = filePart.substring(0, colonIndex);
                            try {
                                lineNumber = Integer.parseInt(filePart.substring(colonIndex + 1));
                            } catch (NumberFormatException e) {
                                // If line number can't be parsed, just use -1
                            }
                        } else {
                            fileName = filePart;
                        }

                        // Create a StackTraceElement
                        StackTraceElement element = new StackTraceElement(className, methodName, fileName, lineNumber);

                        if (inCause) {
                            // If we're in a cause exception, add to the current cause
                            List<StackTraceElement> causeTrace = new ArrayList<>();
                            causeTrace.add(element);
                            if (currentException != reconstructedException) {
                                currentException.setStackTrace(causeTrace.toArray(new StackTraceElement[0]));
                            }
                        } else {
                            // Otherwise add to the main exception's stack trace
                            stackTraceElements.add(element);
                        }
                    }
                } catch (Exception e) {
                    // If parsing fails for any reason, just add a placeholder element
                    StackTraceElement fallbackElement = new StackTraceElement(
                            "UnparsableStackTrace", "fromLine", line, -1);
                    stackTraceElements.add(fallbackElement);
                }
            }

            // Set the stack trace on the main exception
            reconstructedException.setStackTrace(stackTraceElements.toArray(new StackTraceElement[0]));
        } catch (Exception e) {
            // If building the stack trace fails, fall back to a simpler approach
            reconstructedException = new Exception(
                    message + "\nOriginal exception: " + exceptionClass +
                            "\nCouldn't reconstruct stack trace properly: " + e.getMessage());
        }

        return reconstructedException;
    }
}
