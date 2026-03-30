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
import com.vaadin.flow.component.html.Anchor;
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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionException;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * A reusable dialog for generating AI images using prompts.
 */
public class ImagePromptDialog extends Dialog {

    // Button ID constants for Selenium tests
    public static final String                 ID_ACCEPT_BUTTON              = "accept-image-button";
    public static final String                 ID_CANCEL_BUTTON              = "cancel-image-button";
    public static final String                 ID_DARK_NEGATIVE_PROMPT_FIELD = "dark-negative-prompt-field";
    public static final String                 ID_DARK_PROMPT_FIELD          = "dark-prompt-field";
    public static final String                 ID_DARK_UPDATE_BUTTON         = "dark-update-image-button";
    public static final String                 ID_DOWNLOAD_BUTTON            = "download-image-button";
    public static final String                 ID_GENERATE_BUTTON            = "generate-image-button";
    public static final String                 ID_IMAGE_PROMPT_FIELD         = "image-prompt-field";
    public static final String                 ID_NEGATIVE_PROMPT_FIELD      = "negative-prompt-field";
    public static final String                 ID_UPDATE_BUTTON              = "update-image-button";
    public static final String                 ID_UPLOAD_BUTTON              = "upload-image-button";
    private final       Button                 acceptButton;
    private final       AcceptCallback         acceptCallback;
    private final       AvatarService          avatarService;
    private final       Button                 cancelButton;
    private final       String                 darkIconName;
    private final       TextArea               darkNegativePromptField;
    private final       Div                    darkPreviewContainer;
    private final       TextArea               darkPromptField;
    private             Button                 darkUpdateButton;
    private final       Button                 generateButton;
    private volatile    byte[]                 generatedDarkImage;
    private volatile    byte[]                 generatedDarkImageOriginal;
    private             byte[]                 generatedImage;
    private             byte[]                 generatedImageOriginal;
    private volatile    long                   generatedImageSeed            = -1L;
    private             byte[]                 initialImage;
    private final       TextArea               negativePromptField;
    private final       Div                    previewContainer;
    private final       TextArea               promptField;
    private final       StableDiffusionService stableDiffusionService;
    private final       Button                 lightUpdateButton;

    /**
     * Full constructor for the side-by-side light/dark avatar preview dialog.
     *
     * @param avatarService             The avatar generation service
     * @param stableDiffusionService    The Stable Diffusion service
     * @param defaultPrompt             Default prompt text for the light avatar (can be null)
     * @param darkIconName              Icon name for the programmatic dark fallback (e.g., {@code "user"}); can be null
     * @param acceptCallback            Callback that receives both light and dark results plus negative prompts
     * @param initialImage              Existing light original for img2img "Update" mode; can be null
     * @param initialDarkImage          Existing dark original to pre-populate the dark preview; can be null
     * @param defaultDarkPrompt         Default dark prompt (base + dark suffix); null → computed from defaultPrompt
     * @param defaultNegativePrompt     Default negative prompt for the light avatar; null → {@link StableDiffusionService#NEGATIVE_PROMPT}
     * @param defaultDarkNegativePrompt Default negative prompt for the dark avatar; null → same as defaultNegativePrompt
     */
    public ImagePromptDialog(AvatarService avatarService, StableDiffusionService stableDiffusionService, String defaultPrompt,
                             String darkIconName, AcceptCallback acceptCallback, byte[] initialImage, byte[] initialDarkImage,
                             String defaultDarkPrompt, String defaultNegativePrompt, String defaultDarkNegativePrompt) {
        this.avatarService          = avatarService;
        this.stableDiffusionService = stableDiffusionService;
        this.acceptCallback         = acceptCallback;
        this.initialImage           = initialImage;
        this.darkIconName           = darkIconName;

        // Resolve default values for the new prompt fields
        String resolvedNegativePrompt     = defaultNegativePrompt != null ? defaultNegativePrompt : StableDiffusionService.NEGATIVE_PROMPT;
        String resolvedDarkPrompt         = defaultDarkPrompt != null ? defaultDarkPrompt : (defaultPrompt != null ? defaultPrompt + AvatarService.DARK_PROMPT_SUFFIX : AvatarService.DARK_PROMPT_SUFFIX);
        String resolvedDarkNegativePrompt = defaultDarkNegativePrompt != null ? defaultDarkNegativePrompt : resolvedNegativePrompt;

        setId("image-prompt-dialog");
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
            warningDiv.add(" Stable Diffusion API is not available. Please ensure it's running at " + avatarService.getConfig().getApiUrl());
            dialogLayout.add(warningDiv);
        }

        // Prompt text area (light avatar base prompt)
        promptField = new TextArea("Image Description");
        promptField.setId(ID_IMAGE_PROMPT_FIELD);
        promptField.setWidthFull();
        promptField.setPlaceholder("Describe the image you want to generate...");
        promptField.setHelperText("Be specific about style, colors, and composition");
        promptField.setMinHeight("100px");
        if (defaultPrompt != null && !defaultPrompt.isEmpty()) {
            promptField.setValue(defaultPrompt);
        }
        dialogLayout.add(promptField);

        // Negative prompt text area (shared / light)
        negativePromptField = new TextArea("Negative Prompt");
        negativePromptField.setId(ID_NEGATIVE_PROMPT_FIELD);
        negativePromptField.setWidthFull();
        negativePromptField.setHelperText("Things to avoid in the generated image");
        negativePromptField.setMinHeight("80px");
        negativePromptField.setValue(resolvedNegativePrompt);
        dialogLayout.add(negativePromptField);

        // Dark prompt / dark negative prompt
        darkPromptField = new TextArea("Dark Avatar Prompt");
        darkPromptField.setId(ID_DARK_PROMPT_FIELD);
        darkPromptField.setWidthFull();
        darkPromptField.setHelperText("Full prompt for the dark-background variant (base prompt + dark suffix)");
        darkPromptField.setMinHeight("80px");
        darkPromptField.setValue(resolvedDarkPrompt);
        dialogLayout.add(darkPromptField);

        darkNegativePromptField = new TextArea("Dark Avatar Negative Prompt");
        darkNegativePromptField.setId(ID_DARK_NEGATIVE_PROMPT_FIELD);
        darkNegativePromptField.setWidthFull();
        darkNegativePromptField.setHelperText("Things to avoid in the dark avatar");
        darkNegativePromptField.setMinHeight("80px");
        darkNegativePromptField.setValue(resolvedDarkNegativePrompt);
        dialogLayout.add(darkNegativePromptField);

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

        if (initialImage != null && initialImage.length > 0) {
            displayGeneratedImage(initialImage);
        } else {
            Div placeholderText = new Div();
            placeholderText.setText("Generated image will appear here");
            placeholderText.getStyle().set("color", "var(--lumo-contrast-50pct)");
            previewContainer.add(placeholderText);
        }
        dialogLayout.add(previewContainer);

        // --- Preview and upload/download controls ---
        // Upload button
        MemoryBuffer uploadBuffer = new MemoryBuffer();
        Upload       upload       = new Upload(uploadBuffer);
        upload.setId(ID_UPLOAD_BUTTON);
        upload.setAcceptedFileTypes(".png");
        upload.setMaxFiles(1);
        upload.setDropAllowed(true);
        upload.setAutoUpload(true);
        upload.getElement().setAttribute("title", "Upload PNG");

        // Buttons (declare as local variables, and use them in listeners)
        generateButton = new Button("Generate", new Icon(VaadinIcon.MAGIC));
        generateButton.setId(ID_GENERATE_BUTTON);
        generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        generateButton.addClickListener(e -> generateLightVariant());

        lightUpdateButton = new Button(new Icon(VaadinIcon.REFRESH));
        lightUpdateButton.setId(ID_UPDATE_BUTTON);
        lightUpdateButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        lightUpdateButton.getElement().setAttribute("title", "Regenerate light avatar");
        lightUpdateButton.setEnabled(true);
        lightUpdateButton.addClickListener(e -> updateImage());

        acceptButton = new Button("Accept", new Icon(VaadinIcon.CHECK));
        acceptButton.setId(ID_ACCEPT_BUTTON);
        acceptButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        acceptButton.setEnabled(false);
        acceptButton.addClickListener(e -> acceptImage());

        // If initial image exists, enable Accept button and set generatedImage
        if (initialImage != null && initialImage.length > 0) {
            acceptButton.setEnabled(true);
            generatedImage         = initialImage;
            generatedImageOriginal = initialImage;
        }

        cancelButton = new Button("Cancel", new Icon(VaadinIcon.CLOSE));
        cancelButton.setId(ID_CANCEL_BUTTON);
        cancelButton.addClickListener(e -> close());

        // Use local variables in upload succeeded listener
        upload.addSucceededListener(event -> {
            try {
                BufferedImage inputImage = ImageIO.read(uploadBuffer.getInputStream());
                if (inputImage == null) {
                    Notification.show("Invalid PNG file.", 3000, Notification.Position.MIDDLE);
                    return;
                }
                BufferedImage resized = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                Graphics2D    g2d     = resized.createGraphics();
                g2d.drawImage(inputImage, 0, 0, 256, 256, null);
                g2d.dispose();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resized, "png", baos);
                baos.flush();
                byte[] imageBytes = baos.toByteArray();
                baos.close();
                generatedImage = imageBytes;
                displayGeneratedImage(imageBytes);
                acceptButton.setEnabled(true);
                lightUpdateButton.setEnabled(true);
                Notification.show("Image uploaded and resized.", 2000, Notification.Position.BOTTOM_END);
                generateDarkVariant();
            } catch (Exception ex) {
                Notification.show("Failed to process image: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        // Download button
        Anchor downloadAnchor = new Anchor();
        Button downloadButton = new Button(new Icon(VaadinIcon.DOWNLOAD));
        downloadButton.setId(ID_DOWNLOAD_BUTTON);
        downloadButton.getElement().setAttribute("title", "Download PNG");
        downloadAnchor.add(downloadButton);
        downloadAnchor.getElement().setAttribute("download", true);
        downloadButton.addClickListener(e -> {
            byte[] imageToDownload = generatedImage != null ? generatedImage : initialImage;
            if (imageToDownload != null && imageToDownload.length > 0) {
                StreamResource resource = new StreamResource("ai-image.png", () -> new ByteArrayInputStream(imageToDownload));
                resource.setContentType("image/png");
                resource.setCacheTime(0);
                downloadAnchor.setHref(resource);
            } else {
                Notification.show("No image to download.", 2000, Notification.Position.MIDDLE);
            }
        });

        VerticalLayout uploadDownloadCol = new VerticalLayout(upload, downloadAnchor);
        uploadDownloadCol.setPadding(false);
        uploadDownloadCol.setSpacing(true);
        uploadDownloadCol.setAlignItems(FlexComponent.Alignment.STRETCH);
        uploadDownloadCol.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        // ── Light column ──────────────────────────────────────────────────
        com.vaadin.flow.component.html.Span lightLabel = new com.vaadin.flow.component.html.Span("Light");
        lightLabel.getStyle().set("font-weight", "600").set("color", "var(--lumo-secondary-text-color)");
        HorizontalLayout lightTitleRow = new HorizontalLayout(lightLabel, lightUpdateButton);
        lightTitleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        lightTitleRow.setWidthFull();
        lightTitleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        VerticalLayout lightColumn = new VerticalLayout(lightTitleRow, previewContainer);
        lightColumn.setPadding(false);
        lightColumn.setSpacing(true);
        lightColumn.setAlignItems(FlexComponent.Alignment.CENTER);

        // ── Dark column ───────────────────────────────────────────────────
        darkPreviewContainer = new Div();
        darkPreviewContainer.getStyle()
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
                .set("background-color", "#1e1e1e")
                .set("overflow", "hidden");

        if (initialDarkImage != null && initialDarkImage.length > 0) {
            displayInContainer(darkPreviewContainer, initialDarkImage);
            generatedDarkImage         = initialDarkImage;
            generatedDarkImageOriginal = initialDarkImage;
        } else {
            Div darkPlaceholder = new Div();
            darkPlaceholder.setText("Dark variant will appear here");
            darkPlaceholder.getStyle().set("color", "#888").set("text-align", "center");
            darkPreviewContainer.add(darkPlaceholder);
        }

        com.vaadin.flow.component.html.Span darkLabel = new com.vaadin.flow.component.html.Span("Dark");
        darkLabel.getStyle().set("font-weight", "600").set("color", "var(--lumo-secondary-text-color)");

        darkUpdateButton = new Button(new Icon(VaadinIcon.REFRESH));
        darkUpdateButton.setId(ID_DARK_UPDATE_BUTTON);
        darkUpdateButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        darkUpdateButton.getElement().setAttribute("title", "Regenerate dark avatar");
        darkUpdateButton.setEnabled(initialDarkImage != null && initialDarkImage.length > 0);
        darkUpdateButton.addClickListener(e -> generateDarkVariant());

        HorizontalLayout darkTitleRow = new HorizontalLayout(darkLabel, darkUpdateButton);
        darkTitleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        darkTitleRow.setWidthFull();
        darkTitleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        VerticalLayout darkColumn = new VerticalLayout(darkTitleRow, darkPreviewContainer);
        darkColumn.setPadding(false);
        darkColumn.setSpacing(true);
        darkColumn.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout previewRow = new HorizontalLayout(lightColumn, darkColumn, uploadDownloadCol);
        previewRow.setAlignItems(FlexComponent.Alignment.CENTER);
        previewRow.setSpacing(true);
        previewRow.setWidthFull();
        dialogLayout.add(previewRow);

        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout(generateButton, acceptButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        dialogLayout.add(buttonLayout);
        add(dialogLayout);
    }

    // Update methods to use these refs
    private void acceptImage() {
        if (generatedImage != null) {
            String prompt             = promptField.getValue().trim();
            String negativePrompt     = negativePromptField.getValue().trim();
            String darkPrompt         = darkPromptField.getValue().trim();
            String darkNegativePrompt = darkNegativePromptField.getValue().trim();

            GeneratedImageResult lightResult = new GeneratedImageResult(
                    generatedImageOriginal != null ? generatedImageOriginal : generatedImage,
                    prompt,
                    generatedImage
            );
            lightResult.setNegativePrompt(negativePrompt);

            GeneratedImageResult darkResult = (generatedDarkImage != null) ? new GeneratedImageResult(
                    generatedDarkImageOriginal != null ? generatedDarkImageOriginal : generatedDarkImage,
                    darkPrompt,
                    generatedDarkImage)
                    : null;
            if (darkResult != null) {
                darkResult.setNegativePrompt(darkNegativePrompt);
            }

            acceptCallback.accept(lightResult, darkResult);
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

    /**
     * Renders {@code imageBytes} inside the given container, replacing any existing content.
     *
     * @param container  The target {@link Div} container
     * @param imageBytes The PNG image bytes to display
     */
    private void displayInContainer(Div container, byte[] imageBytes) {
        container.removeAll();
        String         resourceName = "image-" + System.currentTimeMillis() + ".png";
        StreamResource resource     = new StreamResource(resourceName, () -> new ByteArrayInputStream(imageBytes));
        resource.setContentType("image/png");
        resource.setCacheTime(0);
        Image img = new Image(resource, "Preview");
        img.setWidth("256px");
        img.setHeight("256px");
        img.getStyle().set("object-fit", "contain").set("display", "block");
        container.add(img);
    }

    /**
     * Generates the dark-avatar variant from the current light image.
     * Safe to call from any thread (UI thread or background thread):
     * all Vaadin state access is protected by a single {@code ui.access()} call.
     * Does nothing when the dark panel is disabled ({@code enableDark == false}).
     */
    private void generateDarkVariant() {
        if (darkPreviewContainer == null) {
            return;
        }
        // volatile fields — safe to read without session lock
        byte[] lightOriginal = generatedImageOriginal != null ? generatedImageOriginal : generatedImage;
        if (lightOriginal == null || lightOriginal.length == 0) {
            return;
        }
        long lightSeed = generatedImageSeed; // volatile read — safe

        getUI().ifPresent(ui -> ui.access(() -> {
            // Read prompts from their respective fields while holding the session lock
            String               darkPrompt         = darkPromptField.getValue().trim();
            String               darkNegativePrompt = darkNegativePromptField.getValue().trim();
            GeneratedImageResult lightResult = new GeneratedImageResult(lightOriginal, promptField.getValue().trim(), null, lightSeed);

            darkPreviewContainer.removeAll();
            if (darkUpdateButton != null) {
                darkUpdateButton.setEnabled(false);
            }

            if (stableDiffusionService.isAvailable()) {
                // ── Loading layout ──────────────────────────────────────────
                VerticalLayout loadingLayout = new VerticalLayout();
                loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                loadingLayout.setPadding(false);
                loadingLayout.setSpacing(true);

                Icon hourglassIcon = new Icon(VaadinIcon.HOURGLASS);
                hourglassIcon.setSize("32px");
                hourglassIcon.getStyle().set("color", "var(--lumo-primary-color)");

                Div loadingText = new Div();
                loadingText.setText("Generating dark variant...");
                loadingText.getStyle().set("color", "#aaa").set("font-weight", "500");

                Div progressText = new Div();
                progressText.setText("Initializing...");
                progressText.getStyle().set("color", "#888").set("font-size", "var(--lumo-font-size-s)");

                com.vaadin.flow.component.progressbar.ProgressBar progressBar =
                        new com.vaadin.flow.component.progressbar.ProgressBar();
                progressBar.setMin(0);
                progressBar.setMax(1);
                progressBar.setValue(0);
                progressBar.setWidth("80%");

                loadingLayout.add(hourglassIcon, loadingText, progressText, progressBar);
                darkPreviewContainer.add(loadingLayout);

                // ── SD img2img in background thread ──────────────────────────
                new Thread(() -> {
                    try {
                        GeneratedImageResult result = avatarService.generateDarkAvatar(
                                darkPrompt, darkNegativePrompt, lightResult,
                                (progress, step, totalSteps) -> ui.access(() -> {
                                    progressBar.setValue(progress);
                                    progressText.setText(String.format("Step %d / %d (%.0f%%)", step, totalSteps, progress * 100));
                                    ui.push();
                                }));
                        generatedDarkImage         = result.getResizedImage();
                        generatedDarkImageOriginal = result.getOriginalImage();
                        ui.access(() -> {
                            displayInContainer(darkPreviewContainer, result.getOriginalImage());
                            if (darkUpdateButton != null) {
                                darkUpdateButton.setEnabled(true);
                            }
                            ui.push();
                        });
                    } catch (StableDiffusionException e) {
                        GeneratedImageResult fallback = avatarService.generateDefaultDarkAvatar(darkIconName);
                        generatedDarkImage         = fallback.getResizedImage();
                        generatedDarkImageOriginal = fallback.getOriginalImage();
                        ui.access(() -> {
                            displayInContainer(darkPreviewContainer, fallback.getOriginalImage());
                            if (darkUpdateButton != null) {
                                darkUpdateButton.setEnabled(true);
                            }
                            ui.push();
                        });
                    }
                }).start();
            } else {
                // Programmatic fallback — fast, run inline while holding the lock
                GeneratedImageResult fallback = avatarService.generateDefaultDarkAvatar(darkIconName);
                generatedDarkImage         = fallback.getResizedImage();
                generatedDarkImageOriginal = fallback.getOriginalImage();
                displayInContainer(darkPreviewContainer, fallback.getOriginalImage());
                if (darkUpdateButton != null) {
                    darkUpdateButton.setEnabled(true);
                }
                ui.push();
            }
        }));
    }

    private void generateLightVariant() {
        String prompt = promptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a description", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Disable button and show loading state
        generateButton.setEnabled(false);
        lightUpdateButton.setEnabled(false);
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
                    String negativePrompt = negativePromptField.getValue().trim();
                    de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult result =
                            avatarService.generateLightAvatar(prompt, negativePrompt, (progress, step, totalSteps) -> {
                                ui.access(() -> {
                                    progressBar.setValue(progress);
                                    progressText.setText(String.format("Step %d / %d (%.0f%%)", step, totalSteps, progress * 100));
                                    ui.push();
                                });
                            });

                    generatedImage         = result.getResizedImage();
                    generatedImageOriginal = result.getOriginalImage();
                    generatedImageSeed     = result.getSeed();
                    initialImage           = result.getResizedImage();

                    ui.access(() -> {
                        displayGeneratedImage(result.getResizedImage());
                        generateButton.setEnabled(true);
                        lightUpdateButton.setEnabled(true);
//                        generateButton.setText("Generate");
                        acceptButton.setEnabled(true);

                        Notification notification = Notification.show("Image generated successfully!", 3000, Notification.Position.BOTTOM_END);
                        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                        // Push UI updates
                        ui.push();
                    });
                    generateDarkVariant();
                } catch (StableDiffusionException ex) {
                    ui.access(() -> {
                        generateButton.setEnabled(true);
                        lightUpdateButton.setEnabled(true);
//                        generateButton.setText("Generate");

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

    private void updateImage() {
        String prompt = promptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a description", 3000, Notification.Position.MIDDLE);
            return;
        }
        generateButton.setEnabled(false);
        lightUpdateButton.setEnabled(false);
        acceptButton.setEnabled(false);
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
        loadingText.setText("Generating light avatar...");
        loadingText.getStyle().set("color", "var(--lumo-contrast-60pct)").set("font-weight", "500");
        Div progressText = new Div();
        progressText.setText("Initializing...");
        progressText.getStyle().set("color", "var(--lumo-contrast-50pct)").set("font-size", "var(--lumo-font-size-s)");
        com.vaadin.flow.component.progressbar.ProgressBar progressBar = new com.vaadin.flow.component.progressbar.ProgressBar();
        progressBar.setMin(0);
        progressBar.setMax(1);
        progressBar.setValue(0);
        progressBar.setWidth("80%");
        loadingLayout.add(hourglassIcon, loadingText, progressText, progressBar);
        previewContainer.add(loadingLayout);
        getUI().ifPresent(ui -> {
            new Thread(() -> {
                try {
                    String               negativePrompt = negativePromptField.getValue().trim();
                    GeneratedImageResult result         =
                            avatarService.generateLightAvatar(prompt, negativePrompt, (progress, step, totalSteps) -> {
                                ui.access(() -> {
                                    progressBar.setValue(progress);
                                    progressText.setText(String.format("Step %d / %d (%.0f%%)", step, totalSteps, progress * 100));
                                    ui.push();
                                });
                            });
                    generatedImage         = result.getResizedImage();
                    generatedImageOriginal = result.getOriginalImage();
                    generatedImageSeed     = result.getSeed();
                    initialImage           = result.getResizedImage();
                    ui.access(() -> {
                        displayGeneratedImage(result.getResizedImage());
                        generateButton.setEnabled(true);
                        lightUpdateButton.setEnabled(true);
                        acceptButton.setEnabled(true);
                        Notification notification = Notification.show("Light avatar regenerated!", 3000, Notification.Position.BOTTOM_END);
                        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        ui.push();
                    });
                } catch (StableDiffusionException ex) {
                    ui.access(() -> {
                        generateButton.setEnabled(true);
                        lightUpdateButton.setEnabled(true);
                        previewContainer.removeAll();
                        Div errorText = new Div();
                        errorText.setText("Failed to regenerate light avatar: " + ex.getMessage());
                        errorText.getStyle().set("color", "var(--lumo-error-text-color)");
                        previewContainer.add(new Icon(VaadinIcon.WARNING), errorText);
                        Notification notification = Notification.show("Regeneration failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        ui.push();
                    });
                }
            }).start();
        });
    }

    /**
     * Functional interface for the accept callback.
     * {@code darkResult} is {@code null} when the dialog was opened without dark-avatar support.
     * The negative prompts are available via {@link GeneratedImageResult#getNegativePrompt()} on each result.
     */
    @FunctionalInterface
    public interface AcceptCallback {
        /**
         * Called when the user accepts the generated image.
         *
         * @param lightResult The light-theme image result (never null); carries {@code negativePrompt}
         * @param darkResult  The dark-theme image result, or {@code null} if not generated; carries {@code negativePrompt}
         */
        void accept(GeneratedImageResult lightResult, GeneratedImageResult darkResult);
    }
}

