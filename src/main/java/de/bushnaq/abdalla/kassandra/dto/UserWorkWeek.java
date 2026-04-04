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

package de.bushnaq.abdalla.kassandra.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;

import java.time.LocalDate;

/**
 * Assignment of a global {@link WorkWeek} to a {@link User} starting on a specific date.
 * Multiple assignments per user are allowed; each becomes effective from its {@code start} date.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class UserWorkWeek extends AbstractTimeAware implements Comparable<UserWorkWeek> {

    /** Primary key. */
    private Long      id;

    /** Date from which this work-week assignment becomes effective. */
    private LocalDate start;

    /** The work week definition assigned to the user. */
    private WorkWeek  workWeek;

    /** The owning user. */
    @ToString.Exclude // avoid debugger loop
    @JsonBackReference
    private User      user;

    /**
     * Convenience constructor.
     *
     * @param workWeek the work week to assign
     * @param start    effective start date
     */
    public UserWorkWeek(WorkWeek workWeek, LocalDate start) {
        this.workWeek = workWeek;
        this.start    = start;
    }

    @Override
    public int compareTo(UserWorkWeek other) {
        if (this.id == null || other.id == null) return 0;
        return this.id.compareTo(other.id);
    }
}

