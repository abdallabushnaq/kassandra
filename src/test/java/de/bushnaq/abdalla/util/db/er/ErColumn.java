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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single column in an ER-diagram table node, carrying the column
 * metadata extracted from the database schema.
 */
@Getter
@Setter
@NoArgsConstructor
public class ErColumn {

    /**
     * SQL data type of the column (e.g., {@code VARCHAR}, {@code UUID}).
     */
    private String dataType;

    /**
     * Whether this column participates in a foreign-key constraint.
     */
    private boolean foreignKey;

    /**
     * Lower-cased column name as returned by {@code INFORMATION_SCHEMA}.
     */
    private String name;

    /**
     * Whether the column allows {@code NULL} values.
     */
    private boolean nullable;

    /**
     * Whether this column is part of the table's primary key.
     */
    private boolean primaryKey;
}

