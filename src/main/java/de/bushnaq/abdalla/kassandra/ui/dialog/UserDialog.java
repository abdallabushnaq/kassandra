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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.rest.api.LocationApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserWorkWeekApi;
import de.bushnaq.abdalla.kassandra.rest.api.WorkWeekApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for creating and editing users.
 */
@Slf4j
public class UserDialog extends Dialog {

    public static final String                 CANCEL_BUTTON                 = "cancel-user-button";
    public static final String                 CONFIRM_BUTTON                = "save-user-button";
    public static final String                 GENERATE_IMAGE_BUTTON         = "generate-user-image-button";
    public static final String                 USER_DIALOG                   = "user-dialog";
    public static final String                 USER_EMAIL_FIELD              = "user-email-field";
    public static final String                 USER_FIRST_WORKING_DAY_PICKER = "user-first-working-day-picker";
    public static final String                 USER_LAST_WORKING_DAY_PICKER  = "user-last-working-day-picker";
    public static final String                 USER_LOCATION_COUNTRY_COMBO   = "user-location-country-combo";
    public static final String                 USER_LOCATION_STATE_COMBO     = "user-location-state-combo";
    public static final String                 USER_NAME_FIELD               = "user-name-field";
    public static final String                 USER_WORK_WEEK_COMBO          = "user-work-week-combo";
    private final       Image                  avatarPreview;
    private final       AvatarService          avatarService;
    private final       AvatarUpdateRequest    avatarUpdateRequest;
    private final       Binder<User>           binder;
    private final       EmailField             emailField;
    private final       Span                   errorMessage;
    private final       DatePicker             firstWorkingDayPicker;
    private volatile    byte[]                 generatedDarkImageBytes;
    private volatile    byte[]                 generatedDarkImageBytesOriginal;
    private             String                 generatedDarkImagePrompt;
    private             String                 generatedDarkNegativePrompt;
    private             byte[]                 generatedImageBytes;
    private             byte[]                 generatedImageBytesOriginal;
    private             String                 generatedImagePrompt;
    private             String                 generatedNegativePrompt;
    private final       Image                  headerAvatar;
    private final       boolean                isEditMode;
    private final       DatePicker             lastWorkingDayPicker;
    private final       LocationApi            locationApi;
    /**
     * ComboBox for country selection; only initialised in create mode (null in edit mode).
     */
    private final       ComboBox<String>       locationCountryComboBox;
    /**
     * ComboBox for state/region selection; only initialised in create mode (null in edit mode).
     */
    private final       ComboBox<String>       locationStateComboBox;
    private final       TextField              nameField;
    private final       Image                  nameFieldAvatar;
    private final       CheckboxGroup<String>  rolesCheckboxGroup;
    /**
     * Reference to the Save button so extra create-mode validations can toggle it.
     */
    private             Button                 saveButtonRef;
    private final       StableDiffusionService stableDiffusionService;
    private final       User                   user;
    private final       UserApi                userApi;
    private final       UserWorkWeekApi        userWorkWeekApi;
    /**
     * ComboBox for selecting the initial work week; only initialised in create mode (null in edit mode).
     */
    private final       ComboBox<WorkWeek>     workWeekComboBox;

    /**
     * Creates a dialog for creating or editing a user.
     *
     * @param user                   The user to edit, or {@code null} for creating a new user
     * @param avatarService          The avatar generation service
     * @param stableDiffusionService The AI image generation service (optional, can be null)
     * @param userApi                The user API for saving user data
     * @param workWeekApi            REST client for global work weeks (used in create mode to populate the combo box)
     * @param userWorkWeekApi        REST client for user work-week assignments (used to create the initial assignment)
     * @param locationApi            REST client for user locations (used to create the initial location in create mode)
     */
    public UserDialog(User user, AvatarService avatarService, StableDiffusionService stableDiffusionService, UserApi userApi,
                      WorkWeekApi workWeekApi, UserWorkWeekApi userWorkWeekApi, LocationApi locationApi) {
        this.user                   = user;
        this.avatarService          = avatarService;
        this.stableDiffusionService = stableDiffusionService;
        this.userApi                = userApi;
        this.userWorkWeekApi        = userWorkWeekApi;
        this.locationApi            = locationApi;
        isEditMode                  = user != null;
        this.binder                 = new Binder<>(User.class);

        if (isEditMode)
            this.avatarUpdateRequest = userApi.getAvatarFull(user.getId());
        else
            this.avatarUpdateRequest = null;

        // Set the dialog title with an icon (avatar image if available)
        String title = isEditMode ? "Edit User" : "Create User";
        headerAvatar = new Image();
        headerAvatar.setWidth("24px");
        headerAvatar.setHeight("24px");
        headerAvatar.getStyle()
                .set("border-radius", "4px")
                .set("object-fit", "cover");
        if (isEditMode) {
            headerAvatar.setSrc(user.getAvatarUrl());
        }
        // For create mode, leave image src empty - will show broken image icon or can be replaced with VaadinIcon
        getHeader().add(VaadinUtil.createDialogHeader(title, headerAvatar));

        setId(USER_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Create dialog-level error message component (initially hidden)
        errorMessage = VaadinUtil.createDialogErrorMessage();
        dialogLayout.add(errorMessage);

        // Name field
        {
            nameField = new TextField("Name");
            nameField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
            nameField.setId(USER_NAME_FIELD);
            nameField.setWidthFull();
            nameField.setRequired(true);
            nameField.setValueChangeMode(ValueChangeMode.EAGER);

            nameFieldAvatar = new Image();
            nameFieldAvatar.setWidth("20px");
            nameFieldAvatar.setHeight("20px");
            nameFieldAvatar.getStyle()
                    .set("border-radius", "4px")
                    .set("object-fit", "cover");
            if (isEditMode) {
                nameFieldAvatar.setSrc(user.getAvatarUrl());
            }

            nameField.setPrefixComponent(nameFieldAvatar);
            binder.forField(nameField)
                    .asRequired("User name is required")
                    .withValidationStatusHandler(status -> {
                        nameField.setInvalid(status.isError());
                        status.getMessage().ifPresent(nameField::setErrorMessage);
                    })
                    .bind(User::getName, User::setName);
        }

        // AI Image generation button (only show if service is available)
        {
            Button generateImageButton = null;
            if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
                generateImageButton = new Button(new Icon(VaadinIcon.MAGIC));
                generateImageButton.setId(GENERATE_IMAGE_BUTTON);
                generateImageButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                generateImageButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
                generateImageButton.addClickListener(e -> openImagePromptDialog());

                // Disable button if name field is empty
                boolean isNameEmpty = nameField.isEmpty();
                generateImageButton.setEnabled(!isNameEmpty);
                if (isNameEmpty) {
                    generateImageButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                    generateImageButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                }
            }

            // Enable/disable button when name field changes
            final Button finalGenerateImageButton = generateImageButton;
            if (finalGenerateImageButton != null) {
                nameField.addValueChangeListener(e -> {
                    boolean isEmpty = e.getValue().trim().isEmpty();
                    finalGenerateImageButton.setEnabled(!isEmpty);

                    // Update button appearance based on state
                    if (isEmpty) {
                        finalGenerateImageButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                        finalGenerateImageButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    } else {
                        finalGenerateImageButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                        finalGenerateImageButton.removeThemeVariants(ButtonVariant.LUMO_CONTRAST);
                    }
                });
            }

            // Layout for name field and button
            HorizontalLayout nameRow = new HorizontalLayout();
            nameRow.setWidthFull();
            nameRow.setAlignItems(FlexComponent.Alignment.END);
            nameRow.add(nameField);
            if (generateImageButton != null) {
                nameRow.add(generateImageButton);
            }
            nameRow.expand(nameField);
            dialogLayout.add(nameRow);
        }

        // Avatar preview (if image exists or will be generated)
        {
            avatarPreview = new Image();
            avatarPreview.setWidth("64px");
            avatarPreview.setHeight("64px");
            avatarPreview.getStyle()
                    .set("border-radius", "4px")
                    .set("object-fit", "cover")
                    .set("border", "1px solid var(--lumo-contrast-20pct)");
            avatarPreview.setVisible(false);

            dialogLayout.add(avatarPreview);
        }

        // Email field
        {
            emailField = new EmailField("Email");
            emailField.setId(USER_EMAIL_FIELD);
            emailField.setWidthFull();
            emailField.setRequired(true);
            emailField.setPrefixComponent(new Icon(VaadinIcon.ENVELOPE));
            emailField.setValueChangeMode(ValueChangeMode.EAGER);

            binder.forField(emailField)
                    .asRequired("Email address is required")
                    .withValidator(email -> email.contains("@"), "Please enter a valid email address")
                    .withValidationStatusHandler(status -> {
                        emailField.setInvalid(status.isError());
                        status.getMessage().ifPresent(emailField::setErrorMessage);
                    })
                    .bind(User::getEmail, User::setEmail);
        }
        // First working day picker
        {
            firstWorkingDayPicker = new DatePicker("First Working Day");
            firstWorkingDayPicker.setI18n(new DatePicker.DatePickerI18n().setDateFormat("yyyy-MM-dd"));
            firstWorkingDayPicker.setId(USER_FIRST_WORKING_DAY_PICKER);
            firstWorkingDayPicker.setWidthFull();
            firstWorkingDayPicker.setPrefixComponent(new Icon(VaadinIcon.CALENDAR_USER));
        }

        // Last working day picker
        {
            lastWorkingDayPicker = new DatePicker("Last Working Day");
            lastWorkingDayPicker.setI18n(new DatePicker.DatePickerI18n().setDateFormat("yyyy-MM-dd"));
            lastWorkingDayPicker.setId(USER_LAST_WORKING_DAY_PICKER);
            lastWorkingDayPicker.setWidthFull();
            lastWorkingDayPicker.setPrefixComponent(new Icon(VaadinIcon.CALENDAR_USER));
        }

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
            firstWorkingDayPicker.setValue(user.getFirstWorkingDay());
            lastWorkingDayPicker.setValue(user.getLastWorkingDay());
        }

        dialogLayout.add(
                emailField,
                firstWorkingDayPicker,
                lastWorkingDayPicker
        );

        // Roles selection (checkbox group)
        {
            rolesCheckboxGroup = new CheckboxGroup<>();
            rolesCheckboxGroup.setLabel("Roles");
            rolesCheckboxGroup.setItems("ADMIN", "USER");
            rolesCheckboxGroup.addThemeVariants();

            if (isEditMode) {
                // Set selected roles for existing user
                rolesCheckboxGroup.setValue(user.getRoleList().stream()
                        .filter(role -> role.equals("ADMIN") || role.equals("USER"))
                        .collect(java.util.stream.Collectors.toSet()));
            } else {
                // Default to USER role for new users
                rolesCheckboxGroup.setValue(java.util.Set.of("USER"));
            }

            dialogLayout.add(rolesCheckboxGroup);
        }

        // Initial location selection — country + state (create mode only)
        if (!isEditMode) {
            locationCountryComboBox = new ComboBox<>("Country");
            locationCountryComboBox.setId(USER_LOCATION_COUNTRY_COMBO);
            locationCountryComboBox.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
            locationCountryComboBox.setRequired(true);
            locationCountryComboBox.setAllowCustomValue(false);
            locationCountryComboBox.setWidthFull();
            locationCountryComboBox.setPrefixComponent(new Icon(VaadinIcon.GLOBE));
            populateCountries();

            locationStateComboBox = new ComboBox<>("State/Region");
            locationStateComboBox.setId(USER_LOCATION_STATE_COMBO);
            locationStateComboBox.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
            locationStateComboBox.setRequired(true);
            locationStateComboBox.setAllowCustomValue(false);
            locationStateComboBox.setWidthFull();
            locationStateComboBox.setPrefixComponent(new Icon(VaadinIcon.MAP_MARKER));
            locationStateComboBox.setEnabled(false);

            locationCountryComboBox.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    populateStates(event.getValue());
                } else {
                    locationStateComboBox.setItems(new HashSet<>());
                    locationStateComboBox.setEnabled(false);
                }
            });

            dialogLayout.add(locationCountryComboBox, locationStateComboBox);
        } else {
            locationCountryComboBox = null;
            locationStateComboBox   = null;
        }

        // Initial work week selection (create mode only)
        if (!isEditMode) {
            List<WorkWeek> allWorkWeeks = workWeekApi.getAll();
            workWeekComboBox = new ComboBox<>("Initial Work Week");
            workWeekComboBox.setId(USER_WORK_WEEK_COMBO);
            workWeekComboBox.setRequired(true);
            workWeekComboBox.setAllowCustomValue(false);
            workWeekComboBox.setWidthFull();
            workWeekComboBox.setPrefixComponent(new Icon(VaadinIcon.CALENDAR));
            workWeekComboBox.setItemLabelGenerator(WorkWeek::getName);
            workWeekComboBox.setItems(allWorkWeeks);
            workWeekComboBox.setHelperText("Select the initial work week for this user");
            // Auto-select when there is only one option
            if (allWorkWeeks.size() == 1) {
                workWeekComboBox.setValue(allWorkWeeks.getFirst());
            }
            dialogLayout.add(workWeekComboBox);
        } else {
            workWeekComboBox = null;
        }

        HorizontalLayout buttonLayout = VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this, binder);
        dialogLayout.add(buttonLayout);
        add(dialogLayout);

        // Find the save button so create-mode combo validations can toggle its enabled state.
        // Our extra binder status listener fires *after* the one registered by createDialogButtonLayout,
        // so its result is always the final state.
        buttonLayout.getChildren()
                .filter(c -> c.getId().map(CONFIRM_BUTTON::equals).orElse(false))
                .findFirst()
                .ifPresent(c -> saveButtonRef = (Button) c);

        if (!isEditMode && saveButtonRef != null) {
            Runnable updateSave = () -> saveButtonRef.setEnabled(isSaveEnabled());
            binder.addStatusChangeListener(e -> updateSave.run());

            workWeekComboBox.addValueChangeListener(e -> {
                workWeekComboBox.setInvalid(e.getValue() == null);
                if (e.getValue() == null) workWeekComboBox.setErrorMessage("Work week is required");
                updateSave.run();
            });

            // Note: locationCountryComboBox already has a listener that calls populateStates (registered
            // earlier). This second listener fires after that one, so states are already populated when
            // we update the invalid states here.
            locationCountryComboBox.addValueChangeListener(e -> {
                locationCountryComboBox.setInvalid(e.getValue() == null);
                if (e.getValue() == null) locationCountryComboBox.setErrorMessage("Country is required");
                // Changing country always invalidates the state selection
                locationStateComboBox.setInvalid(true);
                locationStateComboBox.setErrorMessage("State/region is required");
                updateSave.run();
            });

            locationStateComboBox.addValueChangeListener(e -> {
                boolean invalid = locationStateComboBox.isEnabled() && e.getValue() == null;
                locationStateComboBox.setInvalid(invalid);
                if (invalid) locationStateComboBox.setErrorMessage("State/region is required");
                updateSave.run();
            });

            // Set initial invalid state so the red indicators are visible from the start.
            // (workWeekComboBox may already have a value if auto-selected.)
            workWeekComboBox.setInvalid(workWeekComboBox.getValue() == null);
            if (workWeekComboBox.getValue() == null) workWeekComboBox.setErrorMessage("Work week is required");
            locationCountryComboBox.setInvalid(true);
            locationCountryComboBox.setErrorMessage("Country is required");
            // locationStateComboBox is disabled initially; it will be marked invalid once a country is selected.
        }

        if (isEditMode) {
            binder.readBean(user);
        } else {
            binder.readBean(new User());
        }

        // Trigger validation to show errors for initially empty fields in create mode
        if (!isEditMode) {
            binder.validate();
        }

        // Focus the name field automatically when the dialog opens
        addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                nameField.focus();
            }
        });

        // Allow submitting the form by pressing Enter from any field in the dialog
        Shortcuts.addShortcutListener(this, e -> save(), Key.ENTER);
    }

    // -------------------------------------------------------------------------
    // Validation helpers
    // -------------------------------------------------------------------------

    private void handleGeneratedImage(GeneratedImageResult lightResult, GeneratedImageResult darkResult) {
        generatedImageBytes         = lightResult.getResizedImage();
        generatedImageBytesOriginal = lightResult.getOriginalImage();
        generatedImagePrompt        = lightResult.getPrompt();
        generatedNegativePrompt     = lightResult.getNegativePrompt();
        generatedDarkNegativePrompt = darkResult != null ? darkResult.getNegativePrompt() : null;
        if (darkResult != null) {
            generatedDarkImageBytes         = darkResult.getResizedImage();
            generatedDarkImageBytesOriginal = darkResult.getOriginalImage();
            generatedDarkImagePrompt        = darkResult.getPrompt();
        }

        // Update UI from callback (might be from async thread)
        getUI().ifPresent(ui -> ui.access(() -> {
            // Create StreamResource for the generated image
            StreamResource resource = new StreamResource("user-avatar.png",
                    () -> new ByteArrayInputStream(lightResult.getResizedImage()));

            // Show preview
            avatarPreview.setSrc(resource);
            avatarPreview.setVisible(true);

            // Update header icon and name field icon with StreamResource (works for both create and edit mode)
            headerAvatar.setSrc(resource);
            nameFieldAvatar.setSrc(resource);

            Notification.show("Avatar set successfully", 3000, Notification.Position.BOTTOM_END);

            // Push the UI update
            ui.push();
        }));
    }

    /**
     * Returns {@code true} when the Save button should be enabled.
     * In create mode this requires the binder to be valid AND both the location and work-week
     * combos to have a selected value.
     *
     * @return whether the form is fully valid
     */
    private boolean isSaveEnabled() {
        if (!binder.isValid()) return false;
        if (!isEditMode) {
            if (workWeekComboBox != null && workWeekComboBox.getValue() == null) return false;
            if (locationCountryComboBox != null && locationCountryComboBox.getValue() == null) return false;
            return locationStateComboBox == null || locationStateComboBox.getValue() != null;
        }
        return true;
    }

    private void openImagePromptDialog() {
        // Use stored prompt if available, otherwise generate default prompt
        String defaultPrompt;
        if (avatarUpdateRequest != null && avatarUpdateRequest.getLightAvatarPrompt() != null && !avatarUpdateRequest.getLightAvatarPrompt().isEmpty()) {
            defaultPrompt = avatarUpdateRequest.getLightAvatarPrompt();
        } else {
            defaultPrompt = User.getDefaultLightAvatarPrompt(nameField.getValue());
        }

        String defaultDarkPrompt = (avatarUpdateRequest != null && avatarUpdateRequest.getDarkAvatarPrompt() != null && !avatarUpdateRequest.getDarkAvatarPrompt().isEmpty())
                ? avatarUpdateRequest.getDarkAvatarPrompt()
                : User.getDefaultDarkAvatarPrompt(nameField.getValue());

        String defaultNegativePrompt = (avatarUpdateRequest != null && avatarUpdateRequest.getLightAvatarNegativePrompt() != null && !avatarUpdateRequest.getLightAvatarNegativePrompt().isEmpty())
                ? avatarUpdateRequest.getLightAvatarNegativePrompt()
                : User.getDefaultLightAvatarNegativePrompt();

        String defaultDarkNegativePrompt = (avatarUpdateRequest != null && avatarUpdateRequest.getDarkAvatarNegativePrompt() != null && !avatarUpdateRequest.getDarkAvatarNegativePrompt().isEmpty())
                ? avatarUpdateRequest.getDarkAvatarNegativePrompt()
                : User.getDefaultDarkAvatarNegativePrompt();

        byte[] initialImage     = isEditMode && avatarUpdateRequest != null && avatarUpdateRequest.getLightAvatarImageOriginal() != null && avatarUpdateRequest.getLightAvatarImageOriginal().length > 0 ? avatarUpdateRequest.getLightAvatarImageOriginal() : null;
        byte[] initialDarkImage = isEditMode && avatarUpdateRequest != null && avatarUpdateRequest.getDarkAvatarImageOriginal() != null && avatarUpdateRequest.getDarkAvatarImageOriginal().length > 0 ? avatarUpdateRequest.getDarkAvatarImageOriginal() : null;
        ImagePromptDialog imageDialog = new ImagePromptDialog(
                avatarService,
                stableDiffusionService,
                defaultPrompt,
                "user",
                this::handleGeneratedImage,
                initialImage,
                initialDarkImage,
                defaultDarkPrompt,
                defaultNegativePrompt,
                defaultDarkNegativePrompt
        );
        imageDialog.open();
    }

    /**
     * Populates the country combo box with all countries supported by jollyday,
     * sorted by their display name in the current locale.
     */
    private void populateCountries() {
        Set<String> countryCodeSet = new HashSet<>(HolidayManager.getSupportedCalendarCodes());

        List<String> sortedCodes = countryCodeSet.stream()
                .sorted((c1, c2) -> new Locale("", c1).getDisplayCountry()
                        .compareTo(new Locale("", c2).getDisplayCountry()))
                .collect(Collectors.toList());

        locationCountryComboBox.setItems(sortedCodes);
        locationCountryComboBox.setItemLabelGenerator(code -> {
            Locale locale = new Locale("", code);
            return locale.getDisplayCountry() + " (" + code + ")";
        });
    }

    /**
     * Populates the state combo box for the given country code, enabling it when states are found.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code
     */
    private void populateStates(String countryCode) {
        try {
            HolidayManager manager      = HolidayManager.getInstance(ManagerParameters.create(countryCode));
            Set<String>    stateCodeSet = new HashSet<>(manager.getCalendarHierarchy().getChildren().keySet());

            if (stateCodeSet.isEmpty()) {
                stateCodeSet.add(countryCode);
            }

            Function<String, String> getDesc = stateCode -> {
                if (stateCode.equals(countryCode)) {
                    return "All of " + new Locale("", countryCode).getDisplayCountry();
                }
                try {
                    String desc = manager.getCalendarHierarchy().getChildren().get(stateCode).getDescription();
                    if (desc != null && !desc.isEmpty()) return desc;
                } catch (Exception ignored) {
                }
                return stateCode;
            };

            List<String> sortedStates = stateCodeSet.stream()
                    .sorted((c1, c2) -> getDesc.apply(c1).compareTo(getDesc.apply(c2)))
                    .collect(Collectors.toList());

            locationStateComboBox.setItems(sortedStates);
            locationStateComboBox.setItemLabelGenerator(stateCode -> {
                if (stateCode.equals(countryCode)) {
                    return "All of " + new Locale("", countryCode).getDisplayCountry();
                }
                try {
                    String desc = manager.getCalendarHierarchy().getChildren().get(stateCode).getDescription();
                    if (desc != null && !desc.isEmpty()) return desc + " (" + stateCode + ")";
                } catch (Exception ignored) {
                }
                return stateCode;
            });
            locationStateComboBox.setEnabled(true);
        } catch (Exception ex) {
            Notification n = Notification.show(
                    "Error loading regions for " + countryCode + ": " + ex.getMessage(),
                    3000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            locationStateComboBox.setEnabled(false);
        }
    }

    private void save() {
        // Clear any previous error messages
        VaadinUtil.hideDialogError(errorMessage);

        User userToSave;
        if (isEditMode) {
            userToSave = user;
        } else {
            userToSave = new User();
        }

        userToSave.setName(nameField.getValue().trim());
        userToSave.setEmail(emailField.getValue().trim());

        // Set selected roles
        List<String> selectedRoles = rolesCheckboxGroup.getValue().stream().toList();
        if (selectedRoles.isEmpty()) {
            Notification.show("At least one role must be selected", 3000, Notification.Position.MIDDLE);
            return;
        }
        userToSave.setRoleList(selectedRoles);

        // Validate work week selection in create mode
        if (!isEditMode) {
            if (workWeekComboBox.getValue() == null) {
                workWeekComboBox.setInvalid(true);
                workWeekComboBox.setErrorMessage("Please select a work week");
                return;
            }
            if (locationCountryComboBox.getValue() == null) {
                locationCountryComboBox.setInvalid(true);
                locationCountryComboBox.setErrorMessage("Please select a country");
                return;
            }
            if (locationStateComboBox.getValue() == null) {
                locationStateComboBox.setInvalid(true);
                locationStateComboBox.setErrorMessage("Please select a state/region");
                return;
            }
        }

        // color is managed by the user profile now; ensure a default is present for new users
        if (!isEditMode && userToSave.getColor() == null) {
            userToSave.setColor(new Color(211, 211, 211)); // Light Gray default
        }

        userToSave.setFirstWorkingDay(firstWorkingDayPicker.getValue());
        userToSave.setLastWorkingDay(lastWorkingDayPicker.getValue());

        // Extract avatar data before save (fields are @JsonIgnore so won't be sent via normal update)
        byte[] avatarImage             = generatedImageBytes;
        byte[] avatarImageOriginal     = generatedImageBytesOriginal;
        String avatarPrompt            = generatedImagePrompt;
        byte[] darkAvatarImage         = generatedDarkImageBytes;
        byte[] darkAvatarImageOriginal = generatedDarkImageBytesOriginal;
        String darkAvatarPrompt        = generatedDarkImagePrompt;
        String negativePrompt          = generatedNegativePrompt;
        String darkNegativePrompt      = generatedDarkNegativePrompt;

        if (avatarImage != null) {
            String newHash = AvatarUtil.computeHash(avatarImage);
            userToSave.setLightAvatarHash(newHash);
        } else if (avatarUpdateRequest == null) {
            //generate default avatar if none exists
            GeneratedImageResult image = stableDiffusionService.generateDefaultAvatar("user");
            avatarImage         = image.getResizedImage();
            avatarImageOriginal = image.getOriginalImage();
            avatarPrompt        = image.getPrompt();
            String newHash = AvatarUtil.computeHash(image.getResizedImage());
            userToSave.setLightAvatarHash(newHash);
            // Also generate a dark default programmatically
            GeneratedImageResult darkImage = stableDiffusionService.generateDefaultDarkAvatar("user");
            darkAvatarImage         = darkImage.getResizedImage();
            darkAvatarImageOriginal = darkImage.getOriginalImage();
        }

        try {
            // Save user to backend
            if (isEditMode) {
                userApi.update(userToSave);
                if (avatarImage != null && avatarImageOriginal != null) {
                    userApi.updateAvatarFull(
                            userToSave.getId(),
                            avatarImage,
                            avatarImageOriginal,
                            avatarPrompt,
                            darkAvatarImage,
                            darkAvatarImageOriginal,
                            darkAvatarPrompt,
                            negativePrompt,
                            darkNegativePrompt
                    );
                }
            } else {
                User saved = userApi.persist(userToSave);

                // For create mode, save avatar now since user didn't exist before
                if (avatarImage != null && avatarImageOriginal != null) {
                    userApi.updateAvatarFull(
                            saved.getId(),
                            avatarImage,
                            avatarImageOriginal,
                            avatarPrompt,
                            darkAvatarImage,
                            darkAvatarImageOriginal,
                            darkAvatarPrompt,
                            negativePrompt,
                            darkNegativePrompt
                    );
                }

                // Create the initial work week assignment
                LocalDate workWeekStart = firstWorkingDayPicker.getValue() != null
                        ? firstWorkingDayPicker.getValue()
                        : LocalDate.now();
                userWorkWeekApi.persist(new UserWorkWeek(workWeekComboBox.getValue(), workWeekStart), saved.getId());

                // Create the initial location assignment using the same start date
                LocalDate locationStart = firstWorkingDayPicker.getValue() != null
                        ? firstWorkingDayPicker.getValue()
                        : LocalDate.now();
                locationApi.persist(new Location(
                        locationCountryComboBox.getValue(),
                        locationStateComboBox.getValue(),
                        locationStart), saved.getId());
            }

            Notification.show("User saved successfully", 3000, Notification.Position.MIDDLE);
            close();
        } catch (Exception e) {
            // Handle unique constraint violations for name and email fields
            // Field-independent errors (like FORBIDDEN) go to dialog-level error message
            Map<String, VaadinUtil.FieldErrorHandler> handlers = new HashMap<>();
            handlers.put("name", this::setNameFieldError);
            handlers.put("email", this::setEmailFieldError);
            VaadinUtil.handleApiException(e, handlers, msg -> VaadinUtil.showDialogError(errorMessage, msg));
        }
    }

    /**
     * Sets an error message on the email field.
     *
     * @param errorMessage The error message to display, or null to clear the error
     */
    public void setEmailFieldError(String errorMessage) {
        emailField.setInvalid(errorMessage != null);
        emailField.setErrorMessage(errorMessage);
    }

    /**
     * Sets an error message on the name field.
     *
     * @param errorMessage The error message to display, or null to clear the error
     */
    public void setNameFieldError(String errorMessage) {
        nameField.setInvalid(errorMessage != null);
        nameField.setErrorMessage(errorMessage);
    }
}
