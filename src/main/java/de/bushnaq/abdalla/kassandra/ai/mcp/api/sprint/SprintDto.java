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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Simplified Sprint DTO for AI tools. Excludes all fields marked @JsonIgnore in Sprint.java.
 */
@Data
@NoArgsConstructor
@JsonPropertyOrder({"sprintId", "name", "featureId", "userId", "start", "end", "releaseDate", "originalEstimation", "remaining", "worked", "status", "avatarHash"})
@Schema(description = "A sprint belonging to a feature")
public class SprintDto {
    @Schema(description = "Avatar hash for cache-busting")
    private String        avatarHash;
    @Schema(description = "Sprint end datetime (ISO 8601)")
    private LocalDateTime end;
    @Schema(description = "Feature this sprint belongs to")
    private Long          featureId;
    @Schema(description = "Unique sprint name")
    private String        name;
    @Schema(description = "Original effort estimation (ISO 8601 duration)")
    private Duration      originalEstimation;
    @Schema(description = "Calculated release date (ISO 8601)")
    private LocalDateTime releaseDate;
    @Schema(description = "Remaining effort (ISO 8601 duration)")
    private Duration      remaining;
    @Schema(description = "Unique sprint identifier; use this ID in subsequent operations")
    private Long          sprintId;
    @Schema(description = "Sprint start datetime (ISO 8601)")
    private LocalDateTime start;
    @Schema(description = "Sprint status")
    private Status        status;
    @Schema(description = "User assigned to this sprint")
    private Long          userId;
    @Schema(description = "Effort already worked (ISO 8601 duration)")
    private Duration      worked;

    public SprintDto(Long sprintId, String name, Long featureId, LocalDateTime start, LocalDateTime end, LocalDateTime releaseDate, Duration originalEstimation, Duration remaining, Duration worked, String avatarHash, Status status, Long userId) {
        this.sprintId           = sprintId;
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
