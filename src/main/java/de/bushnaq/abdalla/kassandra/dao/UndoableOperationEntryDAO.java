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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Stores the before and after state of one entity within an undoable operation.
 */
@Entity
@Table(name = "undoable_operation_entries")
@Getter
@Setter
@ToString(exclude = "operation")
@EqualsAndHashCode(of = {"id"})
public class UndoableOperationEntryDAO {
    @Lob
    @Column
    private String afterSnapshot;

    @Lob
    @Column
    private String beforeSnapshot;

    @Column(nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private String entityType;

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    private UndoableOperationDAO operation;

    @Column(nullable = false)
    private int restoreOrder;
}
