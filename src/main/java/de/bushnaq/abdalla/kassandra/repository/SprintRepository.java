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

import de.bushnaq.abdalla.kassandra.dao.SprintDAO;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface SprintRepository extends ListCrudRepository<SprintDAO, Long> {
    boolean existsByName(String name);

    boolean existsByNameAndFeatureId(String name, Long featureId);

    /**
     * Check if a sprint with the given name and featureId exists, excluding the sprint with the specified ID.
     *
     * @param name      The sprint name to check
     * @param featureId The feature ID
     * @param id        The ID of the sprint to exclude from the check
     * @return true if another sprint with this name and featureId exists, false otherwise
     */
    boolean existsByNameAndFeatureIdAndIdNot(String name, Long featureId, Long id);

    List<SprintDAO> findByFeatureId(Long featureId);

    SprintDAO findByName(String name);

    SprintDAO findByNameAndFeatureId(String name, Long featureId);
}