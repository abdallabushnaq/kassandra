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

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = true)
@JsonIdentityInfo(
        scope = OffDay.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class OffDay extends AbstractDateRange implements Comparable<OffDay> {

    private Long       id;
    private OffDayType type;

    @ToString.Exclude//help intellij debugger not to go into a loop
    @JsonBackReference
    private User user;

    public OffDay(LocalDate firstDay, LocalDate lastDay, OffDayType offDayType) {
        super();
        this.setFirstDay(firstDay);
        this.setLastDay(lastDay);
        this.type = offDayType;
    }

    @Override
    public int compareTo(OffDay other) {
        return this.id.compareTo(other.id);
    }

    String getKey() {
        return "D-" + id;
    }


}
