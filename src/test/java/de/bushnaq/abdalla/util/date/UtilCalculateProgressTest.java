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
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class UtilCalculateProgressTest {
    private static final double            EPSILON   = 0.001d;
    static               DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Test
    public void fullProgressTest() {
        Duration worked    = Duration.ofMinutes(10 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        Duration remaining = Duration.ofMinutes(0L);
        //        Long worked = new Long(10 * Util.ONE_WORKING_DAY_MILLIS);
        //        long remaining = 0 * Util.ONE_WORKING_DAY_MILLIS;
        double progress = ReportUtil.calcualteProgress(worked, worked.plus(remaining));
        assertEquals(1.0, progress, EPSILON);
    }

    @Test
    public void halfProgressTest() {
        Duration worked    = Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        Duration remaining = Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        //        Long worked = new Long(5 * Util.ONE_WORKING_DAY_MILLIS);
        //        long remaining = 5 * Util.ONE_WORKING_DAY_MILLIS;
        double progress = ReportUtil.calcualteProgress(worked, worked.plus(remaining));
        assertEquals(0.5, progress, EPSILON);
    }

    @Test
    public void noProgressTest() {
        Duration worked    = Duration.ofMinutes(0L);
        Duration remaining = Duration.ofMinutes(10 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        //        Long worked = new Long(0 * Util.ONE_WORKING_DAY_MILLIS);
        //        long remaining = 10 * Util.ONE_WORKING_DAY_MILLIS;
        double progress = ReportUtil.calcualteProgress(worked, worked.plus(remaining));
        assertEquals(0.0, progress, EPSILON);
    }

    @Test
    public void nullRemainingTest() {
        Duration worked = Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        //        Duration remaining = null;
        //        Long worked = new Long(5 * Util.ONE_WORKING_DAY_MILLIS);
        //        Long remaining = null;
        double progress = ReportUtil.calcualteProgress(worked, null);
        assertEquals(-1, progress, EPSILON);
    }

    @Test
    public void nullWorkedTest() {
        Duration worked    = null;
        Duration remaining = Duration.ofMinutes(5 * ReportUtil.ONE_WORKING_DAY_MINUTES);
        double   progress  = ReportUtil.calcualteProgress(worked, remaining);
        assertEquals(-1, progress, EPSILON);
    }

}
