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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Simplified UserGroup DTO for AI tools.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"groupId", "name", "description", "memberIds", "created", "updated"})
@Schema(description = "A user group used for product access control")
public class UserGroupDto {
    @Schema(description = "Timestamp when the group was created (ISO 8601)")
    private OffsetDateTime created;
    @Schema(description = "Human-readable description of the group", nullable = true)
    private String         description;
    @Schema(description = "Unique group identifier; use this ID in subsequent operations")
    private Long           groupId;
    @Schema(description = "Set of user IDs that are members of this group")
    private Set<Long>      memberIds;
    @Schema(description = "Unique group name")
    private String         name;
    @Schema(description = "Timestamp when the group was last updated (ISO 8601)")
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
