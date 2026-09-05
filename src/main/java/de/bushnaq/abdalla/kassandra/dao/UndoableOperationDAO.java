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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.dao;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single user-visible, product-scoped planning change that can be undone or redone.
 */
@Entity
@Table(
        name = "undoable_operations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"productId", "sequenceNumber"})
)
@Audited
@Getter
@Setter
@ToString(exclude = "entries")
@EqualsAndHashCode(of = {"id"})
public class UndoableOperationDAO {
    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private OffsetDateTime created;

    @NotAudited
    @OneToMany(mappedBy = "operation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UndoableOperationEntryDAO> entries = new ArrayList<>();

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private long sequenceNumber;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private boolean undone;

    /**
     * Adds an entry to this operation while keeping both sides of the association in sync.
     *
     * @param entry the entity snapshot entry to add
     */
    public void addEntry(UndoableOperationEntryDAO entry) {
        entry.setOperation(this);
        entries.add(entry);
    }
}
