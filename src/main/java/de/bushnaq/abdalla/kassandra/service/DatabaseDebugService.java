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
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Service for debugging database content.
 * Prints all database tables to a file for inspection.
 */
@Service
@Log4j2
public class DatabaseDebugService {

    private static final String DB_OUTPUT_FILE = "db.txt";

    private final EntityManager entityManager;

    public DatabaseDebugService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Format a cell value for display, handling special types like BLOBs.
     */
    private String formatCellValue(Object cell, String columnType) {
        if (cell == null) {
            return "null";
        }
        if (columnType.equalsIgnoreCase("BINARY LARGE OBJECT")) {
            return "<BLOB>";
        }
        return cell.toString();
    }

    /**
     * Get all table names from the database.
     */
    private List<String> getAllTableNames() {
        @SuppressWarnings("unchecked")
        List<String> tableNames = entityManager.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA()"
        ).getResultList();

        // Filter out system tables
        return tableNames.stream()
                .filter(name -> !name.equals("SPATIAL_REF_SYS") && !name.equals("GEOMETRY_COLUMNS"))
                .toList();
    }

    /**
     * Print all database tables to db.txt file.
     *
     * @return the path to the output file
     * @throws IOException if file writing fails
     */
    public Path printDatabaseTables() throws IOException {
        log.info("Printing database tables to {}", DB_OUTPUT_FILE);

        Path outputPath = Paths.get(DB_OUTPUT_FILE).toAbsolutePath();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            writer.println("=== Database Content ===");
            writer.println();

            List<String> tableNames = getAllTableNames();

            for (String tableName : tableNames) {
                printTable(writer, tableName);
            }

            writer.flush();
        }

        log.info("Database tables written to {}", outputPath);
        return outputPath;
    }

    /**
     * Print a single table to the writer.
     */
    private void printTable(PrintWriter writer, String tableName) {
        // Get column names and types
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

        // Get all data
        String  dataQuery = "SELECT * FROM " + tableName;
        List<?> results   = entityManager.createNativeQuery(dataQuery).getResultList();

        // Skip empty tables
        if (results.isEmpty()) {
            return;
        }

        // Calculate max width for each column
        int[] maxWidths = new int[columnNames.size()];
        // Initialize with column name lengths
        for (int i = 0; i < columnNames.size(); i++) {
            maxWidths[i] = columnNames.get(i).length();
        }

        // Check data widths
        for (Object row : results) {
            if (row instanceof Object[] cells) {
                for (int i = 0; i < cells.length; i++) {
                    String value = formatCellValue(cells[i], columnTypes.get(i));
                    maxWidths[i] = Math.max(maxWidths[i], value.length());
                }
            } else {
                maxWidths[0] = Math.max(maxWidths[0], row != null ? row.toString().length() : 4);
            }
        }

        // Create dynamic separator
        StringBuilder separatorBuilder = new StringBuilder("+");
        for (int width : maxWidths) {
            separatorBuilder.append("-".repeat(width + 2)).append("+");
        }
        String separator = separatorBuilder.toString();

        // Print table name and header
        writer.println(tableName + ":");
        writer.println(separator);

        // Print column headers
        StringBuilder headerBuilder = new StringBuilder("|");
        for (int i = 0; i < columnNames.size(); i++) {
            headerBuilder.append(String.format(" %-" + maxWidths[i] + "s |", columnNames.get(i)));
        }
        writer.println(headerBuilder);
        writer.println(separator);

        // Print data rows
        for (Object row : results) {
            StringBuilder rowBuilder = new StringBuilder("|");
            if (row instanceof Object[] cells) {
                for (int i = 0; i < cells.length; i++) {
                    String value = formatCellValue(cells[i], columnTypes.get(i));
                    rowBuilder.append(String.format(" %-" + maxWidths[i] + "s |", value));
                }
            } else {
                rowBuilder.append(String.format(" %-" + maxWidths[0] + "s |", row != null ? row.toString() : "null"));
            }
            writer.println(rowBuilder);
        }
        writer.println(separator);
        writer.println();
    }
}
