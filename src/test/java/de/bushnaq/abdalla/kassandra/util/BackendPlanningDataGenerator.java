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

package de.bushnaq.abdalla.kassandra.util;

import de.bushnaq.abdalla.kassandra.dao.FeatureDAO;
import de.bushnaq.abdalla.kassandra.dao.ProductDAO;
import de.bushnaq.abdalla.kassandra.dao.SprintDAO;
import de.bushnaq.abdalla.kassandra.dao.TaskDAO;
import de.bushnaq.abdalla.kassandra.dao.VersionDAO;
import de.bushnaq.abdalla.kassandra.dto.Status;
import de.bushnaq.abdalla.kassandra.dto.TaskMode;
import de.bushnaq.abdalla.kassandra.service.PlanningChangeService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Creates realistic planning DAO graphs directly through the undoable backend service.
 */
public class BackendPlanningDataGenerator {
    private final PlanningChangeService planningChangeService;

    /**
     * Creates a generator that persists entities through the undoable backend service.
     *
     * @param planningChangeService service used to persist fixture data
     */
    public BackendPlanningDataGenerator(PlanningChangeService planningChangeService) {
        this.planningChangeService = planningChangeService;
    }

    /**
     * Creates a product hierarchy with scheduled tasks and realistic effort values.
     *
     * @return generated planning hierarchy
     */
    public PlanningData createPlanningData() {
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
        task.setProgress(0.2f);
        task.setTaskMode(TaskMode.MANUALLY_SCHEDULED);
        task.setImpactOnCost(true);
        task.setCritical(true);
        return task;
    }

    /**
     * Generated planning hierarchy.
     *
     * @param product product
     * @param version version
     * @param feature feature
     * @param sprint sprint
     * @param task parent task
     * @param childTask child task
     */
    public record PlanningData(ProductDAO product, VersionDAO version, FeatureDAO feature, SprintDAO sprint, TaskDAO task,
            TaskDAO childTask) {
    }
}
