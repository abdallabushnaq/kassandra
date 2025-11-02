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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.dialog.DependencyDialog;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.date.DateUtil;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
    public static final String            TASK_GRID_NAME_PREFIX      = "task-grid-name-";
    public static final String            TASK_LIST_PAGE_TITLE       = "task-list-page-title";
    private             Button            cancelButton;
    private final       Clock             clock;
    @Autowired
    protected           Context           context;
    private             Task              draggedTask;          // Track the currently dragged task
    private final       DateTimeFormatter dtfymdhm                   = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm");
    private             Button            editButton;
    private final       GanttErrorHandler eh                         = new GanttErrorHandler();
    private final       FeatureApi        featureApi;
    private             Long              featureId;
    private final       Svg               ganttChart                 = new Svg();
    private             GanttUtil         ganttUtil;
    private             Grid<Task>        grid;
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
            add(headerLayout, createGrid(clock));
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

    private Grid<Task> createGrid(Clock clock) {
        grid = new Grid<>();
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_NO_BORDER, com.vaadin.flow.component.grid.GridVariant.LUMO_NO_ROW_BORDERS);

        createGridColumns();

        // Enable row reordering with drag and drop in edit mode
        grid.setRowsDraggable(false); // Will be enabled in edit mode

        // Enable keyboard navigation in edit mode
        setupKeyboardNavigation();

        // Add drop listener for reordering and dependency management
        grid.addDropListener(event -> {
            if (!isEditMode || draggedTask == null) return;

            Task dropTargetTask = event.getDropTargetItem().orElse(null);

            if (dropTargetTask != null && !draggedTask.equals(dropTargetTask)) {
                // Check drop location to determine action
                com.vaadin.flow.component.grid.dnd.GridDropLocation dropLocation = event.getDropLocation();

                if (dropLocation == com.vaadin.flow.component.grid.dnd.GridDropLocation.ON_TOP) {
                    // Handle dependency creation/removal when dropping ON_TOP
                    handleDependencyDrop(draggedTask, dropTargetTask);
                } else {
                    // Handle reordering when dropping BETWEEN
                    int draggedIndex = taskOrder.indexOf(draggedTask);
                    int targetIndex  = taskOrder.indexOf(dropTargetTask);

                    if (draggedIndex >= 0 && targetIndex >= 0) {
                        // Remove from old parent before moving
                        if (draggedTask.getParentTask() != null) {
                            Task oldParent = draggedTask.getParentTask();
                            oldParent.removeChildTask(draggedTask);
                            markTaskAsModified(oldParent);
                        }

                        moveTask(draggedIndex, targetIndex);

                        // Try to re-parent the task based on its new position
                        indentTask(draggedTask);
                    }
                }
            }

            draggedTask = null; // Clear the dragged task reference
        });

        grid.addDragStartListener(event -> {
            if (isEditMode && !event.getDraggedItems().isEmpty()) {
                draggedTask = event.getDraggedItems().get(0);
                grid.setDropMode(com.vaadin.flow.component.grid.dnd.GridDropMode.ON_TOP_OR_BETWEEN); // Enable drop on top or between rows
            }
        });

        grid.addDragEndListener(event -> {
            draggedTask = null; // Clear reference when drag ends without drop
            grid.setDropMode(null);
        });

        // Add drop filter to prevent invalid dependency creation
        grid.setDropFilter(dropTargetTask -> {
            if (!isEditMode || draggedTask == null || dropTargetTask == null) {
                return true; // Allow drop if not in edit mode or no dragged task
            }

            // For ON_TOP drops, check if dependency creation is valid
            // This uses the same eligibility logic as DependencyDialog
            return isEligiblePredecessor(dropTargetTask, draggedTask);
        });

        // Add borders between columns
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
//        grid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES);

        // Add custom styling for more JIRA-like appearance
//        grid.addClassName("jira-style-grid");

        // Apply custom styling directly to make borders lighter gray
//        grid.getElement().getStyle().set("--lumo-contrast-10pct", "#e0e0e0");

        // Remove the built-in scrollbar from the grid
//        grid.getElement().getStyle().set("overflow", "visible");

        // Set height to auto instead of 100% to allow grid to take only needed space
        grid.setHeight("auto");
        grid.setAllRowsVisible(true);
        setWidthFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        add(grid);
        return grid;
    }

    private void createGridColumns() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(clock.getZone()).withLocale(getLocale());

        // Hidden Identifier column - contains the actual task ID for JavaScript logic
        {
            Grid.Column<Task> identifierColumn = grid.addColumn(task -> task.getId() != null ? task.getId().toString() : "")
                    .setHeader("Identifier")
                    .setAutoWidth(true);
            identifierColumn.setVisible(false);
            identifierColumn.setKey("identifier");
            identifierColumn.setId("task-grid-id-column");
        }

        // Order Column with Up/Down arrows - visible only in edit mode
        {
            grid.addComponentColumn(task -> {
                if (isEditMode) {
                    // Create drag handle icon (burger menu)
                    com.vaadin.flow.component.icon.Icon dragIcon = VaadinIcon.MENU.create();
                    dragIcon.getStyle()
                            .set("cursor", "grab")
                            .set("color", "var(--lumo-secondary-text-color)");

                    Div dragHandle = new Div(dragIcon);
                    dragHandle.getStyle()
                            .set("display", "flex")
                            .set("align-items", "center")
                            .set("justify-content", "center");
                    dragHandle.setTitle("Drag to reorder");

                    return dragHandle;
                } else {
                    return new Div(); // Empty div when not in edit mode
                }
            }).setHeader("").setAutoWidth(true).setWidth("50px");
        }

        //Key
        {
            grid.addColumn(Task::getKey).setHeader("Key").setAutoWidth(true);
        }

        //ID
        {
//            Grid.Column<Task> id = grid.addColumn(Task::getOrderId).setHeader("ID").setAutoWidth(true);
//            id.setId("task-grid-id-column");
            grid.addColumn(Task::getOrderId).setHeader("#").setAutoWidth(true).setId("task-grid-#-column");
        }
        //Dependency
        {
            grid.addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode) {
                    // Editable - show current dependencies as text with an edit button
                    HorizontalLayout container = new HorizontalLayout();
                    container.setSpacing(false);
                    container.setPadding(false);
                    container.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
                    container.setWidthFull();

                    // Style the container to look like an editable field
                    container.getStyle()
                            .set("background-color", "var(--lumo-contrast-10pct)")
                            .set("border", "1px solid var(--lumo-contrast-20pct)")
                            .set("border-radius", "var(--lumo-border-radius-m)")
                            .set("padding", "0")
                            .set("min-height", "var(--lumo-size-m)")
                            .set("cursor", "pointer");

                    // Display current dependencies as text
                    String dependencyText = getDependencyText(task);
                    Div    textDiv        = new Div();
                    textDiv.setText(dependencyText.isEmpty() ? "Click to edit..." : dependencyText);
                    textDiv.getStyle()
                            .set("flex-grow", "1")
                            .set("padding", "var(--lumo-space-xs)")
                            .set("min-width", "0")
                            .set("overflow", "hidden")
                            .set("text-overflow", "ellipsis")
                            .set("white-space", "nowrap");

                    // Add placeholder style if empty
                    if (dependencyText.isEmpty()) {
                        textDiv.getStyle()
                                .set("color", "var(--lumo-secondary-text-color)")
                                .set("font-style", "italic");
                    }

                    // Edit button
                    Button editButton = new Button(VaadinIcon.EDIT.create());
                    editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
                    editButton.getStyle()
                            .set("margin", "0")
                            .set("min-width", "var(--lumo-size-m)");

                    // Stop event propagation before adding click listener
                    editButton.getElement().addEventListener("click", e -> {
                        // This will be executed first and stops propagation
                    }).addEventData("event.stopPropagation()");

                    editButton.addClickListener(e -> {
                        openDependencyEditor(task);
                    });

                    // Make the whole container clickable
                    container.addClickListener(e -> {
                        if (e.getButton() == 0) { // Left mouse button
                            openDependencyEditor(task);
                        }
                    });

                    container.add(textDiv, editButton);
                    return container;
                } else {
                    // Read-only - show dependencies as text
                    Div div = new Div();
                    div.setText(getDependencyText(task));
                    return div;
                }
            })).setHeader("Dependency").setAutoWidth(true);
        }
        //Parent
        {
            grid.addColumn(task -> task.getParentTask() != null ? task.getParentTask().getOrderId() : "").setHeader("Parent").setAutoWidth(true);
        }
        //name - Editable for all task types, with icon on the left
        {
            Grid.Column<Task> nameColumn = grid.addColumn(new ComponentRenderer<>(task -> {
                // Calculate indentation depth
                int depth        = task.getHierarchyDepth();
                int indentPixels = depth * 20;

                // Create container for icon + name
                HorizontalLayout container = new HorizontalLayout();
                container.setSpacing(false);
                container.setPadding(false);
                container.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
                container.getStyle().set("padding-left", indentPixels + "px");

                // Add icon based on task type
                if (task.isMilestone()) {
                    // Diamond shape for milestone
                    Div diamond = new Div();
                    diamond.getElement().getStyle()
                            .set("width", "12px")
                            .set("height", "12px")
                            .set("background-color", "#1976d2")
                            .set("transform", "rotate(45deg)")
                            .set("margin-right", "8px")
                            .set("flex-shrink", "0");
                    diamond.getElement().setAttribute("title", "Milestone");
                    container.add(diamond);
                } else if (task.isStory()) {
                    // Downward triangle for story
                    Div triangle = new Div();
                    triangle.getElement().getStyle()
                            .set("width", "0")
                            .set("height", "0")
                            .set("border-left", "6px solid transparent")
                            .set("border-right", "6px solid transparent")
                            .set("border-top", "10px solid #43a047")
                            .set("margin-right", "8px")
                            .set("flex-shrink", "0");
                    triangle.getElement().setAttribute("title", "Story");
                    container.add(triangle);
                } else if (task.isTask()) {
                    // Task gets no visible icon, but add spacing to match icon width
                    // Triangle width is 12px (6px + 6px) + 8px margin = 20px total
                    Div spacer = new Div();
                    spacer.getElement().getStyle()
                            .set("width", "20px")
                            .set("height", "1px")
                            .set("flex-shrink", "0");
                    container.add(spacer);
                }

                // Add name field or text
                if (isEditMode) {
                    TextField nameField = new TextField();
                    nameField.setValue(task.getName() != null ? task.getName() : "");
                    nameField.setWidthFull();

                    nameField.addValueChangeListener(e -> {
                        if (e.isFromClient()) {
                            task.setName(e.getValue());
                            markTaskAsModified(task);
                        }
                    });
                    container.add(nameField);
                    container.setFlexGrow(1, nameField);
                } else {
                    Div div = new Div();
                    div.setText(task.getName() != null ? task.getName() : "");
                    div.setId(TASK_GRID_NAME_PREFIX + task.getName());
                    container.add(div);
                    container.setFlexGrow(1, div);
                }

                return container;
            })).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
            nameColumn.setId("task-grid-name-column");
        }
        //Start - Editable only for Milestone tasks
        {
            Grid.Column<Task> startColumn = grid.addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode && task.isMilestone()) {
                    // Editable for Milestone tasks
                    DateTimePicker startField = new DateTimePicker();
                    startField.setValue(task.getStart() != null ? task.getStart() : LocalDateTime.now());
                    startField.setWidthFull();
                    startField.setId(TASK_GRID_NAME_PREFIX + task.getName() + "-start-field");

                    startField.addValueChangeListener(e -> {
                        if (e.isFromClient()) {
                            try {
                                LocalDateTime dateTime = e.getValue();
                                if (dateTime != null) {
                                    task.setStart(dateTime);
                                    markTaskAsModified(task);
                                    startField.setInvalid(false);
                                } else {
                                    task.setStart(null);
                                    markTaskAsModified(task);
                                    startField.setInvalid(false);
                                }
                            } catch (Exception ex) {
                                startField.setInvalid(true);
                                startField.setErrorMessage("Invalid date/time format");
                            }
                        }
                    });

                    return startField;
                } else {
                    // Read-only for Story and Task tasks
                    Div div = new Div();
                    if (task.isMilestone())
                        div.setText(task.getStart() != null ? DateUtil.createDateString(task.getStart(), dtfymdhm) : "");
                    else
                        div.setText("");
                    return div;
                }
            })).setHeader("Start").setAutoWidth(true);
        }
        //Assigned - Editable only for Task tasks
        {
            grid.addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    ComboBox<User> userComboBox = new ComboBox<>();
                    userComboBox.setAllowCustomValue(false);
                    userComboBox.setClearButtonVisible(true);
                    userComboBox.setWidthFull();
                    userComboBox.setItemLabelGenerator(User::getName);

                    // Load ALL users from the system (not just sprint users) so we can assign new users
                    List<User> allUsers = userApi.getAll();
                    userComboBox.setItems(allUsers);

                    // Set current value only if task has an assigned user
                    if (task.getResourceId() != null) {
                        try {
                            User currentUser = sprint.getuser(task.getResourceId());
                            if (currentUser != null && allUsers.contains(currentUser)) {
                                userComboBox.setValue(currentUser);
                            }
                        } catch (Exception ex) {
                            logger.warn("Could not set user for task {}: {}", task.getKey(), ex.getMessage());
                        }
                    }

                    userComboBox.addValueChangeListener(e -> {
                        if (e.isFromClient()) {
                            User selectedUser = e.getValue();
                            task.setResourceId(selectedUser != null ? selectedUser.getId() : null);
                            markTaskAsModified(task);
                        }
                    });

                    return userComboBox;
                } else {
                    // Read-only for Milestone and Story tasks
                    Div div = new Div();
                    if (task.isTask())
                        div.setText(task.getResourceId() != null ? sprint.getuser(task.getResourceId()).getName() : "");
                    else
                        div.setText("");
                    return div;
                }
            })).setHeader("Assigned").setAutoWidth(true);
        }

        //Min Estimate - Editable only for Task tasks
        {
            grid.addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    TextField estimateField = new TextField();
                    estimateField.setValue(!task.getMinEstimate().equals(Duration.ZERO) ?
                            DateUtil.createWorkDayDurationString(task.getMinEstimate()) : "");
                    estimateField.setWidthFull();
                    estimateField.setPlaceholder("e.g., 1d 2h 30m");

                    estimateField.addValueChangeListener(e -> {
                        if (e.isFromClient()) {
                            try {
                                Duration duration = DateUtil.parseWorkDayDurationString(e.getValue().strip());
                                task.setMinEstimate(duration);
                                markTaskAsModified(task);
                                estimateField.setInvalid(false);
                            } catch (IllegalArgumentException ex) {
                                estimateField.setInvalid(true);
                                estimateField.setErrorMessage("Invalid format");
                            }
                        }
                    });

                    return estimateField;
                } else {
                    // Read-only for Milestone and Story tasks
                    Div div = new Div();
                    if (task.isTask())
                        div.setText(!task.getMinEstimate().equals(Duration.ZERO) ? DateUtil.createWorkDayDurationString(task.getMinEstimate()) : "");
                    else
                        div.setText("");
                    return div;
                }
            })).setHeader("Min Estimate").setAutoWidth(true);
        }
        //Max Estimate - Editable only for Task tasks
        {
            grid.addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    TextField estimateField = new TextField();
                    estimateField.setValue(!task.getMaxEstimate().equals(Duration.ZERO) ?
                            DateUtil.createWorkDayDurationString(task.getMaxEstimate()) : "");
                    estimateField.setWidthFull();
                    estimateField.setPlaceholder("e.g., 1d 2h 30m");

                    estimateField.addValueChangeListener(e -> {
                        if (e.isFromClient()) {
                            try {
                                Duration duration = DateUtil.parseWorkDayDurationString(e.getValue().strip());
                                task.setMaxEstimate(duration);
                                markTaskAsModified(task);
                                estimateField.setInvalid(false);
                            } catch (IllegalArgumentException ex) {
                                estimateField.setInvalid(true);
                                estimateField.setErrorMessage("Invalid format");
                            }
                        }
                    });

                    return estimateField;
                } else {
                    // Read-only for Milestone and Story tasks
                    Div div = new Div();
                    if (task.isTask())
                        div.setText(!task.getMaxEstimate().equals(Duration.ZERO) ? DateUtil.createWorkDayDurationString(task.getMaxEstimate()) : "");
                    else
                        div.setText("");
                    return div;
                }
            })).setHeader("Max Estimate").setAutoWidth(true);
        }

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

        // Enable drag and drop for reordering
        grid.setRowsDraggable(true);

        // Add visual feedback for edit mode
        grid.addClassName("edit-mode");

        // Refresh grid to show editable components
        grid.getDataProvider().refreshAll();

        // Setup keyboard navigation after grid is rendered
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                """
                        console.log('=== EXECUTING JAVASCRIPT FROM SERVER ===');
                        
                        const grid = document.querySelector('vaadin-grid');
                        console.log('Found grid:', grid);
                        
                        if (!grid) {
                            console.error('Grid not found!');
                            return;
                        }
                        
                        if (grid.keyboardSetupDone) {
                            console.log('Keyboard already setup, skipping');
                            return;
                        }
                        
                        console.log('Setting up keyboard navigation for the first time');
                        grid.keyboardSetupDone = true;
                        
                        grid.addEventListener('keydown', (e) => {
                            console.log('=== GRID KEYDOWN EVENT ===', e.key);
                        
                            if (e.key !== 'Tab') return;
                        
                            console.log('Tab pressed! Shift:', e.shiftKey);
                        
                            // Check if we're in an input field
                            const target = e.target;
                            console.log('Event target:', target.tagName);
                        
                            // Check if the target is within a text input in the shadow DOM
                            const activeElement = grid.shadowRoot?.activeElement || document.activeElement;
                            console.log('Active element:', activeElement?.tagName);
                        
                            const isInInput = activeElement && (
                                activeElement.tagName === 'INPUT' || 
                                activeElement.tagName === 'VAADIN-TEXT-FIELD' || 
                                activeElement.tagName === 'VAADIN-COMBO-BOX' ||
                                activeElement.tagName === 'VAADIN-DATE-TIME-PICKER'
                            );
                        
                            console.log('Is in input:', isInInput);
                        
                            if (!isInInput) {
                                console.log('>>> Not in input - handling indent/outdent');
                                e.preventDefault();
                                e.stopPropagation();
                        
                                // Get the active (selected) item from the grid
                                const activeItem = grid.activeItem;
                                console.log('Grid active item:', activeItem);
                        
                                if (!activeItem) {
                                    console.log('!!! No active item in grid');
                                    return;
                                }
                        
                                // The activeItem properties are mapped by Vaadin
                                // Try various property names that might contain the ID
                                let taskId = activeItem.id || activeItem.orderId || activeItem.col0;
                                console.log('Task ID from active item (trying id/orderId/col1):', taskId);
                        
                                // If still not found, iterate through all properties to find the ID
                                if (!taskId) {
                                    console.log('Trying to find ID in all properties:', Object.keys(activeItem));
                                    // Look for properties that might be the ID
                                    for (let key in activeItem) {
                                        if (key.toLowerCase().includes('id') || key.toLowerCase().includes('order')) {
                                            console.log('Found potential ID property:', key, '=', activeItem[key]);
                                            if (activeItem[key] && !key.includes('node') && !key.includes('key')) {
                                                taskId = activeItem[key];
                                                break;
                                            }
                                        }
                                    }
                                }
                        
                                // Alternative: find the focused cell and get ID from the DOM
                                if (!taskId) {
                                    console.log('!!! No task ID found in active item, trying alternative approach');
                        
                                    const focusedCell = grid.shadowRoot?.querySelector('td[part~="focused-cell"]') || 
                                                      grid.shadowRoot?.querySelector('td[part~="selected-row-cell"]');
                                    console.log('Focused cell:', focusedCell);
                        
                                    if (focusedCell) {
                                        const row = focusedCell.closest('tr');
                                        console.log('Found row from focused cell:', row);
                        
                                        if (row) {
                                            const cells = row.querySelectorAll('td');
                                            console.log('Found cells in row:', cells.length);
                        
                                            // ID should be in the second column (index 1) - the first visible column after drag handle
                                            if (cells.length > 1) {
                                                // Try to get text from the ID column
                                                const idCell = cells[1];
                                                taskId = idCell.textContent?.trim();
                                                console.log('Task ID from cell[1]:', taskId);
                                            }
                                        }
                                    }
                                }
                        
                                if (!taskId) {
                                    console.log('!!! Could not determine task ID');
                                    return;
                                }
                        
                                const eventName = e.shiftKey ? 'outdent-task' : 'indent-task';
                                console.log('>>> Dispatching:', eventName, 'for task:', taskId);
                        
                                grid.dispatchEvent(new CustomEvent(eventName, {
                                    bubbles: true,
                                    composed: true,
                                    detail: { taskId: String(taskId) }
                                }));
                        
                                console.log('>>> Event dispatched successfully');
                            } else {
                                console.log('In input field - handling cell navigation');
                                // TODO: Implement cell navigation if needed
                            }
                        }, true);
                        
                        console.log('=== KEYBOARD NAVIGATION SETUP COMPLETE ===');
                        """
        ));

        logger.info("Edit mode entered, keyboard navigation should be set up");
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

        // Disable drag and drop
        grid.setRowsDraggable(false);

        // Remove visual feedback
        grid.removeClassName("edit-mode");

        // Update JavaScript edit mode state
        grid.getElement().executeJs("this.updateEditMode(false);");

        // Refresh grid to show read-only components
        grid.getDataProvider().refreshAll();
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
     * Get the dependency text for display (comma-separated orderIds of visible predecessors)
     */
    private String getDependencyText(Task task) {
        List<Relation> relations = task.getPredecessors();
        if (relations == null || relations.isEmpty()) {
            return "";
        }

        return relations.stream()
                .filter(Relation::isVisible) // Only show visible dependencies
                .map(relation -> {
                    Task predecessor = sprint.getTaskById(relation.getPredecessorId());
                    return predecessor != null ? String.valueOf(predecessor.getOrderId()) : "";
                })
                .filter(orderId -> !orderId.isEmpty())
                .collect(Collectors.joining(", "));
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

    /**
     * Check if a task is eligible as a predecessor for another task.
     * Uses the same eligibility logic as DependencyDialog.
     *
     * @param predecessor The potential predecessor task
     * @param dependent   The task that would depend on the predecessor
     * @return true if the predecessor is eligible, false otherwise
     */
    private boolean isEligiblePredecessor(Task predecessor, Task dependent) {
        // A task is eligible as a predecessor if:
        // 1. Not the same task (cannot depend on itself)
        // 2. Not a descendant of the dependent (would create cycle)
        // 3. Not an ancestor of the dependent (would create cycle)

        boolean isEligible = !predecessor.getId().equals(dependent.getId()) // Not the same task
                && !predecessor.isDescendantOf(dependent) // Not a descendant (would create cycle)
                && !dependent.isDescendantOf(predecessor); // Not an ancestor (would create cycle)

        if (!isEligible) {
            if (predecessor.getId().equals(dependent.getId())) {
                logger.debug("Cannot create dependency: task {} cannot depend on itself", dependent.getKey());
            } else if (predecessor.isDescendantOf(dependent)) {
                logger.debug("Cannot create dependency: {} is a descendant of {}", predecessor.getKey(), dependent.getKey());
            } else if (dependent.isDescendantOf(predecessor)) {
                logger.debug("Cannot create dependency: {} is an ancestor of {}", predecessor.getKey(), dependent.getKey());
            }
        }

        return isEligible;
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
     * Move a task to a new position and recalculate all orderIds
     */
    private void moveTask(int fromIndex, int toIndex) {
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 ||
                fromIndex >= taskOrder.size() || toIndex >= taskOrder.size()) {
            return;
        }

        logger.info("Moving task from index {} to {}", fromIndex, toIndex);

        // Remove task from old position
        Task movedTask = taskOrder.remove(fromIndex);

        // Insert at new position
        taskOrder.add(toIndex, movedTask);

        // Recalculate orderIds for all tasks based on their new positions
        for (int i = 0; i < taskOrder.size(); i++) {
            Task task = taskOrder.get(i);
            task.setOrderId((long) i);
            markTaskAsModified(task);
        }

        // Refresh the grid to show new order
        grid.getDataProvider().refreshAll();
        logger.info("Task order updated. {} tasks marked as modified.", modifiedTasks.size());
    }

    /**
     * Open a dialog to edit task dependencies
     */
    private void openDependencyEditor(Task task) {
        DependencyDialog dialog = new DependencyDialog(task, sprint, taskOrder, (t, selectedTaskIds) -> {
            // Mark task as modified
            markTaskAsModified(t);

            // Refresh grid
            grid.getDataProvider().refreshAll();
        });
        dialog.open();
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
        grid.setItems(taskOrder);
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

    /**
     * Setup keyboard navigation for Excel-like behavior
     */
    private void setupKeyboardNavigation() {
        // Register server-side event listeners for indent/outdent
        grid.getElement().addEventListener("indent-task", event -> {
            String taskIdStr = event.getEventData().getString("event.detail.taskId");
            if (taskIdStr != null && !taskIdStr.isEmpty()) {
                try {
                    Long taskId = Long.parseLong(taskIdStr);
                    Task task = taskOrder.stream()
                            .filter(t -> t.getId().equals(taskId))
                            .findFirst()
                            .orElse(null);
                    if (task != null) {
                        indentTask(task);
                    }
                } catch (NumberFormatException ex) {
                    logger.warn("Invalid task ID for indent operation: {}", taskIdStr);
                }
            }
        }).addEventData("event.detail.taskId");

        grid.getElement().addEventListener("outdent-task", event -> {
            String taskIdStr = event.getEventData().getString("event.detail.taskId");
            if (taskIdStr != null && !taskIdStr.isEmpty()) {
                try {
                    Long taskId = Long.parseLong(taskIdStr);
                    Task task = taskOrder.stream()
                            .filter(t -> t.getId().equals(taskId))
                            .findFirst()
                            .orElse(null);
                    if (task != null) {
                        outdentTask(task);
                    }
                } catch (NumberFormatException ex) {
                    logger.warn("Invalid task ID for outdent operation: {}", taskIdStr);
                }
            }
        }).addEventData("event.detail.taskId");

        // Add keyboard navigation support for Tab key navigation between editable cells
        // and Tab/Shift+Tab for indent/outdent when focused on a row
        grid.getElement().executeJs(
                """
                        const grid = this;
                        let isEditMode = false;
                        
                        console.log('Setting up keyboard navigation for grid');
                        
                        // Function to update edit mode state from server
                        grid.updateEditMode = function(editMode) {
                            isEditMode = editMode;
                            console.log('Edit mode updated to:', isEditMode);
                        };
                        
                        // Function to get task ID from a row element
                        function getTaskIdFromRow(row) {
                            if (!row) return null;
                            const cells = row.querySelectorAll('vaadin-grid-cell-content');
                            // The first column (index 0) is the hidden Identifier column with actual task ID
                            if (cells.length > 0) {
                                const identifierText = cells[0].textContent?.trim();
                                console.log('Task ID from identifier column (index 0):', identifierText);
                                return identifierText || null;
                            }
                            return null;
                        }
                        
                        // Function to check if we're in a text input
                        function isInTextInput(element) {
                            if (!element) return false;
                        
                            // Check for input elements
                            if (element.tagName === 'INPUT') return true;
                        
                            // Check for Vaadin components
                            const vaadinComponents = ['VAADIN-TEXT-FIELD', 'VAADIN-COMBO-BOX', 'VAADIN-DATE-TIME-PICKER'];
                            if (vaadinComponents.includes(element.tagName)) return true;
                        
                            // Check if inside a shadow DOM of a Vaadin component
                            if (element.getRootNode && element.getRootNode() !== document) {
                                const host = element.getRootNode().host;
                                if (host && vaadinComponents.includes(host.tagName)) return true;
                            }
                        
                            return false;
                        }
                        
                        // Add event listener with higher priority (capture phase)
                        grid.addEventListener('keydown', function(e) {
                            console.log('Keydown event:', e.key, 'Edit mode:', isEditMode);
                        
                            // Only handle Tab key
                            if (e.key !== 'Tab') return;
                        
                            console.log('Tab key pressed, shift:', e.shiftKey);
                        
                            const activeElement = grid.getRootNode().activeElement || document.activeElement;
                            console.log('Active element:', activeElement?.tagName, activeElement);
                        
                            const isInInput = isInTextInput(activeElement);
                            console.log('Is in input field:', isInInput);
                        
                            // If in edit mode and NOT in an input field, handle indent/outdent
                            if (isEditMode && !isInInput) {
                                console.log('>>> Handling indent/outdent');
                                e.preventDefault();
                                e.stopPropagation();
                        
                                // Find the current row - check multiple ways
                                let currentRow = null;
                        
                                // Try to find row from active element
                                if (activeElement) {
                                    currentRow = activeElement.closest('tr');
                                    console.log('Found row from activeElement:', currentRow ? 'yes' : 'no');
                                }
                        
                                // If not found, try to get the focused row from grid
                                if (!currentRow) {
                                    const focusedCell = grid.shadowRoot?.querySelector('[part~="focused-cell"]');
                                    console.log('Focused cell from shadowRoot:', focusedCell);
                                    if (focusedCell) {
                                        currentRow = focusedCell.closest('tr');
                                        console.log('Found row from focused cell:', currentRow ? 'yes' : 'no');
                                    }
                                }
                        
                                // Try another approach - get active item from grid
                                if (!currentRow && grid.activeItem) {
                                    console.log('Grid has active item:', grid.activeItem);
                                    // Try to find the row by data
                                    const allRows = grid.shadowRoot.querySelectorAll('tr');
                                    console.log('Total rows found:', allRows.length);
                                }
                        
                                if (!currentRow) {
                                    console.log('!!! No current row found for indent/outdent');
                                    return;
                                }
                        
                                const taskId = getTaskIdFromRow(currentRow);
                                console.log('Task ID from row:', taskId);
                        
                                if (!taskId) {
                                    console.log('!!! No task ID found for row');
                                    return;
                                }
                        
                                console.log('>>> Dispatching event - task ID:', taskId, 'shift:', e.shiftKey);
                        
                                // Dispatch custom event to server
                                if (e.shiftKey) {
                                    // Shift+Tab = Outdent
                                    grid.dispatchEvent(new CustomEvent('outdent-task', {
                                        detail: { taskId: taskId }
                                    }));
                                    console.log('>>> Outdent event dispatched');
                                } else {
                                    // Tab = Indent
                                    grid.dispatchEvent(new CustomEvent('indent-task', {
                                        detail: { taskId: taskId }
                                    }));
                                    console.log('>>> Indent event dispatched');
                                }
                                return;
                            }
                        
                            // If in an input field, handle cell navigation
                            if (!isInInput) {
                                console.log('Not in input field and not in edit mode or other condition not met');
                                return;
                            }
                        
                            console.log('>>> Handling cell navigation');
                        
                            // Find current cell position
                            let currentCell = activeElement.closest('vaadin-grid-cell-content');
                            if (!currentCell) {
                                currentCell = activeElement.getRootNode().host?.closest('vaadin-grid-cell-content');
                            }
                            if (!currentCell) {
                                console.log('No current cell found for navigation');
                                return;
                            }
                        
                            // Handle Tab key (move to next field)
                            if (e.key === 'Tab' && !e.shiftKey) {
                                e.preventDefault();
                                const next = currentCell.parentElement?.nextElementSibling?.querySelector('input, vaadin-combo-box, vaadin-date-time-picker');
                                if (next) {
                                    setTimeout(() => {
                                        if (next.tagName === 'VAADIN-COMBO-BOX' || next.tagName === 'VAADIN-DATE-TIME-PICKER') {
                                            next.focus();
                                        } else {
                                            next.focus();
                                            next.select();
                                        }
                                    }, 10);
                                } else {
                                    // Move to next row, first editable column
                                    const row = currentCell.closest('tr');
                                    const nextRow = row?.nextElementSibling;
                                    if (nextRow) {
                                        const firstInput = nextRow.querySelector('input, vaadin-combo-box, vaadin-date-time-picker');
                                        if (firstInput) {
                                            setTimeout(() => {
                                                if (firstInput.tagName === 'VAADIN-COMBO-BOX' || firstInput.tagName === 'VAADIN-DATE-TIME-PICKER') {
                                                    firstInput.focus();
                                                } else {
                                                    firstInput.focus();
                                                    firstInput.select();
                                                }
                                            }, 10);
                                        }
                                    }
                                }
                            }
                        
                            // Handle Shift+Tab (move to previous field)
                            if (e.key === 'Tab' && e.shiftKey) {
                                e.preventDefault();
                                const prev = currentCell.parentElement?.previousElementSibling?.querySelector('input, vaadin-combo-box, vaadin-date-time-picker');
                                if (prev) {
                                    setTimeout(() => {
                                        if (prev.tagName === 'VAADIN-COMBO-BOX' || prev.tagName === 'VAADIN-DATE-TIME-PICKER') {
                                            prev.focus();
                                        } else {
                                            prev.focus();
                                            prev.select();
                                        }
                                    }, 10);
                                } else {
                                    // Move to previous row, last editable column
                                    const row = currentCell.closest('tr');
                                    const prevRow = row?.previousElementSibling;
                                    if (prevRow) {
                                        const inputs = prevRow.querySelectorAll('input, vaadin-combo-box, vaadin-date-time-picker');
                                        const lastInput = inputs[inputs.length - 1];
                                        if (lastInput) {
                                            setTimeout(() => {
                                                if (lastInput.tagName === 'VAADIN-COMBO-BOX' || lastInput.tagName === 'VAADIN-DATE-TIME-PICKER') {
                                                    lastInput.focus();
                                                } else {
                                                    lastInput.focus();
                                                    lastInput.select();
                                                }
                                            }, 10);
                                        }
                                    }
                                }
                            }
                        }, true);  // Use capture phase to intercept before other handlers
                        
                        console.log('Keyboard navigation setup complete');
                        """
        );
    }
}
