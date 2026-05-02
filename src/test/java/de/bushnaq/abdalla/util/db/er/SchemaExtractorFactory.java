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

import de.bushnaq.abdalla.util.db.er.intern.H2SchemaExtractor;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory that inspects the live JDBC connection to determine which
 * {@link DatabaseType} is in use, then returns the matching
 * {@link SchemaExtractor} implementation.
 *
 * <p>Detection is performed via {@link java.sql.DatabaseMetaData#getDatabaseProductName()},
 * which is portable across all JDBC drivers.  The product name is matched
 * case-insensitively against known prefixes:
 * <ul>
 *   <li>{@code "h2"}              -&gt; {@link H2SchemaExtractor}</li>
 *   <li>{@code "postgresql"}      -&gt; (not yet implemented, throws)</li>
 *   <li>{@code "mysql"/"mariadb"} -&gt; (not yet implemented, throws)</li>
 * </ul>
 *
 * <p>To add support for a new database:
 * <ol>
 *   <li>Add a value to {@link DatabaseType}.</li>
 *   <li>Create a class that implements {@link SchemaExtractor} for that DB.</li>
 *   <li>Add the product-name prefix to {@link #detect(EntityManager)} and the
 *       factory branch in {@link #create(EntityManager)}.</li>
 * </ol>
 */
@Slf4j
public class SchemaExtractorFactory {

    /**
     * Detects the database engine and returns the appropriate
     * {@link SchemaExtractor} implementation.
     *
     * @param em the JPA {@link EntityManager}; must not be {@code null}
     * @return a {@link SchemaExtractor} suited for the detected database
     * @throws UnsupportedOperationException if the detected {@link DatabaseType}
     *                                       has no extractor implementation yet
     */
    public static SchemaExtractor create(EntityManager em) {
        DatabaseType type = detect(em);
        log.info("Detected database type: {}", type);

        return switch (type) {
            case H2 -> new H2SchemaExtractor();
            case POSTGRESQL -> throw new UnsupportedOperationException(
                    "PostgreSQL SchemaExtractor is not yet implemented");
            case MYSQL -> throw new UnsupportedOperationException(
                    "MySQL/MariaDB SchemaExtractor is not yet implemented");
            case UNKNOWN -> throw new UnsupportedOperationException(
                    "No SchemaExtractor available for unrecognised database type");
        };
    }

    /**
     * Detects the database engine by reading {@link java.sql.DatabaseMetaData}
     * from the underlying JDBC connection.
     *
     * @param em the JPA {@link EntityManager}; must not be {@code null}
     * @return the {@link DatabaseType} that corresponds to the running engine,
     * or {@link DatabaseType#UNKNOWN} if the product name is not
     * recognised
     */
    public static DatabaseType detect(EntityManager em) {
        AtomicReference<String> productName = new AtomicReference<>("unknown");

        em.unwrap(Session.class).doWork(connection ->
                productName.set(connection.getMetaData().getDatabaseProductName()));

        String name = productName.get().toLowerCase();
        log.debug("JDBC database product name: '{}'", name);

        if (name.startsWith("h2")) {
            return DatabaseType.H2;
        } else if (name.startsWith("postgresql")) {
            return DatabaseType.POSTGRESQL;
        } else if (name.startsWith("mysql") || name.startsWith("mariadb")) {
            return DatabaseType.MYSQL;
        } else {
            log.warn("Unrecognised database product name '{}'; returning UNKNOWN", name);
            return DatabaseType.UNKNOWN;
        }
    }
}
