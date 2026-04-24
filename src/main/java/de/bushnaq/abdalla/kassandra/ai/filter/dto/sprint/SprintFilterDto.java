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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.sprint;

import java.util.UUID;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Filter-only DTO for {@link Sprint}.
 *
 * <p>Design decisions that eliminate known LLM generation pitfalls:</p>
 * <ul>
 *   <li>{@code status} is a plain {@link String} (e.g. {@code "STARTED"}) so the LLM
 *       can compare with {@code entity.getStatus() === 'STARTED'} without a
 *       {@code Java.type()} enum lookup.</li>
 *   <li>{@code originalEstimationHours}, {@code workedHours}, {@code remainingHours} are
 *       plain {@code long} values so the LLM can write
 *       {@code entity.getOriginalEstimationHours() > 100} without calling
 *       {@code .toHours()} on a {@link java.time.Duration}.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class SprintFilterDto {

    private OffsetDateTime created;
    private LocalDateTime  end;
    private UUID           featureId;
    private UUID           id;
    private String         name;
    /**
     * Original estimation in whole hours.
     */
    private long           originalEstimationHours;
    private LocalDateTime  releaseDate;
    /**
     * Remaining hours.
     */
    private long           remainingHours;
    private LocalDateTime  start;
    /**
     * Enum name of the sprint status: CREATED, STARTED, or CLOSED.
     */
    private String         status;
    private OffsetDateTime updated;
    private UUID           userId;
    /**
     * Hours already worked.
     */
    private long           workedHours;

    /**
     * Creates a {@code SprintFilterDto} from a {@link Sprint} entity.
     */
    public static SprintFilterDto from(Sprint sprint) {
        SprintFilterDto dto = new SprintFilterDto();
        dto.id                      = sprint.getId();
        dto.name                    = sprint.getName();
        dto.status                  = sprint.getStatus() != null ? sprint.getStatus().name() : null;
        dto.featureId               = sprint.getFeatureId();
        dto.userId                  = sprint.getUserId();
        dto.start                   = sprint.getStart();
        dto.end                     = sprint.getEnd();
        dto.releaseDate             = sprint.getReleaseDate();
        dto.originalEstimationHours = sprint.getOriginalEstimation() != null ? sprint.getOriginalEstimation().toHours() : 0L;
        dto.workedHours             = sprint.getWorked() != null ? sprint.getWorked().toHours() : 0L;
        dto.remainingHours          = sprint.getRemaining() != null ? sprint.getRemaining().toHours() : 0L;
        dto.created                 = sprint.getCreated();
        dto.updated                 = sprint.getUpdated();
        return dto;
    }
}

