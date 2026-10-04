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

import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;

public interface HeaderPrompt extends IconPrompt {

    /**
     * Generate a default dark-background header prompt for a given product name.
     *
     * @param productName The name of the product
     * @return A default dark header prompt string
     */
    static String getDefaultDarkHeaderPrompt(String productName) {
        return getDefaultHeaderPrompt(productName) + AvatarService.DARK_PROMPT_SUFFIX;
    }

    private static String getDefaultHeaderPrompt(String productName) {
        return "panoramic view of (" + productName + ":2), web page header, detailed, high resolution";
    }

    /**
     * Generate a default light-background header prompt for a given product name.
     *
     * @param productName The name of the product
     * @return A default light header prompt string
     */
    static String getDefaultLightHeaderPrompt(String productName) {
        return getDefaultHeaderPrompt(productName) + AvatarService.LIGHT_PROMPT_SUFFIX;
    }

}
