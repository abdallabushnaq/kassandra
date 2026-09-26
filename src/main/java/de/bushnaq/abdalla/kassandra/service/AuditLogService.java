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
package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.dto.AuditEvent;
import de.bushnaq.abdalla.kassandra.dto.AuditPage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Reads safe audit metadata from the existing Envers history.
 */
@Service
public class AuditLogService {
    private static final String CHANGES = String.join(" UNION ALL ",
            source("products_AUD", "Product", "name"),
            source("versions_AUD", "Version", "name"),
            source("features_AUD", "Feature", "name"),
            source("sprints_AUD", "Sprint", "name"),
            source("tasks_AUD", "Task", "name"),
            source("worklogs_AUD", "Worklog", "NULL"),
            source("relations_AUD", "Relation", "NULL"),
            source("oidc_providers_AUD", "Identity Provider", "display_name"),
            source("users_AUD", "User", "name"),
            source("user_groups_AUD", "User Group", "name"),
            source("server_settings_AUD", "Server Setting", "setting_key"));

    @Autowired
    private EntityManager entityManager;

    private static String source(String table, String type, String label) {
        String id = "server_settings_AUD".equals(table) ? "setting_key" : "id";
        String display;
        if ("NULL".equals(label)) {
            display = "NULL";
        } else if ("setting_key".equals(label)) {
            display = "a.setting_key";
        } else {
            display = "COALESCE(a." + label + ", (SELECT previous." + label + " FROM " + table
                    + " previous WHERE previous." + id + " = a." + id + " AND previous.REV < a.REV"
                    + " AND previous.REVTYPE <> 2 ORDER BY previous.REV DESC LIMIT 1))";
        }
        return "SELECT a.REV AS revision, a.REVTYPE AS revision_type, CAST(a." + id
                + " AS VARCHAR(255)) AS entity_id, '" + type + "' AS entity_type, "
                + display + " AS label FROM " + table + " a";
    }

    /**
     * Searches audited changes, applying filters and pagination in the database.
     *
     * @param user   actor email or name filter
     * @param action CREATE, UPDATE, DELETE, or null
     * @param search free text for actor, name, email, action, or ISO date
     * @param from   inclusive start of the timeframe
     * @param to     exclusive end of the timeframe
     * @param page   zero-based page number
     * @param size   results per page, up to 100
     * @return matching audit events and total count
     */
    @Transactional(readOnly = true)
    public AuditPage find(String user, String action, String search, Instant from, Instant to, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || page > Integer.MAX_VALUE / size
                || (from != null && to != null && !from.isBefore(to))) {
            throw new IllegalArgumentException("Invalid audit page or timeframe");
        }
        String normalizedAction = action == null || action.isBlank() ? null : action.toUpperCase(Locale.ROOT);
        if (normalizedAction != null && !List.of("CREATE", "UPDATE", "DELETE").contains(normalizedAction)) {
            throw new IllegalArgumentException("Invalid audit action");
        }
        String base = " FROM (" + CHANGES + ") change JOIN audit_revisions revision ON revision.id = change.revision"
                + " WHERE (:from IS NULL OR revision.timestamp >= :from)"
                + " AND (:to IS NULL OR revision.timestamp < :to)"
                + " AND (:action IS NULL OR change.revision_type = :action)"
                + " AND (:user IS NULL OR LOWER(revision.actor) LIKE :user ESCAPE '\\'"
                + " OR EXISTS (SELECT 1 FROM users_AUD actor_user WHERE LOWER(actor_user.email) = LOWER(revision.actor)"
                + " AND LOWER(actor_user.name) LIKE :user ESCAPE '\\'))"
                + " AND (:search IS NULL OR LOWER(revision.actor) LIKE :search ESCAPE '\\'"
                + " OR LOWER(change.label) LIKE :search ESCAPE '\\' OR LOWER(change.entity_type) LIKE :search ESCAPE '\\'"
                + " OR LOWER(CASE change.revision_type WHEN 0 THEN 'create' WHEN 1 THEN 'update' ELSE 'delete' END) LIKE :search ESCAPE '\\'"
                + " OR (change.entity_type = 'User' AND EXISTS (SELECT 1 FROM users_AUD changed_user"
                + " WHERE CAST(changed_user.id AS VARCHAR(255)) = change.entity_id"
                + " AND LOWER(changed_user.email) LIKE :search ESCAPE '\\'))"
                + " OR EXISTS (SELECT 1 FROM users_AUD actor_user WHERE LOWER(actor_user.email) = LOWER(revision.actor)"
                + " AND LOWER(actor_user.name) LIKE :search ESCAPE '\\')"
                + " OR FORMATDATETIME(DATEADD('MILLISECOND', revision.timestamp, TIMESTAMP '1970-01-01 00:00:00'), 'yyyy-MM-dd') LIKE :search ESCAPE '\\')";
        Query count = bind(entityManager.createNativeQuery("SELECT COUNT(*)" + base), user, normalizedAction, search, from, to);
        long  total = ((Number) count.getSingleResult()).longValue();
        Query rows = bind(entityManager.createNativeQuery(
                        "SELECT change.revision, revision.timestamp, revision.actor, change.revision_type,"
                                + " change.entity_type, change.entity_id, change.label, revision.replay" + base
                                + " ORDER BY revision.timestamp DESC, change.revision DESC, change.entity_type, change.entity_id"),
                user, normalizedAction, search, from, to);
        rows.setFirstResult(page * size);
        rows.setMaxResults(size);
        @SuppressWarnings("unchecked")
        List<Object[]> results = rows.getResultList();
        List<AuditEvent> items = results.stream().map(row -> new AuditEvent(
                ((Number) row[0]).intValue(), Instant.ofEpochMilli(((Number) row[1]).longValue()),
                (String) row[2], switch (((Number) row[3]).intValue()) {
            case 0 -> "CREATE";
            case 1 -> "UPDATE";
            case 2 -> "DELETE";
            default -> throw new IllegalStateException("Unknown Envers revision type: " + row[3]);
        }, (String) row[4], (String) row[5], (String) row[6], (Boolean) row[7])).toList();
        return new AuditPage(items, total, page, size);
    }

    private Query bind(Query query, String user, String action, String search, Instant from, Instant to) {
        return query.setParameter("user", pattern(user))
                .setParameter("action", action == null ? null : List.of("CREATE", "UPDATE", "DELETE").indexOf(action))
                .setParameter("search", pattern(search))
                .setParameter("from", from == null ? null : from.toEpochMilli())
                .setParameter("to", to == null ? null : to.toEpochMilli());
    }

    private String pattern(String value) {
        return value == null || value.isBlank() ? null : "%" + value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
