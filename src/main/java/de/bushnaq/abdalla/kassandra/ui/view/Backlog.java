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
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.rest.api.SprintApi;
import de.bushnaq.abdalla.kassandra.rest.api.TaskApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.WorklogApi;
import de.bushnaq.abdalla.kassandra.service.DatabaseDebugService;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.BacklogDragDropHandler;
import de.bushnaq.abdalla.kassandra.ui.component.SprintCard;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Route(value = "backlog", layout = MainLayout.class)
@PageTitle("Backlog")
@Menu(order = 3, icon = "vaadin:grid-v", title = "Backlog")
@PermitAll
@RolesAllowed({"USER", "ADMIN"})
@Log4j2
public class Backlog extends Main implements BeforeEnterObserver, AfterNavigationObserver {

    public static final String                      ROUTE              = "backlog";
    private             List<Sprint>                allSprints         = new ArrayList<>();
    private             Sprint                      backlogSprint;      // Cached backlog sprint
    private final       VerticalLayout              contentLayout;
    private final       DatabaseDebugService        databaseDebugService; // For debug print DB
    private final       BacklogDragDropHandler      dragDropHandler;
    private             boolean                     hasUrlParameters   = false;
    private             boolean                     isRestoringFromUrl = false;
    private             User                        loggedInUser;       // Current logged-in user
    private             String                      savedSprintIds     = null;
    private             String                      savedUserIds       = null;
    private             String                      searchText         = "";
    private             java.util.Set<Sprint>       selectedSprints    = new java.util.HashSet<>();
    private             java.util.Set<User>         selectedUsers      = new java.util.HashSet<>();
    private final       SprintApi                   sprintApi;
    private             MultiSelectComboBox<Sprint> sprintSelector;
    private final       TaskApi                     taskApi;
    private final       UserApi                     userApi;
    private final       java.util.Map<Long, User>   userMap            = new java.util.HashMap<>();
    private             MultiSelectComboBox<User>   userSelector;
    private             List<User>                  users              = new ArrayList<>();
    private final       WorklogApi                  worklogApi;

    public Backlog(SprintApi sprintApi, TaskApi taskApi, UserApi userApi, WorklogApi worklogApi, DatabaseDebugService databaseDebugService) {
        this.sprintApi            = sprintApi;
        this.taskApi              = taskApi;
        this.userApi              = userApi;
        this.worklogApi           = worklogApi;
        this.databaseDebugService = databaseDebugService;

        // Create drag-drop handler
        this.dragDropHandler = new BacklogDragDropHandler(taskApi, () -> {
            backlogSprint = null; // Clear cache
            loadData();           // Reload all data
        });

        // Load current user
        String userEmail = getUserEmail();
        try {
            loggedInUser = userApi.getByEmail(userEmail);
        } catch (ResponseStatusException e) {
            log.warn("Could not find user with email: {}", userEmail);
        }

        try {
            // Set width full but not height - let content determine height for scrolling
            setWidthFull();
            // Make view background transparent, so AppLayout's gray background is visible
            getStyle().set("background-color", "transparent");

            addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

            // Create header with filters
            HorizontalLayout headerLayout = createHeader();

            // Create content layout for sprint cards
            contentLayout = new VerticalLayout();
            contentLayout.setWidthFull();
            contentLayout.setPadding(false);
            contentLayout.setSpacing(true);
            contentLayout.addClassName(LumoUtility.Gap.LARGE);

            add(headerLayout, contentLayout);

            this.getStyle().set("padding-left", "var(--lumo-space-m)");
            this.getStyle().set("padding-right", "var(--lumo-space-m)");

        } catch (Exception e) {
            log.error("Error initializing Backlog", e);
            throw e;
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
//        log.info("afterNavigation start");
        // Update breadcrumbs
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
                        mainLayout.getBreadcrumbs().addItem("Backlog", Backlog.class);
                    }
                });

        // Set flag to prevent URL updates during restoration
        isRestoringFromUrl = true;
        // Load data and populate grids
        loadData();
        // Clear restoration flag after load complete
        isRestoringFromUrl = false;
//        log.info("== afterNavigation end");
//        log.info("");
    }

    /**
     * Apply filters to the displayed sprints
     */
    private void applyFilters() {
//        log.info(" * applyFilters called");
        try {
            // Clear previous content
            contentLayout.removeAll();

            // Clear previous drag-drop registrations
            dragDropHandler.clearRegistrations();

            if (allSprints.isEmpty()) {
                Div emptyMessage = new Div();
                emptyMessage.setText("No sprints found.");
                emptyMessage.getStyle()
                        .set("padding", "var(--lumo-space-l)")
                        .set("text-align", "center")
                        .set("color", "var(--lumo-secondary-text-color)");
                contentLayout.add(emptyMessage);
                return;
            }

            // Get selected sprints from filter (or all if none selected)
            List<Sprint> sprintsToShow = selectedSprints.isEmpty()
                    ? new ArrayList<>(allSprints)
                    : new ArrayList<>(selectedSprints);

            // Always ensure Backlog sprint is included (even if filtered out or not in selection)
            Sprint backlog = allSprints.stream()
                    .filter(s -> DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(s.getName()))
                    .findFirst()
                    .orElse(null);
            if (backlog != null && !sprintsToShow.contains(backlog)) {
                sprintsToShow.add(backlog);
            }

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

            // Sort sprints by start date (newest first), but Backlog always last
            sprintsToShow.sort((s1, s2) -> {
                // Backlog sprint always goes last
                boolean s1IsBacklog = DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(s1.getName());
                boolean s2IsBacklog = DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(s2.getName());

                if (s1IsBacklog && !s2IsBacklog) return 1;
                if (!s1IsBacklog && s2IsBacklog) return -1;

                // Normal sorting by start date
                if (s1.getStart() == null && s2.getStart() == null) return 0;
                if (s1.getStart() == null) return 1;
                if (s2.getStart() == null) return -1;
                return s2.getStart().compareTo(s1.getStart());
            });

            // Create a card for each sprint
            for (Sprint sprint : sprintsToShow) {
                createSprintSection(sprint);
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
     * Assign a user to a newly created task.
     * Looks at the last task in the backlog with an assigned user.
     * Falls back to the currently logged-in user.
     */
    private void assignUserToNewTask(Task newTask) {
        Long assignedUserId = null;

        Sprint sprint = getBacklogSprint();
        if (sprint != null && !sprint.getTasks().isEmpty()) {
            // Get tasks sorted by orderId (descending to get most recent first)
            List<Task> sortedTasks = sprint.getTasks().stream()
                    .sorted(Comparator.comparingInt(Task::getOrderId).reversed())
                    .toList();

            // Find the first task with an assigned user
            for (Task task : sortedTasks) {
                if (task.getResourceId() != null) {
                    assignedUserId = task.getResourceId();
                    break;
                }
            }
        }

        // Fall back to logged-in user
        if (assignedUserId == null && loggedInUser != null) {
            assignedUserId = loggedInUser.getId();
        }

        if (assignedUserId != null) {
            newTask.setResourceId(assignedUserId);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
//        log.info("");
//        log.info("== beforeEnter start");
        // Read query parameters directly from the event
        Location        location        = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();

        log.info("URL: {}", location.getPath());
        log.info("Query params from event: {}", queryParameters.getParameters());

        // Check if any parameters exist (to distinguish first visit from cleared filters)
        hasUrlParameters = !queryParameters.getParameters().isEmpty();
        log.info("hasUrlParameters: {}", hasUrlParameters);

        // Extract filter values from query parameters
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
        if (queryParameters.getParameters().containsKey("search")) {
            searchText = queryParameters.getParameters().get("search").get(0);
            log.info("Restored search from URL: {}", searchText);
        } else {
            searchText = "";
            log.info("No search in URL, set to empty");
        }
//        log.info("== beforeEnter end");
    }

    /**
     * Creates the header layout with search, filters, and clear button
     */
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.END);
        header.getStyle().set("padding", "var(--lumo-space-m)");
        header.setSpacing(true);

        // 1. Search input box with magnifying glass icon and label for alignment
        TextField searchField = new TextField();
        searchField.setLabel("Search");
        searchField.setPlaceholder("search board");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> {
            // Only update if change is from user interaction, not programmatic
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
            log.info("=== User selector value changed ===");
            log.info("isFromClient: {}", e.isFromClient());
            log.info("Old value IDs: {}", e.getOldValue().stream().map(User::getId).toList());
            log.info("New value IDs: {}", e.getValue().stream().map(User::getId).toList());
            log.info("selectedUsers IDs before: {}", selectedUsers.stream().map(User::getId).toList());

            // Only update if change is from user interaction, not programmatic
            if (e.isFromClient()) {
                selectedUsers = new java.util.HashSet<>(e.getValue());
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
            // Only update if change is from user interaction, not programmatic
            if (e.isFromClient()) {
                selectedSprints = new java.util.HashSet<>(e.getValue());
                updateUrlParameters();
                applyFilters();
            }
        });

        // 4. Clear filter button (right after filters, no spacer)
        Button clearButton = new Button("Clear filter", VaadinIcon.CLOSE_SMALL.create());
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearButton.addClickListener(e -> {
            searchField.clear();
            userSelector.clear();
            sprintSelector.clear();
            selectedSprints.clear();
            selectedUsers.clear();
            searchText = "";
            updateUrlParameters();
            applyFilters();
        });

        // Spacer to push create buttons to the right
        Div spacer = new Div();

        // Create Milestone button
        Button createMilestoneButton = new Button("Create Milestone", VaadinIcon.FLAG.create());
        createMilestoneButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createMilestoneButton.addClickListener(e -> createMilestone());

        // Create Story button
        Button createStoryButton = new Button("Create Story", VaadinIcon.BOOK.create());
        createStoryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createStoryButton.addClickListener(e -> createStory());

        // Create Task button
        Button createTaskButton = new Button("Create Task", VaadinIcon.TASKS.create());
        createTaskButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createTaskButton.addClickListener(e -> createTask());

        // Debug: Print DB button
        Button printDbButton = new Button("Print DB", VaadinIcon.DATABASE.create());
        printDbButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        printDbButton.addClickListener(e -> printDatabaseTables());

        header.add(searchField, userSelector, sprintSelector, clearButton, spacer,
                createMilestoneButton, createStoryButton, createTaskButton, printDbButton);
        header.setFlexGrow(1, spacer);

        return header;
    }

    /**
     * Create a new Milestone task in the Backlog sprint.
     */
    private void createMilestone() {
        Sprint sprint = getBacklogSprint();
        if (sprint == null) {
            Notification.show("Backlog sprint not found", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Get the next orderId before creating the task
        int nextOrderId = sprint.getNextOrderId();

        Task task = new Task();
        task.setName("New Milestone-" + nextOrderId);
        task.setSprintId(sprint.getId());
        task.setOrderId(nextOrderId);
        task.setMilestone(true);
        task.setStart(ParameterOptions.getLocalNow().withHour(8).withMinute(0).withSecond(0).withNano(0));
        // Milestones typically don't have a user assigned

        // Add task to sprint's task list so getNextOrderId() works correctly for next task
        sprint.addTask(task);

        taskApi.persist(task);
        backlogSprint = null; // Clear cache to reload
        loadData();
    }

    /**
     * Create a section for a single sprint with its story cards
     */
    private void createSprintSection(Sprint sprint) {
        // Load tasks and worklogs for this sprint
        List<Task>    tasks    = taskApi.getAll(sprint.getId());
        List<Worklog> worklogs = worklogApi.getAll(sprint.getId());

        // Initialize sprint with all transient fields
        sprint.initialize();
        sprint.initUserMap(users);
        sprint.initTaskMap(tasks, worklogs);
        sprint.recalculate(ParameterOptions.getLocalNow());

        // Register sprint's tasks with drag-drop handler
        dragDropHandler.registerSprint(sprint, tasks);

        // Filter only story tasks (tasks without parent)
        List<Task> stories = new ArrayList<>();
        for (Task task : tasks) {
            if (task.isStory()) {
                stories.add(task);
            }
        }

        // Apply search filter to stories and their child tasks
        if (!searchText.isEmpty() || !selectedUsers.isEmpty()) {
            List<Task> filteredStories = new ArrayList<>();
            for (Task story : stories) {
                // Check if story matches search
                boolean storyMatches = searchText.isEmpty() ||
                        (story.getName() != null && story.getName().toLowerCase().contains(searchText)) ||
                        (story.getId() != null && ("STORY-" + story.getId()).toLowerCase().contains(searchText));

                // Check if any child task matches search or user filter
                boolean hasMatchingChild = false;
                for (Task task : tasks) {
                    if (task.getParentTaskId() != null && task.getParentTaskId().equals(story.getId())) {
                        boolean taskMatchesSearch = searchText.isEmpty() ||
                                (task.getName() != null && task.getName().toLowerCase().contains(searchText)) ||
                                (task.getId() != null && ("T-" + task.getId()).toLowerCase().contains(searchText));

                        boolean taskMatchesUser = selectedUsers.isEmpty() ||
                                (task.getResourceId() != null && selectedUsers.stream()
                                        .anyMatch(user -> user.getId().equals(task.getResourceId())));

                        if (taskMatchesSearch && taskMatchesUser) {
                            hasMatchingChild = true;
                            break;
                        }
                    }
                }

                if (storyMatches || hasMatchingChild) {
                    filteredStories.add(story);
                }
            }
            stories = filteredStories;
        }

        // Only create sprint card if there are stories to show
        // Exception: Backlog sprint should always be visible even when empty
        boolean isBacklogSprint = DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(sprint.getName());
        if (!stories.isEmpty() || isBacklogSprint) {
            // Create SprintCard for this sprint with drag-drop handler
            SprintCard sprintCard = new SprintCard(sprint, stories, tasks, userMap, searchText, selectedUsers, dragDropHandler);

            // Add sprint card to main content
            contentLayout.add(sprintCard);
        }
    }

    /**
     * Create a new Story task in the Backlog sprint.
     */
    private void createStory() {
        Sprint sprint = getBacklogSprint();
        if (sprint == null) {
            Notification.show("Backlog sprint not found", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Get the next orderId before creating the task
        int nextOrderId = sprint.getNextOrderId();

        Task task = new Task();
        task.setName("New Story-" + nextOrderId);
        task.setSprintId(sprint.getId());
        task.setOrderId(nextOrderId);
        // Stories are parent tasks, typically don't have a user directly assigned

        // Add task to sprint's task list so getNextOrderId() works correctly for next task
        sprint.addTask(task);

        taskApi.persist(task);
        backlogSprint = null; // Clear cache to reload
        loadData();
    }

    /**
     * Create a new Task in the Backlog sprint.
     */
    private void createTask() {
        Sprint sprint = getBacklogSprint();
        if (sprint == null) {
            Notification.show("Backlog sprint not found", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Get the next orderId before creating the task
        int nextOrderId = sprint.getNextOrderId();

        Task task = new Task();
        task.setName("New Task-" + nextOrderId);
        task.setSprintId(sprint.getId());
        task.setOrderId(nextOrderId);
        Duration work = Duration.ofHours(7).plus(Duration.ofMinutes(30));
        task.setMinEstimate(work);
        task.setRemainingEstimate(work);

        // Assign user based on last task or logged-in user
        assignUserToNewTask(task);

        // Add task to sprint's task list so getNextOrderId() works correctly for next task
        sprint.addTask(task);

        taskApi.persist(task);
        backlogSprint = null; // Clear cache to reload
        loadData();
    }

    /**
     * Get the cached backlog sprint, loading it if necessary.
     */
    private Sprint getBacklogSprint() {
        if (backlogSprint == null) {
            try {
                backlogSprint = sprintApi.getBacklogSprint();
                // Initialize sprint if needed for task creation
                if (backlogSprint != null) {
                    backlogSprint.initialize();
                    List<Task>    tasks    = taskApi.getAll(backlogSprint.getId());
                    List<Worklog> worklogs = worklogApi.getAll(backlogSprint.getId());
                    backlogSprint.initUserMap(users);
                    backlogSprint.initTaskMap(tasks, worklogs);
                }
            } catch (Exception e) {
                log.error("Failed to load backlog sprint", e);
            }
        }
        return backlogSprint;
    }

    /**
     * Get the currently logged-in user's email.
     */
    private String getUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String         userEmail      = authentication != null ? authentication.getName() : "Guest";

        // If using OIDC, try to get the email address from authentication details
        if (authentication != null &&
                authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null && !email.isEmpty()) {
                userEmail = email;
            }
        }
        return userEmail;
    }

    /**
     * Load all sprints and their tasks, then create a card for each sprint
     */
    private void loadData() {
        try {
            log.info(" * loadData called");

            // Load all users and build userMap
            users = userApi.getAll();
            userMap.clear();
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
            log.info("Loaded {} users", users.size());

            // Load all sprints
            allSprints = new ArrayList<>(sprintApi.getAll());

            // Ensure Backlog sprint is always included (even if not returned by ACL-filtered API)
            boolean hasBacklog = allSprints.stream()
                    .anyMatch(s -> DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(s.getName()));
            if (!hasBacklog) {
                try {
                    Sprint backlogSprint = sprintApi.getBacklogSprint();
                    if (backlogSprint != null) {
                        allSprints.add(backlogSprint);
                        log.info("Added Backlog sprint to allSprints (was not in ACL-filtered list)");
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch Backlog sprint: {}", e.getMessage());
                }
            }

            log.info("Loaded {} sprints (including Backlog)", allSprints.size());

            populateFilters();

            // Apply filters (will show sprints if filters were restored or default selected)
            if (!allSprints.isEmpty()) {
                applyFilters();
            }

        } catch (Exception e) {
            log.error("Error loading sprint data", e);
            contentLayout.removeAll();
            Div errorMessage = new Div();
            errorMessage.setText("Error loading sprints: " + e.getMessage());
            errorMessage.getStyle().set("padding", "var(--lumo-space-l)").set("color", "var(--lumo-error-text-color)");
            contentLayout.add(errorMessage);
        }
    }

    private void populateFilters() {
        // Populate user selector
        if (userSelector != null) {
            userSelector.setItems(users);

            // Restore selected users from URL parameters
            if (savedUserIds != null && !savedUserIds.isEmpty()) {
                log.info("Restoring users from savedUserIds: {}", savedUserIds);
                Set<User> usersToSelect = new java.util.HashSet<>();
                for (String idStr : savedUserIds.split(",")) {
                    try {
                        Long id = Long.parseLong(idStr.trim());
                        users.stream()
                                .filter(u -> u.getId().equals(id))
                                .findFirst()
                                .ifPresent(user -> {
                                    usersToSelect.add(user);
                                    log.info("Added user ID {} to selection", user.getId());
                                });
                    } catch (NumberFormatException e) {
                        log.warn("Invalid user ID in URL: {}", idStr);
                    }
                }
                if (!usersToSelect.isEmpty()) {
                    selectedUsers = usersToSelect;
                    log.info("Setting userSelector value to IDs: {}", usersToSelect.stream().map(User::getId).toList());
                    userSelector.setValue(usersToSelect);
                    log.info("userSelector.setValue() completed");
                } else {
                    log.info("No valid users to restore");
                }
            } else {
                log.info("savedUserIds is null or empty - not restoring users");
            }
        }

        // Populate sprint selector (excluding Backlog since it's always visible)
        if (sprintSelector != null) {
            List<Sprint> selectableSprints = allSprints.stream()
                    .filter(s -> !DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(s.getName()))
                    .toList();
            sprintSelector.setItems(selectableSprints);

            // Restore selected sprints from URL parameters or select first by default
            if (savedSprintIds != null && !savedSprintIds.isEmpty()) {
                Set<Sprint> sprintsToSelect = new java.util.HashSet<>();
                for (String idStr : savedSprintIds.split(",")) {
                    try {
                        Long id = Long.parseLong(idStr.trim());
                        selectableSprints.stream()
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
            } else if (!hasUrlParameters && !selectableSprints.isEmpty()) {
                // Only select first sprint by default if NO URL parameters at all (first visit)
                // If hasUrlParameters is true but no sprints, user explicitly cleared all sprints
                selectedSprints = Set.of(selectableSprints.get(0));
                sprintSelector.setValue(selectedSprints);
                // Explicitly update URL with default selection
                isRestoringFromUrl = false;
                updateUrlParameters();
                isRestoringFromUrl = true;
                log.info("savedSprintIds is null or empty - not restoring sprints");
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
    }

    /**
     * Debug method to print all database tables to db.txt file.
     */
    private void printDatabaseTables() {
        log.info("Print DB button clicked - writing database tables to db.txt");
        try {
            java.nio.file.Path outputPath = databaseDebugService.printDatabaseTables();
            Notification.show("Database tables written to " + outputPath, 3000, Notification.Position.BOTTOM_END);
        } catch (Exception e) {
            log.error("Error printing database tables", e);
            Notification.show("Error printing database: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    /**
     * Update URL parameters to persist filter state
     */
    private void updateUrlParameters() {
        log.info("=== updateUrlParameters called ===");
        log.info("isRestoringFromUrl: {}", isRestoringFromUrl);

        // Don't update URL if we're currently restoring from URL (prevents infinite loop)
        if (isRestoringFromUrl) {
            log.info("Skipping - still restoring from URL");
            return;
        }

        java.util.Map<String, String> params = new java.util.HashMap<>();

        if (!searchText.isEmpty()) {
            params.put("search", searchText);
            log.info("Added search: {}", searchText);
        }

        if (!selectedUsers.isEmpty()) {
            String userIds = selectedUsers.stream()
                    .map(u -> String.valueOf(u.getId()))
                    .collect(java.util.stream.Collectors.joining(","));
            params.put("users", userIds);
            log.info("Added users: {}", userIds);
        } else {
            log.info("No users selected - NOT adding to URL");
        }

        if (!selectedSprints.isEmpty()) {
            String sprintIds = selectedSprints.stream()
                    .map(s -> String.valueOf(s.getId()))
                    .collect(java.util.stream.Collectors.joining(","));
            params.put("sprints", sprintIds);
            log.info("Added sprints: {}", sprintIds);
        } else {
            log.info("No sprints selected - NOT adding to URL");
        }

        // Always add a marker parameter so we can distinguish first visit from cleared filters
        if (params.isEmpty()) {
            params.put("_", ""); // Empty marker parameter
            log.info("Added marker parameter (all filters empty)");
        }

        log.info("Final params map: {}", params);
        log.info("Navigating to URL with params...");

        // Update URL with query parameters (like SprintListView does)
        QueryParameters queryParameters = QueryParameters.simple(params);
        getUI().ifPresent(ui -> {
            log.info("UI present, calling navigate with class and queryParameters");
            log.info("QueryParameters toString: {}", queryParameters);
            ui.navigate(Backlog.class, queryParameters);
            log.info("Navigate called");
        });
        if (getUI().isEmpty()) {
            log.warn("UI not present - cannot navigate!");
        }

        log.info("=== updateUrlParameters complete ===");
    }
}

