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
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "products")
@Audited
@SQLDelete(sql = "UPDATE products SET deleted = true, deleted_at = CURRENT_TIMESTAMP, name = CONCAT(name, ' [deleted] ', id) WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@BatchSize(size = 10)
public class ProductDAO extends AbstractTimeAwareDAO {
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted;
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "dark_avatar_hash", length = 16)
    private String darkAvatarHash;
    @Column(name = "dark_header_hash", length = 16)
    private String darkHeaderHash;
    @Id
//    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id")
    private UUID   id;
    @Column(name = "light_avatar_hash", length = 16)
    private String lightAvatarHash;
    @Column(name = "light_header_hash", length = 16)
    private String lightHeaderHash;
    @Column(nullable = false, unique = true)
    private String name;

    public ProductDAO() {
        this.setId(UUID.randomUUID());
    }

}
