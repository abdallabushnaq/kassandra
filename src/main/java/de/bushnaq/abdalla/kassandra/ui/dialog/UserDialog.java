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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import java.awt.*;
import java.io.ByteArrayInputStream;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for creating and editing users.
 */
public class UserDialog extends Dialog {

    public static final String                 CANCEL_BUTTON                 = "cancel-user-button";
    public static final String                 CONFIRM_BUTTON                = "save-user-button";
    public static final String                 GENERATE_IMAGE_BUTTON         = "generate-user-image-button";
    public static final String                 USER_DIALOG                   = "user-dialog";
    public static final String                 USER_EMAIL_FIELD              = "user-email-field";
    public static final String                 USER_FIRST_WORKING_DAY_PICKER = "user-first-working-day-picker";
    public static final String                 USER_LAST_WORKING_DAY_PICKER  = "user-last-working-day-picker";
    public static final String                 USER_NAME_FIELD               = "user-name-field";
    private final       Image                  avatarPreview;
    private final       AvatarUpdateRequest    avatarUpdateRequest;
    private final       Binder<User>           binder;
    private final       EmailField             emailField;
    private final       DatePicker             firstWorkingDayPicker;
    private             byte[]                 generatedImageBytes;
    private             byte[]                 generatedImageBytesOriginal;
    private             String                 generatedImagePrompt;
    private final       Image                  headerAvatar;
    private final       boolean                isEditMode;
    private final       DatePicker             lastWorkingDayPicker;
    private final       TextField              nameField;
    private final       Image                  nameFieldAvatar;
    private final       StableDiffusionService stableDiffusionService;
    private final       User                   user;
    private final       UserApi                userApi;

    /**
     * Creates a dialog for creating or editing a user.
     *
     * @param user                   The user to edit, or null for creating a new user
     * @param stableDiffusionService The AI image generation service (optional, can be null)
     * @param userApi                The user API for saving user data
     */
    public UserDialog(User user, StableDiffusionService stableDiffusionService, UserApi userApi) {
        this.user                   = user;
        this.stableDiffusionService = stableDiffusionService;
        this.userApi                = userApi;
        isEditMode                  = user != null;
        this.binder                 = new Binder<>(User.class);

        if (isEditMode) {
            binder.readBean(user);
        } else {
            binder.readBean(new User());
        }

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

        // Name field
        {
            nameField = new TextField("Name");
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
                    .set("border-radius", "var(--lumo-border-radius)")
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
            firstWorkingDayPicker.setId(USER_FIRST_WORKING_DAY_PICKER);
            firstWorkingDayPicker.setWidthFull();
            firstWorkingDayPicker.setPrefixComponent(new Icon(VaadinIcon.CALENDAR_USER));
        }

        // Last working day picker
        {
            lastWorkingDayPicker = new DatePicker("Last Working Day");
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

        dialogLayout.add(VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this, binder));
        add(dialogLayout);

        // Trigger validation to show errors for initially empty fields in create mode
        if (!isEditMode) {
            binder.validate();
        }
    }

    private void handleGeneratedImage(GeneratedImageResult result) {
        generatedImageBytes         = result.getResizedImage();
        generatedImageBytesOriginal = result.getOriginalImage();
        generatedImagePrompt        = result.getPrompt();

        // Compute hash from the resized image
//        String newHash = AvatarUtil.computeHash(generatedImageBytes);

        // For edit mode: Update hash in user DTO and save avatar to database immediately
//        if (isEditMode && user != null) {
//            user.setAvatarHash(newHash);
//            userApi.updateAvatarFull(user.getId(), generatedImageBytes, generatedImageBytesOriginal, generatedImagePrompt);
//        }

        // Update UI from callback (might be from async thread)
        getUI().ifPresent(ui -> ui.access(() -> {
            // Create StreamResource for the generated image
            StreamResource resource = new StreamResource("user-avatar.png",
                    () -> new ByteArrayInputStream(result.getResizedImage()));

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

    private void openImagePromptDialog() {
        // Use stored prompt if available, otherwise generate default prompt
        String defaultPrompt;
        if (avatarUpdateRequest != null && avatarUpdateRequest.getAvatarPrompt() != null && !avatarUpdateRequest.getAvatarPrompt().isEmpty()) {
            defaultPrompt = avatarUpdateRequest.getAvatarPrompt();
        } else {
            defaultPrompt = User.getDefaultAvatarPrompt(nameField.getValue());
        }

        byte[] initialImage = isEditMode && avatarUpdateRequest != null && avatarUpdateRequest.getAvatarImageOriginal() != null && avatarUpdateRequest.getAvatarImageOriginal().length > 0 ? avatarUpdateRequest.getAvatarImageOriginal() : null;
        ImagePromptDialog imageDialog = new ImagePromptDialog(
                stableDiffusionService,
                defaultPrompt,
                this::handleGeneratedImage,
                initialImage
        );
        imageDialog.open();
    }

    private void save() {

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

        // Extract avatar data before save (fields are @JsonIgnore so won't be sent via normal update)
        byte[] avatarImage         = generatedImageBytes;
        byte[] avatarImageOriginal = generatedImageBytesOriginal;
        String avatarPrompt        = generatedImagePrompt;

        if (avatarImage != null) {
            String newHash = AvatarUtil.computeHash(avatarImage);
            userToSave.setAvatarHash(newHash);
        } else if (avatarUpdateRequest == null) {
            //generate default avatar if none exists
            GeneratedImageResult image = stableDiffusionService.generateDefaultAvatar("user");
            avatarImage         = image.getResizedImage();
            avatarImageOriginal = image.getOriginalImage();
            avatarPrompt        = image.getPrompt();
            String newHash = AvatarUtil.computeHash(image.getResizedImage());
            userToSave.setAvatarHash(newHash);
        }

        // Save user to backend
        if (isEditMode) {
            userApi.update(userToSave);
            if (avatarImage != null && avatarImageOriginal != null) {
                userApi.updateAvatarFull(
                        userToSave.getId(),
                        avatarImage,
                        avatarImageOriginal,
                        avatarPrompt
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
                        avatarPrompt
                );
            }
        }

        Notification.show("User saved successfully", 3000, Notification.Position.MIDDLE);
        close();

    }
}
