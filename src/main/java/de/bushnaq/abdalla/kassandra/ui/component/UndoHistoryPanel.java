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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory;
import de.bushnaq.abdalla.kassandra.rest.api.UndoRedoApi;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Displays and replays the most recent planning operations for the active products.
 */
public class UndoHistoryPanel extends VerticalLayout {
    private final Runnable                   closeCallback;
    private final int                        historyLimit;
    private final Supplier<Collection<UUID>> productIdsSupplier;
    private final Runnable                   refreshCallback;
    private final UndoRedoApi                undoRedoApi;

    /**
     * Creates the planning history panel.
     *
     * @param undoRedoApi        planning history API client
     * @param productIdsSupplier active product IDs
     * @param historyLimit       maximum number of operations to display
     * @param closeCallback      closes the panel after replay
     * @param refreshCallback    refreshes the active view after replay
     */
    public UndoHistoryPanel(UndoRedoApi undoRedoApi, Supplier<Collection<UUID>> productIdsSupplier,
                            int historyLimit, Runnable closeCallback, Runnable refreshCallback) {
        this.undoRedoApi        = undoRedoApi;
        this.productIdsSupplier = productIdsSupplier;
        this.historyLimit       = Math.max(1, historyLimit);
        this.closeCallback      = closeCallback;
        this.refreshCallback    = refreshCallback;
        setPadding(true);
        setSpacing(true);
        setWidthFull();
    }

    private void addOperation(UndoRedoHistory.Operation operation) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setWidthFull();
        Span title = new Span(operation.getProductName() + ": " + operation.getSummary() + " - "
                + operation.getActor() + " (" + operation.getCreated() + ")");
        title.getStyle().set("font-weight", "600").set("white-space", "normal");
        content.add(title);
        operation.getEntityChanges().forEach(change -> {
            Span entity = new Span(change.getAction() + " " + change.getEntityType() + ": " + change.getDisplayName());
            entity.getStyle().set("white-space", "normal");
            content.add(entity);
        });
        HorizontalLayout row = new HorizontalLayout(content);
        row.setWidthFull();
        row.setFlexGrow(1, content);
        row.getStyle().set("flex-shrink", "0");
        row.addClassName("planning-history-operation");
        if (operation.isUndone()) {
            row.addClassName("planning-history-operation-undone");
        }
        row.addClickListener(event -> replay(operation));
        add(row);
    }

    private boolean isVisible(UndoRedoHistory.EntityChange change) {
        return !"Updated".equals(change.getAction()) || !change.getFieldChanges().isEmpty();
    }

    private VerticalLayout operationDetails(UndoRedoHistory.Operation operation) {
        VerticalLayout details = new VerticalLayout();
        details.setPadding(false);
        details.setSpacing(false);
        Span title = new Span(operation.getProductName() + ": " + operation.getSummary());
        title.getStyle().set("font-weight", "600");
        details.add(title);
        details.add(new Span(operation.getCreated() + " - " + operation.getActor()));
        operation.getEntityChanges().stream().filter(this::isVisible).forEach(change -> {
            Span entity = new Span(change.getAction() + " " + change.getEntityType() + ": " + change.getDisplayName());
            entity.getStyle().set("font-weight", "600");
            details.add(entity);
            change.getFieldChanges().forEach(fieldChange -> details.add(new Span(fieldChange)));
        });
        return details;
    }

    /**
     * Reloads the limited operation history for the active products.
     */
    public void refresh() {
        removeAll();
        Collection<UUID> productIds = productIdsSupplier.get();
        if (productIds.isEmpty()) {
            return;
        }
        undoRedoApi.history(productIds, historyLimit).getOperations().forEach(this::addOperation);
    }

    private void replay(UndoRedoHistory.Operation operation) {
        boolean         undo    = !operation.isUndone();
        UndoRedoHistory preview = undoRedoApi.replayPreview(operation.getProductId(), operation.getId(), undo);
        showReplayConfirmation(undo, operation, preview);
    }

    private void showReplayConfirmation(boolean undo, UndoRedoHistory.Operation selectedOperation, UndoRedoHistory preview) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(undo ? "Confirm undo" : "Confirm redo");
        VerticalLayout operations = new VerticalLayout();
        operations.setPadding(false);
        operations.setSpacing(true);
        preview.getOperations().forEach(operation -> operations.add(operationDetails(operation)));
        operations.setWidthFull();
        operations.setHeight("400px");
        dialog.add(operations);
        Button cancel = new Button("Cancel", event -> dialog.close());
        Button confirm = new Button(undo ? "Undo operations" : "Redo operations", event -> {
            if (undo) {
                undoRedoApi.undoThrough(selectedOperation.getProductId(), selectedOperation.getId());
            } else {
                undoRedoApi.redoThrough(selectedOperation.getProductId(), selectedOperation.getId());
            }
            dialog.close();
            closeCallback.run();
            refreshCallback.run();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(cancel, confirm);
        dialog.setWidth("900px");
        dialog.open();
    }

}
