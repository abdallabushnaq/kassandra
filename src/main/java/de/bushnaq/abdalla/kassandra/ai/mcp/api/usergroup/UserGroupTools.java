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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.usergroup;

import de.bushnaq.abdalla.kassandra.ai.mcp.ToolActivityContextHolder;
import de.bushnaq.abdalla.kassandra.dto.UserGroup;
import de.bushnaq.abdalla.kassandra.rest.api.UserGroupApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring AI native tool implementations for UserGroup operations.
 * <p>
 * User groups are used for product ACL management. A group can contain multiple users,
 * and access to a product can be granted to an entire group at once.
 * <p>
 * Example usage:
 * 1. Use getAllUserGroups() to list all existing groups.
 * 2. Use createUserGroup(name, description) to create a new group.
 * 3. Use addMemberToGroup(groupId, userId) to add a user to a group.
 * 4. Use removeMemberFromGroup(groupId, userId) to remove a user from a group.
 * 5. Use deleteUserGroup(groupId) to delete a group.
 * <p>
 * Uses @Tool annotation for automatic tool registration with ChatClient.
 */
@Component
@Slf4j
public class UserGroupTools {

    @Autowired
    @Qualifier("aiUserGroupApi")
    private UserGroupApi userGroupApi;

    @Tool(description = "Add a user to a group (requires ADMIN).")
    public void addMemberToGroup(
            @ToolParam(description = "The groupId") Long groupId,
            @ToolParam(description = "The userId to add") Long userId) {
        userGroupApi.addMember(groupId, userId);
        ToolActivityContextHolder.reportActivity("Added user " + userId + " to group " + groupId);
    }

    @Tool(description = "Create a new user group (requires ADMIN).")
    public UserGroupDto createUserGroup(
            @ToolParam(description = "Unique group name") String name,
            @ToolParam(description = "Group description", required = false) String description) {
        UserGroup group = userGroupApi.create(name, description, new HashSet<>());
        ToolActivityContextHolder.reportActivity("Created user group '" + group.getName() + "' with ID: " + group.getId());
        return UserGroupDto.from(group);
    }

    @Tool(description = "Delete a user group by its groupId (requires ADMIN).")
    public void deleteUserGroup(
            @ToolParam(description = "The groupId") Long groupId) {
        UserGroup group = userGroupApi.getById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("User group not found with ID: " + groupId);
        }
        ToolActivityContextHolder.reportActivity("Deleting user group '" + group.getName() + "' (ID: " + groupId + ")");
        userGroupApi.deleteById(groupId);
        ToolActivityContextHolder.reportActivity("Deleted user group '" + group.getName() + "' (ID: " + groupId + ")");
    }

    @Tool(description = "Get all user groups (requires ADMIN).")
    public List<UserGroupDto> getAllUserGroups() {
        List<UserGroup> groups = userGroupApi.getAll();
        ToolActivityContextHolder.reportActivity("Found " + groups.size() + " user groups.");
        return groups.stream().map(UserGroupDto::from).collect(Collectors.toList());
    }

    @Tool(description = "Get a user group by its groupId (requires ADMIN).")
    public UserGroupDto getUserGroupById(
            @ToolParam(description = "The groupId") Long groupId) {
        UserGroup group = userGroupApi.getById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("User group not found with ID: " + groupId);
        }
        return UserGroupDto.from(group);
    }

    @Tool(description = "Get a user group by its name (requires ADMIN).")
    public UserGroupDto getUserGroupByName(
            @ToolParam(description = "The group name") String name) {
        return userGroupApi.getByName(name)
                .map(UserGroupDto::from)
                .orElseThrow(() -> new IllegalArgumentException("User group not found with name: " + name));
    }

    @Tool(description = "Remove a user from a group (requires ADMIN).")
    public void removeMemberFromGroup(
            @ToolParam(description = "The groupId") Long groupId,
            @ToolParam(description = "The userId to remove") Long userId) {
        userGroupApi.removeMember(groupId, userId);
        ToolActivityContextHolder.reportActivity("Removed user " + userId + " from group " + groupId);
    }

    @Tool(description = "Update a user group's name and/or description (requires ADMIN).")
    public UserGroupDto updateUserGroup(
            @ToolParam(description = "The groupId") Long groupId,
            @ToolParam(description = "New group name") String name,
            @ToolParam(description = "New group description", required = false) String description) {
        UserGroup existing = userGroupApi.getById(groupId);
        if (existing == null) {
            throw new IllegalArgumentException("User group not found with ID: " + groupId);
        }
        Set<Long> memberIds = existing.getMemberIds() != null ? existing.getMemberIds() : new HashSet<>();
        UserGroup updated   = userGroupApi.update(groupId, name, description, memberIds);
        ToolActivityContextHolder.reportActivity("Updated user group '" + updated.getName() + "' (ID: " + groupId + ")");
        return UserGroupDto.from(updated);
    }
}
