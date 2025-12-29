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

import de.bushnaq.abdalla.kassandra.dao.UserGroupDAO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing UserGroup entities
 */
public interface UserGroupRepository extends ListCrudRepository<UserGroupDAO, Long> {

    /**
     * Count the number of members in a group
     *
     * @param groupId the group ID
     * @return number of members
     */
    @Query("SELECT COUNT(m) FROM UserGroupDAO g JOIN g.members m WHERE g.id = :groupId")
    long countMembersByGroupId(@Param("groupId") Long groupId);

    /**
     * Check if a group with the given name exists
     *
     * @param name the group name to check
     * @return true if a group with this name exists
     */
    boolean existsByName(String name);

    /**
     * Check if a group with the given name exists, excluding the group with the specified ID
     *
     * @param name the group name to check
     * @param id   the ID of the group to exclude from the check
     * @return true if another group with this name exists
     */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * Find a group by its name
     *
     * @param name the group name
     * @return the group, if found
     */
    Optional<UserGroupDAO> findByName(String name);

    /**
     * Find all groups that a user belongs to
     *
     * @param userId the user ID
     * @return list of groups containing this user
     */
    @Query("SELECT g FROM UserGroupDAO g JOIN g.members m WHERE m.id = :userId")
    List<UserGroupDAO> findGroupsByUserId(@Param("userId") Long userId);
}

