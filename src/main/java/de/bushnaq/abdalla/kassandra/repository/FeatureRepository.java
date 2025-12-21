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

import de.bushnaq.abdalla.kassandra.dao.FeatureDAO;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface FeatureRepository extends ListCrudRepository<FeatureDAO, Long> {
    boolean existsByName(String name);

    boolean existsByNameAndVersionId(String name, Long versionId);

    /**
     * Check if a feature with the given name and versionId exists, excluding the feature with the specified ID.
     *
     * @param name      The feature name to check
     * @param versionId The version ID
     * @param id        The ID of the feature to exclude from the check
     * @return true if another feature with this name and versionId exists, false otherwise
     */
    boolean existsByNameAndVersionIdAndIdNot(String name, Long versionId, Long id);

    FeatureDAO findByName(String name);

    FeatureDAO findByNameAndVersionId(String name, Long versionId);

    List<FeatureDAO> findByVersionId(Long versionId);
}