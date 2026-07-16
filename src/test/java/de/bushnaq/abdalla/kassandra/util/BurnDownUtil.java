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

package de.bushnaq.abdalla.kassandra.util;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import de.bushnaq.abdalla.util.TaskUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
public class BurnDownUtil {
    /**
     * Probability that a sprint runs with zero delay (on schedule).
     */
    private static final float DELAY_PROBABILITY = 0.4f;

    /**
     * Maximum over-run factor applied to {@code maxEstimate} when a sprint is delayed.
     */
    private static final float MAX_DELAY_FACTOR = 0.3f;

    public static void generateWorkLogs(PersistingEntityGenerator peg) throws Exception {
        log.info("--------------------------");
        log.info("Generating work logs start");
        for (Sprint sprint : peg.getSprints()) {
            log.info("--------------------------");
            log.info("Generating work logs start for {}", sprint.getName());
//            sprint.initializeCalendars();//make sure the new exceptions are in the user calendars.
            // ~60 % of sprints run on schedule; ~40 % carry a delay of up to MAX_DELAY_FACTOR.
            float delay = peg.random.nextFloat() < (1f - DELAY_PROBABILITY) ? 0.0f : peg.random.nextFloat() * MAX_DELAY_FACTOR;
            log.debug("Sprint '{}' delay factor: {}", sprint.getName(), delay);
            generateWorklogs(peg, sprint, delay, ParameterOptions.getLocalNow());
            log.info("--------------------------");
        }
        log.info("Generating work logs end");
        log.info("--------------------------");
    }

    /**
     * Generates worklogs for the tasks in the sprint, simulating a team working day-by-day.
     * <p>
     * <b>Estimate inflation (delay &gt; 0):</b> before the day-loop starts, each leaf task's
     * {@code remainingEstimate} is inflated in-memory to a value sampled from
     * {@code [maxEstimate, maxEstimate * (1 + delay)]}, so that delayed sprints exceed the
     * worst-case estimate.  Tasks in sprints with {@code delay == 0} keep their original
     * {@code minEstimate}.
     * </p>
     * <p>
     * <b>Dependency gate:</b> work is only logged for a task when all of its predecessors—
     * including predecessors declared on any ancestor task in the hierarchy—have a zero
     * {@code remainingEstimate} (i.e. are effectively done).
     * </p>
     *
     * @param peg
     * @param sprint the sprint to generate worklogs for
     * @param delay  over-run factor (0 = no over-run; 0.3 = up to 30 % above {@code maxEstimate})
     * @param now    the simulated current date/time; work is only logged for days before this date
     */
//    @Transactional
    public static void generateWorklogs(PersistingEntityGenerator peg, Sprint sprint, float delay, LocalDateTime now) {
        try (Profiler pc = new Profiler(SampleType.CPU)) {

            final long SECONDS_PER_WORKING_DAY = 75 * 6 * 60;

            // Step 1: pre-inflate remaining estimates when this sprint is delayed.
//            if (delay > 0f) {
//                for (Task task : sprint.getTasks()) {
//                    if (task.isTask() && task.isImpactOnCost()) {
//                        Duration maxEstimate = task.getMaxEstimate();
//                        if (maxEstimate != null && !maxEstimate.isZero()) {
//                            long     extraSeconds = (long) (delay * maxEstimate.getSeconds() * peg.random.nextFloat());
//                            Duration inflated     = maxEstimate.plusSeconds(extraSeconds);
//                            log.debug("Inflating task '{}': minEstimate={}, inflated remainingEstimate={} (delay={})", task.getName(), task.getMinEstimate(), inflated, delay);
//                            task.setRemainingEstimate(inflated);
//                        }
//                    }
//                }
//            }
            Duration rest = Duration.ofSeconds(1);
            // Step 2: iterate over the days of the sprint
            for (LocalDate day = sprint.getStart().toLocalDate(); !rest.equals(Duration.ZERO) && now.toLocalDate().isAfter(day); day = day.plusDays(1)) {
                LocalDateTime startOfDay = day.atStartOfDay().plusHours(8);
                rest = Duration.ZERO;
                // iterate over all tasks
                for (Task task : sprint.getTasks()) {
                    if (task.isTask() && task.isImpactOnCost()) {
                        if (!task.getRemainingEstimate().isZero()) {
                            //still work to be done
                            if (!day.isBefore(task.getStart().toLocalDate())) {
                                // Day is on or after task start && on or before finish
//                                if (sprint.getName().equals("Paris") && task.getAssignedUser().getName().equals("Christopher Paul")) {
//                                    if (task.getEffectiveCalendar().isWorkingDate(LocalDate.of(2025, 8, 20))) {
//                                        log.error("mist");
//                                    }
//                                }

                                if (task.getEffectiveCalendar().isWorkingDate(day)) {
                                    // is a working day for this user
                                    if (TaskUtil.areAllPredecessorsDone(task, sprint)) {
                                        // we have the whole day
                                        double performance = 1f;//daily performance is usually 100% of the resource availability
//                                            if (peg.random.nextFloat() < 0.2f) {
//                                                //in rare cases, performance can be much worse or better than usual, e.g. due to unexpected problems or overtime
//                                                double minPerformance = 0.5f;//minimum performance of a resource (underwork)
//                                                double maxPerformance = 1.2f;//maximum performance of a resource (overwork)
//                                                performance = minPerformance + peg.random.nextFloat() * (maxPerformance - minPerformance);
//                                            }
//                                            if (sprint.getName().equals("Sydney")) {
//                                                System.out.println();
//                                            }
                                        Duration maxTaskWork = TaskUtil.getTaskWorkPerDay(sprint, day, task, true);
                                        Duration maxWork     = Duration.ofSeconds((long) ((performance * maxTaskWork.getSeconds())));//TODO use user calendar for working day length
                                        Duration w           = maxWork;
                                        Duration delta       = task.getRemainingEstimate().minus(w);
                                        if (delta.isNegative()) {
                                            w = task.getRemainingEstimate();
                                        }
                                        if (day.atStartOfDay().toLocalDate().isEqual(task.getStart().toLocalDate())) {
                                            //first day
                                            peg.addWorklogToBuffer(task, task.getAssignedUser(), task.getStart(), w, task.getName());
                                        } else {
                                            peg.addWorklogToBuffer(task, task.getAssignedUser(), day.atStartOfDay().plusHours(8), w, task.getName());//TODO use user calendar
                                        }
                                        task.calculateStatus(true);
                                    } else {
                                        log.debug("Task '{}' blocked on {} – predecessor not yet done", task.getName(), day);
                                    }
                                }
                            }
                        }
                    }
                    rest = rest.plus(task.getRemainingEstimate()); // accumulate the rest
                }
            }
            sprint.recalculate(ParameterOptions.getLocalNow());
        }
        peg.flushWorklogBuffer(sprint);
        peg.persistTasksAndSprint(sprint);
    }
}
