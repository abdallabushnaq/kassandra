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

import de.bushnaq.abdalla.kassandra.dao.FeatureDAO;
import de.bushnaq.abdalla.kassandra.repository.FeatureRepository;
import de.bushnaq.abdalla.kassandra.repository.VersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/feature")
public class FeatureController {

    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private VersionRepository versionRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        featureRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FeatureDAO> get(@PathVariable Long id) {
        return featureRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/version/{versionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<FeatureDAO> getAll(@PathVariable Long versionId) {
        return featureRepository.findByVersionId(versionId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<FeatureDAO> getAll() {
        return featureRepository.findAll();
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureDAO> save(@RequestBody FeatureDAO feature) {
        return versionRepository.findById(feature.getVersionId()).map(version -> {
            // Check if a feature with the same name already exists for this version
            if (featureRepository.existsByNameAndVersionId(feature.getName(), feature.getVersionId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A feature with name '" + feature.getName() + "' already exists for this version");
            }
            FeatureDAO save = featureRepository.save(feature);
            return ResponseEntity.ok(save);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public FeatureDAO update(@RequestBody FeatureDAO feature) {
        // Check if another feature with the same name exists in the same version (excluding the current feature)
        FeatureDAO existingFeature = featureRepository.findByNameAndVersionId(feature.getName(), feature.getVersionId());
        if (existingFeature != null && !existingFeature.getId().equals(feature.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another feature with name '" + feature.getName() + "' already exists for this version");
        }
        return featureRepository.save(feature);
    }
}
