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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing LM Studio model lifecycle via the LM Studio native REST API
 * ({@code /api/v1/models}, {@code /api/v1/models/load}, {@code /api/v1/models/unload}).
 *
 * <p>The primary entry-point for tests is {@link #ensureModelLoaded(String)}, which:
 * <ol>
 *   <li>Lists all models known to LM Studio.</li>
 *   <li>Unloads every currently-loaded model that is <em>not</em> the required one
 *       (freeing GPU/VRAM before the target model is loaded).</li>
 *   <li>Loads the required model if it is not already loaded.</li>
 * </ol>
 *
 * <p>If LM Studio is not reachable the method logs a warning and returns {@code false}
 * so that callers can decide how to proceed (skip the test, throw, etc.).
 */
@Service
@Slf4j
public class LmStudioService {

    private static final String API_BASE = "/api/v1";
    private static final String LOAD     = API_BASE + "/models/load";
    private static final String MODELS   = API_BASE + "/models";
    private static final String UNLOAD   = API_BASE + "/models/unload";

    private final LmStudioConfig config;
    private final WebClient      webClient;

    public LmStudioService(LmStudioConfig config) {
        this.config = config;

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(config.getApiUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey());
        }

        this.webClient = builder.build();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Ensures that {@code requiredModelId} is the <em>only</em> loaded LLM model in LM Studio.
     *
     * <ol>
     *   <li>Fetches the current model list from LM Studio.</li>
     *   <li>Unloads all loaded LLM instances that do not match {@code requiredModelId}.</li>
     *   <li>Loads {@code requiredModelId} if it is not already loaded.</li>
     * </ol>
     *
     * @param requiredModelId the model key exactly as it appears in LM Studio
     *                        (e.g. {@code "mistralai/ministral-3-3b"})
     * @return {@code true} if the model is loaded and ready, {@code false} if LM Studio
     * is not reachable or the model is not found in the catalogue
     */
    public boolean ensureModelLoaded(String requiredModelId) {
        log.info("Ensuring LM Studio has model '{}' loaded (and only that model)", requiredModelId);

        List<LmStudioModel> models = listModels();
        if (models == null) {
            log.warn("LM Studio is not reachable at {} – skipping model management", config.getApiUrl());
            return false;
        }

        // Verify the requested model exists in the catalogue
        Optional<LmStudioModel> target = models.stream()
                .filter(m -> requiredModelId.equals(m.key()))
                .findFirst();

        if (target.isEmpty()) {
            log.error("Model '{}' is not available in LM Studio (not downloaded). Available models: {}",
                    requiredModelId, models.stream().map(LmStudioModel::key).toList());
            return false;
        }

        // Unload all other loaded LLM instances first to free VRAM
        for (LmStudioModel model : models) {
            if (!"llm".equals(model.type()) && !"vlm".equals(model.type())) {
                continue; // leave embedding models alone
            }
            if (!model.isLoaded()) {
                continue;
            }
            if (requiredModelId.equals(model.key())) {
                log.info("Model '{}' is already loaded – no action needed", requiredModelId);
                continue;
            }
            // Unload each loaded instance of the wrong model
            for (LmStudioLoadedInstance instance : model.loadedInstances()) {
                unloadModel(instance.id());
            }
        }

        // Load the target model if it is not already loaded
        if (!target.get().isLoaded()) {
            return loadModel(requiredModelId);
        }

        log.info("Model '{}' is ready", requiredModelId);
        return true;
    }

    /**
     * Returns {@code true} if the given model is currently loaded in LM Studio.
     * Returns {@code false} if LM Studio is not reachable.
     *
     * @param modelId the model key to check
     */
    public boolean isModelLoaded(String modelId) {
        List<LmStudioModel> models = listModels();
        if (models == null) {
            return false;
        }
        return models.stream()
                .filter(m -> modelId.equals(m.key()))
                .anyMatch(LmStudioModel::isLoaded);
    }

    /**
     * Returns the list of all models known to LM Studio, or {@code null} if the
     * server is not reachable.
     */
    public List<LmStudioModel> listModels() {
        try {
            LmStudioModelsResponse response = webClient.get()
                    .uri(MODELS)
                    .retrieve()
                    .bodyToMono(LmStudioModelsResponse.class)
                    .block(Duration.ofSeconds(config.getTimeoutSeconds()));

            if (response == null || response.models() == null) {
                log.warn("LM Studio returned an empty model list");
                return List.of();
            }

            log.debug("LM Studio model catalogue ({} model(s)):", response.models().size());
            for (LmStudioModel m : response.models()) {
                log.debug("  {} [{}] loaded={}", m.key(), m.type(), m.isLoaded());
            }
            return response.models();

        } catch (WebClientResponseException e) {
            log.error("LM Studio returned HTTP {} when listing models: {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Could not reach LM Studio at {}: {}", config.getApiUrl(), e.getMessage());
            return null;
        }
    }

    /**
     * Loads a model by its key. Returns {@code true} on success.
     *
     * @param modelId the model key (e.g. {@code "mistralai/ministral-3-3b"})
     */
    public boolean loadModel(String modelId) {
        log.info("Loading model '{}' in LM Studio…", modelId);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelId);
        if (config.getContextLength() > 0) {
            body.put("context_length", config.getContextLength());
        }
        body.put("flash_attention", config.isFlashAttention());
        body.put("offload_kv_cache_to_gpu", config.isOffloadKvCacheToGpu());

        try {
            LmStudioLoadResponse response = webClient.post()
                    .uri(LOAD)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(LmStudioLoadResponse.class)
                    .block(Duration.ofSeconds(config.getTimeoutSeconds()));

            if (response != null && "loaded".equals(response.status())) {
                log.info("Model '{}' loaded successfully in {}s", modelId, response.loadTimeSeconds());
                return true;
            }

            log.error("Unexpected load response for '{}': {}", modelId, response);
            return false;

        } catch (WebClientResponseException e) {
            log.error("LM Studio returned HTTP {} when loading '{}': {}", e.getStatusCode(), modelId, e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Failed to load model '{}': {}", modelId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Unloads a model instance by its instance ID. Returns {@code true} on success.
     *
     * @param instanceId the instance ID returned by LM Studio (usually equal to the model key)
     */
    public boolean unloadModel(String instanceId) {
        log.info("Unloading model instance '{}' from LM Studio…", instanceId);

        Map<String, Object> body = new HashMap<>();
        body.put("instance_id", instanceId);

        try {
            webClient.post()
                    .uri(UNLOAD)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(config.getTimeoutSeconds()));

            log.info("Model instance '{}' unloaded", instanceId);
            return true;

        } catch (WebClientResponseException e) {
            log.error("LM Studio returned HTTP {} when unloading '{}': {}", e.getStatusCode(), instanceId, e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Failed to unload model instance '{}': {}", instanceId, e.getMessage(), e);
            return false;
        }
    }
}





