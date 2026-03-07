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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.availability;

import de.bushnaq.abdalla.kassandra.dto.Availability;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Filter-only DTO for {@link Availability} that exposes availability as an integer
 * percentage (0–100) instead of a float (0.0–1.0).
 *
 * <p>Using an integer eliminates floating-point precision issues when the LLM generates
 * equality comparisons such as {@code entity.getAvailabilityPercent() === 80}.</p>
 *
 * <p>This class is used exclusively during the JS-filter step.  The original
 * {@link Availability} objects are always the authoritative data; this DTO is only
 * a view of them for the purpose of AI-generated predicate evaluation.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AvailabilityFilterDto {

    /**
     * Availability expressed as a whole-number percentage, e.g. 80 means 80 %.
     */
    private int            availabilityPercent;
    private OffsetDateTime created;
    private Long           id;
    private LocalDate      start;
    private OffsetDateTime updated;

    /**
     * Creates an {@code AvailabilityFilterDto} from an {@link Availability} entity.
     * The float value is rounded to the nearest integer percentage.
     */
    public static AvailabilityFilterDto from(Availability availability) {
        AvailabilityFilterDto dto = new AvailabilityFilterDto();
        dto.id                  = availability.getId();
        dto.availabilityPercent = Math.round(availability.getAvailability() * 100);
        dto.start               = availability.getStart();
        dto.created             = availability.getCreated();
        dto.updated             = availability.getUpdated();
        return dto;
    }
}

