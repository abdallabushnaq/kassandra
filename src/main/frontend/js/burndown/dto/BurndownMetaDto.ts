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

import {CalendarSize} from "../../CalendarSize";

/**
 * DTO representing metadata for a burndown chart.
 */
export interface BurndownMetaDto {
    firstDayX: number;
    chartStart: string;
    chartEnd: string;
    copyright: string;
    sprintStart: string;
    sprintEnd: string;
    now?: string | null;
    firstWorklogDate?: string | null;
    lastWorklogDate?: string | null;
    releaseDate?: string | null;
    maxWorkedSeconds: number;
    estimatedBestWorkSeconds: number;
    sprintName?: string | null;
    sprintStatus?: string | null;
    sprintClosed: boolean;
    preRun: number;
    postRun: number;
    totalDays: number;
    theme?: Record<string, unknown>;
    calendarSize: CalendarSize;
}
