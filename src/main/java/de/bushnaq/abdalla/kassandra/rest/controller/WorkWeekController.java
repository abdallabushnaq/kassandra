/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

import de.bushnaq.abdalla.kassandra.dao.WorkWeekDAO;
import de.bushnaq.abdalla.kassandra.repository.WorkWeekRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing global work week definitions.
 * All operations require ADMIN role.
 */
@RestController
@RequestMapping("/api/work-week")
@Slf4j
public class WorkWeekController {

    @Autowired
    private WorkWeekRepository workWeekRepository;

    /**
     * Create a new work week.
     *
     * @param workWeek the work week to create
     * @return the created work week
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkWeekDAO> create(@RequestBody WorkWeekDAO workWeek) {
        log.info("Creating work week: {}", workWeek.getName());
        WorkWeekDAO saved = workWeekRepository.save(workWeek);
        return ResponseEntity.ok(saved);
    }

    /**
     * Delete a work week by ID.
     *
     * @param id the work week ID
     * @return 200 OK or 404
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        if (!workWeekRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        log.info("Deleting work week: {}", id);
        workWeekRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all work weeks.
     *
     * @return list of all work weeks
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<WorkWeekDAO> getAll() {
        return workWeekRepository.findAll();
    }

    /**
     * Get a single work week by ID.
     *
     * @param id the work week ID
     * @return the work week, or 404
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<WorkWeekDAO> getById(@PathVariable Long id) {
        return workWeekRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing work week.
     *
     * @param id       the ID of the work week to update
     * @param workWeek the updated data
     * @return 200 OK or 404
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> update(@PathVariable Long id, @RequestBody WorkWeekDAO workWeek) {
        if (!workWeekRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        workWeek.setId(id);
        log.info("Updating work week: {}", id);
        workWeekRepository.save(workWeek);
        return ResponseEntity.ok().build();
    }
}

