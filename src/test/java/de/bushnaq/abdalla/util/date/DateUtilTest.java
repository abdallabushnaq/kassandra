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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag("UnitTest")
class DateUtilTest {
    final private SimpleDateFormat _sdf   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final Logger           logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void addDayTest() {
        LocalDateTime t  = LocalDateTime.now();
        LocalDateTime t1 = DateUtil.addDay(t, 1);
        assertEquals(t.getDayOfYear() + 1, t1.getDayOfYear());

    }

    @Test
    public void conversionToDateTest() {
        LocalDateTime t1 = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).withHour(5);
        Date          d1 = DateUtil.localDateTimeToDate(t1);
        LocalDateTime t2 = DateUtil.dateToLocalDateTime(d1);
        Date          d2 = DateUtil.localDateTimeToDate(t2);
        assertEquals(d1, d2);
        assertEquals(t1, t2);
    }

    private String dateToString(final Date aDate, TimeZone timeZone) {
        _sdf.setTimeZone(timeZone);
        return _sdf.format(aDate);
    }

    @Test
    public void jiraDateParsingTest() {
        {
            LocalDateTime dummy = LocalDateTime.parse("1900-01-01T00:00");
        }
        {
            String            dateString = "2022-09-23T12:04:43.987+0200";
            DateTimeFormatter formatter  = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            dateString = dateString.substring(0, dateString.length() - 2) + ":00";
            OffsetDateTime result = OffsetDateTime.parse(dateString, formatter);
        }

        {
            String            dateString = "2024-09-11T16:16:46.684+0200";
            DateTimeFormatter formatter  = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            dateString = dateString.substring(0, dateString.length() - 2) + ":00";
            OffsetDateTime result = OffsetDateTime.parse(dateString, formatter);
        }
    }

    @Test
    public void marchallingTest() {
        LocalDateTime t1       = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).withHour(5);
        String        time     = marshalTime(t1);
        String        timezone = marshalTimezone(t1);
        LocalDateTime t2       = unmarshal(time, timezone);
        assertEquals(t1, t2);

    }

    private String marshalTime(LocalDateTime t) {
        Date date = DateUtil.localDateTimeToDate(t);
        //                Date.from(t.atZone(ZoneId.systemDefault()).toInstant());
        return dateToString(date, TimeZone.getDefault());
    }

    private String marshalTimezone(LocalDateTime t) {
        return "" + TimeZone.getDefault();
    }

    private Date stringToDate(final String aBuffer, TimeZone timeZone) throws ParseException {
        //        _sdf.setTimeZone(timeZone);
        return _sdf.parse(aBuffer);
    }

    @Test
    public void testDeserialization() {

        LocalDate date1 = LocalDate.parse("2019.10.01", DateTimeFormatter.ofPattern("yyyy.MM.dd"));

    }

    @Test
    public void testSerialization() {
        LocalDateTime ldt = LocalDateTime.now();
        Date          out = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        Date          in  = new Date(System.currentTimeMillis());
        System.out.printf("%s %s%n", out.toGMTString(), in.toGMTString());

        LocalDate date = LocalDate.parse("2019.10.01", DateTimeFormatter.ofPattern("yyyy.MM.dd"));

    }

    @Test
    public void toDayPrecisionTest() {
        LocalDateTime t  = LocalDateTime.now();
        LocalDateTime t1 = DateUtil.toDayPrecision(t);
        assertEquals(0, t1.getHour());
        assertEquals(0, t1.getMinute());
        assertEquals(0, t1.getSecond());
        assertEquals(0, t1.getNano());
        assertNotEquals(0, t1.getMonth());
        assertNotEquals(0, t1.getYear());
    }

    private LocalDateTime unmarshal(String time, String timezone) {
        try {
            TimeZone timeZone2 = TimeZone.getTimeZone(timezone);
            Date     date      = stringToDate(time, TimeZone.getTimeZone(timezone));
            return DateUtil.dateToLocalDateTime(date);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
            return LocalDateTime.MIN;
        }
    }
}
