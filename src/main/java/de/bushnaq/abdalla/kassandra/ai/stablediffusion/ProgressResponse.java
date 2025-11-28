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
import lombok.Data;

/**
 * Response from Stable Diffusion /sdapi/v1/progress endpoint.
 */
@Data
public class ProgressResponse {

    /**
     * Current preview image (base64 encoded, if available)
     */
    @JsonProperty("current_image")
    private String currentImage;
    /**
     * Estimated time remaining in seconds
     */
    @JsonProperty("eta_relative")
    private double etaRelative;
    /**
     * Current progress (0.0 to 1.0)
     */
    private double progress;
    /**
     * Current state information
     */
    private State state;

    @Data
    public static class State {
        /**
         * Whether generation was interrupted
         */
        private boolean interrupted;
        /**
         * Current job description
         */
        private String job;
        /**
         * Job count
         */
        @JsonProperty("job_count")
        private int jobCount;
        /**
         * Job number
         */
        @JsonProperty("job_no")
        private int jobNo;
        /**
         * Sampling step
         */
        @JsonProperty("sampling_step")
        private int samplingStep;
        /**
         * Sampling steps total
         */
        @JsonProperty("sampling_steps")
        private int samplingSteps;
        /**
         * Whether generation is currently skipped
         */
        private boolean skipped;
    }
}

