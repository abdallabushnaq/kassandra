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

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory;
import de.bushnaq.abdalla.kassandra.rest.api.UndoRedoApi;
import de.bushnaq.abdalla.kassandra.ui.dialog.UndoHistoryConfirmationDialog;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
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
    private final Function<UUID, String>     productAvatarUrlResolver;
    private final Consumer<String>           userAvatarCache;
    private final Function<String, String>   userAvatarUrlResolver;

    /**
     * Creates the planning history panel.
     *
     * @param undoRedoApi              planning history API client
     * @param productIdsSupplier       active product IDs
     * @param historyLimit             maximum number of operations to display
     * @param closeCallback            closes the panel after replay
     * @param refreshCallback          refreshes the active view after replay
     * @param productAvatarUrlResolver resolves product avatar URLs
     * @param userAvatarUrlResolver    resolves actor avatar URLs
     * @param userAvatarCache          preloads actor avatar data for the current session
     */
    public UndoHistoryPanel(UndoRedoApi undoRedoApi, Supplier<Collection<UUID>> productIdsSupplier,
                            int historyLimit, Runnable closeCallback, Runnable refreshCallback,
                            Function<UUID, String> productAvatarUrlResolver, Function<String, String> userAvatarUrlResolver,
                            Consumer<String> userAvatarCache) {
        this.undoRedoApi              = undoRedoApi;
        this.productIdsSupplier       = productIdsSupplier;
        this.historyLimit             = Math.max(1, historyLimit);
        this.closeCallback            = closeCallback;
        this.refreshCallback          = refreshCallback;
        this.productAvatarUrlResolver = productAvatarUrlResolver;
        this.userAvatarUrlResolver    = userAvatarUrlResolver;
        this.userAvatarCache          = userAvatarCache;
        setPadding(true);
        setSpacing(true);
        setWidthFull();
    }

    private void addOperation(UndoRedoHistory.Operation operation) {
        boolean undo = !operation.isUndone();
        UndoHistoryOperationDetails details = new UndoHistoryOperationDetails(operation, productAvatarUrlResolver,
                userAvatarUrlResolver, undo ? "Undo" : "Redo", () -> replay(operation));
        details.setWidthFull();
        if (operation.isUndone()) {
            details.addClassName("planning-history-operation-undone");
        }
        add(details);
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
        undoRedoApi.history(productIds, historyLimit).getOperations().forEach(operation -> {
            userAvatarCache.accept(operation.getActor());
            addOperation(operation);
        });
    }

    private void replay(UndoRedoHistory.Operation operation) {
        boolean         undo    = !operation.isUndone();
        UndoRedoHistory preview = undoRedoApi.replayPreview(operation.getProductId(), operation.getId(), undo);
        preview.getOperations().forEach(previewOperation -> userAvatarCache.accept(previewOperation.getActor()));
        UndoHistoryConfirmationDialog dialog = new UndoHistoryConfirmationDialog(undo, preview,
                productAvatarUrlResolver, userAvatarUrlResolver, () -> {
            if (undo) {
                undoRedoApi.undoThrough(operation.getProductId(), operation.getId());
            } else {
                undoRedoApi.redoThrough(operation.getProductId(), operation.getId());
            }
            closeCallback.run();
            refreshCallback.run();
        });
        dialog.open();
    }

}
