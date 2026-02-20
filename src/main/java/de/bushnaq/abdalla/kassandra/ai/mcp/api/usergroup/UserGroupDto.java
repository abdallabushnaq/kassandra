/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.usergroup;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.bushnaq.abdalla.kassandra.dto.UserGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Simplified UserGroup DTO for AI tools.
 * Contains only fields relevant for AI interactions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"groupId", "name", "description", "memberIds", "created", "updated"})
public class UserGroupDto {
    private OffsetDateTime created;
    private String         description;
    private Long           groupId;
    private Set<Long>      memberIds;
    private String         name;
    private OffsetDateTime updated;

    public static UserGroupDto from(UserGroup group) {
        if (group == null) {
            return null;
        }
        UserGroupDto dto = new UserGroupDto();
        dto.setGroupId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setMemberIds(group.getMemberIds());
        dto.setCreated(group.getCreated());
        dto.setUpdated(group.getUpdated());
        return dto;
    }

    public UserGroup toUserGroup() {
        UserGroup group = new UserGroup();
        group.setId(this.groupId);
        group.setName(this.name);
        group.setDescription(this.description);
        group.setMemberIds(this.memberIds);
        return group;
    }
}

