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
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service to handle migration of existing users and initial admin setup.
 * Runs automatically at application startup.
 */
@Service
@Slf4j
public class UserRoleMigrationService {

    @Value("${kassandra.security.initial-admin-email:}")
    private String initialAdminEmail;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a default admin user with the given email
     *
     * @param email the email for the admin user
     */
    private void createDefaultAdmin(String email) {
        UserDAO admin = new UserDAO();
        admin.setEmail(email);
        admin.setName("Admin User");
        admin.setRoles("ADMIN,USER");
        admin.setColor(Color.BLUE);
        admin.setFirstWorkingDay(LocalDate.now());

        userRepository.save(admin);
        log.info("Created default admin user with email: {}", email);
        log.info("⚠️  IMPORTANT: This user needs to log in via OIDC with email: {}", email);
    }

    /**
     * Ensure at least one admin exists in the system.
     * If no admin exists, create one using the configured email or create a default user.
     */
    private void ensureAdminExists() {
        // Check if any admin exists
        boolean hasAdmin = userRepository.findAll().stream()
                .anyMatch(u -> u.getRoles() != null && u.getRoles().contains("ADMIN"));

        if (hasAdmin) {
            log.info("Admin user(s) already exist in the system.");
            return;
        }

        log.warn("No admin users found in the system!");

        // Try to use configured initial admin email
        if (initialAdminEmail != null && !initialAdminEmail.isEmpty()) {
            Optional<UserDAO> userOpt = userRepository.findByEmail(initialAdminEmail);
            if (userOpt.isPresent()) {
                // User exists, promote to admin
                UserDAO user = userOpt.get();
                user.addRole("ADMIN");
                userRepository.save(user);
                log.info("Promoted existing user {} to ADMIN role", user.getEmail());
            } else {
                // Create new admin user
                createDefaultAdmin(initialAdminEmail);
            }
        } else {
            log.error("No initial admin email configured!");
            log.error("Please set 'kassandra.security.initial-admin-email' in application.properties");
            log.error("Or manually create an admin user in the database");
        }
    }

    /**
     * Run migration at startup to ensure all users have roles
     * and at least one admin exists
     */
    @PostConstruct
    public void migrateExistingUsers() {
        log.info("Starting user role migration...");

        // 1. Assign USER role to all users without roles
        List<UserDAO> usersWithoutRoles = userRepository.findAll().stream()
                .filter(user -> user.getRoles() == null || user.getRoles().isEmpty())
                .toList();

        if (!usersWithoutRoles.isEmpty()) {
            log.info("Found {} users without roles. Assigning USER role...", usersWithoutRoles.size());
            usersWithoutRoles.forEach(user -> {
                user.setRoles("USER");
                userRepository.save(user);
                log.info("Assigned USER role to user: {}", user.getEmail());
            });
        }

        // 2. Ensure at least one admin exists
        ensureAdminExists();

        log.info("User role migration completed.");
    }
}

