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

package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.ai.AiAssistantService;
import de.bushnaq.abdalla.kassandra.mcp.api.McpAuthenticationProvider;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 * AI Assistant View - Admin interface for interacting with the AI assistant
 */
@Route(value = "ai", layout = MainLayout.class)
@PageTitle("AI Assistant")
@Menu(order = 10, icon = "vaadin:brain", title = "AI Assistant")
@RolesAllowed("ADMIN")
@Slf4j
public class AiView extends VerticalLayout implements AfterNavigationObserver {

    public static final String                    AI_CLEAR_BUTTON  = "ai-clear-button";
    public static final String                    AI_QUERY_INPUT   = "ai-query-input";
    public static final String                    AI_RESPONSE_AREA = "ai-response-area";
    public static final String                    AI_SUBMIT_BUTTON = "ai-submit-button";
    public static final String                    AI_TOOLS_LIST    = "ai-tools-list";
    public static final String                    AI_VIEW_TITLE    = "ai-view-title";
    private final       AiAssistantService        aiAssistantService;
    private final       Div                       conversationHistory;
    private final       McpAuthenticationProvider mcpAuthProvider;
    private final       TextArea                  queryInput;
    private final       Div                       responseArea;

    public AiView(AiAssistantService aiAssistantService, McpAuthenticationProvider mcpAuthProvider) {
        this.aiAssistantService = aiAssistantService;
        this.mcpAuthProvider    = mcpAuthProvider;

        // Configure layout
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassNames(LumoUtility.Background.BASE);

        // Header
        H2 title = new H2("AI Assistant");
        title.setId(AI_VIEW_TITLE);
        title.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        Paragraph subtitle = new Paragraph(
                "Ask the AI assistant to help you manage users and other system tasks. " +
                        "The AI has access to various API endpoints and can retrieve information for you."
        );
        subtitle.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.MEDIUM);

        // Available Tools Section
        Div toolsSection = createToolsSection();

        // Query Input
        queryInput = new TextArea("Your Question");
        queryInput.setId(AI_QUERY_INPUT);
        queryInput.setPlaceholder("Ask me anything about users, projects, or system data...");
        queryInput.setWidthFull();
        queryInput.setHeight("120px");
        queryInput.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        // Example queries
        Div examplesSection = createExamplesSection();

        // Buttons
        Button submitButton = new Button("Ask AI", VaadinIcon.ARROW_CIRCLE_RIGHT.create());
        submitButton.setId(AI_SUBMIT_BUTTON);
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClickListener(e -> handleQuery());

        Button clearButton = new Button("Clear", VaadinIcon.ERASER.create());
        clearButton.setId(AI_CLEAR_BUTTON);
        clearButton.addClickListener(e -> clearConversation());

        HorizontalLayout buttonLayout = new HorizontalLayout(submitButton, clearButton);
        buttonLayout.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        // Response Area
        conversationHistory = new Div();
        conversationHistory.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM
        );
        conversationHistory.setWidthFull();

        responseArea = new Div();
        responseArea.setId(AI_RESPONSE_AREA);
        responseArea.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.LARGE
        );
        responseArea.setWidthFull();
        responseArea.setMinHeight("300px");
        responseArea.add(conversationHistory);

        // Add initial welcome message
        addSystemMessage("👋 Hello! I'm your AI assistant. Ask me anything about your system data.");

        // Add all components to layout
        add(
                title,
                subtitle,
                toolsSection,
                examplesSection,
                queryInput,
                buttonLayout,
                responseArea
        );
    }

    private void addAiMessage(String message) {
        Div messageDiv = createMessageDiv(message, "ai");
        conversationHistory.add(messageDiv);
        scrollToBottom();
    }

    private void addErrorMessage(String message) {
        Div messageDiv = createMessageDiv(message, "error");
        conversationHistory.add(messageDiv);
        scrollToBottom();
    }

    private void addSystemMessage(String message) {
        Div messageDiv = createMessageDiv(message, "system");
        conversationHistory.add(messageDiv);
        scrollToBottom();
    }

    private void addUserMessage(String message) {
        Div messageDiv = createMessageDiv(message, "user");
        conversationHistory.add(messageDiv);
        scrollToBottom();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        mainLayout.getBreadcrumbs().addItem("AI Assistant", AiView.class);
                    }
                });
    }

    private void clearConversation() {
        conversationHistory.removeAll();
        addSystemMessage("👋 Conversation cleared. How can I help you?");
    }

    private Button createExampleButton(String text) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        button.addClickListener(e -> {
            queryInput.setValue(text);
            queryInput.focus();
        });
        return button;
    }

    private Div createExamplesSection() {
        Div section = new Div();
        section.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        Span examplesLabel = new Span("Example queries: ");
        examplesLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Button example1 = createExampleButton("List all users");
        Button example2 = createExampleButton("Find user with email admin@kassandra.org");
        Button example3 = createExampleButton("Search for users named 'john'");

        HorizontalLayout examples = new HorizontalLayout(examplesLabel, example1, example2, example3);
        examples.setAlignItems(Alignment.CENTER);
        examples.setSpacing(true);
        examples.addClassNames(LumoUtility.FlexWrap.WRAP);

        section.add(examples);
        return section;
    }

    private Div createMessageDiv(String message, String type) {
        Div messageDiv = new Div();
        messageDiv.addClassNames(
                LumoUtility.Padding.SMALL,
                LumoUtility.BorderRadius.SMALL,
                LumoUtility.Margin.Bottom.SMALL
        );

        Span icon    = new Span();
        Span content = new Span(message);
        content.getStyle().set("white-space", "pre-wrap");

        switch (type) {
            case "user":
                messageDiv.addClassNames(LumoUtility.Background.PRIMARY_10);
                icon.setText("👤 You: ");
                icon.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
                break;
            case "ai":
                messageDiv.addClassNames(LumoUtility.Background.SUCCESS_10);
                icon.setText("🤖 AI: ");
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

    private Div createToolsSection() {
        Div section = new Div();
        section.setId(AI_TOOLS_LIST);
        section.addClassNames(
                LumoUtility.Background.PRIMARY_10,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Bottom.MEDIUM
        );

        H3 toolsTitle = new H3("Available API Tools");
        toolsTitle.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);

        String    toolsList = aiAssistantService.getAvailableTools();
        Paragraph toolsText = new Paragraph(toolsList);
        toolsText.addClassNames(LumoUtility.FontSize.SMALL);
        toolsText.getStyle().set("white-space", "pre-line");

        section.add(toolsTitle, toolsText);
        return section;
    }

    private void handleQuery() {
        String query = queryInput.getValue();
        if (query == null || query.trim().isEmpty()) {
            Notification.show("Please enter a question", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        log.info("Processing AI query: {}", query);

        // Add user message to conversation
        addUserMessage(query);

        // Clear input
        queryInput.clear();

        // Show loading state
        addSystemMessage("🤔 Thinking...");

        // Capture the user's token NOW, while we're still in the request thread with SecurityContext
        String capturedToken = mcpAuthProvider.captureCurrentUserToken();

        // Process query asynchronously
        getUI().ifPresent(ui -> {
            new Thread(() -> {
                // Set the captured token in this thread's ThreadLocal
                if (capturedToken != null) {
                    mcpAuthProvider.setToken(capturedToken);
                }
                try {
                    log.info("Calling AI assistant service...");
                    String response = aiAssistantService.processQuery(query);
                    log.info("AI response received: {} characters", response != null ? response.length() : 0);

                    ui.access(() -> {
                        try {
                            log.info("Updating UI with AI response");
                            // Remove loading message
                            removeLastMessage();
                            // Add AI response
                            addAiMessage(response);
                            // Push the update to the browser
                            ui.push();
                            log.info("UI updated successfully");
                        } catch (Exception e) {
                            log.error("Error updating UI with response", e);
                            addErrorMessage("Error displaying response: " + e.getMessage());
                            ui.push();
                        }
                    });
                } catch (Exception e) {
                    log.error("Error processing AI query", e);
                    ui.access(() -> {
                        try {
                            removeLastMessage();
                            addErrorMessage("Error: " + e.getMessage());
                            ui.push();
                        } catch (Exception ex) {
                            log.error("Error updating UI with error message", ex);
                        }
                    });
                } finally {
                    // Always clear the captured token when done
                    mcpAuthProvider.clearCapturedToken();
                }
            }).start();
        });
    }

    private void removeLastMessage() {
        conversationHistory.getChildren()
                .reduce((_first, second) -> second)
                .ifPresent(conversationHistory::remove);
    }

    private void scrollToBottom() {
        responseArea.getElement().executeJs("this.scrollTop = this.scrollHeight;");
    }
}
