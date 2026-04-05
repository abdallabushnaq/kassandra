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
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bushnaq.abdalla.kassandra.ai.filter.AiFilterService;
import de.bushnaq.abdalla.kassandra.dto.WorkWeek;
import de.bushnaq.abdalla.kassandra.rest.api.WorkWeekApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.WorkWeekDialog;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.RolesAllowed;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

/**
 * Admin-only view for managing global work week definitions.
 * Allows admins to create, edit and delete work weeks that can then be assigned to users.
 */
@Route(value = "work-week-list", layout = MainLayout.class)
@PageTitle("Work Week List")
@RolesAllowed("ADMIN")
public class WorkWeekListView extends AbstractMainGrid<WorkWeek> implements AfterNavigationObserver {

    public static final String CREATE_BUTTON             = "create-work-week-button";
    public static final String GLOBAL_FILTER             = "work-week-global-filter";
    public static final String GRID                      = "work-week-grid";
    public static final String GRID_CANNOT_DELETE_BUTTON = "work-week-cannot-delete-ok-button";
    public static final String GRID_DELETE_BUTTON_PREFIX = "work-week-delete-button-";
    public static final String GRID_DESCRIPTION_PREFIX   = "work-week-description-";
    public static final String GRID_EDIT_BUTTON_PREFIX   = "work-week-edit-button-";
    public static final String GRID_NAME_PREFIX          = "work-week-name-";
    public static final String GRID_USER_COUNT_PREFIX    = "work-week-user-count-";
    public static final String GRID_WORKING_DAYS_PREFIX  = "work-week-working-days-";
    public static final String PAGE_TITLE                = "work-week-page-title";
    public static final String ROUTE                     = "work-week-list";
    public static final String ROW_COUNTER               = "work-week-row-counter";
    private final WorkWeekApi workWeekApi;

    /**
     * Constructs the view.
     *
     * @param workWeekApi     REST client for work weeks
     * @param clock           application clock
     * @param aiFilterService AI filter service
     * @param mapper          JSON mapper
     */
    public WorkWeekListView(WorkWeekApi workWeekApi, Clock clock, AiFilterService aiFilterService, JsonMapper mapper) {
        super(clock);
        this.workWeekApi = workWeekApi;

        add(
                createSmartHeader(
                        "Work Weeks",
                        PAGE_TITLE,
                        VaadinIcon.CALENDAR,
                        CREATE_BUTTON,
                        () -> openDialog(null),
                        ROW_COUNTER,
                        GLOBAL_FILTER,
                        aiFilterService, mapper, "WorkWeek"
                ),
                getGridPanelWrapper()
        );
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        refreshGrid();
    }

    private void confirmDelete(WorkWeek workWeek) {
        if (workWeek.getUserCount() > 0) {
            // Show an informational dialog explaining why deletion is not possible
            int count = workWeek.getUserCount();
            String message = "Cannot delete work week \"" + workWeek.getName() + "\": it is currently assigned to "
                    + count + (count == 1 ? " user." : " users.")
                    + " Reassign or remove all user assignments before deleting.";
            com.vaadin.flow.component.dialog.Dialog infoDialog = new com.vaadin.flow.component.dialog.Dialog();
            infoDialog.setWidth("480px");
            infoDialog.getHeader().add(VaadinUtil.createDialogHeader("Cannot Delete", VaadinIcon.WARNING));
            com.vaadin.flow.component.orderedlayout.VerticalLayout layout =
                    new com.vaadin.flow.component.orderedlayout.VerticalLayout();
            layout.setPadding(false);
            layout.setSpacing(true);
            layout.add(new Span(message));
            com.vaadin.flow.component.button.Button okButton =
                    new com.vaadin.flow.component.button.Button("OK",
                            new com.vaadin.flow.component.icon.Icon(VaadinIcon.CHECK));
            okButton.setId(GRID_CANNOT_DELETE_BUTTON);
            okButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
            okButton.addClickListener(e -> infoDialog.close());
            layout.add(okButton);
            infoDialog.add(layout);
            infoDialog.open();
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog("Confirm Delete",
                "Are you sure you want to delete work week \"" + workWeek.getName() + "\"?",
                "Delete",
                () -> {
                    try {
                        workWeekApi.deleteById(workWeek.getId());
                        refreshGrid();
                    } catch (Exception ex) {
                        Notification notification =
                                Notification.show(
                                        "Failed to delete: " + ex.getMessage(), 3000,
                                        Notification.Position.MIDDLE);
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
        dialog.open();
    }

    @Override
    protected void initGrid(Clock clock) {
        getGrid().setId(GRID);

        // Name column
        {
            Grid.Column<WorkWeek> col = getGrid().addColumn(new ComponentRenderer<>(ww -> {
                Span span = new Span(ww.getName());
                span.setId(GRID_NAME_PREFIX + ww.getName());
                return span;
            }));
            VaadinUtil.addSimpleHeader(col, "Name", VaadinIcon.TAG);
        }

        // Description column
        {
            Grid.Column<WorkWeek> col = getGrid().addColumn(new ComponentRenderer<>(ww -> {
                String text = ww.getDescription() != null ? ww.getDescription() : "";
                Span   span = new Span(text);
                span.setId(GRID_DESCRIPTION_PREFIX + (ww.getId() != null ? ww.getId() : ""));
                return span;
            }));
            VaadinUtil.addSimpleHeader(col, "Description", VaadinIcon.INFO_CIRCLE);
        }

        // Working days summary column
        {
            Grid.Column<WorkWeek> col = getGrid().addColumn(new ComponentRenderer<>(ww -> {
                String text = ww.getWorkingDaysSummary();
                Span   span = new Span(text);
                span.setId(GRID_WORKING_DAYS_PREFIX + (ww.getId() != null ? ww.getId() : ""));
                return span;
            }));
            VaadinUtil.addSimpleHeader(col, "Working Days", VaadinIcon.CALENDAR);
        }

        // Users column – shows how many users reference this work week
        {
            Grid.Column<WorkWeek> col = getGrid().addColumn(new ComponentRenderer<>(ww -> {
                int  count = ww.getUserCount();
                Span span  = new Span(count + (count == 1 ? " user" : " users"));
                span.setId(GRID_USER_COUNT_PREFIX + ww.getName());
                span.getStyle()
                        .set("display", "inline-block")
                        .set("padding", "2px 8px")
                        .set("border-radius", "12px")
                        .set("background-color", count > 0
                                ? "var(--lumo-primary-color-10pct)"
                                : "var(--lumo-contrast-10pct)")
                        .set("color", count > 0
                                ? "var(--lumo-primary-text-color)"
                                : "var(--lumo-secondary-text-color)")
                        .set("font-size", "var(--lumo-font-size-s)")
                        .set("font-weight", "500");
                return span;
            }));
            VaadinUtil.addSimpleHeader(col, "Users", VaadinIcon.USERS);
        }

        // Action column
        VaadinUtil.addActionColumn(
                getGrid(),
                GRID_EDIT_BUTTON_PREFIX,
                GRID_DELETE_BUTTON_PREFIX,
                ww -> ww.getName(),
                this::openDialog,
                this::confirmDelete
        );
    }

    private void openDialog(WorkWeek workWeek) {
        WorkWeekDialog dialog = new WorkWeekDialog(workWeek, workWeekApi, this::refreshGrid);
        dialog.open();
    }

    private void refreshGrid() {
        getDataProvider().getItems().clear();
        getDataProvider().getItems().addAll(workWeekApi.getAll());
        getDataProvider().refreshAll();
    }
}

