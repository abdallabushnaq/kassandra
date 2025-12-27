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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.rest.api.SprintApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import java.io.ByteArrayInputStream;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for creating and editing sprints.
 */
public class SprintDialog extends Dialog {

    public static final String                 CANCEL_BUTTON         = "cancel-sprint-button";
    public static final String                 CONFIRM_BUTTON        = "save-sprint-button";
    public static final String                 GENERATE_IMAGE_BUTTON = "generate-sprint-image-button";
    public static final String                 SPRINT_DIALOG         = "sprint-dialog";
    public static final String                 SPRINT_NAME_FIELD     = "sprint-name-field";
    private final       Image                  avatarPreview;
    private final       AvatarUpdateRequest    avatarUpdateRequest;
    private final       Binder<Sprint>         binder;
    private final       Span                   errorMessage;
    private final       Long                   featureId;
    private             byte[]                 generatedImageBytes;
    private             byte[]                 generatedImageBytesOriginal;
    private             String                 generatedImagePrompt;
    private final       Image                  headerIcon;
    private final       boolean                isEditMode;
    private final       TextField              nameField;
    private final       Image                  nameFieldImage;
    private final       Sprint                 sprint;
    private final       SprintApi              sprintApi;
    private final       StableDiffusionService stableDiffusionService;

    /**
     * Creates a dialog for creating or editing a sprint.
     *
     * @param sprint                 The sprint to edit, or null for creating a new sprint
     * @param stableDiffusionService The AI image generation service (optional, can be null)
     * @param sprintApi              The sprint API for saving sprint data
     * @param featureId              The feature ID for new sprints (ignored for edit mode)
     */
    public SprintDialog(Sprint sprint, StableDiffusionService stableDiffusionService, SprintApi sprintApi, Long featureId) {
        this.sprint                 = sprint;
        this.sprintApi              = sprintApi;
        this.stableDiffusionService = stableDiffusionService;
        this.featureId              = featureId;
        isEditMode                  = sprint != null;
        this.binder                 = new Binder<>(Sprint.class);

        // Only fetch avatar if editing an existing sprint
        if (sprint != null)
            this.avatarUpdateRequest = sprintApi.getAvatarFull(sprint.getId());
        else
            this.avatarUpdateRequest = null;
        setId(SPRINT_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);
        String title = isEditMode ? "Edit Sprint" : "Create Sprint";

        // Create header icon using avatar proxy endpoint
        headerIcon = new Image();
        headerIcon.setWidth("24px");
        headerIcon.setHeight("24px");
        headerIcon.getStyle()
                .set("border-radius", "4px")
                .set("object-fit", "cover");
        if (isEditMode) {
            headerIcon.setSrc(sprint.getAvatarUrl());
        }
        // For create mode, leave image src empty
        getHeader().add(VaadinUtil.createDialogHeader(title, headerIcon));

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Create dialog-level error message component (initially hidden)
        errorMessage = VaadinUtil.createDialogErrorMessage();
        dialogLayout.add(errorMessage);

        // Create name field with icon and AI button
        {
            nameField = new TextField("Sprint Name");
            nameField.setId(SPRINT_NAME_FIELD);
            nameField.setWidthFull();
            nameField.setRequired(true);
            nameField.setHelperText("Sprint name must be unique");

            binder.forField(nameField)
                    .asRequired("Sprint name is required")
                    .withValidationStatusHandler(status -> {
                        nameField.setInvalid(status.isError());
                        status.getMessage().ifPresent(nameField::setErrorMessage);
                    })
                    .bind(Sprint::getName, Sprint::setName);

            // Create name field prefix icon using avatar proxy endpoint
            nameFieldImage = new Image();
            nameFieldImage.setWidth("20px");
            nameFieldImage.setHeight("20px");
            nameFieldImage.getStyle()
                    .set("border-radius", "4px")
                    .set("object-fit", "cover");
            if (isEditMode) {
                nameFieldImage.setSrc(sprint.getAvatarUrl());
            }
            // For create mode, leave image src empty
            nameField.setPrefixComponent(nameFieldImage);

            // Set to eager mode so value changes fire on every keystroke
            nameField.setValueChangeMode(ValueChangeMode.EAGER);
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

            // Enable/disable button when name field changes (must be after the if block)
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
            nameRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER); // Center vertically
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

        dialogLayout.add(VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this, binder));

        add(dialogLayout);

        if (isEditMode) {
            binder.readBean(sprint);
        } else {
            binder.readBean(new Sprint());
        }

        // Trigger validation to show errors for initially empty fields in create mode
        if (!isEditMode) {
            binder.validate();
        }
    }

    private void handleGeneratedImage(de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult result) {
        this.generatedImageBytes         = result.getResizedImage();
        this.generatedImageBytesOriginal = result.getOriginalImage();
        this.generatedImagePrompt        = result.getPrompt();

        // Update UI from callback (might be from async thread)
        getUI().ifPresent(ui -> ui.access(() -> {
            // Create StreamResource for the generated image
            StreamResource resource = new StreamResource("sprint-avatar.png", () -> new ByteArrayInputStream(result.getResizedImage()));

            // Show preview
            avatarPreview.setSrc(resource);
            avatarPreview.setVisible(true);

            // Update header icon and name field icon with StreamResource (works for both create and edit mode)
            headerIcon.setSrc(resource);
            nameFieldImage.setSrc(resource);

            Notification.show("Image set successfully", 3000, Notification.Position.BOTTOM_END);

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
            defaultPrompt = Sprint.getDefaultAvatarPrompt(nameField.getValue());
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
        // Clear any previous error messages
        VaadinUtil.hideDialogError(errorMessage);

        Sprint sprintToSave;
        if (isEditMode) {
            sprintToSave = sprint;
            sprintToSave.setName(nameField.getValue().trim());
        } else {
            sprintToSave = new Sprint();
            sprintToSave.setName(nameField.getValue().trim());
            sprintToSave.setFeatureId(featureId);
        }

        // Extract avatar data before save (fields are @JsonIgnore so won't be sent via normal update)
        byte[] avatarImage         = generatedImageBytes;
        byte[] avatarImageOriginal = generatedImageBytesOriginal;
        String avatarPrompt        = generatedImagePrompt;

        if (avatarImage != null) {
            String newHash = AvatarUtil.computeHash(avatarImage);
            sprintToSave.setAvatarHash(newHash);
        } else if (avatarUpdateRequest == null) {
            //generate default avatar if none exists
            GeneratedImageResult image = stableDiffusionService.generateDefaultAvatar("exit");
            avatarImage         = image.getResizedImage();
            avatarImageOriginal = image.getOriginalImage();
            avatarPrompt        = image.getPrompt();
            String newHash = AvatarUtil.computeHash(image.getResizedImage());
            sprintToSave.setAvatarHash(newHash);
        }
        try {
            if (isEditMode) {
                // Edit mode
                sprintApi.update(sprintToSave);

                if (avatarImage != null && avatarImageOriginal != null) {
                    sprintApi.updateAvatarFull(sprintToSave.getId(), avatarImage, avatarImageOriginal, avatarPrompt);
                }

                Notification.show("Sprint updated", 3000, Notification.Position.BOTTOM_START);
            } else {
                // Create mode
                Sprint createdSprint = sprintApi.persist(sprintToSave);

                if (avatarImage != null && avatarImageOriginal != null && createdSprint != null) {
                    sprintApi.updateAvatarFull(createdSprint.getId(), avatarImage, avatarImageOriginal, avatarPrompt);
                }

                Notification.show("Sprint created", 3000, Notification.Position.BOTTOM_START);
            }
            close();
        } catch (Exception e) {
            // Use VaadinUtil to handle the exception with field-specific and dialog-level error routing
            VaadinUtil.handleApiException(e, "name", this::setNameFieldError, msg -> VaadinUtil.showDialogError(errorMessage, msg));
        }
    }

    /**
     * Sets an error message on the sprint name field.
     *
     * @param errorMessage The error message to display, or null to clear the error
     */
    public void setNameFieldError(String errorMessage) {
        nameField.setInvalid(errorMessage != null);
        nameField.setErrorMessage(errorMessage);
    }
}
