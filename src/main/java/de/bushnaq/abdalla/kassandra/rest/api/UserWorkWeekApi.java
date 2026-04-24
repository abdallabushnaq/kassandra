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

package de.bushnaq.abdalla.kassandra.rest.api;

import java.util.UUID;
import de.bushnaq.abdalla.kassandra.dto.UserWorkWeek;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * REST client stub for the {@code /api/user-work-week} endpoint.
 */
@Service
public class UserWorkWeekApi extends AbstractApi {

    /**
     * Constructor used by tests with explicit base URL.
     */
    public UserWorkWeekApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    /**
     * No-arg constructor required for Spring proxying.
     */
    public UserWorkWeekApi() {
    }

    /**
     * Primary Spring-managed constructor.
     */
    @Autowired
    public UserWorkWeekApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    /**
     * Delete a user work-week assignment.
     *
     * @param userId         the owning user ID
     * @param userWorkWeekId the assignment ID
     */
    public void deleteById(UUID userId, UUID userWorkWeekId) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-work-week/{userId}/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                userId,
                userWorkWeekId
        ));
    }

    /**
     * Retrieve a user work-week assignment by ID.
     *
     * @param id the assignment ID
     * @return the assignment
     */
    public UserWorkWeek getById(UUID id) {
        ResponseEntity<UserWorkWeek> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-work-week/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                UserWorkWeek.class,
                id
        ));
        return response.getBody();
    }

    /**
     * Create a new user work-week assignment.
     *
     * @param userWorkWeek the assignment to create
     * @param userId       the owning user ID
     * @return the saved assignment
     */
    public UserWorkWeek persist(UserWorkWeek userWorkWeek, UUID userId) {
        ResponseEntity<UserWorkWeek> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-work-week/{userId}",
                HttpMethod.POST,
                createHttpEntity(userWorkWeek),
                UserWorkWeek.class,
                userId
        ));
        return response.getBody();
    }

    /**
     * Update an existing user work-week assignment.
     *
     * @param userWorkWeek the assignment with updated fields
     * @param userId       the owning user ID
     */
    public void update(UserWorkWeek userWorkWeek, UUID userId) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-work-week/{userId}",
                HttpMethod.PUT,
                createHttpEntity(userWorkWeek),
                Void.class,
                userId
        ));
    }
}

