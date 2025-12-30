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

import de.bushnaq.abdalla.kassandra.dao.LocationDAO;
import de.bushnaq.abdalla.kassandra.repository.LocationRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Check if the current user can modify the availability of the target user.
     * Admins can modify any user's availability, regular users can only modify their own.
     *
     * @param targetUserId the ID of the user whose availability is being modified
     * @return true if modification is allowed, false otherwise
     */
    private boolean canUserModifyLocation(Long targetUserId) {
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
    public ResponseEntity<Object> delete(@PathVariable Long userId, @PathVariable Long id) {
        // Check authorization: Users can only delete their own availability, admins can delete for any user
        if (!canUserModifyLocation(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own location");
        }

        return userRepository.findById(userId).map(user -> {
            LocationDAO location = locationRepository.findById(id).orElseThrow();
            if (Objects.equals(user.getLocations().getFirst().getId(), id))
                throw new IllegalArgumentException("Cannot delete the first location");
            user.getLocations().remove(location);
            userRepository.save(user);
            locationRepository.deleteById(id);
            return ResponseEntity.ok().build(); // Return 200 OK
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationDAO> getById(@PathVariable Long id) {
        return locationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LocationDAO> save(@RequestBody LocationDAO location, @PathVariable Long userId) {
        // Check authorization: Users can only delete their own availability, admins can delete for any user
        if (!canUserModifyLocation(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own location");
        }

        return userRepository.findById(userId).map(user -> {
            location.setUser(user);
            LocationDAO save = locationRepository.save(location);
            return ResponseEntity.ok(save);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Object> update(@RequestBody LocationDAO location, @PathVariable Long userId) {
        // Check authorization: Users can only delete their own availability, admins can delete for any user
        if (!canUserModifyLocation(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own location");
        }

        return userRepository.findById(userId).map(user -> {
            location.setUser(user);
            LocationDAO save = locationRepository.save(location);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}