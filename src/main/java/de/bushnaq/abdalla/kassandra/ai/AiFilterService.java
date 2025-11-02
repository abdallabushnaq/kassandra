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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Main AI Filter service that acts as a facade for different filter generators.
 * This refactored version delegates to specialized implementations for each filter type.
 */
@Service
public class AiFilterService {

    private final   JavaAiFilterGenerator       javaGenerator;
    private final   JavaScriptAiFilterGenerator javaScriptGenerator;
    protected final Logger                      logger = LoggerFactory.getLogger(this.getClass());

    public AiFilterService(JavaScriptAiFilterGenerator javaScriptGenerator, JavaAiFilterGenerator javaGenerator) {
        this.javaScriptGenerator = javaScriptGenerator;
        this.javaGenerator       = javaGenerator;

    }

    /**
     * Helper method to simulate filtering products using JavaScript functions
     * Now uses GraalVM Context API for secure execution
     */
    public <T> List<T> applyJavaScriptSearchQuery(String jsFunction, List<T> testProducts, LocalDate now) throws Exception {
        // Define the JavaScript function once with the provided function body
        String completeFunction = String.format("function filterEntity(entity, now) {\n %s \n}", jsFunction);
        System.out.println(completeFunction);

        // Evaluate the function definition in the secure context
        try (Context jsContext = createContext()) {
            jsContext.eval("js", completeFunction);

            return testProducts.stream()
                    .filter(product -> {
                        try {
                            // Put the Java object into the JavaScript context
                            jsContext.getBindings("js").putMember("currentEntity", product);
                            jsContext.getBindings("js").putMember("currentDate", now);

                            // Call the JavaScript function and get the result as a Value
                            Value result = jsContext.eval("js", "filterEntity(currentEntity, currentDate)");


                            // Explicitly check for undefined and throw exception
                            if (result == null || result.isNull()) {
                                throw new RuntimeException("JavaScript function returned null/undefined for entity: " + product);
                            }

                            // Convert JavaScript result to boolean
                            if (result.isBoolean()) {
                                return result.asBoolean();
                            }
                            if (result.isNumber()) {
                                return result.asDouble() != 0.0;
                            }
                            if (result.isString()) {
                                return !result.asString().isEmpty();
                            }
                            // For other objects, check if they're truthy
                            return true; // Non-null, non-primitive objects are truthy
                        } catch (Exception e) {
                            logger.warn("JavaScript execution failed for product: {}", e.getMessage());
                            e.printStackTrace(); // Add stack trace for debugging
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    public <T> boolean applyJavaScriptSearchQuery(String jsFunction, T entity, LocalDate now) {
        try (Context jsContext = createContext()) {
            // Define the JavaScript function once with the provided function body
            String completeFunction = String.format("function filterEntity(entity, now) {\n %s \n}", jsFunction);
            System.out.println(completeFunction);

            // Evaluate the function definition in the secure context
            jsContext.eval("js", completeFunction);
//        try {
            // Put the Java object into the JavaScript context
            jsContext.getBindings("js").putMember("currentEntity", entity);
            jsContext.getBindings("js").putMember("currentDate", now);

            // Call the JavaScript function and get the result as a Value
            Value result = jsContext.eval("js", "filterEntity(currentEntity, currentDate)");


            // Explicitly check for undefined and throw exception
            if (result == null || result.isNull()) {
                throw new RuntimeException("JavaScript function returned null/undefined for entity: " + entity);
            }

            // Convert JavaScript result to boolean
            if (result.isBoolean()) {
                return result.asBoolean();
            }
            if (result.isNumber()) {
                return result.asDouble() != 0.0;
            }
            if (result.isString()) {
                return !result.asString().isEmpty();
            }
            // For other objects, check if they're truthy
            return true; // Non-null, non-primitive objects are truthy
//        } catch (Exception e) {
//            logger.warn("JavaScript execution failed for product: {}", e.getMessage());
//            e.printStackTrace(); // Add stack trace for debugging
//            return false;
//        }
        }
    }

    private Context createContext() {
        final Context jsContext;
        // Create secure GraalVM Context with comprehensive security settings
        HostAccess secureHostAccess = HostAccess.newBuilder(HostAccess.EXPLICIT)
                .allowPublicAccess(true)  // Allow calling public methods like getName()
                .build();

        ResourceLimits secureResourceLimits = ResourceLimits.newBuilder()
                .statementLimit(200, null)                    // Limit to 200 statements
                .build();
        jsContext = Context.newBuilder("js")
                // Security: Comprehensive access controls
                .allowHostAccess(secureHostAccess)           // Limited host access for getter methods
                .allowIO(IOAccess.NONE)                      // ❌ No file system access
                .allowNativeAccess(false)                    // ❌ No native code execution
                .allowCreateThread(false)                    // ❌ No thread creation
                // Allow specific Java time classes for date operations
                .allowHostClassLookup(className -> {
                    return
//                            className.equals("java.time.OffsetDateTime") ||
//                            className.equals("java.time.LocalDateTime") ||
//                            className.equals("java.time.LocalDate") ||
//                            className.equals("java.time.Year") ||
//                            className.equals("java.time.Duration") ||
//                            className.equals("java.time.ZoneOffset") ||
                            className.equals("java.lang.String") ||
                                    className.equals("java.lang.Integer") ||
                                    className.equals("java.lang.Boolean") ||
                                    className.startsWith("java.time.") ||
                                    className.equals("de.bushnaq.abdalla.kassandra.dto.Status") ||
                                    className.equals("de.bushnaq.abdalla.kassandra.dto.OffDayType");
                })
                .allowEnvironmentAccess(org.graalvm.polyglot.EnvironmentAccess.NONE) // ❌ No env variables
                .allowPolyglotAccess(org.graalvm.polyglot.PolyglotAccess.NONE)       // ❌ No other languages
                .resourceLimits(secureResourceLimits)        // Resource limits for DoS protection
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        return jsContext;
    }

    /**
     * Parses a natural language search query using LLM with the specified filter type.
     *
     * @param query      The natural language query from the user
     * @param entityType The type of entity being searched (e.g., "Product", "Version")
     * @param filterType The type of filter to generate (REGEX, JAVASCRIPT, or JAVA)
     * @return Filter string for filtering objects (format depends on filter type)
     */
    public String parseQuery(String query, String entityType, AiFilterGenerator.FilterType filterType) {
        return switch (filterType) {
            case JAVASCRIPT -> javaScriptGenerator.generateFilter(query, entityType);
            case JAVA -> javaGenerator.generateFilter(query, entityType);
        };
    }

    /**
     * Parses a natural language search query and returns a compiled Java Predicate.
     *
     * @param query      The natural language query from the user
     * @param entityType The type of entity being searched (e.g., "Product", "Version")
     * @param now        The current date to use for date-based filtering (for testing purposes)
     * @param <T>        The entity type
     * @return A compiled Predicate that can be used to filter entities
     * @throws RuntimeException if compilation fails
     */
    public <T> Predicate<T> parseQueryToPredicate(String query, String entityType, LocalDate now) {
        return javaGenerator.generatePredicate(query, entityType, now);
    }

}
