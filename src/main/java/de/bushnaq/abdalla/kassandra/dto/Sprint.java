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

package de.bushnaq.abdalla.kassandra.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.report.dao.WorklogRemaining;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import de.bushnaq.abdalla.util.DurationDeserializer;
import de.bushnaq.abdalla.util.DurationSerializer;
import de.bushnaq.abdalla.util.date.DateUtil;
import de.bushnaq.abdalla.util.date.ReportUtil;
import lombok.*;
import net.sf.mpxj.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class Sprint extends AbstractTimeAware implements Comparable<Sprint> {

    private       String          avatarHash;
    @JsonIgnore
    private       ProjectCalendar calendar;
    private       LocalDateTime   end;
    @JsonIgnore
    public        List<Throwable> exceptions  = new ArrayList<>();
    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private       Feature         feature;
    private       Long            featureId;
    private       Long            id;
    private       String          name;
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private       Duration        originalEstimation;
    @JsonIgnore
    private final ProjectFile     projectFile = new ProjectFile();
    private       LocalDateTime   releaseDate;//calculated from the task work, worklogs and remaining work
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private       Duration        remaining;
    private       LocalDateTime   start;
    private       Status          status      = Status.CREATED;
    @JsonIgnore
    transient     Map<Long, Task> taskMap     = new HashMap<>();
    @JsonIgnore
    private       List<Task>      tasks       = new ArrayList<>();
    private       Long            userId;
    @JsonIgnore
    transient     Map<Long, User> userMap     = new HashMap<>();
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private       Duration        worked;
    @JsonIgnore
    List<WorklogRemaining> worklogRemaining = new ArrayList<>();
    @JsonIgnore
    private List<Worklog> worklogs = new ArrayList<>();

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void addWorklogRemaining(Task task) {
        Duration timeSpentMinutes         = task.getTimeSpent();
        Duration remainingEstimateMinutes = task.getRemainingEstimate();
        if (timeSpentMinutes.equals(Duration.ZERO) && remainingEstimateMinutes.equals(Duration.ZERO)) {
            return;
        }
        if (timeSpentMinutes == null && remainingEstimateMinutes == null) {
            return;
        }
        if (timeSpentMinutes == null) {
            timeSpentMinutes = Duration.ZERO;
        }
        if (remainingEstimateMinutes == null) {
            remainingEstimateMinutes = Duration.ZERO;
        }
        if (task.getResourceId() != null) {
            WorklogRemaining w = new WorklogRemaining(getId(), task.getId(), task.getKey(), task.getAssignedUser(), timeSpentMinutes, remainingEstimateMinutes);
            worklogRemaining.add(w);
        } else {
            WorklogRemaining w = new WorklogRemaining(getId(), task.getId(), task.getKey(), null, timeSpentMinutes, remainingEstimateMinutes);
            worklogRemaining.add(w);
        }
    }

    @Override
    public int compareTo(Sprint other) {
        return this.id.compareTo(other.id);
    }

    private void defineCalendar() {
        final boolean[] DEFAULT_WORKING_WEEK = {false, true, true, true, true, true, false};
        // HolidaysDownloader hd = new HolidaysDownloader();
        // Map<LocalDate, String> downloadHolidays = hd.downloadHolidays();

        // define the base calendar
        calendar = projectFile.addDefaultBaseCalendar();
        calendar.setWorkingDay(DayOfWeek.SATURDAY, false);
        calendar.setWorkingDay(DayOfWeek.SUNDAY, false);
        for (int index = 0; index < 7; index++) {
            DayOfWeek day = DayOfWeek.of(index + 1);
            //            calendar.setWorkingDay(day, DEFAULT_WORKING_WEEK[index]);
            if (calendar.isWorkingDay(day)) {
                ProjectCalendarHours hours = calendar.addCalendarHours(DayOfWeek.of(index + 1));
                hours.add(new LocalTimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)));
                hours.add(new LocalTimeRange(LocalTime.of(13, 0), LocalTime.of(16, 30)));
            }
        }
//        BankHolidayReader bhr = new BankHolidayReader();
//        context.bankHolidays = bhr.read(context.parameters.smbParameters, ParameterOptions.now.toLocalDate());
//        for (LocalDate ld : context.bankHolidays.keySet()) {
//            // System.out.println(ld.toString() + " " + bankHolidays.get(ld));
//            ProjectCalendarException calendarException = calendar.addCalendarException(ld, ld);
//            calendarException.setName(context.bankHolidays.get(ld));
//        }
    }

    /**
     * Get the avatar URL with hash parameter for proper caching.
     * The hash ensures that when the avatar changes, the URL changes, forcing the browser to fetch the new image.
     *
     * @return The avatar URL with hash parameter if hash is available, otherwise just the base URL
     */
    @JsonIgnore
    public String getAvatarUrl() {
        String url = "/frontend/avatar-proxy/sprint/" + id;
        if (avatarHash != null && !avatarHash.isEmpty()) {
            url += "?h=" + avatarHash;
        }
        return url;
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This provides a consistent prompt format for sprint avatars.
     *
     * @return A default prompt string for generating sprint avatar images
     */
    @JsonIgnore
    public String getDefaultAvatarPrompt() {
        return getDefaultAvatarPrompt(name);
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This static method provides a consistent prompt format for sprint avatars.
     *
     * @param sprintName The name of the sprint
     * @return A default prompt string for generating sprint avatar images
     */
    public static String getDefaultAvatarPrompt(String sprintName) {
        return "Icon representing the city '" + sprintName + "', minimalist, flat design, vector graphics, simple shapes, clean lines, modern style, centered composition, white background";
    }

    @JsonIgnore
    public LocalDateTime getEarliestStartDate() {
        LocalDateTime earliestDate = ParameterOptions.getLocalNow();
        for (Task task : getTasks()) {
            if (task.getStart() == null) {
                continue;
            }
            if (!task.isMilestone() && (task.getChildTasks().isEmpty()) && (task.getDuration() != null && !task.getDuration().isZero())) {
                if (task.getStart().isBefore(earliestDate)) {
                    earliestDate = task.getStart();
                }
            } else {
                //ignore milestones
            }
        }
        return earliestDate;
    }

    @JsonIgnore
    public String getKey() {
        return "S-" + id;
    }

    @JsonIgnore
    public LocalDateTime getLatestFinishDate() {
        LocalDateTime latestDate = ParameterOptions.getLocalNow();
        for (Task task : getTasks()) {
            if (!task.isMilestone() && (task.getChildTasks().isEmpty()) && (task.getDuration() != null && !task.getDuration().isZero())) {
                if (task.getFinish().isAfter(latestDate)) {
                    latestDate = task.getFinish();
                }
            } else {
                //ignore milestones
            }
        }
        return latestDate;
    }

    /**
     * Get the next available orderId for a new task in the sprint
     *
     * @return The next orderId to use (0 if sprint is empty, otherwise max orderId + 1)
     */
    @JsonIgnore
    public int getNextOrderId() {
        return getTasks().isEmpty() ? 0 :
                getTasks().stream()
                        .mapToInt(Task::getOrderId)
                        .max()
                        .orElse(-1) + 1;
    }

    public Task getTaskById(Long predecessorId) {
        return tasks.stream().filter(task -> task.getId().equals(predecessorId)).findFirst().orElse(null);
    }

    @JsonIgnore
    public User getUser() {
        return getuser(userId);
    }

    public User getuser(Long resourceId) {
        return userMap.get(resourceId);
    }

    public void initTaskMap(List<Task> tasks, List<Worklog> worklogs) {
        this.worklogs = worklogs;
        taskMap.clear();
        for (Task task : tasks) {
            taskMap.put(task.getId(), task);
        }
        tasks.forEach(task -> {
            //set the parent task
            if (task.getParentTaskId() != null) {
                task.setParentTask(taskMap.get(task.getParentTaskId()));
                //add the task to the parent task
                task.getParentTask().addChildTask(task);
            }
            if (worklogs != null) {
                for (Worklog worklog : worklogs) {
                    if (worklog.getTaskId().equals(task.getId())) {
                        task.addWorklog(worklog);
                    }
                }
            }
            task.setSprint(this);
            task.initialize();
            addWorklogRemaining(task);
        });
        this.tasks = tasks;
    }

    public void initUserMap(List<User> users) {
        userMap.clear();
        for (User user : users) {
            user.initialize(this);
            userMap.put(user.getId(), user);
        }
        if (userId == null) {
//            calendar = projectFile.getDefaultCalendar();
        } else {
            calendar = getUser().getCalendar();
        }
    }

    public void initialize() {
        setProjectProperties();
        initializeCalendar();
    }

    public void initialize(GanttContext gc) {
        tasks.clear();
        taskMap.clear();
        worklogRemaining.clear();
        //map users to their ids
        gc.allUsers.forEach(user -> userMap.put(user.getId(), user));
        //populate tasks list
        gc.allTasks.forEach(task -> {
            if (task.getSprintId().equals(id)) {
                addTask(task);
            }
        });
        gc.allWorklogs.forEach(worklog -> {
            if (worklog.getSprintId().equals(id)) {
                worklogs.add(worklog);
            }
        });
        //map tasks to their ids
        tasks.forEach(task -> taskMap.put(task.getId(), task));
        tasks.forEach(task -> {
            //set the parent task
            if (task.getParentTaskId() != null) {
                task.setParentTask(taskMap.get(task.getParentTaskId()));
                //add the task to the parent task
                task.getParentTask().addChildTask(task);
            }
            for (Worklog worklog : worklogs) {
                if (worklog.getTaskId().equals(task.getId())) {
                    task.addWorklog(worklog);
                }
            }
            task.setSprint(this);
            task.initialize();
            addWorklogRemaining(task);
        });
        if (userId == null) calendar = gc.getProjectFile().getDefaultCalendar();
        else {
            calendar = getUser().getCalendar();
        }

    }

    private void initializeCalendar() {
        defineCalendar();
        ProjectProperties projectProperties = projectFile.getProjectProperties();
        ProjectCalendar   projectCalendar   = projectFile.getCalendarByName(ProjectCalendar.DEFAULT_BASE_CALENDAR_NAME);
        if (projectCalendar == null) {
            projectCalendar = projectFile.getDefaultCalendar();
        }
        if (projectCalendar == null) {
            projectCalendar = projectFile.addDefaultBaseCalendar();
            for (DayOfWeek day : DayOfWeek.values()) {
                if (projectCalendar.getCalendarDayType(day) == DayType.WORKING) {
                    ProjectCalendarHours hours = projectCalendar.getCalendarHours(day);
                    hours.clear();
                    hours.add(new LocalTimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)));
                    hours.add(new LocalTimeRange(LocalTime.of(13, 0), LocalTime.of(16, 30)));
                }
            }
//            BankHolidayReader bhr = new BankHolidayReader();
//            context.bankHolidays = bhr.read(context.parameters.smbParameters, ParameterOptions.now.toLocalDate());
//            for (LocalDate ld : context.bankHolidays.keySet()) {
//                // System.out.println(ld.toString() + " " + bankHolidays.get(ld));
//                ProjectCalendarException calendarException = projectCalendar.addCalendarException(ld, ld);
//                calendarException.setName(context.bankHolidays.get(ld));
//            }

        }
        ProjectCalendar projectPropertiesCalendar = projectProperties.getDefaultCalendar();
        if (projectPropertiesCalendar == null) {
            projectProperties.setDefaultCalendar(projectCalendar);
        }

    }

    @JsonIgnore
    public boolean isClosed() {
        return getStatus().equals(Status.CLOSED);
    }

    public void propagateTimeTracking() {
        worked             = Duration.ZERO;
        originalEstimation = Duration.ZERO;
        remaining          = Duration.ZERO;
        for (Task task : getTasks()) {
            worked             = worked.plus(task.getTimeSpent());
            originalEstimation = originalEstimation.plus(task.getOriginalEstimate());
            remaining          = remaining.plus(task.getRemainingEstimate());
        }
    }

    public void recalculate(LocalDateTime now) {
        propagateTimeTracking();
        if (getRemaining() != null && getRemaining().isZero() && worklogs != null && !worklogs.isEmpty()) {
            //if all work has been done, set the release date to the last worklog date
            releaseDate = DateUtil.offsetDateTimeToLocalDateTime(worklogs.getLast().getStart());
        } else {
            //calculate the release date based on the work done and the remaining work
            releaseDate = ReportUtil.calculateReleaseDate(getStart(), now, getWorked(), DateUtil.add(getWorked(), getRemaining()));
        }
    }

    @JsonIgnore
    private void setProjectProperties() {
        ProjectProperties properties = projectFile.getProjectProperties();
//        properties.setProjectTitle(new File(XlsxUtil.removeExtension(xlsxFile)).getName());
//        properties.setAuthor("XLSX-to-MPP " + moduleVersion);
        properties.setCurrentDate(null);
        properties.setDefaultStartTime(LocalTime.of(8, 0));
        properties.setDefaultEndTime(LocalTime.of(16, 30));
        properties.setMinutesPerDay((int) (7.5 * 60));
        properties.setMinutesPerWeek(5 * (int) (7.5 * 60));
        //        properties.setDefaultDurationUnits(TimeUnit.HOURS);
        //        TaskUtil.createMetaData(projectFile);
        //        Map<String, Object> customProperties = new HashMap<>();
        //        HashMap<Task, MetaData> mdMap = new HashMap<>();
        //        customProperties.put(MetaData.METADATA, mdMap);
        //        properties.setCustomProperties(customProperties);
        //        HashMap<Task, MetaData> mdMap1 = (HashMap<Task, MetaData>)projectfile.getProjectProperties().getCustomProperties().get(MetaData.METADATA);
    }
}
