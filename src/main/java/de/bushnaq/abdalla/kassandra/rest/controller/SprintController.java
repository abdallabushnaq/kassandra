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

import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dao.SprintAvatarDAO;
import de.bushnaq.abdalla.kassandra.dao.SprintAvatarGenerationDataDAO;
import de.bushnaq.abdalla.kassandra.dao.SprintDAO;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.repository.*;
import de.bushnaq.abdalla.kassandra.rest.exception.UniqueConstraintViolationException;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.service.ProductAclService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sprint")
@Slf4j
public class SprintController {

    /** Sorts sprints by start date ascending (nulls last), then by id for a stable secondary order. */
    private static final Comparator<SprintDAO> BY_START_THEN_ID =
            Comparator.comparing(SprintDAO::getStart, Comparator.nullsLast(Comparator.naturalOrder()))
                      .thenComparing(SprintDAO::getId);

    @Autowired
    EntityManager entityManager;
    @Autowired
    private FeatureRepository                    featureRepository;
    @Autowired
    private ProductAclService                    productAclService;
    @Autowired
    private SprintAvatarGenerationDataRepository sprintAvatarGenerationDataRepository;
    @Autowired
    private SprintAvatarRepository               sprintAvatarRepository;
    @Autowired
    private SprintRepository                     sprintRepository;
    @Autowired
    private VersionRepository                    versionRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("@aclSecurityService.hasSprintAccess(#id) or hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable UUID id) {
        // Prevent deletion of the Backlog sprint
        SprintDAO sprint = sprintRepository.findById(id).orElseThrow();
        if (DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(sprint.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the Backlog sprint");
        }

        // Delete avatars first (cascade delete)
        sprintAvatarRepository.deleteBySprintId(id);
        sprintAvatarGenerationDataRepository.deleteBySprintId(id);
        // Then delete sprint
        sprintRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@aclSecurityService.hasSprintAccess(#id) or hasRole('ADMIN')")
    public SprintDAO get(@PathVariable UUID id) {
        SprintDAO sprintEntity = sprintRepository.findById(id).orElseThrow();
        return sprintEntity;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<SprintDAO> getAll() {
        // Admin can see all sprints
        if (SecurityUtils.isAdmin()) {
            return sprintRepository.findAll().stream()
                    .sorted(BY_START_THEN_ID)
                    .collect(Collectors.toList());
        }

        // Regular users only see sprints of products they have access to
        return sprintRepository.findAll().stream()
                .filter(sprint -> {
                    UUID productId = featureRepository.findById(sprint.getFeatureId())
                            .flatMap(feature -> versionRepository.findById(feature.getVersionId()))
                            .map(version -> version.getProductId())
                            .orElse(null);
                    return productId != null && productAclService.hasAccess(productId, SecurityUtils.getUserEmail());
                })
                .sorted(BY_START_THEN_ID)
                .collect(Collectors.toList());
    }

    @GetMapping("/feature/{featureId}")
    @PreAuthorize("@aclSecurityService.hasFeatureAccess(#featureId) or hasRole('ADMIN')")
    public List<SprintDAO> getAll(@PathVariable UUID featureId) {
        return sprintRepository.findByFeatureId(featureId).stream()
                .sorted(BY_START_THEN_ID)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/avatar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarWrapper> getAvatar(@PathVariable UUID id) {
        return sprintAvatarRepository.findBySprintId(id)
                .map(avatar -> {
                    if (avatar.getLightAvatarImage() == null || avatar.getLightAvatarImage().length == 0) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body((AvatarWrapper) null);
                    }
                    return ResponseEntity.ok(new AvatarWrapper(avatar.getLightAvatarImage()));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("/{id}/avatar/full")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarUpdateRequest> getAvatarFull(@PathVariable UUID id) {
        AvatarUpdateRequest response = new AvatarUpdateRequest();

        // Get avatar images (light + dark)
        sprintAvatarRepository.findBySprintId(id)
                .ifPresent(avatar -> {
                    response.setLightAvatarImage(avatar.getLightAvatarImage());
                    response.setDarkAvatarImage(avatar.getDarkAvatarImage());
                });

        // Get generation data (originals + prompt)
        sprintAvatarGenerationDataRepository.findBySprintId(id)
                .ifPresent(genData -> {
                    response.setLightAvatarImageOriginal(genData.getLightAvatarImageOriginal());
                    response.setDarkAvatarImageOriginal(genData.getDarkAvatarImageOriginal());
                    response.setLightAvatarPrompt(genData.getLightAvatarPrompt());
                    response.setDarkAvatarPrompt(genData.getDarkAvatarPrompt());
                    response.setLightAvatarNegativePrompt(genData.getLightAvatarNegativePrompt());
                    response.setDarkAvatarNegativePrompt(genData.getDarkAvatarNegativePrompt());
                });

        return ResponseEntity.ok(response);
    }

    @GetMapping("/backlog")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public SprintDAO getBacklogSprint() {
        SprintDAO backlog = sprintRepository.findByName(DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME);
        if (backlog == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Backlog sprint not found");
        }
        return backlog;
    }

    @GetMapping("/feature/{featureId}/by-name/{name}")
    @PreAuthorize("@aclSecurityService.hasFeatureAccess(#featureId) or hasRole('ADMIN')")
    public ResponseEntity<SprintDAO> getByName(@PathVariable UUID featureId, @PathVariable String name) {
        SprintDAO sprint = sprintRepository.findByNameAndFeatureId(name, featureId);
        if (sprint == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sprint);
    }

    /**
     * Return the dark-mode avatar for the given sprint.
     * Falls back to the light avatar when no dark variant has been stored yet.
     *
     * @param id The sprint ID
     * @return The dark avatar image, or the light avatar as fallback, or 404 if no avatar exists
     */
    @GetMapping("/{id}/dark-avatar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarWrapper> getDarkAvatar(@PathVariable UUID id) {
        return sprintAvatarRepository.findBySprintId(id)
                .map(avatar -> {
                    byte[] imageBytes = avatar.getDarkAvatarImage();
                    if (imageBytes == null || imageBytes.length == 0) {
                        // Fall back to light image when dark variant not yet generated
                        imageBytes = avatar.getLightAvatarImage();
                    }
                    if (imageBytes == null || imageBytes.length == 0) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body((AvatarWrapper) null);
                    }
                    return ResponseEntity.ok(new AvatarWrapper(imageBytes));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping()
    @PreAuthorize("@aclSecurityService.hasFeatureAccess(#sprintDAO.featureId) or hasRole('ADMIN')")
    @Transactional
    public SprintDAO save(@RequestBody SprintDAO sprintDAO) {
        // Prevent creating another Backlog sprint (globally unique name)
        if (DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(sprintDAO.getName())) {
            SprintDAO existingBacklog = sprintRepository.findByName(DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME);
            if (existingBacklog != null) {
                throw new UniqueConstraintViolationException("Sprint", "name", sprintDAO.getName());
            }
        }

        // Check if a sprint with the same name already exists for this feature
        if (sprintRepository.existsByNameAndFeatureId(sprintDAO.getName(), sprintDAO.getFeatureId())) {
            throw new UniqueConstraintViolationException("Sprint", "name", sprintDAO.getName());
        }
        entityManager.persist(sprintDAO);
//        SprintDAO save = sprintRepository.save(sprintDAO);
        return sprintDAO;
    }

    @PutMapping()
    @PreAuthorize("@aclSecurityService.hasSprintAccess(#sprintEntity.id) or hasRole('ADMIN')")
    @Transactional
    public SprintDAO update(@RequestBody SprintDAO sprintEntity) {
        // Check if another sprint with the same name exists in the same feature (excluding the current sprint)
        if (sprintRepository.existsByNameAndFeatureIdAndIdNot(sprintEntity.getName(), sprintEntity.getFeatureId(), sprintEntity.getId())) {
            throw new UniqueConstraintViolationException("Sprint", "name", sprintEntity.getName());
        }
        return sprintRepository.save(sprintEntity);
    }

    @PutMapping("/{id}/avatar/full")
    @PreAuthorize("@aclSecurityService.hasSprintAccess(#id) or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> updateAvatarFull(@PathVariable UUID id, @RequestBody AvatarUpdateRequest request) {
        // Verify sprint exists
        Optional<SprintDAO> sprintOpt = sprintRepository.findById(id);
        if (sprintOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        SprintDAO sprint = sprintOpt.get();

        // Update or create avatar image (light)
        if (request.getLightAvatarImage() != null && request.getLightAvatarImage().length != 0) {
            SprintAvatarDAO avatar = sprintAvatarRepository.findBySprintId(id)
                    .orElse(new SprintAvatarDAO());
            avatar.setSprintId(id);
            avatar.setLightAvatarImage(request.getLightAvatarImage());
            sprintAvatarRepository.save(avatar);
        }

        // Update or create dark avatar image; compute and persist darkAvatarHash automatically
        if (request.getDarkAvatarImage() != null && request.getDarkAvatarImage().length != 0) {
            SprintAvatarDAO avatar = sprintAvatarRepository.findBySprintId(id)
                    .orElse(new SprintAvatarDAO());
            avatar.setSprintId(id);
            avatar.setDarkAvatarImage(request.getDarkAvatarImage());
            sprintAvatarRepository.save(avatar);

            sprint.setDarkAvatarHash(AvatarUtil.computeHash(request.getDarkAvatarImage()));
            sprintRepository.save(sprint);
        }

        // Update or create generation data (light + dark originals + prompts)
        if (request.getLightAvatarImageOriginal() != null || request.getDarkAvatarImageOriginal() != null
                || request.getLightAvatarPrompt() != null || request.getDarkAvatarPrompt() != null
                || request.getLightAvatarNegativePrompt() != null || request.getDarkAvatarNegativePrompt() != null) {
            SprintAvatarGenerationDataDAO genData = sprintAvatarGenerationDataRepository.findBySprintId(id)
                    .orElse(new SprintAvatarGenerationDataDAO());
            genData.setSprintId(id);

            if (request.getLightAvatarImageOriginal() != null && request.getLightAvatarImageOriginal().length != 0) {
                genData.setLightAvatarImageOriginal(request.getLightAvatarImageOriginal());
            }

            if (request.getDarkAvatarImageOriginal() != null && request.getDarkAvatarImageOriginal().length != 0) {
                genData.setDarkAvatarImageOriginal(request.getDarkAvatarImageOriginal());
            }

            if (request.getLightAvatarPrompt() != null) {
                genData.setLightAvatarPrompt(request.getLightAvatarPrompt());
            }

            if (request.getDarkAvatarPrompt() != null) {
                genData.setDarkAvatarPrompt(request.getDarkAvatarPrompt());
            }

            if (request.getLightAvatarNegativePrompt() != null) {
                genData.setLightAvatarNegativePrompt(request.getLightAvatarNegativePrompt());
            }

            if (request.getDarkAvatarNegativePrompt() != null) {
                genData.setDarkAvatarNegativePrompt(request.getDarkAvatarNegativePrompt());
            }

            sprintAvatarGenerationDataRepository.save(genData);
        }

        log.info("SprintController.updateAvatarFull: Updated avatar for sprintId {}", id);
        return ResponseEntity.ok().build();
    }
}