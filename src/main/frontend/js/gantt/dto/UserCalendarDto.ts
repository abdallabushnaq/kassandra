// gantt/dto/user-calendar-dto.ts
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import {CalendarException} from './CalendarException.js';

/**
 * Calendar data shared by Gantt tasks assigned to one user.
 */
export interface UserCalendarDto {
    id: string;
    exceptions: CalendarException[];
}
