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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.TiledDiffusionOptions;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.CozyStylePrompt;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.ui.component.HelpDrawer;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A reusable dialog for generating AI images using prompts.
 */
@Slf4j
public class ImagePromptDialog extends Dialog {
    /** Stable ID of the dialog help button. */
    public static final String HELP_BUTTON_ID = "image-prompt-dialog-help-button";

    public static final String                        DARK_THEMED_BACKGROUND_COLOR    = "#111111";
    // Button ID constants for Selenium tests
    public static final String                        ID_ACCEPT_BUTTON                = "accept-image-button";
    /**
     * Element ID of the shared avatar description editor.
     */
    public static final String                        ID_AVATAR_PROMPT_FIELD          = "avatar-prompt-field";
    public static final String                        ID_CANCEL_BUTTON                = "cancel-image-button";
    public static final String                        ID_DARK_DOWNLOAD_BUTTON         = "dark-download-image-button";
    public static final String                        ID_DARK_HEADER_DOWNLOAD_BUTTON  = "dark-header-download-image-button";
    public static final String                        ID_DARK_HEADER_UPDATE_BUTTON    = "dark-update-header-button";
    public static final String                        ID_DARK_HEADER_UPLOAD_BUTTON    = "dark-header-upload-image-button";
    public static final String                        ID_DARK_UPDATE_BUTTON           = "dark-update-image-button";
    public static final String                        ID_DARK_UPLOAD_BUTTON           = "dark-upload-image-button";
    public static final String                        ID_GENERATE_BUTTON              = "generate-image-button";
    /**
     * Element ID of the shared header description editor.
     */
    public static final String                        ID_HEADER_PROMPT_FIELD          = "header-prompt-field";
    public static final String                        ID_LIGHT_DOWNLOAD_BUTTON        = "light-download-image-button";
    public static final String                        ID_LIGHT_HEADER_DOWNLOAD_BUTTON = "light-header-download-image-button";
    public static final String                        ID_LIGHT_HEADER_UPDATE_BUTTON   = "light-update-header-button";
    public static final String                        ID_LIGHT_HEADER_UPLOAD_BUTTON   = "light-header-upload-image-button";
    public static final String                        ID_LIGHT_UPDATE_BUTTON          = "light-update-image-button";
    public static final String                        ID_LIGHT_UPLOAD_BUTTON          = "light-upload-image-button";
    /**
     * Element ID of the shared avatar exclusions editor.
     */
    public static final String                        ID_NEGATIVE_PROMPT_FIELD        = "negative-prompt-field";
    /**
     * Element ID of the button that randomly selects a concrete Cozy style.
     */
    public static final String                        ID_RANDOM_STYLE_BUTTON          = "random-image-style-button";
    /**
     * Element ID of the shared Cozy style selector.
     */
    public static final String                        ID_STYLE_SELECT                 = "image-style-select";
    /**
     * Element ID of the dialog itself, used by Selenium to locate the overlay.
     */
    public static final String                        IMAGE_PROMPT_DIALOG             = "image-prompt-dialog";
    public static final String                        LIGHT_THEMED_BACKGROUND_COLOR   = "#f5f5f5";
    private final       Button                        acceptButton;
    private final       Consumer<AvatarUpdateRequest> acceptCallback;
    private             TextArea                      avatarPromptField;
    private final       AvatarService                 avatarService;
    private final       Button                        cancelButton;
    private             Div                           darkHeaderPreviewContainer;
    private             Button                        darkHeaderUpdateButton;
    private             Upload                        darkHeaderUploadButton;
    private             Div                           darkPreviewContainer;
    private             Button                        darkUpdateButton;
    private             Upload                        darkUploadButton;
    private final       Button                        generateButton;
    private             byte[]                        generatedDarkHeaderImage;
    private volatile    byte[]                        generatedDarkImage;
    private volatile    byte[]                        generatedDarkImageOriginal;
    private             byte[]                        generatedLightHeaderImage;
    private volatile    long                          generatedLightHeaderSeed        = -1L;
    private             byte[]                        generatedLightImage;
    private             byte[]                        generatedLightImageOriginal;
    private volatile    long                          generatedLightImageSeed         = -1L;
    private             TextArea                      headerPromptField;
    private final       boolean                       headersEnabled;
    private final       String                        iconName;// used to generate default images in case stable diffusion service is unavailable
    private             byte[]                        initialImage;
    private             Div                           lightHeaderPreviewContainer;
    private             Button                        lightHeaderUpdateButton;
    private             Upload                        lightHeaderUploadButton;
    private             Div                           lightPreviewContainer;
    private             Button                        lightUpdateButton;
    private             Upload                        lightUploadButton;
    private             TextArea                      negativePromptField;
    private             int                           pendingGenerations;
    private             HorizontalLayout              previewRow;
    private final       Button                        randomStyleButton               = new Button("\u2684");
    private final       Select<CozyStylePrompt>       styleSelect                     = new Select<>();
    private             TiledDiffusionOptions         tiledDeffusionOptions;

    /**
     * Creates shared generation editors with separate light and dark image controls.
     *
     * @param avatarService          the avatar and header generation service
     * @param stableDiffusionService the image service
     * @param initialValues          stored images and theme-independent inputs; never modified
     * @param iconName               placeholder icon used when generation is unavailable
     * @param headersEnabled         whether header editors and previews are shown
     * @param acceptCallback         receives a new request with accepted images and editable inputs
     */
    public ImagePromptDialog(AvatarService avatarService, StableDiffusionService stableDiffusionService,
                             AvatarUpdateRequest initialValues, String iconName, boolean headersEnabled,
                             Consumer<AvatarUpdateRequest> acceptCallback) {
        this.avatarService  = avatarService;
        this.acceptCallback = acceptCallback;
        this.initialImage   = initialValues.getLightAvatarImageOriginal();
        this.iconName       = iconName;
        this.headersEnabled = headersEnabled;
        byte[] initialLightPreview = initialValues.getLightAvatarImage() != null
                ? initialValues.getLightAvatarImage() : initialImage;
        byte[] initialDarkPreview = initialValues.getDarkAvatarImage() != null
                ? initialValues.getDarkAvatarImage() : initialValues.getDarkAvatarImageOriginal();

        setId("image-prompt-dialog");
        setWidth("1000px");
        setMaxHeight("95vh");
        getHeader().add(VaadinUtil.createDialogHeader("Generate AI Image", VaadinIcon.MAGIC));
        HelpDrawer helpDrawer = new HelpDrawer("Generate AI Image", "help/image-prompt-dialog.md", true);
        getHeader().add(helpDrawer.createToggleButton(HELP_BUTTON_ID));
        add(helpDrawer);
        addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                helpDrawer.close();
            }
        });

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        styleSelect.setLabel("Header style");
        styleSelect.setId(ID_STYLE_SELECT);
        styleSelect.setItems(CozyStylePrompt.values());
        styleSelect.setEmptySelectionAllowed(false);
        styleSelect.setItemLabelGenerator(style -> {
            if (style == null) {
                return "";
            }
            String[] words = style.name().toLowerCase(Locale.ROOT).split("_");
            for (int i = 0; i < words.length; i++) {
                words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
            }
            return String.join(" ", words);
        });
        styleSelect.setValue(initialValues.getStyle() != null ? initialValues.getStyle() : CozyStylePrompt.randomStyle());
        randomStyleButton.setId(ID_RANDOM_STYLE_BUTTON);
        randomStyleButton.setTooltipText("Select a random style");
        randomStyleButton.getElement().setAttribute("aria-label", "Select a random style");
        randomStyleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        randomStyleButton.getStyle().set("font-size", "24px");
        randomStyleButton.addClickListener(event -> styleSelect.setValue(CozyStylePrompt.randomStyle()));
        HorizontalLayout styleRow = new HorizontalLayout(styleSelect, randomStyleButton);
        styleRow.setAlignItems(FlexComponent.Alignment.END);

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
        generateButton.addClickListener(e -> generateLightAvatar());

        acceptButton = new Button("Accept", new Icon(VaadinIcon.CHECK));
        acceptButton.setId(ID_ACCEPT_BUTTON);
        acceptButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
        acceptButton.setEnabled(true);
        acceptButton.addClickListener(e -> acceptImage());

        if (initialLightPreview != null && initialLightPreview.length > 0) {
            acceptButton.setEnabled(true);
            generatedLightImage         = initialLightPreview;
            generatedLightImageOriginal = initialImage != null ? initialImage : initialLightPreview;
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
        createLightElements(initialValues.getAvatarPrompt(), initialValues.getNegativePrompt(), initialValues.getHeaderPrompt(),
                initialLightPreview, initialValues.getLightHeaderImage());
        previewRow.add(createLightColumn());

        createDarkElements(initialDarkPreview, initialValues.getDarkHeaderImage());
        if (initialValues.getDarkAvatarImageOriginal() != null) {
            generatedDarkImageOriginal = initialValues.getDarkAvatarImageOriginal();
        }
        previewRow.add(createDarkColumn());

        dialogLayout.add(avatarPromptField, negativePromptField, previewRow);
        if (headersEnabled) {
            HorizontalLayout headerRow = new HorizontalLayout(createHeaderColumn(false), createHeaderColumn(true));
            headerRow.setWidthFull();
            headerRow.setAlignItems(FlexComponent.Alignment.STRETCH);
            dialogLayout.add(headerPromptField, styleRow, headerRow);
        }
        dialogLayout.add(createActionRow(generateButton));

        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout(acceptButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        dialogLayout.add(buttonLayout);
        add(dialogLayout);
    }

    private void acceptImage() {
        AvatarUpdateRequest request = new AvatarUpdateRequest();
        request.setAvatarPrompt(avatarPromptField.getValue().trim());
        request.setNegativePrompt(negativePromptField.getValue().trim());
        request.setLightAvatarImage(generatedLightImage);
        request.setLightAvatarImageOriginal(generatedLightImageOriginal);
        request.setDarkAvatarImage(generatedDarkImage);
        request.setDarkAvatarImageOriginal(generatedDarkImageOriginal);
        if (headersEnabled) {
            request.setHeaderPrompt(headerPromptField.getValue().trim());
            request.setStyle(styleSelect.getValue());
            request.setLightHeaderImage(generatedLightHeaderImage);
            request.setDarkHeaderImage(generatedDarkHeaderImage);
        }
        acceptCallback.accept(request);
        close();
    }

    private void beginGeneration() {
        pendingGenerations++;
        updateGenerationControls();
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

        VerticalLayout column = new VerticalLayout(titleRow, darkPreviewContainer);
        column.setId("dark-column");
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

    private void createDarkElements(byte[] initialDarkImage, byte[] initialDarkHeader) {
        darkUploadButton     = createImageUpload(ID_DARK_UPLOAD_BUTTON, "Upload dark avatar", imageBytes -> {
            generatedDarkImage         = imageBytes;
            generatedDarkImageOriginal = imageBytes;
            displayInContainer(darkPreviewContainer, imageBytes);
            darkUpdateButton.setEnabled(true);
            Notification.show("Dark avatar uploaded.", 2000, Notification.Position.BOTTOM_END);
        });
        darkUpdateButton     = createUpdateButton(ID_DARK_UPDATE_BUTTON, "Regenerate dark avatar", this::generateDarkAvatar);
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
        darkHeaderUploadButton     = createImageUpload(ID_DARK_HEADER_UPLOAD_BUTTON, "Upload dark header", imageBytes -> {
            generatedDarkHeaderImage = imageBytes;
            displayHeader(darkHeaderPreviewContainer, imageBytes);
            Notification.show("Dark header uploaded.", 2000, Notification.Position.BOTTOM_END);
        });
        darkHeaderUpdateButton     = createUpdateButton(ID_DARK_HEADER_UPDATE_BUTTON, "Regenerate dark header", this::generateDarkHeader);
        darkHeaderPreviewContainer = createPreviewContainer(DARK_THEMED_BACKGROUND_COLOR, "48px");
        if (initialDarkHeader != null && initialDarkHeader.length > 0) {
            generatedDarkHeaderImage = initialDarkHeader;
            displayHeader(darkHeaderPreviewContainer, initialDarkHeader);
        } else {
            darkHeaderPreviewContainer.setText("Generated dark header will appear here");
        }
    }

    private VerticalLayout createHeaderColumn(boolean dark) {
        HorizontalLayout titleRow = createTitleRow(dark ? "Dark Header" : "Light Header",
                dark ? darkHeaderUploadButton : lightHeaderUploadButton,
                createImageDownloadButton(dark ? ID_DARK_HEADER_DOWNLOAD_BUTTON : ID_LIGHT_HEADER_DOWNLOAD_BUTTON,
                        dark ? "Download dark header" : "Download light header",
                        dark ? "dark-header.png" : "light-header.png",
                        () -> dark ? generatedDarkHeaderImage : generatedLightHeaderImage),
                dark ? darkHeaderUpdateButton : lightHeaderUpdateButton);
        VerticalLayout column = new VerticalLayout(titleRow,
                dark ? darkHeaderPreviewContainer : lightHeaderPreviewContainer);
        column.setId(dark ? "dark-header-column" : "light-header-column");
        column.setPadding(false);
        column.setSpacing(false);
        column.setMargin(true);
        column.setWidth("50%");
        return column;
    }

    private Anchor createImageDownloadButton(String id, String title, String fileName, java.util.function.Supplier<byte[]> imageSupplier) {
        Anchor downloadAnchor = new Anchor();
        Button downloadButton = new Button(new Icon(VaadinIcon.DOWNLOAD));
        downloadButton.setId(id);
        downloadButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        downloadButton.setTooltipText(title);
        downloadAnchor.add(downloadButton);
        downloadAnchor.getElement().setAttribute("download", fileName);
        downloadButton.addClickListener(e -> {
            byte[] imageToDownload = imageSupplier.get();
            if (imageToDownload == null || imageToDownload.length == 0) {
                Notification.show("No image to download.", 2000, Notification.Position.MIDDLE);
                return;
            }
            DownloadHandler downloadHandler = DownloadHandler.fromInputStream(event -> new DownloadResponse(
                    new ByteArrayInputStream(imageToDownload),
                    fileName,
                    "image/png",
                    imageToDownload.length));
            downloadAnchor.setHref(downloadHandler);
            downloadAnchor.getElement().setAttribute("download", fileName);
            downloadAnchor.getElement().executeJs("this.click();");
        });
        return downloadAnchor;
    }

    private Upload createImageUpload(String id, String title, java.util.function.Consumer<byte[]> imageConsumer) {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, uploadedBytes) -> {
            String fileName      = metadata.fileName();
            String mimeType      = metadata.contentType();
            long   contentLength = metadata.contentLength();
            log.info("Uploading image: fileName={}, mimeType={}, contentLength={}", fileName, mimeType, contentLength);
            try {
                if (uploadedBytes == null || uploadedBytes.length == 0) {
                    Notification.show("Invalid PNG file.", 3000, Notification.Position.MIDDLE);
                    return;
                }
                BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(uploadedBytes));
                if (inputImage == null) {
                    Notification.show("Invalid PNG file.", 3000, Notification.Position.MIDDLE);
                    return;
                }
                BufferedImage resized = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                Graphics2D    g2d     = resized.createGraphics();
                try {
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2d.drawImage(inputImage, 0, 0, 256, 256, null);
                } finally {
                    g2d.dispose();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resized, "png", baos);
                baos.flush();
                byte[] imageBytes = baos.toByteArray();
                baos.close();
                imageConsumer.accept(imageBytes);
            } catch (Exception ex) {
                Notification.show("Failed to process image: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        }));
        upload.setId(id);
        upload.setAcceptedFileTypes(".png");
        upload.setMaxFiles(1);
        upload.setDropAllowed(true);
        upload.setAutoUpload(true);
        upload.getElement().setAttribute("title", title);
        return upload;
    }

    private VerticalLayout createLightColumn() {
        HorizontalLayout titleRow = createTitleRow("Light Avatar",
                lightUploadButton,
                createImageDownloadButton(ID_LIGHT_DOWNLOAD_BUTTON, "Download light avatar", "light-avatar.png",
                        () -> generatedLightImage != null ? generatedLightImage : initialImage),
                lightUpdateButton);

        VerticalLayout column = new VerticalLayout(titleRow, lightPreviewContainer);
        column.setId("light-column");
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
            generatedLightImageSeed     = -1L;
            displayGeneratedImage(lightPreviewContainer, imageBytes);
            acceptButton.setEnabled(true);
            lightUpdateButton.setEnabled(true);
            Notification.show("Light avatar uploaded.", 2000, Notification.Position.BOTTOM_END);
        });
        lightUpdateButton     = createUpdateButton(ID_LIGHT_UPDATE_BUTTON, "Regenerate light avatar", this::generateLightAvatar);
        lightPreviewContainer = createPreviewContainer(LIGHT_THEMED_BACKGROUND_COLOR, "272px");
        if (initialLightImage != null && initialLightImage.length > 0) {
            displayGeneratedImage(lightPreviewContainer, initialLightImage);
        } else {
            Div placeholderText = new Div();
            placeholderText.setText("Generated image will appear here");
            placeholderText.getStyle().set("color", "var(--lumo-contrast-50pct)");
            lightPreviewContainer.add(placeholderText);
        }
        avatarPromptField   = createPromptField("Avatar Prompt", ID_AVATAR_PROMPT_FIELD,
                "Describe the image you want to generate. Be specific about style, colors, and composition",
                defaultPrompt != null && !defaultPrompt.isEmpty() ? defaultPrompt : null);
        negativePromptField = createPromptField("Avatar Negative Prompt", ID_NEGATIVE_PROMPT_FIELD,
                "Things to avoid in the generated image", defaultNegativePrompt);

        lightHeaderUploadButton     = createImageUpload(ID_LIGHT_HEADER_UPLOAD_BUTTON, "Upload light header", imageBytes -> {
            generatedLightHeaderImage = imageBytes;
            generatedLightHeaderSeed  = -1L;
            tiledDeffusionOptions     = null;
            displayHeader(lightHeaderPreviewContainer, imageBytes);
            Notification.show("Light header uploaded.", 2000, Notification.Position.BOTTOM_END);
        });
        lightHeaderUpdateButton     = createUpdateButton(ID_LIGHT_HEADER_UPDATE_BUTTON, "Regenerate light header", this::generateLightHeader);
        lightHeaderPreviewContainer = createPreviewContainer(LIGHT_THEMED_BACKGROUND_COLOR, "48px");
        if (initialLightHeader != null && initialLightHeader.length > 0) {
            generatedLightHeaderImage = initialLightHeader;
            displayHeader(lightHeaderPreviewContainer, initialLightHeader);
        } else {
            lightHeaderPreviewContainer.setText("Generated light header will appear here");
        }
        headerPromptField = createPromptField("Header Prompt", ID_HEADER_PROMPT_FIELD,
                "Describe the header image without theme or style instructions", defaultLightHeaderPrompt);
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
        field.setMinHeight("60px");
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

    private Button createUpdateButton(String id, String title, Runnable action) {
        Button button = new Button(new Icon(VaadinIcon.REFRESH));
        button.setId(id);
        button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        button.setTooltipText(title);
        button.addClickListener(event -> action.run());
        return button;
    }

    private void displayGeneratedImage(Div container, byte[] imageBytes) {
        container.removeAll();

        String dataUri = VaadinUtil.dataUriFromBytes(imageBytes);
        if (dataUri == null) {
            container.add(new Div("Image unavailable"));
            return;
        }

        Image image = new Image(dataUri, "Generated image");
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
        String dataUri = VaadinUtil.dataUriFromBytes(imageBytes);
        if (dataUri == null) {
            container.add(new Div("Header unavailable"));
            return;
        }
        Image image = new Image(dataUri, "Header preview");
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
        String dataUri = VaadinUtil.dataUriFromBytes(imageBytes);
        if (dataUri == null) {
            container.add(new Div("Preview unavailable"));
            return;
        }
        Image img = new Image(dataUri, "Preview");
        img.setWidth("256px");
        img.setHeight("256px");
        img.getStyle().set("object-fit", "contain").set("display", "block");
        container.add(img);
    }

    private void finishGeneration() {
        pendingGenerations--;
        updateGenerationControls();
    }

    private void generateDarkAvatar() {
        generateDarkAvatar(avatarPromptField.getValue().trim(), negativePromptField.getValue().trim());
    }

    private void generateDarkAvatar(String darkPrompt, String darkNegativePrompt) {
        if (darkPrompt.isBlank()) {
            Notification.show("Please enter a dark avatar description", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (darkPreviewContainer == null) {
            return;
        }
        // volatile fields — safe to read without session lock
        byte[] lightOriginal = generatedLightImageOriginal != null ? generatedLightImageOriginal : generatedLightImage;
        if (lightOriginal == null || lightOriginal.length == 0) {
            Notification.show("Generate or upload a light avatar first", 3000, Notification.Position.MIDDLE);
            return;
        }
        long lightSeed = generatedLightImageSeed; // volatile read — safe

        getUI().ifPresent(ui -> {
            GeneratedImageResult lightResult = new GeneratedImageResult(lightOriginal, avatarPromptField.getValue().trim(), null, lightSeed);

            beginGeneration();
            darkPreviewContainer.removeAll();
            if (darkUpdateButton != null) {
                darkUpdateButton.setEnabled(false);
            }

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
                    GeneratedImageResult result = avatarService.generateDarkAvatarWithFallback(
                            darkPrompt, darkNegativePrompt, lightResult, iconName,
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
                        finishGeneration();
                        ui.push();
                    });
                } catch (RuntimeException e) {
                    ui.access(() -> {
                        generateButton.setEnabled(true);
                        darkUpdateButton.setEnabled(true);

                        lightPreviewContainer.removeAll();
                        Div errorText = new Div();
                        errorText.setText("Failed to generate image: " + e.getMessage());
                        errorText.getStyle().set("color", "var(--lumo-error-text-color)");
                        darkPreviewContainer.add(new Icon(VaadinIcon.WARNING), errorText);

                        Notification notification = Notification.show("Generation failed: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        finishGeneration();
                        ui.push();
                    });
                }
            }).start();
        });
    }

    private void generateDarkHeader() {
        String prompt = headerPromptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a dark header description", 3000, Notification.Position.MIDDLE);
            return;
        }
        generateHeader(darkHeaderPreviewContainer, darkHeaderUpdateButton, "Generating dark header...", prompt,
                generatedLightHeaderSeed, true, styleSelect.getValue(), null);
    }

    private void generateHeader(Div container, Button updateButton, String label, String prompt, long seed, boolean dark,
                                CozyStylePrompt style, String matchingDarkPrompt) {
        beginGeneration();
        updateButton.setEnabled(false);
        acceptButton.setEnabled(false);
        getUI().ifPresent(ui -> ui.getPage().executeJs("""
                const header = document.querySelector('[data-page-header]');
                if (!header) throw new Error('Page header is unavailable');
                const bounds = header.getBoundingClientRect();
                return bounds.width + 'x' + bounds.height;
                """).then(String.class, dimensions -> {
            try {
                String[]              size    = dimensions.split("x");
                TiledDiffusionOptions options = dark && tiledDeffusionOptions != null ? tiledDeffusionOptions : TiledDiffusionOptions.forHeader(Double.parseDouble(size[0]), Double.parseDouble(size[1]));
                generateHeader(container, updateButton, label, prompt, seed, dark, options, style, matchingDarkPrompt);
            } catch (IllegalArgumentException exception) {
                updateButton.setEnabled(true);
                acceptButton.setEnabled(generatedLightImage != null);
                Notification.show("Cannot size header: " + exception.getMessage(), 5000, Notification.Position.MIDDLE);
                finishGeneration();
            }
        }, error -> {
            updateButton.setEnabled(true);
            acceptButton.setEnabled(generatedLightImage != null);
            Notification.show("Cannot measure page header: " + error, 5000, Notification.Position.MIDDLE);
            finishGeneration();
        }));
    }

    private void generateHeader(Div container, Button updateButton, String label, String prompt, long seed, boolean dark,
                                TiledDiffusionOptions options, CozyStylePrompt style, String matchingDarkPrompt) {
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
                final GeneratedImageResult result;
                if (dark) {
                    GeneratedImageResult lightHeader = new GeneratedImageResult(generatedLightHeaderImage, "", null, seed);
                    result = avatarService.generateDarkHeaderWithFallback(prompt, lightHeader, iconName,
                            (value, step, total) -> ui.access(() -> {
                                progressBar.setValue(value);
                                progressText.setText(String.format("Step %d / %d (%.0f%%)", step, total, value * 100));
                                ui.push();
                            }),
                            options, style);

                    generatedDarkHeaderImage = result.getResizedImage();
                } else {
                    result = avatarService.generateLightHeaderWithFallback(prompt, iconName,
                            (value, step, total) -> ui.access(() -> {
                                progressBar.setValue(value);
                                progressText.setText(String.format("Step %d / %d (%.0f%%)", step, total, value * 100));
                                ui.push();
                            }),
                            options, style);

                    generatedLightHeaderImage = result.getResizedImage();
                    generatedLightHeaderSeed  = result.getSeed();
                    tiledDeffusionOptions     = options;
                }


                ui.access(() -> {
                    displayHeader(container, result.getResizedImage());
                    updateButton.setEnabled(true);
                    acceptButton.setEnabled(generatedLightImage != null);
                    Notification notification = Notification.show(dark ? "Dark header generated!" : "Light header generated!",
                            3000, Notification.Position.BOTTOM_END);
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    if (!dark && matchingDarkPrompt != null) {
                        beginGeneration();
                        generateHeader(darkHeaderPreviewContainer, darkHeaderUpdateButton, "Generating dark header...",
                                matchingDarkPrompt, result.getSeed(), true, options, style, null);
                    }
                    finishGeneration();
                    ui.push();
                });
            } catch (RuntimeException exception) {
                ui.access(() -> {
                    progressText.setText("Failed to generate header: " + exception.getMessage());
                    updateButton.setEnabled(true);
                    acceptButton.setEnabled(generatedLightImage != null);
                    finishGeneration();
                    ui.push();
                });
            }
        }).start());
    }

    private void generateLightAvatar() {
        String prompt = avatarPromptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a description", 3000, Notification.Position.MIDDLE);
            return;
        }

        String negativePrompt = negativePromptField.getValue().trim();
        String headerPrompt   = headerPromptField.getValue().trim();
        if (headersEnabled && headerPrompt.isEmpty()) {
            Notification.show("Please enter a header description", 3000, Notification.Position.MIDDLE);
            return;
        }
        CozyStylePrompt style = styleSelect.getValue();
        beginGeneration();
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
                    GeneratedImageResult result =
                            avatarService.generateLightAvatarWithFallback(prompt, negativePrompt, iconName,
                                    (progress, step, totalSteps) -> {
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
                        generateDarkAvatar(prompt, negativePrompt);
                        if (headersEnabled) {
                            generateHeader(lightHeaderPreviewContainer, lightHeaderUpdateButton, "Generating light header...",
                                    headerPrompt, -1L, false, style, headerPrompt);
                        }
                        finishGeneration();
                        ui.push();
                    });
                } catch (RuntimeException ex) {
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
                        finishGeneration();
                        ui.push();
                    });
                }
            }).start();
        });
    }

    private void generateLightHeader() {
        String prompt = headerPromptField.getValue().trim();
        if (prompt.isEmpty()) {
            Notification.show("Please enter a light header description", 3000, Notification.Position.MIDDLE);
            return;
        }
        generateHeader(lightHeaderPreviewContainer, lightHeaderUpdateButton, "Generating light header...", prompt, -1L, false,
                styleSelect.getValue(), null);
    }

    private void updateGenerationControls() {
        boolean idle = pendingGenerations == 0;
        styleSelect.setEnabled(idle);
        randomStyleButton.setEnabled(idle);
        generateButton.setEnabled(idle);
        lightUpdateButton.setEnabled(idle);
        darkUpdateButton.setEnabled(idle);
        lightHeaderUpdateButton.setEnabled(idle);
        darkHeaderUpdateButton.setEnabled(idle);
        lightUploadButton.setEnabled(idle);
        darkUploadButton.setEnabled(idle);
        lightHeaderUploadButton.setEnabled(idle);
        darkHeaderUploadButton.setEnabled(idle);
        avatarPromptField.setEnabled(idle);
        negativePromptField.setEnabled(idle);
        headerPromptField.setEnabled(idle);
        acceptButton.setEnabled(idle);
    }

}
