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

package de.bushnaq.abdalla.kassandra.ui.component;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService;
import de.bushnaq.abdalla.kassandra.ai.mcp.SessionToolActivityContext;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.Disposable;

import java.util.List;

/**
 * Reusable chat agent panel component.
 * Encapsulates the full AI assistant chat UI: conversation history,
 * query input, submit/clear buttons, and async streaming logic.
 * Can be embedded in any Vaadin view.
 */
@Slf4j
public class ChatAgentPanel extends VerticalLayout {

    public static final String                                        AI_CLEAR_BUTTON   = "ai-clear-button";
    public static final String                                        AI_LAST_RESPONSE  = "ai-last-response";
    public static final String                                        AI_QUERY_INPUT    = "ai-query-input";
    public static final String                                        AI_RESPONSE_AREA  = "ai-response-area";
    public static final String                                        AI_SUBMIT_BUTTON  = "ai-submit-button";
    private volatile    boolean                                       activityStreaming = false;
    private final       AiAssistantService                            aiAssistantService;
    private final       Div                                           conversationHistory;
    private             String                                        conversationId;
    /**
     * Route key of the page currently using this panel. Set by restoreOrStart().
     */
    private             String                                        currentRouteKey;
    private             User                                          currentUser;
    /**
     * Called on the UI thread after every successful AI reply — use to refresh the host view's grid.
     */
    private             Runnable                                      onAiReply;
    private final       TextArea                                      queryInput;
    private final       Div                                           responseArea;
    // Plain list reference for the active slot — re-pointed on every restoreOrStart().
    // Safe to use from any thread because snapshotMessage() synchronises on it.
    private             List<ChatPanelSessionState.ChatMessageRecord> sessionMessages;
    // Direct reference to the session state bean — resolved once on the request thread.
    private final       ChatPanelSessionState                         sessionState;
    private final       Button                                        submitButton;
    private final       UserApi                                       userApi;
    /**
     * Current navigation context sentence prepended to every query so the AI knows which entities are selected.
     */
    private             String                                        viewContext       = null;

    public ChatAgentPanel(AiAssistantService aiAssistantService, UserApi userApi) {
        this(aiAssistantService, userApi, null);
    }

    public ChatAgentPanel(AiAssistantService aiAssistantService, UserApi userApi, ChatPanelSessionState sessionState) {
        this.aiAssistantService = aiAssistantService;
        this.userApi            = userApi;
        this.sessionState       = sessionState;
        // sessionMessages and conversationId are set by restoreOrStart() on first afterNavigation call.
        // Start with null / a temporary UUID so the component is valid before restoreOrStart() fires.
        this.sessionMessages = null;
        this.conversationId  = java.util.UUID.randomUUID().toString();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("overflow", "hidden");

        // Conversation history container (scrollable)
        conversationHistory = new Div();
        conversationHistory.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.SMALL
        );
        conversationHistory.setWidthFull();
        conversationHistory.getStyle()
                .set("overflow-y", "auto")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("padding", "4px")
                .set("flex", "1 1 auto")
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("font-family", "monospace");

        // Response area - takes all available space
        responseArea = new Div();
        responseArea.setId(AI_RESPONSE_AREA);
        responseArea.addClassNames(LumoUtility.BorderRadius.SMALL);
        responseArea.setWidthFull();
        responseArea.setSizeFull();
        responseArea.getStyle()
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("flex-direction", "column");
        responseArea.add(conversationHistory);

        // Query Input
        queryInput = new TextArea();
        queryInput.setId(AI_QUERY_INPUT);
        queryInput.setPlaceholder("Ask me anything... (Enter to send, Ctrl+Enter for new line)");
        queryInput.setWidthFull();
        queryInput.setSizeFull();
        queryInput.setMinHeight("40px");
        queryInput.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("--vaadin-input-field-font-size", "var(--lumo-font-size-xxs)");

        // Handle Enter key to submit
        queryInput.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        queryInput.getElement().executeJs(
                "this.inputElement.addEventListener('keydown', (e) => {" +
                        "  if (e.key === 'Enter' && !e.ctrlKey && !e.shiftKey) {" +
                        "    e.preventDefault();" +
                        "    $0.$server.submitQuery();" +
                        "  }" +
                        "});", getElement());

        // Buttons row - fixed at bottom
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(false);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.getStyle()
                .set("gap", "4px")
                .set("flex", "0 0 auto")
                .set("margin-top", "4px");

        submitButton = new Button("Send", VaadinIcon.ARROW_CIRCLE_RIGHT.create());
        submitButton.setId(AI_SUBMIT_BUTTON);
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        submitButton.addClickListener(e -> handleQuery());

        Button clearButton = new Button("Clear", VaadinIcon.ERASER.create());
        clearButton.setId(AI_CLEAR_BUTTON);
        clearButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        clearButton.addClickListener(e -> clearConversation());

        buttonLayout.add(submitButton, clearButton);

        // Wrapper for queryInput used in SplitLayout
        Div queryInputWrapper = new Div(queryInput);
        queryInputWrapper.setSizeFull();
        queryInputWrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("min-height", "40px");

        // SplitLayout: response area (top) / query input (bottom)
        SplitLayout splitLayout = new SplitLayout(responseArea, queryInputWrapper);
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(80);

        Div splitLayoutWrapper = new Div(splitLayout);
        splitLayoutWrapper.setWidthFull();
        splitLayoutWrapper.getStyle()
                .set("flex", "1 1 0")
                .set("min-height", "0")
                .set("overflow", "hidden")
                .set("height", "calc(100% - 40px)");

        add(splitLayoutWrapper, buttonLayout);

        // History replay and welcome message are handled by restoreOrStart(),
        // which is called from each view's afterNavigation().
        // Show a placeholder until then.
        addWelcomeMessage();
    }

    private void addAiMessage(String message) {
        Div messageDiv = createMessageDiv(message, "ai", AI_LAST_RESPONSE);
        conversationHistory.add(messageDiv);
        scrollToBottom();
        snapshotMessage("ai", message);
        // Notify the host view so it can refresh its grid
        if (onAiReply != null) {
            onAiReply.run();
        }
    }

    private void addErrorMessage(String message) {
        Div messageDiv = createMessageDiv(message, "error");
        conversationHistory.add(messageDiv);
        scrollToBottom();
        snapshotMessage("error", message);
    }

    private void addSystemMessage(String message) {
        Div messageDiv = createMessageDiv(message, "system");
        conversationHistory.add(messageDiv);
        scrollToBottom();
        snapshotMessage("system", message);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void addToolMessage(String message) {
        Div messageDiv = createMessageDiv(message, "tool");
        conversationHistory.add(messageDiv);
        scrollToBottom();
        snapshotMessage("tool", message);
    }

    private void addUserMessage(String message) {
        Div messageDiv = createMessageDiv(message, "user");
        conversationHistory.add(messageDiv);
        scrollToBottom();
        snapshotMessage("user", message);
    }

    private void addWelcomeMessage() {
        String welcomeMessage = "👋 Hello! I'm Kassandra powered by " + aiAssistantService.getModelName() + ". Ask me anything about your system data.";
        addSystemMessage(welcomeMessage);
    }

    public void clearConversation() {
        activityStreaming = false;
        conversationHistory.removeAll();
        // Destroy the server-side ChatMemory for this conversation.
        aiAssistantService.clearConversation(conversationId);
        if (sessionState != null && currentRouteKey != null) {
            // Remove the old slot and create a fresh one so the next message starts clean.
            sessionState.removeSlot(currentRouteKey);
            ChatPanelSessionState.ConversationSlot fresh = sessionState.getOrCreateSlot(currentRouteKey);
            this.conversationId  = fresh.conversationId;
            this.sessionMessages = fresh.messages;
        } else {
            this.conversationId  = java.util.UUID.randomUUID().toString();
            this.sessionMessages = null;
        }
        addWelcomeMessage();
    }

    private Div createMessageDiv(String message, String type) {
        return createMessageDiv(message, type, null);
    }

    private Div createMessageDiv(String message, String type, String contentId) {
        Div messageDiv = new Div();
        messageDiv.addClassNames(
                LumoUtility.BorderRadius.SMALL
        );
        messageDiv.getStyle()
                .set("padding", "user".equals(type) ? "4px 6px" : "1px 4px")
                .set("margin-bottom", "2px");

        Span icon    = new Span();
        Span content = new Span(message);
        content.getStyle().set("white-space", "pre-wrap");
        if (contentId != null) {
            content.setId(contentId);
        }

        switch (type) {
            case "user":
                messageDiv.addClassNames(LumoUtility.Background.PRIMARY_10);
                if (currentUser != null && currentUser.getAvatarHash() != null && !currentUser.getAvatarHash().isEmpty()) {
                    com.vaadin.flow.component.html.Image avatarImage = new com.vaadin.flow.component.html.Image();
                    avatarImage.setWidth("16px");
                    avatarImage.setHeight("16px");
                    avatarImage.getStyle()
                            .set("border-radius", "4px")
                            .set("object-fit", "cover")
                            .set("margin-right", "4px");
                    avatarImage.setSrc(currentUser.getAvatarUrl());
                    messageDiv.add(avatarImage);
                }
                String userLabel = (currentUser != null && currentUser.getName() != null && !currentUser.getName().isEmpty()) ? currentUser.getName() : "You";
                icon.setText(userLabel + ": ");
                icon.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
                break;
            case "ai":
                messageDiv.addClassNames(LumoUtility.Background.SUCCESS_10);
                icon.setText("🤖 Kassandra: ");
                icon.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
                break;
            case "system":
                messageDiv.addClassNames(LumoUtility.Background.CONTRAST_5);
                icon.setText("ℹ️ ");
                break;
            case "tool":
                icon.setText("🪛 ");
                icon.getStyle().set("color", "yellow");
                break;
            case "error":
                messageDiv.addClassNames(LumoUtility.Background.ERROR_10);
                icon.setText("⚠️ ");
                break;
        }

        messageDiv.add(icon, content);
        return messageDiv;
    }

    private void handleQuery() {
        String query = queryInput.getValue();
        if (query == null || query.trim().isEmpty()) {
            Notification.show("Please enter a question", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        log.info("Processing AI query: {}", query);

        addUserMessage(query);
        queryInput.clear();

        getUI().ifPresent(ui -> ui.access(() -> submitButton.setEnabled(false)));

        // Build the effective query – prepend view context so the AI knows the current selection
        final String effectiveQuery = (viewContext != null && !viewContext.isEmpty())
                ? "[Context: " + viewContext + "]\n" + query
                : query;

        SecurityContext capturedSecurityContext = SecurityContextHolder.getContext();
        final String    username                = SecurityUtils.getUserEmail();

        getUI().ifPresent(ui -> {
            // Set up tool activity streaming listener before subscribing
            SessionToolActivityContext activityContext = aiAssistantService.getActivityContext(conversationId);
            activityStreaming = true;
            activityContext.setActivityListener(msg -> {
                if (!activityStreaming) return;
                ui.access(() -> {
                    addToolMessage(msg);
                    ui.push();
                });
            });

            // Create the streaming bubble on the UI thread before subscribing
            Span contentSpan = startAiStreamingBubble();
            ui.push();

            StringBuilder accumulator = new StringBuilder();

            Disposable subscription = aiAssistantService.streamQuery(username, effectiveQuery, conversationId, capturedSecurityContext)
                    .doOnNext(token -> {
                        accumulator.append(token);
                        ui.access(() -> {
                            contentSpan.setText(accumulator.toString());
                            scrollToBottom();
                            ui.push();
                        });
                    })
                    .doOnError(err -> {
                        log.error("Streaming error during AI query", err);
                        ui.access(() -> {
                            // Replace the streaming bubble's content with an error indicator
                            contentSpan.setText("⚠️ Error: " + err.getMessage());
                            activityStreaming = false;
                            activityContext.setActivityListener(null);
                            submitButton.setEnabled(true);
                            ui.push();
                        });
                    })
                    .doFinally(signal -> {
                        String finalText = aiAssistantService.removeThinkingFromResponse(accumulator.toString());
                        log.info("AI streaming complete: {} characters, signal={}", finalText != null ? finalText.length() : 0, signal);
                        ui.access(() -> {
                            // Apply cleaned text (removes any <think>…</think> remnants)
                            if (finalText != null && !finalText.isEmpty()) {
                                contentSpan.setText(finalText);
                            }
                            snapshotMessage("ai", finalText != null ? finalText : accumulator.toString());
                            if (onAiReply != null) {
                                onAiReply.run();
                            }
                            activityStreaming = false;
                            activityContext.setActivityListener(null);
                            submitButton.setEnabled(true);
                            ui.push();
                        });
                    })
                    .subscribe();
        });
    }

    private void removeLastMessage() {
        conversationHistory.getChildren()
                .reduce((_first, second) -> second)
                .ifPresent(conversationHistory::remove);
    }

    /**
     * Replays stored message records into the conversation history UI on reload.
     * All message types (user, ai, system, tool, error) are replayed.
     */
    private void replayHistory(List<ChatPanelSessionState.ChatMessageRecord> records) {
        for (ChatPanelSessionState.ChatMessageRecord record : records) {
            String role       = record.role();
            String contentId  = "ai".equals(role) ? AI_LAST_RESPONSE : null;
            Div    messageDiv = createMessageDiv(record.text(), role, contentId);
            conversationHistory.add(messageDiv);
        }
        scrollToBottom();
    }

    /**
     * Switches this panel to the conversation slot for the given route key.
     * <p>
     * If a slot already exists (returning to a page) its messages are replayed and
     * its conversationId is reused — the server-side ChatMemory is untouched.
     * If no slot exists yet a fresh one is created and the welcome message is shown.
     * <p>
     * Must be called on the Vaadin UI / request thread (from afterNavigation).
     */
    public void restoreOrStart(String routeKey) {
        this.currentRouteKey = routeKey;

        if (sessionState == null) {
            // No session state (e.g. standalone use) — just reset the UI.
            conversationHistory.removeAll();
            addWelcomeMessage();
            return;
        }

        ChatPanelSessionState.ConversationSlot slot = sessionState.getOrCreateSlot(routeKey);
        this.conversationId  = slot.conversationId;
        this.sessionMessages = slot.messages;

        conversationHistory.removeAll();
        activityStreaming = false;

        if (!slot.messages.isEmpty()) {
            replayHistory(slot.messages);
        } else {
            addWelcomeMessage();
        }
    }

    private void scrollToBottom() {
        conversationHistory.getElement().executeJs("this.scrollTop = this.scrollHeight;");
    }

    /**
     * Sets the current user for avatar and name display in messages.
     * Call this from the parent view's afterNavigation.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Registers a callback that is invoked on the UI thread immediately after
     * every AI reply is rendered. Use this to refresh the host view's data grid
     * so changes made by the AI are reflected without a manual page reload.
     */
    public void setOnAiReply(Runnable onAiReply) {
        this.onAiReply = onAiReply;
    }

    /**
     * Sets the navigation context sentence that is silently prepended to every user query
     * so the AI always knows which entities are currently selected in the UI.
     * Call this from the parent view's afterNavigation.
     */
    public void setViewContext(String context) {
        this.viewContext = context;
    }

    /**
     * Appends any message type to the plain session list for UI replay on F5.
     * The eviction cap mirrors the agent's ChatMemory window: only "user" and "ai"
     * messages count toward MAX_MESSAGES, because tool/system/error messages are
     * never stored in the agent's history and must not shift the window.
     * Safe to call from any thread — operates on the captured List reference,
     * never on the session-scoped proxy.
     */
    private void snapshotMessage(String role, String text) {
        if (sessionMessages == null) return;
        synchronized (sessionMessages) {
            if ("user".equals(role) || "ai".equals(role)) {
                // Count only agent-history messages toward the cap, then evict oldest
                // user/ai pair from the front until we are within budget.
                long agentMessageCount = sessionMessages.stream()
                        .filter(r -> "user".equals(r.role()) || "ai".equals(r.role()))
                        .count();
                while (agentMessageCount >= AiAssistantService.MAX_MESSAGES) {
                    // Remove the first user or ai message (and stop after one removal per call)
                    for (int i = 0; i < sessionMessages.size(); i++) {
                        if ("user".equals(sessionMessages.get(i).role()) || "ai".equals(sessionMessages.get(i).role())) {
                            sessionMessages.remove(i);
                            agentMessageCount--;
                            break;
                        }
                    }
                }
            }
            sessionMessages.add(new ChatPanelSessionState.ChatMessageRecord(role, text));
        }
    }

    /**
     * Creates an empty AI message bubble and returns the inner content Span so tokens
     * can be appended to it in-place as they arrive from the streaming response.
     */
    private Span startAiStreamingBubble() {
        Div messageDiv = new Div();
        messageDiv.addClassNames(LumoUtility.BorderRadius.SMALL, LumoUtility.Background.SUCCESS_10);
        messageDiv.getStyle()
                .set("padding", "1px 4px")
                .set("margin-bottom", "2px");
        Span icon = new Span("🤖 Kassandra: ");
        icon.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        Span content = new Span();
        content.setId(AI_LAST_RESPONSE);
        content.getStyle().set("white-space", "pre-wrap");
        messageDiv.add(icon, content);
        conversationHistory.add(messageDiv);
        scrollToBottom();
        return content;
    }

    /**
     * Called from JavaScript when Enter key is pressed in queryInput.
     */
    @ClientCallable
    public void submitQuery() {
        handleQuery();
    }
}

