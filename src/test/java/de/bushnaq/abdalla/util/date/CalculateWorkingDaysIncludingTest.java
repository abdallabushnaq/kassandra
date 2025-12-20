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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class CalculateWorkingDaysIncludingTest {
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Test
    public void endOnSaturdayTest() {
        LocalDate start = LocalDateTime.parse("2018-01-09 08:00", formatter).toLocalDate();
        LocalDate end   = LocalDateTime.parse("2018-04-02 08:00", formatter).toLocalDate();
        //        Calendar start = new GregorianCalendar(2018, Calendar.JANUARY, 9, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.APRIL, 2, 8, 0);
        int workingDays = DateUtil.calculateWorkingDaysIncluding(start, end);
        assertEquals(60, workingDays);
    }

    @Test
    public void endOnSundayTest() {
        LocalDate start = LocalDateTime.parse("2018-01-09 08:00", formatter).toLocalDate();
        LocalDate end   = LocalDateTime.parse("2018-04-03 08:00", formatter).toLocalDate();
        //        Calendar start = new GregorianCalendar(2018, Calendar.JANUARY, 9, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.APRIL, 3, 8, 0);
        int workingDays = DateUtil.calculateWorkingDaysIncluding(start, end);
        assertEquals(61, workingDays);
    }

    @Test
    public void noWeekendTest() {
        LocalDate start = LocalDateTime.parse("2018-03-12 08:00", formatter).toLocalDate();
        LocalDate end   = LocalDateTime.parse("2018-03-16 08:00", formatter).toLocalDate();
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 12, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.MARCH, 16, 8, 0);
        int workingDays = DateUtil.calculateWorkingDaysIncluding(start, end);
        assertEquals(5, workingDays);
    }

    @Test
    public void oneWeekendTest() {
        LocalDate start = LocalDateTime.parse("2018-03-12 08:00", formatter).toLocalDate();
        LocalDate end   = LocalDateTime.parse("2018-03-23 08:00", formatter).toLocalDate();
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 12, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.MARCH, 23, 8, 0);
        int workingDays = DateUtil.calculateWorkingDaysIncluding(start, end);
        assertEquals(10, workingDays);
    }

    @Test
    public void severalWeekendsTest() {
        LocalDate start = LocalDateTime.parse("2018-08-28 08:00", formatter).toLocalDate();
        LocalDate end   = LocalDateTime.parse("2018-09-13 08:00", formatter).toLocalDate();
        //        Calendar start = new GregorianCalendar(2018, Calendar.AUGUST, 28, 11, 17);
        //        Calendar end = new GregorianCalendar(2018, Calendar.SEPTEMBER, 13, 15, 53);
        int workingDays = DateUtil.calculateWorkingDaysIncluding(start, end);
        assertEquals(13, workingDays);
    }

    @Test
    public void startOnSaturdayTest() {
        LocalDate start = LocalDateTime.parse("2018-03-10 08:00", formatter).toLocalDate();
        LocalDate end   = LocalDateTime.parse("2018-03-23 08:00", formatter).toLocalDate();
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 10, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.MARCH, 23, 8, 0);
        int workingDays = DateUtil.calculateWorkingDaysIncluding(start, end);
        assertEquals(10, workingDays);
    }

    @Test
    public void startOnSundayTest() {
        LocalDate start = LocalDateTime.parse("2018-03-11 08:00", formatter).toLocalDate();
        LocalDate end   = LocalDateTime.parse("2018-03-23 08:00", formatter).toLocalDate();
        //        Calendar start = new GregorianCalendar(2018, Calendar.MARCH, 11, 8, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.MARCH, 23, 8, 0);
        int workingDays = DateUtil.calculateWorkingDaysIncluding(start, end);
        assertEquals(10, workingDays);
    }

    @Test
    public void winterTimeTest() {
        LocalDate start = LocalDateTime.parse("2018-10-28 08:00", formatter).toLocalDate();
        LocalDate end   = LocalDateTime.parse("2018-11-01 08:00", formatter).toLocalDate();
        //        Calendar start = new GregorianCalendar(2018, Calendar.OCTOBER, 28, 11, 17);
        //        Calendar end = new GregorianCalendar(2018, Calendar.NOVEMBER, 1, 15, 53);
        int workingDays = DateUtil.calculateWorkingDaysIncluding(start, end);
        assertEquals(4, workingDays);
    }

    @Test
    public void winterTimeTestAtMidnight() {
        LocalDate start = LocalDateTime.parse("2018-10-28 00:00", formatter).toLocalDate();
        LocalDate end   = LocalDateTime.parse("2018-11-01 00:00", formatter).toLocalDate();
        //        Calendar start = new GregorianCalendar(2018, Calendar.OCTOBER, 28, 0, 0);
        //        Calendar end = new GregorianCalendar(2018, Calendar.NOVEMBER, 1, 0, 0);
        int workingDays = DateUtil.calculateWorkingDaysIncluding(start, end);
        assertEquals(4, workingDays);
    }

}
