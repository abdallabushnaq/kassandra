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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;

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
public class TaskCard extends Div {

    private final Task            task;
    private final Map<Long, User> userMap;

    public TaskCard(Task task, Map<Long, User> userMap) {
        this.task    = task;
        this.userMap = userMap;

        addClassName("task-card");
        setId("task-card-" + task.getId());
        setWidthFull(); // Make card use full width of the lane

        // Make the card draggable
        getElement().setAttribute("draggable", "true");
        getElement().setProperty("taskId", task.getId().toString());

        createCardContent();
        applyStyling();
    }

    private void applyStyling() {
        getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
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
        taskKey.getStyle().set("color", "#9E9E9E"); // Gray color
        taskKey.getStyle().set("font-size", "var(--lumo-font-size-xs)"); // Smaller text size

        Span remainingEffort = new Span(formatDuration(task.getRemainingEstimate()));
        remainingEffort.addClassName("task-card-remaining");
        remainingEffort.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        remainingEffort.getStyle().set("color", "#616161"); // Dark gray text
        remainingEffort.getStyle().set("background", "#E0E0E0"); // Gray background
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
}

