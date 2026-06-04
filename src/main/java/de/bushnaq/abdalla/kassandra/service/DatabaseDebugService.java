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

package de.bushnaq.abdalla.kassandra.service;

import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Service for debugging database content.
 * Supports printing all (or a filtered subset of) database tables either to {@link System#out}
 * or to a file ({@value #DB_OUTPUT_FILE}).
 */
@Service
@Log4j2
public class DatabaseDebugService {

    private static final String DB_OUTPUT_FILE = "db.txt";

    @Autowired
    private EntityManager entityManager;

    /**
     * Renders a single table as a formatted ASCII string.  Returns an empty string for tables
     * that contain no rows.
     *
     * @param tableName the name of the table to render
     * @return formatted ASCII table, or an empty string if the table is empty
     */
    private String buildTableContent(String tableName) {
        @SuppressWarnings("unchecked")
        List<String> columnNames = entityManager.createNativeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = SCHEMA() AND TABLE_NAME = '" + tableName + "' " +
                        "ORDER BY ORDINAL_POSITION"
        ).getResultList();

        @SuppressWarnings("unchecked")
        List<String> columnTypes = entityManager.createNativeQuery(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = SCHEMA() AND TABLE_NAME = '" + tableName + "' " +
                        "ORDER BY ORDINAL_POSITION"
        ).getResultList();

        List<?> results = entityManager.createNativeQuery("SELECT * FROM " + tableName).getResultList();

        if (results.isEmpty()) {
            return "";
        }

        // Calculate column widths: start with header width, then widen for data.
        int[] maxWidths = new int[columnNames.size()];
        for (int i = 0; i < columnNames.size(); i++) {
            maxWidths[i] = columnNames.get(i).length();
        }
        for (Object row : results) {
            if (row instanceof Object[] cells) {
                for (int i = 0; i < cells.length; i++) {
                    maxWidths[i] = Math.max(maxWidths[i], formatCellValue(cells[i], columnTypes.get(i)).length());
                }
            } else {
                maxWidths[0] = Math.max(maxWidths[0], row != null ? row.toString().length() : 4);
            }
        }

        // Build separator row.
        StringBuilder sep = new StringBuilder("+");
        for (int width : maxWidths) {
            sep.append("-".repeat(width + 2)).append("+");
        }
        String separator = sep.toString();

        StringBuilder output = new StringBuilder();
        output.append("\n").append(tableName).append(":\n").append(separator).append("\n");

        // Header.
        output.append("|");
        for (int i = 0; i < columnNames.size(); i++) {
            output.append(String.format(" %-" + maxWidths[i] + "s |", columnNames.get(i)));
        }
        output.append("\n").append(separator).append("\n");

        // Data rows.
        for (Object row : results) {
            output.append("|");
            if (row instanceof Object[] cells) {
                for (int i = 0; i < cells.length; i++) {
                    output.append(String.format(" %-" + maxWidths[i] + "s |", formatCellValue(cells[i], columnTypes.get(i))));
                }
            } else {
                output.append(String.format(" %-" + maxWidths[0] + "s |", row != null ? row.toString() : "null"));
            }
            output.append("\n");
        }
        output.append(separator).append("\n");

        return output.toString();
    }

    /**
     * Formats a single cell value for display.
     * <ul>
     *   <li>Returns {@code "null"} for {@code null} values.</li>
     *   <li>Returns {@code "<BLOB>"} for binary-large-object columns.</li>
     *   <li>Converts 16-byte arrays to UUID strings (H2 returns UUID columns as raw bytes in
     *       native queries).</li>
     *   <li>Falls back to {@code toString()} for all other types.</li>
     * </ul>
     *
     * @param value      the raw cell value returned by a native query
     * @param columnType the SQL data type of the column
     * @return a human-readable string representation
     */
    private String formatCellValue(Object value, String columnType) {
        if (value == null) {
            return "null";
        }
        if (columnType.equalsIgnoreCase("BINARY LARGE OBJECT")) {
            return "<BLOB>";
        }
        if (value instanceof byte[] bytes && bytes.length == 16) {
            // H2 returns UUID columns as raw 16-byte arrays in native queries.
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (bytes[i] & 0xffL);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (bytes[i] & 0xffL);
            }
            return new UUID(msb, lsb).toString();
        }
        return value.toString();
    }

    /**
     * Returns all user-table names in the current H2 schema, excluding H2 system tables.
     *
     * @return list of table names
     */
    private List<String> getAllTableNames() {
        @SuppressWarnings("unchecked")
        List<String> tableNames = entityManager.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA()"
        ).getResultList();

        return tableNames.stream()
                .filter(name -> !name.equals("SPATIAL_REF_SYS") && !name.equals("GEOMETRY_COLUMNS"))
                .toList();
    }

    /**
     * Writes all database tables to {@value #DB_OUTPUT_FILE}.
     *
     * @return the absolute path of the output file
     * @throws IOException if the file cannot be written
     */
    @Deprecated
    public Path printDatabaseTables() throws IOException {
        log.info("Printing database tables to {}", DB_OUTPUT_FILE);

        Path outputPath = Paths.get(DB_OUTPUT_FILE).toAbsolutePath();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            writer.println("=== Database Content ===");
            writer.println();

            for (String tableName : getAllTableNames()) {
                String content = buildTableContent(tableName);
                if (!content.isEmpty()) {
                    writer.print(content);
                }
            }

            writer.flush();
        }

        log.info("Database tables written to {}", outputPath);
        return outputPath;
    }

    /**
     * Prints all non-empty tables in the current schema to {@link System#out}.
     */
    public void printTables() {
        printTables(null);
    }

    /**
     * Prints the specified tables (or all non-empty tables when {@code filterTableNames} is
     * {@code null}) to {@link System#out}.
     *
     * @param filterTableNames optional array of table names to include; pass {@code null} to print
     *                         every non-empty table
     */
    public void printTables(String[] filterTableNames) {
        try {
            List<String> tableNames = getAllTableNames();

            if (filterTableNames != null && filterTableNames.length > 0) {
                List<String> filterList = List.of(filterTableNames);
                tableNames = tableNames.stream()
                        .filter(filterList::contains)
                        .toList();
            }

            StringBuilder output = new StringBuilder("\n=== Database Content ===\n");
            for (String tableName : tableNames) {
                output.append(buildTableContent(tableName));
            }

            System.out.println(output);
        } catch (Exception e) {
            log.error("Error printing tables: {}", e.getMessage(), e);
        }
    }
}
