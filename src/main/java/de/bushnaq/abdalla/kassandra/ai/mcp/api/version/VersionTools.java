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
import de.bushnaq.abdalla.kassandra.ai.mcp.ToolContextHelper;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.rest.api.VersionApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
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
    private static final String VERSION_FIELDS             = """
            versionId (number): Unique identifier of the version, used to map features to a version,
            name (string): The version name,
            productId (number): The product this version belongs to,
            created (ISO 8601 datetime string): Timestamp when the version was created,
            updated (ISO 8601 datetime string): Timestamp when the version was last updated
            """;
    private static final String RETURNS_VERSION_ARRAY_JSON = "Returns: JSON array of Version objects. Each Version contains: " + VERSION_FIELDS;
    private static final String RETURNS_VERSION_JSON       = "Returns: JSON Version object with fields: " + VERSION_FIELDS;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    @Qualifier("aiVersionApi")
    private VersionApi versionApi;

    @Tool(description = "Create a new version for a product. " +
            "IMPORTANT: The returned JSON includes an 'versionId' field - you MUST extract and use this ID for subsequent operations (like deleting this version). " +
            RETURNS_VERSION_JSON)
    public String createVersion(
            @ToolParam(description = "The version name (must be unique)") String name,
            @ToolParam(description = "The productId this version belongs to") Long productId,
            ToolContext toolContext) {
        ToolContextHelper.setup(toolContext);
        try {
            Version version = new Version();
            version.setName(name);
            version.setProductId(productId);
            Version savedVersion = versionApi.persist(version);
            ToolActivityContextHolder.reportActivity("created version '" + savedVersion.getName() + "' with ID: " + savedVersion.getId() + " for product " + productId);
            VersionDto versionDto = VersionDto.from(savedVersion);
            return jsonMapper.writeValueAsString(versionDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error creating version: " + e.getMessage());
            return "Error: " + e.getMessage();
        } finally {
            ToolContextHelper.cleanup();
        }
    }

    @Tool(description = "Delete a version by its versionId. " +
            "IMPORTANT: You must provide the exact versionId. If you just created a version, use the 'versionId' field from the createVersion response. " +
            "Do NOT guess or use a different version's ID. " +
            "Returns: Success message (string) confirming deletion")
    public String deleteVersion(
            @ToolParam(description = "The versionId") Long versionId,
            ToolContext toolContext) {
        ToolContextHelper.setup(toolContext);
        try {
            // First, get the version details to log what we're about to delete
            Version versionToDelete = versionApi.getById(versionId);
            if (versionToDelete != null) {
                ToolActivityContextHolder.reportActivity("Deleting version '" + versionToDelete.getName() + "' (ID: " + versionId + ")");
            } else {
                ToolActivityContextHolder.reportActivity("Attempting to delete version with ID: " + versionId + " (version not found)");
            }

            versionApi.deleteById(versionId);
            ToolActivityContextHolder.reportActivity("Successfully deleted version with ID: " + versionId);
            return "Version deleted successfully with ID: " + versionId;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error deleting version " + versionId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        } finally {
            ToolContextHelper.cleanup();
        }
    }

    @Tool(description = "Get a list of all versions accessible to the current user. Good if you need to retrieve versions for all products. " + RETURNS_VERSION_ARRAY_JSON)
    public String getAllVersions(ToolContext toolContext) {
        ToolContextHelper.setup(toolContext);
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
        } finally {
            ToolContextHelper.cleanup();
        }
    }

    @Tool(description = "Get a list of all versions for a product. " + RETURNS_VERSION_ARRAY_JSON)
    public String getAllVersionsByProductId(
            @ToolParam(description = "The productId") Long productId,
            ToolContext toolContext) {
        ToolContextHelper.setup(toolContext);
        try {
            List<Version> versions = versionApi.getAll(productId);
            ToolActivityContextHolder.reportActivity("Found " + versions.size() + " versions for product " + productId + ".");
            List<VersionDto> versionDtos = versions.stream()
                    .map(VersionDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(versionDtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Failed getting all versions for product " + productId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        } finally {
            ToolContextHelper.cleanup();
        }
    }

    @Tool(description = "Get a specific version by its versionId. " + RETURNS_VERSION_JSON)
    public String getVersionById(
            @ToolParam(description = "The versionId") Long versionId,
            ToolContext toolContext) {
        ToolContextHelper.setup(toolContext);
        try {
            ToolActivityContextHolder.reportActivity("Getting version with ID: " + versionId);
            Version version = versionApi.getById(versionId);
            if (version != null) {
                VersionDto versionDto = VersionDto.from(version);
                return jsonMapper.writeValueAsString(versionDto);
            }
            return "Version not found with ID: " + versionId;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting version " + versionId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        } finally {
            ToolContextHelper.cleanup();
        }
    }

    @Tool(description = "Update an existing version by its versionId. " + RETURNS_VERSION_JSON)
    public String updateVersion(
            @ToolParam(description = "The versionId") Long versionId,
            @ToolParam(description = "The new version name") String name,
            ToolContext toolContext) {
        ToolContextHelper.setup(toolContext);
        try {
            ToolActivityContextHolder.reportActivity("Updating version " + versionId + " with name: " + name);
            Version version = versionApi.getById(versionId);
            if (version == null) {
                return "Version not found with ID: " + versionId;
            }
            version.setName(name);
            versionApi.update(version);
            VersionDto versionDto = VersionDto.from(version);
            return jsonMapper.writeValueAsString(versionDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error updating version " + versionId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        } finally {
            ToolContextHelper.cleanup();
        }
    }
}
