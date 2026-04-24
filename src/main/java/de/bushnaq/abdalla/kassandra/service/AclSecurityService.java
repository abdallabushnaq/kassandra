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

import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.dao.VersionDAO;
import de.bushnaq.abdalla.kassandra.repository.*;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for ACL security checks.
 * Used in @PreAuthorize expressions to control access to products and their child entities.
 */
@Service
@Slf4j
public class AclSecurityService {

    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private ProductAclService productAclService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SprintRepository  sprintRepository;
    @Autowired
    private TaskRepository    taskRepository;
    @Autowired
    private UserRepository    userRepository;
    @Autowired
    private VersionRepository versionRepository;

    /**
     * Check if current user can manage ACL for a product
     * Only admins or users with existing access can manage ACL
     *
     * @param productId the product ID
     * @return true if user can manage ACL
     */
    public boolean canManageProductAcl(UUID productId) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        return hasProductAccess(productId);
    }

    /**
     * Get the product ID for a feature
     *
     * @param featureId the feature ID
     * @return the product ID, or null if not found
     */
    public UUID getProductIdForFeature(UUID featureId) {
        return featureRepository.findById(featureId)
                .flatMap(feature -> versionRepository.findById(feature.getVersionId()))
                .map(VersionDAO::getProductId)
                .orElse(null);
    }

    /**
     * Get the product ID for a sprint
     *
     * @param sprintId the sprint ID
     * @return the product ID, or null if not found
     */
    public UUID getProductIdForSprint(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .flatMap(sprint -> featureRepository.findById(sprint.getFeatureId()))
                .flatMap(feature -> versionRepository.findById(feature.getVersionId()))
                .map(VersionDAO::getProductId)
                .orElse(null);
    }

    /**
     * Get the product ID for a task
     *
     * @param taskId the task ID
     * @return the product ID, or null if not found
     */
    public UUID getProductIdForTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .flatMap(task -> sprintRepository.findById(task.getSprintId()))
                .flatMap(sprint -> featureRepository.findById(sprint.getFeatureId()))
                .flatMap(feature -> versionRepository.findById(feature.getVersionId()))
                .map(VersionDAO::getProductId)
                .orElse(null);
    }

    /**
     * Get the product ID for a version
     *
     * @param versionId the version ID
     * @return the product ID, or null if not found
     */
    public UUID getProductIdForVersion(UUID versionId) {
        return versionRepository.findById(versionId)
                .map(VersionDAO::getProductId)
                .orElse(null);
    }

    /**
     * Check if current user has access to a feature
     *
     * @param featureId the feature ID
     * @return true if user has access
     */
    public boolean hasFeatureAccess(UUID featureId) {
        return featureRepository.findById(featureId)
                .flatMap(feature -> versionRepository.findById(feature.getVersionId()))
                .map(VersionDAO::getProductId)
                .map(this::hasProductAccess)
                .orElse(false);
    }

    /**
     * Check if current user has access to a product
     *
     * @param productId the product ID
     * @return true if user has access
     */
    public boolean hasProductAccess(UUID productId) {
        String userEmail = SecurityUtils.getUserEmail();
        if (SecurityUtils.GUEST.equals(userEmail)) {
            return false;
        }

        // Admin has access to everything
        if (SecurityUtils.isAdmin()) {
            return true;
        }

        // Check ACL
        Optional<UserDAO> user = userRepository.findByEmail(userEmail);
        return user.isPresent() && productAclService.hasUserAccess(productId, user.get().getId());
    }

    /**
     * Check if current user has access to a product by its name.
     *
     * @param name the product name
     * @return true if user has access
     */
    public boolean hasProductAccessByName(String name) {
        var product = productRepository.findByName(name);
        if (product == null) {
            return false;
        }
        return hasProductAccess(product.getId());
    }

    /**
     * Check if current user has access to a sprint
     *
     * @param sprintId the sprint ID
     * @return true if user has access
     */
    public boolean hasSprintAccess(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .flatMap(sprint -> featureRepository.findById(sprint.getFeatureId()))
                .flatMap(feature -> versionRepository.findById(feature.getVersionId()))
                .map(VersionDAO::getProductId)
                .map(this::hasProductAccess)
                .orElse(false);
    }

    /**
     * Check if current user has access to a task
     *
     * @param taskId the task ID
     * @return true if user has access
     */
    public boolean hasTaskAccess(UUID taskId) {
        return taskRepository.findById(taskId)
                .flatMap(task -> sprintRepository.findById(task.getSprintId()))
                .flatMap(sprint -> featureRepository.findById(sprint.getFeatureId()))
                .flatMap(feature -> versionRepository.findById(feature.getVersionId()))
                .map(VersionDAO::getProductId)
                .map(this::hasProductAccess)
                .orElse(false);
    }

    /**
     * Check if current user has access to a version
     *
     * @param versionId the version ID
     * @return true if user has access
     */
    public boolean hasVersionAccess(UUID versionId) {
        return versionRepository.findById(versionId)
                .map(VersionDAO::getProductId)
                .map(this::hasProductAccess)
                .orElse(false);
    }
}

