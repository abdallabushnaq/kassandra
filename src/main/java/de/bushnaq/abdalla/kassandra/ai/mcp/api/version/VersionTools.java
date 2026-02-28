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

import de.bushnaq.abdalla.kassandra.ai.mcp.KassandraToolCallResultConverter;
import de.bushnaq.abdalla.kassandra.ai.mcp.ToolActivityContextHolder;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.rest.api.VersionApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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

    @Autowired
    @Qualifier("aiVersionApi")
    private VersionApi versionApi;

    @Tool(description = "Create a new version for a product.", resultConverter = KassandraToolCallResultConverter.class)
    public VersionDto createVersion(
            @ToolParam(description = "Unique version name") String name,
            @ToolParam(description = "The productId this version belongs to") Long productId) {
        Version version = new Version();
        version.setName(name);
        version.setProductId(productId);
        Version savedVersion = versionApi.persist(version);
        ToolActivityContextHolder.reportActivity("created version '" + savedVersion.getName() + "' with ID: " + savedVersion.getId() + " for product " + productId);
        return VersionDto.from(savedVersion);
    }

    @Tool(description = "Delete a version by its versionId.", resultConverter = KassandraToolCallResultConverter.class)
    public void deleteVersion(
            @ToolParam(description = "The versionId") Long versionId) {
        Version version = versionApi.getById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("Version not found with ID: " + versionId);
        }
        ToolActivityContextHolder.reportActivity("Deleting version '" + version.getName() + "' (ID: " + versionId + ")");
        versionApi.deleteById(versionId);
        ToolActivityContextHolder.reportActivity("Deleted version '" + version.getName() + "' (ID: " + versionId + ")");
    }

    @Tool(description = "Get all versions accessible to the current user.", resultConverter = KassandraToolCallResultConverter.class)
    public List<VersionDto> getAllVersions() {
        List<Version> versions = versionApi.getAll();
        ToolActivityContextHolder.reportActivity("Found " + versions.size() + " versions.");
        return versions.stream().map(VersionDto::from).collect(Collectors.toList());
    }

    @Tool(description = "Get all versions for a product.", resultConverter = KassandraToolCallResultConverter.class)
    public List<VersionDto> getAllVersionsByProductId(
            @ToolParam(description = "The productId") Long productId) {
        List<Version> versions = versionApi.getAll(productId);
        ToolActivityContextHolder.reportActivity("Found " + versions.size() + " versions for product " + productId + ".");
        return versions.stream().map(VersionDto::from).collect(Collectors.toList());
    }

    @Tool(description = "Get a version by its versionId.", resultConverter = KassandraToolCallResultConverter.class)
    public VersionDto getVersionById(
            @ToolParam(description = "The versionId") Long versionId) {
        Version version = versionApi.getById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("Version not found with ID: " + versionId);
        }
        return VersionDto.from(version);
    }

    @Tool(description = "Update a version name by its versionId.", resultConverter = KassandraToolCallResultConverter.class)
    public VersionDto updateVersion(
            @ToolParam(description = "The versionId") Long versionId,
            @ToolParam(description = "The new version name") String name) {
        Version version = versionApi.getById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("Version not found with ID: " + versionId);
        }
        ToolActivityContextHolder.reportActivity("Updating version " + versionId + " with name: " + name);
        version.setName(name);
        versionApi.update(version);
        return VersionDto.from(version);
    }
}
