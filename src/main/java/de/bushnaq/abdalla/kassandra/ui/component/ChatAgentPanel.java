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
import de.bushnaq.abdalla.kassandra.ai.mcp.QueryResult;
import de.bushnaq.abdalla.kassandra.ai.mcp.SessionToolActivityContext;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.AuthenticationProvider;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import lombok.extern.slf4j.Slf4j;

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
    private             User                                          currentUser;
    private final       AuthenticationProvider                        mcpAuthProvider;
    /**
     * Called on the UI thread after every successful AI reply — use to refresh the host view's grid.
     */
    private             Runnable                                      onAiReply;
    private final       TextArea                                      queryInput;
    private final       Div                                           responseArea;
    // Plain list reference captured from the session bean — safe to use on any thread.
    private final       List<ChatPanelSessionState.ChatMessageRecord> sessionMessages;
    // Direct reference to the session state bean — resolved once on the request thread.
    // Never accessed from background threads; only the plain List inside it is passed into lambdas.
    private final       ChatPanelSessionState                         sessionState;
    private final       Button                                        submitButton;
    private final       UserApi                                       userApi;
    /**
     * Current navigation context sentence prepended to every query so the AI knows which entities are selected.
     */
    private             String                                        viewContext       = null;

    public ChatAgentPanel(AiAssistantService aiAssistantService, AuthenticationProvider mcpAuthProvider, UserApi userApi) {
        this(aiAssistantService, mcpAuthProvider, userApi, null);
    }

    public ChatAgentPanel(AiAssistantService aiAssistantService, AuthenticationProvider mcpAuthProvider, UserApi userApi, ChatPanelSessionState sessionState) {
        this.aiAssistantService = aiAssistantService;
        this.mcpAuthProvider    = mcpAuthProvider;
        this.userApi            = userApi;
        this.sessionState       = sessionState;
        // Capture the plain List reference NOW while we are on the HTTP request thread.
        // Background threads will append to this list directly — never via the scoped proxy.
        this.sessionMessages = (sessionState != null) ? sessionState.getMessages() : null;

        // Reuse the stored conversationId so server-side ChatMemory is not abandoned on F5
        this.conversationId = (sessionState != null) ? sessionState.getConversationId() : java.util.UUID.randomUUID().toString();

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
                .set("flex", "1 1 auto");

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
                .set("font-size", "var(--lumo-font-size-s)")
                .set("--vaadin-input-field-font-size", "var(--lumo-font-size-s)");

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

        // On reload: replay stored messages; on fresh start: show welcome message
        if (sessionState != null && !sessionState.getMessages().isEmpty()) {
            replayHistory(sessionState.getMessages());
        } else {
            addWelcomeMessage();
        }
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
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void addSystemMessage(String message) {
        Div messageDiv = createMessageDiv(message, "system");
        conversationHistory.add(messageDiv);
        scrollToBottom();
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

    private void clearConversation() {
        conversationHistory.removeAll();
        aiAssistantService.clearConversation(conversationId);
        conversationId = java.util.UUID.randomUUID().toString();
        if (sessionState != null) {
            // Still on the request/UI thread here — safe to call the proxy
            sessionState.reset(conversationId);
        } else if (sessionMessages != null) {
            sessionMessages.clear();
        }
    }

    private Div createMessageDiv(String message, String type) {
        return createMessageDiv(message, type, null);
    }

    private Div createMessageDiv(String message, String type, String contentId) {
        Div messageDiv = new Div();
        messageDiv.addClassNames(
                LumoUtility.BorderRadius.SMALL,
                LumoUtility.FontSize.XSMALL
        );
        messageDiv.getStyle()
                .set("padding", "4px 6px")
                .set("margin-bottom", "2px");

        Span icon    = new Span();
        Span content = new Span(message);
        content.getStyle().set("white-space", "pre-wrap");
        content.getStyle().set("font-family", "monospace");
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

        addSystemMessage("🤔 Thinking...");

        // Build the effective query – prepend view context so the AI knows the current selection
        final String effectiveQuery = (viewContext != null && !viewContext.isEmpty())
                ? "[Context: " + viewContext + "]\n" + query
                : query;

        String capturedToken = mcpAuthProvider.captureCurrentUserToken();
        getUI().ifPresent(ui -> {
            new Thread(() -> {
                final SessionToolActivityContext[] activityContextRef = new SessionToolActivityContext[1];
                try {
                    if (capturedToken != null) {
                        mcpAuthProvider.setToken(capturedToken);
                    }
                    activityContextRef[0] = aiAssistantService.getActivityContext(conversationId);
                    activityStreaming     = true;
                    if (activityContextRef[0] != null) {
                        activityContextRef[0].setActivityListener(msg -> {
                            if (!activityStreaming) return;
                            ui.access(() -> {
                                addSystemMessage("[AI activity] " + msg);
                                ui.push();
                            });
                        });
                    }
                    QueryResult response = aiAssistantService.processQueryWithThinking(effectiveQuery, conversationId);
                    log.info("AI response received: {} characters", response != null ? response.content().length() : 0);

                    ui.access(() -> {
                        try {
                            log.info("Updating UI with AI response");
                            removeLastMessage();
                            addAiMessage(response.content());
                        } catch (Exception e) {
                            log.error("Error updating UI with response", e);
                            addErrorMessage("Error displaying response: " + e.getMessage());
                        } finally {
                            activityStreaming = false;
                            if (activityContextRef[0] != null) activityContextRef[0].setActivityListener(null);
                            submitButton.setEnabled(true);
                            ui.push();
                        }
                    });
                } catch (Exception e) {
                    log.error("Error processing AI query", e);
                    ui.access(() -> {
                        try {
                            removeLastMessage();
                            addErrorMessage("Error: " + e.getMessage());
                        } catch (Exception ex) {
                            log.error("Error updating UI with error message", ex);
                        } finally {
                            activityStreaming = false;
                            if (activityContextRef[0] != null) activityContextRef[0].setActivityListener(null);
                            submitButton.setEnabled(true);
                            ui.push();
                        }
                    });
                }
            }).start();
        });
    }

    private void removeLastMessage() {
        conversationHistory.getChildren()
                .reduce((_first, second) -> second)
                .ifPresent(conversationHistory::remove);
    }

    /**
     * Replays stored message records into the conversation history UI on reload.
     * Only user and AI messages are replayed (system/error are transient).
     */
    private void replayHistory(List<ChatPanelSessionState.ChatMessageRecord> records) {
        for (ChatPanelSessionState.ChatMessageRecord record : records) {
            Div messageDiv = createMessageDiv(record.text(), record.role());
            conversationHistory.add(messageDiv);
        }
        scrollToBottom();
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
     * Appends a user or AI message to the plain session list.
     * Safe to call from any thread — operates on the captured List reference,
     * never on the session-scoped proxy.
     */
    private void snapshotMessage(String role, String text) {
        if (sessionMessages == null) return;
        if (!"user".equals(role) && !"ai".equals(role)) return;
        synchronized (sessionMessages) {
            if (sessionMessages.size() >= ChatPanelSessionState.MAX_MESSAGES) {
                sessionMessages.remove(0);
            }
            sessionMessages.add(new ChatPanelSessionState.ChatMessageRecord(role, text));
        }
    }

    /**
     * Called from JavaScript when Enter key is pressed in queryInput.
     */
    @ClientCallable
    public void submitQuery() {
        handleQuery();
    }
}

