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
import de.bushnaq.abdalla.kassandra.dto.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class TaskApi extends AbstractApi {

    public TaskApi(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl) {
        super(restTemplate, objectMapper, baseUrl);
    }

    @Autowired
    public TaskApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    public TaskApi() {

    }


    public void deleteById(Long taskId) throws org.springframework.web.client.RestClientException {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/task/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                taskId
        ));
    }

    public List<Task> getAll(Long sprintId) {
        ResponseEntity<Task[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/task/sprint/{sprintId}",
                HttpMethod.GET,
                createHttpEntity(),
                Task[].class,
                sprintId
        ));
        Task[] body = response.getBody();
        if (body == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(body));
    }

    public List<Task> getAll() {
        ResponseEntity<Task[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/task",
                HttpMethod.GET,
                createHttpEntity(),
                Task[].class
        ));
        Task[] body = response.getBody();
        if (body == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(body));
    }

    public Task getById(Long id) {
        ResponseEntity<Task> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/task/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                Task.class,
                id
        ));
        return response.getBody();
    }

    public Task persist(Task task) {
        String a = task.getName();
        ResponseEntity<Task> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/task",
                HttpMethod.POST,
                createHttpEntity(task),
                Task.class
        ));
        return response.getBody();
    }

    public void update(Task task) {
        String a = task.getName();
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/task",
                HttpMethod.PUT,
                createHttpEntity(task),
                Void.class
        ));
    }

    public void updateTaskStatus(Long taskId, de.bushnaq.abdalla.kassandra.dto.TaskStatus newStatus) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/task/{id}/status/{status}",
                HttpMethod.PUT,
                createHttpEntity(),
                Void.class,
                taskId,
                newStatus
        ));
    }
}
