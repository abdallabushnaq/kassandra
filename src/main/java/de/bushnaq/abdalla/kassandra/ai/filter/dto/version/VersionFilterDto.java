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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.version;

import java.util.UUID;
import de.bushnaq.abdalla.kassandra.dto.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Filter-only DTO for {@link Version}.
 *
 * <p>Exposes only the fields the LLM needs to evaluate a filter predicate:
 * {@code name}, {@code productId}, {@code created}, {@code updated}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class VersionFilterDto {

    private OffsetDateTime created;
    private UUID           id;
    private String         name;
    private UUID           productId;
    private OffsetDateTime updated;

    /**
     * Creates a {@code VersionFilterDto} from a {@link Version} entity.
     */
    public static VersionFilterDto from(Version version) {
        VersionFilterDto dto = new VersionFilterDto();
        dto.id        = version.getId();
        dto.name      = version.getName();
        dto.productId = version.getProductId();
        dto.created   = version.getCreated();
        dto.updated   = version.getUpdated();
        return dto;
    }
}

