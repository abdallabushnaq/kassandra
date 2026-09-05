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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * REST client for product-scoped planning history and replay.
 */
@Service
public class UndoRedoApi extends AbstractApi {

    /**
     * Creates the API client for tests with an explicit base URL.
     *
     * @param restTemplate HTTP client
     * @param jsonMapper JSON mapper
     * @param baseUrl API base URL
     */
    public UndoRedoApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    /**
     * Creates the Spring-managed API client.
     *
     * @param restTemplate HTTP client
     * @param jsonMapper JSON mapper
     */
    @Autowired
    public UndoRedoApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    /**
     * Gets product history and current undo/redo availability.
     *
     * @param productId product ID
     * @return history state
     */
    public UndoRedoHistory history(UUID productId) {
        ResponseEntity<UndoRedoHistory> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{id}/history", HttpMethod.GET, createHttpEntity(), UndoRedoHistory.class, productId));
        return response.getBody();
    }

    /**
     * Gets a limited globally ordered history for multiple active products.
     *
     * @param productIds product IDs in the active page scope
     * @param limit maximum number of operations to retrieve
     * @return combined history state
     */
    public UndoRedoHistory history(Collection<UUID> productIds, int limit) {
        if (productIds.isEmpty()) {
            UndoRedoHistory history = new UndoRedoHistory();
            history.setOperations(java.util.List.of());
            return history;
        }
        String ids = productIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        ResponseEntity<UndoRedoHistory> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/history?productIds={productIds}&limit={limit}", HttpMethod.GET, createHttpEntity(),
                UndoRedoHistory.class, ids, limit));
        return response.getBody();
    }

    /**
     * Gets the operations that will be replayed by a selected undo or redo action.
     *
     * @param productId product ID
     * @param operationId selected operation ID
     * @param undo whether the selected action is undo
     * @return replay preview
     */
    public UndoRedoHistory replayPreview(UUID productId, UUID operationId, boolean undo) {
        ResponseEntity<UndoRedoHistory> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{productId}/history/{operationId}/preview?undo={undo}", HttpMethod.GET,
                createHttpEntity(), UndoRedoHistory.class, productId, operationId, undo));
        return response.getBody();
    }

    /**
     * Redoes the next undone product operation.
     *
     * @param productId product ID
     * @return updated history state
     */
    public UndoRedoHistory redo(UUID productId) {
        ResponseEntity<UndoRedoHistory> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{id}/redo", HttpMethod.POST, createHttpEntity(), UndoRedoHistory.class, productId));
        return response.getBody();
    }

    /**
     * Reapplies the consecutive undone operations through the selected operation.
     *
     * @param productId product ID
     * @param operationId last operation to reapply
     * @return updated history state
     */
    public UndoRedoHistory redoThrough(UUID productId, UUID operationId) {
        ResponseEntity<UndoRedoHistory> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{productId}/redo/{operationId}", HttpMethod.POST, createHttpEntity(),
                UndoRedoHistory.class, productId, operationId));
        return response.getBody();
    }

    /**
     * Undoes the latest applied product operation.
     *
     * @param productId product ID
     * @return updated history state
     */
    public UndoRedoHistory undo(UUID productId) {
        ResponseEntity<UndoRedoHistory> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{id}/undo", HttpMethod.POST, createHttpEntity(), UndoRedoHistory.class, productId));
        return response.getBody();
    }

    /**
     * Reverts the consecutive applied operations through the selected operation.
     *
     * @param productId product ID
     * @param operationId oldest operation to revert
     * @return updated history state
     */
    public UndoRedoHistory undoThrough(UUID productId, UUID operationId) {
        ResponseEntity<UndoRedoHistory> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{productId}/undo/{operationId}", HttpMethod.POST, createHttpEntity(),
                UndoRedoHistory.class, productId, operationId));
        return response.getBody();
    }
}
