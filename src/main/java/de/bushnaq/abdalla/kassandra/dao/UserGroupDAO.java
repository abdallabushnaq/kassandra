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
 * Groups can contain multiple users and be assigned access to products.
 */
@Entity
@Table(name = "user_groups")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"members"})
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Proxy(lazy = false)
public class UserGroupDAO extends AbstractTimeAwareDAO {

    @Column(length = 500)
    private String description;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserDAO> members = new HashSet<>();
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Add a user to this group
     *
     * @param user the user to add
     */
    public void addMember(UserDAO user) {
        members.add(user);
    }

    /**
     * Get the number of members in this group
     *
     * @return member count
     */
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }

    /**
     * Remove a user from this group
     *
     * @param user the user to remove
     */
    public void removeMember(UserDAO user) {
        members.remove(user);
    }
}

