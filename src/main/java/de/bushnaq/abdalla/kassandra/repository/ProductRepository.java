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

import de.bushnaq.abdalla.kassandra.dao.ProductDAO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends ListCrudRepository<ProductDAO, Long> {
    boolean existsByName(String name);

    /**
     * Check if a product with the given name exists, excluding the product with the specified ID.
     * Useful for update operations to check for name conflicts with other products.
     *
     * @param name The product name to check
     * @param id   The ID of the product to exclude from the check
     * @return true if another product with this name exists, false otherwise
     */
    boolean existsByNameAndIdNot(String name, Long id);

    ProductDAO findByName(String name);

    /**
     * Find products whose names contain the given string, ignoring case sensitivity.
     *
     * @param partialName The partial name to search for in product names
     * @return A list of products whose names contain the specified string (case-insensitive)
     */
    @Query("SELECT p FROM ProductDAO p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :partialName, '%'))")
    List<ProductDAO> findByNameContainingIgnoreCase(@Param("partialName") String partialName);
}