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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.feature;

import de.bushnaq.abdalla.kassandra.dto.Feature;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Simplified Feature DTO for AI tools.
 */
@Data
@NoArgsConstructor
public class FeatureDto {
    private OffsetDateTime created;
    private Long           id;
    private String         name;
    private OffsetDateTime updated;
    private Long           versionId;

    /**
     * Custom constructor for FeatureDto with explicit parameter order.
     */
    public FeatureDto(Long id, String name, Long versionId, OffsetDateTime created, OffsetDateTime updated) {
        this.id        = id;
        this.name      = name;
        this.versionId = versionId;
        this.created   = created;
        this.updated   = updated;
    }

    public static FeatureDto from(Feature feature) {
        if (feature == null) {
            return null;
        }
        return new FeatureDto(
                feature.getId(),
                feature.getName(),
                feature.getVersionId(),
                feature.getCreated(),
                feature.getUpdated()
        );
    }

    public Feature toFeature() {
        Feature feature = new Feature();
        feature.setId(this.id);
        feature.setName(this.name);
        feature.setVersionId(this.versionId);
        feature.setCreated(this.created);
        feature.setUpdated(this.updated);
        return feature;
    }
}
