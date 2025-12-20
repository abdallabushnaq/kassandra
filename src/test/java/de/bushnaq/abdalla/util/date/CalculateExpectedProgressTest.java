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
public class CalculateExpectedProgressTest {
    private static final double            EPSILON   = 0.001d;
    static               DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Test
    public void endOnSaturdayTest() {
        LocalDateTime start = LocalDateTime.parse("2018-01-09 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-02-19 08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2018-04-02 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.JANUARY, 9, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.FEBRUARY, 19, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.APRIL, 2, 8, 0);
        Duration worked    = Duration.ofMinutes(10 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        Duration remaining = Duration.ofMinutes(0L);
        double   progress  = ReportUtil.calcualteExpectedProgress(start, now, end, worked, null, null, remaining);
        assertEquals(0.5d, progress, EPSILON);
    }

    @Test
    public void endOnSundayTest() {
        LocalDateTime start = LocalDateTime.parse("2018-01-09 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-02-19 08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2018-04-02 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.JANUARY, 9, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.FEBRUARY, 19, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.APRIL, 2, 8, 0);
        Duration worked    = Duration.ofMinutes(10 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        Duration remaining = Duration.ofMinutes(0L);
        double   progress  = ReportUtil.calcualteExpectedProgress(start, now, end, worked, null, null, remaining);
        assertEquals(0.5d, progress, EPSILON);
    }

    @Test
    public void noWeekendTest() {
        LocalDateTime start = LocalDateTime.parse("2018-03-12 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-03-14 08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2018-03-16 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 12, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.MARCH, 14, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        Duration worked    = Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        Duration remaining = Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        double   progress  = ReportUtil.calcualteExpectedProgress(start, now, end, worked, null, null, remaining);
        assertEquals(0.6d, progress, EPSILON);
    }

    @Test
    public void startOnSaturdayTest() {
        LocalDateTime start = LocalDateTime.parse("2018-03-10 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-03-16 08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2018-03-23 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 10, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.MARCH, 23, 8, 0);
        Duration worked    = Duration.ofMinutes(4 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        Duration remaining = Duration.ofMinutes(6 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        double   progress  = ReportUtil.calcualteExpectedProgress(start, now, end, worked, null, null, remaining);
        assertEquals(0.5d, progress, EPSILON);
    }

    @Test
    public void startOnSundayTest() {
        LocalDateTime start = LocalDateTime.parse("2018-03-11 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-03-16 08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2018-03-23 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 11, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.MARCH, 23, 8, 0);
        Duration worked    = Duration.ofMinutes(2 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        Duration remaining = Duration.ofMinutes(8 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        double   progress  = ReportUtil.calcualteExpectedProgress(start, now, end, worked, null, null, remaining);
        assertEquals(0.5d, progress, EPSILON);
    }

    @Test
    public void weekendTest() {
        LocalDateTime start = LocalDateTime.parse("2018-03-12 08:00", formatter);
        LocalDateTime now   = LocalDateTime.parse("2018-03-16 08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2018-03-23 08:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 12, 8, 0);
        //        Calendar now = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.MARCH, 23, 8, 0);
        Duration worked    = Duration.ofMinutes(10 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        Duration remaining = Duration.ofMinutes(0L);
        double   progress  = ReportUtil.calcualteExpectedProgress(start, now, end, worked, null, null, remaining);
        assertEquals(0.5d, progress, EPSILON);
    }

}
