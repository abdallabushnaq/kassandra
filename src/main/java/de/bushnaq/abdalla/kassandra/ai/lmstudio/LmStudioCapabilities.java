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

/**
 * Model capabilities as returned by the LM Studio {@code GET /api/v1/models} endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LmStudioCapabilities(

        /** Whether the model supports vision / image inputs. */
        boolean vision,

        /** Whether the model was trained for tool / function calling. */
        @JsonProperty("trained_for_tool_use")
        boolean trainedForToolUse
) {
}

