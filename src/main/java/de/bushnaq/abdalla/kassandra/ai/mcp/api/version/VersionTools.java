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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.version;

import de.bushnaq.abdalla.kassandra.ai.mcp.ToolActivityContextHolder;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.rest.api.VersionApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI native tool implementations for Version operations.
 * <p>
 * Versions belong to Products, and each Version can have multiple Features.
 * To list all features for a product, first use getAllVersionsByProductId to get all versions for the product,
 * then use FeatureTools to get features for each version.
 * <p>
 * Example traversal:
 * 1. Use ProductTools.getAllProducts() to list products.
 * 2. Use VersionTools.getAllVersionsByProductId(productId) to list versions for a product.
 * 3. Use FeatureTools.getAllFeaturesByVersionId(versionId) to list features for a version.
 * <p>
 * Uses @Tool annotation for automatic tool registration with ChatClient.
 */
@Component
@Slf4j
public class VersionTools {
    private static final String VERSION_FIELDS             =
            "id (number): Unique identifier of the version, used to map features to a version, " +
                    "name (string): The version name, " +
                    "created (ISO 8601 datetime string): Timestamp when the version was created, " +
                    "updated (ISO 8601 datetime string): Timestamp when the version was last updated, " +
                    "productId (number): The product this version belongs to";
    private static final String RETURNS_VERSION_ARRAY_JSON = "Returns: JSON array of Version objects. Each Version contains: " + VERSION_FIELDS;
    private static final String RETURNS_VERSION_JSON       = "Returns: JSON Version object with fields: " + VERSION_FIELDS;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    @Qualifier("aiVersionApi")
    private VersionApi versionApi;

    @Tool(description = "Create a new version for a product (requires USER or ADMIN role). " + RETURNS_VERSION_JSON)
    public String createVersion(
            @ToolParam(description = "The version name (must be unique)") String name,
            @ToolParam(description = "The product ID this version belongs to") Long productId) {
        try {
            ToolActivityContextHolder.reportActivity("Creating version with name: " + name + " for product " + productId);
            Version version = new Version();
            version.setName(name);
            version.setProductId(productId);
            Version    savedVersion = versionApi.persist(version);
            VersionDto versionDto   = VersionDto.from(savedVersion);
            return jsonMapper.writeValueAsString(versionDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error creating version: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a version by ID (requires access or admin role). " +
            "Returns: Success message (string) confirming deletion")
    public String deleteVersion(
            @ToolParam(description = "The version ID") Long id) {
        try {
            ToolActivityContextHolder.reportActivity("Deleting version with ID: " + id);
            versionApi.deleteById(id);
            return "Version deleted successfully with ID: " + id;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error deleting version " + id + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all versions accessible to the current user (Admin sees all). Good if you need to retrieve versions for many products. " + RETURNS_VERSION_ARRAY_JSON)
    public String getAllVersions() {
        try {
            ToolActivityContextHolder.reportActivity("Getting all versions");
            List<Version> versions = versionApi.getAll();
            ToolActivityContextHolder.reportActivity("Found " + versions.size() + " versions.");
            List<VersionDto> versionDtos = versions.stream()
                    .map(VersionDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(versionDtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting all versions: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all versions for a product (requires access or admin role). " + RETURNS_VERSION_ARRAY_JSON)
    public String getAllVersionsByProductId(
            @ToolParam(description = "The product ID") Long productId) {
        try {
            ToolActivityContextHolder.reportActivity("Getting all versions for product ID: " + productId);
            List<Version> versions = versionApi.getAll(productId);
            ToolActivityContextHolder.reportActivity("Found " + versions.size() + " versions.");
            List<VersionDto> versionDtos = versions.stream()
                    .map(VersionDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(versionDtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting all versions for product " + productId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific version by its ID (requires access or admin role). " + RETURNS_VERSION_JSON)
    public String getVersionById(
            @ToolParam(description = "The version ID") Long id) {
        try {
            ToolActivityContextHolder.reportActivity("Getting version with ID: " + id);
            Version version = versionApi.getById(id);
            if (version != null) {
                VersionDto versionDto = VersionDto.from(version);
                return jsonMapper.writeValueAsString(versionDto);
            }
            return "Version not found with ID: " + id;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting version " + id + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing version (requires access or admin role). " + RETURNS_VERSION_JSON)
    public String updateVersion(
            @ToolParam(description = "The version ID") Long id,
            @ToolParam(description = "The new version name") String name) {
        try {
            ToolActivityContextHolder.reportActivity("Updating version " + id + " with name: " + name);
            Version version = versionApi.getById(id);
            if (version == null) {
                return "Version not found with ID: " + id;
            }
            version.setName(name);
            versionApi.update(version);
            VersionDto versionDto = VersionDto.from(version);
            return jsonMapper.writeValueAsString(versionDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error updating version " + id + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
