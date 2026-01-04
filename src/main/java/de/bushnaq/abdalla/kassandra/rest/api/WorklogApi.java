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

import de.bushnaq.abdalla.kassandra.dto.Worklog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;

@Service
public class WorklogApi extends AbstractApi {

    public WorklogApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    @Autowired
    public WorklogApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    public WorklogApi() {

    }

    public void deleteById(Long id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/worklog/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    public List<Worklog> getAll() {
        ResponseEntity<Worklog[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/worklog",
                HttpMethod.GET,
                createHttpEntity(),
                Worklog[].class
        ));
        return Arrays.asList(response.getBody());
    }

    public List<Worklog> getAll(Long sprintId) {
        ResponseEntity<Worklog[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/worklog/sprint/{sprintId}",
                HttpMethod.GET,
                createHttpEntity(),
                Worklog[].class,
                sprintId
        ));
        return Arrays.asList(response.getBody());
    }

    public Worklog getById(Long id) {
        ResponseEntity<Worklog> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/worklog/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                Worklog.class,
                id
        ));
        return response.getBody();
    }

    public Worklog persist(Worklog worklog) {
        ResponseEntity<Worklog> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/worklog",
                HttpMethod.POST,
                createHttpEntity(worklog),
                Worklog.class
        ));
        return response.getBody();
    }

    public void update(Worklog worklog) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/worklog",
                HttpMethod.PUT,
                createHttpEntity(worklog),
                Void.class
        ));
    }
}