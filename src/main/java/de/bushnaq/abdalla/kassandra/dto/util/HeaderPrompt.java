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

public interface HeaderPrompt extends IconPrompt {

    /**
     * Returns the fixed exclusions used for both header variants.
     *
     * @return the header negative prompt
     */
    static String getDefaultHeaderNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT + ", person";
    }

    /**
     * Creates a theme-independent header description without style instructions.
     *
     * @param productName The name of the product
     * @return the base header prompt
     */
    static String getDefaultHeaderPrompt(String productName) {
        return "panoramic view of (" + productName + ":2), web page header, detailed, high resolution";
    }

    /**
     * Creates a theme-independent header description for this entity.
     *
     * @return the base header prompt
     */
    @JsonIgnore
    default String getDefaultHeaderPrompt() {
        return getDefaultHeaderPrompt(getName());
    }

}
