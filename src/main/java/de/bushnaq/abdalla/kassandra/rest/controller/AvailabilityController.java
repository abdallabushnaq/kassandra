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
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    private UserRepository userRepository;

    /**
     * Check if the current user can modify the availability of the target user.
     * Admins can modify any user's availability, regular users can only modify their own.
     *
     * @param targetUserId the ID of the user whose availability is being modified
     * @return true if modification is allowed, false otherwise
     */
    private boolean canUserModifyAvailability(UUID targetUserId) {
        // Admins can modify any user's availability
        if (SecurityUtils.isAdmin()) {
            return true;
        }

        // Regular users can only modify their own availability
        String currentUserEmail = SecurityUtils.getUserEmail();
        return userRepository.findById(targetUserId)
                .map(user -> currentUserEmail.equals(user.getEmail()))
                .orElse(false);
    }

    @DeleteMapping("/{userId}/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Object> delete(@PathVariable UUID userId, @PathVariable UUID id) {
        // Check authorization: Users can only delete their own availability, admins can delete for any user
        if (!canUserModifyAvailability(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own availability");
        }

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
    public ResponseEntity<AvailabilityDAO> getById(@PathVariable UUID id) {
        return availabilityRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Transactional
    public ResponseEntity<AvailabilityDAO> save(@RequestBody AvailabilityDAO availability, @PathVariable UUID userId) {
        // Check authorization: Users can only save their own availability, admins can save for any user
        if (!canUserModifyAvailability(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own availability");
        }

        return userRepository.findById(userId).map(user -> {
            availability.setUser(user);
            entityManager.persist(availability); // INSERT, no SELECT, no cascade conflict
//            AvailabilityDAO save = availabilityRepository.save(availability);
            return ResponseEntity.ok(availability);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Object> update(@RequestBody AvailabilityDAO availability, @PathVariable UUID userId) {
        // Check authorization: Users can only update their own availability, admins can update for any user
        if (!canUserModifyAvailability(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own availability");
        }

        return userRepository.findById(userId).map(user -> {
//            if (!availabilityRepository.existsById(availability.getId())) {
//                return ResponseEntity.notFound().build();
//            }
            availability.setUser(user);
            availabilityRepository.save(availability);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}