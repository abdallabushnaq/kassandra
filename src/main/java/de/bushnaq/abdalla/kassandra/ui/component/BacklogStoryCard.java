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

import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * A component representing a story with its child tasks in the Backlog view.
 * <p>
 * Displays a story header with key, name, status, and effort estimate.
 * Can be expanded to show child tasks which are indented 6px to the right.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Slf4j
public class BacklogStoryCard extends Div {

    private final List<Task>             childTasks;
    private final BacklogDragDropHandler dragDropHandler;
    private       Icon                   expandIcon;
    private       boolean                expanded = true; // Default to expanded
    private final Sprint                 sprint;
    private final Task                   story;
    private       VerticalLayout         tasksContainer;
    private final Map<Long, User>        userMap;

    public BacklogStoryCard(Task story, List<Task> childTasks, Map<Long, User> userMap,
                            Sprint sprint, BacklogDragDropHandler dragDropHandler) {
        this.story           = story;
        this.childTasks      = childTasks;
        this.userMap         = userMap;
        this.sprint          = sprint;
        this.dragDropHandler = dragDropHandler;

        addClassName("backlog-story-card");
        setWidthFull();

        createContent();
        applyStyling();
        setupDragSource();
        setupDropTarget();
    }

    private void applyStyling() {
        getStyle()
                .set("margin-bottom", "6px"); // 6px vertical spacing between stories
    }

    private void createContent() {
        // Create container with gray background and rounded edges
        Div containerBox = new Div();
        containerBox.setWidthFull();
        containerBox.getStyle()
                .set("background", "var(--lumo-contrast-5pct)") // Gray background
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("padding", "8px")
                .set("box-sizing", "border-box");

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(false);
        contentLayout.setWidthFull();

        // Story header
        HorizontalLayout storyHeader = createStoryHeader();
        contentLayout.add(storyHeader);

        // Tasks container (initially visible if expanded)
        tasksContainer = createTasksContainer();
        tasksContainer.setVisible(expanded);
        contentLayout.add(tasksContainer);

        containerBox.add(contentLayout);
        add(containerBox);
    }

    private HorizontalLayout createStoryHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(true);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        header.getStyle()
                .set("cursor", "pointer")
                .set("padding", "4px 0");

        // Expand/collapse arrow icon
        expandIcon = new Icon(expanded ? VaadinIcon.CHEVRON_DOWN : VaadinIcon.CHEVRON_RIGHT);
        expandIcon.getStyle()
                .set("color", "var(--lumo-body-text-color)")
                .set("width", "12px")
                .set("height", "12px")
                .set("flex-shrink", "0");

        // Story key (bold, small, gray)
        Span storyKey = new Span(formatStoryKey(story));
        storyKey.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("white-space", "nowrap")
                .set("margin-right", "var(--lumo-space-xs)");

        // Story name (normal, can wrap)
        Span storyName = new Span(story.getName());
        storyName.getStyle()
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "var(--lumo-body-text-color)")
                .set("flex", "1")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        // Status badge with color based on status
        Span statusBadge = new Span(story.getEffectiveStatus().name());

        // Determine background color based on status
        String backgroundColor = switch (story.getEffectiveStatus()) {
            case TODO -> "var(--lumo-contrast-60pct)"; // Gray for open/todo tasks
            case IN_PROGRESS -> "var(--lumo-primary-color)"; // Blue for in-progress tasks
            case DONE -> "var(--lumo-success-color)"; // Green for completed tasks
        };

        statusBadge.getStyle()
                .set("padding", "2px 6px")
                .set("border-radius", "3px")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "normal")
                .set("background", backgroundColor)
                .set("color", "white")
                .set("white-space", "nowrap")
                .set("margin-left", "var(--lumo-space-s)")
                .set("margin-right", "var(--lumo-space-s)");

        // Effort estimate
        Span effort = new Span(formatDuration(story.getMinEstimate()));
        effort.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("white-space", "nowrap")
                .set("margin-right", "var(--lumo-space-xs)");

        // Task count indicator
        Span taskCount = new Span("(" + childTasks.size() + " tasks)");
        taskCount.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("white-space", "nowrap");

        header.add(expandIcon, storyKey, storyName, statusBadge, effort, taskCount);

        // Click handler to toggle expand/collapse
        header.addClickListener(e -> toggleExpand());

        return header;
    }

    private VerticalLayout createTasksContainer() {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();
        container.getStyle()
                .set("margin-top", "4px"); // Small top margin

        // Add each child task
        for (Task task : childTasks) {
            BacklogTaskCard taskCard = new BacklogTaskCard(task, userMap, sprint, dragDropHandler);
            container.add(taskCard);
        }

        return container;
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isZero()) {
            return "0h";
        }

        long hours = duration.toHours();
        if (hours < 8) {
            return hours + "h";
        } else {
            long days           = hours / 8;
            long remainingHours = hours % 8;
            if (remainingHours == 0) {
                return days + "d";
            } else {
                return days + "d " + remainingHours + "h";
            }
        }
    }

    private String formatStoryKey(Task story) {
        if (story.getId() != null) {
            return "STORY-" + story.getId();
        }
        return "STORY-???";
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (tasksContainer != null) {
            tasksContainer.setVisible(expanded);
        }
        if (expandIcon != null) {
            expandIcon.getElement().setAttribute("icon",
                    expanded ? "vaadin:chevron-down" : "vaadin:chevron-right");
        }
    }

    /**
     * Setup this card as a drag source.
     */
    private void setupDragSource() {
        DragSource<BacklogStoryCard> dragSource = DragSource.create(this);
        dragSource.setDraggable(true);
        dragSource.setDragData(story);

        dragSource.addDragStartListener(e -> {
            dragDropHandler.onDragStart(story, sprint);
            getStyle().set("opacity", "0.5");
        });

        dragSource.addDragEndListener(e -> {
            getStyle().remove("opacity");
        });
    }

    /**
     * Setup this card as a drop target for reordering.
     */
    private void setupDropTarget() {
        DropTarget<BacklogStoryCard> dropTarget = DropTarget.create(this);
        dropTarget.setDropEffect(DropEffect.MOVE);

        dropTarget.addDropListener(event -> {
            Task draggedTask = dragDropHandler.getDraggedTask();
            if (draggedTask != null && !draggedTask.equals(story)) {
                // Drop before this story
                dragDropHandler.handleDropOnTask(story, sprint, false);
            }
        });

        // Visual feedback for drag over using element event listeners
        getElement().addEventListener("dragenter", e ->
                getStyle().set("outline", "2px dashed var(--lumo-primary-color)")
        );
        getElement().addEventListener("dragleave", e ->
                getStyle().remove("outline")
        );
        getElement().addEventListener("drop", e ->
                getStyle().remove("outline")
        );
    }

    private void toggleExpand() {
        setExpanded(!expanded);
    }
}

