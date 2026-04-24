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

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Supports client side id generation.
 */
@Entity
@Table(name = "off_days")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class OffDayDAO extends AbstractDateRangeDAO {

    @Id
    @Column(name = "id")
    private UUID       id;
    @Column(nullable = false)
    private OffDayType type;
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude//help intellij debugger not to go into a loop
    @JsonBackReference
    private UserDAO    user;

    public OffDayDAO() {
        this.setId(UUID.randomUUID());
    }
}
