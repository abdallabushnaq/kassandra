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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.location;

import java.util.UUID;
import de.bushnaq.abdalla.kassandra.dto.Location;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Filter-only DTO for {@link Location}.
 *
 * <p>Exposes only the fields the LLM needs to evaluate a filter predicate
 * ({@code country}, {@code state}, {@code start}, {@code created}, {@code updated}).
 * Using a dedicated DTO keeps the filter layer decoupled from the full entity
 * and makes every AI filter test look the same.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class LocationFilterDto {

    private String         country;
    private OffsetDateTime created;
    private UUID           id;
    private LocalDate      start;
    private String         state;
    private OffsetDateTime updated;

    /**
     * Creates a {@code LocationFilterDto} from a {@link Location} entity.
     */
    public static LocationFilterDto from(Location location) {
        LocationFilterDto dto = new LocationFilterDto();
        dto.id      = location.getId();
        dto.country = location.getCountry();
        dto.state   = location.getState();
        dto.start   = location.getStart();
        dto.created = location.getCreated();
        dto.updated = location.getUpdated();
        return dto;
    }
}

