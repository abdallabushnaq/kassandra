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

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.feature.FeatureTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductAclTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.sprint.SprintTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.user.UserTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.usergroup.UserGroupTools;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.version.VersionTools;
import de.bushnaq.abdalla.profiler.TimeKeeping;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.augment.AugmentedToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static de.bushnaq.abdalla.util.AnsiColorConstants.*;

/**
 * AI Assistant Service that uses Spring AI's native tool calling.
 * Uses Spring AI's ChatMemory and MessageChatMemoryAdvisor for conversation history.
 * Tools are defined using @Tool annotation in dedicated tool classes.
 */
@Service
@Slf4j
public class AiAssistantService {

    /**
     * Maximum number of user+AI messages kept in the snapshot.
     * tool/system/error messages are also stored for UI replay but do not count toward this limit,
     * matching the agent's ChatMemory window which only tracks user and AI turns.
     */
    public static final  int                                     MAX_MESSAGES         = 20;
    private static final String                                  SYSTEM_PROMPT        = """
            - You are Kassandra an AI assistant that helps manage a project management system.
            - Keep your answers short and to the point. Use tools to get information instead of making assumptions.
            - Careful when you are using an ID in a tool, make sure it is the correct one, you will not be able to guess an ID.
            - The ID is the unique identifier of an entity in the system, for example a product, version, feature or sprint. You can only get an ID by using a tool that returns it, you cannot make up an ID or guess it.
            - updating or deleting operations need an ID. You can get elements by name to obtain their ID. 
            - Your answer should be complete. Do not show templates or create answers using fake values.
            - Do not invent any parameters, optional parameters can be ignored, if you do not know the value.
            - Take a deep breath and think step-by-step.
            - Read the question carefully and try to find a solution for it.
            - When asked to delete, update, or create items, you MUST call the appropriate tool. Never fabricate a confirmation message. Never claim you performed an action without calling a tool first.
            - Explain your thought process within <think></think> tags to indicate your thinking process.
            - The system is made of a hierarchical structure of a list of Product(s), each product has a list of Version(s), each Version has a list of Feature(s) and each Feature has a list of Sprint(s).
            - This hierarchy is linked together using foreign keys. For example, a Version has a productId to its parent Product, a Feature has a versionId to its parent Version and a Sprint has a featureId to its parent Feature.
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
    private              ProductAclTools                         productAclTools;
    @Autowired
    private              ProductTools                            productTools;
    @Autowired
    private              SprintTools                             sprintTools;
    @Autowired
    private              UserGroupTools                          userGroupTools;
    @Autowired
    private              UserTools                               userTools;
    @Autowired
    private              VersionTools                            versionTools;

    private String augmentSystemPrompt(String systemPrompt) {
        return "Today is " + DateUtil.createDateString(ParameterOptions.getNow().toLocalDate(), DateTimeFormatter.ISO_LOCAL_DATE) + ". " + systemPrompt;
    }

    /**
     * Clear the conversation history for a specific conversation ID
     */
    public void clearConversation(String conversationId) {
        conversationMemories.remove(conversationId);
        activityContexts.remove(conversationId);
        log.info("Cleared conversation history for: {}", conversationId);
    }

    private List<AugmentedToolCallbackProvider<AgentThinking>> createToolCallbackProviders(Object[] userTools, List<ThinkingStep> thinkingSteps) {
        List<AugmentedToolCallbackProvider<AgentThinking>> augmentedToolCallbackProvider = new ArrayList<AugmentedToolCallbackProvider<AgentThinking>>();
        for (Object userTool : userTools) {
            AugmentedToolCallbackProvider<AgentThinking> toolProvider = AugmentedToolCallbackProvider
                    .<AgentThinking>builder()
                    .toolObject(userTool)
                    .argumentType(AgentThinking.class)
                    .argumentConsumer(event -> {
                        AgentThinking thinking = event.arguments();
                        log.info("{}Tool {}{}: {}{}{}", ANSI_GRAY, event.toolDefinition().name(), ANSI_RESET, ANSI_DARK_GRAY, thinking.innerThought(), ANSI_RESET);
                        thinkingSteps.add(ThinkingStep.create(event.toolDefinition().name(), thinking));
                    })
                    .removeExtraArgumentsAfterProcessing(true)
                    .build();
            augmentedToolCallbackProvider.add(toolProvider);
        }
        return augmentedToolCallbackProvider;
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
        Object[]      toolBeans = {productTools, productAclTools, userTools, userGroupTools, versionTools, featureTools, sprintTools};
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
//            log.info("Creating new ChatMemory for conversation: {}", id);
            return MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(MAX_MESSAGES)
                    .build();
        });
    }

    /**
     * Process a user query using the AI assistant with Spring AI native tool calling.
     * Returns the full ChatResponse which includes metadata and thinking process for reasoning models.
     *
     * @param userQuery      The user's query
     * @param conversationId Unique identifier for this conversation session
     * @return QueryResult containing both the thinking process and final content
     */
    public QueryResult processQueryWithThinking(String username, String userQuery, String conversationId) {
        try (TimeKeeping t = new TimeKeeping()) {
            log.info("{}{}: {}{}{}", ANSI_YELLOW, username, ANSI_BLUE, userQuery, ANSI_RESET);
            List<ThinkingStep> thinkingSteps = new ArrayList<>();

            // Log model information
            try {
                String modelName = chatModel.getDefaultOptions().getModel();
            } catch (Exception e) {
                log.debug("Could not determine model name: {}", e.getMessage());
            }

            // Get or create the ChatMemory for this conversation
            ChatMemory chatMemory = getOrCreateMemory(conversationId);

            // Create MessageChatMemoryAdvisor with conversation ID
            MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(conversationId)
                    .build();

            Object[]                                           userTools                     = {this.userTools, userGroupTools, productTools, productAclTools, versionTools, featureTools, sprintTools};
            List<AugmentedToolCallbackProvider<AgentThinking>> augmentedToolCallbackProvider = createToolCallbackProviders(userTools, thinkingSteps);

            // Create ChatClient with:
            // - System prompt defining the assistant's role
            // - Memory advisor for conversation history
            // - Tool beans with @Tool annotated methods
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultSystem(augmentSystemPrompt(SYSTEM_PROMPT))
                    .defaultAdvisors(memoryAdvisor)
                    .defaultToolCallbacks(augmentedToolCallbackProvider.get(0), augmentedToolCallbackProvider.get(1), augmentedToolCallbackProvider.get(2), augmentedToolCallbackProvider.get(3), augmentedToolCallbackProvider.get(4), augmentedToolCallbackProvider.get(5), augmentedToolCallbackProvider.get(6))
                    .build();

            // Build toolContext with auth token and activity context for propagation
            // to @Tool methods on any thread Spring AI may use.
            SessionToolActivityContext    activityCtx    = activityContexts.computeIfAbsent(conversationId, id -> new SessionToolActivityContext());
            java.util.Map<String, Object> toolContextMap = ToolContextHelper.buildContextMap(null, activityCtx);

            ChatResponse chatResponse = chatClient.prompt(userQuery)
                    .toolContext(toolContextMap)
                    .call()
                    .chatResponse();  // Get full ChatResponse instead of just text
//                ResponseWithReasoning chatResponse = chatClient.prompt(userQuery)
//                        .call()
//                        .entity(ResponseWithReasoning.class);

            // Add defensive null checks for LMStudio compatibility
            if (chatResponse == null) {
                log.error("ChatResponse is null - LMStudio returned no response");
                throw new RuntimeException("LLM returned null response");
            }

//            log.debug("ChatResponse metadata: {}", chatResponse.getMetadata());

//            log.debug("ChatResponse results count: {}", chatResponse.getResults() != null ? chatResponse.getResults().size() : "null");

            if (chatResponse.getResult() == null) {
                log.error("ChatResponse.getResult() is null - LMStudio response may be malformed");
                log.error("Full ChatResponse: {}", chatResponse);
                throw new RuntimeException("LLM response has no result. Check if LMStudio model is loaded and responding correctly.");
            }

//                if (chatResponse.getResult().getOutput() == null) {
//                    log.error("ChatResponse.getResult().getOutput() is null");
//                    throw new RuntimeException("LLM response result has no output");
//                }
//                Generation                      result    = chatResponse.getResult();
//                AssistantMessage                output    = chatResponse.getResult().getOutput();
//                List<AssistantMessage.ToolCall> toolCalls = chatResponse.getResult().getOutput().getToolCalls();
//                String                          text      = chatResponse.getResult().getOutput().getText();
//                String text = chatResponse.getResult().getOutput().getText();
//                String thinking = chatResponse.thinking().innerThought();
            String thinking = extractThinkingProcess(chatResponse);
            String text     = removeThinkingFromResponse(chatResponse.getResult().getOutput().getText());
//            log.info("=== AI query processing complete ===");
//            log.info("Final response length: {} characters", text != null ? text.length() : 0);
//            if (thinking != null && !thinking.isEmpty()) {
//                log.info("Thinking process length: {} characters", thinking.length());
//            }

            QueryResult queryResult = new QueryResult(text, thinkingSteps);
            String      modelName   = getModelName();
            String      response    = text;
//            if (result.hasThinking()) {
//                for (ThinkingStep step : result.thinkingSteps()) {
//                    log.info("{}Tool{}{}: {}{}{}", ANSI_GRAY, step.toolName(), ANSI_RESET, ANSI_DARK_GRAY, step.agentThinking().innerThought(), ANSI_RESET);
//                }
//            }
            if (thinking != null && !thinking.isEmpty())
                log.info("{}({}ms) {}: {}{}{}", ANSI_BLUE, t.getDelta().getNano() / 1000000, modelName, ANSI_YELLOW, thinking, ANSI_RESET);
            log.info("{}({}ms) {}: {}{}{}", ANSI_YELLOW, t.getDelta().getNano() / 1000000, modelName, ANSI_GREEN, response, ANSI_RESET);
            return queryResult;
        }
    }

    /**
     * Extract the actual answer from AI response by removing thinking process.
     * Public so that {@link de.bushnaq.abdalla.kassandra.ui.component.ChatAgentPanel}
     * can strip thinking tokens from the accumulated streaming text at completion.
     */
    public String removeThinkingFromResponse(String rawResponse) {
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

    /**
     * Set the chat model. Useful for testing with different models.
     *
     * @param chatModel The ChatModel to use
     */
    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
        log.debug("Chat model updated");
    }

    /**
     * Stream a user query using the AI assistant with Spring AI native tool calling.
     * Returns a Flux of text token chunks that can be subscribed to for progressive UI updates.
     * Tool calls are still executed synchronously before text tokens begin streaming.
     * <p>
     * Authentication and activity context are propagated to @Tool methods via Spring AI's
     * {@link org.springframework.ai.chat.model.ToolContext}, not ThreadLocals.
     * <p>
     * Note: thinking tokens (e.g. {@code <think>…</think>}) may appear in the stream briefly.
     * The caller is responsible for stripping them from the accumulated text at completion
     * using {@link #removeThinkingFromResponse(String)}.
     *
     * @param username       The username of the requesting user (for logging)
     * @param userQuery      The user's query
     * @param conversationId Unique identifier for this conversation session
     * @param capturedToken  OIDC bearer token captured on the UI thread (may be null for test mode)
     * @return Flux of raw text token chunks
     */
    public Flux<String> streamQuery(String username, String userQuery, String conversationId,
                                    String capturedToken) {
        log.info("{}{}: {}{}{} [streaming]", ANSI_YELLOW, username, ANSI_BLUE, userQuery, ANSI_RESET);

        List<ThinkingStep> thinkingSteps = new ArrayList<>();
        ChatMemory         chatMemory    = getOrCreateMemory(conversationId);
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(conversationId)
                .build();

        Object[]                                           toolBeans                     = {this.userTools, userGroupTools, productTools, productAclTools, versionTools, featureTools, sprintTools};
        List<AugmentedToolCallbackProvider<AgentThinking>> augmentedToolCallbackProvider = createToolCallbackProviders(toolBeans, thinkingSteps);

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem(augmentSystemPrompt(SYSTEM_PROMPT))
                .defaultAdvisors(memoryAdvisor)
                .defaultToolCallbacks(augmentedToolCallbackProvider.get(0), augmentedToolCallbackProvider.get(1), augmentedToolCallbackProvider.get(2), augmentedToolCallbackProvider.get(3), augmentedToolCallbackProvider.get(4), augmentedToolCallbackProvider.get(5), augmentedToolCallbackProvider.get(6))
                .build();

        // Build toolContext with auth token and activity context — propagated to every
        // @Tool method by Spring AI regardless of which thread executes the tool.
        SessionToolActivityContext    activityCtx    = activityContexts.computeIfAbsent(conversationId, id -> new SessionToolActivityContext());
        java.util.Map<String, Object> toolContextMap = ToolContextHelper.buildContextMap(capturedToken, activityCtx);

        return chatClient.prompt(userQuery)
                .toolContext(toolContextMap)
                .stream()
                .chatResponse()
                .filter(r -> r != null
                        && r.getResult() != null
                        && r.getResult().getOutput() != null
                        && r.getResult().getOutput().getText() != null
                        && !r.getResult().getOutput().getText().isEmpty())
                .map(r -> {
                    String text = r.getResult().getOutput().getText();
                    return text != null ? text : "";
                })
                .filter(t -> !t.isEmpty());
    }
}
