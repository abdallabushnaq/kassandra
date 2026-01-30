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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.sprint;

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Status;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Simplified Sprint DTO for AI tools. Excludes all fields marked @JsonIgnore in Sprint.java.
 */
@Data
@NoArgsConstructor
public class SprintDto {
    private String        avatarHash;
    private LocalDateTime end;
    private Long          featureId;
    private Long          id;
    private String        name;
    private Duration      originalEstimation;
    private LocalDateTime releaseDate;
    private Duration      remaining;
    private LocalDateTime start;
    private Status        status;
    private Long          userId;
    private Duration      worked;

    public SprintDto(Long id, String name, Long featureId, LocalDateTime start, LocalDateTime end, LocalDateTime releaseDate, Duration originalEstimation, Duration remaining, Duration worked, String avatarHash, Status status, Long userId) {
        this.id                 = id;
        this.name               = name;
        this.featureId          = featureId;
        this.start              = start;
        this.end                = end;
        this.releaseDate        = releaseDate;
        this.originalEstimation = originalEstimation;
        this.remaining          = remaining;
        this.worked             = worked;
        this.avatarHash         = avatarHash;
        this.status             = status;
        this.userId             = userId;
    }

    public static SprintDto from(Sprint sprint) {
        if (sprint == null) return null;
        return new SprintDto(
                sprint.getId(),
                sprint.getName(),
                sprint.getFeatureId(),
                sprint.getStart(),
                sprint.getEnd(),
                sprint.getReleaseDate(),
                sprint.getOriginalEstimation(),
                sprint.getRemaining(),
                sprint.getWorked(),
                sprint.getAvatarHash(),
                sprint.getStatus(),
                sprint.getUserId()
        );
    }
}
