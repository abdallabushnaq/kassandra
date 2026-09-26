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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads safe audit metadata from the existing Envers history.
 */
@Service
public class AuditLogService {
    private static final Map<String, String>      TABLES      = Map.ofEntries(
            Map.entry("Product", "products_AUD"), Map.entry("Version", "versions_AUD"),
            Map.entry("Feature", "features_AUD"), Map.entry("Sprint", "sprints_AUD"),
            Map.entry("Task", "tasks_AUD"), Map.entry("Worklog", "worklogs_AUD"),
            Map.entry("Relation", "relations_AUD"), Map.entry("Identity Provider", "oidc_providers_AUD"),
            Map.entry("User", "users_AUD"), Map.entry("User Group", "user_groups_AUD"),
            Map.entry("Server Setting", "server_settings_AUD"));
    private static final Map<String, Set<String>> SAFE_FIELDS = Map.ofEntries(
            Map.entry("Product", fields("name darkAvatarHash darkHeaderHash lightAvatarHash lightHeaderHash deleted deletedAt")),
            Map.entry("Version", fields("name productId deleted deletedAt")),
            Map.entry("Feature", fields("name versionId darkAvatarHash darkHeaderHash lightAvatarHash lightHeaderHash deleted deletedAt")),
            Map.entry("Sprint", fields("name featureId darkAvatarHash darkHeaderHash lightAvatarHash lightHeaderHash endDate originalEstimation releaseDate remaining startDate status userId worked deleted deletedAt")),
            Map.entry("Task", fields("name critical duration finish impactOnCost maxEstimate milestone minEstimate notes orderId parentTaskId progress remainingEstimate resourceId sprintId start taskMode taskStatus timeSpent deleted deletedAt")),
            Map.entry("Worklog", fields("authorId comment sprintId start taskId timeRemainingEstimate timeSpent updateAuthorId deleted deletedAt")),
            Map.entry("Relation", fields("predecessorId visible deleted deletedAt")),
            Map.entry("Identity Provider", fields("clientId displayName discoveryUri enabled issuerUri registrationId scopes")),
            Map.entry("User", fields("color darkAvatarHash email firstWorkingDay lastWorkingDay lightAvatarHash name roles")),
            Map.entry("User Group", fields("description name")),
            Map.entry("Server Setting", fields("auditValue encrypted")));
    private static final String                   CHANGES     = String.join(" UNION ALL ",
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
    private       EntityManager             entityManager;
    private final Map<String, List<String>> columns = new ConcurrentHashMap<>();

    private static Set<String> fields(String names) {
        return Set.copyOf(Arrays.asList(names.toLowerCase(Locale.ROOT).split(" ")));
    }

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
                }, (String) row[4], (String) row[5], (String) row[6], (Boolean) row[7], List.of()))
                .map(this::enrich).toList();
        return new AuditPage(items, total, page, size);
    }

    private AuditEvent enrich(AuditEvent event) {
        String       label   = "Relation".equals(event.entityType()) ? relationLabel(event) : event.label();
        List<String> changes = event.action().equals("UPDATE") ? changes(event) : List.of();
        return new AuditEvent(event.revision(), event.timestamp(), event.actor(), event.action(), event.entityType(),
                event.entityId(), label, event.replay(), changes);
    }

    private String relationLabel(AuditEvent event) {
        UUID id = UUID.fromString(event.entityId());
        List<?> owners = entityManager.createNativeQuery("""
                SELECT task_id FROM taskdao_relationdao_aud
                WHERE id = :id AND rev <= :revision
                ORDER BY rev DESC LIMIT 1
                """, UUID.class).setParameter("id", id).setParameter("revision", event.revision()).getResultList();
        List<?> predecessors = entityManager.createNativeQuery("""
                SELECT predecessor_id FROM relations_aud
                WHERE id = :id AND rev <= :revision AND revtype <> 2
                ORDER BY rev DESC LIMIT 1
                """, UUID.class).setParameter("id", id).setParameter("revision", event.revision()).getResultList();
        if (owners.isEmpty() || predecessors.isEmpty()) {
            return event.entityId();
        }
        return taskName((UUID) predecessors.getFirst(), event.revision()) + " -> "
                + taskName((UUID) owners.getFirst(), event.revision());
    }

    private String taskName(UUID id, int revision) {
        List<?> names = entityManager.createNativeQuery("""
                SELECT name FROM tasks_aud WHERE id = :id AND rev <= :revision AND revtype <> 2
                ORDER BY rev DESC LIMIT 1
                """).setParameter("id", id).setParameter("revision", revision).getResultList();
        return names.isEmpty() || names.getFirst() == null ? id.toString() : names.getFirst().toString();
    }

    private List<String> changes(AuditEvent event) {
        String table = TABLES.get(event.entityType());
        List<String> names = columns.computeIfAbsent(event.entityType(), type -> {
            @SuppressWarnings("unchecked")
            List<String> available = entityManager.createNativeQuery("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_name = :table ORDER BY ordinal_position
                    """, String.class).setParameter("table", table.toUpperCase(Locale.ROOT)).getResultList();
            return available.stream()
                    .filter(name -> SAFE_FIELDS.get(type).contains(name.replace("_", "").toLowerCase(Locale.ROOT)))
                    .toList();
        });
        if (names.isEmpty()) {
            return List.of();
        }
        String              id     = "Server Setting".equals(event.entityType()) ? "setting_key" : "id";
        Map<String, Object> before = values(table, id, event.entityId(), event.revision(), false, names);
        Map<String, Object> after  = values(table, id, event.entityId(), event.revision(), true, names);
        if ("User Group".equals(event.entityType())) {
            before.put("memberIds", members("user_group_members_aud", "group_id", "user_id", event, true));
            after.put("memberIds", members("user_group_members_aud", "group_id", "user_id", event, false));
        } else if ("Task".equals(event.entityType())) {
            before.put("predecessors", members("taskdao_relationdao_aud", "task_id", "id", event, true));
            after.put("predecessors", members("taskdao_relationdao_aud", "task_id", "id", event, false));
        }
        return HistoricalFieldChanges.between(before, after);
    }

    private List<UUID> members(String table, String owner, String member, AuditEvent event, boolean before) {
        return entityManager.createNativeQuery("SELECT association." + member + " FROM " + table + " association"
                        + " WHERE association." + owner + " = :id AND association.rev ="
                        + " (SELECT MAX(previous.rev) FROM " + table + " previous"
                        + " WHERE previous." + owner + " = association." + owner
                        + " AND previous." + member + " = association." + member
                        + " AND previous.rev " + (before ? "<" : "<=") + " :revision)"
                        + " AND association.revtype <> 2", UUID.class)
                .setParameter("id", UUID.fromString(event.entityId()))
                .setParameter("revision", event.revision()).getResultList();
    }

    private Map<String, Object> values(String table, String id, String entityId, int revision, boolean exact,
                                       List<String> names) {
        Object key = "setting_key".equals(id) ? entityId : UUID.fromString(entityId);
        if (!exact) {
            List<?> previousTypes = entityManager.createNativeQuery("SELECT revtype FROM " + table
                            + " WHERE " + id + " = :id AND rev < :revision ORDER BY rev DESC LIMIT 1")
                    .setParameter("id", key).setParameter("revision", revision).getResultList();
            if (!previousTypes.isEmpty() && ((Number) previousTypes.getFirst()).intValue() == 2) {
                Map<String, Object> deleted = new LinkedHashMap<>();
                names.forEach(name -> deleted.put(fieldName(name), "<deleted>"));
                return deleted;
            }
        }
        String sql = "SELECT " + String.join(", ", names) + " FROM " + table
                + " WHERE " + id + " = :id AND rev " + (exact ? "= :revision" : "< :revision AND revtype <> 2")
                + " ORDER BY rev DESC LIMIT 1";
        Query query = entityManager.createNativeQuery(sql).setParameter("id", key)
                .setParameter("revision", revision);
        List<?>             rows   = query.getResultList();
        Map<String, Object> result = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Object[] values = names.size() == 1 ? new Object[]{rows.getFirst()} : (Object[]) rows.getFirst();
            for (int index = 0; index < names.size(); index++) {
                Object value = values[index];
                if (value instanceof byte[] bytes && bytes.length == 16) {
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    value = new UUID(buffer.getLong(), buffer.getLong());
                }
                result.put(fieldName(names.get(index)), value);
            }
        }
        return result;
    }

    private String fieldName(String column) {
        String[]      parts = column.toLowerCase(Locale.ROOT).split("_");
        StringBuilder field = new StringBuilder(parts[0]);
        for (int part = 1; part < parts.length; part++) {
            field.append(Character.toUpperCase(parts[part].charAt(0))).append(parts[part].substring(1));
        }
        return "auditValue".contentEquals(field) ? "value" : field.toString();
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
