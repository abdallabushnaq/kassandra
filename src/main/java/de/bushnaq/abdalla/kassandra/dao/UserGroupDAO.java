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

package de.bushnaq.abdalla.kassandra.dao;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Proxy;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a user group for ACL management.
 * Groups can contain multiple users (stored as IDs) and be assigned access to products.
 * Uses @ElementCollection to store member user IDs in a join table without needing full User entities.
 */
@Entity
@Table(name = "user_groups")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Proxy(lazy = false)
public class UserGroupDAO extends AbstractTimeAwareDAO {

    @Column(length = 500)
    private String description;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long   id;

    /**
     * Member user IDs stored in a join table.
     * This is more efficient than maintaining full User entity references.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_group_members",
            joinColumns = @JoinColumn(name = "group_id")
    )
    @Column(name = "user_id")
    private Set<Long> memberIds = new HashSet<>();

    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Add a user to this group by ID
     *
     * @param userId the user ID to add
     */
    public void addMember(Long userId) {
        memberIds.add(userId);
    }

    /**
     * Get the number of members in this group
     *
     * @return member count
     */
    public int getMemberCount() {
        return memberIds != null ? memberIds.size() : 0;
    }

    /**
     * Remove a user from this group by ID
     *
     * @param userId the user ID to remove
     */
    public void removeMember(Long userId) {
        memberIds.remove(userId);
    }
}

