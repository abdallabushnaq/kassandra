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
package de.bushnaq.abdalla.kassandra.dto;

import java.time.Instant;
import java.util.List;

/**
 * Safe display metadata for one audited entity change.
 *
 * @param revision     revision number
 * @param timestamp    time of the change
 * @param actor        authenticated email or system identity
 * @param action       create, update, or delete
 * @param entityType   changed entity type
 * @param entityId     changed entity identifier
 * @param label        display name at the time of the change, when available
 * @param replay       whether this change was made by undo or redo
 * @param fieldChanges safe field-level differences for updates
 */
public record AuditEvent(int revision, Instant timestamp, String actor, String action, String entityType,
                         String entityId, String label, boolean replay, List<String> fieldChanges) {
}
