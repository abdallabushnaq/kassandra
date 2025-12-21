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

import de.bushnaq.abdalla.kassandra.dao.ProductAvatarDAO;
import de.bushnaq.abdalla.kassandra.dao.ProductAvatarGenerationDataDAO;
import de.bushnaq.abdalla.kassandra.dao.ProductDAO;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.repository.ProductAvatarGenerationDataRepository;
import de.bushnaq.abdalla.kassandra.repository.ProductAvatarRepository;
import de.bushnaq.abdalla.kassandra.repository.ProductRepository;
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
@RequestMapping("/api/product")
@Slf4j
public class ProductController {

    @Autowired
    private ProductAvatarGenerationDataRepository productAvatarGenerationDataRepository;
    @Autowired
    private ProductAvatarRepository               productAvatarRepository;
    @Autowired
    private ProductRepository                     productRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable Long id) {
        // Delete avatars first (cascade delete)
        productAvatarRepository.deleteByProductId(id);
        productAvatarGenerationDataRepository.deleteByProductId(id);
        // Then delete product
        productRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Optional<ProductDAO> get(@PathVariable Long id) {
        ProductDAO productEntity = productRepository.findById(id).orElseThrow();
        return Optional.of(productEntity);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<ProductDAO> getAll() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}/avatar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarWrapper> getAvatar(@PathVariable Long id) {
        return productAvatarRepository.findByProductId(id)
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
        productAvatarRepository.findByProductId(id)
                .ifPresent(avatar -> response.setAvatarImage(avatar.getAvatarImage()));

        // Get generation data
        productAvatarGenerationDataRepository.findByProductId(id)
                .ifPresent(genData -> {
                    response.setAvatarImageOriginal(genData.getAvatarImageOriginal());
                    response.setAvatarPrompt(genData.getAvatarPrompt());
                });

        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDAO save(@RequestBody ProductDAO product) {
        // Check if a product with the same name already exists
        if (productRepository.existsByName(product.getName())) {
            throw new UniqueConstraintViolationException("Product", "name", product.getName());
        }
        return productRepository.save(product);
    }

    @PutMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public void update(@RequestBody ProductDAO product) {
        // Check if another product with the same name exists (excluding the current product)
        if (productRepository.existsByNameAndIdNot(product.getName(), product.getId())) {
            throw new UniqueConstraintViolationException("Product", "name", product.getName());
        }
        productRepository.save(product);
    }


    @PutMapping("/{id}/avatar/full")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> updateAvatarFull(@PathVariable Long id, @RequestBody AvatarUpdateRequest request) {
        // Verify product exists
        Optional<ProductDAO> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Update or create avatar image
        if (request.getAvatarImage() != null && request.getAvatarImage().length != 0) {
            ProductAvatarDAO avatar = productAvatarRepository.findByProductId(id)
                    .orElse(new ProductAvatarDAO());
            avatar.setProductId(id);
            avatar.setAvatarImage(request.getAvatarImage());
            productAvatarRepository.save(avatar);
        }

        // Update or create generation data
        if (request.getAvatarImageOriginal() != null || request.getAvatarPrompt() != null) {
            ProductAvatarGenerationDataDAO genData = productAvatarGenerationDataRepository.findByProductId(id)
                    .orElse(new ProductAvatarGenerationDataDAO());
            genData.setProductId(id);

            if (request.getAvatarImageOriginal() != null && request.getAvatarImageOriginal().length != 0) {
                genData.setAvatarImageOriginal(request.getAvatarImageOriginal());
            }

            if (request.getAvatarPrompt() != null) {
                genData.setAvatarPrompt(request.getAvatarPrompt());
            }

            productAvatarGenerationDataRepository.save(genData);
        }


        log.info("ProductController.updateAvatarFull: Updated avatar for productId {}", id);
        return ResponseEntity.ok().build();
    }
}