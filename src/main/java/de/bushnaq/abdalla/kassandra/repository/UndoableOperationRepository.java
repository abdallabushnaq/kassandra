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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UndoableOperationRepository extends ListCrudRepository<UndoableOperationDAO, UUID> {
    List<UndoableOperationDAO> findByProductIdOrderBySequenceNumberDesc(UUID productId);

    List<UndoableOperationDAO> findByProductIdInOrderByCreatedDesc(Collection<UUID> productIds, Pageable pageable);

    Optional<UndoableOperationDAO> findFirstByProductIdAndUndoneFalseOrderBySequenceNumberDesc(UUID productId);

    Optional<UndoableOperationDAO> findFirstByProductIdAndUndoneTrueOrderBySequenceNumberAsc(UUID productId);

    @Query("SELECT COALESCE(MAX(o.sequenceNumber), 0) FROM UndoableOperationDAO o WHERE o.productId = :productId")
    long findMaxSequenceNumber(UUID productId);
}
