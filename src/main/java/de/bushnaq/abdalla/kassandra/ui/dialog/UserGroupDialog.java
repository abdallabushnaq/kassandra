/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.UserGroup;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserGroupApi;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for creating and editing user groups.
 */
public class UserGroupDialog extends Dialog {

    public static final String                    CANCEL_BUTTON           = "cancel-user-group-button";
    public static final String                    CONFIRM_BUTTON          = "save-user-group-button";
    public static final String                    GROUP_DESCRIPTION_FIELD = "user-group-description-field";
    public static final String                    GROUP_DIALOG            = "user-group-dialog";
    public static final String                    GROUP_MEMBERS_FIELD     = "user-group-members-field";
    public static final String                    GROUP_NAME_FIELD        = "user-group-name-field";
    private final       List<User>                allUsers;
    private final       Binder<UserGroup>         binder;
    private final       TextArea                  descriptionField;
    private final       Span                      errorMessage;
    private final       boolean                   isEditMode;
    private final       MultiSelectComboBox<User> membersField;
    private final       TextField                 nameField;
    private final       UserGroup                 userGroup;
    private final       UserGroupApi              userGroupApi;

    /**
     * Creates a dialog for creating or editing a user group.
     *
     * @param userGroup    The user group to edit, or null for creating a new user group
     * @param userGroupApi The user group API for saving user group data
     * @param userApi      The user API for loading available users
     */
    public UserGroupDialog(UserGroup userGroup, UserGroupApi userGroupApi, UserApi userApi) {
        this.userGroup    = userGroup;
        this.userGroupApi = userGroupApi;
        this.isEditMode   = userGroup != null;
        this.binder       = new Binder<>(UserGroup.class);

        // Load all users for member selection
        this.allUsers = userApi.getAll();

        // Set the dialog title
        String title      = isEditMode ? "Edit User Group" : "Create User Group";
        Icon   headerIcon = new Icon(VaadinIcon.GROUP);
        getHeader().add(VaadinUtil.createDialogHeader(title, headerIcon));

        setId(GROUP_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Create dialog-level error message component (initially hidden)
        errorMessage = VaadinUtil.createDialogErrorMessage();
        dialogLayout.add(errorMessage);

        // Name field
        {
            nameField = new TextField("Group Name");
            nameField.setId(GROUP_NAME_FIELD);
            nameField.setWidthFull();
            nameField.setRequired(true);
            nameField.setHelperText("Group name must be unique");
            nameField.setPrefixComponent(new Icon(VaadinIcon.GROUP));

            binder.forField(nameField)
                    .asRequired("Group name is required")
                    .withValidationStatusHandler(status -> {
                        nameField.setInvalid(status.isError());
                        status.getMessage().ifPresent(nameField::setErrorMessage);
                    })
                    .bind(UserGroup::getName, UserGroup::setName);

            dialogLayout.add(nameField);
        }

        // Description field
        {
            descriptionField = new TextArea("Description");
            descriptionField.setId(GROUP_DESCRIPTION_FIELD);
            descriptionField.setWidthFull();
            descriptionField.setMaxLength(500);
            descriptionField.setHelperText("Optional description for this group");
            descriptionField.setPrefixComponent(new Icon(VaadinIcon.INFO_CIRCLE));
            descriptionField.setHeight("100px");

            binder.forField(descriptionField)
                    .bind(UserGroup::getDescription, UserGroup::setDescription);

            dialogLayout.add(descriptionField);
        }

        // Members field (multi-select combo box)
        {
            membersField = new MultiSelectComboBox<>("Members");
            membersField.setId(GROUP_MEMBERS_FIELD);
            membersField.setWidthFull();
            membersField.setItems(allUsers);
            membersField.setItemLabelGenerator(User::getName);
            membersField.setHelperText("Select users to add to this group");

            // Render users with their avatars in the dropdown
            membersField.setRenderer(new ComponentRenderer<>(user -> {
                var layout = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
                layout.setSpacing(true);
                layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

                // User avatar
                com.vaadin.flow.component.html.Image avatar = new com.vaadin.flow.component.html.Image();
                avatar.setWidth("24px");
                avatar.setHeight("24px");
                avatar.getStyle()
                        .set("border-radius", "4px")
                        .set("object-fit", "cover");
                avatar.setSrc(user.getAvatarUrl());

                // User name
                Span nameSpan = new Span(user.getName());

                layout.add(avatar, nameSpan);
                return layout;
            }));

            // Custom binding for memberIds
            binder.forField(membersField)
                    .bind(
                            // Getter: Convert memberIds (Set<Long>) to Set<User>
                            group -> {
                                if (group.getMemberIds() == null || group.getMemberIds().isEmpty()) {
                                    return new HashSet<>();
                                }
                                return allUsers.stream()
                                        .filter(user -> group.getMemberIds().contains(user.getId()))
                                        .collect(Collectors.toSet());
                            },
                            // Setter: Convert Set<User> to memberIds (Set<Long>)
                            (group, selectedUsers) -> {
                                if (selectedUsers == null || selectedUsers.isEmpty()) {
                                    group.setMemberIds(new HashSet<>());
                                } else {
                                    Set<Long> memberIds = selectedUsers.stream()
                                            .map(User::getId)
                                            .collect(Collectors.toSet());
                                    group.setMemberIds(memberIds);
                                }
                            }
                    );

            dialogLayout.add(membersField);
        }

        // Buttons
        dialogLayout.add(VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this, binder));

        add(dialogLayout);

        // Read bean data
        if (isEditMode) {
            binder.readBean(userGroup);
        } else {
            UserGroup newGroup = new UserGroup();
            newGroup.setMemberIds(new HashSet<>());
            binder.readBean(newGroup);
        }

        // Trigger validation to show errors for initially empty fields in create mode
        if (!isEditMode) {
            binder.validate();
        }
    }

    private void save() {
        // Clear any previous error messages
        VaadinUtil.hideDialogError(errorMessage);

        UserGroup userGroupToSave;
        if (isEditMode) {
            userGroupToSave = userGroup;
        } else {
            userGroupToSave = new UserGroup();
        }

        try {
            binder.writeBean(userGroupToSave);
        } catch (ValidationException e) {
            return;
        }

        try {
            if (isEditMode) {
                // Edit mode
                userGroupApi.update(userGroupToSave);
                Notification.show("User group updated", 3000, Notification.Position.BOTTOM_START);
            } else {
                // Create mode
                userGroupApi.persist(userGroupToSave);
                Notification.show("User group created", 3000, Notification.Position.BOTTOM_START);
            }
            close();
        } catch (Exception e) {
            // Use VaadinUtil to handle the exception with field-specific and dialog-level error routing
            // Field-specific errors (like CONFLICT) go to the name field
            // Field-independent errors (like FORBIDDEN) go to the dialog-level error message
            VaadinUtil.handleApiException(e, "name", this::setNameFieldError, msg -> VaadinUtil.showDialogError(errorMessage, msg));
        }
    }

    /**
     * Sets an error message on the group name field.
     *
     * @param errorMessage The error message to display, or null to clear the error
     */
    public void setNameFieldError(String errorMessage) {
        nameField.setInvalid(errorMessage != null);
        nameField.setErrorMessage(errorMessage);
    }
}

