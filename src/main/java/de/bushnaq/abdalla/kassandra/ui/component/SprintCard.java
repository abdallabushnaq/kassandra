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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A component representing a sprint with its stories in the Backlog view.
 * <p>
 * Displays a sprint header with key, name, and date range, followed by
 * BacklogStoryCard components for each story in the sprint.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Slf4j
public class SprintCard extends Div {

    private final List<Task>          allTasks;
    private final DateTimeFormatter   dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private       Icon                expandIcon;
    private       boolean             expanded      = true; // Default to expanded
    private final String              searchText;
    private final java.util.Set<User> selectedUsers;
    private final Sprint              sprint;
    private final List<Task>          stories;
    private       VerticalLayout      storiesContainer;
    private final Map<Long, User>     userMap;

    public SprintCard(Sprint sprint, List<Task> stories, List<Task> allTasks, Map<Long, User> userMap,
                      String searchText, java.util.Set<User> selectedUsers) {
        this.sprint        = sprint;
        this.stories       = stories;
        this.allTasks      = allTasks;
        this.userMap       = userMap;
        this.searchText    = searchText != null ? searchText : "";
        this.selectedUsers = selectedUsers != null ? selectedUsers : new java.util.HashSet<>();

        addClassName("sprint-card");
        setWidthFull();

        // Create container with gray background that wraps everything
        Div grayContainer = new Div();
        grayContainer.setWidthFull();
        grayContainer.getStyle()
                .set("background", "#F5F5F5") // Light gray background
                .set("border-radius", "8px") // Rounded corners
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)") // Padding all around
                .set("box-sizing", "border-box");

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(false);
        contentLayout.setWidthFull();

        // Add header and stories container to content layout
        contentLayout.add(createSprintHeader());
        createStoriesContainer();
        contentLayout.add(storiesContainer);

        grayContainer.add(contentLayout);
        add(grayContainer);

        applyStyling();
    }

    private void applyStyling() {
        getStyle()
                .set("margin-bottom", "29px"); // 29px vertical space between sprint cards
        setWidthFull();
    }

    private HorizontalLayout createSprintHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(true);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.getStyle()
                .set("cursor", "pointer")
                .set("margin-bottom", "var(--lumo-space-s)"); // Space between header and stories

        // Expand/collapse arrow icon
        expandIcon = new Icon(expanded ? VaadinIcon.CHEVRON_DOWN : VaadinIcon.CHEVRON_RIGHT);
        expandIcon.getStyle()
                .set("color", "#000000")
                .set("width", "12px")
                .set("height", "12px")
                .set("flex-shrink", "0");

        // Sprint key (bold, small, gray)
        Span sprintKey = new Span(formatSprintKey(sprint));
        sprintKey.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "#9E9E9E")
                .set("white-space", "nowrap")
                .set("margin-right", "var(--lumo-space-xs)");

        // Sprint name (plain black)
        Span sprintName = new Span(sprint.getName());
        sprintName.getStyle()
                .set("font-weight", "normal")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "#000000")
                .set("margin-right", "var(--lumo-space-m)");

        // Date range (gray, secondary)
        Span dateRange = new Span(formatDateRange(sprint));
        dateRange.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("white-space", "nowrap");

        // Story count
        Span storyCount = new Span("(" + stories.size() + " stories)");
        storyCount.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "var(--lumo-space-s)");

        header.add(expandIcon, sprintKey, sprintName, dateRange, storyCount);

        // Click handler to toggle expand/collapse
        header.addClickListener(e -> toggleExpand());

        return header;
    }

    private void createStoriesContainer() {
        storiesContainer = new VerticalLayout();
        storiesContainer.setPadding(false);
        storiesContainer.setSpacing(false);
        storiesContainer.setWidthFull();
        storiesContainer.getStyle()
                .set("margin-top", "var(--lumo-space-xs)"); // Small top margin

        populateStories();
        storiesContainer.setVisible(expanded);
    }

    private String formatDateRange(Sprint sprint) {
        if (sprint.getStart() == null || sprint.getEnd() == null) {
            return "No dates set";
        }

        String startDate = sprint.getStart().format(dateFormatter);
        String endDate   = sprint.getEnd().format(dateFormatter);

        return startDate + " - " + endDate;
    }

    private String formatSprintKey(Sprint sprint) {
        if (sprint.getId() != null) {
            return "S-" + sprint.getId();
        }
        return "S-???";
    }

    public boolean isExpanded() {
        return expanded;
    }

    private void populateStories() {
        storiesContainer.removeAll();

        if (stories.isEmpty()) {
            Div emptyMessage = new Div();
            emptyMessage.setText("No stories in this sprint");
            emptyMessage.getStyle()
                    .set("padding", "var(--lumo-space-m)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
            storiesContainer.add(emptyMessage);
            return;
        }

        // For each story, get its child tasks and apply filters
        for (Task story : stories) {
            List<Task> childTasks = allTasks.stream()
                    .filter(t -> t.getParentTaskId() != null && t.getParentTaskId().equals(story.getId()))
                    .collect(Collectors.toList());

            // Apply search filter if active
            if (!searchText.isEmpty()) {
                childTasks = childTasks.stream()
                        .filter(t -> (t.getName() != null && t.getName().toLowerCase().contains(searchText)) ||
                                (t.getId() != null && ("T-" + t.getId()).toLowerCase().contains(searchText)))
                        .collect(Collectors.toList());
            }

            // Apply user filter if active
            if (!selectedUsers.isEmpty()) {
                childTasks = childTasks.stream()
                        .filter(t -> t.getResourceId() != null && selectedUsers.stream()
                                .anyMatch(user -> user.getId().equals(t.getResourceId())))
                        .collect(Collectors.toList());
            }

            // Only add story if it has tasks after filtering
            if (!childTasks.isEmpty()) {
                BacklogStoryCard storyCard = new BacklogStoryCard(story, childTasks, userMap);
                storiesContainer.add(storyCard);
            }
        }

        // If no stories have visible tasks after filtering, show message
        if (storiesContainer.getComponentCount() == 0) {
            Div emptyMessage = new Div();
            emptyMessage.setText("No tasks match the current filters");
            emptyMessage.getStyle()
                    .set("padding", "var(--lumo-space-m)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
            storiesContainer.add(emptyMessage);
        }
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (storiesContainer != null) {
            storiesContainer.setVisible(expanded);
        }
        if (expandIcon != null) {
            expandIcon.getElement().setAttribute("icon",
                    expanded ? "vaadin:chevron-down" : "vaadin:chevron-right");
        }
    }

    private void toggleExpand() {
        setExpanded(!expanded);
    }
}

