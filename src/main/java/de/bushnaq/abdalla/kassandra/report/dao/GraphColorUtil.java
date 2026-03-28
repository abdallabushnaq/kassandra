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
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;

import java.awt.*;
import java.time.LocalDate;


public class GraphColorUtil {

    public static Color getDayOfMonthBgColor(Theme kassandraTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> kassandraTheme.xAxesTheme.dayOfMonthBgColor;
            case SATURDAY, SUNDAY -> kassandraTheme.xAxesTheme.dayOfMonthWeekendBgColor;
            default -> null;
        };

    }

    public static Color getDayOfMonthTextColor(Theme kassandraTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> kassandraTheme.xAxesTheme.dayOfMonthTextColor;
            case SATURDAY, SUNDAY -> kassandraTheme.xAxesTheme.dayOfMonthWeekendTextColor;
            default -> null;
        };

    }

    public static Color getDayOfWeekBgColor(Theme kassandraTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, MONDAY, THURSDAY, TUESDAY, WEDNESDAY -> kassandraTheme.xAxesTheme.dayOfweekBgColor;
            case SATURDAY, SUNDAY -> kassandraTheme.xAxesTheme.dayOfweekWeekendBgColor;
            default -> null;
        };

    }

    public static Color getDayOfWeekTextColor(Theme kassandraTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, MONDAY, THURSDAY, TUESDAY, WEDNESDAY -> kassandraTheme.xAxesTheme.dayOfWeekTextColor;
            case SATURDAY, SUNDAY -> kassandraTheme.xAxesTheme.dayOfWeekWeekendTextColor;
            default -> null;
        };

    }

    public static ProjectCalendarException getException(Theme kassandraTheme, ProjectCalendar pc, LocalDate currentDate) {
        if (!pc.isWorkingDate(currentDate)) {
            return pc.getException(currentDate);
        }
        return null;
    }

    public static Color getGanttDayStripeColor(Theme kassandraTheme, ProjectCalendar pc, LocalDate currentDate) {
        if (pc.isWorkingDate(currentDate)) {
            return kassandraTheme.xAxesTheme.dayOfweekBgColor;
        } else {
            ProjectCalendarException exception = pc.getException(currentDate);
            if (exception != null) {
                if (exception.getName().equals(OffDayType.VACATION.name())) {
                    return kassandraTheme.ganttTheme.vacationBgColor;
                } else if (exception.getName().equals(OffDayType.TRIP.name())) {
                    return kassandraTheme.ganttTheme.tripBgColor;
                } else if (exception.getName().equals(OffDayType.SICK.name())) {
                    return kassandraTheme.ganttTheme.sickBgColor;
                } else {
                    return kassandraTheme.ganttTheme.holidayBgColor;
                }
            }
            return kassandraTheme.xAxesTheme.dayOfMonthWeekendBgColor;
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
