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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Stable Diffusion WebUI API integration.
 */
@Configuration
@ConfigurationProperties(prefix = "stable-diffusion")
@Data
public class StableDiffusionConfig {

    /**
     * Base URL of the Stable Diffusion WebUI API
     */
    private String apiUrl         = "http://localhost:7860";
    /**
     * CFG Scale (Classifier Free Guidance)
     */
    private double cfgScale       = 7.0;
    /**
     * Default sampler algorithm (optimized for SD3 Medium)
     */
    private String defaultSampler = "DPM++ 2M Karras";
    /**
     * Default number of sampling steps
     */
    private int    defaultSteps   = 20;
    /**
     * Size to generate images at (before resizing)
     */
    private int    generationSize = 512;
    /**
     * Final output size for avatars/icons
     */
    private int    outputSize     = 64;
    /**
     * Timeout in seconds for API requests
     */
    private int    timeoutSeconds = 60;
}

