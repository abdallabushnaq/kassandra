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

import de.bushnaq.abdalla.kassandra.dao.FeatureAvatarDAO;
import de.bushnaq.abdalla.kassandra.dao.FeatureAvatarGenerationDataDAO;
import de.bushnaq.abdalla.kassandra.dao.FeatureDAO;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.repository.FeatureAvatarGenerationDataRepository;
import de.bushnaq.abdalla.kassandra.repository.FeatureAvatarRepository;
import de.bushnaq.abdalla.kassandra.repository.FeatureRepository;
import de.bushnaq.abdalla.kassandra.repository.VersionRepository;
import de.bushnaq.abdalla.kassandra.rest.exception.UniqueConstraintViolationException;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.service.ProductAclService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feature")
@Slf4j
public class FeatureController {

    @Autowired
    private FeatureAvatarGenerationDataRepository featureAvatarGenerationDataRepository;
    @Autowired
    private FeatureAvatarRepository               featureAvatarRepository;
    @Autowired
    private FeatureRepository                     featureRepository;
    @Autowired
    private ProductAclService                     productAclService;
    @Autowired
    private VersionRepository                     versionRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("@aclSecurityService.hasFeatureAccess(#id) or hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable Long id) {
        // Delete avatars first (cascade delete)
        featureAvatarRepository.deleteByFeatureId(id);
        featureAvatarGenerationDataRepository.deleteByFeatureId(id);
        // Then delete feature
        featureRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@aclSecurityService.hasFeatureAccess(#id) or hasRole('ADMIN')")
    public ResponseEntity<FeatureDAO> get(@PathVariable Long id) {
        return featureRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/version/{versionId}")
    @PreAuthorize("@aclSecurityService.hasVersionAccess(#versionId) or hasRole('ADMIN')")
    public List<FeatureDAO> getAll(@PathVariable Long versionId) {
        return featureRepository.findByVersionId(versionId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<FeatureDAO> getAll() {
        // Admin can see all features
        if (SecurityUtils.isAdmin()) {
            return featureRepository.findAll();
        }

        // Regular users only see features of products they have access to
        return featureRepository.findAll().stream()
                .filter(feature -> {
                    Long productId = versionRepository.findById(feature.getVersionId())
                            .map(version -> version.getProductId())
                            .orElse(null);
                    return productId != null && productAclService.hasAccess(productId, SecurityUtils.getUserEmail());
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/avatar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarWrapper> getAvatar(@PathVariable Long id) {
        return featureAvatarRepository.findByFeatureId(id)
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
        featureAvatarRepository.findByFeatureId(id)
                .ifPresent(avatar -> response.setAvatarImage(avatar.getAvatarImage()));

        // Get generation data
        featureAvatarGenerationDataRepository.findByFeatureId(id)
                .ifPresent(genData -> {
                    response.setAvatarImageOriginal(genData.getAvatarImageOriginal());
                    response.setAvatarPrompt(genData.getAvatarPrompt());
                });

        return ResponseEntity.ok(response);
    }

    @PostMapping()
    @PreAuthorize("@aclSecurityService.hasVersionAccess(#feature.versionId) or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<FeatureDAO> save(@RequestBody FeatureDAO feature) {
        return versionRepository.findById(feature.getVersionId()).map(version -> {
            // Check if a feature with the same name already exists for this version
            if (featureRepository.existsByNameAndVersionId(feature.getName(), feature.getVersionId())) {
                throw new UniqueConstraintViolationException("Feature", "name", feature.getName());
            }
            FeatureDAO save = featureRepository.save(feature);
            return ResponseEntity.ok(save);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping()
    @PreAuthorize("@aclSecurityService.hasFeatureAccess(#feature.id) or hasRole('ADMIN')")
    @Transactional
    public FeatureDAO update(@RequestBody FeatureDAO feature) {
        // Check if another feature with the same name exists in the same version (excluding the current feature)
        if (featureRepository.existsByNameAndVersionIdAndIdNot(feature.getName(), feature.getVersionId(), feature.getId())) {
            throw new UniqueConstraintViolationException("Feature", "name", feature.getName());
        }
        return featureRepository.save(feature);
    }

    @PutMapping("/{id}/avatar/full")
    @PreAuthorize("@aclSecurityService.hasFeatureAccess(#id) or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> updateAvatarFull(@PathVariable Long id, @RequestBody AvatarUpdateRequest request) {
        // Verify feature exists
        Optional<FeatureDAO> featureOpt = featureRepository.findById(id);
        if (featureOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Update or create avatar image
        if (request.getAvatarImage() != null && request.getAvatarImage().length != 0) {
            FeatureAvatarDAO avatar = featureAvatarRepository.findByFeatureId(id)
                    .orElse(new FeatureAvatarDAO());
            avatar.setFeatureId(id);
            avatar.setAvatarImage(request.getAvatarImage());
            featureAvatarRepository.save(avatar);
        }

        // Update or create generation data
        if (request.getAvatarImageOriginal() != null || request.getAvatarPrompt() != null) {
            FeatureAvatarGenerationDataDAO genData = featureAvatarGenerationDataRepository.findByFeatureId(id)
                    .orElse(new FeatureAvatarGenerationDataDAO());
            genData.setFeatureId(id);

            if (request.getAvatarImageOriginal() != null && request.getAvatarImageOriginal().length != 0) {
                genData.setAvatarImageOriginal(request.getAvatarImageOriginal());
            }

            if (request.getAvatarPrompt() != null) {
                genData.setAvatarPrompt(request.getAvatarPrompt());
            }

            featureAvatarGenerationDataRepository.save(genData);
        }

        log.info("FeatureController.updateAvatarFull: Updated avatar for featureId {}", id);
        return ResponseEntity.ok().build();
    }
}
