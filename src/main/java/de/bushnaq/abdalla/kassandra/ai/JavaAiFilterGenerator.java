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

import de.bushnaq.abdalla.kassandra.ai.config.AiFilterConfig;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.function.Predicate;

import static de.bushnaq.abdalla.util.AnsiColorConstants.*;

/**
 * AI filter generator for Java predicates.
 * Converts natural language queries into Java code that gets compiled and executed on the fly.
 */
@Component
public class JavaAiFilterGenerator implements AiFilterGenerator {
    private static final String             JAVA_PROMPT_TEMPLATE   = """
            You are a Java method body generator for filtering Java objects. Convert natural language search queries into Java code that can be compiled and executed.
            
            IMPORTANT CONTEXT: You are filtering %s entities. The 'entity' parameter passed to your method is a %s object. This entity is never null.
            
            The Java class you'll be filtering has this structure:
            %s
            
            %s
            
            IMPORTANT RULES:
            - Generate ONLY the method body code that goes INSIDE a boolean test(%s entity) method. Do not include method signature or class declaration. do not write 'public boolean test(%s entity){...}'.
            - The method body should return true if the entity matches the search criteria, false otherwise.
            - You can access fields directly using entity.getFieldName() getter methods.
            - For date comparisons, use OffsetDateTime/LocalDateTime classes.
            - Handle exceptions gracefully - return false if any operation fails.
            - Use proper Java operators and control structures.
            - For string searches, use direct getter access with .toLowerCase().contains().
            - The entity parameter is already typed as %s, so you can access its getter methods directly.
            - Never use reflection - only use public getter methods like entity.getName(), entity.getCreated(), etc.
            - Return ONLY the method body code, no method signature, no class declaration.
            - Explain your thought process within <think></think> tags to indicate your thinking process.
            - Important: add ```java ``` tags to indicate the actual code you are returning.
            - CRITICAL: Do not ue LocalDate.now(). For current date/time reference operations, use the 'now' field (LocalDate now) that is available in the class instead of calling LocalDate.now() or similar methods.
              now or getNow() is not a field of the entity, but of the filter class itself.
              Example: Use 'now' instead of 'LocalDate.now()' for getting the current date.
              Example: Use 'now.minusDays(7)' for getting a week ago from the current date.
            - CRITICAL DATE INTERPRETATION: When filtering by "after [Month] [Year]" (e.g., "after July 2024"):
              * This means after the ENTIRE month has ended
              * Use the LAST day of that month at 23:59:59, not the first day
              * Example: "after July 2024" means after 2024-07-31 23:59:59, NOT after 2024-07-01
              * Example: "after March 2025" means after 2025-03-31 23:59:59, NOT after 2025-03-01
              * Alternatively, use the first day of the NEXT month: "after July 2024" = on or after 2024-08-01 00:00:00
            
            %s
            
            Now generate a Java method body for this EXACT query:
            "%s"
            """;
    private static final Logger             logger                 = LoggerFactory.getLogger(JavaAiFilterGenerator.class);
    private final        ChatClient         chatModel;
    public static        int                compilerExceptionCount = 0;
    private final        JavaFilterCompiler javaFilterCompiler;

    public JavaAiFilterGenerator(ChatClient.Builder builder, JavaFilterCompiler javaFilterCompiler) {
        this.chatModel          = builder.build();
        this.javaFilterCompiler = javaFilterCompiler;
    }

    /**
     * Builds an enhanced query that includes feedback from previous compilation attempts.
     */
    private String buildQueryWithFeedback(String originalQuery, String previousCode, String previousError, int attemptNumber) {
        if (attemptNumber == 1 || previousCode == null || previousError == null) {
            // First attempt - use original query
            return originalQuery;
        }

        // Build feedback-enhanced query for subsequent attempts
        String feedbackQuery = "IMPORTANT: The previous attempt failed to compile. Please fix the compilation issues.\n\n" +
                "Original query: \"" + originalQuery + "\"\n\n" +
                "Previous failed code:\n" +
//                "<java>\n" +
                previousCode +
//                "\n</java>\n" +
                "\n\nCompilation errors:\n" +
                previousError +
                "\n" +
                "Please generate corrected Java method body code that fixes these compilation errors " +
                "while still implementing the original query: \"" + originalQuery + "\"";

        return feedbackQuery;
    }

    private String extractJavaCodeFromResponse(String content) {
        //extract all between ```java and ```
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        String trimmedContent = content.trim();

        // More flexible regex that handles various markdown code block formats
        String javaCode = trimmedContent.replaceAll("(?s).*?```java\\s*\\n?(.*?)\\n?```.*", "$1").trim();

        // If the above didn't match, try without the java language specifier
        if (javaCode.equals(trimmedContent.trim())) {
            javaCode = trimmedContent.replaceAll("(?s).*?```\\s*\\n?(.*?)\\n?```.*", "$1").trim();
        }

        return javaCode;
    }

    @Override
    public String generateFilter(String query, String entityType) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

//        logger.info("Generating Java filter for query: '{}' and entity type: '{}'", query, entityType);

        try {
            AiFilterConfig.PromptConfig config = AiFilterConfig.getPromptConfig(entityType);

            // Create prompt with current year context and entity-specific configuration
            String formattedPrompt = String.format(JAVA_PROMPT_TEMPLATE,
                    entityType,           // You are filtering %s entities
                    entityType,           // The 'entity' parameter passed to your method is already a %s object
                    config.javaClass,     // The Java class you'll be filtering has this structure:
                    config.specialConsiderations,
                    entityType,           // Generate ONLY the method body code that goes inside a boolean test(%s entity) method
                    entityType,           // Do not include method signature or class declaration. do not write boolean test(%s entity){}
                    entityType,           // The entity parameter is already typed as %s, so you can access its methods and fields directly
                    config.javaExamples,
                    query);

            // Create prompt and get response using Spring AI
            Prompt prompt = new Prompt(formattedPrompt);

            System.out.printf("Java LLM prompt for '%s%s%s'\n%s%s%s\n\n", ANSI_BLUE, entityType, ANSI_RESET, ANSI_GREEN, formattedPrompt, ANSI_RESET);
//            logger.debug("Java LLM prompt for '{}': {}", entityType, formattedPrompt);

            ChatClient.ChatClientRequestSpec request  = chatModel.prompt(prompt);
            ChatClient.CallResponseSpec      response = request.call();
            String                           content  = response.content();

            System.out.printf("Java LLM raw response\n\n%s%s%s\n\n", ANSI_YELLOW, content, ANSI_RESET);
//            logger.debug("Java LLM raw response: {}", content);

            // Extract actual answer from response (remove thinking process)
            String extractedAnswerWithoutThinking = removeThinkingFromResponse(content);
            String extractedCode                  = extractJavaCodeFromResponse(extractedAnswerWithoutThinking);

            System.out.printf("Java LLM extracted answer\n\n%s%s%s\n\n", ANSI_YELLOW, extractedCode, ANSI_RESET);
//            logger.debug("Java LLM extracted answer: {}", extractedCode);

            if (extractedCode == null || extractedCode.trim().isEmpty()) {
                logger.warn("Java code generation failed, result is empty");
                throw new RuntimeException("Java code generation failed, result is empty");
            }

            return extractedCode.trim();

        } catch (Exception e) {
            logger.error("Error generating Java filter: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Java filter: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a Java filter and compiles it into a Predicate.
     *
     * @param query      The natural language query from the user
     * @param entityType The type of entity being searched (e.g., "Product", "Version")
     * @param now        The current date to use for date-based filtering (for testing purposes)
     * @param <T>        The entity type
     * @return A compiled Predicate that can be used to filter entities
     * @throws RuntimeException if compilation fails
     */
    public <T> Predicate<T> generatePredicate(String query, String entityType, LocalDate now) {
        Profiler.setAbbreviatedReport(true); // Enable abbreviated report for performance profiling
        try (Profiler timeKeeping1 = new Profiler(this.getClass().getSimpleName(), SampleType.AI)) {
            if (query == null || query.trim().isEmpty()) {
                return entity -> true; // Return a predicate that matches everything
            }

            logger.info("Generating Java Predicate for query: '{}' and entity type: '{}'", query, entityType);

            return generatePredicateWithFeedback(query, entityType, now, 5);
        }
    }

    /**
     * Generates a predicate with iterative feedback mechanism.
     * If compilation fails, provides error details to the AI for correction.
     */
    private <T> Predicate<T> generatePredicateWithFeedback(String query, String entityType, LocalDate now, int maxRetries) {
        String previousCode  = null;
        String previousError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String generatedJavaCode = null;
            try {
                // Build the prompt with feedback from previous attempts
                String effectiveQuery = buildQueryWithFeedback(query, previousCode, previousError, attempt);

                // Generate Java code using LLM
                generatedJavaCode = generateFilter(effectiveQuery, entityType);

                // Compile the Java code and return the Predicate
                try (Profiler timeKeeping2 = new Profiler(this.getClass().getSimpleName(), SampleType.CPU)) {
                    return javaFilterCompiler.compileFilter(generatedJavaCode, entityType, now);
                }

            } catch (Exception e) {
                Throwable t = e.getCause();
                if (t instanceof JavaCompilationException) {
                    previousCode  = generatedJavaCode;//((JavaCompilationException) t).getFailedCode();
                    previousError = ((JavaCompilationException) t).getAiFeedbackMessage();
                    compilerExceptionCount++;

                    logger.warn("Compilation failed on attempt {} for query '{}': {}", attempt, query, e.getMessage());

                    if (attempt == maxRetries) {
                        logger.error("Failed to generate compilable Java Predicate after {} attempts for query: '{}'", maxRetries, query);
                        throw new RuntimeException("Failed to generate compilable Java Predicate after " + maxRetries + " attempts: " + t.getMessage(), e);
                    }

                    // Log the feedback that will be provided to the AI
//                    logger.debug("Providing feedback to AI for attempt {}: {}", attempt + 1, previousError);
                } else {
                    logger.error("Unexpected error on attempt {} for query '{}': {}", attempt, query, e.getMessage(), e);

                    // For unexpected errors, we don't have structured feedback, so treat it as a general failure
                    if (attempt == maxRetries) {
                        throw new RuntimeException("Failed to generate Java Predicate after " +
                                maxRetries + " attempts: " + e.getMessage(), e);
                    }
                }
            }
        }

        // This should never be reached due to the exception handling above
        return null;
    }

//    /**
//     * @deprecated Use generatePredicateWithFeedback(String, String, LocalDate, int) instead
//     */
//    @Deprecated
//    private <T> Predicate<T> generatePredicateWithFeedback(String query, String entityType, int maxRetries) {
//        return generatePredicateWithFeedback(query, entityType, LocalDate.now(), maxRetries);
//    }

    @Override
    public FilterType getFilterType() {
        return FilterType.JAVA;
    }

    /**
     * Extract the actual answer from AI response by removing thinking process.
     */
    private String removeThinkingFromResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return rawResponse;
        }

        String response = rawResponse.trim();

        // Remove content between thinking tags
        response = response.replaceAll("(?s)<think>.*?</think>", "").trim();
        response = response.replaceAll("(?s)<thinking>.*?</thinking>", "").trim();
        response = response.replaceAll("(?s)<!--\\s*thinking.*?-->", "").trim();

        // Remove lines that start with reasoning markers
        response = response.replaceAll("(?m)^(Thinking:|Let me think:).*$", "").trim();

        // Extract content after answer markers
        if (response.matches("(?s).*\\b(Answer|Result|Output):\\s*(.*)")) {
            String[] parts = response.split("\\b(?:Answer|Result|Output):\\s*", 2);
            if (parts.length > 1) {
                response = parts[1].trim();
            }
        }

        return response.isEmpty() ? rawResponse : response;
    }
}
