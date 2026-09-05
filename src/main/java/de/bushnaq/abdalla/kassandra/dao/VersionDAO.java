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

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "versions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"productId", "name"})
)
@Audited
@SQLDelete(sql = "UPDATE versions SET deleted = true, deleted_at = CURRENT_TIMESTAMP, name = CONCAT(name, ' [deleted] ', id) WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@BatchSize(size = 10)
public class VersionDAO extends AbstractTimeAwareDAO {
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted;
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Id
//    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID productId;

    public VersionDAO() {
        this.setId(UUID.randomUUID());
    }

}
