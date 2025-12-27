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

import de.bushnaq.abdalla.kassandra.dao.SprintAvatarDAO;
import de.bushnaq.abdalla.kassandra.dao.SprintAvatarGenerationDataDAO;
import de.bushnaq.abdalla.kassandra.dao.SprintDAO;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.repository.FeatureRepository;
import de.bushnaq.abdalla.kassandra.repository.SprintAvatarGenerationDataRepository;
import de.bushnaq.abdalla.kassandra.repository.SprintAvatarRepository;
import de.bushnaq.abdalla.kassandra.repository.SprintRepository;
import de.bushnaq.abdalla.kassandra.rest.exception.UniqueConstraintViolationException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sprint")
@Slf4j
public class SprintController {

    @Autowired
    private FeatureRepository                    featureRepository;
    @Autowired
    private SprintAvatarGenerationDataRepository sprintAvatarGenerationDataRepository;
    @Autowired
    private SprintAvatarRepository               sprintAvatarRepository;
    @Autowired
    private SprintRepository                     sprintRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Transactional
    public void delete(@PathVariable Long id) {
        // Delete avatars first (cascade delete)
        sprintAvatarRepository.deleteBySprintId(id);
        sprintAvatarGenerationDataRepository.deleteBySprintId(id);
        // Then delete sprint
        sprintRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public SprintDAO get(@PathVariable Long id) {
        SprintDAO sprintEntity = sprintRepository.findById(id).orElseThrow();
        return sprintEntity;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<SprintDAO> getAll() {
        return sprintRepository.findAll();
    }

    @GetMapping("/feature/{featureId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<SprintDAO> getAll(@PathVariable Long featureId) {
        return sprintRepository.findByFeatureId(featureId);
    }

    @GetMapping("/{id}/avatar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarWrapper> getAvatar(@PathVariable Long id) {
        return sprintAvatarRepository.findBySprintId(id)
                .map(avatar -> {
                    if (avatar.getAvatarImage() == null || avatar.getAvatarImage().length == 0) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body((AvatarWrapper) null);
                    }
                    return ResponseEntity.ok(new AvatarWrapper(avatar.getAvatarImage()));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("/{id}/avatar/full")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarUpdateRequest> getAvatarFull(@PathVariable Long id) {
        AvatarUpdateRequest response = new AvatarUpdateRequest();

        // Get avatar image
        sprintAvatarRepository.findBySprintId(id)
                .ifPresent(avatar -> response.setAvatarImage(avatar.getAvatarImage()));

        // Get generation data
        sprintAvatarGenerationDataRepository.findBySprintId(id)
                .ifPresent(genData -> {
                    response.setAvatarImageOriginal(genData.getAvatarImageOriginal());
                    response.setAvatarPrompt(genData.getAvatarPrompt());
                });

        return ResponseEntity.ok(response);
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public SprintDAO save(@RequestBody SprintDAO sprintDAO) {
        // Check if a sprint with the same name already exists for this feature
        if (sprintRepository.existsByNameAndFeatureId(sprintDAO.getName(), sprintDAO.getFeatureId())) {
            throw new UniqueConstraintViolationException("Sprint", "name", sprintDAO.getName());
        }
        SprintDAO save = sprintRepository.save(sprintDAO);
        return save;
    }

    @PutMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public SprintDAO update(@RequestBody SprintDAO sprintEntity) {
        // Check if another sprint with the same name exists in the same feature (excluding the current sprint)
        if (sprintRepository.existsByNameAndFeatureIdAndIdNot(sprintEntity.getName(), sprintEntity.getFeatureId(), sprintEntity.getId())) {
            throw new UniqueConstraintViolationException("Sprint", "name", sprintEntity.getName());
        }
        return sprintRepository.save(sprintEntity);
    }

    @PutMapping("/{id}/avatar/full")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Transactional
    public ResponseEntity<Void> updateAvatarFull(@PathVariable Long id, @RequestBody AvatarUpdateRequest request) {
        // Verify sprint exists
        Optional<SprintDAO> sprintOpt = sprintRepository.findById(id);
        if (sprintOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Update or create avatar image
        if (request.getAvatarImage() != null && request.getAvatarImage().length != 0) {
            SprintAvatarDAO avatar = sprintAvatarRepository.findBySprintId(id)
                    .orElse(new SprintAvatarDAO());
            avatar.setSprintId(id);
            avatar.setAvatarImage(request.getAvatarImage());
            sprintAvatarRepository.save(avatar);
        }

        // Update or create generation data
        if (request.getAvatarImageOriginal() != null || request.getAvatarPrompt() != null) {
            SprintAvatarGenerationDataDAO genData = sprintAvatarGenerationDataRepository.findBySprintId(id)
                    .orElse(new SprintAvatarGenerationDataDAO());
            genData.setSprintId(id);

            if (request.getAvatarImageOriginal() != null && request.getAvatarImageOriginal().length != 0) {
                genData.setAvatarImageOriginal(request.getAvatarImageOriginal());
            }

            if (request.getAvatarPrompt() != null) {
                genData.setAvatarPrompt(request.getAvatarPrompt());
            }

            sprintAvatarGenerationDataRepository.save(genData);
        }

        log.info("SprintController.updateAvatarFull: Updated avatar for sprintId {}", id);
        return ResponseEntity.ok().build();
    }
}