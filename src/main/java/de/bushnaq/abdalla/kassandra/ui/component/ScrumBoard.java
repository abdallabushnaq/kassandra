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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.TaskApi;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Scrum board component that displays stories with their task lanes.
 * <p>
 * Manages drag-and-drop functionality for moving tasks between status lanes
 * and handles automatic story status updates.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Slf4j
public class ScrumBoard extends VerticalLayout {

    private final VerticalLayout     contentLayout;
    private final Map<Long, Boolean> expandedStories = new HashMap<>(); // Track expanded state per story ID
    private       String             filterText      = "";
    private final Sprint             sprint;
    private final List<StoryCard>    storyCards      = new ArrayList<>();
    private final TaskApi            taskApi;
    private final Map<Long, User>    userMap;

    public ScrumBoard(Sprint sprint, TaskApi taskApi, Map<Long, User> userMap) {
        this(sprint, taskApi, userMap, "");
    }

    public ScrumBoard(Sprint sprint, TaskApi taskApi, Map<Long, User> userMap, String filterText) {
        this.sprint     = sprint;
        this.taskApi    = taskApi;
        this.userMap    = userMap;
        this.filterText = filterText != null ? filterText.toLowerCase() : "";

        setPadding(false);
        setSpacing(true);
        setWidthFull();

        contentLayout = new VerticalLayout();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(true);
        contentLayout.setWidthFull();

        add(contentLayout);

        initializeDragAndDrop();
        refresh();
    }

    private Task findTaskById(Long taskId) {
        if (sprint.getTasks() == null) {
            return null;
        }
        return sprint.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElse(null);
    }

    public Sprint getSprint() {
        return sprint;
    }

    @ClientCallable
    public void handleDrop(String taskIdStr, String statusStr) {
        try {
            Long       taskId    = Long.parseLong(taskIdStr);
            TaskStatus newStatus = TaskStatus.valueOf(statusStr);

            // Find the task
            Task task = findTaskById(taskId);
            if (task == null) {
                showError("Task not found");
                return;
            }

            // Check if status actually changed
            if (task.getTaskStatus() == newStatus) {
                return; // No change needed
            }

            // Update task status
            handleTaskStatusChange(task, newStatus);

        } catch (Exception e) {
            log.error("Error handling drop", e);
            showError("Failed to update task status: " + e.getMessage());
        }
    }

    private void handleTaskStatusChange(Task task, TaskStatus newStatus) {
        try {
            // Update task status via API
            taskApi.updateTaskStatus(task.getId(), newStatus);

            // Update local task object
            task.setTaskStatus(newStatus);

            // Refresh the board to show the changes
            refresh();

            // Show success notification
            showSuccess("Task " + task.getId() + " moved to " + newStatus.name());

        } catch (Exception e) {
            log.error("Error updating task status", e);
            showError("Failed to update task status: " + e.getMessage());
        }
    }

    private void initializeDragAndDrop() {
        // Add JavaScript to handle drag and drop
        getElement().executeJs(
                "const board = this;" +
                        "board.addEventListener('drop', function(e) {" +
                        "  e.preventDefault();" +
                        "  const taskId = e.dataTransfer.getData('text/plain');" +
                        "  const target = e.target.closest('.task-lane');" +
                        "  if (target && taskId) {" +
                        "    const status = target.getAttribute('data-status');" +
                        "    board.$server.handleDrop(taskId, status);" +
                        "  }" +
                        "});" +
                        "board.addEventListener('dragstart', function(e) {" +
                        "  if (e.target.classList.contains('task-card')) {" +
                        "    const taskId = e.target.getAttribute('id').replace('task-card-', '');" +
                        "    e.dataTransfer.setData('text/plain', taskId);" +
                        "    e.dataTransfer.effectAllowed = 'move';" +
                        "  }" +
                        "});"
        );
    }

    /**
     * Check if a task matches the current filter text
     */
    private boolean matchesFilter(Task task) {
        if (filterText == null || filterText.isEmpty()) {
            return true;
        }
        String searchableText = task.getSearchableText();
        return searchableText != null && searchableText.toLowerCase().contains(filterText);
    }

    public void refresh() {
        // Save current expanded states before clearing
        storyCards.forEach(card ->
                expandedStories.put(card.getStory().getId(), card.isExpanded())
        );

        contentLayout.removeAll();
        storyCards.clear();

        List<Task> tasks = sprint.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            showEmptyState();
            return;
        }

        // Group tasks by story
        Map<Task, List<Task>> storiesWithTasks = new HashMap<>();

        // Find all stories and their child tasks
        for (Task task : tasks) {
            if (task.isStory()) {
                List<Task> childTasks = tasks.stream()
                        .filter(t -> t.getParentTaskId() != null && t.getParentTaskId().equals(task.getId()))
                        .collect(Collectors.toList());

                // If filter is active, filter child tasks
                if (filterText != null && !filterText.isEmpty()) {
                    childTasks = childTasks.stream()
                            .filter(t -> matchesFilter(t))
                            .collect(Collectors.toList());
                }

                // Only add story if it has child tasks after filtering OR if the story itself matches the filter
                if (!childTasks.isEmpty() || (filterText != null && !filterText.isEmpty() && matchesFilter(task))) {
                    storiesWithTasks.put(task, childTasks);
                }
            }
        }

        // Create StoryCard for each story
        storiesWithTasks.forEach((story, childTasks) -> {
            StoryCard storyCard = new StoryCard(story, childTasks, userMap, this::handleTaskStatusChange);

            // Restore expanded state (default to true if not found)
            boolean shouldExpand = expandedStories.getOrDefault(story.getId(), true);
            storyCard.setExpanded(shouldExpand);

            storyCards.add(storyCard);
            contentLayout.add(storyCard);
        });

        if (storyCards.isEmpty()) {
            showEmptyState();
        }
    }

    private void showEmptyState() {
        Div emptyState = new Div();
        emptyState.addClassName("empty-state");

        Span message = new Span("No stories found in this sprint");
        message.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("padding", "var(--lumo-space-xl)");

        emptyState.add(message);
        emptyState.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("min-height", "200px");

        contentLayout.add(emptyState);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}

