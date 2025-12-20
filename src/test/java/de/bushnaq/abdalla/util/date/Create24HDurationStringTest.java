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

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class Create24HDurationStringTest {
    static DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    //            ("yyyy-MM-dd HH:mm:ss:AAAA");

    private void test(String expectedResuslt, LocalDateTime start, LocalDateTime end, boolean fixedWidth) {
        Duration delta  = Duration.between(start, end);
        String   result = DateUtil.create24hDurationString(delta, true, true, true, true, fixedWidth);
        System.out.println(result);
        System.out.println(expectedResuslt);
        assertEquals(expectedResuslt, result);
    }

    @Test
    public void test_0() throws IOException {
        LocalDateTime start = LocalDateTime.parse("2019-08-01T08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2019-08-01T08:00", formatter);
        //        Calendar start = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //        Calendar end = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //                    "00w 0d 00h:00m 00s:000ms"
        test("                        ", start, end, true);
    }

    @Test
    public void test_01d() throws IOException {
        LocalDateTime start = LocalDateTime.parse("2019-08-01T08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2019-08-02T08:00", formatter);
        //        Calendar start = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //        Calendar end = new GregorianCalendar(2019, Calendar.AUGUST, 1 + 1, 8, 0);
        //                    "00w 0d 00h:00m 00s:000ms"
        test("    1d                  ", start, end, true);
        test("1d", start, end, false);
    }

    @Test
    public void test_01h() throws IOException {
        LocalDateTime start = LocalDateTime.parse("2019-08-01T08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2019-08-01T09:00", formatter);
        //        Calendar start = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //        Calendar end = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8 + 1, 0);
        //                    "00w 0d 00h:00m 00s:000ms"
        test("        1h              ", start, end, true);
        test("1h", start, end, false);
    }

    @Test
    public void test_01m() throws IOException {
        LocalDateTime start = LocalDateTime.parse("2019-08-01T08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2019-08-01T08:01", formatter);
        //        Calendar start = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //        Calendar end = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 1);
        //                    "00w 0d 00h:00m 00s:000ms"
        test("            1m          ", start, end, true);
        test("1m", start, end, false);
    }

    @Test
    public void test_01ms() throws IOException {
        LocalDateTime start = LocalDateTime.parse("2019-08-01T08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2019-08-01T08:00:00", formatter).plusNanos(1000000);
        //        Calendar start = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //        Calendar end = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //        end.add(Calendar.MILLISECOND, 1);
        //                    "00w 0d 00h:00m 00s:000ms"
        test("                     1ms", start, end, true);
        test("1ms", start, end, false);
    }

    @Test
    public void test_01s() throws IOException {
        LocalDateTime start = LocalDateTime.parse("2019-08-01T08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2019-08-01T08:00:01", formatter);
        //        Calendar start = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //        Calendar end = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //        end.add(Calendar.SECOND, 1);
        //                    "00w 0d 00h:00m 00s:000ms"
        test("                1s      ", start, end, true);
        test("1s", start, end, false);
    }

    @Test
    public void test_01w() throws IOException {
        LocalDateTime start = LocalDateTime.parse("2019-08-01T08:00", formatter);
        LocalDateTime end   = LocalDateTime.parse("2019-08-08T08:00", formatter);
        //        Calendar start = new GregorianCalendar(2019, Calendar.AUGUST, 1, 8, 0);
        //        Calendar end = new GregorianCalendar(2019, Calendar.AUGUST, 1 + 7, 8, 0);
        //                    "00w 0d 00h:00m 00s:000ms"
        test(" 1w                     ", start, end, true);
        test("1w", start, end, false);
    }

}
