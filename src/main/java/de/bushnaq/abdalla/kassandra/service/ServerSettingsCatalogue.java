/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declares the supported administrator-managed server settings.
 */
@Component
public class ServerSettingsCatalogue {

    /**
     * Settings categories displayed by the administration UI.
     */
    public static final Category                AI_MODELS        = new Category("ai-models", "AI / Models", "MAGIC", 2, false, true);
    /**
     * Settings categories displayed by the administration UI.
     */
    public static final Category                GENERAL          = new Category("general", "General", "COG", 0, false, true);
    /**
     * Settings categories displayed by the administration UI.
     */
    public static final Category                LM_STUDIO        = new Category("lm-studio", "AI / LM Studio", "CLOUD", 4, true, false);
    /**
     * Settings categories displayed by the administration UI.
     */
    public static final Category                LOGGING          = new Category("logging", "Logging", "FILE_TEXT", 1, false, true);
    /**
     * Settings categories displayed by the administration UI.
     */
    public static final Category                OPENAI           = new Category("openai", "AI / OpenAI", "CLOUD", 3, false, true);
    /**
     * Settings categories displayed by the administration UI.
     */
    public static final Category                STABLE_DIFFUSION = new Category("stable-diffusion", "AI / Stable Diffusion", "PALETTE", 5, true, false);
    private final       Map<String, Definition> definitions      = definitions();

    private static void add(Map<String, Definition> definitions, Definition definition) {
        definitions.put(definition.key(), definition);
    }

    private static DefinitionBuilder bool(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.BOOLEAN);
    }

    private static DefinitionBuilder colour(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.COLOUR);
    }

    private static DefinitionBuilder decimal(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.DECIMAL);
    }

    private static Map<String, Definition> definitions() {
        Map<String, Definition> result = new LinkedHashMap<>();
        // General application behaviour.
        add(result, integer("kassandra.holidays.look-ahead-months", GENERAL, "Holiday look-ahead months", "Number of months used when calculating holidays for user locations.")
                .defaultValue(24)
                .minimum(1)
                .maximum(120)
                .build());
        add(result, integer("kassandra.undo-redo.history-limit", GENERAL, "Planning history limit", "Maximum number of operations displayed in the planning history panel.")
                .defaultValue(5)
                .minimum(1)
                .maximum(100)
                .build());

        // Runtime diagnostic logging.
        add(result, logLevel("logging.level.de.bushnaq.abdalla.kassandra.ai.mcp.ContextPropagatingToolCallbackProvider",
                LOGGING, "MCP tool callback logging level", "Controls logging of serialized tool responses sent to the AI assistant.")
                .defaultValue("DEBUG")
                .options("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF")
                .build());

        // Per-request AI model options.
        add(result, text("kassandra.ai.insights-model", AI_MODELS, "Insights model", "Model identifier used to generate sprint insights.")
                .defaultValue("qwen/qwen3.5-9b")
                .maximumLength(200)
                .build());
        add(result, text("kassandra.ai.filter-model", AI_MODELS, "Filter model", "Model identifier used to generate AI filters.")
                .defaultValue("qwen3.5-4b@q8_0")
                .maximumLength(200)
                .build());
        add(result, text("kassandra.ai.mcp-model", AI_MODELS, "Assistant model", "Model identifier used by the AI assistant.")
                .defaultValue("ministral-3-8b-reasoning-2512")
                .maximumLength(200)
                .build());
        add(result, decimal("kassandra.ai.temperature", AI_MODELS, "Temperature", "Sampling temperature used by the configured AI model.")
                .defaultValue(0)
                .minimum(0)
                .maximum(2)
                .build());
        add(result, integer("kassandra.ai.max-tokens", AI_MODELS, "Maximum tokens", "Maximum number of tokens generated by AI requests.")
                .defaultValue(20480)
                .minimum(1)
                .maximum(131072)
                .build());
        add(result, integer("kassandra.ai.seed", AI_MODELS, "Random seed", "Optional seed for reproducible AI output.")
                .defaultValue(42)
                .minimum(0)
                .maximum(Integer.MAX_VALUE)
                .build());

        // OpenAI-compatible chat-completion connection. The client is created at application startup.
        add(result, url("kassandra.openai.base-url", OPENAI, "Base URL", "Base URL of the OpenAI-compatible chat-completion API.")
                .defaultValue("http://localhost:1234")
                .maximumLength(2048)
                .restartRequired()
                .build());
        add(result, password("kassandra.openai.api-key", OPENAI, "API key", "Optional OpenAI-compatible API credential. It is encrypted and never displayed after saving.")
                .restartRequired()
                .build());

        // LM Studio native API connection and model-load configuration.
        add(result, url("kassandra.lm-studio.api-url", LM_STUDIO, "API URL", "Base URL of the LM Studio native API.")
                .defaultValue("http://localhost:1234")
                .maximumLength(2048)
                .testable()
                .build());
        add(result, password("kassandra.lm-studio.api-key", LM_STUDIO, "API key", "Optional LM Studio API credential. It is encrypted and never displayed after saving.")
                .build());
        add(result, integer("kassandra.lm-studio.context-length", LM_STUDIO, "Context length", "Maximum context length passed when loading a model. Zero uses the model default.")
                .defaultValue(20480)
                .minimum(0)
                .maximum(131072)
                .build());
        add(result, bool("kassandra.lm-studio.flash-attention", LM_STUDIO, "Enable Flash Attention", "Enable Flash Attention when LM Studio loads a model.")
                .defaultValue(true)
                .build());
        add(result, bool("kassandra.lm-studio.offload-kv-cache-to-gpu", LM_STUDIO, "Offload KV cache to GPU", "Offload the LM Studio key-value cache to GPU memory.")
                .defaultValue(true)
                .build());
        add(result, integer("kassandra.lm-studio.timeout-seconds", LM_STUDIO, "Request timeout", "Maximum duration in seconds for LM Studio API requests.")
                .defaultValue(300)
                .minimum(1)
                .maximum(900)
                .build());

        // Stable Diffusion image-generation defaults and service connection.
        add(result, url("stable-diffusion.api-url", STABLE_DIFFUSION, "API URL", "Base URL of the Stable Diffusion WebUI API.")
                .defaultValue("http://localhost:7861")
                .maximumLength(2048)
                .testable()
                .build());
        add(result, integer("stable-diffusion.timeout-seconds", STABLE_DIFFUSION, "Request timeout", "Maximum duration in seconds for Stable Diffusion API requests.")
                .defaultValue(60)
                .minimum(1)
                .maximum(900)
                .build());
        add(result, integer("stable-diffusion.model-load-timeout-seconds", STABLE_DIFFUSION, "Model load timeout", "Maximum duration in seconds to wait for a Stable Diffusion model to load.")
                .defaultValue(400)
                .minimum(1)
                .maximum(1800)
                .build());
        add(result, integer("stable-diffusion.default-steps", STABLE_DIFFUSION, "Default sampling steps", "Default number of diffusion sampling steps.")
                .defaultValue(20)
                .minimum(1)
                .maximum(150)
                .build());
        add(result, decimal("stable-diffusion.cfg-scale", STABLE_DIFFUSION, "CFG scale", "Classifier-free guidance scale.")
                .defaultValue(7)
                .minimum(0)
                .maximum(30)
                .build());
        add(result, decimal("stable-diffusion.default-denoising-strength", STABLE_DIFFUSION, "Denoising strength", "Default strength for image-to-image generation.")
                .defaultValue(.75)
                .minimum(0)
                .maximum(1)
                .build());
        add(result, text("stable-diffusion.default-sampler", STABLE_DIFFUSION, "Default sampler", "Sampler algorithm used for image generation.")
                .defaultValue("DPM++ 2M Karras")
                .maximumLength(100)
                .build());
        add(result, integer("stable-diffusion.generation-size", STABLE_DIFFUSION, "Generation size", "Square pixel size used before resizing generated images.")
                .defaultValue(512)
                .minimum(64)
                .maximum(2048)
                .build());
        add(result, integer("stable-diffusion.output-size", STABLE_DIFFUSION, "Output size", "Final square pixel size for generic generated images.")
                .defaultValue(64)
                .minimum(16)
                .maximum(2048)
                .build());
        add(result, integer("stable-diffusion.avatar-output-size", STABLE_DIFFUSION, "Avatar output size", "Final square pixel size for generated avatars.")
                .defaultValue(256)
                .minimum(16)
                .maximum(2048)
                .build());
        add(result, text("stable-diffusion.model-name", STABLE_DIFFUSION, "Model name", "Stable Diffusion checkpoint file name.")
                .defaultValue("xl/bonoboXL_v20.safetensors")
                .maximumLength(200)
                .build());
        add(result, colour("stable-diffusion.avatar-dark-background-color", STABLE_DIFFUSION, "Dark avatar background", "CSS hex colour used behind avatars in the dark theme.")
                .defaultValue("#242323")
                .build());
        add(result, colour("stable-diffusion.avatar-light-background-color", STABLE_DIFFUSION, "Light avatar background", "CSS hex colour used behind avatars in the light theme.")
                .defaultValue("#FFFFFF")
                .build());
        return java.util.Collections.unmodifiableMap(result);
    }

    /**
     * Gets a setting definition by its stable key.
     *
     * @param key stable setting key
     * @return setting definition
     * @throws IllegalArgumentException when the key is unsupported
     */
    public Definition get(String key) {
        Definition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported server setting: " + key);
        }
        return definition;
    }

    /**
     * Gets a settings category by its stable key.
     *
     * @param key stable category key
     * @return category definition
     * @throws IllegalArgumentException when the key is unsupported
     */
    public Category getCategory(String key) {
        return list().stream()
                .map(Definition::category)
                .distinct()
                .filter(category -> category.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported server settings category: " + key));
    }

    private static DefinitionBuilder integer(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.INTEGER);
    }

    /**
     * Lists setting definitions in their display order.
     *
     * @return immutable setting definitions
     */
    public List<Definition> list() {
        return List.copyOf(definitions.values());
    }

    private static DefinitionBuilder logLevel(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.LOG_LEVEL);
    }

    private static DefinitionBuilder password(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.PASSWORD).secret();
    }

    private static DefinitionBuilder text(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.TEXT);
    }

    private static DefinitionBuilder url(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.URL);
    }

    /**
     * Defines the editor type used by the dynamic settings view.
     */
    public enum Type {
        BOOLEAN, COLOUR, DECIMAL, INTEGER, LOG_LEVEL, PASSWORD, TEXT, URL
    }

    /**
     * Defines one category shared by related server settings.
     */
    public record Category(String key, String label, String icon, int order, boolean allowDisable,
                           boolean enabledByDefault) {
    }

    /**
     * Defines the presentation and validation contract of one setting.
     */
    public record Definition(String key, Category category, String label, String description, Type type,
                             String defaultValue,
                             boolean secret, boolean restartRequired, Double minimum, Double maximum,
                             List<String> options, boolean testable) {
    }

    /**
     * Builds a readable declarative setting definition.
     */
    private static final class DefinitionBuilder {

        private final Category     category;
        private       String       defaultValue;
        private final String       description;
        private final String       key;
        private final String       label;
        private       Double       maximum;
        private       Double       minimum;
        private       List<String> options = List.of();
        private       boolean      restartRequired;
        private       boolean      secret;
        private       boolean      testable;
        private final Type         type;

        private DefinitionBuilder(String key, Category category, String label, String description, Type type) {
            this.category    = category;
            this.description = description;
            this.key         = key;
            this.label       = label;
            this.type        = type;
        }

        private Definition build() {
            return new Definition(key, category, label, description, type, defaultValue, secret, restartRequired,
                    minimum, maximum, options, testable);
        }

        private DefinitionBuilder defaultValue(boolean defaultValue) {
            this.defaultValue = Boolean.toString(defaultValue);
            return this;
        }

        private DefinitionBuilder defaultValue(double defaultValue) {
            this.defaultValue = Double.toString(defaultValue);
            return this;
        }

        private DefinitionBuilder defaultValue(long defaultValue) {
            this.defaultValue = Long.toString(defaultValue);
            return this;
        }

        private DefinitionBuilder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        private DefinitionBuilder maximum(double maximum) {
            this.maximum = maximum;
            return this;
        }

        private DefinitionBuilder maximumLength(int maximumLength) {
            this.maximum = (double) maximumLength;
            return this;
        }

        private DefinitionBuilder minimum(double minimum) {
            this.minimum = minimum;
            return this;
        }

        private DefinitionBuilder options(String... options) {
            this.options = List.of(options);
            return this;
        }

        private DefinitionBuilder restartRequired() {
            this.restartRequired = true;
            return this;
        }

        private DefinitionBuilder secret() {
            this.defaultValue = "";
            this.secret       = true;
            return this;
        }

        private DefinitionBuilder testable() {
            this.testable = true;
            return this;
        }
    }
}
