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
import lombok.*;

import java.time.LocalDate;

/**
 * {@code LegalLocation} class
 * This class stores the work contract working location that will be used to determine the official public holidays.
 * A user might be working in Melbourne Australia and wil get paid non-working days as everybody else in Melbourne,
 * next year the same user might be working in Germany and will receive paid non-working days like everybody else in Germany.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class Location extends AbstractTimeAware implements Comparable<Location> {

    private String    country;
    private Long      id;
    private LocalDate start;
    private String    state;

    @ToString.Exclude//help intellij debugger not to go into a loop
    @JsonBackReference
    private User user;

    public Location(String country, String state, LocalDate start) {
        this.country = country;
        this.state   = state;
        this.setStart(start);
    }

    @Override
    public int compareTo(Location other) {
        return this.id.compareTo(other.id);
    }

    String getKey() {
        return "L-" + id;
    }


}
