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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for creating and editing features.
 */
public class FeatureDialog extends Dialog {

    public static final String       CANCEL_BUTTON      = "cancel-feature-button";
    public static final String       CONFIRM_BUTTON     = "save-feature-button";
    public static final String       FEATURE_DIALOG     = "feature-dialog";
    public static final String       FEATURE_NAME_FIELD = "feature-name-field";
    private final       Feature      feature;
    private final       boolean      isEditMode;
    private final       TextField    nameField;
    private final       SaveCallback onSaveCallback;

    /**
     * Creates a dialog for creating or editing a feature.
     *
     * @param feature        The feature to edit, or null for creating a new feature
     * @param onSaveCallback Callback that receives the feature with updated values and a reference to this dialog
     */
    public FeatureDialog(Feature feature, SaveCallback onSaveCallback) {
        this.feature        = feature;
        this.onSaveCallback = onSaveCallback;
        isEditMode          = feature != null;

        // Set the dialog title with an icon
        String title = isEditMode ? "Edit Feature" : "Create Feature";
        getHeader().add(VaadinUtil.createDialogHeader(title, VaadinIcon.LIGHTBULB));

        setId(FEATURE_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        nameField = new TextField("Feature Name");
        nameField.setId(FEATURE_NAME_FIELD);
        nameField.setWidthFull();
        nameField.setRequired(true);
        // Add helper text explaining the uniqueness requirement
        nameField.setHelperText("Feature name must be unique");
        nameField.setPrefixComponent(new Icon(VaadinIcon.LIGHTBULB));

        if (isEditMode) {
            nameField.setValue(feature.getName());
        }

        dialogLayout.add(nameField);

        dialogLayout.add(VaadinUtil.createDialogButtonLayout("Save", CONFIRM_BUTTON, "Cancel", CANCEL_BUTTON, this::save, this));
        add(dialogLayout);
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
        }

        // Call the save callback with the feature and a reference to this dialog
        onSaveCallback.save(featureToSave, this);
    }


    /**
     * Sets an error message on the name field to indicate uniqueness violation
     *
     * @param errorMessage The error message to show
     */
    public void setErrorMessage(String errorMessage) {
        nameField.setErrorMessage(errorMessage);
        nameField.setInvalid(true);
    }

    /**
     * Functional interface for the save callback that receives the edited feature and a reference to this dialog
     */
    @FunctionalInterface
    public interface SaveCallback {
        void save(Feature feature, FeatureDialog dialog);
    }
}
