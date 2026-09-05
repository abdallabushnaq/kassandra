/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory;

import java.util.UUID;
import java.util.function.Function;

/**
 * Confirms the complete server-selected range of planning operations to undo or redo.
 */
public class UndoHistoryConfirmationDialog extends Dialog {
    private final Function<UUID, String>   productAvatarUrlResolver;
    private final Function<String, String> userAvatarUrlResolver;

    /**
     * Creates a dialog that displays all operations affected by a replay.
     *
     * @param undo                     whether the replay undoes rather than redoes operations
     * @param preview                  server-authoritative replay preview
     * @param productAvatarUrlResolver resolves product avatar URLs
     * @param userAvatarUrlResolver    resolves actor avatar URLs
     * @param confirmCallback          performs the selected replay
     */
    public UndoHistoryConfirmationDialog(boolean undo, UndoRedoHistory preview,
                                         Function<UUID, String> productAvatarUrlResolver,
                                         Function<String, String> userAvatarUrlResolver, Runnable confirmCallback) {
        this.productAvatarUrlResolver = productAvatarUrlResolver;
        this.userAvatarUrlResolver    = userAvatarUrlResolver;
        setHeaderTitle(undo ? "Confirm undo" : "Confirm redo");

        VerticalLayout operations = new VerticalLayout();
        operations.setPadding(false);
        operations.setSpacing(true);
        preview.getOperations().forEach(operation -> operations.add(operationDetails(operation)));
        operations.setWidthFull();
        operations.setHeight("400px");
        operations.getStyle().set("overflow-y", "auto");
        add(operations);

        Button cancel = new Button("Cancel", event -> close());
        Button confirm = new Button(undo ? "Undo operations" : "Redo operations", event -> {
            confirmCallback.run();
            close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, confirm);
        setWidth("900px");
    }

    private Image createAvatar(String source, String alt) {
        if (source == null) {
            return null;
        }
        Image avatar = new Image(source, alt);
        avatar.setWidth("24px");
        avatar.setHeight("24px");
        avatar.getStyle().set("border-radius", "4px").set("object-fit", "cover");
        return avatar;
    }

    private boolean isVisible(UndoRedoHistory.EntityChange change) {
        return !"Updated".equals(change.getAction()) || !change.getFieldChanges().isEmpty();
    }

    private VerticalLayout operationDetails(UndoRedoHistory.Operation operation) {
        VerticalLayout details = new VerticalLayout();
        details.setPadding(false);
        details.setSpacing(false);

        HorizontalLayout title = new HorizontalLayout();
        title.setAlignItems(Alignment.CENTER);
        Image productAvatar = createAvatar(productAvatarUrlResolver.apply(operation.getProductId()), operation.getProductName());
        if (productAvatar != null) {
            title.add(productAvatar);
        }
        Span summary = new Span(operation.getProductName() + ": " + operation.getSummary());
        summary.getStyle().set("font-weight", "600");
        title.add(summary);
        details.add(title);

        HorizontalLayout actor = new HorizontalLayout();
        actor.setAlignItems(Alignment.CENTER);
        Image userAvatar = createAvatar(userAvatarUrlResolver.apply(operation.getActor()), operation.getActor());
        if (userAvatar != null) {
            actor.add(userAvatar);
        }
        actor.add(new Span(operation.getCreated() + " - " + operation.getActor()));
        details.add(actor);

        var visibleChanges = operation.getEntityChanges().stream().filter(this::isVisible).toList();
        if (visibleChanges.isEmpty()) {
            details.add(new Span("No updates in any fields."));
        } else {
            visibleChanges.forEach(change -> {
                Span entity = new Span(change.getAction() + " " + change.getEntityType() + ": " + change.getDisplayName());
                entity.getStyle().set("font-weight", "600");
                details.add(entity);
                change.getFieldChanges().forEach(fieldChange -> details.add(new Span(fieldChange)));
            });
        }
        return details;
    }
}
