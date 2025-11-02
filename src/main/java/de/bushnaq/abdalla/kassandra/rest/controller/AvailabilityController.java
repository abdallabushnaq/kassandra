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

import de.bushnaq.abdalla.kassandra.dao.AvailabilityDAO;
import de.bushnaq.abdalla.kassandra.repository.AvailabilityRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private UserRepository userRepository;

    @DeleteMapping("/{userId}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> delete(@PathVariable Long userId, @PathVariable Long id) {
        return userRepository.findById(userId).map(user ->
                availabilityRepository.findById(id).map(availability -> {
                    if (Objects.equals(user.getAvailabilities().getFirst().getId(), id))
                        throw new IllegalArgumentException("Cannot delete the first availability");
                    user.getAvailabilities().remove(availability);
                    userRepository.save(user);
                    availabilityRepository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build()) // Return 404 if availability not found
        ).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvailabilityDAO> getById(@PathVariable Long id) {
        return availabilityRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AvailabilityDAO> save(@RequestBody AvailabilityDAO availability, @PathVariable Long userId) {
        return userRepository.findById(userId).map(user -> {
            availability.setUser(user);
            AvailabilityDAO save = availabilityRepository.save(availability);
            return ResponseEntity.ok(save);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> update(@RequestBody AvailabilityDAO availability, @PathVariable Long userId) {
        return userRepository.findById(userId).map(user -> {
            availability.setUser(user);
            AvailabilityDAO save = availabilityRepository.save(availability);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}