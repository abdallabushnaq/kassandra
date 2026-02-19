/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService;
import de.bushnaq.abdalla.kassandra.ai.mcp.api.AuthenticationProvider;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.component.ChatAgentPanel;
import de.bushnaq.abdalla.kassandra.ui.component.ChatPanelSessionState;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.UserDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import de.bushnaq.abdalla.util.ColorUtil;
import jakarta.annotation.security.RolesAllowed;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

@Route(value = "user-list", layout = MainLayout.class)
@PageTitle("User List Page")
@RolesAllowed("ADMIN") // Only admins can access this view
public class UserListView extends AbstractMainGrid<User> implements AfterNavigationObserver {
    public static final  String                 CREATE_USER_BUTTON             = "create-user-button";
    private static final String                 PARAM_AI_PANEL                 = "aiPanel";
    public static final  String                 ROUTE                          = "user-list";
    public static final  String                 USER_AI_PANEL_BUTTON           = "user-ai-panel-button";
    public static final  String                 USER_GLOBAL_FILTER             = "user-global-filter";
    public static final  String                 USER_GRID                      = "user-grid";
    public static final  String                 USER_GRID_DELETE_BUTTON_PREFIX = "user-grid-delete-button-prefix-";
    public static final  String                 USER_GRID_EDIT_BUTTON_PREFIX   = "user-grid-edit-button-prefix-";
    public static final  String                 USER_GRID_NAME_PREFIX          = "user-grid-name-";
    public static final  String                 USER_LIST_PAGE_TITLE           = "user-list-page-title";
    public static final  String                 USER_ROW_COUNTER               = "user-row-counter";
    private final        Button                 aiToggleButton;
    private final        SplitLayout            bodySplit;
    private final        ChatAgentPanel         chatAgentPanel;
    private final        Div                    chatPane;
    private final        Clock                  clock;
    private final        ChatPanelSessionState  sessionState;
    private final        StableDiffusionService stableDiffusionService;
    private final        UserApi                userApi;

    public UserListView(UserApi userApi, Clock clock, AiFilterService aiFilterService, JsonMapper mapper,
                        StableDiffusionService stableDiffusionService,
                        AiAssistantService aiAssistantService, AuthenticationProvider mcpAuthProvider,
                        ChatPanelSessionState chatPanelSessionState) {
        super(clock);
        this.userApi                = userApi;
        this.clock                  = clock;
        this.stableDiffusionService = stableDiffusionService;
        this.sessionState           = chatPanelSessionState;

        add(createSmartHeader("Users", USER_LIST_PAGE_TITLE, VaadinIcon.USERS,
                CREATE_USER_BUTTON, () -> openUserDialog(null),
                USER_ROW_COUNTER, USER_GLOBAL_FILTER, aiFilterService, mapper, "User"));

        aiToggleButton = new Button("AI");
        aiToggleButton.setId(USER_AI_PANEL_BUTTON);
        aiToggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        aiToggleButton.getElement().setAttribute("title", "AI Assistant");
        addHeaderButton(aiToggleButton);

        chatAgentPanel = new ChatAgentPanel(aiAssistantService, mcpAuthProvider, userApi, chatPanelSessionState);
        chatAgentPanel.setSizeFull();

        chatPane = new Div(chatAgentPanel);
        chatPane.getStyle()
                .set("height", "100%").set("overflow", "hidden")
                .set("transition", "width 0.3s ease, opacity 0.3s ease")
                .set("width", "0").set("opacity", "0").set("min-width", "0");

        bodySplit = new SplitLayout(getGridPanelWrapper(), chatPane);
        bodySplit.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        bodySplit.setSizeFull();
        bodySplit.setSplitterPosition(100);
        add(bodySplit);

        aiToggleButton.addClickListener(e -> {
            sessionState.setPanelOpen(!sessionState.isPanelOpen());
            applyChatPaneState(sessionState.isPanelOpen());
        });
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        mainLayout.getBreadcrumbs().addItem("Users", UserListView.class);
                    }
                });

        applyChatPaneState(sessionState.isPanelOpen());

        final String userEmail  = SecurityUtils.getUserEmail();
        User         userFromDb = null;
        if (!userEmail.equals(SecurityUtils.GUEST)) {
            try {
                userFromDb = userApi.getByEmail(userEmail).get();
            } catch (Exception ignored) {
            }
        }
        chatAgentPanel.setCurrentUser(userFromDb);
        chatAgentPanel.setOnAiReply(this::refreshGrid);

        refreshGrid();
    }

    private void applyChatPaneState(boolean open) {
        if (open) {
            chatPane.getStyle().set("width", "35%").set("opacity", "1").set("min-width", "280px");
            bodySplit.setSplitterPosition(65);
            aiToggleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            chatPane.getStyle().set("width", "0").set("opacity", "0").set("min-width", "0");
            bodySplit.setSplitterPosition(100);
            aiToggleButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
    }

    private void confirmDelete(User user) {
        String message = "Are you sure you want to delete user \"" + user.getName() + "\"?";
        ConfirmDialog dialog = new ConfirmDialog(
                "Confirm Delete",
                message,
                "Delete",
                () -> {
                    userApi.deleteById(user.getId());
                    refreshGrid();
                    Notification.show("User deleted", 3000, Notification.Position.BOTTOM_START);
                }
        );
        dialog.open();
    }

    protected void initGrid(Clock clock) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(clock.getZone()).withLocale(getLocale());

        getGrid().setId(USER_GRID);

        {
            // Add avatar image column
            Grid.Column<User> avatarColumn = getGrid().addColumn(new ComponentRenderer<>(user -> {
//                if (user.getAvatarPrompt() != null && !user.getAvatarPrompt().isEmpty())
                {
                    // User has a custom image - use URL-based loading
                    com.vaadin.flow.component.html.Image avatar = new com.vaadin.flow.component.html.Image();
                    avatar.setWidth("24px");
                    avatar.setHeight("24px");
                    avatar.getStyle()
                            .set("border-radius", "4px") // Slightly rounded corners for avatars
                            .set("object-fit", "cover")
                            .set("display", "block")
                            .set("margin", "0")
                            .set("padding", "0");

                    // Use hash-based URL for proper caching
                    avatar.setSrc(user.getAvatarUrl());
                    avatar.setAlt(user.getName());
                    return avatar;
                }
//                else {
//                    // No custom image - show default VaadinIcon
//                    Icon defaultIcon = new Icon(VaadinIcon.USER);
//                    defaultIcon.setSize("20px");
//                    defaultIcon.getStyle()
//                            .set("color", "var(--lumo-contrast-50pct)")
//                            .set("padding", "0")
//                            .set("margin", "0")
//                            .set("display", "block");
//                    return defaultIcon;
//                }
            }));
            avatarColumn.setWidth("48px");
            avatarColumn.setFlexGrow(0);
            avatarColumn.setHeader("");
        }

        {
            Grid.Column<User> keyColumn = getGrid().addColumn(User::getKey);
            VaadinUtil.addFilterableHeader(getGrid(), keyColumn, "Key", VaadinIcon.KEY, User::getKey);
        }

        {
            // Add name column with filtering and sorting
            Grid.Column<User> nameColumn = getGrid().addColumn(new ComponentRenderer<>(user -> {
                Div div = new Div();
                // Always show color indicator using user's color or default light gray
                Div square = new Div();
                square.setMinHeight("16px");
                square.setMaxHeight("16px");
                square.setMinWidth("16px");
                square.setMaxWidth("16px");
                square.getStyle().set("float", "left");
                square.getStyle().set("margin", "1px");
                square.getStyle().set("background-color", ColorUtil.colorToHexString(user.getColor()));
                div.add(square);
                div.add(user.getName());
                div.setId(USER_GRID_NAME_PREFIX + user.getName());
                return div;
            }));

            // Configure a custom comparator to properly sort by the name property
            nameColumn.setComparator((user1, user2) -> user1.getName().compareToIgnoreCase(user2.getName()));

            VaadinUtil.addSimpleHeader(nameColumn, "Name", VaadinIcon.USER);
        }

        {
            Grid.Column<User> emailColumn = getGrid().addColumn(User::getEmail);
            VaadinUtil.addSimpleHeader(emailColumn, "Email", VaadinIcon.ENVELOPE);
        }

        {
            // Add roles column
            Grid.Column<User> rolesColumn = getGrid().addColumn(user -> {
                List<String> roles = user.getRoleList();
                return roles.isEmpty() ? "USER" : String.join(", ", roles);
            });
            VaadinUtil.addSimpleHeader(rolesColumn, "Roles", VaadinIcon.SHIELD);
        }

        {
            Grid.Column<User> firstWorkingDayColumn = getGrid().addColumn(user -> user.getFirstWorkingDay() != null ? user.getFirstWorkingDay().toString() : "");
            VaadinUtil.addSimpleHeader(firstWorkingDayColumn, "First Working Day", VaadinIcon.CALENDAR_USER);
        }

        {
            Grid.Column<User> lastWorkingDayColumn = getGrid().addColumn(user -> user.getLastWorkingDay() != null ? user.getLastWorkingDay().toString() : "");
            VaadinUtil.addSimpleHeader(lastWorkingDayColumn, "Last Working Day", VaadinIcon.CALENDAR_USER);
        }

        {
            Grid.Column<User> createdColumn = getGrid().addColumn(user -> dateTimeFormatter.format(user.getCreated()));
            VaadinUtil.addSimpleHeader(createdColumn, "Created", VaadinIcon.CALENDAR);
        }

        {
            Grid.Column<User> updatedColumn = getGrid().addColumn(user -> dateTimeFormatter.format(user.getUpdated()));
            VaadinUtil.addSimpleHeader(updatedColumn, "Updated", VaadinIcon.CALENDAR);
        }

        // Add actions column using VaadinUtil
        VaadinUtil.addActionColumn(
                getGrid(),
                USER_GRID_EDIT_BUTTON_PREFIX,
                USER_GRID_DELETE_BUTTON_PREFIX,
                User::getName,
                this::openUserDialog,
                this::confirmDelete
        );

    }

    private void openUserDialog(User user) {
        UserDialog dialog = new UserDialog(user, stableDiffusionService, userApi);
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                // Dialog was closed, refresh the grid
                refreshGrid();
            }
        });
        dialog.open();
    }

    private void refreshGrid() {
        // Clear existing items
        getDataProvider().getItems().clear();

        // Fetch fresh data from API (with updated hashes)
        getDataProvider().getItems().addAll(userApi.getAll());

        // Force complete refresh of the grid
        getDataProvider().refreshAll();

        // Force the grid to re-render
        getGrid().getDataProvider().refreshAll();

        // Push UI updates if in push mode
        getUI().ifPresent(ui -> ui.push());
    }
}
