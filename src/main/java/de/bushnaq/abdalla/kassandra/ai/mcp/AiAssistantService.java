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

import de.bushnaq.abdalla.kassandra.ai.mcp.api.feature.FeatureTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.sprint.SprintTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.user.UserTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.version.VersionTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Assistant Service that uses Spring AI's native tool calling.
 * Uses Spring AI's ChatMemory and MessageChatMemoryAdvisor for conversation history.
 * Tools are defined using @Tool annotation in dedicated tool classes.
 */
@Service
@Slf4j
public class AiAssistantService {

    private static final String                                  SYSTEM_PROMPT        = """
            You are an AI assistant that helps manage a project management system.
            You have access to various tools to interact with the system.
            Use the available tools when needed to fulfill user requests.
            After using tools, provide helpful and concise responses based on the results.
            If you don't need to use any tools, just provide a direct answer.
            """;
    // Store ToolActivityContext per conversation/session for UI streaming
    private final        Map<String, SessionToolActivityContext> activityContexts     = new ConcurrentHashMap<>();
    @Autowired
    private              OllamaChatModel                         chatModel;
    // Store ChatMemory per conversation/session
    private final        Map<String, ChatMemory>                 conversationMemories = new ConcurrentHashMap<>();
    @Autowired
    private              FeatureTools                            featureTools;
    @Autowired
    private              ProductTools                            productTools;
    @Autowired
    private              SprintTools                             sprintTools;
    @Autowired
    private              UserTools                               userTools;
    @Autowired
    private              VersionTools                            versionTools;

    /**
     * Clear the conversation history for a specific conversation ID
     */
    public void clearConversation(String conversationId) {
        conversationMemories.remove(conversationId);
        log.info("Cleared conversation history for: {}", conversationId);
    }

    /**
     * Get the activity context for a conversation (for UI streaming).
     * Always returns a context, creating and storing a new one if needed.
     */
    public SessionToolActivityContext getActivityContext(String conversationId) {
        SessionToolActivityContext activityContext = activityContexts.computeIfAbsent(conversationId, id -> new SessionToolActivityContext());
        ToolActivityContextHolder.setContext(activityContext);
        return activityContext;
    }

    /**
     * Dynamically build a list of available tools from all registered tool beans using reflection.
     */
    public String getAvailableTools() {
        Object[]      toolBeans = {productTools, userTools, versionTools, featureTools, sprintTools};
        StringBuilder sb        = new StringBuilder();
        for (Object bean : toolBeans) {
            for (Method method : bean.getClass().getMethods()) {
                Tool toolAnnotation = method.getAnnotation(Tool.class);
                if (toolAnnotation != null) {
                    sb.append("• ")
                            .append(method.getName())
                            .append(": ")
                            .append(toolAnnotation.description())
                            .append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Get or create a ChatMemory for the given conversation ID
     */
    private ChatMemory getOrCreateMemory(String conversationId) {
        return conversationMemories.computeIfAbsent(conversationId, id -> {
            log.info("Creating new ChatMemory for conversation: {}", id);
            return MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(20) // Keep last 20 messages in context
                    .build();
        });
    }

    /**
     * Process a user query using the AI assistant with Spring AI native tool calling.
     * Tools are automatically discovered from classes with @Tool annotated methods.
     *
     * @param userQuery      The user's query
     * @param conversationId Unique identifier for this conversation session
     */
    public String processQuery(String userQuery, String conversationId) {
        log.info("=== Starting AI query processing ===");

        // Log model information
        try {
            String modelName = chatModel.getDefaultOptions().getModel();
            log.info("Using LLM Model: {}", modelName != null ? modelName : "default");
        } catch (Exception e) {
            log.debug("Could not determine model name: {}", e.getMessage());
        }

        log.info("User query: {}", userQuery);
        log.info("Conversation ID: {}", conversationId);

        // Get or create the ChatMemory for this conversation
        ChatMemory chatMemory = getOrCreateMemory(conversationId);

        // Create MessageChatMemoryAdvisor with conversation ID
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(conversationId)
                .build();

        // Create ChatClient with:
        // - System prompt defining the assistant's role
        // - Memory advisor for conversation history
        // - Tool beans with @Tool annotated methods
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(memoryAdvisor)
                .defaultTools(productTools, userTools, versionTools, featureTools, sprintTools)
                .build();

        // Create or get the activity context for this conversation
//        SessionToolActivityContext activityContext = activityContexts.computeIfAbsent(conversationId, id -> new SessionToolActivityContext());
        // Store context in ThreadLocal for tool access
//        ToolActivityContextHolder.setContext(activityContext);
        try {
            log.info("Calling LLM via ChatClient with native Spring AI tool support...");
            String response = chatClient.prompt()
                    .user(userQuery)
                    .call()
                    .content();
            log.info("=== AI query processing complete ===");
            log.info("Final response length: {} characters", response != null ? response.length() : 0);
            return response;
        } finally {
            ToolActivityContextHolder.clear();
        }
    }
}
