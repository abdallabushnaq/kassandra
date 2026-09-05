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

package de.bushnaq.abdalla.kassandra.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The available undo/redo state and chronological product operation history.
 */
@Getter
@Setter
public class UndoRedoHistory {
    private boolean canRedo;
    private boolean canUndo;
    private List<Operation> operations;

    /**
     * One user-visible planning operation.
     */
    @Getter
    @Setter
    public static class Operation {
        private String actor;
        private OffsetDateTime created;
        private List<EntityChange> entityChanges;
        private UUID id;
        private String kind;
        private UUID productId;
        private String productName;
        private long sequenceNumber;
        private String summary;
        private boolean undone;
    }

    /**
     * A named planning entity affected by one undoable operation.
     */
    @Getter
    @Setter
    public static class EntityChange {
        private String action;
        private String displayName;
        private String entityType;
        private List<String> fieldChanges;
    }
}
