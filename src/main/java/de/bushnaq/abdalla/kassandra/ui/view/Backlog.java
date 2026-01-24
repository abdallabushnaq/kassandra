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

import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.CrossGridDragDropCoordinator;
import de.bushnaq.abdalla.kassandra.ui.component.TaskGrid;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Route(value = "backlog", layout = MainLayout.class)
@PageTitle("Backlog")
@Menu(order = 3, icon = "vaadin:grid-v", title = "Backlog")
@PermitAll // When security is enabled, allow all authenticated users
@RolesAllowed({"USER", "ADMIN"}) // Allow access to users with specific roles
@Log4j2
public class Backlog extends Main implements AfterNavigationObserver, BeforeEnterObserver {
    public static final String                       BACKLOG_PAGE_TITLE_ID      = "backlog-page-title";
    public static final String                       CANCEL_BUTTON_ID           = "cancel-tasks-button";
    public static final String                       CLEAR_FILTER_BUTTON_ID     = "clear-filter-button";
    public static final String                       CREATE_MILESTONE_BUTTON_ID = "create-milestone-button";
    public static final String                       CREATE_STORY_BUTTON_ID     = "create-story-button";
    public static final String                       CREATE_TASK_BUTTON_ID      = "create-task-button";
    public static final String                       EDIT_BUTTON_ID             = "edit-tasks-button";
    public static final String                       ROUTE                      = "backlog";
    public static final String                       SAVE_BUTTON_ID             = "save-tasks-button";
    public static final String                       SEARCH_FIELD_ID            = "search-field";
    public static final String                       SPRINT_SELECTOR_ID         = "sprint-selector";
    public static final String                       USER_SELECTOR_ID           = "user-selector";
    private             List<Sprint>                 allSprints                 = new ArrayList<>();
    private final       TaskGrid                     backlogGrid;                        // Grid for Backlog sprint (always at bottom)
    private final       VerticalLayout               backlogGridPanel;                   // Panel containing backlog grid
    private             Sprint                       backlogSprint              = null;  // Cached Backlog sprint (always shown at bottom)
    private             Button                       cancelButton;
    private final       Clock                        clock;
    @Autowired
    protected           Context                      context;
    private final       CrossGridDragDropCoordinator dragDropCoordinator;              // Coordinator for cross-grid drag & drop
    private             Button                       editButton;
    private final       GanttErrorHandler            eh                         = new GanttErrorHandler();
    private final       FeatureApi                   featureApi;
    private             Long                         featureId;
    //    private final       Svg                          ganttChart                 = new Svg();
    private final       Div                          ganttChartContainer;
    private             CompletableFuture<Void>      ganttGenerationFuture;
    private             GanttUtil                    ganttUtil;
    private final       TaskGrid                     grid;
    private final       HorizontalLayout             headerLayout;
    private final       JsonMapper                   jsonMapper;
    private static      Long                         lastShownSprintId          = null;  // Static to persist across navigation
    private             User                         loggedInUser               = null;
    private final       ProductApi                   productApi;
    private             Long                         productId;
    private             Button                       saveButton;
    private             TextField                    searchField;
    private             String                       searchText                 = "";
    private             Sprint                       selectedSprint             = null;  // The sprint selected in dropdown (not Backlog)
    private             java.util.Set<User>          selectedUsers              = new java.util.HashSet<>();
    private             Sprint                       sprint;                             // Current sprint being displayed in grid
    private final       SprintApi                    sprintApi;
    private             Long                         sprintId;
    private             ComboBox<Sprint>             sprintSelector;
    private final       TaskApi                      taskApi;
    private final       UserApi                      userApi;
    private             MultiSelectComboBox<User>    userSelector;
    private             List<User>                   users                      = new ArrayList<>();
    private final       VersionApi                   versionApi;
    private             Long                         versionId;
    private final       WorklogApi                   worklogApi;

    public Backlog(WorklogApi worklogApi, TaskApi taskApi, SprintApi sprintApi, ProductApi productApi, VersionApi versionApi, FeatureApi featureApi, UserApi userApi, Clock clock, JsonMapper jsonMapper) {
        this.worklogApi = worklogApi;
        this.taskApi    = taskApi;
        this.sprintApi  = sprintApi;
        this.productApi = productApi;
        this.versionApi = versionApi;
        this.featureApi = featureApi;
        this.userApi    = userApi;
        this.clock      = clock;
        this.jsonMapper = jsonMapper;

        try {
            // Set width full but not height - let content determine height for scrolling
            setWidthFull();
            // Make view background transparent, so AppLayout's gray background is visible
            getStyle().set("background-color", "transparent");

            // Apply tree-grid-wrapper styling to the main view
            setClassName("tree-grid-wrapper");
            addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
            this.getStyle().set("padding-left", "var(--lumo-space-m)");
            this.getStyle().set("padding-right", "var(--lumo-space-m)");

            headerLayout = createHeaderWithButtons();

            // Create main sprint grid
            grid = createGrid(clock);

            // Create panel wrapper for sprint grid
            VerticalLayout gridPanelWrapper = new VerticalLayout();
            gridPanelWrapper.setPadding(false);
            gridPanelWrapper.setSpacing(false);
            gridPanelWrapper.setWidthFull();
            gridPanelWrapper.addClassName("tree-grid-panel-wrapper");

            VerticalLayout innerWrapper = new VerticalLayout();
            innerWrapper.setPadding(false);
            innerWrapper.setSpacing(false);
            gridPanelWrapper.add(innerWrapper);

            VerticalLayout gridPanel = new VerticalLayout(grid);
            gridPanel.setPadding(false);
            gridPanel.setSpacing(false);
            gridPanel.setWidthFull();
            gridPanel.addClassName("tree-grid-panel");

            innerWrapper.add(gridPanel);

            // Create Gantt chart container (placed between sprint grid and backlog)
            ganttChartContainer = new Div();
            ganttChartContainer.getStyle()
                    .set("overflow-x", "auto")
                    .set("width", "100%")
                    .set("margin-top", "var(--lumo-space-m)");

            // Create backlog grid (always shown at bottom)
            backlogGrid = createGrid(clock);

            // Create panel wrapper for backlog grid with a header
            backlogGridPanel = new VerticalLayout();
            backlogGridPanel.setPadding(false);
            backlogGridPanel.setSpacing(false);
            backlogGridPanel.setWidthFull();
            backlogGridPanel.addClassName("tree-grid-panel-wrapper");
            backlogGridPanel.getStyle().set("margin-top", "var(--lumo-space-l)");

            // Add a header/separator for the backlog section
            Div backlogHeader = new Div();
            backlogHeader.setText("Backlog");
            backlogHeader.getStyle()
                    .set("font-size", "var(--lumo-font-size-l)")
                    .set("font-weight", "600")
                    .set("padding", "var(--lumo-space-m)")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-m) var(--lumo-border-radius-m) 0 0");

            VerticalLayout backlogInnerWrapper = new VerticalLayout();
            backlogInnerWrapper.setPadding(false);
            backlogInnerWrapper.setSpacing(false);

            VerticalLayout backlogGridPanelInner = new VerticalLayout(backlogGrid);
            backlogGridPanelInner.setPadding(false);
            backlogGridPanelInner.setSpacing(false);
            backlogGridPanelInner.setWidthFull();
            backlogGridPanelInner.addClassName("tree-grid-panel");

            backlogInnerWrapper.add(backlogGridPanelInner);
            backlogGridPanel.add(backlogHeader, backlogInnerWrapper);

            // Setup cross-grid drag & drop coordinator
            dragDropCoordinator = new CrossGridDragDropCoordinator(this::handleCrossGridTransfer);
            dragDropCoordinator.register(grid);
            dragDropCoordinator.register(backlogGrid);
            grid.setDragDropCoordinator(dragDropCoordinator);
            backlogGrid.setDragDropCoordinator(dragDropCoordinator);

            // Add components in order: header, sprint grid, gantt chart, backlog grid
            add(headerLayout, ganttChartContainer, gridPanelWrapper, backlogGridPanel);

            String userEmail = getUserEmail();
            try {
                loggedInUser = userApi.getByEmail(userEmail);
            } catch (ResponseStatusException e) {
                log.warn("Could not find user with email: " + userEmail, e);
            }

        } catch (Exception e) {
            log.error("Error initializing Backlog view", e);
            throw e;
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        //- Get query parameters
        com.vaadin.flow.router.Location location        = event.getLocation();
        QueryParameters                 queryParameters = location.getQueryParameters();
        if (queryParameters.getParameters().containsKey("product")) {
            this.productId = Long.parseLong(queryParameters.getParameters().get("product").getFirst());
        }
        if (queryParameters.getParameters().containsKey("version")) {
            this.versionId = Long.parseLong(queryParameters.getParameters().get("version").getFirst());
        }
        if (queryParameters.getParameters().containsKey("feature")) {
            this.featureId = Long.parseLong(queryParameters.getParameters().get("feature").getFirst());
        }
        if (queryParameters.getParameters().containsKey("sprint")) {
            this.sprintId = Long.parseLong(queryParameters.getParameters().get("sprint").getFirst());
        }

        // If no sprint ID provided, use last shown sprint or find first non-Backlog sprint
        if (this.sprintId == null) {
            if (lastShownSprintId != null) {
                // Use last shown sprint
                this.sprintId = lastShownSprintId;
                log.info("No sprint ID provided, using last shown sprint (ID: {})", this.sprintId);
            } else {
                // Find first non-Backlog sprint
                try {
                    List<Sprint> sprints = sprintApi.getAll();
                    Sprint firstNonBacklog = sprints.stream()
                            .filter(s -> !"Backlog".equals(s.getName()))
                            .findFirst()
                            .orElse(null);

                    if (firstNonBacklog != null) {
                        this.sprintId = firstNonBacklog.getId();
                        log.info("No sprint ID provided, using first non-Backlog sprint: {} (ID: {})",
                                firstNonBacklog.getName(), this.sprintId);
                    } else {
                        // No sprints available at all
                        log.warn("No sprints found");
                        return;
                    }
                } catch (Exception e) {
                    log.error("Error fetching sprints", e);
                    return;
                }
            }
        }

        // Remember this sprint for next time
        lastShownSprintId = this.sprintId;

        ganttUtil = new GanttUtil(context);
        loadData();

        // Update sprint selector to show the current sprint (in case it was determined automatically)
        updateSprintSelectorValue();

        //- Update breadcrumbs
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();

                        // Only show full breadcrumb trail if we have product/version/feature context
                        if (productId != null && versionId != null && featureId != null) {
                            Product product = productApi.getById(productId);
                            mainLayout.getBreadcrumbs().addItem("Products (" + product.getName() + ")", ProductListView.class);
                            {
                                Map<String, String> params = new HashMap<>();
                                params.put("product", String.valueOf(productId));
                                Version version = versionApi.getById(versionId);
                                mainLayout.getBreadcrumbs().addItem("Versions (" + version.getName() + ")", VersionListView.class, params);
                            }
                            {
                                Map<String, String> params = new HashMap<>();
                                params.put("product", String.valueOf(productId));
                                params.put("version", String.valueOf(versionId));
                                Feature feature = featureApi.getById(featureId);
                                mainLayout.getBreadcrumbs().addItem("Features (" + feature.getName() + ")", FeatureListView.class, params);
                            }
                            {
                                Map<String, String> params = new HashMap<>();
                                params.put("product", String.valueOf(productId));
                                params.put("version", String.valueOf(versionId));
                                params.put("feature", String.valueOf(featureId));
                                mainLayout.getBreadcrumbs().addItem("Sprints (" + sprint.getName() + ")", SprintListView.class, params);
                            }
                            {
                                Map<String, String> params = new HashMap<>();
                                params.put("product", String.valueOf(productId));
                                params.put("version", String.valueOf(versionId));
                                params.put("feature", String.valueOf(featureId));
                                params.put("sprint", String.valueOf(sprintId));
                                mainLayout.getBreadcrumbs().addItem("Backlog", Backlog.class, params);
                            }
                        } else {
                            // Simple breadcrumb when accessed from main menu
                            mainLayout.getBreadcrumbs().addItem("Backlog", Backlog.class);
                        }
                    }
                });

        //- populate grid with selected sprint + Backlog sprint
        refreshGrid();
    }

    /**
     * Apply search and user filters to both grids.
     * Filters the displayed tasks based on search text and selected users.
     */
    private void applyFilters() {
        // Apply filters to sprint grid
        if (sprint != null) {
            List<Task> sprintTasks = filterTasks(sprint.getTasks());
            grid.updateData(sprint, sprintTasks, users);
        }

        // Apply filters to backlog grid
        if (backlogSprint != null) {
            List<Task> backlogTasks = filterTasks(backlogSprint.getTasks());
            backlogGrid.updateData(backlogSprint, backlogTasks, users);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Read query parameters
        com.vaadin.flow.router.Location location        = event.getLocation();
        QueryParameters                 queryParameters = location.getQueryParameters();

        // Extract sprint ID from URL if present
        if (queryParameters.getParameters().containsKey("sprint")) {
            this.sprintId = Long.parseLong(queryParameters.getParameters().get("sprint").getFirst());
        }

        // Populate the sprint selector after we have the sprint ID
        populateSprintSelector();
    }

    /**
     * Cancel edit mode and discard all changes
     */
    private void cancelEditMode() {
        grid.getModifiedTasks().clear();

        // Reload data to discard changes
        loadData();
        refreshGrid();
        exitEditMode();
    }

    private TaskGrid createGrid(Clock clock) {
        TaskGrid grid = new TaskGrid(clock, getLocale(), jsonMapper);
        grid.setOnSaveAllChangesAndRefresh(this::saveAllChangesAndRefresh);

        grid.setWidthFull();

        return grid;
    }

    /**
     * Creates the header layout with search, filters, create buttons, Edit, Save, and Cancel buttons
     */
    private HorizontalLayout createHeaderWithButtons() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.END);
        header.getStyle().set("padding", "var(--lumo-space-m)");
        header.setSpacing(true);

        // 1. Search input box with magnifying glass icon
        searchField = new TextField();
        searchField.setId(SEARCH_FIELD_ID);
        searchField.setLabel("Search");
        searchField.setPlaceholder("search tasks");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                searchText = e.getValue() != null ? e.getValue().toLowerCase().trim() : "";
                applyFilters();
            }
        });
        searchField.setWidth("200px");

        // 2. User multi-select dropdown
        userSelector = new MultiSelectComboBox<>();
        userSelector.setId(USER_SELECTOR_ID);
        userSelector.setLabel("User");
        userSelector.setItemLabelGenerator(User::getName);
        userSelector.setPlaceholder("Select users");
        userSelector.setWidth("200px");
        userSelector.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                selectedUsers = new java.util.HashSet<>(e.getValue());
                applyFilters();
            }
        });

        // 3. Sprint single-select dropdown (changed from multi-select)
        sprintSelector = new ComboBox<>();
        sprintSelector.setId(SPRINT_SELECTOR_ID);
        sprintSelector.setLabel("Sprint");
        sprintSelector.setItemLabelGenerator(Sprint::getName);
        sprintSelector.setPlaceholder("Select sprint");
        sprintSelector.setWidth("200px");
        sprintSelector.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                selectedSprint = e.getValue();
                loadDataForSelectedSprint();
            }
        });

        // 4. Clear filter button
        Button clearButton = new Button("Clear filter", VaadinIcon.CLOSE_SMALL.create());
        clearButton.setId(CLEAR_FILTER_BUTTON_ID);
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearButton.addClickListener(e -> {
            searchField.clear();
            userSelector.clear();
            selectedUsers.clear();
            searchText = "";
            applyFilters();
        });

        // Spacer to push create buttons to the right
        Div spacer = new Div();

        // Create Milestone button
        Button createMilestoneButton = new Button("Create Milestone", VaadinIcon.FLAG.create());
        createMilestoneButton.setId(CREATE_MILESTONE_BUTTON_ID);
        createMilestoneButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createMilestoneButton.addClickListener(e -> createMilestone());

        // Create Story button
        Button createStoryButton = new Button("Create Story", VaadinIcon.BOOK.create());
        createStoryButton.setId(CREATE_STORY_BUTTON_ID);
        createStoryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createStoryButton.addClickListener(e -> createStory());

        // Create Task button
        Button createTaskButton = new Button("Create Task", VaadinIcon.TASKS.create());
        createTaskButton.setId(CREATE_TASK_BUTTON_ID);
        createTaskButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createTaskButton.addClickListener(e -> grid.createTask(loggedInUser));

        // Create Edit button
        editButton = new Button("Edit", VaadinIcon.EDIT.create());
        editButton.setId(EDIT_BUTTON_ID);
        editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editButton.addClickListener(e -> enterEditMode());

        // Create Save button
        saveButton = new Button("Save", VaadinIcon.CHECK.create());
        saveButton.setId(SAVE_BUTTON_ID);
        saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        saveButton.setVisible(false);
        saveButton.addClickListener(e -> saveAllChangesAndRefresh());

        // Create Cancel button
        cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create());
        cancelButton.setId(CANCEL_BUTTON_ID);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelButton.setVisible(false);
        cancelButton.addClickListener(e -> cancelEditMode());

        // Add all components to header
        header.add(searchField, userSelector, sprintSelector, clearButton, spacer,
                createMilestoneButton, createStoryButton, createTaskButton, editButton, saveButton, cancelButton);
        header.setFlexGrow(1, spacer);

        return header;
    }

    /**
     * Create a new Milestone task
     */
    private void createMilestone() {
        long nextOrderId = sprint.getNextOrderId();

        Task task = new Task();
        task.setName("New Milestone-" + nextOrderId);
        task.setSprint(sprint);
        task.setSprintId(sprint.getId());
        task.setMilestone(true);
        task.setStart(ParameterOptions.getLocalNow().withHour(8).withMinute(0).withSecond(0).withNano(0));

        Task saved = taskApi.persist(task);
        loadData();
        refreshGrid();
    }

    /**
     * Creates a deep copy (snapshot) of the sprint and all its tasks.
     * This is necessary for async Gantt chart generation to avoid race conditions
     * when tasks are being saved/updated while the chart is being rendered.
     *
     * @param original the sprint to copy
     * @return a deep copy of the sprint with all relationships preserved
     */
    private Sprint createSprintSnapshot(Sprint original) {
        try {
            // Use Jackson to create a deep copy through serialization/deserialization
            String sprintJson = jsonMapper.writeValueAsString(original);
            Sprint snapshot   = jsonMapper.readValue(sprintJson, Sprint.class);

            // Need to reconstruct the relationships and initialize the sprint
            // Get a copy of the tasks list
            List<Task> tasksCopy = new ArrayList<>();
            for (Task originalTask : original.getTasks()) {
                String taskJson = jsonMapper.writeValueAsString(originalTask);
                Task   taskCopy = jsonMapper.readValue(taskJson, Task.class);
                tasksCopy.add(taskCopy);
            }

            // Get a copy of worklogs
            List<Worklog> worklogsCopy = new ArrayList<>();
            for (Worklog originalWorklog : original.getWorklogs()) {
                String  worklogJson = jsonMapper.writeValueAsString(originalWorklog);
                Worklog worklogCopy = jsonMapper.readValue(worklogJson, Worklog.class);
                worklogsCopy.add(worklogCopy);
            }

            // Get a copy of users (serialize/deserialize to ensure deep copy)
            List<User> usersCopy = new ArrayList<>();
            for (User originalUser : original.getUserMap().values()) {
                String userJson = jsonMapper.writeValueAsString(originalUser);
                User   userCopy = jsonMapper.readValue(userJson, User.class);
                usersCopy.add(userCopy);
            }

            // Initialize the snapshot - this is critical!
            // First initialize the sprint itself (sets up ProjectFile and properties)
            snapshot.initialize();

            // Then initialize user map (sets up calendars)
            snapshot.initUserMap(usersCopy);

            // Then initialize task map (sets up task relationships and sprint references)
            snapshot.initTaskMap(tasksCopy, worklogsCopy);

            // Finally recalculate (updates calculated fields like release date)
            snapshot.recalculate(ParameterOptions.getLocalNow());

            log.debug("Created sprint snapshot with {} tasks, {} users, {} worklogs",
                    tasksCopy.size(), usersCopy.size(), worklogsCopy.size());
            return snapshot;
        } catch (Exception e) {
            log.error("Error creating sprint snapshot, falling back to original reference", e);
            // If snapshot creation fails, return the original (better than crashing)
            return original;
        }
    }

    /**
     * Create a new Story task
     */
    private void createStory() {
        long nextOrderId = sprint.getNextOrderId();

        Task task = new Task();
        task.setName("New Story-" + nextOrderId);
        task.setSprint(sprint);
        task.setSprintId(sprint.getId());

        Task saved = taskApi.persist(task);
        loadData();
        refreshGrid();
    }

    /**
     * Display error message for Gantt chart generation failure
     */
    private void displayGanttError(Throwable throwable) {
        ganttChartContainer.removeAll();

        // Convert stack trace to string
        StringWriter stringWriter = new StringWriter();
        PrintWriter  printWriter  = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        String stackTrace = stringWriter.toString();

        // Display error message with stack trace
        Paragraph errorParagraph = new Paragraph("Error generating Gantt chart: " + throwable.getMessage());
        errorParagraph.getStyle()
                .set("color", "var(--lumo-error-color)")
                .set("font-weight", "bold");

        Paragraph stackTraceParagraph = new Paragraph(stackTrace);
        stackTraceParagraph.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-family", "monospace")
                .set("font-size", "12px")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("max-height", "300px")
                .set("overflow-y", "auto");

        Div errorContainer = new Div(errorParagraph, stackTraceParagraph);
        errorContainer.getStyle()
                .set("padding", "var(--lumo-space-m)");

        ganttChartContainer.add(errorContainer);
    }

    /**
     * Enter edit mode - enable editing for all rows
     */
    private void enterEditMode() {
        grid.setEditMode(true);
        grid.getModifiedTasks().clear();

        // Update button visibility
        editButton.setVisible(false);
        saveButton.setVisible(true);
        cancelButton.setVisible(true);

        // Enable drag and drop for reordering
        grid.setRowsDraggable(true);

        // Add visual feedback for edit mode
        grid.addClassName("edit-mode");

        // Refresh grid to show editable components
        grid.getDataProvider().refreshAll();

    }

    /**
     * Exit edit mode
     */
    private void exitEditMode() {
        grid.setEditMode(false);

        // Update button visibility
        editButton.setVisible(true);
        saveButton.setVisible(false);
        cancelButton.setVisible(false);

        // Disable drag and drop
        grid.setRowsDraggable(true);

        // Remove visual feedback
        grid.removeClassName("edit-mode");


        // Refresh grid to show read-only components
        grid.getDataProvider().refreshAll();
    }

    /**
     * Filter a list of tasks based on current search text and selected users.
     */
    private List<Task> filterTasks(List<Task> tasks) {
        if (searchText.isEmpty() && selectedUsers.isEmpty()) {
            return new ArrayList<>(tasks);
        }

        List<Task> filteredTasks = new ArrayList<>();

        for (Task task : tasks) {
            boolean matchesSearch = searchText.isEmpty() ||
                    (task.getName() != null && task.getName().toLowerCase().contains(searchText)) ||
                    (task.getId() != null && ("T-" + task.getId()).toLowerCase().contains(searchText));

            boolean matchesUser = selectedUsers.isEmpty() ||
                    (task.getResourceId() != null && selectedUsers.stream()
                            .anyMatch(user -> user.getId().equals(task.getResourceId())));

            // For stories (parent tasks), also check if any child task matches
            if (task.isStory()) {
                boolean hasMatchingChild = false;
                for (Task childTask : tasks) {
                    if (childTask.getParentTaskId() != null && childTask.getParentTaskId().equals(task.getId())) {
                        boolean childMatchesSearch = searchText.isEmpty() ||
                                (childTask.getName() != null && childTask.getName().toLowerCase().contains(searchText)) ||
                                (childTask.getId() != null && ("T-" + childTask.getId()).toLowerCase().contains(searchText));

                        boolean childMatchesUser = selectedUsers.isEmpty() ||
                                (childTask.getResourceId() != null && selectedUsers.stream()
                                        .anyMatch(user -> user.getId().equals(childTask.getResourceId())));

                        if (childMatchesSearch && childMatchesUser) {
                            hasMatchingChild = true;
                            break;
                        }
                    }
                }

                // Story is included if:
                // 1. It has a matching child (child matches both search AND user filter), OR
                // 2. The story itself matches both search AND user filter
                if (hasMatchingChild || (matchesSearch && matchesUser)) {
                    filteredTasks.add(task);
                }
            } else {
                // For regular tasks, just check the task itself
                if (matchesSearch && matchesUser) {
                    filteredTasks.add(task);
                }
            }
        }

        return filteredTasks;
    }

    /**
     * Find the story that should be the parent for a task at the given position.
     * Looks backwards from the position to find the enclosing story.
     *
     * @param taskOrder The ordered list of tasks
     * @param position  The position to check
     * @return The parent story, or null if no parent found
     */
    private Task findParentStoryForPosition(List<Task> taskOrder, int position) {
        // Look backwards from position to find the enclosing story
        for (int i = position - 1; i >= 0; i--) {
            Task candidate = taskOrder.get(i);
            if (candidate.isStory()) {
                return candidate;
            }
        }
        return null;
    }

    private void generateGanttChart() {
        // Cancel any previous generation in progress
        if (ganttGenerationFuture != null && !ganttGenerationFuture.isDone()) {
            ganttGenerationFuture.cancel(true);
            log.debug("Cancelled previous Gantt chart generation");
        }


        // Clear container and show loading indicator
        ganttChartContainer.removeAll();
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("300px");

        Span loadingText = new Span("Generating Gantt chart...");
        loadingText.getStyle()
                .set("margin-right", "var(--lumo-space-m)")
                .set("font-style", "italic")
                .set("color", "var(--lumo-secondary-text-color)");

        Div loadingContainer = new Div(loadingText, progressBar);
        loadingContainer.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("padding", "var(--lumo-space-m)");

        ganttChartContainer.add(loadingContainer);

        // Capture UI and security context
        UI             ui             = UI.getCurrent();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Sprint         sprintSnapshot = createSprintSnapshot(this.sprint); // Create deep copy of sprint

        // Generate chart asynchronously
        long startTime = System.currentTimeMillis();
        ganttGenerationFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in background thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                log.debug("Starting async Gantt chart generation");
                Svg svg = new Svg();
                RenderUtil.generateGanttChartSvg(Backlog.this.context, sprintSnapshot, svg);
                return svg;
            } catch (Exception e) {
                // Wrap checked exception in runtime exception for CompletableFuture
                throw new RuntimeException("Error generating Gantt chart", e);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }).thenAccept(svg -> {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Gantt chart generated in {} ms", elapsed);

            // Update UI on UI thread
            ui.access(() -> {
                try {
                    ganttChartContainer.removeAll();

                    // Configure Gantt chart for proper scrolling display
                    svg.getStyle()
                            .set("margin-top", "var(--lumo-space-m)")
                            .set("max-width", "100%")
                            .set("height", "auto")
                            .set("display", "block");

                    ganttChartContainer.add(svg);
                    ui.push(); // Push changes to client
                    log.trace("Gantt chart added to UI and pushed to client");
                } catch (Exception e) {
                    log.error("Error adding Gantt chart to UI", e);
                    displayGanttError(e);
                    ui.push(); // Push error to client
                }
            });
        }).exceptionally(ex -> {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Error generating Gantt chart after {} ms: {}", elapsed, ex.getMessage(), ex);

            // Show error in UI
            ui.access(() -> {
                displayGanttError(ex);
                ui.push(); // Push error to client
            });
            return null;
        });
    }

    /**
     * Get the currently logged-in user's name or email.
     * Copied from MainLayout for consistency.
     */
    private String getUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String         userEmail      = authentication != null ? authentication.getName() : "Guest";

        // If using OIDC, try to get the email address from authentication details
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null && !email.isEmpty()) {
                userEmail = email;
            }
        }
        return userEmail;
    }

    /**
     * Handle task transfer between Sprint grid and Backlog grid.
     * This is called when a task is dragged from one grid and dropped into another.
     *
     * @param event The cross-grid transfer event containing all necessary information
     */
    private void handleCrossGridTransfer(CrossGridDragDropCoordinator.CrossGridTransferEvent event) {
        Task             task         = event.task();
        List<Task>       childTasks   = event.childTasks();
        TaskGrid         sourceGrid   = event.sourceGrid();
        TaskGrid         targetGrid   = event.targetGrid();
        Task             dropTarget   = event.dropTargetTask();
        GridDropLocation dropLocation = event.dropLocation();

        Sprint sourceSprint = sourceGrid.getSprint();
        Sprint targetSprint = targetGrid.getSprint();

        if (sourceSprint == null || targetSprint == null) {
            log.warn("Cannot transfer task - source or target sprint is null");
            return;
        }

        log.info("Cross-grid transfer: {} from sprint '{}' to sprint '{}'",
                task.getKey(), sourceSprint.getName(), targetSprint.getName());

        // Collect all modified tasks for batch saving
        java.util.Set<Task> modifiedTasks = new java.util.HashSet<>();

        // 1. Handle parent-child relationships - remove from current parent
        Task oldParent = task.getParentTask();
        if (oldParent != null) {
            oldParent.removeChildTask(task);
            modifiedTasks.add(oldParent);
            log.debug("Removed task {} from parent {}", task.getKey(), oldParent.getKey());
        }

        // 2. Remove from source sprint's task list
        sourceSprint.getTasks().remove(task);
        sourceGrid.getTaskOrder().remove(task);
        log.debug("Removed task {} from source sprint", task.getKey());

        // 3. If moving a story, also remove all children from source
        if (task.isStory() && !childTasks.isEmpty()) {
            for (Task child : childTasks) {
                sourceSprint.getTasks().remove(child);
                sourceGrid.getTaskOrder().remove(child);
                log.debug("Removed child task {} from source sprint", child.getKey());
            }
        }

        // 4. Remove broken relations (predecessors that now point across sprints)
        List<Task> allMovedTasks = new ArrayList<>();
        allMovedTasks.add(task);
        if (task.isStory() && !childTasks.isEmpty()) {
            allMovedTasks.addAll(childTasks);
        }
        removeBrokenRelations(allMovedTasks, sourceSprint, targetSprint, modifiedTasks);

        // 5. Update task's sprint reference
        task.setSprintId(targetSprint.getId());
        task.setSprint(targetSprint);

        // 6. Add to target sprint
        targetSprint.addTask(task);

        // 7. Determine insertion position in target grid
        List<Task> targetTaskOrder = targetGrid.getTaskOrder();
        int        insertIndex;

        if (dropTarget != null) {
            int dropTargetIndex = targetTaskOrder.indexOf(dropTarget);
            if (dropLocation == GridDropLocation.BELOW) {
                // If dropping below a story, insert after its last child
                if (dropTarget.isStory() && !dropTarget.getChildTasks().isEmpty()) {
                    Task lastChild = dropTarget.getChildTasks().getLast();
                    insertIndex = targetTaskOrder.indexOf(lastChild) + 1;
                } else {
                    insertIndex = dropTargetIndex + 1;
                }
            } else {
                insertIndex = dropTargetIndex;
            }
        } else {
            // Drop at end
            insertIndex = targetTaskOrder.size();
        }

        // 8. Insert into target task order
        if (insertIndex >= targetTaskOrder.size()) {
            targetTaskOrder.add(task);
        } else {
            targetTaskOrder.add(insertIndex, task);
        }
        insertIndex++; // Move past the task we just inserted for children

        // 9. Determine new parent for tasks (if the task needs parenting)
        if (task.isTask()) {
            Task newParent = findParentStoryForPosition(targetTaskOrder, targetTaskOrder.indexOf(task));
            if (newParent != null) {
                newParent.addChildTask(task);
                modifiedTasks.add(newParent);
                log.debug("Added task {} to new parent {}", task.getKey(), newParent.getKey());
            }
        }

        // 10. If moving a story, also move all children
        if (task.isStory() && !childTasks.isEmpty()) {
            for (Task child : childTasks) {
                // Update sprint reference
                child.setSprintId(targetSprint.getId());
                child.setSprint(targetSprint);
                targetSprint.addTask(child);

                // Insert child after the story (maintaining order)
                if (insertIndex >= targetTaskOrder.size()) {
                    targetTaskOrder.add(child);
                } else {
                    targetTaskOrder.add(insertIndex, child);
                }
                insertIndex++;

                // Mark child as modified
                child.setStart(null); // Reset start date to force recalculation
                modifiedTasks.add(child);
                log.debug("Moved child task {} to target sprint", child.getKey());
            }
        }

        // 11. Recalculate order IDs for both grids and mark affected tasks as modified
        recalculateOrderIdsAndMarkModified(targetTaskOrder, modifiedTasks);
        recalculateOrderIdsAndMarkModified(sourceGrid.getTaskOrder(), modifiedTasks);

        // 12. Transfer expansion state for Stories from source to target grid
        if (task.isStory()) {
            boolean wasExpanded = sourceGrid.removeAndReturnExpansionState(task.getId());
            if (wasExpanded) {
                targetGrid.addExpansionState(task.getId());
            }
        }

        // 13. Mark the main task as modified
        task.setStart(null); // Reset start date to force recalculation
        modifiedTasks.add(task);

        // 14. Add all modified tasks to the appropriate grid's modifiedTasks collection
        log.info("Marking {} tasks as modified from cross-grid transfer", modifiedTasks.size());
        for (Task modifiedTask : modifiedTasks) {
            // Add to the grid that owns this task (based on sprint)
            if (modifiedTask.getSprintId() != null && modifiedTask.getSprintId().equals(targetSprint.getId())) {
                targetGrid.getModifiedTasks().add(modifiedTask);
            } else {
                sourceGrid.getModifiedTasks().add(modifiedTask);
            }
        }

        // 15. Save all changes and refresh both grids
        saveAllChangesAndRefresh();
    }

    private void loadData() {
        //- populate grid with tasks of the sprint
        long time = System.currentTimeMillis();

        // Capture the security context from the current thread
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Load in parallel with security context propagation
        CompletableFuture<Sprint> sprintFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                Sprint s = sprintApi.getById(sprintId);
                s.initialize();
                return s;
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        CompletableFuture<List<User>> usersFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return userApi.getAll(sprintId);
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution

            }
        });

        CompletableFuture<List<Task>> tasksFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return taskApi.getAll(sprintId);
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        CompletableFuture<List<Worklog>> worklogsFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return worklogApi.getAll(sprintId);
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });
        CompletableFuture<List<User>> userFuture = CompletableFuture.supplyAsync(() -> {
            // Set security context in this thread
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                return userApi.getAll();
            } finally {
                SecurityContextHolder.clearContext();// Clear the security context after execution
            }
        });

        // Also load Backlog sprint (always shown at bottom)
        CompletableFuture<Sprint> backlogSprintFuture = CompletableFuture.supplyAsync(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                Sprint backlog = sprintApi.getBacklogSprint();
                if (backlog != null) {
                    backlog.initialize();
                    // Load tasks and worklogs for backlog
                    List<Task>    backlogTasks    = taskApi.getAll(backlog.getId());
                    List<Worklog> backlogWorklogs = worklogApi.getAll(backlog.getId());
                    List<User>    backlogUsers    = userApi.getAll(backlog.getId());
                    backlog.initUserMap(backlogUsers);
                    backlog.initTaskMap(backlogTasks, backlogWorklogs);
                    backlog.recalculate(ParameterOptions.getLocalNow());
                }
                return backlog;
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        // Wait for all futures and combine results
        try {
            sprint = sprintFuture.get();
            time   = System.currentTimeMillis();
            sprint.initUserMap(usersFuture.get());
            sprint.initTaskMap(tasksFuture.get(), worklogsFuture.get());
            users = userFuture.get();
            log.trace("sprint, user, task and worklog maps initialized in {} ms", System.currentTimeMillis() - time);
            sprint.recalculate(ParameterOptions.getLocalNow());

            // Get backlog sprint (may be null if it doesn't exist yet)
            backlogSprint = backlogSprintFuture.get();
            if (backlogSprint != null && backlogSprint.getId().equals(sprint.getId())) {
                // If selected sprint IS the backlog, don't duplicate it
                backlogSprint = null;
            }

            // Populate user selector with users from this sprint
            if (userSelector != null) {
                userSelector.setItems(users);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error loading sprint data", e);
            // Handle exception appropriately
        }
        ganttUtil.levelResources(eh, sprint, "", ParameterOptions.getLocalNow());
    }

    /**
     * Load data for the selected sprint from the sprint selector dropdown.
     * This is called when the user selects a different sprint.
     */
    private void loadDataForSelectedSprint() {
        if (selectedSprint == null) {
            return;
        }

        // Update sprintId to the selected sprint
        this.sprintId = selectedSprint.getId();

        // Reload data for the new sprint
        loadData();
        applyFilters();
        refreshGrid();
    }

    /**
     * Populate the sprint selector with all available sprints.
     * The Backlog sprint is excluded since it's always shown at the bottom.
     */
    private void populateSprintSelector() {
        if (sprintSelector == null) {
            return;
        }

        try {
            // Load all sprints and filter out Backlog (it's always visible at bottom)
            allSprints = sprintApi.getAll().stream()
                    .filter(s -> !"Backlog".equals(s.getName()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

            // Sort sprints by start date (newest first)
            allSprints.sort((s1, s2) -> {
                if (s1.getStart() == null && s2.getStart() == null) return 0;
                if (s1.getStart() == null) return 1;
                if (s2.getStart() == null) return -1;
                return s2.getStart().compareTo(s1.getStart());
            });

            sprintSelector.setItems(allSprints);

            // Select the current sprint if available
            if (sprintId != null) {
                allSprints.stream()
                        .filter(s -> s.getId().equals(sprintId))
                        .findFirst()
                        .ifPresent(s -> {
                            selectedSprint = s;
                            sprintSelector.setValue(s);
                        });
            }
        } catch (Exception e) {
            log.error("Error loading sprints for selector", e);
        }
    }

    /**
     * Recalculate order IDs for all tasks in the list based on their current positions.
     *
     * @param taskOrder The list of tasks to update
     */
    private void recalculateOrderIds(List<Task> taskOrder) {
        for (int i = 0; i < taskOrder.size(); i++) {
            taskOrder.get(i).setOrderId(i);
        }
    }

    /**
     * Recalculate order IDs for all tasks and mark them as modified for batch saving.
     *
     * @param taskOrder     The list of tasks to update
     * @param modifiedTasks Set to collect tasks that were modified
     */
    private void recalculateOrderIdsAndMarkModified(List<Task> taskOrder, java.util.Set<Task> modifiedTasks) {
        for (int i = 0; i < taskOrder.size(); i++) {
            Task task = taskOrder.get(i);
            if (task.getOrderId() == null || task.getOrderId() != i) {
                task.setOrderId(i);
                modifiedTasks.add(task);
            }
        }
    }

    /**
     * Refresh both grids and the Gantt chart.
     * Sprint grid shows the selected sprint's tasks.
     * Backlog grid shows the Backlog sprint's tasks (always at bottom).
     */
    private void refreshGrid() {
        // Update sprint grid
        if (sprint != null) {
            grid.updateData(sprint, new ArrayList<>(sprint.getTasks()), users);
        } else {
            log.warn("Cannot refresh sprint grid - sprint is null");
        }

        // Update backlog grid
        if (backlogSprint != null) {
            backlogGrid.updateData(backlogSprint, new ArrayList<>(backlogSprint.getTasks()), users);
            backlogGridPanel.setVisible(true);
        } else {
            // Hide backlog panel if no backlog sprint exists
            backlogGridPanel.setVisible(false);
        }

        // Generate Gantt chart only for the selected sprint (not for Backlog)
        // Backlog sprint doesn't have scheduling, so Gantt chart doesn't make sense for it
        if (sprint != null && !"Backlog".equals(sprint.getName())) {
            generateGanttChart();
        } else if (ganttChartContainer != null) {
            // Clear the Gantt chart container if we're showing Backlog or no sprint
            ganttChartContainer.removeAll();
        }
    }

    /**
     * Removes broken relations when tasks are moved between sprints.
     * <p>
     * When a task is moved from one sprint to another, any predecessor/successor relationships
     * that now span across sprints need to be removed to maintain data integrity.
     * <p>
     * This method handles two cases:
     * <ol>
     *     <li>Predecessors of moved tasks that reference tasks remaining in the source sprint</li>
     *     <li>Tasks in the source sprint that have moved tasks as predecessors</li>
     * </ol>
     *
     * @param movedTasks    List of tasks being moved (includes the main task and any child tasks)
     * @param sourceSprint  The sprint the tasks are being moved from
     * @param targetSprint  The sprint the tasks are being moved to
     * @param modifiedTasks Set to collect tasks that were modified (for batch saving)
     */
    private void removeBrokenRelations(List<Task> movedTasks, Sprint sourceSprint, Sprint targetSprint, java.util.Set<Task> modifiedTasks) {
        // Create a set of moved task IDs for quick lookup
        java.util.Set<Long> movedTaskIds = movedTasks.stream()
                .map(Task::getId)
                .collect(java.util.stream.Collectors.toSet());

        // 1. Remove predecessors of moved tasks that reference tasks in the source sprint
        for (Task movedTask : movedTasks) {
            List<Relation> predecessors = movedTask.getPredecessors();
            if (predecessors != null && !predecessors.isEmpty()) {
                List<Relation> relationsToRemove = new ArrayList<>();
                for (Relation relation : predecessors) {
                    Long predecessorTaskId = relation.getPredecessorId();
                    // If the predecessor is NOT in the moved tasks list, it's staying in source sprint
                    // So this relation becomes broken
                    if (!movedTaskIds.contains(predecessorTaskId)) {
                        Task predecessorTask = sourceSprint.getTaskById(predecessorTaskId);
                        if (predecessorTask != null) {
                            relationsToRemove.add(relation);
                            log.info("Removing broken relation: {} had predecessor {} which stays in sprint '{}'",
                                    movedTask.getKey(), predecessorTask.getKey(), sourceSprint.getName());
                        }
                    }
                }
                if (!relationsToRemove.isEmpty()) {
                    predecessors.removeAll(relationsToRemove);
                    modifiedTasks.add(movedTask);
                }
            }
        }

        // 2. Remove relations from tasks in source sprint that have moved tasks as predecessors
        for (Task sourceTask : sourceSprint.getTasks()) {
            // Skip tasks that are being moved
            if (movedTaskIds.contains(sourceTask.getId())) {
                continue;
            }

            List<Relation> predecessors = sourceTask.getPredecessors();
            if (predecessors != null && !predecessors.isEmpty()) {
                List<Relation> relationsToRemove = new ArrayList<>();
                for (Relation relation : predecessors) {
                    Long predecessorTaskId = relation.getPredecessorId();
                    // If the predecessor is in the moved tasks, this relation becomes broken
                    if (movedTaskIds.contains(predecessorTaskId)) {
                        relationsToRemove.add(relation);
                        Task movedPredecessor = movedTasks.stream()
                                .filter(t -> t.getId().equals(predecessorTaskId))
                                .findFirst()
                                .orElse(null);
                        log.info("Removing broken relation: {} in sprint '{}' had predecessor {} which moved to sprint '{}'",
                                sourceTask.getKey(), sourceSprint.getName(),
                                movedPredecessor != null ? movedPredecessor.getKey() : predecessorTaskId,
                                targetSprint.getName());
                    }
                }
                if (!relationsToRemove.isEmpty()) {
                    predecessors.removeAll(relationsToRemove);
                    // Mark task as modified for batch saving
                    modifiedTasks.add(sourceTask);
                }
            }
        }
    }

    /**
     * Save all modified tasks to backend from both sprint grid and backlog grid
     */
    private void saveAllChangesAndRefresh() {
        // Collect modified tasks from both grids
        java.util.Set<Task> allModifiedTasks = new java.util.HashSet<>();
        allModifiedTasks.addAll(grid.getModifiedTasks());
        allModifiedTasks.addAll(backlogGrid.getModifiedTasks());

        if (allModifiedTasks.isEmpty()) {
            exitEditMode();
            return;
        }

        log.info("Saving {} modified tasks ({} from sprint grid, {} from backlog grid)", allModifiedTasks.size(), grid.getModifiedTasks().size(), backlogGrid.getModifiedTasks().size());

        // Persist all modified tasks
        for (Task task : allModifiedTasks) {
            if (!task.isMilestone())
                task.setStart(null); // Reset start date to force recalculation
            taskApi.persist(task);
        }

        // Clear modified tasks from both grids and reload data
        grid.getModifiedTasks().clear();
        backlogGrid.getModifiedTasks().clear();
        loadData();
        refreshGrid();
        exitEditMode();
    }

    /**
     * Update the sprint selector value to match the current sprintId.
     * Called after afterNavigation determines the sprint (e.g., when navigating from main menu).
     */
    private void updateSprintSelectorValue() {
        if (sprintSelector == null || sprintId == null) {
            return;
        }

        // Find and select the sprint matching the current sprintId
        allSprints.stream()
                .filter(s -> s.getId().equals(sprintId))
                .findFirst()
                .ifPresent(s -> {
                    selectedSprint = s;
                    sprintSelector.setValue(s);
                });
    }


}
