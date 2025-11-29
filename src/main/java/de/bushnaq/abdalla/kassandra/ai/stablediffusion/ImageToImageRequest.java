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

/*
 * Request DTO for Stable Diffusion image-to-image API
 */
package de.bushnaq.abdalla.kassandra.ai.stablediffusion;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageToImageRequest {
    /**
     * Batch size
     */
    @JsonProperty("batch_size")
    private Integer  batchSize;
    /**
     * CFG Scale (Classifier Free Guidance)
     */
    @JsonProperty("cfg_scale")
    private Double   cfgScale;
    /**
     * Height of the generated image
     */
    private Integer  height;
    /**
     * The base64-encoded initial image
     */
    @JsonProperty("init_images")
    private String[] initImages;
    /**
     * The negative prompt describing what to avoid
     */
    @JsonProperty("negative_prompt")
    private String   negativePrompt;
    /**
     * The positive prompt describing what to generate
     */
    private String   prompt;
    /**
     * Sampler algorithm to use
     */
    @JsonProperty("sampler_name")
    private String   samplerName;
    /**
     * Random seed for reproducibility (optional)
     */
    private Long     seed;
    /**
     * Number of sampling steps
     */
    private Integer  steps;
    /**
     * Width of the generated image
     */
    private Integer  width;
}

