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
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SprintApi extends AbstractApi {

    @Autowired
    public SprintApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    public SprintApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    public SprintApi() {

    }

    public void deleteById(UUID id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/sprint/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    public List<Sprint> getAll() {
        ResponseEntity<Sprint[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/sprint",
                HttpMethod.GET,
                createHttpEntity(),
                Sprint[].class
        ));
        return Arrays.asList(response.getBody());
    }

    public List<Sprint> getAll(UUID featureId) {
        ResponseEntity<Sprint[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/sprint/feature/{featureId}",
                HttpMethod.GET,
                createHttpEntity(),
                Sprint[].class,
                featureId
        ));
        return Arrays.asList(response.getBody());
    }

    /**
     * Get all avatar info (resized, original, and prompt) for a sprint.
     *
     * @param sprintId The sprint ID
     * @return AvatarUpdateRequest containing avatarImage, avatarImageOriginal, and avatarPrompt, or null if not found
     */
    public AvatarUpdateRequest getAvatarFull(UUID sprintId) {
        try {
            ResponseEntity<AvatarUpdateRequest> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/sprint/{id}/avatar/full",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarUpdateRequest.class,
                    sprintId
            ));
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching full avatar for sprint ID {}: {}", sprintId, e.getMessage());
            return null;
        }
    }

    /**
     * Get avatar image bytes for a sprint.
     *
     * @param sprintId The sprint ID
     * @return The avatar image as byte array, or null if not found
     */
    public AvatarWrapper getAvatarImage(UUID sprintId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/sprint/{id}/avatar",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    sprintId
            ));
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching avatar image for sprint ID {}: {}", sprintId, e.getMessage());
            return null;
        }
    }

    /**
     * Get the global Backlog sprint.
     *
     * @return The Backlog sprint
     */
    public Sprint getBacklogSprint() {
        ResponseEntity<Sprint> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/sprint/backlog",
                HttpMethod.GET,
                createHttpEntity(),
                Sprint.class
        ));
        return response.getBody();
    }

    public Sprint getById(UUID id) {
        ResponseEntity<Sprint> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/sprint/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                Sprint.class,
                id
        ));
        return response.getBody();
    }

    public Optional<Sprint> getByName(UUID featureId, String name) {
        try {
            ResponseEntity<Sprint> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/sprint/feature/{featureId}/by-name/{name}",
                    HttpMethod.GET,
                    createHttpEntity(),
                    Sprint.class,
                    featureId,
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
     * Get the dark-mode avatar image bytes for a sprint.
     * The server falls back to the light image automatically if no dark variant has been stored yet.
     *
     * @param sprintId The sprint ID
     * @return The dark avatar image (or light fallback), or null if not found
     */
    public AvatarWrapper getDarkAvatarImage(UUID sprintId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/sprint/{id}/dark-avatar",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    sprintId
            ));
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching dark avatar image for sprint ID {}: {}", sprintId, e.getMessage());
            return null;
        }
    }

    /**
     * Get the dark-mode header image bytes for a sprint.
     *
     * @param sprintId The sprint ID
     * @return The dark header image, or its light fallback, or null if not found
     */
    public AvatarWrapper getDarkHeaderImage(UUID sprintId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/sprint/{id}/dark-header",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    sprintId
            ));
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching dark header image for sprint ID {}: {}", sprintId, e.getMessage());
            return null;
        }
    }

    /**
     * Get the light-mode header image bytes for a sprint.
     *
     * @param sprintId The sprint ID
     * @return The header image, or null if not found
     */
    public AvatarWrapper getHeaderImage(UUID sprintId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/sprint/{id}/header",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    sprintId
            ));
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching header image for sprint ID {}: {}", sprintId, e.getMessage());
            return null;
        }
    }

    public Sprint persist(Sprint sprint) {
        ResponseEntity<Sprint> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/sprint",
                HttpMethod.POST,
                createHttpEntity(sprint),
                Sprint.class
        ));
        return response.getBody();
    }

    public void update(Sprint sprint) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/sprint",
                HttpMethod.PUT,
                createHttpEntity(sprint),
                Void.class
        ));
    }

    /**
     * Updates image variants and theme-independent generation metadata.
     * Null fields leave stored values unchanged.
     *
     * @param sprintId the entity ID
     * @param request  images and editable generation inputs
     */
    public void updateAvatarFull(UUID sprintId, AvatarUpdateRequest request) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/sprint/{id}/avatar/full",
                HttpMethod.PUT,
                createHttpEntity(request),
                Void.class,
                sprintId
        ));
    }
}