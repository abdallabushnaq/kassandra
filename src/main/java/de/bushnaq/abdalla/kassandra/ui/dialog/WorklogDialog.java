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

package de.bushnaq.abdalla.kassandra.ui.dialog;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.rest.api.TaskApi;
import de.bushnaq.abdalla.kassandra.rest.api.WorklogApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A dialog for logging work on a task.
 * Allows users to enter time spent, adjust time remaining, add comments, and set the date/time.
 */
@Slf4j
public class WorklogDialog extends Dialog {

    public static final String            CANCEL_BUTTON        = "cancel-worklog-button";
    public static final String            COMMENT_FIELD        = "worklog-comment-field";
    public static final String            DATETIME_PICKER      = "worklog-datetime-picker";
    public static final String            SAVE_BUTTON          = "save-worklog-button";
    public static final String            TIME_REMAINING_FIELD = "worklog-time-remaining-field";
    public static final String            TIME_SPENT_FIELD     = "worklog-time-spent-field";
    public static final String            TITLE_ID             = "worklog-dialog-title";
    public static final String            WORKLOG_DIALOG       = "worklog-dialog";
    private final       TextArea          commentField;
    private final       Long              currentUserId;
    private final       DateTimePicker    dateTimePicker;
    private final       Consumer<Worklog> onSave;
    private final       Task              task;
    private final       TaskApi           taskApi;
    private final       TextField         timeRemainingField;
    private final       TextField         timeSpentField;
    private final       WorklogApi        worklogApi;

    /**
     * Creates a dialog for logging work on a task.
     * Note: Validation should be done before opening this dialog.
     *
     * @param task          The task to log work for
     * @param worklogApi    The worklog API for saving worklog data
     * @param onSave        Callback to execute after successfully saving the worklog
     * @param currentUserId ID of the currently logged-in user
     */
    public WorklogDialog(Task task, TaskApi taskApi, WorklogApi worklogApi, Consumer<Worklog> onSave, Long currentUserId) {
        this.task          = task;
        this.taskApi       = taskApi;
        this.worklogApi    = worklogApi;
        this.onSave        = onSave;
        this.currentUserId = currentUserId;

        setId(WORKLOG_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);

        // Set dialog header with clock icon
        String title = "Log Work";
        getHeader().add(VaadinUtil.createDialogHeader(title, TITLE_ID, VaadinIcon.CLOCK));

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Task information - key and name in one line, no background
        HorizontalLayout taskInfoLayout = new HorizontalLayout();
        taskInfoLayout.setSpacing(true);
        taskInfoLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        taskInfoLayout.setPadding(false);
        taskInfoLayout.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        Span taskKey = new Span(task.getKey());
        taskKey.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        Span separator = new Span("—");
        separator.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "0 var(--lumo-space-xs)");

        Span taskName = new Span(task.getName());
        taskName.getStyle()
                .set("color", "var(--lumo-secondary-text-color)");

        taskInfoLayout.add(taskKey, separator, taskName);
        dialogLayout.add(taskInfoLayout);

        // Time fields side by side
        HorizontalLayout timeFieldsLayout = new HorizontalLayout();
        timeFieldsLayout.setWidthFull();
        timeFieldsLayout.setSpacing(true);

        // Time spent field
        timeSpentField = new TextField("Time Spent");
        timeSpentField.setId(TIME_SPENT_FIELD);
        timeSpentField.setWidth("50%");
        timeSpentField.setRequired(true);
        timeSpentField.setHelperText("e.g., 1d 2h 30m");
        timeSpentField.setPlaceholder("1d 2h 30m");

        // Reserve space for error message to prevent dialog resizing
        timeSpentField.getStyle().set("min-height", "100px");

        // Set value change mode to EAGER for real-time updates
        timeSpentField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);

        // Add value change listener for both validation and real-time updates
        timeSpentField.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                String value = e.getValue().strip();

                // Always update time remaining, even if format is invalid
                updateTimeRemaining(value);

                // Validate the format
                if (!value.isEmpty()) {
                    try {
                        Duration duration = DateUtil.parseWorkDayDurationString(value);
                        if (duration.isZero()) {
                            timeSpentField.setInvalid(true);
                            timeSpentField.setErrorMessage("Time spent must be greater than zero");
                        } else {
                            timeSpentField.setInvalid(false);
                        }
                    } catch (IllegalArgumentException ex) {
                        timeSpentField.setInvalid(true);
                        timeSpentField.setErrorMessage("Invalid format");
                    }
                } else {
                    timeSpentField.setInvalid(false);
                }
            }
        });

        // Time remaining field
        timeRemainingField = new TextField("Time Remaining");
        timeRemainingField.setId(TIME_REMAINING_FIELD);
        timeRemainingField.setWidth("50%");
        timeRemainingField.setHelperText("Auto-calculated");
        timeRemainingField.setPlaceholder("1d 2h 30m");

        // Reserve space for error message to prevent dialog resizing
        timeRemainingField.getStyle().set("min-height", "100px");

        timeRemainingField.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                String value = e.getValue().strip();
                if (!value.isEmpty()) {
                    try {
                        DateUtil.parseWorkDayDurationString(value);
                        timeRemainingField.setInvalid(false);
                    } catch (IllegalArgumentException ex) {
                        timeRemainingField.setInvalid(true);
                        timeRemainingField.setErrorMessage("Invalid format");
                    }
                } else {
                    timeRemainingField.setInvalid(false);
                }
            }
        });

        // Initialize with current remaining estimate
        if (task.getRemainingEstimate() != null && !task.getRemainingEstimate().isZero()) {
            timeRemainingField.setValue(DateUtil.createWorkDayDurationString(task.getRemainingEstimate()));
        }

        timeFieldsLayout.add(timeSpentField, timeRemainingField);
        dialogLayout.add(timeFieldsLayout);

        // Comment field
        commentField = new TextArea("Comment");
        commentField.setId(COMMENT_FIELD);
        commentField.setWidthFull();
        commentField.setPlaceholder("Add a comment about the work done...");
        commentField.setHeight("100px");
        dialogLayout.add(commentField);

        // DateTime picker
        dateTimePicker = new DateTimePicker("Date & Time");
        dateTimePicker.setId(DATETIME_PICKER);
        dateTimePicker.setWidthFull();
        dateTimePicker.setValue(LocalDateTime.now());
        dateTimePicker.setRequiredIndicatorVisible(true);
        dialogLayout.add(dateTimePicker);

        // Buttons
        dialogLayout.add(VaadinUtil.createDialogButtonLayout(
                "Save", SAVE_BUTTON,
                "Cancel", CANCEL_BUTTON,
                this::save,
                this
        ));

        add(dialogLayout);
    }

    /**
     * Saves the worklog
     */
    private void save() {
        try {
            // Check if time spent field is invalid
            if (timeSpentField.isInvalid()) {
                showError("Please enter a valid time spent (e.g., 1d 2h 30m)");
                return;
            }

            // Check if time remaining field is invalid
            if (timeRemainingField.isInvalid()) {
                showError("Please enter a valid time remaining (e.g., 1d 2h 30m)");
                return;
            }

            // Validate time spent is not empty
            String timeSpentValue = timeSpentField.getValue();
            if (timeSpentValue == null || timeSpentValue.isBlank()) {
                showError("Please enter time spent");
                return;
            }

            // Parse time spent
            Duration timeSpent = DateUtil.parseWorkDayDurationString(timeSpentValue.strip());
            if (timeSpent.isZero()) {
                showError("Time spent must be greater than zero");
                return;
            }

            // Parse time remaining (optional)
            Duration timeRemaining      = null;
            String   timeRemainingValue = timeRemainingField.getValue();
            if (timeRemainingValue != null && !timeRemainingValue.isBlank()) {
                timeRemaining = DateUtil.parseWorkDayDurationString(timeRemainingValue.strip());
            }

            // Validate date and time
            if (dateTimePicker.getValue() == null) {
                showError("Please select a date and time");
                return;
            }

            // Create worklog
            Worklog worklog = new Worklog();
            worklog.setTaskId(task.getId());
            worklog.setSprintId(task.getSprintId());
            worklog.setTimeSpent(timeSpent);
            worklog.setComment(commentField.getValue());
            worklog.setAuthorId(currentUserId);
            worklog.setUpdateAuthorId(currentUserId);

            // Convert LocalDateTime to OffsetDateTime (using system default zone, then convert to UTC)
            OffsetDateTime startDateTime = dateTimePicker.getValue().atZone(ZoneOffset.systemDefault()).toOffsetDateTime();
            worklog.setStart(startDateTime);

            // Save via API
            Worklog savedWorklog = worklogApi.persist(worklog);

            task.addTimeSpent(savedWorklog.getTimeSpent());
            task.setRemainingEstimate(timeRemaining);
            task.recalculate();
            taskApi.update(task);


            // If time remaining was specified, update the task's remaining estimate
            if (timeRemaining != null) {
                // This would require a task update API call
                // For now, we'll just log it
                log.info("Time remaining specified: {}, task update needed", timeRemaining);
            }

            showSuccess("Work logged successfully");

            // Call the callback
            if (onSave != null) {
                onSave.accept(savedWorklog);
            }

            close();

        } catch (Exception e) {
            log.error("Error saving worklog", e);
            showError("Failed to save worklog: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    /**
     * Updates the time remaining field based on the provided time spent value.
     *
     * @param timeSpentStr The time spent string value
     */
    private void updateTimeRemaining(String timeSpentStr) {
        if (timeSpentStr == null || timeSpentStr.isBlank()) {
            // Reset to original estimate if time spent is cleared
            if (task.getRemainingEstimate() != null && !task.getRemainingEstimate().isZero()) {
                timeRemainingField.setValue(DateUtil.createWorkDayDurationString(task.getRemainingEstimate()));
            } else {
                timeRemainingField.clear();
            }
            return;
        }

        try {
            Duration timeSpent = DateUtil.parseWorkDayDurationString(timeSpentStr.strip());

            if (task.getRemainingEstimate() != null && !task.getRemainingEstimate().isZero()) {
                Duration remaining = task.getRemainingEstimate().minus(timeSpent);

                // Don't allow negative remaining time
                if (remaining.isNegative()) {
                    remaining = Duration.ZERO;
                }

                timeRemainingField.setValue(DateUtil.createWorkDayDurationString(remaining));
            } else {
                // No estimate, so we can't calculate remaining
                timeRemainingField.clear();
            }
        } catch (IllegalArgumentException ex) {
            // Invalid format - don't update time remaining
            // User is still typing, so we don't want to clear the field or show errors
            // The validation will show errors in the time spent field when they're done typing
        }
    }
}
