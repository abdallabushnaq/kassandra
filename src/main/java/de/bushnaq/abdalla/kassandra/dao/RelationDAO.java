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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "relations")
@Audited
@SQLDelete(sql = "UPDATE relations SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@BatchSize(size = 10)
public class RelationDAO {
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted;
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Id
//    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id")
    UUID id;

    @Column(nullable = false)
    UUID predecessorId;

    @Column(nullable = false)
    Boolean visible;

    public RelationDAO() {
        this.setId(UUID.randomUUID());
    }

}
