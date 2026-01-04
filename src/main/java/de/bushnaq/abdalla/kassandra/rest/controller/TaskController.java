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

package de.bushnaq.abdalla.kassandra.rest.controller;

import de.bushnaq.abdalla.kassandra.dao.TaskDAO;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.repository.FeatureRepository;
import de.bushnaq.abdalla.kassandra.repository.SprintRepository;
import de.bushnaq.abdalla.kassandra.repository.TaskRepository;
import de.bushnaq.abdalla.kassandra.repository.VersionRepository;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.service.ProductAclService;
import jakarta.transaction.Transactional;
import org.openqa.selenium.json.JsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private ProductAclService productAclService;
    @Autowired
    private SprintRepository  sprintRepository;
    @Autowired
    private TaskRepository    taskRepository;
    @Autowired
    private VersionRepository versionRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("@aclSecurityService.hasTaskAccess(#id) or hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable Long id) {
        taskRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@aclSecurityService.hasTaskAccess(#id) or hasRole('ADMIN')")
    public Optional<TaskDAO> get(@PathVariable Long id) throws JsonException {
        Optional<TaskDAO> task = taskRepository.findById(id);
        return task;
    }

    @GetMapping("/sprint/{sprintId}")
    @PreAuthorize("@aclSecurityService.hasSprintAccess(#sprintId) or hasRole('ADMIN')")
    public List<TaskDAO> getAll(@PathVariable Long sprintId) {
        return taskRepository.findBySprintIdOrderByOrderIdAsc(sprintId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<TaskDAO> getAll() {
        // Admin can see all tasks
        if (SecurityUtils.isAdmin()) {
            return taskRepository.findAllByOrderByOrderIdAsc();
        }

        // Regular users only see tasks of products they have access to
        return taskRepository.findAllByOrderByOrderIdAsc().stream()
                .filter(task -> {
                    Long productId = sprintRepository.findById(task.getSprintId())
                            .flatMap(sprint -> featureRepository.findById(sprint.getFeatureId()))
                            .flatMap(feature -> versionRepository.findById(feature.getVersionId()))
                            .map(version -> version.getProductId())
                            .orElse(null);
                    return productId != null && productAclService.hasAccess(productId, SecurityUtils.getUserEmail());
                })
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("@aclSecurityService.hasSprintAccess(#task.sprintId) or hasRole('ADMIN')")
    @Transactional
    public TaskDAO save(@RequestBody TaskDAO task) {
        // Assign the next available orderId if not set or set to 0

        if (task.getOrderId() == null || task.getOrderId() == -1) {
            Integer maxOrderId = taskRepository.findMaxOrderId(task.getSprintId());
            task.setOrderId(maxOrderId + 1);
        }
        TaskDAO    save   = taskRepository.save(task);
        JsonMapper mapper = new JsonMapper();
        String     s1     = mapper.writeValueAsString(save);
        Task       t1     = mapper.readValue(s1, Task.class);
        return save;
    }

    @PutMapping()
    @PreAuthorize("@aclSecurityService.hasTaskAccess(#task.id) or hasRole('ADMIN')")
    @Transactional
    public void update(@RequestBody TaskDAO task) {
        taskRepository.save(task);
    }

    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("@aclSecurityService.hasTaskAccess(#id) or hasRole('ADMIN')")
    @Transactional
    public void updateStatus(@PathVariable Long id, @PathVariable de.bushnaq.abdalla.kassandra.dto.TaskStatus status) {
        Optional<TaskDAO> taskOptional = taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            TaskDAO task = taskOptional.get();
            task.setTaskStatus(status);
            taskRepository.save(task);
        }
    }
}