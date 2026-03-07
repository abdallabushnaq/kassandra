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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a single model entry returned by the LM Studio {@code GET /api/v1/models} endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LmStudioModel(

        /** Unique model key, e.g. {@code "mistralai/ministral-3-3b"}. */
        String key,

        /** Human-readable model name. */
        @JsonProperty("display_name")
        String displayName,

        /** {@code "llm"} or {@code "embedding"}. */
        String type,

        /** Model publisher name. */
        String publisher,

        /** Model architecture, e.g. {@code "mistral"}. May be null for embedding models. */
        String architecture,

        /** Model file format: {@code "gguf"}, {@code "mlx"}, or {@code null}. */
        String format,

        /** Size of the model file in bytes. */
        @JsonProperty("size_bytes")
        Long sizeBytes,

        /** Human-readable parameter count, e.g. {@code "7B"}. May be null. */
        @JsonProperty("params_string")
        String paramsString,

        /** Maximum context length supported by this model (in tokens). */
        @JsonProperty("max_context_length")
        Integer maxContextLength,

        /** Currently loaded instances of this model (empty list if not loaded). */
        @JsonProperty("loaded_instances")
        List<LmStudioLoadedInstance> loadedInstances,

        /** Model capabilities. Absent for embedding models. */
        LmStudioCapabilities capabilities
) {
    /**
     * Returns {@code true} if at least one instance of this model is currently loaded.
     */
    public boolean isLoaded() {
        return loadedInstances != null && !loadedInstances.isEmpty();
    }
}

