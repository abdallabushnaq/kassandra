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

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts the full ER schema from the live H2 database by querying
 * {@code INFORMATION_SCHEMA}.  The result is a plain {@link ErSchema} object
 * that is independent of JPA or Spring — it can be passed directly to
 * {@link ErDiagramRenderer}.
 *
 * <p>Three passes are made:
 * <ol>
 *   <li>Tables and their columns (name, type, nullability).</li>
 *   <li>Primary-key column flags via {@code TABLE_CONSTRAINTS} +
 *       {@code KEY_COLUMN_USAGE}.</li>
 *   <li>Real FK constraints via {@code REFERENTIAL_CONSTRAINTS} joined to
 *       {@code KEY_COLUMN_USAGE}; FK column flags are also marked on the
 *       owning {@link ErColumn}.</li>
 * </ol>
 *
 * <p><b>Note:</b> columns that carry bare {@code UUID} FK values without a
 * JPA {@code @ManyToOne} annotation will <em>not</em> appear as FK constraints
 * here because Hibernate never creates a {@code FOREIGN KEY} DDL statement for
 * them.
 */
@Slf4j
public class SchemaExtractor {

    /**
     * Builds and returns a fully-populated {@link ErSchema} for the current
     * H2 database schema.
     *
     * @param em the JPA {@link EntityManager} used to execute native queries
     * @return schema model containing all base tables, columns, PKs, and FKs
     */
    @SuppressWarnings("unchecked")
    public ErSchema extract(EntityManager em) {
        ErSchema             schema = new ErSchema();
        Map<String, ErTable> byName = new HashMap<>();

        // ── Pass 1: tables + columns ──────────────────────────────────────
        List<String> tableNames = em.createNativeQuery(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                                + "WHERE TABLE_SCHEMA = SCHEMA() AND TABLE_TYPE = 'BASE TABLE' "
                                + "ORDER BY TABLE_NAME")
                .getResultList();

        for (String rawName : tableNames) {
            String  tableName = rawName.toLowerCase();
            ErTable table     = new ErTable();
            table.setTableName(tableName);

            List<Object[]> cols = em.createNativeQuery(
                            "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE "
                                    + "FROM INFORMATION_SCHEMA.COLUMNS "
                                    + "WHERE TABLE_SCHEMA = SCHEMA() AND TABLE_NAME = '" + rawName + "' "
                                    + "ORDER BY ORDINAL_POSITION")
                    .getResultList();

            for (Object[] row : cols) {
                ErColumn col = new ErColumn();
                col.setName(row[0].toString().toLowerCase());
                col.setDataType(row[1].toString().toUpperCase());
                col.setNullable("YES".equalsIgnoreCase(row[2].toString()));
                table.getColumns().add(col);
            }

            schema.getTables().add(table);
            byName.put(rawName.toUpperCase(), table);
            log.debug("Extracted table {} with {} columns", tableName, table.getColumns().size());
        }

        // ── Pass 2: primary-key flags ─────────────────────────────────────
        for (ErTable table : schema.getTables()) {
            String rawTableName = table.getTableName().toUpperCase();

            List<String> pkCols = em.createNativeQuery(
                            "SELECT kcu.COLUMN_NAME "
                                    + "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc "
                                    + "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu "
                                    + "  ON tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA "
                                    + "  AND tc.CONSTRAINT_NAME  = kcu.CONSTRAINT_NAME "
                                    + "WHERE tc.TABLE_SCHEMA = SCHEMA() "
                                    + "  AND tc.TABLE_NAME = '" + rawTableName + "' "
                                    + "  AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'")
                    .getResultList();

            for (String pkCol : pkCols) {
                String lower = pkCol.toLowerCase();
                table.getColumns().stream()
                        .filter(c -> c.getName().equals(lower))
                        .forEach(c -> c.setPrimaryKey(true));
            }
        }

        // ── Pass 3: foreign-key constraints ──────────────────────────────
        List<Object[]> fkRows = em.createNativeQuery(
                        "SELECT fk.TABLE_NAME, fk.COLUMN_NAME, pk.TABLE_NAME, pk.COLUMN_NAME, rc.CONSTRAINT_NAME "
                                + "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc "
                                + "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE fk "
                                + "  ON fk.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA "
                                + "  AND fk.CONSTRAINT_NAME  = rc.CONSTRAINT_NAME "
                                + "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE pk "
                                + "  ON pk.CONSTRAINT_SCHEMA = rc.UNIQUE_CONSTRAINT_SCHEMA "
                                + "  AND pk.CONSTRAINT_NAME  = rc.UNIQUE_CONSTRAINT_NAME "
                                + "ORDER BY rc.CONSTRAINT_NAME, fk.ORDINAL_POSITION")
                .getResultList();

        for (Object[] row : fkRows) {
            String fromTable      = row[0].toString().toLowerCase();
            String fromColumn     = row[1].toString().toLowerCase();
            String toTable        = row[2].toString().toLowerCase();
            String toColumn       = row[3].toString().toLowerCase();
            String constraintName = row[4].toString();

            ErForeignKey fk = new ErForeignKey(constraintName, fromColumn, fromTable, toColumn, toTable);
            schema.getForeignKeys().add(fk);

            // Mark the FK column in the owning table
            ErTable owningTable = byName.get(fromTable.toUpperCase());
            if (owningTable != null) {
                owningTable.getColumns().stream()
                        .filter(c -> c.getName().equals(fromColumn))
                        .forEach(c -> c.setForeignKey(true));
            }
            log.debug("FK: {}.{} → {}.{} ({})", fromTable, fromColumn, toTable, toColumn, constraintName);
        }

        log.info("Extracted {} tables and {} FK constraints", schema.getTables().size(), schema.getForeignKeys().size());
        return schema;
    }
}

