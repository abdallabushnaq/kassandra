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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@JsonIdentityInfo(
        scope = Product.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class Product extends AbstractTimeAware implements Comparable<Product> {

    private String        darkAvatarHash;
    private Long          id;
    private String        lightAvatarHash;
    private String        name;
    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private List<Version> versions = new ArrayList<>();

    public void addVersion(Version version) {
        versions.add(version);
        version.setProduct(this);
    }

    @Override
    public int compareTo(Product other) {
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
            return "/frontend/dark-avatar-proxy/product/" + id + "?h=" + darkAvatarHash;
        }
        String url = "/frontend/avatar-proxy/product/" + id;
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

    private static String getDefaultAvatarPrompt(String productName) {
        return "Detailed representation of '" + productName + "', studio lighting, reflective highlights, high detail, 8k resolution, 50mm lens";
    }

    /**
     * Return the default negative prompt used when generating dark-background avatars.
     *
     * @return The default dark negative prompt string
     */
    public static String getDefaultDarkAvatarNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT + ", person";
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
     * Generate a default dark-background avatar prompt for a given product name.
     *
     * @param productName The name of the product
     * @return A default dark avatar prompt string
     */
    public static String getDefaultDarkAvatarPrompt(String productName) {
        return getDefaultAvatarPrompt(productName) + AvatarService.DARK_PROMPT_SUFFIX;
    }

    /**
     * Return the default negative prompt used when generating light-background avatars.
     *
     * @return The default negative prompt string
     */
    @JsonIgnore
    public static String getDefaultLightAvatarNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT + ", person";
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This provides a consistent prompt format for product avatars.
     *
     * @return A default prompt string for generating product avatar images
     */
    @JsonIgnore
    public String getDefaultLightAvatarPrompt() {
        return getDefaultLightAvatarPrompt(name);
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This static method provides a consistent prompt format for product avatars.
     *
     * @param productName The name of the product
     * @return A default prompt string for generating product avatar images
     */
    public static String getDefaultLightAvatarPrompt(String productName) {
        return getDefaultAvatarPrompt(productName) + AvatarService.LIGHT_PROMPT_SUFFIX;
    }

    @JsonIgnore
    public String getKey() {
        return "P-" + id;
    }

    public void initialize(GanttContext gc) {
        versions.clear();
        gc.allVersions.forEach(version -> {
            if (Objects.equals(version.getProductId(), id)) {
                addVersion(version);
            }
        });
        versions.forEach(version -> version.initialize(gc));
    }

    public void removeVersion(Version version) {
        versions.remove(version);
    }
}
