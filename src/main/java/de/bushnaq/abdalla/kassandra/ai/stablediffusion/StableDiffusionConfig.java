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

package de.bushnaq.abdalla.kassandra.ai.stablediffusion;

import lombok.Data;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsService;
import de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Stable Diffusion WebUI API integration.
 */
@Configuration
@ConfigurationProperties(prefix = "stable-diffusion")
@Data
public class StableDiffusionConfig {

    @Autowired(required = false)
    private ServerSettingsService serverSettingsService;
    /**
     * Base URL of the Stable Diffusion WebUI API
     */
    private String                apiUrl                     = "http://localhost:7861";
    /**
     * Background color for dark-theme avatars, as a CSS hex string (e.g., "#000000").
     * Used by AvatarService to generate dark avatars with a matching background.
     * Configurable via 'stable-diffusion.avatar-dark-background-color'.
     */
    private String                avatarDarkBackgroundColor  = "#242323";
    /**
     * Background color for light-theme avatars, as a CSS hex string (e.g., "#FFFFFF").
     * Used by AvatarService to generate light avatars with a matching background.
     * Configurable via 'stable-diffusion.avatar-light-background-color'.
     */
    private String                avatarLightBackgroundColor = "#FFFFFF";
    /**
     * Final output size for AI-generated avatars (light and dark variants).
     * Defaults to 256 to preserve detail; differs from the generic {@code outputSize}.
     */
    private int                   avatarOutputSize           = 256;
    /**
     * CFG Scale (Classifier Free Guidance)
     */
    private double                cfgScale                   = 7.0;
    /**
     * Default denoising strength for image-to-image requests (0.0 = no change, 1.0 = fully new image)
     */
    private double                defaultDenoisingStrength   = 0.75;
    /**
     * Default sampler algorithm (optimized for SD3 Medium)
     */
    private String                defaultSampler             = "DPM++ 2M Karras";
    /**
     * Default number of sampling steps
     */
    private int                   defaultSteps               = 20;
    /**
     * Size to generate images at (before resizing)
     */
    private int                   generationSize             = 512;
    /**
     * Timeout in seconds for model-loading requests ({@code POST /sdapi/v1/options}).
     * Loading a new checkpoint can take several minutes; this timeout must be large enough
     * to cover the full load time. Configurable via {@code stable-diffusion.model-load-timeout-seconds}.
     */
    private int                   modelLoadTimeoutSeconds    = 400;
    private String                modelName                  = "realisticVisionV60B1_v51HyperVAE.safetensors";
    /**
     * Final output size for avatars/icons
     */
    private int                   outputSize                 = 64;
    /**
     * Timeout in seconds for API requests
     */
    private int                   timeoutSeconds             = 120;

    /**
     * Gets the current Stable Diffusion API URL.
     *
     * @return current API URL
     */
    public String getApiUrl() {
        return value(Keys.STABLE_DIFFUSION_API_URL, apiUrl);
    }

    /**
     * Gets the current dark-avatar background colour.
     *
     * @return CSS hex colour
     */
    public String getAvatarDarkBackgroundColor() {
        return value(Keys.STABLE_DIFFUSION_AVATAR_DARK_BACKGROUND_COLOR, avatarDarkBackgroundColor);
    }

    /**
     * Gets the current light-avatar background colour.
     *
     * @return CSS hex colour
     */
    public String getAvatarLightBackgroundColor() {
        return value(Keys.STABLE_DIFFUSION_AVATAR_LIGHT_BACKGROUND_COLOR, avatarLightBackgroundColor);
    }

    /**
     * Gets the current avatar output size.
     *
     * @return output size in pixels
     */
    public int getAvatarOutputSize() {
        return integer(Keys.STABLE_DIFFUSION_AVATAR_OUTPUT_SIZE, avatarOutputSize);
    }

    /**
     * Gets the current CFG scale.
     *
     * @return configured CFG scale
     */
    public double getCfgScale() {
        return decimal(Keys.STABLE_DIFFUSION_CFG_SCALE, cfgScale);
    }

    /**
     * Gets the current default denoising strength.
     *
     * @return configured denoising strength
     */
    public double getDefaultDenoisingStrength() {
        return decimal(Keys.STABLE_DIFFUSION_DEFAULT_DENOISING_STRENGTH, defaultDenoisingStrength);
    }

    /**
     * Gets the current default sampler.
     *
     * @return configured sampler
     */
    public String getDefaultSampler() {
        return value(Keys.STABLE_DIFFUSION_DEFAULT_SAMPLER, defaultSampler);
    }

    /**
     * Gets the current default number of sampling steps.
     *
     * @return configured sampling steps
     */
    public int getDefaultSteps() {
        return integer(Keys.STABLE_DIFFUSION_DEFAULT_STEPS, defaultSteps);
    }

    /**
     * Gets the current image generation size.
     *
     * @return generation size in pixels
     */
    public int getGenerationSize() {
        return integer(Keys.STABLE_DIFFUSION_GENERATION_SIZE, generationSize);
    }

    /**
     * Gets the current model-load timeout.
     *
     * @return timeout in seconds
     */
    public int getModelLoadTimeoutSeconds() {
        return integer(Keys.STABLE_DIFFUSION_MODEL_LOAD_TIMEOUT_SECONDS, modelLoadTimeoutSeconds);
    }

    /**
     * Gets the current Stable Diffusion model name.
     *
     * @return configured model name
     */
    public String getModelName() {
        return value(Keys.STABLE_DIFFUSION_MODEL_NAME, modelName);
    }

    /**
     * Gets the current generic output size.
     *
     * @return output size in pixels
     */
    public int getOutputSize() {
        return integer(Keys.STABLE_DIFFUSION_OUTPUT_SIZE, outputSize);
    }

    /**
     * Gets the current request timeout.
     *
     * @return timeout in seconds
     */
    public int getTimeoutSeconds() {
        return integer(Keys.STABLE_DIFFUSION_TIMEOUT_SECONDS, timeoutSeconds);
    }

    private double decimal(String key, double fallback) {
        return Double.parseDouble(value(key, Double.toString(fallback)));
    }

    private int integer(String key, int fallback) {
        return Integer.parseInt(value(key, Integer.toString(fallback)));
    }

    private String value(String key, String fallback) {
        return serverSettingsService == null ? fallback : serverSettingsService.value(key, fallback);
    }
}
