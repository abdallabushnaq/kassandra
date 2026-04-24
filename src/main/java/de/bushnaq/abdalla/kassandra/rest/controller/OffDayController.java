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

import de.bushnaq.abdalla.kassandra.dao.OffDayDAO;
import de.bushnaq.abdalla.kassandra.repository.OffDayRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/offday")
public class OffDayController {

    @Autowired
    private OffDayRepository offDayRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Check if the current user can modify the availability of the target user.
     * Admins can modify any user's availability, regular users can only modify their own.
     *
     * @param targetUserId the ID of the user whose availability is being modified
     * @return true if modification is allowed, false otherwise
     */
    private boolean canUserModifyOffDay(UUID targetUserId) {
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
        // Check authorization: Users can only delete their own offday, admins can delete for any user
        if (!canUserModifyOffDay(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own availability");
        }

        return userRepository.findById(userId).map(
                user -> {
                    OffDayDAO offDay = offDayRepository.findById(id).orElseThrow();
                    user.getOffDays().remove(offDay);
                    userRepository.save(user);
                    offDayRepository.deleteById(id);
                    return ResponseEntity.ok().build(); // Return 200 OK
                }
        ).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Optional<OffDayDAO> getById(@PathVariable UUID id) {
        OffDayDAO e = offDayRepository.findById(id).orElseThrow();
        return Optional.of(e);
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<OffDayDAO> save(@RequestBody OffDayDAO offDay, @PathVariable UUID userId) {
        // Check authorization: Users can only delete their own offday, admins can delete for any user
        if (!canUserModifyOffDay(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own availability");
        }

        return userRepository.findById(userId).map(user -> {
            // Check for overlapping OffDays
            List<OffDayDAO> overlappingOffDays = offDayRepository.findOverlappingOffDays(
                    user,
                    offDay.getFirstDay(),
                    offDay.getLastDay(),
                    null // No ID to exclude since this is a new record
            );

            if (!overlappingOffDays.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This off day overlaps with existing off days for the user"
                );
            }

            offDay.setUser(user);
            OffDayDAO save = offDayRepository.save(offDay);
            return ResponseEntity.ok(save); // Return 200 OK
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Batch-saves a list of off days for the given user in a single transaction.
     * <p>
     * The caller (test data generators) is responsible for ensuring that the supplied off days do not
     * overlap with each other or with existing records. No per-item overlap check is performed here so
     * that the entire list can be flushed efficiently in one round-trip.
     * </p>
     *
     * @param offDays list of off days to persist
     * @param userId  ID of the owning user
     * @return the saved off days including their server-assigned IDs
     */
    @PostMapping("/{userId}/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<OffDayDAO>> saveBatch(@RequestBody List<OffDayDAO> offDays, @PathVariable UUID userId) {
        if (!canUserModifyOffDay(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own availability");
        }
        return userRepository.findById(userId).map(user -> {
            List<OffDayDAO> saved = new ArrayList<>(offDays.size());
            for (OffDayDAO offDay : offDays) {
                offDay.setUser(user);
                saved.add(offDayRepository.save(offDay));
            }
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Object> update(@RequestBody OffDayDAO offDay, @PathVariable UUID userId) {
        // Check authorization: Users can only delete their own offday, admins can delete for any user
        if (!canUserModifyOffDay(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own availability");
        }

        return userRepository.findById(userId).map(user -> {
            // Check for overlapping OffDays, excluding the current offDay being updated
            List<OffDayDAO> overlappingOffDays = offDayRepository.findOverlappingOffDays(
                    user,
                    offDay.getFirstDay(),
                    offDay.getLastDay(),
                    offDay.getId() // Exclude the current offDay
            );

            if (!overlappingOffDays.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This off day overlaps with existing off days for the user"
                );
            }

            offDay.setUser(user);
            OffDayDAO save = offDayRepository.save(offDay);
            return ResponseEntity.ok().build(); // Return 200 OK
        }).orElse(ResponseEntity.notFound().build());
    }
}