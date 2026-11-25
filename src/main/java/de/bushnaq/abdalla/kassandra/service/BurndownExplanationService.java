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
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Assembles a deterministic, selected-sprint review brief from chart evidence and independent contributors.
 * The brief keeps observed signals separate and never subtracts them from the chart gap as if causation were measured.
 */
@Service
public class BurndownExplanationService {

    /** Whether the chart supplies a positive gap that is still unquantified, or no applicable comparison. */
    public enum GapAttribution {
        /** No comparable actual and guide points were available. */
        NO_COMPARISON,
        /** A comparison exists, but actual remaining work is not above the guide. */
        NO_POSITIVE_GAP,
        /** A positive gap exists, but this brief does not attribute it to any evidence. */
        NOT_QUANTIFIED
    }

    /** Suggested human review focus; not a claim that the listed evidence caused the chart result. */
    public enum Action {
        /** Review an observed chain of explicit unfinished prerequisites. */
        REVIEW_BLOCKED_HANDOFF,
        /** Review the recorded estimate and remaining work for an overrun-only group. */
        REVIEW_ESTIMATE_AND_REMAINING_WORK
    }

    /**
     * Task observations joined by explicit dependency links for one focused review.
     *
     * @param id stable representative task identifier used internally to order groups
     * @param blockers observed blocked successors in this connected group
     * @param overruns observed estimate crossings in this connected group
     * @param suggestedAction human review focus derived from the observations present
     */
    public record TaskGroup(UUID id, List<DependencyBlockerContributor.Observation> blockers,
                            List<EstimateOverrunContributor.Observation> overruns, Action suggestedAction) {
        /**
         * Freezes observations so the group remains a stable brief value.
         * @param id stable representative task identifier used internally to order groups
         * @param blockers observed blocked successors in this connected group
         * @param overruns observed estimate crossings in this connected group
         * @param suggestedAction human review focus derived from the observations present
         */
        public TaskGroup {
            blockers = List.copyOf(blockers);
            overruns = List.copyOf(overruns);
        }
    }

    /**
     * Reports how much task-level history could be compared without treating unavailable data as a negative finding.
     *
     * @param dependencyCoverage comparison window status for dependency evidence
     * @param unavailableDependencyTasks candidate tasks with insufficient dependency history or inputs
     * @param estimateCoverage comparison window status for estimate evidence
     * @param comparedEstimateTasks tasks with comparable baseline and current work totals
     * @param unavailableEstimateTasks effort tasks that could not be compared
     */
    public record TaskCoverage(DependencyBlockerContributor.Coverage dependencyCoverage, int unavailableDependencyTasks,
                               EstimateOverrunContributor.Coverage estimateCoverage, int comparedEstimateTasks,
                               int unavailableEstimateTasks) {
    }

    /**
     * Deterministic output combining chart comparison, task groups, aggregate capacity, and a separate forward outlook.
     *
     * @param chart last-completed-day chart comparison, if available
     * @param gapAttribution indicates whether a positive comparison gap exists without assigning its cause
     * @param taskGroups ranked task observations connected by explicit dependencies
     * @param taskCoverage coverage for the historical task comparisons
     * @param capacity aggregate sickness-overlap observation, independent of task groups
     * @param outlook comparison with a forward remaining-work schedule
     */
    public record Brief(BurndownEvidenceService.Evidence chart, GapAttribution gapAttribution, List<TaskGroup> taskGroups,
                        TaskCoverage taskCoverage, CapacityOverlapContributor.Result capacity,
                        ReleaseForecastContributor.ForecastComparison outlook) {
        /**
         * Freezes ranked task groups so downstream ranking or rendering cannot mutate the brief.
         * @param chart last-completed-day chart comparison, if available
         * @param gapAttribution indicates whether a positive comparison gap exists without assigning its cause
         * @param taskGroups ranked task observations connected by explicit dependencies
         * @param taskCoverage coverage for the historical task comparisons
         * @param capacity aggregate sickness-overlap observation, independent of task groups
         * @param outlook comparison with a forward remaining-work schedule
         */
        public Brief {
            taskGroups = List.copyOf(taskGroups);
        }
    }

    @Autowired
    private BurndownEvidenceService evidenceService;
    @Autowired
    private EstimateOverrunContributor estimateContributor;
    @Autowired
    private DependencyBlockerContributor dependencyContributor;
    @Autowired
    private CapacityOverlapContributor capacityContributor;
    @Autowired
    private ReleaseForecastContributor forecastContributor;

    /**
     * Builds a deterministic review brief from a live sprint and an optional earlier capture. Dependency links
     * connect observations for review; they do not establish that an overrun caused a blocker.
     *
     * @param sprint already authorized sprint used to build the chart
     * @param chart live chart for that sprint
     * @param prior earlier capture for the same sprint, or {@code null} when unavailable
     * @return scoped observations, coverage and a separate release outlook
     * @throws IllegalArgumentException if the supplied chart or prior capture belongs to another sprint
     */
    public Brief explain(Sprint sprint, GanttBurndownChartDto chart, BurndownEvidenceService.TaskSnapshot prior) {
        // Capture chart and task facts together so every contributor observes the same selected-sprint cutoff.
        var current = evidenceService.capture(sprint, chart);
        // Historical comparisons require a prior capture; contributors report unavailable coverage when it is absent.
        var dependencies = dependencyContributor.evaluate(current, prior);
        var estimates = estimateContributor.evaluate(current, prior);
        var comparison = current.chart().comparison();
        // A positive gap is a review context, not a basis for attributing any portion to a contributor.
        var attribution = comparison == null ? GapAttribution.NO_COMPARISON
                : comparison.gapSeconds() > 0 ? GapAttribution.NOT_QUANTIFIED : GapAttribution.NO_POSITIVE_GAP;
        var coverage = new TaskCoverage(dependencies.coverage(), dependencies.unavailableTasks(), estimates.coverage(),
                estimates.comparedTasks(), estimates.unavailableTasks());
        // Keep capacity and forward outlook separate: neither is linked to historical task groups by date alone.
        return new Brief(current.chart(), attribution, group(dependencies.observations(), estimates.observations()), coverage,
                capacityContributor.evaluate(sprint, chart), forecastContributor.evaluate(sprint, chart));
    }

    private static List<TaskGroup> group(List<DependencyBlockerContributor.Observation> blockers,
                                         List<EstimateOverrunContributor.Observation> overruns) {
        // Build an undirected graph only from observed explicit prerequisite links; resource-ordering edges are excluded.
        Map<UUID, Set<UUID>> links = new TreeMap<>();
        Map<UUID, DependencyBlockerContributor.Observation> blockersByTask = new HashMap<>();
        Map<UUID, EstimateOverrunContributor.Observation> overrunsByTask = new HashMap<>();
        for (var blocker : blockers) {
            blockersByTask.put(blocker.taskId(), blocker);
            Set<UUID> prerequisites = links.computeIfAbsent(blocker.taskId(), ignored -> new HashSet<>());
            for (UUID predecessor : blocker.unfinishedPredecessors()) {
                prerequisites.add(predecessor);
                links.computeIfAbsent(predecessor, ignored -> new HashSet<>()).add(blocker.taskId());
            }
        }
        for (var overrun : overruns) {
            overrunsByTask.put(overrun.taskId(), overrun);
            links.computeIfAbsent(overrun.taskId(), ignored -> new HashSet<>());
        }

        List<TaskGroup> groups = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        // Each connected component is one review group; standalone overruns remain their own groups.
        for (UUID id : links.keySet()) {
            if (!visited.add(id))
                continue;
            List<DependencyBlockerContributor.Observation> groupBlockers = new ArrayList<>();
            List<EstimateOverrunContributor.Observation> groupOverruns = new ArrayList<>();
            Deque<UUID> pending = new ArrayDeque<>();
            pending.add(id);
            // Traverse the component iteratively so multi-step dependency chains are kept together.
            while (!pending.isEmpty()) {
                UUID task = pending.removeFirst();
                if (blockersByTask.containsKey(task))
                    groupBlockers.add(blockersByTask.get(task));
                if (overrunsByTask.containsKey(task))
                    groupOverruns.add(overrunsByTask.get(task));
                for (UUID neighbor : links.get(task))
                    if (visited.add(neighbor))
                        pending.addLast(neighbor);
            }
            // Stable ordering makes the baseline reproducible and helps compare it with model-selected rankings.
            groupBlockers.sort(Comparator.comparing(DependencyBlockerContributor.Observation::priorScheduledStart)
                    .thenComparing(DependencyBlockerContributor.Observation::taskId));
            groupOverruns.sort(Comparator.comparingLong(EstimateOverrunContributor.Observation::excessSeconds).reversed()
                    .thenComparing(EstimateOverrunContributor.Observation::taskId));
            Action action = groupBlockers.isEmpty() ? Action.REVIEW_ESTIMATE_AND_REMAINING_WORK : Action.REVIEW_BLOCKED_HANDOFF;
            groups.add(new TaskGroup(id, groupBlockers, groupOverruns, action));
        }
        // Apply one consistent ranking after grouping, independent of map traversal or snapshot task order.
        return rankTaskGroups(groups);
    }

    /** Orders groups by blocked handoffs, actionable unfinished overruns, then largest excess and stable ID. */
    static List<TaskGroup> rankTaskGroups(List<TaskGroup> groups) {
        return groups.stream().sorted(Comparator.<TaskGroup>comparingInt(value -> value.blockers().size()).reversed()
                .thenComparing(group -> group.blockers().isEmpty() && group.overruns().stream().noneMatch(overrun ->
                        overrun.currentStatus() == TaskStatus.TODO || overrun.currentStatus() == TaskStatus.IN_PROGRESS))
                .thenComparing(Comparator.comparingLong(BurndownExplanationService::largestOverrun).reversed())
                .thenComparing(TaskGroup::id)).toList();
    }

    /** Returns the largest observed excess, or zero when this group contains no overrun. */
    private static long largestOverrun(TaskGroup group) {
        return group.overruns().stream().mapToLong(EstimateOverrunContributor.Observation::excessSeconds).max().orElse(0);
    }
}
