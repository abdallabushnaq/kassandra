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

package de.bushnaq.abdalla.kassandra.ai.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared configuration for entity-specific prompts across all AI filter implementations.
 */
public class AiFilterConfig {

    private static final Map<String, PromptConfig> PROMPT_CONFIGS = initializePromptConfigs();

    /**
     * Gets the prompt configuration for the specified entity type.
     *
     * @param entityType The entity type (e.g., "Product", "Version")
     * @return The prompt configuration, or Product config as fallback
     */
    public static PromptConfig getPromptConfig(String entityType) {
        return PROMPT_CONFIGS.getOrDefault(entityType, PROMPT_CONFIGS.get("Product"));
    }

    /**
     * Initialize prompt configurations for different entity types
     */
    private static Map<String, PromptConfig> initializePromptConfigs() {
        Map<String, PromptConfig> configs = new HashMap<>();

        // Load configurations from separate config classes
        configs.put("Product", ProductConfig.getConfig());
        configs.put("Version", VersionConfig.getConfig());
        configs.put("Feature", FeatureConfig.getConfig());
        configs.put("Sprint", SprintConfig.getConfig());
        configs.put("User", UserConfig.getConfig());
        configs.put("Availability", AvailabilityConfig.getConfig());
        configs.put("Location", LocationConfig.getConfig());
        configs.put("OffDay", OffDayConfig.getConfig());

        return configs;
    }

    /**
     * Configuration class for entity-specific prompts
     */
    public static class PromptConfig {
        public final String javaClass;
        public final String javaExamples;
        public final String javascriptExamples;
        public final String specialConsiderations;

//        public PromptConfig(String javaClass, String specialConsiderations, String javaExamples) {
//            this.javaClass             = javaClass;
//            this.specialConsiderations = specialConsiderations;
//            this.javascriptExamples    = null;
//            this.javaExamples          = javaExamples;
//        }

        public PromptConfig(String javaClass, String specialConsiderations, String javascriptExamples, String javaExamples) {
            this.javaClass             = javaClass;
            this.specialConsiderations = specialConsiderations;
            this.javascriptExamples    = javascriptExamples;
            this.javaExamples          = javaExamples;
        }
    }
}
