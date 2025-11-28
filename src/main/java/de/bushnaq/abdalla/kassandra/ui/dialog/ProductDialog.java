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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.StreamResource;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import java.io.ByteArrayInputStream;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for creating and editing products.
 */
public class ProductDialog extends Dialog {

    public static final String                 CANCEL_BUTTON         = "cancel-product-button";
    public static final String                 CONFIRM_BUTTON        = "save-product-button";
    public static final String                 GENERATE_IMAGE_BUTTON = "generate-product-image-button";
    public static final String                 PRODUCT_DIALOG        = "product-dialog";
    public static final String                 PRODUCT_NAME_FIELD    = "product-name-field";
    private final       Image                  avatarPreview;
    private             byte[]                 generatedImageBytes;
    private final       boolean                isEditMode;
    private final       TextField              nameField;
    private final       Product                product;
    private final       SaveCallback           saveCallback;
    private final       StableDiffusionService stableDiffusionService;

    /**
     * Creates a dialog for creating or editing a product.
     *
     * @param product                The product to edit, or null for creating a new product
     * @param stableDiffusionService The AI image generation service (optional, can be null)
     * @param saveCallback           Callback that receives the product with updated values and a reference to this dialog
     */
    public ProductDialog(Product product, StableDiffusionService stableDiffusionService, SaveCallback saveCallback) {
        this.product                = product;
        this.saveCallback           = saveCallback;
        this.stableDiffusionService = stableDiffusionService;
        isEditMode                  = product != null;

        setId(PRODUCT_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);
        String title = isEditMode ? "Edit Product" : "Create Product";
        getHeader().add(VaadinUtil.createDialogHeader(title, VaadinIcon.CUBE));

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Create name field with icon and AI button
        nameField = new TextField("Product Name");
        nameField.setId(PRODUCT_NAME_FIELD);
        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setHelperText("Product name must be unique");
        nameField.setPrefixComponent(new Icon(VaadinIcon.CUBE));

        // Set to eager mode so value changes fire on every keystroke
        nameField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);

        if (isEditMode) {
            nameField.setValue(product.getName());
        }

        // AI Image generation button (only show if service is available)
        Button generateImageButton = null;
        if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
            generateImageButton = new Button("Generate Image", new Icon(VaadinIcon.MAGIC));
            generateImageButton.setId(GENERATE_IMAGE_BUTTON);
            generateImageButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            generateImageButton.addClickListener(e -> openImagePromptDialog());

            // Disable button if name field is empty
            boolean isNameEmpty = nameField.isEmpty();
            generateImageButton.setEnabled(!isNameEmpty);
            if (isNameEmpty) {
                generateImageButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                generateImageButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }

            nameField.setSuffixComponent(generateImageButton);
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

        dialogLayout.add(nameField);

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

    private void handleGeneratedImage(byte[] imageBytes) {
        this.generatedImageBytes = imageBytes;

        // Update UI from callback (might be from async thread)
        getUI().ifPresent(ui -> ui.access(() -> {
            // Show preview
            StreamResource resource = new StreamResource("product-avatar.png",
                    () -> new ByteArrayInputStream(imageBytes));
            avatarPreview.setSrc(resource);
            avatarPreview.setVisible(true);

            Notification.show("Image set successfully", 3000, Notification.Position.BOTTOM_END);

            // Push the UI update
            ui.push();
        }));
    }

    private void openImagePromptDialog() {
        String defaultPrompt = nameField.getValue().isEmpty()
                ? "Modern tech product icon, minimalist, flat design"
                : "Icon representing " + nameField.getValue() + ", minimalist, flat design";

        ImagePromptDialog imageDialog = new ImagePromptDialog(
                stableDiffusionService,
                defaultPrompt,
                this::handleGeneratedImage
        );
        imageDialog.open();
    }

    private void save() {
        if (nameField.getValue().trim().isEmpty()) {
            Notification.show("Please enter a product name", 3000, Notification.Position.MIDDLE);
            return;
        }

        Product productToSave;
        if (isEditMode) {
            productToSave = product;
            productToSave.setName(nameField.getValue().trim());
        } else {
            productToSave = new Product();
            productToSave.setName(nameField.getValue().trim());
        }

        // Set generated image if available
        if (generatedImageBytes != null) {
            productToSave.setAvatarImage(generatedImageBytes);
        }

        // Call the save callback with the product and a reference to this dialog
        saveCallback.save(productToSave, this);

    }

    /**
     * Sets an error message on the product name field.
     *
     * @param errorMessage The error message to display, or null to clear the error
     */
    public void setNameFieldError(String errorMessage) {
        nameField.setInvalid(errorMessage != null);
        nameField.setErrorMessage(errorMessage);
    }

    /**
     * Functional interface for the save callback that receives both the product and a reference to this dialog
     */
    @FunctionalInterface
    public interface SaveCallback {
        void save(Product product, ProductDialog dialog);
    }
}