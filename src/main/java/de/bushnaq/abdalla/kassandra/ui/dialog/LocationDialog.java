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
import de.bushnaq.abdalla.kassandra.dto.Location;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.LocationApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

public class LocationDialog extends Dialog {
    public static final String           CANCEL_BUTTON             = "location-dialog-cancel";
    public static final String           CONFIRM_BUTTON            = "location-dialog-confirm";
    public static final String           LOCATION_COUNTRY_FIELD    = "location-country-field";
    public static final String           LOCATION_DIALOG           = "location-dialog";
    public static final String           LOCATION_START_DATE_FIELD = "location-start-date-field";
    public static final String           LOCATION_STATE_FIELD      = "location-state-field";
    private final       Binder<Location> binder                    = new Binder<>(Location.class);
    private final       ComboBox<String> countryComboBox           = new ComboBox<>("Country");
    private final       boolean          isEditMode;
    private final       Location         location;
    private final       LocationApi      locationApi;
    private final       Runnable         onSaveCallback;
    private final       DatePicker       startDatePicker           = new DatePicker("Start Date");
    private final       ComboBox<String> stateComboBox             = new ComboBox<>("State/Region");
    private final       User             user;

    public LocationDialog(Location location, User user, LocationApi locationApi, Runnable onSaveCallback) {
        this.location       = location != null ? location : new Location();
        this.user           = user;
        this.locationApi    = locationApi;
        this.onSaveCallback = onSaveCallback;
        this.isEditMode     = location != null;

        // Set the dialog title with an icon
        String title = isEditMode ? "Edit Location" : "Create Location";
        getHeader().add(VaadinUtil.createDialogHeader(title, VaadinIcon.MAP_MARKER));

        setId(LOCATION_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);

        VerticalLayout content = createDialogContent();
        add(content);

        configureFormBinder();
        initFormValues();
    }

    private void configureFormBinder() {
        // Country binding
        binder.forField(countryComboBox)
                .asRequired("Country is required")
                .withValidationStatusHandler(status -> {
                    countryComboBox.setInvalid(status.isError());
                    status.getMessage().ifPresent(countryComboBox::setErrorMessage);
                })
                .bind(Location::getCountry, Location::setCountry);

        // State binding
        binder.forField(stateComboBox)
                .asRequired("State/Region is required")
                .withValidationStatusHandler(status -> {
                    stateComboBox.setInvalid(status.isError());
                    status.getMessage().ifPresent(stateComboBox::setErrorMessage);
                })
                .bind(Location::getState, Location::setState);

        // Start date binding with custom validator to ensure uniqueness
        binder.forField(startDatePicker)
                .asRequired("Start date is required")
                .withValidator(this::validateStartDateUniqueness, "A location with this start date already exists")
                .withValidationStatusHandler(status -> {
                    startDatePicker.setInvalid(status.isError());
                    status.getMessage().ifPresent(startDatePicker::setErrorMessage);
                })
                .bind(Location::getStart, Location::setStart);

        // Load data into the form
        if (isEditMode) {
            // Important: Populate states for the selected country BEFORE reading the bean
            // so that the state value can be properly set
            if (location.getCountry() != null) {
                populateStates(location.getCountry());
            }
            binder.readBean(location);
        } else {
            binder.readBean(new Location());
        }

        // Trigger validation to show errors for initially empty fields in create mode
        if (!isEditMode) {
            binder.validate();
        }
    }

    private VerticalLayout createDialogContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setAlignItems(FlexComponent.Alignment.STRETCH);

        // Configure country combobox with ISO 3166-1 alpha-2 values
        countryComboBox.setId(LOCATION_COUNTRY_FIELD);
        countryComboBox.setRequired(true);
        countryComboBox.setAllowCustomValue(false);
        countryComboBox.setPrefixComponent(new Icon(VaadinIcon.GLOBE));
        populateCountries();

        // Country change listener to update states
        countryComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                populateStates(event.getValue());
            } else {
                stateComboBox.setItems(new HashSet<>());
                stateComboBox.setEnabled(false);
            }
        });

        // Configure state/region combobox
        stateComboBox.setId(LOCATION_STATE_FIELD);
        stateComboBox.setRequired(true);
        stateComboBox.setAllowCustomValue(false);
        stateComboBox.setPrefixComponent(new Icon(VaadinIcon.MAP_MARKER));
        stateComboBox.setEnabled(false); // Initially disabled until country is selected

        // Configure start date picker
        startDatePicker.setI18n(new DatePicker.DatePickerI18n().setDateFormat("yyyy-MM-dd"));
        startDatePicker.setId(LOCATION_START_DATE_FIELD);
        startDatePicker.setRequired(true);
        startDatePicker.setMax(LocalDate.now().plusYears(10));
        startDatePicker.setPrefixComponent(new Icon(VaadinIcon.CALENDAR));

        content.add(countryComboBox, stateComboBox, startDatePicker, VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this, binder));
        return content;
    }

    private void initFormValues() {
        if (!isEditMode) {
            // Set default values for new location
            startDatePicker.setValue(LocalDate.now());
        }
        // Note: For edit mode, state population is handled in configureFormBinder()
        // before binder.readBean() to ensure the state can be properly set
    }

    private void populateCountries() {
        // Get list of supported countries from jollyday
        Set<String> countryCodeSet = new HashSet<>();
        for (String countryCode : HolidayManager.getSupportedCalendarCodes()) {
            countryCodeSet.add(countryCode);
        }

        // Convert to list and sort by display country name
        List<String> sortedCountryCodes = countryCodeSet.stream()
                .sorted((code1, code2) -> {
                    Locale locale1 = new Locale("", code1);
                    Locale locale2 = new Locale("", code2);
                    return locale1.getDisplayCountry().compareTo(locale2.getDisplayCountry());
                })
                .collect(Collectors.toList());

        countryComboBox.setItems(sortedCountryCodes);

        // Set custom label generator to display country name
        countryComboBox.setItemLabelGenerator(countryCode -> {
            Locale locale = new Locale("", countryCode);
            return locale.getDisplayCountry() + " (" + countryCode + ")";
        });
    }

    private void populateStates(String countryCode) {
        // Get list of supported states for the selected country from jollyday
        Set<String> stateCodeSet = new HashSet<>();
        try {
            HolidayManager manager = HolidayManager.getInstance(ManagerParameters.create(countryCode));
            stateCodeSet.addAll(manager.getCalendarHierarchy().getChildren().keySet());

            // If there are no states, add an empty one to represent the whole country
            if (stateCodeSet.isEmpty()) {
                stateCodeSet.add(countryCode);
                stateComboBox.setItems(stateCodeSet);
            } else {
                // Create a map of descriptions to state codes
                Map<String, String> stateDescriptionMap = null;

                // Function to get the best description for a state
                Function<String, String> getStateDescription = stateCode -> {
                    if (stateCode.equals(countryCode)) {
                        return "All of " + new Locale("", countryCode).getDisplayCountry();
                    }

                    try {
                        String description = manager.getCalendarHierarchy().getChildren().get(stateCode).getDescription();
                        return description;
                    } catch (Exception e) {
                        // If we can't get the description, just use the code
                    }

                    return stateCode;
                };

                // Sort state codes by their descriptions
                List<String> sortedStateCodes = stateCodeSet.stream()
                        .sorted((code1, code2) -> {
                            String desc1 = getStateDescription.apply(code1);
                            String desc2 = getStateDescription.apply(code2);
                            return desc1.compareTo(desc2);
                        })
                        .collect(Collectors.toList());

                stateComboBox.setItems(sortedStateCodes);
            }

            stateComboBox.setEnabled(true);

            // Set custom label generator for state names
            stateComboBox.setItemLabelGenerator(stateCode -> {
                if (stateCode.equals(countryCode)) {
                    return "All of " + new Locale("", countryCode).getDisplayCountry();
                }

                // Try to get a more descriptive name from CalendarHierarchy
                try {
                    String description = manager.getCalendarHierarchy().getChildren().get(stateCode).getDescription();
                    if (description != null && !description.isEmpty()) {
                        return description + " (" + stateCode + ")";
                    }
                } catch (Exception e) {
                    // If we can't get the description, just use the code
                }

                return stateCode;
            });
        } catch (Exception e) {
            Notification notification = Notification.show("Error loading regions for " + countryCode + ": " + e.getMessage(), 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            stateComboBox.setEnabled(false);
        }
    }

    private void save() {
        try {
            // Validate all fields first
            if (!binder.validate().isOk()) {
                return; // Stop here if validation fails
            }

            // Write values to bean
            binder.writeBean(location);

            // Ensure user association is set
            location.setUser(user);

            if (isEditMode) {
                locationApi.update(location, user.getId());
                Notification.show("Location updated", 3000, Notification.Position.MIDDLE);
            } else {
                locationApi.persist(location, user.getId());
                Notification.show("Location added", 3000, Notification.Position.MIDDLE);
            }

            // Call the callback
            onSaveCallback.run();
            close();
        } catch (ValidationException e) {
            System.out.printf("Validation failed: %s%n", e.getMessage());
            // Validation errors will already be displayed next to the fields
        } catch (Exception e) {
            System.out.printf("Error saving location: %s%n", e.getMessage());
            Notification notification = Notification.show(
                    "Error saving location: " + e.getMessage(),
                    3000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean validateStartDateUniqueness(LocalDate date) {
        if (date == null) return true; // Let the required validator handle null values

        // If editing existing record and date hasn't changed, it's valid
        if (isEditMode && location.getStart() != null && location.getStart().equals(date)) {
            return true;
        }

        // Check if any other location record exists with the same date
        return user.getLocations().stream()
                .noneMatch(loc -> date.equals(loc.getStart()) && !loc.equals(location));
    }
}
