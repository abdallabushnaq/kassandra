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

import de.bushnaq.abdalla.kassandra.dto.*;
import net.sf.mpxj.ProjectCalendar;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Detects unplanned sick leave on critical-path work and propagates its delay through sprint dependencies.
 */
@Component
public class SickLeaveImpactContributor {

    private static int countWorkingDays(ProjectCalendar calendar, LocalDate firstDay, LocalDate lastDay) {
        if (firstDay == null || lastDay == null || lastDay.isBefore(firstDay)) {
            return 0;
        }
        int workingDays = 0;
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            if (calendar.isWorkingDate(day)) {
                workingDays++;
            }
        }
        return workingDays;
    }

    private static int countWorkingDays(ProjectCalendar calendar, LocalDate firstDay, LocalDateTime finish) {
        if (firstDay == null || finish == null || !finish.isAfter(firstDay.atStartOfDay())) {
            return 0;
        }
        int workingDays = 0;
        for (LocalDate day = firstDay; day.atStartOfDay().isBefore(finish); day = day.plusDays(1)) {
            if (calendar.isWorkingDate(day)) {
                workingDays++;
            }
        }
        return workingDays;
    }

    /**
     * Evaluates one sprint's critical-path sick leave and simulates its effect on dependent tasks. Sick days
     * encountered while delayed work is being performed are included in the simulation.
     *
     * @param sprint sprint with initialized tasks and user off-day records
     * @return detected sick days, per-task delays, and the total schedule and effort impact
     * @throws IllegalStateException if the sprint contains a cycle in its in-sprint dependencies
     */
    public SprintImpact evaluate(Sprint sprint) {
        Objects.requireNonNull(sprint, "sprint");
        Map<UUID, Task> tasksById = new LinkedHashMap<>();
        for (Task task : sprint.getTasks()) {
            tasksById.put(task.getId(), task);
        }

        Map<UUID, ProjectCalendar> baseCalendars = new HashMap<>();
        try {
            for (User user : sprint.getUserMap().values()) {
                baseCalendars.put(user.getId(), user.createCalendarWithoutSickDays(sprint));
            }

            Map<UUID, Set<LocalDate>> sickDaysByUser = findSickDaysByUser(sprint);
            Set<LocalDate>            sickDays       = new TreeSet<>();
            Map<UUID, Set<LocalDate>> unplannedSickDaysByUser = new HashMap<>();
            for (Task task : tasksById.values()) {
                if (!isEligibleCriticalTask(task)) {
                    continue;
                }
                ProjectCalendar calendar = getBaseCalendar(sprint, task, baseCalendars);
                for (LocalDate day : sickDaysByUser.getOrDefault(task.getResourceId(), Set.of())) {
                    if (isWithinTask(task, day) && calendar.isWorkingDate(day)) {
                        sickDays.add(day);
                        unplannedSickDaysByUser.computeIfAbsent(task.getResourceId(), ignored -> new TreeSet<>()).add(day);
                    }
                }
            }

            Map<UUID, Integer> taskDelayDays = simulateDelays(sprint, tasksById, baseCalendars, sickDaysByUser,
                    unplannedSickDaysByUser, sickDays);
            int                scheduleDelayDays = 0;
            int                effortDelayDays   = 0;
            for (Task task : tasksById.values()) {
                int delayDays = taskDelayDays.get(task.getId());
                effortDelayDays = Math.addExact(effortDelayDays, delayDays);
                if (task.isCritical()) {
                    scheduleDelayDays = Math.addExact(scheduleDelayDays, delayDays);
                }
            }
            return new SprintImpact(new ArrayList<>(sickDays), taskDelayDays, scheduleDelayDays, effortDelayDays);
        } finally {
            for (ProjectCalendar calendar : baseCalendars.values()) {
                sprint.getProjectFile().getCalendars().remove(calendar);
            }
        }
    }

    private static Map<UUID, Set<LocalDate>> findSickDaysByUser(Sprint sprint) {
        Map<UUID, Set<LocalDate>> sickDaysByUser = new HashMap<>();
        for (User user : sprint.getUserMap().values()) {
            Set<LocalDate> sickDays = new TreeSet<>();
            if (user.getOffDays() != null) {
                for (OffDay offDay : user.getOffDays()) {
                    if (offDay.getType() != OffDayType.SICK || offDay.getFirstDay() == null || offDay.getLastDay() == null) {
                        continue;
                    }
                    for (LocalDate day = offDay.getFirstDay(); !day.isAfter(offDay.getLastDay()); day = day.plusDays(1)) {
                        sickDays.add(day);
                    }
                }
            }
            sickDaysByUser.put(user.getId(), sickDays);
        }
        return sickDaysByUser;
    }

    private static ProjectCalendar getBaseCalendar(Sprint sprint, Task task, Map<UUID, ProjectCalendar> baseCalendars) {
        ProjectCalendar calendar = baseCalendars.get(task.getResourceId());
        if (calendar != null) {
            return calendar;
        }
        calendar = sprint.getCalendar();
        return calendar != null ? calendar : sprint.getProjectFile().getDefaultCalendar();
    }

    private static boolean isEligibleCriticalTask(Task task) {
        return task.isCritical() && task.isTask() && task.getChildTasks().isEmpty() && task.isImpactOnCost()
                && task.getResourceId() != null && task.getStart() != null && task.getFinish() != null
                && task.getFinish().isAfter(task.getStart());
    }

    private static boolean isWithinTask(Task task, LocalDate day) {
        LocalDateTime taskStart  = task.getStart();
        LocalDateTime taskFinish = task.getFinish();
        return taskStart != null && taskFinish != null && !day.isBefore(taskStart.toLocalDate())
                && !day.isAfter(taskFinish.toLocalDate()) && day.atStartOfDay().isBefore(taskFinish);
    }

    private static Map<UUID, Integer> simulateDelays(Sprint sprint, Map<UUID, Task> tasksById,
                                                     Map<UUID, ProjectCalendar> baseCalendars,
                                                     Map<UUID, Set<LocalDate>> sickDaysByUser,
                                                     Map<UUID, Set<LocalDate>> unplannedSickDaysByUser,
                                                     Set<LocalDate> detectedSickDays) {
        Map<UUID, Integer>    remainingPredecessors   = new HashMap<>();
        Map<UUID, List<UUID>> dependentsByPredecessor = new HashMap<>();
        for (Task task : tasksById.values()) {
            int inSprintPredecessors = 0;
            for (Relation relation : task.getPredecessors()) {
                UUID predecessorId = relation.getPredecessorId();
                if (tasksById.containsKey(predecessorId)) {
                    dependentsByPredecessor.computeIfAbsent(predecessorId, ignored -> new ArrayList<>()).add(task.getId());
                    inSprintPredecessors++;
                }
            }
            remainingPredecessors.put(task.getId(), inSprintPredecessors);
        }

        Deque<UUID> ready = new ArrayDeque<>();
        for (UUID taskId : tasksById.keySet()) {
            if (remainingPredecessors.get(taskId) == 0) {
                ready.addLast(taskId);
            }
        }

        Map<UUID, SimulatedTask> simulations = new LinkedHashMap<>();
        while (!ready.isEmpty()) {
            UUID      taskId                  = ready.removeFirst();
            Task      task                    = tasksById.get(taskId);
            LocalDate latestPredecessorFinish = null;
            for (Relation relation : task.getPredecessors()) {
                SimulatedTask predecessor = simulations.get(relation.getPredecessorId());
                if (predecessor != null && predecessor.actualFinish() != null
                        && (latestPredecessorFinish == null || predecessor.actualFinish().isAfter(latestPredecessorFinish))) {
                    latestPredecessorFinish = predecessor.actualFinish();
                }
            }

            SimulatedTask simulation = simulateTask(sprint, task, latestPredecessorFinish, baseCalendars, sickDaysByUser,
                    unplannedSickDaysByUser, detectedSickDays);
            simulations.put(taskId, simulation);

            for (UUID dependentId : dependentsByPredecessor.getOrDefault(taskId, List.of())) {
                int remaining = remainingPredecessors.merge(dependentId, -1, Integer::sum);
                if (remaining == 0) {
                    ready.addLast(dependentId);
                }
            }
        }
        if (simulations.size() != tasksById.size()) {
            throw new IllegalStateException("Cannot evaluate sick-leave impact for a sprint with cyclic dependencies.");
        }

        Map<UUID, Integer> taskDelayDays = new LinkedHashMap<>();
        simulations.forEach((taskId, simulation) -> taskDelayDays.put(taskId, simulation.delayDays()));
        return taskDelayDays;
    }

    private static SimulatedTask simulateTask(Sprint sprint, Task task, LocalDate latestPredecessorFinish,
                                              Map<UUID, ProjectCalendar> baseCalendars,
                                              Map<UUID, Set<LocalDate>> sickDaysByUser,
                                              Map<UUID, Set<LocalDate>> unplannedSickDaysByUser,
                                              Set<LocalDate> detectedSickDays) {
        LocalDateTime plannedStart  = task.getStart();
        LocalDateTime plannedFinish = task.getFinish();
        if (plannedStart == null || plannedFinish == null) {
            return new SimulatedTask(latestPredecessorFinish, 0);
        }

        LocalDate plannedStartDate = plannedStart.toLocalDate();
        LocalDate actualStart = latestPredecessorFinish != null && latestPredecessorFinish.isAfter(plannedStartDate)
                ? latestPredecessorFinish : plannedStartDate;
        ProjectCalendar calendar = getBaseCalendar(sprint, task, baseCalendars);
        int remainingWorkDays = task.isTask() && task.getChildTasks().isEmpty()
                ? countWorkingDays(calendar, plannedStartDate, plannedFinish) : 0;
        if (remainingWorkDays == 0) {
            LocalDate actualFinish = plannedFinish.toLocalDate();
            if (latestPredecessorFinish != null && latestPredecessorFinish.isAfter(actualFinish)) {
                actualFinish = latestPredecessorFinish;
            }
            return new SimulatedTask(actualFinish, countWorkingDays(calendar, plannedStartDate.plusDays(1), actualStart));
        }

        boolean        applySickLeave     = isEligibleCriticalTask(task) || actualStart.isAfter(plannedStartDate);
        Set<LocalDate> taskSickDays       = sickDaysByUser.getOrDefault(task.getResourceId(), Set.of());
        Set<LocalDate> unplannedSickDays = unplannedSickDaysByUser.getOrDefault(task.getResourceId(), Set.of());
        LocalDate      firstActualWorkDay = null;
        LocalDate      actualFinish       = null;
        int            sickDaysAfterStart = 0;
        for (LocalDate day = actualStart; remainingWorkDays > 0; day = day.plusDays(1)) {
            if (!calendar.isWorkingDate(day)) {
                continue;
            }
            if (applySickLeave && taskSickDays.contains(day) && unplannedSickDays.contains(day)) {
                detectedSickDays.add(day);
                if (firstActualWorkDay != null) {
                    sickDaysAfterStart++;
                }
                continue;
            }
            if (firstActualWorkDay == null) {
                firstActualWorkDay = day;
            }
            actualFinish = day;
            remainingWorkDays--;
        }
        int startDelay = countWorkingDays(calendar, plannedStartDate.plusDays(1), firstActualWorkDay);
        return new SimulatedTask(actualFinish, Math.addExact(startDelay, sickDaysAfterStart));
    }

    private record SimulatedTask(LocalDate actualFinish, int delayDays) {
    }

    /**
     * Sick-leave observations and the resulting per-task and sprint-wide delays.
     *
     * @param sickDays          unique sick-leave dates detected on critical-path work
     * @param taskDelayDays     propagated delay per task ID, including zero-delay tasks
     * @param scheduleDelayDays sum of propagated delays for critical-path tasks
     * @param effortDelayDays   sum of propagated delays for all sprint tasks
     */
    public record SprintImpact(List<LocalDate> sickDays, Map<UUID, Integer> taskDelayDays,
                               int scheduleDelayDays, int effortDelayDays) {
        /**
         * Freezes the observations derived from this sprint.
         *
         * @param sickDays          unique sick-leave dates detected on critical-path work
         * @param taskDelayDays     propagated delay per task ID, including zero-delay tasks
         * @param scheduleDelayDays sum of propagated delays for critical-path tasks
         * @param effortDelayDays   sum of propagated delays for all sprint tasks
         */
        public SprintImpact {
            sickDays      = List.copyOf(sickDays);
            taskDelayDays = Collections.unmodifiableMap(new LinkedHashMap<>(taskDelayDays));
        }
    }
}
