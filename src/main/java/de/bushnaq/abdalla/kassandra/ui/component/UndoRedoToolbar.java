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
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory;
import de.bushnaq.abdalla.kassandra.rest.api.UndoRedoApi;

import java.util.UUID;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shared active-product controls for server-authoritative undo and redo.
 */
public class UndoRedoToolbar extends HorizontalLayout {
    public static final String REDO_BUTTON_ID = "main-layout-redo-button";
    public static final String UNDO_BUTTON_ID = "main-layout-undo-button";
    private final UndoRedoApi undoRedoApi;
    private final Supplier<Collection<UUID>> productIdsSupplier;
    private final Consumer<UndoRedoHistory> refreshConsumer;
    private final Button undoButton;
    private final Button redoButton;
    private final ContextMenu undoHistoryMenu;
    private final ContextMenu redoHistoryMenu;

    /**
     * Creates toolbar controls for products supplied by the active view.
     *
     * @param undoRedoApi history API client
     * @param productIdsSupplier active product ID supplier
     * @param refreshConsumer view callback invoked after replay
     */
    public UndoRedoToolbar(UndoRedoApi undoRedoApi, Supplier<Collection<UUID>> productIdsSupplier,
            Consumer<UndoRedoHistory> refreshConsumer) {
        this.undoRedoApi = undoRedoApi;
        this.productIdsSupplier = productIdsSupplier;
        this.refreshConsumer = refreshConsumer;
        undoButton = new Button(VaadinIcon.ARROW_BACKWARD.create(), event -> replay(true));
        redoButton = new Button(VaadinIcon.ARROW_FORWARD.create(), event -> replay(false));
        Button undoHistoryButton = new Button(VaadinIcon.CHEVRON_DOWN_SMALL.create());
        Button redoHistoryButton = new Button(VaadinIcon.CHEVRON_DOWN_SMALL.create());
        undoButton.setId(UNDO_BUTTON_ID);
        redoButton.setId(REDO_BUTTON_ID);
        undoButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        redoButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        undoHistoryButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        redoHistoryButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        undoButton.setTooltipText("Undo");
        redoButton.setTooltipText("Redo");
        undoHistoryButton.setTooltipText("Undo history");
        redoHistoryButton.setTooltipText("Redo history");
        undoHistoryMenu = new ContextMenu(undoHistoryButton);
        redoHistoryMenu = new ContextMenu(redoHistoryButton);
        undoHistoryMenu.setOpenOnClick(true);
        redoHistoryMenu.setOpenOnClick(true);
        add(undoButton, undoHistoryButton, redoButton, redoHistoryButton);
        setPadding(false);
        setSpacing(false);
        refresh();
    }

    /**
     * Reloads button states from active product histories.
     */
    public void refresh() {
        Collection<UUID> productIds = productIdsSupplier.get();
        if (productIds.isEmpty()) {
            undoButton.setEnabled(false);
            redoButton.setEnabled(false);
            undoHistoryMenu.removeAll();
            redoHistoryMenu.removeAll();
            return;
        }
        update(undoRedoApi.history(productIds));
    }

    private void replay(boolean undo) {
        UndoRedoHistory.Operation operation = nextOperation(undo);
        if (operation == null) {
            return;
        }
        replayThrough(undo, operation.getId(), operation.getProductId());
    }

    private void replayThrough(boolean undo, UUID operationId) {
        UndoRedoHistory history = undoRedoApi.history(productIdsSupplier.get());
        UndoRedoHistory.Operation operation = history.getOperations().stream()
                .filter(candidate -> candidate.getId().equals(operationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Operation is outside the active product scope"));
        replayThrough(undo, operationId, operation.getProductId());
    }

    private void replayThrough(boolean undo, UUID operationId, UUID productId) {
        UndoRedoHistory history = undo ? undoRedoApi.undoThrough(productId, operationId) : undoRedoApi.redoThrough(productId, operationId);
        refresh();
        refreshConsumer.accept(history);
    }

    private void update(UndoRedoHistory history) {
        undoButton.setEnabled(history.isCanUndo());
        redoButton.setEnabled(history.isCanRedo());
        undoHistoryMenu.removeAll();
        redoHistoryMenu.removeAll();
        history.getOperations().stream()
                .filter(operation -> !operation.isUndone())
                .forEach(operation -> addOperation(undoHistoryMenu, operation, true));
        history.getOperations().stream()
                .filter(UndoRedoHistory.Operation::isUndone)
                .sorted((left, right) -> Long.compare(left.getSequenceNumber(), right.getSequenceNumber()))
                .forEach(operation -> addOperation(redoHistoryMenu, operation, false));
        history.getOperations().stream()
                .filter(operation -> !operation.isUndone())
                .findFirst()
                .ifPresent(operation -> undoButton.setTooltipText("Undo: " + operation.getSummary()));
        history.getOperations().stream()
                .filter(UndoRedoHistory.Operation::isUndone)
                .min((left, right) -> Long.compare(left.getSequenceNumber(), right.getSequenceNumber()))
                .ifPresent(operation -> redoButton.setTooltipText("Redo: " + operation.getSummary()));
    }

    private UndoRedoHistory.Operation nextOperation(boolean undo) {
        UndoRedoHistory history = undoRedoApi.history(productIdsSupplier.get());
        return history.getOperations().stream()
                .filter(operation -> undo != operation.isUndone())
                .min((left, right) -> undo
                        ? right.getCreated().compareTo(left.getCreated())
                        : left.getCreated().compareTo(right.getCreated()))
                .orElse(null);
    }

    private void addOperation(ContextMenu menu, UndoRedoHistory.Operation operation, boolean undo) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setWidth("360px");
        content.getStyle().set("gap", "var(--lumo-space-xs)");

        Span title = new Span(operation.getProductName() + ": " + operation.getSummary() + " - "
                + operation.getActor() + " (" + operation.getCreated() + ")");
        title.getStyle().set("font-weight", "600").set("white-space", "normal");
        content.add(title);
        operation.getEntityChanges().forEach(change -> {
            Span entity = new Span(change.getAction() + " " + change.getEntityType() + ": " + change.getDisplayName());
            entity.getStyle().set("white-space", "normal");
            content.add(entity);
        });

        MenuItem item = menu.addItem(content);
        item.addClickListener(event -> replayThrough(undo, operation.getId()));
    }
}
