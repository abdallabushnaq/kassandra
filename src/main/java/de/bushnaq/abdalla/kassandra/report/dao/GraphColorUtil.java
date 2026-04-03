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
import java.time.DayOfWeek;
import java.time.LocalDate;


public class GraphColorUtil {

    public static Color getDayOfMonthBgColor(Theme theme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> theme.xAxesTheme.dayOfMonthBgColor;
            case SATURDAY, SUNDAY -> theme.xAxesTheme.dayOfMonthWeekendBgColor;
            default -> null;
        };

    }

    public static Color getDayOfMonthTextColor(Theme theme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> theme.xAxesTheme.dayOfMonthTextColor;
            case SATURDAY, SUNDAY -> theme.xAxesTheme.dayOfMonthWeekendTextColor;
            default -> null;
        };

    }

    public static Color getDayOfWeekBgColor(Theme theme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, MONDAY, THURSDAY, TUESDAY, WEDNESDAY -> theme.xAxesTheme.dayOfweekBgColor;
            case SATURDAY -> theme.xAxesTheme.dayOfweekSaturdayBgColor;
            case SUNDAY -> theme.xAxesTheme.dayOfweekSundayBgColor;
            default -> null;
        };

    }

    public static Color getDayOfWeekStripBgColor(Theme theme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, MONDAY, THURSDAY, TUESDAY, WEDNESDAY -> theme.xAxesTheme.dayOfweekBgColor;
            case SATURDAY -> theme.chartTheme.dayOfweekSaturdayBgColor;
            case SUNDAY -> theme.chartTheme.dayOfweekSundayBgColor;
            default -> null;
        };

    }

    public static Color getDayOfWeekTextColor(Theme theme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, MONDAY, THURSDAY, TUESDAY, WEDNESDAY -> theme.xAxesTheme.dayOfWeekTextColor;
            case SATURDAY, SUNDAY -> theme.xAxesTheme.dayOfWeekWeekendTextColor;
            default -> null;
        };

    }

    public static ProjectCalendarException getException(Theme theme, ProjectCalendar pc, LocalDate currentDate) {
        if (!pc.isWorkingDate(currentDate)) {
            return pc.getException(currentDate);
        }
        return null;
    }

    public static Color getGanttDayStripeColor(Theme theme, ProjectCalendar pc, LocalDate currentDate) {
        if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY || pc.isWorkingDate(currentDate)) {
            return getDayOfWeekStripBgColor(theme, currentDate);
        } else {
            ProjectCalendarException exception = pc.getException(currentDate);
            if (exception != null) {
                if (exception.getName().equals(OffDayType.VACATION.name())) {
                    return theme.ganttTheme.vacationBgColor;
                } else if (exception.getName().equals(OffDayType.TRIP.name())) {
                    return theme.ganttTheme.tripBgColor;
                } else if (exception.getName().equals(OffDayType.SICK.name())) {
                    return theme.ganttTheme.sickBgColor;
                } else {
                    return theme.ganttTheme.holidayBgColor;
                }
            }
            return theme.xAxesTheme.dayOfMonthWeekendBgColor;
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
