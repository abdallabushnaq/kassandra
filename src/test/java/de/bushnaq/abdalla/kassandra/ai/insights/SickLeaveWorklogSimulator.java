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

package de.bushnaq.abdalla.kassandra.ai.insights;

import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.dao.WorklogRemaining;
import de.bushnaq.abdalla.util.TaskUtil;
import net.sf.mpxj.ProjectCalendar;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

final class SickLeaveWorklogSimulator {
    private static final long SECONDS_PER_WORKING_DAY = 75L * 6L * 60L;

    static ReplayResult replayWithoutSickLeave(Sprint sprint, LocalDateTime cutoff) {
        Objects.requireNonNull(sprint, "sprint");
        Objects.requireNonNull(cutoff, "cutoff");
        if (sprint.getStart() == null) {
            throw new IllegalArgumentException("Cannot replay worklogs for a sprint without a start date.");
        }

        Map<User, ProjectCalendar> originalCalendars = new LinkedHashMap<>();
        List<ProjectCalendar>       temporaryCalendars = new ArrayList<>();
        Map<Task, TaskState>        originalTaskStates = new LinkedHashMap<>();
        Map<UUID, Deque<Duration>>  pendingWorklogs = new LinkedHashMap<>();
        Map<UUID, LocalDate>        firstWorklogDates = new HashMap<>();
        Map<UUID, LocalDate>        lastWorklogDates = new HashMap<>();
        Map<UUID, List<ReplayWorklog>> replayWorklogs = new LinkedHashMap<>();

        try {
            // Replace each user's calendar only for the replay so the original sick-day records remain untouched.
            for (User user : sprint.getUserMap().values()) {
                ProjectCalendar originalCalendar = user.getCalendar();
                originalCalendars.put(user, originalCalendar);
                ProjectCalendar calendarWithoutSickLeave = user.createCalendarWithoutSickDays(sprint);
                temporaryCalendars.add(calendarWithoutSickLeave);
                user.setCalendar(calendarWithoutSickLeave);
            }

            // Preserve each source worklog's duration and order, then reset task progress for the replay.
            for (Task task : sprint.getTasks()) {
                originalTaskStates.put(task, new TaskState(task.getRemainingEstimate(), task.getTimeSpent()));
                List<Worklog> worklogs = task.getWorklogs().stream()
                        .sorted(Comparator.comparing(Worklog::getStart, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
                Deque<Duration> taskWorklogs = new ArrayDeque<>();
                Duration totalLoggedWork = Duration.ZERO;
                for (Worklog worklog : worklogs) {
                    if (worklog.getStart() == null || worklog.getTimeSpent() == null
                            || worklog.getTimeSpent().isNegative()) {
                        throw new IllegalStateException("Invalid source worklog for task '" + task.getKey() + "'.");
                    }
                    if (!worklog.getTimeSpent().isZero()) {
                        taskWorklogs.addLast(worklog.getTimeSpent());
                        totalLoggedWork = totalLoggedWork.plus(worklog.getTimeSpent());
                    }
                }

                pendingWorklogs.put(task.getId(), taskWorklogs);
                replayWorklogs.put(task.getId(), new ArrayList<>());
                Duration remainingWork = task.getRemainingEstimate() == null
                        ? Duration.ZERO : task.getRemainingEstimate();
                task.setRemainingEstimate(totalLoggedWork.plus(remainingWork));
                task.setTimeSpent(Duration.ZERO);
            }

            // Replay logged work, then use available capacity to finish remaining estimates through the cutoff.
            LocalDate firstDay = sprint.getStart().toLocalDate();
            for (LocalDate day = firstDay; cutoff.toLocalDate().isAfter(day); day = day.plusDays(1)) {
                if (sprint.getTasks().stream()
                        .filter(task -> task.isTask() && task.isImpactOnCost())
                        .allMatch(task -> task.getRemainingEstimate().isZero())) {
                    break;
                }

                Map<UUID, Duration> userWorkToday = new HashMap<>();
                Map<UUID, Duration> taskWorkToday = new HashMap<>();
                for (Task task : sprint.getTasks()) {
                    Deque<Duration> worklogs = pendingWorklogs.get(task.getId());
                    if (worklogs == null || !task.isTask() || !task.isImpactOnCost()
                            || task.getStart() == null || day.isBefore(task.getStart().toLocalDate())) {
                        continue;
                    }

                    ProjectCalendar calendar = task.getEffectiveCalendar();
                    if (calendar == null || !calendar.isWorkingDate(day)) {
                        continue;
                    }

                    // TaskUtil supplies the existing task-day limit and blocks work until predecessors are done.
                    Duration taskDailyLimit = TaskUtil.getTaskWorkPerDay(sprint, day, task, false)
                            .minus(taskWorkToday.getOrDefault(task.getId(), Duration.ZERO));
                    UUID userId = task.getResourceId();
                    Duration userDailyLimit = Duration.ofSeconds((long) (SECONDS_PER_WORKING_DAY * task.getAvailability()))
                            .minus(userWorkToday.getOrDefault(userId, Duration.ZERO));
                    while (taskDailyLimit.isPositive() && userDailyLimit.isPositive()
                            && !task.getRemainingEstimate().isZero()) {
                        // Once recorded work is exhausted, assume the task continues using its available daily capacity.
                        boolean hasSourceWorklog = !worklogs.isEmpty();
                        Duration worklogRemaining = hasSourceWorklog
                                ? worklogs.peekFirst() : task.getRemainingEstimate();
                        Duration work = minimum(worklogRemaining, taskDailyLimit, userDailyLimit,
                                task.getRemainingEstimate());
                        if (!work.isPositive()) {
                            break;
                        }

                        if (hasSourceWorklog) {
                            if (work.equals(worklogRemaining)) {
                                worklogs.removeFirst();
                            } else {
                                worklogs.removeFirst();
                                worklogs.addFirst(worklogRemaining.minus(work));
                            }
                        }
                        task.setRemainingEstimate(task.getRemainingEstimate().minus(work));
                        task.setTimeSpent(task.getTimeSpent().plus(work));
                        userWorkToday.merge(userId, work, Duration::plus);
                        taskWorkToday.merge(task.getId(), work, Duration::plus);
                        taskDailyLimit = taskDailyLimit.minus(work);
                        userDailyLimit = userDailyLimit.minus(work);
                        firstWorklogDates.putIfAbsent(task.getId(), day);
                        lastWorklogDates.put(task.getId(), day);
                    }
                }

                // Materialize each task's daily total as a chartable worklog with its resulting remaining estimate.
                for (Map.Entry<UUID, Duration> entry : taskWorkToday.entrySet()) {
                    Task task = sprint.getTaskById(entry.getKey());
                    LocalDateTime worklogStart = task.getStart().toLocalDate().equals(day)
                            ? task.getStart() : day.atTime(8, 0);
                    replayWorklogs.get(task.getId()).add(new ReplayWorklog(worklogStart, entry.getValue(),
                            task.getRemainingEstimate()));
                }
            }

            // Capture replay outcomes before the finally block restores the original task state and calendars.
            Map<UUID, TaskReplay> taskResults = new LinkedHashMap<>();
            for (Task task : sprint.getTasks()) {
                LocalDate firstWorklogDate = firstWorklogDates.get(task.getId());
                Duration unreplayedWork = pendingWorklogs.get(task.getId()).stream()
                        .reduce(Duration.ZERO, Duration::plus);
                taskResults.put(task.getId(), new TaskReplay(firstWorklogDate, lastWorklogDates.get(task.getId()),
                        replayProgress(task), task.getTimeSpent(), task.getRemainingEstimate(),
                        replayWorklogs.get(task.getId()), unreplayedWork));
            }
            return new ReplayResult(taskResults);
        } finally {
            // Always return the loaded snapshot to its original state, including when replay fails.
            originalTaskStates.forEach((task, state) -> {
                task.setRemainingEstimate(state.remainingEstimate());
                task.setTimeSpent(state.timeSpent());
            });
            originalCalendars.forEach(User::setCalendar);
            for (ProjectCalendar calendar : temporaryCalendars) {
                sprint.getProjectFile().getCalendars().remove(calendar);
            }
        }
    }

    private static Duration minimum(Duration... durations) {
        return Arrays.stream(durations).min(Duration::compareTo).orElse(Duration.ZERO);
    }

    private static float replayProgress(Task task) {
        Duration timeSpent = task.getTimeSpent();
        Duration remainingEstimate = task.getRemainingEstimate();
        if (timeSpent == null || remainingEstimate == null) {
            return task.getProgress();
        }
        Duration totalWork = timeSpent.plus(remainingEstimate);
        if (totalWork.isZero()) {
            return task.getProgress();
        }
        return (float) timeSpent.toSeconds() / totalWork.toSeconds();
    }

    record ReplayResult(Map<UUID, TaskReplay> tasksById) {
        ReplayResult {
            tasksById = Collections.unmodifiableMap(new LinkedHashMap<>(tasksById));
        }
    }

    record TaskReplay(LocalDate firstWorklogDate, LocalDate lastWorklogDate, float progress,
                      Duration timeSpent, Duration remainingEstimate, List<ReplayWorklog> worklogs,
                      Duration unreplayedWork) {
        TaskReplay {
            worklogs = List.copyOf(worklogs);
        }
    }

    record ReplayWorklog(LocalDateTime start, Duration timeSpent, Duration remainingEstimate) {
    }

    private record TaskState(Duration remainingEstimate, Duration timeSpent) {
    }

    static AppliedReplay applyToSprint(Sprint sprint, ReplayResult replay, LocalDateTime now) {
        return AppliedReplay.create(sprint, replay, now);
    }

    static final class AppliedReplay implements AutoCloseable {
        private final Map<Task, TaskChartState> originalTaskStates = new LinkedHashMap<>();
        private final Map<User, ProjectCalendar> originalCalendars = new LinkedHashMap<>();
        private final List<ProjectCalendar> temporaryCalendars = new ArrayList<>();
        private final Sprint sprint;
        private final SprintChartState originalSprintState;

        private AppliedReplay(Sprint sprint) {
            this.sprint = sprint;
            originalSprintState = new SprintChartState(new ArrayList<>(sprint.getWorklogs()),
                    new ArrayList<>(sprint.getWorklogRemaining()), sprint.getWorked(), sprint.getRemaining(),
                    sprint.getOriginalEstimation(), sprint.getReleaseDate(), sprint.getStatus());
        }

        private static AppliedReplay create(Sprint sprint, ReplayResult replay, LocalDateTime now) {
            AppliedReplay appliedReplay = new AppliedReplay(sprint);
            try {
                appliedReplay.apply(replay, now);
                return appliedReplay;
            } catch (RuntimeException exception) {
                appliedReplay.close();
                throw exception;
            }
        }

        private void apply(ReplayResult replay, LocalDateTime now) {
            for (User user : sprint.getUserMap().values()) {
                ProjectCalendar originalCalendar = user.getCalendar();
                originalCalendars.put(user, originalCalendar);
                ProjectCalendar calendarWithoutSickLeave = user.createCalendarWithoutSickDays(sprint);
                temporaryCalendars.add(calendarWithoutSickLeave);
                user.setCalendar(calendarWithoutSickLeave);
            }

            List<Worklog> replayedWorklogs = new ArrayList<>();
            for (Task task : sprint.getTasks()) {
                originalTaskStates.put(task, new TaskChartState(task.getWorklogs(), task.getTimeSpent(),
                        task.getRemainingEstimate(), task.getProgress(), task.getTaskStatus()));
                TaskReplay taskReplay = replay.tasksById().get(task.getId());
                if (taskReplay == null) {
                    throw new IllegalStateException("Replay did not return task '" + task.getKey() + "'.");
                }

                List<Worklog> taskWorklogs = new ArrayList<>();
                for (ReplayWorklog replayWorklog : taskReplay.worklogs()) {
                    Worklog worklog = new Worklog();
                    worklog.setId(UUID.randomUUID());
                    worklog.setSprintId(sprint.getId());
                    worklog.setTaskId(task.getId());
                    worklog.setAuthorId(task.getResourceId());
                    worklog.setStart(replayWorklog.start());
                    worklog.setTimeSpent(replayWorklog.timeSpent());
                    worklog.setTimeRemainingEstimate(replayWorklog.remainingEstimate());
                    worklog.setComment("Sickness-free replay");
                    taskWorklogs.add(worklog);
                    replayedWorklogs.add(worklog);
                }
                task.setWorklogs(taskWorklogs);
                task.setTimeSpent(taskReplay.timeSpent());
                task.setRemainingEstimate(taskReplay.remainingEstimate());
                task.setProgress(taskReplay.progress());
            }
            sprint.getTasks().stream().filter(Task::isTask).forEach(task -> task.calculateStatus(false));

            replayedWorklogs.sort(Comparator.comparing(Worklog::getStart));
            sprint.getWorklogs().clear();
            sprint.getWorklogs().addAll(replayedWorklogs);
            sprint.getWorklogRemaining().clear();
            sprint.getTasks().forEach(sprint::addWorklogRemaining);
            sprint.recalculate(now);
        }

        @Override
        public void close() {
            originalTaskStates.forEach((task, state) -> {
                task.setWorklogs(state.worklogs());
                task.setTimeSpent(state.timeSpent());
                task.setRemainingEstimate(state.remainingEstimate());
                task.setProgress(state.progress());
                task.setTaskStatus(state.taskStatus());
            });
            originalCalendars.forEach(User::setCalendar);
            temporaryCalendars.forEach(calendar -> sprint.getProjectFile().getCalendars().remove(calendar));
            sprint.getWorklogs().clear();
            sprint.getWorklogs().addAll(originalSprintState.worklogs());
            sprint.getWorklogRemaining().clear();
            sprint.getWorklogRemaining().addAll(originalSprintState.worklogRemaining());
            sprint.setWorked(originalSprintState.worked());
            sprint.setRemaining(originalSprintState.remaining());
            sprint.setOriginalEstimation(originalSprintState.originalEstimation());
            sprint.setReleaseDate(originalSprintState.releaseDate());
            sprint.setStatus(originalSprintState.status());
        }
    }

    private record TaskChartState(List<Worklog> worklogs, Duration timeSpent, Duration remainingEstimate,
                                  Float progress, TaskStatus taskStatus) {
    }

    private record SprintChartState(List<Worklog> worklogs, List<WorklogRemaining> worklogRemaining,
                                    Duration worked, Duration remaining, Duration originalEstimation,
                                    LocalDateTime releaseDate, Status status) {
    }
}
