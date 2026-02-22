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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.AuthenticationProvider;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.ChatAgentPanel;
import de.bushnaq.abdalla.kassandra.ui.component.ChatPanelSessionState;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 * AI Assistant View - Admin interface for interacting with the AI assistant
 */
@Route(value = "kassandra", layout = MainLayout.class)
@PageTitle("Kassandra")
@Menu(order = 10, icon = "vaadin:brain", title = "Kassandra")
@RolesAllowed("ADMIN")
@Slf4j
public class Kassandra extends VerticalLayout implements AfterNavigationObserver {

    // Re-exported for backwards compatibility with tests that reference Kassandra.AI_*
    public static final  String                AI_CLEAR_BUTTON  = ChatAgentPanel.AI_CLEAR_BUTTON;
    public static final  String                AI_LAST_RESPONSE = ChatAgentPanel.AI_LAST_RESPONSE;
    public static final  String                AI_QUERY_INPUT   = ChatAgentPanel.AI_QUERY_INPUT;
    public static final  String                AI_RESPONSE_AREA = ChatAgentPanel.AI_RESPONSE_AREA;
    public static final  String                AI_SUBMIT_BUTTON = ChatAgentPanel.AI_SUBMIT_BUTTON;
    public static final  String                AI_VIEW_TITLE    = "ai-view-title";
    private static final String                ROUTE_KEY        = "kassandra";
    private final        ChatAgentPanel        chatAgentPanel;
    private final        ChatPanelSessionState sessionState;
    private final        UserApi               userApi;

    public Kassandra(AiAssistantService aiAssistantService, AuthenticationProvider mcpAuthProvider, UserApi userApi,
                     ChatPanelSessionState chatPanelSessionState) {
        this.userApi      = userApi;
        this.sessionState = chatPanelSessionState;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames(LumoUtility.Background.BASE);

        // Main horizontal layout: left (future audit log) | right (chat)
        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.setSizeFull();
        mainContent.setSpacing(false);
        mainContent.setPadding(false);

        // Left panel - placeholder for future audit log
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

        // Right panel - Chat interface via reusable component
        Div rightPanel = new Div();
        rightPanel.setWidth("50%");
        rightPanel.getStyle()
                .set("height", "100%")
                .set("padding", "4px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("overflow", "hidden");

        chatAgentPanel = new ChatAgentPanel(aiAssistantService, mcpAuthProvider, userApi, chatPanelSessionState);
        rightPanel.add(chatAgentPanel);

        mainContent.add(leftPanel, rightPanel);
        add(mainContent);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        chatAgentPanel.restoreOrStart(ROUTE_KEY);

        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        mainLayout.getBreadcrumbs().addItem("Kassandra", Kassandra.class);
                    }
                });

        final String userEmail  = SecurityUtils.getUserEmail();
        User         userFromDb = null;
        if (!userEmail.equals(SecurityUtils.GUEST)) {
            try {
                userFromDb = userApi.getByEmail(userEmail).get();
            } catch (Exception e) {
                // User not found or error - default avatar will be used
            }
        }
        chatAgentPanel.setCurrentUser(userFromDb);
    }
}

