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

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
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

        // Configure main layout - full size horizontal split
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames(LumoUtility.Background.BASE);

        // Main horizontal layout for left (future audit log) and right (chat) panels
        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.setSizeFull();
        mainContent.setSpacing(false);
        mainContent.setPadding(false);

        // Left panel placeholder for future audit log
        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setWidth("50%");
        leftPanel.setHeightFull();
        leftPanel.setPadding(false);
        leftPanel.setSpacing(false);
        leftPanel.getStyle().set("padding", "4px");
        leftPanel.addClassNames(LumoUtility.Background.CONTRAST_5);

        H3 placeholderTitle = new H3("Audit Log");
        placeholderTitle.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        placeholderTitle.getStyle().set("margin", "0");
        Paragraph placeholder = new Paragraph("Audit log will be displayed here.");
        placeholder.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.XSMALL);
        placeholder.getStyle().set("margin", "0");
        leftPanel.add(placeholderTitle, placeholder);

        // Right panel - Chat interface
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setWidth("50%");
        rightPanel.setHeightFull();
        rightPanel.setPadding(false);
        rightPanel.setSpacing(false);
        rightPanel.getStyle().set("padding", "4px");

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
                .set("padding", "4px");

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

        // Make conversation history fill the response area
        conversationHistory.getStyle().set("flex", "1 1 auto");

        // Query Input - no label, smaller font
        queryInput = new TextArea();
        queryInput.setId(AI_QUERY_INPUT);
        queryInput.setPlaceholder("Ask me anything... (Enter to send, Ctrl+Enter for new line)");
        queryInput.setWidthFull();
        queryInput.setSizeFull();
        queryInput.setMinHeight("40px");
        queryInput.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("--vaadin-input-field-font-size", "var(--lumo-font-size-s)");

        // Handle Enter key to submit, Ctrl+Enter or Shift+Enter for new line
        // We need to force the value sync before submitting since Vaadin only syncs on blur
        queryInput.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        queryInput.getElement().executeJs(
                "this.inputElement.addEventListener('keydown', (e) => {" +
                        "  if (e.key === 'Enter' && !e.ctrlKey && !e.shiftKey) {" +
                        "    e.preventDefault();" +
                        "    $0.$server.submitQuery();" +
                        "  }" +
                        "});", getElement());

        // Buttons below queryInput - fixed size, always visible at bottom
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(false);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.getStyle()
                .set("gap", "4px")
                .set("flex", "0 0 auto")
                .set("margin-top", "4px");

        Button submitButton = new Button("Send", VaadinIcon.ARROW_CIRCLE_RIGHT.create());
        submitButton.setId(AI_SUBMIT_BUTTON);
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        submitButton.addClickListener(e -> handleQuery());

        Button clearButton = new Button("Clear", VaadinIcon.ERASER.create());
        clearButton.setId(AI_CLEAR_BUTTON);
        clearButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        clearButton.addClickListener(e -> clearConversation());

        buttonLayout.add(submitButton, clearButton);

        // Wrapper for queryInput to be used in SplitLayout
        Div queryInputWrapper = new Div(queryInput);
        queryInputWrapper.setSizeFull();
        queryInputWrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("min-height", "40px");

        // Use SplitLayout for draggable divider between response area and queryInput only
        SplitLayout splitLayout = new SplitLayout(responseArea, queryInputWrapper);
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(80); // 80% for response area, 20% for input

        // Wrapper to contain the SplitLayout and prevent it from overflowing
        Div splitLayoutWrapper = new Div(splitLayout);
        splitLayoutWrapper.setWidthFull();
        splitLayoutWrapper.getStyle()
                .set("flex", "1 1 0")
                .set("min-height", "0")
                .set("overflow", "hidden")
                .set("height", "calc(100% - 40px)"); // Reserve space for buttons

        // Configure right panel to use flexbox for proper layout
        // SplitLayout wrapper takes available space, buttons stay fixed at bottom
        rightPanel.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("overflow", "hidden");
        rightPanel.add(splitLayoutWrapper, buttonLayout);

        // Add panels to main content
        mainContent.add(leftPanel, rightPanel);

        // Add main content to this view
        add(mainContent);

        // Add initial welcome message with available tools
        addWelcomeMessageWithTools();
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

    private void addWelcomeMessageWithTools() {
        String toolsList = aiAssistantService.getAvailableTools();
        String welcomeMessage = "👋 Hello! I'm your AI assistant. Ask me anything about your system data.\n\n" +
                "📋 Available API Tools:\n" + toolsList;
        addSystemMessage(welcomeMessage);
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
        addWelcomeMessageWithTools();
    }

    private Div createMessageDiv(String message, String type) {
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

    /**
     * Called from JavaScript when Enter key is pressed in queryInput.
     * Must be public and annotated with @ClientCallable to be accessible from client-side.
     */
    @ClientCallable
    public void submitQuery() {
        handleQuery();
    }
}
