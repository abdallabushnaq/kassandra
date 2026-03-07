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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.offday;

import de.bushnaq.abdalla.kassandra.dto.OffDay;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Filter-only DTO for {@link OffDay}.
 *
 * <p>Exposes only the fields the LLM needs to evaluate a filter predicate
 * ({@code type}, {@code firstDay}, {@code lastDay}, {@code created}, {@code updated}).
 * The {@code type} is stored as a {@link String} (the enum name, e.g. {@code "VACATION"})
 * so the LLM can compare it with a plain string literal without needing a
 * {@code Java.type()} lookup for the enum class.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class OffDayFilterDto {

    private OffsetDateTime created;
    private LocalDate      firstDay;
    private Long           id;
    private LocalDate      lastDay;
    /**
     * Enum name of the off-day type: VACATION, SICK, TRIP, or HOLIDAY.
     */
    private String         type;
    private OffsetDateTime updated;

    /**
     * Creates an {@code OffDayFilterDto} from an {@link OffDay} entity.
     */
    public static OffDayFilterDto from(OffDay offDay) {
        OffDayFilterDto dto = new OffDayFilterDto();
        dto.id       = offDay.getId();
        dto.type     = offDay.getType() != null ? offDay.getType().name() : null;
        dto.firstDay = offDay.getFirstDay();
        dto.lastDay  = offDay.getLastDay();
        dto.created  = offDay.getCreated();
        dto.updated  = offDay.getUpdated();
        return dto;
    }
}

