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

package de.bushnaq.abdalla.kassandra.mcp;

import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Registers User API endpoints as MCP tools
 */
@Component
@Slf4j
public class UserMcpTools {

    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    private McpServer mcpServer;
    @Autowired
    private UserRepository userRepository;

    private void registerGetAllUsers() {
        McpTool tool = McpTool.create(
                "get_all_users",
                "Get a list of all users in the system",
                List.of()
        );

        mcpServer.registerTool(tool, _args -> {
            try {
                List<UserDAO> users = userRepository.findAll();
                return jsonMapper.writeValueAsString(users);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    private void registerGetUserByEmail() {
        McpTool tool = McpTool.create(
                "get_user_by_email",
                "Get a specific user by their email address",
                List.of(
                        new McpTool.McpToolParameter("email", "string", "The user email", true)
                )
        );

        mcpServer.registerTool(tool, args -> {
            try {
                String email = args.get("email").toString();
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
                return "Error: " + e.getMessage();
            }
        });
    }

    private void registerGetUserById() {
        McpTool tool = McpTool.create(
                "get_user_by_id",
                "Get a specific user by their ID",
                List.of(
                        new McpTool.McpToolParameter("id", "integer", "The user ID", true)
                )
        );

        mcpServer.registerTool(tool, args -> {
            try {
                Long id = Long.valueOf(args.get("id").toString());
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
                return "Error: " + e.getMessage();
            }
        });
    }

    private void registerGetUserByName() {
        McpTool tool = McpTool.create(
                "get_user_by_name",
                "Get a specific user by their name",
                List.of(
                        new McpTool.McpToolParameter("name", "string", "The user name", true)
                )
        );

        mcpServer.registerTool(tool, args -> {
            try {
                String name = args.get("name").toString();
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
                return "Error: " + e.getMessage();
            }
        });
    }

    private void registerGetUserRoles() {
        McpTool tool = McpTool.create(
                "get_user_roles",
                "Get the roles assigned to a specific user (Admin only)",
                List.of(
                        new McpTool.McpToolParameter("userId", "integer", "The user ID", true)
                )
        );

        mcpServer.registerTool(tool, args -> {
            try {
                // Note: This is a simplified version. In production, you'd want to call the UserRoleService
                Long userId = Long.valueOf(args.get("userId").toString());
                return userRepository.findById(userId)
                        .map(_user -> "User roles endpoint requires UserRoleService integration")
                        .orElse("User not found with ID: " + userId);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    private void registerSearchUsers() {
        McpTool tool = McpTool.create(
                "search_users",
                "Search for users by partial name match (case-insensitive)",
                List.of(
                        new McpTool.McpToolParameter("partialName", "string", "Partial name to search for", true)
                )
        );

        mcpServer.registerTool(tool, args -> {
            try {
                String        partialName = args.get("partialName").toString();
                List<UserDAO> users       = userRepository.findByNameContainingIgnoreCase(partialName);
                return jsonMapper.writeValueAsString(users);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    @PostConstruct
    public void registerTools() {
        registerGetAllUsers();
        registerGetUserById();
        registerGetUserByName();
        registerGetUserByEmail();
        registerSearchUsers();
        registerGetUserRoles();
    }
}
