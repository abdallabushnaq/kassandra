// gantt/dto/task-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {CalendarException} from './calendar-exception.js';

/**
 * DTO representing a task in the Gantt chart.
 */
export interface TaskDto {
    id: number | string;
    key?: string;
    name?: string;
    start: Date | null;
    finish: Date | null;
    rowIndex: number;
    fillColor?: string;
    textColor?: string;
    borderColor?: string;
    progressColor?: string;
    progress?: number;
    milestone?: boolean;
    story?: boolean;
    critical?: boolean;
    manuallyScheduled?: boolean;
    assignedUserName?: string | null;
    assignedUserAvailability?: string | null;
    assignedUserCountry?: string | null;
    assignedUserState?: string | null;
    calendarExceptions?: CalendarException[];
    predecessors?: { predecessorId: number | string; visible?: boolean }[];
}
