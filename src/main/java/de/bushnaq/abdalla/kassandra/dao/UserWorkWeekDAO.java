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

package de.bushnaq.abdalla.kassandra.dao;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;

/**
 * Assignment of a global {@link WorkWeekDAO} to a {@link UserDAO} starting on a specific date.
 * Multiple assignments per user are allowed; each one becomes effective from its {@code start} date.
 */
@Entity
@Table(name = "user_work_weeks")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@BatchSize(size = 10)
public class UserWorkWeekDAO extends AbstractTimeAwareDAO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** The date from which this work-week assignment becomes effective. */
    @Column(nullable = false)
    private LocalDate start;

    /** The work week definition assigned to the user. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "work_week_id", nullable = false)
    private WorkWeekDAO workWeek;

    /** The user who owns this assignment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude // avoid debugger loop
    @JsonBackReference
    private UserDAO user;
}

