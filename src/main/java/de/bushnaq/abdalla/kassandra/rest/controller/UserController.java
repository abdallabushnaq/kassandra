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
import de.bushnaq.abdalla.kassandra.dao.UserAvatarDAO;
import de.bushnaq.abdalla.kassandra.dao.UserAvatarGenerationDataDAO;
import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.repository.LocationRepository;
import de.bushnaq.abdalla.kassandra.repository.UserAvatarGenerationDataRepository;
import de.bushnaq.abdalla.kassandra.repository.UserAvatarRepository;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import de.bushnaq.abdalla.kassandra.rest.debug.DebugUtil;
import jakarta.persistence.EntityManager;
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
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    DebugUtil debugUtil;

    @Autowired
    EntityManager entityManager;

    @Autowired
    private LocationRepository                 locationRepository;
    @Autowired
    private UserAvatarGenerationDataRepository userAvatarGenerationDataRepository;
    @Autowired
    private UserAvatarRepository               userAvatarRepository;
    @Autowired
    private UserRepository                     userRepository;

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
        return userAvatarRepository.findByUserId(id)
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
        userAvatarRepository.findByUserId(id)
                .ifPresent(avatar -> response.setAvatarImage(avatar.getAvatarImage()));

        // Get generation data
        userAvatarGenerationDataRepository.findByUserId(id)
                .ifPresent(genData -> {
                    response.setAvatarImageOriginal(genData.getAvatarImageOriginal());
                    response.setAvatarPrompt(genData.getAvatarPrompt());
                });

        return ResponseEntity.ok(response);
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


    @PutMapping("/{id}/avatar/full")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> updateAvatarFull(@PathVariable Long id, @RequestBody AvatarUpdateRequest request) {
        // Verify user exists
        Optional<UserDAO> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Update or create avatar image
        if (request.getAvatarImage() != null && request.getAvatarImage().length != 0) {
            UserAvatarDAO avatar = userAvatarRepository.findByUserId(id)
                    .orElse(new UserAvatarDAO());
            avatar.setUserId(id);
            avatar.setAvatarImage(request.getAvatarImage());
            userAvatarRepository.save(avatar);
        }

        // Update or create generation data
        if (request.getAvatarImageOriginal() != null || request.getAvatarPrompt() != null) {
            UserAvatarGenerationDataDAO genData = userAvatarGenerationDataRepository.findByUserId(id)
                    .orElse(new UserAvatarGenerationDataDAO());
            genData.setUserId(id);

            if (request.getAvatarImageOriginal() != null && request.getAvatarImageOriginal().length != 0) {
                genData.setAvatarImageOriginal(request.getAvatarImageOriginal());
            }

            if (request.getAvatarPrompt() != null) {
                genData.setAvatarPrompt(request.getAvatarPrompt());
            }

            userAvatarGenerationDataRepository.save(genData);
        }


        log.info("UserController.updateAvatarFull: Updated avatar for userId {}", id);
        return ResponseEntity.ok().build();
    }
}
