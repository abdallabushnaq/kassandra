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

package de.bushnaq.abdalla.kassandra.dao;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.bushnaq.abdalla.kassandra.dto.Status;
import de.bushnaq.abdalla.util.DurationDeserializer;
import de.bushnaq.abdalla.util.DurationSerializer;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Proxy;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sprints",
        uniqueConstraints = @UniqueConstraint(columnNames = {"featureId", "name"})
)
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Proxy(lazy = false)
@Hidden
@Schema(hidden = true)
public class SprintDAO extends AbstractTimeAwareDAO {
    @Column(name = "end_date", nullable = true)  // renamed from 'end' as it is reserved in H2 databases
    private LocalDateTime end;
    @Column(nullable = false)
    private Long          featureId;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long          id;
    @Column(nullable = false)
    private String        name;
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    @Column(nullable = true)
    private Duration      originalEstimation = Duration.ZERO;
    private LocalDateTime releaseDate;//calculated from the task work, worklogs and remaining work
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    @Column(nullable = true)
    private Duration      remaining          = Duration.ZERO;
    @Column(name = "start_date", nullable = true)  // renamed from 'start'
    private LocalDateTime start;
    @Column(nullable = false)
    private Status        status;
    @Column(nullable = true)
    private Long          userId;
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    @Column(nullable = true)
    private Duration      worked             = Duration.ZERO;

}
