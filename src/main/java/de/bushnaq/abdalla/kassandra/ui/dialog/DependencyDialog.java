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

package de.bushnaq.abdalla.kassandra.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A reusable dialog for editing task dependencies.
 */
public class DependencyDialog extends Dialog {

    private final Map<Long, Checkbox> checkboxMap = new HashMap<>();
    private final Logger              logger      = LoggerFactory.getLogger(this.getClass());
    private final SaveCallback        saveCallback;
    private final Sprint              sprint;
    private final Task                task;
    private final List<Task>          taskOrder;

    /**
     * Creates a dialog for editing task dependencies.
     *
     * @param task         The task to edit dependencies for
     * @param sprint       The sprint containing all tasks
     * @param taskOrder    The ordered list of all tasks in the sprint
     * @param saveCallback Callback that receives the updated task and selected task IDs
     */
    public DependencyDialog(Task task, Sprint sprint, List<Task> taskOrder, SaveCallback saveCallback) {
        this.task         = task;
        this.sprint       = sprint;
        this.taskOrder    = taskOrder;
        this.saveCallback = saveCallback;

        setHeaderTitle("Edit Dependencies for Task #" + task.getOrderId());
        setWidth("600px");
        setHeight("500px");

        // Main layout
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setSizeFull();

        // Info text
        Paragraph info = new Paragraph("Select tasks that must be completed before this task can start:");
        info.getStyle().set("margin-top", "0");

        // Get current visible predecessors
        Set<Long> currentPredecessorIds = task.getPredecessors().stream()
                .filter(Relation::isVisible)
                .map(Relation::getPredecessorId)
                .collect(Collectors.toSet());

        // Create checkbox group for task selection
        VerticalLayout checkboxContainer = new VerticalLayout();
        checkboxContainer.setSpacing(false);
        checkboxContainer.setPadding(false);
        checkboxContainer.getStyle()
                .set("overflow-y", "auto")
                .set("flex-grow", "1")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)");

        // Create checkboxes for ALL tasks, but disable ineligible ones
        for (Task t : taskOrder) {
            Checkbox checkbox = new Checkbox();

            // Format label: "5 - Implement login feature"
            String label = String.format("%d - %s", t.getOrderId(), t.getName());

            // Calculate indentation depth
            int depth        = t.getHierarchyDepth();
            int indentPixels = depth * 20;

            // Add icon based on task type
            HorizontalLayout checkboxLayout = new HorizontalLayout();
            checkboxLayout.setSpacing(false);
            checkboxLayout.setPadding(false);
            checkboxLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            checkboxLayout.getStyle().set("padding-left", indentPixels + "px");

            if (t.isMilestone()) {
                Div diamond = new Div();
                diamond.getElement().getStyle()
                        .set("width", "8px")
                        .set("height", "8px")
                        .set("background-color", "#1976d2")
                        .set("transform", "rotate(45deg)")
                        .set("margin-right", "8px")
                        .set("flex-shrink", "0");
                checkboxLayout.add(diamond);
            } else if (t.isStory()) {
                Div triangle = new Div();
                triangle.getElement().getStyle()
                        .set("width", "0")
                        .set("height", "0")
                        .set("border-left", "4px solid transparent")
                        .set("border-right", "4px solid transparent")
                        .set("border-top", "7px solid #43a047")
                        .set("margin-right", "8px")
                        .set("flex-shrink", "0");
                checkboxLayout.add(triangle);
            } else {
                // Add spacer for tasks to align with other task types
                Div spacer = new Div();
                spacer.getElement().getStyle()
                        .set("width", "15px")
                        .set("height", "1px")
                        .set("flex-shrink", "0");
                checkboxLayout.add(spacer);
            }

            checkbox.setLabel(label);
            checkbox.setValue(currentPredecessorIds.contains(t.getId()));

            // Check if this task is eligible as a predecessor
            boolean isEligible = !t.getId().equals(task.getId()) // Not the task itself
                    && !t.isDescendantOf(task) // Not a descendant (would create cycle)
                    && !task.isDescendantOf(t); // Not an ancestor (would create cycle)

            if (!isEligible) {
                checkbox.setEnabled(false);
                // Add tooltip to explain why it's disabled
                if (t.getId().equals(task.getId())) {
                    checkbox.setTooltipText("Cannot depend on itself");
                } else if (t.isDescendantOf(task)) {
                    checkbox.setTooltipText("Cannot depend on a child task (would create cycle)");
                } else if (task.isDescendantOf(t)) {
                    checkbox.setTooltipText("Cannot depend on a parent task (would create cycle)");
                }
            }

            checkboxMap.put(t.getId(), checkbox);

            // Wrap checkbox in the layout to apply indentation and icons
            checkboxLayout.add(checkbox);
            checkboxContainer.add(checkboxLayout);
        }

        // Quick edit text field (alternative input method)
        TextField quickEditField = new TextField("Quick Edit (comma-separated #s)");
        quickEditField.setWidthFull();
        quickEditField.setPlaceholder("e.g., 3, 5, 12");
        quickEditField.setValue(getDependencyText());
        quickEditField.setHelperText("You can also type order IDs directly");

        // Sync quick edit field with checkboxes
        quickEditField.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                String value = e.getValue().trim();
                if (value.isEmpty()) {
                    // Uncheck all enabled checkboxes
                    checkboxMap.values().forEach(cb -> {
                        if (cb.isEnabled()) {
                            cb.setValue(false);
                        }
                    });
                } else {
                    // Parse comma-separated values
                    Set<Long> orderIds = parseOrderIds(value);

                    // Update checkboxes based on parsed IDs
                    for (Task t : taskOrder) {
                        Checkbox cb = checkboxMap.get(t.getId());
                        if (cb != null && cb.isEnabled()) {
                            cb.setValue(orderIds.contains(t.getOrderId()));
                        }
                    }
                }
            }
        });

        // Sync checkboxes with quick edit field
        for (Task t : taskOrder) {
            Checkbox cb = checkboxMap.get(t.getId());
            if (cb != null) {
                cb.addValueChangeListener(e -> {
                    if (e.isFromClient()) {
                        // Update quick edit field with current selection (only enabled checkboxes)
                        String newValue = taskOrder.stream()
                                .filter(task1 -> {
                                    Checkbox checkbox = checkboxMap.get(task1.getId());
                                    return checkbox != null && checkbox.isEnabled() && checkbox.getValue();
                                })
                                .map(task1 -> String.valueOf(task1.getOrderId()))
                                .collect(Collectors.joining(", "));
                        quickEditField.setValue(newValue);
                    }
                });
            }
        }

        // Add components to layout
        layout.add(info, checkboxContainer, quickEditField);

        // Footer buttons
        Button saveButton = new Button("Save", event -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", event -> close());

        getFooter().add(cancelButton, saveButton);

        add(layout);
    }

    /**
     * Get the dependency text for display (comma-separated orderIds of visible predecessors)
     */
    private String getDependencyText() {
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
     * Parse comma-separated order IDs from a string
     */
    private Set<Long> parseOrderIds(String input) {
        Set<Long> orderIds = new HashSet<>();
        if (input == null || input.trim().isEmpty()) {
            return orderIds;
        }

        String[] parts = input.split(",");
        for (String part : parts) {
            try {
                Long orderId = Long.parseLong(part.trim());
                orderIds.add(orderId);
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
                logger.warn("Invalid order ID in input: {}", part.trim());
            }
        }
        return orderIds;
    }

    /**
     * Save the dependency changes
     */
    private void save() {
        // Get selected task IDs (only from enabled checkboxes)
        Set<Long> selectedTaskIds = checkboxMap.entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled() && entry.getValue().getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // Update task dependencies
        updateTaskDependencies(selectedTaskIds);

        // Call the save callback
        saveCallback.save(task, selectedTaskIds);

        close();
    }

    /**
     * Update task dependencies based on selected task IDs
     */
    private void updateTaskDependencies(Set<Long> selectedTaskIds) {
        // Remove all visible predecessors
        task.getPredecessors().removeIf(Relation::isVisible);

        // Add new visible predecessors
        for (Long taskId : selectedTaskIds) {
            Task predecessor = sprint.getTaskById(taskId);
            if (predecessor != null) {
                task.addPredecessor(predecessor, true); // true = visible
            }
        }

        logger.info("Updated dependencies for task {}: {}", task.getKey(), selectedTaskIds);
    }

    /**
     * Functional interface for the save callback that receives the task and selected task IDs
     */
    @FunctionalInterface
    public interface SaveCallback {
        void save(Task task, Set<Long> selectedTaskIds);
    }
}

