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
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.util.db.er;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a foreign-key constraint discovered in the database schema,
 * carrying the owning (FK) side and the referenced (PK) side.
 */
@Getter
@AllArgsConstructor
public class ErForeignKey {

    /**
     * Name of the constraint as defined in the database.
     */
    private String constraintName;

    /**
     * Column in the FK table that carries the foreign-key value.
     */
    private String fromColumn;

    /**
     * Table that owns the foreign-key column (the "many" side).
     */
    private String fromTable;

    /**
     * Primary-key column in the referenced table.
     */
    private String toColumn;

    /**
     * Table being referenced (the "one" side).
     */
    private String toTable;
}

