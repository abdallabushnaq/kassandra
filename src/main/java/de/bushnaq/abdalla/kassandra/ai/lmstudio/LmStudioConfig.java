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

package de.bushnaq.abdalla.kassandra.ai.lmstudio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for LM Studio API integration.
 * Controls model lifecycle management (load/unload) via the LM Studio native REST API.
 */
@Configuration
@ConfigurationProperties(prefix = "lm-studio")
@Data
public class LmStudioConfig {

    /**
     * Optional Bearer token for LM Studio authentication.
     * Leave empty to skip the Authorization header (LM Studio default has no auth).
     */
    private String apiKey = "";
    /**
     * Base URL of the LM Studio server (native API, not the OpenAI-compatible endpoint).
     * The native API lives at /api/v1/* on the same host/port as the OpenAI-compatible API.
     */
    private String apiUrl = "http://localhost:1234";
    /**
     * Maximum context length (in tokens) to use when loading a model.
     * 0 means "use the model's default".
     */
    private int contextLength = 0;

    /**
     * Whether to enable Flash Attention when loading a model.
     * Reduces memory usage and improves generation speed on supported hardware.
     */
    private boolean flashAttention = true;

    /**
     * Whether to offload the KV cache to GPU memory when loading a model.
     */
    private boolean offloadKvCacheToGpu = true;

    /**
     * Timeout in seconds for API requests to LM Studio.
     */
    private int timeoutSeconds = 300;
}

