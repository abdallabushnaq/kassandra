/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.dto;

import de.bushnaq.abdalla.kassandra.dao.AbstractTimeAwareDAO;
import lombok.*;

import java.time.Duration;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class Worklog extends AbstractTimeAwareDAO implements Comparable<Worklog> {

    private Long           authorId;
    private String         comment;
    private Long           id;
    private Long           sprintId;
    private OffsetDateTime start;
    private Long           taskId;
    private Duration       timeSpent;
    private Long           updateAuthorId;

    @Override
    public int compareTo(Worklog other) {
        return this.id.compareTo(other.id);
    }
}
