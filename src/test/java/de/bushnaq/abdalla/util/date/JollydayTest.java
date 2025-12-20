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

import de.focus_shift.jollyday.core.Holiday;
import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class JollydayTest {
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    public void endOnSaturdayTest() {
        LocalDate      startDateInclusive = LocalDate.parse("2019-01-01", formatter);
        LocalDate      endDateInclusive   = LocalDate.parse("2019-12-31", formatter);
        HolidayManager holidayManager     = HolidayManager.getInstance(ManagerParameters.create("de"));
        Set<Holiday>   holidays           = holidayManager.getHolidays(startDateInclusive, endDateInclusive, "nw");

        List<Holiday> list = new ArrayList<>(holidays);
        for (Holiday holiday : list.stream().sorted().toList()) {
            System.out.format("%s %s%n", holiday.getDate(), holiday.getDescription());
        }
        assertEquals(holidays.size(), 11);

    }


}
