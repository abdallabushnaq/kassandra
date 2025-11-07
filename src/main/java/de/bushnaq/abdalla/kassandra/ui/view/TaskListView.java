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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Route("task-list")
@PageTitle("Task List Page")
@CssImport("./styles/grid-styles.css")
@PermitAll // When security is enabled, allow all authenticated users
@RolesAllowed({"USER", "ADMIN"}) // Allow access to users with specific roles
public class TaskListView extends Main implements AfterNavigationObserver {
    public static final String            CANCEL_BUTTON_ID           = "cancel-tasks-button";
    public static final String            CREATE_MILESTONE_BUTTON_ID = "create-milestone-button";
    public static final String            CREATE_STORY_BUTTON_ID     = "create-story-button";
    public static final String            CREATE_TASK_BUTTON_ID      = "create-task-button";
    public static final String            EDIT_BUTTON_ID             = "edit-tasks-button";
    public static final String            SAVE_BUTTON_ID             = "save-tasks-button";
    public static final String            TASK_LIST_PAGE_TITLE       = "task-list-page-title";
    private             Button            cancelButton;
    private final       Clock             clock;
    @Autowired
    protected           Context           context;
    private             Button            editButton;
    private final       GanttErrorHandler eh                         = new GanttErrorHandler();
    private final       FeatureApi        featureApi;
    private             Long              featureId;
    private final       Svg               ganttChart                 = new Svg();
    private             GanttUtil         ganttUtil;
    private final       TaskGrid          grid;
    private final       HorizontalLayout  headerLayout;
    private             boolean           isEditMode                 = false;// Edit mode state management
    private final       Logger            logger                     = LoggerFactory.getLogger(this.getClass());
    private final       Set<Task>         modifiedTasks              = new HashSet<>();
    private final       ProductApi        productApi;
    private             Long              productId;
    private             Button            saveButton;
    private             Sprint            sprint;
    private final       SprintApi         sprintApi;
    private             Long              sprintId;
    private final       TaskApi           taskApi;
    private             List<Task>        taskOrder                  = new ArrayList<>(); // Track current order in memory
    private final       UserApi           userApi;
    private final       VersionApi        versionApi;
    private             Long              versionId;
    private final       WorklogApi        worklogApi;

    public TaskListView(WorklogApi worklogApi, TaskApi taskApi, SprintApi sprintApi, ProductApi productApi, VersionApi versionApi, FeatureApi featureApi, UserApi userApi, Clock clock) {
        this.worklogApi = worklogApi;
        this.taskApi    = taskApi;
        this.sprintApi  = sprintApi;
        this.productApi = productApi;
        this.versionApi = versionApi;
        this.featureApi = featureApi;
        this.userApi    = userApi;
        this.clock      = clock;

        try {
            setSizeFull();
            addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
            headerLayout = createHeaderWithButtons();
            grid         = createGrid(clock);
            add(headerLayout, grid);
            this.getStyle().set("padding-left", "var(--lumo-space-m)");
            this.getStyle().set("padding-right", "var(--lumo-space-m)");
        } catch (Exception e) {
            logger.error("Error initializing TaskListView", e);
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
     * Assign a user to a newly created task.
     * First tries to find a task above in the ordered list with an assigned user.
     * If not found, assigns the currently logged-in user.
     */
    private void assignUserToNewTask(Task newTask) {
        Long assignedUserId = null;

        // Find the index of the new task in the ordered list
        int newTaskIndex = taskOrder.indexOf(newTask);

        // Look for a task above with an assigned user
        if (newTaskIndex > 0) {
            for (int i = newTaskIndex - 1; i >= 0; i--) {
                Task previousTask = taskOrder.get(i);
                if (previousTask.getResourceId() != null) {
                    assignedUserId = previousTask.getResourceId();
                    break;
                }
            }
        }

        // If no user found above, use the currently logged-in user
        if (assignedUserId == null) {
            String currentUserName = getUserName();
            try {
                User currentUser = userApi.getByName(currentUserName);
                if (currentUser != null) {
                    assignedUserId = currentUser.getId();
                }
            } catch (Exception e) {
                logger.warn("Could not find user with name: " + currentUserName, e);
            }
        }

        // Assign the user to the new task
        if (assignedUserId != null) {
            newTask.setResourceId(assignedUserId);
        }
    }

    /**
     * Cancel edit mode and discard all changes
     */
    private void cancelEditMode() {
        modifiedTasks.clear();

        // Reload data to discard changes
        loadData();
        refreshGrid();
        exitEditMode();
    }

    private TaskGrid createGrid(Clock clock) {
        // Initialize with empty user list - will be populated in refreshGrid()
        TaskGrid grid = new TaskGrid(clock, getLocale(), new ArrayList<>());

        // Set up callbacks for grid actions
        grid.setOnTaskModified((task, editMode) -> markTaskAsModified(task));
        grid.setOnIndentTask(this::indentTask);
        grid.setOnOutdentTask(this::outdentTask);
        grid.setOnDependencyDrop(this::handleDependencyDrop);

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
        title.setId(TASK_LIST_PAGE_TITLE);
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
        createTaskButton.addClickListener(e -> createTask());

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
        saveButton.addClickListener(e -> saveAllChanges());

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
     * Create a new Task with default estimates
     */
    private void createTask() {
        long nextOrderId = sprint.getNextOrderId();

        Task task = new Task();
        task.setName("New Task-" + nextOrderId);
        task.setSprint(sprint);
        task.setSprintId(sprint.getId());
        sprint.addTask(task);
        Duration work = Duration.ofHours(7).plus(Duration.ofMinutes(30));
        task.setMinEstimate(work);
        task.setOriginalEstimate(work);
        task.setRemainingEstimate(work);
        taskOrder.add(task);
        indentTask(task);

        // Assign user to the new task
        assignUserToNewTask(task);

        Task saved = taskApi.persist(task);
        loadData();
        refreshGrid();
    }

    /**
     * Enter edit mode - enable editing for all rows
     */
    private void enterEditMode() {
        isEditMode = true;
        modifiedTasks.clear();

        // Update button visibility
        editButton.setVisible(false);
        saveButton.setVisible(true);
        cancelButton.setVisible(true);

        // Update grid edit mode
        grid.setEditMode(true);

        logger.info("Edit mode entered");
    }

    /**
     * Exit edit mode
     */
    private void exitEditMode() {
        isEditMode = false;

        // Update button visibility
        editButton.setVisible(true);
        saveButton.setVisible(false);
        cancelButton.setVisible(false);

        // Update grid edit mode
        grid.setEditMode(false);
    }

    /**
     * Find the previous story (parent candidate) in the task list before the given task
     */
    private Task findPreviousStory(Task task) {
        int taskIndex = taskOrder.indexOf(task);
        if (taskIndex <= 0) {
            return null;
        }

        // Get the current parent of the task
        Task currentParent = task.getParentTask();

        // Search backwards for a story that could be a parent
        // The story must be at the same hierarchy level (same parent as the task)
        for (int i = taskIndex - 1; i >= 0; i--) {
            Task candidate = taskOrder.get(i);

            // Only stories can be parents, and prevent circular references
            if (candidate.isStory() && !candidate.isAncestorOf(task)) {
                // Check if the candidate is at the same hierarchy level
                // (has the same parent as the task)
                if (candidate.getParentTask() == currentParent) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void generateGanttChart() {
        try {
            long time = System.currentTimeMillis();
            RenderUtil.generateGanttChartSvg(context, sprint, ganttChart);

            // Configure Gantt chart for proper scrolling display
            ganttChart.getStyle()
                    .set("margin-top", "var(--lumo-space-m)")
                    .set("max-width", "100%")
                    .set("height", "auto")
                    .set("display", "block");

            // Add the chart in a container div for better scrolling behavior
            Div chartContainer = new Div(ganttChart);
            chartContainer.getStyle()
                    .set("overflow-x", "auto")
                    .set("width", "100%");

            add(chartContainer);
            logger.info("Gantt chart generated in {} ms", System.currentTimeMillis() - time);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            // Convert stack trace to string
            StringWriter stringWriter = new StringWriter();
            PrintWriter  printWriter  = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            String stackTrace = stringWriter.toString();

            // Display error message with stack trace
            Paragraph errorParagraph      = new Paragraph("Error generating gantt chart: " + e.getMessage());
            Paragraph stackTraceParagraph = new Paragraph(stackTrace);
            stackTraceParagraph.getStyle().set("white-space", "pre-wrap").set("font-family", "monospace").set("font-size", "12px");

            Div errorContainer = new Div(errorParagraph, stackTraceParagraph);
            add(errorContainer);
        }
    }


    /**
     * Get the currently logged-in user's name or email.
     * Copied from MainLayout for consistency.
     */
    private String getUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String         userName       = authentication != null ? authentication.getName() : "Guest";

        // If using OIDC, try to get the email address from authentication details
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null && !email.isEmpty()) {
                userName = email;
            }
        }
        return userName;
    }

    /**
     * Handle dependency creation/removal when dropping a task ON_TOP of another task.
     * If dependency exists, it will be removed. Otherwise, it will be created.
     *
     * @param sourceTask The task being dragged (will become dependent on target)
     * @param targetTask The task being dropped onto (will become predecessor of source)
     */
    private void handleDependencyDrop(Task sourceTask, Task targetTask) {
        logger.info("Handling dependency drop: {} onto {}", sourceTask.getKey(), targetTask.getKey());

        // Check if dependency already exists (target is already a predecessor of source)
        boolean dependencyExists = sourceTask.getPredecessors().stream()
                .filter(Relation::isVisible)
                .anyMatch(relation -> relation.getPredecessorId().equals(targetTask.getId()));

        if (dependencyExists) {
            // Remove the dependency
            sourceTask.getPredecessors().removeIf(relation ->
                    relation.isVisible() && relation.getPredecessorId().equals(targetTask.getId()));
            logger.info("Removed dependency: {} no longer depends on {}", sourceTask.getKey(), targetTask.getKey());
        } else {
            // Add the dependency (target becomes predecessor of source)
            sourceTask.addPredecessor(targetTask, true); // true = visible
            logger.info("Created dependency: {} now depends on {}", sourceTask.getKey(), targetTask.getKey());
        }

        // Mark the source task as modified
        markTaskAsModified(sourceTask);

        // Refresh grid to show updated dependencies
        grid.getDataProvider().refreshAll();
    }

    /**
     * Indent task - make it a child of the previous story (Tab key)
     */
    private void indentTask(Task task) {
        Task previousStory = findPreviousStory(task);
        if (previousStory == null) {
            logger.debug("Cannot indent task {} - no valid parent found", task.getKey());
            return;
        }

        logger.info("Indenting task {} to become child of {}", task.getKey(), previousStory.getKey());

        // Remove from current parent if any
        if (task.getParentTask() != null) {
            task.getParentTask().removeChildTask(task);
        }

        // Add to new parent
        previousStory.addChildTask(task);
        markTaskAsModified(task);
        markTaskAsModified(previousStory);

        // Refresh grid to show updated hierarchy
        grid.getDataProvider().refreshAll();
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

        // Wait for all futures and combine results
        try {
            sprint = sprintFuture.get();
            logger.info("sprint loaded and initialized in {} ms", System.currentTimeMillis() - time);
            time = System.currentTimeMillis();
            sprint.initUserMap(usersFuture.get());
            sprint.initTaskMap(tasksFuture.get(), worklogsFuture.get());
            logger.info("sprint user, task and worklog maps initialized in {} ms", System.currentTimeMillis() - time);
            sprint.recalculate(ParameterOptions.getLocalNow());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error loading sprint data", e);
            // Handle exception appropriately
        }
        ganttUtil.levelResources(eh, sprint, "", ParameterOptions.getLocalNow());
    }

    /**
     * Mark a task as modified
     */
    private void markTaskAsModified(Task task) {
        modifiedTasks.add(task);
        logger.debug("Task {} marked as modified. Total modified: {}", task.getKey(), modifiedTasks.size());
    }


    /**
     * Outdent task - remove it as a child from its parent (Shift+Tab key)
     */
    private void outdentTask(Task task) {
        if (task.getParentTask() == null) {
            logger.debug("Cannot outdent task {} - it has no parent", task.getKey());
            return;
        }

        logger.info("Outdenting task {} from parent {}", task.getKey(), task.getParentTask().getKey());

        Task oldParent = task.getParentTask();
        oldParent.removeChildTask(task);
        if (oldParent.getParentTask() != null)
            oldParent.getParentTask().addChildTask(task);
        markTaskAsModified(task);
        markTaskAsModified(oldParent);

        // Refresh grid to show updated hierarchy
        grid.getDataProvider().refreshAll();
    }

    /**
     * Refresh the grid data and Gantt chart
     */
    private void refreshGrid() {
        // Update taskOrder list with current sprint tasks
        taskOrder = new ArrayList<>(sprint.getTasks());

        // Get fresh user list from API
        List<User> users = userApi.getAll();

        // Update grid with all data
        grid.updateData(sprint, taskOrder, users);
        generateGanttChart();
    }

    /**
     * Save all modified tasks to backend
     */
    private void saveAllChanges() {
        if (modifiedTasks.isEmpty()) {
            exitEditMode();
            return;
        }

        logger.info("Saving {} modified tasks", modifiedTasks.size());

        // Persist all modified tasks
        for (Task task : modifiedTasks) {
            if (!task.isMilestone())
                task.setStart(null); // Reset start date to force recalculation
            taskApi.persist(task);
        }

        // Clear modified tasks and reload data
        modifiedTasks.clear();
        loadData();
        refreshGrid();
        exitEditMode();
    }
}
