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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.version;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.bushnaq.abdalla.kassandra.dto.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Simplified Version DTO for AI tools.
 * Contains only fields relevant for AI interactions, excluding internal fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "name", "productId", "created", "updated"})
public class VersionDto {
    private OffsetDateTime created;
    private Long           id;
    private String         name;
    private Long           productId;
    private OffsetDateTime updated;

    /**
     * Custom constructor for VersionDto with explicit parameter order.
     */
    public VersionDto(Long id, String name, Long productId, OffsetDateTime created, OffsetDateTime updated) {
        this.id        = id;
        this.name      = name;
        this.productId = productId;
        this.created   = created;
        this.updated   = updated;
    }

    public static VersionDto from(Version version) {
        if (version == null) {
            return null;
        }
        return new VersionDto(
                version.getId(),
                version.getName(),
                version.getProductId(),
                version.getCreated(),
                version.getUpdated()
        );
    }

    public Version toVersion() {
        Version version = new Version();
        version.setId(this.id);
        version.setName(this.name);
        version.setCreated(this.created);
        version.setUpdated(this.updated);
        version.setProductId(this.productId);
        return version;
    }
}
