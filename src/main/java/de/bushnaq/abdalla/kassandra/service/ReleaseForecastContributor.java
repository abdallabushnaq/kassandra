/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskMode;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Compares the chart's linear release date with a fresh schedule of the remaining effort.
 * The scenario uses current assignments, calendars, availability and explicit dependencies; it is an outlook,
 * not a historical replay or an explanation of the burn-down gap.
 */
@Component
public class ReleaseForecastContributor {

    /** Coverage distinguishes a calculated forecast from each reason a reliable scenario could not be built. */
    public enum Coverage {
        /** Both the chart forecast and remaining-work scenario produced comparable release dates. */
        AVAILABLE,
        /** A current marker or linear chart release date was unavailable. */
        NO_LINEAR_FORECAST,
        /** No positive remaining effort was found in the selected sprint. */
        NO_REMAINING_WORK,
        /** Required task, calendar, dependency, or assignment inputs were incomplete. */
        INCOMPLETE_SCHEDULE,
        /** The scheduler could not produce a valid schedule for the constructed scenario. */
        SCHEDULING_FAILED
    }

    /** Direction of the signed calendar-day difference relative to the chart's linear release date. */
    public enum Direction {
        /** The linear forecast date is earlier than the remaining-work scenario's finish date. */
        LINEAR_EARLIER,
        /** Both forecasts finish on the same calendar date. */
        SAME_DAY,
        /** The linear forecast date is later than the remaining-work scenario's finish date. */
        LINEAR_LATER
    }

    /**
     * A positive day difference means the remaining-work schedule finishes later than the linear forecast.
     *
     * @param coverage whether a comparison was calculated
     * @param capturedAt current chart marker anchoring the scenario
     * @param linearRelease chart's extrapolated linear release date
     * @param scheduledRelease scenario's latest remaining-work finish
     * @param differenceInCalendarDays scheduled finish date minus linear date, or {@code null} if unavailable
     * @param direction relative ordering of the two dates, or {@code null} if unavailable
     */
    public record ForecastComparison(Coverage coverage, LocalDateTime capturedAt, LocalDateTime linearRelease,
                                     LocalDateTime scheduledRelease, Long differenceInCalendarDays, Direction direction) {
    }

    /**
     * Compares the chart's linear forecast with a scenario that schedules remaining work using the current calendars and assignments.
     *
     * @param sprint already authorized sprint used to build the chart
     * @param chart live chart for that sprint
     * @return comparison or a reason it could not be calculated
     * @throws IllegalArgumentException if the chart belongs to a different sprint
     */
    public ForecastComparison evaluate(Sprint sprint, GanttBurndownChartDto chart) {
        // Guard against pairing chart metadata with tasks from another sprint.
        if (sprint.getId() == null || !sprint.getId().equals(chart.sprintId))
            throw new IllegalArgumentException("The burn-down chart does not belong to the supplied sprint");
        LocalDateTime now = chart.meta.now;
        LocalDateTime linear = chart.meta.releaseDate;
        // A closed sprint or missing chart markers has no meaningful remaining-work comparison.
        if (chart.meta.sprintClosed || sprint.isClosed())
            return unavailable(Coverage.NO_REMAINING_WORK, now, linear);
        if (now == null || linear == null)
            return unavailable(Coverage.NO_LINEAR_FORECAST, now, linear);

        Map<UUID, Task> source = new LinkedHashMap<>();
        Set<UUID> work = new LinkedHashSet<>();
        for (Task task : sprint.getTasks()) {
            // Ignore tasks explicitly assigned to another sprint before checking IDs or work.
            if (task.getSprintId() != null && !sprint.getId().equals(task.getSprintId()))
                continue;
            // Duplicate or absent IDs make dependency reconstruction ambiguous.
            if (task.getId() == null || source.putIfAbsent(task.getId(), task) != null)
                return unavailable(Coverage.INCOMPLETE_SCHEDULE, now, linear);
            if (!task.isMilestone() && task.isImpactOnCost() && task.getChildTasks().isEmpty()) {
                Duration remaining = task.getRemainingEstimate();
                // Missing/negative remaining estimates are not equivalent to zero work.
                if (remaining == null || remaining.isNegative())
                    return unavailable(Coverage.INCOMPLETE_SCHEDULE, now, linear);
                if (remaining.isPositive()) {
                    // A finished or non-effort task with remaining effort cannot be safely scheduled.
                    if (task.getTaskStatus() == TaskStatus.DONE || !task.isTask())
                        return unavailable(Coverage.INCOMPLETE_SCHEDULE, now, linear);
                    work.add(task.getId());
                }
            }
        }
        // No positive remaining work yields no alternative finish date to compare.
        if (work.isEmpty())
            return unavailable(Coverage.NO_REMAINING_WORK, now, linear);
        if (sprint.getCalendar() == null)
            return unavailable(Coverage.INCOMPLETE_SCHEDULE, now, linear);

        // Build a detached scenario so re-leveling cannot modify the live sprint or chart schedule.
        Sprint replay = remainingSchedule(sprint, source, work, now);
        if (replay == null)
            return unavailable(Coverage.INCOMPLETE_SCHEDULE, now, linear);
        GanttErrorHandler diagnostics = new GanttErrorHandler();
        // Reuse the production resource scheduler so the scenario follows the same working-day and capacity rules.
        new GanttUtil().levelResources(diagnostics, replay, "", now);
        if (!diagnostics.exceptions.isEmpty() || !replay.exceptions.isEmpty())
            return unavailable(Coverage.SCHEDULING_FAILED, now, linear);

        LocalDateTime finish = now;
        for (UUID id : work) {
            Task task = replay.getTaskById(id);
            // If any remaining effort cannot be scheduled strictly from now onward, the scenario is incomplete.
            if (task.getStart() == null || task.getStart().isBefore(now) || task.getFinish() == null
                    || !task.getFinish().isAfter(task.getStart()))
                return unavailable(Coverage.SCHEDULING_FAILED, now, linear);
            if (task.getFinish().isAfter(finish))
                finish = task.getFinish();
        }
        // Compare local calendar dates, not elapsed 24-hour periods; the result is a day-level outlook.
        long days = ChronoUnit.DAYS.between(linear.toLocalDate(), finish.toLocalDate());
        Direction direction = days > 0 ? Direction.LINEAR_EARLIER : days < 0 ? Direction.LINEAR_LATER : Direction.SAME_DAY;
        return new ForecastComparison(Coverage.AVAILABLE, now, linear, finish, days, direction);
    }

    private static Sprint remainingSchedule(Sprint sprint, Map<UUID, Task> source, Set<UUID> work, LocalDateTime now) {
        Set<UUID> included = new LinkedHashSet<>(work);
        // Include parents needed by the scheduler to preserve hierarchy while keeping zero-effort parent containers.
        for (UUID id : work) {
            Set<UUID> path = new HashSet<>();
            for (Task task = source.get(id); task.getParentTaskId() != null; ) {
                if (!path.add(task.getId()))
                    return null;
                task = source.get(task.getParentTaskId());
                // A cycle, missing parent, or malformed leaf hierarchy prevents a faithful replay.
                if (task == null || task.getChildTasks().isEmpty())
                    return null;
                included.add(task.getId());
            }
        }
        Deque<UUID> pending = new ArrayDeque<>(included);
        // Add unfinished explicit prerequisites; hidden resource-ordering edges are not contractual dependencies.
        while (!pending.isEmpty()) {
            Task task = source.get(pending.removeFirst());
            for (Relation relation : task.getPredecessors()) {
                if (!relation.isVisible())
                    continue;
                Task predecessor = source.get(relation.getPredecessorId());
                // A missing prerequisite cannot be silently ignored because it could change the forecast finish.
                if (predecessor == null)
                    return null;
                if (predecessor.getTaskStatus() == TaskStatus.DONE || included.contains(predecessor.getId()))
                    continue;
                // Only top-level milestones without remaining effort can safely be carried into this scenario.
                if (!predecessor.isMilestone() || predecessor.getParentTaskId() != null)
                    return null;
                included.add(predecessor.getId());
                pending.addLast(predecessor.getId());
            }
        }

        Sprint replay = new Sprint();
        // Copy only required schedule facts into a separate sprint; live task state remains untouched.
        replay.setName("Remaining-work forecast");
        replay.setCalendar(sprint.getCalendar());
        Map<UUID, Task> copies = new LinkedHashMap<>();
        for (Task original : source.values()) {
            if (!included.contains(original.getId()))
                continue;
            // Every assigned item needs a usable person calendar and positive current availability.
            if (!hasCapacity(sprint, original, replay))
                return null;
            Task copy = new Task();
            copy.setId(original.getId());
            copy.setName("Forecast task");
            copy.setSprint(replay);
            copy.setSprintId(replay.getId());
            copy.setOrderId(original.getOrderId());
            copy.setResourceId(original.getResourceId());
            copy.setMilestone(original.isMilestone());
            // Only selected remaining-work leaves carry effort; hierarchy and prerequisite nodes are zero-duration.
            Duration effort = work.contains(original.getId()) ? original.getRemainingEstimate() : Duration.ZERO;
            copy.setMinEstimate(effort);
            copy.setMaxEstimate(effort);
            copy.setRemainingEstimate(effort);
            copy.setTaskMode(original.getTaskMode());
            if (original.getTaskMode() == TaskMode.MANUALLY_SCHEDULED) {
                // Preserve manual commitments, but never let a past start shift the remaining-work scenario backward.
                if (original.getStart() == null)
                    return null;
                copy.setStart(original.getStart().isBefore(now) ? now : original.getStart());
            }
            copies.put(copy.getId(), copy);
            replay.addTask(copy);
        }
        for (Task copy : copies.values()) {
            Task original = source.get(copy.getId());
            Task parent = copies.get(original.getParentTaskId());
            if (parent != null)
                parent.addChildTask(copy);
            // Recreate only explicit unfinished dependency links among tasks retained in the scenario.
            for (Relation relation : original.getPredecessors()) {
                Task predecessor = copies.get(relation.getPredecessorId());
                if (relation.isVisible() && predecessor != null
                        && source.get(predecessor.getId()).getTaskStatus() != TaskStatus.DONE)
                    copy.addPredecessor(predecessor, true);
            }
        }
        return replay;
    }

    private static boolean hasCapacity(Sprint sprint, Task task, Sprint replay) {
        // Unassigned work has no person-specific calendar to validate; the sprint calendar still schedules it.
        if (task.getResourceId() == null)
            return true;
        User user = sprint.getUser(task.getResourceId());
        // No user, calendar, availability record, or positive finite allocation means capacity is unknown.
        if (user == null || user.getCalendar() == null || user.getAvailabilities().isEmpty())
            return false;
        float availability = user.getAvailabilities().getLast().getAvailability();
        if (!Float.isFinite(availability) || availability <= 0)
            return false;
        replay.getUserMap().put(task.getResourceId(), user);
        return true;
    }

    /** Returns a typed unavailable result without fabricating a scheduled finish or date difference. */
    private static ForecastComparison unavailable(Coverage coverage, LocalDateTime now, LocalDateTime linear) {
        return new ForecastComparison(coverage, now, linear, null, null, null);
    }
}
