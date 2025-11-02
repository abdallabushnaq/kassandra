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
import de.bushnaq.abdalla.kassandra.dto.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class VersionApi extends AbstractApi {

    public VersionApi(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl) {
        super(restTemplate, objectMapper, baseUrl);
    }

    @Autowired
    public VersionApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    public VersionApi() {

    }

    public void deleteById(Long id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/version/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    public List<Version> getAll() {
        ResponseEntity<Version[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/version",
                HttpMethod.GET,
                createHttpEntity(),
                Version[].class
        ));
        return Arrays.asList(response.getBody());
    }

    public List<Version> getAll(Long productId) {
        ResponseEntity<Version[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/version/product/{productId}",
                HttpMethod.GET,
                createHttpEntity(),
                Version[].class,
                productId
        ));
        return Arrays.asList(response.getBody());
    }

    public Version getById(Long id) {
        ResponseEntity<Version> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/version/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                Version.class,
                id
        ));
        return response.getBody();
    }

    public Version persist(Version version) {
        ResponseEntity<Version> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/version",
                HttpMethod.POST,
                createHttpEntity(version),
                Version.class
        ));
        return response.getBody();
    }

    public void update(Version version) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/version",
                HttpMethod.PUT,
                createHttpEntity(version),
                Void.class
        ));
    }
}