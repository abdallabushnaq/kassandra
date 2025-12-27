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

import com.fasterxml.jackson.core.JsonProcessingException;
import de.bushnaq.abdalla.kassandra.dao.TaskDAO;
import de.bushnaq.abdalla.kassandra.repository.SprintRepository;
import de.bushnaq.abdalla.kassandra.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private TaskRepository   taskRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void delete(@PathVariable Long id) {
        taskRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Optional<TaskDAO> get(@PathVariable Long id) throws JsonProcessingException {
        Optional<TaskDAO> task = taskRepository.findById(id);
        return task;
    }

    @GetMapping("/sprint/{sprintId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<TaskDAO> getAll(@PathVariable Long sprintId) {
        return taskRepository.findBySprintIdOrderByOrderIdAsc(sprintId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<TaskDAO> getAll() {
        return taskRepository.findAllByOrderByOrderIdAsc();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public TaskDAO save(@RequestBody TaskDAO task) {
        // Assign the next available orderId if not set or set to 0

        if (task.getOrderId() == null || task.getOrderId() == -1) {
            Integer maxOrderId = taskRepository.findMaxOrderId(task.getSprintId());
            task.setOrderId(maxOrderId + 1);
        }
        return taskRepository.save(task);
    }

    @PutMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void update(@RequestBody TaskDAO task) {
        taskRepository.save(task);
    }

    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void updateStatus(@PathVariable Long id, @PathVariable de.bushnaq.abdalla.kassandra.dto.TaskStatus status) {
        Optional<TaskDAO> taskOptional = taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            TaskDAO task = taskOptional.get();
            task.setTaskStatus(status);
            taskRepository.save(task);
        }
    }
}