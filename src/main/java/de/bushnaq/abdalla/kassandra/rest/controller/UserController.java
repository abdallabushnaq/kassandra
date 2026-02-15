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

import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dao.UserAvatarDAO;
import de.bushnaq.abdalla.kassandra.dao.UserAvatarGenerationDataDAO;
import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.repository.*;
import de.bushnaq.abdalla.kassandra.rest.debug.DebugUtil;
import de.bushnaq.abdalla.kassandra.rest.exception.UniqueConstraintViolationException;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.service.UserRoleService;
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
    private UserGroupRepository                userGroupRepository;
    @Autowired
    private UserRepository                     userRepository;
    @Autowired
    private UserRoleService                    userRoleService;

    /**
     * Check if the current user can modify the target user.
     * Admins can modify any user, regular users can only modify themselves.
     *
     * @param targetUserEmail the email of the user being modified
     * @return true if modification is allowed, false otherwise
     */
    private boolean canUserModifyUser(String targetUserEmail) {
        // Admins can modify any user
        if (SecurityUtils.isAdmin()) {
            return true;
        }

        // Regular users can only modify themselves
        String currentUserEmail = SecurityUtils.getUserEmail();
        return currentUserEmail.equals(targetUserEmail);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable Long id) {
        // Delete avatars first (cascade delete)
        userAvatarRepository.deleteByUserId(id);
        userAvatarGenerationDataRepository.deleteByUserId(id);
        // Then delete user
        userRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserDAO> get(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sprint/{sprintId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserDAO> getAll(@PathVariable Long sprintId) {
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

    /**
     * Get roles for a specific user
     *
     * @param id the user ID
     * @return list of roles
     */
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<String> getRoles(@PathVariable Long id) {
        return userRoleService.getRoles(id);
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserDAO save(@RequestBody UserDAO user) {
        // Check name uniqueness
        if (userRepository.findByName(user.getName()).isPresent()) {
            throw new UniqueConstraintViolationException("User", "name", user.getName());
        }

        // Check email uniqueness
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new UniqueConstraintViolationException("User", "email", user.getEmail());
        }

        UserDAO savedUser = userRepository.save(user);

        // Automatically add user to "All" group
        userGroupRepository.findByName(DefaultEntitiesInitializer.ALL_USERS_GROUP_NAME)
                .ifPresent(allGroup -> {
                    allGroup.addMember(savedUser.getId());
                    userGroupRepository.save(allGroup);
                    log.info("Added user {} to 'All' group", savedUser.getName());
                });

        return savedUser;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void update(@RequestBody UserDAO user) {
        // Check authorization: Users can only update their own profile, admins can update any
        if (!canUserModifyUser(user.getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own profile");
        }

        // Check name uniqueness (excluding current user)
        if (userRepository.existsByNameAndIdNot(user.getName(), user.getId())) {
            throw new UniqueConstraintViolationException("User", "name", user.getName());
        }

        // Check email uniqueness (excluding current user)
        if (userRepository.existsByEmailAndIdNot(user.getEmail(), user.getId())) {
            throw new UniqueConstraintViolationException("User", "email", user.getEmail());
        }

        userRepository.save(user);
    }

    @PutMapping("/{id}/avatar/full")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Transactional
    public ResponseEntity<Void> updateAvatarFull(@PathVariable Long id, @RequestBody AvatarUpdateRequest request) {
        // Verify user exists
        Optional<UserDAO> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Check authorization: Users can only update their own avatar, admins can update any
        UserDAO user = userOpt.get();
        if (!canUserModifyUser(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

    /**
     * Update roles for a specific user
     *
     * @param id    the user ID
     * @param roles list of roles to assign
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateRoles(@PathVariable Long id, @RequestBody List<String> roles) {
        try {
            userRoleService.assignRoles(id, roles);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid role update request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.error("Cannot update roles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
