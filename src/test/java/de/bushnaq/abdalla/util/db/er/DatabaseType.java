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

/**
 * Enumeration of database engines that have a corresponding
 * {@link SchemaExtractor} implementation.
 *
 * <p>New values should be added here when a new extractor is introduced, and
 * {@code SchemaExtractorFactory} updated to map JDBC product names to the new
 * value.
 */
public enum DatabaseType {

    /** H2 in-memory / file-backed database. */
    H2,

    /** PostgreSQL relational database. */
    POSTGRESQL,

    /** MySQL / MariaDB relational database. */
    MYSQL,

    /** Any engine whose product name is not recognised. */
    UNKNOWN
}


