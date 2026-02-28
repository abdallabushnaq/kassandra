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

import de.bushnaq.abdalla.kassandra.ai.mcp.KassandraToolCallResultConverter;
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

    @Autowired
    @Qualifier("aiFeatureApi")
    private   FeatureApi             featureApi;
    @Autowired
    protected StableDiffusionService stableDiffusionService;

    @Tool(description = "Create a new feature for a version.", resultConverter = KassandraToolCallResultConverter.class)
    public FeatureDto createFeature(
            @ToolParam(description = "Unique feature name") String name,
            @ToolParam(description = "The versionId this feature belongs to") Long versionId,
            @ToolParam(description = "Stable-diffusion prompt for the avatar", required = false) String avatarPrompt) {
        Feature feature = new Feature();
        feature.setName(name);
        feature.setVersionId(versionId);
        if (avatarPrompt == null || avatarPrompt.isEmpty()) {
            avatarPrompt = feature.getDefaultAvatarPrompt();
        }
        GeneratedImageResult image;
        if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
            try {
                image = generateFeatureAvatar(name);
            } catch (StableDiffusionException e) {
                log.warn("Failed to generate image for feature {}: {}", name, e.getMessage());
                image = stableDiffusionService.generateDefaultAvatar("lightbulb");
            }
        } else {
            log.warn("Stable Diffusion not available, using default avatar for feature: {}", name);
            image = stableDiffusionService.generateDefaultAvatar("lightbulb");
        }
        feature.setAvatarHash(AvatarUtil.computeHash(image.getResizedImage()));
        Feature savedFeature = featureApi.persist(feature);
        featureApi.updateAvatarFull(savedFeature.getId(), image.getResizedImage(), image.getOriginalImage(), image.getPrompt());
        ToolActivityContextHolder.reportActivity("created feature '" + savedFeature.getName() + "' with ID: " + savedFeature.getId());
        return FeatureDto.from(savedFeature);
    }

    @Tool(description = "Delete a feature by its featureId.", resultConverter = KassandraToolCallResultConverter.class)
    public void deleteFeature(
            @ToolParam(description = "The featureId") Long featureId) {
        Feature feature = featureApi.getById(featureId);
        if (feature == null) {
            throw new IllegalArgumentException("Feature not found with ID: " + featureId);
        }
        ToolActivityContextHolder.reportActivity("Deleting feature '" + feature.getName() + "' (ID: " + featureId + ")");
        featureApi.deleteById(featureId);
        ToolActivityContextHolder.reportActivity("Deleted feature '" + feature.getName() + "' (ID: " + featureId + ")");
    }

    private @NonNull GeneratedImageResult generateFeatureAvatar(String name) throws StableDiffusionException {
        String prompt = Feature.getDefaultAvatarPrompt(name);
        log.trace("Generating image for feature: {} with prompt: {}", name, prompt);
        return stableDiffusionService.generateImageWithOriginal(prompt);
    }

    @Tool(description = "Get all features accessible to the current user.", resultConverter = KassandraToolCallResultConverter.class)
    public List<FeatureDto> getAllFeatures() {
        List<Feature> features = featureApi.getAll();
        ToolActivityContextHolder.reportActivity("Found " + features.size() + " features.");
        return features.stream().map(FeatureDto::from).collect(Collectors.toList());
    }

    @Tool(description = "Get all features for a version.", resultConverter = KassandraToolCallResultConverter.class)
    public List<FeatureDto> getAllFeaturesByVersionId(
            @ToolParam(description = "The versionId") Long versionId) {
        List<Feature> features = featureApi.getAll(versionId);
        ToolActivityContextHolder.reportActivity("Found " + features.size() + " features for version " + versionId + ".");
        return features.stream().map(FeatureDto::from).collect(Collectors.toList());
    }

    @Tool(description = "Get a feature by its featureId.", resultConverter = KassandraToolCallResultConverter.class)
    public FeatureDto getFeatureById(
            @ToolParam(description = "The featureId") Long featureId) {
        Feature feature = featureApi.getById(featureId);
        if (feature == null) {
            throw new IllegalArgumentException("Feature not found with ID: " + featureId);
        }
        return FeatureDto.from(feature);
    }

    @Tool(description = "Get a feature by name within a version.", resultConverter = KassandraToolCallResultConverter.class)
    public FeatureDto getFeatureByName(
            @ToolParam(description = "The versionId the feature belongs to") Long versionId,
            @ToolParam(description = "The feature name") String name) {
        return featureApi.getByName(versionId, name)
                .map(FeatureDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Feature '" + name + "' not found in version " + versionId));
    }

    @Tool(description = "Update a feature name by its featureId.", resultConverter = KassandraToolCallResultConverter.class)
    public FeatureDto updateFeature(
            @ToolParam(description = "The featureId") Long id,
            @ToolParam(description = "The new feature name") String name) {
        Feature feature = featureApi.getById(id);
        if (feature == null) {
            throw new IllegalArgumentException("Feature not found with ID: " + id);
        }
        ToolActivityContextHolder.reportActivity("Updating feature " + id + " with name: " + name);
        feature.setName(name);
        featureApi.update(feature);
        return FeatureDto.from(feature);
    }
}
