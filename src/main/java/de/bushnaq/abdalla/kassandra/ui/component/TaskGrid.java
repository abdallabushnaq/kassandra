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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.ui.dialog.DependencyDialog;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
public class TaskGrid extends Grid<Task> {
    public static final String               ASSIGNED_FIELD     = "-assigned-field";
    public static final String               MAX_ESTIMATE_FIELD = "-max-estimate-field";
    public static final String               MIN_ESTIMATE_FIELD = "-min-estimate-field";
    public static final String               NAME_FIELD         = "-name-field";
    public static final String               START_FIELD        = "-start-field";
    public static final String               TASK_GRID_PREFIX   = "task-grid-";
    private final       List<User>           allUsers           = new ArrayList<>();
    private final       TaskClipboardHandler clipboardHandler;
    private final       Clock                clock;
    private             String               dragMode;
    private             Task                 draggedTask;          // Track the currently dragged task
    private final       DateTimeFormatter    dtfymdhm           = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm");
    private             boolean              isCtrlKeyPressed   = false; // Track if Ctrl key is pressed during drop
    @Getter
    @Setter
    private             boolean              isEditMode         = false;// Edit mode state management
    private final       Locale               locale;
    @Getter
    private final       Set<Task>            modifiedTasks      = new HashSet<>();
    private final       ObjectMapper         objectMapper;
    @Setter
    private             Consumer<Task>       onPersistTask;
    @Setter
    private             Runnable             onSaveAllChangesAndRefresh;
    private             Sprint               sprint;
    private             List<Task>           taskOrder          = new ArrayList<>(); // Track current order in memory


    public TaskGrid(Clock clock, Locale locale, ObjectMapper objectMapper) {
        this.clock            = clock;
        this.locale           = locale;
        this.objectMapper     = objectMapper;
        this.clipboardHandler = new TaskClipboardHandler(
                this,
                objectMapper
        );

        setSelectionMode(SelectionMode.SINGLE);
        addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        createGridColumns();
        setupDragAndDrop();
        setupKeyboardNavigation();

        // Add borders between columns
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        // Set height to auto instead of 100% to allow grid to take only needed space
        setHeight("auto");
        setAllRowsVisible(true);


        // Add borders between columns
        addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_NO_BORDER, com.vaadin.flow.component.grid.GridVariant.LUMO_NO_ROW_BORDERS);
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

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
        {
            addComponentColumn(task -> {
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
            addColumn(Task::getKey).setHeader("Key").setAutoWidth(true);
        }

        //ID
        {
//            Grid.Column<Task> id = grid.addColumn(Task::getOrderId).setHeader("ID").setAutoWidth(true);
//            id.setId("task-grid-id-column");
            addColumn(Task::getOrderId).setHeader("#").setAutoWidth(true).setId("task-grid-#-column");
        }
        //Dependency
        {
            addColumn(new ComponentRenderer<>(task -> {
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
            addColumn(task -> task.getParentTask() != null ? task.getParentTask().getOrderId() : "").setHeader("Parent").setAutoWidth(true);
        }
        //name - Editable for all task types, with icon on the left
        {
            Grid.Column<Task> nameColumn = addColumn(new ComponentRenderer<>(task -> {
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
                    nameField.setId(TASK_GRID_PREFIX + task.getName() + NAME_FIELD);
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
                    div.setId(TASK_GRID_PREFIX + task.getName());
                    container.add(div);
                    container.setFlexGrow(1, div);
                }

                return container;
            })).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
            nameColumn.setId("task-grid-name-column");
        }
        //Start - Editable only for Milestone tasks
        {
            Grid.Column<Task> startColumn = addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode && task.isMilestone()) {
                    // Editable for Milestone tasks
                    DateTimePicker startField = new DateTimePicker();
                    startField.setValue(task.getStart() != null ? task.getStart() : LocalDateTime.now());
                    startField.setWidthFull();
                    startField.setId(TASK_GRID_PREFIX + task.getName() + START_FIELD);

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
            addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    ComboBox<User> userComboBox = new ComboBox<>();
                    userComboBox.setId(TASK_GRID_PREFIX + task.getName() + ASSIGNED_FIELD);
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
            addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    TextField estimateField = new TextField();
                    estimateField.setId(TASK_GRID_PREFIX + task.getName() + MIN_ESTIMATE_FIELD);
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
            addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    TextField estimateField = new TextField();
                    estimateField.setId(TASK_GRID_PREFIX + task.getName() + MAX_ESTIMATE_FIELD);
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
     * Create a new Task with default estimates
     */
    public void createTask(User loggedInUser) {
        long nextOrderId = sprint.getNextOrderId();

        Task task = new Task();
        task.setName("New Task-" + nextOrderId);
//        task.setSprint(sprint);
//        task.setSprintId(sprint.getId());
//        sprint.addTask(task);
        Duration work = Duration.ofHours(7).plus(Duration.ofMinutes(30));
        task.setMinEstimate(work);
        task.setOriginalEstimate(work);
        task.setRemainingEstimate(work);
//        taskOrder.add(task);
//        indentTask(task);
        assignUserToNewTask(task, loggedInUser);
//        onSaveAllChangesAndRefresh.run();
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

        // Refresh grid to show updated hierarchy
//        getDataProvider().refreshAll();
    }

    private boolean isEligibleMoveTarget(Task dropTargetTask, Task draggedTask) {
        if (draggedTask.isTask()) {
            return dropTargetTask.isTask();
        } else if (draggedTask.isMilestone()) {
            return dropTargetTask.isStory();
        } else if (draggedTask.isStory()) {
            return dropTargetTask.isStory();
        }
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
                && !dependent.isDescendantOf(predecessor); // Not an ancestor (would create cycle)

        if (!isEligible) {
            if (predecessor.getId().equals(dependent.getId())) {
                log.debug("Cannot create dependency: task {} cannot depend on itself", dependent.getKey());
            } else if (predecessor.isDescendantOf(dependent)) {
                log.debug("Cannot create dependency: {} is a descendant of {}", predecessor.getKey(), dependent.getKey());
            } else if (dependent.isDescendantOf(predecessor)) {
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

    private void moveToNewParent(Task task, Task newStory) {
        // Remove from current parent if any
        if (task.getParentTask() != null) {
            task.getParentTask().removeChildTask(task);
        }

        //what is the orderId of the last child of the new parent?
        Task lastChild = newStory.getChildTasks().getLast();
        if (lastChild == null)
            lastChild = newStory;
        // Add to new parent
        newStory.addChildTask(task);
        markTaskAsModified(task);
        markTaskAsModified(newStory);
        moveTaskAfter(task, lastChild);//zero based index
//        onSaveAllChangesAndRefresh.run();
        // Refresh grid to show updated hierarchy
//        getDataProvider().refreshAll();
    }

    /**
     * Open a dialog to edit task dependencies
     */
    private void openDependencyEditor(Task task) {
        DependencyDialog dialog = new DependencyDialog(task, sprint, taskOrder, (t, selectedTaskIds) -> {
            // Mark task as modified
            markTaskAsModified(t);

            // Refresh grid
            getDataProvider().refreshAll();
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

        // Refresh grid to show updated hierarchy
//        getDataProvider().refreshAll();
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
                            window.addEventListener('keydown', (e) => {
                                if ((e.key === 'Control' || e.key === 'Meta')) {
                                    // Ignore auto-repeat: e.repeat true means key is held
                                    if (e.repeat) return;
                                    if (!grid.__ctrlKeyPressed) {
                                        grid.__ctrlKeyPressed = true;
                                        send(true);
                                    }
                                }
                            }, true);
                            window.addEventListener('keyup', (e) => {
                                if ((e.key === 'Control' || e.key === 'Meta')) {
                                    if (grid.__ctrlKeyPressed) {
                                        grid.__ctrlKeyPressed = false;
                                        send(false);
                                    }
                                }
                            }, true);
                        }
                        // Allow drop events to fire (HTML5 DnD spec requires canceling dragover)
                        grid.addEventListener('dragover', (e) => { e.preventDefault(); });
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
//                log.info("starting dependency drag mode");
                dragMode = "dependency";
                setDropMode(GridDropMode.ON_TOP); // dependency mode
            } else {
//                log.info("starting reorder drag mode");
                dragMode = "reorder";
                setDropMode(GridDropMode.BETWEEN); // reorder mode
            }
        });

        setDropFilter(dropTargetTask -> {
            log.trace("DropFilter {} {} {}", draggedTask.getKey(), dropTargetTask.getKey(), dragMode);
            if (isEditMode || draggedTask == null || dragMode == null) return false;
            switch (dragMode) {
                case "dependency":
                    return isEligiblePredecessor(dropTargetTask, draggedTask);
                case "reorder": {
                    log.trace("DropFilter {}", isEligibleMoveTarget(dropTargetTask, draggedTask));
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
            log.trace("DropListener {} {}", draggedTask.getKey(), dragMode);
            if (isEditMode || draggedTask == null || dragMode == null) return;

            Task             dropTargetTask = event.getDropTargetItem().orElse(null);
            GridDropLocation dropLocation   = event.getDropLocation();

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
                            Task lastChild = null;
                            for (Task child : new LinkedList<>(draggedTask.getChildTasks())) {
                                if (lastChild == null)
                                    moveTaskAfter(child, draggedTask);
                                else
                                    moveTaskAfter(child, lastChild);
                                lastChild = child;
                            }
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
            draggedTask      = null; // Clear reference when drag ends without drop
            isCtrlKeyPressed = false; // Reset modifier key state
            setDropMode(null);
            dragMode = null;
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
                                    const selectedItems = grid.selectedItems;
                                    console.log('selectedItems:', selectedItems);
                        
                                    if (selectedItems && selectedItems.length > 0) {
                                        console.warn('--- items selected, count:', selectedItems.length);
                        
                                        // Get the first selected item
                                        const selectedItem = selectedItems[0];
                                        console.log('selectedItem:', selectedItem);
                        
                                        // Extract task key from col2 (format: "T-{ID}")
                                        const taskKey = selectedItem.col2;
                                        console.log('taskKey from col2:', taskKey);
                        
                                        if (taskKey && taskKey.startsWith('T-')) {
                                            // Extract ID from "T-{ID}" format
                                            const taskId = taskKey.substring(2);
                                            console.log('Extracted task ID:', taskId);
                        
                                            if (taskId) {
                                                console.warn('--- dispatching copy event with taskId:', taskId);
                                                // Dispatch copy event to server
                                                grid.dispatchEvent(new CustomEvent('copy-task', {
                                                    detail: { taskId: taskId }
                                                }));
                                                e.preventDefault();
                                            } else {
                                                console.warn('Could not parse task ID from key:', taskKey);
                                            }
                                        } else {
                                            console.warn('Task key not in expected format (T-{ID}):', taskKey);
                                        }
                                    } else {
                                        console.log('No items selected');
                                    }
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
                    log.warn("Invalid task ID for indent operation: {}", taskIdStr);
                }
            }
        }).addEventData("event.detail.taskId");

        getElement().addEventListener("outdent-task", event -> {
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
                    log.warn("Invalid task ID for outdent operation: {}", taskIdStr);
                }
            }
        }).addEventData("event.detail.taskId");

        // Register server-side event listener for copy (Ctrl+C)
        getElement().addEventListener("copy-task", event -> {
            String taskIdStr = event.getEventData().getString("event.detail.taskId");
            if (taskIdStr != null && !taskIdStr.isEmpty()) {
                try {
                    Long taskId = Long.parseLong(taskIdStr);
                    Task task = taskOrder.stream()
                            .filter(t -> t.getId().equals(taskId))
                            .findFirst()
                            .orElse(null);
                    if (task != null && !isEditMode) {
                        clipboardHandler.handleCopy(task);
                    }
                } catch (NumberFormatException ex) {
                    log.warn("Invalid task ID for copy operation: {}", taskIdStr);
                }
            }
        }).addEventData("event.detail.taskId");

        // Register server-side event listener for paste (Ctrl+V)
        getElement().addEventListener("paste-task", event -> {
            String clipboardData = event.getEventData().getString("event.detail.clipboardData");
            if (clipboardData != null && !clipboardData.isEmpty() && !isEditMode) {
                clipboardHandler.handlePaste(clipboardData);
            }
        }).addEventData("event.detail.clipboardData");

    }

    public void updateData(Sprint sprint, List<Task> taskOrder, List<User> allUsers) {
        this.sprint    = sprint;
        this.taskOrder = taskOrder;
        this.allUsers.clear();
        this.allUsers.addAll(allUsers);
        setItems(taskOrder);
    }


}

