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
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * calcualteEfficiency calculation.
 * calcualteWorkDaysMiliseconsDelay calculation
 */
@Tag("UnitTest")
public class ParseDurationStringTest {

    @Test
    public void endOnSaturdayTest() {
        double hoursPerDay  = 7.5;
        double hoursPerWeek = 37.5;
        {
            // Result: Duration of 9.5 hours (7.5 + 2)
            Duration duration = DateUtil.parseWorkDayDurationString("1d 2h", hoursPerDay, hoursPerWeek);
            assertEquals(Duration.ofHours(9).plus(30, ChronoUnit.MINUTES), duration);
        }

        {
            // Result: Duration of 52.5 hours (37.5 + 15)
            Duration duration = DateUtil.parseWorkDayDurationString("1w 2d", hoursPerDay, hoursPerWeek);
            assertEquals(Duration.ofHours(52).plus(30, ChronoUnit.MINUTES), duration);
        }

    }

}
