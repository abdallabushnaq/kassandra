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

import de.bushnaq.abdalla.kassandra.dto.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

@Service
public class UserGroupApi extends AbstractApi {

    public UserGroupApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    @Autowired
    public UserGroupApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    public UserGroupApi() {

    }

    /**
     * Add a user to a group
     *
     * @param groupId the group ID
     * @param userId  the user ID
     */
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

    /**
     * Create a new user group (legacy method for tests)
     *
     * @param name        the group name
     * @param description the group description
     * @param memberIds   the IDs of users to add as members
     * @return the created group
     */
    public UserGroup create(String name, String description, Set<Long> memberIds) {
        UserGroup userGroup = new UserGroup();
        userGroup.setName(name);
        userGroup.setDescription(description);
        userGroup.setMemberIds(memberIds != null ? memberIds : new HashSet<>());
        return persist(userGroup);
    }

    /**
     * Delete a user group by ID
     *
     * @param id the group ID
     */
    public void deleteById(Long id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-group/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    /**
     * Get all user groups
     *
     * @return list of all groups
     */
    public List<UserGroup> getAll() {
        ResponseEntity<UserGroup[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-group",
                HttpMethod.GET,
                createHttpEntity(),
                UserGroup[].class
        ));
        return Arrays.asList(response.getBody());
    }

    /**
     * Get a single user group by ID
     *
     * @param id the group ID
     * @return the group
     */
    public UserGroup getById(Long id) {
        ResponseEntity<UserGroup> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-group/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                UserGroup.class,
                id
        ));
        return response.getBody();
    }

    /**
     * Create a new user group
     *
     * @param userGroup the group to create
     * @return the created group
     */
    public UserGroup persist(UserGroup userGroup) {
        ResponseEntity<UserGroup> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-group",
                HttpMethod.POST,
                createHttpEntity(userGroup),
                UserGroup.class
        ));
        return response.getBody();
    }

    /**
     * Remove a user from a group
     *
     * @param groupId the group ID
     * @param userId  the user ID
     */
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

    /**
     * Update an existing user group
     *
     * @param userGroup the group to update
     */
    public void update(UserGroup userGroup) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", userGroup.getName());
        body.put("description", userGroup.getDescription());
        body.put("memberIds", userGroup.getMemberIds());

        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/user-group/{id}",
                HttpMethod.PUT,
                createHttpEntity(body),
                Void.class,
                userGroup.getId()
        ));
    }

    /**
     * Update an existing user group (legacy method for tests)
     *
     * @param id          the group ID
     * @param name        the group name
     * @param description the group description
     * @param memberIds   the IDs of users in the group
     * @return the updated group
     */
    public UserGroup update(Long id, String name, String description, Set<Long> memberIds) {
        UserGroup userGroup = getById(id);
        userGroup.setName(name);
        userGroup.setDescription(description);
        userGroup.setMemberIds(memberIds != null ? memberIds : new HashSet<>());

        update(userGroup);

        return getById(id);
    }
}

