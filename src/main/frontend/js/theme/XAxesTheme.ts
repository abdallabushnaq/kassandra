// theme/xaxes-theme.ts
// Mirrors Java: XAxesTheme (including monthBgColors[12] array)
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class XAxesTheme {
    // Day of Month
    readonly dayOfMonthBgColor:          number | null;
    readonly dayOfMonthBorderColor:      number | null;
    readonly dayOfMonthTextColor:        number | null;
    readonly dayOfMonthWeekendBgColor:   number | null;
    readonly dayOfMonthWeekendTextColor: number | null;
    // Day of Week
    readonly dayOfWeekBorderColor:       number | null;
    readonly dayOfWeekTextColor:         number | null;
    readonly dayOfWeekWeekendTextColor:  number | null;
    readonly dayOfweekBgColor:           number | null;
    readonly dayOfweekSaturdayBgColor:   number | null;
    readonly dayOfweekSundayBgColor:     number | null;
    // Events / Milestones
    readonly futureEventColor:           number | null;
    readonly milestoneFlagColor:         number | null;
    readonly milestoneTextColor:         number | null;
    /** Array of 12 colors (Jan–Dec). Mirrors Java XAxesTheme.monthBgColors. */
    readonly monthBgColors:              (number | null)[];
    readonly monthBorderColor:           number | null;
    readonly monthTextColor:             number | null;
    // Now / Past
    readonly nowEventColor:              number | null;
    readonly pastEventColor:             number | null;
    // Week
    readonly weekBgColor:                number | null;
    readonly weekBorderColor:            number | null;
    readonly weekTextColor:              number | null;
    // Year
    readonly yearBgColor:                number | null;
    readonly yearBorderColor:            number | null;
    readonly yearTextColor:              number | null;

    constructor(data: Record<string, unknown> = {}) {
        this.dayOfMonthBgColor          = (data['dayOfMonthBgColor']          as number | null) ?? null;
        this.dayOfMonthBorderColor      = (data['dayOfMonthBorderColor']      as number | null) ?? null;
        this.dayOfMonthTextColor        = (data['dayOfMonthTextColor']        as number | null) ?? null;
        this.dayOfMonthWeekendBgColor   = (data['dayOfMonthWeekendBgColor']   as number | null) ?? null;
        this.dayOfMonthWeekendTextColor = (data['dayOfMonthWeekendTextColor'] as number | null) ?? null;
        this.dayOfWeekBorderColor       = (data['dayOfWeekBorderColor']       as number | null) ?? null;
        this.dayOfWeekTextColor         = (data['dayOfWeekTextColor']         as number | null) ?? null;
        this.dayOfWeekWeekendTextColor  = (data['dayOfWeekWeekendTextColor']  as number | null) ?? null;
        this.dayOfweekBgColor           = (data['dayOfweekBgColor']           as number | null) ?? null;
        this.dayOfweekSaturdayBgColor   = (data['dayOfweekSaturdayBgColor']   as number | null) ?? null;
        this.dayOfweekSundayBgColor     = (data['dayOfweekSundayBgColor']     as number | null) ?? null;
        this.futureEventColor           = (data['futureEventColor']           as number | null) ?? null;
        this.milestoneFlagColor         = (data['milestoneFlagColor']         as number | null) ?? null;
        this.milestoneTextColor         = (data['milestoneTextColor']         as number | null) ?? null;
        this.monthBgColors              = Array.isArray(data['monthBgColors'])
            ? (data['monthBgColors'] as (number | null)[]).slice()
            : new Array<number | null>(12).fill(null);
        this.monthBorderColor           = (data['monthBorderColor']           as number | null) ?? null;
        this.monthTextColor             = (data['monthTextColor']             as number | null) ?? null;
        this.nowEventColor              = (data['nowEventColor']              as number | null) ?? null;
        this.pastEventColor             = (data['pastEventColor']             as number | null) ?? null;
        this.weekBgColor                = (data['weekBgColor']                as number | null) ?? null;
        this.weekBorderColor            = (data['weekBorderColor']            as number | null) ?? null;
        this.weekTextColor              = (data['weekTextColor']              as number | null) ?? null;
        this.yearBgColor                = (data['yearBgColor']                as number | null) ?? null;
        this.yearBorderColor            = (data['yearBorderColor']            as number | null) ?? null;
        this.yearTextColor              = (data['yearTextColor']              as number | null) ?? null;
    }
}

