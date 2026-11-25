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
 * Identifies cautiously observed blocked handoffs from two captures of the same sprint.
 * A result requires a still-TODO successor with an unchanged explicit unfinished prerequisite; it does not prove
 * continuous waiting or quantify a contribution to the burn-down gap.
 */
@Component
public class DependencyBlockerContributor {

    /** Explains why the requested pair of snapshots could or could not be compared. */
    public enum Coverage {
        /** A prior capture and an advancing completed-day window were available. */
        COMPARABLE_WINDOW,
        /** No earlier capture was supplied. */
        NO_PRIOR_SNAPSHOT,
        /** Either capture lacks a usable completed-day cutoff. */
        NO_COMPLETED_DAY_CUTOFF,
        /** The later capture or cutoff did not advance beyond the earlier one. */
        NON_ADVANCING_WINDOW
    }

    /**
     * One observation per successor, regardless of the number of known unfinished prerequisites.
     *
     * @param taskId successor task identified as still blocked
     * @param priorScheduledStart successor's scheduled start in the earlier capture
     * @param unfinishedPredecessors explicit prerequisites observed unfinished in both captures
     */
    public record Observation(UUID taskId, LocalDateTime priorScheduledStart, List<UUID> unfinishedPredecessors) {
        /**
         * Freezes the prerequisite IDs.
         * @param taskId successor task identified as still blocked
         * @param priorScheduledStart successor's scheduled start in the earlier capture
         * @param unfinishedPredecessors explicit prerequisites observed unfinished in both captures
         */
        public Observation {
            // A blocker observation is a fixed capture result, not a live view of dependency state.
            unfinishedPredecessors = List.copyOf(unfinishedPredecessors);
        }
    }

    /**
     * Unavailable tasks may also have observations if some, but not all, prerequisites could be evaluated.
     *
     * @param coverage whether the two captures define a comparable window
     * @param throughDate last completed day included in the current capture
     * @param observedAt current capture marker
     * @param unavailableTasks candidate successors for which some required evidence was missing
     * @param observations observed blocked successors and their known unfinished prerequisites
     */
    public record Result(Coverage coverage, LocalDate throughDate, LocalDateTime observedAt,
                         int unavailableTasks, List<Observation> observations) {
        /**
         * Freezes observations for downstream consumers.
         * @param coverage whether the two captures define a comparable window
         * @param throughDate last completed day included in the current capture
         * @param observedAt current capture marker
         * @param unavailableTasks candidate successors for which some required evidence was missing
         * @param observations observed blocked successors and their known unfinished prerequisites
         */
        public Result {
            // Keep output stable if a downstream renderer or selector retains the result.
            observations = List.copyOf(observations);
        }
    }

    /**
     * Finds tasks still marked TODO without logged work through the chart cutoff, whose previously scheduled
     * start has passed and whose explicit prerequisites were unfinished in both captures. This does not
     * establish a continuous wait, its duration, or how much of the chart gap the blocker explains.
     *
     * @param current current snapshot for the selected sprint
     * @param prior earlier snapshot, or {@code null} if none is available
     * @return observed blockers and coverage
     * @throws IllegalArgumentException if the snapshots are for different sprints
     */
    public Result evaluate(TaskSnapshot current, TaskSnapshot prior) {
        LocalDate through = current.chart().throughDate();
        LocalDateTime now = current.chart().capturedAt();
        // No earlier state means there is no basis for a historical transition claim.
        if (prior == null)
            return new Result(Coverage.NO_PRIOR_SNAPSHOT, through, now, 0, List.of());
        if (!current.sprintId().equals(prior.sprintId()))
            throw new IllegalArgumentException("Cannot compare task evidence from different sprints");

        LocalDateTime before = prior.chart().capturedAt();
        // Both captures need real completed-day boundaries before a start can be judged late.
        if (before == null || now == null || through == null
                || current.chart().coverage() == BurndownEvidenceService.Coverage.NO_COMPLETED_SPRINT_DAY)
            return new Result(Coverage.NO_COMPLETED_DAY_CUTOFF, through, now, 0, List.of());
        LocalDateTime cutoff = through.plusDays(1).atStartOfDay();
        // Require an advancing capture and at least one completed day after the baseline.
        if (!now.isAfter(before) || !cutoff.isAfter(before))
            return new Result(Coverage.NON_ADVANCING_WINDOW, through, now, 0, List.of());

        Map<UUID, TaskEvidence> latestById = index(current);
        Map<UUID, TaskEvidence> earlierById = index(prior);
        List<Observation> observations = new ArrayList<>();
        int unavailable = 0;
        for (TaskEvidence latest : current.tasks()) {
            // Only tasks that remain effort work, are still TODO (or have unknown status), have no logged burn,
            // and expose explicit or out-of-scope dependencies are candidates for a blocker observation.
            if (!latest.effortTask() || (latest.status() != null && latest.status() != TaskStatus.TODO)
                    || (latest.loggedSeconds() != null && latest.loggedSeconds() != 0)
                    || (latest.explicitPredecessors().isEmpty() && latest.outOfScopePredecessors() == 0))
                continue;

            TaskEvidence earlier = earlierById.get(latest.taskId());
            // The earlier state must establish that this task had a scheduled start in the future at capture time.
            // Missing history or current worklog/status evidence makes the candidate unavailable, not clear.
            if (earlier == null || !earlier.effortTask() || earlier.status() != TaskStatus.TODO || earlier.scheduledStart() == null
                    || earlier.scheduledStart().isBefore(before) || latest.loggedSeconds() == null || latest.status() == null) {
                unavailable++;
                continue;
            }
            if (!earlier.scheduledStart().isBefore(cutoff))
                continue;

            // Missing/out-of-scope dependency history is tracked separately from known blockers.
            boolean missing = latest.outOfScopePredecessors() > 0;
            List<UUID> blockers = new ArrayList<>();
            for (UUID predecessorId : latest.explicitPredecessors()) {
                TaskEvidence predecessor = latestById.get(predecessorId);
                TaskEvidence earlierPredecessor = earlierById.get(predecessorId);
                // A predecessor completed by the current capture is no longer an unfinished handoff.
                if (predecessor != null && predecessor.status() == TaskStatus.DONE)
                    continue;
                // Require the same explicit edge and unfinished state in both captures; a newly added edge cannot
                // establish that the successor was waiting on it during the comparison window.
                if (predecessorId.equals(latest.taskId()) || predecessor == null || earlierPredecessor == null
                        || predecessor.status() == null || earlierPredecessor.status() == null
                        || !earlier.explicitPredecessors().contains(predecessorId)
                        || earlierPredecessor.status() == TaskStatus.DONE) {
                    missing = true;
                } else {
                    blockers.add(predecessorId);
                }
            }
            // Partial observations can coexist with unavailable prerequisites, so preserve both signals.
            if (missing)
                unavailable++;
            if (!blockers.isEmpty())
                observations.add(new Observation(latest.taskId(), earlier.scheduledStart(), blockers.stream().distinct().sorted().toList()));
        }
        // Stable ordering makes review output independent of task collection iteration order.
        observations.sort(Comparator.comparing(Observation::priorScheduledStart).thenComparing(Observation::taskId));
        return new Result(Coverage.COMPARABLE_WINDOW, through, now, unavailable, observations);
    }

    /** Indexes one capture by task ID for constant-time successor and predecessor lookups. */
    private static Map<UUID, TaskEvidence> index(TaskSnapshot snapshot) {
        Map<UUID, TaskEvidence> result = new HashMap<>();
        snapshot.tasks().forEach(task -> result.put(task.taskId(), task));
        return result;
    }
}
