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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.user;

import java.util.UUID;
import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Filter-only DTO for {@link User}.
 *
 * <p>Exposes the fields the LLM needs to evaluate a filter predicate:
 * {@code name}, {@code email}, {@code firstWorkingDay}, {@code lastWorkingDay}
 * (nullable — {@code null} means still employed), {@code now} (injected current
 * date for tenure calculations), {@code created}, {@code updated}.</p>
 *
 * <p>{@link java.awt.Color} is intentionally omitted — it is not meaningful
 * for any text- or date-based filter query.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class UserFilterDto {

    private OffsetDateTime created;
    private String         email;
    private LocalDate      firstWorkingDay;
    private UUID           id;
    /**
     * {@code null} means the user is still employed.
     */
    private LocalDate      lastWorkingDay;
    private String         name;
    /**
     * Current date injected at filter-evaluation time; used for tenure
     * calculations such as {@code "employed for more than 3 years"}.
     */
    private LocalDate      now;
    private OffsetDateTime updated;

    /**
     * Creates a {@code UserFilterDto} from a {@link User} entity.
     * {@code now} is left {@code null} here and must be set by the caller
     * (or injected by the filter engine).
     */
    public static UserFilterDto from(User user) {
        UserFilterDto dto = new UserFilterDto();
        dto.id              = user.getId();
        dto.name            = user.getName();
        dto.email           = user.getEmail();
        dto.firstWorkingDay = user.getFirstWorkingDay();
        dto.lastWorkingDay  = user.getLastWorkingDay();
        dto.created         = user.getCreated();
        dto.updated         = user.getUpdated();
        return dto;
    }
}

