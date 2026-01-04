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

import de.bushnaq.abdalla.kassandra.dto.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Service
public class LocationApi extends AbstractApi {

    public LocationApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    public LocationApi() {

    }

    @Autowired
    public LocationApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    //TODO use ids instead of objects
    public void deleteById(Long userId, Long locationId) throws org.springframework.web.client.RestClientException {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/location/{userId}/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                userId,
                locationId
        ));
    }

    public Location getById(Long id) {
        ResponseEntity<Location> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/location/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                Location.class,
                id
        ));
        return response.getBody();
    }

    public Location persist(Location location, Long userId) {
        ResponseEntity<Location> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/location/{userId}",
                HttpMethod.POST,
                createHttpEntity(location),
                Location.class,
                userId
        ));
        return response.getBody();
    }

    public void update(Location location, Long userId) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/location/{userId}",
                HttpMethod.PUT,
                createHttpEntity(location),
                Void.class,
                userId
        ));
    }
}
