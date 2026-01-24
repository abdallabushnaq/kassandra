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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.util.ColorUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;

/**
 * A card component representing a task in a Scrum board.
 * <p>
 * Displays task information including key, title, remaining effort, and assigned user.
 * The card is draggable and can be moved between different status lanes.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Slf4j
public class TaskCard extends Div {

    private final Runnable        onClickHandler;
    private final Task            task;
    private final Map<Long, User> userMap;

    public TaskCard(Task task, Map<Long, User> userMap) {
        this(task, userMap, null);
    }

    public TaskCard(Task task, Map<Long, User> userMap, Runnable onClickHandler) {
        this.task           = task;
        this.userMap        = userMap;
        this.onClickHandler = onClickHandler;

        addClassName("task-card");
        setId("task-card-" + task.getId());
        setWidthFull(); // Make card use full width of the lane

        createCardContent();
        applyStyling();

        // Add click listener if handler is provided
        if (onClickHandler != null) {
            getElement().addEventListener("click", e -> onClickHandler.run());
        }

        log.info("Creating TaskCard for task: {} (ID: {})", task.getName(), task.getId());

        // Make the card draggable using Vaadin's DragSource API
        DragSource<TaskCard> dragSource = DragSource.create(this);
        dragSource.setDragData(task); // Store the task object for type-safe retrieval
        dragSource.addDragStartListener(event -> {
            log.info("Drag START for TaskCard: {} (ID: {})", task.getName(), task.getId());
            addClassName("dragging");
        });
        dragSource.addDragEndListener(event -> {
            log.info("Drag END for TaskCard: {} (ID: {})", task.getName(), task.getId());
            removeClassName("dragging");
        });

        log.info("DragSource created and configured for TaskCard: {}", task.getId());
    }

    private void applyStyling() {
        // Get user color for left border
        String userColor = getUserColor();

        getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", "4px solid " + userColor) // 4px colored left border
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)")
                .set("cursor", "grab")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("transition", "all 0.2s ease")
                .set("box-sizing", "border-box");

        // Hover effect using JavaScript
        getElement().executeJs(
                "this.addEventListener('mouseenter', () => { " +
                        "  this.style.boxShadow = 'var(--lumo-box-shadow-s)'; " +
                        "  this.style.transform = 'translateY(-2px)'; " +
                        "});" +
                        "this.addEventListener('mouseleave', () => { " +
                        "  this.style.boxShadow = 'var(--lumo-box-shadow-xs)'; " +
                        "  this.style.transform = 'translateY(0)'; " +
                        "});"
        );
    }


    private void createCardContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        // Top row: Task title (left) and assigned user (right)
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(HorizontalLayout.Alignment.START);
        topRow.setPadding(false);
        topRow.setSpacing(true);

        Span title = new Span(task.getName());
        title.addClassName("task-card-title");
        title.getStyle().set("font-size", "var(--lumo-font-size-m)");
        title.getStyle().set("font-weight", "500");
        title.getStyle().set("overflow", "hidden");
        title.getStyle().set("text-overflow", "ellipsis");
        title.getStyle().set("white-space", "nowrap");
        title.getStyle().set("flex", "1");

        topRow.add(title, getAssignedUserComponent());

        // Bottom row: Task key (left) and remaining effort (right)
        HorizontalLayout bottomRow = new HorizontalLayout();
        bottomRow.setWidthFull();
        bottomRow.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        bottomRow.setAlignItems(HorizontalLayout.Alignment.END);
        bottomRow.setPadding(false);
        bottomRow.setSpacing(true);

        Span taskKey = new Span(formatTaskKey(task));
        taskKey.addClassName("task-card-key");
        taskKey.getStyle().set("font-weight", "bold");
        taskKey.getStyle().set("color", "var(--lumo-secondary-text-color)"); // Gray color
        taskKey.getStyle().set("font-size", "var(--lumo-font-size-xs)"); // Smaller text size

        Span remainingEffort = new Span(formatDuration(task.getRemainingEstimate()));
        remainingEffort.addClassName("task-card-remaining");
        remainingEffort.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        remainingEffort.getStyle().set("color", "var(--lumo-secondary-text-color)"); // Dark gray text
        remainingEffort.getStyle().set("background", "var(--lumo-contrast-20pct)"); // Gray background
        remainingEffort.getStyle().set("padding", "2px 6px"); // Padding for badge look
        remainingEffort.getStyle().set("border-radius", "4px"); // Rounded corners

        bottomRow.add(taskKey, remainingEffort);

        content.add(topRow, bottomRow);
        add(content);
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isZero()) {
            return "0d";
        }

        long totalHours = duration.toHours();
        long days       = totalHours / 8; // Assuming 8-hour workday
        long hours      = totalHours % 8;

        if (days > 0 && hours > 0) {
            return String.format("%dd %dh", days, hours);
        } else if (days > 0) {
            return String.format("%dd", days);
        } else {
            return String.format("%dh", hours);
        }
    }

    private String formatTaskKey(Task task) {
        if (task.getId() != null) {
            return "TASK-" + task.getId();
        }
        return "TASK-???";
    }

    private com.vaadin.flow.component.Component getAssignedUserComponent() {
        if (task.getResourceId() != null && userMap.containsKey(task.getResourceId())) {
            User user = userMap.get(task.getResourceId());
//            if (user.getAvatarImage() != null && user.getAvatarImage().length > 0)
            {
                com.vaadin.flow.component.html.Image avatar = new com.vaadin.flow.component.html.Image();
                avatar.setWidth("24px");
                avatar.setHeight("24px");
                avatar.getStyle()
                        .set("border-radius", "4px")
                        .set("object-fit", "cover")
                        .set("display", "inline-block")
                        .set("vertical-align", "middle");
//                com.vaadin.flow.server.StreamResource resource = new com.vaadin.flow.server.StreamResource(
//                        "task-user-avatar-" + user.getId() + ".png",
//                        () -> new java.io.ByteArrayInputStream(user.getAvatarImage())
//                );
//                resource.setContentType("image/png");
//                resource.setCacheTime(0);
//                avatar.setSrc(resource);
                avatar.setSrc(user.getAvatarUrl());
                avatar.setAlt(user.getName());
                avatar.getElement().setProperty("title", user.getName());
                return avatar;
            }
//            else {
//                Span name = new Span("\uD83D\uDC64 " + user.getName());
//                name.addClassName("task-card-user");
//                name.getStyle().set("font-size", "var(--lumo-font-size-xs)");
//                name.getStyle().set("color", "var(--lumo-secondary-text-color)");
//                name.getStyle().set("white-space", "nowrap");
//                return name;
//            }
        }
        Span unassigned = new Span("\uD83D\uDC64 Unassigned");
        unassigned.addClassName("task-card-user");
        unassigned.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        unassigned.getStyle().set("color", "var(--lumo-secondary-text-color)");
        unassigned.getStyle().set("white-space", "nowrap");
        return unassigned;
    }

    public Task getTask() {
        return task;
    }

    /**
     * Get the color for the assigned user
     */
    private String getUserColor() {
        if (task.getResourceId() != null && userMap.containsKey(task.getResourceId())) {
            User user = userMap.get(task.getResourceId());
            // Use the user's configured color
            return ColorUtil.colorToHexString(user.getColor());
        }
        // Default gray color for unassigned tasks
        return "var(--lumo-contrast-30pct)";
    }
}

