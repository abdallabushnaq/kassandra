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
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A component representing a story with its task lanes in a Scrum board.
 * <p>
 * Displays a story header with key, title, and status, followed by three vertical lanes:
 * TO DO, IN PROGRESS, and DONE. Each lane contains TaskCard components that can be
 * dragged between lanes to change their status.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
public class StoryCard extends Div {

    private final List<Task>                   childTasks;
    private       VerticalLayout               doneLane;
    private       boolean                      expanded = true; // Default to expanded
    private       VerticalLayout               inProgressLane;
    private       HorizontalLayout             lanesContainer;
    private final Consumer<Task>               onTaskClick;
    private final Consumer<Task>               onTaskTitleClick;
    private final BiConsumer<Task, TaskStatus> onTaskStatusChange;
    private final Task                         story;
    private       VerticalLayout               todoLane;
    private final Map<UUID, User>              userMap;

    /**
     * Constructs a StoryCard without click handlers.
     *
     * @param story              the story task acting as the board header
     * @param childTasks         mutable list of child tasks to display in lanes
     * @param userMap            user map for avatar display
     * @param onTaskStatusChange callback invoked when a task is dropped into a new status lane
     */
    public StoryCard(Task story, List<Task> childTasks, Map<UUID, User> userMap,
                     BiConsumer<Task, TaskStatus> onTaskStatusChange) {
        this(story, childTasks, userMap, onTaskStatusChange, null, null);
    }

    /**
     * Constructs a StoryCard with a card-body click handler.
     *
     * @param story              the story task
     * @param childTasks         mutable list of child tasks
     * @param userMap            user map for avatar display
     * @param onTaskStatusChange callback for drag-and-drop status changes
     * @param onTaskClick        handler invoked when a task card body is clicked; may be {@code null}
     */
    public StoryCard(Task story, List<Task> childTasks, Map<UUID, User> userMap,
                     BiConsumer<Task, TaskStatus> onTaskStatusChange, Consumer<Task> onTaskClick) {
        this(story, childTasks, userMap, onTaskStatusChange, onTaskClick, null);
    }

    /**
     * Constructs a StoryCard with separate card-body and title click handlers.
     *
     * @param story               the story task
     * @param childTasks          mutable list of child tasks
     * @param userMap             user map for avatar display
     * @param onTaskStatusChange  callback for drag-and-drop status changes
     * @param onTaskClick         handler invoked when a task card body is clicked; may be {@code null}
     * @param onTaskTitleClick    handler invoked when the task title is clicked; may be {@code null}
     */
    public StoryCard(Task story, List<Task> childTasks, Map<UUID, User> userMap,
                     BiConsumer<Task, TaskStatus> onTaskStatusChange, Consumer<Task> onTaskClick,
                     Consumer<Task> onTaskTitleClick) {
        this.story              = story;
        this.childTasks         = childTasks;
        this.userMap            = userMap;
        this.onTaskStatusChange = onTaskStatusChange;
        this.onTaskClick        = onTaskClick;
        this.onTaskTitleClick   = onTaskTitleClick;

        addClassName("story-card");
        setWidthFull();

        createStoryHeader();
        createLanes();
        populateLanes();
        applyStyling();
    }

    private void applyStyling() {
        getStyle()
                .set("margin-bottom", "29px"); // 29px vertical space between story boards
        setWidthFull();
    }

    private void clearLaneTasks(VerticalLayout lane) {
        lane.getChildren()
                .filter(component -> component instanceof TaskCard)
                .collect(Collectors.toList())
                .forEach(lane::remove);
    }

    private VerticalLayout createLane(String title, TaskStatus status) {
        VerticalLayout lane = new VerticalLayout();
        // Add unique ID for Selenium testing: story-name-status
        String laneId = VaadinUtil.generateStoryLaneId(story, status);
        lane.setId(laneId);
        lane.addClassName("task-lane");
        lane.addClassName("task-lane-" + status.name().toLowerCase().replace("_", "-"));
        lane.setWidth("33.33%");
        lane.setPadding(false);
        lane.setSpacing(false); // Disable default spacing
        lane.getStyle()
                .set("padding", "var(--lumo-space-xs)") // compact inner padding
                .set("background", "var(--lumo-contrast-5pct)") // Lighter gray background
                .set("border-radius", "4px") // subtly rounded corners
                .set("min-height", "80px")
                .set("box-sizing", "border-box")
                .set("gap", "2px"); // 2px vertical space between task cards

        // Lane header
        Div laneHeader = new Div();
        laneHeader.addClassName("lane-header");
        Span laneTitle = new Span(title);
        laneTitle.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-xs)") // Extra small text size
                .set("color", "#616161"); // Dark gray color

        Span taskCount = new Span(" (0)");
        taskCount.addClassName("task-count-" + status.name().toLowerCase().replace("_", "-"));
        taskCount.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)") // Extra small text size
                .set("color", "var(--lumo-secondary-text-color)");

        laneHeader.add(laneTitle, taskCount);
        lane.add(laneHeader);

        // Setup drop zone
        setupDropZone(lane, status);

        return lane;
    }

    private void createLanes() {
        lanesContainer = new HorizontalLayout();
        lanesContainer.setWidthFull();
        lanesContainer.setPadding(false);
        lanesContainer.setSpacing(false); // Disable default spacing
        lanesContainer.getStyle()
                .set("min-height", "100px")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)") // tighter vertical padding
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

    private void createStoryHeader() {
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

        // Story key (bold, smaller, gray)
        Span storyKey = new Span(formatStoryKey(story));
        storyKey.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-xs)") // Smaller text size
                .set("color", "#9E9E9E"); // Gray color

        // Story title (plain black)
        Span storyTitle = new Span(story.getName());
        storyTitle.getStyle()
                .set("font-weight", "normal")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "#000000"); // Plain black

        // Subtask count
        Span subtaskCount = new Span("(" + childTasks.size() + " subtasks)");
        subtaskCount.getStyle()
                .set("font-weight", "normal")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "var(--lumo-secondary-text-color)");

        // Story status badge with color based on status
        Span statusBadge = new Span(story.getEffectiveStatus().name());

        // Determine background color based on status
        String backgroundColor = switch (story.getEffectiveStatus()) {
            case TODO -> "var(--lumo-contrast-60pct)"; // Gray for open/todo tasks
            case IN_PROGRESS -> "var(--lumo-primary-color)"; // Blue for in-progress tasks
            case DONE -> "var(--lumo-success-color)"; // Green for completed tasks
        };

        statusBadge.getStyle()
                .set("padding", "1px 4px") // Minimal padding
                .set("border-radius", "3px")
                .set("font-size", "var(--lumo-font-size-xs)") // Extra small text size
                .set("font-weight", "normal")
                .set("background", backgroundColor)
                .set("color", "white");

        header.add(expandIcon, storyKey, storyTitle, subtaskCount, statusBadge);
        add(header);
    }

    private String formatStoryKey(Task story) {
        if (story.getId() != null) {
            return "STORY-" + story.getId();
        }
        return "STORY-???";
    }

    public Task getStory() {
        return story;
    }

    public boolean isExpanded() {
        return expanded;
    }

    private void populateLanes() {
        // Clear existing tasks
        clearLaneTasks(todoLane);
        clearLaneTasks(inProgressLane);
        clearLaneTasks(doneLane);

        // Group tasks by status
        Map<TaskStatus, List<Task>> tasksByStatus = childTasks.stream()
                .collect(Collectors.groupingBy(Task::getTaskStatus));

        // Populate each lane
        tasksByStatus.getOrDefault(TaskStatus.TODO, List.of()).forEach(task -> {
            TaskCard card = new TaskCard(task, userMap,
                    onTaskClick != null ? () -> onTaskClick.accept(task) : null,
                    onTaskTitleClick != null ? () -> onTaskTitleClick.accept(task) : null);
            setupTaskCardDragHandlers(card, task);
            todoLane.add(card);
        });

        tasksByStatus.getOrDefault(TaskStatus.IN_PROGRESS, List.of()).forEach(task -> {
            TaskCard card = new TaskCard(task, userMap,
                    onTaskClick != null ? () -> onTaskClick.accept(task) : null,
                    onTaskTitleClick != null ? () -> onTaskTitleClick.accept(task) : null);
            setupTaskCardDragHandlers(card, task);
            inProgressLane.add(card);
        });

        tasksByStatus.getOrDefault(TaskStatus.DONE, List.of()).forEach(task -> {
            TaskCard card = new TaskCard(task, userMap,
                    onTaskClick != null ? () -> onTaskClick.accept(task) : null,
                    onTaskTitleClick != null ? () -> onTaskTitleClick.accept(task) : null);
            setupTaskCardDragHandlers(card, task);
            doneLane.add(card);
        });

        // Update task counts
        updateTaskCount(todoLane, tasksByStatus.getOrDefault(TaskStatus.TODO, List.of()).size(), TaskStatus.TODO);
        updateTaskCount(inProgressLane, tasksByStatus.getOrDefault(TaskStatus.IN_PROGRESS, List.of()).size(), TaskStatus.IN_PROGRESS);
        updateTaskCount(doneLane, tasksByStatus.getOrDefault(TaskStatus.DONE, List.of()).size(), TaskStatus.DONE);
    }

    public void refresh(List<Task> updatedChildTasks) {
        this.childTasks.clear();
        this.childTasks.addAll(updatedChildTasks);
        populateLanes();
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        updateExpandState();
    }

    private void setupDropZone(VerticalLayout lane, TaskStatus targetStatus) {
        // Create drop target using Vaadin's DropTarget API
        DropTarget<VerticalLayout> dropTarget = DropTarget.create(lane);
        dropTarget.setActive(true);

        dropTarget.addDropListener(event -> {
            // Get drag source component
            event.getDragSourceComponent().ifPresent(source -> {
                if (source instanceof TaskCard taskCard) {
                    Task task = taskCard.getTask();
                    // Only process if status actually changed
                    if (task.getTaskStatus() != targetStatus) {
                        // The callback will update the task status and refresh the board
                        onTaskStatusChange.accept(task, targetStatus);
                    }
                }
            });
            // Reset background after drop
            lane.getStyle().set("background", "var(--lumo-contrast-5pct)");
        });

        // Add visual feedback on drag over
        lane.getElement().addEventListener("dragenter", e -> {
            lane.getStyle().set("background", "var(--lumo-primary-color-10pct)");
        });

        lane.getElement().addEventListener("dragleave", e -> {
            lane.getStyle().set("background", "var(--lumo-contrast-5pct)");
        });
    }

    private void setupTaskCardDragHandlers(TaskCard card, Task task) {
        // No longer needed - TaskCard handles its own drag styling via DragSource
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

    private void updateTaskCount(VerticalLayout lane, int count, TaskStatus status) {
        String className = "task-count-" + status.name().toLowerCase().replace("_", "-");
        // The Span is inside the lane header Div, so we need to search recursively
        lane.getChildren()
                .filter(component -> component instanceof Div)
                .findFirst()
                .ifPresent(laneHeader -> {
                    laneHeader.getChildren()
                            .filter(component -> component.getElement().getClassList().contains(className))
                            .findFirst()
                            .ifPresent(component -> {
                                if (component instanceof Span) {
                                    ((Span) component).setText(" (" + count + ")");
                                }
                            });
                });
    }
}

