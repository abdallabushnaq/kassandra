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
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.RangeValidator;
import de.bushnaq.abdalla.kassandra.dto.Availability;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.AvailabilityApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import java.time.LocalDate;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

public class AvailabilityDialog extends Dialog {
    public static final String               AVAILABILITY_DIALOG           = "availability-dialog";
    public static final String               AVAILABILITY_PERCENTAGE_FIELD = "availability-percentage-field";
    public static final String               AVAILABILITY_START_DATE_FIELD = "availability-start-date-field";
    public static final String               CANCEL_BUTTON                 = "availability-dialog-cancel";
    public static final String               CONFIRM_BUTTON                = "availability-dialog-confirm";
    private final       Availability         availability;
    private final       AvailabilityApi      availabilityApi;
    private final       IntegerField         availabilityField             = new IntegerField("Availability (%)");
    private final       Binder<Availability> binder                        = new Binder<>(Availability.class);
    private final       boolean              isNewAvailability;
    private final       Runnable             onSaveCallback;
    private final       DatePicker           startDateField                = new DatePicker("Start Date");
    private final       User                 user;

    public AvailabilityDialog(Availability availability, User user, AvailabilityApi availabilityApi, Runnable onSaveCallback) {
        this.availabilityApi   = availabilityApi;
        this.user              = user;
        this.onSaveCallback    = onSaveCallback;
        this.isNewAvailability = (availability == null);
        this.availability      = isNewAvailability ? new Availability() : availability;

        // Set the dialog title with an icon
        String title = availability == null ? "Create Availability" : "Edit Availability";
        getHeader().add(VaadinUtil.createDialogHeader(title, VaadinIcon.CHART));

        setId(AVAILABILITY_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);

        // Setup form and actions
        add(createForm(), VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this));
        configureFormBinder();
    }


    private void configureFormBinder() {
        // Start date binding with custom validator to ensure uniqueness
        binder.forField(startDateField)
                .asRequired("Start date is required")
                .withValidator(this::validateStartDateUniqueness, "Another availability record already exists with this date")
                .withValidationStatusHandler(status -> {
                    startDateField.setInvalid(status.getMessage().isPresent());
                    startDateField.setErrorMessage(status.getMessage().orElse(""));
                })
                .bind(Availability::getStart, Availability::setStart);

        // Availability binding (as percentage, converting between 0-150% display and 0-1.5 stored value)
        binder.forField(availabilityField)
                .asRequired("Availability is required")
                .withConverter(
                        value -> value != null ? value.floatValue() / 100 : 0.0f,  // Double to float and convert from percentage
                        value -> value != null ? (int) (value * 100) : 0  // float to Double and convert to percentage
                )
                .withValidator(new RangeValidator<>("Availability must be between 0% and 150%", Float::compare, 0.0f, 1.5f))
                .withValidationStatusHandler(status -> {
                    availabilityField.setInvalid(status.getMessage().isPresent());
                    availabilityField.setErrorMessage(status.getMessage().orElse(""));
                })
                .bind(Availability::getAvailability, Availability::setAvailability);

        // Load data into the form
        if (!isNewAvailability) {
            binder.readBean(availability);
            // If editing, convert the stored value (0-1.5) to percentage display (0-150)
            availabilityField.setValue((int) (availability.getAvailability() * 100));
        }
    }

    private FormLayout createForm() {
        FormLayout formLayout = new FormLayout();

        // Configure fields
        startDateField.setI18n(new DatePicker.DatePickerI18n().setDateFormat("yyyy-MM-dd"));
        startDateField.setWidthFull();
        startDateField.setHelperText("The date when this availability level begins");
        startDateField.setId(AVAILABILITY_START_DATE_FIELD);
        startDateField.setPrefixComponent(new Icon(VaadinIcon.CALENDAR));

        availabilityField.setWidthFull();
        availabilityField.setHelperText("Value between 0-150% (e.g., 100 = full time)");
        availabilityField.setSuffixComponent(new Span("%"));
        availabilityField.setPrefixComponent(new Icon(VaadinIcon.CHART));
        availabilityField.setId(AVAILABILITY_PERCENTAGE_FIELD);

        formLayout.add(startDateField, availabilityField);
        return formLayout;
    }

    private void save() {
        try {
            // Validate all fields first - this will trigger validation and show error messages
            if (!binder.validate().isOk()) {
                return; // Stop here if validation fails
            }

            // Write values to bean
            binder.writeBean(availability);

            // Ensure user association is set
            availability.setUser(user);

            // Save to backend
            if (isNewAvailability) {
                availabilityApi.persist(availability, user.getId());
            } else {
                availabilityApi.update(availability, user.getId());
            }

            // Notify success
            String message = isNewAvailability ? "New availability record added" : "Availability record updated";
            Notification.show(message, 3000, Notification.Position.MIDDLE);

            // Trigger refresh callback and close dialog
            onSaveCallback.run();
            close();
        } catch (ValidationException e) {
            System.out.printf("Validation failed: %s%n", e.getMessage());
            // Validation errors will already be displayed next to the fields
            // No need for additional notification
        } catch (Exception e) {
            System.out.printf("Validation failed: %s%n", e.getMessage());
            // Only show notification for unexpected errors
            Notification notification = Notification.show("Error saving availability: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean validateStartDateUniqueness(LocalDate date) {
        if (date == null) return true; // Let the required validator handle null values

        // If editing existing record and date hasn't changed, it's valid
        if (!isNewAvailability && availability.getStart().equals(date)) {
            return true;
        }

        // Check if any other availability record exists with the same date
        return user.getAvailabilities().stream().noneMatch(a -> date.equals(a.getStart()) && !a.equals(availability));
    }

}
