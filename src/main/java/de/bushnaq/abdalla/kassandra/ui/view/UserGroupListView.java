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

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.dto.UserGroup;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserGroupApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.UserGroupDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.RolesAllowed;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@Route("user-group-list")
@PageTitle("User Group List Page")
@RolesAllowed("ADMIN") // Only admins can access this view
public class UserGroupListView extends AbstractMainGrid<UserGroup> implements AfterNavigationObserver {
    public static final String       CREATE_GROUP_BUTTON             = "create-user-group-button";
    public static final String       GROUP_GLOBAL_FILTER             = "user-group-global-filter";
    public static final String       GROUP_GRID                      = "user-group-grid";
    public static final String       GROUP_GRID_DELETE_BUTTON_PREFIX = "user-group-grid-delete-button-prefix-";
    public static final String       GROUP_GRID_EDIT_BUTTON_PREFIX   = "user-group-grid-edit-button-prefix-";
    public static final String       GROUP_GRID_NAME_PREFIX          = "user-group-grid-name-";
    public static final String       GROUP_LIST_PAGE_TITLE           = "user-group-list-page-title";
    public static final String       GROUP_ROW_COUNTER               = "user-group-row-counter";
    public static final String       ROUTE                           = "user-group-list";
    private final       UserApi      userApi;
    private final       UserGroupApi userGroupApi;

    public UserGroupListView(UserGroupApi userGroupApi, UserApi userApi, Clock clock, AiFilterService aiFilterService, JsonMapper mapper) {
        super(clock);
        this.userGroupApi = userGroupApi;
        this.userApi      = userApi;

        add(
                createSmartHeader(
                        "User Groups",
                        GROUP_LIST_PAGE_TITLE,
                        VaadinIcon.GROUP,
                        CREATE_GROUP_BUTTON,
                        () -> openUserGroupDialog(null),
                        GROUP_ROW_COUNTER,
                        GROUP_GLOBAL_FILTER,
                        aiFilterService, mapper, "UserGroup"
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
                        mainLayout.getBreadcrumbs().addItem("User Groups", UserGroupListView.class);
                    }
                });

        refreshGrid();
    }

    private void confirmDelete(UserGroup userGroup) {
        String message = "Are you sure you want to delete user group \"" + userGroup.getName() + "\"?";
        ConfirmDialog dialog = new ConfirmDialog(
                "Confirm Delete",
                message,
                "Delete",
                () -> {
                    userGroupApi.deleteById(userGroup.getId());
                    refreshGrid();
                    Notification.show("User group deleted", 3000, Notification.Position.BOTTOM_START);
                }
        );
        dialog.open();
    }

    protected void initGrid(Clock clock) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(clock.getZone()).withLocale(getLocale());

        getGrid().setId(GROUP_GRID);

        // Add click listener to open edit dialog when clicking on a row
        getGrid().addItemClickListener(event -> {
            UserGroup selectedUserGroup = event.getItem();
            openUserGroupDialog(selectedUserGroup);
        });

        {
            Grid.Column<UserGroup> keyColumn = getGrid().addColumn(UserGroup::getKey);
            VaadinUtil.addFilterableHeader(getGrid(), keyColumn, "Key", VaadinIcon.KEY, UserGroup::getKey);
        }

        {
            // Add name column with filtering and sorting
            Grid.Column<UserGroup> nameColumn = getGrid().addColumn(new ComponentRenderer<>(userGroup -> {
                Div div = new Div();
                div.add(userGroup.getName());
                div.setId(GROUP_GRID_NAME_PREFIX + userGroup.getName());
                return div;
            }));

            // Configure a custom comparator to properly sort by the name property
            nameColumn.setComparator((userGroup1, userGroup2) -> userGroup1.getName().compareToIgnoreCase(userGroup2.getName()));

            VaadinUtil.addSimpleHeader(nameColumn, "Name", VaadinIcon.GROUP);
        }

        {
            Grid.Column<UserGroup> descriptionColumn = getGrid().addColumn(UserGroup::getDescription);
            VaadinUtil.addSimpleHeader(descriptionColumn, "Description", VaadinIcon.INFO_CIRCLE);
        }

        {
            // Add member count column with badge styling
            Grid.Column<UserGroup> memberCountColumn = getGrid().addColumn(new ComponentRenderer<>(userGroup -> {
                Div badge = new Div();
                int count = userGroup.getMemberCount();
                badge.setText(count + (count == 1 ? " member" : " members"));
                badge.getStyle()
                        .set("display", "inline-block")
                        .set("padding", "2px 8px")
                        .set("border-radius", "12px")
                        .set("background-color", "var(--lumo-contrast-10pct)")
                        .set("font-size", "var(--lumo-font-size-s)")
                        .set("font-weight", "500");
                return badge;
            }));
            VaadinUtil.addSimpleHeader(memberCountColumn, "Members", VaadinIcon.USERS);
        }

        {
            Grid.Column<UserGroup> createdColumn = getGrid().addColumn(userGroup -> dateTimeFormatter.format(userGroup.getCreated()));
            VaadinUtil.addSimpleHeader(createdColumn, "Created", VaadinIcon.CALENDAR);
        }

        {
            Grid.Column<UserGroup> updatedColumn = getGrid().addColumn(userGroup -> dateTimeFormatter.format(userGroup.getUpdated()));
            VaadinUtil.addSimpleHeader(updatedColumn, "Updated", VaadinIcon.CALENDAR);
        }

        // Add actions column using VaadinUtil
        VaadinUtil.addActionColumn(
                getGrid(),
                GROUP_GRID_EDIT_BUTTON_PREFIX,
                GROUP_GRID_DELETE_BUTTON_PREFIX,
                UserGroup::getName,
                this::openUserGroupDialog,
                this::confirmDelete
        );

    }

    private void openUserGroupDialog(UserGroup userGroup) {
        UserGroupDialog dialog = new UserGroupDialog(userGroup, userGroupApi, userApi);
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

        // Fetch fresh data from API
        getDataProvider().getItems().addAll(userGroupApi.getAll());

        // Force complete refresh of the grid
        getDataProvider().refreshAll();

        // Force the grid to re-render
        getGrid().getDataProvider().refreshAll();

        // Push UI updates if in push mode
        getUI().ifPresent(ui -> ui.push());
    }
}

