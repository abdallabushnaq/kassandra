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

package de.bushnaq.abdalla.kassandra.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

/**
 * DTO representing an Access Control List entry for a product.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class ProductAclEntry extends AbstractTimeAware implements Comparable<ProductAclEntry> {

    // For UI display
    private String       displayName;
    private Long groupId;
    private Long id;
    private Long productId;
    private AclEntryType type;
    private Long userId;

    @Override
    public int compareTo(ProductAclEntry other) {
        return this.id.compareTo(other.id);
    }

    @JsonIgnore
    public String getKey() {
        return "ACL-" + id;
    }

    /**
     * Check if this entry is for a group
     *
     * @return true if this entry grants access to a group
     */
    @JsonIgnore
    public boolean isGroupEntry() {
        return groupId != null;
    }

    /**
     * Check if this entry is for a user
     *
     * @return true if this entry grants access to a user
     */
    @JsonIgnore
    public boolean isUserEntry() {
        return userId != null;
    }

    public enum AclEntryType {
        USER, GROUP
    }
}

