// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import type {GanttZoomSettings} from './gantt/dto/GanttZoomSettings.js';
import type {SprintOverviewZoomSettings} from './sprints-overview/dto/SprintOverviewZoomSettings.js';
import type {CalendarAxesZoomSettings} from './CalendarAxesZoomSettings.js';

/** A rendering threshold displayed by the chart zoom meter. */
export interface ZoomThreshold {
    effectiveDayWidth: number;
    description: string;
}

/** Maps Gantt rendering settings to their zoom-meter descriptions. */
export function toGanttZoomThresholds(settings: GanttZoomSettings, calendarAxes: CalendarAxesZoomSettings): ZoomThreshold[] {
    return [
        {effectiveDayWidth: settings.renderTaskProgressBarAtDayWidth, description: 'Task progress bars'},
        {effectiveDayWidth: settings.renderTaskProgressTextAtDayWidth, description: 'Task progress text'},
        {effectiveDayWidth: settings.renderTaskKeyAtDayWidth, description: 'Task keys'},
        {effectiveDayWidth: settings.renderTaskNonWorkingDaysAtDayWidth, description: 'Task non-working-day shading'},
        {effectiveDayWidth: settings.renderTaskBorderAtDayWidth, description: 'Task borders'},
        {effectiveDayWidth: settings.renderMilestoneStartDateAtDayWidth, description: 'Milestone start dates'},
        {effectiveDayWidth: settings.renderMilestoneStartTimeAtDayWidth, description: 'Milestone start times'},
        {effectiveDayWidth: settings.renderMilestoneBorderAtDayWidth, description: 'Milestone borders'},
        {effectiveDayWidth: settings.renderDependenciesAtDayWidth, description: 'Task dependencies'},
        {effectiveDayWidth: calendarAxes.dayOfMonthMinDayWidth, description: 'Day-of-month labels'},
        {effectiveDayWidth: calendarAxes.dayOfWeekMinDayWidth, description: 'Day-of-week labels and grid'},
        {effectiveDayWidth: calendarAxes.monthMinDayWidth, description: 'Month labels'},
        {effectiveDayWidth: calendarAxes.weekMinDayWidth, description: 'Week labels'},
    ];
}

/** Maps sprint overview rendering settings to their zoom-meter descriptions. */
export function toSprintOverviewZoomThresholds(settings: SprintOverviewZoomSettings, calendarAxes: CalendarAxesZoomSettings): ZoomThreshold[] {
    return [
        {effectiveDayWidth: settings.renderSprintStartDateAtDayWidth, description: 'Sprint start dates'},
        {effectiveDayWidth: settings.renderSprintEndDateAtDayWidth, description: 'Sprint end dates'},
        {effectiveDayWidth: settings.renderSprintProgressAtDayWidth, description: 'Sprint progress fills'},
        {effectiveDayWidth: settings.renderSprintStatusAtDayWidth, description: 'Sprint status labels'},
        {effectiveDayWidth: calendarAxes.dayOfMonthMinDayWidth, description: 'Day-of-month labels'},
        {effectiveDayWidth: calendarAxes.dayOfWeekMinDayWidth, description: 'Day-of-week labels and grid'},
        {effectiveDayWidth: calendarAxes.monthMinDayWidth, description: 'Month labels'},
        {effectiveDayWidth: calendarAxes.weekMinDayWidth, description: 'Week labels'},
    ];
}
