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

/**
 * Strategy interface for extracting an {@link EntityRelationshipSchema} from a
 * live database.  Each supported database type provides its own implementation
 * (e.g. {@link H2SchemaExtractor}) that issues the appropriate
 * vendor-specific queries against {@code INFORMATION_SCHEMA} or an equivalent
 * catalogue.
 *
 * <p>Implementations are expected to be stateless; any given instance may be
 * reused across multiple calls to {@link #extract(EntityManager)}.
 */
public interface SchemaExtractor {

    /**
     * Extracts the full ER schema — tables, columns, primary keys, and foreign
     * key constraints — from the database reachable through the supplied
     * {@link EntityManager}.
     *
     * @param em the JPA {@link EntityManager} used to execute native queries;
     *           must not be {@code null}
     * @return a fully-populated {@link EntityRelationshipSchema}; never
     * {@code null}
     */
    EntityRelationshipSchema extract(EntityManager em);
}
