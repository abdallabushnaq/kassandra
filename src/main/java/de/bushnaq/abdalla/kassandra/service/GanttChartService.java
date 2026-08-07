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

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;
import de.bushnaq.abdalla.kassandra.report.dao.theme.DarkTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.LightTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.CalendarExceptionDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttChartDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.RelationDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.TaskDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.UserCalendarDto;
import de.bushnaq.abdalla.kassandra.rest.dto.theme.ThemeDto;
import de.bushnaq.abdalla.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlgraphics.java2d.color.ColorUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;

/**
 * Builds a {@link GanttChartDto} from a fully-loaded {@link Sprint} so that
 * the client-side {@code gantt-chart.js} can render an interactive, zoomable
 * Gantt chart without any further server calls.
 *
 * <p>Colour computation mirrors {@code AbstractGanttRenderer.drawTask()} and
 * related methods. Calendar exceptions are extracted once from each assigned
 * user's effective calendar so the browser can determine working vs.
 * non-working days per row without duplicating holiday data.
 */
@Service
@Slf4j
public class GanttChartService {

    /**
     * Default number of extra days rendered after the last task finishes.
     */
    public static final int       DEFAULT_POST_RUN = 14;
    /**
     * Default number of extra days rendered before the first task starts.
     */
    public static final int       DEFAULT_PRE_RUN  = 14;
    private final       DarkTheme darkTheme;

    // ── Public API ────────────────────────────────────────────────────────────
    private final LightTheme lightTheme;

    @Autowired
    public GanttChartService(LightTheme lightTheme, DarkTheme darkTheme) {
        this.lightTheme = lightTheme;
        this.darkTheme  = darkTheme;
    }

    /**
     * Builds the complete DTO for the given sprint using default preRun/postRun padding.
     *
     * @param sprint fully loaded sprint (tasks, users, worklogs must be populated)
     * @param now    current date/time used for the "N" now-marker
     * @param dark   {@code true} → use dark theme colours
     * @return populated DTO ready for JSON serialisation
     */
    public GanttChartDto build(Sprint sprint, LocalDateTime now, boolean dark) {
        return build(sprint, now, dark, DEFAULT_PRE_RUN, DEFAULT_POST_RUN);
    }

    /**
     * Builds the complete DTO for the given sprint.
     *
     * <p>The {@code preRun} and {@code postRun} values mirror the same-named fields in
     * {@link de.bushnaq.abdalla.kassandra.report.burndown.RenderDao}: the chart window
     * is extended by {@code preRun} days before the earliest task start and {@code postRun}
     * days after the latest task finish.  This leaves space for user names (left margin) and
     * task names that overflow the right edge of a bar.
     *
     * @param sprint  fully loaded sprint (tasks, users, worklogs must be populated)
     * @param now     current date/time used for the "N" now-marker
     * @param dark    {@code true} → use dark theme colours
     * @param preRun  number of extra days to show before the earliest task start
     * @param postRun number of extra days to show after the latest task finish
     * @return populated DTO ready for JSON serialisation
     */
    public GanttChartDto build(Sprint sprint, LocalDateTime now, boolean dark, int preRun, int postRun) {
        Theme         theme = dark ? darkTheme : lightTheme;
        GanttChartDto dto   = new GanttChartDto();

        // ── Chart date range ─────────────────────────────────────────────
//        LocalDate chartStartDate = sprint.getEarliestStartDate().toLocalDate().minusDays(preRun);
//        LocalDate chartEndDate   = sprint.getLatestFinishDate().toLocalDate().plusDays(postRun);
//
//        // Stretch range to include 'now' unless the sprint is already closed
//        if (now != null && !Status.CLOSED.equals(sprint.getStatus())) {
//            LocalDate today = now.toLocalDate();
//            if (today.isBefore(chartStartDate))
//                chartStartDate = today.minusDays(1);
//            if (today.isAfter(chartEndDate))
//                chartEndDate = today.plusDays(1);
//        }

        LocalDateTime chartStart = GanttChartService.getChartStart(sprint, now, preRun).atStartOfDay();
        LocalDateTime chartEnd   = GanttChartService.getChartEnd(sprint, now, postRun).atStartOfDay();
//        int           totalDays  = (int) ChronoUnit.DAYS.between(chartStart, chartEnd) + 1;
        dto.meta.firstDayX    = 0;
        dto.meta.chartStart   = chartStart;
        dto.meta.chartEnd     = chartEnd;
        dto.meta.copyright    = Util.generateCopyrightString(ParameterOptions.getLocalNow());
        dto.meta.now          = now;
        dto.meta.sprintName   = sprint.getName();
        dto.meta.sprintStart  = sprint.getStart();
        dto.meta.sprintEnd    = sprint.getEnd();
        dto.meta.sprintStatus = sprint.getStatus().name();
        dto.meta.preRun       = preRun;
        dto.meta.postRun      = postRun;
        dto.meta.theme        = ThemeDto.fromTheme(theme);
        dto.meta.calendarSize = CalendarSize.YEARS;


        // ── Task rows ─────────────────────────────────────────────────────
        LocalDate calendarStart = getRenderedCalendarStart(sprint, now, preRun);
        LocalDate calendarEnd   = getRenderedCalendarEnd(sprint, now, preRun, postRun);
        int       rowIndex      = 0;
        Set<UUID> calendarIds   = new HashSet<>();
        for (Task task : sprint.getTasks()) {
            if (GanttUtil.isValidTask(task)) {
                TaskDto taskDto = buildTaskDto(task, rowIndex, theme);
                dto.tasks.add(taskDto);
                addUserCalendar(dto, task.getAssignedUser(), calendarIds, calendarStart, calendarEnd);
                rowIndex++;
            }
        }

        return dto;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private TaskDto buildTaskDto(Task task, int rowIndex, Theme theme) {
        TaskDto dto = new TaskDto();

        // ── Identity / scheduling fields ─────────────────────────────────
        dto.id                = task.getId();
        dto.key               = task.getKey();
        dto.name              = task.getName();
        dto.start             = task.getStart();
        dto.finish            = task.getFinish();
        dto.milestone         = task.isMilestone() && task.getChildTasks().isEmpty();
        dto.story             = !task.getChildTasks().isEmpty();
        dto.critical          = task.isCritical();
        dto.manuallyScheduled = task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED;
        dto.progress          = task.getProgress() != null ? task.getProgress().doubleValue() : 0.0;
        dto.rowIndex          = rowIndex;

        if (task.getAssignedUser() != null) {
            dto.calendarId       = task.getAssignedUser().getId();
            dto.assignedUserName = task.getAssignedUser().getName();
            // User availability percentage and location for tooltip
            if (!task.getAssignedUser().getAvailabilities().isEmpty()) {
                Number availability = task.getAssignedUser().getAvailabilities().getLast().getAvailability();
                dto.assignedUserAvailability = String.format("%.0f%%", availability.doubleValue() * 100);
            }
            if (!task.getAssignedUser().getLocations().isEmpty()) {
                dto.assignedUserCountry = task.getAssignedUser().getLocations().getLast().getCountry();
                dto.assignedUserState   = task.getAssignedUser().getLocations().getLast().getState();
            }
        }

        // ── Colour computation (mirrors AbstractGanttRenderer.drawTask) ──
        dto.textColor   = colorToHex(theme.ganttTheme.taskTextColor);
        dto.borderColor = task.isCritical()
                ? colorToHex(theme.ganttTheme.criticalTaskBorderColor)
                : colorToHex(theme.ganttTheme.taskBorderColor);

        Color fillColor;
        if (dto.milestone) {
            fillColor     = theme.ganttTheme.milestoneBgColor;
            dto.textColor = colorToHex(theme.ganttTheme.milestoneTextColor);
        } else if (dto.story) {
            fillColor     = theme.ganttTheme.storyColor;
            dto.textColor = colorToHex(theme.ganttTheme.storyTextColor);
        } else if (task.getAssignedUser() != null && task.getAssignedUser().getColor() != null) {
            Color userColor = task.getAssignedUser().getColor();
            // lighten by 50% then apply taskTransparency alpha (mirrors generateGanttColor)
            Color lightened = ColorUtil.lightenColor(userColor, 0.5f);
            fillColor = new Color(lightened.getRed(), lightened.getGreen(), lightened.getBlue(),
                    theme.ganttTheme.taskTransparency);
            // progress bar: lighter user colour (60% lightening)
            Color progressBase = ColorUtil.lightenColor(userColor, 0.6f);
            dto.progressColor = colorToHexWithAlpha(progressBase, 255);
        } else {
            fillColor = theme.burndownTheme.getAuthorColor(28);
        }
        dto.fillColor = colorToHexWithAlpha(fillColor, fillColor.getAlpha());

        // ── Dependency relations ─────────────────────────────────────────
        if (task.getPredecessors() != null) {
            for (Relation relation : task.getPredecessors()) {
                RelationDto rd = new RelationDto();
                rd.predecessorId = relation.getPredecessorId();
                rd.visible       = relation.isVisible();
                dto.predecessors.add(rd);
            }
        }

        return dto;
    }

    private static void addUserCalendar(GanttChartDto chart, User user, Set<UUID> calendarIds,
                                        LocalDate calendarStart, LocalDate calendarEnd) {
        if (user == null || user.getId() == null || !calendarIds.add(user.getId()))
            return;

        UserCalendarDto calendarDto = new UserCalendarDto();
        calendarDto.id = user.getId();
        ProjectCalendar calendar = user.getCalendar();
        if (calendar != null) {
            for (ProjectCalendarException exception : calendar.getCalendarExceptions()) {
                LocalDate exceptionStart = exception.getFromDate();
                LocalDate exceptionEnd   = exception.getToDate() != null ? exception.getToDate() : exceptionStart;
                if (exceptionStart.isAfter(calendarEnd) || exceptionEnd.isBefore(calendarStart))
                    continue;

                CalendarExceptionDto exceptionDto = new CalendarExceptionDto();
                exceptionDto.from   = exceptionStart.isBefore(calendarStart) ? calendarStart : exceptionStart;
                exceptionDto.to     = exceptionEnd.isAfter(calendarEnd) ? calendarEnd : exceptionEnd;
                exceptionDto.name   = exception.getName();
                exceptionDto.type   = getCalendarExceptionType(exception.getName());
                exceptionDto.letter = getCalendarExceptionLetter(exceptionDto.type);
                calendarDto.exceptions.add(exceptionDto);
            }
        }
        chart.calendars.add(calendarDto);
    }

    private static LocalDate getRenderedCalendarEnd(Sprint sprint, LocalDateTime now, int preRun, int postRun) {
        LocalDate end = sprint.getEnd().toLocalDate();
        if (now != null && now.toLocalDate().isAfter(end))
            end = now.toLocalDate();
        if (sprint.getStart().toLocalDate().isAfter(end))
            end = sprint.getStart().toLocalDate();
        return end.plusDays(preRun + postRun);
    }

    private static LocalDate getRenderedCalendarStart(Sprint sprint, LocalDateTime now, int preRun) {
        LocalDate start = sprint.getStart().toLocalDate();
        if (now != null && now.toLocalDate().isBefore(start))
            start = now.toLocalDate();
        if (sprint.getEnd().toLocalDate().isBefore(start))
            start = sprint.getEnd().toLocalDate();
        return start.minusDays(preRun);
    }

    // ── Colour utilities ──────────────────────────────────────────────────────

    /**
     * Converts a Java {@link Color} to a 6-digit hex string (#rrggbb).
     */
    private static String colorToHex(Color color) {
        if (color == null) return "#000000";
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Converts a Java {@link Color} to an 8-digit hex string (#rrggbbaa),
     * using the supplied alpha value (0–255) rather than the colour's own alpha.
     */
    private static String colorToHexWithAlpha(Color color, int alpha) {
        if (color == null) return "#000000ff";
        int a = alpha & 0xff;
        return String.format("#%02x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    public static @NonNull LocalDate getChartEnd(Sprint sprint, LocalDateTime now, int postRun) {
        // Extend the window by preRun / postRun days so there is room for labels
        // printed to the left of the first bar and to the right of the last bar.
        LocalDate chartEndDate = sprint.getLatestFinishDate().toLocalDate().plusDays(postRun);
        // Extend to include 'now' (unless sprint is closed)
        if (now != null && !sprint.isClosed()) {
            LocalDate today = now.toLocalDate();
            if (today.isAfter(chartEndDate))
                chartEndDate = today.plusDays(1);
        }
        return chartEndDate;
    }

    public static @NonNull LocalDate getChartStart(Sprint sprint, LocalDateTime now, int preRun) {
        // Extend the window by preRun / postRun days so there is room for labels
        // printed to the left of the first bar and to the right of the last bar.
        LocalDate chartStartDate = sprint.getEarliestStartDate().toLocalDate().minusDays(preRun);
        // Extend to include 'now' (unless sprint is closed)
        if (now != null && !sprint.isClosed()) {
            LocalDate today = now.toLocalDate();
            if (today.isBefore(chartStartDate))
                chartStartDate = today.minusDays(1);
        }
        return chartStartDate;
    }

    private static String getCalendarExceptionLetter(String type) {
        return switch (type) {
            case "VACATION" -> "V";
            case "TRIP" -> "T";
            case "SICK" -> "S";
            default -> "H";
        };
    }

    private static String getCalendarExceptionType(String name) {
        for (OffDayType type : OffDayType.values()) {
            if (type.name().equals(name))
                return type.name();
        }
        return OffDayType.HOLIDAY.name();
    }
}
