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

package de.bushnaq.abdalla.kassandra.repository;

import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends ListCrudRepository<UserDAO, Long> {

    /**
     * Check if a user with the given email exists, excluding the user with the specified ID.
     *
     * @param email The email to check
     * @param id    The ID of the user to exclude from the check
     * @return true if another user with this email exists, false otherwise
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Check if a user with the given name exists, excluding the user with the specified ID.
     *
     * @param name The name to check
     * @param id   The ID of the user to exclude from the check
     * @return true if another user with this name exists, false otherwise
     */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * Find user by email address, ignoring case sensitivity.
     *
     * @param email The email address to search for
     * @return An Optional containing the user if found, or empty if not found
     */
    @Query("SELECT u FROM UserDAO u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<UserDAO> findByEmail(@Param("email") String email);

    Optional<UserDAO> findByName(String name);

    /**
     * Find users whose names contain the given string, ignoring case sensitivity.
     *
     * @param partialName The partial name to search for in user names
     * @return A list of users whose names contain the specified string (case-insensitive)
     */
    @Query("SELECT u FROM UserDAO u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :partialName, '%'))")
    List<UserDAO> findByNameContainingIgnoreCase(@Param("partialName") String partialName);

    @Query("SELECT DISTINCT u FROM UserDAO u WHERE u.id IN " +
            "(SELECT t.resourceId FROM TaskDAO t WHERE t.sprintId = :sprintId AND t.resourceId IS NOT NULL)")
    List<UserDAO> findBySprintId(@Param("sprintId") Long sprintId);
}