/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.util;

import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to manage H2 database state for tests
 * This allows exporting a database snapshot after data generation
 * and importing it in subsequent test runs to save time.
 */
@Component
public class H2DatabaseStateManager {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String            SNAPSHOTS_DIR  = "test-database-snapshots";
    private static final Logger            logger         = LoggerFactory.getLogger(H2DatabaseStateManager.class);
    @Autowired
    private              DataSource        dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${spring.datasource.password}")
    private String       password;
    @Value("${spring.datasource.username}")
    private String       username;

    /**
     * Properly drops all user tables from the database
     * This ensures we can restore from a snapshot without table conflicts
     */
    private void dropAllTables() {
        try (Connection conn = dataSource.getConnection()) {
            // Disable foreign key constraints first
            conn.createStatement().execute("SET REFERENTIAL_INTEGRITY FALSE");

            // Get all table names from the database
            List<String> tableNames = new ArrayList<>();
            ResultSet    rs         = conn.getMetaData().getTables(null, "PUBLIC", "%", new String[]{"TABLE"});
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                // Skip H2's internal tables
                if (!tableName.startsWith("SYSTEM_")) {
                    tableNames.add(tableName);
                }
            }

            // Drop each table
            for (String tableName : tableNames) {
                try {
                    logger.debug("Dropping table: {}", tableName);
                    conn.createStatement().execute("DROP TABLE IF EXISTS \"" + tableName + "\"");
                } catch (SQLException e) {
                    logger.warn("Could not drop table {}: {}", tableName, e.getMessage());
                }
            }

            // Re-enable foreign key constraints
            conn.createStatement().execute("SET REFERENTIAL_INTEGRITY TRUE");
            logger.info("All tables dropped successfully");
        } catch (Exception e) {
            logger.error("Error dropping tables", e);
            throw new RuntimeException("Failed to prepare database for snapshot import", e);
        }
    }

    /**
     * Exports the current in-memory database to a snapshot file
     *
     * @param snapshotName Name identifier for this snapshot
     * @return Path to the created snapshot file
     */
    public String exportDatabaseSnapshot(String snapshotName) {
        try (Profiler pc = new Profiler(SampleType.FILE)) {
            try {
                // Ensure directory exists
                File snapshotsDir = new File(SNAPSHOTS_DIR);
                if (!snapshotsDir.exists()) {
                    snapshotsDir.mkdirs();
                }

                // Create a timestamped filename for the snapshot
                String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
                String filename  = SNAPSHOTS_DIR + File.separator + snapshotName + "_" + timestamp + ".zip";

                // Execute the SCRIPT command
                try (Connection conn = dataSource.getConnection()) {
                    String scriptCommand = "SCRIPT TO '" + Paths.get(filename).toAbsolutePath() + "' COMPRESSION ZIP";

                    logger.info("Exporting database snapshot to: {}", filename);
                    conn.createStatement().execute(scriptCommand);
                    logger.info("Database snapshot exported successfully");

                    return filename;
                }
            } catch (Exception e) {
                logger.error("Failed to export database snapshot", e);
                throw new RuntimeException("Failed to export database snapshot", e);
            }
        }
    }

    /**
     * Find the latest snapshot file for a given snapshot name
     *
     * @param snapshotName The name of the snapshot
     * @return Path to the latest snapshot file or null if none found
     */
    public String findLatestSnapshot(String snapshotName) {
        try {
            File snapshotsDir = new File(SNAPSHOTS_DIR);
            if (!snapshotsDir.exists()) {
                return null;
            }

            File[] files = snapshotsDir.listFiles((dir, name) -> name.startsWith(snapshotName + "_") && name.endsWith(".zip"));
            if (files == null || files.length == 0) {
                return null;
            }

            // Sort by last modified date to get the most recent snapshot
            File latestSnapshot = files[0];
            for (File file : files) {
                if (file.lastModified() > latestSnapshot.lastModified()) {
                    latestSnapshot = file;
                }
            }

            return latestSnapshot.getAbsolutePath();
        } catch (Exception e) {
            logger.error("Error finding latest snapshot", e);
            return null;
        }
    }

    /**
     * Imports a database snapshot from a file
     *
     * @param snapshotFilePath Path to the snapshot file to import
     * @return true if import was successful
     */
    public boolean importDatabaseSnapshot(String snapshotFilePath) {
        try {
            File snapshotFile = new File(snapshotFilePath);
            if (!snapshotFile.exists()) {
                logger.warn("Database snapshot file not found: {}", snapshotFilePath);
                return false;
            }

            logger.info("Importing database snapshot from: {}", snapshotFilePath);

            // First we need to drop all existing tables to avoid conflicts
            dropAllTables();

            // Run the RUNSCRIPT command to restore from snapshot
            try (Connection conn = dataSource.getConnection()) {
                String runScriptCommand = "RUNSCRIPT FROM '" +
                        Paths.get(snapshotFilePath).toAbsolutePath() +
                        "' COMPRESSION ZIP";
                conn.createStatement().execute(runScriptCommand);
                logger.info("Database snapshot imported successfully");
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to import database snapshot", e);
            return false;
        }
    }
}
