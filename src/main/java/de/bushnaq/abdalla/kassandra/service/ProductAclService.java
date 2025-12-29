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
    private UserGroupRepository       userGroupRepository;
    @Autowired
    private UserRepository            userRepository;

    /**
     * Delete all ACL entries for a product
     * This should be called when a product is deleted
     *
     * @param productId the product ID
     */
    @Transactional
    @CacheEvict(value = "productAcl", allEntries = true)
    public void deleteProductAcl(Long productId) {
        aclRepository.deleteByProductId(productId);
        log.info("Deleted all ACL entries for product {}", productId);
    }

    /**
     * Get all product IDs accessible by user
     *
     * @param userId the user ID
     * @return list of accessible product IDs
     */
    @Cacheable(value = "productAcl", key = "'products-' + #userId")
    public List<Long> getAccessibleProductIds(Long userId) {
        return aclRepository.findProductIdsByUserAccess(userId);
    }

    /**
     * Get all ACL entries for a product
     *
     * @param productId the product ID
     * @return list of ACL entries
     */
    public List<ProductAclEntryDAO> getProductAcl(Long productId) {
        return aclRepository.findByProductId(productId);
    }

    /**
     * Automatically grant access to product creator
     * This should be called when a new product is created
     *
     * @param productId     the product ID
     * @param creatorUserId the user ID of the creator
     */
    @Transactional
    @CacheEvict(value = "productAcl", allEntries = true)
    public void grantCreatorAccess(Long productId, Long creatorUserId) {
        if (!aclRepository.existsByProductIdAndUserId(productId, creatorUserId)) {
            ProductAclEntryDAO entry = new ProductAclEntryDAO();
            entry.setProductId(productId);
            entry.setUserId(creatorUserId);
            aclRepository.save(entry);
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
    @CacheEvict(value = "productAcl", allEntries = true)
    public ProductAclEntryDAO grantGroupAccess(Long productId, Long groupId) {
        validateProductExists(productId);
        validateGroupExists(groupId);

        if (aclRepository.existsByProductIdAndGroupId(productId, groupId)) {
            throw new IllegalArgumentException("Group already has access to this product");
        }

        ProductAclEntryDAO entry = new ProductAclEntryDAO();
        entry.setProductId(productId);
        entry.setGroupId(groupId);

        ProductAclEntryDAO savedEntry = aclRepository.save(entry);
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
    @CacheEvict(value = "productAcl", allEntries = true)
    public ProductAclEntryDAO grantUserAccess(Long productId, Long userId) {
        validateProductExists(productId);
        validateUserExists(userId);

        if (aclRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new IllegalArgumentException("User already has access to this product");
        }

        ProductAclEntryDAO entry = new ProductAclEntryDAO();
        entry.setProductId(productId);
        entry.setUserId(userId);

        ProductAclEntryDAO savedEntry = aclRepository.save(entry);
        log.info("Granted user {} access to product {}", userId, productId);
        return savedEntry;
    }

    /**
     * Check if user has access to product by email (convenience method)
     *
     * @param productId the product ID
     * @param userEmail the user email
     * @return true if user has access
     */
    public boolean hasAccess(Long productId, String userEmail) {
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
    public boolean hasUserAccess(Long productId, Long userId) {
        return aclRepository.hasUserAccessToProduct(productId, userId);
    }

    /**
     * Revoke group access
     *
     * @param productId the product ID
     * @param groupId   the group ID
     */
    @Transactional
    @CacheEvict(value = "productAcl", allEntries = true)
    public void revokeGroupAccess(Long productId, Long groupId) {
        aclRepository.deleteByProductIdAndGroupId(productId, groupId);
        log.info("Revoked group {} access to product {}", groupId, productId);
    }

    /**
     * Revoke user access
     *
     * @param productId the product ID
     * @param userId    the user ID
     */
    @Transactional
    @CacheEvict(value = "productAcl", allEntries = true)
    public void revokeUserAccess(Long productId, Long userId) {
        aclRepository.deleteByProductIdAndUserId(productId, userId);
        log.info("Revoked user {} access to product {}", userId, productId);
    }

    private void validateGroupExists(Long groupId) {
        if (!userGroupRepository.existsById(groupId)) {
            throw new EntityNotFoundException("Group not found: " + groupId);
        }
    }

    private void validateProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
    }
}

