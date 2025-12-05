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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.rest.api.TaskApi;
import de.bushnaq.abdalla.kassandra.ui.view.GroupingMode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A Scrum board component that displays merged data from multiple sprints.
 * <p>
 * Can group by Features (stories from multiple sprints under same feature) or
 * by Stories (traditional view with all stories across sprints).
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Slf4j
public class MergedScrumBoard extends VerticalLayout {

    private final List<Task>         allTasks;
    private final VerticalLayout     contentLayout;
    private final Map<Long, Boolean> expandedFeatures = new HashMap<>();
    private final Map<Long, Boolean> expandedStories  = new HashMap<>();
    private final Map<Long, Feature> featureMap;
    private       String             filterText       = "";
    private final GroupingMode       groupingMode;
    private final Set<User>          selectedUsers;
    private final List<Sprint>       sprints;
    private final List<StoryCard>    storyCards       = new ArrayList<>();
    private final TaskApi            taskApi;
    private final Map<Long, User>    userMap;

    public MergedScrumBoard(List<Sprint> sprints, List<Task> allTasks, TaskApi taskApi,
                            Map<Long, User> userMap, String filterText, GroupingMode groupingMode,
                            Map<Long, Feature> featureMap, Set<User> selectedUsers) {
        this.sprints       = sprints;
        this.allTasks      = allTasks;
        this.taskApi       = taskApi;
        this.userMap       = userMap;
        this.filterText    = filterText != null ? filterText.toLowerCase() : "";
        this.groupingMode  = groupingMode;
        this.featureMap    = featureMap;
        this.selectedUsers = selectedUsers;

        setPadding(false);
        setSpacing(true);
        setWidthFull();

        contentLayout = new VerticalLayout();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(true);
        contentLayout.setWidthFull();

        add(contentLayout);

        // Drag-and-drop is now handled by individual components (TaskCard, FeatureCard, StoryCard)
        refresh();
    }

    private Task findTaskById(Long taskId) {
        return allTasks.stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElse(null);
    }


    private void handleTaskStatusChange(Task task, TaskStatus newStatus) {
        try {
            log.info("handleTaskStatusChange called for task {} from {} to {}",
                    task.getId(), task.getTaskStatus(), newStatus);

            // Update task status via API
            taskApi.updateTaskStatus(task.getId(), newStatus);

            // Update local task object
            task.setTaskStatus(newStatus);

            // Refresh the board to show the changes
            // Story status will be recalculated correctly - story only moves to IN_PROGRESS
            // when ALL tasks are at least IN_PROGRESS (no TODO tasks remain)
            refresh();

            // Show success notification
            showSuccess("Task " + task.getId() + " moved to " + newStatus.name());

        } catch (Exception e) {
            log.error("Error updating task status", e);
            showError("Failed to update task status: " + e.getMessage());
        }
    }


    /**
     * Check if a task matches the current filter text
     */
    private boolean matchesFilter(Task task) {
        if (filterText == null || filterText.isEmpty()) {
            return matchesUserFilter(task);
        }
        String  searchableText = task.getSearchableText();
        boolean textMatch      = searchableText != null && searchableText.toLowerCase().contains(filterText);
        return textMatch && matchesUserFilter(task);
    }

    /**
     * Check if a story matches the filter in Features mode
     */
    private boolean matchesFilterFeaturesMode(Feature feature, Task story) {
        if (filterText == null || filterText.isEmpty()) {
            return true;
        }

        // Check feature name
        if (feature != null && feature.getName() != null
                && feature.getName().toLowerCase().contains(filterText)) {
            return true;
        }

        // Check story
        if (story.getSearchableText() != null
                && story.getSearchableText().toLowerCase().contains(filterText)) {
            return true;
        }

        // Check any child task of this story
        List<Task> childTasks = allTasks.stream()
                .filter(t -> t.getParentTaskId() != null && t.getParentTaskId().equals(story.getId()))
                .collect(Collectors.toList());

        for (Task task : childTasks) {
            if (task.getSearchableText() != null
                    && task.getSearchableText().toLowerCase().contains(filterText)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a task matches the selected users filter
     */
    private boolean matchesUserFilter(Task task) {
        // If no users selected, show all tasks
        if (selectedUsers == null || selectedUsers.isEmpty()) {
            return true;
        }

        // Check if task is assigned to one of the selected users
        if (task.getResourceId() == null) {
            return false; // Unassigned tasks don't match when users are filtered
        }

        return selectedUsers.stream()
                .anyMatch(user -> user.getId().equals(task.getResourceId()));
    }

    public void refresh() {
        if (groupingMode == GroupingMode.FEATURES) {
            refreshFeaturesMode();
        } else {
            refreshStoriesMode();
        }
    }

    /**
     * Refresh in Features mode - group stories by features across all sprints
     */
    private void refreshFeaturesMode() {
        contentLayout.removeAll();
        storyCards.clear();

        if (allTasks.isEmpty()) {
            showEmptyState();
            return;
        }

        // Group sprints by feature
        Map<Long, List<Sprint>> sprintsByFeature = sprints.stream()
                .filter(s -> s.getFeatureId() != null)
                .collect(Collectors.groupingBy(Sprint::getFeatureId));

        // For each feature, get all stories from all sprints belonging to that feature
        Map<Feature, List<Task>> storiesByFeature = new HashMap<>();

        for (Map.Entry<Long, List<Sprint>> entry : sprintsByFeature.entrySet()) {
            Long         featureId      = entry.getKey();
            List<Sprint> featureSprints = entry.getValue();

            Feature feature = featureMap.get(featureId);
            if (feature == null) {
                log.warn("Feature {} not found in feature map", featureId);
                continue;
            }

            // Get all stories from tasks that belong to any of these sprints
            List<Task> stories = allTasks.stream()
                    .filter(Task::isStory)
                    .filter(task -> featureSprints.stream()
                            .anyMatch(sprint -> sprint.getId().equals(task.getSprintId())))
                    .collect(Collectors.toList());

            // Filter stories based on search text if active
            if (filterText != null && !filterText.isEmpty()) {
                stories = stories.stream()
                        .filter(story -> matchesFilterFeaturesMode(feature, story))
                        .collect(Collectors.toList());
            }

            if (!stories.isEmpty()) {
                storiesByFeature.put(feature, stories);
            }
        }

        // Create FeatureCard for each feature
        storiesByFeature.forEach((feature, stories) -> {
            FeatureCard featureCard = new FeatureCard(
                    feature,
                    stories,
                    allTasks,
                    userMap,
                    this::handleTaskStatusChange,
                    filterText,
                    selectedUsers
            );

            // Only add feature if it has visible stories after filtering
            if (featureCard.hasVisibleStories()) {
                // Restore expanded state (default to true if not found)
                boolean shouldExpand = expandedFeatures.getOrDefault(feature.getId(), true);
                featureCard.setExpanded(shouldExpand);

                contentLayout.add(featureCard);
            }
        });

        if (contentLayout.getComponentCount() == 0) {
            showEmptyState();
        }
    }

    /**
     * Refresh in Stories mode - group all tasks by their parent stories
     */
    private void refreshStoriesMode() {
        // Save current expanded states before clearing
        storyCards.forEach(card ->
                expandedStories.put(card.getStory().getId(), card.isExpanded())
        );

        contentLayout.removeAll();
        storyCards.clear();

        if (allTasks.isEmpty()) {
            showEmptyState();
            return;
        }

        // Group tasks by story
        Map<Task, List<Task>> storiesWithTasks = new HashMap<>();

        // Find all stories and their child tasks
        for (Task task : allTasks) {
            if (task.isStory()) {
                List<Task> childTasks = allTasks.stream()
                        .filter(t -> t.getParentTaskId() != null && t.getParentTaskId().equals(task.getId()))
                        .collect(Collectors.toList());

                // Apply filters to child tasks
                if (filterText != null && !filterText.isEmpty()) {
                    childTasks = childTasks.stream()
                            .filter(this::matchesFilter)
                            .collect(Collectors.toList());
                } else {
                    // Even if no text filter, still apply user filter
                    childTasks = childTasks.stream()
                            .filter(this::matchesUserFilter)
                            .collect(Collectors.toList());
                }

                // Only add story if it has child tasks after filtering
                if (!childTasks.isEmpty()) {
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

        Span message = new Span("No stories found in selected sprints");
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

