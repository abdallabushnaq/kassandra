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
        Div avatarContainer = getUserAvatar();

        line.add(taskKey, taskName, statusBadge, effort);
        if (avatarContainer != null) {
            line.add(avatarContainer);
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

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }

        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        } else {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "TODO" -> "#757575"; // Gray
            case "IN_PROGRESS" -> "#1976D2"; // Blue
            case "DONE" -> "#388E3C"; // Green
            default -> "#9E9E9E"; // Default gray
        };
    }

    private Div getUserAvatar() {
        if (task.getResourceId() == null) {
            return null;
        }

        User user = userMap.get(task.getResourceId());
        if (user == null) {
            return null;
        }

        Div avatar = new Div();
        avatar.getStyle()
                .set("width", "24px")
                .set("height", "24px")
                .set("border-radius", "50%")
                .set("background", colorToHex(user.getColor()))
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("color", "white")
                .set("font-size", "10px")
                .set("font-weight", "bold")
                .set("flex-shrink", "0");

        // Add initials
        String initials     = getInitials(user.getName());
        Span   initialsSpan = new Span(initials);
        avatar.add(initialsSpan);

        return avatar;
    }

    private String getUserColor() {
        if (task.getResourceId() == null) {
            return "#E0E0E0"; // Light gray for unassigned
        }

        User user = userMap.get(task.getResourceId());
        if (user == null || user.getColor() == null) {
            return "#E0E0E0";
        }

        return colorToHex(user.getColor());
    }
}

