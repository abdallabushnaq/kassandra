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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
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

    public static final String                 DARK_THEMED_BACKGROUND_COLOR    = "#111111";
    // Button ID constants for Selenium tests
    public static final String                 ID_ACCEPT_BUTTON                = "accept-image-button";
    public static final String                 ID_CANCEL_BUTTON                = "cancel-image-button";
    public static final String                 ID_DARK_DOWNLOAD_BUTTON         = "dark-download-image-button";
    public static final String                 ID_DARK_HEADER_DOWNLOAD_BUTTON  = "dark-header-download-image-button";
    public static final String                 ID_DARK_HEADER_PROMPT_FIELD     = "dark-header-prompt-field";
    public static final String                 ID_DARK_HEADER_UPDATE_BUTTON    = "dark-update-header-button";
    public static final String                 ID_DARK_HEADER_UPLOAD_BUTTON    = "dark-header-upload-image-button";
    public static final String                 ID_DARK_NEGATIVE_PROMPT_FIELD   = "dark-negative-prompt-field";
    public static final String                 ID_DARK_PROMPT_FIELD            = "dark-prompt-field";
    public static final String                 ID_DARK_UPDATE_BUTTON           = "dark-update-image-button";
    public static final String                 ID_DARK_UPLOAD_BUTTON           = "dark-upload-image-button";
    public static final String                 ID_GENERATE_BUTTON              = "generate-image-button";
    public static final String                 ID_LIGHT_DOWNLOAD_BUTTON        = "light-download-image-button";
    public static final String                 ID_LIGHT_HEADER_DOWNLOAD_BUTTON = "light-header-download-image-button";
    public static final String                 ID_LIGHT_HEADER_PROMPT_FIELD    = "light-header-prompt-field";
    public static final String                 ID_LIGHT_HEADER_UPDATE_BUTTON   = "light-update-header-button";
    public static final String                 ID_LIGHT_HEADER_UPLOAD_BUTTON   = "light-header-upload-image-button";
    public static final String                 ID_LIGHT_NEGATIVE_PROMPT_FIELD  = "light-negative-prompt-field";
    public static final String                 ID_LIGHT_PROMPT_FIELD           = "light-prompt-field";
    public static final String                 ID_LIGHT_UPDATE_BUTTON          = "light-update-image-button";
    public static final String                 ID_LIGHT_UPLOAD_BUTTON          = "light-upload-image-button";
    /**
     * Element ID of the dialog itself, used by Selenium to locate the overlay.
     */
    public static final String                 IMAGE_PROMPT_DIALOG             = "image-prompt-dialog";
    public static final String                 LIGHT_THEMED_BACKGROUND_COLOR   = "#f5f5f5";
    private final       Button                 acceptButton;
    private final       AcceptCallback         acceptCallback;
    private final       AvatarService          avatarService;
    private final       Button                 cancelButton;
    private             Div                    darkHeaderPreviewContainer;
    private             TextArea               darkHeaderPromptField;
    private             Button                 darkHeaderUpdateButton;
    private             Upload                 darkHeaderUploadButton;
    private final       String                 darkIconName;
    private             TextArea               darkNegativePromptField;
    private             Div                    darkPreviewContainer;
    private             TextArea               darkPromptField;
    private             Button                 darkUpdateButton;
    private             Upload                 darkUploadButton;
    private final       Button                 generateButton;
    private             byte[]                 generatedDarkHeaderImage;
    private volatile    byte[]                 generatedDarkImage;
    private volatile    byte[]                 generatedDarkImageOriginal;
    private             byte[]                 generatedLightHeaderImage;
    private volatile    long                   generatedLightHeaderSeed        = -1L;
    private             byte[]                 generatedLightImage;
    private             byte[]                 generatedLightImageOriginal;
    private volatile    long                   generatedLightImageSeed         = -1L;
    private final       HeaderAcceptCallback   headerAcceptCallback;
    private             byte[]                 initialImage;
    private             Div                    lightHeaderPreviewContainer;
    private             TextArea               lightHeaderPromptField;
    private             Button                 lightHeaderUpdateButton;
    private             Upload                 lightHeaderUploadButton;
    private             TextArea               lightNegativePromptField;
    private             Div                    lightPreviewContainer;
    private             TextArea               lightPromptField;
    private             Button                 lightUpdateButton;
    private             Upload                 lightUploadButton;
    private             HorizontalLayout       previewRow;
    private final       StableDiffusionService stableDiffusionService;

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
        this(avatarService, stableDiffusionService, defaultPrompt, darkIconName, acceptCallback, null, initialImage,
                initialDarkImage, null, null, defaultDarkPrompt, defaultNegativePrompt, defaultDarkNegativePrompt, null, null);
    }

    /**
     * Full constructor including editable light and dark header previews.
     *
     * @param headerAcceptCallback     callback receiving accepted avatar and header results; may be null
     * @param initialLightHeader       existing light header image; may be null
     * @param initialDarkHeader        existing dark header image; may be null
     * @param defaultLightHeaderPrompt default light header prompt; may be null
     * @param defaultDarkHeaderPrompt  default dark header prompt; may be null
     */
    public ImagePromptDialog(AvatarService avatarService, StableDiffusionService stableDiffusionService, String defaultPrompt,
                             String darkIconName, AcceptCallback acceptCallback, HeaderAcceptCallback headerAcceptCallback, byte[] initialImage,
                             byte[] initialDarkImage, byte[] initialLightHeader, byte[] initialDarkHeader, String defaultDarkPrompt,
                             String defaultNegativePrompt, String defaultDarkNegativePrompt, String defaultLightHeaderPrompt,
                             String defaultDarkHeaderPrompt) {
        this.avatarService          = avatarService;
        this.stableDiffusionService = stableDiffusionService;
        this.acceptCallback         = acceptCallback;
        this.initialImage           = initialImage;
        this.darkIconName           = darkIconName;
        this.headerAcceptCallback   = headerAcceptCallback;

        // Resolve default values for the new prompt fields
        String resolvedNegativePrompt     = defaultNegativePrompt != null ? defaultNegativePrompt : StableDiffusionService.NEGATIVE_PROMPT;
        String resolvedDarkPrompt         = defaultDarkPrompt != null ? defaultDarkPrompt : (defaultPrompt != null ? defaultPrompt + AvatarService.DARK_PROMPT_SUFFIX : AvatarService.DARK_PROMPT_SUFFIX);
        String resolvedDarkNegativePrompt = defaultDarkNegativePrompt != null ? defaultDarkNegativePrompt : resolvedNegativePrompt;

        setId("image-prompt-dialog");
        setWidth("1000px");
        setMaxHeight("95vh");
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

        generateButton = new Button("Generate", new Icon(VaadinIcon.MAGIC));
        generateButton.setId(ID_GENERATE_BUTTON);
        generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        generateButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
        generateButton.addClickListener(e -> generateLightVariant());

        acceptButton = new Button("Accept", new Icon(VaadinIcon.CHECK));
        acceptButton.setId(ID_ACCEPT_BUTTON);
        acceptButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
        acceptButton.setEnabled(false);
        acceptButton.addClickListener(e -> acceptImage());

        if (initialImage != null && initialImage.length > 0) {
            acceptButton.setEnabled(true);
            generatedLightImage         = initialImage;
            generatedLightImageOriginal = initialImage;
        }

        cancelButton = new Button("Cancel", new Icon(VaadinIcon.CLOSE));
        cancelButton.setId(ID_CANCEL_BUTTON);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        cancelButton.addClickListener(e -> close());

        if (previewRow == null) {
            previewRow = new HorizontalLayout();
            previewRow.setId("preview-row");
            previewRow.setAlignItems(FlexComponent.Alignment.STRETCH);
            previewRow.setSpacing(true);
//            previewRow.setMargin(true);
            previewRow.setWidthFull();
        }
        createLightElements(defaultPrompt, resolvedNegativePrompt, defaultLightHeaderPrompt, initialImage, initialLightHeader);
        previewRow.add(createLightColumn());

        createDarkElements(resolvedDarkPrompt, resolvedDarkNegativePrompt, defaultDarkHeaderPrompt, initialDarkImage, initialDarkHeader);
        previewRow.add(createDarkColumn());

        dialogLayout.add(previewRow);

        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout(acceptButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        dialogLayout.add(buttonLayout);
        add(dialogLayout);
    }

    // Update methods to use these refs
    private void acceptImage() {
        if (generatedLightImage != null) {
            String prompt             = lightPromptField.getValue().trim();
            String negativePrompt     = lightNegativePromptField.getValue().trim();
            String darkPrompt         = darkPromptField.getValue().trim();
            String darkNegativePrompt = darkNegativePromptField.getValue().trim();

            GeneratedImageResult lightResult = new GeneratedImageResult(
                    generatedLightImageOriginal != null ? generatedLightImageOriginal : generatedLightImage,
                    prompt,
                    generatedLightImage
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

            if (headerAcceptCallback != null) {
                headerAcceptCallback.accept(lightResult, darkResult,
                        createHeaderResult(generatedLightHeaderImage, lightHeaderPromptField.getValue().trim()),
                        createHeaderResult(generatedDarkHeaderImage, darkHeaderPromptField.getValue().trim()));
            } else {
                acceptCallback.accept(lightResult, darkResult);
            }
            close();
        }
    }

    private HorizontalLayout createActionRow(Button generateButton) {
        HorizontalLayout actionRow = new HorizontalLayout(generateButton);
        actionRow.setId("action-row");
        actionRow.setPadding(false);
        actionRow.setSpacing(true);
        actionRow.setAlignItems(FlexComponent.Alignment.CENTER);
        actionRow.setWidthFull();
        actionRow.getStyle().set("flex-wrap", "wrap");
        return actionRow;
    }

    private VerticalLayout createDarkColumn() {
        HorizontalLayout titleRow = createTitleRow("Dark Avatar",
                darkUploadButton,
                createImageDownloadButton(ID_DARK_DOWNLOAD_BUTTON, "Download dark avatar", "dark-avatar.png",
                        () -> generatedDarkImage != null ? generatedDarkImage : generatedDarkImageOriginal),
                darkUpdateButton);

        VerticalLayout column = new VerticalLayout(titleRow, darkPreviewContainer, darkPromptField, darkNegativePromptField);
        column.setId("dark-column");
        if (headerAcceptCallback != null) {
            HorizontalLayout headerTitleRow = createTitleRow("Dark Header",
                    darkHeaderUploadButton,
                    createImageDownloadButton(ID_DARK_HEADER_DOWNLOAD_BUTTON, "Download dark header", "dark-header.png",
                            () -> generatedDarkHeaderImage),
                    darkHeaderUpdateButton);
            column.add(headerTitleRow, darkHeaderPreviewContainer, darkHeaderPromptField);
        }
        column.add(createActionRow(generateButton));
        column.setPadding(false);
        column.setSpacing(false);
        column.setMargin(true);
        column.setAlignItems(FlexComponent.Alignment.CENTER);
        column.setWidth("50%");
        column.getStyle()
                .set("background-color", "transparent")
                .set("border-radius", "var(--lumo-border-radius-l)");
        return column;
    }

    private void createDarkElements(String defaultDarkPrompt, String defaultDarkNegativePrompt, String defaultDarkHeaderPrompt,
                                    byte[] initialDarkImage, byte[] initialDarkHeader) {
        darkUploadButton     = createImageUpload(ID_DARK_UPLOAD_BUTTON, "Upload dark avatar", imageBytes -> {
            generatedDarkImage         = imageBytes;
            generatedDarkImageOriginal = imageBytes;
            displayInContainer(darkPreviewContainer, imageBytes);
            darkUpdateButton.setEnabled(true);
            Notification.show("Dark avatar uploaded.", 2000, Notification.Position.BOTTOM_END);
        });
        darkUpdateButton     = createHeaderUpdateButton(ID_DARK_UPDATE_BUTTON, "Regenerate dark avatar", this::generateDarkVariant);
        darkPreviewContainer = createPreviewContainer(DARK_THEMED_BACKGROUND_COLOR, "272px");
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
        darkPromptField            = createPromptField("Dark Avatar Prompt", ID_DARK_PROMPT_FIELD,
                "Describe the image you want to generate. Be specific about style, colors, and composition", defaultDarkPrompt);
        darkNegativePromptField    = createPromptField("Dark Avatar Negative Prompt", ID_DARK_NEGATIVE_PROMPT_FIELD,
                "Things to avoid in the dark avatar", defaultDarkNegativePrompt);
        darkHeaderUploadButton     = createImageUpload(ID_DARK_HEADER_UPLOAD_BUTTON, "Upload dark header", imageBytes -> {
            generatedDarkHeaderImage = imageBytes;
            displayHeader(darkHeaderPreviewContainer, imageBytes);
            Notification.show("Dark header uploaded.", 2000, Notification.Position.BOTTOM_END);
        });
        darkHeaderUpdateButton     = createHeaderUpdateButton(ID_DARK_HEADER_UPDATE_BUTTON, "Regenerate dark header", this::generateDarkHeader);
        darkHeaderPreviewContainer = createPreviewContainer(DARK_THEMED_BACKGROUND_COLOR, "48px");
        if (initialDarkHeader != null && initialDarkHeader.length > 0) {
            generatedDarkHeaderImage = initialDarkHeader;
            displayHeader(darkHeaderPreviewContainer, initialDarkHeader);
        } else {
            darkHeaderPreviewContainer.setText("Generated dark header will appear here");
        }
        darkHeaderPromptField = createPromptField("Dark Header Prompt", ID_DARK_HEADER_PROMPT_FIELD,
                "Describe the dark-theme header image", defaultDarkHeaderPrompt);
    }

    private GeneratedImageResult createHeaderResult(byte[] image, String prompt) {
        return image == null ? null : new GeneratedImageResult(image, prompt, image);
    }

    private Button createHeaderUpdateButton(String id, String title, Runnable action) {
        Button button = new Button(new Icon(VaadinIcon.REFRESH));
        button.setId(id);
        button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        button.getElement().setAttribute("title", title);
        button.addClickListener(event -> action.run());
        return button;
    }

    private Anchor createImageDownloadButton(String id, String title, String fileName, java.util.function.Supplier<byte[]> imageSupplier) {
        Anchor downloadAnchor = new Anchor();
        Button downloadButton = new Button(new Icon(VaadinIcon.DOWNLOAD));
        downloadButton.setId(id);
        downloadButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        downloadButton.getElement().setAttribute("title", title);
        downloadAnchor.add(downloadButton);
        downloadAnchor.getElement().setAttribute("download", true);
        downloadButton.addClickListener(e -> {
            byte[] imageToDownload = imageSupplier.get();
            if (imageToDownload != null && imageToDownload.length > 0) {
                StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(imageToDownload));
                resource.setContentType("image/png");
                resource.setCacheTime(0);
                downloadAnchor.setHref(resource);
            } else {
                Notification.show("No image to download.", 2000, Notification.Position.MIDDLE);
            }
        });
        return downloadAnchor;
    }

    private Upload createImageUpload(String id, String title, java.util.function.Consumer<byte[]> imageConsumer) {
        MemoryBuffer uploadBuffer = new MemoryBuffer();
        Upload       upload       = new Upload(uploadBuffer);
        upload.setId(id);
        upload.setAcceptedFileTypes(".png");
        upload.setMaxFiles(1);
        upload.setDropAllowed(true);
        upload.setAutoUpload(true);
        upload.getElement().setAttribute("title", title);
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
                imageConsumer.accept(imageBytes);
            } catch (Exception ex) {
                Notification.show("Failed to process image: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });
        return upload;
    }

    private VerticalLayout createLightColumn() {
        HorizontalLayout titleRow = createTitleRow("Light Avatar",
                lightUploadButton,
                createImageDownloadButton(ID_LIGHT_DOWNLOAD_BUTTON, "Download light avatar", "light-avatar.png",
                        () -> generatedLightImage != null ? generatedLightImage : initialImage),
                lightUpdateButton);

        VerticalLayout column = new VerticalLayout(titleRow, lightPreviewContainer, lightPromptField, lightNegativePromptField);
        column.setId("light-column");
        if (headerAcceptCallback != null) {
            HorizontalLayout headerTitleRow = createTitleRow("Light Header",
                    lightHeaderUploadButton,
                    createImageDownloadButton(ID_LIGHT_HEADER_DOWNLOAD_BUTTON, "Download light header", "light-header.png",
                            () -> generatedLightHeaderImage),
                    lightHeaderUpdateButton);
            column.add(headerTitleRow, lightHeaderPreviewContainer, lightHeaderPromptField);
        }
        column.add(createActionRow(generateButton));
        column.setPadding(false);
        column.setSpacing(false);
        column.setMargin(true);
        column.setAlignItems(FlexComponent.Alignment.CENTER);
        column.setWidth("50%");
        column.getStyle()
                .set("background-color", "transparent")
                .set("border-radius", "var(--lumo-border-radius-l)");
        return column;
    }

    private void createLightElements(String defaultPrompt, String defaultNegativePrompt, String defaultLightHeaderPrompt, byte[] initialLightImage, byte[] initialLightHeader) {
        lightUploadButton     = createImageUpload(ID_LIGHT_UPLOAD_BUTTON, "Upload light avatar", imageBytes -> {
            generatedLightImage         = imageBytes;
            generatedLightImageOriginal = imageBytes;
            displayGeneratedImage(lightPreviewContainer, imageBytes);
            acceptButton.setEnabled(true);
            lightUpdateButton.setEnabled(true);
            generateDarkVariant();
            if (headerAcceptCallback != null) {
                generateLightHeader();
            }
            Notification.show("Light avatar uploaded.", 2000, Notification.Position.BOTTOM_END);
        });
        lightUpdateButton     = createHeaderUpdateButton(ID_LIGHT_UPDATE_BUTTON, "Regenerate light avatar", this::updateImage);
        lightPreviewContainer = createPreviewContainer(LIGHT_THEMED_BACKGROUND_COLOR, "272px");
        if (initialLightImage != null && initialLightImage.length > 0) {
            displayGeneratedImage(lightPreviewContainer, initialLightImage);
        } else {
            Div placeholderText = new Div();
            placeholderText.setText("Generated image will appear here");
            placeholderText.getStyle().set("color", "var(--lumo-contrast-50pct)");
            lightPreviewContainer.add(placeholderText);
        }
        lightPromptField         = createPromptField("Image Description", ID_LIGHT_PROMPT_FIELD,
                "Describe the image you want to generate. Be specific about style, colors, and composition",
                defaultPrompt != null && !defaultPrompt.isEmpty() ? defaultPrompt : null);
        lightNegativePromptField = createPromptField("Negative Prompt", ID_LIGHT_NEGATIVE_PROMPT_FIELD,
                "Things to avoid in the generated image", defaultNegativePrompt);

        lightHeaderUploadButton     = createImageUpload(ID_LIGHT_HEADER_UPLOAD_BUTTON, "Upload light header", imageBytes -> {
            generatedLightHeaderImage = imageBytes;
            displayHeader(lightHeaderPreviewContainer, imageBytes);
            Notification.show("Light header uploaded.", 2000, Notification.Position.BOTTOM_END);
        });
        lightHeaderUpdateButton     = createHeaderUpdateButton(ID_LIGHT_HEADER_UPDATE_BUTTON, "Regenerate light header", this::generateLightHeader);
        lightHeaderPreviewContainer = createPreviewContainer(LIGHT_THEMED_BACKGROUND_COLOR, "48px");
        if (initialLightHeader != null && initialLightHeader.length > 0) {
            generatedLightHeaderImage = initialLightHeader;
            displayHeader(lightHeaderPreviewContainer, initialLightHeader);
        } else {
            lightHeaderPreviewContainer.setText("Generated light header will appear here");
        }
        lightHeaderPromptField = createPromptField("Light Header Prompt", ID_LIGHT_HEADER_PROMPT_FIELD,
                "Describe the light-theme header image", defaultLightHeaderPrompt);
    }

    private Div createPreviewContainer(String backgroundColor, String height) {
        Div container = new Div();
        container.getStyle().set("border", "1px dashed var(--lumo-contrast-30pct)")
                .set("border-radius", "var(--lumo-border-radius)")
                .set("width", "100%")
                .set("height", height)
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background-color", backgroundColor)
                .set("overflow", "hidden");
        return container;
    }

    private TextArea createPromptField(String label, String id, String placeholderText, String value) {
        TextArea field = new TextArea(label);
        field.setId(id);
        field.setWidth("100%");
        field.setMinHeight("120px");
        field.setClearButtonVisible(true);
        field.setPlaceholder(placeholderText);
        if (value != null) {
            field.setValue(value);
        }
        field.getStyle().set("background-color", "transparent");
        field.getElement().getStyle().set("--lumo-text-field-background-color", "transparent");
        return field;
    }

    private HorizontalLayout createTitleRow(String titleText, Component... actions) {
        Span             label    = new Span(titleText);
        HorizontalLayout titleRow = new HorizontalLayout(label);
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);

        HorizontalLayout actionGroup = new HorizontalLayout();
        actionGroup.setPadding(false);
        actionGroup.setSpacing(false);
        actionGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        actionGroup.getStyle().set("margin-left", "auto");

        for (int i = 0; i < actions.length; i++) {
            Component action = actions[i];
            if (action == null) {
                continue;
            }
            if (i == 0) {
                action.getElement().getStyle().set("margin-right", "var(--lumo-space-s)");
            } else if (i == 1) {
                action.getElement().getStyle().set("margin-right", "var(--lumo-space-xs)");
            }
            actionGroup.add(action);
        }

        titleRow.add(actionGroup);
        return titleRow;
    }

    private void displayGeneratedImage(Div container, byte[] imageBytes) {
        container.removeAll();

        final byte[] imageBytesForResource = imageBytes;
        String       resourceName          = "generated-image-" + System.currentTimeMillis() + ".png";

        StreamResource resource = new StreamResource(resourceName,
                () -> new ByteArrayInputStream(imageBytesForResource));

        resource.setContentType("image/png");
        resource.setCacheTime(0);

        Image image = new Image(resource, "Generated image");
        image.setWidth("256px");
        image.setHeight("256px");
        image.getStyle()
                .set("border-radius", "var(--lumo-border-radius)")
                .set("object-fit", "contain")
                .set("display", "block");

        container.add(image);
    }

    private void displayHeader(Div container, byte[] imageBytes) {
        container.removeAll();
        Image image = new Image(new StreamResource("header-" + System.currentTimeMillis() + ".png",
                () -> new ByteArrayInputStream(imageBytes)), "Header preview");
        image.setWidthFull();
        image.setHeightFull();
        image.getStyle().set("object-fit", "contain");
        container.add(image);
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

    private void generateDarkHeader() {
        String prompt = darkHeaderPromptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a dark header description", 3000, Notification.Position.MIDDLE);
            return;
        }
        generateHeader(darkHeaderPreviewContainer, darkHeaderUpdateButton, "Generating dark header...", prompt,
                generatedLightHeaderSeed, true);
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
        byte[] lightOriginal = generatedLightImageOriginal != null ? generatedLightImageOriginal : generatedLightImage;
        if (lightOriginal == null || lightOriginal.length == 0) {
            return;
        }
        long lightSeed = generatedLightImageSeed; // volatile read — safe

        getUI().ifPresent(ui -> ui.access(() -> {
            // Read prompts from their respective fields while holding the session lock
            String               darkPrompt         = darkPromptField.getValue().trim();
            String               darkNegativePrompt = darkNegativePromptField.getValue().trim();
            GeneratedImageResult lightResult        = new GeneratedImageResult(lightOriginal, lightPromptField.getValue().trim(), null, lightSeed);

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

    private void generateHeader(Div container, Button updateButton, String label, String prompt, long seed, boolean dark) {
        updateButton.setEnabled(false);
        acceptButton.setEnabled(false);
        container.removeAll();
        Div loadingText = new Div(label);
        loadingText.getStyle().set("color", "var(--lumo-contrast-60pct)").set("font-weight", "500").set("line-height", "1");
        Div progressText = new Div("Initializing...");
        progressText.getStyle().set("color", "var(--lumo-contrast-50pct)").set("font-size", "var(--lumo-font-size-s)").set("line-height", "1");
        com.vaadin.flow.component.progressbar.ProgressBar progressBar = new com.vaadin.flow.component.progressbar.ProgressBar();
        progressBar.setMin(0);
        progressBar.setMax(1);
        progressBar.setValue(0);
        progressBar.setWidth("80%");
        VerticalLayout loadingLayout = new VerticalLayout(loadingText, progressText, progressBar);
        loadingLayout.setPadding(false);
        loadingLayout.setSpacing(false);
        loadingLayout.getStyle().set("gap", "0");
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loadingLayout.setWidthFull();
        container.add(loadingLayout);
        getUI().ifPresent(ui -> new Thread(() -> {
            try {
                GeneratedImageResult result = dark && generatedLightHeaderImage != null
                        ? avatarService.generateDarkHeaderWithFallback(prompt, lightNegativePromptField.getValue().trim(),
                        new GeneratedImageResult(generatedLightHeaderImage, "", null, seed), darkIconName,
                        (value, step, total) -> ui.access(() -> {
                            progressBar.setValue(value);
                            progressText.setText(String.format("Step %d / %d (%.0f%%)", step, total, value * 100));
                            ui.push();
                        }))
                        : avatarService.generateLightHeaderWithFallback(prompt, lightNegativePromptField.getValue().trim(), darkIconName,
                        (value, step, total) -> ui.access(() -> {
                            progressBar.setValue(value);
                            progressText.setText(String.format("Step %d / %d (%.0f%%)", step, total, value * 100));
                            ui.push();
                        }));
                if (dark) {
                    generatedDarkHeaderImage = result.getResizedImage();
                } else {
                    generatedLightHeaderImage = result.getResizedImage();
                    generatedLightHeaderSeed  = result.getSeed();
                }
                ui.access(() -> {
                    displayHeader(container, result.getResizedImage());
                    updateButton.setEnabled(true);
                    acceptButton.setEnabled(generatedLightImage != null);
                    Notification notification = Notification.show(dark ? "Dark header generated!" : "Light header generated!",
                            3000, Notification.Position.BOTTOM_END);
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    ui.push();
                });
            } catch (RuntimeException exception) {
                ui.access(() -> {
                    progressText.setText("Failed to generate header: " + exception.getMessage());
                    updateButton.setEnabled(true);
                    acceptButton.setEnabled(generatedLightImage != null);
                    ui.push();
                });
            }
        }).start());
    }

    private void generateLightHeader() {
        String prompt = lightHeaderPromptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a light header description", 3000, Notification.Position.MIDDLE);
            return;
        }
        generateHeader(lightHeaderPreviewContainer, lightHeaderUpdateButton, "Generating light header...", prompt, -1L, false);
    }

    private void generateLightVariant() {
        String prompt = lightPromptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a description", 3000, Notification.Position.MIDDLE);
            return;
        }

        generateButton.setEnabled(false);
        lightUpdateButton.setEnabled(false);
        acceptButton.setEnabled(false);
        lightPreviewContainer.removeAll();

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
        lightPreviewContainer.add(loadingLayout);

        getUI().ifPresent(ui -> {
            new Thread(() -> {
                try {
                    String negativePrompt = lightNegativePromptField.getValue().trim();
                    de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult result =
                            avatarService.generateLightAvatar(prompt, negativePrompt, (progress, step, totalSteps) -> {
                                ui.access(() -> {
                                    progressBar.setValue(progress);
                                    progressText.setText(String.format("Step %d / %d (%.0f%%)", step, totalSteps, progress * 100));
                                    ui.push();
                                });
                            });

                    generatedLightImage         = result.getResizedImage();
                    generatedLightImageOriginal = result.getOriginalImage();
                    generatedLightImageSeed     = result.getSeed();
                    initialImage                = result.getResizedImage();

                    ui.access(() -> {
                        displayGeneratedImage(lightPreviewContainer, result.getResizedImage());
                        generateButton.setEnabled(true);
                        lightUpdateButton.setEnabled(true);
                        acceptButton.setEnabled(true);

                        Notification notification = Notification.show("Image generated successfully!", 3000, Notification.Position.BOTTOM_END);
                        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        ui.push();
                    });
                    generateDarkVariant();
                    if (headerAcceptCallback != null) {
                        generateLightHeader();
                    }
                } catch (StableDiffusionException ex) {
                    ui.access(() -> {
                        generateButton.setEnabled(true);
                        lightUpdateButton.setEnabled(true);

                        lightPreviewContainer.removeAll();
                        Div errorText = new Div();
                        errorText.setText("Failed to generate image: " + ex.getMessage());
                        errorText.getStyle().set("color", "var(--lumo-error-text-color)");
                        lightPreviewContainer.add(new Icon(VaadinIcon.WARNING), errorText);

                        Notification notification = Notification.show("Generation failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        ui.push();
                    });
                }
            }).start();
        });
    }

    private void updateImage() {
        String prompt = lightPromptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a description", 3000, Notification.Position.MIDDLE);
            return;
        }
        generateButton.setEnabled(false);
        lightUpdateButton.setEnabled(false);
        acceptButton.setEnabled(false);
        lightPreviewContainer.removeAll();
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
        lightPreviewContainer.add(loadingLayout);
        getUI().ifPresent(ui -> {
            new Thread(() -> {
                try {
                    String negativePrompt = lightNegativePromptField.getValue().trim();
                    GeneratedImageResult result =
                            avatarService.generateLightAvatar(prompt, negativePrompt, (progress, step, totalSteps) -> {
                                ui.access(() -> {
                                    progressBar.setValue(progress);
                                    progressText.setText(String.format("Step %d / %d (%.0f%%)", step, totalSteps, progress * 100));
                                    ui.push();
                                });
                            });
                    generatedLightImage         = result.getResizedImage();
                    generatedLightImageOriginal = result.getOriginalImage();
                    generatedLightImageSeed     = result.getSeed();
                    initialImage                = result.getResizedImage();
                    ui.access(() -> {
                        displayGeneratedImage(lightPreviewContainer, result.getResizedImage());
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
                        lightPreviewContainer.removeAll();
                        Div errorText = new Div();
                        errorText.setText("Failed to regenerate light avatar: " + ex.getMessage());
                        errorText.getStyle().set("color", "var(--lumo-error-text-color)");
                        lightPreviewContainer.add(new Icon(VaadinIcon.WARNING), errorText);
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

    /**
     * Callback invoked when the dialog accepts avatar and header variants.
     */
    @FunctionalInterface
    public interface HeaderAcceptCallback {
        /**
         * Accept all generated image variants.
         *
         * @param lightAvatar light avatar result
         * @param darkAvatar  dark avatar result
         * @param lightHeader light header result
         * @param darkHeader  dark header result
         */
        void accept(GeneratedImageResult lightAvatar, GeneratedImageResult darkAvatar,
                    GeneratedImageResult lightHeader, GeneratedImageResult darkHeader);
    }
}
