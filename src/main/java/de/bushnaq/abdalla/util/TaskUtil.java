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

package de.bushnaq.abdalla.util;

import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.report.dao.MetaData;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;


@Slf4j
public class TaskUtil {

    private static final long SECONDS_PER_HOUR        = 60 * 60;
    private static final long SECONDS_PER_WORKING_DAY = 75L * 6L * 60L; // 27000

    /**
     * Returns {@code true} when all predecessors of the given task—including predecessors
     * declared on any ancestor task in the parent hierarchy—have a zero
     * {@code remainingEstimate} (i.e. are done).
     * <p>
     * Predecessors that cannot be resolved within the sprint (e.g. cross-sprint relations)
     * are treated as done so they do not block execution.
     * </p>
     *
     * @param task   the task to check
     * @param sprint the sprint that owns the task
     * @return {@code true} if no unfinished predecessors exist; {@code false} otherwise
     */
    public static boolean areAllPredecessorsDone(Task task, Sprint sprint) {
        Task cursor = task;
        while (cursor != null) {
            for (Relation relation : cursor.getPredecessors()) {
                Task predecessor = sprint.getTaskById(relation.getPredecessorId());
                if (predecessor != null && !predecessor.getRemainingEstimate().isZero()) {
                    return false;
                }
            }
            cursor = cursor.getParentTask();
        }
        return true;
    }

    protected static LocalDate calculateDayFromIndex(LocalDate firstDate, int index) {
        return DateUtil.addDay(firstDate, index);
    }

    private static int calculateDayIndex(LocalDate firstDate, LocalDate date) {
        return DateUtil.calculateDays(firstDate, date);
    }

    public static HashMap<Task, MetaData> createMetaData(Sprint projectFile) {
        //TODO fix creating metadata
//        ProjectProperties properties = projectFile.getProjectProperties();
//        if (properties.getCustomProperties() == null || properties.getCustomProperties().get(MetaData.METADATA) == null) {
//            Map<String, Object>     customProperties = new HashMap<>();
//            HashMap<Task, MetaData> mdMap            = new HashMap<>();
//            customProperties.put(MetaData.METADATA, mdMap);
//            properties.setCustomProperties(customProperties);
//            return mdMap;
//        } else {
//            return (HashMap<Task, MetaData>) projectFile.getProjectProperties().getCustomProperties().get(MetaData.METADATA);
//        }
        return null;
    }

    public static MetaData getTaskMetaData(Task task) {
//        HashMap<Task, MetaData> mdMap = createMetaData(task.getParentFile());
//        return mdMap.get(task);
        return null;
    }

    public static Duration getTaskWorkPerDay(Sprint sprint, LocalDate day, Task task, boolean print) {
        Duration      work       = Duration.ZERO;
        LocalDateTime startOfDay = day.atStartOfDay().plusHours(8);
        if (task.isTask() && task.isImpactOnCost()) {
            //has impact on cost
            if (!day.isBefore(task.getStart().toLocalDate())) {
                // Day is on or after task start
                if (task.getEffectiveCalendar().isWorkingDate(day)) {
                    // is a working day for this user
//                    if (task.getStart().isBefore(startOfDay) || task.getStart().isEqual(startOfDay))
                    {
                        if (!task.getRemainingEstimate().isZero()) {
                            if (areAllPredecessorsDone(task, sprint)) {
                                Number        availability   = task.getAvailability();
                                LocalDateTime start          = task.getStart();
                                LocalDateTime stop           = task.getFinish();
                                int           startDayIndex  = calculateDayIndex(sprint.getStart().toLocalDate(), start.toLocalDate());
                                int           stopDayIndex   = calculateDayIndex(sprint.getStart().toLocalDate(), stop.toLocalDate());
                                int           dayIndex       = calculateDayIndex(sprint.getStart().toLocalDate(), day);
                                LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(day.atStartOfDay());
                                LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(day.atStartOfDay());
                                LocalDateTime dayStartTime   = DateUtil.calculateDayStartTime(day.atStartOfDay());
                                LocalDateTime dayEndTime     = DateUtil.calculateDayEndTime(day.atStartOfDay());


                                if (dayIndex >= startDayIndex /*&& dayIndex <= stopDayIndex*/) {
                                    if (dayIndex == startDayIndex && dayIndex == stopDayIndex) {
                                        //start and stop on this day
                                        // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
                                        // start time might not be in the morning at exactly 8:00
                                        // stop time might not be in the afternoon 16:30
                                        work = Duration.between(start, stop);
                                        work = Duration.ofSeconds((long) (work.toSeconds() * availability.doubleValue()));
                                        //is lunch between start and stop?
                                        if (start.isBefore(lunchStartTime) && stop.isAfter(lunchStopTime)) {
                                            work = work.minus(Duration.ofHours(1));
                                            if (print)
                                                log.info("[1] {} {} start <= 12:00 && stop >= 13:00 work= {}", day, task.getName(), work);
                                        } else {
                                            if (print)
                                                log.info("[1] {} {} start & stop <= 12:00 work= {}", day, task.getName(), work);
                                        }

                                    } else if (dayIndex == startDayIndex) {
                                        //start on this day
                                        // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
                                        // start time might not be in the morning at exactly 8:00
                                        work = Duration.between(start, dayEndTime);
                                        if (start.isBefore(lunchStartTime)) {
                                            work = work.minus(Duration.ofHours(1));
                                            work = Duration.ofSeconds((long) (work.toSeconds() * availability.doubleValue()));
                                            if (print)
                                                log.info("[2] {} {} start <= 12:00 & stop > today work= {}", day, task.getName(), work);
                                        } else {
                                            work = Duration.ofSeconds((long) (work.toSeconds() * availability.doubleValue()));
                                            if (print)
                                                log.info("[2] {} {} start > 13:00 & stop > today work= {}", day, task.getName(), work);
                                        }
                                    } else if (dayIndex == stopDayIndex) {
                                        //stop on this day
                                        // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
                                        // last day, stop time might not be in the afternoon 16:30
                                        work = Duration.between(dayStartTime, dayEndTime).minus(Duration.ofHours(1));
//                                        work = work.minus(Duration.ofHours(1));
                                        work = Duration.ofSeconds((long) (work.toSeconds() * availability.doubleValue()));
                                        //we need to ensure that if there is time, the user keeps on working on this task until it is done.
                                        //how much would that user be able to work today additionally?
//                                        Duration additionalUserWork = Duration.ofSeconds((long) (Duration.between(stop, dayEndTime).toSeconds() * availability.doubleValue()));
//                                        if (task.getRemainingEstimate().minus(work).minus(additionalUserWork).isNegative()) {
//                                            work = task.getRemainingEstimate();
//                                        }
                                        if (print)
                                            log.info("[3] {} {} start < today & stop >= 13:00 work= {}", day, task.getName(), work);
                                    } else {
                                        //start && stop not on this day
                                        // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
                                        work = Duration.between(dayStartTime, dayEndTime).minusHours(1);
                                        work = Duration.ofSeconds((long) (work.toSeconds() * availability.doubleValue()));
                                        if (print)
                                            log.info("[4] {} {} start < today & stop >= today work= {}", day, task.getName(), work);
                                    }
                                }

//                                LocalDateTime start         = task.getStart();
//                                LocalDateTime stop          = task.getFinish();
//                                int           startDayIndex = calculateDayIndex(sprint.getStart().toLocalDate(), start.toLocalDate());
//                                int           stopDayIndex  = calculateDayIndex(sprint.getStart().toLocalDate(), stop.toLocalDate());
//                                long          oneDay        = 75 * SECONDS_PER_HOUR / 10;
//                                if (stopDayIndex == startDayIndex) {
//                                    // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
//                                    // start time might not be in the morning at exactly 8:00
//                                    // stop time might not be in the afternoon 15:30
//                                    LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(start);
//                                    LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(start);
//                                    if (start.isBefore(lunchStartTime) || start.isEqual(lunchStartTime)) {
//                                        // if(start <= 12:00)
//                                        if (stop.isBefore(lunchStartTime) || stop.isEqual(lunchStartTime)) {
//                                            // if(stop <= 12:00)
//                                            // (stop - start)/7.5
//                                            work = Duration.between(start, stop);
//                                            if (print)
//                                                System.out.printf("[1] %s start & stop <= 12:00 work=%s%n", task.getName(), work);
//                                        } else if (stop.isAfter(lunchStopTime) || stop.isEqual(lunchStopTime)) {
//                                            // if(stop >= 13:00)
//                                            // ( stop - start -1h )/7.5
//                                            work = Duration.between(start, stop).minusHours(1);
//                                            if (print)
//                                                System.out.printf("[2] %s start <= 12:00 & stop >= 13:00 work=%s%n", task.getName(), work);
//                                        } else {
//                                        }
//                                    } else if (start.isAfter(lunchStopTime) || start.isEqual(lunchStopTime)) {
//                                        // if(start >= 13:00 && stop >= 13:00)
//                                        // (stop - Start))/7.5
//                                        work = Duration.between(start, stop);
//                                        if (print)
//                                            System.out.printf("[3] %s start >= 13:00 & stop >= 13:00 work=%s%n", task.getName(), work);
//                                    } else {
//                                    }
//
//                                } else {
//                                    // start
//                                    {
//                                        // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 -  16:30 (7.5h, with lunch time at 12:00)
//                                        // start time might not be in the morning at exactly 8:00
//                                        LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(start);
//                                        LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(start);
//                                        if (start.isBefore(lunchStartTime) || start.isEqual(lunchStartTime)) {
//                                            work = Duration.between(start.toLocalTime(), LocalTime.of(16, 30)).minusHours(1);
//                                            if (print)
//                                                System.out.printf("[4] %s start <= 12:00 & stop > today work=%s%n", task.getName(), work);
//                                        } else if (start.isAfter(lunchStopTime) || start.isEqual(lunchStopTime)) {
//                                            work = Duration.between(start.toLocalTime(), LocalTime.of(16, 30));
//                                            if (print)
//                                                System.out.printf("[5] %s start  >= 13:00 & stop > today work=%s%n", task.getName(), work);
//                                        } else {
//                                        }
//                                    }
//                                    // end
//                                    {
//                                        // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
//                                        // last day, stop time might not be in the afternoon 16:30
//                                        LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(stop);
//                                        LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(stop);
//                                        if (stop.isBefore(lunchStartTime) || stop.isEqual(lunchStartTime)) {
//                                            // if(stop <= 12:00)
//                                            // (stop - 8:00)/7.5
//                                            work = Duration.between(LocalTime.of(8, 0), stop.toLocalTime());
//                                            if (print)
//                                                System.out.printf("[6] %s start < today & stop <= 12:00 work=%s%n", task.getName(), work);
//                                        } else if (stop.isAfter(lunchStopTime) || stop.isEqual(lunchStopTime)) {
//                                            // if(stop >= 13:00)
//                                            // (stop - 8:00 -1h)/7.5
//                                            work = Duration.between(LocalTime.of(8, 0), stop.toLocalTime()).minusHours(1);
//                                            if (print)
//                                                System.out.printf("[7] %s start < today & stop >= 13:00 work=%s%n", task.getName(), work);
//                                        } else {
//                                        }
//                                    }
//                                }
//                                if (startDayIndex + 1 < stopDayIndex) {
//                                    for (int index = startDayIndex + 1; index < stopDayIndex; index++) {
//                                        LocalDate today = calculateDayFromIndex(sprint.getStart().toLocalDate(), index);
//                                        if (task.getEffectiveCalendar().isWorkingDate(today))
////                                    if (isResourceWorkingDay(context, task.getAssignedUser(), today))
//                                        {
//                                            work = Duration.between(LocalTime.of(8, 0), LocalTime.of(16, 30)).minusHours(1);
//                                            if (print)
//                                                System.out.printf("[8] %s start < today & stop > today fraction=1.0 work=%s%n", task.getName(), work);
//                                        }
//                                    }
//                                }
//                            } else {
//                                log.debug("Task '{}' blocked on {} – predecessor not yet done", task.getName(), day);
//                            }
                            }
                        }
                    }
                }
            }
        }
        return work;
    }

    public static void putTaskMetaData(Task task, MetaData md) {
//        HashMap<Task, MetaData> mdMap = createMetaData(task.getParentFile());
//        mdMap.put(task, md);
    }
}
