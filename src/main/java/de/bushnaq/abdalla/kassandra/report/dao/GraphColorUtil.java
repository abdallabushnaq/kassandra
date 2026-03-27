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
import de.bushnaq.abdalla.kassandra.report.dao.theme.GraphicsTheme;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;

import java.awt.*;
import java.time.LocalDate;


public class GraphColorUtil {

    public static Color getDayOfMonthBgColor(GraphicsTheme graphicsTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> graphicsTheme.xAxesTheme.XAxesDayOfMonthBgColor;
            case SATURDAY, SUNDAY -> graphicsTheme.xAxesTheme.XAxesDayOfMonthWeekendBgColor;
            default -> null;
        };

    }

    public static Color getDayOfMonthTextColor(GraphicsTheme graphicsTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, TUESDAY, MONDAY, THURSDAY, WEDNESDAY -> graphicsTheme.xAxesTheme.XAxesDayOfMonthTextColor;
            case SATURDAY, SUNDAY -> graphicsTheme.xAxesTheme.XAxesDayOfMonthWeekendTextColor;
            default -> null;
        };

    }

    public static Color getDayOfWeekBgColor(GraphicsTheme graphicsTheme, LocalDate startCal) {
        return switch (startCal.getDayOfWeek()) {
            case FRIDAY, MONDAY, THURSDAY, TUESDAY, WEDNESDAY -> graphicsTheme.xAxesTheme.XAxesDayOfweekBgColor;
            case SATURDAY, SUNDAY -> graphicsTheme.xAxesTheme.XAxesDayOfweekWeekendBgColor;
            default -> null;
        };

    }

    public static ProjectCalendarException getException(GraphicsTheme graphicsTheme, ProjectCalendar pc, LocalDate currentDate) {
        if (!pc.isWorkingDate(currentDate)) {
            return pc.getException(currentDate);
        }
        return null;
    }

    public static Color getGanttDayStripeColor(GraphicsTheme graphicsTheme, ProjectCalendar pc, LocalDate currentDate) {
        if (pc.isWorkingDate(currentDate)) {
            return graphicsTheme.xAxesTheme.XAxesDayOfweekBgColor;
        } else {
            ProjectCalendarException exception = pc.getException(currentDate);
            if (exception != null) {
                if (exception.getName().equals(OffDayType.VACATION.name())) {
                    return graphicsTheme.ganttTheme.ganttVacationBgColor;
                } else if (exception.getName().equals(OffDayType.TRIP.name())) {
                    return graphicsTheme.ganttTheme.ganttTripBgColor;
                } else if (exception.getName().equals(OffDayType.SICK.name())) {
                    return graphicsTheme.ganttTheme.ganttSickBgColor;
                } else {
                    return graphicsTheme.ganttTheme.ganttHolidayBgColor;
                }
            }
            return graphicsTheme.xAxesTheme.XAxesDayOfMonthWeekendBgColor;
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
