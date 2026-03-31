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

package de.bushnaq.abdalla.kassandra.repository;

import de.bushnaq.abdalla.kassandra.dao.TaskDAO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TaskRepository extends ListCrudRepository<TaskDAO, Long> {
    List<TaskDAO> findAllByOrderByOrderIdAsc();

    List<TaskDAO> findBySprintId(Long sprintId);

    List<TaskDAO> findBySprintIdOrderByOrderIdAsc(Long sprintId);

    /**
     * Find all tasks that are direct children of the given parent task.
     *
     * @param parentTaskId the ID of the parent task
     * @return list of child tasks
     */
    List<TaskDAO> findByParentTaskId(Long parentTaskId);

    /**
     * Find all tasks that have at least one predecessor relation pointing to one of the given task IDs.
     * Used to clean up inbound dependency references before deleting a set of tasks.
     *
     * @param ids the set of task IDs that are being deleted
     * @return tasks with at least one matching predecessor entry
     */
    @Query("SELECT DISTINCT t FROM TaskDAO t JOIN t.predecessors r WHERE r.predecessorId IN :ids")
    List<TaskDAO> findByPredecessorIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT COALESCE(MAX(t.orderId), -1) FROM TaskDAO t where sprintId=:sprintId")
    Integer findMaxOrderId(Long sprintId);
}