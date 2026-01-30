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

import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Spring AI native tool implementations for User operations.
 * Uses @Tool annotation for automatic tool registration with ChatClient.
 */
@Component
@Slf4j
public class UserTools {

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private UserRepository userRepository;

    @Tool(description = "Get a list of all users in the system. Returns JSON array of user objects.")
    public String getAllUsers() {
        try {
            log.info("Getting all users");
            List<UserDAO> users = userRepository.findAll();
            return jsonMapper.writeValueAsString(users);
        } catch (Exception e) {
            log.error("Error getting all users: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific user by their email address. Returns JSON user object or not found message.")
    public String getUserByEmail(
            @ToolParam(description = "The user email address") String email) {
        try {
            log.info("Getting user by email: {}", email);
            return userRepository.findByEmail(email)
                    .map(user -> {
                        try {
                            return jsonMapper.writeValueAsString(user);
                        } catch (Exception e) {
                            return "Error serializing user: " + e.getMessage();
                        }
                    })
                    .orElse("User not found with email: " + email);
        } catch (Exception e) {
            log.error("Error getting user by email {}: {}", email, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific user by their ID. Returns JSON user object or not found message.")
    public String getUserById(
            @ToolParam(description = "The user ID") Long id) {
        try {
            log.info("Getting user by ID: {}", id);
            return userRepository.findById(id)
                    .map(user -> {
                        try {
                            return jsonMapper.writeValueAsString(user);
                        } catch (Exception e) {
                            return "Error serializing user: " + e.getMessage();
                        }
                    })
                    .orElse("User not found with ID: " + id);
        } catch (Exception e) {
            log.error("Error getting user by ID {}: {}", id, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific user by their name. Returns JSON user object or not found message.")
    public String getUserByName(
            @ToolParam(description = "The user name") String name) {
        try {
            log.info("Getting user by name: {}", name);
            return userRepository.findByName(name)
                    .map(user -> {
                        try {
                            return jsonMapper.writeValueAsString(user);
                        } catch (Exception e) {
                            return "Error serializing user: " + e.getMessage();
                        }
                    })
                    .orElse("User not found with name: " + name);
        } catch (Exception e) {
            log.error("Error getting user by name {}: {}", name, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Search for users by partial name match (case-insensitive). Returns JSON array of matching users.")
    public String searchUsers(
            @ToolParam(description = "Partial name to search for") String partialName) {
        try {
            log.info("Searching users by partial name: {}", partialName);
            List<UserDAO> users = userRepository.findByNameContainingIgnoreCase(partialName);
            return jsonMapper.writeValueAsString(users);
        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
