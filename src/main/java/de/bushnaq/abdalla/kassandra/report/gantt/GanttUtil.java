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

package de.bushnaq.abdalla.kassandra.report.gantt;

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.MpxjUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.time.temporal.ChronoUnit.SECONDS;

public class GanttUtil {
    private static final String DELIVERY_BUFFER          = "Delivery buffer (from critical path tasks)";
    private static final String DELIVERY_BUFFER_LEGACY_1 = "delivery buffer";
    private static final String DELIVERY_BUFFER_LEGACY_2 = "time contingency reserve";

    private static final String    ERROR_103_TASK_IS_MANUALLY_SCHEDULED_AND_CANNOT_FULLFILL_ITS_DEPENDENCY = "Error #103: Task [%d]'%s' is manually scheduled and cannot fullfill its dependency to task [%d]'%s'.";
    private static final String    ERROR_104_TASK_CANNOT_FULLFILL_ITS_DEPENDENCY                           = "Error #104: Task [%d]'%s' start %s cannot fullfill its dependency to task [%d]'%s' finish %s.";
    private final        Context   context;
    private final        Task      deliveryBufferTask                                                      = null;
    private final        Set<Task> finishSet                                                               = new HashSet<>();
    //    private long count = 0;
    private final        DateUtil  localDateTimeUtil                                                       = new DateUtil();
    private final        Logger    logger                                                                  = LoggerFactory.getLogger(this.getClass());
    private final        Set<Task> manualSet                                                               = new HashSet<>();
    int maxLoop;
    //    private              ProjectProperties projectProperties                                                       = null;
    private final Set<Task> startSet = new HashSet<>();
    int testCriticalCounter;

    public GanttUtil(Context context) {
        this.context = context;
    }

//    private Duration calculateDeliveryBuffer(Sprint projectFile) throws ProjectsDashboardException {
//        Duration deliveryBuffer = Duration.ZERO;
//        int      i              = 0;
//        for (Task task : projectFile.getTasks()) {
//            for (ResourceAssignment resourceAssignment : task.getResourceAssignments()) {

    /// /                Number   units = resourceAssignment.getUnits();
//                Duration work = task.getWork();
//                if (work != null) {
//                    if (task.isCritical()) {
//
//                        MetaData md = TaskUtil.getTaskMetaData(task);
//                        if (md != null) {
//                            if (md.risk != null) {
//                                Double risk         = md.risk;
//                                double availability = 100 / units.doubleValue();
//                                //            double a = Math.ceil(work.getDuration() * availability * 60 * 60) / (60 * 60);
//                                //            double d = work.getDuration();
//                                //            double a = availability * work.getDuration();
//                                double a = work.getDuration() * availability;
//                                //            double seconds = (Math.round(a * 60 * 10) * 6);
//                                //                    a = (Math.round(a * 60 * 10) * 6) / (60 * 10 * 6.0);
//                                Duration duration = Duration.getInstance(a * risk, work.getUnits());
//                                //            duration = duration.convertUnits(TimeUnit.HOURS, projectProperties);
//                                //            String ds = DateUtil.createDurationString(duration, true, true, true);
//                                deliveryBuffer = Duration.add(deliveryBuffer, duration, projectProperties);
//
//                                //delivery buffer = (effort/availability)*risk
//                                logger.info(String.format("Delivery buffer #%d for task '%s' %s%s.", i++, task.getName(), deliveryBuffer.getDuration(),
//                                        deliveryBuffer.getUnits().getName()));
//                            } else if (md.maxDuration != null) {
//                                double maxDuration  = md.maxDuration * 7.5;
//                                double availability = 100 / units.doubleValue();
//                                //            double a = Math.ceil(work.getDuration() * availability * 60 * 60) / (60 * 60);
//                                //            double d = work.getDuration();
//                                //            double a = availability * work.getDuration();
//                                double a = work.getDuration() * availability;
//                                //            double seconds = (Math.round(a * 60 * 10) * 6);
//                                //                    a = (Math.round(a * 60 * 10) * 6) / (60 * 10 * 6.0);
//                                Duration duration = Duration.getInstance(maxDuration - a, work.getUnits());
//                                if (duration.getDuration() > 1f / (60 * 10)) {
//                                    //            duration = duration.convertUnits(TimeUnit.HOURS, projectProperties);
//                                    //            String ds = DateUtil.createDurationString(duration, true, true, true);
//                                    deliveryBuffer = Duration.add(deliveryBuffer, duration, projectProperties);
//
//                                    //delivery buffer = (effort/availability)*risk
//                                    logger.info(String.format("Delivery buffer #%d for task '%s' %s.", i++, task.getName(),
//                                            DateUtil.createWorkDayDurationString(MpxjUtil.toJavaDuration(duration), false, true, false)));
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            //            logger.info(String.format("Delivery buffer %.2f%s.", deliveryBuffer.getDuration(), deliveryBuffer.getUnits().getName()));
//        }
//        return deliveryBuffer;
//    }
    public static int calculateNumberOfTasks(Sprint sprint) {
        int size = 0;
        for (Task task : sprint.getTasks()) {
            if (isValidTask(task)) {
                size++;
            }
        }
        return size;
    }

    public void createResourceDependencies(Sprint projectFile) throws Exception {
        try (Profiler pc = new Profiler(SampleType.CPU)) {
            boolean anythingChanged = false;
            do {
                anythingChanged = false;
                anythingChanged = createResourceDependencies(projectFile, anythingChanged);
            } while (anythingChanged);
        }
    }

    /**
     * Creates missing dependencies between tasks that are assigned to the same resource,
     * to allow critical path calculation by just taking dependencies into consideration
     *
     * @param sprint
     * @param anythingChanged
     * @return
     */
    private boolean createResourceDependencies(Sprint sprint, boolean anythingChanged) {
        for (Task task1 : sprint.getTasks()) {
            //find overlapping tasks with same resource assignment
            for (Task task2 : sprint.getTasks()) {
                if (task1.getId() < task2.getId()) {
                    if (useSameAssignee(task1, task2)) {
                        if (overlap(task1, task2)) {
                            if (!hasDependency(task1, task2)) {
                                //move second one after first one
//                                Relation r     = new Relation(task1, false);
//                                Relation saved = relationApi.persist(r);
//                                task2.addPredecessor(saved);
                                task2.addPredecessor(task1, false);
                                anythingChanged = true;
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return anythingChanged;
    }

    public static boolean equals(ProjectCalendar calendar, LocalDateTime d1, LocalDateTime d2) {
        if (d1 == null && d2 == null) {
            return true;
        }
        if (d1 == null || d2 == null) {
            return false;
        }
        if (d1.equals(d2)) {
            return true;
        }
        //TODO reintroduce calendar fixed
        {
            //Date might be end of a working day, while reference is beginning of next working day?
            LocalDateTime nextDay = calendar.getNextWorkStart(d1);

            if (nextDay.equals(d2)) {
                return true;
            }
        }
        {
            //Date might be beginning of next day working day, while reference is end of previous working day?
            LocalDateTime nextDay = calendar.getNextWorkStart(d2);
            return d1.equals(nextDay);
        }
    }

    public static boolean equals(Duration duration, Duration duration2) {
        return DateUtil.createDurationString(duration, true, true, true).equals(DateUtil.createDurationString(duration2, true, true, true));
    }

//    /**
//     * equal timestamps even if there is a out of office duration in between
//     *
//     * @param calendar
//     * @param d1
//     * @param d2
//     * @return
//     */
//    public static boolean equals(ProjectCalendar calendar, LocalDateTime d1, LocalDateTime d2) {
//        if (d1 == null && d2 == null) {
//            return true;
//        }
//        if (d1 == null || d2 == null) {
//            return false;
//        }
//        if (d1.equals(d2)) {
//            return true;
//        }
//        {
//            //Date might be end of a working day, while reference is beginning of next working day?
//            //            LocalDateTime nextDay = calendar.getDate(d1, Duration.getInstance(0, TimeUnit.DAYS));
//            LocalDateTime nextDay = calendar.getNextWorkStart(d1);
//
//            if (nextDay.equals(d2)) {
//                return true;
//            }
//        }
//        {
//            //Date might be beginning of next day working day, while reference is end of previous working day?
//            //            LocalDateTime nextDay = calendar.getDate(d2, Duration.getInstance(0, TimeUnit.DAYS));
//            LocalDateTime nextDay = calendar.getNextWorkStart(d2);
//            return d1.equals(nextDay);
//        }
//    }

    public static ProjectCalendar getCalendar(Task task) {
        return task.getEffectiveCalendar();
    }


    public Task getDeliveryBufferTask() {
        return deliveryBufferTask;
    }

    private Duration getDurationFromWork(GanttErrorHandler eh, Task task) {
        float availability = 1;//tasks without resources have 100% availability
        if (task.getAssignedUser() != null) {
            User resourceAssignment = task.getAssignedUser();
            availability = resourceAssignment.getAvailabilities().getLast().getAvailability();
        }
        Duration work = task.getOriginalEstimate();
        if (work != null) {
            {
                double inverseAvailability = 1 / availability;
                double durationUnits       = inverseAvailability * work.getSeconds();
                durationUnits = Math.round(durationUnits / 6) * 6;

                return Duration.of((long) durationUnits, SECONDS);
            }
        } else {
            return Duration.ZERO;
        }
    }

    public static LocalDateTime getEarliestStartDate(Sprint projectFile) {
        LocalDateTime earliestDate = null;
        for (Task task : projectFile.getTasks()) {
            if (GanttUtil.isValidTask(task)) {
                if (!task.isMilestone() && (task.getChildTasks().isEmpty()) && (task.getDuration() != null && !task.getDuration().isZero())) {
                    if (earliestDate == null || task.getStart().isBefore(earliestDate)) {
                        earliestDate = task.getStart();
                    }
                } else {
                    //ignore milestones
                }
            }
        }
        return earliestDate;
    }

    private LocalDateTime getFirstChildStart(Task task) {
        LocalDateTime start = null;
        for (Task child : task.getChildTasks()) {
            if (child.getStart() != null && (start == null || child.getStart().isBefore(start))) {
                start = child.getStart();
            }
        }
        return start;
    }

    private LocalDateTime getFirstManualChildStart(Task task) {
        LocalDateTime start = null;
        for (Task child : task.getChildTasks()) {
            if (isManual(child)) {
                if (child.getStart() != null && (start == null || child.getStart().isBefore(start))) {
                    start = child.getStart();
                }
            }
        }
        return start;
    }

    private LocalDateTime getLastChildFinish(Task task) {
        LocalDateTime finish = null;
        for (Task child : task.getChildTasks()) {
            if (child.getFinish() != null && (finish == null || child.getFinish().isAfter(finish))) {
                finish = child.getFinish();
            }
        }
        return finish;
    }

    private LocalDateTime getLastStartConstraint(Task task) {
        LocalDateTime finish = null;
        for (Relation relation : task.getPredecessors()) {
            Task sourceTask = task;
            Task targetTask = task.getSprint().getTaskById(relation.getPredecessorId());
            //            if (sourceTask.getUniqueID() == task.getUniqueID() && targetTask.getStart() != null && targetTask.getDuration() != null) {
            //                Date localFinish = calendar.getDate(targetTask.getStart(), targetTask.getDuration(), true);
            //                if (finish == null || calendar.getDate(targetTask.getStart(), targetTask.getDuration(), true).after(finish)) {
            //                    finish = localFinish;
            //                }
            //            }
            if (sourceTask.getId() == task.getId() && targetTask.getFinish() != null) {
                //                Date localFinish = calendar.getDate(targetTask.getStart(), targetTask.getDuration(), true);
                //                Date localFinish = targetTask.getFinish();
                if (finish == null || targetTask.getFinish().isAfter(finish)) {
                    finish = targetTask.getFinish();
                }
            }
        }
        if (finish != null && task.getParentTask() != null) {
            if (task.getParentTask().getStart() != null && task.getParentTask().getStart().isAfter(finish)) {
                finish = task.getParentTask().getStart();
            }
        }
        return finish;
    }

    private LocalDateTime getStart(Task task) {
        if (task == null) {
            return null;
        }
        return task.getStart();
    }

    private boolean hasChildTasks(Task task) {
        return !task.getChildTasks().isEmpty();
    }

    private boolean hasDependency(Task task1, Task task2) {

        //is task2 one of task1's its predecessors?
        for (Relation r : task1.getPredecessors()) {
            if (Objects.equals(r.getPredecessorId(), task2.getId())) {
                return true;
            }
        }
        //is task1 one of task2's its predecessors?
        for (Relation r : task2.getPredecessors()) {
            if (Objects.equals(r.getPredecessorId(), task1.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDirectDependencies(Task task) {
        if (task == null) {
            return false;
        }
        return !task.getPredecessors().isEmpty();
    }

    private boolean hasHierarchicalDependencies(Task task) {
        //the task has no predecessors
        //none of its parents has predecessor
        if (task == null) {
            return false;
        }
        if (!task.getPredecessors().isEmpty())
            return true;
        return hasHierarchicalDependencies(task.getParentTask());
    }

    private boolean hasStart(Task task) {
        return task != null && task.getStart() != null;
    }

    private boolean isDeliveryBufferTask(Task task) {
        return task.getName().equalsIgnoreCase(DELIVERY_BUFFER) || task.getName().equalsIgnoreCase(DELIVERY_BUFFER_LEGACY_1)
                || task.getName().equalsIgnoreCase(DELIVERY_BUFFER_LEGACY_2);
    }

    private boolean isManual(Task task) {
        return task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED;
    }

    public static boolean isValidTask(Task task) {
        return true;
    }

    public void levelResources(GanttErrorHandler eh, Sprint sprint, String projectRequestKey, LocalDateTime currentStartTime) {
        long time = System.currentTimeMillis();
        try {
            long checks     = 0;
            int  iterations = 0;
            logger.trace(String.format("Calculating critical path for %d tasks.", sprint.getTasks().size()));
            maxLoop = Math.max(sprint.getTasks().size() * sprint.getTasks().size(), sprint.getTasks().size() * 10);
            boolean anythingChanged = false;
            printCase("#", "ID", "Task Name", "Method", "Start", "Finish", "Duration");
            //        logger.trace(String.format("[#] [ID][Task Name           ][ Method__ start_______________ finish_______________"));
            do {
                anythingChanged = false;
                do {
//                    logger.trace(String.format("Iteration %d/%d.", iterations, maxLoop));
                    anythingChanged = false;
                    //[M] manual
                    //+manual
                    //-milestone
                    //-duration
                    //-children, this means +work
                    for (Task task : sprint.getTasks()) {
                        checks++;
                        if (isManual(task) /*&& !task.isMilestone()*/ && (task.getDuration() == null || (task.getDuration().isZero() && !task.isMilestone())) && !hasChildTasks(task)) {
                            Duration duration = getDurationFromWork(eh, task);
                            task.setDuration(duration);
                            if (task.getStart() != null && duration != null) {
                                //TODO reintroduce calendar fixed
                                ProjectCalendar calendar = getCalendar(task);
                                LocalDateTime   finish   = calendar.getDate(task.getStart(), MpxjUtil.toMpjxDuration(duration));
//                            LocalDateTime finish = task.getStart().plus(duration);
                                task.setFinish(finish);
                            }
                            anythingChanged = true;
                            printCase("M", "setFinish", task);
                        }
                    }

                    //[2]
                    for (Task task : sprint.getTasks()) {
                        checks++;
                        //-manual
                        //-children
                        //-dependencies
                        //-parent with start
                        //TODO reintroduce calendar fixed
                        ProjectCalendar calendar = getCalendar(task);
                        if (!isManual(task) && !hasChildTasks(task) && !hasHierarchicalDependencies(task) && !hasStart(task.getParentTask())
                                && !equals(calendar, currentStartTime, task.getStart())) {
                            setStart(eh, task, currentStartTime);
                            anythingChanged = true;
                            printCase("2", "setStart", task);
                        }
                    }

                    //[3]
                    {
                        for (Task task : sprint.getTasks()) {
                            checks++;
                            LocalDateTime start = getLastStartConstraint(task);
                            //-manual
                            //-children
                            //+dependency with finish
                            if (!isManual(task) && !hasChildTasks(task)) {
                                if (start != null) {
                                    //TODO reintroduce calendar fixed
                                    ProjectCalendar calendar = getCalendar(task);
                                    start = calendar.getNextWorkStart(start);//ensure we are not starting on a none-working-day
                                    if (!equals(getCalendar(task), start, task.getStart())) {
                                        setStart(eh, task, start);
                                        anythingChanged = true;
                                        printCase("3", "setStart", task);
                                    }
                                }
                            }

                        }
                    }

                    //[1]
                    {
                        for (Task task : sprint.getTasks()) {
                            checks++;
                            boolean depends = hasHierarchicalDependencies(task);
                            //-manual
                            //+children
                            //-dependency
                            LocalDateTime start = getFirstChildStart(task);
                            //TODO reintroduce calendar fixed
                            ProjectCalendar calendar = getCalendar(task);
                            if (!isManual(task) && hasChildTasks(task) && !depends) {
                                if (start != null && !equals(calendar, start, task.getStart())) {
                                    setStart(eh, task, start);
                                    anythingChanged = true;
                                    printCase("1", "setStart", task);
                                }
                            }
                            LocalDateTime finish = getLastChildFinish(task);
                            if (!isManual(task) && hasChildTasks(task) && !depends) {
                                if (finish != null && !equals(calendar, finish, task.getFinish())) {
                                    setFinish(task, finish);
                                    anythingChanged = true;
                                    printCase("1", "setFinish", task);
                                }
                            }

                        }
                    }

                    //[4]
                    {
                        for (Task task : sprint.getTasks()) {
                            checks++;
                            //-manual
                            //+children
                            //+dependencies
                            //+dependency with finish
                            //TBD +children without start
                            //TODO should also check if children have constraints for start
                            if (!isManual(task) && hasChildTasks(task) && hasDirectDependencies(task)) {
                                LocalDateTime lastStartConstraint   = getLastStartConstraint(task);
                                LocalDateTime firstManualChildStart = getFirstManualChildStart(task);
                                LocalDateTime firstChildStart       = getFirstChildStart(task);
                                //we have to start at least after the constraints and before the children
                                LocalDateTime start;
                                if (firstChildStart != null && lastStartConstraint != null
                                        && (lastStartConstraint.isBefore(firstChildStart) || lastStartConstraint.isEqual(firstChildStart))) {
                                    start = firstChildStart;
                                } else if (firstManualChildStart != null) {
                                    start = firstManualChildStart;
                                } else {
                                    start = lastStartConstraint;
                                }
                                LocalDateTime finish = getLastChildFinish(task);
                                //TODO reintroduce calendar fixed
                                ProjectCalendar calendar = getCalendar(task);
                                if (start != null) {
                                    start = calendar.getNextWorkStart(start);
                                    if (!equals(calendar, start, task.getStart())) {
                                        setStart(eh, task, start);
                                        anythingChanged = true;
                                        printCase("4", "setStart", task);
                                    }
                                }
                                if (finish != null && !equals(calendar, finish, task.getFinish())) {
                                    setFinish(task, finish);
                                    anythingChanged = true;
                                    printCase("4", "setFinish", task);
                                }
                            }
                        }
                    }
                    //[5]
                    for (Task task : sprint.getTasks()) {
                        checks++;
                        //-manual
                        //-children
                        //-dependencies
                        //+parent with dependencies
                        LocalDateTime start = getStart(task.getParentTask());
                        if (!isManual(task) && !hasChildTasks(task) && !hasDirectDependencies(task) && hasHierarchicalDependencies(task.getParentTask())) {
                            if (start != null) {
                                //TODO reintroduce calendar fixed
                                ProjectCalendar calendar = getCalendar(task);
                                start = calendar.getNextWorkStart(start);
                                if (!equals(calendar, start, task.getStart())) {

                                    setStart(eh, task, start);
                                    anythingChanged = true;
                                    printCase("5", "setStart", task);
                                }
                            }
                        }
                    }
                    iterations++;

                    //TODO debugging code
                    {
                        Duration days = Duration.between(sprint.getEarliestStartDate(), sprint.getLatestFinishDate());
                        if (days.minus(Duration.ofDays(365)).isPositive()) {
                            throw new LevelingResourcesException(String.format("Could not level resources after %d days, assuming dependency loop.", days.toDays()));
                        }
                    }


                    if (!eh.isTrue("Error #040. We have detected a dependency loop involving tasks and Categories. Please check the generated team planner chart and fix the dependency loop in your Excel sheet.", iterations < maxLoop)) {
                        throw new LevelingResourcesException(String.format("Could not level resources after %d iterations, assuming dependency loop.", iterations));
                    }
                    if (checks % sprint.getTasks().size() == 0) {
//                        System.out.println(".");
                    }
                } while (anythingChanged);
                anythingChanged = createResourceDependencies(sprint, anythingChanged);
            } while (anythingChanged);
            checks = testForNull(/*eh,*/ sprint, checks);
            checks = testRelationsAreHonored(eh, sprint, checks, "");
            markCriticalPath(eh, sprint, currentStartTime);
            logger.trace(String.format("executed %d checks to level resources.", checks));
        } catch (LevelingResourcesException e) {
            logger.error("Error leveling resources: " + e.getMessage());
        }
        sprint.setStart(sprint.getEarliestStartDate());
        logger.trace("Setting start date of sprint to earliest start date: {}", sprint.getStart());
        sprint.setEnd(sprint.getLatestFinishDate());
        logger.trace("Setting end date of sprint to latest finish date: {}", sprint.getEnd());
        logger.trace("Critical path calculated in {} ms", System.currentTimeMillis() - time);
    }

    private void markCriticalPath(GanttErrorHandler eh, Sprint sprint, LocalDateTime currentStartTime) throws LevelingResourcesException {
        LocalDateTime startDate  = sprint.getEarliestStartDate();
        LocalDateTime finishDate = sprint.getLatestFinishDate();
        for (Task task : sprint.getTasks()) {
            task.setCritical(false);
        }
        startSet.clear();

        long    checks = 0;
        boolean anythingChanged;
//        do {
        anythingChanged = false;
        do {
            anythingChanged     = false;
            testCriticalCounter = 0;
            for (Task task : sprint.getTasks()) {
                anythingChanged = testCritical(eh, startDate, finishDate, anythingChanged, task);
                checks++;
            }
            if (!eh.isTrue("Error #040. We have detected a dependency loop involving tasks and Categories. Please check the generated team planner chart and fix the dependency loop in your Excel sheet.", checks < maxLoop)) {
                throw new LevelingResourcesException(String.format("Could not mark critical path  after %d iterations, assuming dependency loop.", checks));
            }
            if (checks % sprint.getTasks().size() == 0) {
//                System.out.println(".");
            }
        } while (anythingChanged);
//        } while (anythingChanged);

        for (Task task : sprint.getTasks()) {
            if (task.getChildTasks().isEmpty() && (startSet.contains(task) || manualSet.contains(task)) && finishSet.contains(task)) {
                task.setCritical(true);
            }
        }

        logger.trace(String.format("executed %d checks to mark critical path.", checks));
    }

    private boolean overlap(Task task1, Task task2) {
        //  s1   f1
        //s2   f2
        //    s2   f2
        //---overlapping
        if (task1.getStart() != null && task1.getFinish() != null && task2.getStart() != null && task2.getFinish() != null) {
            //            long os = task1.getStart();
            //            long of = task1.getFinish();
            //            long ps = task2.getStart();
            //            long pf = task2.getFinish();
            boolean overlapping = !task1.getStart().isEqual(task1.getFinish())
                    && DateUtil.isOverlapping(task1.getStart(), task1.getFinish(), false, task2.getStart(), task2.getFinish());
            return overlapping;
        }
        return false;
    }

    private void printCase(String caseName, String id, String taskName, String methodName, String start, String finish, String duration) {
        logger.trace(String.format("[%s] [%2s][%-20s][%-9s][%20s][%20s][%19s]", caseName, id, taskName, methodName, start, finish, duration));
    }

    private void printCase(String caseName, String methodName, Task task) {
        printCase(caseName, "" + task.getId(), task.getName(), methodName, DateUtil.createDateString(task.getStart(), localDateTimeUtil.dtfymdhms),
                DateUtil.createDateString(task.getFinish(), localDateTimeUtil.dtfymdhms),
                DateUtil.createDurationString(task.getDuration(), true, true, true));
    }

    private void printTasks(Sprint projectFile) {
        for (Task task : projectFile.getTasks()) {
            printCase("PR", "printTasks", task);
        }
    }

    public static int queryNumberOfChildren(Task task) {
        int count = 1;
        for (Task child : task.getChildTasks()) {
            count += queryNumberOfChildren(child);
        }
        return count;
    }

    private void setFinish(Task task, LocalDateTime finish) {
        task.setFinish(finish);
        if (!task.getChildTasks().isEmpty()) {
            LocalDateTime start = task.getStart();
            if (finish != null && start != null) {
                ProjectCalendar calendar = getCalendar(task);
                Duration        duration = MpxjUtil.toJavaDuration(calendar.getWork(start, finish, TimeUnit.DAYS));
                task.setDuration(duration);
            }
        }
    }

    private void setStart(GanttErrorHandler eh, Task task, LocalDateTime endOfLastTask) {
        ProjectCalendar calendar = getCalendar(task);
        LocalDateTime   start    = calendar.getNextWorkStart(endOfLastTask);
        task.setStart(start);
        if (task.isMilestone()) {
            task.setFinish(start);
            Duration duration = Duration.ZERO;
            task.setDuration(duration);
        } else if (!hasChildTasks(task)) {//task
            Duration duration = getDurationFromWork(eh, task);
            task.setDuration(duration);
            LocalDateTime finish = calendar.getDate(start, MpxjUtil.toMpjxDuration(duration));
            task.setFinish(finish);
        } else {//parent
            LocalDateTime finish = task.getFinish();
            if (start != null & finish != null) {
                Duration duration = MpxjUtil.toJavaDuration(calendar.getWork(start, finish, TimeUnit.DAYS));
                task.setDuration(duration);
                task.setFinish(finish);
            }
        }
    }

    /**
     * Task is critical if it cannot move and all tasks that depend on it can also not move until the end date
     *
     * @param startDate
     * @param finishDate
     * @param anythingChanged
     * @param task
     * @return
     */
    private boolean testCritical(GanttErrorHandler eh, LocalDateTime startDate, LocalDateTime finishDate, boolean anythingChanged, Task task) throws LevelingResourcesException {
        if (!eh.isTrue("Error #040. We have detected a dependency loop involving tasks and Categories. Please check the generated team planner chart and fix the dependency loop in your Excel sheet.",
                testCriticalCounter < maxLoop * 10)) {
            throw new LevelingResourcesException(String.format("Could not test critical path  after %d iterations, assuming dependency loop.", testCriticalCounter));
        }
        testCriticalCounter++;
        if (!startSet.contains(task)) {
            //are we starting at the beginning of the project?
            ProjectCalendar calendar = getCalendar(task);
            if (equals(calendar, task.getStart(), startDate)) {
                startSet.add(task);
                anythingChanged = true;
            }
        }
        if (!startSet.contains(task)) {
            //are we starting after a task that is in the startSet?
            for (Relation r : task.getPredecessors()) {
                Task predecessor = task.getSprint().getTaskById(r.getPredecessorId());
                if (startSet.contains(predecessor)) {
                    ProjectCalendar calendar = getCalendar(task);
                    if (equals(calendar, task.getStart(), predecessor.getFinish())) {
                        startSet.add(task);
                        anythingChanged = true;
                    }
                }
            }
        }
        if (!startSet.contains(task)) {
            //are we starting with a parent that is in the startSet?
            ProjectCalendar calendar = getCalendar(task);
            Task            parent   = task.getParentTask();
            if (startSet.contains(parent)) {
                if (parent != null && equals(calendar, task.getStart(), parent.getStart())) {
                    startSet.add(task);
                    anythingChanged = true;
                }
            }

        }
        if (!startSet.contains(task)) {
            //are we starting with a child that is in the startSet
            for (Task child : task.getChildTasks()) {
                if (startSet.contains(child)) {
                    ProjectCalendar calendar = getCalendar(child);
                    if (equals(calendar, task.getStart(), child.getStart())) {
                        startSet.add(task);
                        anythingChanged = true;
                    }
                }

            }
        }

        if (!manualSet.contains(task)) {
            for (Relation r : task.getPredecessors()) {
                Task predecessor = task.getSprint().getTaskById(r.getPredecessorId());
                if (manualSet.contains(predecessor)) {
                    //are we starting after a task that is in the manualSet?
                    ProjectCalendar calendar = getCalendar(task);
                    if (equals(calendar, task.getStart(), predecessor.getFinish())) {
                        manualSet.add(task);
                        anythingChanged = true;
                    }
                }
            }
        }
        if (!manualSet.contains(task)) {
            //are we starting with a parent that is in the manualSet?
            ProjectCalendar calendar = getCalendar(task);
            Task            parent   = task.getParentTask();
            if (manualSet.contains(parent)) {
                if (parent != null && equals(calendar, task.getStart(), parent.getStart())) {
                    manualSet.add(task);
                    anythingChanged = true;
                }
            }

        }
        if (!manualSet.contains(task)) {
            //are we starting with a child that is in the manualSet
            for (Task child : task.getChildTasks()) {
                if (manualSet.contains(child)) {
                    ProjectCalendar calendar = getCalendar(child);
                    if (equals(calendar, task.getStart(), child.getStart())) {
                        manualSet.add(task);
                        anythingChanged = true;
                    }
                }

            }
        }

        if (!finishSet.contains(task)) {
            ProjectCalendar calendar = getCalendar(task);
            //are we ending at the end of the project?
            if (equals(calendar, task.getFinish(), finishDate)) {
                finishSet.add(task);
                anythingChanged = true;
            }
        }
        if (!finishSet.contains(task)) {
            //are we ending with a parent that is in the finishSet?
            ProjectCalendar calendar = getCalendar(task);
            Task            parent   = task.getParentTask();
            if (finishSet.contains(parent)) {
                if (parent != null && equals(calendar, task.getFinish(), parent.getFinish())) {
                    finishSet.add(task);
                    anythingChanged = true;
                }
            }

        }
        if (finishSet.contains(task)) {
            for (Relation r : task.getPredecessors()) {
                Task predecessor = task.getSprint().getTaskById(r.getPredecessorId());
                if (!finishSet.contains(predecessor)) {
                    //predecessor finishes at same time a task starts that is in the finishSet?
                    ProjectCalendar calendar = getCalendar(task);
                    if (equals(calendar, predecessor.getFinish(), task.getStart())) {
                        finishSet.add(predecessor);
                        anythingChanged = true;
                    }
                }
            }

        }
        if (!finishSet.contains(task)) {
            for (Task child : task.getChildTasks()) {
                if (finishSet.contains(child)) {
                    ProjectCalendar calendar = getCalendar(child);
                    if (equals(calendar, task.getFinish(), child.getFinish())) {
                        finishSet.add(task);
                        anythingChanged = true;
                    }
                }

            }
        }
        if (finishSet.contains(task) && task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED) {
            manualSet.add(task);
        }
        for (Relation r : task.getPredecessors()) {
            Task predecessor = task.getSprint().getTaskById(r.getPredecessorId());
            anythingChanged = testCritical(eh, startDate, finishDate, anythingChanged, predecessor);
        }
        return anythingChanged;
    }

    private long testForNull(/*GanttErrorHandler eh,*/ Sprint projectFile, long checks) {
        for (Task task : projectFile.getTasks()) {
            printCase("TS", "testForNull", task);
        }
        for (Task task : projectFile.getTasks()) {
            checks++;
//            eh.isNotNull(String.format("Error #030. Task '%s' has no start date.", task.getName()), task, task.getStart());
//            eh.isNotNull(String.format("Error #031. Task '%s' has no finish date.", task.getName()), task, task.getFinish());
//            eh.isNotNull(String.format("Error #032. Task '%s' has no duration.", task.getName()), task, task.getDuration());
        }
        return checks;
    }

    public void testRelationsAreHonored(GanttErrorHandler eh, Sprint projectFile) throws Exception {
        try (Profiler pc = new Profiler(SampleType.CPU)) {
            testRelationsAreHonored(eh, projectFile, 0, null);
        }
    }

    private long testRelationsAreHonored(GanttErrorHandler eh, Sprint projectFile, long checks, String xlsxFile) {
        //Test if all relations have been honored
        //[M] manual
        //+manual
        //-milestone
        //-duration
        //-children, this means +work
        for (Task task : projectFile.getTasks()) {
            checks++;
            if (isManual(task)) {
                for (Relation relation : task.getPredecessors()) {
                    Task sourceTask = task;
                    Task targetTask = projectFile.getTaskById(relation.getPredecessorId());
                    if (sourceTask.getId() == task.getId()) {
//                        eh.isTrue(
//                                String.format(ERROR_103_TASK_IS_MANUALLY_SCHEDULED_AND_CANNOT_FULLFILL_ITS_DEPENDENCY, context.getRowIndexByTaskId(task),
//                                        task.getName(), context.getRowIndexByTaskId(targetTask), targetTask.getName()),
//                                task, task.getStart().isAfter(targetTask.getFinish()) || equals(getCalendar(task), task.getStart(), targetTask.getFinish()));
                    }
                }
                try {
                    logger.trace(String.format("[M] [%d][%s] %s %s %s test", task.getId(), task.getName(),
                            DateUtil.createDateString(task.getStart(), localDateTimeUtil.dtfymdhms),
                            DateUtil.createDateString(task.getFinish(), localDateTimeUtil.dtfymdhms),
                            DateUtil.createDurationString(task.getDuration(), true, true, true)));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                for (Relation relation : task.getPredecessors()) {
                    Task sourceTask = task;
                    Task targetTask = projectFile.getTaskById(relation.getPredecessorId());
                    //                    logger.trace(String.format("task %s %s %s", task.getName(), DateUtil.createDateString(task.getStart(), dateUtil.sdfymdhms),
                    //                            DateUtil.createDateString(task.getFinish(), dateUtil.sdfymdhms)));
                    //                    logger.trace(
                    //                            String.format("targetTask %s %s %s", targetTask.getName(), DateUtil.createDateString(targetTask.getStart(), dateUtil.sdfymdhms),
                    //                                    DateUtil.createDateString(targetTask.getFinish(), dateUtil.sdfymdhms)));
                    if (sourceTask.getId() == task.getId() && task.getStart() != null && targetTask.getFinish() != null) {
//                        if (eh.isTrue(
//                                String.format(ERROR_104_TASK_CANNOT_FULLFILL_ITS_DEPENDENCY, context.getRowIndexByTaskId(task), task.getName(),
//                                        DateUtil.createDateString(task.getStart(), localDateTimeUtil.dtfymdhms), context.getRowIndexByTaskId(targetTask),
//                                        targetTask.getName(), DateUtil.createDateString(targetTask.getFinish(), localDateTimeUtil.dtfymdhms)),
//                                task, task.getStart().isAfter(targetTask.getFinish()) || equals(getCalendar(task), task.getStart(), targetTask.getFinish()))) {
//                        }
                    }

                }
            }
        }
        return checks;
    }

    private boolean useSameAssignee(Task task1, Task task2) {
        return task1.getResourceId() != null && task1.getResourceId() == task2.getResourceId();
    }

}
