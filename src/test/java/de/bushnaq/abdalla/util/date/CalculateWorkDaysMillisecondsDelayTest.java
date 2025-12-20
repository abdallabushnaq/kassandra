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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class CalculateWorkDaysMillisecondsDelayTest {
    static        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Logger            logger    = LoggerFactory.getLogger(this.getClass());

    @Test
    public void ct2Test() {
        //start=2018.Aug.28 11:17 now=2018.Sep.14 08:39 end=2018.Okt.08 18:00 worked=01w 02d 05h 39m remaining=03w 01d 02h 31m

        LocalDateTime start = LocalDateTime.parse("2018-08-28 11:17:00", formatter);
        //        Calendar start = new GregorianCalendar(2018, Calendar.AUGUST, 28, 11, 17);
        LocalDateTime now = LocalDateTime.parse("2018-09-13 08:39:00", formatter);
        //        Calendar now = new GregorianCalendar(2018, Calendar.SEPTEMBER, 13, 8, 39);
        LocalDateTime end = LocalDateTime.parse("2018-10-08 18:00:00", formatter);
        //        Calendar end = new GregorianCalendar(2018, Calendar.OCTOBER, 8, 18, 0);
        Duration worked = Duration.ofMinutes(7 * ReportUtil.ONE_WORKING_DAY_MINUTES).plusHours(5).plusMinutes(39);
        //        Long worked = new Long(7 * Util.ONE_WORKING_DAY_MILLIS + (5 * 60 + 39) * 60 * 1000);
        Duration remaining = Duration.ofMinutes(16 * ReportUtil.ONE_WORKING_DAY_MINUTES).plusHours(2).plusMinutes(31);
        //        Long remaining = new Long(16 * Util.ONE_WORKING_DAY_MILLIS + (2 * 60 + 31) * 60 * 1000);
        logger.info(String.format("start =     %s", start.format(formatter)));
        logger.info(String.format("now =       %s", now.format(formatter)));
        logger.info(String.format("end =       %s", end.format(formatter)));
        logger.info(String.format("worked =    %s", DateUtil.createDurationString(worked, false, true, true)));
        logger.info(String.format("remaining = %s", DateUtil.createDurationString(remaining, false, true, true)));

        Duration calcualteWorkDaysMiliseconsDelay = ReportUtil.calcualteWorkDaysMiliseconsDelay(start, now, end, worked, null, null, remaining);

        logger.info(String.format("Effort Estimate = %s", DateUtil.createWorkDayDurationString(worked.plus(remaining), false, true, false)));

        logger.info(String.format("Current sprint delay = %s", DateUtil.createDurationString(calcualteWorkDaysMiliseconsDelay, false, true, false)));

        assertEquals("4d 03h 46m", DateUtil.createDurationString(calcualteWorkDaysMiliseconsDelay, false, true, false));
    }

}
