package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
    public static final String            TASK_GRID_NAME_PREFIX = "task-grid-name-";
    private final       List<User>        allUsers              = new ArrayList<>();
    private final       Clock             clock;
    private             Task              draggedTask;          // Track the currently dragged task
    private final       DateTimeFormatter dtfymdhm              = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm");
    private             boolean           isAltKeyPressed       = false; // Track if Alt key is pressed during drop
    @Getter
    @Setter
    private             boolean           isEditMode            = false;// Edit mode state management
    private final       Locale            locale;
    @Getter
    private final       Set<Task>         modifiedTasks         = new HashSet<>();
    @Setter
    private             Consumer<Task>    onPersistTask;
    @Setter
    private             Runnable          onSaveAllChangesAndRefresh;
    private             Sprint            sprint;
    private             List<Task>        taskOrder             = new ArrayList<>(); // Track current order in memory


    public TaskGrid(Clock clock, Locale locale) {
        this.clock  = clock;
        this.locale = locale;

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
//            String currentUserName = loginUser;
//            try {
//                User currentUser = userApi.getByName(currentUserName);
//                if (currentUser != null) {
//                    assignedUserId = currentUser.getId();
//                }
//            } catch (Exception e) {
//                log.warn("Could not find user with name: " + currentUserName, e);
//            }
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
            Grid.Column<Task> startColumn = addColumn(new ComponentRenderer<>(task -> {
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
            addColumn(new ComponentRenderer<>(task -> {
                if (isEditMode && task.isTask()) {
                    // Editable for Task tasks
                    ComboBox<User> userComboBox = new ComboBox<>();
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
    void createTask(User loggedInUser) {
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
        assignUserToNewTask(task, loggedInUser);
//        onPersistTask.accept(task);
        onSaveAllChangesAndRefresh.run();
//        loadData();
//        refreshGrid();
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
        getDataProvider().refreshAll();
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
        getDataProvider().refreshAll();
    }

    private boolean isEligibleParent(Task newParent, Task task) {
        return newParent.isStory()
                && !task.equals(newParent)
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
        log.debug("Task {} marked as modified. Total modified: {}", task.getKey(), modifiedTasks.size());
    }

    /**
     * Move a task to a new position and recalculate all orderIds
     */
    private void moveTask(int fromIndex, int toIndex) {
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 ||
                fromIndex >= taskOrder.size() || toIndex >= taskOrder.size()) {
            return;
        }

        log.info("Moving task from index {} to {}", fromIndex, toIndex);

        // Remove task from old position
        Task movedTask = taskOrder.remove(fromIndex);

        // Insert at new position
        taskOrder.add(toIndex, movedTask);

        // Recalculate orderIds for all tasks based on their new positions
        for (int i = 0; i < taskOrder.size(); i++) {
            Task task = taskOrder.get(i);
            task.setOrderId(i);
            markTaskAsModified(task);
        }

        // Refresh the grid to show new order
        getDataProvider().refreshAll();
        log.info("Task order updated. {} tasks marked as modified.", modifiedTasks.size());
    }

    private void moveToNewParent(Task task, Task newStory) {
        // Remove from current parent if any
        if (task.getParentTask() != null) {
            task.getParentTask().removeChildTask(task);
        }

        // Add to new parent
        newStory.addChildTask(task);
        markTaskAsModified(task);
        markTaskAsModified(newStory);
        moveTask(task.getOrderId(), newStory.getChildTasks().getLast().getOrderId() + 1);
        onSaveAllChangesAndRefresh.run();
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
        getDataProvider().refreshAll();
    }

    /**
     * Called from client-side JavaScript to set the Alt key state during drop operations
     */
    @ClientCallable
    public void setAltKeyPressed(boolean altKeyPressed) {
        this.isAltKeyPressed = altKeyPressed;
        log.debug("Alt key state set to: {}", altKeyPressed);
    }

    private void setupDragAndDrop() {
        // Enable row reordering with drag and drop in edit mode
        setRowsDraggable(true); // Will be enabled in edit mode

        // Setup JavaScript to capture Alt key state during drag operations
        getElement().executeJs(
                """
                        const grid = this;
                        
                        // Capture Alt key state during dragover
                        grid.addEventListener('dragover', (e) => {
                            grid._altKeyPressed = e.altKey;
                        });
                        
                        // Capture Alt key state during drop
                        grid.addEventListener('drop', (e) => {
                            grid._altKeyPressed = e.altKey;
                            // Send Alt key state to server
                            grid.$server.setAltKeyPressed(e.altKey);
                        });
                        """
        );

        // Add drop listener for reordering and dependency management
        addDropListener(event -> {
            if (isEditMode || draggedTask == null) return;

            Task dropTargetTask = event.getDropTargetItem().orElse(null);

            if (dropTargetTask != null && !draggedTask.equals(dropTargetTask)) {
                // Check drop location to determine action
                com.vaadin.flow.component.grid.dnd.GridDropLocation dropLocation = event.getDropLocation();

                if (dropLocation == com.vaadin.flow.component.grid.dnd.GridDropLocation.ON_TOP) {
                    // Check if Alt key is pressed to modify behavior
                    if (isAltKeyPressed) {
                        // Handle dependency creation/removal when dropping ON_TOP
                        handleDependencyDrop(draggedTask, dropTargetTask);
                    } else {
                        // handle parent change
                        if (isEligibleParent(dropTargetTask, draggedTask)) {
                            moveToNewParent(draggedTask, dropTargetTask);

                        } else {
                            log.info("Cannot change parent: {} is not eligible as parent for {}", dropTargetTask.getKey(), draggedTask.getKey());
                        }
                    }
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

            draggedTask     = null; // Clear the dragged task reference
            isAltKeyPressed = false; // Reset Alt key state
        });

        addDragStartListener(event -> {
            if (!isEditMode && !event.getDraggedItems().isEmpty()) {
                draggedTask = event.getDraggedItems().get(0);
                setDropMode(com.vaadin.flow.component.grid.dnd.GridDropMode.ON_TOP_OR_BETWEEN); // Enable drop on top or between rows
            }
        });

        addDragEndListener(event -> {
            draggedTask     = null; // Clear reference when drag ends without drop
            isAltKeyPressed = false; // Reset Alt key state
            setDropMode(null);
        });

        // Add drop filter to prevent invalid dependency creation
        setDropFilter(

                dropTargetTask -> {
                    log.info("{} {} {} {} {}", isEditMode, draggedTask == null, dropTargetTask == null, isEligibleParent(dropTargetTask, draggedTask), isEligiblePredecessor(dropTargetTask, draggedTask));
                    if (isEditMode || draggedTask == null || dropTargetTask == null) {
                        return true; // Allow drop if not in edit mode or no dragged task
                    }

                    // For ON_TOP drops, check if dependency creation is valid
                    // This uses the same eligibility logic as DependencyDialog
                    if (isAltKeyPressed) {
                        return isEligibleParent(dropTargetTask, draggedTask);
                    } else {
                        return isEligiblePredecessor(dropTargetTask, draggedTask);
                    }
                });
    }

    /**
     * Setup keyboard navigation for Excel-like behavior
     */
    private void setupKeyboardNavigation() {
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

        // Add keyboard navigation support for Tab key navigation between editable cells
        // and Tab/Shift+Tab for indent/outdent when focused on a row
        getElement().executeJs(
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

    public void updateData(Sprint sprint, List<Task> taskOrder, List<User> allUsers) {
        this.sprint    = sprint;
        this.taskOrder = taskOrder;
        this.allUsers.clear();
        this.allUsers.addAll(allUsers);
        setItems(taskOrder);
    }


}