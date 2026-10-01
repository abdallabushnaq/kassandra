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

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.dao.ProductAclEntryDAO;
import de.bushnaq.abdalla.kassandra.repository.ProductAclEntryRepository;
import de.bushnaq.abdalla.kassandra.repository.ProductRepository;
import de.bushnaq.abdalla.kassandra.repository.UserGroupRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing Product Access Control Lists.
 * This service handles granting and revoking access to products for users and groups.
 */
@Service
@Slf4j
public class ProductAclService {

    @Autowired
    private ProductAclEntryRepository aclRepository;
    @Autowired
    private ProductRepository         productRepository;
    @Autowired
    private PlanningChangeService     planningChangeService;
    @Autowired
    private UserGroupRepository       userGroupRepository;
    @Autowired
    private UserRepository            userRepository;

    /**
     * Get all product IDs accessible by user
     *
     * @param userId the user ID
     * @return list of accessible product IDs
     */
    @Cacheable(value = "productAcl", key = "'products-' + #userId")
    public List<UUID> getAccessibleProductIds(UUID userId) {
        return aclRepository.findProductIdsByUserAccess(userId);
    }

    /**
     * Get all ACL entries for a product
     *
     * @param productId the product ID
     * @return list of ACL entries
     */
    public List<ProductAclEntryDAO> getProductAcl(UUID productId) {
        return aclRepository.findByProductId(productId);
    }

    /**
     * Automatically grant access to product creator
     *
     * @param productId     the product ID
     * @param creatorUserId the user ID of the creator
     */
    @Transactional
    public void grantCreatorAccess(UUID productId, UUID creatorUserId) {
        if (!aclRepository.existsByProductIdAndUserId(productId, creatorUserId)) {
            ProductAclEntryDAO entry = new ProductAclEntryDAO();
            entry.setProductId(productId);
            entry.setUserId(creatorUserId);
            planningChangeService.persist(entry, "Granted creator access");
            log.info("Granted creator access to product {} for user {}", productId, creatorUserId);
        }
    }

    /**
     * Grant access to a group for a product
     *
     * @param productId the product ID
     * @param groupId   the group ID
     * @return the created ACL entry
     * @throws EntityNotFoundException  if product or group not found
     * @throws IllegalArgumentException if group already has access
     */
    @Transactional
    public ProductAclEntryDAO grantGroupAccess(UUID productId, UUID groupId) {
        ProductAclEntryDAO savedEntry = planningChangeService.persist(groupEntry(productId, groupId), "Granted group access");
        log.info("Granted group {} access to product {}", groupId, productId);
        return savedEntry;
    }

    /**
     * Grant access to a user for a product
     *
     * @param productId the product ID
     * @param userId    the user ID
     * @return the created ACL entry
     * @throws EntityNotFoundException  if product or user not found
     * @throws IllegalArgumentException if user already has access
     */
    @Transactional
    public ProductAclEntryDAO grantUserAccess(UUID productId, UUID userId) {
        validateProductExists(productId);
        validateUserExists(userId);

        if (aclRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new IllegalArgumentException("User already has access to this product");
        }

        ProductAclEntryDAO entry = new ProductAclEntryDAO();
        entry.setProductId(productId);
        entry.setUserId(userId);

        ProductAclEntryDAO savedEntry = planningChangeService.persist(entry, "Granted user access");
        log.info("Granted user {} access to product {}", userId, productId);
        return savedEntry;
    }

    /**
     * Audits the default product's bootstrap permission without adding user-visible history.
     *
     * @param productId the default product ID
     * @param groupId   the all-users group ID
     * @throws EntityNotFoundException  if the product or group does not exist
     * @throws IllegalArgumentException if the group already has access
     */
    @Transactional
    @CacheEvict(value = "productAcl", allEntries = true)
    public void initializeDefaultGroupAccess(UUID productId, UUID groupId) {
        aclRepository.save(groupEntry(productId, groupId));
    }

    /**
     * Check if user has access to product by email (convenience method)
     *
     * @param productId the product ID
     * @param userEmail the user email
     * @return true if user has access
     */
    public boolean hasAccess(UUID productId, String userEmail) {
        return userRepository.findByEmail(userEmail)
                .map(user -> hasUserAccess(productId, user.getId()))
                .orElse(false);
    }

    /**
     * Check if user has access to product (either directly or through groups)
     *
     * @param productId the product ID
     * @param userId    the user ID
     * @return true if user has access
     */
    @Cacheable(value = "productAcl", key = "'access-' + #productId + '-' + #userId")
    public boolean hasUserAccess(UUID productId, UUID userId) {
        return aclRepository.hasUserAccessToProduct(productId, userId);
    }

    /**
     * Revoke group access
     *
     * @param productId the product ID
     * @param groupId   the group ID
     */
    @Transactional
    public void revokeGroupAccess(UUID productId, UUID groupId) {
        aclRepository.findByProductIdAndGroupId(productId, groupId)
                .ifPresent(entry -> planningChangeService.delete(ProductAclEntryDAO.class, entry.getId(), "Revoked group access"));
        log.info("Revoked group {} access to product {}", groupId, productId);
    }

    /**
     * Revoke user access
     *
     * @param productId the product ID
     * @param userId    the user ID
     */
    @Transactional
    public void revokeUserAccess(UUID productId, UUID userId) {
        aclRepository.findByProductIdAndUserId(productId, userId)
                .ifPresent(entry -> planningChangeService.delete(ProductAclEntryDAO.class, entry.getId(), "Revoked user access"));
        log.info("Revoked user {} access to product {}", userId, productId);
    }

    private void validateGroupExists(UUID groupId) {
        if (!userGroupRepository.existsById(groupId)) {
            throw new EntityNotFoundException("Group not found: " + groupId);
        }
    }

    private ProductAclEntryDAO groupEntry(UUID productId, UUID groupId) {
        validateProductExists(productId);
        validateGroupExists(groupId);
        if (aclRepository.existsByProductIdAndGroupId(productId, groupId)) {
            throw new IllegalArgumentException("Group already has access to this product");
        }
        ProductAclEntryDAO entry = new ProductAclEntryDAO();
        entry.setProductId(productId);
        entry.setGroupId(groupId);
        return entry;
    }

    private void validateProductExists(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }
    }

    private void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
    }
}
