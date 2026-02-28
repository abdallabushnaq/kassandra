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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.user;

import de.bushnaq.abdalla.kassandra.ai.mcp.KassandraToolCallResultConverter;
import de.bushnaq.abdalla.kassandra.ai.mcp.ToolActivityContextHolder;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Spring AI native tool implementations for User operations.
 * <p>
 * Users represent individuals in the system. They can be assigned roles, colors, and
 * working-day ranges, and can be members of user groups.
 * <p>
 * Example usage:
 * 1. Use getAllUsers() to list all users, or searchUsers(partialName) to find by name.
 * 2. Use getUserByEmail(email) or getUserByName(name) to look up a specific user and obtain their userId.
 * 3. Use createUser(name, email, ...) to create a new user. The userId is returned and must be used for further operations.
 * 4. Use updateUser(userId, ...) to change a user's name, email, color, roles, or working-day range.
 * 5. Use deleteUser(userId) to delete a user. Always look up the userId first — never guess it.
 * <p>
 * When asked to add a user to a group after creating them, first obtain the groupId via
 * UserGroupTools.getUserGroupByName(name), then call UserGroupTools.addMemberToGroup(groupId, userId).
 * <p>
 */
@Component
@Slf4j
public class UserTools {

    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    @Qualifier("aiUserApi")
    private UserApi    userApi;

    @Tool(description = "Create a new user.", resultConverter = KassandraToolCallResultConverter.class)
    public UserDto createUser(
            @ToolParam(description = "The user name") String name,
            @ToolParam(description = "The user email address") String email,
            @ToolParam(description = "User color in hex format (e.g. '#336699')", required = false) String colorHex,
            @ToolParam(description = "Comma-separated roles: ROLE_USER or ROLE_ADMIN", required = false) String roles,
            @ToolParam(description = "First working day (YYYY-MM-DD); defaults to today", required = false) String firstWorkingDay,
            @ToolParam(description = "Last working day (YYYY-MM-DD); omit if still employed", required = false) String lastWorkingDay) {
        log.info("Creating user: name={}, email={}", name, email);
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setColor(parseColorFromHex(colorHex));
        user.setRoles(roles != null ? roles : "ROLE_USER");
        user.setFirstWorkingDay(firstWorkingDay != null && !firstWorkingDay.isEmpty()
                ? LocalDate.parse(firstWorkingDay) : LocalDate.now());
        if (lastWorkingDay != null && !lastWorkingDay.isEmpty()) {
            user.setLastWorkingDay(LocalDate.parse(lastWorkingDay));
        }
        User createdUser = userApi.persist(user);
        ToolActivityContextHolder.reportActivity("created user '" + createdUser.getName() + "' with ID: " + createdUser.getId());
        return UserDto.from(createdUser);
    }

    @Tool(description = "Delete a user by their userId.", resultConverter = KassandraToolCallResultConverter.class)
    public void deleteUser(
            @ToolParam(description = "The userId") Long userId) {
        User user = userApi.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        ToolActivityContextHolder.reportActivity("Deleting user '" + user.getName() + "' (ID: " + userId + ")");
        userApi.deleteById(userId);
        ToolActivityContextHolder.reportActivity("Deleted user '" + user.getName() + "' (ID: " + userId + ")");
    }

    //    @Tool(description = "Get all users in the system.")
//    public String getAllUsers() {
//        try {
//            List<User> users = userApi.getAll();
//            log.info("read " + users.size() + " users.");
//            List<UserDto> userDtos = users.stream().map(UserDto::from).toList();
//            return jsonMapper.writeValueAsString(userDtos);
//        } catch (Exception e) {
//            log.error("Error getting all users: {}", e.getMessage());
//            return "Error: " + e.getMessage();
//        }
//    }
    @Tool(description = "Get all users in the system.", resultConverter = KassandraToolCallResultConverter.class)
    public List<UserDto> getAllUsers() {
        List<User> users = userApi.getAll();
        log.info("read {} users.", users.size());
        return users.stream().map(UserDto::from).toList();
    }


    @Tool(description = "Get a user by their email address.", resultConverter = KassandraToolCallResultConverter.class)
    public UserDto getUserByEmail(
            @ToolParam(description = "The user email address") String email) {
        return userApi.getByEmail(email)
                .map(UserDto::from)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    @Tool(description = "Get a user by their userId.", resultConverter = KassandraToolCallResultConverter.class)
    public UserDto getUserById(
            @ToolParam(description = "The userId") Long userId) {
        User user = userApi.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        return UserDto.from(user);
    }

    @Tool(description = "Get a user by their name.", resultConverter = KassandraToolCallResultConverter.class)
    public UserDto getUserByName(
            @ToolParam(description = "The user name") String name) {
        User user = userApi.getByName(name);
        if (user == null) {
            throw new IllegalArgumentException("User not found with name: " + name);
        }
        return UserDto.from(user);
    }

    /**
     * Parse a hex color string into a Color object.
     * Supports formats: "#RRGGBB" or "#RRGGBBAA".
     * Returns default blue if input is null, empty, or invalid.
     */
    private Color parseColorFromHex(String colorHex) {
        if (colorHex == null || colorHex.isEmpty()) {
            return new Color(51, 102, 204);
        }
        try {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            return new Color((int) Long.parseLong(hex, 16), true);
        } catch (NumberFormatException e) {
            log.warn("Invalid color format '{}', using default blue color", colorHex);
            return new Color(51, 102, 204);
        }
    }

    @Tool(description = "Search for users by partial name (case-insensitive).", resultConverter = KassandraToolCallResultConverter.class)
    public List<UserDto> searchUsers(
            @ToolParam(description = "Partial name to search for") String partialName) {
        List<User> users = userApi.searchByName(partialName);
        return users.stream().map(UserDto::from).toList();
    }

    @Tool(description = "Update an existing user by their userId.", resultConverter = KassandraToolCallResultConverter.class)
    public UserDto updateUser(
            @ToolParam(description = "The userId") Long userId,
            @ToolParam(description = "New user name", required = false) String name,
            @ToolParam(description = "New email address", required = false) String email,
            @ToolParam(description = "New color in hex format (e.g. '#FF0000')", required = false) String colorHex,
            @ToolParam(description = "New roles (ROLE_USER or ROLE_ADMIN)", required = false) String roles,
            @ToolParam(description = "New first working day (YYYY-MM-DD)", required = false) String firstWorkingDay,
            @ToolParam(description = "New last working day (YYYY-MM-DD)", required = false) String lastWorkingDay) {
        User user = userApi.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        if (name != null && !name.isEmpty()) user.setName(name);
        if (email != null && !email.isEmpty()) user.setEmail(email);
        if (colorHex != null && !colorHex.isEmpty()) user.setColor(parseColorFromHex(colorHex));
        if (roles != null && !roles.isEmpty()) user.setRoles(roles);
        if (firstWorkingDay != null && !firstWorkingDay.isEmpty()) user.setFirstWorkingDay(LocalDate.parse(firstWorkingDay));
        if (lastWorkingDay != null && !lastWorkingDay.isEmpty()) user.setLastWorkingDay(LocalDate.parse(lastWorkingDay));
        userApi.update(user);
        log.info("updated user: id={}", userId);
        return UserDto.from(user);
    }
}
