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

import de.bushnaq.abdalla.kassandra.dto.TaskStatus;

import de.bushnaq.abdalla.kassandra.service.BurndownEvidenceService.Evidence;
import de.bushnaq.abdalla.kassandra.service.BurndownEvidenceService.TaskEvidence;
import de.bushnaq.abdalla.kassandra.service.BurndownEvidenceService.TaskSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detects task effort that newly crossed a previously recorded maximum estimate between two captures.
 * A later estimate change cannot erase the earlier recorded limit, but worklog edits can affect observed totals.
 */
@Component
public class EstimateOverrunContributor {

    /**
     * Observed crossing of a previously recorded maximum; not an attribution of the chart gap.
     *
     * @param taskId task whose logged total crossed its earlier maximum
     * @param currentStatus current workflow status, if known
     * @param estimateRecordedAt earlier capture time when the maximum was observed
     * @param priorMaximumSeconds maximum estimate recorded at the earlier capture
     * @param priorLoggedSeconds complete logged total at the earlier capture
     * @param currentLoggedSeconds complete logged total at the current capture
     * @param excessSeconds current logged total minus the earlier maximum
     */
    public record Observation(UUID taskId, TaskStatus currentStatus, LocalDateTime estimateRecordedAt, long priorMaximumSeconds,
                              long priorLoggedSeconds, long currentLoggedSeconds, long excessSeconds) {
    }

    /**
     * Identifies whether the snapshots were comparable and how many current effort tasks lacked evidence.
     *
     * @param coverage comparison-window availability
     * @param afterDate earlier completed-day cutoff
     * @param throughDate current completed-day cutoff
     * @param comparedTasks effort tasks with usable baseline and current totals
     * @param unavailableTasks effort tasks that could not be compared
     * @param observations newly crossed prior maxima in descending excess order
     */
    public record Result(Coverage coverage, LocalDate afterDate, LocalDate throughDate,
                         int comparedTasks, int unavailableTasks, List<Observation> observations) {
        /**
         * Freezes observations for downstream consumers.
         * @param coverage comparison-window availability
         * @param afterDate earlier completed-day cutoff
         * @param throughDate current completed-day cutoff
         * @param comparedTasks effort tasks with usable baseline and current totals
         * @param unavailableTasks effort tasks that could not be compared
         * @param observations newly crossed prior maxima in descending excess order
         */
        public Result {
            // Freeze the observations because these are facts derived from a specific pair of captures.
            observations = List.copyOf(observations);
        }
    }

    /** Reasons a sprint-wide comparison can or cannot be attempted. */
    public enum Coverage {
        /** The two cutoffs can be compared; individual tasks may still be unavailable. */
        COMPARABLE_WINDOW,
        /** No earlier capture was supplied. */
        NO_PRIOR_SNAPSHOT,
        /** One capture does not provide a completed-day cutoff. */
        NO_COMPLETED_DAY_CUTOFF,
        /** The current capture or its cutoff does not advance beyond the prior capture. */
        NON_ADVANCING_WINDOW
    }

    /**
     * Finds new crossings of the prior recorded task maximum. The later estimate cannot erase a crossing.
     * The comparison uses what was observed at each capture; edited or backfilled worklogs can change the
     * observed totals, so the result does not date the work itself or establish why the sprint is behind.
     *
     * @param current current snapshot for the selected sprint
     * @param prior earlier snapshot, or {@code null} if none is available
     * @return observations and coverage for this comparison
     * @throws IllegalArgumentException if the snapshots are for different sprints
     */
    public Result evaluate(TaskSnapshot current, TaskSnapshot prior) {
        // Without a baseline, report missing comparison coverage rather than asserting there were no overruns.
        if (prior == null)
            return new Result(Coverage.NO_PRIOR_SNAPSHOT, null, current.chart().throughDate(), 0, 0, List.of());
        if (!current.sprintId().equals(prior.sprintId()))
            throw new IllegalArgumentException("Cannot compare task evidence from different sprints");

        Evidence before = prior.chart();
        Evidence after  = current.chart();
        // Both sides need completed-day boundaries; the chart's point coverage is not itself the task-history cutoff.
        if (!hasCutoff(before) || !hasCutoff(after))
            return new Result(Coverage.NO_COMPLETED_DAY_CUTOFF, before.throughDate(), after.throughDate(), 0, 0, List.of());
        // Only compare advancing observations; repeated or older snapshots cannot establish a new crossing.
        if (!after.capturedAt().isAfter(before.capturedAt()) || !after.throughDate().isAfter(before.throughDate()))
            return new Result(Coverage.NON_ADVANCING_WINDOW, before.throughDate(), after.throughDate(), 0, 0, List.of());

        Map<UUID, TaskEvidence> baseline = new HashMap<>();
        prior.tasks().forEach(task -> baseline.put(task.taskId(), task));
        List<Observation> observations = new ArrayList<>();
        int compared = 0;
        int unavailable = 0;
        for (TaskEvidence latest : current.tasks()) {
            // Non-effort work has no estimate/burn relationship relevant to this signal.
            if (!latest.effortTask())
                continue;
            TaskEvidence earlier = baseline.get(latest.taskId());
            // Require a common task, a recorded prior maximum, and complete non-decreasing worklog totals.
            // A decrease can indicate edited/backfilled data, so it is not interpreted as a negative overrun.
            if (earlier == null || !earlier.effortTask() || earlier.recordedMaxEstimateSeconds() == null
                    || earlier.loggedSeconds() == null || latest.loggedSeconds() == null
                    || latest.loggedSeconds() < earlier.loggedSeconds()) {
                unavailable++;
                continue;
            }
            compared++;
            long maximum = earlier.recordedMaxEstimateSeconds();
            // Count only a crossing during the observed interval, not a task already above its limit at baseline.
            if (earlier.loggedSeconds() <= maximum && latest.loggedSeconds() > maximum)
                observations.add(new Observation(latest.taskId(), latest.status(), before.capturedAt(), maximum,
                        earlier.loggedSeconds(), latest.loggedSeconds(), latest.loggedSeconds() - maximum));
        }
        // Largest excess first provides deterministic and immediately reviewable ordering.
        observations.sort(Comparator.comparingLong(Observation::excessSeconds).reversed().thenComparing(Observation::taskId));
        return new Result(Coverage.COMPARABLE_WINDOW, before.throughDate(), after.throughDate(),
                compared, unavailable, observations);
    }

    /** Requires an actual capture time and completed-day cutoff, while allowing other chart points to be absent. */
    private static boolean hasCutoff(Evidence evidence) {
        return evidence.capturedAt() != null && evidence.throughDate() != null
                && evidence.coverage() != BurndownEvidenceService.Coverage.NO_COMPLETED_SPRINT_DAY;
    }
}
