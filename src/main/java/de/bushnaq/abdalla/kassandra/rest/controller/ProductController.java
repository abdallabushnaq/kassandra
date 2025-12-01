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

import de.bushnaq.abdalla.kassandra.dao.ProductDAO;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
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
        Optional<ProductDAO> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            ProductDAO product = productOpt.get();
            if (product.getAvatarImage() == null || product.getAvatarImage().length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(new AvatarWrapper(product.getAvatarImage()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}/avatar/full")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarUpdateRequest> getAvatarFull(@PathVariable Long id) {
        Optional<ProductDAO> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            ProductDAO          product  = productOpt.get();
            AvatarUpdateRequest response = new AvatarUpdateRequest();
            response.setAvatarImage(product.getAvatarImage());
            response.setAvatarImageOriginal(product.getAvatarImageOriginal());
            response.setAvatarPrompt(product.getAvatarPrompt());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping(consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDAO save(@RequestBody ProductDAO product) {
        // Check if a product with the same name already exists
        if (productRepository.existsByName(product.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with name '" + product.getName() + "' already exists");
        }
        return productRepository.save(product);
    }

    @PutMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public void update(@RequestBody ProductDAO product) {
        // Check if another product with the same name exists (excluding the current product)
        ProductDAO existingProduct = productRepository.findByName(product.getName());
        if (existingProduct != null && !existingProduct.getId().equals(product.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another product with name '" + product.getName() + "' already exists");
        }
        productRepository.save(product);
    }

    @PutMapping("/{id}/avatar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateAvatar(@PathVariable Long id, @RequestBody byte[] avatarData) {
        Optional<ProductDAO> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            ProductDAO product = productOpt.get();
            product.setAvatarImage(avatarData);
            productRepository.save(product);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}/avatar/full")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateAvatarFull(@PathVariable Long id, @RequestBody AvatarUpdateRequest request) {
        Optional<ProductDAO> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            ProductDAO product = productOpt.get();

            // Decode and set resized avatar if provided
            if (request.getAvatarImage() != null && request.getAvatarImage().length != 0) {
                product.setAvatarImage(request.getAvatarImage());
            }

            // Decode and set original avatar if provided
            if (request.getAvatarImageOriginal() != null && request.getAvatarImageOriginal().length != 0) {
                product.setAvatarImageOriginal(request.getAvatarImageOriginal());
            }

            // Set prompt if provided
            if (request.getAvatarPrompt() != null) {
                product.setAvatarPrompt(request.getAvatarPrompt());
            }

            productRepository.save(product);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}