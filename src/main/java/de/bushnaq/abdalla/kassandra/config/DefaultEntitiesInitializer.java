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
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.config;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dao.*;
import de.bushnaq.abdalla.kassandra.dto.Status;
import de.bushnaq.abdalla.kassandra.repository.*;
import de.bushnaq.abdalla.kassandra.service.ProductAclService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * Initializes default entities on application startup.
 * Creates a default Product, Version, Feature, Backlog sprint, and "All" user group if they don't exist.
 * The Backlog sprint is a global sprint where new tasks are created from the Backlog view.
 * The "All" group contains all users and has access to the Default product.
 */
@Component
@Order(1)
@Slf4j
public class DefaultEntitiesInitializer implements ApplicationRunner {

    public static final String ALL_USERS_GROUP_NAME = "All";
    public static final String BACKLOG_SPRINT_NAME  = "Backlog";
    public static final String DEFAULT_NAME         = "Default";

    @Autowired
    private FeatureRepository         featureRepository;
    @Autowired
    private ProductAclEntryRepository productAclEntryRepository;
    @Autowired
    private ProductAclService         productAclService;
    @Autowired
    private ProductRepository         productRepository;
    @Autowired
    private SprintRepository          sprintRepository;
    @Autowired
    private UserGroupRepository       userGroupRepository;
    @Autowired
    private UserRepository            userRepository;
    @Autowired
    private VersionRepository         versionRepository;

    private void createMissingEntities() {
        // Find or create default product
        ProductDAO product = productRepository.findByName(DEFAULT_NAME);
        if (product == null) {
            product = new ProductDAO();
            product.setCreated(ParameterOptions.getNow());
            product.setUpdated(ParameterOptions.getNow());
            product.setName(DEFAULT_NAME);
            product = productRepository.save(product);
            log.info("Created default product with ID: {}", product.getId());
        }

        // Find or create default version
        VersionDAO version = versionRepository.findByNameAndProductId(DEFAULT_NAME, product.getId());
        if (version == null) {
            version = new VersionDAO();
            version.setCreated(ParameterOptions.getNow());
            version.setUpdated(ParameterOptions.getNow());
            version.setName(DEFAULT_NAME);
            version.setProductId(product.getId());
            version = versionRepository.save(version);
            log.info("Created default version with ID: {}", version.getId());
        }

        // Find or create default feature
        FeatureDAO feature = featureRepository.findByNameAndVersionId(DEFAULT_NAME, version.getId());
        if (feature == null) {
            feature = new FeatureDAO();
            feature.setCreated(ParameterOptions.getNow());
            feature.setUpdated(ParameterOptions.getNow());
            feature.setName(DEFAULT_NAME);
            feature.setVersionId(version.getId());
            feature = featureRepository.save(feature);
            log.info("Created default feature with ID: {}", feature.getId());
        }

        // Find or create backlog sprint (check by name globally, not per feature)
        SprintDAO backlogSprint = sprintRepository.findByName(BACKLOG_SPRINT_NAME);
        if (backlogSprint == null) {
            backlogSprint = new SprintDAO();
            backlogSprint.setCreated(ParameterOptions.getNow());
            backlogSprint.setUpdated(ParameterOptions.getNow());
            backlogSprint.setName(BACKLOG_SPRINT_NAME);
            backlogSprint.setFeatureId(feature.getId());
            backlogSprint.setStatus(Status.CREATED);
            backlogSprint = sprintRepository.save(backlogSprint);
            log.info("Created backlog sprint with ID: {}", backlogSprint.getId());
        }

        // Find or create "All" user group with all existing users
        UserGroupDAO allUsersGroup = userGroupRepository.findByName(ALL_USERS_GROUP_NAME).orElse(null);
        if (allUsersGroup == null) {
            allUsersGroup = new UserGroupDAO();
            allUsersGroup.setName(ALL_USERS_GROUP_NAME);
            allUsersGroup.setDescription("Contains all users. Automatically updated when users are created.");

            // Add all existing users to the group
            List<UserDAO> allUsers = userRepository.findAll();
            allUsersGroup.setMemberIds(new HashSet<>());
            for (UserDAO user : allUsers) {
                allUsersGroup.addMember(user.getId());
            }

            allUsersGroup = userGroupRepository.save(allUsersGroup);
            log.info("Created 'All' user group with ID: {} and {} members", allUsersGroup.getId(), allUsersGroup.getMemberCount());
        }

        // Grant "All" group access to the Default product (if not already granted)
        if (!productAclEntryRepository.existsByProductIdAndGroupId(product.getId(), allUsersGroup.getId())) {
            productAclService.grantGroupAccess(product.getId(), allUsersGroup.getId());
            log.info("Granted 'All' group access to Default product");
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createMissingEntities();
    }
}
