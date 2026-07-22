/*
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0
 */

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.report.burndown.DayWork;
import de.bushnaq.abdalla.kassandra.report.dao.*;
import de.bushnaq.abdalla.kassandra.report.dao.theme.DarkTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.LightTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.AuthorSeriesDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.BurndownMetaDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttChartDto;
import de.bushnaq.abdalla.kassandra.rest.dto.theme.ThemeDto;
import de.bushnaq.abdalla.util.ErrorException;
import de.bushnaq.abdalla.util.Util;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

import static de.bushnaq.abdalla.kassandra.report.burndown.BurnDownRenderer.WORK_OUTSIDE_ALLOWED_TIME_BOUNDARIES_OCCURRED;
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
    public static final  int               DEFAULT_POST_RUN                                          = GanttChartService.DEFAULT_POST_RUN;
    /**
     * Default extra days shown before sprintStart (matches GanttChartService).
     */
    public static final  int               DEFAULT_PRE_RUN                                           = GanttChartService.DEFAULT_PRE_RUN;
    private static final String            ERROR_106_AGNTT_START_DATE_NOT_MACTHING_SPRINT_START_DATE = "Error #106: Gantt start date %s does not match sprint start date %s. Ignoring Gantt chart guide information";
    private static final int               ONE_WEEK                                                  = 7;
    private static final long              SECONDS_PER_HOUR                                          = 60 * 60;
    /**
     * Seconds in a 7.5-hour working day (8:00–12:00, 13:00–16:30).
     */
    private static final long              SECONDS_PER_WORKING_DAY                                   = 75L * 6L * 60L; // 27000
    private final        DarkTheme         darkTheme;
    private final        GanttChartService ganttChartService;
    private final        LightTheme        lightTheme;
    // ── Public API ─────────────────────────────────────────────────────────────
    public               Milestones        milestones;

    // ── Private helpers ─────────────────────────────────────────────────────────

    @Autowired
    public GanttBurndownChartService(LightTheme lightTheme, DarkTheme darkTheme, GanttChartService ganttChartService) {
        this.lightTheme        = lightTheme;
        this.darkTheme         = darkTheme;
        this.ganttChartService = ganttChartService;
    }

    /**
     * Builds the DTO using default preRun/postRun padding.
     *
     * @param sprint fully loaded sprint (tasks, users, worklogs populated)
     * @param now    current date/time for the "N" milestone
     * @param dark   {@code true} → dark theme colours
     */
    public GanttBurndownChartDto build(Sprint sprint, LocalDateTime now, boolean dark) throws Exception {
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
    public GanttBurndownChartDto build(Sprint sprint, LocalDateTime now, boolean dark, int preRun, int postRun) throws Exception {
        Theme                 theme = dark ? darkTheme : lightTheme;
        GanttBurndownChartDto dto   = new GanttBurndownChartDto();

        List<Worklog>          worklogs                   = sprint.getWorklogs() != null ? sprint.getWorklogs() : List.of();
        List<WorklogRemaining> wlRemainingList            = sprint.getWorklogRemaining() != null ? sprint.getWorklogRemaining() : List.of();
        AuthorsContribution    usersTotalContribution     = new AuthorsContribution();
        Map<User, DayWork[]>   usersWorkPerDayAccumulated = new HashMap<>();// work per author and day, were every day has the amount of work done at all previous days, first day has 0 work done


        createMilestones(sprint, now);
        calculateAuthorContribution(sprint, worklogs, wlRemainingList, usersTotalContribution);

        calculateUserWorkPerDayAccumulated(sprint, milestones, worklogs, wlRemainingList, usersTotalContribution, usersWorkPerDayAccumulated);//TODO stop passing Milestonres as parameter


        // Sort worklogs chronologically for correct running-total computation
        List<Worklog> sortedWorklogs = worklogs.stream().sorted(Comparator.comparing(Worklog::getStart)).toList();

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
            LocalDateTime wdt = w.getStart();
            if (firstWorklogDt == null || wdt.isBefore(firstWorklogDt)) firstWorklogDt = wdt;
            if (lastWorklogDt == null || wdt.isAfter(lastWorklogDt)) lastWorklogDt = wdt;
        }

        // ── Build burndown sprintOverviewMetaDto ────────────────────────────────────────────────
        BurndownMetaDto meta = dto.meta;
        meta.firstDayX    = Y_AXIS_WIDTH;
        meta.chartStart   = chartStart;
        meta.chartEnd     = chartEnd;
        meta.sprintStart  = sprint.getStart();
        meta.sprintEnd    = sprint.getEnd();
        meta.now          = now;
        meta.copyright    = Util.generateCopyrightString(ParameterOptions.getLocalNow());
        meta.releaseDate  = sprint.getReleaseDate();
        meta.preRun       = preRun;
        meta.postRun      = postRun;
        meta.totalDays    = totalDays;
        meta.sprintName   = sprint.getName();
        meta.sprintStatus = sprint.getStatus().name();
        meta.sprintClosed = sprint.isClosed();
        meta.theme        = ThemeDto.fromTheme(theme);

        Duration maxWorked = Duration.ZERO;
        if (sprint.getWorked() != null)
            maxWorked = maxWorked.plus(sprint.getWorked());
        if (sprint.getRemaining() != null)
            maxWorked = maxWorked.plus(sprint.getRemaining());
        meta.maxWorkedSeconds         = maxWorked.toSeconds();
        meta.estimatedBestWorkSeconds = maxWorked.toSeconds();
        meta.calendarSize             = CalendarSize.YEARS;

        // Hide "N" milestone: if sprint closed and now is >7 days after sprintEnd
        if (sprint.isClosed() && now != null && sprint.getEnd() != null) {
            if (now.toLocalDate().isAfter(sprint.getEnd().toLocalDate().plusDays(7))) {
                meta.now = null;
            }
        }

        // ── Collect unique authors (worklogs first, then worklogRemaining) ─────
        // Use LinkedHashMap to preserve insertion order (matches Java's sorted-key-list order)
        Map<UUID, User> authorMap = new LinkedHashMap<>();
//        Map<UUID, Long> workedSecs    = new HashMap<>();
//        Map<UUID, Long> remainingSecs = new HashMap<>();
//
        for (Worklog w : sortedWorklogs) {
            User u = sprint.getUser(w.getAuthorId());
            if (u != null)
                authorMap.put(u.getId(), u);
        }
//        for (WorklogRemaining wr : wlRemainingList) {
//            User u = wr.getAuthor();
//            if (u != null) {
//                authorMap.put(u.getId(), u);
//                remainingSecs.merge(u.getId(), wr.getRemaining().getSeconds(), Long::sum);
//            }
//        }
//        for (UUID id : authorMap.keySet()) {
//            workedSecs.put(id, 0L);
//        }
//
//        // ── Per-author accumulated work arrays ─────────────────────────────────
        int arrayLen = totalDays + 3;
//        Map<UUID, long[]>   workArrays = new LinkedHashMap<>();
        Map<UUID, String[]> tooltipArr = new LinkedHashMap<>();
//
        for (UUID id : authorMap.keySet()) {
//            workArrays.put(id, new long[arrayLen]);
            tooltipArr.put(id, new String[arrayLen]);
        }
//
//        // Running totals per author (for accumulation)
//        Map<UUID, Long> running = new HashMap<>();
//        for (UUID id : authorMap.keySet()) running.put(id, 0L);
//
        for (Worklog w : sortedWorklogs) {
            User u = sprint.getUser(w.getAuthorId());
            if (u == null) continue;
            UUID id = u.getId();
//
//            long newTotal = running.getOrDefault(id, 0L) + w.getTimeSpent().getSeconds();
//            running.put(id, newTotal);
//            workedSecs.merge(id, w.getTimeSpent().getSeconds(), Long::sum);
//
//            // Day offset from chartStart
            LocalDate workDay = w.getStart().toLocalDate();
            int       day     = (int) ChronoUnit.DAYS.between(chartStart.toLocalDate(), workDay);
            if (day < 0) day = 0;
//
            if (day + 1 < arrayLen) {
//                workArrays.get(id)[day + 1] = newTotal;
//                                              lastDayWithValue = Math.max(day, lastDayWithValue);
//
//                // Build tooltip entry: "key | duration | comment"
                Task   t       = sprint.getTaskById(w.getTaskId());
                String key     = (t != null) ? t.getKey() : "?";
                String dur     = formatDuration(w.getTimeSpent());
                String comment = w.getComment() != null ? w.getComment() : "";
                String entry   = key + "\t" + "<b>" + dur + "</b>" + "\t" + comment;
                String prev    = tooltipArr.get(id)[day + 1];
                tooltipArr.get(id)[day + 1] = (prev != null && !prev.isEmpty()) ? prev + "\n" + entry : entry;
            }
        }
        if (!usersTotalContribution.isEmpty()) {
            for (User user : usersTotalContribution.getSortedKeyList()) {
                AuthorSeriesDto a = new AuthorSeriesDto();
                a.userName              = user.getName();
                a.color                 = user.getColor().getRGB();
                a.totalRemainingSeconds = usersTotalContribution.get(user).remaining.toSeconds();
                a.totalWorkedSeconds    = usersTotalContribution.get(user).worked.toSeconds();
                a.accumulatedWorkPerDay = new ArrayList<Long>();
                for (int i = 0; i < usersWorkPerDayAccumulated.get(user).length; i++) {
                    DayWork dw = usersWorkPerDayAccumulated.get(user)[i];
                    a.accumulatedWorkPerDay.add(dw.duration.toSeconds());
                }
                String[] tt = tooltipArr.get(user.getId());
                for (int d = 0; d < arrayLen; d++) {
                    a.tooltipPerDay.add(buildTooltip(tt[d], user.getName()));
                }
//
                dto.authors.add(a);
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

//        // Forward-fill each author's array up to nowDayIndex + 2
//        for (UUID id : authorMap.keySet()) {
//            long[] aw   = workArrays.get(id);
//            long   last = 0;
//            for (int i = 1; i < arrayLen; i++) {
//                if (i <= nowDayIndex + 2) {
//                    if (aw[i] == 0 || last > aw[i]) aw[i] = last;
//                    last = aw[i];
//                }
//            }
//        }

        // ── Build AuthorSeriesDto list ─────────────────────────────────────────
        // Sort authors by descending total estimated work (worked + remaining), matching
        // Java's AuthorsContribution.getSortedKeyList() / MapValueCopmparator behaviour,
        // so that the stacking order in BurnDownRenderer.drawBurnDown() is faithful.
        List<UUID> sortedAuthorIds = new ArrayList<>(authorMap.keySet());
        if (!usersTotalContribution.isEmpty()) {
            for (User user : usersTotalContribution.getSortedKeyList()) {
                sortedAuthorIds.add(user.getId());
            }
        }


//        sortedAuthorIds.sort((a1, a2) -> {
//            long t1 = workedSecs.getOrDefault(a1, 0L) + remainingSecs.getOrDefault(a1, 0L);
//            long t2 = workedSecs.getOrDefault(a2, 0L) + remainingSecs.getOrDefault(a2, 0L);
//            return Long.compare(t2, t1);
//        });
//
//        int colorIdx = 0;
//        for (UUID id : sortedAuthorIds) {
//            User                                  u = authorMap.get(id);
//            GanttBurndownChartDto.AuthorSeriesDto a = new GanttBurndownChartDto.AuthorSeriesDto();
//            a.userName              = u.getName();
//            a.totalWorkedSeconds    = workedSecs.getOrDefault(id, 0L);
//            a.totalRemainingSeconds = remainingSecs.getOrDefault(id, 0L);
//
//            // Color: lighten user color by 75% towards white, alpha = 128 (~50%)
//            Color userColor = u.getColor();
//            if (userColor == null) {
//                int ci = colorIdx % theme.burndownTheme.burnDownColor.length;
//                userColor = theme.burndownTheme.burnDownColor[ci];
//            }
//            Color lightened = ColorUtil.lightenColor(userColor, 0.75f);
//            a.color = colorToHexWithAlpha(lightened, 128);
//
//            // Arrays
//            long[]   aw = workArrays.get(id);
//            String[] tt = tooltipArr.get(id);
//            for (int d = 0; d < arrayLen; d++) {
//                a.accumulatedWorkPerDay.add(aw[d]);
//                a.tooltipPerDay.add(buildTooltip(tt[d], u.getName()));
//            }
//
//            dto.authors.add(a);
//            colorIdx++;
//        }


        // ── Gantt guides ───────────────────────────────────────────────────────
        {
//            int startDayIndex = calculateDayIndex(sprint.getStart());
            int stopDayIndex = calculateDayIndex(sprint.getEnd());

            BurnDownGuide ganttWorkWithoutBufferPerDayAccumulated = new BurnDownGuide(sprint.getStart(), stopDayIndex);
            BurnDownGuide ganttWorkWithBufferPerDayAccumulated    = new BurnDownGuide(sprint.getStart(), stopDayIndex);
            initGanttGuide(sprint, now, ganttWorkWithoutBufferPerDayAccumulated, false);
            initGanttGuide(sprint, now, ganttWorkWithBufferPerDayAccumulated, true);
            dto.ganttGuideWithoutBuffer = new ArrayList<>();
            for (Duration duration : ganttWorkWithoutBufferPerDayAccumulated.getDwindlingWork()) {
                dto.ganttGuideWithoutBuffer.add(duration.toSeconds());
            }
            dto.ganttGuideWithBuffer = new ArrayList<>();
            for (Duration duration : ganttWorkWithBufferPerDayAccumulated.getDwindlingWork()) {
                dto.ganttGuideWithBuffer.add(duration.toSeconds());
            }
        }

//        buildGanttGuides(sprint, chartStart, totalDays, dto);

        // Raise maxWorked if guides exceed it
//        if (dto.ganttGuideWithoutBuffer != null && !dto.ganttGuideWithoutBuffer.isEmpty()) {
//            long gMax = dto.ganttGuideWithoutBuffer.get(0);
//            if (gMax > meta.maxWorkedSeconds)
//                meta.maxWorkedSeconds = gMax;
//        }
//        if (dto.ganttGuideWithBuffer != null && !dto.ganttGuideWithBuffer.isEmpty()) {
//            long gMax = dto.ganttGuideWithBuffer.get(0);
//            if (gMax > meta.maxWorkedSeconds)
//                meta.maxWorkedSeconds = gMax;
//        }

        // ── Gantt task rows (delegate to GanttChartService) ───────────────────
        GanttChartDto ganttDto = ganttChartService.build(sprint, now, dark, preRun, postRun);
        dto.tasks = ganttDto.tasks;

        return dto;
    }

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

    public void calculateAuthorContribution(Sprint sprint, List<Worklog> worklogList, List<WorklogRemaining> worklogRemaining,
                                            AuthorsContribution authorsContribution) {
        if (worklogRemaining != null) {
            for (WorklogRemaining work : worklogRemaining) {
                AuthorContribution ac = authorsContribution.get(work.getAuthor());
                if (ac == null) {
                    ac = new AuthorContribution();
                    authorsContribution.put(work.getAuthor(), ac);
                }
                ac.addRemaining(work.getRemaining());
            }
        }
        if (worklogList != null) {
            for (Worklog work : worklogList) {
                AuthorContribution ac = authorsContribution.get(sprint.getUser(work.getAuthorId()));
                if (ac == null) {
                    ac = new AuthorContribution();
                    authorsContribution.put(sprint.getUser(work.getAuthorId()), ac);
                }
                ac.addWorked(work.getTimeSpent());
            }
        }
    }

    protected LocalDate calculateDayFromIndex(int index) {
        LocalDate firstMilestoneDay = milestones.firstMilestone;
        return DateUtil.addDay(firstMilestoneDay, index);
    }

    protected int calculateDayIndex(LocalDate date) {
        LocalDate firstMilestoneDay = milestones.firstMilestone;
        return DateUtil.calculateDays(firstMilestoneDay, date);
    }

    protected int calculateDayIndex(LocalDateTime date) {
        return calculateDayIndex(date.toLocalDate());
    }

    public void calculateUserWorkPerDayAccumulated(Sprint sprint, Milestones milestones, List<Worklog> worklog, List<WorklogRemaining> worklogRemaining, AuthorsContribution usersTotalContribution, Map<User, DayWork[]> usersWorkPerDayAccumulated) {
        this.milestones = milestones;
        Map<User, Duration> authorWorkSum = new HashMap<>();// total work done by any author
        for (User user : usersTotalContribution.getSortedKeyList()) {
            if (authorWorkSum.get(user) == null) {
                authorWorkSum.put(user, Duration.ZERO);
            }
        }
        int days = DateUtil.calculateDays(milestones.firstMilestone, milestones.lastMilestone) + 1;
        if (worklog != null) {
            int lastDayIndexWithValue = 0;// the last day any author has data
            System.out.println("========================================================================================");
            for (Worklog work : worklog) {
//                if (sprint.getTaskById(work.getTaskId()).getKey().equals("T-2"))
//                if (sprint.getUser(work.getAuthorId()).getName().equals("Jennifer Holleman")) {
//                    System.out.println("start=" + work.getStart() + " " + DateUtil.createDurationString(work.getTimeSpent(), false, true, true) + " " + sprint.getUser(work.getAuthorId()).getName());
//                }
                Duration  aws     = authorWorkSum.get(sprint.getUser(work.getAuthorId()));
                DayWork[] dayWork = usersWorkPerDayAccumulated.get(sprint.getUser(work.getAuthorId()));
                if (dayWork == null) {
                    // create a list of work per day for authors that have worked
                    dayWork    = new DayWork[days + 3];// first value is 0
                    dayWork[0] = new DayWork();
                    usersWorkPerDayAccumulated.put(sprint.getUser(work.getAuthorId()), dayWork);
                }
                aws = aws.plus(work.getTimeSpent());
                authorWorkSum.put(sprint.getUser(work.getAuthorId()), aws);
                int day = (DateUtil.calculateDays(milestones.firstMilestone, DateUtil.toDayPrecision(work.getStart())));
                if (day < 0) {
                    // ignore any data before first day of sprint
                    day = 0;
                }
                if (day < days) {
                    {
                        if (dayWork[day + 1] != null) {
                            dayWork[day + 1].setDuration(aws);
                            dayWork[day + 1].add(sprint, work);
                        } else {
                            dayWork[day + 1] = new DayWork(aws);// every day has the amount of work done at that day and all days before that
                            dayWork[day + 1].add(sprint, work);
                        }
                        if (!aws.isZero()) {
                            lastDayIndexWithValue = Math.max(day, lastDayIndexWithValue);
                        }
                    }
                } else {
//                    numberOfWorkExceptions++;
                    log.error(WORK_OUTSIDE_ALLOWED_TIME_BOUNDARIES_OCCURRED);
                }

            }
            System.out.println("========================================================================================");
            milestones.add(calculateDayFromIndex(lastDayIndexWithValue + 1), "L", "last value + 1", Color.red, true);
        }
        milestones.calculate();
        int nowDayIndex = (DateUtil.calculateDays(milestones.firstMilestone, DateUtil.min(milestones.lastMilestone, milestones.get("N").time)));
        for (User user : usersTotalContribution.getSortedKeyList()) {
            Duration  last = Duration.ZERO;
            DayWork[] aw   = usersWorkPerDayAccumulated.get(user);
            if (aw == null) {
                // create a list of work per day for authors that have no work done yet and thus
                // where missed in the above use case involving worklog
                aw    = new DayWork[days + 3];
                aw[0] = new DayWork();
                usersWorkPerDayAccumulated.put(user, aw);
            }
            // fill in empty parts of the days list
            for (int i = 1; i < usersWorkPerDayAccumulated.get(user).length; i++) {
                if (i <= nowDayIndex + 2) {
                    if (usersWorkPerDayAccumulated.get(user)[i] == null
                            || last.toSeconds() > usersWorkPerDayAccumulated.get(user)[i].duration.toSeconds()) {
                        usersWorkPerDayAccumulated.get(user)[i] = new DayWork(last);
                    }
                    last = usersWorkPerDayAccumulated.get(user)[i].duration;
                } else {
                    // ignore anything after today (only makes sense in a test scenario, where we
                    // simulate a now time in the past)
                    if (usersWorkPerDayAccumulated.get(user)[i] == null) {
                        usersWorkPerDayAccumulated.get(user)[i] = new DayWork(Duration.ZERO);
                    }
                }
            }
        }
    }

    private void calculateWorkPerDay(Sprint sprint, Task task, BurnDownGuide guide, boolean print) throws Exception {
        //TODO must use user calendar for start/end times
        LocalDateTime start = task.getStart();
        LocalDateTime stop  = task.getFinish();
        if (!task.isMilestone()) {
            if (GanttUtil.isValidTask(task) && !task.isMilestone() && task.getChildTasks().isEmpty()) {
                if (stop.isBefore(start) || stop.isEqual(start)) {
                    sprint.exceptions.add(new ErrorException(String.format("Task %s finish time has to be after start time. Ignoring task.", task.getName())));
                } else {
                    //                    Duration duration0 = task.getDuration();
//                    for (User assignment : task.getAssignedUser())
                    {
//                        Resource resource =  assignment.getResource();

//                        Number availability = assignment.getUnits();
                        Number availability = task.getAvailability();
                        if (/*resource != null &&*/ availability != null) {
                            int  startDayIndex = calculateDayIndex(start);
                            int  stopDayIndex  = calculateDayIndex(stop);
                            long oneDay        = 75 * SECONDS_PER_HOUR / 10;
                            //                            long oneHour = SECONDS_PER_HOUR;
                            if (stopDayIndex == startDayIndex) {
                                // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
                                // start time might not be in the morning at exactly 8:00
                                // stop time might not be in the afternoon 15:30
                                LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(start);
                                LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(start);
                                if (start.isBefore(lunchStartTime) || start.isEqual(lunchStartTime)) {
                                    // if(start <= 12:00)
                                    if (stop.isBefore(lunchStartTime) || stop.isEqual(lunchStartTime)) {
                                        // if(stop <= 12:00)
                                        // (stop - start)/7.5
                                        double   fraction = (double) Duration.between(start, stop).getSeconds() / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        if (print)
                                            System.out.printf("[1] %s start & stop <= 12:00 fraction=%f work=%s%n", task.getName(), fraction, work);
                                        guide.add(startDayIndex, work);
                                    } else if (stop.isAfter(lunchStopTime) || stop.isEqual(lunchStopTime)) {
                                        // if(stop >= 13:00)
                                        // ( stop - start -1h )/7.5
                                        double   fraction = (double) Duration.between(start, stop).minusHours(1).getSeconds() / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        if (print)
                                            System.out.printf("[2] %s start <= 12:00 & stop >= 13:00 fraction=%f work=%s%n", task.getName(), fraction, work);
                                        guide.add(startDayIndex, work);
                                    } else {
                                        throw new Exception(String.format("Task %s stop within lunch time", task.getName()));
                                    }
                                } else if (start.isAfter(lunchStopTime) || start.isEqual(lunchStopTime)) {
                                    // if(start >= 13:00 && stop >= 13:00)
                                    // (stop - Start))/7.5
                                    double   fraction = ((double) Duration.between(start, stop).getSeconds()) / oneDay;
                                    Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                    if (print)
                                        System.out.printf("[3] %s start >= 13:00 & stop >= 13:00 fraction=%f work=%s%n", task.getName(), fraction, work);
                                    guide.add(startDayIndex, work);
                                } else {
                                    throw new Exception(String.format("Task %s stop before start", task.getName()));
                                }

                            } else {
                                // start
                                {
                                    // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 -  16:30 (7.5h, with lunch time at 12:00)
                                    // start time might not be in the morning at exactly 8:00
                                    LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(start);
                                    LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(start);
                                    if (start.isBefore(lunchStartTime) || start.isEqual(lunchStartTime)) {
                                        double   fraction = ((double) Duration.between(start.toLocalTime(), LocalTime.of(16, 30)).minusHours(1).getSeconds()) / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        if (print)
                                            System.out.printf("[4] %s start <= 12:00 & stop > today fraction=%f work=%s%n", task.getName(), fraction, work);
                                        guide.add(startDayIndex, work);
                                    } else if (start.isAfter(lunchStopTime) || start.isEqual(lunchStopTime)) {
                                        double   fraction = ((double) Duration.between(start.toLocalTime(), LocalTime.of(16, 30)).minusHours(1).getSeconds()) / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        if (print)
                                            System.out.printf("[5] %s start  >= 13:00 & stop > today fraction=%f work=%s%n", task.getName(), fraction, work);
                                        guide.add(startDayIndex, work);
                                    } else {
                                        throw new Exception(String.format("Task %s start within lunch time", task.getName()));
                                    }
                                }
                                // end
                                {
                                    // ---We assume that a task is worked on every day from 8:00 - 12:00, 13:00 - 16:30 (7.5h, with lunch time at 12:00)
                                    // last day, stop time might not be in the afternoon 16:30
                                    LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(stop);
                                    LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(stop);
                                    if (stop.isBefore(lunchStartTime) || stop.isEqual(lunchStartTime)) {
                                        // if(stop <= 12:00)
                                        // (stop - 8:00)/7.5
                                        double   fraction = ((double) Duration.between(LocalTime.of(8, 0), stop.toLocalTime()).getSeconds()) / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        if (print)
                                            System.out.printf("[6] %s start < today & stop <= 12:00 fraction=%f work=%s%n", task.getName(), fraction, work);
                                        guide.add(stopDayIndex, work);
                                    } else if (stop.isAfter(lunchStopTime) || stop.isEqual(lunchStopTime)) {
                                        // if(stop >= 13:00)
                                        // (stop - 8:00 -1h)/7.5
                                        double   fraction = ((double) Duration.between(LocalTime.of(8, 0), stop.toLocalTime()).minusHours(1).getSeconds()) / oneDay;
                                        Duration work     = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        if (print)
                                            System.out.printf("[7] %s start < today & stop >= 13:00 fraction=%f work=%s%n", task.getName(), fraction, work);
                                        guide.add(stopDayIndex, work);
                                    } else {
                                        throw new Exception(String.format("Task %s stop within lunch time", task.getName()));
                                    }
                                }
                            }
                            if (startDayIndex + 1 < stopDayIndex) {
                                for (int index = startDayIndex + 1; index < stopDayIndex; index++) {
                                    LocalDate today = calculateDayFromIndex(index);
                                    if (task.getEffectiveCalendar().isWorkingDate(today))
//                                    if (isResourceWorkingDay(context, task.getAssignedUser(), today))
                                    {
                                        Duration work = Duration.ofSeconds((long) (availability.doubleValue() * SECONDS_PER_WORKING_DAY));
                                        if (print)
                                            System.out.printf("[8] %s start < today & stop > today fraction=1.0 work=%s%n", task.getName(), work);
                                        guide.add(index, work);
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
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

    protected void createMilestones(Sprint sprint, LocalDateTime now) {
        if (milestones == null) {
            //TODO should we include first and last worklog date?
            milestones = new Milestones(sprint.getName());
            if (sprint.getStart() != null) {
                milestones.add(sprint.getStart().toLocalDate(), "S", "Start (Start of project)", Color.blue);
            }
            milestones.add(now.toLocalDate(), "N", "Now (current date)", Color.blue);
            if (sprint.getEnd() != null) {
                milestones.add(sprint.getEnd().toLocalDate(), "E", "End (End of project)", Color.blue);
            }
            if (sprint.getReleaseDate() != null) {
                milestones.add(sprint.getReleaseDate().toLocalDate(), "R", "Release (Estimated release date)", Color.blue);
            }
            if (isHideNow(now, sprint.getEnd(), sprint.isClosed())) {
                milestones.remove("N");
            }
//        if (firstWorklog != null && (sprint.getStart() == null || !firstWorklog.toLocalDate().isEqual(sprint.getStart().toLocalDate()))) {
//            milestones.add(firstWorklog.toLocalDate(), "F", "First punch-in", Color.blue);
//        }
//        if (lastWorklog != null && (sprint.getEnd() == null || !lastWorklog.toLocalDate().isEqual(sprint.getEnd().toLocalDate()))) {
//            milestones.add(lastWorklog.toLocalDate(), "L", "Last punch-out", Color.blue);
//        }
            milestones.calculate();
            // milestones.print();//debugging code
        }
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

    /**
     * Initialize BurnDownGuide object from gantt chart of this project to visualize
     * planned burn down rate
     *
     * @throws Exception
     */
    public void initGanttGuide(Sprint sprint, LocalDateTime now, BurnDownGuide burndownguide, boolean isWithBuffer) throws Exception {
        System.out.println("--------------------------------------------------------------------------------------------");

        for (Task task : sprint.getTasks()) {
            if (!task.isMilestone() && task.getChildTasks().isEmpty()) {
                //only include tasks that have an impact on the cost, as otherwise they are also not included in the sprint (e.g. delivery buffers)
                if (task.isImpactOnCost() || isWithBuffer) {
                    calculateWorkPerDay(sprint, task, burndownguide, false);
                }
            }
        }
        burndownguide.convertToAccumulatedValues();
        System.out.println("--------------------------------------------------------------------------------------------");
    }

    protected boolean isHideNow(LocalDateTime now, LocalDateTime end, boolean completed) {
        if (completed) {
            // We do not want to keep drawing the graph further and further to include the
            // current date, if it is closed.
            return end != null && now.isAfter(DateUtil.addDay(end, ONE_WEEK));
        }
        return false;
    }

    private static boolean isWorkingDay(LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

}
