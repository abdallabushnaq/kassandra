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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.UserDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.PermitAll;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@Route("user-list")
@PageTitle("User List Page")
@Menu(order = 2, icon = "vaadin:users", title = "Users")
@PermitAll // When security is enabled, allow all authenticated users
public class UserListView extends AbstractMainGrid<User> implements AfterNavigationObserver {
    public static final String                 CREATE_USER_BUTTON             = "create-user-button";
    public static final String                 ROUTE                          = "user-list";
    public static final String                 USER_GLOBAL_FILTER             = "user-global-filter";
    public static final String                 USER_GRID                      = "user-grid";
    public static final String                 USER_GRID_DELETE_BUTTON_PREFIX = "user-grid-delete-button-prefix-";
    public static final String                 USER_GRID_EDIT_BUTTON_PREFIX   = "user-grid-edit-button-prefix-";
    public static final String                 USER_GRID_NAME_PREFIX          = "user-grid-name-";
    public static final String                 USER_LIST_PAGE_TITLE           = "user-list-page-title";
    public static final String                 USER_ROW_COUNTER               = "user-row-counter";
    private final       Clock                  clock;
    private final       StableDiffusionService stableDiffusionService;
    private final       UserApi                userApi;

    public UserListView(UserApi userApi, Clock clock, AiFilterService aiFilterService, ObjectMapper mapper, StableDiffusionService stableDiffusionService) {
        super(clock);
        this.userApi                = userApi;
        this.clock                  = clock;
        this.stableDiffusionService = stableDiffusionService;

        add(
                createSmartHeader(
                        "Users",
                        USER_LIST_PAGE_TITLE,
                        VaadinIcon.USERS,
                        CREATE_USER_BUTTON,
                        () -> openUserDialog(null),
                        USER_ROW_COUNTER,
                        USER_GLOBAL_FILTER,
                        aiFilterService, mapper, "User"
                ),
                getGridPanelWrapper()
        );
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

        refreshGrid();
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
                String colorHex;
                if (user.getColor() != null) {
                    colorHex = "#" + Integer.toHexString(user.getColor().getRGB()).substring(2);
                } else {
                    colorHex = "#D3D3D3"; // Light Gray default
                }
                square.getStyle().set("background-color", colorHex);
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
