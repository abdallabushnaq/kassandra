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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private VersionRepository versionRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void delete(@PathVariable Long id) {
        versionRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<VersionDAO> get(@PathVariable Long id) {
        return versionRepository.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<VersionDAO> getAll(@PathVariable Long productId) {
        return versionRepository.findByProductId(productId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<VersionDAO> getAll() {
        return versionRepository.findAll();
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void update(@RequestBody VersionDAO version) {
        // Check if another version with the same name exists in the same product (excluding the current version)
        if (versionRepository.existsByNameAndProductIdAndIdNot(version.getName(), version.getProductId(), version.getId())) {
            throw new UniqueConstraintViolationException("Version", "name", version.getName());
        }
        versionRepository.save(version);
    }
}