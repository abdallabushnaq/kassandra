/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskMode;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.rest.api.TaskApi;
import de.bushnaq.abdalla.kassandra.rest.api.WorklogApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.Map;

/**
 * A dialog that displays full task details, allows editing of task fields,
 * and provides inline management (view, edit, delete) of the task's work logs.
 * <p>
 * Read-only fields show calculated values (duration, finish, progress, etc.).
 * Editable fields cover name, notes, minEstimate, and maxEstimate.
 * The worklog section lists all logged work entries with Edit and Delete actions.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Slf4j
public class TaskDialog extends Dialog {

    public static final  String            CANCEL_BUTTON      = "cancel-task-dialog-button";
    public static final  String            SAVE_TASK_BUTTON   = "save-task-button";
    public static final  String            TASK_DIALOG        = "task-dialog";
    private static final DateTimeFormatter DATE_FORMATTER     = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FMT      = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final        Binder<Task>      binder             = new Binder<>(Task.class);
    private final UUID currentUserId;
    private final        Runnable          onRefresh;
    private final        Task              task;
    private final        TaskApi           taskApi;
    private final        Map<UUID, User>   userMap;
    private final        WorklogApi        worklogApi;
    /** Container rebuilt whenever the worklog list changes. */
    private              VerticalLayout    worklogContainer;
    /** Read-only field references so they can be refreshed after worklog changes. */
    private              TextField         roTimeSpent;
    private              TextField         roRemainingEstimate;
    private              TextField         roProgress;
    /** Read-only start field shown in auto-scheduled mode. */
    private              TextField         roStart;
    /** Editable start DateTimePicker — visible only in manual-scheduling mode. */
    private              DateTimePicker    startField;

    /**
     * Creates a TaskDialog for the given task.
     *
     * @param task          the task to display and edit
     * @param taskApi       REST API for task persistence
     * @param worklogApi    REST API for worklog persistence
     * @param userMap       map of user IDs to {@link User} objects (for displaying author names)
     * @param currentUserId ID of the currently authenticated user; may be {@code null}
     * @param onRefresh     callback invoked after any change that requires a board refresh
     */
    public TaskDialog(Task task, TaskApi taskApi, WorklogApi worklogApi,
                      Map<UUID, User> userMap, UUID currentUserId, Runnable onRefresh) {
        this.task          = task;
        this.taskApi       = taskApi;
        this.worklogApi    = worklogApi;
        this.userMap       = userMap;
        this.currentUserId = currentUserId;
        this.onRefresh     = onRefresh;

        setId(TASK_DIALOG);
        setWidth("620px");
        setMaxHeight("90vh");
        setResizable(true);

        getHeader().add(VaadinUtil.createDialogHeader(task.getKey() + " — " + task.getName(), VaadinIcon.CLIPBOARD_TEXT));

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        content.add(buildReadOnlySection());
        content.add(new Hr());
        content.add(buildEditSection());
        content.add(new Hr());
        content.add(buildWorklogSection());

        // Close button at the bottom
        Button closeButton = new Button("Close", new Icon(VaadinIcon.CLOSE));
        closeButton.setId(CANCEL_BUTTON);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(e -> close());
        HorizontalLayout footer = new HorizontalLayout(closeButton);
        footer.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        footer.setWidthFull();
        content.add(footer);

        add(content);

        binder.readBean(task);
    }

    // -----------------------------------------------------------------------
    // Read-only section
    // -----------------------------------------------------------------------

    /**
     * Builds the read-only section showing calculated task values, plus the
     * "Manual scheduling" checkbox and (when active) an editable start
     * {@link DateTimePicker}.
     * <p>
     * Start and Finish always occupy the same two-column row. Duration, when
     * present, spans both columns so that the Start/Finish pairing is never
     * displaced. When manual scheduling is enabled the Start read-only field is
     * hidden with CSS {@code visibility:hidden} so its space is reserved and
     * no other field shifts position. The editable {@link DateTimePicker} for
     * start appears to the right of the checkbox in a separate row below.
     * </p>
     *
     * @return a {@link VerticalLayout} wrapping the read-only form and the
     *         scheduling controls row
     */
    private VerticalLayout buildReadOnlySection() {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("480px", 2)
        );

        roTimeSpent        = roField("Time Spent", formatDuration(task.getTimeSpent()));
        roRemainingEstimate = roField("Remaining Estimate", formatDuration(task.getRemainingEstimate()));
        roProgress         = roField("Progress", formatProgress(task.getProgress()));

        form.add(roTimeSpent, roRemainingEstimate, roProgress);
        form.add(roField("Status", task.getEffectiveStatus().name()));

        // Duration spans both columns so Start/Finish always land on their own row together
        if (task.getDuration() != null) {
            TextField durationField = roField("Duration", formatDuration(task.getDuration()));
            form.add(durationField);
            form.setColspan(durationField, 2);
        }

        // Start (col-1) and Finish (col-2) — always added so they are permanently paired
        roStart = roField("Start", task.getStart() != null ? DATE_FORMATTER.format(task.getStart()) : "");
        TextField roFinish = roField("Finish", task.getFinish() != null ? DATE_FORMATTER.format(task.getFinish()) : "");
        form.add(roStart, roFinish);

        wrapper.add(form);

        // Scheduling controls row.
        // Both the checkbox wrapper and the DateTimePicker have a label above them so
        // they share the same label+control structure and never shift each other.
        // The DateTimePicker is hidden via CSS visibility (not setVisible) so it always
        // reserves its space — the row height stays constant regardless of mode.
        boolean isManual = task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED;

        Checkbox manualCheckbox = new Checkbox(); // label is the Span above, not inline
        binder.forField(manualCheckbox)
                .withConverter(
                        b -> b ? TaskMode.MANUALLY_SCHEDULED : TaskMode.AUTO_SCHEDULED,
                        mode -> mode == TaskMode.MANUALLY_SCHEDULED,
                        "Invalid scheduling mode"
                )
                .bind(Task::getTaskMode, Task::setTaskMode);

        // Label above the checkbox matches the DateTimePicker label style
        Span checkboxLabel = new Span("Manual scheduling");
        checkboxLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "500");

        VerticalLayout checkboxWrapper = new VerticalLayout(checkboxLabel, manualCheckbox);
        checkboxWrapper.setPadding(false);
        checkboxWrapper.setSpacing(false);
        checkboxWrapper.getStyle().set("gap", "var(--lumo-space-xs)");

        startField = new DateTimePicker("Start");
        binder.forField(startField)
                .bind(Task::getStart, Task::setStart);

        // Keep the Start slot in the FormLayout reserved via CSS visibility
        if (isManual) {
            roStart.getStyle().set("visibility", "hidden");
        }
        // DateTimePicker always occupies its space — toggle visibility only, never display/size
        startField.getStyle().set("visibility", isManual ? "visible" : "hidden");

        manualCheckbox.addValueChangeListener(e -> {
            boolean manual = e.getValue();
            roStart.getStyle().set("visibility", manual ? "hidden" : "visible");
            startField.getStyle().set("visibility", manual ? "visible" : "hidden");
        });

        HorizontalLayout schedulingRow = new HorizontalLayout(checkboxWrapper, startField);
        schedulingRow.setWidthFull();
        schedulingRow.setAlignItems(HorizontalLayout.Alignment.START);
        schedulingRow.setPadding(false);
        schedulingRow.setFlexGrow(1, startField);
        wrapper.add(schedulingRow);

        return wrapper;
    }

    /**
     * Creates a labelled read-only {@link TextField}.
     *
     * @param label the field label
     * @param value the initial value to display
     * @return configured read-only TextField
     */
    private TextField roField(String label, String value) {
        TextField field = new TextField(label);
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        field.setReadOnly(true);
        field.setValue(value != null ? value : "");
        return field;
    }

    // -----------------------------------------------------------------------
    // Editable section
    // -----------------------------------------------------------------------

    /**
     * Builds the editable section with a Binder-backed form and a Save Task button.
     *
     * @return a {@link VerticalLayout} containing the editable form and its save button
     */
    private VerticalLayout buildEditSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        Span heading = new Span("Edit Task");
        heading.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-m)");
        section.add(heading);

        // Name
        TextField nameField = new TextField("Name");
        nameField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        nameField.setWidthFull();
        nameField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        binder.forField(nameField)
                .asRequired("Name is required")
                .withValidationStatusHandler(s -> {
                    nameField.setInvalid(s.isError());
                    s.getMessage().ifPresent(nameField::setErrorMessage);
                })
                .bind(Task::getName, Task::setName);
        section.add(nameField);

        // Notes
        TextArea notesField = new TextArea("Notes");
        notesField.setWidthFull();
        notesField.setHeight("80px");
        binder.forField(notesField)
                .bind(Task::getNotes, Task::setNotes);
        section.add(notesField);

        // Min / Max estimate side by side
        FormLayout estimateForm = new FormLayout();
        estimateForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        TextField minEstField = new TextField("Min Estimate");
        minEstField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        minEstField.setHelperText("e.g. 1d 2h");
        minEstField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        binder.forField(minEstField)
                .withConverter(
                        s -> {
                            if (s == null || s.isBlank()) return Duration.ZERO;
                            try {
                                return DateUtil.parseWorkDayDurationString(s.strip());
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        },
                        d -> d != null ? DateUtil.createWorkDayDurationString(d) : "",
                        "Invalid duration"
                )
                .withValidator((d, ctx) -> d == null
                        ? ValidationResult.error("Invalid duration format (e.g. 1d 2h)")
                        : ValidationResult.ok())
                .withValidationStatusHandler(s -> {
                    minEstField.setInvalid(s.isError());
                    s.getMessage().ifPresent(minEstField::setErrorMessage);
                })
                .bind(Task::getMinEstimate, Task::setMinEstimate);

        TextField maxEstField = new TextField("Max Estimate");
        maxEstField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        maxEstField.setHelperText("e.g. 2d 4h");
        maxEstField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        binder.forField(maxEstField)
                .withConverter(
                        s -> {
                            if (s == null || s.isBlank()) return Duration.ZERO;
                            try {
                                return DateUtil.parseWorkDayDurationString(s.strip());
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        },
                        d -> d != null ? DateUtil.createWorkDayDurationString(d) : "",
                        "Invalid duration"
                )
                .withValidator((d, ctx) -> d == null
                        ? ValidationResult.error("Invalid duration format (e.g. 2d 4h)")
                        : ValidationResult.ok())
                .withValidationStatusHandler(s -> {
                    maxEstField.setInvalid(s.isError());
                    s.getMessage().ifPresent(maxEstField::setErrorMessage);
                })
                .bind(Task::getMaxEstimate, Task::setMaxEstimate);

        estimateForm.add(minEstField, maxEstField);
        section.add(estimateForm);


        // Save Task button
        Button saveButton = new Button("Save Task", new Icon(VaadinIcon.CHECK));
        saveButton.setId(SAVE_TASK_BUTTON);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        saveButton.setEnabled(binder.isValid());
        binder.addStatusChangeListener(e -> saveButton.setEnabled(binder.isValid()));
        saveButton.addClickListener(e -> saveTask());

        HorizontalLayout saveRow = new HorizontalLayout(saveButton);
        saveRow.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        saveRow.setWidthFull();
        section.add(saveRow);

        return section;
    }

    /**
     * Persists the edited task fields via the task API.
     */
    private void saveTask() {
        if (!binder.writeBeanIfValid(task)) {
            showError("Please fix validation errors before saving.");
            return;
        }
        try {
            taskApi.update(task);
            showSuccess("Task saved.");
            if (onRefresh != null) {
                onRefresh.run();
            }
        } catch (Exception e) {
            log.error("Error saving task {}", task.getId(), e);
            showError("Failed to save task: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Worklog section
    // -----------------------------------------------------------------------

    /**
     * Builds the worklog section header (label + Log Work button) and the
     * scrollable list of existing work log entries.
     *
     * @return a {@link VerticalLayout} containing the worklog section
     */
    private VerticalLayout buildWorklogSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        // Section header row
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setAlignItems(HorizontalLayout.Alignment.CENTER);
        headerRow.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);

        Span heading = new Span("Work Logs");
        heading.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-m)");

        Button logWorkButton = new Button("Log Work", new Icon(VaadinIcon.CLOCK));
        logWorkButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        // Only the assigned user may log work
        boolean canLogWork = currentUserId != null && currentUserId.equals(task.getResourceId());
        logWorkButton.setEnabled(canLogWork);
        if (!canLogWork) {
            logWorkButton.setTooltipText("Only the assigned user can log work on this task.");
        }
        logWorkButton.addClickListener(e -> openCreateWorklogDialog());

        headerRow.add(heading, logWorkButton);
        section.add(headerRow);

        // Scrollable worklog list
        worklogContainer = new VerticalLayout();
        worklogContainer.setPadding(false);
        worklogContainer.setSpacing(false);
        worklogContainer.getStyle()
                .set("max-height", "280px")
                .set("overflow-y", "auto")
                .set("gap", "4px");
        section.add(worklogContainer);

        rebuildWorklogRows();
        return section;
    }

    /**
     * Clears and repopulates the worklog rows from {@code task.getWorklogs()}.
     */
    private void rebuildWorklogRows() {
        worklogContainer.removeAll();

        List<Worklog> worklogs = task.getWorklogs();
        if (worklogs == null || worklogs.isEmpty()) {
            Span empty = new Span("No work logged yet.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            worklogContainer.add(empty);
            return;
        }

        for (Worklog wl : List.copyOf(worklogs)) {
            worklogContainer.add(buildWorklogRow(wl));
        }
    }

    /**
     * Builds a single worklog row displaying date, time spent, comment, author name,
     * and Edit / Delete action buttons.
     *
     * @param worklog the worklog entry to display
     * @return a styled {@link HorizontalLayout} row
     */
    private HorizontalLayout buildWorklogRow(Worklog worklog) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(HorizontalLayout.Alignment.CENTER);
        row.setSpacing(true);
        row.setPadding(false);
        row.getStyle()
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "4px 0");

        // Date
        String dateText = worklog.getStart() != null
                ? DATE_ONLY_FMT.format(worklog.getStart().toLocalDate()) : "—";
        Span dateSpan = new Span(dateText);
        dateSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("min-width", "80px");

        // Time spent
        Span timeSpan = new Span(formatDuration(worklog.getTimeSpent()));
        timeSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "bold")
                .set("min-width", "50px");

        // Comment
        String commentText = worklog.getComment() != null ? worklog.getComment() : "";
        Span   commentSpan = new Span(commentText);
        commentSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("flex", "1")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        // Author name
        User   author     = worklog.getAuthorId() != null ? userMap.get(worklog.getAuthorId()) : null;
        String authorName = author != null ? author.getName() : "Unknown";
        Span   authorSpan = new Span(authorName);
        authorSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("min-width", "80px");

        // Edit and Delete buttons — only for the worklog's author or an admin
        boolean canEdit = currentUserId != null
                && (currentUserId.equals(worklog.getAuthorId()) || isAdmin());

        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        editButton.setEnabled(canEdit);
        editButton.getElement().setAttribute("title", canEdit ? "Edit work log" : "Only the author can edit this log");
        editButton.addClickListener(e -> openEditWorklogDialog(worklog));

        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_ERROR);
        deleteButton.setEnabled(canEdit);
        deleteButton.getElement().setAttribute("title", canEdit ? "Delete work log" : "Only the author can delete this log");
        deleteButton.addClickListener(e -> confirmDeleteWorklog(worklog));

        row.add(dateSpan, timeSpan, commentSpan, authorSpan, editButton, deleteButton);
        return row;
    }

    // -----------------------------------------------------------------------
    // Worklog CRUD operations
    // -----------------------------------------------------------------------

    /**
     * Opens the {@link WorklogDialog} in create mode to add a new work log to this task.
     */
    private void openCreateWorklogDialog() {
        WorklogDialog dlg = new WorklogDialog(task, taskApi, worklogApi, savedWorklog -> {
            // WorklogDialog already updated task.timeSpent; we just need to add the
            // worklog to the in-memory list and refresh the UI.
            task.getWorklogs().add(savedWorklog);
            refreshWorklogUi();
        }, currentUserId);
        dlg.open();
    }

    /**
     * Opens the {@link WorklogDialog} in edit mode for the given worklog.
     *
     * @param worklog the worklog entry to edit
     */
    private void openEditWorklogDialog(Worklog worklog) {
        WorklogDialog dlg = new WorklogDialog(worklog, task, taskApi, worklogApi, updatedWorklog -> {
            // WorklogDialog mutated the worklog in place and updated task.timeSpent
            refreshWorklogUi();
        }, currentUserId);
        dlg.open();
    }

    /**
     * Shows a confirmation dialog before deleting a worklog entry.
     *
     * @param worklog the worklog entry to delete
     */
    private void confirmDeleteWorklog(Worklog worklog) {
        String dateText    = worklog.getStart() != null ? DATE_ONLY_FMT.format(worklog.getStart().toLocalDate()) : "?";
        String description = DateUtil.createWorkDayDurationString(worklog.getTimeSpent()) + " on " + dateText;

        ConfirmDialog confirm = new ConfirmDialog(
                "Delete Work Log",
                "Delete work log: " + description + "?",
                "Delete",
                () -> deleteWorklog(worklog)
        );
        confirm.open();
    }

    /**
     * Deletes a worklog entry, adjusts the task's time spent, and refreshes the UI.
     *
     * @param worklog the worklog entry to delete
     */
    private void deleteWorklog(Worklog worklog) {
        try {
            worklogApi.deleteById(worklog.getId());

            // Adjust task.timeSpent
            Duration reduced = task.getTimeSpent().minus(worklog.getTimeSpent());
            task.setTimeSpent(reduced.isNegative() ? Duration.ZERO : reduced);
            task.recalculate();
            taskApi.update(task);

            task.getWorklogs().remove(worklog);
            refreshWorklogUi();
            showSuccess("Work log deleted.");
        } catch (Exception e) {
            log.error("Error deleting worklog {}", worklog.getId(), e);
            showError("Failed to delete work log: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // UI refresh helpers
    // -----------------------------------------------------------------------

    /**
     * Refreshes the worklog list rows and the read-only aggregate fields
     * (time spent, remaining estimate, progress) after any worklog change.
     */
    private void refreshWorklogUi() {
        rebuildWorklogRows();
        roTimeSpent.setValue(formatDuration(task.getTimeSpent()));
        roRemainingEstimate.setValue(formatDuration(task.getRemainingEstimate()));
        roProgress.setValue(formatProgress(task.getProgress()));
        if (onRefresh != null) {
            onRefresh.run();
        }
    }

    // -----------------------------------------------------------------------
    // Formatting helpers
    // -----------------------------------------------------------------------

    /**
     * Formats a {@link Duration} as a human-readable work-day string.
     *
     * @param d the duration to format; may be {@code null}
     * @return formatted string such as {@code "1d 2h"}, or {@code "0m"} for zero/null
     */
    private String formatDuration(Duration d) {
        if (d == null) return "0m";
        return DateUtil.createWorkDayDurationString(d);
    }

    /**
     * Formats a progress value (0–1) as a percentage string.
     *
     * @param progress progress number; may be {@code null}
     * @return formatted string such as {@code "42%"}
     */
    private String formatProgress(Number progress) {
        if (progress == null) return "0%";
        return String.format("%.0f%%", progress.doubleValue() * 100);
    }

    /**
     * Returns {@code true} if the current authenticated user holds the ADMIN role.
     *
     * @return {@code true} for admin users
     */
    private boolean isAdmin() {
        return de.bushnaq.abdalla.kassandra.security.SecurityUtils.isAdmin();
    }

    private void showError(String message) {
        Notification n = Notification.show(message, 4000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}


