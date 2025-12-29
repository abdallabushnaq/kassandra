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

/**
 * Entity representing an Access Control List entry for a product.
 * Each entry grants access to either a user or a group (but not both).
 */
@Entity
@Table(
        name = "product_acl_entries",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"product_id", "user_id"}),
                @UniqueConstraint(columnNames = {"product_id", "group_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Proxy(lazy = false)
public class ProductAclEntryDAO extends AbstractTimeAwareDAO {

    @Column(name = "group_id")
    private Long groupId;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "user_id")
    private Long userId;

    /**
     * Check if this entry is for a group
     *
     * @return true if this entry grants access to a group
     */
    public boolean isGroupEntry() {
        return groupId != null;
    }

    /**
     * Check if this entry is for a user
     *
     * @return true if this entry grants access to a user
     */
    public boolean isUserEntry() {
        return userId != null;
    }

    /**
     * Validate that either userId or groupId is set, but not both
     */
    @PrePersist
    @PreUpdate
    private void validateEntry() {
        if ((userId == null && groupId == null) || (userId != null && groupId != null)) {
            throw new IllegalStateException(
                    "ACL entry must have either userId or groupId, but not both"
            );
        }
    }
}

