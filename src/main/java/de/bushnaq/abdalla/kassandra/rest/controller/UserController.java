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

import com.fasterxml.jackson.core.JsonProcessingException;
import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.repository.LocationRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import de.bushnaq.abdalla.kassandra.rest.debug.DebugUtil;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    DebugUtil debugUtil;

    @Autowired
    EntityManager entityManager;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        userRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserDAO> get(@PathVariable Long id) throws JsonProcessingException {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sprint/{sprintId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserDAO> getAll(@PathVariable Long sprintId) throws JsonProcessingException {
        return userRepository.findBySprintId(sprintId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserDAO> getAll() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}/avatar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarWrapper> getAvatar(@PathVariable Long id) {
        Optional<UserDAO> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            UserDAO user = userOpt.get();
            if (user.getAvatarImage() == null || user.getAvatarImage().length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(new AvatarWrapper(user.getAvatarImage()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}/avatar/full")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<AvatarUpdateRequest> getAvatarFull(@PathVariable Long id) {
        Optional<UserDAO> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            UserDAO             user     = userOpt.get();
            AvatarUpdateRequest response = new AvatarUpdateRequest();
            // Set resized avatar image (may be null)
            response.setAvatarImage(user.getAvatarImage());
            // Set original avatar image as byte[] if present
            response.setAvatarImageOriginal(user.getAvatarImageOriginal());
            // Set prompt
            response.setAvatarPrompt(user.getAvatarPrompt());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserDAO> getByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserDAO> getByName(@PathVariable String name) {
        return userRepository.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDAO save(@RequestBody UserDAO user) {
        return userRepository.save(user);
    }

    /**
     * Search for users by partial name, ignoring case sensitivity.
     *
     * @param partialName The partial name to search for
     * @return A list of users whose names contain the specified partial name (case-insensitive)
     */
    @GetMapping("/search/{partialName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserDAO> searchByNameContaining(@PathVariable String partialName) {
        return userRepository.findByNameContainingIgnoreCase(partialName);
    }

    @PutMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public void update(@RequestBody UserDAO user) {
        userRepository.save(user);
    }

    @PutMapping("/{id}/avatar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateAvatar(@PathVariable Long id, @RequestBody byte[] avatarData) {
        Optional<UserDAO> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            UserDAO user = userOpt.get();
            user.setAvatarImage(avatarData);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}/avatar/full")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateAvatarFull(@PathVariable Long id, @RequestBody AvatarUpdateRequest request) {

        Optional<UserDAO> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            UserDAO user = userOpt.get();

            // Decode and set resized avatar if provided
            if (request.getAvatarImage() != null && request.getAvatarImage().length != 0) {
                user.setAvatarImage(request.getAvatarImage());
            }

            // Decode and set original avatar if provided
            if (request.getAvatarImageOriginal() != null && request.getAvatarImageOriginal().length != 0) {
                user.setAvatarImageOriginal(request.getAvatarImageOriginal());
            }

            // Set prompt if provided
            if (request.getAvatarPrompt() != null) {
                user.setAvatarPrompt(request.getAvatarPrompt());
            }

            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log.error("UserController.updateAvatarFull: Updating avatar for userId {}", id);
            log.error("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            userRepository.save(user);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
