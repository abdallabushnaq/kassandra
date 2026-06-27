// theme/calendar-theme.ts
// Mirrors Java: CalendarTheme
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

export class CalendarTheme {
    readonly fillingDayTextColor: number | null;
    readonly holidayBgColor:      number | null;
    readonly holidayTextColor:    number | null;
    readonly monthNameColor:      number | null;
    readonly normalDayTextColor:  number | null;
    readonly sickBgColor:         number | null;
    readonly sickTextColor:       number | null;
    readonly todayBgColor:        number | null;
    readonly todayTextColor:      number | null;
    readonly tripBgColor:         number | null;
    readonly tripTextColor:       number | null;
    readonly vacationBgColor:     number | null;
    readonly vacationTextColor:   number | null;
    readonly weekDayTextColor:    number | null;
    readonly weekendBgColor:      number | null;
    readonly weekendTextColor:    number | null;
    readonly yearTextColor:       number | null;

    constructor(data: Record<string, number | null> = {}) {
        this.fillingDayTextColor = data['fillingDayTextColor'] ?? null;
        this.holidayBgColor      = data['holidayBgColor']      ?? null;
        this.holidayTextColor    = data['holidayTextColor']    ?? null;
        this.monthNameColor      = data['monthNameColor']      ?? null;
        this.normalDayTextColor  = data['normalDayTextColor']  ?? null;
        this.sickBgColor         = data['sickBgColor']         ?? null;
        this.sickTextColor       = data['sickTextColor']       ?? null;
        this.todayBgColor        = data['todayBgColor']        ?? null;
        this.todayTextColor      = data['todayTextColor']      ?? null;
        this.tripBgColor         = data['tripBgColor']         ?? null;
        this.tripTextColor       = data['tripTextColor']       ?? null;
        this.vacationBgColor     = data['vacationBgColor']     ?? null;
        this.vacationTextColor   = data['vacationTextColor']   ?? null;
        this.weekDayTextColor    = data['weekDayTextColor']    ?? null;
        this.weekendBgColor      = data['weekendBgColor']      ?? null;
        this.weekendTextColor    = data['weekendTextColor']    ?? null;
        this.yearTextColor       = data['yearTextColor']       ?? null;
    }
}

