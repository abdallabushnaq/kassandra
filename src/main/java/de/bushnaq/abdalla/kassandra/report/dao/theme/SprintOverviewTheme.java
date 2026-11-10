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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.report.dao.theme;

import java.awt.*;

public class SprintOverviewTheme {
    /** Background color for sprints that have not started. */
    public Color createdSprintBgColor;
    /** Background color for sprints that are in progress. */
    public Color startedSprintBgColor;
    /** Background color for completed sprints. */
    public Color closedSprintBgColor;
    /** Text color for sprint labels. */
    public Color sprintTextColor;
    /** Text color for sprint start and end dates. */
    public Color sprintDateTextColor;
    /** Fill color for the sprint progress bar. */
    public Color sprintProgressColor;
}
