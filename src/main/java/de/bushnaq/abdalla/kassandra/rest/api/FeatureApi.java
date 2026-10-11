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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FeatureApi extends AbstractApi {

    public FeatureApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    @Autowired
    public FeatureApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    public FeatureApi() {

    }

    public void deleteById(UUID id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/feature/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    public List<Feature> getAll() {
        ResponseEntity<Feature[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/feature",
                HttpMethod.GET,
                createHttpEntity(),
                Feature[].class
        ));
        return Arrays.asList(response.getBody());
    }

    public List<Feature> getAll(UUID versionId) {
        ResponseEntity<Feature[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/feature/version/{versionId}",
                HttpMethod.GET,
                createHttpEntity(),
                Feature[].class,
                versionId
        ));
        return Arrays.asList(response.getBody());
    }

    /**
     * Get all avatar info (resized, original, and prompt) for a feature.
     *
     * @param featureId The feature ID
     * @return AvatarUpdateRequest containing avatarImage, avatarImageOriginal, and avatarPrompt, or null if not found
     */
    public AvatarUpdateRequest getAvatarFull(UUID featureId) {
        try {
            ResponseEntity<AvatarUpdateRequest> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/feature/{id}/avatar/full",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarUpdateRequest.class,
                    featureId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get avatar image bytes for a feature.
     *
     * @param featureId The feature ID
     * @return The avatar image as byte array, or null if not found
     */
    public AvatarWrapper getAvatarImage(UUID featureId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/feature/{id}/avatar",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    featureId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Feature getById(UUID id) {
        ResponseEntity<Feature> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/feature/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                Feature.class,
                id
        ));
        return response.getBody();
    }

    public Optional<Feature> getByName(UUID versionId, String name) {
        try {
            ResponseEntity<Feature> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/feature/version/{versionId}/by-name/{name}",
                    HttpMethod.GET,
                    createHttpEntity(),
                    Feature.class,
                    versionId,
                    name
            ));
            return Optional.of(response.getBody());
        } catch (ErrorResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Get the dark-mode avatar image bytes for a feature.
     * The server falls back to the light image automatically if no dark variant has been stored yet.
     *
     * @param featureId The feature ID
     * @return The dark avatar image (or light fallback), or null if not found
     */
    public AvatarWrapper getDarkAvatarImage(UUID featureId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/feature/{id}/dark-avatar",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    featureId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the dark-mode header image bytes for a feature.
     *
     * @param featureId The feature ID
     * @return The dark header image, or its light fallback, or null if not found
     */
    public AvatarWrapper getDarkHeaderImage(UUID featureId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/feature/{id}/dark-header",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    featureId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the light-mode header image bytes for a feature.
     *
     * @param featureId The feature ID
     * @return The header image, or null if not found
     */
    public AvatarWrapper getHeaderImage(UUID featureId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/feature/{id}/header",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    featureId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Feature persist(Feature feature) {
        ResponseEntity<Feature> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/feature",
                HttpMethod.POST,
                createHttpEntity(feature),
                Feature.class
        ));
        return response.getBody();
    }

    public void update(Feature feature) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/feature",
                HttpMethod.PUT,
                createHttpEntity(feature),
                Void.class
        ));
    }

    /**
     * Updates image variants and theme-independent generation metadata.
     * Null fields leave stored values unchanged.
     *
     * @param featureId the entity ID
     * @param request   images and editable generation inputs
     */
    public void updateAvatarFull(UUID featureId, AvatarUpdateRequest request) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/feature/{id}/avatar/full",
                HttpMethod.PUT,
                createHttpEntity(request),
                Void.class,
                featureId
        ));
    }
}