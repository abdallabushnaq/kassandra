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

package de.bushnaq.abdalla.kassandra.rest.controller;

import de.bushnaq.abdalla.kassandra.dao.UserGroupDAO;
import de.bushnaq.abdalla.kassandra.repository.UserGroupRepository;
import de.bushnaq.abdalla.kassandra.service.UserGroupService;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REST controller for managing user groups.
 * All operations require ADMIN role.
 */
@RestController
@RequestMapping("/api/user-group")
@Slf4j
public class UserGroupController {

    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    private UserGroupService    userGroupService;

    /**
     * Add a user to a group
     *
     * @param groupId the group ID
     * @param userId  the user ID
     */
    @PostMapping("/{groupId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void addMember(@PathVariable Long groupId, @PathVariable Long userId) {
        log.info("Adding user {} to group {}", userId, groupId);
        userGroupService.addUserToGroup(groupId, userId);
    }

    /**
     * Create a new user group
     *
     * @param request the group creation request
     * @return the created group
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserGroupDAO create(@RequestBody UserGroupRequest request) {
        log.info("Creating user group: {}", request.getName());
        return userGroupService.createGroup(
                request.getName(),
                request.getDescription(),
                request.getMemberIds()
        );
    }

    /**
     * Delete a user group
     *
     * @param id the group ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable Long id) {
        log.info("Deleting user group: {}", id);
        userGroupService.deleteGroup(id);
    }

    /**
     * Get a single user group by ID
     *
     * @param id the group ID
     * @return the group
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<UserGroupDAO> get(@PathVariable Long id) {
        return userGroupRepository.findById(id);
    }

    /**
     * Get all user groups
     *
     * @return list of all groups
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserGroupDAO> getAll() {
        return userGroupService.getAllGroups();
    }

    /**
     * Remove a user from a group
     *
     * @param groupId the group ID
     * @param userId  the user ID
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        log.info("Removing user {} from group {}", userId, groupId);
        userGroupService.removeUserFromGroup(groupId, userId);
    }

    /**
     * Update an existing user group
     *
     * @param id      the group ID
     * @param request the group update request
     * @return the updated group
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserGroupDAO update(@PathVariable Long id, @RequestBody UserGroupRequest request) {
        log.info("Updating user group: {}", id);
        return userGroupService.updateGroup(
                id,
                request.getName(),
                request.getDescription(),
                request.getMemberIds()
        );
    }
}

/**
 * Request DTO for creating/updating user groups
 */
@Data
class UserGroupRequest {
    private String    description;
    private Set<Long> memberIds;
    private String    name;
}

