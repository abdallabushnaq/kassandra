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

import java.util.List;

/**
 * Response DTO from Stable Diffusion text-to-image API
 */
@Data
public class ImageGenerationResponse {

    /**
     * List of generated images in base64 format
     */
    private List<String> images;
    /**
     * Information about the generation process
     */
    private String info;
    /**
     * Generation parameters used
     */
    private Object parameters;
}

