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

import de.bushnaq.abdalla.kassandra.dao.WorklogDAO;
import de.bushnaq.abdalla.kassandra.repository.WorklogRepository;
import de.bushnaq.abdalla.kassandra.service.PlanningChangeService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/worklog")
public class WorklogController {

    @Autowired
    EntityManager entityManager;
    @Autowired
    private WorklogRepository worklogRepository;
    @Autowired
    private PlanningChangeService planningChangeService;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void delete(@PathVariable UUID id) {
        planningChangeService.delete(WorklogDAO.class, id, "Deleted worklog");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Optional<WorklogDAO> get(@PathVariable UUID id) {
        Optional<WorklogDAO> task = worklogRepository.findById(id);
        return task;
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<WorklogDAO> getAll() {
        return worklogRepository.findAll();
    }

    @GetMapping("/sprint/{sprintId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<WorklogDAO> getBySprintId(@PathVariable UUID sprintId) {
        return worklogRepository.findBySprintId(sprintId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Transactional
    public ResponseEntity<WorklogDAO> save(@RequestBody WorklogDAO worklog) {
        planningChangeService.persist(worklog, "Created worklog");
        return ResponseEntity.ok(worklog);
    }

    /**
     * Batch-saves a list of worklogs in a single transaction.
     * <p>
     * The caller (test data generators) is responsible for ensuring the supplied worklogs reference
     * valid task and sprint IDs. No per-item validation is performed so the entire list can be
     * flushed efficiently in one round-trip.
     * </p>
     *
     * @param worklogs list of worklogs to persist
     * @return the saved worklogs including their server-assigned IDs
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<WorklogDAO>> saveBatch(@RequestBody List<WorklogDAO> worklogs) {
        return ResponseEntity.ok(planningChangeService.persistBatch(worklogs, "Created worklog batch"));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public WorklogDAO update(@RequestBody WorklogDAO worklog) {
        return planningChangeService.update(worklog, "Updated worklog");
    }

}