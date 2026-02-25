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
import tools.jackson.databind.json.JsonMapper;

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
    private JsonMapper   jsonMapper;
    @Autowired
    @Qualifier("aiUserGroupApi")
    private UserGroupApi userGroupApi;

    @Tool(description = "Add a user to a group (requires ADMIN).")
    public String addMemberToGroup(
            @ToolParam(description = "The groupId") Long groupId,
            @ToolParam(description = "The userId to add") Long userId) {
        try {
            ToolActivityContextHolder.reportActivity("Adding user " + userId + " to group " + groupId);
            userGroupApi.addMember(groupId, userId);
            ToolActivityContextHolder.reportActivity("Added user " + userId + " to group " + groupId);
            return "User " + userId + " added to group " + groupId + " successfully";
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error adding user to group: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Create a new user group (requires ADMIN).")
    public String createUserGroup(
            @ToolParam(description = "Unique group name") String name,
            @ToolParam(description = "Group description", required = false) String description) {
        try {
            ToolActivityContextHolder.reportActivity("Creating user group with name: " + name);
            UserGroup group = userGroupApi.create(name, description, new HashSet<>());
            ToolActivityContextHolder.reportActivity("Created user group '" + group.getName() + "' with ID: " + group.getId());
            return jsonMapper.writeValueAsString(UserGroupDto.from(group));
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error creating user group: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a user group by its groupId (requires ADMIN).")
    public String deleteUserGroup(
            @ToolParam(description = "The groupId") Long groupId) {
        try {
            ToolActivityContextHolder.reportActivity("Deleting user group with ID: " + groupId);
            userGroupApi.deleteById(groupId);
            ToolActivityContextHolder.reportActivity("Deleted user group with ID: " + groupId);
            return "User group deleted successfully with ID: " + groupId;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error deleting user group " + groupId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get all user groups (requires ADMIN).")
    public String getAllUserGroups() {
        try {
            List<UserGroup> groups = userGroupApi.getAll();
            ToolActivityContextHolder.reportActivity("Found " + groups.size() + " user groups.");
            List<UserGroupDto> dtos = groups.stream()
                    .map(UserGroupDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(dtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting all user groups: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a user group by its groupId (requires ADMIN).")
    public String getUserGroupById(
            @ToolParam(description = "The groupId") Long groupId) {
        try {
            ToolActivityContextHolder.reportActivity("Getting user group with ID: " + groupId);
            UserGroup group = userGroupApi.getById(groupId);
            if (group != null) {
                return jsonMapper.writeValueAsString(UserGroupDto.from(group));
            }
            return "User group not found with ID: " + groupId;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting user group " + groupId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a user group by its name (requires ADMIN).")
    public String getUserGroupByName(
            @ToolParam(description = "The group name") String name) {
        try {
            ToolActivityContextHolder.reportActivity("Getting user group with name: " + name);
            return userGroupApi.getByName(name)
                    .map(group -> {
                        try {
                            ToolActivityContextHolder.reportActivity("Found user group '" + group.getName() + "' with ID: " + group.getId());
                            return jsonMapper.writeValueAsString(UserGroupDto.from(group));
                        } catch (Exception e) {
                            return "Error serializing user group: " + e.getMessage();
                        }
                    })
                    .orElseGet(() -> {
                        ToolActivityContextHolder.reportActivity("No user group found with name: " + name);
                        return "User group not found with name: " + name;
                    });
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting user group by name '" + name + "': " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Remove a user from a group (requires ADMIN).")
    public String removeMemberFromGroup(
            @ToolParam(description = "The groupId") Long groupId,
            @ToolParam(description = "The userId to remove") Long userId) {
        try {
            ToolActivityContextHolder.reportActivity("Removing user " + userId + " from group " + groupId);
            userGroupApi.removeMember(groupId, userId);
            ToolActivityContextHolder.reportActivity("Removed user " + userId + " from group " + groupId);
            return "User " + userId + " removed from group " + groupId + " successfully";
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error removing user from group: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update a user group's name and/or description (requires ADMIN).")
    public String updateUserGroup(
            @ToolParam(description = "The groupId") Long groupId,
            @ToolParam(description = "New group name") String name,
            @ToolParam(description = "New group description", required = false) String description) {
        try {
            ToolActivityContextHolder.reportActivity("Updating user group ID: " + groupId);
            UserGroup existing = userGroupApi.getById(groupId);
            if (existing == null) {
                return "User group not found with ID: " + groupId;
            }
            Set<Long> memberIds = existing.getMemberIds() != null ? existing.getMemberIds() : new HashSet<>();
            UserGroup updated   = userGroupApi.update(groupId, name, description, memberIds);
            ToolActivityContextHolder.reportActivity("Updated user group '" + updated.getName() + "' (ID: " + groupId + ")");
            return jsonMapper.writeValueAsString(UserGroupDto.from(updated));
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error updating user group " + groupId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
