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

import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.AuthorSeriesDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Captures a live burn-down comparison and the matching selected-sprint task facts.
 * Captures are immutable in-memory observations, not historical replay, persisted snapshots, or AI prompt payloads.
 */
@Service
public class BurndownEvidenceService {

    /** Whether the current chart provides a comparable actual and guide point. */
    public enum Coverage {
        /** Both actual and guide points were available at the completed-day boundary. */
        AVAILABLE,
        /** The chart has no current marker from which to derive a cutoff. */
        NO_CURRENT_MARKER,
        /** The sprint has not yet reached a completed day. */
        NO_COMPLETED_SPRINT_DAY,
        /** At least one required actual series point was missing. */
        NO_ACTUAL_POINT,
        /** The buffered guide point was missing. */
        NO_GUIDE_POINT
    }

    /**
     * Remaining effort at one chart boundary; positive gap means actual remaining effort exceeds the guide.
     *
     * @param actualRemainingSeconds sum of remaining effort reconstructed from chart author series
     * @param guideRemainingSeconds recalculated chart guide with its delivery buffer
     * @param gapSeconds actual minus guide; positive means actual is above the guide
     */
    public record Comparison(long actualRemainingSeconds, long guideRemainingSeconds, long gapSeconds) {
    }

    /**
     * Live observation against the chart's recalculated, with-buffer guide, not an original plan or historical snapshot.
     *
     * @param capturedAt chart's current marker used for this observation
     * @param throughDate last completed day included in the comparison
     * @param coverage whether both chart series supplied the required comparison points
     * @param comparison signed comparison, or {@code null} when unavailable
     */
    public record Evidence(LocalDateTime capturedAt, LocalDate throughDate, Coverage coverage, Comparison comparison) {
    }

    /**
     * Minimal task facts used by contributors.
     * Schedule, estimate and status are current; logged effort is bounded by the chart cutoff.
     *
     * @param taskId selected-sprint task identifier
     * @param effortTask whether this is a leaf task whose work affects effort
     * @param status current workflow status, or {@code null} if unknown
     * @param scheduledStart current scheduled start, or {@code null} if unavailable
     * @param scheduledFinish current scheduled finish, or {@code null} if unavailable
     * @param recordedMaxEstimateSeconds positive current maximum estimate, or {@code null} if no baseline exists
     * @param loggedSeconds complete worklog total through the chart cutoff, or {@code null} if unavailable
     * @param explicitPredecessors visible, in-scope dependency IDs
     * @param resourcePredecessors hidden scheduler-ordering IDs, kept separate from explicit dependencies
     * @param outOfScopePredecessors number of distinct dependency references not included in this sprint snapshot
     */
    public record TaskEvidence(UUID taskId, boolean effortTask, TaskStatus status, LocalDateTime scheduledStart,
                               LocalDateTime scheduledFinish, Long recordedMaxEstimateSeconds, Long loggedSeconds,
                               List<UUID> explicitPredecessors, List<UUID> resourcePredecessors, long outOfScopePredecessors) {
        /**
         * Freezes dependency IDs at capture time.
         *
         * @param taskId selected-sprint task identifier
         * @param effortTask whether this is a leaf task whose work affects effort
         * @param status current workflow status, or {@code null} if unknown
         * @param scheduledStart current scheduled start, or {@code null} if unavailable
         * @param scheduledFinish current scheduled finish, or {@code null} if unavailable
         * @param recordedMaxEstimateSeconds positive current maximum estimate, or {@code null} if no baseline exists
         * @param loggedSeconds complete worklog total through the chart cutoff, or {@code null} if unavailable
         * @param explicitPredecessors visible, in-scope dependency IDs
         * @param resourcePredecessors hidden scheduler-ordering IDs, kept separate from explicit dependencies
         * @param outOfScopePredecessors number of distinct dependency references not included in this sprint snapshot
         */
        public TaskEvidence {
            // Copy both dependency lists so a snapshot cannot be altered after capture.
            explicitPredecessors = List.copyOf(explicitPredecessors);
            resourcePredecessors = List.copyOf(resourcePredecessors);
        }
    }

    /**
     * Server-side chart and task facts from one live capture; this is neither an as-of query nor a prompt payload.
     *
     * @param sprintId sprint that scopes all captured facts
     * @param chart chart evidence at this capture's cutoff
     * @param tasks immutable selected-sprint task facts
     */
    public record TaskSnapshot(UUID sprintId, Evidence chart, List<TaskEvidence> tasks) {
        /**
         * Freezes the captured task facts.
         *
         * @param sprintId sprint that scopes all captured facts
         * @param chart chart evidence at this capture's cutoff
         * @param tasks immutable selected-sprint task facts
         */
        public TaskSnapshot {
            // Keep the snapshot independent of the mutable sprint task collection.
            tasks = List.copyOf(tasks);
        }
    }

    /**
     * Captures task facts from the same loaded sprint used to build the chart. Effort is grouped by worklog start day,
     * including work before sprint start, like the chart. Missing worklog input is not interpreted as zero.
     *
     * @param sprint already authorized sprint, with its task and worklog data loaded
     * @param chart live chart built from that sprint state
     * @return immutable chart and task evidence, without worklog text or resource IDs
     * @throws IllegalArgumentException if the chart is not scoped to the supplied sprint
     */
    public TaskSnapshot capture(Sprint sprint, GanttBurndownChartDto chart) {
        UUID sprintId = sprint.getId();
        // Reject accidental cross-sprint joins before combining chart points with task records.
        if (sprintId == null || !sprintId.equals(chart.sprintId))
            throw new IllegalArgumentException("The burn-down chart does not belong to the supplied sprint");

        Evidence evidence = capture(chart);
        // Keep only this sprint's tasks and sort them so repeated captures have deterministic task ordering.
        List<Task> tasks = sprint.getTasks().stream()
                .filter(task -> task.getId() != null && (task.getSprintId() == null || sprintId.equals(task.getSprintId())))
                .sorted(Comparator.comparing(Task::getId))
                .toList();
        Set<UUID> taskIds = new HashSet<>();
        tasks.forEach(task -> taskIds.add(task.getId()));
        Map<UUID, Duration> work = new HashMap<>();
        Set<UUID> incompleteWork = new HashSet<>();
        // Worklog totals are meaningful only when the chart cutoff and sprint start define a completed-day window.
        boolean workAvailable = sprint.getWorklogs() != null && evidence.capturedAt() != null
                && evidence.throughDate() != null && sprint.getStart() != null
                && !evidence.throughDate().isBefore(sprint.getStart().toLocalDate());
        if (workAvailable) {
            // Include worklogs by their start time before the day-after-cutoff boundary, matching chart day semantics.
            LocalDateTime cutoff = evidence.throughDate().plusDays(1).atStartOfDay();
            for (Worklog log : sprint.getWorklogs()) {
                // Exclude logs for other tasks/sprints; retain unknown duration or timestamp as missing evidence.
                if (!taskIds.contains(log.getTaskId()) || (log.getSprintId() != null && !sprintId.equals(log.getSprintId())))
                    continue;
                if (log.getStart() == null) {
                    incompleteWork.add(log.getTaskId());
                } else if (log.getStart().isBefore(cutoff)) {
                    if (log.getTimeSpent() == null)
                        incompleteWork.add(log.getTaskId());
                    else
                        work.merge(log.getTaskId(), log.getTimeSpent(), Duration::plus);
                }
            }
        }

        List<TaskEvidence> facts = new ArrayList<>();
        for (Task task : tasks) {
            UUID id = task.getId();
            List<Relation> predecessors = task.getPredecessors() == null ? List.of() : task.getPredecessors();
            // Count cross-sprint/out-of-scope references for coverage, but do not expose those IDs as task evidence.
            long outOfScope = predecessors.stream().map(Relation::getPredecessorId)
                    .filter(predecessor -> !taskIds.contains(predecessor)).distinct().count();
            // Only leaf effort work can contribute to estimates or remaining-work evidence.
            boolean effortTask = !task.isMilestone() && task.isImpactOnCost() && task.getChildTasks().isEmpty() && task.isTask();
            Duration max = task.getMaxEstimate();
            // A non-positive or absent maximum is not a recorded baseline against which to claim an overrun.
            Long recordedMax = effortTask && max != null && max.toSeconds() > 0 ? max.toSeconds() : null;
            // Null means worklog evidence is incomplete/unavailable; zero is reserved for a complete period with no logs.
            Long logged = workAvailable && !incompleteWork.contains(id) ? work.getOrDefault(id, Duration.ZERO).toSeconds() : null;
            facts.add(new TaskEvidence(id, effortTask, task.getTaskStatus(), task.getStart(), task.getFinish(),
                    recordedMax, logged, predecessorIds(predecessors, taskIds, true),
                    predecessorIds(predecessors, taskIds, false), outOfScope));
        }
        return new TaskSnapshot(sprintId, evidence, facts);
    }

    /**
     * Captures the last completed day's chart values without retaining author names or worklog details.
     *
     * @param chart live chart DTO for one already authorized sprint
     * @return chart evidence with a reason when no comparison is available
     */
    public Evidence capture(GanttBurndownChartDto chart) {
        LocalDateTime now = chart.meta.now;
        // Without the chart's current marker there is no reliable way to define the last completed day.
        if (now == null)
            return unavailable(null, null, Coverage.NO_CURRENT_MARKER);

        LocalDate boundary = now.toLocalDate();
        LocalDate through = boundary.minusDays(1);
        // Ignore the in-progress day so actual and guide values use a completed-day boundary.
        if (chart.meta.sprintStart == null || through.isBefore(chart.meta.sprintStart.toLocalDate()))
            return unavailable(now, through, Coverage.NO_COMPLETED_SPRINT_DAY);

        // Series are indexed from their unpadded origin; the current-day boundary selects the previous completed point.
        long slot = chart.seriesStart == null ? -1 : ChronoUnit.DAYS.between(chart.seriesStart, boundary);
        Long guide = valueAt(chart.ganttGuideWithoutBuffer, slot) == null ? null : valueAt(chart.ganttGuideWithBuffer, slot);
        // The buffered guide is the comparison target; a missing point is unavailable, not zero remaining effort.
        if (guide == null)
            return unavailable(now, through, Coverage.NO_GUIDE_POINT);

        long actual = 0;
        boolean hasActual = false;
        for (AuthorSeriesDto author : chart.authors) {
            // Reconstruct each author's remaining work from its total minus the cumulative burn at this slot.
            long estimated = author.totalWorkedSeconds + author.totalRemainingSeconds;
            if (estimated == 0)
                continue;
            Long worked = valueAt(author.accumulatedWorkPerDay, slot);
            if (worked == null)
                return unavailable(now, through, Coverage.NO_ACTUAL_POINT);
            actual += estimated - worked;
            hasActual = true;
        }
        if (!hasActual)
            return unavailable(now, through, Coverage.NO_ACTUAL_POINT);

        // Keep the signed difference so consumers can distinguish above-guide from on/below-guide outcomes.
        return new Evidence(now, through, Coverage.AVAILABLE, new Comparison(actual, guide, actual - guide));
    }

    /** Creates a coverage result without fabricating a comparison. */
    private static Evidence unavailable(LocalDateTime now, LocalDate through, Coverage coverage) {
        return new Evidence(now, through, coverage, null);
    }

    /** Separates explicit task dependencies from hidden scheduler-generated resource ordering. */
    private static List<UUID> predecessorIds(List<Relation> predecessors, Set<UUID> taskIds, boolean visible) {
        return predecessors.stream().filter(relation -> relation.isVisible() == visible)
                .map(Relation::getPredecessorId).filter(taskIds::contains).distinct().sorted().toList();
    }

    /** Reads one chart slot, treating an absent series or out-of-range slot as missing evidence. */
    private static Long valueAt(List<Long> series, long slot) {
        return series == null || slot < 0 || slot >= series.size() ? null : series.get((int) slot);
    }
}
