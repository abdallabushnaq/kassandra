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
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.util.MpxjUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.mpxj.*;
import net.sf.mpxj.mspdi.MSPDIWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for exporting sprint data to various file formats.
 * <p>
 * Supported formats:
 * <ul>
 *     <li><b>JSON</b> – a simple container with {@code users}, {@code sprint}, and {@code tasks} keys,
 *     following the same structure used by the Gantt regression tests.</li>
 *     <li><b>MSPDI XML</b> – Microsoft Project XML format, produced via the MPXJ library.
 *     The export mirrors the inverse of {@code MPXJReader.load}: users become resources,
 *     tasks become MPXJ tasks with hierarchy and predecessor relations preserved,
 *     and work estimates are written as resource-assignment work values.</li>
 * </ul>
 */
@Service
@Slf4j
public class SprintExportService {

    @Autowired
    private JsonMapper jsonMapper;

    /**
     * Exports the sprint to MSPDI XML format (Microsoft Project XML) using the MPXJ library.
     * <p>
     * The conversion mirrors the inverse of {@code MPXJReader.load}:
     * <ul>
     *     <li>Users are written as project resources.</li>
     *     <li>Tasks are written with their parent/child hierarchy preserved.</li>
     *     <li>Resource assignments carry the {@code minEstimate} value as work.</li>
     *     <li>Predecessor (finish-to-start) relations are written for each task dependency.</li>
     * </ul>
     *
     * @param sprint the fully initialised sprint (users, tasks, and worklogs already loaded)
     * @return raw bytes of the MSPDI XML document
     * @throws Exception if the MPXJ writer encounters an error
     */
    public byte[] exportToMspdiXml(Sprint sprint) throws Exception {
        ProjectFile projectFile = new ProjectFile();

        // Project properties
        ProjectProperties props = projectFile.getProjectProperties();
        props.setName(sprint.getName());
        if (sprint.getStart() != null) {
            props.setStartDate(sprint.getStart());
        }

        // Users → MPXJ resources
        Map<UUID, net.sf.mpxj.Resource> userToResourceMap = new LinkedHashMap<>();
        for (User user : sprint.getUserMap().values().stream()
                .sorted(Comparator.comparing(User::getName))
                .toList()) {
            net.sf.mpxj.Resource resource = projectFile.addResource();
            resource.setName(user.getName());
            if (user.getEmail() != null) {
                resource.setEmailAddress(user.getEmail());
            }
            userToResourceMap.put(user.getId(), resource);
        }

        // Tasks → MPXJ tasks (hierarchical, parents before children)
        Map<UUID, net.sf.mpxj.Task> mpxjTaskMap = new LinkedHashMap<>();
        List<Task> rootTasks = sprint.getTasks().stream()
                .filter(t -> t.getParentTaskId() == null)
                .sorted(Comparator.comparingInt(Task::getOrderId))
                .toList();
        for (Task root : rootTasks) {
            net.sf.mpxj.Task mpxjRoot = projectFile.addTask();
            populateMpxjTask(mpxjRoot, root, userToResourceMap);
            mpxjTaskMap.put(root.getId(), mpxjRoot);
            addChildTasksRecursively(mpxjRoot, root, userToResourceMap, mpxjTaskMap);
        }

        // Predecessor relations (second pass, all tasks now in mpxjTaskMap)
        for (Task task : sprint.getTasks()) {
            net.sf.mpxj.Task mpxjTask = mpxjTaskMap.get(task.getId());
            if (mpxjTask == null) {
                continue;
            }
            for (Relation relation : task.getPredecessors()) {
                net.sf.mpxj.Task predecessorMpxjTask = mpxjTaskMap.get(relation.getPredecessorId());
                if (predecessorMpxjTask != null) {
                    mpxjTask.addPredecessor(new net.sf.mpxj.Relation.Builder()
                            .predecessorTask(predecessorMpxjTask)
                            .type(RelationType.FINISH_START)
                            .lag(Duration.getInstance(0, TimeUnit.HOURS)));
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        new MSPDIWriter().write(projectFile, out);
        return out.toByteArray();
    }

    /**
     * Exports the sprint to JSON format using the same container structure as the Gantt
     * regression tests ({@code users}, {@code sprint}, {@code tasks} keys).
     *
     * @param sprint the fully initialised sprint (users, tasks, and worklogs already loaded)
     * @return UTF-8 encoded JSON bytes
     * @throws Exception if the Jackson serialiser encounters an error
     */
    public byte[] exportToJson(Sprint sprint) throws Exception {
        Map<String, Object> container = new LinkedHashMap<>();
        container.put("users", sprint.getUserMap().values().stream()
                .sorted(Comparator.comparing(User::getName))
                .toList());
        container.put("sprint", sprint);
        container.put("tasks", sprint.getTasks());

        String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(container);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    /**
     * Recursively adds child MPXJ tasks under the given parent.
     *
     * @param mpxjParent        the MPXJ parent task to add children to
     * @param parent            the Kassandra parent task whose children should be added
     * @param userToResourceMap mapping of user IDs to their MPXJ resource counterparts
     * @param mpxjTaskMap       accumulator map of Kassandra task ID → MPXJ task
     */
    private void addChildTasksRecursively(net.sf.mpxj.Task mpxjParent, Task parent,
            Map<UUID, net.sf.mpxj.Resource> userToResourceMap,
            Map<UUID, net.sf.mpxj.Task> mpxjTaskMap) {
        List<Task> children = parent.getChildTasks().stream()
                .sorted(Comparator.comparingInt(Task::getOrderId))
                .toList();
        for (Task child : children) {
            net.sf.mpxj.Task mpxjChild = mpxjParent.addTask();
            populateMpxjTask(mpxjChild, child, userToResourceMap);
            mpxjTaskMap.put(child.getId(), mpxjChild);
            addChildTasksRecursively(mpxjChild, child, userToResourceMap, mpxjTaskMap);
        }
    }

    /**
     * Copies scalar properties and the resource assignment from a Kassandra {@link Task}
     * onto the given MPXJ task.  Predecessor relations are handled separately in a second
     * pass once all tasks have been added to the project.
     *
     * @param mpxjTask          the MPXJ task to populate
     * @param task              the source Kassandra task
     * @param userToResourceMap mapping of user IDs to their MPXJ resource counterparts
     */
    private void populateMpxjTask(net.sf.mpxj.Task mpxjTask, Task task,
            Map<UUID, net.sf.mpxj.Resource> userToResourceMap) {
        mpxjTask.setName(task.getName());
        mpxjTask.setMilestone(task.isMilestone());

        if (task.getTaskMode() != null) {
            mpxjTask.setTaskMode(task.getTaskMode() == de.bushnaq.abdalla.kassandra.dto.TaskMode.MANUALLY_SCHEDULED
                    ? net.sf.mpxj.TaskMode.MANUALLY_SCHEDULED
                    : net.sf.mpxj.TaskMode.AUTO_SCHEDULED);
        }
        if (task.getStart() != null) {
            mpxjTask.setStart(task.getStart());
        }
        if (task.getFinish() != null) {
            mpxjTask.setFinish(task.getFinish());
        }
        if (task.getDuration() != null && !task.getDuration().isZero()) {
            mpxjTask.setDuration(MpxjUtil.toMpjxDuration(task.getDuration()));
        }

        // Resource assignment
        if (task.getResourceId() != null) {
            net.sf.mpxj.Resource resource = userToResourceMap.get(task.getResourceId());
            if (resource != null) {
                ResourceAssignment assignment = mpxjTask.addResourceAssignment(resource);
                java.time.Duration work = task.getMinEstimate();
                if (work != null && !work.isZero()) {
                    assignment.setWork(MpxjUtil.toMpjxDuration(work));
                }
            }
        }
    }
}


