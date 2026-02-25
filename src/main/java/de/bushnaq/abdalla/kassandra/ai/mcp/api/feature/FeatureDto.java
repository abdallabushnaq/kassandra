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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Simplified Feature DTO for AI tools.
 */
@Data
@NoArgsConstructor
@JsonPropertyOrder({"featureId", "name", "versionId", "created", "updated", "avatarPrompt"})
@Schema(description = "A feature belonging to a version")
public class FeatureDto {
    @Schema(description = "Stable-diffusion prompt used to generate the feature avatar")
    private String         avatarPrompt;
    @Schema(description = "Timestamp when the feature was created (ISO 8601)")
    private OffsetDateTime created;
    @Schema(description = "Unique feature identifier; use this ID in subsequent operations")
    private Long           featureId;
    @Schema(description = "Unique feature name")
    private String         name;
    @Schema(description = "Timestamp when the feature was last updated (ISO 8601)")
    private OffsetDateTime updated;
    @Schema(description = "Version this feature belongs to")
    private Long           versionId;

    /**
     * Custom constructor for FeatureDto with explicit parameter order.
     */
    public FeatureDto(Long featureId, String name, Long versionId, OffsetDateTime created, OffsetDateTime updated, String avatarPrompt) {
        this.featureId    = featureId;
        this.name         = name;
        this.versionId    = versionId;
        this.created      = created;
        this.updated      = updated;
        this.avatarPrompt = avatarPrompt;
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
                feature.getUpdated(),
                feature.getDefaultAvatarPrompt()
        );
    }

    public Feature toFeature() {
        Feature feature = new Feature();
        feature.setId(this.featureId);
        feature.setName(this.name);
        feature.setVersionId(this.versionId);
        feature.setCreated(this.created);
        feature.setUpdated(this.updated);
        return feature;
    }
}
