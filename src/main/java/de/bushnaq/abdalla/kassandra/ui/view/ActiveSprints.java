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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.MergedScrumBoard;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.log4j.Log4j2;

import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private final       FeatureApi                  featureApi;
    private final       Map<Long, Feature>          featureMap                   = new HashMap<>();
    private             ComboBox<GroupingMode>      groupingModeSelector;
    private             String                      searchText                   = "";
    private             Set<Sprint>                 selectedSprints              = new HashSet<>();
    private             Set<User>                   selectedUsers                = new HashSet<>();
    private final       SprintApi                   sprintApi;
    private             MultiSelectComboBox<Sprint> sprintSelector;
    private final       TaskApi                     taskApi;
    private final       UserApi                     userApi;
    private final       Map<Long, User>             userMap                      = new HashMap<>();
    private             MultiSelectComboBox<User>   userSelector;
    private             List<User>                  users                        = new ArrayList<>();
    private final       WorklogApi                  worklogApi;

    public ActiveSprints(FeatureApi featureApi, SprintApi sprintApi, TaskApi taskApi, UserApi userApi, WorklogApi worklogApi) {
        this.featureApi = featureApi;
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
            List<Sprint> sprintsToShow = selectedSprints.isEmpty() ? allSprints : new ArrayList<>(selectedSprints);

            if (sprintsToShow.isEmpty()) {
                Div emptyMessage = new Div();
                emptyMessage.setText("No sprints match the current filters.");
                emptyMessage.getStyle()
                        .set("padding", "var(--lumo-space-l)")
                        .set("text-align", "center")
                        .set("color", "var(--lumo-secondary-text-color)");
                contentLayout.add(emptyMessage);
                return;
            }

            // Get grouping mode
            GroupingMode mode = groupingModeSelector != null ? groupingModeSelector.getValue() : GroupingMode.FEATURES;

            // Merge all selected sprints into a combined view
            createMergedBoard(sprintsToShow, mode, searchText);

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

        // 2. User multi-select dropdown
        userSelector = new MultiSelectComboBox<>();
        userSelector.setLabel("User");
        userSelector.setItemLabelGenerator(User::getName);
        userSelector.setPlaceholder("Select users");
        userSelector.setWidth("300px");
        userSelector.addValueChangeListener(e -> {
            selectedUsers = new HashSet<>(e.getValue());
            applyFilters();
        });

        // 3. Sprint multi-select dropdown
        sprintSelector = new MultiSelectComboBox<>();
        sprintSelector.setLabel("Sprint");
        sprintSelector.setItemLabelGenerator(Sprint::getName);
        sprintSelector.setPlaceholder("Select sprints");
        sprintSelector.setWidth("300px");
        sprintSelector.addValueChangeListener(e -> {
            selectedSprints = new HashSet<>(e.getValue());
            applyFilters();
        });

        // Add spacer to push remaining items to the right
        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");

        // 4. Grouping mode selector
        groupingModeSelector = new ComboBox<>();
        groupingModeSelector.setLabel("Group by");
        groupingModeSelector.setItems(GroupingMode.values());
        groupingModeSelector.setItemLabelGenerator(GroupingMode::getDisplayName);
        groupingModeSelector.setValue(GroupingMode.FEATURES); // Default to Features mode
        groupingModeSelector.setWidth("200px");
        groupingModeSelector.addValueChangeListener(e -> applyFilters());

        // 5. Clear filter button
        Button clearButton = new Button("Clear filter", VaadinIcon.CLOSE_SMALL.create());
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearButton.addClickListener(e -> {
            searchField.clear();
            userSelector.clear();
            sprintSelector.clear();
            selectedSprints.clear();
            selectedUsers.clear();
            searchText = "";
            // Don't call applyFilters here - let the value change listeners handle it
        });

        header.add(searchField, userSelector, sprintSelector, spacer, groupingModeSelector, clearButton);

        return header;
    }

    /**
     * Create a merged board combining all selected sprints
     */
    private void createMergedBoard(List<Sprint> sprints, GroupingMode mode, String filterText) {
        // Merge all tasks from selected sprints
        List<Task> allTasks = new ArrayList<>();
        for (Sprint sprint : sprints) {
            if (sprint.getTasks() != null) {
                allTasks.addAll(sprint.getTasks());
            }
        }

        if (allTasks.isEmpty()) {
            Div emptyMessage = new Div();
            emptyMessage.setText("No tasks found in selected sprints.");
            emptyMessage.getStyle()
                    .set("padding", "var(--lumo-space-l)")
                    .set("text-align", "center")
                    .set("color", "var(--lumo-secondary-text-color)");
            contentLayout.add(emptyMessage);
            return;
        }

        // Create a virtual sprint that contains all merged tasks
        Sprint mergedSprint = new Sprint();
        mergedSprint.setId(-1L); // Virtual ID
        mergedSprint.setName("Merged View");
        mergedSprint.getTasks().addAll(allTasks);

        // Set featureId based on mode - for features mode, we'll handle multiple features
        // For now, use the first sprint's featureId (will be handled in ScrumBoard)
        if (!sprints.isEmpty() && sprints.get(0).getFeatureId() != null) {
            mergedSprint.setFeatureId(sprints.get(0).getFeatureId());
        }

        // Create merged scrum board
        MergedScrumBoard mergedBoard = new MergedScrumBoard(
                sprints,
                allTasks,
                taskApi,
                userMap,
                filterText,
                mode,
                featureMap,
                selectedUsers
        );

        contentLayout.add(mergedBoard);
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

            // Populate user selector
            if (userSelector != null) {
                userSelector.setItems(users);
            }

            // Load all features and cache them
            List<Feature> allFeatures = featureApi.getAll();
            featureMap.clear();
            for (Feature feature : allFeatures) {
                featureMap.put(feature.getId(), feature);
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
            if (sprintSelector != null) {
                sprintSelector.setItems(allSprints);

                // Select first sprint by default if available
                if (!allSprints.isEmpty()) {
                    selectedSprints = Set.of(allSprints.get(0));
                    sprintSelector.setValue(selectedSprints);
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

