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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.feature;

import de.bushnaq.abdalla.kassandra.ai.mcp.ToolActivityContextHolder;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.rest.api.FeatureApi;
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
 * Spring AI native tool implementations for Feature operations.
 * <p>
 * Features belong to Versions, which in turn belong to Products.
 * To list all features for a product, first retrieve its versions (using VersionTools),
 * then for each version, use getAllFeaturesByVersionId to get its features.
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
public class FeatureTools {
    private static final String     FEATURE_FIELDS             =
            "id (number): Unique identifier of the feature, " +
                    "name (string): The feature name, " +
                    "created (ISO 8601 datetime string): Timestamp when the feature was created, " +
                    "updated (ISO 8601 datetime string): Timestamp when the feature was last updated, " +
                    "versionId (number): The version this feature belongs to";
    private static final String     RETURNS_FEATURE_ARRAY_JSON = "Returns: JSON array of Feature objects. Each Feature contains: " + FEATURE_FIELDS;
    private static final String     RETURNS_FEATURE_JSON       = "Returns: JSON Feature object with fields: " + FEATURE_FIELDS;
    @Autowired
    @Qualifier("aiFeatureApi")
    private              FeatureApi featureApi;
    @Autowired
    private              JsonMapper jsonMapper;

    @Tool(description = "Create a new feature for a version(requires USER or ADMIN role). " + RETURNS_FEATURE_JSON)
    public String createFeature(
            @ToolParam(description = "The feature name (must be unique)") String name,
            @ToolParam(description = "The version ID this feature belongs to") Long versionId) {
        try {
            ToolActivityContextHolder.reportActivity("Creating feature with name: " + name + " for version " + versionId);
            Feature feature = new Feature();
            feature.setName(name);
            feature.setVersionId(versionId);
            Feature    savedFeature = featureApi.persist(feature);
            FeatureDto featureDto   = FeatureDto.from(savedFeature);
            return jsonMapper.writeValueAsString(featureDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error creating feature: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a feature by ID (requires access or admin role). " +
            "Returns: Success message (string) confirming deletion")
    public String deleteFeature(
            @ToolParam(description = "The feature ID") Long id) {
        try {
            ToolActivityContextHolder.reportActivity("Deleting feature with ID: " + id);
            featureApi.deleteById(id);
            return "Feature deleted successfully with ID: " + id;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error deleting feature " + id + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all features accessible to the current user (Admin sees all). Good if you need to retrieve features for many versions. " + RETURNS_FEATURE_ARRAY_JSON)
    public String getAllFeatures() {
        try {
            ToolActivityContextHolder.reportActivity("Getting all features");
            List<Feature> features = featureApi.getAll();
            ToolActivityContextHolder.reportActivity("Found " + features.size() + " features.");
            List<FeatureDto> featureDtos = features.stream()
                    .map(FeatureDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(featureDtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting all features: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all features for a version (requires access or admin role). This can be used to retrieve all features belonging to a specific product version. " + RETURNS_FEATURE_ARRAY_JSON)
    public String getAllFeaturesByVersionId(
            @ToolParam(description = "The version ID (use VersionTools to get version IDs for a product)") Long versionId) {
        try {
            ToolActivityContextHolder.reportActivity("Getting all features for version ID: " + versionId);
            List<Feature> features = featureApi.getAll(versionId);
            ToolActivityContextHolder.reportActivity("Found " + features.size() + " features.");
            List<FeatureDto> featureDtos = features.stream()
                    .map(FeatureDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(featureDtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting all features for version " + versionId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific feature by its ID (requires access or admin role). " + RETURNS_FEATURE_JSON)
    public String getFeatureById(
            @ToolParam(description = "The feature ID") Long id) {
        try {
            ToolActivityContextHolder.reportActivity("Getting feature with ID: " + id);
            Feature feature = featureApi.getById(id);
            if (feature != null) {
                FeatureDto featureDto = FeatureDto.from(feature);
                return jsonMapper.writeValueAsString(featureDto);
            }
            return "Feature not found with ID: " + id;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting feature " + id + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing feature (requires access or admin role). " + RETURNS_FEATURE_JSON)
    public String updateFeature(
            @ToolParam(description = "The feature ID") Long id,
            @ToolParam(description = "The new feature name") String name) {
        try {
            ToolActivityContextHolder.reportActivity("Updating feature " + id + " with name: " + name);
            Feature feature = featureApi.getById(id);
            if (feature == null) {
                return "Feature not found with ID: " + id;
            }
            feature.setName(name);
            featureApi.update(feature);
            FeatureDto featureDto = FeatureDto.from(feature);
            return jsonMapper.writeValueAsString(featureDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error updating feature " + id + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
