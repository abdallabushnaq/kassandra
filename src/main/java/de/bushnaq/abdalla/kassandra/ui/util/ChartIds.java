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

package de.bushnaq.abdalla.kassandra.ui.util;

/**
 * Java counterparts of the identifiers defined by the TypeScript chart classes.
 */
public final class ChartIds {
    /** Readiness identifier for the burndown portion of the combined chart. */
    public static final String BURNDOWN_CHART = "burndown-chart";
    /** Readiness identifier for the combined Gantt and burndown chart. */
    public static final String GANTT_BURNDOWN_CHART = "gantt-burndown-chart";
    /** Readiness identifier for the Gantt chart. */
    public static final String GANTT_CHART = "gantt-chart";
    /** Readiness identifier for the sprints overview chart. */
    public static final String SPRINTS_OVERVIEW_CHART = "sprints-overview-chart";

    private ChartIds() {
    }
}
