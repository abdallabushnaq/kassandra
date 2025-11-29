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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.StreamResource;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionConfig;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionException;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for generating AI images using prompts.
 */
public class ImagePromptDialog extends Dialog {

    public static final String         ACCEPT_BUTTON       = "accept-image-button";
    public static final String         CANCEL_BUTTON       = "cancel-image-button";
    public static final String         GENERATE_BUTTON     = "generate-image-button";
    public static final String         IMAGE_PROMPT_DIALOG = "image-prompt-dialog";
    public static final String         PROMPT_FIELD        = "image-prompt-field";
    private final       Button         acceptButton;
    private final       AcceptCallback acceptCallback;
    private final       Button         generateButton;
    private             byte[]         generatedImage;
    private final       Div            previewContainer;
    private final       TextArea       promptField;
    @Autowired
    StableDiffusionConfig stableDiffusionConfig;
    private final StableDiffusionService stableDiffusionService;

    /**
     * Creates a dialog for generating AI images.
     *
     * @param stableDiffusionService The Stable Diffusion service
     * @param defaultPrompt          Default prompt text (can be null)
     * @param acceptCallback         Callback that receives the generated image bytes
     */
    public ImagePromptDialog(StableDiffusionService stableDiffusionService, String defaultPrompt, AcceptCallback acceptCallback) {
        this.stableDiffusionService = stableDiffusionService;
        this.acceptCallback         = acceptCallback;

        setId(IMAGE_PROMPT_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);
        getHeader().add(VaadinUtil.createDialogHeader("Generate AI Image", VaadinIcon.MAGIC));

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Check if Stable Diffusion is available
        if (!stableDiffusionService.isAvailable()) {
            Div warningDiv = new Div();
            warningDiv.getStyle()
                    .set("background-color", "var(--lumo-error-color-10pct)")
                    .set("color", "var(--lumo-error-text-color)")
                    .set("padding", "var(--lumo-space-m)")
                    .set("border-radius", "var(--lumo-border-radius)")
                    .set("margin-bottom", "var(--lumo-space-m)");
            warningDiv.add(new Icon(VaadinIcon.WARNING));
            warningDiv.add(" Stable Diffusion API is not available. Please ensure it's running at " + stableDiffusionConfig.getApiUrl());
            dialogLayout.add(warningDiv);
        }

        // Prompt text area
        promptField = new TextArea("Image Description");
        promptField.setId(PROMPT_FIELD);
        promptField.setWidthFull();
        promptField.setPlaceholder("Describe the image you want to generate...");
        promptField.setHelperText("Be specific about style, colors, and composition");
        promptField.setMinHeight("100px");
        if (defaultPrompt != null && !defaultPrompt.isEmpty()) {
            promptField.setValue(defaultPrompt);
        }
        dialogLayout.add(promptField);

        // Preview container
        previewContainer = new Div();
        previewContainer.getStyle()
                .set("border", "1px dashed var(--lumo-contrast-30pct)")
                .set("border-radius", "var(--lumo-border-radius)")
                .set("padding", "var(--lumo-space-m)")
                .set("width", "256px")
                .set("height", "256px")
                .set("min-width", "256px")
                .set("min-height", "256px")
                .set("max-width", "256px")
                .set("max-height", "256px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("overflow", "hidden");

        Div placeholderText = new Div();
        placeholderText.setText("Generated image will appear here");
        placeholderText.getStyle().set("color", "var(--lumo-contrast-50pct)");
        previewContainer.add(placeholderText);

        dialogLayout.add(previewContainer);

        // Buttons
        generateButton = new Button("Generate", new Icon(VaadinIcon.MAGIC));
        generateButton.setId(GENERATE_BUTTON);
        generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateButton.addClickListener(e -> generateImage());

        acceptButton = new Button("Accept", new Icon(VaadinIcon.CHECK));
        acceptButton.setId(ACCEPT_BUTTON);
        acceptButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        acceptButton.setEnabled(false);
        acceptButton.addClickListener(e -> acceptImage());

        Button cancelButton = new Button("Cancel", new Icon(VaadinIcon.CLOSE));
        cancelButton.setId(CANCEL_BUTTON);
        cancelButton.addClickListener(e -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(generateButton, acceptButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        dialogLayout.add(buttonLayout);

        add(dialogLayout);
    }

    private void acceptImage() {
        if (generatedImage != null) {
            acceptCallback.accept(generatedImage);
            close();
        }
    }

    private void displayGeneratedImage(byte[] imageBytes) {
        previewContainer.removeAll();

        // Store bytes in final variable for proper closure
        final byte[] imageBytesForResource = imageBytes;

        // Create unique resource name to avoid caching issues
        String resourceName = "generated-image-" + System.currentTimeMillis() + ".png";

        StreamResource resource = new StreamResource(resourceName,
                () -> new ByteArrayInputStream(imageBytesForResource));

        // Disable caching to ensure fresh image is always shown
        resource.setContentType("image/png");
        resource.setCacheTime(0);

        Image image = new Image(resource, "Generated image");
        image.setWidth("256px");
        image.setHeight("256px");
        image.getStyle()
                .set("border-radius", "var(--lumo-border-radius)")
                .set("object-fit", "contain")
                .set("display", "block");

        previewContainer.add(image);
    }

    private void generateImage() {
        String prompt = promptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a description", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Disable button and show loading state
        generateButton.setEnabled(false);
        generateButton.setText("Generating...");
        acceptButton.setEnabled(false);

        // Clear preview and show progress bar
        previewContainer.removeAll();

        VerticalLayout loadingLayout = new VerticalLayout();
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loadingLayout.setPadding(false);
        loadingLayout.setSpacing(true);

        Icon hourglassIcon = new Icon(VaadinIcon.HOURGLASS);
        hourglassIcon.setSize("32px");
        hourglassIcon.getStyle().set("color", "var(--lumo-primary-color)");

        Div loadingText = new Div();
        loadingText.setText("Generating image...");
        loadingText.getStyle()
                .set("color", "var(--lumo-contrast-60pct)")
                .set("font-weight", "500");

        Div progressText = new Div();
        progressText.setText("Initializing...");
        progressText.getStyle()
                .set("color", "var(--lumo-contrast-50pct)")
                .set("font-size", "var(--lumo-font-size-s)");

        com.vaadin.flow.component.progressbar.ProgressBar progressBar = new com.vaadin.flow.component.progressbar.ProgressBar();
        progressBar.setMin(0);
        progressBar.setMax(1);
        progressBar.setValue(0);
        progressBar.setWidth("80%");

        loadingLayout.add(hourglassIcon, loadingText, progressText, progressBar);
        previewContainer.add(loadingLayout);

        // Generate image asynchronously
        getUI().ifPresent(ui -> {
            new Thread(() -> {
                try {
                    // Generate with progress callback
                    byte[] imageBytes = stableDiffusionService.generateImage(prompt, 256, (progress, step, totalSteps) -> {
                        // Update UI with progress
                        ui.access(() -> {
                            progressBar.setValue(progress);
                            progressText.setText(String.format("Step %d / %d (%.0f%%)", step, totalSteps, progress * 100));
                            ui.push();
                        });
                    });

                    generatedImage = imageBytes;

                    ui.access(() -> {
                        displayGeneratedImage(imageBytes);
                        generateButton.setEnabled(true);
                        generateButton.setText("Generate");
                        acceptButton.setEnabled(true);

                        Notification notification = Notification.show("Image generated successfully!", 3000, Notification.Position.BOTTOM_END);
                        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                        // Push UI updates
                        ui.push();
                    });
                } catch (StableDiffusionException ex) {
                    ui.access(() -> {
                        generateButton.setEnabled(true);
                        generateButton.setText("Generate");

                        previewContainer.removeAll();
                        Div errorText = new Div();
                        errorText.setText("Failed to generate image: " + ex.getMessage());
                        errorText.getStyle().set("color", "var(--lumo-error-text-color)");
                        previewContainer.add(new Icon(VaadinIcon.WARNING), errorText);

                        Notification notification = Notification.show("Generation failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

                        // Push UI updates
                        ui.push();
                    });
                }
            }).start();
        });
    }

    /**
     * Functional interface for the accept callback
     */
    @FunctionalInterface
    public interface AcceptCallback {
        void accept(byte[] imageBytes);
    }
}

