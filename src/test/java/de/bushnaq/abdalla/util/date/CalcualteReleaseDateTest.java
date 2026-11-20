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

package de.bushnaq.abdalla.util.date;

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.ui.view.SprintStatistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("UnitTest")
public class CalcualteReleaseDateTest {
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    //    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void bigDelayTest() {
        LocalDateTime start = LocalDateTime.parse("2018-03-12 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-03-16 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 12, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        //100% efficency
        Duration      worked         = Duration.ofMinutes(2 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        Duration      remaining      = Duration.ofMinutes(8 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        LocalDateTime release        = ReportUtil.calculateReleaseDate(start, now, worked, worked.plus(remaining));
        LocalDateTime expectedResult = LocalDateTime.parse("2018-04-13 08:00", formatter);
        //        Calendar expectedResult = new GregorianCalendar(2018, Calendar.APRIL, 13, 8, 0);
        assertEquals(expectedResult.format(formatter), release.format(formatter));
    }

    @Test
    public void ct2Test() {
        //start=2018.Aug.28 11:17 now=2018.Sep.14 08:39 end=2018.Okt.08 18:00 worked=01w 02d 05h 39m remaining=03w 01d 02h 31m
        LocalDateTime start = LocalDateTime.parse("2018-08-28 11:17", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-09-13 08:39", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.AUGUST, 28, 11, 17);
        //        Calendar now = new GregorianCalendar(2018, Calendar.SEPTEMBER, 13, 8, 39);
        Duration worked    = Duration.ofMinutes(7 * ReportUtil.ONE_WORKING_DAY_MINUTES + 5).plusHours(5).plusMinutes(39);
        Duration remaining = Duration.ofMinutes(16 * ReportUtil.ONE_WORKING_DAY_MINUTES).plusHours(2).plusMinutes(31);

        //        Long worked = new Long(7 * Util.ONE_WORKING_DAY_MILLIS + (5 * 60 + 39) * 60 * 1000);
        //        long remaining = 16 * Util.ONE_WORKING_DAY_MILLIS + (2 * 60 + 31) * 60 * 1000;
        LocalDateTime release = ReportUtil.calculateReleaseDate(start, now, worked, worked.plus(remaining));
        //        logger.trace(String.format("release=%s", DateUtil.createDateString(release, dateUtil.sdfymdhm)));
        LocalDateTime expectedResult = LocalDateTime.parse("2018-10-23 11:17", formatter);
        //        Calendar expectedResult = new GregorianCalendar(2018, Calendar.OCTOBER, 23, 11, 17);
        assertEquals(expectedResult.format(formatter), release.format(formatter));
    }

    @Test
    public void midwayFinishedTest() {
        LocalDateTime start = LocalDateTime.parse("2018-03-12 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-03-16 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 12, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        //100% efficency
        Duration worked    = Duration.ofMinutes(10 * ReportUtil.ONE_WORKING_DAY_MINUTES + 5);
        Duration remaining = Duration.ofMinutes(0L);
        //        Long worked = new Long(10 * Util.ONE_WORKING_DAY_MILLIS);
        //        long remaining = 0 * Util.ONE_WORKING_DAY_MILLIS;
        LocalDateTime release        = ReportUtil.calculateReleaseDate(start, now, worked, worked.plus(remaining));
        LocalDateTime expectedResult = LocalDateTime.parse("2018-03-16 08:00", formatter);
        //        Calendar expectedResult = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        assertEquals(expectedResult.format(formatter), release.format(formatter));
    }

    @Test
    public void noDelayTest() {
        LocalDateTime start = LocalDateTime.parse("2018-03-12 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-03-16 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 12, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        //100% efficency
        Duration worked    = Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES + 5);
        Duration remaining = Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        //        Long worked = new Long(5 * Util.ONE_WORKING_DAY_MILLIS);
        //        long remaining = 5 * Util.ONE_WORKING_DAY_MILLIS;
        LocalDateTime release        = ReportUtil.calculateReleaseDate(start, now, worked, worked.plus(remaining));
        LocalDateTime expectedResult = LocalDateTime.parse("2018-03-23 08:00", formatter);
        //        Calendar expectedResult = new GregorianCalendar(2018, Calendar.MARCH, 23, 8, 0);
        assertEquals(expectedResult.format(formatter), release.format(formatter));
    }

    @Test
    public void smallDelayTest() {
        LocalDateTime start = LocalDateTime.parse("2018-03-12 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-03-16 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 12, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        //100% efficency
        Duration worked    = Duration.ofMinutes(4 * ReportUtil.ONE_WORKING_DAY_MINUTES + 5);
        Duration remaining = Duration.ofMinutes(6 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        //        Long worked = new Long(4 * Util.ONE_WORKING_DAY_MILLIS);
        //        long remaining = 6 * Util.ONE_WORKING_DAY_MILLIS;
        LocalDateTime release        = ReportUtil.calculateReleaseDate(start, now, worked, worked.plus(remaining));
        LocalDateTime expectedResult = LocalDateTime.parse("2018-03-28 08:00", formatter);
        //        Calendar expectedResult = new GregorianCalendar(2018, Calendar.MARCH, 28, 8, 0);
        assertEquals(expectedResult.format(formatter), release.format(formatter));
    }

    @Test
    void releaseDelayCountsOnlyWorkingDaysAfterTheEnd() {
        LocalDateTime start = LocalDateTime.parse("2026-02-02T08:00");
        LocalDateTime end   = LocalDateTime.parse("2026-02-24T10:13:54");

        assertEquals(0.0, ReportUtil.calculateExtrapolatedScheduleDelayFraction(start, end, end.minusDays(1)));
        assertEquals(0.0, ReportUtil.calculateExtrapolatedScheduleDelayFraction(start, end, end.withHour(18)));
        assertEquals("0m", ReportUtil.calculateExtrapolatedScheduleDelayString(start, end, end.withHour(18)));
        assertEquals(1.0 / 17, ReportUtil.calculateExtrapolatedScheduleDelayFraction(start, end, end.plusDays(1)));
        assertEquals(DateUtil.createWorkDayDurationString(Duration.ofMinutes(ReportUtil.ONE_WORKING_DAY_MINUTES), false, true, false),
                ReportUtil.calculateExtrapolatedScheduleDelayString(start, end, end.plusDays(1)));
        assertEquals(4.0 / 17, ReportUtil.calculateExtrapolatedScheduleDelayFraction(start, end, end.plusDays(6)));
        assertEquals(19.0 / 17, ReportUtil.calculateExtrapolatedScheduleDelayFraction(start, end, LocalDateTime.parse("2026-03-23T08:00")));
        assertNull(ReportUtil.calculateExtrapolatedScheduleDelayFraction(start, end, null));
        assertNull(ReportUtil.calculateExtrapolatedScheduleDelayString(start, end, null));
        assertNull(ReportUtil.calculateExtrapolatedScheduleDelayFraction(end.plusDays(4), end.plusDays(5), end.plusDays(6)));
    }

    @Test
    void forecastDelayUsesTheSameWorkingDayCalculation() {
        LocalDateTime start     = LocalDateTime.parse("2018-03-12T08:00");
        LocalDateTime now       = LocalDateTime.parse("2018-03-16T08:00");
        LocalDateTime end       = LocalDateTime.parse("2018-03-23T08:00");
        Duration      worked    = Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES + 5);
        Duration      estimated = worked.plus(Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES));

        assertEquals(0.0, ReportUtil.calculateExtrapolatedScheduleDelayFraction(start, now, end, worked, estimated));
        assertEquals("0m", ReportUtil.calculateExtrapolatedScheduleDelayString(start, now, end, worked, estimated));
        assertNull(ReportUtil.calculateExtrapolatedScheduleDelayFraction(start, now, end, Duration.ZERO, estimated));
        assertNull(ReportUtil.calculateExtrapolatedScheduleDelayString(start, now, end, Duration.ZERO, estimated));
    }

    @Test
    void sprintStatisticsUseActualOrForecastReleaseDate() {
        Sprint sprint = new Sprint();
        sprint.setName("Berlin");
        sprint.setStart(LocalDateTime.parse("2026-02-02T08:00"));
        sprint.setEnd(LocalDateTime.parse("2026-02-24T10:13:54"));
        sprint.setWorked(Duration.ofHours(74));
        sprint.setRemaining(Duration.ZERO);
        sprint.setOriginalEstimation(Duration.ofHours(74));
        sprint.setReleaseDate(LocalDateTime.parse("2026-02-24T08:00"));

        SprintStatistics completed = new SprintStatistics(sprint, LocalDateTime.parse("2026-03-23T08:00"));
        assertEquals("0m (0%)", completed.getExtrapolatedScheduleDelayDisplay());
        assertEquals("GOOD", completed.getExtrapolatedStatus());
        assertEquals("2026.02.24", completed.getExtrapolatedReleaseDate());

        sprint.setReleaseDate(LocalDateTime.parse("2026-02-25T08:00"));
        SprintStatistics completedLate = new SprintStatistics(sprint, LocalDateTime.parse("2026-03-23T08:00"));
        assertEquals(1.0 / 17, completedLate.getExtrapolatedDelayFraction());
        assertEquals("2026.02.25", completedLate.getExtrapolatedReleaseDate());

        sprint.setRemaining(Duration.ofHours(10));
        sprint.setReleaseDate(LocalDateTime.parse("2026-03-02T08:00"));
        SprintStatistics active = new SprintStatistics(sprint, LocalDateTime.parse("2026-02-20T08:00"));
        assertEquals(4.0 / 17, active.getExtrapolatedDelayFraction());
        assertEquals("CRITICAL", active.getExtrapolatedStatus());

        sprint.setReleaseDate(null);
        SprintStatistics unavailable = new SprintStatistics(sprint, LocalDateTime.parse("2026-02-20T08:00"));
        assertEquals("NA", unavailable.getExtrapolatedScheduleDelayDisplay());
    }

}
