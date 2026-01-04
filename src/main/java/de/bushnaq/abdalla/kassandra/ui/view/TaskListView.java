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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
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

@Route("task-list")
@PageTitle("Task List Page")
@PermitAll // When security is enabled, allow all authenticated users
@RolesAllowed({"USER", "ADMIN"}) // Allow access to users with specific roles
@Log4j2
public class TaskListView extends Main implements AfterNavigationObserver {
    public static final String                  CANCEL_BUTTON_ID           = "cancel-tasks-button";
    public static final String                  CREATE_MILESTONE_BUTTON_ID = "create-milestone-button";
    public static final String                  CREATE_STORY_BUTTON_ID     = "create-story-button";
    public static final String                  CREATE_TASK_BUTTON_ID      = "create-task-button";
    public static final String                  EDIT_BUTTON_ID             = "edit-tasks-button";
    public static final String                  SAVE_BUTTON_ID             = "save-tasks-button";
    public static final String                  TASK_LIST_PAGE_TITLE_ID    = "task-list-page-title";
    private             Button                  cancelButton;
    private final       Clock                   clock;
    @Autowired
    protected           Context                 context;
    private             Button                  editButton;
    private final       GanttErrorHandler       eh                         = new GanttErrorHandler();
    private final       FeatureApi              featureApi;
    private             Long                    featureId;
    private final       Svg                     ganttChart                 = new Svg();
    private             Div                     ganttChartContainer;
    private             CompletableFuture<Void> ganttGenerationFuture;
    private             GanttUtil               ganttUtil;
    private final       TaskGrid                grid;
    private final       HorizontalLayout        headerLayout;
    private final       JsonMapper              jsonMapper;
    private             User                    loggedInUser               = null;
    private final       ProductApi              productApi;
    private             Long                    productId;
    private             Button                  saveButton;
    private             Sprint                  sprint;
    private final       SprintApi               sprintApi;
    private             Long                    sprintId;
    private final       TaskApi                 taskApi;
    private final       UserApi                 userApi;
    private             List<User>              users                      = new ArrayList<>();
    private final       VersionApi              versionApi;
    private             Long                    versionId;
    private final       WorklogApi              worklogApi;

    public TaskListView(WorklogApi worklogApi, TaskApi taskApi, SprintApi sprintApi, ProductApi productApi, VersionApi versionApi, FeatureApi featureApi, UserApi userApi, Clock clock, JsonMapper jsonMapper) {
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

            addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
            headerLayout = createHeaderWithButtons();
            grid         = createGrid(clock);
            add(headerLayout, grid);
            this.getStyle().set("padding-left", "var(--lumo-space-m)");
            this.getStyle().set("padding-right", "var(--lumo-space-m)");
            String userEmail = getUserEmail();
            try {
                loggedInUser = userApi.getByEmail(userEmail);
            } catch (ResponseStatusException e) {
                log.warn("Could not find user with email: " + userEmail, e);
            }

        } catch (Exception e) {
            log.error("Error initializing TaskListView", e);
            throw e;
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        //- Get query parameters
        Location        location        = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
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
//            pageTitle.setText("Task of Sprint ID: " + sprintId);
        }
        ganttUtil = new GanttUtil(context);
        loadData();

        //- Update breadcrumbs
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout mainLayout) {
                        mainLayout.getBreadcrumbs().clear();
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
                            mainLayout.getBreadcrumbs().addItem("Tasks", TaskListView.class, params);
                        }
                    }
                });

        //- populate grid
//        pageTitle.setText("Task of Sprint ID: " + sprintId);
        refreshGrid();
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
        // Set up callbacks for grid actions
        grid.setOnPersistTask(this::onPersistTask);
        grid.setOnSaveAllChangesAndRefresh(this::saveAllChangesAndRefresh);

        grid.setWidthFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        return grid;
    }

    /**
     * Creates the header layout with Create, Edit, Save, and Cancel buttons
     */
    private HorizontalLayout createHeaderWithButtons() {
        // Create header without the create button (we'll add three buttons manually)
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        header.getStyle().set("padding", "var(--lumo-space-m)");

        // Create title with icon
        com.vaadin.flow.component.icon.Icon icon = VaadinIcon.TASKS.create();
        icon.getStyle().set("margin-right", "var(--lumo-space-s)");

        com.vaadin.flow.component.html.H2 title = new com.vaadin.flow.component.html.H2("Tasks");
        title.setId(TASK_LIST_PAGE_TITLE_ID);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "600");

        HorizontalLayout titleLayout = new HorizontalLayout(icon, title);
        titleLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(false);

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
        header.add(titleLayout, createMilestoneButton, createStoryButton, createTaskButton, editButton, saveButton, cancelButton);
        header.expand(titleLayout); // Make title take remaining space

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

    private void generateGanttChart() {
        // Cancel any previous generation in progress
        if (ganttGenerationFuture != null && !ganttGenerationFuture.isDone()) {
            ganttGenerationFuture.cancel(true);
            log.debug("Cancelled previous Gantt chart generation");
        }

        // Initialize container if needed
        if (ganttChartContainer == null) {
            ganttChartContainer = new Div();
            ganttChartContainer.getStyle()
                    .set("overflow-x", "auto")
                    .set("width", "100%")
                    .set("margin-top", "var(--lumo-space-m)");
            add(ganttChartContainer);
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
                RenderUtil.generateGanttChartSvg(TaskListView.this.context, sprintSnapshot, svg);
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

        // Wait for all futures and combine results
        try {
            sprint = sprintFuture.get();
            time   = System.currentTimeMillis();
            sprint.initUserMap(usersFuture.get());
            sprint.initTaskMap(tasksFuture.get(), worklogsFuture.get());
            users = userFuture.get();
            log.trace("sprint, user, task and worklog maps initialized in {} ms", System.currentTimeMillis() - time);
            sprint.recalculate(ParameterOptions.getLocalNow());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error loading sprint data", e);
            // Handle exception appropriately
        }
        ganttUtil.levelResources(eh, sprint, "", ParameterOptions.getLocalNow());
    }

    private void onPersistTask(Task task) {
        Task saved = taskApi.persist(task);
    }

    /**
     * Refresh the grid data and Gantt chart
     */
    private void refreshGrid() {
        // Update taskOrder list with current sprint tasks
        grid.updateData(sprint, new ArrayList<>(sprint.getTasks()), users);
        generateGanttChart();
    }

    /**
     * Save all modified tasks to backend
     */
    private void saveAllChangesAndRefresh() {
        if (grid.getModifiedTasks().isEmpty()) {
            exitEditMode();
            return;
        }

        log.info("Saving {} modified tasks", grid.getModifiedTasks().size());

        // Persist all modified tasks
        for (Task task : grid.getModifiedTasks()) {
            if (!task.isMilestone())
                task.setStart(null); // Reset start date to force recalculation
            taskApi.persist(task);
        }

        // Clear modified tasks and reload data
        grid.getModifiedTasks().clear();
        loadData();
        refreshGrid();
        exitEditMode();
    }


}
