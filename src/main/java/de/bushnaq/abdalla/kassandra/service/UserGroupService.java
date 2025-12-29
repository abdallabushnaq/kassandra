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
import de.bushnaq.abdalla.kassandra.dao.UserGroupDAO;
import de.bushnaq.abdalla.kassandra.repository.UserGroupRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing user groups for ACL.
 * This service handles group creation, updates, and member management.
 */
@Service
@Slf4j
public class UserGroupService {

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Add user to group
     *
     * @param groupId the group ID
     * @param userId  the user ID
     * @throws EntityNotFoundException if group or user not found
     */
    @Transactional
    @CacheEvict(value = "userGroups", allEntries = true)
    public void addUserToGroup(Long groupId, Long userId) {
        UserGroupDAO group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));
        UserDAO user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        group.addMember(user);
        userGroupRepository.save(group);
        log.info("Added user {} to group {}", user.getName(), group.getName());
    }

    /**
     * Create a new user group (admin only)
     *
     * @param name        the group name
     * @param description the group description
     * @param memberIds   the IDs of users to add as members
     * @return the created group
     * @throws IllegalArgumentException if group name already exists
     */
    @Transactional
    @CacheEvict(value = "userGroups", allEntries = true)
    public UserGroupDAO createGroup(String name, String description, Set<Long> memberIds) {
        if (userGroupRepository.existsByName(name)) {
            throw new IllegalArgumentException("Group with name '" + name + "' already exists");
        }

        UserGroupDAO group = new UserGroupDAO();
        group.setName(name);
        group.setDescription(description);

        if (memberIds != null && !memberIds.isEmpty()) {
            Set<UserDAO> members = new HashSet<>(userRepository.findAllById(memberIds));
            group.setMembers(members);
        }

        UserGroupDAO savedGroup = userGroupRepository.save(group);
        log.info("Created user group: {} with {} members", name, savedGroup.getMemberCount());
        return savedGroup;
    }

    /**
     * Delete a group
     *
     * @param groupId the group ID
     */
    @Transactional
    @CacheEvict(value = "userGroups", allEntries = true)
    public void deleteGroup(Long groupId) {
        userGroupRepository.deleteById(groupId);
        log.info("Deleted user group: {}", groupId);
    }

    /**
     * Get all groups
     *
     * @return list of all groups
     */
    @Cacheable(value = "userGroups", key = "'all'")
    public List<UserGroupDAO> getAllGroups() {
        return userGroupRepository.findAll();
    }

    /**
     * Get a single group by ID
     *
     * @param groupId the group ID
     * @return the group
     * @throws EntityNotFoundException if group not found
     */
    @Cacheable(value = "userGroups", key = "'group-' + #groupId")
    public UserGroupDAO getGroup(Long groupId) {
        return userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));
    }

    /**
     * Get groups for a specific user
     *
     * @param userId the user ID
     * @return list of groups the user belongs to
     */
    @Cacheable(value = "userGroups", key = "'user-' + #userId")
    public List<UserGroupDAO> getGroupsForUser(Long userId) {
        return userGroupRepository.findGroupsByUserId(userId);
    }

    /**
     * Remove user from group
     *
     * @param groupId the group ID
     * @param userId  the user ID
     * @throws EntityNotFoundException if group not found
     */
    @Transactional
    @CacheEvict(value = "userGroups", allEntries = true)
    public void removeUserFromGroup(Long groupId, Long userId) {
        UserGroupDAO group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));

        group.getMembers().removeIf(user -> user.getId().equals(userId));
        userGroupRepository.save(group);
        log.info("Removed user {} from group {}", userId, group.getName());
    }

    /**
     * Update an existing group
     *
     * @param groupId     the group ID
     * @param name        the new name
     * @param description the new description
     * @param memberIds   the new set of member IDs
     * @return the updated group
     * @throws EntityNotFoundException  if group not found
     * @throws IllegalArgumentException if new name conflicts with another group
     */
    @Transactional
    @CacheEvict(value = "userGroups", allEntries = true)
    public UserGroupDAO updateGroup(Long groupId, String name, String description, Set<Long> memberIds) {
        UserGroupDAO group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));

        if (!group.getName().equals(name) && userGroupRepository.existsByName(name)) {
            throw new IllegalArgumentException("Group with name '" + name + "' already exists");
        }

        group.setName(name);
        group.setDescription(description);

        Set<UserDAO> members = new HashSet<>(userRepository.findAllById(memberIds));
        group.setMembers(members);

        UserGroupDAO updatedGroup = userGroupRepository.save(group);
        log.info("Updated user group: {} now has {} members", name, updatedGroup.getMemberCount());
        return updatedGroup;
    }
}

