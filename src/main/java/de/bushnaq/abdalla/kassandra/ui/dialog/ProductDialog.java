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
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    private final       AvatarUpdateRequest    avatarUpdateRequest;
    private final       Binder<Product>        binder;
    private             byte[]                 generatedImageBytes;
    private             byte[]                 generatedImageBytesOriginal;
    private             String                 generatedImagePrompt;
    private final       Image                  headerIcon;
    private final       boolean                isEditMode;
    private final       TextField              nameField;
    private final       Image                  nameFieldImage;
    private final       Product                product;
    private final       ProductApi             productApi;
    private final       StableDiffusionService stableDiffusionService;

    /**
     * Creates a dialog for creating or editing a product.
     *
     * @param product                The product to edit, or null for creating a new product
     * @param stableDiffusionService The AI image generation service (optional, can be null)
     * @param productApi             The product API for saving product data
     */
    public ProductDialog(Product product, StableDiffusionService stableDiffusionService, ProductApi productApi) {
        this.product                = product;
        this.productApi             = productApi;
        this.stableDiffusionService = stableDiffusionService;
        isEditMode                  = product != null;
        this.binder                 = new Binder<>(Product.class);

        // Only fetch avatar if editing an existing product
        if (product != null)
            this.avatarUpdateRequest = productApi.getAvatarFull(product.getId());
        else
            this.avatarUpdateRequest = null;
        setId(PRODUCT_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);
        String title = isEditMode ? "Edit Product" : "Create Product";

        // Create header icon using avatar proxy endpoint
        headerIcon = new Image();
        headerIcon.setWidth("24px");
        headerIcon.setHeight("24px");
        headerIcon.getStyle()
                .set("border-radius", "4px")
                .set("object-fit", "cover");
        if (isEditMode) {
            headerIcon.setSrc(product.getAvatarUrl());
        }
        // For create mode, leave image src empty
        getHeader().add(VaadinUtil.createDialogHeader(title, headerIcon));

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Create name field with icon and AI button
        {
            nameField = new TextField("Product Name");
            nameField.setId(PRODUCT_NAME_FIELD);
            nameField.setWidthFull();
            nameField.setRequired(true);
            nameField.setHelperText("Product name must be unique");

            binder.forField(nameField)
                    .asRequired("Product name is required")
                    .withValidationStatusHandler(status -> {
                        nameField.setInvalid(status.isError());
                        status.getMessage().ifPresent(nameField::setErrorMessage);
                    })
                    .bind(Product::getName, Product::setName);

            // Create name field prefix icon using avatar proxy endpoint
            nameFieldImage = new Image();
            nameFieldImage.setWidth("20px");
            nameFieldImage.setHeight("20px");
            nameFieldImage.getStyle()
                    .set("border-radius", "4px")
                    .set("object-fit", "cover");
            if (isEditMode) {
                nameFieldImage.setSrc(product.getAvatarUrl());
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
            binder.readBean(product);
        } else {
            binder.readBean(new Product());
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

        // Compute hash from the resized image
//        String newHash = AvatarUtil.computeHash(generatedImageBytes);

        // For edit mode: Update hash in product DTO and save avatar to database immediately
//        if (isEditMode && product != null) {
//            product.setAvatarHash(newHash);
//            productApi.updateAvatarFull(product.getId(), generatedImageBytes, generatedImageBytesOriginal, generatedImagePrompt);
//        }

        // Update UI from callback (might be from async thread)
        getUI().ifPresent(ui -> ui.access(() -> {
            // Create StreamResource for the generated image
            StreamResource resource = new StreamResource("product-avatar.png", () -> new ByteArrayInputStream(result.getResizedImage()));

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
            defaultPrompt = Product.getDefaultAvatarPrompt(nameField.getValue());
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
        Product productToSave;
        if (isEditMode) {
            productToSave = product;
        } else {
            productToSave = new Product();
        }

        try {
            binder.writeBean(productToSave);
        } catch (ValidationException e) {
            return;
        }

        // Extract avatar data before save (fields are @JsonIgnore so won't be sent via normal update)
        byte[] avatarImage         = generatedImageBytes;
        byte[] avatarImageOriginal = generatedImageBytesOriginal;
        String avatarPrompt        = generatedImagePrompt;

        if (avatarImage != null) {
            String newHash = AvatarUtil.computeHash(avatarImage);
            productToSave.setAvatarHash(newHash);
        } else if (avatarUpdateRequest == null) {
            //generate default avatar if none exists
            GeneratedImageResult image = stableDiffusionService.generateDefaultAvatar("cube");
            avatarImage         = image.getResizedImage();
            avatarImageOriginal = image.getOriginalImage();
            avatarPrompt        = image.getPrompt();
            String newHash = AvatarUtil.computeHash(image.getResizedImage());
            productToSave.setAvatarHash(newHash);
        }
        try {
            if (isEditMode) {
                // Edit mode
                productApi.update(productToSave);

                if (avatarImage != null && avatarImageOriginal != null) {
                    productApi.updateAvatarFull(productToSave.getId(), avatarImage, avatarImageOriginal, avatarPrompt);
                }

                Notification.show("Product updated", 3000, Notification.Position.BOTTOM_START);
            } else {
                // Create mode
                Product createdProduct = productApi.persist(productToSave);

                if (avatarImage != null && avatarImageOriginal != null && createdProduct != null) {
                    productApi.updateAvatarFull(createdProduct.getId(), avatarImage, avatarImageOriginal, avatarPrompt);
                }

                Notification.show("Product created", 3000, Notification.Position.BOTTOM_START);
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
     * Sets an error message on the product name field.
     *
     * @param errorMessage The error message to display, or null to clear the error
     */
    public void setNameFieldError(String errorMessage) {
        nameField.setInvalid(errorMessage != null);
        nameField.setErrorMessage(errorMessage);
    }
}

