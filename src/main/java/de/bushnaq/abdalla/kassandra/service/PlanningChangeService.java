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

import de.bushnaq.abdalla.kassandra.audit.AuditOperationContextHolder;
import de.bushnaq.abdalla.kassandra.dao.FeatureDAO;
import de.bushnaq.abdalla.kassandra.dao.ProductDAO;
import de.bushnaq.abdalla.kassandra.dao.RelationDAO;
import de.bushnaq.abdalla.kassandra.dao.SprintDAO;
import de.bushnaq.abdalla.kassandra.dao.TaskDAO;
import de.bushnaq.abdalla.kassandra.dao.UndoableOperationDAO;
import de.bushnaq.abdalla.kassandra.dao.UndoableOperationEntryDAO;
import de.bushnaq.abdalla.kassandra.dao.VersionDAO;
import de.bushnaq.abdalla.kassandra.dao.WorklogDAO;
import de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory;
import de.bushnaq.abdalla.kassandra.repository.FeatureRepository;
import de.bushnaq.abdalla.kassandra.repository.ProductRepository;
import de.bushnaq.abdalla.kassandra.repository.SprintRepository;
import de.bushnaq.abdalla.kassandra.repository.TaskRepository;
import de.bushnaq.abdalla.kassandra.repository.UndoableOperationRepository;
import de.bushnaq.abdalla.kassandra.repository.VersionRepository;
import de.bushnaq.abdalla.kassandra.repository.WorklogRepository;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.util.date.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Records and replays planning-data changes as product-scoped undoable operations.
 */
@Service
@Slf4j
public class PlanningChangeService {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private JsonMapper objectMapper;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UndoableOperationRepository undoableOperationRepository;
    @Autowired
    private VersionRepository versionRepository;
    @Autowired
    private WorklogRepository worklogRepository;

    /**
     * Returns whether the product has a currently applied operation to undo.
     *
     * @param productId the product history to inspect
     * @return {@code true} when undo is available
     */
    public boolean canUndo(UUID productId) {
        return undoableOperationRepository.findFirstByProductIdAndUndoneFalseOrderBySequenceNumberDesc(productId).isPresent();
    }

    /**
     * Returns whether the product has a currently undone operation to redo.
     *
     * @param productId the product history to inspect
     * @return {@code true} when redo is available
     */
    public boolean canRedo(UUID productId) {
        return undoableOperationRepository.findFirstByProductIdAndUndoneTrueOrderBySequenceNumberAsc(productId).isPresent();
    }

    /**
     * Deletes a task, its descendants, and inbound predecessor references as one operation.
     *
     * @param taskId root task ID
     * @param summary user-visible operation description
     * @throws IllegalArgumentException when the task does not exist
     */
    @Transactional
    public void deleteTaskTree(UUID taskId, String summary) {
        TaskDAO rootTask = entityManager.find(TaskDAO.class, taskId);
        if (rootTask == null) {
            throw new IllegalArgumentException("Planning entity does not exist: " + taskId);
        }
        UUID productId = resolveProductId(rootTask);
        List<TaskDAO> tasksToDelete = new java.util.ArrayList<>();
        collectTaskTree(rootTask, tasksToDelete);
        java.util.Set<UUID> taskIds = tasksToDelete.stream().map(TaskDAO::getId).collect(java.util.stream.Collectors.toSet());
        List<TaskDAO> inboundTasks = taskRepository.findByPredecessorIdIn(taskIds).stream()
                .filter(task -> !taskIds.contains(task.getId()))
                .toList();

        UndoableOperationDAO operation = createOperation(productId, "DELETE", summary);
        int restoreOrder = tasksToDelete.size();
        for (TaskDAO task : tasksToDelete) {
            addEntry(operation, TaskDAO.class, task.getId(), snapshot(task), null, restoreOrder--);
        }
        for (TaskDAO inboundTask : inboundTasks) {
            String beforeSnapshot = snapshot(inboundTask);
            inboundTask.getPredecessors().removeIf(relation -> taskIds.contains(relation.getPredecessorId()));
            addEntry(operation, TaskDAO.class, inboundTask.getId(), beforeSnapshot, snapshot(inboundTask), ++restoreOrder);
        }
        tasksToDelete.reversed().forEach(entityManager::remove);
        flushAndClearContext();
    }

    /**
     * Deletes a planning entity and all planning descendants as one operation.
     *
     * @param entityType product, version, feature, or sprint entity class
     * @param id root entity ID
     * @param summary user-visible operation description
     * @throws IllegalArgumentException when the root does not exist or is unsupported
     */
    @Transactional
    public void deleteTree(Class<?> entityType, UUID id, String summary) {
        if (entityType == TaskDAO.class) {
            deleteTaskTree(id, summary);
            return;
        }
        Object root = entityManager.find(entityType, id);
        if (root == null) {
            throw new IllegalArgumentException("Planning entity does not exist: " + id);
        }
        List<Object> entities = new java.util.ArrayList<>();
        collectPlanningTree(root, entities);
        java.util.Set<UUID> taskIds = entities.stream()
                .filter(TaskDAO.class::isInstance)
                .map(TaskDAO.class::cast)
                .map(TaskDAO::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<TaskDAO> inboundTasks = taskIds.isEmpty() ? List.of() : taskRepository.findByPredecessorIdIn(taskIds).stream()
                .filter(task -> !taskIds.contains(task.getId()))
                .toList();
        UndoableOperationDAO operation = createOperation(resolveProductId(root), "DELETE", summary);
        int restoreOrder = entities.size();
        for (Object entity : entities) {
            addEntry(operation, entity.getClass(), entityId(entity), snapshot(entity), null, restoreOrder--);
        }
        for (TaskDAO inboundTask : inboundTasks) {
            String beforeSnapshot = snapshot(inboundTask);
            inboundTask.getPredecessors().removeIf(relation -> taskIds.contains(relation.getPredecessorId()));
            addEntry(operation, TaskDAO.class, inboundTask.getId(), beforeSnapshot, snapshot(inboundTask), ++restoreOrder);
        }
        entities.reversed().forEach(entityManager::remove);
        flushAndClearContext();
    }

    /**
     * Deletes an existing planning entity and records its previous state.
     *
     * @param entityType entity class
     * @param id entity ID
     * @param summary user-visible operation description
     * @throws IllegalArgumentException when the entity does not exist
     */
    @Transactional
    public void delete(Class<?> entityType, UUID id, String summary) {
        Object existing = entityManager.find(entityType, id);
        if (existing == null) {
            throw new IllegalArgumentException("Planning entity does not exist: " + id);
        }
        UUID productId = resolveProductId(existing);
        UndoableOperationDAO operation = createOperation(productId, "DELETE", summary);
        addEntry(operation, entityType, id, snapshot(existing), null, 0);
        entityManager.remove(existing);
        flushAndClearContext();
    }

    /**
     * Returns the product-scoped operation history, newest first.
     *
     * @param productId the product history to retrieve
     * @return immutable operation metadata and snapshots
     */
    public List<UndoableOperationDAO> history(UUID productId) {
        return undoableOperationRepository.findByProductIdOrderBySequenceNumberDesc(productId);
    }

    /**
     * Returns globally ordered history for multiple products.
     *
     * @param productIds products whose history is requested
     * @return operations ordered newest first
     */
    public List<UndoableOperationDAO> history(java.util.Collection<UUID> productIds) {
        return undoableOperationRepository.findByProductIdInOrderByCreatedDesc(productIds);
    }

    /**
     * Returns the names of the entities changed by an operation.
     *
     * @param operation recorded planning operation
     * @return entity type and name descriptions
     */
    public List<UndoRedoHistory.EntityChange> entityChanges(UndoableOperationDAO operation) {
        return operation.getEntries().stream()
                .map(this::entityChange)
                .toList();
    }

    /**
     * Records and persists a newly created planning entity.
     *
     * @param <T> entity type
     * @param entity entity to persist
     * @param summary user-visible operation description
     * @return the managed persisted entity
     */
    @Transactional
    public <T> T persist(T entity, String summary) {
        UUID productId = resolveProductId(entity);
        UUID entityId = entityId(entity);
        UndoableOperationDAO operation = createOperation(productId, "CREATE", summary);
        entityManager.persist(entity);
        addEntry(operation, entity.getClass(), entityId, null, snapshot(entity), 0);
        flushAndClearContext();
        return entity;
    }

    /**
     * Records and persists a batch of new planning entities as one product operation.
     *
     * @param <T> entity type
     * @param entities entities to persist
     * @param summary user-visible operation description
     * @return the persisted entities
     * @throws IllegalArgumentException when entities are empty or belong to different products
     */
    @Transactional
    public <T> List<T> persistBatch(List<T> entities, String summary) {
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("Cannot record an empty planning batch");
        }
        UUID productId = resolveProductId(entities.getFirst());
        if (entities.stream().anyMatch(entity -> !productId.equals(resolveProductId(entity)))) {
            throw new IllegalArgumentException("A planning batch must belong to one product");
        }
        UndoableOperationDAO operation = createOperation(productId, "CREATE", summary);
        int restoreOrder = 0;
        for (T entity : entities) {
            entityManager.persist(entity);
            addEntry(operation, entity.getClass(), entityId(entity), null, snapshot(entity), restoreOrder++);
        }
        flushAndClearContext();
        return entities;
    }

    /**
     * Reapplies the latest undone operation for a product.
     *
     * @param productId product history to redo
     * @throws IllegalStateException when no redo operation is available
     */
    @Transactional
    public void redo(UUID productId) {
        lockProduct(productId);
        UndoableOperationDAO operation = undoableOperationRepository.findFirstByProductIdAndUndoneTrueOrderBySequenceNumberAsc(productId)
                .orElseThrow(() -> new IllegalStateException("No redo operation is available"));
        replayRedo(operation);
    }

    /**
     * Reapplies all consecutive undone operations through the selected operation.
     *
     * @param productId product history to redo
     * @param operationId last operation to reapply
     * @throws IllegalArgumentException when the selected operation is not available to redo
     */
    @Transactional
    public void redoThrough(UUID productId, UUID operationId) {
        lockProduct(productId);
        List<UndoableOperationDAO> operations = undoableOperationRepository.findByProductIdOrderBySequenceNumberDesc(productId).stream()
                .filter(UndoableOperationDAO::isUndone)
                .sorted(Comparator.comparingLong(UndoableOperationDAO::getSequenceNumber))
                .toList();
        int targetIndex = indexOfOperation(operations, operationId);
        for (int index = 0; index <= targetIndex; index++) {
            replayRedo(operations.get(index));
        }
    }

    private void replayRedo(UndoableOperationDAO operation) {
        logOperation("Redoing", operation);
        AuditOperationContextHolder.setOperationId(operation.getId());
        try {
            operation.getEntries().stream()
                    .sorted(Comparator.comparingInt(UndoableOperationEntryDAO::getRestoreOrder))
                    .forEach(entry -> restore(entry, entry.getAfterSnapshot()));
            operation.setUndone(false);
            entityManager.flush();
        } finally {
            AuditOperationContextHolder.clear();
        }
    }

    /**
     * Records and persists an update to an existing planning entity.
     *
     * @param <T> entity type
     * @param entity detached entity containing the replacement state
     * @param summary user-visible operation description
     * @return the managed replacement entity
     * @throws IllegalArgumentException when the entity does not exist or moves to another product
     */
    @Transactional
    public <T> T update(T entity, String summary) {
        UUID entityId = entityId(entity);
        Object existing = entityManager.find(entity.getClass(), entityId);
        if (existing == null) {
            throw new IllegalArgumentException("Planning entity does not exist: " + entityId);
        }

        UUID productId = resolveProductId(existing);
        if (!productId.equals(resolveProductId(entity))) {
            throw new IllegalArgumentException("Moving planning data between products is not supported");
        }
        UndoableOperationDAO operation = createOperation(productId, "UPDATE", summary);
        addEntry(operation, entity.getClass(), entityId, snapshot(existing), snapshot(entity), 0);
        T merged = entityManager.merge(entity);
        flushAndClearContext();
        return merged;
    }

    /**
     * Records and persists a batch of updates as one product operation.
     *
     * @param <T> entity type
     * @param entities detached replacement entities
     * @param summary user-visible operation description
     * @return the managed replacement entities
     * @throws IllegalArgumentException when an entity does not exist or belongs to another product
     */
    @Transactional
    public <T> List<T> updateBatch(List<T> entities, String summary) {
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("Cannot record an empty planning batch");
        }
        Object firstExisting = entityManager.find(entities.getFirst().getClass(), entityId(entities.getFirst()));
        if (firstExisting == null) {
            throw new IllegalArgumentException("Planning entity does not exist: " + entityId(entities.getFirst()));
        }
        UUID productId = resolveProductId(firstExisting);
        UndoableOperationDAO operation = createOperation(productId, "BATCH_UPDATE", summary);
        List<T> merged = new java.util.ArrayList<>(entities.size());
        int restoreOrder = 0;
        for (T entity : entities) {
            Object existing = entityManager.find(entity.getClass(), entityId(entity));
            if (existing == null || !productId.equals(resolveProductId(existing))
                    || !productId.equals(resolveProductId(entity))) {
                throw new IllegalArgumentException("A planning batch must update existing entities of one product");
            }
            addEntry(operation, entity.getClass(), entityId(entity), snapshot(existing), snapshot(entity), restoreOrder++);
            merged.add(entityManager.merge(entity));
        }
        flushAndClearContext();
        return merged;
    }

    /**
     * Records an in-place mutation of an existing entity.
     *
     * @param <T> entity type
     * @param entityType entity class
     * @param id entity ID
     * @param summary user-visible operation description
     * @param mutation mutation to apply to the managed entity
     * @throws IllegalArgumentException when the entity does not exist
     */
    @Transactional
    public <T> void update(Class<T> entityType, UUID id, String summary, Consumer<T> mutation) {
        T entity = entityManager.find(entityType, id);
        if (entity == null) {
            throw new IllegalArgumentException("Planning entity does not exist: " + id);
        }
        UndoableOperationDAO operation = createOperation(resolveProductId(entity), "UPDATE", summary);
        String beforeSnapshot = snapshot(entity);
        mutation.accept(entity);
        addEntry(operation, entityType, id, beforeSnapshot, snapshot(entity), 0);
        flushAndClearContext();
    }

    /**
     * Reverts the latest applied operation for a product.
     *
     * @param productId product history to undo
     * @throws IllegalStateException when no undo operation is available
     */
    @Transactional
    public void undo(UUID productId) {
        lockProduct(productId);
        UndoableOperationDAO operation = undoableOperationRepository.findFirstByProductIdAndUndoneFalseOrderBySequenceNumberDesc(productId)
                .orElseThrow(() -> new IllegalStateException("No undo operation is available"));
        replayUndo(operation);
    }

    /**
     * Reverts all consecutive applied operations through the selected operation.
     *
     * @param productId product history to undo
     * @param operationId oldest operation to revert
     * @throws IllegalArgumentException when the selected operation is not available to undo
     */
    @Transactional
    public void undoThrough(UUID productId, UUID operationId) {
        lockProduct(productId);
        List<UndoableOperationDAO> operations = undoableOperationRepository.findByProductIdOrderBySequenceNumberDesc(productId).stream()
                .filter(operation -> !operation.isUndone())
                .toList();
        int targetIndex = indexOfOperation(operations, operationId);
        for (int index = 0; index <= targetIndex; index++) {
            replayUndo(operations.get(index));
        }
    }

    private int indexOfOperation(List<UndoableOperationDAO> operations, UUID operationId) {
        for (int index = 0; index < operations.size(); index++) {
            if (operations.get(index).getId().equals(operationId)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Planning operation is not available for replay: " + operationId);
    }

    private void replayUndo(UndoableOperationDAO operation) {
        logOperation("Undoing", operation);
        AuditOperationContextHolder.setOperationId(operation.getId());
        try {
            operation.getEntries().stream()
                    .sorted(Comparator.comparingInt(UndoableOperationEntryDAO::getRestoreOrder).reversed())
                    .forEach(entry -> restore(entry, entry.getBeforeSnapshot()));
            operation.setUndone(true);
            entityManager.flush();
        } finally {
            AuditOperationContextHolder.clear();
        }
    }

    private void addEntry(UndoableOperationDAO operation, Class<?> entityType, UUID entityId, String beforeSnapshot,
            String afterSnapshot, int restoreOrder) {
        UndoableOperationEntryDAO entry = new UndoableOperationEntryDAO();
        entry.setEntityType(entityType.getName());
        entry.setEntityId(entityId);
        entry.setBeforeSnapshot(beforeSnapshot);
        entry.setAfterSnapshot(afterSnapshot);
        entry.setRestoreOrder(restoreOrder);
        operation.addEntry(entry);
    }

    private UndoRedoHistory.EntityChange entityChange(UndoableOperationEntryDAO entry) {
        String snapshot = entry.getAfterSnapshot() != null ? entry.getAfterSnapshot() : entry.getBeforeSnapshot();
        String entityType = entry.getEntityType().substring(entry.getEntityType().lastIndexOf('.') + 1)
                .replace("DAO", "");
        UndoRedoHistory.EntityChange change = new UndoRedoHistory.EntityChange();
        change.setAction(entry.getBeforeSnapshot() == null ? "Created"
                : entry.getAfterSnapshot() == null ? "Deleted" : "Updated");
        change.setEntityType(entityType);
        if (snapshot == null) {
            change.setDisplayName(entry.getEntityId().toString());
            return change;
        }
        try {
            java.util.Map<?, ?> values = objectMapper.readValue(snapshot, java.util.Map.class);
            if (WorklogDAO.class.getName().equals(entry.getEntityType())) {
                change.setDisplayName(worklogDisplayName(values, entry.getEntityId()));
                return change;
            }
            Object name = values.get("name");
            change.setDisplayName(name == null ? entry.getEntityId().toString() : name.toString());
        } catch (JacksonException exception) {
            log.warn("Could not read the snapshot name for {} {}", entityType, entry.getEntityId(), exception);
            change.setDisplayName(entry.getEntityId().toString());
        }
        return change;
    }

    private String worklogDisplayName(java.util.Map<?, ?> values, UUID entityId) {
        Object start = values.get("start");
        Object timeSpent = values.get("timeSpent");
        if (!(start instanceof String startValue) || !(timeSpent instanceof String timeSpentValue)) {
            return entityId.toString();
        }
        try {
            LocalDateTime startTime = LocalDateTime.parse(startValue);
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(startTime) + " - "
                    + DateUtil.createDurationString(java.time.Duration.parse(timeSpentValue), false, true, false);
        } catch (RuntimeException exception) {
            log.warn("Could not format worklog {} for history display", entityId, exception);
            return entityId.toString();
        }
    }

    private void logOperation(String action, UndoableOperationDAO operation) {
        log.info("{} operation {} by {}: {}", action, operation.getId(), operation.getActor(), operation.getSummary());
        operation.getEntries().forEach(entry -> {
            UndoRedoHistory.EntityChange change = entityChange(entry);
            log.info("{} {} {} ({})", action, change.getEntityType(), change.getDisplayName(), entry.getEntityId());
        });
    }

    private UndoableOperationDAO createOperation(UUID productId, String kind, String summary) {
        lockProduct(productId);
        UUID clientOperationId = AuditOperationContextHolder.getOperationId();
        if (clientOperationId != null) {
            UndoableOperationDAO existingOperation = undoableOperationRepository.findById(clientOperationId).orElse(null);
            if (existingOperation != null) {
                if (!productId.equals(existingOperation.getProductId())) {
                    throw new IllegalArgumentException("A planning operation cannot span products");
                }
                return existingOperation;
            }
        }
        undoableOperationRepository.findByProductIdOrderBySequenceNumberDesc(productId).stream()
                .filter(UndoableOperationDAO::isUndone)
                .forEach(undoableOperationRepository::delete);
        UndoableOperationDAO operation = new UndoableOperationDAO();
        if (clientOperationId != null) {
            operation.setId(clientOperationId);
        }
        operation.setProductId(productId);
        operation.setKind(kind);
        operation.setSummary(summary);
        operation.setActor(SecurityUtils.getUserEmail());
        operation.setCreated(OffsetDateTime.now());
        operation.setSequenceNumber(undoableOperationRepository.findMaxSequenceNumber(productId) + 1);
        entityManager.persist(operation);
        AuditOperationContextHolder.setOperationId(operation.getId());
        return operation;
    }

    private UUID entityId(Object entity) {
        if (entity instanceof ProductDAO product) {
            return product.getId();
        }
        if (entity instanceof VersionDAO version) {
            return version.getId();
        }
        if (entity instanceof FeatureDAO feature) {
            return feature.getId();
        }
        if (entity instanceof SprintDAO sprint) {
            return sprint.getId();
        }
        if (entity instanceof TaskDAO task) {
            return task.getId();
        }
        if (entity instanceof WorklogDAO worklog) {
            return worklog.getId();
        }
        if (entity instanceof RelationDAO relation) {
            return relation.getId();
        }
        throw new IllegalArgumentException("Unsupported planning entity: " + entity.getClass().getName());
    }

    private void flushAndClearContext() {
        try {
            entityManager.flush();
        } finally {
            AuditOperationContextHolder.clear();
        }
    }

    private void lockProduct(UUID productId) {
        if (productRepository.existsById(productId)) {
            productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product does not exist: " + productId));
        }
    }

    private UUID resolveProductId(Object entity) {
        if (entity instanceof ProductDAO product) {
            return product.getId();
        }
        if (entity instanceof VersionDAO version) {
            return version.getProductId();
        }
        if (entity instanceof FeatureDAO feature) {
            return versionRepository.findById(feature.getVersionId())
                    .orElseThrow(() -> new IllegalArgumentException("Version does not exist: " + feature.getVersionId()))
                    .getProductId();
        }
        if (entity instanceof SprintDAO sprint) {
            return resolveProductId(featureRepository.findById(sprint.getFeatureId())
                    .orElseThrow(() -> new IllegalArgumentException("Feature does not exist: " + sprint.getFeatureId())));
        }
        if (entity instanceof TaskDAO task) {
            return resolveProductId(sprintRepository.findById(task.getSprintId())
                    .orElseThrow(() -> new IllegalArgumentException("Sprint does not exist: " + task.getSprintId())));
        }
        if (entity instanceof WorklogDAO worklog) {
            return resolveProductId(sprintRepository.findById(worklog.getSprintId())
                    .orElseThrow(() -> new IllegalArgumentException("Sprint does not exist: " + worklog.getSprintId())));
        }
        throw new IllegalArgumentException("Unsupported planning entity: " + entity.getClass().getName());
    }

    private void restore(UndoableOperationEntryDAO entry, String snapshot) {
        Class<?> entityType = entityType(entry.getEntityType());
        Object existing = entityManager.find(entityType, entry.getEntityId());
        if (snapshot == null) {
            if (existing != null) {
                entityManager.remove(existing);
            }
            return;
        }
        try {
            Object restored = objectMapper.readValue(snapshot, entityType);
            revive(entityType, entry.getEntityId());
            if (restored instanceof TaskDAO task) {
                task.getPredecessors().forEach(relation -> revive(RelationDAO.class, relation.getId()));
            }
            entityManager.merge(restored);
        } catch (JacksonException e) {
            throw new IllegalStateException("Cannot restore planning-history snapshot", e);
        }
    }

    private void revive(Class<?> entityType, UUID entityId) {
        entityManager.createNativeQuery("UPDATE " + tableName(entityType) + " SET deleted = false, deleted_at = NULL WHERE id = :id")
                .setParameter("id", entityId)
                .executeUpdate();
    }

    private String snapshot(Object entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (JacksonException e) {
            throw new IllegalStateException("Cannot capture planning-history snapshot", e);
        }
    }

    private void collectTaskTree(TaskDAO task, List<TaskDAO> tasks) {
        tasks.add(task);
        taskRepository.findByParentTaskId(task.getId()).forEach(child -> collectTaskTree(child, tasks));
    }

    private void collectPlanningTree(Object root, List<Object> entities) {
        entities.add(root);
        if (root instanceof ProductDAO product) {
            versionRepository.findByProductId(product.getId())
                    .forEach(version -> collectPlanningTree(version, entities));
        } else if (root instanceof VersionDAO version) {
            featureRepository.findByVersionId(version.getId())
                    .forEach(feature -> collectPlanningTree(feature, entities));
        } else if (root instanceof FeatureDAO feature) {
            sprintRepository.findByFeatureId(feature.getId())
                    .forEach(sprint -> collectPlanningTree(sprint, entities));
        } else if (root instanceof SprintDAO sprint) {
            java.util.Set<UUID> childTaskIds = taskRepository.findBySprintId(sprint.getId()).stream()
                    .map(TaskDAO::getParentTaskId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            taskRepository.findBySprintId(sprint.getId()).stream()
                    .filter(task -> !childTaskIds.contains(task.getId()))
                    .forEach(task -> collectTaskTreeAsObjects(task, entities));
            entities.addAll(worklogRepository.findBySprintId(sprint.getId()));
        } else {
            throw new IllegalArgumentException("Unsupported planning tree root: " + root.getClass().getName());
        }
    }

    private void collectTaskTreeAsObjects(TaskDAO task, List<Object> entities) {
        entities.add(task);
        taskRepository.findByParentTaskId(task.getId()).forEach(child -> collectTaskTreeAsObjects(child, entities));
    }

    private Class<?> entityType(String entityType) {
        return switch (entityType) {
        case "de.bushnaq.abdalla.kassandra.dao.ProductDAO" -> ProductDAO.class;
        case "de.bushnaq.abdalla.kassandra.dao.VersionDAO" -> VersionDAO.class;
        case "de.bushnaq.abdalla.kassandra.dao.FeatureDAO" -> FeatureDAO.class;
        case "de.bushnaq.abdalla.kassandra.dao.SprintDAO" -> SprintDAO.class;
        case "de.bushnaq.abdalla.kassandra.dao.TaskDAO" -> TaskDAO.class;
        case "de.bushnaq.abdalla.kassandra.dao.WorklogDAO" -> WorklogDAO.class;
        case "de.bushnaq.abdalla.kassandra.dao.RelationDAO" -> RelationDAO.class;
        default -> throw new IllegalArgumentException("Unsupported planning entity type: " + entityType);
        };
    }

    private String tableName(Class<?> entityType) {
        if (entityType == ProductDAO.class) {
            return "products";
        }
        if (entityType == VersionDAO.class) {
            return "versions";
        }
        if (entityType == FeatureDAO.class) {
            return "features";
        }
        if (entityType == SprintDAO.class) {
            return "sprints";
        }
        if (entityType == TaskDAO.class) {
            return "tasks";
        }
        if (entityType == WorklogDAO.class) {
            return "worklogs";
        }
        if (entityType == RelationDAO.class) {
            return "relations";
        }
        throw new IllegalArgumentException("Unsupported planning entity: " + entityType.getName());
    }
}
