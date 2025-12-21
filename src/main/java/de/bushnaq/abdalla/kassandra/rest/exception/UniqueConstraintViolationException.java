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

package de.bushnaq.abdalla.kassandra.rest.exception;

import lombok.Getter;

/**
 * Exception thrown when a unique constraint violation is detected before attempting database operation.
 * This allows us to provide field-specific error information to the client.
 */
@Getter
public class UniqueConstraintViolationException extends RuntimeException {

    private final String entityType;
    private final String field;
    private final Object value;

    /**
     * Creates a new unique constraint violation exception.
     *
     * @param entityType The type of entity (e.g., "Product", "User")
     * @param field      The field that has a duplicate value
     * @param value      The duplicate value
     */
    public UniqueConstraintViolationException(String entityType, String field, Object value) {
        super(String.format("A %s with %s '%s' already exists",
                entityType.toLowerCase(), field, value));
        this.entityType = entityType;
        this.field      = field;
        this.value      = value;
    }

    /**
     * Creates a new unique constraint violation exception with custom message.
     *
     * @param entityType The type of entity (e.g., "Product", "User")
     * @param field      The field that has a duplicate value
     * @param value      The duplicate value
     * @param message    Custom error message
     */
    public UniqueConstraintViolationException(String entityType, String field, Object value, String message) {
        super(message);
        this.entityType = entityType;
        this.field      = field;
        this.value      = value;
    }
}

