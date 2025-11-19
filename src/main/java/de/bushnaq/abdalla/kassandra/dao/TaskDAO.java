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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.bushnaq.abdalla.kassandra.dto.TaskMode;
import de.bushnaq.abdalla.util.DurationDeserializer;
import de.bushnaq.abdalla.util.DurationSerializer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Proxy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * represents a task in a Gantt chart.
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = false)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Proxy(lazy = false)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TaskDAO {
    @Column(nullable = false)
    private Boolean critical;

    @Column(nullable = true)
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration duration;

    @Column(nullable = true)
    private LocalDateTime finish;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long              id;
    @Column(nullable = false)
    private boolean           impactOnCost      = true;
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration          maxEstimate       = Duration.ZERO;
    @Column(nullable = false)
    private boolean           milestone;
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration          minEstimate       = Duration.ZERO;
    @Column(nullable = false)
    private String            name;
    @Column(nullable = false)
    private Integer           orderId           = -1;
    @Column(nullable = true)
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration          originalEstimate  = Duration.ZERO;
    @Column(nullable = true)
    private Long              parentTaskId;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", referencedColumnName = "id")
    private List<RelationDAO> predecessors      = new ArrayList<>();
    @Column(nullable = false)
    private Number            progress;
    @Column(nullable = true)
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration          remainingEstimate = Duration.ZERO;
    @Column(nullable = true)
    private Long              resourceId;
    @Column(nullable = false)
    private Long              sprintId;
    @Column(nullable = true)
    private LocalDateTime     start;
    @Column(nullable = false)
    private TaskMode          taskMode;
    @Column(nullable = true)
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration          timeSpent         = Duration.ZERO;

}
