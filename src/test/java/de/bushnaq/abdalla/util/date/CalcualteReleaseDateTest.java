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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
