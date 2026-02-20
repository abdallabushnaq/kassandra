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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.product;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.bushnaq.abdalla.kassandra.dto.ProductAclEntry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Simplified ProductAclEntry DTO for AI tools.
 * Contains only fields relevant for AI interactions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "productId", "userId", "groupId", "type", "displayName", "created", "updated"})
public class ProductAclDto {
    private OffsetDateTime               created;
    private String                       displayName;
    private Long                         groupId;
    private Long                         id;
    private Long                         productId;
    private ProductAclEntry.AclEntryType type;
    private OffsetDateTime               updated;
    private Long                         userId;

    public static ProductAclDto from(ProductAclEntry entry) {
        if (entry == null) {
            return null;
        }
        ProductAclDto dto = new ProductAclDto();
        dto.setId(entry.getId());
        dto.setProductId(entry.getProductId());
        dto.setUserId(entry.getUserId());
        dto.setGroupId(entry.getGroupId());
        dto.setType(entry.getType());
        dto.setDisplayName(entry.getDisplayName());
        dto.setCreated(entry.getCreated());
        dto.setUpdated(entry.getUpdated());
        return dto;
    }
}

