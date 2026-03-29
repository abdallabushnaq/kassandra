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
     * @return The light avatar image, or null if not found
     */
    public AvatarWrapper getAvatarImage(Long userId) {
        ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/{id}/avatar",
                HttpMethod.GET,
                createHttpEntity(),
                AvatarWrapper.class,
                userId
        ));
        return response.getBody();
    }

    /**
     * Get the dark-mode avatar image bytes for a user.
     * The server falls back to the light image automatically if no dark variant has been stored yet.
     *
     * @param userId The user ID
     * @return The dark avatar image (or light fallback), or null if not found
     */
    public AvatarWrapper getDarkAvatarImage(Long userId) {
        ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/{id}/dark-avatar",
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
     * Update user avatar with light-theme fields only (resized, original, and prompt).
     * Delegates to the full overload with null dark and negative-prompt fields.
     *
     * @param userId        The user ID
     * @param resizedImage  The resized light avatar image bytes (e.g., 64x64)
     * @param originalImage The original light avatar image bytes (e.g., 512x512)
     * @param prompt        The prompt used to generate the light avatar
     */
    public void updateAvatarFull(Long userId, byte[] resizedImage, byte[] originalImage, String prompt) {
        updateAvatarFull(userId, resizedImage, originalImage, prompt, null, null, null, null, null);
    }

    /**
     * Update user avatar with both light and dark theme fields in a single request.
     * Delegates to the full overload with null negative-prompt fields.
     *
     * @param userId             The user ID
     * @param resizedImage       Resized light avatar image bytes (e.g., 64x64), or null to skip
     * @param originalImage      Original light avatar image bytes (e.g., 512x512), or null to skip
     * @param prompt             Prompt used to generate the light avatar, or null to skip
     * @param darkResizedImage   Resized dark avatar image bytes (e.g., 64x64), or null to skip
     * @param darkOriginalImage  Original dark avatar image bytes (e.g., 512x512), or null to skip
     */
    public void updateAvatarFull(Long userId, byte[] resizedImage, byte[] originalImage, String prompt,
            byte[] darkResizedImage, byte[] darkOriginalImage) {
        updateAvatarFull(userId, resizedImage, originalImage, prompt, darkResizedImage, darkOriginalImage, null, null, null);
    }

    /**
     * Update user avatar with all light, dark, and prompt fields in a single request.
     * All fields are optional — pass {@code null} to leave the stored value unchanged.
     *
     * @param userId                  The user ID
     * @param resizedImage            Resized light avatar image bytes (e.g., 64x64), or null to skip
     * @param originalImage           Original light avatar image bytes (e.g., 512x512), or null to skip
     * @param prompt                  Prompt used to generate the light avatar, or null to skip
     * @param darkResizedImage        Resized dark avatar image bytes (e.g., 64x64), or null to skip
     * @param darkOriginalImage       Original dark avatar image bytes (e.g., 512x512), or null to skip
     * @param darkPrompt              Prompt used to generate the dark avatar (includes dark suffix), or null to skip
     * @param negativePrompt          Negative prompt for the light avatar, or null to skip
     * @param darkNegativePrompt      Negative prompt for the dark avatar, or null to skip
     */
    public void updateAvatarFull(Long userId, byte[] resizedImage, byte[] originalImage, String prompt,
            byte[] darkResizedImage, byte[] darkOriginalImage,
            String darkPrompt, String negativePrompt, String darkNegativePrompt) {
        AvatarUpdateRequest request = new AvatarUpdateRequest();
        request.setAvatarImage(resizedImage);
        request.setAvatarImageOriginal(originalImage);
        request.setAvatarPrompt(prompt);
        request.setDarkAvatarImage(darkResizedImage);
        request.setDarkAvatarImageOriginal(darkOriginalImage);
        request.setDarkAvatarPrompt(darkPrompt);
        request.setAvatarNegativePrompt(negativePrompt);
        request.setDarkAvatarNegativePrompt(darkNegativePrompt);

        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/{id}/avatar/full",
                HttpMethod.PUT,
                createHttpEntity(request),
                Void.class,
                userId
        ));
    }
}
