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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
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
            You are Kassandra an AI assistant that helps manage a project management system.
            You have access to various tools to interact with the system.
            Use the available tools when needed to fulfill user requests.
            After using tools, provide helpful and concise responses based on the results.
            If you don't need to use any tools, just provide a direct answer.
            The system is made of a hierarchical structure of a list of Product(s), each product has a list of Version(s), each Version has a list of Feature(s) and each Feature has a list of Sprint(s).
            This hierarchy is linked together using foreign keys. For example, a Version has a productId to its parent Product, a Feature has a versionId to its parent Version and a Sprint has a featureId to its parent Feature.
            """;
    // Store ToolActivityContext per conversation/session for UI streaming
    private final        Map<String, SessionToolActivityContext> activityContexts     = new ConcurrentHashMap<>();
    @Autowired
    private              ChatModel                               chatModel;
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
     * Extract thinking/reasoning process from ChatResponse.
     * For reasoning models like DeepSeek-R1, this extracts the thinking tokens.
     */
    private String extractThinkingProcess(ChatResponse chatResponse) {
        try {
            // Null-safe checks for LMStudio compatibility
            if (chatResponse == null) {
                return null;
            }

            // Try to get thinking from metadata
            if (chatResponse.getMetadata() != null) {
                Object thinking = chatResponse.getMetadata().get("thinking");
                if (thinking != null) {
                    return thinking.toString();
                }
            }

            // Some models include thinking in the raw content between special tags
            if (chatResponse.getResult() != null &&
                    chatResponse.getResult().getOutput() != null) {
                String rawContent = chatResponse.getResult().getOutput().getText();
                if (rawContent != null) {
                    // Check for <think> tags (DeepSeek-R1 format)
                    if (rawContent.contains("<think>") && rawContent.contains("</think>")) {
                        int startIdx = rawContent.indexOf("<think>");
                        int endIdx   = rawContent.indexOf("</think>") + 8;
                        if (startIdx >= 0 && endIdx > startIdx) {
                            return rawContent.substring(startIdx + 7, endIdx - 8);
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.debug("Could not extract thinking process: {}", e.getMessage());
            return null;
        }
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

    public String getModelName() {
        String modelName = chatModel.getDefaultOptions().getModel();
        return modelName != null ? modelName : "default";
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

    /**
     * Process a user query using the AI assistant with Spring AI native tool calling.
     * Returns the full ChatResponse which includes metadata and thinking process for reasoning models.
     *
     * @param userQuery      The user's query
     * @param conversationId Unique identifier for this conversation session
     * @return QueryResult containing both the thinking process and final content
     */
    public QueryResult processQueryWithThinking(String userQuery, String conversationId) {
        log.info("=== Starting AI query processing (with thinking) ===");

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

        try {
            log.info("Calling LLM via ChatClient with native Spring AI tool support...");
            ChatResponse chatResponse = chatClient.prompt()
                    .user(userQuery)
                    .call()
                    .chatResponse();  // Get full ChatResponse instead of just content

            // Add defensive null checks for LMStudio compatibility
            if (chatResponse == null) {
                log.error("ChatResponse is null - LMStudio returned no response");
                throw new RuntimeException("LLM returned null response");
            }

            log.debug("ChatResponse metadata: {}", chatResponse.getMetadata());
            log.debug("ChatResponse results count: {}", chatResponse.getResults() != null ? chatResponse.getResults().size() : "null");

            if (chatResponse.getResult() == null) {
                log.error("ChatResponse.getResult() is null - LMStudio response may be malformed");
                log.error("Full ChatResponse: {}", chatResponse);
                throw new RuntimeException("LLM response has no result. Check if LMStudio model is loaded and responding correctly.");
            }

            if (chatResponse.getResult().getOutput() == null) {
                log.error("ChatResponse.getResult().getOutput() is null");
                throw new RuntimeException("LLM response result has no output");
            }
            List<AssistantMessage.ToolCall> toolCalls    = chatResponse.getResult().getOutput().getToolCalls();
            ChatGenerationMetadata          metadata     = chatResponse.getResult().getMetadata();
            String                          finishReason = metadata.getFinishReason();
            String                          content      = chatResponse.getResult().getOutput().getText();
            String                          thinking     = extractThinkingProcess(chatResponse);

            log.info("=== AI query processing complete ===");
            log.info("Final response length: {} characters", content != null ? content.length() : 0);
            if (thinking != null && !thinking.isEmpty()) {
                log.info("Thinking process length: {} characters", thinking.length());
            }

            return new QueryResult(content, thinking);
        } finally {
            ToolActivityContextHolder.clear();
        }
    }

    /**
     * Set the chat model. Useful for testing with different models.
     *
     * @param chatModel The ChatModel to use
     */
    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
        log.debug("Chat model updated");
    }
}
