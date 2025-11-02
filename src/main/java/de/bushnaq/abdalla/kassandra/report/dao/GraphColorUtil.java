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
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;

import java.awt.*;
import java.time.LocalDate;


public class GraphColorUtil {

    public static Color getDayOfMonthBgColor(BurnDownGraphicsTheme graphicsTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> graphicsTheme.XAxesDayOfMonthBgColor;
            case SATURDAY, SUNDAY -> graphicsTheme.XAxesDayOfMonthWeekendBgColor;
            default -> null;
        };

    }

    public static Color getDayOfMonthTextColor(BurnDownGraphicsTheme graphicsTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> graphicsTheme.XAxesDayOfMonthTextColor;
            case SATURDAY, SUNDAY -> graphicsTheme.XAxesDayOfMonthWeekendTextColor;
            default -> null;
        };

    }

    public static Color getDayOfWeekBgColor(BurnDownGraphicsTheme graphicsTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, MONDAY, THURSDAY, TUESDAY, WEDNESDAY -> graphicsTheme.XAxesDayOfweekBgColor;
            case SATURDAY, SUNDAY -> graphicsTheme.XAxesDayOfweekWeekendBgColor;
            default -> null;
        };

    }

    public static ProjectCalendarException getException(BurnDownGraphicsTheme graphicsTheme, ProjectCalendar pc, LocalDate currentDate) {
        if (!pc.isWorkingDate(currentDate)) {
            return pc.getException(currentDate);
        }
        return null;
    }

    public static Color getGanttDayStripeColor(BurnDownGraphicsTheme graphicsTheme, ProjectCalendar pc, LocalDate currentDate) {
        if (pc.isWorkingDate(currentDate)) {
            return graphicsTheme.XAxesDayOfweekBgColor;
        } else {
            ProjectCalendarException exception = pc.getException(currentDate);
            if (exception != null) {
                if (exception.getName().equals(OffDayType.VACATION.name())) {
                    return graphicsTheme.ganttVacationBgColor;
                } else if (exception.getName().equals(OffDayType.TRIP.name())) {
                    return graphicsTheme.ganttTripBgColor;
                } else if (exception.getName().equals(OffDayType.SICK.name())) {
                    return graphicsTheme.ganttSickBgColor;
                } else {
                    return graphicsTheme.ganttHolidayBgColor;
                }
            }
            return graphicsTheme.XAxesDayOfMonthWeekendBgColor;
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
