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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bushnaq.abdalla.kassandra.dto.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class UserApi extends AbstractApi {

    public UserApi(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl) {
        super(restTemplate, objectMapper, baseUrl);
    }

    @Autowired
    public UserApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
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

    public User getByEmail(String email) {
        ResponseEntity<User> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user/email/{email}",
                HttpMethod.GET,
                createHttpEntity(),
                User.class,
                email
        ));
        return response.getBody();
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
}
