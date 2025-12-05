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

import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A component representing a feature with its story lanes in a Scrum board.
 * <p>
 * Displays a feature header with key, title, and story count, followed by three vertical lanes:
 * TO DO, IN PROGRESS, and DONE. Each lane contains StoryTaskCard components that display
 * stories as cards with their indented child tasks.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Slf4j
public class FeatureCard extends Div {

    private final List<Task>                   allTasks;
    private       VerticalLayout               doneLane;
    private       boolean                      expanded = true; // Default to expanded
    private final Feature                      feature;
    private final String                       filterText;
    private       VerticalLayout               inProgressLane;
    private       HorizontalLayout             lanesContainer;
    private final Consumer<Task>               onTaskClick;
    private final BiConsumer<Task, TaskStatus> onTaskStatusChange;
    private final Set<User>                    selectedUsers;
    private final List<Task>                   stories;
    private       VerticalLayout               todoLane;
    private final Map<Long, User>              userMap;

    public FeatureCard(Feature feature, List<Task> stories, List<Task> allTasks, Map<Long, User> userMap,
                       BiConsumer<Task, TaskStatus> onTaskStatusChange, String filterText, Set<User> selectedUsers) {
        this(feature, stories, allTasks, userMap, onTaskStatusChange, filterText, selectedUsers, null);
    }

    public FeatureCard(Feature feature, List<Task> stories, List<Task> allTasks, Map<Long, User> userMap,
                       BiConsumer<Task, TaskStatus> onTaskStatusChange, String filterText, Set<User> selectedUsers,
                       Consumer<Task> onTaskClick) {
        this.feature            = feature;
        this.stories            = stories;
        this.allTasks           = allTasks;
        this.userMap            = userMap;
        this.onTaskStatusChange = onTaskStatusChange;
        this.filterText         = filterText != null ? filterText.toLowerCase() : "";
        this.selectedUsers      = selectedUsers;
        this.onTaskClick        = onTaskClick;

        addClassName("feature-card");
        setWidthFull();

        createFeatureHeader();
        createLanes();
        populateLanes();
        applyStyling();
    }

    private void applyStyling() {
        getStyle()
                .set("margin-bottom", "29px"); // 29px vertical space between feature boards
        setWidthFull();
    }

    private void clearLaneStories(VerticalLayout lane) {
        lane.getChildren()
                .filter(component -> component instanceof StoryTaskCard)
                .collect(Collectors.toList())
                .forEach(lane::remove);
    }

    private long countStoryCards(VerticalLayout lane) {
        return lane.getChildren()
                .filter(component -> component instanceof StoryTaskCard)
                .count();
    }

    private void createFeatureHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(true);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.getStyle()
                .set("background", "transparent") // Transparent background
                .set("padding", "var(--lumo-space-s) 0"); // Only vertical padding

        // Expand/collapse arrow icon
        Icon expandIcon = new Icon(expanded ? VaadinIcon.CHEVRON_DOWN : VaadinIcon.CHEVRON_RIGHT);
        expandIcon.getStyle()
                .set("cursor", "pointer")
                .set("color", "#000000") // Same black color as title
                .set("width", "12px") // Smaller size
                .set("height", "12px");

        expandIcon.addClickListener(e -> toggleExpand());

        // Feature key (bold, smaller, gray)
        Span featureKey = new Span(formatFeatureKey(feature));
        featureKey.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-xs)") // Smaller text size
                .set("color", "#9E9E9E"); // Gray color

        // Feature title (plain black)
        Span featureTitle = new Span(feature.getName());
        featureTitle.getStyle()
                .set("font-weight", "normal")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "#000000"); // Plain black

        // Story count
        Span storyCount = new Span("(" + stories.size() + " stories)");
        storyCount.getStyle()
                .set("font-weight", "normal")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "var(--lumo-secondary-text-color)");

        header.add(expandIcon, featureKey, featureTitle, storyCount);
        add(header);
    }

    private VerticalLayout createLane(String title, TaskStatus status) {
        VerticalLayout lane = new VerticalLayout();
        lane.addClassName("task-lane"); // Changed from story-lane to task-lane for drag-drop
        lane.addClassName("task-lane-" + status.name().toLowerCase().replace("_", "-"));
        lane.setWidth("33.33%");
        lane.setPadding(true);
        lane.setSpacing(false); // Disable default spacing
        lane.getStyle()
                .set("background", "#F5F5F5") // Lighter gray background
                .set("border-radius", "8px") // Rounded corners
                .set("min-height", "150px")
                .set("box-sizing", "border-box")
                .set("gap", "2px"); // 2px vertical space between story cards

        // Lane header
        Div laneHeader = new Div();
        laneHeader.addClassName("lane-header");
        Span laneTitle = new Span(title);
        laneTitle.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-xs)") // Extra small text size
                .set("color", "#616161"); // Dark gray color

        Span storyCount = new Span(" (0)");
        storyCount.addClassName("story-count-" + status.name().toLowerCase().replace("_", "-"));
        storyCount.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)") // Extra small text size
                .set("color", "var(--lumo-secondary-text-color)");

        laneHeader.add(laneTitle, storyCount);
        lane.add(laneHeader);

        // Setup drop zone for drag and drop
        setupDropZone(lane, status);

        return lane;
    }

    private void createLanes() {
        lanesContainer = new HorizontalLayout();
        lanesContainer.setWidthFull();
        lanesContainer.setPadding(false);
        lanesContainer.setSpacing(false); // Disable default spacing
        lanesContainer.getStyle()
                .set("min-height", "200px")
                .set("padding", "var(--lumo-space-m)")
                .set("box-sizing", "border-box")
                .set("gap", "9px"); // 9px horizontal space between lanes

        // TO DO Lane
        todoLane = createLane("TO DO", TaskStatus.TODO);

        // IN PROGRESS Lane
        inProgressLane = createLane("IN PROGRESS", TaskStatus.IN_PROGRESS);

        // DONE Lane
        doneLane = createLane("DONE", TaskStatus.DONE);

        lanesContainer.add(todoLane, inProgressLane, doneLane);
        add(lanesContainer);
    }

    private String formatFeatureKey(Feature feature) {
        if (feature.getId() != null) {
            return "F-" + feature.getId();
        }
        return "F-???";
    }

    public Feature getFeature() {
        return feature;
    }

    private List<Task> getFilteredChildTasks(Task story) {
        List<Task> childTasks = allTasks.stream()
                .filter(t -> t.getParentTaskId() != null && t.getParentTaskId().equals(story.getId()))
                .collect(Collectors.toList());

        // Apply search filter if active
        if (filterText != null && !filterText.isEmpty()) {
            childTasks = childTasks.stream()
                    .filter(t -> t.getSearchableText() != null
                            && t.getSearchableText().toLowerCase().contains(filterText))
                    .collect(Collectors.toList());
        }

        // Apply user filter if active
        if (selectedUsers != null && !selectedUsers.isEmpty()) {
            childTasks = childTasks.stream()
                    .filter(t -> t.getResourceId() != null && selectedUsers.stream()
                            .anyMatch(user -> user.getId().equals(t.getResourceId())))
                    .collect(Collectors.toList());
        }

        return childTasks;
    }

    public boolean hasVisibleStories() {
        // Check if any story has visible tasks after filtering
        return stories.stream()
                .anyMatch(story -> !getFilteredChildTasks(story).isEmpty());
    }

    public boolean isExpanded() {
        return expanded;
    }

    private void populateLanes() {
        // Clear existing story cards
        clearLaneStories(todoLane);
        clearLaneStories(inProgressLane);
        clearLaneStories(doneLane);

        // For each story, we need to show it in each lane where it has tasks
        // This allows tasks to be in different lanes than their story
        for (Task story : stories) {
            List<Task> allChildTasks = getFilteredChildTasks(story);

            if (allChildTasks.isEmpty()) {
                continue; // Skip stories with no visible tasks after filtering
            }

            // Get the story's effective status (based on all its tasks)
            TaskStatus storyStatus = story.getEffectiveStatus();

            // Group this story's tasks by their actual status
            Map<TaskStatus, List<Task>> tasksByStatus = allChildTasks.stream()
                    .collect(Collectors.groupingBy(Task::getTaskStatus));

            // Create StoryTaskCard in each lane where this story has tasks
            List<Task> todoTasks = tasksByStatus.getOrDefault(TaskStatus.TODO, List.of());
            if (!todoTasks.isEmpty()) {
                // Use simplified header if story's effective status is not TODO (tasks are in different lane than story)
                boolean       useSimplifiedHeader = (storyStatus != TaskStatus.TODO);
                StoryTaskCard card                = new StoryTaskCard(story, todoTasks, userMap, onTaskStatusChange, useSimplifiedHeader, onTaskClick);
                todoLane.add(card);
            }

            List<Task> inProgressTasks = tasksByStatus.getOrDefault(TaskStatus.IN_PROGRESS, List.of());
            if (!inProgressTasks.isEmpty()) {
                // Use simplified header if story's effective status is not IN_PROGRESS
                boolean       useSimplifiedHeader = (storyStatus != TaskStatus.IN_PROGRESS);
                StoryTaskCard card                = new StoryTaskCard(story, inProgressTasks, userMap, onTaskStatusChange, useSimplifiedHeader, onTaskClick);
                inProgressLane.add(card);
            }

            List<Task> doneTasks = tasksByStatus.getOrDefault(TaskStatus.DONE, List.of());
            if (!doneTasks.isEmpty()) {
                // Use simplified header if story's effective status is not DONE
                boolean       useSimplifiedHeader = (storyStatus != TaskStatus.DONE);
                StoryTaskCard card                = new StoryTaskCard(story, doneTasks, userMap, onTaskStatusChange, useSimplifiedHeader, onTaskClick);
                doneLane.add(card);
            }
        }

        // Count story cards (stories can appear in multiple lanes now)
        long todoCount       = countStoryCards(todoLane);
        long inProgressCount = countStoryCards(inProgressLane);
        long doneCount       = countStoryCards(doneLane);

        // Update story counts
        updateStoryCount(todoLane, (int) todoCount, TaskStatus.TODO);
        updateStoryCount(inProgressLane, (int) inProgressCount, TaskStatus.IN_PROGRESS);
        updateStoryCount(doneLane, (int) doneCount, TaskStatus.DONE);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        updateExpandState();
    }

    /**
     * Setup drop zone for drag and drop functionality
     */
    private void setupDropZone(VerticalLayout lane, TaskStatus targetStatus) {
        log.info("Setting up drop zone for lane: {}", targetStatus);

        // Create drop target using Vaadin's DropTarget API
        DropTarget<VerticalLayout> dropTarget = DropTarget.create(lane);
        dropTarget.setActive(true);

        dropTarget.addDropListener(event -> {
            log.info("Drop event triggered on lane: {}", targetStatus);

            // Get drag source component
            event.getDragSourceComponent().ifPresent(source -> {
                log.info("Drag source component type: {}", source.getClass().getName());
                log.info("Drag source component ID: {}", source.getId().orElse("NO-ID"));

                if (source instanceof TaskCard taskCard) {
                    Task task = taskCard.getTask();
                    log.info("TaskCard found! Task ID: {}, Task Name: {}, Current Status: {}, Target Status: {}",
                            task.getId(), task.getName(), task.getTaskStatus(), targetStatus);

                    // Only process if status actually changed
                    if (task.getTaskStatus() != targetStatus) {
                        log.info("Status changed, calling onTaskStatusChange for task: {}", task.getId());
                        // The callback will update the task status and refresh the board
                        // Story will only move to IN_PROGRESS when ALL tasks are at least IN_PROGRESS
                        onTaskStatusChange.accept(task, targetStatus);
                    } else {
                        log.info("Status unchanged, skipping update for task: {}", task.getId());
                    }
                } else {
                    log.warn("Drag source is NOT a TaskCard! Type: {}", source.getClass().getName());
                }
            });

            if (event.getDragSourceComponent().isEmpty()) {
                log.warn("No drag source component found in drop event!");
            }

            // Reset background after drop
            lane.getStyle().set("background", "#F5F5F5");
        });

        // Add visual feedback on drag over
        lane.getElement().addEventListener("dragenter", e -> {
            log.debug("Drag enter on lane: {}", targetStatus);
            lane.getStyle().set("background", "var(--lumo-primary-color-10pct)");
        });

        lane.getElement().addEventListener("dragleave", e -> {
            log.debug("Drag leave on lane: {}", targetStatus);
            lane.getStyle().set("background", "#F5F5F5");
        });
    }

    private void toggleExpand() {
        expanded = !expanded;
        updateExpandState();
    }

    private void updateExpandState() {
        if (lanesContainer != null) {
            lanesContainer.setVisible(expanded);
        }

        // Update the icon in the header
        getChildren()
                .filter(component -> component instanceof HorizontalLayout)
                .findFirst()
                .ifPresent(header -> {
                    header.getChildren()
                            .filter(component -> component instanceof Icon)
                            .findFirst()
                            .ifPresent(iconComponent -> {
                                Icon icon = (Icon) iconComponent;
                                icon.getElement().setAttribute("icon",
                                        expanded ? "vaadin:chevron-down" : "vaadin:chevron-right");
                            });
                });
    }

    private void updateStoryCount(VerticalLayout lane, int count, TaskStatus status) {
        String className = "story-count-" + status.name().toLowerCase().replace("_", "-");
        lane.getChildren()
                .filter(component -> component.getElement().getClassList().contains(className))
                .findFirst()
                .ifPresent(component -> {
                    if (component instanceof Span) {
                        ((Span) component).setText(" (" + count + ")");
                    }
                });
    }
}

