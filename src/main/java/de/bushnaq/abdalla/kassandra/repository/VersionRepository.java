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

package de.bushnaq.abdalla.kassandra.repository;

import de.bushnaq.abdalla.kassandra.dao.VersionDAO;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.UUID;

public interface VersionRepository extends ListCrudRepository<VersionDAO, UUID> {
    boolean existsByName(String name);

    boolean existsByNameAndProductId(String name, UUID productId);

    /**
     * Check if a version with the given name and productId exists, excluding the version with the specified ID.
     *
     * @param name      The version name to check
     * @param productId The product ID
     * @param id        The ID of the version to exclude from the check
     * @return true if another version with this name and productId exists, false otherwise
     */
    boolean existsByNameAndProductIdAndIdNot(String name, UUID productId, UUID id);

    VersionDAO findByName(String name);

    VersionDAO findByNameAndProductId(String name, UUID productId);

    List<VersionDAO> findByProductId(UUID productId);
}