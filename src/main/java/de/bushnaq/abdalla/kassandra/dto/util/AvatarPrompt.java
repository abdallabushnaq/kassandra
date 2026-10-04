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

package de.bushnaq.abdalla.kassandra.dto.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;

public interface AvatarPrompt {
    String getAvatarUrl(boolean dark);

    /**
     * Get the light avatar URL with hash parameter for proper caching.
     * Delegates to {@link #getAvatarUrl(boolean)} with {@code dark = false}.
     *
     * @return The avatar URL with hash parameter if hash is available, otherwise just the base URL
     */
    @JsonIgnore
    default String getAvatarUrl() {
        return getAvatarUrl(false);
    }

    static String getDefaultAvatarPrompt(String name) {
        return "Professional avatar portrait of '" + name + "', software developer";
    }

    /**
     * Return the default negative prompt used when generating dark-background avatars.
     *
     * @return The default dark negative prompt string
     */
    static String getDefaultDarkAvatarNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT;
    }

    /**
     * Generate a default dark-background avatar prompt for a given product name.
     *
     * @param productName The name of the product
     * @return A default dark avatar prompt string
     */
    static String getDefaultDarkAvatarPrompt(String productName) {
        return getDefaultAvatarPrompt(productName) + AvatarService.DARK_PROMPT_SUFFIX;
    }

    /**
     * Generate a default dark-background avatar prompt by appending the dark suffix to the base prompt.
     *
     * @return A default dark avatar prompt string
     */
    @JsonIgnore
    default String getDefaultDarkAvatarPrompt() {
        return getDefaultDarkAvatarPrompt(getName());
    }

    /**
     * Return the default negative prompt used when generating light-background avatars.
     *
     * @return The default negative prompt string
     */
    @JsonIgnore
    static String getDefaultLightAvatarNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT;
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This static method provides a consistent prompt format for product avatars.
     *
     * @param productName The name of the product
     * @return A default prompt string for generating product avatar images
     */
    static String getDefaultLightAvatarPrompt(String productName) {
        return getDefaultAvatarPrompt(productName) + AvatarService.LIGHT_PROMPT_SUFFIX;
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This provides a consistent prompt format for product avatars.
     *
     * @return A default prompt string for generating product avatar images
     */
    @JsonIgnore
    default String getDefaultLightAvatarPrompt() {
        return getDefaultLightAvatarPrompt(getName());
    }

    /**
     * Get the name of the entity associated with this prompt. To be overwritten by the entity implementing this interface
     *
     * @return The name as a String
     */
    String getName();
}
