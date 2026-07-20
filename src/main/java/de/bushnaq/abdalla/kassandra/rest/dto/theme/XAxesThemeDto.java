/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.rest.dto.theme;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mirrors Java: {@code XAxesTheme}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XAxesThemeDto {
    // Day of Month
    public Integer   dayOfMonthBgColor;
    public Integer   dayOfMonthBorderColor;
    public Integer   dayOfMonthTextColor;
    public Integer   dayOfMonthWeekendBgColor;
    public Integer   dayOfMonthWeekendTextColor;
    // Day of Week
    public Integer   dayOfWeekBorderColor;
    public Integer   dayOfWeekTextColor;
    public Integer   dayOfWeekWeekendTextColor;
    public Integer   dayOfweekBgColor;
    public Integer   dayOfweekSaturdayBgColor;
    public Integer   dayOfweekSundayBgColor;
    // Events
    public Integer   futureEventColor;
    public Integer   milestoneFlagColor;
    public Integer   milestoneTextColor;
    // Month
    public Integer[] monthBgColors = new Integer[12];
    public Integer   monthBorderColor;
    public Integer   monthTextColor;
    // Now / Past
    public Integer   nowEventColor;
    public Integer   pastEventColor;
    // Week
    public Integer   weekBgColor;
    public Integer   weekBorderColor;
    public Integer   weekTextColor;
    // Year
    public Integer   yearBgColor;
    public Integer   yearBorderColor;
    public Integer   yearTextColor;
}
