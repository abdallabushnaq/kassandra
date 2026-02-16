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

import de.bushnaq.abdalla.kassandra.dao.VersionDAO;
import de.bushnaq.abdalla.kassandra.repository.ProductRepository;
import de.bushnaq.abdalla.kassandra.repository.VersionRepository;
import de.bushnaq.abdalla.kassandra.rest.exception.UniqueConstraintViolationException;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.service.ProductAclService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    @Autowired
    private ProductAclService productAclService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private VersionRepository versionRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("@aclSecurityService.hasVersionAccess(#id) or hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable Long id) {
        versionRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@aclSecurityService.hasVersionAccess(#id) or hasRole('ADMIN')")
    public ResponseEntity<VersionDAO> get(@PathVariable Long id) {
        return versionRepository.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("@aclSecurityService.hasProductAccess(#productId) or hasRole('ADMIN')")
    public List<VersionDAO> getAll(@PathVariable Long productId) {
        return versionRepository.findByProductId(productId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<VersionDAO> getAll() {
        // Admin can see all versions
        if (SecurityUtils.isAdmin()) {
            return versionRepository.findAll();
        }

        // Regular users only see versions of products they have access to
        return versionRepository.findAll().stream()
                .filter(version -> productAclService.hasAccess(version.getProductId(), SecurityUtils.getUserEmail()))
                .collect(Collectors.toList());
    }

    @GetMapping("/product/{productId}/by-name/{name}")
    @PreAuthorize("@aclSecurityService.hasProductAccess(#productId) or hasRole('ADMIN')")
    public ResponseEntity<VersionDAO> getByName(@PathVariable Long productId, @PathVariable String name) {
        VersionDAO version = versionRepository.findByNameAndProductId(name, productId);
        if (version == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(version);
    }

    @PostMapping()
    @PreAuthorize("@aclSecurityService.hasProductAccess(#version.productId) or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<VersionDAO> persist(@RequestBody VersionDAO version) {
        return productRepository.findById(version.getProductId()).map(product -> {
            // Check if a version with the same name already exists for this product
            if (versionRepository.existsByNameAndProductId(version.getName(), version.getProductId())) {
                throw new UniqueConstraintViolationException("Version", "name", version.getName());
            }
            VersionDAO save = versionRepository.save(version);
            return ResponseEntity.ok(save);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping()
    @PreAuthorize("@aclSecurityService.hasVersionAccess(#version.id) or hasRole('ADMIN')")
    @Transactional
    public void update(@RequestBody VersionDAO version) {
        // Check if another version with the same name exists in the same product (excluding the current version)
        if (versionRepository.existsByNameAndProductIdAndIdNot(version.getName(), version.getProductId(), version.getId())) {
            throw new UniqueConstraintViolationException("Version", "name", version.getName());
        }
        versionRepository.save(version);
    }
}