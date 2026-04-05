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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import de.bushnaq.abdalla.kassandra.dto.WorkDaySchedule;
import de.bushnaq.abdalla.kassandra.dto.WorkWeek;
import de.bushnaq.abdalla.kassandra.rest.api.WorkWeekApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialog for creating and editing global {@link WorkWeek} definitions.
 * Displays a 7-row schedule table (one row per day of the week) where each row has
 * a working-day toggle and four time pickers (work start/end, lunch start/end).
 */
public class WorkWeekDialog extends Dialog {

    public static final String CANCEL_BUTTON              = "work-week-dialog-cancel";
    public static final String CONFIRM_BUTTON             = "work-week-dialog-confirm";
    public static final String DESCRIPTION_FIELD          = "work-week-description-field";
    public static final String NAME_FIELD                 = "work-week-name-field";
    /**
     * ID prefix for per-day working-day checkboxes.
     * Full ID is {@code WORKING_DAY_CHECK_ID_PREFIX + dayName.toLowerCase()},
     * e.g. {@code "work-week-dialog-working-monday"}.
     */
    public static final String WORKING_DAY_CHECK_ID_PREFIX = "work-week-dialog-working-";
    public static final String WORK_WEEK_DIALOG           = "work-week-dialog";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final TextField   nameField        = new TextField("Name");
    private final TextField   descriptionField = new TextField("Description");
    private final boolean     isEditMode;
    private final WorkWeek    workWeek;
    private final WorkWeekApi workWeekApi;
    private final Runnable    onSaveCallback;

    // Per-day UI controls
    private final Checkbox[]   workingDayChecks = new Checkbox[7];
    private final TimePicker[] workStartPickers = new TimePicker[7];
    private final TimePicker[] workEndPickers   = new TimePicker[7];
    private final TimePicker[] lunchStartPickers= new TimePicker[7];
    private final TimePicker[] lunchEndPickers  = new TimePicker[7];

    private static final String[] DAY_LABELS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    /**
     * Opens the dialog.
     *
     * @param workWeek       existing work week to edit, or {@code null} to create
     * @param workWeekApi    REST API stub
     * @param onSaveCallback called after a successful save
     */
    public WorkWeekDialog(WorkWeek workWeek, WorkWeekApi workWeekApi, Runnable onSaveCallback) {
        this.workWeek       = workWeek != null ? workWeek : new WorkWeek();
        this.workWeekApi    = workWeekApi;
        this.onSaveCallback = onSaveCallback;
        this.isEditMode     = workWeek != null;

        String title = isEditMode ? "Edit Work Week" : "Create Work Week";
        getHeader().add(VaadinUtil.createDialogHeader(title, VaadinIcon.CALENDAR));

        setId(WORK_WEEK_DIALOG);
        setWidth("860px");

        add(createContent());
        loadValues();

        Shortcuts.addShortcutListener(this, e -> save(), Key.ENTER);
    }

    // -------------------------------------------------------------------------

    private VerticalLayout createContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        // Name & description
        nameField.setId(NAME_FIELD);
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setPrefixComponent(VaadinIcon.TAG.create());

        descriptionField.setId(DESCRIPTION_FIELD);
        descriptionField.setWidthFull();
        descriptionField.setPrefixComponent(VaadinIcon.INFO_CIRCLE.create());

        FormLayout meta = new FormLayout(nameField, descriptionField);
        meta.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        layout.add(meta);

        // Header row for the schedule table
        layout.add(buildScheduleHeader());

        // One row per day
        WorkDaySchedule[] schedules = getSchedules();
        for (int i = 0; i < 7; i++) {
            layout.add(buildDayRow(i, schedules[i]));
        }

        // Buttons
        layout.add(VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this, null));
        return layout;
    }

    private HorizontalLayout buildScheduleHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.add(styledLabel("Day", "120px"));
        header.add(styledLabel("Working", "80px"));
        header.add(styledLabel("Work Start", "120px"));
        header.add(styledLabel("Lunch Start", "120px"));
        header.add(styledLabel("Lunch End", "120px"));
        header.add(styledLabel("Work End", "120px"));
        return header;
    }

    private HorizontalLayout buildDayRow(int index, @SuppressWarnings("unused") WorkDaySchedule schedule) {
        Checkbox   check      = new Checkbox();
        check.setId(WORKING_DAY_CHECK_ID_PREFIX + DAY_LABELS[index].toLowerCase());
        TimePicker wStart     = timePicker("08:00");
        TimePicker wEnd       = timePicker("17:00");
        TimePicker lStart     = timePicker("12:00");
        TimePicker lEnd       = timePicker("13:00");

        workingDayChecks[index]  = check;
        workStartPickers[index]  = wStart;
        workEndPickers[index]    = wEnd;
        lunchStartPickers[index] = lStart;
        lunchEndPickers[index]   = lEnd;

        // Enable/disable time pickers based on working-day toggle
        check.addValueChangeListener(e -> setTimePickersEnabled(index, e.getValue()));

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.BASELINE);
        row.add(styledLabel(DAY_LABELS[index], "120px"));
        row.add(withWidth(check, "80px"));
        row.add(withWidth(wStart, "120px"));
        row.add(withWidth(lStart, "120px"));
        row.add(withWidth(lEnd, "120px"));
        row.add(withWidth(wEnd, "120px"));
        return row;
    }

    private void setTimePickersEnabled(int index, boolean enabled) {
        workStartPickers[index].setEnabled(enabled);
        workEndPickers[index].setEnabled(enabled);
        lunchStartPickers[index].setEnabled(enabled);
        lunchEndPickers[index].setEnabled(enabled);
    }

    private TimePicker timePicker(String defaultValue) {
        TimePicker tp = new TimePicker();
        tp.setValue(LocalTime.parse(defaultValue, TIME_FMT));
        return tp;
    }

    private Span styledLabel(String text, String width) {
        Span s = new Span(text);
        s.getStyle().set("font-weight", "bold").set("min-width", width).set("width", width);
        return s;
    }

    private <C extends com.vaadin.flow.component.Component> C withWidth(C component, String width) {
        component.getElement().getStyle().set("min-width", width).set("width", width);
        return component;
    }

    private WorkDaySchedule[] getSchedules() {
        return new WorkDaySchedule[]{
            workWeek.getMonday(),
            workWeek.getTuesday(),
            workWeek.getWednesday(),
            workWeek.getThursday(),
            workWeek.getFriday(),
            workWeek.getSaturday(),
            workWeek.getSunday()
        };
    }

    private void loadValues() {
        nameField.setValue(workWeek.getName() != null ? workWeek.getName() : "");
        descriptionField.setValue(workWeek.getDescription() != null ? workWeek.getDescription() : "");

        WorkDaySchedule[] schedules = getSchedules();
        for (int i = 0; i < 7; i++) {
            WorkDaySchedule s = schedules[i];
            if (s == null) s = new WorkDaySchedule();
            workingDayChecks[i].setValue(s.isWorkingDay());
            if (s.getWorkStart()  != null) workStartPickers[i].setValue(s.getWorkStart());
            if (s.getWorkEnd()    != null) workEndPickers[i].setValue(s.getWorkEnd());
            if (s.getLunchStart() != null) lunchStartPickers[i].setValue(s.getLunchStart());
            if (s.getLunchEnd()   != null) lunchEndPickers[i].setValue(s.getLunchEnd());
            setTimePickersEnabled(i, s.isWorkingDay());
        }
    }

    private void save() {
        String name = nameField.getValue();
        if (name == null || name.isBlank()) {
            nameField.setInvalid(true);
            nameField.setErrorMessage("Name is required");
            return;
        }

        workWeek.setName(name.strip());
        workWeek.setDescription(descriptionField.getValue());

        List<WorkDaySchedule> scheduleList = List.of(
            workWeek.getMonday(),    workWeek.getTuesday(), workWeek.getWednesday(),
            workWeek.getThursday(),  workWeek.getFriday(),  workWeek.getSaturday(),
            workWeek.getSunday()
        );
        for (int i = 0; i < 7; i++) {
            WorkDaySchedule s = scheduleList.get(i);
            boolean working = workingDayChecks[i].getValue();
            s.setWorkingDay(working);
            s.setWorkStart (working ? workStartPickers[i].getValue()  : null);
            s.setWorkEnd   (working ? workEndPickers[i].getValue()    : null);
            s.setLunchStart(working ? lunchStartPickers[i].getValue() : null);
            s.setLunchEnd  (working ? lunchEndPickers[i].getValue()   : null);
        }

        try {
            if (isEditMode) {
                workWeekApi.update(workWeek);
            } else {
                workWeekApi.persist(workWeek);
            }
            onSaveCallback.run();
            close();
        } catch (Exception ex) {
            Notification notification = Notification.show(
                    "Failed to save: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}





