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
import de.bushnaq.abdalla.kassandra.dto.Availability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AvailabilityApi extends AbstractApi {

    public AvailabilityApi(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl) {
        super(restTemplate, objectMapper, baseUrl);
    }

    public AvailabilityApi() {
    }

    @Autowired
    public AvailabilityApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    //TODO use ids instead of objects
    public void deleteById(Long userId, Long availabilityId) throws org.springframework.web.client.RestClientException {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/availability/{userId}/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                userId,
                availabilityId
        ));
    }

    public Availability getById(Long id) {
        ResponseEntity<Availability> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/availability/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                Availability.class,
                id
        ));
        return response.getBody();
    }

    public Availability persist(Availability availability, Long userId) {
        ResponseEntity<Availability> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/availability/{userId}",
                HttpMethod.POST,
                createHttpEntity(availability),
                Availability.class,
                userId
        ));
        return response.getBody();
    }

    public void update(Availability availability, Long userId) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/availability/{userId}",
                HttpMethod.PUT,
                createHttpEntity(availability),
                Void.class,
                userId
        ));
    }
}