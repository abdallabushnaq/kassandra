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

package de.bushnaq.abdalla.kassandra.rest.dto.overview;

/**
 * Day-width thresholds for rendering details in the sprint overview chart.
 */
public class SprintOverviewZoomSettings {
    /**
     * Minimum day width for rendering the sprint end date.
     */
    public int renderSprintEndDateAtDayWidth   = 8;
    /**
     * Minimum day width for rendering the sprint progress fill.
     */
    public int renderSprintProgressAtDayWidth  = 8;
    /**
     * Minimum day width for rendering the sprint start date.
     */
    public int renderSprintStartDateAtDayWidth = 8;
    /**
     * Minimum day width for rendering the sprint status.
     */
    public int renderSprintStatusAtDayWidth    = 8;
}
