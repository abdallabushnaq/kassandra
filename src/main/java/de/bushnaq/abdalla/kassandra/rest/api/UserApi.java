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
import de.bushnaq.abdalla.kassandra.dto.User;
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

@Service
public class UserApi extends AbstractApi {

    public UserApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    @Autowired
    public UserApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    public UserApi() {

    }

    public void deleteById(Long id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    public List<User> getAll() {
        ResponseEntity<User[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user",
                HttpMethod.GET,
                createHttpEntity(),
                User[].class
        ));
        return Arrays.asList(response.getBody());
    }

    /**
     * Get all users assigned to any task that belongs to this sprint.
     *
     * @param sprintId id of the sprint
     * @return list of users
     */
    public List<User> getAll(Long sprintId) {
        ResponseEntity<User[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/sprint/{sprintId}",
                HttpMethod.GET,
                createHttpEntity(),
                User[].class,
                sprintId
        ));
        return Arrays.asList(response.getBody());
    }

    /**
     * Get all avatar info (resized, original, and prompt) for a user.
     *
     * @param userId The user ID
     * @return AvatarUpdateRequest containing avatarImage, avatarImageOriginal, and avatarPrompt, or null if not found
     */
    public AvatarUpdateRequest getAvatarFull(Long userId) {
        try {
            ResponseEntity<AvatarUpdateRequest> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/user/{id}/avatar/full",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarUpdateRequest.class,
                    userId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get avatar image bytes for a user.
     *
     * @param userId The user ID
     * @return The avatar image as byte array, or null if not found
     */
    public AvatarWrapper getAvatarImage(Long userId) {
        // Log message converters
//        restTemplate.getMessageConverters().forEach(c -> System.out.println("Converter: " + c.getClass()));
        ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/{id}/avatar",
                HttpMethod.GET,
                createHttpEntity(),
                AvatarWrapper.class,
                userId
        ));
        return response.getBody();
    }

    public Optional<User> getByEmail(String email) {
        try {
            ResponseEntity<User> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/user/email/{email}",
                    HttpMethod.GET,
                    createHttpEntity(),
                    User.class,
                    email
            ));
            return Optional.of(response.getBody());
        } catch (ErrorResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public User getById(Long id) {
        ResponseEntity<User> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                User.class,
                id
        ));
        return response.getBody();
    }

    public User getByName(String name) {
        ResponseEntity<User> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/name/{name}",
                HttpMethod.GET,
                createHttpEntity(),
                User.class,
                name
        ));
        return response.getBody();
    }

    public User persist(User user) {
        ResponseEntity<User> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user",
                HttpMethod.POST,
                createHttpEntity(user),
                User.class
        ));
        return response.getBody();
    }

    /**
     * Search for users whose names contain the specified string (case-insensitive).
     *
     * @param partialName The partial name to search for in user names
     * @return A list of users whose names contain the specified string (case-insensitive)
     */
    public List<User> searchByName(String partialName) {
        ResponseEntity<User[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/search/{partialName}",
                HttpMethod.GET,
                createHttpEntity(),
                User[].class,
                partialName
        ));
        return Arrays.asList(response.getBody());

    }

    public void update(User user) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user",
                HttpMethod.PUT,
                createHttpEntity(user),
                Void.class
        ));
    }

    /**
     * Update user avatar with all fields (resized, original, and prompt).
     *
     * @param userId        The user ID
     * @param resizedImage  The resized avatar image bytes (e.g., 64x64)
     * @param originalImage The original avatar image bytes (e.g., 512x512)
     * @param prompt        The prompt used to generate the avatar
     */
    public void updateAvatarFull(Long userId, byte[] resizedImage, byte[] originalImage, String prompt) {
        AvatarUpdateRequest request = new AvatarUpdateRequest();

        if (resizedImage != null) {
            request.setAvatarImage(resizedImage);
        }

        if (originalImage != null) {
            request.setAvatarImageOriginal(originalImage);
        }

        request.setAvatarPrompt(prompt);

        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/{id}/avatar/full",
                HttpMethod.PUT,
                createHttpEntity(request),
                Void.class,
                userId
        ));
    }
}
