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
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;

/**
 * A read-only card component representing a task in the Backlog view.
 * <p>
 * Displays task information in a single line including key, name, status,
 * effort estimate, and user avatar. Features a colored left border matching
 * the assigned user's color.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Slf4j
public class BacklogTaskCard extends Div {

    private final Task            task;
    private final Map<Long, User> userMap;

    public BacklogTaskCard(Task task, Map<Long, User> userMap) {
        this.task    = task;
        this.userMap = userMap;

        addClassName("backlog-task-card");
        setWidthFull();

        createTaskLine();
        applyStyling();
    }

    private void applyStyling() {
        // Get user color for left border
        String userColor = getUserColor();

        getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", "4px solid " + userColor) // 4px colored left border
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-xs)")
                .set("box-sizing", "border-box")
                .set("margin-left", "6px") // 6px indentation
                .set("margin-bottom", "2px"); // 2px vertical spacing
    }

    /**
     * Convert java.awt.Color to hex string
     */
    private String colorToHex(java.awt.Color color) {
        if (color == null) {
            return "#E0E0E0";
        }
        return "#" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase();
    }

    private void createTaskLine() {
        HorizontalLayout line = new HorizontalLayout();
        line.setWidthFull();
        line.setPadding(false);
        line.setSpacing(true);
        line.setAlignItems(HorizontalLayout.Alignment.CENTER);
        line.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);

        // Task key (bold, small)
        Span taskKey = new Span(formatTaskKey(task));
        taskKey.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "#9E9E9E")
                .set("white-space", "nowrap")
                .set("margin-right", "var(--lumo-space-xs)");

        // Task name (normal, can wrap)
        Span taskName = new Span(task.getName());
        taskName.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("flex", "1")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        // Status badge
        Span statusBadge = new Span(task.getTaskStatus().name());
        statusBadge.getStyle()
                .set("padding", "1px 4px")
                .set("border-radius", "3px")
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("font-weight", "normal")
                .set("background", getStatusColor(task.getTaskStatus().name()))
                .set("color", "white")
                .set("white-space", "nowrap")
                .set("margin-left", "var(--lumo-space-xs)")
                .set("margin-right", "var(--lumo-space-xs)");

        // Effort estimate
        Span effort = new Span(formatDuration(task.getOriginalEstimate()));
        effort.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("white-space", "nowrap")
                .set("margin-right", "var(--lumo-space-xs)");

        // User avatar (if assigned)
        com.vaadin.flow.component.Component avatarComponent = getUserAvatar();

        line.add(taskKey, taskName, statusBadge, effort);
        if (avatarComponent != null) {
            line.add(avatarComponent);
        }

        add(line);
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

    private String formatTaskKey(Task task) {
        if (task.getId() != null) {
            return "T-" + task.getId();
        }
        return "T-???";
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "TODO" -> "var(--lumo-contrast-60pct)"; // Gray
            case "IN_PROGRESS" -> "var(--lumo-primary-color)"; // Blue
            case "DONE" -> "var(--lumo-success-color)"; // Green
            default -> "var(--lumo-secondary-text-color)"; // Default gray
        };
    }

    private com.vaadin.flow.component.Component getUserAvatar() {
        if (task.getResourceId() == null) {
            return null;
        }

        User user = userMap.get(task.getResourceId());
        if (user == null) {
            return null;
        }

        // Use actual avatar image with same styling as TaskCard in ActiveSprints
        com.vaadin.flow.component.html.Image avatar = new com.vaadin.flow.component.html.Image();
        avatar.setWidth("24px");
        avatar.setHeight("24px");
        avatar.getStyle()
                .set("border-radius", "4px")
                .set("object-fit", "cover")
                .set("display", "inline-block")
                .set("vertical-align", "middle");
        avatar.setSrc(user.getAvatarUrl());
        avatar.setAlt(user.getName());
        avatar.getElement().setProperty("title", user.getName());

        return avatar;
    }

    private String getUserColor() {
        if (task.getResourceId() == null) {
            return "var(--lumo-contrast-20pct)"; // Light gray for unassigned
        }

        User user = userMap.get(task.getResourceId());
        if (user == null || user.getColor() == null) {
            return "var(--lumo-contrast-20pct)";
        }

        return colorToHex(user.getColor());
    }
}

