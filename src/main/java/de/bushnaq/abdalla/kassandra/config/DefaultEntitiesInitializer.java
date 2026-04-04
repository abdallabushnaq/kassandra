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

import java.time.LocalTime;
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

    public static final String                    ALL_USERS_GROUP_NAME  = "All";
    public static final String                    BACKLOG_SPRINT_NAME   = "Backlog";
    public static final String                    DEFAULT_NAME          = "Default";
    public static final String                    WORK_WEEK_5X8         = "Western 5x8";
    public static final String                    WORK_WEEK_ISLAMIC_5X8 = "Islamic Sun-Thu 5x8";
    public static final String                    WORK_WEEK_JEWISH_5X8  = "Jewish Sun-Thu 5x8";
    @Autowired
    private             FeatureRepository         featureRepository;
    @Autowired
    private             ProductAclEntryRepository productAclEntryRepository;
    @Autowired
    private             ProductAclService         productAclService;
    @Autowired
    private             ProductRepository         productRepository;
    @Autowired
    private             SprintRepository          sprintRepository;
    @Autowired
    private             UserGroupRepository       userGroupRepository;
    @Autowired
    private             UserRepository            userRepository;
    @Autowired
    private             VersionRepository         versionRepository;
    @Autowired
    private             WorkWeekRepository        workWeekRepository;

    private WorkDayScheduleDAO buildSchedule(boolean working,
                                             LocalTime workStart, LocalTime workEnd, LocalTime lunchStart, LocalTime lunchEnd) {
        if (working) return new WorkDayScheduleDAO(workStart, workEnd, lunchStart, lunchEnd);
        return new WorkDayScheduleDAO();
    }

    /**
     * Creates a named work week with the given day pattern if it does not already exist.
     * All working days use 08:00–17:00 with a 12:00–13:00 lunch break.
     *
     * @param name        unique work week name
     * @param description human-readable description
     * @param mon         whether Monday is a working day
     * @param tue         whether Tuesday is a working day
     * @param wed         whether Wednesday is a working day
     * @param thu         whether Thursday is a working day
     * @param fri         whether Friday is a working day
     * @param sat         whether Saturday is a working day
     * @param sun         whether Sunday is a working day
     */
    private void createDefaultWorkWeekIfAbsent(String name, String description,
                                               boolean mon, boolean tue, boolean wed, boolean thu, boolean fri, boolean sat, boolean sun) {
        if (workWeekRepository.findByName(name).isEmpty()) {
            LocalTime workStart  = LocalTime.of(8, 0);
            LocalTime workEnd    = LocalTime.of(17, 0);
            LocalTime lunchStart = LocalTime.of(12, 0);
            LocalTime lunchEnd   = LocalTime.of(13, 0);

            WorkWeekDAO ww = new WorkWeekDAO();
            ww.setName(name);
            ww.setDescription(description);
            ww.setMonday(buildSchedule(mon, workStart, workEnd, lunchStart, lunchEnd));
            ww.setTuesday(buildSchedule(tue, workStart, workEnd, lunchStart, lunchEnd));
            ww.setWednesday(buildSchedule(wed, workStart, workEnd, lunchStart, lunchEnd));
            ww.setThursday(buildSchedule(thu, workStart, workEnd, lunchStart, lunchEnd));
            ww.setFriday(buildSchedule(fri, workStart, workEnd, lunchStart, lunchEnd));
            ww.setSaturday(buildSchedule(sat, workStart, workEnd, lunchStart, lunchEnd));
            ww.setSunday(buildSchedule(sun, workStart, workEnd, lunchStart, lunchEnd));
            workWeekRepository.save(ww);
            log.info("Created default work week '{}'", name);
        }
    }

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

        // Create default work weeks if they don't exist
        createDefaultWorkWeekIfAbsent(WORK_WEEK_5X8,
                "Standard Monday–Friday 8-hour work week with a 1-hour lunch break",
                true, true, true, true, true, false, false);
        createDefaultWorkWeekIfAbsent(WORK_WEEK_ISLAMIC_5X8,
                "Sunday–Thursday 8-hour work week (common in Arab countries) with a 1-hour lunch break",
                true, true, true, true, true, false, true);
        createDefaultWorkWeekIfAbsent(WORK_WEEK_JEWISH_5X8,
                "Sunday–Thursday 8-hour work week (Israeli work week) with a 1-hour lunch break",
                true, true, true, true, true, false, true);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createMissingEntities();
    }
}
