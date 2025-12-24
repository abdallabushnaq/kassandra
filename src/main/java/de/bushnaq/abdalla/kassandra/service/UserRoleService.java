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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing user roles stored in the database.
 * This service handles role assignment, validation, and synchronization.
 */
@Service
@Slf4j
public class UserRoleService {

    private static final List<String> VALID_ROLES = Arrays.asList("ADMIN", "USER");

    @Autowired
    private UserRepository userRepository;

    /**
     * Assign roles to a user
     *
     * @param userId the user ID
     * @param roles  list of roles to assign
     * @throws IllegalArgumentException if user not found or invalid roles provided
     * @throws IllegalStateException    if trying to remove last admin
     */
    @Transactional
    @CacheEvict(value = "userRoles", allEntries = true) // Clear cache when roles change
    public void assignRoles(Long userId, List<String> roles) {
        // Validate roles
        for (String role : roles) {
            if (!VALID_ROLES.contains(role)) {
                throw new IllegalArgumentException("Invalid role: " + role + ". Valid roles are: " + VALID_ROLES);
            }
        }

        // Get user
        UserDAO user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Check if this is the last admin and we're removing admin role
        boolean wasAdmin    = user.hasRole("ADMIN");
        boolean willBeAdmin = roles.contains("ADMIN");
        if (wasAdmin && !willBeAdmin) {
            // Check if there are other admins
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.hasRole("ADMIN"))
                    .count();
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot remove ADMIN role from the last admin user");
            }
        }

        // Assign roles
        user.setRoleList(roles);
        userRepository.save(user);
        log.info("Assigned roles {} to user {}", roles, user.getEmail());
    }

    /**
     * Ensure at least one admin exists in the system
     *
     * @return true if at least one admin exists, false otherwise
     */
    public boolean ensureAtLeastOneAdmin() {
        long adminCount = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ADMIN"))
                .count();
        return adminCount > 0;
    }

    /**
     * Get roles for a user by ID
     *
     * @param userId the user ID
     * @return list of roles
     * @throws IllegalArgumentException if user not found
     */
    public List<String> getRoles(Long userId) {
        UserDAO user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return user.getRoleList();
    }

    /**
     * Get roles for a user by email (used for OIDC users).
     * <p>
     * For UI: Called once during authentication by CustomOidcUserService,
     * then roles stored in SecurityContext for session.
     * <p>
     * For API: Called for each stateless REST request via JWT converter,
     * cached to avoid repeated database queries.
     *
     * @param email the user's email
     * @return list of roles, or empty list if user not found
     */
    @Cacheable(value = "userRoles", key = "#email")
    public List<String> getRolesByEmail(String email) {
        log.info("🔍 DATABASE QUERY: Loading roles from database for email: {}", email);

        Optional<UserDAO> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            List<String> roles = user.get().getRoleList();
            log.info("✅ User found: {} with roles: {}", user.get().getName(), String.join(", ", roles));
            return roles;
        }

        log.warn("❌ User NOT found for email: {}", email);
        return List.of(); // Empty list if user not found
    }
}

