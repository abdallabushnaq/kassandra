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

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for creating and editing versions.
 */
public class VersionDialog extends Dialog {

    public static final String          CANCEL_BUTTON      = "cancel-version-button";
    public static final String          CONFIRM_BUTTON     = "save-version-button";
    public static final String          VERSION_DIALOG     = "version-dialog";
    public static final String          VERSION_NAME_FIELD = "version-name-field";
    private final       Binder<Version> binder;
    private final       boolean         isEditMode;
    private final       TextField       nameField;
    private final       SaveCallback    saveCallback;
    private final       Version         version;

    /**
     * Creates a dialog for creating or editing a version.
     *
     * @param version      The version to edit, or null for creating a new version
     * @param saveCallback Callback that receives the version with updated values and a reference to this dialog
     */
    public VersionDialog(Version version, SaveCallback saveCallback) {
        this.version      = version;
        this.saveCallback = saveCallback;
        isEditMode        = version != null;
        this.binder       = new Binder<>(Version.class);

        // Set the dialog title with an icon
        String title = isEditMode ? "Edit Version" : "Create Version";
        getHeader().add(VaadinUtil.createDialogHeader(title, VaadinIcon.TAG));

        setId(VERSION_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        // Create name field with icon
        {
            nameField = new TextField("Version Name");
            nameField.setId(VERSION_NAME_FIELD);
            nameField.setWidthFull();
            nameField.setRequired(true);
            // Add helper text explaining the uniqueness requirement
            nameField.setHelperText("Version name must be unique");
            nameField.setPrefixComponent(new Icon(VaadinIcon.TAG));
            nameField.setValueChangeMode(ValueChangeMode.EAGER);

            binder.forField(nameField)
                    .asRequired("Version name is required")
                    .withValidationStatusHandler(status -> {
                        nameField.setInvalid(status.isError());
                        status.getMessage().ifPresent(nameField::setErrorMessage);
                    })
                    .bind(Version::getName, Version::setName);
        }

        dialogLayout.add(nameField);

        dialogLayout.add(VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this, binder));

        add(dialogLayout);

        if (isEditMode) {
            binder.readBean(version);
        } else {
            binder.readBean(new Version());
        }

        // Trigger validation to show errors for initially empty fields in create mode
        if (!isEditMode) {
            binder.validate();
        }
    }

    private void save() {

        Version versionToSave;
        if (isEditMode) {
            versionToSave = version;
            versionToSave.setName(nameField.getValue().trim());
        } else {
            versionToSave = new Version();
            versionToSave.setName(nameField.getValue().trim());
        }

        // Call the save callback with the version and a reference to this dialog
        saveCallback.save(versionToSave, this);

    }

    /**
     * Sets an error message on the version name field.
     *
     * @param errorMessage The error message to display, or null to clear the error
     */
    public void setNameFieldError(String errorMessage) {
        nameField.setInvalid(errorMessage != null);
        nameField.setErrorMessage(errorMessage);
    }

    /**
     * Functional interface for the save callback that receives both the version and a reference to this dialog
     */
    @FunctionalInterface
    public interface SaveCallback {
        void save(Version version, VersionDialog dialog);
    }
}
