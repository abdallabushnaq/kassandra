/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

import de.bushnaq.abdalla.kassandra.dao.UserWorkWeekDAO;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import de.bushnaq.abdalla.kassandra.repository.UserWorkWeekRepository;
import de.bushnaq.abdalla.kassandra.repository.WorkWeekRepository;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for managing user work-week assignments.
 * Admins can modify any user's assignments; regular users can only modify their own.
 */
@RestController
@RequestMapping("/api/user-work-week")
public class UserWorkWeekController {

    @Autowired
    private UserRepository         userRepository;
    @Autowired
    private UserWorkWeekRepository userWorkWeekRepository;
    @Autowired
    private WorkWeekRepository     workWeekRepository;

    /**
     * Check whether the current principal may modify assignments for {@code targetUserId}.
     *
     * @param targetUserId the target user ID
     * @return {@code true} if modification is allowed
     */
    private boolean canModify(Long targetUserId) {
        if (SecurityUtils.isAdmin()) return true;
        String email = SecurityUtils.getUserEmail();
        return userRepository.findById(targetUserId)
                .map(u -> email.equals(u.getEmail()))
                .orElse(false);
    }

    /**
     * Get a single user work-week assignment by ID.
     *
     * @param id the assignment ID
     * @return the assignment, or 404
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserWorkWeekDAO> getById(@PathVariable Long id) {
        return userWorkWeekRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new work-week assignment for a user.
     * The request body must include a {@code workWeek} field with at least the work week's {@code id}.
     *
     * @param userId       the owning user ID
     * @param userWorkWeek the assignment to create
     * @return the saved assignment, or 404 when user/work week is not found
     */
    @PostMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserWorkWeekDAO> save(@PathVariable Long userId, @RequestBody UserWorkWeekDAO userWorkWeek) {
        if (!canModify(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own work week assignments");
        }
        return userRepository.findById(userId).map(user -> {
            userWorkWeek.setUser(user);
            // Re-attach the work week entity so JPA doesn't try to cascade it
            if (userWorkWeek.getWorkWeek() != null && userWorkWeek.getWorkWeek().getId() != null) {
                workWeekRepository.findById(userWorkWeek.getWorkWeek().getId())
                        .ifPresent(userWorkWeek::setWorkWeek);
            }
            UserWorkWeekDAO saved = userWorkWeekRepository.save(userWorkWeek);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing work-week assignment.
     *
     * @param userId       the owning user ID
     * @param userWorkWeek the updated assignment
     * @return 200 OK or 404
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Object> update(@PathVariable Long userId, @RequestBody UserWorkWeekDAO userWorkWeek) {
        if (!canModify(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own work week assignments");
        }
        return userRepository.findById(userId).map(user -> {
            userWorkWeek.setUser(user);
            if (userWorkWeek.getWorkWeek() != null && userWorkWeek.getWorkWeek().getId() != null) {
                workWeekRepository.findById(userWorkWeek.getWorkWeek().getId())
                        .ifPresent(userWorkWeek::setWorkWeek);
            }
            userWorkWeekRepository.save(userWorkWeek);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a work-week assignment.
     * The assignment with the earliest start date is considered the "first" and may not be deleted.
     *
     * @param userId the owning user ID
     * @param id     the assignment ID
     * @return 200 OK or 404
     */
    @DeleteMapping("/{userId}/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Object> delete(@PathVariable Long userId, @PathVariable Long id) {
        if (!canModify(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own work week assignments");
        }
        return userRepository.findById(userId).map(user -> {
            UserWorkWeekDAO uww = userWorkWeekRepository.findById(id).orElseThrow();
            // The assignment with the earliest start date cannot be deleted
            Optional<UserWorkWeekDAO> firstWorkWeek = user.getUserWorkWeeks().stream()
                    .min(Comparator.comparing(UserWorkWeekDAO::getStart));
            if (firstWorkWeek.isPresent() && Objects.equals(firstWorkWeek.get().getId(), id)) {
                throw new IllegalArgumentException("Cannot delete the first work week assignment");
            }
            user.getUserWorkWeeks().remove(uww);
            userRepository.save(user);
            userWorkWeekRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}

