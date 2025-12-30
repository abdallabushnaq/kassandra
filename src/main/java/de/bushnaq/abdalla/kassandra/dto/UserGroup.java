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

import java.util.HashSet;
import java.util.Set;

/**
 * DTO representing a user group for ACL management.
 * Uses memberIds instead of full User objects to avoid Jackson serialization issues
 * when the same user belongs to multiple groups.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class UserGroup extends AbstractTimeAware implements Comparable<UserGroup> {

    private String    description;
    private Long      id;
    private Set<Long> memberIds = new HashSet<>();
    private String    name;

    @Override
    public int compareTo(UserGroup other) {
        return this.id.compareTo(other.id);
    }

    @JsonIgnore
    public String getKey() {
        return "UG-" + id;
    }

    /**
     * Get the number of members in this group
     *
     * @return member count
     */
    @JsonIgnore
    public int getMemberCount() {
        return memberIds != null ? memberIds.size() : 0;
    }
}

