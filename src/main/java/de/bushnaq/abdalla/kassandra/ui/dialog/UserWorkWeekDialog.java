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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.UserWorkWeek;
import de.bushnaq.abdalla.kassandra.dto.WorkWeek;
import de.bushnaq.abdalla.kassandra.rest.api.UserWorkWeekApi;
import de.bushnaq.abdalla.kassandra.rest.api.WorkWeekApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import java.time.LocalDate;
import java.util.List;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * Dialog for assigning a global {@link WorkWeek} to a {@link User} with an effective start date.
 * Mirrors the pattern of {@link LocationDialog}.
 */
public class UserWorkWeekDialog extends Dialog {

    public static final String CANCEL_BUTTON         = "user-work-week-dialog-cancel";
    public static final String CONFIRM_BUTTON        = "user-work-week-dialog-confirm";
    public static final String START_DATE_FIELD      = "user-work-week-start-date-field";
    public static final String USER_WORK_WEEK_DIALOG = "user-work-week-dialog";
    public static final String WORK_WEEK_COMBO_FIELD = "user-work-week-combo-field";

    private final Binder<UserWorkWeek> binder          = new Binder<>(UserWorkWeek.class);
    private final boolean              isEditMode;
    private final Runnable             onSaveCallback;
    private final DatePicker           startDatePicker = new DatePicker("Start Date");
    private final User                 user;
    private final UserWorkWeek         userWorkWeek;
    private final UserWorkWeekApi      userWorkWeekApi;
    private final WorkWeekApi          workWeekApi;
    private final ComboBox<WorkWeek>   workWeekCombo   = new ComboBox<>("Work Week");

    /**
     * Opens the dialog.
     *
     * @param userWorkWeek    existing assignment to edit, or {@code null} to create
     * @param user            the user to whom the work week is assigned
     * @param userWorkWeekApi REST API stub for assignments
     * @param workWeekApi     REST API stub for global work weeks
     * @param onSaveCallback  called after a successful save
     */
    public UserWorkWeekDialog(UserWorkWeek userWorkWeek, User user,
                              UserWorkWeekApi userWorkWeekApi, WorkWeekApi workWeekApi, Runnable onSaveCallback) {
        this.userWorkWeek    = userWorkWeek != null ? userWorkWeek : new UserWorkWeek();
        this.user            = user;
        this.userWorkWeekApi = userWorkWeekApi;
        this.workWeekApi     = workWeekApi;
        this.onSaveCallback  = onSaveCallback;
        this.isEditMode      = userWorkWeek != null;

        String title = isEditMode ? "Edit Work Week Assignment" : "Assign Work Week";
        getHeader().add(VaadinUtil.createDialogHeader(title, VaadinIcon.CALENDAR));

        setId(USER_WORK_WEEK_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);

        add(createContent());
        configureBinder();

        Shortcuts.addShortcutListener(this, e -> save(), Key.ENTER);
    }

    // -------------------------------------------------------------------------

    private void configureBinder() {
        binder.forField(workWeekCombo)
                .asRequired("Work week is required")
                .withValidationStatusHandler(status -> {
                    workWeekCombo.setInvalid(status.isError());
                    status.getMessage().ifPresent(workWeekCombo::setErrorMessage);
                })
                .bind(UserWorkWeek::getWorkWeek, UserWorkWeek::setWorkWeek);

        binder.forField(startDatePicker)
                .asRequired("Start date is required")
                .withValidator(this::validateStartDateUniqueness, "A work week with this start date already exists")
                .withValidationStatusHandler(status -> {
                    startDatePicker.setInvalid(status.isError());
                    status.getMessage().ifPresent(startDatePicker::setErrorMessage);
                })
                .bind(UserWorkWeek::getStart, UserWorkWeek::setStart);

        if (isEditMode) {
            binder.readBean(this.userWorkWeek);
        } else {
            startDatePicker.setValue(LocalDate.now());
            binder.readBean(new UserWorkWeek());
        }
    }

    private VerticalLayout createContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);

        // Populate work week combo box
        workWeekCombo.setId(WORK_WEEK_COMBO_FIELD);
        workWeekCombo.setRequired(true);
        workWeekCombo.setAllowCustomValue(false);
        workWeekCombo.setPrefixComponent(new Icon(VaadinIcon.CALENDAR));
        workWeekCombo.setItemLabelGenerator(WorkWeek::getName);
        List<WorkWeek> allWorkWeeks = workWeekApi.getAll();
        workWeekCombo.setItems(allWorkWeeks);

        // Start date picker
        startDatePicker.setId(START_DATE_FIELD);
        startDatePicker.setRequired(true);
        startDatePicker.setI18n(new DatePicker.DatePickerI18n().setDateFormat("yyyy-MM-dd"));
        startDatePicker.setPrefixComponent(new Icon(VaadinIcon.CALENDAR));

        layout.add(workWeekCombo, startDatePicker,
                VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this, binder));
        return layout;
    }

    private void save() {
        try {
            binder.writeBean(userWorkWeek);
        } catch (ValidationException ex) {
            return; // Validation errors are shown inline
        }
        try {
            if (isEditMode) {
                userWorkWeekApi.update(userWorkWeek, user.getId());
            } else {
                userWorkWeek.setUser(user);
                userWorkWeekApi.persist(userWorkWeek, user.getId());
            }
            onSaveCallback.run();
            close();
        } catch (Exception ex) {
            Notification notification = Notification.show(
                    "Failed to save: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean validateStartDateUniqueness(LocalDate date) {
        if (date == null || user == null) return true;
        return user.getUserWorkWeeks().stream()
                .filter(uww -> !isEditMode || !uww.getId().equals(userWorkWeek.getId()))
                .noneMatch(uww -> date.equals(uww.getStart()));
    }
}

