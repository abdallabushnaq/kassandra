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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import de.bushnaq.abdalla.kassandra.ai.filter.AiFilterService;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.UserWorkWeek;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserWorkWeekApi;
import de.bushnaq.abdalla.kassandra.rest.api.WorkWeekApi;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.UserWorkWeekDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View for managing work week assignments for a single user.
 * Mirrors the pattern of {@link LocationListView}: the user is identified via the URL parameter
 * {@code user-email}, or falls back to the currently authenticated user.
 */
@Route(value = "user-work-week/:user-email?", layout = MainLayout.class)
@PageTitle("User Work Week")
@PermitAll
public class UserWorkWeekListView extends AbstractMainGrid<UserWorkWeek>
        implements BeforeEnterObserver, AfterNavigationObserver {

    public static final String CREATE_BUTTON             = "create-user-work-week-button";
    public static final String GLOBAL_FILTER             = "user-work-week-global-filter";
    public static final String GRID                      = "user-work-week-grid";
    public static final String GRID_DELETE_BUTTON_PREFIX = "user-work-week-delete-button-";
    public static final String GRID_EDIT_BUTTON_PREFIX   = "user-work-week-edit-button-";
    public static final String GRID_NAME_PREFIX          = "user-work-week-name-";
    public static final String GRID_START_DATE_PREFIX    = "user-work-week-start-";
    public static final String PAGE_TITLE                = "user-work-week-page-title";
    public static final String ROUTE                     = "user-work-week";
    public static final String ROW_COUNTER               = "user-work-week-row-counter";
    private       User              currentUser;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final UserApi           userApi;
    private final UserWorkWeekApi   userWorkWeekApi;
    private final WorkWeekApi       workWeekApi;

    /**
     * Constructs the view.
     *
     * @param userWorkWeekApi REST client for user work-week assignments
     * @param workWeekApi     REST client for global work weeks
     * @param userApi         REST client for users
     * @param clock           application clock
     * @param aiFilterService AI filter service
     * @param mapper          JSON mapper
     */
    public UserWorkWeekListView(UserWorkWeekApi userWorkWeekApi, WorkWeekApi workWeekApi, UserApi userApi,
                                Clock clock, AiFilterService aiFilterService, JsonMapper mapper) {
        super(clock);
        this.userWorkWeekApi = userWorkWeekApi;
        this.workWeekApi     = workWeekApi;
        this.userApi         = userApi;

        add(
                createSmartHeader(
                        "User Work Week",
                        PAGE_TITLE,
                        VaadinIcon.CALENDAR,
                        CREATE_BUTTON,
                        () -> openDialog(null),
                        ROW_COUNTER,
                        GLOBAL_FILTER,
                        aiFilterService, mapper, "UserWorkWeek"
                ),
                getGridPanelWrapper()
        );
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        refreshGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String       userEmailParam   = event.getRouteParameters().get("user-email").orElse(null);
        final String currentUserEmail = SecurityUtils.getUserEmail();
        final String userEmail        = (userEmailParam == null && currentUserEmail != null) ? currentUserEmail : userEmailParam;

        if (userEmail != null) {
            try {
                currentUser = userApi.getByEmail(userEmail).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                    Notification notification = Notification.show("User not found: " + userEmail, 3000, Notification.Position.MIDDLE);
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    event.forwardTo("");
                } else {
                    throw ex;
                }
            }
        } else {
            event.forwardTo("");
        }
    }

    private void confirmDelete(UserWorkWeek userWorkWeek) {
        if (getDataProvider().getItems().size() <= 1) {
            Notification notification = Notification.show(
                    "Cannot delete - Users must have at least one work week assignment", 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog("Confirm Delete",
                "Are you sure you want to remove this work week assignment?",
                "Delete",
                () -> {
                    try {
                        userWorkWeekApi.deleteById(currentUser.getId(), userWorkWeek.getId());
                        refreshGrid();
                        Notification.show("Work week assignment removed", 3000, Notification.Position.MIDDLE);
                    } catch (Exception ex) {
                        Notification notification = Notification.show(
                                "Failed to delete: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
        dialog.open();
    }

    @Override
    protected void initGrid(Clock clock) {
        getGrid().setId(GRID);

        // Start date column
        {
            Grid.Column<UserWorkWeek> col = getGrid().addColumn(new ComponentRenderer<>(uww -> {
                String dateStr = uww.getStart() != null ? uww.getStart().format(dateFormatter) : "";
                Span   span    = new Span(dateStr);
                span.setId(GRID_START_DATE_PREFIX + dateStr);
                return span;
            }));
            VaadinUtil.addSimpleHeader(col, "Start Date", VaadinIcon.CALENDAR);
        }

        // Work week name column
        {
            Grid.Column<UserWorkWeek> col = getGrid().addColumn(new ComponentRenderer<>(uww -> {
                String name = uww.getWorkWeek() != null ? uww.getWorkWeek().getName() : "";
                Span   span = new Span(name);
                span.setId(GRID_NAME_PREFIX + name);
                return span;
            }));
            VaadinUtil.addSimpleHeader(col, "Work Week", VaadinIcon.CALENDAR);
        }

        // Working days summary column
        {
            Grid.Column<UserWorkWeek> col = getGrid().addColumn(new ComponentRenderer<>(uww -> {
                String summary = uww.getWorkWeek() != null ? uww.getWorkWeek().getWorkingDaysSummary() : "";
                return new Span(summary);
            }));
            VaadinUtil.addSimpleHeader(col, "Working Days", VaadinIcon.CLOCK);
        }

        // Action column
        VaadinUtil.addActionColumn(
                getGrid(),
                GRID_EDIT_BUTTON_PREFIX,
                GRID_DELETE_BUTTON_PREFIX,
                uww -> uww.getStart() != null ? uww.getStart().format(dateFormatter) : "",
                this::openDialog,
                this::confirmDelete,
                uww -> {
                    if (getDataProvider().getItems().size() <= 1) {
                        return VaadinUtil.DeleteValidationResult.invalid("Cannot delete - Users must have at least one work week assignment");
                    }
                    // The first assignment (earliest start date) cannot be deleted
                    UserWorkWeek first = getDataProvider().getItems().stream()
                            .filter(u -> u.getStart() != null)
                            .min(Comparator.comparing(UserWorkWeek::getStart))
                            .orElse(null);
                    if (first != null && first.getId() != null && first.getId().equals(uww.getId())) {
                        return VaadinUtil.DeleteValidationResult.invalid("Cannot delete the first work week assignment");
                    }
                    return VaadinUtil.DeleteValidationResult.valid();
                }
        );
    }

    private void openDialog(UserWorkWeek userWorkWeek) {
        UserWorkWeekDialog dialog = new UserWorkWeekDialog(
                userWorkWeek, currentUser, userWorkWeekApi, workWeekApi, this::refreshGrid);
        dialog.open();
    }

    private void refreshGrid() {
        if (currentUser != null) {
            currentUser = userApi.getById(currentUser.getId());
            List<UserWorkWeek> sorted = currentUser.getUserWorkWeeks().stream()
                    .sorted(Comparator.comparing(UserWorkWeek::getStart,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .collect(Collectors.toList());
            getDataProvider().getItems().clear();
            getDataProvider().getItems().addAll(sorted);
            getDataProvider().refreshAll();
        }
    }
}


