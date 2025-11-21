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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskMode;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles clipboard operations (copy/paste) for tasks in the TaskGrid.
 * This class encapsulates all logic related to serializing, deserializing,
 * and managing task copy-paste operations.
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Log4j2
public class TaskClipboardHandler {
    private final TaskGrid     grid;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for TaskClipboardHandler
     *
     * @param grid         The grid component for JavaScript execution
     * @param objectMapper Jackson ObjectMapper for JSON serialization
     */
    public TaskClipboardHandler(TaskGrid grid, ObjectMapper objectMapper) {
        this.grid         = grid;
        this.objectMapper = objectMapper;
    }

    /**
     * Clear transient data from a task that shouldn't be copied.
     *
     * @param task The task to clear data from
     */
    private void clearTaskFields(Task task) {
        task.setCritical(null);
        task.setDuration(null);
        task.setFinish(null);
        task.setId(null);
        task.setParentTask(null);
        task.setParentTaskId(null);
        task.setOrderId(null);
        task.setOriginalEstimate(null);
        task.setPredecessors(null);
        task.setProgress(null);
        task.setRemainingEstimate(null);
        task.setSprint(null);
        task.setSprintId(null);
        task.setStart(null);
        task.setTaskMode(null);
        task.setTimeSpent(null);
    }

    /**
     * Deserialize a task from clipboard JSON.
     * Handles both single tasks and stories with children.
     *
     * @param json The JSON string to deserialize
     * @return The deserialized task
     * @throws Exception if deserialization fails
     */
    private Task deserializeTask(String json) throws Exception {
        try {
            // Try to deserialize as a single task
            return objectMapper.readValue(json, Task.class);
        } catch (Exception e) {
            // Try to deserialize as a story with children wrapper
            try {
                Map<String, Object> wrapper = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
                });
                if (wrapper.containsKey("story") && wrapper.containsKey("children")) {
                    Task story = objectMapper.convertValue(wrapper.get("story"), Task.class);
                    List<Task> children = objectMapper.convertValue(
                            wrapper.get("children"),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Task.class)
                    );
                    story.getChildTasks().clear();
                    story.getChildTasks().addAll(children);
                    return story;
                }
            } catch (Exception ex) {
                log.warn("Failed to deserialize as story with children", ex);
            }
            throw e;
        }
    }

    /**
     * Ensure a task name is unique by adding " (copy)" suffix and incrementing numbers if needed.
     *
     * @param originalName The original task name
     * @return A unique name that doesn't exist in the current taskOrder
     */
    private String ensureUniqueName(String originalName) {
        String baseName      = originalName + " (copy)";
        String candidateName = baseName;
        int    counter       = 1;

        // Keep checking and incrementing until we find a unique name
        while (isNameTaken(candidateName)) {
            candidateName = baseName + " " + counter;
            counter++;
        }

        return candidateName;
    }

    /**
     * Handle copy operation for a task.
     * Serializes the task and its children (for stories) to JSON and sends to client clipboard.
     * Clears transient data before serialization for cleaner JSON.
     *
     * @param task The task to copy
     */
    public void handleCopy(Task task) {
        try {
            // Serialize then deserialize to create a deep copy (works for both tasks and stories)
            String jsonCopy = serializeTask(task);
            Task   taskCopy = deserializeTask(jsonCopy);

            // Clear transient data from the copy
            clearTaskFields(taskCopy);
            if (taskCopy.isStory()) {
                // Also clear transient data from all children
                for (Task child : taskCopy.getChildTasks()) {
                    clearTaskFields(child);
                }
            }

            // Serialize the clean copy for clipboard
            String json = serializeTask(taskCopy);

            // Send to client clipboard via JavaScript
            grid.getElement().executeJs(
                    "navigator.clipboard.writeText($0).then(() => {" +
                            "  console.log('Task copied to clipboard');" +
                            "}, (err) => {" +
                            "  console.error('Failed to copy task to clipboard', err);" +
                            "});",
                    json
            );

            // Show success notification
            String       taskType     = task.isStory() ? "Story" : (task.isMilestone() ? "Milestone" : "Task");
            Notification notification = Notification.show(taskType + " '" + task.getName() + "' copied to clipboard", 3000, Notification.Position.BOTTOM_START);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            log.info("Copied task {} to clipboard", task.getName());
        } catch (Exception e) {
            log.error("Error copying task to clipboard", e);
            Notification notification = Notification.show("Failed to copy task to clipboard", 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Handle paste operation.
     * Deserializes tasks from JSON and adds them to the list.
     *
     * @param clipboardData The JSON string from the clipboard
     */
    public void handlePaste(String clipboardData) {
        try {
            // Try to deserialize as a single task first
            Task pastedTask = deserializeTask(clipboardData);
            if (pastedTask != null) {
                validateAndFixTaskFields(pastedTask);

                // Ensure unique name for the pasted task
                pastedTask.setName(ensureUniqueName(pastedTask.getName()));

                grid.addTask(pastedTask);
                // If it's a story with children, persist children too
                if (pastedTask.isStory() && !pastedTask.getChildTasks().isEmpty()) {
                    for (Task childTask : pastedTask.getChildTasks()) {
                        validateAndFixTaskFields(childTask);

                        // Ensure unique name for each child task
                        childTask.setName(ensureUniqueName(childTask.getName()));

                        grid.addTask(childTask);
                    }
                }

                // Show success notification
                String taskType   = pastedTask.isStory() ? "Story" : (pastedTask.isMilestone() ? "Milestone" : "Task");
                int    childCount = pastedTask.isStory() ? pastedTask.getChildTasks().size() : 0;
                String message    = taskType + " '" + pastedTask.getName() + "' pasted from clipboard";
                if (childCount > 0) {
                    message += " with " + childCount + " child task" + (childCount > 1 ? "s" : "");
                }
                Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                log.info("Pasted task {} from clipboard", pastedTask.getName());
            }
        } catch (Exception e) {
            log.error("Error pasting task from clipboard", e);
            Notification notification = Notification.show("Failed to paste task from clipboard", 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Check if a task name already exists in the taskOrder list.
     *
     * @param name The name to check
     * @return true if the name is already taken, false otherwise
     */
    private boolean isNameTaken(String name) {
        List<Task> taskOrder = grid.getTaskOrder();
        return taskOrder.stream()
                .anyMatch(task -> task.getName().equals(name));
    }

    /**
     * Serialize a task to JSON for clipboard.
     * Handles both simple tasks and stories with children.
     *
     * @param task The task to serialize
     * @return JSON string representation
     * @throws Exception if serialization fails
     */
    private String serializeTask(Task task) throws Exception {
        if (task.isStory()) {
            // For stories, create a map structure with story and children
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("story", task);
            wrapper.put("children", task.getChildTasks());
            return objectMapper.writeValueAsString(wrapper);
        } else {
            // For simple tasks/milestones, serialize directly
            return objectMapper.writeValueAsString(task);
        }
    }

    private void validateAndFixTaskFields(Task task) throws IllegalArgumentException {
        clearTaskFields(task);
        //fields that are not copy/pasted but need default vales for db to accept
        task.setTaskMode(TaskMode.AUTO_SCHEDULED);
        task.setOrderId(-1);
        task.setCritical(false);
        task.setProgress(0);
        if (task.getName() == null || task.getName().isBlank()) {
            throw new IllegalArgumentException("Task name cannot be null or blank");
        }
    }

}

