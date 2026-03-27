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

package de.bushnaq.abdalla.kassandra.report.dao.theme;

import java.awt.*;

public class GanttTheme {
    public Color ganttCriticalRelationColor;
    public Color ganttCriticalTaskBorderColor;
    public Color ganttGridColor         = new Color(0xe5f2ff, false);
    public Color ganttHolidayBgColor    = new Color(0xffe6e6);
    public Color ganttIdColor;
    public Color ganttIdErrorColor;
    public Color ganttIdTextColor;
    public Color ganttIdTextErrorColor;
    public Color ganttMilestoneColor;
    public Color ganttMilestoneTextColor;
    public Color ganttOutOfOfficeColor;
    public Color ganttRelationColor;
    public Color ganttRequestMilestoneColor;
    public Color ganttSickBgColor       = new Color(0xffe6e6);
    public Color ganttStoryColor;
    public Color ganttStoryTextColor;
    public Color ganttTaskBorderColor;
    public Color ganttTaskTextColor;
    public Color ganttTaskTickLineColor = new Color(183, 216, 240);
    public Color ganttTaskTickTextColor = new Color(0, 0, 0, 127);
    public Color ganttTripBgColor       = new Color(0xffe6e6);
    public Color ganttVacationBgColor   = new Color(0xffe6e6);

    public GanttTheme() {
    }
}