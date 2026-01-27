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

package de.bushnaq.abdalla.kassandra.ai;

import de.bushnaq.abdalla.kassandra.mcp.McpServer;
import de.bushnaq.abdalla.kassandra.mcp.McpTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Assistant Service that uses MCP tools to interact with the API
 */
@Service
@Slf4j
public class AiAssistantService {

    private static final int MAX_ITERATIONS = 5; // Prevent infinite loops

    @Autowired
    private OllamaChatModel chatModel;
    @Autowired
    private JsonMapper      jsonMapper;
    @Autowired
    private McpServer       mcpServer;

    /**
     * Build the system prompt with available tools
     */
    private String buildSystemPrompt() {
        List<McpTool> tools = mcpServer.listTools();

        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI assistant that helps manage a project management system. ");
        sb.append("You have access to the following API tools:\n\n");

        for (McpTool tool : tools) {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
            try {
                String params = jsonMapper.writeValueAsString(tool.getInputSchema());
                sb.append("  Parameters: ").append(params).append("\n");
            } catch (Exception e) {
                log.error("Error serializing tool params", e);
            }
        }

        sb.append("\nTo use a tool, respond with the following format:\n");
        sb.append("TOOL_CALL: tool_name\n");
        sb.append("ARGUMENTS: {\"param1\": \"value1\", \"param2\": \"value2\"}\n\n");
        sb.append("After receiving tool results, process them and provide a helpful response to the user.\n");
        sb.append("If you don't need to use any tools, just provide a direct answer.\n");

        return sb.toString();
    }

    /**
     * Extract tool call from AI response
     */
    private Map<String, Object> extractToolCall(String aiResponse) {
        Pattern toolPattern = Pattern.compile("TOOL_CALL:\\s*([\\w_]+)", Pattern.CASE_INSENSITIVE);
        Pattern argsPattern = Pattern.compile("ARGUMENTS:\\s*(.+?)(?=\\n\\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher toolMatcher = toolPattern.matcher(aiResponse);
        Matcher argsMatcher = argsPattern.matcher(aiResponse);

        if (toolMatcher.find() && argsMatcher.find()) {
            String toolName = toolMatcher.group(1);
            String argsJson = argsMatcher.group(1).trim();

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = jsonMapper.readValue(argsJson, Map.class);
                return Map.of("name", toolName, "arguments", arguments);
            } catch (Exception e) {
                log.error("Error parsing tool arguments: {}", argsJson, e);
            }
        }

        return null;
    }

    /**
     * Get a list of available tools as a formatted string
     */
    public String getAvailableTools() {
        List<McpTool> tools = mcpServer.listTools();
        StringBuilder sb    = new StringBuilder();

        for (McpTool tool : tools) {
            sb.append("• ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Process a user query using the AI assistant with MCP tools.
     * Note: The caller must ensure mcpAuthProvider.captureCurrentUserToken() is called
     * before invoking this method if running in a different thread context.
     */
    public String processQuery(String userQuery) {
        log.info("=== Starting AI query processing ===");

        // Log model information
        try {
            String modelName = chatModel.getDefaultOptions().getModel();
            log.info("Using LLM Model: {}", modelName != null ? modelName : "default");
        } catch (Exception e) {
            log.debug("Could not determine model name: {}", e.getMessage());
        }

        log.info("User query: {}", userQuery);

        List<String> conversationHistory = new ArrayList<>();

        // Build initial prompt with available tools
        String systemPrompt = buildSystemPrompt();
        log.debug("System prompt length: {} characters", systemPrompt.length());
        conversationHistory.add(systemPrompt);
        conversationHistory.add("User: " + userQuery);

        String finalResponse = "";
        int    iteration     = 0;

        while (iteration < MAX_ITERATIONS) {
            iteration++;
            log.info("--- Iteration {} ---", iteration);

            // Get AI response
            String fullPrompt = String.join("\n\n", conversationHistory);
            log.debug("Full prompt length: {} characters", fullPrompt.length());

            log.info("Calling LLM...");
            ChatResponse response   = chatModel.call(new Prompt(fullPrompt));
            String       aiResponse = response.getResult().getOutput().getText();

            log.info("AI response (iteration {}): {} characters", iteration, aiResponse.length());
            log.debug("AI response content: {}", aiResponse);
            conversationHistory.add("Assistant: " + aiResponse);

            // Check if AI wants to use a tool
            Map<String, Object> toolCall = extractToolCall(aiResponse);

            if (toolCall != null) {
                String toolName = (String) toolCall.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = (Map<String, Object>) toolCall.get("arguments");

                log.info("Executing tool: {} with arguments: {}", toolName, arguments);

                // Execute the tool
                String toolResult = mcpServer.executeTool(toolName, arguments);
                log.info("Tool '{}' returned {} characters", toolName, toolResult != null ? toolResult.length() : 0);
                log.debug("Tool result: {}", toolResult);

                // Add tool result to conversation
                conversationHistory.add("Tool Result: " + toolResult);

                // Continue the loop to let AI process the result
            } else {
                // No more tool calls, this is the final response
                log.info("No tool call detected, using response as final answer");
                finalResponse = aiResponse;
                break;
            }
        }

        if (iteration >= MAX_ITERATIONS) {
            log.warn("Reached maximum iterations ({}), returning partial response", MAX_ITERATIONS);
            finalResponse = "I apologize, but I've reached the maximum number of steps. Here's what I found so far:\n" + finalResponse;
        }

        log.info("=== AI query processing complete ===");
        log.info("Final response length: {} characters", finalResponse.length());

        return finalResponse;
    }
}
