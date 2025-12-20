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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.rest.api.FeatureApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for creating and editing features.
 */
public class FeatureDialog extends Dialog {

    public static final String                 CANCEL_BUTTON         = "cancel-feature-button";
    public static final String                 CONFIRM_BUTTON        = "save-feature-button";
    public static final String                 FEATURE_DIALOG        = "feature-dialog";
    public static final String                 FEATURE_NAME_FIELD    = "feature-name-field";
    public static final String                 GENERATE_IMAGE_BUTTON = "generate-feature-image-button";
    private final       Image                  avatarPreview;
    private final       AvatarUpdateRequest    avatarUpdateRequest;
    private final       Feature                feature;
    private final       FeatureApi             featureApi;
    private             byte[]                 generatedImageBytes;
    private             byte[]                 generatedImageBytesOriginal;
    private             String                 generatedImagePrompt;
    private final       Image                  headerIcon;
    private final       boolean                isEditMode;
    private final       TextField              nameField;
    private final       Image                  nameFieldImage;
    private final       StableDiffusionService stableDiffusionService;
    private final       Long                   versionId;

    /**
     * Creates a dialog for creating or editing a feature.
     *
     * @param feature                The feature to edit, or null for creating a new feature
     * @param stableDiffusionService The AI image generation service (optional, can be null)
     * @param featureApi             The feature API for saving feature data
     * @param versionId              The version ID for new features (ignored for edit mode)
     */
    public FeatureDialog(Feature feature, StableDiffusionService stableDiffusionService, FeatureApi featureApi, Long versionId) {
        this.feature                = feature;
        this.featureApi             = featureApi;
        this.stableDiffusionService = stableDiffusionService;
        this.versionId              = versionId;
        isEditMode                  = feature != null;

        // Only fetch avatar if editing an existing feature
        if (feature != null)
            this.avatarUpdateRequest = featureApi.getAvatarFull(feature.getId());
        else
            this.avatarUpdateRequest = null;
        setId(FEATURE_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);
        String title = isEditMode ? "Edit Feature" : "Create Feature";

        // Create header icon using avatar proxy endpoint
        headerIcon = new Image();
        headerIcon.setWidth("24px");
        headerIcon.setHeight("24px");
        headerIcon.getStyle()
                .set("border-radius", "4px")
                .set("object-fit", "cover");
        if (isEditMode) {
            headerIcon.setSrc(feature.getAvatarUrl());
        }
        // For create mode, leave image src empty
        getHeader().add(VaadinUtil.createDialogHeader(title, headerIcon));

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Create name field with icon and AI button
        nameField = new TextField("Feature Name");
        nameField.setId(FEATURE_NAME_FIELD);
        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setHelperText("Feature name must be unique");

        // Create name field prefix icon using avatar proxy endpoint
        nameFieldImage = new Image();
        nameFieldImage.setWidth("20px");
        nameFieldImage.setHeight("20px");
        nameFieldImage.getStyle()
                .set("border-radius", "4px")
                .set("object-fit", "cover");
        if (isEditMode) {
            nameFieldImage.setSrc(feature.getAvatarUrl());
        }
        // For create mode, leave image src empty
        nameField.setPrefixComponent(nameFieldImage);

        // Set to eager mode so value changes fire on every keystroke
        nameField.setValueChangeMode(ValueChangeMode.EAGER);

        if (isEditMode) {
            nameField.setValue(feature.getName());
        }

        // AI Image generation button (only show if service is available)
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

        // Avatar preview (if image exists or will be generated)
        avatarPreview = new Image();
        avatarPreview.setWidth("64px");
        avatarPreview.setHeight("64px");
        avatarPreview.getStyle()
                .set("border-radius", "var(--lumo-border-radius)")
                .set("object-fit", "cover")
                .set("border", "1px solid var(--lumo-contrast-20pct)");
        avatarPreview.setVisible(false);

        dialogLayout.add(avatarPreview);

        dialogLayout.add(VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this));

        add(dialogLayout);
    }

    private void handleGeneratedImage(de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult result) {
        this.generatedImageBytes         = result.getResizedImage();
        this.generatedImageBytesOriginal = result.getOriginalImage();
        this.generatedImagePrompt        = result.getPrompt();

        // Update UI from callback (might be from async thread)
        getUI().ifPresent(ui -> ui.access(() -> {
            // Create StreamResource for the generated image
            StreamResource resource = new StreamResource("feature-avatar.png", () -> new ByteArrayInputStream(result.getResizedImage()));

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
            defaultPrompt = Feature.getDefaultAvatarPrompt(nameField.getValue());
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
        if (nameField.getValue().trim().isEmpty()) {
            Notification.show("Please enter a feature name", 3000, Notification.Position.MIDDLE);
            return;
        }

        Feature featureToSave;
        if (isEditMode) {
            featureToSave = feature;
            featureToSave.setName(nameField.getValue().trim());
        } else {
            featureToSave = new Feature();
            featureToSave.setName(nameField.getValue().trim());
            featureToSave.setVersionId(versionId);
        }

        // Extract avatar data before save (fields are @JsonIgnore so won't be sent via normal update)
        byte[] avatarImage         = generatedImageBytes;
        byte[] avatarImageOriginal = generatedImageBytesOriginal;
        String avatarPrompt        = generatedImagePrompt;

        if (avatarImage != null) {
            String newHash = AvatarUtil.computeHash(avatarImage);
            featureToSave.setAvatarHash(newHash);
        } else if (avatarUpdateRequest == null) {
            //generate default avatar if none exists
            GeneratedImageResult image = stableDiffusionService.generateDefaultAvatar("lightbulb");
            avatarImage         = image.getResizedImage();
            avatarImageOriginal = image.getOriginalImage();
            avatarPrompt        = image.getPrompt();
            String newHash = AvatarUtil.computeHash(image.getResizedImage());
            featureToSave.setAvatarHash(newHash);
        }
        try {
            if (isEditMode) {
                // Edit mode
                featureApi.update(featureToSave);

                if (avatarImage != null && avatarImageOriginal != null) {
                    featureApi.updateAvatarFull(featureToSave.getId(), avatarImage, avatarImageOriginal, avatarPrompt);
                }

                Notification.show("Feature updated", 3000, Notification.Position.BOTTOM_START);
            } else {
                // Create mode
                Feature createdFeature = featureApi.persist(featureToSave);

                if (avatarImage != null && avatarImageOriginal != null && createdFeature != null) {
                    featureApi.updateAvatarFull(createdFeature.getId(), avatarImage, avatarImageOriginal, avatarPrompt);
                }

                Notification.show("Feature created", 3000, Notification.Position.BOTTOM_START);
            }
            close();
        } catch (Exception e) {
            if (e instanceof ResponseStatusException && ((ResponseStatusException) e).getStatusCode().equals(HttpStatus.CONFLICT)) {
                setNameFieldError(((ResponseStatusException) e).getReason());
                // Keep the dialog open so the user can correct the name
            } else {
                // For other errors, show generic message and keep dialog open
                Notification notification = new Notification("An error occurred: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
                notification.open();
                // Keep the dialog open so the user can correct the issue
            }
        }
    }

    /**
     * Sets an error message on the feature name field.
     *
     * @param errorMessage The error message to display, or null to clear the error
     */
    public void setNameFieldError(String errorMessage) {
        nameField.setInvalid(errorMessage != null);
        nameField.setErrorMessage(errorMessage);
    }
}
