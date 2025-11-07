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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Grid component for displaying and editing tasks in the TaskListView.
 * Extracted from TaskListView to reduce file size and improve maintainability.
 */
public class TaskGrid extends Grid<Task> {
    public static final String                    TASK_GRID_NAME_PREFIX = "task-grid-name-";
    private final       List<User>                allUsers;
    private final       Clock                     clock;
    private             Task                      draggedTask;
    private final       DateTimeFormatter         dtfymdhm              = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm");
    // State management
    private             boolean                   isEditMode            = false;
    private final       Locale                    locale;
    private final       Logger                    logger                = LoggerFactory.getLogger(this.getClass());
    private             BiConsumer<Task, Task>    onDependencyDrop;
    private             Consumer<Task>            onIndentTask;
    private             Consumer<Task>            onOutdentTask;
    private             BiConsumer<Task, Boolean> onTaskModified;
    private             Sprint                    sprint;
    private             List<Task>                taskOrder;

    public TaskGrid(Clock clock, Locale locale, List<User> allUsers) {
        this.clock    = clock;
        this.locale   = locale;
        this.allUsers = allUsers;

        addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        createGridColumns();
        setupDragAndDrop();
        setupKeyboardNavigation();

        // Add borders between columns
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        // Set height to auto instead of 100% to allow grid to take only needed space
        setHeight("auto");
        setAllRowsVisible(true);
    }

    private void createGridColumns() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(clock.getZone()).withLocale(locale);

        // Hidden Identifier column - contains the actual task ID for JavaScript logic
        {
            Column<Task> identifierColumn = addColumn(task -> task.getId() != null ? task.getId().toString() : "")
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
            Column<Task> nameColumn = addColumn(new ComponentRenderer<>(task -> {
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
            Column<Task> startColumn = addColumn(new ComponentRenderer<>(task -> {
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

                    // Use the cached list of all users
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
     * Get the dependency text for display (comma-separated orderIds of visible predecessors)
     */
    private String getDependencyText(Task task) {
        if (sprint == null) return "";

        List<Relation> relations = task.getPredecessors();
        if (relations == null || relations.isEmpty()) {
            return "";
        }

        return relations.stream()
                .filter(Relation::isVisible)
                .map(relation -> {
                    Task predecessor = sprint.getTaskById(relation.getPredecessorId());
                    return predecessor != null ? String.valueOf(predecessor.getOrderId()) : "";
                })
                .filter(orderId -> !orderId.isEmpty())
                .collect(Collectors.joining(", "));
    }

    /**
     * Check if a task is eligible as a predecessor for another task.
     */
    private boolean isEligiblePredecessor(Task predecessor, Task dependent) {
        boolean isEligible = !predecessor.getId().equals(dependent.getId())
                && !predecessor.isDescendantOf(dependent)
                && !dependent.isDescendantOf(predecessor);

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

    private void markTaskAsModified(Task task) {
        if (onTaskModified != null) {
            onTaskModified.accept(task, isEditMode);
        }
    }

    /**
     * Open a dialog to edit task dependencies
     */
    private void openDependencyEditor(Task task) {
        if (sprint == null || taskOrder == null) return;

        DependencyDialog dialog = new DependencyDialog(task, sprint, taskOrder, (t, selectedTaskIds) -> {
            // Mark task as modified
            markTaskAsModified(t);
            // Refresh grid
            getDataProvider().refreshAll();
        });
        dialog.open();
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        setRowsDraggable(editMode);

        if (editMode) {
            addClassName("edit-mode");
        } else {
            removeClassName("edit-mode");
            getElement().executeJs("this.updateEditMode(false);");
        }

        getDataProvider().refreshAll();
    }

    public void setOnDependencyDrop(BiConsumer<Task, Task> callback) {
        this.onDependencyDrop = callback;
    }

    // Public methods to control grid behavior

    public void setOnIndentTask(Consumer<Task> callback) {
        this.onIndentTask = callback;
    }

    public void setOnOutdentTask(Consumer<Task> callback) {
        this.onOutdentTask = callback;
    }

    public void setOnTaskModified(BiConsumer<Task, Boolean> callback) {
        this.onTaskModified = callback;
    }

    private void setupDragAndDrop() {
        // Enable row reordering with drag and drop in edit mode
        setRowsDraggable(true);

        // Add drop listener for reordering and dependency management
        addDropListener(event -> {
            if (isEditMode || draggedTask == null) return;

            Task dropTargetTask = event.getDropTargetItem().orElse(null);

            if (dropTargetTask != null && !draggedTask.equals(dropTargetTask)) {
                // Check drop location to determine action
                com.vaadin.flow.component.grid.dnd.GridDropLocation dropLocation = event.getDropLocation();

                if (dropLocation == com.vaadin.flow.component.grid.dnd.GridDropLocation.ON_TOP) {
                    // Handle dependency creation/removal when dropping ON_TOP
                    if (onDependencyDrop != null) {
                        onDependencyDrop.accept(draggedTask, dropTargetTask);
                    }
                } else {
                    // Handle reordering when dropping BETWEEN - this is now handled in parent view
                    // We just notify the parent about the indices
                    int draggedIndex = taskOrder.indexOf(draggedTask);
                    int targetIndex  = taskOrder.indexOf(dropTargetTask);

                    if (draggedIndex >= 0 && targetIndex >= 0) {
                        // Remove from old parent before moving
                        if (draggedTask.getParentTask() != null) {
                            Task oldParent = draggedTask.getParentTask();
                            oldParent.removeChildTask(draggedTask);
                            markTaskAsModified(oldParent);
                        }

                        // Move task (this should be handled by parent)
                        taskOrder.remove(draggedIndex);
                        taskOrder.add(targetIndex, draggedTask);

                        // Try to re-parent the task based on its new position
                        if (onIndentTask != null) {
                            onIndentTask.accept(draggedTask);
                        }
                    }
                }
            }

            draggedTask = null; // Clear the dragged task reference
        });

        addDragStartListener(event -> {
            if (!isEditMode && !event.getDraggedItems().isEmpty()) {
                draggedTask = event.getDraggedItems().get(0);
                setDropMode(com.vaadin.flow.component.grid.dnd.GridDropMode.ON_TOP_OR_BETWEEN);
            }
        });

        addDragEndListener(event -> {
            draggedTask = null;
            setDropMode(null);
        });

        // Add drop filter to prevent invalid dependency creation
        setDropFilter(dropTargetTask -> {
            if (isEditMode || draggedTask == null || dropTargetTask == null) {
                return true;
            }
            return isEligiblePredecessor(dropTargetTask, draggedTask);
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
                    if (task != null && onIndentTask != null) {
                        onIndentTask.accept(task);
                    }
                } catch (NumberFormatException ex) {
                    logger.warn("Invalid task ID for indent operation: {}", taskIdStr);
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
                    if (task != null && onOutdentTask != null) {
                        onOutdentTask.accept(task);
                    }
                } catch (NumberFormatException ex) {
                    logger.warn("Invalid task ID for outdent operation: {}", taskIdStr);
                }
            }
        }).addEventData("event.detail.taskId");

        // Add keyboard navigation support for Tab key navigation between editable cells
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
                        
                            // Get active element
                            const activeElement = document.activeElement;
                        
                            if (!isInTextInput(activeElement)) {
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
                        
                                // Try various property names that might contain the ID
                                let taskId = activeItem.id || activeItem.orderId || activeItem.col0;
                                console.log('Task ID from active item:', taskId);
                        
                                // If still not found, iterate through all properties to find the ID
                                if (!taskId) {
                                    console.log('Trying to find ID in all properties:', Object.keys(activeItem));
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
                            }
                        }, true);
                        
                        console.log('=== KEYBOARD NAVIGATION SETUP COMPLETE ===');
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

