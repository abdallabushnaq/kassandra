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

import de.bushnaq.abdalla.kassandra.config.KassandraProperties;
import de.bushnaq.abdalla.kassandra.dto.ServerSettingTestResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Declares the supported administrator-managed server settings.
 */
@Component
public class ServerSettingsCatalogue {

    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);

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
    @Autowired
    private             LoggingSystem           loggingSystem;
    private             Map<String, Definition> definitions;

    /**
     * Initializes setting definitions after infrastructure required by setting callbacks is available.
     */
    @PostConstruct
    public void initializeDefinitions() {
        definitions = definitions();
    }

    private static void add(Map<String, Definition> definitions, Definition definition) {
        definitions.put(definition.key(), definition);
    }

    private DefinitionBuilder bool(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.BOOLEAN);
    }

    private DefinitionBuilder colour(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.COLOUR);
    }

    private DefinitionBuilder decimal(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.DECIMAL);
    }

    private Map<String, Definition> definitions() {
        Map<String, Definition> result = new LinkedHashMap<>();
        // General application behaviour.
        add(result, integer(Keys.HOLIDAY_LOOK_AHEAD_MONTHS, GENERAL, "Holiday look-ahead months", "Number of months used when calculating holidays for user locations.")
                .defaultValue(24)
                .minimum(1)
                .maximum(120)
                .onUpdate(value -> KassandraProperties.setHolidayLookAheadMonths(Long.parseLong(value)))
                .build());
        add(result, integer(Keys.UNDO_REDO_HISTORY_LIMIT, GENERAL, "Planning history limit", "Maximum number of operations displayed in the planning history panel.")
                .defaultValue(5)
                .minimum(1)
                .maximum(100)
                .build());

        // Runtime diagnostic logging.
        add(result, logLevel(Keys.MCP_TOOL_CALLBACK_LOGGING_LEVEL,
                LOGGING, "MCP tool callback logging level", "Controls logging of serialized tool responses sent to the AI assistant.")
                .defaultValue("DEBUG")
                .options("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF")
                .onUpdate(value -> applyLoggingLevel(Keys.MCP_TOOL_CALLBACK_LOGGING_LEVEL, value))
                .build());

        // Per-request AI model options.
        add(result, text(Keys.AI_INSIGHTS_MODEL, AI_MODELS, "Insights model", "Model identifier used to generate sprint insights.")
                .defaultValue("qwen/qwen3.5-9b")
                .maximumLength(200)
                .build());
        add(result, text(Keys.AI_FILTER_MODEL, AI_MODELS, "Filter model", "Model identifier used to generate AI filters.")
                .defaultValue("qwen3.5-4b@q8_0")
                .maximumLength(200)
                .build());
        add(result, text(Keys.AI_MCP_MODEL, AI_MODELS, "Assistant model", "Model identifier used by the AI assistant.")
                .defaultValue("ministral-3-8b-reasoning-2512")
                .maximumLength(200)
                .build());
        add(result, decimal(Keys.AI_TEMPERATURE, AI_MODELS, "Temperature", "Sampling temperature used by the configured AI model.")
                .defaultValue(0)
                .minimum(0)
                .maximum(2)
                .build());
        add(result, integer(Keys.AI_MAX_TOKENS, AI_MODELS, "Maximum tokens", "Maximum number of tokens generated by AI requests.")
                .defaultValue(20480)
                .minimum(1)
                .maximum(131072)
                .build());
        add(result, integer(Keys.AI_SEED, AI_MODELS, "Random seed", "Optional seed for reproducible AI output.")
                .defaultValue(42)
                .minimum(0)
                .maximum(Integer.MAX_VALUE)
                .build());

        // OpenAI-compatible chat-completion connection. The client is created at application startup.
        add(result, url(Keys.OPENAI_BASE_URL, OPENAI, "Base URL", "Base URL of the OpenAI-compatible chat-completion API.")
                .defaultValue("http://localhost:1234")
                .maximumLength(2048)
                .restartRequired()
                .build());
        add(result, password(Keys.OPENAI_API_KEY, OPENAI, "API key", "Optional OpenAI-compatible API credential. It is encrypted and never displayed after saving.")
                .restartRequired()
                .build());

        // LM Studio native API connection and model-load configuration.
        add(result, url(Keys.LM_STUDIO_API_URL, LM_STUDIO, "API URL", "Base URL of the LM Studio native API.")
                .defaultValue("http://localhost:1234")
                .maximumLength(2048)
                .testWithGet("/api/v1/models")
                .build());
        add(result, password(Keys.LM_STUDIO_API_KEY, LM_STUDIO, "API key", "Optional LM Studio API credential. It is encrypted and never displayed after saving.")
                .build());
        add(result, integer(Keys.LM_STUDIO_CONTEXT_LENGTH, LM_STUDIO, "Context length", "Maximum context length passed when loading a model. Zero uses the model default.")
                .defaultValue(20480)
                .minimum(0)
                .maximum(131072)
                .build());
        add(result, bool(Keys.LM_STUDIO_FLASH_ATTENTION, LM_STUDIO, "Enable Flash Attention", "Enable Flash Attention when LM Studio loads a model.")
                .defaultValue(true)
                .build());
        add(result, bool(Keys.LM_STUDIO_OFFLOAD_KV_CACHE_TO_GPU, LM_STUDIO, "Offload KV cache to GPU",
                "Offload the LM Studio key-value cache to GPU memory.")
                .defaultValue(true)
                .build());
        add(result, integer(Keys.LM_STUDIO_TIMEOUT_SECONDS, LM_STUDIO, "Request timeout", "Maximum duration in seconds for LM Studio API requests.")
                .defaultValue(300)
                .minimum(1)
                .maximum(900)
                .build());

        // Stable Diffusion image-generation defaults and service connection.
        add(result, url(Keys.STABLE_DIFFUSION_API_URL, STABLE_DIFFUSION, "API URL", "Base URL of the Stable Diffusion WebUI API.")
                .defaultValue("http://localhost:7861")
                .maximumLength(2048)
                .testWithGet("/sdapi/v1/options")
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

    private void applyLoggingLevel(String key, String value) {
        String loggerName = key.substring("logging.level.".length());
        loggingSystem.setLogLevel(loggerName, LogLevel.valueOf(value.toUpperCase(Locale.ROOT)));
    }

    private ServerSettingTestResult httpGetTest(String baseUrl, String path) {
        try {
            HttpStatusCode status = WebClient.create(baseUrl).get().uri(path).exchangeToMono(response -> {
                HttpStatusCode responseStatus = response.statusCode();
                return response.releaseBody().thenReturn(responseStatus);
            }).block(TEST_TIMEOUT);
            return testResult(status != null && status.is2xxSuccessful(),
                    status == null ? "The server did not return a response." : "Server returned HTTP " + status.value() + ".");
        } catch (Exception e) {
            return testResult(false, "Connection failed: " + e.getMessage());
        }
    }

    private DefinitionBuilder integer(String key, Category category, String label, String description) {
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

    private ServerSettingTestResult testResult(boolean successful, String message) {
        ServerSettingTestResult result = new ServerSettingTestResult();
        result.setMessage(message);
        result.setSuccessful(successful);
        return result;
    }

    private void validate(Type type, String label, boolean secret, Double minimum, Double maximum, List<String> options,
                          String value) {
        if (value == null || value.isBlank() && !secret && type != Type.TEXT) {
            throw new IllegalArgumentException(label + " is required");
        }
        try {
            switch (type) {
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException(label + " must be true or false");
                    }
                }
                case COLOUR -> {
                    if (!value.matches("^#[0-9a-fA-F]{6}$")) {
                        throw new IllegalArgumentException(label + " must be a six-digit CSS hex colour");
                    }
                }
                case DECIMAL -> range(label, minimum, maximum, Double.parseDouble(value));
                case INTEGER -> range(label, minimum, maximum, Long.parseLong(value));
                case LOG_LEVEL -> {
                    if (!options.contains(value.toUpperCase(Locale.ROOT))) {
                        throw new IllegalArgumentException(label + " must be one of " + String.join(", ", options));
                    }
                }
                case PASSWORD, TEXT -> maximumLength(label, maximum, value);
                case URL -> {
                    maximumLength(label, maximum, value);
                    if (!value.matches("^https?://[^\\s]+$")) {
                        throw new IllegalArgumentException(label + " must be an HTTP or HTTPS URL");
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number", e);
        }
    }

    private void maximumLength(String label, Double maximum, String value) {
        if (maximum != null && value.length() > maximum) {
            throw new IllegalArgumentException(label + " is too long");
        }
    }

    private void range(String label, Double minimum, Double maximum, double value) {
        if (!Double.isFinite(value) || minimum != null && value < minimum || maximum != null && value > maximum) {
            throw new IllegalArgumentException(label + " is outside the allowed range");
        }
    }

    private DefinitionBuilder logLevel(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.LOG_LEVEL);
    }

    private DefinitionBuilder password(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.PASSWORD).secret();
    }

    private DefinitionBuilder text(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.TEXT);
    }

    private DefinitionBuilder url(String key, Category category, String label, String description) {
        return new DefinitionBuilder(key, category, label, description, Type.URL);
    }

    /**
     * Defines the editor type used by the dynamic settings view.
     */
    public enum Type {
        BOOLEAN, COLOUR, DECIMAL, INTEGER, LOG_LEVEL, PASSWORD, TEXT, URL
    }

    /**
     * Stable keys for administrator-managed server settings.
     */
    public static final class Keys {

        /**
         * Assistant model identifier.
         */
        public static final String AI_MCP_MODEL                                   = "kassandra.ai.mcp-model";
        /**
         * Filter model identifier.
         */
        public static final String AI_FILTER_MODEL                                = "kassandra.ai.filter-model";
        /**
         * Sprint insights model identifier.
         */
        public static final String AI_INSIGHTS_MODEL                              = "kassandra.ai.insights-model";
        /**
         * AI request token limit.
         */
        public static final String AI_MAX_TOKENS                                  = "kassandra.ai.max-tokens";
        /**
         * AI random seed.
         */
        public static final String AI_SEED                                        = "kassandra.ai.seed";
        /**
         * AI sampling temperature.
         */
        public static final String AI_TEMPERATURE                                 = "kassandra.ai.temperature";
        /**
         * Holiday calculation horizon.
         */
        public static final String HOLIDAY_LOOK_AHEAD_MONTHS                      = "kassandra.holidays.look-ahead-months";
        /**
         * LM Studio API credential.
         */
        public static final String LM_STUDIO_API_KEY                              = "kassandra.lm-studio.api-key";
        /**
         * LM Studio API URL.
         */
        public static final String LM_STUDIO_API_URL                              = "kassandra.lm-studio.api-url";
        /**
         * LM Studio context length.
         */
        public static final String LM_STUDIO_CONTEXT_LENGTH                       = "kassandra.lm-studio.context-length";
        /**
         * LM Studio Flash Attention setting.
         */
        public static final String LM_STUDIO_FLASH_ATTENTION                      = "kassandra.lm-studio.flash-attention";
        /**
         * LM Studio KV-cache offloading setting.
         */
        public static final String LM_STUDIO_OFFLOAD_KV_CACHE_TO_GPU              = "kassandra.lm-studio.offload-kv-cache-to-gpu";
        /**
         * LM Studio request timeout.
         */
        public static final String LM_STUDIO_TIMEOUT_SECONDS                      = "kassandra.lm-studio.timeout-seconds";
        /**
         * MCP callback logger level.
         */
        public static final String MCP_TOOL_CALLBACK_LOGGING_LEVEL                = "logging.level.de.bushnaq.abdalla.kassandra.ai.mcp.ContextPropagatingToolCallbackProvider";
        /**
         * OpenAI-compatible API credential.
         */
        public static final String OPENAI_API_KEY                                 = "kassandra.openai.api-key";
        /**
         * OpenAI-compatible API URL.
         */
        public static final String OPENAI_BASE_URL                                = "kassandra.openai.base-url";
        /**
         * Stable Diffusion API URL.
         */
        public static final String STABLE_DIFFUSION_API_URL                       = "stable-diffusion.api-url";
        /**
         * Stable Diffusion dark avatar background.
         */
        public static final String STABLE_DIFFUSION_AVATAR_DARK_BACKGROUND_COLOR  = "stable-diffusion.avatar-dark-background-color";
        /**
         * Stable Diffusion light avatar background.
         */
        public static final String STABLE_DIFFUSION_AVATAR_LIGHT_BACKGROUND_COLOR = "stable-diffusion.avatar-light-background-color";
        /**
         * Stable Diffusion avatar output size.
         */
        public static final String STABLE_DIFFUSION_AVATAR_OUTPUT_SIZE            = "stable-diffusion.avatar-output-size";
        /**
         * Stable Diffusion CFG scale.
         */
        public static final String STABLE_DIFFUSION_CFG_SCALE                     = "stable-diffusion.cfg-scale";
        /**
         * Stable Diffusion default denoising strength.
         */
        public static final String STABLE_DIFFUSION_DEFAULT_DENOISING_STRENGTH    = "stable-diffusion.default-denoising-strength";
        /**
         * Stable Diffusion default sampler.
         */
        public static final String STABLE_DIFFUSION_DEFAULT_SAMPLER               = "stable-diffusion.default-sampler";
        /**
         * Stable Diffusion default step count.
         */
        public static final String STABLE_DIFFUSION_DEFAULT_STEPS                 = "stable-diffusion.default-steps";
        /**
         * Stable Diffusion image generation size.
         */
        public static final String STABLE_DIFFUSION_GENERATION_SIZE               = "stable-diffusion.generation-size";
        /**
         * Stable Diffusion model loading timeout.
         */
        public static final String STABLE_DIFFUSION_MODEL_LOAD_TIMEOUT_SECONDS    = "stable-diffusion.model-load-timeout-seconds";
        /**
         * Stable Diffusion model name.
         */
        public static final String STABLE_DIFFUSION_MODEL_NAME                    = "stable-diffusion.model-name";
        /**
         * Stable Diffusion generic output size.
         */
        public static final String STABLE_DIFFUSION_OUTPUT_SIZE                   = "stable-diffusion.output-size";
        /**
         * Stable Diffusion request timeout.
         */
        public static final String STABLE_DIFFUSION_TIMEOUT_SECONDS               = "stable-diffusion.timeout-seconds";
        /**
         * Planning history length.
         */
        public static final String UNDO_REDO_HISTORY_LIMIT                        = "kassandra.undo-redo.history-limit";

        private Keys() {
        }
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
                             String defaultValue, boolean secret, boolean restartRequired, Double minimum,
                             Double maximum,
                             List<String> options, Consumer<String> validator,
                             Function<String, ServerSettingTestResult> tester,
                             Consumer<String> updateHandler) {

        /**
         * Applies this definition's value validation.
         *
         * @param value proposed setting value
         */
        public void validate(String value) {
            validator.accept(value);
        }

        /**
         * Runs this definition's connectivity test.
         *
         * @param value validated value to test
         * @return result reported by the setting-specific test
         * @throws IllegalArgumentException when this setting does not support testing
         */
        public ServerSettingTestResult test(String value) {
            if (tester == null) {
                throw new IllegalArgumentException("This setting does not support a connection test");
            }
            return tester.apply(value);
        }

        /**
         * Applies this definition's optional runtime update action.
         *
         * @param value persisted setting value
         */
        public void update(String value) {
            if (updateHandler != null) {
                updateHandler.accept(value);
            }
        }

        /**
         * Indicates whether this setting has a connection test.
         *
         * @return {@code true} when a connection test is available
         */
        public boolean testable() {
            return tester != null;
        }
    }

    /**
     * Builds a readable declarative setting definition.
     */
    private final class DefinitionBuilder {

        private final Category                                  category;
        private       String                                    defaultValue;
        private final String                                    description;
        private final String                                    key;
        private final String                                    label;
        private       Double                                    maximum;
        private       Double                                    minimum;
        private       List<String>                              options = List.of();
        private       boolean                                   restartRequired;
        private       boolean                                   secret;
        private       Function<String, ServerSettingTestResult> tester;
        private final Type                                      type;
        private       Consumer<String>                          updateHandler;

        private DefinitionBuilder(String key, Category category, String label, String description, Type type) {
            this.category    = category;
            this.description = description;
            this.key         = key;
            this.label       = label;
            this.type        = type;
        }

        private Definition build() {
            return new Definition(key, category, label, description, type, defaultValue, secret, restartRequired,
                    minimum, maximum, options, validator(), tester, updateHandler);
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

        private DefinitionBuilder onUpdate(Consumer<String> updateHandler) {
            this.updateHandler = updateHandler;
            return this;
        }

        private DefinitionBuilder testWithGet(String path) {
            tester = value -> httpGetTest(value, path);
            return this;
        }

        private Consumer<String> validator() {
            return value -> validate(type, label, secret, minimum, maximum, options, value);
        }
    }
}
