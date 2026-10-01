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

package de.bushnaq.abdalla.kassandra.repository;

import de.bushnaq.abdalla.kassandra.dao.UndoableOperationDAO;
import de.bushnaq.abdalla.kassandra.dao.UndoableOperationEntryDAO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UndoableOperationRepository extends ListCrudRepository<UndoableOperationDAO, UUID> {

    /**
     * Finds all products that have journaled planning operations.
     *
     * @return product IDs with undo/redo history
     */
    @Query("SELECT DISTINCT o.productId FROM UndoableOperationDAO o")
    List<UUID> findDistinctProductIds();

    /**
     * Finds the most recent applied source entries for the product itself.
     *
     * @param productId  product whose lifecycle is inspected
     * @param entityType product entity class name
     * @param pageable   bounds the lifecycle lookup
     * @return applied product entries, newest first
     */
    @Query("""
            SELECT e FROM UndoableOperationEntryDAO e
            WHERE e.operation.productId = :productId AND e.entityId = :productId
              AND e.entityType = :entityType AND e.operation.undone = false
            ORDER BY e.operation.sequenceNumber DESC, e.revisionNumber DESC, e.restoreOrder DESC
            """)
    List<UndoableOperationEntryDAO> findAppliedProductEntries(UUID productId, String entityType, Pageable pageable);

    /**
     * Finds the earliest undone source entries for the product itself.
     *
     * @param productId  product whose lifecycle is inspected
     * @param entityType product entity class name
     * @param pageable   bounds the lifecycle lookup
     * @return undone product entries, oldest first
     */
    @Query("""
            SELECT e FROM UndoableOperationEntryDAO e
            WHERE e.operation.productId = :productId AND e.entityId = :productId
              AND e.entityType = :entityType AND e.operation.undone = true
            ORDER BY e.operation.sequenceNumber ASC, e.revisionNumber ASC, e.restoreOrder ASC
            """)
    List<UndoableOperationEntryDAO> findUndoneProductEntries(UUID productId, String entityType, Pageable pageable);

    /**
     * Finds permission entries captured at an operation's lifecycle revision.
     *
     * @param operationId    operation owning the lifecycle boundary
     * @param revisionNumber exact source revision
     * @param entityType     ACL entity class name
     * @return permission entries at that boundary
     */
    @Query("""
            SELECT e FROM UndoableOperationEntryDAO e
            WHERE e.operation.id = :operationId AND e.revisionNumber = :revisionNumber AND e.entityType = :entityType
            """)
    List<UndoableOperationEntryDAO> findLifecycleAclEntries(UUID operationId, int revisionNumber, String entityType);

    List<UndoableOperationDAO> findByProductIdOrderBySequenceNumberDesc(UUID productId);

    List<UndoableOperationDAO> findByProductIdInOrderByCreatedDesc(Collection<UUID> productIds, Pageable pageable);

    Optional<UndoableOperationDAO> findFirstByProductIdAndUndoneFalseOrderBySequenceNumberDesc(UUID productId);

    Optional<UndoableOperationDAO> findFirstByProductIdAndUndoneTrueOrderBySequenceNumberAsc(UUID productId);

    @Query("SELECT COALESCE(MAX(o.sequenceNumber), 0) FROM UndoableOperationDAO o WHERE o.productId = :productId")
    long findMaxSequenceNumber(UUID productId);
}
