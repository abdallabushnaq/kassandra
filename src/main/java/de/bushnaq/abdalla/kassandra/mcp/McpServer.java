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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * MCP (Model Context Protocol) Server implementation
 * Allows LLMs to discover and invoke API endpoints as tools
 */
@Service
@Slf4j
public class McpServer {

    private final Map<String, Function<Map<String, Object>, String>> handlers = new ConcurrentHashMap<>();
    private final JsonMapper                                         jsonMapper;
    private final Map<String, McpTool>                               tools    = new ConcurrentHashMap<>();

    public McpServer(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Execute a tool by name with given arguments
     */
    public String executeTool(String toolName, Map<String, Object> arguments) {
        Function<Map<String, Object>, String> handler = handlers.get(toolName);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }

        try {
            log.info("Executing MCP tool: {} with arguments: {}", toolName, arguments);
            return handler.apply(arguments);
        } catch (Exception e) {
            log.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get tool definitions in a format suitable for LLM prompts
     */
    public String getToolsAsPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available API Tools:\n\n");

        tools.values().forEach(tool -> {
            sb.append("Tool: ").append(tool.getName()).append("\n");
            sb.append("Description: ").append(tool.getDescription()).append("\n");
            sb.append("Parameters:\n");

            try {
                String schemaJson = jsonMapper.writeValueAsString(tool.getInputSchema());
                sb.append(schemaJson).append("\n\n");
            } catch (Exception e) {
                log.error("Error serializing tool schema", e);
            }
        });

        return sb.toString();
    }

    /**
     * List all available tools
     */
    public List<McpTool> listTools() {
        return List.copyOf(tools.values());
    }

    /**
     * Register a tool with its handler
     */
    public void registerTool(McpTool tool, Function<Map<String, Object>, String> handler) {
        tools.put(tool.getName(), tool);
        handlers.put(tool.getName(), handler);
        log.info("Registered MCP tool: {}", tool.getName());
    }
}
