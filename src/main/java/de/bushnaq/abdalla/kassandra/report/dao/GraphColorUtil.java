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

package de.bushnaq.abdalla.kassandra.report.dao;

import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.report.dao.theme.KassandraTheme;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;

import java.awt.*;
import java.time.LocalDate;


public class GraphColorUtil {

    public static Color getDayOfMonthBgColor(KassandraTheme kassandraTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> kassandraTheme.xAxesTheme.XAxesDayOfMonthBgColor;
            case SATURDAY, SUNDAY -> kassandraTheme.xAxesTheme.XAxesDayOfMonthWeekendBgColor;
            default -> null;
        };

    }

    public static Color getDayOfMonthTextColor(KassandraTheme kassandraTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> kassandraTheme.xAxesTheme.XAxesDayOfMonthTextColor;
            case SATURDAY, SUNDAY -> kassandraTheme.xAxesTheme.XAxesDayOfMonthWeekendTextColor;
            default -> null;
        };

    }

    public static Color getDayOfWeekBgColor(KassandraTheme kassandraTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, MONDAY, THURSDAY, TUESDAY, WEDNESDAY -> kassandraTheme.xAxesTheme.XAxesDayOfweekBgColor;
            case SATURDAY, SUNDAY -> kassandraTheme.xAxesTheme.XAxesDayOfweekWeekendBgColor;
            default -> null;
        };

    }

    public static ProjectCalendarException getException(KassandraTheme kassandraTheme, ProjectCalendar pc, LocalDate currentDate) {
        if (!pc.isWorkingDate(currentDate)) {
            return pc.getException(currentDate);
        }
        return null;
    }

    public static Color getGanttDayStripeColor(KassandraTheme kassandraTheme, ProjectCalendar pc, LocalDate currentDate) {
        if (pc.isWorkingDate(currentDate)) {
            return kassandraTheme.xAxesTheme.XAxesDayOfweekBgColor;
        } else {
            ProjectCalendarException exception = pc.getException(currentDate);
            if (exception != null) {
                if (exception.getName().equals(OffDayType.VACATION.name())) {
                    return kassandraTheme.ganttTheme.ganttVacationBgColor;
                } else if (exception.getName().equals(OffDayType.TRIP.name())) {
                    return kassandraTheme.ganttTheme.ganttTripBgColor;
                } else if (exception.getName().equals(OffDayType.SICK.name())) {
                    return kassandraTheme.ganttTheme.ganttSickBgColor;
                } else {
                    return kassandraTheme.ganttTheme.ganttHolidayBgColor;
                }
            }
            return kassandraTheme.xAxesTheme.XAxesDayOfMonthWeekendBgColor;
        }
    }

    public static String getOffDayLetter(ProjectCalendarException exception) {
        if (exception != null) {
            if (exception.getName().equals(OffDayType.VACATION.name())) {
                return "V";
            } else if (exception.getName().equals(OffDayType.TRIP.name())) {
                return "T";
            } else if (exception.getName().equals(OffDayType.SICK.name())) {
                return "S";
            } else {
                return "H";
            }
        }
        return null;
    }
}
