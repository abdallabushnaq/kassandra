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

import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.dao.theme.DarkTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.LightTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.rest.dto.GanttChartDto;
import de.bushnaq.abdalla.kassandra.rest.dto.ThemeDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlgraphics.java2d.color.ColorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds a {@link GanttChartDto} from a fully-loaded {@link Sprint} so that
 * the client-side {@code gantt-chart.js} can render an interactive, zoomable
 * Gantt chart without any further server calls.
 *
 * <p>Colour computation mirrors {@code AbstractGanttRenderer.drawTask()} and
 * related methods.  Calendar exceptions are extracted from each task's
 * assigned user's off-day list so the browser can determine working vs.
 * non-working days per row.
 */
@Service
@Slf4j
public class GanttChartService {

    private final DarkTheme  darkTheme;
    private final LightTheme lightTheme;

    @Autowired
    public GanttChartService(LightTheme lightTheme, DarkTheme darkTheme) {
        this.lightTheme = lightTheme;
        this.darkTheme  = darkTheme;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Default number of extra days rendered before the first task starts. */
    public static final int DEFAULT_PRE_RUN  = 14;
    /** Default number of extra days rendered after the last task finishes. */
    public static final int DEFAULT_POST_RUN = 14;

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
     * @param sprint   fully loaded sprint (tasks, users, worklogs must be populated)
     * @param now      current date/time used for the "N" now-marker
     * @param dark     {@code true} → use dark theme colours
     * @param preRun   number of extra days to show before the earliest task start
     * @param postRun  number of extra days to show after the latest task finish
     * @return populated DTO ready for JSON serialisation
     */
    public GanttChartDto build(Sprint sprint, LocalDateTime now, boolean dark, int preRun, int postRun) {
        Theme         theme = dark ? darkTheme : lightTheme;
        GanttChartDto dto   = new GanttChartDto();

        // ── Chart date range ─────────────────────────────────────────────
        // Extend the window by preRun / postRun days so there is room for labels
        // printed to the left of the first bar and to the right of the last bar.
        LocalDate chartStartDate = sprint.getEarliestStartDate().toLocalDate().minusDays(preRun);
        LocalDate chartEndDate   = sprint.getLatestFinishDate().toLocalDate().plusDays(postRun);

        // Stretch range to include 'now' unless the sprint is already closed
        if (now != null && !Status.CLOSED.equals(sprint.getStatus())) {
            LocalDate today = now.toLocalDate();
            if (today.isBefore(chartStartDate)) chartStartDate = today.minusDays(1);
            if (today.isAfter(chartEndDate)) chartEndDate = today.plusDays(1);
        }

        dto.meta.chartStart = chartStartDate.atStartOfDay();
        dto.meta.chartEnd   = chartEndDate.atStartOfDay();
        dto.meta.now        = now;
        dto.meta.sprintName = sprint.getName();
        dto.meta.preRun     = preRun;
        dto.meta.postRun    = postRun;
        dto.meta.theme = ThemeDto.fromTheme(theme);

        // ── Project-level milestones ───────────────────────────────────
        // These appear as S (start), E (end), N (now) markers in the calendar header
        GanttChartDto.MilestoneDto startMilestone = new GanttChartDto.MilestoneDto();
        startMilestone.date  = sprint.getEarliestStartDate().toLocalDate();
        startMilestone.letter = "S";
        startMilestone.label  = "Start";
        dto.milestones.add(startMilestone);

        GanttChartDto.MilestoneDto endMilestone = new GanttChartDto.MilestoneDto();
        endMilestone.date  = sprint.getLatestFinishDate().toLocalDate();
        endMilestone.letter = "E";
        endMilestone.label  = "End";
        dto.milestones.add(endMilestone);

        if (now != null && !Status.CLOSED.equals(sprint.getStatus())) {
            GanttChartDto.MilestoneDto nowMilestone = new GanttChartDto.MilestoneDto();
            nowMilestone.date   = now.toLocalDate();
            nowMilestone.letter = "N";
            nowMilestone.label  = "Now";
            dto.milestones.add(nowMilestone);
        }

        // ── Task rows ─────────────────────────────────────────────────────
        int rowIndex = 0;
        for (Task task : sprint.getTasks()) {
            if (GanttUtil.isValidTask(task)) {
                dto.tasks.add(buildTaskDto(task, rowIndex, theme));
                rowIndex++;
            }
        }

        return dto;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private GanttChartDto.TaskDto buildTaskDto(Task task, int rowIndex, Theme theme) {
        GanttChartDto.TaskDto dto = new GanttChartDto.TaskDto();

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
            fillColor   = theme.ganttTheme.milestoneBgColor;
            dto.textColor = colorToHex(theme.ganttTheme.milestoneTextColor);
        } else if (dto.story) {
            fillColor   = theme.ganttTheme.storyColor;
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
                GanttChartDto.RelationDto rd = new GanttChartDto.RelationDto();
                rd.predecessorId = relation.getPredecessorId();
                rd.visible       = relation.isVisible();
                dto.predecessors.add(rd);
            }
        }

        // ── Calendar exceptions from assigned user's off-day list ────────
        if (task.getAssignedUser() != null) {
            List<OffDay> offDays = task.getAssignedUser().getOffDays();
            if (offDays != null) {
                for (OffDay offDay : offDays) {
                    GanttChartDto.CalendarExceptionDto ex = new GanttChartDto.CalendarExceptionDto();
                    ex.from   = offDay.getFirstDay();
                    ex.to     = offDay.getLastDay();
                    ex.type   = offDay.getType() != null ? offDay.getType().name() : "HOLIDAY";
                    ex.letter = getOffDayLetter(offDay.getType());
                    dto.calendarExceptions.add(ex);
                }
            }
        }

        return dto;
    }

    // ── Colour utilities ──────────────────────────────────────────────────────

    /** Converts a Java {@link Color} to a 6-digit hex string (#rrggbb). */
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
        int a = Math.max(0, Math.min(255, alpha));
        return String.format("#%02x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    /** Returns the single-letter code for an off-day type. */
    private static String getOffDayLetter(OffDayType type) {
        if (type == null) return "H";
        return switch (type) {
            case VACATION -> "V";
            case TRIP     -> "T";
            case SICK     -> "S";
            case HOLIDAY  -> "H";
        };
    }
}


