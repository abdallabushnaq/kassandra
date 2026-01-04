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

import de.bushnaq.abdalla.kassandra.dto.OffDay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OffDayApi extends AbstractApi {

    public OffDayApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    public OffDayApi() {

    }

    @Autowired
    public OffDayApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    //TODO use ids instead of objects
    public void deleteById(Long userId, Long offDayId) throws org.springframework.web.client.RestClientException {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/offday/{userId}/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                userId,
                offDayId
        ));
    }

    public OffDay getById(Long id) {
        ResponseEntity<OffDay> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/offday/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                OffDay.class,
                id
        ));
        return response.getBody();
    }

    public OffDay persist(OffDay offDay, Long userId) {
        ResponseEntity<OffDay> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/offday/{userId}",
                HttpMethod.POST,
                createHttpEntity(offDay),
                OffDay.class,
                userId
        ));
        return response.getBody();
    }

    public void update(OffDay offDay, Long userId) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/offday/{userId}",
                HttpMethod.PUT,
                createHttpEntity(offDay),
                Void.class,
                userId
        ));
    }

}