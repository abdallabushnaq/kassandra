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
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.rest.api.SprintApi;
import de.bushnaq.abdalla.kassandra.rest.api.TaskApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.WorklogApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.ScrumBoard;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.log4j.Log4j2;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route("active-sprints")
@PageTitle("Active Sprints")
@Menu(order = 4, icon = "vaadin:tasks", title = "Active Sprints")
@PermitAll
@RolesAllowed({"USER", "ADMIN"})
@Log4j2
public class ActiveSprints extends Main implements AfterNavigationObserver {
    public static final String                      ACTIVE_SPRINTS_PAGE_TITLE_ID = "active-sprints-title";
    public static final String                      ROUTE                        = "active-sprints";
    private             List<Sprint>                allSprints                   = new ArrayList<>();
    private final       VerticalLayout              contentLayout;
    private final       DateTimeFormatter           dateFormatter                = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private             String                      searchText                   = "";
    private final       SprintApi                   sprintApi;
    private             MultiSelectComboBox<Sprint> sprintFilter;
    private final       TaskApi                     taskApi;
    private final       UserApi                     userApi;
    private final       Map<Long, User>             userMap                      = new HashMap<>();
    private             List<User>                  users                        = new ArrayList<>();
    private final       WorklogApi                  worklogApi;

    public ActiveSprints(SprintApi sprintApi, TaskApi taskApi, UserApi userApi, WorklogApi worklogApi) {
        this.sprintApi  = sprintApi;
        this.taskApi    = taskApi;
        this.userApi    = userApi;
        this.worklogApi = worklogApi;

        try {
            // Set width full but not height - let content determine height for scrolling
            setWidthFull();
            // Make view background transparent, so AppLayout's gray background is visible
            getStyle().set("background-color", "transparent");

            addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

            // Create header
            HorizontalLayout headerLayout = createHeader();

            // Create content layout for scrum boards
            contentLayout = new VerticalLayout();
            contentLayout.setWidthFull();
            contentLayout.setPadding(false);
            contentLayout.setSpacing(true);
            contentLayout.addClassName(LumoUtility.Gap.LARGE);

            add(headerLayout, contentLayout);

            this.getStyle().set("padding-left", "var(--lumo-space-m)");
            this.getStyle().set("padding-right", "var(--lumo-space-m)");

        } catch (Exception e) {
            log.error("Error initializing ActiveSprints", e);
            throw e;
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        // Update breadcrumbs
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        mainLayout.getBreadcrumbs().addItem("Active Sprints", ActiveSprints.class);
                    }
                });

        // Load data and populate scrum boards
        loadData();
    }

    /**
     * Apply filters to the displayed sprints and tasks
     */
    private void applyFilters() {
        try {
            // Clear previous content
            contentLayout.removeAll();

            if (allSprints.isEmpty()) {
                Div emptyMessage = new Div();
                emptyMessage.setText("No active sprints found. Sprints must have status 'STARTED' to appear here.");
                emptyMessage.getStyle()
                        .set("padding", "var(--lumo-space-l)")
                        .set("text-align", "center")
                        .set("color", "var(--lumo-secondary-text-color)");
                contentLayout.add(emptyMessage);
                return;
            }

            // Get selected sprints from filter (or all if none selected)
            List<Sprint> sprintsToShow = allSprints;
            if (sprintFilter != null && sprintFilter.getSelectedItems() != null && !sprintFilter.getSelectedItems().isEmpty()) {
                sprintsToShow = new ArrayList<>(sprintFilter.getSelectedItems());
            }

            // Show all selected sprints - filtering happens at task level in ScrumBoard
            for (Sprint sprint : sprintsToShow) {
                createSprintSection(sprint, searchText);
            }

            if (sprintsToShow.isEmpty()) {
                Div emptyMessage = new Div();
                emptyMessage.setText("No sprints match the current filters.");
                emptyMessage.getStyle()
                        .set("padding", "var(--lumo-space-l)")
                        .set("text-align", "center")
                        .set("color", "var(--lumo-secondary-text-color)");
                contentLayout.add(emptyMessage);
            }

        } catch (Exception e) {
            log.error("Error applying filters", e);
            Div errorMessage = new Div();
            errorMessage.setText("Error applying filters: " + e.getMessage());
            errorMessage.getStyle()
                    .set("padding", "var(--lumo-space-l)")
                    .set("color", "var(--lumo-error-text-color)");
            contentLayout.add(errorMessage);
        }
    }

    /**
     * Creates the header layout with search, sprint filter, and clear button
     */
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        header.getStyle().set("padding", "var(--lumo-space-m)");
        header.setSpacing(true);

        // 1. Search input box with magnifying glass icon and label for alignment
        TextField searchField = new TextField();
        searchField.setLabel("Search");
        searchField.setPlaceholder("search board");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> {
            searchText = e.getValue() != null ? e.getValue().toLowerCase().trim() : "";
            applyFilters();
        });
        searchField.setWidth("300px");

        // 2. Sprint multi-select dropdown
        sprintFilter = new MultiSelectComboBox<>();
        sprintFilter.setLabel("Sprint");
        sprintFilter.setItemLabelGenerator(Sprint::getName);
        sprintFilter.setPlaceholder("Select sprints");
        sprintFilter.setWidth("300px");
        sprintFilter.addValueChangeListener(e -> applyFilters());

        // 3. Clear filter button
        Button clearButton = new Button("Clear filter", VaadinIcon.CLOSE_SMALL.create());
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearButton.addClickListener(e -> {
            searchField.clear();
            sprintFilter.clear();
            searchText = "";
            // Don't call applyFilters here - let the value change listeners handle it
        });

        header.add(searchField, sprintFilter, clearButton);

        return header;
    }

    /**
     * Create a section for a single sprint with its scrum board
     */
    private void createSprintSection(Sprint sprint, String filterText) {
        // Create container for this sprint
        VerticalLayout sprintContainer = new VerticalLayout();
        sprintContainer.setWidthFull();
        sprintContainer.setPadding(false);
        sprintContainer.setSpacing(true);
        sprintContainer.getStyle()
                .set("margin-bottom", "var(--lumo-space-l)");

        // Create sprint header
        HorizontalLayout sprintHeader = new HorizontalLayout();
        sprintHeader.setWidthFull();
        sprintHeader.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

        Icon sprintIcon = VaadinIcon.CALENDAR_CLOCK.create();
        sprintIcon.getStyle().set("margin-right", "var(--lumo-space-s)");

        H3 sprintTitle = new H3(sprint.getName());
        sprintTitle.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "500");

        // Add sprint dates
        String dateRange = "";
        if (sprint.getStart() != null && sprint.getEnd() != null) {
            dateRange = " (" + dateFormatter.format(sprint.getStart()) +
                    " - " + dateFormatter.format(sprint.getEnd()) + ")";
        }
        Div dateInfo = new Div();
        dateInfo.setText(dateRange);
        dateInfo.getStyle()
                .set("margin-left", "var(--lumo-space-m)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        HorizontalLayout titleLayout = new HorizontalLayout(sprintIcon, sprintTitle, dateInfo);
        titleLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(false);

        sprintHeader.add(titleLayout);

        // Create ScrumBoard for this sprint with filter text for task-level filtering
        ScrumBoard scrumBoard = new ScrumBoard(sprint, taskApi, userMap, filterText);

        // Add header and scrum board to sprint container
        sprintContainer.add(sprintHeader, scrumBoard);

        // Add sprint container to main content
        contentLayout.add(sprintContainer);
    }

    /**
     * Load all active sprints and create a scrum board for each
     */
    private void loadData() {
        try {
            // Load all users
            users = userApi.getAll();
            userMap.clear();
            for (User user : users) {
                userMap.put(user.getId(), user);
            }

            // Load all sprints and filter only STARTED sprints
            List<Sprint> allSprintsFromApi = sprintApi.getAll();
            allSprints = allSprintsFromApi.stream()
                    .filter(sprint -> sprint.getStatus() == Status.STARTED)
                    .collect(Collectors.toList());

            // Load tasks and worklogs for each sprint and initialize
            for (Sprint sprint : allSprints) {
                List<Task>    tasks    = taskApi.getAll(sprint.getId());
                List<Worklog> worklogs = worklogApi.getAll(sprint.getId());

                // Initialize sprint with all transient fields
                sprint.initialize();
                sprint.initUserMap(users);
                sprint.initTaskMap(tasks, worklogs);
                sprint.recalculate(ParameterOptions.getLocalNow());
            }

            // Populate sprint filter dropdown with all active sprints
            if (sprintFilter != null) {
                sprintFilter.setItems(allSprints);

                // Select first sprint by default
                if (!allSprints.isEmpty()) {
                    sprintFilter.setValue(java.util.Set.of(allSprints.get(0)));
                }
            }

            // Apply filters to show sprints (will be called by value change listener)
            // Only call if no sprints to avoid double call
            if (allSprints.isEmpty()) {
                applyFilters();
            }

        } catch (Exception e) {
            log.error("Error loading active sprint data", e);
            contentLayout.removeAll();
            Div errorMessage = new Div();
            errorMessage.setText("Error loading active sprints: " + e.getMessage());
            errorMessage.getStyle()
                    .set("padding", "var(--lumo-space-l)")
                    .set("color", "var(--lumo-error-text-color)");
            contentLayout.add(errorMessage);
        }
    }
}

