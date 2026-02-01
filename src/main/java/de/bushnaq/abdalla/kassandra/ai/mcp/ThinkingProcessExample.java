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

/**
 * Example usage of AiAssistantService with thinking process extraction.
 * <p>
 * This demonstrates how to use both methods:
 * 1. processQuery() - Returns only the final answer (existing method)
 * 2. processQueryWithThinking() - Returns both the thinking process and final answer (new method)
 */
public class ThinkingProcessExample {

    public static void demonstrateUsage(AiAssistantService aiService, String conversationId) {
        String userQuery = "List all products";

        // Option 1: Get only the final answer (existing method)
        String answer = aiService.processQuery(userQuery, conversationId);
        System.out.println("Answer: " + answer);

        // Option 2: Get both thinking process and answer (new method)
        // This is useful for reasoning models like DeepSeek-R1
        QueryResult result = aiService.processQueryWithThinking(userQuery, conversationId);

        if (result.hasThinking()) {
            System.out.println("=== Thinking Process ===");
            System.out.println(result.thinking());
            System.out.println("\n=== Final Answer ===");
            System.out.println(result.content());
        } else {
            System.out.println("Answer: " + result.content());
        }

        // Or get formatted output with both
        System.out.println(result.getFullResponse());
    }
}
