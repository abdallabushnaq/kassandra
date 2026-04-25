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

package de.bushnaq.abdalla.kassandra.report.burndown;

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.report.dao.CalendarSize;
import de.bushnaq.abdalla.kassandra.report.dao.WorklogRemaining;
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class RenderDao {
    public CalendarSize           calendarSize = CalendarSize.YEARS;
    public int                    chartHeight;
    public int                    chartWidth;//if 0, day width is set to MAX_DAY_WIDTH and cartWidth is calculated from maxDays and dayWidth.
    public Context                context;
    public String                 cssClass;
    public LocalDateTime          end;
    public Duration               estimatedBestWork;
    public Duration               estimatedWorstWork;
    public int                    firstDayX    = 0;//shifts the calendarXAxes
    public LocalDateTime          firstWorklog;
    public Theme                  kassandraTheme;
    public LocalDateTime          lastWorklog;
    public Integer                limit        = null;
    public String                 link;
    public Duration               maxWorked;
    public String                 name;
    public LocalDateTime          now;
    public int                    numberOfLines;
    public int                    postRun;
    public int                    preRun;
    public LocalDateTime          release;
    public Duration               remaining;
    public Sprint                 sprint;
    public List<Sprint>           sprintList;
    //    public String                 sprintName;
    public LocalDateTime          start;
    public List<Worklog>          worklog;
    public List<WorklogRemaining> worklogRemaining;

}
