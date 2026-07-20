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
 * Mirrors Java: {@code GanttTheme}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GanttThemeDto {
    public Integer criticalRelationColor;
    public Integer criticalTaskBorderColor;
    public Integer gridColor;
    public Integer holidayBgColor;
    public Integer idBgColor;
    public Integer idTextColor;
    public Integer milestoneBgColor;
    public Integer milestoneTextColor;
    public Integer outOfOfficeColor;
    public Integer relationColor;
    public Integer requestMilestoneColor;
    public Integer sickBgColor;
    public Integer storyColor;
    public Integer storyTextColor;
    public Integer taskBorderColor;
    public Integer taskTextColor;
    public Integer taskTickLineColor;
    public Integer taskTickTextColor;
    /**
     * 0–255; alpha for normal task segments
     */
    public Integer taskTransparency;
    /**
     * 0–255; alpha for non-working-day task segments
     */
    public Integer taskWeekEndTransparency;
    public Integer tripBgColor;
    public Integer vacationBgColor;
}
