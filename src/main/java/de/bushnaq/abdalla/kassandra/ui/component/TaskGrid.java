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

package de.bushnaq.abdalla.kassandra.ui.component;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.ui.dialog.DependencyDialog;
import de.bushnaq.abdalla.util.ColorUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class TaskGrid extends TreeGrid<Task> {
    public static final String                       TASK_GRID_ASSIGNED_PREFIX   = "task-grid-assigned-";
    public static final String                       TASK_GRID_DEPENDENCY_PREFIX = "task-grid-dependency-";
    public static final String                       TASK_GRID_ID_PREFIX         = "task-grid-id-";
    public static final String                       TASK_GRID_KEY_PREFIX        = "task-grid-key-";
    public static final String                       TASK_GRID_MAX_EST_PREFIX    = "task-grid-max-est-";
    public static final String                       TASK_GRID_MIN_EST_PREFIX    = "task-grid-min-est-";
    public static final String                       TASK_GRID_NAME_PREFIX       = "task-grid-name-";
    public static final String                       TASK_GRID_PARENT_PREFIX     = "task-grid-parent-";
    public static final String                       TASK_GRID_START_PREFIX      = "task-grid-start-";
    private final       List<User>                   allUsers                    = new ArrayList<>();
    private final       TaskClipboardHandler         clipboardHandler;
    private final       Clock                        clock;
    @Setter
    private             CrossGridDragDropCoordinator dragDropCoordinator; // Coordinator for cross-grid drag & drop
    private             String                       dragMode;
    private             Task                         draggedTask;          // Track the currently dragged task
    private final       DateTimeFormatter            dtfymdhm                    = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm");
    /**
     * -- SETTER --
     * Set whether items should be expanded initially when data is loaded.
     * Must be called before updateData() to take effect.
     *
     * @param expandInitially true to expand all items initially, false to keep them collapsed
     */
    @Setter
    private             boolean                      expandInitially             = true; // Control whether to expand all items on first load
    private final       Set<Long>                    expandedTaskIds             = new HashSet<>(); // Track expanded task IDs for state preservation
    private             String                       externalDragMode;     // Drag mode from external grid
    private             TaskGrid                     externalDragSource;   // Source grid for cross-grid drags
    private             Task                         externalDraggedTask;  // Task being dragged from another grid
    private             boolean                      isCtrlKeyPressed            = false; // Track if Ctrl key is pressed during drop
    @Getter
    @Setter
    private             boolean                      isEditMode                  = false;// Edit mode state management
    private             boolean                      isFirstLoad                 = true; // Track if this is the first data load for current sprint
    private final       JsonMapper                   jsonMapper;
    private final       Locale                       locale;
    @Getter
    private final       Set<Task>                    modifiedTasks               = new HashSet<>();
    @Setter
    private             Runnable                     onSaveAllChangesAndRefresh;
    @Getter
    private             Sprint                       sprint;
    @Getter
    private             List<Task>                   taskOrder                   = new ArrayList<>(); // Track current order in memory


    public TaskGrid(Clock clock, Locale locale, JsonMapper jsonMapper) {
        this.clock            = clock;
        this.locale           = locale;
        this.jsonMapper       = jsonMapper;
        this.clipboardHandler = new TaskClipboardHandler(
                this,
                jsonMapper
        );

        setSelectionMode(SelectionMode.SINGLE);
        addClassName("task-grid"); // Add CSS class for styling
        addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        createGridColumns();
        setupDragAndDrop();
        setupKeyboardNavigation();
        setupExpansionListeners();

        // Set height to auto instead of 100% to allow grid to take only needed space
        setHeight("auto");
        setAllRowsVisible(true);


    }

    /**
     * Add expansion state for a task being received from another grid.
     *
     * @param taskId The ID of the task being received
     */
    public void addExpansionState(Long taskId) {
        expandedTaskIds.add(taskId);
    }

    public void addTask(Task task) {
        task.setSprint(sprint);
        task.setSprintId(sprint.getId());
        sprint.addTask(task);
        taskOrder.add(task);
        if (!task.isStory()) {
            indentTask(task);
        } else {
            markTaskAsModified(task);
        }
        onSaveAllChangesAndRefresh.run();
    }

    /**
     * Recursively add a task and its children to a flat list.
     */
    private void addTaskAndChildren(List<Task> list, Task task) {
        list.add(task);
        for (Task child : task.getChildTasks()) {
            addTaskAndChildren(list, child);
        }
    }

    /**
     * Assign a user to a newly created task.
     * First tries to find a task above in the ordered list with an assigned user.
     * If not found, assigns the currently logged-in user.
     */
    private void assignUserToNewTask(Task newTask, User loggedInUser) {
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
            if (loggedInUser != null) {
                assignedUserId = loggedInUser.getId();
            }
        }

        // Assign the user to the new task
        if (assignedUserId != null) {
            newTask.setResourceId(assignedUserId);
        }
    }

    /**
     * Rebuild the flat taskOrder list from the hierarchical tree structure in depth-first order.
     * This ensures taskOrder matches the visual tree order.
     */
    private List<Task> buildFlatOrderFromTree() {
        List<Task> flatList = new ArrayList<>();
        List<Task> rootTasks = taskOrder.stream()
                .filter(t -> t.getParentTask() == null)
                .sorted(Comparator.comparingInt(Task::getOrderId))
                .collect(Collectors.toList());

        for (Task root : rootTasks) {
            addTaskAndChildren(flatList, root);
        }
        return flatList;
    }

    private void createGridColumns() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(clock.getZone()).withLocale(getLocale());

        // Hidden Identifier column - contains the actual task ID for JavaScript logic
        {
            Grid.Column<Task> identifierColumn = addColumn(task -> task.getId() != null ? task.getId().toString() : "")
                    .setHeader("Identifier")
                    .setAutoWidth(true);
            identifierColumn.setVisible(false);
            identifierColumn.setKey("identifier");
            identifierColumn.setId("task-grid-id-column");
        }

        // Order Column with Up/Down arrows - visible only in edit mode
//        {
//            addComponentColumn((Task task) -> {
//                if (isEditMode) {
//                    // Create drag handle icon (burger menu)
//                    com.vaadin.flow.component.icon.Icon dragIcon = VaadinIcon.MENU.create();
//                    dragIcon.getStyle()
//                            .set("cursor", "grab")
//                            .set("color", "var(--lumo-secondary-text-color)");
//
//                    Div dragHandle = new Div(dragIcon);
//                    dragHandle.getStyle()
//                            .set("display", "flex")
//                            .set("align-items", "center")
//                            .set("justify-content", "center");
//                    dragHandle.setTitle("Drag to reorder");
//
//                    return dragHandle;
//                } else {
//                    return new Div(); // Empty div when not in edit mode
//                }
//            }).setHeader("").setAutoWidth(true).setWidth("50px");
//        }

        // User Color Indicator Column - shows colored bar for assigned user
        {
            Grid.Column<Task> colorColumn = addComponentColumn((Task task) -> {
                HorizontalLayout wrapper = new HorizontalLayout();
                if (task.isTask()) {
                    wrapper.getElement().getStyle()
                            .set("background-color", "transparent")
                            .set("width", "14px")
                            .set("height", "32px")
                            .set("flex-shrink", "0")
                            .set("row-gap", "0px")
                            .set("--_lumo-grid-border-width", "0px")
                            .set("gap", "0px");
                    Div spacer = new Div();
                    spacer.getElement().getStyle()
                            .set("width", "10px")
                            .set("height", "32px")
                            .set("background-color", "transparent")
                            .set("flex-shrink", "0");
                    wrapper.add(spacer);
                } else {
                    wrapper.getElement().getStyle()
                            .set("background-color", "var(--lumo-base-color)")
                            .set("width", "14px")
                            .set("height", "32px")
                            .set("flex-shrink", "0")
                            .set("row-gap", "0px")
                            .set("--_lumo-grid-border-width", "0px")
                            .set("gap", "0px");
                }

                // Create a colored vertical bar
                Div    colorBar  = new Div();
                String userColor = getUserColor(task);

                colorBar.getStyle()
                        .set("background", "var(--lumo-base-color)")
                        .set("border-left", "4px solid " + userColor) // 4px colored left border
                        .set("border-radius", "var(--lumo-border-radius-m)")
                        .set("cursor", "grab")
                        .set("transition", "all 0.2s ease")
                        .set("min-height", "32px")
                        .set("flex-shrink", "0")
                        .set("box-sizing", "border-box");

                // Add tooltip with user name if assigned
                if (task.getResourceId() != null && sprint != null) {
                    try {
                        User user = sprint.getuser(task.getResourceId());
                        if (user != null) {
                            colorBar.getElement().setProperty("title", "Assigned to: " + user.getName());
                        }
                    } catch (Exception e) {
                        // User not found
                    }
                }
                wrapper.add(colorBar);
                return wrapper;
            }).setHeader("").setWidth("14px").setFlexGrow(0);
            colorColumn.setKey("user-color");
            colorColumn.setId("task-grid-user-color-column");
            colorColumn.setFrozen(true); // Keep this column fixed on the left
        }

        //name - Editable for all task types, with icon on the left and key integrated
        {
            Grid.Column<Task> nameColumn = addComponentHierarchyColumn((Task task) -> {
                // Create container for icon + key + name (TreeGrid handles indentation automatically)
                HorizontalLayout container = new HorizontalLayout();
                container.setSpacing(false);
                container.setPadding(false);
                container.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
                container.setWidthFull(); // Ensure full width for proper border display
                // Store task ID as element property for JavaScript access
                container.getElement().setProperty("taskId", task.getId().toString());


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

                // Add task key (bold, small, gray) - similar to Backlog style
                Span keySpan = new Span(task.getKey());
                keySpan.setId(TASK_GRID_KEY_PREFIX + task.getName());
                keySpan.getStyle()
                        .set("font-weight", "bold")
                        .set("font-size", "var(--lumo-font-size-xs)")
                        .set("color", "#9E9E9E") // Match Backlog gray color
                        .set("white-space", "nowrap")
                        .set("margin-right", "var(--lumo-space-s)")
                        .set("flex-shrink", "0");
                container.add(keySpan);

                // Add name field or text
                if (isEditMode) {
                    TextField nameField = new TextField();
                    nameField.setId(TASK_GRID_NAME_PREFIX + task.getName());
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
                    div.getStyle()
                            .set("overflow", "hidden")
                            .set("text-overflow", "ellipsis")
                            .set("white-space", "nowrap");
                    container.add(div);
                    container.setFlexGrow(1, div);
                }

                return container;
            }).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
            nameColumn.setId("task-grid-name-column");
        }

        //ID
//        {
//            addColumn(new ComponentRenderer<>((Task task) -> {
//                Div div = new Div();
//                div.setText(String.valueOf(task.getOrderId()));
//                div.setId(TASK_GRID_ID_PREFIX + task.getName());
//                return div;
//            })).setHeader("#").setAutoWidth(true).setId("task-grid-#-column");
//        }
        //Dependency
        {
            addColumn(new ComponentRenderer<>((Task task) -> {
                if (isEditMode) {
                    // Editable - show current dependencies as text with an edit button
                    HorizontalLayout container = new HorizontalLayout();
                    container.setId(TASK_GRID_DEPENDENCY_PREFIX + task.getName());
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
                    div.getStyle()
                            .set("font-weight", "bold")
                            .set("font-size", "var(--lumo-font-size-xs)")
                            .set("color", "#9E9E9E") // Match Backlog gray color
                            .set("white-space", "nowrap")
                            .set("margin-right", "var(--lumo-space-s)")
                            .set("flex-shrink", "0");
                    div.setId(TASK_GRID_DEPENDENCY_PREFIX + task.getName());
                    div.setText(getDependencyText(task));
                    return div;
                }
            })).setHeader("Dependency").setAutoWidth(true);
        }
        //Parent
        {
            addColumn(new ComponentRenderer<>((Task task) -> {
                Div div = new Div();
                div.getStyle()
                        .set("font-weight", "bold")
                        .set("font-size", "var(--lumo-font-size-xs)")
                        .set("color", "#9E9E9E") // Match Backlog gray color
                        .set("white-space", "nowrap")
                        .set("margin-right", "var(--lumo-space-s)")
                        .set("flex-shrink", "0");
                div.setText(task.getParentTask() != null ? "T-" + task.getParentTask().getOrderId() : "");
                div.setId(TASK_GRID_PARENT_PREFIX + task.getName());
                return div;
            })).setHeader("Parent").setAutoWidth(true);
        }
        //Start - Editable only for Milestone tasks
        {
            Grid.Column<Task> startColumn = addColumn(new ComponentRenderer<>((Task task) -> {
                if (isEditMode && task.isMilestone()) {
                    // Editable for Milestone tasks
                    DateTimePicker startField = new DateTimePicker();
                    startField.setValue(task.getStart() != null ? task.getStart() : LocalDateTime.now());
                    startField.setWidthFull();
                    startField.setId(TASK_GRID_START_PREFIX + task.getName());

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
                    div.setId(TASK_GRID_START_PREFIX + task.getName());
                    if (task.isMilestone())
                        div.setText(task.getStart() != null ? DateUtil.createDateString(task.getStart(), dtfymdhm) : "");
                    else
                        div.setText("");
                    return div;
                }
            })).setHeader("Start").setAutoWidth(true);
        }

        //Min Estimate - Editable only for Task tasks
        {
            addColumn(new ComponentRenderer<>((Task task) -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    TextField estimateField = new TextField();
                    estimateField.setId(TASK_GRID_MIN_EST_PREFIX + task.getName());
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
                    div.setId(TASK_GRID_MIN_EST_PREFIX + task.getName());
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
            addColumn(new ComponentRenderer<>((Task task) -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    TextField estimateField = new TextField();
                    estimateField.setId(TASK_GRID_MAX_EST_PREFIX + task.getName());
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
                    div.setId(TASK_GRID_MAX_EST_PREFIX + task.getName());
                    if (task.isTask())
                        div.setText(!task.getMaxEstimate().equals(Duration.ZERO) ? DateUtil.createWorkDayDurationString(task.getMaxEstimate()) : "");
                    else
                        div.setText("");
                    return div;
                }
            })).setHeader("Max Estimate").setAutoWidth(true);
        }

        //Assigned - Editable only for Task tasks - with avatar image on the left
        {
            addColumn(new ComponentRenderer<>((Task task) -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    ComboBox<User> userComboBox = new ComboBox<>();
                    userComboBox.setId(TASK_GRID_ASSIGNED_PREFIX + task.getName());
                    userComboBox.setAllowCustomValue(false);
                    userComboBox.setClearButtonVisible(true);
                    userComboBox.setWidthFull();
                    userComboBox.setItemLabelGenerator(User::getName);

                    // Load ALL users from the system (not just sprint users) so we can assign new users
//                    List<User> allUsers = userApi.getAll();
                    userComboBox.setItems(allUsers);

                    // Set current value only if task has an assigned user
                    if (task.getResourceId() != null) {
                        try {
                            User currentUser = sprint.getuser(task.getResourceId());
                            if (currentUser != null && allUsers.contains(currentUser)) {
                                userComboBox.setValue(currentUser);
                            }
                        } catch (Exception ex) {
                            log.warn("Could not set user for task {}: {}", task.getKey(), ex.getMessage());
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
                    // Read-only for all tasks - show avatar + name
                    HorizontalLayout container = new HorizontalLayout();
                    container.setId(TASK_GRID_ASSIGNED_PREFIX + task.getName());
                    container.setSpacing(true);
                    container.setPadding(false);
                    container.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

                    if (task.isTask() && task.getResourceId() != null) {
                        try {
                            User user = sprint.getuser(task.getResourceId());
                            if (user != null) {
                                // Add avatar image
                                com.vaadin.flow.component.html.Image avatar = new com.vaadin.flow.component.html.Image();
                                avatar.setWidth("24px");
                                avatar.setHeight("24px");
                                avatar.getStyle()
                                        .set("border-radius", "4px")
                                        .set("object-fit", "cover")
                                        .set("display", "inline-block")
                                        .set("vertical-align", "middle")
                                        .set("flex-shrink", "0");
                                avatar.setSrc(user.getAvatarUrl());
                                avatar.setAlt(user.getName());
                                avatar.getElement().setProperty("title", user.getName());

                                // Add user name
                                Span userName = new Span(user.getName());
                                userName.getStyle()
                                        .set("overflow", "hidden")
                                        .set("text-overflow", "ellipsis")
                                        .set("white-space", "nowrap");

                                container.add(avatar, userName);
                            }
                        } catch (Exception ex) {
                            log.warn("Could not get user for task {}: {}", task.getKey(), ex.getMessage());
                        }
                    }

                    return container;
                }
            })).setHeader("Assigned").setAutoWidth(true);
        }

    }

    /**
     * Create a new Task with default estimates
     */
    public void createTask(User loggedInUser) {
        long nextOrderId = sprint.getNextOrderId();

        Task task = new Task();
        task.setName("New Task-" + nextOrderId);
        Duration work = Duration.ofHours(7).plus(Duration.ofMinutes(30));
        task.setMinEstimate(work);
        task.setRemainingEstimate(work);
        taskOrder.add(task);
        assignUserToNewTask(task, loggedInUser);
        addTask(task);
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

    /**
     * Force expand or collapse all stories.
     * This overrides the normal expansion state management.
     *
     * @param expand true to expand all stories, false to collapse all
     */
    public void forceExpandCollapseAll(boolean expand) {
        List<Task> stories = taskOrder.stream()
                .filter(Task::isStory)
                .collect(Collectors.toList());

        if (expand) {
            // Expand all stories
            expandRecursively(stories, Integer.MAX_VALUE);
            // Update tracking set
            stories.stream()
                    .map(Task::getId)
                    .forEach(expandedTaskIds::add);
        } else {
            // Collapse all stories
            collapseRecursively(stories, Integer.MAX_VALUE);
            // Update tracking set
            stories.stream()
                    .map(Task::getId)
                    .forEach(expandedTaskIds::remove);
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
                    return predecessor != null ? "T-" + predecessor.getOrderId() : "";
                })
                .filter(orderId -> !orderId.isEmpty())
                .collect(Collectors.joining(", "));
    }

    /**
     * Get the color for the assigned user of a task
     * Returns a color for the left border styling
     */
    private String getUserColor(Task task) {
        if (task.getResourceId() != null && sprint != null) {
            try {
                User user = sprint.getuser(task.getResourceId());
                if (user != null) {
                    // Use the user's configured color
                    return ColorUtil.colorToHexString(user.getColor());
                }
            } catch (Exception e) {
                // User not found, fall through to default
            }
        }
        // Default gray color for unassigned tasks or milestones/stories
        return "var(--lumo-contrast-20pct)";
    }

    /**
     * Handle dependency creation/removal when dropping a task ON_TOP of another task.
     * If dependency exists, it will be removed. Otherwise, it will be created.
     *
     * @param sourceTask The task being dragged (will become dependent on target)
     * @param targetTask The task being dropped onto (will become predecessor of source)
     */
    private void handleDependencyDrop(Task sourceTask, Task targetTask) {
        log.info("Handling dependency drop: {} onto {}", sourceTask.getKey(), targetTask.getKey());

        // Check if dependency already exists (target is already a predecessor of source)
        boolean dependencyExists = sourceTask.getPredecessors().stream()
                .filter(Relation::isVisible)
                .anyMatch(relation -> relation.getPredecessorId().equals(targetTask.getId()));

        if (dependencyExists) {
            // Remove the dependency
            sourceTask.getPredecessors().removeIf(relation ->
                    relation.isVisible() && relation.getPredecessorId().equals(targetTask.getId()));
            log.info("Removed dependency: {} no longer depends on {}", sourceTask.getKey(), targetTask.getKey());
        } else {
            // Add the dependency (target becomes predecessor of source)
            sourceTask.addPredecessor(targetTask, true); // true = visible
            log.info("Created dependency: {} now depends on {}", sourceTask.getKey(), targetTask.getKey());
        }

        // Mark the source task as modified
        markTaskAsModified(sourceTask);

        // Refresh grid to show updated dependencies
//        getDataProvider().refreshAll();
    }

    /**
     * Indent task - make it a child of the previous story (Tab key)
     */
    private void indentTask(Task task) {
        Task previousStory = findPreviousStory(task);
        if (previousStory == null) {
            log.debug("Cannot indent task {} - no valid parent found", task.getKey());
            return;
        }

        log.info("Indenting task {} to become child of {}", task.getKey(), previousStory.getKey());

        // Remove from current parent if any
        if (task.getParentTask() != null) {
            task.getParentTask().removeChildTask(task);
        }

        // Add to new parent
        previousStory.addChildTask(task);
        markTaskAsModified(task);
        markTaskAsModified(previousStory);

        // Refresh tree to show updated hierarchy
        refreshTreeData();
    }

    /**
     * Check if a task is eligible as a drop target for cross-grid move operations.
     * Used when dragging a task from another grid.
     *
     * @param dropTarget  The potential drop target task in this grid (can be null for empty grid)
     * @param draggedTask The task being dragged from another grid
     * @return true if the drop target is valid for cross-grid transfer
     */
    private boolean isEligibleCrossGridMoveTarget(Task dropTarget, Task draggedTask) {
        if (draggedTask == null) {
            return false;
        }
        // Allow drop on empty grid (dropTarget is null)
        if (dropTarget == null) {
            return true;
        }
        // Tasks can be dropped before/after any task or story
        if (draggedTask.isTask()) {
            return true;
        }
        // Stories can be dropped before/after other stories
        if (draggedTask.isStory()) {
            return dropTarget.isStory();
        }
        // Milestones can be dropped before/after stories or milestones
        if (draggedTask.isMilestone()) {
            return dropTarget.isStory() || dropTarget.isMilestone();
        }
        return false;
    }

    private boolean isEligibleMoveTarget(Task dropTargetTask, Task draggedTask) {
        if (draggedTask.isTask()) {
            return dropTargetTask.isTask();//task on task
        } else if (draggedTask.isMilestone()) {
            return dropTargetTask.isStory();//milestone on story
        } else if (draggedTask.isStory()) {
            return dropTargetTask.isStory();//story on story
        }
        log.info(dropTargetTask);
        return false;
    }

    private boolean isEligibleParent(Task newParent, Task task) {
        return newParent.isStory()
                && !task.equals(newParent)
                && !task.getParentTask().equals(newParent)
                && !newParent.isDescendantOf(task);
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
                && !predecessor.isAncestorOf(dependent); // Not an ancestor (would create cycle)

        if (!isEligible) {
            if (predecessor.getId().equals(dependent.getId())) {
                log.debug("Cannot create dependency: task {} cannot depend on itself", dependent.getKey());
            } else if (predecessor.isDescendantOf(dependent)) {
                log.debug("Cannot create dependency: {} is a descendant of {}", predecessor.getKey(), dependent.getKey());
            } else if (predecessor.isAncestorOf(dependent)) {
                log.debug("Cannot create dependency: {} is an ancestor of {}", predecessor.getKey(), dependent.getKey());
            }
        }

        return isEligible;
    }

    /**
     * Mark a task as modified
     */
    private void markTaskAsModified(Task task) {
        modifiedTasks.add(task);
//        log.debug("Task {} marked as modified. Total modified: {}", task.getKey(), modifiedTasks.size());
    }

    /// /        getDataProvider().refreshAll();
    /// /        onSaveAllChangesAndRefresh.run();
//        log.info("Task order updated. {} tasks marked as modified.", modifiedTasks.size());
//    }
    private void moveTaskAfter(Task task, Task after) {
        log.info("Moving task from index {} to after {}", task.getOrderId(), after.getOrderId());

        // Remove task from old position
        taskOrder.remove(task);

        // Insert at new position
        int targetIndex = taskOrder.indexOf(after);
        taskOrder.add(targetIndex + 1, task);

        // Recalculate orderIds for all tasks based on their new positions
        //TODO optimize - only update affected tasks
        for (int i = 0; i < taskOrder.size(); i++) {
            Task t = taskOrder.get(i);
            t.setOrderId(i);
            markTaskAsModified(t);
        }

        log.info("Task order updated. {} tasks marked as modified.", modifiedTasks.size());
    }

    private void moveTaskBefore(Task task, Task before) {
        log.info("Moving task from index {} to before {}", task.getOrderId(), before.getOrderId());

        // Remove task from old position
        taskOrder.remove(task);

        // Insert at new position
        int targetIndex = taskOrder.indexOf(before);
        taskOrder.add(targetIndex, task);

        // Recalculate orderIds for all tasks based on their new positions
        //TODO optimize - only update affected tasks
        for (int i = 0; i < taskOrder.size(); i++) {
            Task t = taskOrder.get(i);
            t.setOrderId(i);
            markTaskAsModified(t);
        }

        log.info("Task order updated. {} tasks marked as modified.", modifiedTasks.size());
    }

//    private void moveToNewParent(Task task, Task newStory) {
//        // Remove from current parent if any
//        if (task.getParentTask() != null) {
//            task.getParentTask().removeChildTask(task);
//        }
//
//        //what is the orderId of the last child of the new parent?
//        Task lastChild = newStory.getChildTasks().getLast();
//        if (lastChild == null)
//            lastChild = newStory;
//        // Add to new parent
//        newStory.addChildTask(task);
//        markTaskAsModified(task);
//        markTaskAsModified(newStory);
//        moveTaskAfter(task, lastChild);//zero based index
//
//        // Refresh tree to show updated hierarchy
//        refreshTreeData();
//    }

    /**
     * Called by the CrossGridDragDropCoordinator when a drag ends in another grid.
     * Clears the external drag state and resets drop mode if not in own drag.
     */
    public void onExternalDragEnd() {
        this.externalDragSource  = null;
        this.externalDraggedTask = null;
        this.externalDragMode    = null;

        // Only clear drop mode if we didn't start our own drag
        if (draggedTask == null) {
            setDropMode(null);
        }
        log.debug("External drag ended");
    }

    /**
     * Called by the CrossGridDragDropCoordinator when a drag starts in another grid.
     * Enables this grid to accept drops from the external source.
     *
     * @param task       The task being dragged from another grid
     * @param sourceGrid The grid where the drag originated
     * @param mode       The drag mode ("reorder" or "dependency")
     */
    public void onExternalDragStart(Task task, TaskGrid sourceGrid, String mode) {
        if (isEditMode) {
            return; // Don't accept external drags in edit mode
        }
        this.externalDragSource  = sourceGrid;
        this.externalDraggedTask = task;
        this.externalDragMode    = mode;

        // Enable drop mode for reorder operations (not dependency)
        if (!"dependency".equals(mode)) {
            // Use BETWEEN for precise positioning, but ON_GRID if the grid is empty
            if (taskOrder.isEmpty()) {
                setDropMode(GridDropMode.ON_GRID);
            } else {
                setDropMode(GridDropMode.BETWEEN);
            }
            log.debug("External drag started from another grid: task={}, mode={}, dropMode={}",
                    task != null ? task.getKey() : "null", mode, taskOrder.isEmpty() ? "ON_GRID" : "BETWEEN");
        }
    }

    /**
     * Open a dialog to edit task dependencies
     */
    private void openDependencyEditor(Task task) {
        DependencyDialog dialog = new DependencyDialog(task, sprint, taskOrder, (t, selectedTaskIds) -> {
            // Mark task as modified
            markTaskAsModified(t);

            // Refresh tree grid
            refreshTreeData();
        });
        dialog.open();
    }

    /**
     * Outdent task - remove it as a child from its parent (Shift+Tab key)
     */
    private void outdentTask(Task task) {
        if (task.getParentTask() == null) {
            log.debug("Cannot outdent task {} - it has no parent", task.getKey());
            return;
        }

        log.info("Outdenting task {} from parent {}", task.getKey(), task.getParentTask().getKey());

        Task oldParent = task.getParentTask();
        oldParent.removeChildTask(task);
        if (oldParent.getParentTask() != null)
            oldParent.getParentTask().addChildTask(task);
        markTaskAsModified(task);
        markTaskAsModified(oldParent);

        // Refresh tree to show updated hierarchy
        refreshTreeData();
    }

    /**
     * Refresh the tree data structure after hierarchy changes.
     * Preserves expansion state for all expanded tasks.
     */
    private void refreshTreeData() {
        // Build hierarchical tree structure using only tasks in taskOrder (respecting filters)
        TreeData<Task> treeData = new TreeData<>();

        // Create a set of task IDs that are in the filtered taskOrder for fast lookup
        Set<Long> filteredTaskIds = taskOrder.stream()
                .map(Task::getId)
                .collect(Collectors.toSet());

        // Get root tasks (stories/milestones) from the filtered list
        List<Task> rootTasks = taskOrder.stream()
                .filter(t -> t.getParentTask() == null)
                .sorted(Comparator.comparingInt(Task::getOrderId))
                .collect(Collectors.toList());

        // Add root tasks and their filtered children
        // We use a custom child provider that only returns children present in taskOrder
        treeData.addItems(rootTasks, task -> {
            if (task.getChildTasks() == null) {
                return Collections.emptyList();
            }
            // Only return child tasks that are in the filtered taskOrder
            return task.getChildTasks().stream()
                    .filter(child -> filteredTaskIds.contains(child.getId()))
                    .sorted(Comparator.comparingInt(Task::getOrderId))
                    .collect(Collectors.toList());
        });

        // Set the tree data
        setTreeData(treeData);

        // Restore expansion state - expand all by default or restore previous state
        if (isFirstLoad && expandInitially) {
            // First time for this sprint - expand all if configured
            expandRecursively(rootTasks, Integer.MAX_VALUE);
            // Record the expanded state (all stories)
            taskOrder.stream()
                    .filter(Task::isStory)
                    .map(Task::getId)
                    .forEach(expandedTaskIds::add);
            isFirstLoad = false;
        } else {
            // Not first load - restore previous expansion state
            if (!expandedTaskIds.isEmpty()) {
                // Restore expansion state for existing stories
                taskOrder.stream()
                        .filter(task -> expandedTaskIds.contains(task.getId()))
                        .forEach(this::expand);
            }

            // If expandInitially is true, expand NEW stories that aren't tracked yet
            if (expandInitially) {
                List<Task> newStories = taskOrder.stream()
                        .filter(Task::isStory)
                        .filter(task -> !expandedTaskIds.contains(task.getId()))
                        .collect(Collectors.toList());

                if (!newStories.isEmpty()) {
                    // Expand the new stories
                    expandRecursively(newStories, Integer.MAX_VALUE);
                    // Add them to the tracking set
                    newStories.stream()
                            .map(Task::getId)
                            .forEach(expandedTaskIds::add);
                }
            }
        }
    }

    /**
     * Transfer expansion state for a task being moved to another grid.
     * Call this when moving a Story between grids to preserve its expansion state.
     *
     * @param taskId The ID of the task being moved
     * @return true if the task was expanded in this grid (should be expanded in target)
     */
    public boolean removeAndReturnExpansionState(Long taskId) {
        return expandedTaskIds.remove(taskId);
    }

    /**
     * Called from client-side JavaScript to set the Alt key state during drop operations
     */
    @ClientCallable
    public void setCtrlKeyPressed(boolean ctrlKeyPressed) {
        this.isCtrlKeyPressed = ctrlKeyPressed;
        log.debug("Ctrl/Meta key state set to: {}", ctrlKeyPressed);
    }

    private void setupDragAndDrop() {
        // Enable row reordering with drag and drop in edit mode
        setRowsDraggable(true); // Will be enabled in edit mode

        // Setup JavaScript to capture Ctrl/Meta key state during drag operations
        getElement().executeJs(
                """
                        const grid = this;
                        
                        // Use Ctrl (Windows/Linux) and Meta (macOS) to toggle dependency drag mode.
                        if (!grid.__modifierKeyHandlersInstalled) {
                            grid.__modifierKeyHandlersInstalled = true;
                            grid.__ctrlKeyPressed = false; // local state to avoid repeat spam
                            const send = (pressed) => {
                                if (grid.$server && grid.$server.setCtrlKeyPressed) {
                                    grid.$server.setCtrlKeyPressed(pressed);
                                }
                            };
                            const syncState = (pressed) => {
                                if (grid.__ctrlKeyPressed === pressed) {
                                    return;
                                }
                                console.log('Syncing Ctrl key state to:', pressed);
                                grid.__ctrlKeyPressed = pressed;
                                send(pressed);
                            };
                            window.addEventListener('keydown', (e) => {
                                if ((e.key === 'Control' || e.key === 'Meta')) {
                                    if (e.repeat) return;
                                    console.log('Keydown: Control pressed');
                                    syncState(true);
                                }
                            }, true);
                            window.addEventListener('keyup', (e) => {
                                if ((e.key === 'Control' || e.key === 'Meta')) {
                                    console.log('Keyup: Control released');
                                    syncState(false);
                                }
                            }, true);
                            // Sync state at dragend to sync back to reality
                            // Use capture phase to catch the event before Vaadin consumes it
                            grid.addEventListener('dragend', (e) => {
                                console.log('Dragend: ctrlKey=', e.ctrlKey, 'metaKey=', e.metaKey);
                                syncState(e.ctrlKey || e.metaKey);
                            }, true);
                        }
                        """
        );

        /*
         * A drag filter function can be used to specify the rows that are available for dragging. The function receives an item and returns true if the row can be dragged, false otherwise.
         */
        setDragFilter(dragSourceTask ->
                {
//                    log.trace("DragFilter {}", isEditMode);
                    if (isEditMode) return false;

                    if (isCtrlKeyPressed) {
                        // dependency drag mode
                        //allow dragging stories and tasks
//                        log.trace("isCtrlKeyPressed {}", isCtrlKeyPressed);
                        return dragSourceTask != null && (dragSourceTask.isTask() || dragSourceTask.isStory()) && !isEditMode;
                    } else {
                        // reorder drag mode
                        return true;
                    }
                }
        );

        addDragStartListener(event -> {
//            log.trace("DragStartListener {} {}", event.getDraggedItems().isEmpty(), isCtrlKeyPressed);
            if (isEditMode || event.getDraggedItems().isEmpty()) return;

            draggedTask = event.getDraggedItems().getFirst();
            if (isCtrlKeyPressed) {
                log.info("starting dependency drag mode");
                dragMode = "dependency";
                setDropMode(GridDropMode.ON_TOP); // dependency mode
            } else {
                log.info("starting reorder drag mode");
                dragMode = "reorder";
                setDropMode(GridDropMode.BETWEEN); // reorder mode
            }

            // Notify coordinator about drag start for cross-grid support
            if (dragDropCoordinator != null) {
                dragDropCoordinator.notifyDragStart(this, draggedTask, dragMode);
            }
        });

        setDropFilter(dropTargetTask -> {
            // Check for external (cross-grid) drag first
            if (externalDraggedTask != null && externalDragSource != null) {
                // Cross-grid drops: only allow reorder mode, not dependency
                if ("dependency".equals(externalDragMode)) {
                    log.trace("Cross-grid dependency drop not allowed");
                    return false;
                }
                boolean eligible = isEligibleCrossGridMoveTarget(dropTargetTask, externalDraggedTask);
                log.trace("Cross-grid DropFilter: dropTarget={}, draggedTask={}, eligible={}",
                        dropTargetTask != null ? dropTargetTask.getKey() : "null",
                        externalDraggedTask.getKey(), eligible);
                return eligible;
            }

            // Same-grid drag logic
            if (isEditMode || draggedTask == null || dragMode == null) return false;
//            log.trace("DropFilter {} {} {}", draggedTask.getKey(), dropTargetTask.getKey(), dragMode);
            switch (dragMode) {
                case "dependency":
                    log.trace("dependency DropFilter {}", isEligiblePredecessor(dropTargetTask, draggedTask));
                    return isEligiblePredecessor(dropTargetTask, draggedTask);
                case "reorder": {
                    log.trace("reorder DropFilter {}", isEligibleMoveTarget(dropTargetTask, draggedTask));
                    return isEligibleMoveTarget(dropTargetTask, draggedTask);
//                    return false;
                }
            }
//            log.info("DropFilter {} {} {} {} {}", isEditMode, draggedTask == null, dropTargetTask == null, isEligibleParent(dropTargetTask, draggedTask), isEligiblePredecessor(dropTargetTask, draggedTask));
//            if (isEditMode || draggedTask == null || dropTargetTask == null) {
//                return true; // Allow drop if not in edit mode or no dragged task
//            }
            return false;

        });

        // Add drop listener for reordering and dependency management
        addDropListener(event -> {
            Task             dropTargetTask = event.getDropTargetItem().orElse(null);
            GridDropLocation dropLocation   = event.getDropLocation();

            // Check for cross-grid drop first
            if (dragDropCoordinator != null && dragDropCoordinator.isCrossGridDrag(this)) {
                log.info("Cross-grid drop detected: dropTarget={}, location={}",
                        dropTargetTask != null ? dropTargetTask.getKey() : "null (empty grid)", dropLocation);
                // Allow drop even if dropTargetTask is null (empty grid case)
                dragDropCoordinator.handleCrossGridDrop(this, dropTargetTask, dropLocation);
                // Clear external drag state
                externalDragSource  = null;
                externalDraggedTask = null;
                externalDragMode    = null;
                return;
            }

            // Same-grid drop logic
            if (isEditMode || draggedTask == null || dragMode == null) return;
            log.trace("DropListener {} {}", draggedTask.getKey(), dragMode);

            if (dropTargetTask == null)
                log.warn("Drop target task is null for dragged task {}", draggedTask.getKey());

            if (dropTargetTask != null && !draggedTask.equals(dropTargetTask)) {

                switch (dragMode) {
                    case "dependency": {
                        log.info("dropped {} on {}", draggedTask.getKey(), dropTargetTask.getKey());
                        // Check if Ctrl/Meta key is pressed to modify behavior
                        handleDependencyDrop(draggedTask, dropTargetTask);
                        onSaveAllChangesAndRefresh.run();
                    }
                    break;
                    case "reorder": {
                        if (draggedTask.isTask() || draggedTask.isMilestone()) {
                            log.info("dropped {} before {}", draggedTask.getKey(), dropTargetTask.getKey());

                            // Remove from old parent before moving
                            if (draggedTask.getParentTask() != null) {
                                Task oldParent = draggedTask.getParentTask();
                                oldParent.removeChildTask(draggedTask);
                                markTaskAsModified(oldParent);//TODO not needed?
                            }
                            //move the task
                            if (dropLocation == GridDropLocation.BELOW) {
                                log.info("dropped {} after {}", draggedTask.getKey(), dropTargetTask.getKey());
                                moveTaskAfter(draggedTask, dropTargetTask);
                            } else {
                                log.info("dropped {} before {}", draggedTask.getKey(), dropTargetTask.getKey());
                                moveTaskBefore(draggedTask, dropTargetTask);
                            }
                            // Try to re-parent the task based on its new position
                            if (draggedTask.isTask())
                                indentTask(draggedTask);
                            onSaveAllChangesAndRefresh.run();
                        } else if (draggedTask.isStory()) {

                            if (dropLocation == GridDropLocation.BELOW) {
                                // dragging a story below another story will move it after the last child of that story
                                log.info("dropped {} after {}", draggedTask.getKey(), dropTargetTask.getKey());
                                moveTaskAfter(draggedTask, dropTargetTask.getChildTasks().getLast());
                            } else {
                                log.info("dropped {} before {}", draggedTask.getKey(), dropTargetTask.getKey());
                                moveTaskBefore(draggedTask, dropTargetTask);
                            }
                            //move all children along with the story
//                            Task lastChild = null;
//                            for (Task child : new LinkedList<>(draggedTask.getChildTasks())) {
//                                if (lastChild == null)
//                                    moveTaskAfter(child, draggedTask);
//                                else
//                                    moveTaskAfter(child, lastChild);
//                                lastChild = child;
//                            }
                            onSaveAllChangesAndRefresh.run();
                        }
                    }
                    break;
                }
            }

            draggedTask      = null; // Clear the dragged task reference
            isCtrlKeyPressed = false; // Reset modifier key state
            dragMode         = null;
        });

        addDragEndListener(event -> {
            // Notify coordinator about drag end for cross-grid support
            if (dragDropCoordinator != null) {
                dragDropCoordinator.notifyDragEnd(this);
            }

            draggedTask      = null; // Clear reference when drag ends without drop
            isCtrlKeyPressed = false; // Reset modifier key state
            setDropMode(null);
            dragMode = null;
        });

    }

    /**
     * Setup listeners to track expansion state changes
     */
    private void setupExpansionListeners() {
        addExpandListener(event -> {
            event.getItems().stream()
                    .map(Task::getId)
                    .forEach(expandedTaskIds::add);
            log.debug("Task expanded: {}, total expanded: {}",
                    event.getItems().stream().map(Task::getKey).collect(Collectors.joining(", ")),
                    expandedTaskIds.size());
        });

        addCollapseListener(event -> {
            event.getItems().stream()
                    .map(Task::getId)
                    .forEach(expandedTaskIds::remove);
            log.debug("Task collapsed: {}, total expanded: {}",
                    event.getItems().stream().map(Task::getKey).collect(Collectors.joining(", ")),
                    expandedTaskIds.size());
        });
    }

    /**
     * Setup keyboard navigation for Excel-like behavior
     * Used by JS code
     */
    private void setupKeyboardNavigation() {
        // Setup JavaScript for keyboard shortcuts including copy/paste
        getElement().executeJs(
                """
                        const grid = this;
                        if (!grid.__keyboardHandlersInstalled) {
                            grid.__keyboardHandlersInstalled = true;
                        
                            // Add keyboard event listener for copy/paste
                            grid.addEventListener('keydown', (e) => {
                                // Handle Ctrl+C or Cmd+C (copy)
                                if ((e.ctrlKey || e.metaKey) && e.key === 'c') {
                                    console.warn('--- copy shortcut detected');
                                    grid.dispatchEvent(new CustomEvent('copy-task'));
                                    e.preventDefault();
                                }
                        
                                // Handle Ctrl+V or Cmd+V (paste)
                                if ((e.ctrlKey || e.metaKey) && e.key === 'v') {
                                    console.warn('--- past shortcut detected');
                                    // Read from clipboard
                                    navigator.clipboard.readText().then(text => {
                                        if (text) {
                                            // Dispatch paste event to server
                                            grid.dispatchEvent(new CustomEvent('paste-task', {
                                                detail: { clipboardData: text }
                                            }));
                                        }
                                    }).catch(err => {
                                        console.error('Failed to read clipboard:', err);
                                    });
                                    e.preventDefault();
                                }
                            });
                        }
                        """
        );

        // Register server-side event listeners for indent/outdent
        getElement().addEventListener("indent-task", event -> {
            String taskIdStr = event.getEventData().get("event.detail.taskId").asText();
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
                    log.warn("Invalid task ID for indent operation: {}", taskIdStr);
                }
            }
        }).addEventData("event.detail.taskId");

        getElement().addEventListener("outdent-task", event -> {
            String taskIdStr = event.getEventData().get("event.detail.taskId").asText();
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
                    log.warn("Invalid task ID for outdent operation: {}", taskIdStr);
                }
            }
        }).addEventData("event.detail.taskId");

        // Register server-side event listener for copy (Ctrl+C)
        getElement().addEventListener("copy-task", event -> {
            // Get the selected task from the grid's selection
            Set<Task> selectedItems = getSelectedItems();
            if (selectedItems != null && !selectedItems.isEmpty() && !isEditMode) {
                Task selectedTask = selectedItems.iterator().next();
                if (selectedTask != null) {
                    log.info("Copy operation detected for task: {} (ID: {})", selectedTask.getKey(), selectedTask.getId());
                    clipboardHandler.handleCopy(selectedTask);
                } else {
                    log.warn("Selected task is null");
                }
            } else {
                if (isEditMode) {
                    log.debug("Copy operation ignored - grid is in edit mode");
                } else {
                    log.debug("Copy operation detected but no task is selected");
                }
            }
        });

        // Register server-side event listener for paste (Ctrl+V)
        getElement().addEventListener("paste-task", event -> {
            String clipboardData = event.getEventData().get("event.detail.clipboardData").asText();
            if (clipboardData != null && !clipboardData.isEmpty() && !isEditMode) {
                clipboardHandler.handlePaste(clipboardData);
            }
        }).addEventData("event.detail.clipboardData");

    }

    public void updateData(Sprint sprint, List<Task> taskOrder, List<User> allUsers) {
        // Only clear expansion state when loading a DIFFERENT sprint
        boolean sprintChanged = this.sprint == null ||
                !Objects.equals(this.sprint.getId(), sprint.getId());

        this.sprint    = sprint;
        this.taskOrder = taskOrder;
        this.allUsers.clear();
        this.allUsers.addAll(allUsers);

        if (sprintChanged) {
            // Clear expansion state and reset first load flag for new sprint
            expandedTaskIds.clear();
            isFirstLoad = true;
        }

        // Build and display hierarchical tree structure
        refreshTreeData();
    }


}


