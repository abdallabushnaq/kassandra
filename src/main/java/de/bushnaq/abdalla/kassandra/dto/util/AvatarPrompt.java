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
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;

public interface AvatarPrompt {
    /**
     * Returns the avatar URL for the requested theme.
     *
     * @param dark whether to use the dark theme
     * @return the avatar URL
     */
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

    /**
     * Creates a theme-independent portrait description.
     *
     * @param name the person's name
     * @return the base avatar prompt
     */
    static String getDefaultAvatarPrompt(String name) {
        return "Professional avatar portrait of '" + name + "', software developer";
    }

    /**
     * Returns the default exclusions shared by both avatar variants.
     *
     * @return the avatar negative prompt
     */
    static String getDefaultAvatarNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT;
    }

    /**
     * Creates a theme-independent portrait description for this entity.
     *
     * @return the base avatar prompt
     */
    @JsonIgnore
    default String getDefaultAvatarPrompt() {
        return getDefaultAvatarPrompt(getName());
    }

    /**
     * Get the name of the entity associated with this prompt. To be overwritten by the entity implementing this interface
     *
     * @return The name as a String
     */
    String getName();
}
