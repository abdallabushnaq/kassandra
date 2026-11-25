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

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Finds aggregate days when multiple scheduled sprint assignees were unavailable due only to sickness.
 * It reports counts, not identities or lost effort, and does not infer a cause for the burn-down gap.
 */
@Component
public class CapacityOverlapContributor {

    /** Coverage distinguishes a real zero-overlap result from missing date or calendar inputs. */
    public enum Coverage {
        /** The completed-day window and sprint calendar were available, including when there was no overlap. */
        AVAILABLE,
        /** No completed sprint day exists at the chart cutoff. */
        NO_COMPLETED_SPRINT_DAY,
        /** The sprint calendar needed to interpret working days is missing. */
        NO_SPRINT_CALENDAR
    }

    /**
     * Count of distinct unavailable assignees on one completed sprint day.
     *
     * @param date completed sprint day on which overlap was observed
     * @param unavailableAssignees distinct assigned people unavailable from sickness-only schedule changes
     */
    public record DayOverlap(LocalDate date, int unavailableAssignees) {
    }

    /**
     * Aggregate result and its input-coverage indicators.
     *
     * @param coverage whether the requested window and sprint calendar were available
     * @param throughDate last completed day considered, or {@code null} if there is no cutoff
     * @param tasksWithMissingInputs scheduled tasks omitted because required schedule or person-calendar facts were missing
     * @param overlaps days with at least two distinct unavailable assignees
     */
    public record Result(Coverage coverage, LocalDate throughDate, int tasksWithMissingInputs, List<DayOverlap> overlaps) {
        /**
         * Freezes the aggregate observations so callers cannot mutate the captured result.
         * @param coverage whether the requested window and sprint calendar were available
         * @param throughDate last completed day considered, or {@code null} if there is no cutoff
         * @param tasksWithMissingInputs scheduled tasks omitted because required schedule or person-calendar facts were missing
         * @param overlaps days with at least two distinct unavailable assignees
         */
        public Result {
            overlaps = List.copyOf(overlaps);
        }
    }

    /**
     * Finds completed days on which at least two distinct scheduled assignees were unavailable due to sickness.
     * This compares against the chart's recalculated sickness-free schedule, not a historical baseline.
     *
     * @param sprint authorized sprint with the same leveled tasks and calendars used to build the chart
     * @param chart current chart for that sprint
     * @return aggregate counts only; no assignee or task identifiers
     * @throws IllegalArgumentException if the chart belongs to a different sprint
     */
    public Result evaluate(Sprint sprint, GanttBurndownChartDto chart) {
        UUID sprintId = sprint.getId();
        if (sprintId == null || !sprintId.equals(chart.sprintId))
            throw new IllegalArgumentException("The burn-down chart does not belong to the supplied sprint");
        LocalDate through = chart.meta.now == null ? null : chart.meta.now.toLocalDate().minusDays(1);
        // Do not turn an absent completed day into a zero-overlap finding.
        if (through == null || sprint.getStart() == null || through.isBefore(sprint.getStart().toLocalDate()))
            return new Result(Coverage.NO_COMPLETED_SPRINT_DAY, through, 0, List.of());
        // The sprint calendar defines the working-day rules used by the chart's sickness-free schedule.
        if (sprint.getCalendar() == null)
            return new Result(Coverage.NO_SPRINT_CALENDAR, through, 0, List.of());

        LocalDate first = sprint.getStart().toLocalDate();
        Map<LocalDate, Set<UUID>> unavailableByDay = new TreeMap<>();
        int missing = 0;
        for (Task task : sprint.getTasks()) {
            // Count only leaf effort tasks in this sprint that are assigned and can intersect the completed-day window.
            if (!task.isTask() || !task.getChildTasks().isEmpty() || task.getResourceId() == null
                    || (task.getSprintId() != null && !sprintId.equals(task.getSprintId())))
                continue;
            // Exclude tasks that begin after the observation cutoff or finish before the sprint window begins.
            if (task.getStart() != null && !task.getStart().isBefore(through.plusDays(1).atStartOfDay()))
                continue;
            if (task.getFinish() != null && !task.getFinish().isAfter(first.atStartOfDay()))
                continue;
            User user = sprint.getUser(task.getResourceId());
            // Missing scheduling facts make this task unavailable for analysis, not evidence of an absence.
            if (user == null || user.getCalendar() == null || task.getStart() == null
                    || task.getFinish() == null || !task.getFinish().isAfter(task.getStart())) {
                missing++;
                continue;
            }

            LocalDate day = task.getStart().toLocalDate();
            // Clip tasks that started before the sprint so only sprint days are considered.
            if (day.isBefore(first))
                day = first;
            while (!day.isAfter(through) && day.atStartOfDay().isBefore(task.getFinish())) {
                // A non-working person-day counts only when the chart schedule says sickness alone removed that workday.
                if (!user.getCalendar().isWorkingDate(day) && GanttBurndownChartService.isSicknessOnlyWorkingDay(sprint, user, day))
                    unavailableByDay.computeIfAbsent(day, ignored -> new HashSet<>()).add(task.getResourceId());
                day = day.plusDays(1);
            }
        }
        // The per-day set deduplicates a person with multiple scheduled tasks; retain only genuine multi-person overlap.
        List<DayOverlap> overlaps = unavailableByDay.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .map(entry -> new DayOverlap(entry.getKey(), entry.getValue().size()))
                .toList();
        return new Result(Coverage.AVAILABLE, through, missing, overlaps);
    }
}
