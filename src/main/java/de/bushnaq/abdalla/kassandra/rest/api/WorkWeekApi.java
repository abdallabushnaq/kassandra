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

import de.bushnaq.abdalla.kassandra.dto.WorkWeek;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;

/**
 * REST client stub for the {@code /api/work-week} endpoint.
 */
@Service
public class WorkWeekApi extends AbstractApi {

    /**
     * Constructor used by tests with explicit base URL.
     */
    public WorkWeekApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    /**
     * No-arg constructor required for Spring proxying.
     */
    public WorkWeekApi() {
    }

    /**
     * Primary Spring-managed constructor.
     */
    @Autowired
    public WorkWeekApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    /**
     * Delete a work week by ID.
     *
     * @param id the work week ID
     */
    public void deleteById(Long id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/work-week/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    /**
     * Retrieve all work weeks.
     *
     * @return list of all work weeks
     */
    public List<WorkWeek> getAll() {
        ResponseEntity<WorkWeek[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/work-week",
                HttpMethod.GET,
                createHttpEntity(),
                WorkWeek[].class
        ));
        return Arrays.asList(response.getBody());
    }

    /**
     * Retrieve a work week by ID.
     *
     * @param id the work week ID
     * @return the work week
     */
    public WorkWeek getById(Long id) {
        ResponseEntity<WorkWeek> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/work-week/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                WorkWeek.class,
                id
        ));
        return response.getBody();
    }

    /**
     * Create a new work week.
     *
     * @param workWeek the work week to create
     * @return the created work week
     */
    public WorkWeek persist(WorkWeek workWeek) {
        ResponseEntity<WorkWeek> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/work-week",
                HttpMethod.POST,
                createHttpEntity(workWeek),
                WorkWeek.class
        ));
        return response.getBody();
    }

    /**
     * Update an existing work week.
     *
     * @param workWeek the work week with updated fields (must have a non-null ID)
     */
    public void update(WorkWeek workWeek) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/work-week/{id}",
                HttpMethod.PUT,
                createHttpEntity(workWeek),
                Void.class,
                workWeek.getId()
        ));
    }
}

