/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class Feature extends AbstractTimeAware implements Comparable<Feature> {
    private String darkAvatarHash;
    private UUID   id;
    private String lightAvatarHash;
    private String name;

    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private List<Sprint> sprints = new ArrayList<>();

    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private Version version;

    private UUID versionId;

    public void addSprint(Sprint sprint) {
        sprint.setFeature(this);
        sprints.add(sprint);
    }

    @Override
    public int compareTo(Feature other) {
        return this.id.compareTo(other.id);
    }

    /**
     * Get the avatar URL for the given theme variant.
     * When {@code dark} is {@code true} and a dark avatar has been generated, the dark variant URL is
     * returned. Falls back to the light variant URL when no dark avatar is available yet.
     *
     * @param dark {@code true} to request the dark-background avatar variant
     * @return The avatar URL with hash parameter for cache-busting
     */
    @JsonIgnore
    public String getAvatarUrl(boolean dark) {
        if (dark && darkAvatarHash != null && !darkAvatarHash.isEmpty()) {
            return "/frontend/dark-avatar-proxy/feature/" + id + "?h=" + darkAvatarHash;
        }
        String url = "/frontend/avatar-proxy/feature/" + id;
        if (lightAvatarHash != null && !lightAvatarHash.isEmpty()) {
            url += "?h=" + lightAvatarHash;
        }
        return url;
    }

    /**
     * Get the light avatar URL with hash parameter for proper caching.
     * Delegates to {@link #getAvatarUrl(boolean)} with {@code dark = false}.
     *
     * @return The avatar URL with hash parameter if hash is available, otherwise just the base URL
     */
    @JsonIgnore
    public String getAvatarUrl() {
        return getAvatarUrl(false);
    }

    private static String getDefaultAvatarPrompt(String featureName) {
        return "app icon '" + featureName + "'" + StableDiffusionService.LORA;
    }

    /**
     * Return the default negative prompt used when generating dark-background avatars.
     *
     * @return The default dark negative prompt string
     */
    public static String getDefaultDarkAvatarNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT;
    }

    /**
     * Generate a default dark-background avatar prompt by appending the dark suffix to the base prompt.
     *
     * @return A default dark avatar prompt string
     */
    @JsonIgnore
    public String getDefaultDarkAvatarPrompt() {
        return getDefaultDarkAvatarPrompt(name);
    }

    /**
     * Generate a default dark-background avatar prompt for a given feature name.
     *
     * @param featureName The name of the feature
     * @return A default dark avatar prompt string
     */
    @JsonIgnore
    public static String getDefaultDarkAvatarPrompt(String featureName) {
        return getDefaultAvatarPrompt(featureName) + AvatarService.DARK_PROMPT_SUFFIX;
    }

    /**
     * Return the default negative prompt used when generating light-background avatars.
     *
     * @return The default negative prompt string
     */
    public static String getDefaultLightAvatarNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT;
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This provides a consistent prompt format for feature avatars.
     *
     * @return A default prompt string for generating feature avatar images
     */
    @JsonIgnore
    public String getDefaultLightAvatarPrompt() {
        return getDefaultLightAvatarPrompt(name);
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This static method provides a consistent prompt format for feature avatars.
     *
     * @param featureName The name of the feature
     * @return A default prompt string for generating feature avatar images
     */
    public static String getDefaultLightAvatarPrompt(String featureName) {
        return getDefaultAvatarPrompt(featureName) + AvatarService.LIGHT_PROMPT_SUFFIX;
    }

    @JsonIgnore
    public String getKey() {
        return "F-" + id;
    }

    public void initialize(GanttContext gc) {
        sprints.clear();
        gc.allSprints.forEach(sprint -> {
            if (Objects.equals(sprint.getFeatureId(), id)) {
                addSprint(sprint);
            }
        });
        sprints.forEach(sprint -> sprint.initialize(gc));
    }

    public void removePrint(Sprint sprint) {
        sprints.remove(sprint);
    }
}
