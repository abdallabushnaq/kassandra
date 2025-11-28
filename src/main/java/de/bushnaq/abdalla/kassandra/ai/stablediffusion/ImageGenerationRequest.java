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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for Stable Diffusion text-to-image API
 */
@Data
@Builder
public class ImageGenerationRequest {

    /**
     * Number of images to generate in batch
     */
    @JsonProperty("batch_size")
    private Integer batchSize;
    /**
     * CFG Scale (Classifier Free Guidance)
     */
    @JsonProperty("cfg_scale")
    private Double cfgScale;
    /**
     * Height of the generated image
     */
    private Integer height;
    /**
     * The negative prompt describing what to avoid
     */
    @JsonProperty("negative_prompt")
    private String negativePrompt;
    /**
     * The positive prompt describing what to generate
     */
    private String prompt;
    /**
     * Sampler algorithm to use
     */
    @JsonProperty("sampler_name")
    private String samplerName;
    /**
     * Random seed for reproducibility (optional)
     */
    private Long seed;
    /**
     * Number of sampling steps
     */
    private Integer steps;
    /**
     * Width of the generated image
     */
    private Integer width;
}

