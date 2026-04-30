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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a database table in the ER diagram.  The {@code x}, {@code y},
 * {@code width}, and {@code height} fields are populated by
 * {@link ErDiagramRenderer} during the layout pass and have no meaning prior
 * to rendering.
 */
@Getter
@Setter
@NoArgsConstructor
public class ErTable {

    /**
     * Columns belonging to this table, in ordinal order.
     */
    private List<ErColumn> columns = new ArrayList<>();

    /**
     * Computed pixel height of the table box (header + all rows). Set by the renderer.
     */
    private int height;

    /**
     * Lower-cased table name.
     */
    private String tableName;

    /**
     * Computed pixel width of the table box. Set by the renderer.
     */
    private int width;

    /**
     * Pixel x-coordinate of the top-left corner of the table box. Set by the renderer.
     */
    private int x;

    /**
     * Pixel y-coordinate of the top-left corner of the table box. Set by the renderer.
     */
    private int y;

    /**
     * Returns the zero-based index of the column with the given name, or
     * {@code 0} if not found (falls back to the first column so connectors
     * are always drawable).
     *
     * @param columnName the column name to look up (case-insensitive)
     * @return zero-based column index
     */
    public int columnIndex(String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return 0;
    }
}

