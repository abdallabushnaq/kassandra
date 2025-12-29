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
import de.bushnaq.abdalla.kassandra.dto.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class UserGroupApi extends AbstractApi {

    public UserGroupApi(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl) {
        super(restTemplate, objectMapper, baseUrl);
    }

    @Autowired
    public UserGroupApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    public UserGroupApi() {
    }

    public void addMember(Long groupId, Long userId) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-group/{groupId}/members/{userId}",
                HttpMethod.POST,
                createHttpEntity(),
                Void.class,
                groupId,
                userId
        ));
    }

    public UserGroup create(String name, String description, Set<Long> memberIds) {
        return executeWithErrorHandling(() -> {
            Map<String, Object> request = new HashMap<>();
            request.put("name", name);
            request.put("description", description);
            request.put("memberIds", memberIds);

            ResponseEntity<UserGroup> response = restTemplate.exchange(
                    getBaseUrl() + "/user-group",
                    HttpMethod.POST,
                    createHttpEntity(request),
                    UserGroup.class
            );
            return response.getBody();
        });
    }

    public void deleteById(Long id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-group/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    public List<UserGroup> getAll() {
        return executeWithErrorHandling(() -> {
            ResponseEntity<UserGroup[]> response = restTemplate.exchange(
                    getBaseUrl() + "/user-group",
                    HttpMethod.GET,
                    createHttpEntity(),
                    UserGroup[].class
            );
            UserGroup[] groups = response.getBody();
            return groups != null ? Arrays.asList(groups) : new ArrayList<>();
        });
    }

    public UserGroup getById(Long id) {
        return executeWithErrorHandling(() -> {
            ResponseEntity<UserGroup> response = restTemplate.exchange(
                    getBaseUrl() + "/user-group/{id}",
                    HttpMethod.GET,
                    createHttpEntity(),
                    UserGroup.class,
                    id
            );
            return response.getBody();
        });
    }

    public void removeMember(Long groupId, Long userId) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-group/{groupId}/members/{userId}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                groupId,
                userId
        ));
    }

    public UserGroup update(Long id, String name, String description, Set<Long> memberIds) {
        return executeWithErrorHandling(() -> {
            Map<String, Object> request = new HashMap<>();
            request.put("name", name);
            request.put("description", description);
            request.put("memberIds", memberIds);

            ResponseEntity<UserGroup> response = restTemplate.exchange(
                    getBaseUrl() + "/user-group/{id}",
                    HttpMethod.PUT,
                    createHttpEntity(request),
                    UserGroup.class,
                    id
            );
            return response.getBody();
        });
    }
}

