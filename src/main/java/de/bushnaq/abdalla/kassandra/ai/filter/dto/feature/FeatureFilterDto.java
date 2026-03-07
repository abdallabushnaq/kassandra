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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.feature;

import de.bushnaq.abdalla.kassandra.dto.Feature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Filter-only DTO for {@link Feature}.
 *
 * <p>Exposes only the fields the LLM needs to evaluate a filter predicate
 * ({@code name}, {@code versionId}, {@code created}, {@code updated}).
 * Using a dedicated DTO keeps the filter layer decoupled from the full entity
 * and makes every AI filter test look the same.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class FeatureFilterDto {

    private OffsetDateTime created;
    private Long           id;
    private String         name;
    private OffsetDateTime updated;
    private Long           versionId;

    /**
     * Creates a {@code FeatureFilterDto} from a {@link Feature} entity.
     */
    public static FeatureFilterDto from(Feature feature) {
        FeatureFilterDto dto = new FeatureFilterDto();
        dto.id        = feature.getId();
        dto.name      = feature.getName();
        dto.versionId = feature.getVersionId();
        dto.created   = feature.getCreated();
        dto.updated   = feature.getUpdated();
        return dto;
    }
}

