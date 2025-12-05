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
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A component representing a story with its child tasks in Features mode.
 * <p>
 * The story itself is displayed as a card (non-draggable as per requirements),
 * followed by its child tasks which are indented 6px to the right and individually draggable.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Slf4j
public class StoryTaskCard extends VerticalLayout {

    private final List<Task>                   childTasks;
    private final Consumer<Task>               onTaskClick;
    private final BiConsumer<Task, TaskStatus> onTaskStatusChange;
    private final boolean                      showSimplifiedHeader;
    private final Task                         story;
    private final Map<Long, User>              userMap;

    public StoryTaskCard(Task story, List<Task> childTasks, Map<Long, User> userMap,
                         BiConsumer<Task, TaskStatus> onTaskStatusChange, boolean showSimplifiedHeader) {
        this(story, childTasks, userMap, onTaskStatusChange, showSimplifiedHeader, null);
    }

    public StoryTaskCard(Task story, List<Task> childTasks, Map<Long, User> userMap,
                         BiConsumer<Task, TaskStatus> onTaskStatusChange, boolean showSimplifiedHeader,
                         Consumer<Task> onTaskClick) {
        this.story                = story;
        this.childTasks           = childTasks;
        this.userMap              = userMap;
        this.onTaskStatusChange   = onTaskStatusChange;
        this.showSimplifiedHeader = showSimplifiedHeader;
        this.onTaskClick          = onTaskClick;

        setPadding(false);
        setSpacing(false);
        setWidthFull();

        log.info("Creating StoryTaskCard for story: {} (ID: {}) with {} child tasks, simplified: {}",
                story.getName(), story.getId(), childTasks.size(), showSimplifiedHeader);

        createContent();
    }

    private void createContent() {
        // Create container with gray background and rounded edges
        Div containerBox = new Div();
        containerBox.setWidthFull(); // Ensure full width
        containerBox.getStyle()
                .set("background", "#E8E8E8") // Darker gray background for better visibility
                .set("border", "1px solid #D0D0D0") // Subtle border for definition
                .set("border-radius", "8px") // Rounded edges
                .set("padding", "8px") // Padding inside the box
                .set("margin-bottom", "6px") // 6px margin to bottom
                .set("box-sizing", "border-box")
                .set("width", "100%"); // Explicitly set width

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(false);
        contentLayout.setWidthFull();

        // Choose header style based on mode
        if (showSimplifiedHeader) {
            // Simplified header: just key and name on one line (for tasks in different lane than story)
            HorizontalLayout storyHeader = createSimplifiedStoryHeader();
            contentLayout.add(storyHeader);
        } else {
            // Full story card (for when all tasks are in same lane as story)
            Div storyCard = createStoryCard();
            contentLayout.add(storyCard);
        }

        // Create container for child tasks with 6px left indentation
        if (!childTasks.isEmpty()) {
            VerticalLayout taskContainer = new VerticalLayout();
            taskContainer.setPadding(false);
            taskContainer.setSpacing(false);
            taskContainer.setWidthFull();
            taskContainer.getStyle()
                    .set("margin-left", "6px")
                    .set("margin-top", "2px") // Add small top margin
                    .set("gap", "2px"); // 2px vertical space between tasks

            // Add each child task - TaskCard handles its own drag behavior
            for (Task task : childTasks) {
                TaskCard taskCard = new TaskCard(task, userMap, onTaskClick != null ? () -> onTaskClick.accept(task) : null);
                taskContainer.add(taskCard);
            }

            contentLayout.add(taskContainer);
        }

        containerBox.add(contentLayout);
        add(containerBox);
    }

    /**
     * Creates a simplified story header with just key and name on one line
     * Similar to how stories are displayed in "group by Story" mode
     */
    private HorizontalLayout createSimplifiedStoryHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(true);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.getStyle()
                .set("margin-bottom", "4px"); // Small margin below header

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

        header.add(storyKey, storyTitle);
        return header;
    }

    /**
     * Creates a full story card with status badge and task count
     * Used when all tasks are in the same lane as the story
     */
    private Div createStoryCard() {
        Div storyCard = new Div();
        storyCard.addClassName("story-card");
        storyCard.setWidthFull();

        // Story card styling (similar to TaskCard but not draggable)
        storyCard.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("margin-bottom", "2px")
                .set("box-sizing", "border-box");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        // Top row: Story title (left) and status badge (right)
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(HorizontalLayout.Alignment.START);
        topRow.setPadding(false);
        topRow.setSpacing(true);

        Span title = new Span(story.getName());
        title.addClassName("story-card-title");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-m)")
                .set("font-weight", "500")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap")
                .set("flex", "1");

        // Story status badge
        Span statusBadge = new Span(story.getEffectiveStatus().name());
        statusBadge.getStyle()
                .set("padding", "1px 4px")
                .set("border-radius", "3px")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "normal")
                .set("background", "#1976D2") // Darker blue background
                .set("color", "white")
                .set("white-space", "nowrap");

        topRow.add(title, statusBadge);

        // Bottom row: Story key (left) and task count (right)
        HorizontalLayout bottomRow = new HorizontalLayout();
        bottomRow.setWidthFull();
        bottomRow.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        bottomRow.setAlignItems(HorizontalLayout.Alignment.END);
        bottomRow.setPadding(false);
        bottomRow.setSpacing(true);

        Span storyKey = new Span(formatStoryKey(story));
        storyKey.addClassName("story-card-key");
        storyKey.getStyle()
                .set("font-weight", "bold")
                .set("color", "#9E9E9E") // Gray color
                .set("font-size", "var(--lumo-font-size-xs)");

        Span taskCount = new Span(childTasks.size() + " tasks");
        taskCount.addClassName("story-card-task-count");
        taskCount.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "#616161")
                .set("background", "#E0E0E0")
                .set("padding", "2px 6px")
                .set("border-radius", "4px");

        bottomRow.add(storyKey, taskCount);

        content.add(topRow, bottomRow);
        storyCard.add(content);

        return storyCard;
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
}
