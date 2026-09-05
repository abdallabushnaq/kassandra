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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.audit;

import java.util.UUID;

/**
 * Holds the command-journal operation associated with the current transaction thread.
 */
public final class AuditOperationContextHolder {
    private static final ThreadLocal<UUID> OPERATION_ID = new ThreadLocal<>();

    private AuditOperationContextHolder() {
    }

    /**
     * Removes the operation association from the current thread.
     */
    public static void clear() {
        OPERATION_ID.remove();
    }

    /**
     * Returns the operation associated with the current thread.
     *
     * @return the operation ID, or {@code null} when the mutation is not journaled
     */
    public static UUID getOperationId() {
        return OPERATION_ID.get();
    }

    /**
     * Associates an undoable operation with the current thread.
     *
     * @param operationId the persisted command-journal operation ID
     */
    public static void setOperationId(UUID operationId) {
        OPERATION_ID.set(operationId);
    }
}
