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

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;

import static de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DIALOG_DEFAULT_WIDTH;

/**
 * A reusable dialog for confirming delete operations.
 */
public class ConfirmDialog extends Dialog {

    public static final String CANCEL_BUTTON  = "cancel-confirm-button";
    public static final String CONFIRM_BUTTON = "confirm-button";
    public static final String CONFIRM_DIALOG = "confirm-dialog";

    /**
     * Creates a confirmation dialog for delete operations.
     *
     * @param title             The dialog title
     * @param message           The confirmation message to display
     * @param confirmButtonText Text for the delete/confirm button
     * @param action            Runnable to execute when confirmed
     */
    public ConfirmDialog(String title, String message, String confirmButtonText, Runnable action) {
        setId(CONFIRM_DIALOG);
        setWidth(DIALOG_DEFAULT_WIDTH);
        getHeader().add(VaadinUtil.createDialogHeader(title, VaadinIcon.HOURGLASS));

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.add(message);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        var buttonLayout = VaadinUtil.createDialogButtonLayout(
                confirmButtonText,
                CONFIRM_BUTTON,
                "Cancel",
                CANCEL_BUTTON,
                () -> {
                    action.run();
                    close();
                },
                this
        );

        // Add error theme variant to the confirm button (first button in layout)
        buttonLayout.getComponentAt(0).getElement().getThemeList().add(ButtonVariant.LUMO_ERROR.getVariantName());

        dialogLayout.add(buttonLayout);
        add(dialogLayout);
    }
}
