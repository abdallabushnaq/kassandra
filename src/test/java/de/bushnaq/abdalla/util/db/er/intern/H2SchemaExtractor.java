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

package de.bushnaq.abdalla.util.db.er.intern;

import de.bushnaq.abdalla.util.db.er.*;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SchemaExtractor} implementation for H2 databases.
 *
 * <p>Queries the H2 {@code INFORMATION_SCHEMA} catalogue in three passes:
 * <ol>
 *   <li>Enumerate all base tables and their columns (name, type, nullability).</li>
 *   <li>Mark primary-key columns via {@code TABLE_CONSTRAINTS} +
 *       {@code KEY_COLUMN_USAGE}.</li>
 *   <li>Populate real FK constraints via {@code REFERENTIAL_CONSTRAINTS} joined
 *       to {@code KEY_COLUMN_USAGE}, and mark the owning FK column on its
 *       {@link EntityRelationshipColumn}.</li>
 * </ol>
 *
 * <p><b>Note:</b> columns carrying bare {@code UUID} FK values without a JPA
 * {@code @ManyToOne} will <em>not</em> appear as FK constraints because
 * Hibernate never emits a {@code FOREIGN KEY} DDL clause for them.
 */
@Slf4j
public class H2SchemaExtractor implements SchemaExtractor {

    /**
     * {@inheritDoc}
     *
     * <p>Queries the H2 {@code INFORMATION_SCHEMA} to build the schema model.
     */
    @Override
    @SuppressWarnings("unchecked")
    public EntityRelationshipSchema extract(EntityManager em) {
        EntityRelationshipSchema             schema = new EntityRelationshipSchema();
        Map<String, EntityRelationshipTable> byName = new HashMap<>();

        // ── Pass 1: tables + columns ──────────────────────────────────────
        List<String> tableNames = em.createNativeQuery("""
                        SELECT TABLE_NAME
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_SCHEMA = SCHEMA()
                          AND TABLE_TYPE = 'BASE TABLE'
                        ORDER BY TABLE_NAME""")
                .getResultList();

        for (String rawName : tableNames) {
            String                  tableName = rawName.toLowerCase();
            EntityRelationshipTable table     = new EntityRelationshipTable();
            table.setTableName(tableName);

            List<Object[]> cols = em.createNativeQuery("""
                            SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
                            FROM INFORMATION_SCHEMA.COLUMNS
                            WHERE TABLE_SCHEMA = SCHEMA()
                              AND TABLE_NAME = '""" + rawName + """
                            '
                            ORDER BY ORDINAL_POSITION""")
                    .getResultList();

            for (Object[] row : cols) {
                EntityRelationshipColumn col = new EntityRelationshipColumn();
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
        for (EntityRelationshipTable table : schema.getTables()) {
            String rawTableName = table.getTableName().toUpperCase();

            List<String> pkCols = em.createNativeQuery("""
                            SELECT kcu.COLUMN_NAME
                            FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                            JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                              ON tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                             AND tc.CONSTRAINT_NAME  = kcu.CONSTRAINT_NAME
                            WHERE tc.TABLE_SCHEMA = SCHEMA()
                              AND tc.TABLE_NAME = '""" + rawTableName + """
                            '
                              AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'""")
                    .getResultList();

            for (String pkCol : pkCols) {
                String lower = pkCol.toLowerCase();
                table.getColumns().stream()
                        .filter(c -> c.getName().equals(lower))
                        .forEach(c -> c.setPrimaryKey(true));
            }
        }

        // ── Pass 3: foreign-key constraints ──────────────────────────────
        List<Object[]> fkRows = em.createNativeQuery("""
                        SELECT fk.TABLE_NAME, fk.COLUMN_NAME, pk.TABLE_NAME, pk.COLUMN_NAME, rc.CONSTRAINT_NAME
                        FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc
                        JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE fk
                          ON fk.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA
                         AND fk.CONSTRAINT_NAME  = rc.CONSTRAINT_NAME
                        JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE pk
                          ON pk.CONSTRAINT_SCHEMA = rc.UNIQUE_CONSTRAINT_SCHEMA
                         AND pk.CONSTRAINT_NAME  = rc.UNIQUE_CONSTRAINT_NAME
                        ORDER BY rc.CONSTRAINT_NAME, fk.ORDINAL_POSITION""")
                .getResultList();

        for (Object[] row : fkRows) {
            String fromTable      = row[0].toString().toLowerCase();
            String fromColumn     = row[1].toString().toLowerCase();
            String toTable        = row[2].toString().toLowerCase();
            String toColumn       = row[3].toString().toLowerCase();
            String constraintName = row[4].toString();

            EntityRelationshipForeignKey fk = new EntityRelationshipForeignKey(constraintName, fromColumn, fromTable, toColumn, toTable);
            schema.getForeignKeys().add(fk);

            // Mark the FK column on the owning table
            EntityRelationshipTable owningTable = byName.get(fromTable.toUpperCase());
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

