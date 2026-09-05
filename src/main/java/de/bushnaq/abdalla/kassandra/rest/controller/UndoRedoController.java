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

package de.bushnaq.abdalla.kassandra.rest.controller;

import de.bushnaq.abdalla.kassandra.dao.UndoableOperationDAO;
import de.bushnaq.abdalla.kassandra.config.KassandraProperties;
import de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory;
import de.bushnaq.abdalla.kassandra.repository.ProductRepository;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.service.AclSecurityService;
import de.bushnaq.abdalla.kassandra.service.PlanningChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Provides product-scoped planning history and its global linear undo/redo operations.
 */
@RestController
@RequestMapping("/api")
public class UndoRedoController {
    @Autowired
    private AclSecurityService aclSecurityService;
    @Autowired
    private KassandraProperties kassandraProperties;
    @Autowired
    private PlanningChangeService planningChangeService;
    @Autowired
    private ProductRepository productRepository;

    /**
     * Gets the product's planning history without exposing stored entity snapshots.
     *
     * @param productId product ID
     * @return product undo/redo availability and operations, newest first
     */
    @GetMapping("/product/{productId}/history")
    @PreAuthorize("@aclSecurityService.hasProductAccess(#productId) or hasRole('ADMIN')")
    public UndoRedoHistory history(@PathVariable UUID productId) {
        return history(List.of(productId), null);
    }

    /**
     * Gets globally ordered history for the specified accessible products.
     *
     * @param productIds product IDs in the active page scope
     * @param limit maximum number of operations to return
     * @return undo/redo availability and product operations, newest first
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public UndoRedoHistory history(@org.springframework.web.bind.annotation.RequestParam Collection<UUID> productIds,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit) {
        if (productIds.isEmpty()) {
            UndoRedoHistory emptyHistory = new UndoRedoHistory();
            emptyHistory.setOperations(List.of());
            return emptyHistory;
        }
        if (!SecurityUtils.isAdmin() && productIds.stream().anyMatch(productId -> !aclSecurityService.hasProductAccess(productId))) {
            throw new org.springframework.security.access.AccessDeniedException("Access to product history is denied");
        }
        UndoRedoHistory history = new UndoRedoHistory();
        int operationLimit = limit == null ? kassandraProperties.getUndoRedo().getHistoryLimit() : limit;
        if (operationLimit < 1) {
            throw new IllegalArgumentException("History limit must be at least one");
        }
        history.setOperations(planningChangeService.history(productIds, operationLimit).stream().map(this::operation).toList());
        history.setCanUndo(history.getOperations().stream().anyMatch(operation -> !operation.isUndone()));
        history.setCanRedo(history.getOperations().stream().anyMatch(UndoRedoHistory.Operation::isUndone));
        return history;
    }

    /**
     * Reapplies the latest undone planning operation for a product.
     *
     * @param productId product ID
     * @return updated product undo/redo availability and history
     */
    @PostMapping("/product/{productId}/redo")
    @PreAuthorize("@aclSecurityService.hasProductAccess(#productId) or hasRole('ADMIN')")
    public UndoRedoHistory redo(@PathVariable UUID productId) {
        planningChangeService.redo(productId);
        return history(productId);
    }

    /**
     * Reapplies the consecutive undone operations through the selected operation.
     *
     * @param productId product ID
     * @param operationId last operation to reapply
     * @return updated product undo/redo availability and history
     */
    @PostMapping("/product/{productId}/redo/{operationId}")
    @PreAuthorize("@aclSecurityService.hasProductAccess(#productId) or hasRole('ADMIN')")
    public UndoRedoHistory redoThrough(@PathVariable UUID productId, @PathVariable UUID operationId) {
        planningChangeService.redoThrough(productId, operationId);
        return history(productId);
    }

    /**
     * Reverts the latest applied planning operation for a product.
     *
     * @param productId product ID
     * @return updated product undo/redo availability and history
     */
    @PostMapping("/product/{productId}/undo")
    @PreAuthorize("@aclSecurityService.hasProductAccess(#productId) or hasRole('ADMIN')")
    public UndoRedoHistory undo(@PathVariable UUID productId) {
        planningChangeService.undo(productId);
        return history(productId);
    }

    /**
     * Reverts the consecutive applied operations through the selected operation.
     *
     * @param productId product ID
     * @param operationId oldest operation to revert
     * @return updated product undo/redo availability and history
     */
    @PostMapping("/product/{productId}/undo/{operationId}")
    @PreAuthorize("@aclSecurityService.hasProductAccess(#productId) or hasRole('ADMIN')")
    public UndoRedoHistory undoThrough(@PathVariable UUID productId, @PathVariable UUID operationId) {
        planningChangeService.undoThrough(productId, operationId);
        return history(productId);
    }

    /**
     * Gets the exact operation range that a selected undo or redo action will replay.
     *
     * @param productId product whose history is inspected
     * @param operationId selected operation
     * @param undo whether the selected action is undo
     * @return operations that will be replayed
     */
    @GetMapping("/product/{productId}/history/{operationId}/preview")
    @PreAuthorize("@aclSecurityService.hasProductAccess(#productId) or hasRole('ADMIN')")
    public UndoRedoHistory replayPreview(@PathVariable UUID productId, @PathVariable UUID operationId,
            @org.springframework.web.bind.annotation.RequestParam boolean undo) {
        UndoRedoHistory history = new UndoRedoHistory();
        history.setOperations(planningChangeService.replayPreview(productId, operationId, undo).stream().map(this::operation).toList());
        return history;
    }

    private UndoRedoHistory.Operation operation(UndoableOperationDAO source) {
        UndoRedoHistory.Operation target = new UndoRedoHistory.Operation();
        target.setId(source.getId());
        target.setActor(source.getActor());
        target.setCreated(source.getCreated());
        target.setEntityChanges(planningChangeService.entityChanges(source));
        target.setKind(source.getKind());
        target.setProductId(source.getProductId());
        target.setProductName(productRepository.findById(source.getProductId())
                .map(product -> product.getName())
                .orElse(source.getProductId().toString()));
        target.setSequenceNumber(source.getSequenceNumber());
        target.setSummary(source.getSummary());
        target.setUndone(source.isUndone());
        return target;
    }
}
