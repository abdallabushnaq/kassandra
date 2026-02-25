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
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionException;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.rest.api.FeatureApi;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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

    /**
     * Schema description for Feature objects returned by tools.
     * Describes the JSON structure and field meanings.
     * Used in @Tool annotations - must be a compile-time constant.
     */
    private static final String                 FEATURE_FIELDS             = """
            featureId (number): Unique identifier of the feature, used to map sprints to a feature,
            name (string): The feature name,
            versionId (number): The version this feature belongs to,
            created (ISO 8601 datetime string): Timestamp when the feature was created,
            updated (ISO 8601 datetime string): Timestamp when the feature was last updated,
            avatarPrompt (string): Default avatar prompt for stable-diffusion to generate.
            """;
    private static final String                 RETURNS_FEATURE_ARRAY_JSON = "Returns: JSON array of Feature objects. Each Feature contains: " + FEATURE_FIELDS;
    private static final String                 RETURNS_FEATURE_JSON       = "Returns: JSON Feature object with fields: " + FEATURE_FIELDS;
    @Autowired
    @Qualifier("aiFeatureApi")
    private              FeatureApi             featureApi;
    @Autowired
    private              JsonMapper             jsonMapper;
    @Autowired
    protected            StableDiffusionService stableDiffusionService;

    @Tool(description = "Create a new feature for a specific version. " +
            "IMPORTANT: The returned JSON includes an 'featureId' field - you MUST extract and use this ID for subsequent operations (like deleting or updating this feature). " +
            RETURNS_FEATURE_JSON)
    public String createFeature(
            @ToolParam(description = "The feature name (must be unique)") String name,
            @ToolParam(description = "The versionId this feature belongs to") Long versionId,
            @ToolParam(description = "(Optional) The feature avatar stable-diffusion prompt. If null or empty, a default prompt will be generated.") String avatarPrompt) {
        try {
            Feature feature = new Feature();
            feature.setName(name);
            feature.setVersionId(versionId);

            if (avatarPrompt == null || avatarPrompt.isEmpty()) {
                avatarPrompt = feature.getDefaultAvatarPrompt();
            }
            {
                GeneratedImageResult image     = null;
                long                 startTime = System.currentTimeMillis();
                if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
                    try {
                        image = generateFeatureAvatar(name);
                    } catch (StableDiffusionException e) {
                        System.err.println("Failed to generate image for feature " + name + ": " + e.getMessage());
                        image = stableDiffusionService.generateDefaultAvatar("lightbulb");
                    }
                } else {
                    log.warn("Stable Diffusion not available, using default avatar for feature: " + name);
                    image = stableDiffusionService.generateDefaultAvatar("lightbulb");
                }
                feature.setAvatarHash(AvatarUtil.computeHash(image.getResizedImage()));
                Feature savedFeature = featureApi.persist(feature);
                featureApi.updateAvatarFull(savedFeature.getId(), image.getResizedImage(), image.getOriginalImage(), image.getPrompt());
                ToolActivityContextHolder.reportActivity("created feature '" + savedFeature.getName() + "' with ID: " + savedFeature.getId());
                FeatureDto featureDto = FeatureDto.from(savedFeature);
                return jsonMapper.writeValueAsString(featureDto);
            }
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Failed creating feature: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a feature by its featureId. " +
            "IMPORTANT: You must provide the exact featureId. If you just created a feature, use the 'featureId' field from the createFeature response. " +
            "Do NOT guess or use a different featureId. " +
            "Returns: Success message (string) confirming deletion")
    public String deleteFeature(
            @ToolParam(description = "The featureId") Long featureId) {
        try {
            // First, get the feature details to log what we're about to delete
            Feature featureToDelete = featureApi.getById(featureId);
            if (featureToDelete != null) {
                ToolActivityContextHolder.reportActivity("Deleting feature '" + featureToDelete.getName() + "' (ID: " + featureId + ")");
            } else {
                ToolActivityContextHolder.reportActivity("Attempting to delete feature with ID: " + featureId + " (feature not found)");
            }

            featureApi.deleteById(featureId);
            ToolActivityContextHolder.reportActivity("Successfully deleted feature with ID: " + featureId);
            return "Feature deleted successfully with ID: " + featureId;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error deleting feature " + featureId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private @NonNull GeneratedImageResult generateFeatureAvatar(String name) throws StableDiffusionException {
        String prompt = Feature.getDefaultAvatarPrompt(name);
        log.trace("Generating image for feature: " + name + " with prompt: " + prompt);
        GeneratedImageResult image = stableDiffusionService.generateImageWithOriginal(prompt);
        return image;
    }


    @Tool(description = "Get a list of all features accessible to the current user (Admin sees all). Good if you need to retrieve features for all versions or all possible products. " + RETURNS_FEATURE_ARRAY_JSON)
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
            @ToolParam(description = "The versionId (use VersionTools to get version IDs for a product)") Long versionId) {
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
            @ToolParam(description = "The featureId") Long featureId) {
        try {
            ToolActivityContextHolder.reportActivity("Getting feature with ID: " + featureId);
            Feature feature = featureApi.getById(featureId);
            if (feature != null) {
                FeatureDto featureDto = FeatureDto.from(feature);
                return jsonMapper.writeValueAsString(featureDto);
            }
            return "Feature not found with ID: " + featureId;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting feature " + featureId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific feature by its name within a version. " + RETURNS_FEATURE_JSON)
    public String getFeatureByName(
            @ToolParam(description = "The versionId the feature belongs to") Long versionId,
            @ToolParam(description = "The feature name") String name) {
        try {
            ToolActivityContextHolder.reportActivity("Getting feature with name: " + name + " in version " + versionId);
            return featureApi.getByName(versionId, name)
                    .map(feature -> {
                        try {
                            return jsonMapper.writeValueAsString(FeatureDto.from(feature));
                        } catch (Exception e) {
                            return "Error: " + e.getMessage();
                        }
                    })
                    .orElse("Feature not found with name: " + name + " in version " + versionId);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting feature by name " + name + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing feature name by its featureId. " + RETURNS_FEATURE_JSON)
    public String updateFeature(
            @ToolParam(description = "The featureId") Long id,
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
