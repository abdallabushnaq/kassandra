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
 * Uses @Tool annotation for automatic tool registration with ChatClient.
 */
@Component
@Slf4j
public class UserTools {
    private static final String USER_FIELDS             = """
            userId (number): Unique identifier of the user,
            name (string): The user name,
            email (string): The user email,
            color (string): The user color in hex format,
            roles (string): The user roles (comma-separated, 'ROLE_USER' or 'ROLE_ADMIN'),
            firstWorkingDay (ISO 8601 date string): The first working day,
            lastWorkingDay (ISO 8601 date string): The last working day
            """;
    private static final String RETURNS_USER_ARRAY_JSON = "Returns: JSON array of User objects. Each User contains: " + USER_FIELDS;
    private static final String RETURNS_USER_JSON       = "Returns: JSON User object with fields: " + USER_FIELDS;

    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    @Qualifier("aiUserApi")
    private UserApi    userApi;


    @Tool(description = "Create a new user. " +
            "IMPORTANT: The returned JSON includes an 'userId' field - you MUST extract and use this ID for subsequent operations (like deleting this user). " +
            RETURNS_USER_JSON)
    public String createUser(
            @ToolParam(description = "The user name") String name,
            @ToolParam(description = "The user email address") String email,
            @ToolParam(description = "The user color in hex format (e.g., '#FF0000' for red, '#336699' for blue). If not provided, defaults to blue.") String colorHex,
            @ToolParam(description = "(optional) The user roles (comma-separated, 'ROLE_USER' or 'ROLE_ADMIN')") String roles,
            @ToolParam(description = "(optional) The first working day in ISO format (YYYY-MM-DD), optional. If not provided, today's date will be used.") String firstWorkingDay,
            @ToolParam(description = "(optional) The last working day in ISO format (YYYY-MM-DD), optional. If not provided, user is still employed.") String lastWorkingDay) {
        try {
            log.info("Creating user: name={}, email={}", name, email);
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setColor(parseColorFromHex(colorHex));
            if (roles == null)
                user.setRoles("ROLE_USER");
            else
                user.setRoles(roles);

            // Use today's date if firstWorkingDay is not provided
            if (firstWorkingDay != null && !firstWorkingDay.isEmpty()) {
                user.setFirstWorkingDay(LocalDate.parse(firstWorkingDay));
            } else {
                user.setFirstWorkingDay(LocalDate.now());
            }

            if (lastWorkingDay != null && !lastWorkingDay.isEmpty()) {
                user.setLastWorkingDay(LocalDate.parse(lastWorkingDay));
            }
            User createdUser = userApi.persist(user);
            ToolActivityContextHolder.reportActivity("created user '" + createdUser.getName() + "' with ID: " + createdUser.getId());
            return jsonMapper.writeValueAsString(UserDto.from(createdUser));
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a user by ID (requires access or admin role). " +
            "IMPORTANT: You must provide the exact userId. If you just created a user, use the 'id' field from the createUser response. " +
            "Do NOT guess or use a different user's ID. " +
            "Returns: Success message (string) confirming deletion")
    public String deleteUser(
            @ToolParam(description = "The userId") Long userId) {
        try {
            // First, get the user details to log what we're about to delete
            User userToDelete = userApi.getById(userId);
            if (userToDelete != null) {
                ToolActivityContextHolder.reportActivity("Deleting user '" + userToDelete.getName() + "' (ID: " + userId + ")");
            } else {
                ToolActivityContextHolder.reportActivity("Attempting to delete user with ID: " + userId + " (user not found)");
            }

            userApi.deleteById(userId);
            ToolActivityContextHolder.reportActivity("Successfully deleted user with ID: " + userId);
            return "User with ID " + userId + " deleted successfully";
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error deleting user: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all users in the system. " + RETURNS_USER_ARRAY_JSON)
    public String getAllUsers() {
        try {
            List<User> users = userApi.getAll();
            log.info("read " + users.size() + " users.");
            List<UserDto> userDtos = users.stream().map(UserDto::from).toList();
            return jsonMapper.writeValueAsString(userDtos);
        } catch (Exception e) {
            log.error("Error getting all users: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific user by their email address. " + RETURNS_USER_JSON)
    public String getUserByEmail(
            @ToolParam(description = "The user email address") String email) {
        try {
            log.info("Getting user by email: {}", email);
            User user = userApi.getByEmail(email).get();
            if (user != null) {
                return jsonMapper.writeValueAsString(UserDto.from(user));
            }
            return "User not found with email: " + email;
        } catch (Exception e) {
            log.error("Error getting user by email {}: {}", email, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific user by their userId. " + RETURNS_USER_JSON)
    public String getUserById(
            @ToolParam(description = "The userId") Long userId) {
        try {
            log.info("Getting user by ID: {}", userId);
            User user = userApi.getById(userId);
            if (user != null) {
                return jsonMapper.writeValueAsString(UserDto.from(user));
            }
            return "User not found with ID: " + userId;
        } catch (Exception e) {
            log.error("Error getting user by ID {}: {}", userId, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific user by their name. " + RETURNS_USER_JSON)
    public String getUserByName(
            @ToolParam(description = "The user name") String name) {
        try {
            log.info("Getting user by name: {}", name);
            User user = userApi.getByName(name);
            if (user != null) {
                return jsonMapper.writeValueAsString(UserDto.from(user));
            }
            return "User not found with name: " + name;
        } catch (Exception e) {
            log.error("Error getting user by name {}: {}", name, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Parse a hex color string into a Color object.
     * Supports formats: "#RRGGBB" or "#RRGGBBAA"
     * Returns a default blue color if input is null, empty, or invalid.
     *
     * @param colorHex Hex color string (e.g., "#FF0000" or "#FF0000FF")
     * @return Color object
     */
    private Color parseColorFromHex(String colorHex) {
        if (colorHex == null || colorHex.isEmpty()) {
            return new Color(51, 102, 204); // Default blue
        }

        try {
            // If color string has # prefix, parse it
            if (colorHex.startsWith("#")) {
                return new Color((int) Long.parseLong(colorHex.substring(1), 16), true);
            } else {
                // Parse integer RGB value without prefix
                return new Color((int) Long.parseLong(colorHex, 16), true);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid color format '{}', using default blue color", colorHex);
            return new Color(51, 102, 204); // Default blue
        }
    }

    @Tool(description = "Search for users by partial name match (case-insensitive). " + RETURNS_USER_ARRAY_JSON)
    public String searchUsers(
            @ToolParam(description = "Partial name to search for") String partialName) {
        try {
            log.info("Searching users by partial name: {}", partialName);
            List<User>    users    = userApi.searchByName(partialName);
            List<UserDto> userDtos = users.stream().map(UserDto::from).toList();
            return jsonMapper.writeValueAsString(userDtos);
        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing user. " + RETURNS_USER_JSON)
    public String updateUser(
            @ToolParam(description = "The userId") Long userId,
            @ToolParam(description = "The new user name, optional") String name,
            @ToolParam(description = "The new user email address, optional") String email,
            @ToolParam(description = "The new user color in hex format (e.g., '#FF0000' for red), optional") String colorHex,
            @ToolParam(description = "The new user roles, optional") String roles,
            @ToolParam(description = "The new first working day in ISO format (YYYY-MM-DD), optional") String firstWorkingDay,
            @ToolParam(description = "The new last working day in ISO format (YYYY-MM-DD), optional") String lastWorkingDay) {
        try {
            User user = userApi.getById(userId);
            if (user == null) {
                return "User not found with ID: " + userId;
            }
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            }
            if (email != null && !email.isEmpty()) {
                user.setEmail(email);
            }
            if (colorHex != null && !colorHex.isEmpty()) {
                user.setColor(parseColorFromHex(colorHex));
            }
            if (roles != null && !roles.isEmpty()) {
                user.setRoles(roles);
            }
            if (firstWorkingDay != null && !firstWorkingDay.isEmpty()) {
                user.setFirstWorkingDay(LocalDate.parse(firstWorkingDay));
            }
            if (lastWorkingDay != null && !lastWorkingDay.isEmpty()) {
                user.setLastWorkingDay(LocalDate.parse(lastWorkingDay));
            }
            userApi.update(user);
            log.info("updated user: id={}", userId);
            UserDto userDto = UserDto.from(user);
            return jsonMapper.writeValueAsString(userDto);
        } catch (Exception e) {
            log.error("Error updating user {}: {}", userId, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
