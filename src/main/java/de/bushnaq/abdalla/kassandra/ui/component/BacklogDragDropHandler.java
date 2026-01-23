/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.rest.api.TaskApi;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;

/**
 * Centralized handler for drag-and-drop operations in the Backlog view.
 * Manages moving tasks between sprints and reordering tasks within sprints.
 */
@Log4j2
public class BacklogDragDropHandler {

    @Getter
    private Task   draggedTask;
    private final Runnable              onRefresh;
    private Sprint sourceSprintOfDraggedTask;
    private final Map<Long, Sprint>     sprintMap;      // sprintId -> sprint
    private final Map<Long, List<Task>> sprintTasksMap; // sprintId -> ordered tasks
    private final TaskApi               taskApi;

    public BacklogDragDropHandler(TaskApi taskApi, Runnable onRefresh) {
        this.taskApi        = taskApi;
        this.onRefresh      = onRefresh;
        this.sprintTasksMap = new HashMap<>();
        this.sprintMap      = new HashMap<>();
    }

    /**
     * Clear all sprint registrations.
     * Should be called before re-registering sprints (e.g., on refresh).
     */
    public void clearRegistrations() {
        sprintTasksMap.clear();
        sprintMap.clear();
    }

    /**
     * Handle drop on a sprint (at the end of the sprint's task list).
     */
    public void handleDropOnSprint(Sprint targetSprint) {
        if (draggedTask == null) return;

        log.info("Dropping {} onto sprint {} (at end)", draggedTask.getName(), targetSprint.getName());

        List<Task> targetTasks   = sprintTasksMap.getOrDefault(targetSprint.getId(), new ArrayList<>());
        List<Task> modifiedTasks = new ArrayList<>();

        // If moving between sprints
        if (!targetSprint.getId().equals(draggedTask.getSprintId())) {
            // Remove from source sprint's task list
            List<Task> sourceTasks = sprintTasksMap.get(sourceSprintOfDraggedTask.getId());
            if (sourceTasks != null) {
                sourceTasks.remove(draggedTask);
                // Also remove child tasks if it's a story
                if (draggedTask.isStory()) {
                    for (Task child : new ArrayList<>(draggedTask.getChildTasks())) {
                        sourceTasks.remove(child);
                    }
                }
                // Recalculate orderIds for source sprint
                recalculateOrderIds(sourceTasks, modifiedTasks);
            }

            // Update task's sprint
            draggedTask.setSprintId(targetSprint.getId());
        } else {
            // Same sprint - remove from current position
            targetTasks.remove(draggedTask);
            if (draggedTask.isStory()) {
                for (Task child : new ArrayList<>(draggedTask.getChildTasks())) {
                    targetTasks.remove(child);
                }
            }
        }

        // Calculate new orderId (at end of target sprint)
        int newOrderId = targetTasks.isEmpty() ? 0 :
                targetTasks.stream().mapToInt(Task::getOrderId).max().orElse(0) + 1;

        draggedTask.setOrderId(newOrderId);
        targetTasks.add(draggedTask);
        modifiedTasks.add(draggedTask);

        // If it's a story, also move all child tasks
        if (draggedTask.isStory()) {
            int childOrderId = newOrderId + 1;
            for (Task child : draggedTask.getChildTasks()) {
                child.setSprintId(targetSprint.getId());
                child.setOrderId(childOrderId++);
                targetTasks.add(child);
                modifiedTasks.add(child);
            }
        }

        // Persist all modified tasks
        persistModifiedTasks(modifiedTasks);

        // Clear drag state and refresh
        onDragEnd();
        onRefresh.run();
    }

    /**
     * Handle drop on a specific task (before or after).
     */
    public void handleDropOnTask(Task targetTask, Sprint targetSprint, boolean dropBelow) {
        if (draggedTask == null || draggedTask.equals(targetTask)) return;

        log.info("Dropping {} {} {} in sprint {}",
                draggedTask.getName(),
                dropBelow ? "after" : "before",
                targetTask.getName(),
                targetSprint.getName());

        List<Task> modifiedTasks = new ArrayList<>();

        // Get target sprint's task list
        List<Task> targetTasks = sprintTasksMap.getOrDefault(targetSprint.getId(), new ArrayList<>());

        // Collect tasks to move (dragged task + children if story)
        List<Task> tasksToMove = new ArrayList<>();
        tasksToMove.add(draggedTask);
        if (draggedTask.isStory()) {
            tasksToMove.addAll(draggedTask.getChildTasks());
        }

        // If moving between sprints
        boolean crossSprintMove = !targetSprint.getId().equals(draggedTask.getSprintId());
        if (crossSprintMove) {
            // Remove from source sprint's task list
            List<Task> sourceTasks = sprintTasksMap.get(sourceSprintOfDraggedTask.getId());
            if (sourceTasks != null) {
                for (Task task : tasksToMove) {
                    sourceTasks.remove(task);
                }
                // Recalculate orderIds for source sprint
                recalculateOrderIds(sourceTasks, modifiedTasks);
            }

            // Update sprint for all moved tasks
            for (Task task : tasksToMove) {
                task.setSprintId(targetSprint.getId());
            }
        } else {
            // Same sprint - just remove from current positions
            for (Task task : tasksToMove) {
                targetTasks.remove(task);
            }
        }

        // Find insertion point
        int targetIndex = targetTasks.indexOf(targetTask);
        if (targetIndex == -1) {
            targetIndex = targetTasks.size();
        } else if (dropBelow) {
            // If dropping below a story, insert after all its children
            if (targetTask.isStory() && !targetTask.getChildTasks().isEmpty()) {
                Task lastChild      = targetTask.getChildTasks().get(targetTask.getChildTasks().size() - 1);
                int  lastChildIndex = targetTasks.indexOf(lastChild);
                if (lastChildIndex != -1) {
                    targetIndex = lastChildIndex + 1;
                } else {
                    targetIndex++;
                }
            } else {
                targetIndex++;
            }
        }

        // Insert all tasks at the new position
        for (int i = 0; i < tasksToMove.size(); i++) {
            Task task = tasksToMove.get(i);
            targetTasks.add(targetIndex + i, task);
        }

        // Recalculate orderIds for target sprint
        recalculateOrderIds(targetTasks, modifiedTasks);

        // Persist all modified tasks
        persistModifiedTasks(modifiedTasks);

        // Clear drag state and refresh
        onDragEnd();
        onRefresh.run();
    }

    /**
     * Called when drag ends (cleanup).
     */
    public void onDragEnd() {
        this.draggedTask               = null;
        this.sourceSprintOfDraggedTask = null;
    }

    /**
     * Called when drag starts.
     */
    public void onDragStart(Task task, Sprint sourceSprint) {
        this.draggedTask               = task;
        this.sourceSprintOfDraggedTask = sourceSprint;
        log.info("Drag started: {} (ID: {}) from sprint {}", task.getName(), task.getId(), sourceSprint.getName());
    }

    /**
     * Persist all modified tasks to backend.
     */
    private void persistModifiedTasks(List<Task> modifiedTasks) {
        log.info("Persisting {} modified tasks", modifiedTasks.size());
        for (Task task : modifiedTasks) {
            taskApi.update(task);
        }
    }

    /**
     * Recalculate orderId for all tasks in the list based on their position.
     */
    private void recalculateOrderIds(List<Task> tasks, List<Task> modifiedTasks) {
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.getOrderId() != i) {
                task.setOrderId(i);
                if (!modifiedTasks.contains(task)) {
                    modifiedTasks.add(task);
                }
            }
        }
    }

    /**
     * Register a sprint's tasks for drag-drop operations.
     * Must be called for each sprint before drag-drop can work.
     */
    public void registerSprint(Sprint sprint, List<Task> tasks) {
        // Sort by orderId to maintain order
        List<Task> sortedTasks = new ArrayList<>(tasks);
        sortedTasks.sort(Comparator.comparingInt(Task::getOrderId));
        sprintTasksMap.put(sprint.getId(), sortedTasks);
        sprintMap.put(sprint.getId(), sprint);
    }
}
