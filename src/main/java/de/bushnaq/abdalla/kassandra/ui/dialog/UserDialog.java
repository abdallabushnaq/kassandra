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

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import java.awt.*;
import java.util.function.Consumer;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for creating and editing users.
 */
public class UserDialog extends Dialog {

    public static final String         CANCEL_BUTTON                 = "cancel-user-button";
    public static final String         CONFIRM_BUTTON                = "save-user-button";
    public static final String         USER_DIALOG                   = "user-dialog";
    public static final String         USER_EMAIL_FIELD              = "user-email-field";
    public static final String         USER_FIRST_WORKING_DAY_PICKER = "user-first-working-day-picker";
    public static final String         USER_LAST_WORKING_DAY_PICKER  = "user-last-working-day-picker";
    public static final String         USER_NAME_FIELD               = "user-name-field";
    private final       EmailField     emailField;
    private final       DatePicker     firstWorkingDayPicker;
    private final       boolean        isEditMode;
    private final       DatePicker     lastWorkingDayPicker;
    private final       TextField      nameField;
    private final       Consumer<User> saveCallback;
    private final       User           user;

    /**
     * Creates a dialog for creating or editing a user.
     *
     * @param user         The user to edit, or null for creating a new user
     * @param saveCallback Callback that receives the user with updated values
     */
    public UserDialog(User user, Consumer<User> saveCallback) {
        this.user         = user;
        this.saveCallback = saveCallback;
        isEditMode        = user != null;

        // Set the dialog title with an icon
        String title = isEditMode ? "Edit User" : "Create User";
        getHeader().add(VaadinUtil.createDialogHeader(title, VaadinIcon.USER));

        setId(USER_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Name field
        nameField = new TextField("Name");
        nameField.setId(USER_NAME_FIELD);
        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setPrefixComponent(new Icon(VaadinIcon.USER));

        // Email field
        emailField = new EmailField("Email");
        emailField.setId(USER_EMAIL_FIELD);
        emailField.setWidthFull();
        emailField.setPrefixComponent(new Icon(VaadinIcon.ENVELOPE));

        // First working day picker
        firstWorkingDayPicker = new DatePicker("First Working Day");
        firstWorkingDayPicker.setId(USER_FIRST_WORKING_DAY_PICKER);
        firstWorkingDayPicker.setWidthFull();
        firstWorkingDayPicker.setPrefixComponent(new Icon(VaadinIcon.CALENDAR_USER));

        // Last working day picker
        lastWorkingDayPicker = new DatePicker("Last Working Day");
        lastWorkingDayPicker.setId(USER_LAST_WORKING_DAY_PICKER);
        lastWorkingDayPicker.setWidthFull();
        lastWorkingDayPicker.setPrefixComponent(new Icon(VaadinIcon.CALENDAR_USER));

        // Add validation to ensure last working day is after first working day
        firstWorkingDayPicker.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                lastWorkingDayPicker.setMin(event.getValue());
            }
        });

        lastWorkingDayPicker.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                firstWorkingDayPicker.setMax(event.getValue());
            }
        });

        if (isEditMode) {
            nameField.setValue(user.getName() != null ? user.getName() : "");
            emailField.setValue(user.getEmail() != null ? user.getEmail() : "");
            firstWorkingDayPicker.setValue(user.getFirstWorkingDay());
            lastWorkingDayPicker.setValue(user.getLastWorkingDay());
        }

        dialogLayout.add(
                nameField,
                emailField,
                firstWorkingDayPicker,
                lastWorkingDayPicker
        );

        dialogLayout.add(VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this));
        add(dialogLayout);
    }

    private void save() {
        if (nameField.getValue().trim().isEmpty()) {
            Notification.show("Please enter a user name", 3000, Notification.Position.MIDDLE);
            return;
        }

        User userToSave;
        if (isEditMode) {
            userToSave = user;
        } else {
            userToSave = new User();
        }

        userToSave.setName(nameField.getValue().trim());
        userToSave.setEmail(emailField.getValue().trim());

        // color is managed by the user profile now; ensure a default is present for new users
        if (!isEditMode && userToSave.getColor() == null) {
            userToSave.setColor(new Color(211, 211, 211)); // Light Gray default
        }

        userToSave.setFirstWorkingDay(firstWorkingDayPicker.getValue());
        userToSave.setLastWorkingDay(lastWorkingDayPicker.getValue());

        saveCallback.accept(userToSave);
        close();

    }
}
