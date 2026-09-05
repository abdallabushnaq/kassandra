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

import de.bushnaq.abdalla.kassandra.dao.*;
import de.bushnaq.abdalla.kassandra.dto.Status;
import de.bushnaq.abdalla.kassandra.dto.TaskMode;
import de.bushnaq.abdalla.kassandra.repository.FeatureRepository;
import de.bushnaq.abdalla.kassandra.repository.ProductRepository;
import de.bushnaq.abdalla.kassandra.repository.SprintRepository;
import de.bushnaq.abdalla.kassandra.repository.TaskRepository;
import de.bushnaq.abdalla.kassandra.repository.VersionRepository;
import de.bushnaq.abdalla.kassandra.repository.WorklogRepository;
import de.bushnaq.abdalla.kassandra.util.AbstractTestUtil;
import de.bushnaq.abdalla.kassandra.util.BackendPlanningDataGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests product-scoped planning history and its soft-delete replay behavior.
 */
@Tag("UnitTest")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class PlanningChangeServiceTest extends AbstractTestUtil {
    @Autowired
    private PlanningChangeService planningChangeService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private VersionRepository versionRepository;
    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private WorklogRepository worklogRepository;
    @Autowired
    private JsonMapper jsonMapper;

    /**
     * Retains a deleted product as a tombstone and restores it through undo.
     */
    @Test
    @WithMockUser(username = "history-user", roles = "ADMIN")
    public void deleteUndoRedoRetainsTombstoneAndAuditHistory() {
        ProductDAO product = new ProductDAO();
        product.setName("History product");
        UUID productId = product.getId();
        planningChangeService.persist(product, "Created product");

        product.setName("Renamed history product");
        planningChangeService.update(product, "Renamed product");
        planningChangeService.deleteTree(ProductDAO.class, productId, "Deleted product hierarchy");

        assertTrue(productRepository.findById(productId).isEmpty());
        assertEquals(1, ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM products WHERE id = :id")
                .setParameter("id", productId)
                .getSingleResult()).intValue());
        assertEquals(Boolean.TRUE, entityManager.createNativeQuery("SELECT deleted FROM products WHERE id = :id")
                .setParameter("id", productId)
                .getSingleResult());

        planningChangeService.undo(productId);
        assertEquals(Boolean.FALSE, entityManager.createNativeQuery("SELECT deleted FROM products WHERE id = :id")
                .setParameter("id", productId)
                .getSingleResult());
        assertEquals("Renamed history product", productRepository.findById(productId).orElseThrow().getName());
        assertTrue(planningChangeService.canRedo(productId));

        planningChangeService.redo(productId);
        assertTrue(productRepository.findById(productId).isEmpty());
        assertFalse(planningChangeService.canRedo(productId));
        assertEquals(3, planningChangeService.history(productId).size());
        assertTrue(((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM audit_revisions").getSingleResult()).intValue() >= 5);
    }

    /**
     * Undoes and redoes worklog creation, update, and deletion.
     */
    @Test
    @WithMockUser(username = "history-user", roles = "ADMIN")
    public void worklogAddUpdateAndDeleteUndo() {
        PlanningData data = createPlanningData();
        // Add Worklog
        WorklogDAO worklog = new WorklogDAO();
        worklog.setAuthorId(UUID.randomUUID());
        worklog.setSprintId(data.sprint().getId());
        worklog.setTaskId(data.task().getId());
        worklog.setStart(LocalDateTime.of(2026, 1, 5, 9, 0));
        worklog.setTimeSpent(Duration.ofHours(2));
        worklog.setTimeRemainingEstimate(Duration.ofHours(6));
        worklog.setComment("initial");
        planningChangeService.persist(worklog, "Created worklog");
        planningChangeService.undo(data.product().getId());
        assertTrue(worklogRepository.findById(worklog.getId()).isEmpty());
        planningChangeService.redo(data.product().getId());

        // Update Worklog
        worklog.setComment("updated");
        planningChangeService.update(worklog, "Updated worklog");
        planningChangeService.undo(data.product().getId());
        assertEquals("initial", worklogRepository.findById(worklog.getId()).orElseThrow().getComment());

        // Delete Worklog
        planningChangeService.delete(WorklogDAO.class, worklog.getId(), "Deleted worklog");
        planningChangeService.undo(data.product().getId());
        assertTrue(worklogRepository.findById(worklog.getId()).isPresent());
    }

    /**
     * Undoes and redoes relation add, update, and removal through the owning task.
     */
    @Test
    @WithMockUser(username = "history-user", roles = "ADMIN")
    public void relationAddUpdateAndDeleteUndo() {
        PlanningData data = createPlanningData();
        // Add Relation
        RelationDAO relation = new RelationDAO();
        relation.setPredecessorId(data.task().getId());
        relation.setVisible(true);
        data.childTask().getPredecessors().add(relation);
        TaskDAO child = planningChangeService.update(data.childTask(), "Added relation");
        planningChangeService.undo(data.product().getId());
        assertTrue(taskRepository.findById(child.getId()).orElseThrow().getPredecessors().isEmpty());
        planningChangeService.redo(data.product().getId());

        // Update Relation
        child = taskRepository.findById(child.getId()).orElseThrow();
        child.getPredecessors().getFirst().setVisible(false);
        child = planningChangeService.update(child, "Updated relation");
        planningChangeService.undo(data.product().getId());
        assertTrue(taskRepository.findById(child.getId()).orElseThrow().getPredecessors().getFirst().getVisible());

        // Delete Relation
        child = taskRepository.findById(child.getId()).orElseThrow();
        child.getPredecessors().clear();
        planningChangeService.update(child, "Deleted relation");
        planningChangeService.undo(data.product().getId());
        assertEquals(1, taskRepository.findById(child.getId()).orElseThrow().getPredecessors().size());
    }

    /**
     * Restores a deleted task subtree and reverts a batch task-date move.
     */
    @Test
    @WithMockUser(username = "history-user", roles = "ADMIN")
    public void taskDeleteAndTaskTreeDateMoveUndo() {
        PlanningData data = createPlanningData();
        // Update Task and Subtasks
        LocalDateTime originalParentStart = data.task().getStart();
        LocalDateTime originalChildStart = data.childTask().getStart();
        data.task().setStart(originalParentStart.plusDays(3));
        data.childTask().setStart(originalChildStart.plusDays(3));
        planningChangeService.updateBatch(List.of(data.task(), data.childTask()), "Moved task tree");
        planningChangeService.undo(data.product().getId());
        assertEquals(originalParentStart, taskRepository.findById(data.task().getId()).orElseThrow().getStart());
        assertEquals(originalChildStart, taskRepository.findById(data.childTask().getId()).orElseThrow().getStart());

        // Delete Task Tree
        planningChangeService.deleteTaskTree(data.task().getId(), "Deleted task hierarchy");
        assertTrue(taskRepository.findById(data.task().getId()).isEmpty());
        assertTrue(taskRepository.findById(data.childTask().getId()).isEmpty());
        planningChangeService.undo(data.product().getId());
        assertTrue(taskRepository.findById(data.task().getId()).isPresent());
        assertTrue(taskRepository.findById(data.childTask().getId()).isPresent());
    }

    /**
     * Restores the complete previous task state after two consecutive undo operations.
     *
     * @throws Exception if a DAO snapshot cannot be serialized
     */
    @Test
    @WithMockUser(username = "history-user", roles = "ADMIN")
    public void twoConsecutiveUndosRestoreCompleteTaskStates() throws Exception {
        BackendPlanningDataGenerator.PlanningData data = new BackendPlanningDataGenerator(planningChangeService).createPlanningData();
        TaskDAO original = copy(data.task(), TaskDAO.class);
        data.task().setName("First update");
        data.task().setMinEstimate(Duration.ofHours(36));
        data.task().setMaxEstimate(Duration.ofHours(56));
        data.task().setDuration(Duration.ofDays(7));
        planningChangeService.update(data.task(), "First task update");
        TaskDAO firstUpdate = copy(taskRepository.findById(data.task().getId()).orElseThrow(), TaskDAO.class);
        data.task().setName("Second update");
        data.task().setRemainingEstimate(Duration.ofHours(12));
        data.task().setProgress(0.7f);
        planningChangeService.update(data.task(), "Second task update");

        planningChangeService.undo(data.product().getId());
        assertDaoState(firstUpdate, taskRepository.findById(data.task().getId()).orElseThrow());
        planningChangeService.undo(data.product().getId());
        assertDaoState(original, taskRepository.findById(data.task().getId()).orElseThrow());
    }

    /**
     * Restores each parent aggregate after an update and complete subtree deletion.
     */
    @Test
    @WithMockUser(username = "history-user", roles = "ADMIN")
    public void sprintFeatureVersionAndProductAddUpdateDeleteUndo() {
        PlanningData data = createPlanningData();
        // Update and Delete Sprint
        data.sprint().setStart(data.sprint().getStart().plusDays(2));
        planningChangeService.update(data.sprint(), "Moved sprint");
        planningChangeService.undo(data.product().getId());
        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0), sprintRepository.findById(data.sprint().getId()).orElseThrow().getStart());
        planningChangeService.deleteTree(SprintDAO.class, data.sprint().getId(), "Deleted sprint hierarchy");
        planningChangeService.undo(data.product().getId());
        assertTrue(sprintRepository.findById(data.sprint().getId()).isPresent());
        // Update and Delete Feature
        data.feature().setName("Renamed feature");
        planningChangeService.update(data.feature(), "Updated feature");
        planningChangeService.undo(data.product().getId());
        assertEquals("Feature", featureRepository.findById(data.feature().getId()).orElseThrow().getName());
        planningChangeService.deleteTree(FeatureDAO.class, data.feature().getId(), "Deleted feature hierarchy");
        planningChangeService.undo(data.product().getId());
        assertTrue(featureRepository.findById(data.feature().getId()).isPresent());
        // Update and Delete Version
        data.version().setName("2.0");
        planningChangeService.update(data.version(), "Updated version");
        planningChangeService.undo(data.product().getId());
        assertEquals("1.0", versionRepository.findById(data.version().getId()).orElseThrow().getName());
        planningChangeService.deleteTree(VersionDAO.class, data.version().getId(), "Deleted version hierarchy");
        planningChangeService.undo(data.product().getId());
        assertTrue(versionRepository.findById(data.version().getId()).isPresent());
        // Delete Product
        planningChangeService.deleteTree(ProductDAO.class, data.product().getId(), "Deleted product hierarchy");
        planningChangeService.undo(data.product().getId());
        assertTrue(productRepository.findById(data.product().getId()).isPresent());
    }

    private PlanningData createPlanningData() {
        ProductDAO product = new ProductDAO();
        product.setName("Product");
        planningChangeService.persist(product, "Created product");
        VersionDAO version = new VersionDAO();
        version.setProductId(product.getId());
        version.setName("1.0");
        planningChangeService.persist(version, "Created version");
        FeatureDAO feature = new FeatureDAO();
        feature.setVersionId(version.getId());
        feature.setName("Feature");
        planningChangeService.persist(feature, "Created feature");
        SprintDAO sprint = new SprintDAO();
        sprint.setFeatureId(feature.getId());
        sprint.setName("Sprint");
        sprint.setStatus(Status.CREATED);
        sprint.setStart(LocalDateTime.of(2026, 1, 1, 9, 0));
        sprint.setEnd(LocalDateTime.of(2026, 1, 14, 17, 0));
        sprint.setOriginalEstimation(Duration.ofHours(120));
        sprint.setRemaining(Duration.ofHours(120));
        sprint.setWorked(Duration.ZERO);
        planningChangeService.persist(sprint, "Created sprint");
        TaskDAO task = task(sprint.getId(), null, "Task", LocalDateTime.of(2026, 1, 2, 9, 0));
        planningChangeService.persist(task, "Created task");
        TaskDAO child = task(sprint.getId(), task.getId(), "Child task", LocalDateTime.of(2026, 1, 3, 9, 0));
        planningChangeService.persist(child, "Created child task");
        return new PlanningData(product, version, feature, sprint, task, child);
    }

    private TaskDAO task(UUID sprintId, UUID parentTaskId, String name, LocalDateTime start) {
        TaskDAO task = new TaskDAO();
        task.setSprintId(sprintId);
        task.setParentTaskId(parentTaskId);
        task.setName(name);
        task.setStart(start);
        task.setFinish(start.plusDays(5));
        task.setDuration(Duration.ofDays(5));
        task.setMinEstimate(Duration.ofHours(32));
        task.setMaxEstimate(Duration.ofHours(48));
        task.setRemainingEstimate(Duration.ofHours(40));
        task.setTimeSpent(Duration.ofHours(8));
        task.setImpactOnCost(true);
        task.setCritical(true);
        task.setProgress(0.0f);
        task.setTaskMode(TaskMode.MANUALLY_SCHEDULED);
        return task;
    }

    private <T> T copy(T source, Class<T> entityType) throws Exception {
        return jsonMapper.readValue(jsonMapper.writeValueAsString(source), entityType);
    }

    private void assertDaoState(Object expected, Object actual) throws Exception {
        assertEquals(jsonMapper.writeValueAsString(expected), jsonMapper.writeValueAsString(actual));
    }

    private record PlanningData(ProductDAO product, VersionDAO version, FeatureDAO feature, SprintDAO sprint, TaskDAO task,
            TaskDAO childTask) {
    }
}
