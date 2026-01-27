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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents an MCP (Model Context Protocol) tool definition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {
    private String              description;
    private Map<String, Object> inputSchema;
    private String              name;

    /**
     * Create a tool with parameters
     */
    public static McpTool create(String name, String description, List<McpToolParameter> parameters) {
        McpTool tool = new McpTool();
        tool.setName(name);
        tool.setDescription(description);

        // Build JSON Schema for parameters
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", parameters.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                McpToolParameter::getName,
                                p -> Map.of(
                                        "type", p.getType(),
                                        "description", p.getDescription()
                                )
                        )),
                "required", parameters.stream()
                        .filter(McpToolParameter::isRequired)
                        .map(McpToolParameter::getName)
                        .toList()
        );

        tool.setInputSchema(schema);
        return tool;
    }

    @Data
    @NoArgsConstructor
    public static class McpToolParameter {
        private String  description;
        private String  name;
        private boolean required;
        private String  type;        // "string", "number", "integer", "boolean", "object", "array"

        /**
         * Constructor with parameters in the order used by calling code
         *
         * @param name        the parameter name
         * @param type        the parameter type (string, number, integer, boolean, object, array)
         * @param description the parameter description
         * @param required    whether the parameter is required
         */
        public McpToolParameter(String name, String type, String description, boolean required) {
            this.name        = name;
            this.type        = type;
            this.description = description;
            this.required    = required;
        }
    }
}
