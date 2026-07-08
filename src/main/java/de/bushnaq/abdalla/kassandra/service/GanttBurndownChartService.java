/*
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0
 */

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;
import de.bushnaq.abdalla.kassandra.report.dao.WorklogRemaining;
import de.bushnaq.abdalla.kassandra.report.dao.theme.DarkTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.LightTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.rest.dto.GanttBurndownChartDto;
import de.bushnaq.abdalla.kassandra.rest.dto.GanttChartDto;
import de.bushnaq.abdalla.kassandra.rest.dto.ThemeDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlgraphics.java2d.color.ColorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

import static de.bushnaq.abdalla.kassandra.report.burndown.BurnDownRenderer.Y_AXIS_WIDTH;

/**
 * Builds a {@link GanttBurndownChartDto} from a fully-loaded {@link Sprint} so that
 * the client-side {@code gantt-burndown-bundle.js} can render an interactive,
 * zoomable combined Gantt+Burndown chart without any further server calls.
 *
 * <p>The burndown section mirrors the computation done in Java's
 * {@link de.bushnaq.abdalla.kassandra.report.burndown.BurnDownRenderer}.
 * The Gantt task rows reuse {@link GanttChartService}.
 */
@Service
@Slf4j
public class GanttBurndownChartService {

    /**
     * Default extra days shown after sprintEnd (matches GanttChartService).
     */
    public static final  int  DEFAULT_POST_RUN        = GanttChartService.DEFAULT_POST_RUN;
    /**
     * Default extra days shown before sprintStart (matches GanttChartService).
     */
    public static final  int  DEFAULT_PRE_RUN         = GanttChartService.DEFAULT_PRE_RUN;
    /**
     * Seconds in a 7.5-hour working day (8:00–12:00, 13:00–16:30).
     */
    private static final long SECONDS_PER_WORKING_DAY = 75L * 6L * 60L; // 27000

    private final DarkTheme         darkTheme;
    private final GanttChartService ganttChartService;
    private final LightTheme        lightTheme;

    @Autowired
    public GanttBurndownChartService(LightTheme lightTheme, DarkTheme darkTheme,
                                     GanttChartService ganttChartService) {
        this.lightTheme        = lightTheme;
        this.darkTheme         = darkTheme;
        this.ganttChartService = ganttChartService;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Builds the DTO using default preRun/postRun padding.
     *
     * @param sprint fully loaded sprint (tasks, users, worklogs populated)
     * @param now    current date/time for the "N" milestone
     * @param dark   {@code true} → dark theme colours
     */
    public GanttBurndownChartDto build(Sprint sprint, LocalDateTime now, boolean dark) {
        return build(sprint, now, dark, DEFAULT_PRE_RUN, DEFAULT_POST_RUN);
    }

    /**
     * Builds the DTO for the given sprint.
     *
     * @param sprint  fully loaded sprint
     * @param now     current date/time for the "N" milestone
     * @param dark    {@code true} → dark theme
     * @param preRun  extra days before sprintStart
     * @param postRun extra days after sprintEnd
     */
    public GanttBurndownChartDto build(Sprint sprint, LocalDateTime now, boolean dark,
                                       int preRun, int postRun) {
        Theme                 theme = dark ? darkTheme : lightTheme;
        GanttBurndownChartDto dto   = new GanttBurndownChartDto();

        List<Worklog>          worklogs        = sprint.getWorklogs() != null ? sprint.getWorklogs() : List.of();
        List<WorklogRemaining> wlRemainingList = sprint.getWorklogRemaining() != null ? sprint.getWorklogRemaining() : List.of();

        // Sort worklogs chronologically for correct running-total computation
        List<Worklog> sortedWorklogs = worklogs.stream()
                .sorted(Comparator.comparing(Worklog::getStart))
                .toList();

        // ── Chart window ────────────────────────────────────────────────────────
        LocalDate chartStartDate = sprint.getEarliestStartDate().toLocalDate().minusDays(preRun);
        LocalDate chartEndDate   = sprint.getLatestFinishDate().toLocalDate().plusDays(postRun);

        // Extend to include 'now' (unless sprint is closed)
        if (now != null && !sprint.isClosed()) {
            LocalDate today = now.toLocalDate();
            if (today.isBefore(chartStartDate)) chartStartDate = today.minusDays(1);
            if (today.isAfter(chartEndDate)) chartEndDate = today.plusDays(1);
        }

        LocalDateTime chartStart = chartStartDate.atStartOfDay();
        LocalDateTime chartEnd   = chartEndDate.atStartOfDay();
        int           totalDays  = (int) ChronoUnit.DAYS.between(chartStart, chartEnd) + 1;

        // ── Milestone dates ─────────────────────────────────────────────────────
        LocalDateTime firstWorklogDt   = null;
        LocalDateTime lastWorklogDt    = null;
        int           lastDayWithValue = 0;

        for (Worklog w : sortedWorklogs) {
            LocalDateTime wdt = w.getStart().toLocalDateTime();
            if (firstWorklogDt == null || wdt.isBefore(firstWorklogDt)) firstWorklogDt = wdt;
            if (lastWorklogDt == null || wdt.isAfter(lastWorklogDt)) lastWorklogDt = wdt;
        }

        // ── Build burndown meta ────────────────────────────────────────────────
        GanttBurndownChartDto.BurndownMeta meta = dto.burndownMeta;
        meta.firstDayX    = Y_AXIS_WIDTH;
        meta.chartStart   = chartStart;
        meta.chartEnd     = chartEnd;
        meta.sprintStart  = sprint.getStart();
        meta.sprintEnd    = sprint.getEnd();
        meta.now          = now;
        meta.releaseDate  = sprint.getReleaseDate();
        meta.preRun       = preRun;
        meta.postRun      = postRun;
        meta.totalDays    = totalDays;
        meta.sprintName   = sprint.getName();
        meta.sprintStatus = sprint.getStatus().name();
        meta.sprintClosed = sprint.isClosed();
        meta.theme        = ThemeDto.fromTheme(theme);

        Duration maxWorked = Duration.ZERO;
        if (sprint.getWorked() != null) maxWorked = maxWorked.plus(sprint.getWorked());
        if (sprint.getRemaining() != null) maxWorked = maxWorked.plus(sprint.getRemaining());
        meta.maxWorkedSeconds         = maxWorked.getSeconds();
        meta.estimatedBestWorkSeconds = maxWorked.getSeconds();
        meta.calendarSize             = CalendarSize.YEARS;

        // Hide "N" milestone: if sprint closed and now is >7 days after sprintEnd
        if (sprint.isClosed() && now != null && sprint.getEnd() != null) {
            if (now.toLocalDate().isAfter(sprint.getEnd().toLocalDate().plusDays(7))) {
                meta.now = null;
            }
        }

        // ── Collect unique authors (worklogs first, then worklogRemaining) ─────
        // Use LinkedHashMap to preserve insertion order (matches Java's sorted-key-list order)
        Map<UUID, User> authorMap     = new LinkedHashMap<>();
        Map<UUID, Long> workedSecs    = new HashMap<>();
        Map<UUID, Long> remainingSecs = new HashMap<>();

        for (Worklog w : sortedWorklogs) {
            User u = sprint.getUser(w.getAuthorId());
            if (u != null) authorMap.put(u.getId(), u);
        }
        for (WorklogRemaining wr : wlRemainingList) {
            User u = wr.getAuthor();
            if (u != null) {
                authorMap.put(u.getId(), u);
                remainingSecs.merge(u.getId(), wr.getRemaining().getSeconds(), Long::sum);
            }
        }
        for (UUID id : authorMap.keySet()) {
            workedSecs.put(id, 0L);
        }

        // ── Per-author accumulated work arrays ─────────────────────────────────
        int                 arrayLen   = totalDays + 3;
        Map<UUID, long[]>   workArrays = new LinkedHashMap<>();
        Map<UUID, String[]> tooltipArr = new LinkedHashMap<>();

        for (UUID id : authorMap.keySet()) {
            workArrays.put(id, new long[arrayLen]);
            tooltipArr.put(id, new String[arrayLen]);
        }

        // Running totals per author (for accumulation)
        Map<UUID, Long> running = new HashMap<>();
        for (UUID id : authorMap.keySet()) running.put(id, 0L);

        for (Worklog w : sortedWorklogs) {
            User u = sprint.getUser(w.getAuthorId());
            if (u == null) continue;
            UUID id = u.getId();

            long newTotal = running.getOrDefault(id, 0L) + w.getTimeSpent().getSeconds();
            running.put(id, newTotal);
            workedSecs.merge(id, w.getTimeSpent().getSeconds(), Long::sum);

            // Day offset from chartStart
            LocalDate workDay = w.getStart().toLocalDate();
            int       day     = (int) ChronoUnit.DAYS.between(chartStart.toLocalDate(), workDay);
            if (day < 0) day = 0;

            if (day + 1 < arrayLen) {
                workArrays.get(id)[day + 1] = newTotal;
                                              lastDayWithValue = Math.max(day, lastDayWithValue);

                // Build tooltip entry: "key | duration | comment"
                Task   t       = sprint.getTaskById(w.getTaskId());
                String key     = (t != null) ? t.getKey() : "?";
                String dur     = formatDuration(w.getTimeSpent());
                String comment = w.getComment() != null ? w.getComment() : "";
                String entry   = key + "\t" + "<b>" + dur + "</b>" + "\t" + comment;
                String prev    = tooltipArr.get(id)[day + 1];
                tooltipArr.get(id)[day + 1] = (prev != null && !prev.isEmpty()) ? prev + "\n" + entry : entry;
            }
        }

        // Set last/first worklog milestone dates
        if (!sortedWorklogs.isEmpty()) {
            meta.firstWorklogDate = (firstWorklogDt != null) ? firstWorklogDt.toLocalDate().atStartOfDay() : null;
            meta.lastWorklogDate  = chartStart.toLocalDate().plusDays(lastDayWithValue + 1).atStartOfDay();
        }

        // Compute nowDayIndex for forward-fill limit
        int nowDayIndex = totalDays - 1;
        if (now != null) {
            nowDayIndex = (int) ChronoUnit.DAYS.between(chartStart.toLocalDate(), now.toLocalDate());
            nowDayIndex = Math.max(0, Math.min(nowDayIndex, totalDays - 1));
        }

        // Forward-fill each author's array up to nowDayIndex + 2
        for (UUID id : authorMap.keySet()) {
            long[] aw   = workArrays.get(id);
            long   last = 0;
            for (int i = 1; i < arrayLen; i++) {
                if (i <= nowDayIndex + 2) {
                    if (aw[i] == 0 || last > aw[i]) aw[i] = last;
                    last = aw[i];
                }
            }
        }

        // ── Build AuthorSeriesDto list ─────────────────────────────────────────
        // Sort authors by descending total estimated work (worked + remaining), matching
        // Java's AuthorsContribution.getSortedKeyList() / MapValueCopmparator behaviour,
        // so that the stacking order in BurnDownRenderer.drawBurnDown() is faithful.
        List<UUID> sortedAuthorIds = new ArrayList<>(authorMap.keySet());
        sortedAuthorIds.sort((a1, a2) -> {
            long t1 = workedSecs.getOrDefault(a1, 0L) + remainingSecs.getOrDefault(a1, 0L);
            long t2 = workedSecs.getOrDefault(a2, 0L) + remainingSecs.getOrDefault(a2, 0L);
            return Long.compare(t2, t1);
        });

        int colorIdx = 0;
        for (UUID id : sortedAuthorIds) {
            User                                  u = authorMap.get(id);
            GanttBurndownChartDto.AuthorSeriesDto a = new GanttBurndownChartDto.AuthorSeriesDto();
            a.userName              = u.getName();
            a.totalWorkedSeconds    = workedSecs.getOrDefault(id, 0L);
            a.totalRemainingSeconds = remainingSecs.getOrDefault(id, 0L);

            // Color: lighten user color by 75% towards white, alpha = 128 (~50%)
            Color userColor = u.getColor();
            if (userColor == null) {
                int ci = colorIdx % theme.burndownTheme.burnDownColor.length;
                userColor = theme.burndownTheme.burnDownColor[ci];
            }
            Color lightened = ColorUtil.lightenColor(userColor, 0.75f);
            a.color = colorToHexWithAlpha(lightened, 128);

            // Arrays
            long[]   aw = workArrays.get(id);
            String[] tt = tooltipArr.get(id);
            for (int d = 0; d < arrayLen; d++) {
                a.accumulatedWorkPerDay.add(aw[d]);
                a.tooltipPerDay.add(buildTooltip(tt[d], u.getName()));
            }

            dto.authors.add(a);
            colorIdx++;
        }

        // ── Gantt guides ───────────────────────────────────────────────────────
        buildGanttGuides(sprint, chartStart, totalDays, dto);

        // Raise maxWorked if guides exceed it
        if (dto.ganttGuideWithoutBuffer != null && !dto.ganttGuideWithoutBuffer.isEmpty()) {
            long gMax = dto.ganttGuideWithoutBuffer.get(0);
            if (gMax > meta.maxWorkedSeconds) meta.maxWorkedSeconds = gMax;
        }
        if (dto.ganttGuideWithBuffer != null && !dto.ganttGuideWithBuffer.isEmpty()) {
            long gMax = dto.ganttGuideWithBuffer.get(0);
            if (gMax > meta.maxWorkedSeconds) meta.maxWorkedSeconds = gMax;
        }

        // ── Gantt task rows (delegate to GanttChartService) ───────────────────
        GanttChartDto ganttDto = ganttChartService.build(sprint, now, dark, preRun, postRun);
        dto.tasks = ganttDto.tasks;

        return dto;
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    /**
     * Computes gantt-derived planned burn-down guides and stores them in the DTO.
     * Mirrors Java: {@code BurnDownRenderer.initGanttGuide()} +
     * {@code BurnDownGuide.convertToAccumulatedValues()}.
     */
    private void buildGanttGuides(Sprint sprint, LocalDateTime chartStart,
                                  int totalDays, GanttBurndownChartDto dto) {
        if (sprint.getStart() == null || sprint.getEnd() == null) return;

        LocalDate csDate      = chartStart.toLocalDate();
        LocalDate sprintStart = sprint.getStart().toLocalDate();
        LocalDate sprintEnd   = sprint.getEnd().toLocalDate();
        int       startDayIdx = (int) ChronoUnit.DAYS.between(csDate, sprintStart);
        int       stopDayIdx  = (int) ChronoUnit.DAYS.between(csDate, sprintEnd);

        if (stopDayIdx < startDayIdx || startDayIdx < 0 || stopDayIdx >= totalDays + 3) return;

        // dailyWork[d] = work planned for day d (from chartStart), in seconds
        long[] withBuffer    = new long[totalDays + 3];
        long[] withoutBuffer = new long[totalDays + 3];

        for (Task task : sprint.getTasks()) {
            if (task.isMilestone() || !task.getChildTasks().isEmpty()) continue;
            if (!GanttUtil.isValidTask(task)) continue;
            if (task.getStart() == null || task.getFinish() == null) continue;

            float avail  = task.getAvailability();
            long  spd    = (long) (avail * SECONDS_PER_WORKING_DAY);
            int   tStart = (int) ChronoUnit.DAYS.between(csDate, task.getStart().toLocalDate());
            int   tStop  = (int) ChronoUnit.DAYS.between(csDate, task.getFinish().toLocalDate());
            tStart = Math.max(0, Math.min(tStart, withBuffer.length - 1));
            tStop  = Math.max(0, Math.min(tStop, withBuffer.length - 1));

            for (int d = tStart; d <= tStop; d++) {
                LocalDate day = csDate.plusDays(d);
                if (isWorkingDay(day)) {
                    withBuffer[d] += spd;
                    if (task.isImpactOnCost()) {
                        withoutBuffer[d] += spd;
                    }
                }
            }
        }

        List<Long> withBufferList    = convertToRemainingWork(withBuffer, totalDays);
        List<Long> withoutBufferList = convertToRemainingWork(withoutBuffer, totalDays);
        if (withBufferList != null) dto.ganttGuideWithBuffer = withBufferList;
        if (withoutBufferList != null) dto.ganttGuideWithoutBuffer = withoutBufferList;
    }

    /**
     * Builds tooltip HTML from raw tab-delimited worklog entries.
     * Mirrors Java: {@code DayWork.transactionsToTooltips()}.
     */
    private static String buildTooltip(String rawEntries, String authorName) {
        if (rawEntries == null || rawEntries.isBlank()) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(authorName)
                .append(" <table><tr><th><b>Key</b></th><th><b>Work</b></th><th><b>Summary</b></th></tr>");
        for (String entry : rawEntries.split("\n")) {
            String[] parts = entry.split("\t", 3);
            sb.append("<tr>");
            for (String p : parts) sb.append("<td>").append(p).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    /**
     * Converts a Java {@link Color} to a 6-digit hex string (#rrggbb).
     */
    static String colorToHex(Color color) {
        if (color == null) return "#000000";
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Converts a Java {@link Color} to an 8-digit hex string (#rrggbbaa),
     * using the supplied alpha value (0–255).
     */
    static String colorToHexWithAlpha(Color color, int alpha) {
        if (color == null) return "#000000ff";
        int a = Math.max(0, Math.min(255, alpha));
        return String.format("#%02x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    /**
     * Converts a per-day work array to a dwindling (remaining work) array.
     * Mirrors Java: {@code BurnDownGuide.convertToAccumulatedValues()}.
     * Returns {@code null} if total work is zero.
     */
    private static List<Long> convertToRemainingWork(long[] dailyWork, int days) {
        long total = 0;
        for (int i = 0; i < days; i++) total += dailyWork[i];
        if (total == 0) return null;

        List<Long> result = new ArrayList<>(days + 1);
        result.add(total);
        for (int i = 1; i <= days; i++) {
            result.add(result.get(i - 1) - dailyWork[i - 1]);
        }
        return result;
    }

    /**
     * Formats a Duration as "Xh Ym".
     * Mirrors Java: {@code DateUtil.create24hDurationString(duration, false, true, false)}.
     */
    private static String formatDuration(Duration d) {
        long totalSecs = d.getSeconds();
        long hours     = totalSecs / 3600;
        long minutes   = (totalSecs % 3600) / 60;
        if (hours > 0 && minutes > 0) return hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h";
        return minutes + "m";
    }

    private static boolean isWorkingDay(LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }
}
