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

package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.Availability;
import de.bushnaq.abdalla.kassandra.dto.Location;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.dialog.ImagePromptDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.vaadin.addons.tatu.ColorPicker;

import java.awt.*;
import java.util.List;

@Route(value = "profile/:user-email?", layout = MainLayout.class)
@PageTitle("User Profile")
@PermitAll
public class UserProfileView extends Main implements BeforeEnterObserver {
    public static final String                                        GENERATE_AVATAR_BUTTON = "generate-avatar-button";
    public static final String                                        PROFILE_PAGE_TITLE     = "profile-page-title";
    public static final String                                        ROUTE                  = "profile";
    public static final String                                        SAVE_PROFILE_BUTTON    = "save-profile-button";
    public static final String                                        USER_COLOR_PICKER      = "user-color-picker";
    public static final String                                        USER_EMAIL_FIELD       = "user-email-field";
    public static final String                                        USER_NAME_FIELD        = "user-name-field";
    private             com.vaadin.flow.component.html.Image          avatarPreview;
    private             ColorPicker                                   colorPicker;
    private             User                                          currentUser;
    private             byte[]                                        generatedAvatarBytes;
    private             byte[]                                        generatedAvatarBytesOriginal;
    private             String                                        generatedAvatarPrompt;
    private             com.vaadin.flow.component.html.Image          headerAvatarImage;
    private             com.vaadin.flow.component.textfield.TextField nameField;
    private             com.vaadin.flow.component.html.Image          nameFieldAvatarImage;
    private final       StableDiffusionService                        stableDiffusionService;
    private final       UserApi                                       userApi;

    public UserProfileView(UserApi userApi, StableDiffusionService stableDiffusionService) {
        this.userApi                = userApi;
        this.stableDiffusionService = stableDiffusionService;

        setSizeFull();
        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN
        );
        getStyle().set("padding-left", "var(--lumo-space-m)");
        getStyle().set("padding-right", "var(--lumo-space-m)");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Get username from URL parameter or use the currently authenticated user
        String userEmailParam = event.getRouteParameters().get("user-email").orElse(null);

        Authentication authentication  = SecurityContextHolder.getContext().getAuthentication();
        String         currentUsername = authentication != null ? authentication.getName() : null;

        // If no username is provided, use the current authenticated user
        final String userEmail = (userEmailParam == null && currentUsername != null) ? currentUsername : userEmailParam;

        if (userEmail != null) {
            try {
                // Find user by username using the direct getByEmail method
                currentUser = userApi.getByEmail(userEmail);
                initializeView();
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                    // Create a new user since one wasn't found
                    User newUser = createDefaultUser(userEmail);
                    currentUser = userApi.persist(newUser);
                    initializeView();
                    Notification notification = Notification.show("Created new user: " + userEmail, 3000, Notification.Position.MIDDLE);
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    throw ex;
                }
            }
        } else {
            // Redirect to main page if no username and not authenticated
            event.forwardTo("");
        }
    }

    /**
     * Creates a default user with standard availability and location
     *
     * @param username The name for the new user
     * @return A new User object with default settings
     */
    private User createDefaultUser(String username) {
        User user = new User();
        user.setName(username);
        user.setEmail(username);
        user.setFirstWorkingDay(java.time.LocalDate.now());
        user.setColor(new java.awt.Color(51, 102, 204));
        Availability availability = new Availability(1.0f, java.time.LocalDate.now());
        user.addAvailability(availability);
        Location location = new Location("DE", "nw", java.time.LocalDate.now());
        user.addLocation(location);
        return user;
    }

    private void handleGeneratedAvatar(de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult result) {
        this.generatedAvatarBytes         = result.getResizedImage();
        this.generatedAvatarBytesOriginal = result.getOriginalImage();
        this.generatedAvatarPrompt        = result.getPrompt();

        // Update UI from callback (might be from async thread)
        getUI().ifPresent(ui -> ui.access(() -> {
            // Create or update preview
            if (avatarPreview == null) {
                // Avatar preview was an Icon, we need to recreate the view with an Image
                // Easiest way is to just reinitialize the entire view
                initializeView();
                // After reinitializing, the generated avatar bytes are still stored
                // and will be used when user saves
            } else {
                // Update existing preview using resized image
                com.vaadin.flow.server.StreamResource resource = new com.vaadin.flow.server.StreamResource(
                        "user-avatar-preview-" + System.currentTimeMillis() + ".png",
                        () -> new java.io.ByteArrayInputStream(result.getResizedImage())
                );
                avatarPreview.setSrc(resource);
            }

            // Update header icon if it exists
            if (headerAvatarImage != null) {
                com.vaadin.flow.server.StreamResource headerResource = new com.vaadin.flow.server.StreamResource(
                        "user-profile-header-" + System.currentTimeMillis() + ".png",
                        () -> new java.io.ByteArrayInputStream(result.getResizedImage())
                );
                headerAvatarImage.setSrc(headerResource);
            }

            // Update name field icon
            if (nameFieldAvatarImage == null) {
                // Create new image component if it doesn't exist
                nameFieldAvatarImage = new com.vaadin.flow.component.html.Image();
                nameFieldAvatarImage.setWidth("20px");
                nameFieldAvatarImage.setHeight("20px");
                nameFieldAvatarImage.getStyle()
                        .set("border-radius", "4px")
                        .set("object-fit", "cover");
                nameField.setPrefixComponent(nameFieldAvatarImage);
            }
            com.vaadin.flow.server.StreamResource nameFieldResource = new com.vaadin.flow.server.StreamResource(
                    "user-name-field-" + System.currentTimeMillis() + ".png",
                    () -> new java.io.ByteArrayInputStream(result.getResizedImage())
            );
            nameFieldAvatarImage.setSrc(nameFieldResource);

            Notification.show("Avatar generated successfully", 3000, Notification.Position.BOTTOM_END);

            // Push the UI update
            ui.push();
        }));
    }

    private void initializeView() {
        removeAll();

        // Create header
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setPadding(false);
        headerLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

        // Create title icon (avatar or default icon)
        com.vaadin.flow.component.Component titleIcon;
//        if (currentUser.getAvatarImage() != null && currentUser.getAvatarImage().length > 0)
        {
            headerAvatarImage = new com.vaadin.flow.component.html.Image();
            headerAvatarImage.setWidth("32px");
            headerAvatarImage.setHeight("32px");
            headerAvatarImage.getStyle()
                    .set("border-radius", "4px")
                    .set("object-fit", "cover");
//            com.vaadin.flow.server.StreamResource resource = new com.vaadin.flow.server.StreamResource(
//                    "user-profile-" + System.currentTimeMillis() + ".png",
//                    () -> new java.io.ByteArrayInputStream(currentUser.getAvatarImage())
//            );
//            resource.setContentType("image/png");
//            resource.setCacheTime(0);
//            headerAvatarImage.setSrc(resource);
            headerAvatarImage.setSrc("/frontend/avatar-proxy/user/" + currentUser.getId());
            titleIcon = headerAvatarImage;
        }
//        else {
//            headerAvatarImage = null;
//            titleIcon         = new Icon(VaadinIcon.USER);
//        }

        H2 pageTitle = new H2("User Profile");
        pageTitle.setId(PROFILE_PAGE_TITLE);
        pageTitle.addClassNames(
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.Margin.Bottom.SMALL
        );

        HorizontalLayout titleLayout = new HorizontalLayout(titleIcon, pageTitle);
        titleLayout.setSpacing(true);
        titleLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

        headerLayout.add(titleLayout);
        headerLayout.getStyle().set("padding-bottom", "var(--lumo-space-m)");

        // User info
        nameField = new com.vaadin.flow.component.textfield.TextField("Name");
        nameField.setId(USER_NAME_FIELD);
        nameField.setValue(currentUser.getName() != null ? currentUser.getName() : "");
        nameField.setWidthFull();
        nameField.setRequired(true);

        // Set prefix icon (avatar or default icon)
//        if (currentUser.getAvatarPrompt() != null && !currentUser.getAvatarPrompt().isEmpty())
        {
            nameFieldAvatarImage = new com.vaadin.flow.component.html.Image();
            nameFieldAvatarImage.setWidth("20px");
            nameFieldAvatarImage.setHeight("20px");
            nameFieldAvatarImage.getStyle()
                    .set("border-radius", "4px")
                    .set("object-fit", "cover");
            // Use REST API endpoint for avatar - enables browser caching
            nameFieldAvatarImage.setSrc("/frontend/avatar-proxy/user/" + currentUser.getId());
            nameField.setPrefixComponent(nameFieldAvatarImage);
        }
//        else {
//            nameFieldAvatarImage = null;
//            nameField.setPrefixComponent(new Icon(VaadinIcon.USER));
//        }

        // Add wand button to the right of the name field, aligned to the bottom
        Button generateAvatarButton = null;
        if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
            generateAvatarButton = new Button(new Icon(VaadinIcon.MAGIC));
            generateAvatarButton.setId(GENERATE_AVATAR_BUTTON);
            generateAvatarButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            generateAvatarButton.getStyle().set("color", "var(--lumo-primary-contrast-color)");
            generateAvatarButton.addClickListener(e -> openAvatarPromptDialogWithInitialImage());
        }
        HorizontalLayout nameRow = new HorizontalLayout();
        nameRow.setWidthFull();
        nameRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END); // Align to bottom
        nameRow.add(nameField);
        if (generateAvatarButton != null) {
            nameRow.add(generateAvatarButton);
        }
        nameRow.expand(nameField);

        com.vaadin.flow.component.textfield.TextField emailField = new com.vaadin.flow.component.textfield.TextField("Email");
        emailField.setId(USER_EMAIL_FIELD);
        emailField.setValue(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        emailField.setReadOnly(true);
        emailField.setWidthFull();
        emailField.setPrefixComponent(new Icon(VaadinIcon.ENVELOPE));

        // Color picker (editable)
        colorPicker = new ColorPicker();
        colorPicker.setId(USER_COLOR_PICKER);
        colorPicker.setWidth("100%");
        colorPicker.setLabel("User Color");
        colorPicker.setPresets(List.of(
                new ColorPicker.ColorPreset("#FF0000", "Red"),
                new ColorPicker.ColorPreset("#0000FF", "Blue"),
                new ColorPicker.ColorPreset("#008000", "Green"),
                new ColorPicker.ColorPreset("#FFFF00", "Yellow"),
                new ColorPicker.ColorPreset("#FFA500", "Orange"),
                new ColorPicker.ColorPreset("#800080", "Purple"),
                new ColorPicker.ColorPreset("#FFC0CB", "Pink"),
                new ColorPicker.ColorPreset("#00FFFF", "Cyan"),
                new ColorPicker.ColorPreset("#FF00FF", "Magenta"),
                new ColorPicker.ColorPreset("#D3D3D3", "Light Gray"),
                new ColorPicker.ColorPreset("#808080", "Gray"),
                new ColorPicker.ColorPreset("#A9A9A9", "Dark Gray"),
                new ColorPicker.ColorPreset("#000000", "Black")
        ));
        if (currentUser.getColor() != null) {
            String colorHex = String.format("#%06X", (0xFFFFFF & currentUser.getColor().getRGB()));
            colorPicker.setValue(colorHex);
        }

        // Save button
        Button saveButton = new Button("Save Changes", new Icon(VaadinIcon.CHECK));
        saveButton.setId(SAVE_PROFILE_BUTTON);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveProfile());

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(true);
        formLayout.setSpacing(true);
        formLayout.setMaxWidth("600px");
        formLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        formLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        formLayout.add(nameRow, emailField, colorPicker, saveButton);

        add(headerLayout, formLayout);
    }

    private void openAvatarPromptDialog() {
        String defaultPrompt = nameField.getValue().isEmpty()
                ? "Professional portrait avatar, minimalist, flat design, simple background"
                : "Professional portrait avatar of " + nameField.getValue() + ", minimalist, flat design, simple background, icon style";

        ImagePromptDialog imageDialog = new ImagePromptDialog(
                stableDiffusionService,
                defaultPrompt,
                this::handleGeneratedAvatar
        ); // Do not pass initial image
        imageDialog.open();
    }

    private void openAvatarPromptDialogWithInitialImage() {
        String defaultPrompt = nameField.getValue().isEmpty()
                ? "Professional portrait avatar, minimalist, flat design, simple background"
                : "Professional portrait avatar of " + nameField.getValue() + ", minimalist, flat design, simple background, icon style";

        // Fetch avatar image if it exists (needed for img2img)
        byte[] initialImage = null;
//        if (currentUser.getAvatarPrompt() != null && !currentUser.getAvatarPrompt().isEmpty())
        {
            try {
                // Fetch the avatar image from the REST API
                initialImage = userApi.getAvatarImage(currentUser.getId()).getAvatar();
            } catch (Exception e) {
                // If fetch fails, continue without initial image
                System.err.println("Failed to fetch avatar image: " + e.getMessage());
            }
        }

        ImagePromptDialog imageDialog = new ImagePromptDialog(
                stableDiffusionService,
                defaultPrompt,
                this::handleGeneratedAvatar,
                initialImage
        );
        imageDialog.open();
    }

    private void saveProfile() {
        // Validate name field
        if (nameField.getValue() == null || nameField.getValue().trim().isEmpty()) {
            Notification notification = Notification.show("Name cannot be empty", 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            // Update name
            currentUser.setName(nameField.getValue().trim());

            // Convert Vaadin color string to AWT Color
            String colorValue = colorPicker.getValue();
            if (colorValue != null && !colorValue.isEmpty()) {
                currentUser.setColor(Color.decode(colorValue));
            }

            // Save user (without avatar - it's @JsonIgnore)
            userApi.update(currentUser);

            // Update avatar separately if available
            if (generatedAvatarBytes != null && generatedAvatarBytesOriginal != null) {
                userApi.updateAvatarFull(currentUser.getId(), generatedAvatarBytes, generatedAvatarBytesOriginal, generatedAvatarPrompt);
            }

            Notification notification = Notification.show("Profile updated successfully", 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Refresh the view to show updated data
            initializeView();
        } catch (Exception ex) {
            Notification notification = Notification.show("Failed to update profile: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
