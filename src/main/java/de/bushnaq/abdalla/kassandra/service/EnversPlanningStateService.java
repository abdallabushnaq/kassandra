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

import de.bushnaq.abdalla.kassandra.audit.AuditRevisionEntity;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Loads immutable planning states from Hibernate Envers revisions.
 */
@Service
public class EnversPlanningStateService {
    @Autowired
    private EntityManager entityManager;

    /**
     * Loads an entity's exact audit row at a revision.
     *
     * @param entityType     audited entity class
     * @param entityId       entity ID
     * @param revisionNumber exact Envers revision number
     * @return the audited state and revision type when the entity changed in that revision
     */
    public Optional<HistoricalState> findAtRevision(Class<?> entityType, UUID entityId, int revisionNumber) {
        List<?> rows = auditReader().createQuery()
                .forRevisionsOfEntity(entityType, false, true)
                .add(AuditEntity.id().eq(entityId))
                .add(AuditEntity.revisionNumber().eq(revisionNumber))
                .add(AuditEntity.revisionProperty("replay").eq(false))
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(state(rows.getFirst()));
    }

    /**
     * Loads the latest audited state strictly before a revision.
     *
     * @param entityType     audited entity class
     * @param entityId       entity ID
     * @param revisionNumber exclusive Envers revision number
     * @return the preceding audited state when it exists
     */
    public Optional<HistoricalState> findBeforeRevision(Class<?> entityType, UUID entityId, int revisionNumber) {
        List<?> rows = auditReader().createQuery()
                .forRevisionsOfEntity(entityType, false, true)
                .add(AuditEntity.id().eq(entityId))
                .add(AuditEntity.revisionNumber().lt(revisionNumber))
                .add(AuditEntity.revisionType().ne(RevisionType.DEL))
                .add(AuditEntity.revisionProperty("replay").eq(false))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(state(rows.getFirst()));
    }

    /**
     * Loads the latest audited entity state at or before a revision.
     *
     * @param entityType     audited entity class
     * @param entityId       entity ID
     * @param revisionNumber inclusive Envers revision number
     * @return the latest audited state when it exists
     */
    public Optional<HistoricalState> findAtOrBeforeRevision(Class<?> entityType, UUID entityId, int revisionNumber) {
        List<?> rows = auditReader().createQuery()
                .forRevisionsOfEntity(entityType, false, true)
                .add(AuditEntity.id().eq(entityId))
                .add(AuditEntity.revisionNumber().le(revisionNumber))
                .add(AuditEntity.revisionType().ne(RevisionType.DEL))
                .add(AuditEntity.revisionProperty("replay").eq(false))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(state(rows.getFirst()));
    }

    /**
     * Lists the predecessor relation IDs associated with a task at a revision.
     *
     * @param taskId         task ID
     * @param revisionNumber inclusive Envers revision number
     * @return relation IDs that belonged to the task at that revision
     */
    public List<UUID> taskPredecessorIdsAtRevision(UUID taskId, int revisionNumber) {
        return entityManager.createNativeQuery("""
                        SELECT association.id
                        FROM taskdao_relationdao_aud association
                        WHERE association.task_id = :taskId
                          AND association.rev = (
                              SELECT MAX(previousAssociation.rev)
                              FROM taskdao_relationdao_aud previousAssociation
                              JOIN audit_revisions previousRevision ON previousRevision.id = previousAssociation.rev
                              WHERE previousAssociation.task_id = association.task_id
                                AND previousAssociation.id = association.id
                                AND previousAssociation.rev <= :revisionNumber
                                AND previousRevision.replay = false
                          )
                          AND association.revtype <> 2
                        """, UUID.class)
                .setParameter("taskId", taskId)
                .setParameter("revisionNumber", revisionNumber)
                .getResultList();
    }

    /**
     * Returns whether a task's predecessor membership changed in a revision.
     *
     * @param taskId         task ID
     * @param revisionNumber exact Envers revision number
     * @return {@code true} when the revision changed a predecessor association
     */
    public boolean hasTaskPredecessorChangeAtRevision(UUID taskId, int revisionNumber) {
        Number changes = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM taskdao_relationdao_aud
                        WHERE task_id = :taskId
                          AND rev = :revisionNumber
                        """)
                .setParameter("taskId", taskId)
                .setParameter("revisionNumber", revisionNumber)
                .getSingleResult();
        return changes.longValue() > 0;
    }

    /**
     * Reserves the Envers revision for the current transaction and returns its generated number.
     *
     * @return the current transaction's Envers revision number
     */
    @SuppressWarnings("deprecation")
    public int currentRevisionNumber() {
        // Hibernate Envers has not provided the documented replacement for this API.
        return auditReader().getCurrentRevision(AuditRevisionEntity.class, true).getId();
    }

    private AuditReader auditReader() {
        return AuditReaderFactory.get(entityManager);
    }

    private HistoricalState state(Object result) {
        Object[] values = (Object[]) result;
        return new HistoricalState(values[0], ((AuditRevisionEntity) values[1]).getId(), (RevisionType) values[2]);
    }

    /**
     * An entity state and its Envers revision type.
     *
     * @param entity         audited entity state; it can be {@code null} for a deletion revision
     * @param revisionNumber Envers revision number that produced the state
     * @param revisionType   Envers change type
     */
    public record HistoricalState(Object entity, int revisionNumber, RevisionType revisionType) {
    }
}
