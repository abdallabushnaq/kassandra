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

import java.util.*;
import java.util.stream.Collectors;

@Route("active-sprints")
@PageTitle("Active Sprints")
@Menu(order = 4, icon = "vaadin:tasks", title = "Active Sprints")
@PermitAll
@RolesAllowed({"USER", "ADMIN"})
@Log4j2
public class ActiveSprints extends Main implements AfterNavigationObserver {
    public static final String                      ID_CLEAR_FILTERS_BUTTON = "clear-filters-button";
    //    public static final String                      ACTIVE_SPRINTS_PAGE_TITLE_ID = "active-sprints-title";
    public static final String                      ROUTE                   = "active-sprints";
    private             List<Sprint>                allSprints              = new ArrayList<>();
    private final       VerticalLayout              contentLayout;
    //    private final       DateTimeFormatter           dateFormatter      = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final       FeatureApi                  featureApi;
    private final       Map<Long, Feature>          featureMap              = new HashMap<>();
    private             ComboBox<GroupingMode>      groupingModeSelector;
    private             boolean                     hasUrlParameters        = false;
    private             boolean                     isRestoringFromUrl      = false;
    private             String                      savedGroupByValue       = null;
    private             String                      savedSprintIds          = null;
    private             String                      savedUserIds            = null;
    private             String                      searchText              = "";
    private             Set<Sprint>                 selectedSprints         = new HashSet<>();
    private             Set<User>                   selectedUsers           = new HashSet<>();
    private final       SprintApi                   sprintApi;
    private             MultiSelectComboBox<Sprint> sprintSelector;
    private final       TaskApi                     taskApi;
    private final       UserApi                     userApi;
    private final       Map<Long, User>             userMap                 = new HashMap<>();
    private             MultiSelectComboBox<User>   userSelector;
    private             List<User>                  users                   = new ArrayList<>();
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

        // Read query parameters directly (like SprintListView does)
        com.vaadin.flow.router.Location location        = event.getLocation();
        QueryParameters                 queryParameters = location.getQueryParameters();

//        log.info("=== afterNavigation called (ActiveSprints) ===");
//        log.info("URL: {}", location.getPath());
//        log.info("Query params from event: {}", queryParameters.getParameters());

        // Check if this is likely the "first" afterNavigation on F5 (no params but UI might have them)
        // If the event has no params but we have component state, skip this call
        if (queryParameters.getParameters().isEmpty() && !selectedSprints.isEmpty()) {
            log.info("Event has no params but we have state - likely first afterNavigation on F5, skipping");
            return;
        }

        // Set flag to prevent URL updates during restoration
        isRestoringFromUrl = true;

        // Check if any parameters exist (to distinguish first visit from cleared filters)
        hasUrlParameters = !queryParameters.getParameters().isEmpty();
        log.info("hasUrlParameters: {}", hasUrlParameters);

        // Restore filter state from URL - save to fields
        if (queryParameters.getParameters().containsKey("sprints")) {
            savedSprintIds = queryParameters.getParameters().get("sprints").get(0);
            log.info("Restored sprints from URL: {}", savedSprintIds);
        } else {
            savedSprintIds = null;
            log.info("No sprints in URL, set to null");
        }
        if (queryParameters.getParameters().containsKey("users")) {
            savedUserIds = queryParameters.getParameters().get("users").get(0);
            log.info("Restored users from URL: {}", savedUserIds);
        } else {
            savedUserIds = null;
            log.info("No users in URL, set to null");
        }
        if (queryParameters.getParameters().containsKey("groupBy")) {
            savedGroupByValue = queryParameters.getParameters().get("groupBy").get(0);
            log.info("Restored groupBy from URL: {}", savedGroupByValue);
        } else {
            savedGroupByValue = null;
            log.info("No groupBy in URL, set to null");
        }
        if (queryParameters.getParameters().containsKey("search")) {
            searchText = queryParameters.getParameters().get("search").get(0);
            log.info("Restored search from URL: {}", searchText);
        } else {
            searchText = "";
            log.info("No search in URL, set to empty");
        }

        // Load data and populate scrum boards
        loadData();

        // Clear restoration flag after load complete
        isRestoringFromUrl = false;
//        log.info("=== afterNavigation complete (ActiveSprints) ===");
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
            // Only update if change is from user interaction
            if (e.isFromClient()) {
                searchText = e.getValue() != null ? e.getValue().toLowerCase().trim() : "";
                updateUrlParameters();
                applyFilters();
            }
        });
        searchField.setWidth("300px");

        // 2. User multi-select dropdown
        userSelector = new MultiSelectComboBox<>();
        userSelector.setLabel("User");
        userSelector.setItemLabelGenerator(User::getName);
        userSelector.setPlaceholder("Select users");
        userSelector.setWidth("300px");
        userSelector.addValueChangeListener(e -> {
            log.info("=== User selector value changed (ActiveSprints) ===");
            log.info("isFromClient: {}", e.isFromClient());
            log.info("Old value IDs: {}", e.getOldValue().stream().map(User::getId).toList());
            log.info("New value IDs: {}", e.getValue().stream().map(User::getId).toList());
            log.info("selectedUsers IDs before: {}", selectedUsers.stream().map(User::getId).toList());

            // Only update if change is from user interaction
            if (e.isFromClient()) {
                selectedUsers = new HashSet<>(e.getValue());
                log.info("selectedUsers IDs after update: {}", selectedUsers.stream().map(User::getId).toList());
                log.info("Calling updateUrlParameters synchronously...");
                updateUrlParameters();
                log.info("Calling applyFilters...");
                applyFilters();
            } else {
                log.info("Skipping update - not from client");
            }
        });

        // 3. Sprint multi-select dropdown
        sprintSelector = new MultiSelectComboBox<>();
        sprintSelector.setLabel("Sprint");
        sprintSelector.setItemLabelGenerator(Sprint::getName);
        sprintSelector.setPlaceholder("Select sprints");
        sprintSelector.setWidth("300px");
        sprintSelector.addValueChangeListener(e -> {
            // Only update if change is from user interaction
            if (e.isFromClient()) {
                selectedSprints = new HashSet<>(e.getValue());
                updateUrlParameters();
                applyFilters();
            }
        });

        // 4. Grouping mode selector
        groupingModeSelector = new ComboBox<>();
        groupingModeSelector.setLabel("Group by");
        groupingModeSelector.setItems(GroupingMode.values());
        groupingModeSelector.setItemLabelGenerator(GroupingMode::getDisplayName);
        groupingModeSelector.setValue(GroupingMode.FEATURES); // Default to Features mode
        groupingModeSelector.setWidth("200px");
        groupingModeSelector.addValueChangeListener(e -> {
            // Only update if change is from user interaction
            if (e.isFromClient()) {
                updateUrlParameters();
                applyFilters();
            }
        });

        // 5. Clear filter button (right after filters, no spacer)
        Button clearButton = new Button("Clear filter", VaadinIcon.CLOSE_SMALL.create());
        clearButton.setId(ID_CLEAR_FILTERS_BUTTON);
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearButton.addClickListener(e -> {
            searchField.clear();
            userSelector.clear();
            sprintSelector.clear();
            selectedSprints.clear();
            selectedUsers.clear();
            searchText = "";
            groupingModeSelector.setValue(GroupingMode.FEATURES);
            updateUrlParameters();
            applyFilters();
        });

        header.add(searchField, userSelector, sprintSelector, groupingModeSelector, clearButton);

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
                selectedUsers,
                worklogApi
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

                // Restore selected users from URL
                if (savedUserIds != null && !savedUserIds.isEmpty()) {
                    Set<User> usersToSelect = new HashSet<>();
                    for (String idStr : savedUserIds.split(",")) {
                        try {
                            Long id = Long.parseLong(idStr.trim());
                            users.stream()
                                    .filter(u -> u.getId().equals(id))
                                    .findFirst()
                                    .ifPresent(usersToSelect::add);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid user ID in URL: {}", idStr);
                        }
                    }
                    if (!usersToSelect.isEmpty()) {
                        selectedUsers = usersToSelect;
                        userSelector.setValue(usersToSelect);
                    }
                }
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

                // Restore selected sprints from URL or select first sprint by default
                if (savedSprintIds != null && !savedSprintIds.isEmpty()) {
                    Set<Sprint> sprintsToSelect = new HashSet<>();
                    for (String idStr : savedSprintIds.split(",")) {
                        try {
                            Long id = Long.parseLong(idStr.trim());
                            allSprints.stream()
                                    .filter(s -> s.getId().equals(id))
                                    .findFirst()
                                    .ifPresent(sprintsToSelect::add);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid sprint ID in URL: {}", idStr);
                        }
                    }
                    if (!sprintsToSelect.isEmpty()) {
                        selectedSprints = sprintsToSelect;
                        sprintSelector.setValue(sprintsToSelect);
                    }
                } else if (!hasUrlParameters && !allSprints.isEmpty()) {
                    // Only select first sprint by default if NO URL parameters at all (first visit)
                    // If hasUrlParameters is true but no sprints, user explicitly cleared all sprints
                    selectedSprints = Set.of(allSprints.get(0));
                    sprintSelector.setValue(selectedSprints);
                }
            }

            // Restore grouping mode from URL
            if (savedGroupByValue != null && groupingModeSelector != null) {
                try {
                    GroupingMode mode = GroupingMode.valueOf(savedGroupByValue);
                    groupingModeSelector.setValue(mode);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid groupBy value in URL: {}", savedGroupByValue);
                }
            }

            // Restore search text in field
            if (searchText != null && !searchText.isEmpty()) {
                getUI().ifPresent(ui -> ui.access(() -> {
                    if (getChildren().findFirst().isPresent()) {
                        getChildren()
                                .filter(c -> c instanceof HorizontalLayout)
                                .findFirst()
                                .ifPresent(header -> {
                                    header.getChildren()
                                            .filter(c -> c instanceof TextField)
                                            .map(c -> (TextField) c)
                                            .findFirst()
                                            .ifPresent(field -> field.setValue(searchText));
                                });
                    }
                }));
            }

            // Apply filters to show sprints
            if (!allSprints.isEmpty()) {
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

    /**
     * Update URL parameters to persist filter state
     */
    private void updateUrlParameters() {
        // Don't update URL if we're currently restoring from URL (prevents infinite loop)
        if (isRestoringFromUrl) {
            return;
        }

        Map<String, String> params = new HashMap<>();

        if (!searchText.isEmpty()) {
            params.put("search", searchText);
        }

        if (!selectedUsers.isEmpty()) {
            String userIds = selectedUsers.stream()
                    .map(u -> String.valueOf(u.getId()))
                    .collect(Collectors.joining(","));
            params.put("users", userIds);
        }

        if (!selectedSprints.isEmpty()) {
            String sprintIds = selectedSprints.stream()
                    .map(s -> String.valueOf(s.getId()))
                    .collect(Collectors.joining(","));
            params.put("sprints", sprintIds);
        }

        if (groupingModeSelector != null && groupingModeSelector.getValue() != null) {
            params.put("groupBy", groupingModeSelector.getValue().name());
        }

        // Always add a marker parameter so we can distinguish first visit from cleared filters
        if (params.isEmpty()) {
            params.put("_", ""); // Empty marker parameter
        }

        // Update URL with query parameters
        QueryParameters queryParameters = QueryParameters.simple(params);
        getUI().ifPresent(ui -> ui.navigate(ActiveSprints.class, queryParameters));
    }
}

