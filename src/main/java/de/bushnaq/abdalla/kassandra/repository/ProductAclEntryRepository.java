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

import de.bushnaq.abdalla.kassandra.dao.ProductAclEntryDAO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for managing Product ACL entries
 */
public interface ProductAclEntryRepository extends ListCrudRepository<ProductAclEntryDAO, Long> {

    /**
     * Delete all ACL entries for a product
     *
     * @param productId the product ID
     */
    void deleteByProductId(Long productId);

    /**
     * Delete a specific group's access to a product
     *
     * @param productId the product ID
     * @param groupId   the group ID
     */
    void deleteByProductIdAndGroupId(Long productId, Long groupId);

    /**
     * Delete a specific user's access to a product
     *
     * @param productId the product ID
     * @param userId    the user ID
     */
    void deleteByProductIdAndUserId(Long productId, Long userId);

    /**
     * Check if a specific group has access to a product
     *
     * @param productId the product ID
     * @param groupId   the group ID
     * @return true if the group has access
     */
    boolean existsByProductIdAndGroupId(Long productId, Long groupId);

    /**
     * Check if a specific user has direct access to a product
     *
     * @param productId the product ID
     * @param userId    the user ID
     * @return true if the user has direct access
     */
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    /**
     * Find all ACL entries for a product
     *
     * @param productId the product ID
     * @return list of ACL entries
     */
    List<ProductAclEntryDAO> findByProductId(Long productId);

    /**
     * Find all product IDs that a user has access to (either directly or through groups)
     *
     * @param userId the user ID
     * @return list of product IDs
     */
    @Query("SELECT DISTINCT acl.productId FROM ProductAclEntryDAO acl " +
            "WHERE acl.userId = :userId OR acl.groupId IN " +
            "(SELECT g.id FROM UserGroupDAO g JOIN g.members m WHERE m.id = :userId)")
    List<Long> findProductIdsByUserAccess(@Param("userId") Long userId);

    /**
     * Check if a user has access to a product (either directly or through groups)
     *
     * @param productId the product ID
     * @param userId    the user ID
     * @return true if the user has access
     */
    @Query("SELECT CASE WHEN COUNT(acl) > 0 THEN true ELSE false END " +
            "FROM ProductAclEntryDAO acl " +
            "WHERE acl.productId = :productId AND " +
            "(acl.userId = :userId OR acl.groupId IN " +
            "(SELECT g.id FROM UserGroupDAO g JOIN g.members m WHERE m.id = :userId))")
    boolean hasUserAccessToProduct(
            @Param("productId") Long productId,
            @Param("userId") Long userId
    );
}

