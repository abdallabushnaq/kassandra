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

package de.bushnaq.abdalla.util.date;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

class DateTimeStruct {
    String character;//unit
    String separator;//from previous unit
    int    width;//max digits

    DateTimeStruct(final String aSeperator, final String aCharacter, final int aWidth) {
        separator = aSeperator;
        character = aCharacter;
        width     = aWidth;
    }
}

public class DateUtil {
    private static final int               DAY_INDEX           = 4;
    private static final int               HOUR_INDEX          = 3;
    private static final int               MILLI_SECONDS_INDEX = 0;
    private static final int               MINUTE_INDEX        = 2;
    private static final int               SECONDS_INDEX       = 1;
    private static final int               WEEK_INDEX          = 5;
    public final         DateTimeFormatter dtfymd              = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    public final         DateTimeFormatter dtfymdhm            = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm");
    public final         DateTimeFormatter dtfymdhms           = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm:ss");
    public final         DateTimeFormatter dtfymdhmss          = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm:ss.SSS");

    /**
     * adds durations even if one of them or both are null
     *
     * @param d1
     * @param d2
     * @return
     */
    public static Duration add(Duration d1, Duration d2) {
        if (d1 == null && d2 == null) {
            return null;
        }
        if (d2 == null) {
            return d1;
        }
        if (d1 == null) {
            return d2;
        }
        return d1.plus(d2);
    }

    public static LocalDate addDay(LocalDate start, int days) {
        return start.plusDays(days);

    }

    //    public static String create24hDurationString(long aTime, final boolean aUseSeconds, final boolean aUseCharacters, final boolean aPrintLeadingZeros) {
    //        return create24hDurationString(aTime, aUseSeconds, false, aUseCharacters, aPrintLeadingZeros);
    //    }

    public static LocalDateTime addDay(LocalDateTime start, int days) {
        return start.plusDays(days);

    }

    /**
     * Hardened
     *
     * @param startDate
     * @param workingDays
     * @return
     */
    public static LocalDateTime addNoneWorkingDays(LocalDateTime startDate, long workingDays) {
        if (workingDays > 300) {
            return null;
        } else if (workingDays > 2) {
            //---Do not include startDate
            int workingDaysIndex = 1;
            while (workingDaysIndex < workingDays) {
                if (isWorkDay(startDate)) {
                    workingDaysIndex++;
                } else {
                }
                startDate = startDate.plusDays(1);
            }
            //---ensure the last day is not a weekend day
            while (!isWorkDay(startDate)) {
                startDate = startDate.plusDays(1);
            }
            return startDate;
        } else {
            return startDate;
        }
    }

    @Deprecated()
    //TODO use user calendar"
    public static LocalDateTime calculateDayEndTime(LocalDateTime time) {
        return time.truncatedTo(ChronoUnit.DAYS).withHour(16).withMinute(30);
    }

    @Deprecated()
    //TODO use user calendar"
    public static LocalDateTime calculateDayStartTime(LocalDateTime time) {
        return time.truncatedTo(ChronoUnit.DAYS).withHour(8);
    }

    public static int calculateDays(LocalDate first, LocalDate last) {
        return calculateDays(first.atStartOfDay(), last.atStartOfDay());
    }

    public static int calculateDays(LocalDate first, LocalDateTime last) {
        return calculateDays(first.atStartOfDay(), last);
    }

    public static int calculateDays(LocalDateTime first, LocalDate last) {
        return calculateDays(first, last.atStartOfDay());
    }

    public static int calculateDays(LocalDateTime first, LocalDateTime last) {
        Duration noOfDaysBetween = Duration.between(first, last);
        return (int) noOfDaysBetween.toDays();
    }

    @Deprecated()
    //TODO use user calendar"
    public static LocalDateTime calculateLunchStartTime(LocalDateTime time) {
        return time.truncatedTo(ChronoUnit.DAYS).withHour(12);
    }

    @Deprecated()
    //TODO use user calendar"
    public static LocalDateTime calculateLunchStopTime(LocalDateTime time) {
        return time.truncatedTo(ChronoUnit.DAYS).withHour(13);
    }

    /**
     * Hardened
     *
     * @param start
     * @param end
     * @return
     */
    public static int calculateWorkingDaysIncluding(LocalDate start, LocalDate end) {

        //        logger.trace(String.format("start=%s, end=%s", Util.createDateString(startCal.getTimeInMillis(), sdtf), Util.createDateString(endCal.getTimeInMillis(), sdtf)));
        int workDays = 0;

        //make sure we start with the smaller date
        if (start.isAfter(end)) {
            return 0;
        }
        end = end.plusDays(1);

        do {
            //excluding start date
            if (start.getDayOfWeek() != DayOfWeek.SATURDAY && start.getDayOfWeek() != DayOfWeek.SUNDAY) {
                ++workDays;
            }
            start = start.plusDays(1);
        } while (start.isBefore(end)); //excluding end date

        return workDays;
    }

    public static int calculateWorkingDaysIncluding(LocalDateTime start, LocalDateTime end) {
        return calculateWorkingDaysIncluding(start.toLocalDate(), end.toLocalDate());
    }

    public static String create24hDurationString(Duration aTime, final boolean aUseSeconds, final boolean aUseCharacters, final boolean aPrintLeadingZeros) {
        if (aTime == null) {
            return "NA";
        } else {
            return create24hDurationString(aTime, aUseSeconds, false, aUseCharacters, aPrintLeadingZeros);
        }
    }

    /**
     * If aUseCharacters is true, seconds will be followed with s, hours with h... Result will be xd xh:xm:xs or x x:x:x
     *
     * @param aTime
     * @param aUseSeconds
     * @param aUseCharacters
     * @param aPrintLeadingZeros
     * @return String
     */
    public static String create24hDurationString(Duration aTime, final boolean aUseSeconds, final boolean useMilliSeconds, final boolean aUseCharacters,
                                                 final boolean aPrintLeadingZeros) {
        String prefix;
        if (aTime.isNegative()) {
            prefix = "-";
            aTime  = aTime.abs();
        } else {
            prefix = "";
        }
        String       _result     = "";
        final long[] _timePieces = {0, 0, 0, 0, 0, 0};
        final DateTimeStruct[] _time = {new DateTimeStruct(":", "ms", 3), new DateTimeStruct(" ", "s", 2), new DateTimeStruct(" ", "m", 2),
                new DateTimeStruct(" ", "h", 2), new DateTimeStruct(" ", "d", 2), new DateTimeStruct(" ", "w", 3)};
        _timePieces[WEEK_INDEX]          = aTime.getSeconds() / (86400L * 7L);//assuming 5 day working week
        aTime                            = aTime.minusSeconds(_timePieces[WEEK_INDEX] * (86400L * 7L));//assuming 5 day working week
        _timePieces[DAY_INDEX]           = aTime.getSeconds() / 86400L;//assuming 7.5h day
        aTime                            = aTime.minusSeconds(_timePieces[DAY_INDEX] * 86400L);//assuming 7.5h day
        _timePieces[HOUR_INDEX]          = aTime.getSeconds() / 3600L;
        aTime                            = aTime.minusSeconds(_timePieces[HOUR_INDEX] * 3600L);
        _timePieces[MINUTE_INDEX]        = aTime.getSeconds() / 60L;
        aTime                            = aTime.minusSeconds(_timePieces[MINUTE_INDEX] * 60L);
        _timePieces[SECONDS_INDEX]       = aTime.getSeconds();
        aTime                            = aTime.minusSeconds(_timePieces[SECONDS_INDEX]);
        _timePieces[MILLI_SECONDS_INDEX] = aTime.getNano() / 1000000;

        boolean _weFoundTheFirstNonezeroValue = aPrintLeadingZeros;
        int     _indexEnd                     = 0;
        if (!useMilliSeconds) {
            _indexEnd = 1;
        }
        if (!aUseSeconds) {
            _indexEnd = 2;
        }
        for (int _index = WEEK_INDEX; _index >= _indexEnd; _index--) {
            if ((_timePieces[_index] != 0) || _weFoundTheFirstNonezeroValue) {
                if (aUseCharacters) {
                    if (_timePieces[_index] != 0) {
                        _result += longToString(_timePieces[_index], _weFoundTheFirstNonezeroValue);
                        _result += _time[_index].character;
                    }

                    if (_index != _indexEnd) {
                        _result += _time[_index].separator;
                    } else {
                        // ---Do not add a seperator at the end
                        if (_timePieces[_index] == 0) {
                            _result += "0";
                            _result += _time[_index].character;
                        }

                    }
                } else {
                    _result += longToString(_timePieces[_index], _weFoundTheFirstNonezeroValue);
                    if (_index != _indexEnd) {
                        _result += _time[_index].separator;
                    } else {
                        // ---Do not add a seperator at the end
                    }
                }
                _weFoundTheFirstNonezeroValue = true;
            } else {
                // ---Ignore all leading zero values
            }
        }
        // ---In case the result is empty
        if (_result.length() == 0) {
            if (aUseCharacters) {
                if (aUseSeconds) {
                    _result = "0s";
                } else {
                    _result = "0m";
                }
            } else {
                _result = "0";
            }
        } else {
            // ---The result is not empty
        }
        return prefix + _result;
    }

    public static String create24hDurationString(Duration duration, final boolean aUseSeconds, final boolean useMilliSeconds, final boolean aUseCharacters,
                                                 final boolean aPrintLeadingZeros, boolean fixedSize) {
        String prefix;
        if (duration.isNegative()) {
            prefix   = "-";
            duration = duration.abs();
        } else {
            prefix = "";
        }
        String       _result     = "";
        final long[] _timePieces = {0, 0, 0, 0, 0, 0};
        final DateTimeStruct[] _time = {new DateTimeStruct("", "ms", 3), new DateTimeStruct(":", "s", 2), new DateTimeStruct(" ", "m", 2),
                new DateTimeStruct(":", "h", 2), new DateTimeStruct(" ", "d", 1), new DateTimeStruct(" ", "w", 2)};
        _timePieces[WEEK_INDEX]          = duration.getSeconds() / (86400L * 7L);//assuming 5 day working week
        duration                         = duration.minusSeconds(_timePieces[WEEK_INDEX] * (86400L * 7L));//assuming 5 day working week
        _timePieces[DAY_INDEX]           = duration.getSeconds() / 86400L;//assuming 7.5h day
        duration                         = duration.minusSeconds(_timePieces[DAY_INDEX] * 86400L);//assuming 7.5h day
        _timePieces[HOUR_INDEX]          = duration.getSeconds() / 3600L;
        duration                         = duration.minusSeconds(_timePieces[HOUR_INDEX] * 3600L);
        _timePieces[MINUTE_INDEX]        = duration.getSeconds() / 60L;
        duration                         = duration.minusSeconds(_timePieces[MINUTE_INDEX] * 60L);
        _timePieces[SECONDS_INDEX]       = duration.getSeconds();
        duration                         = duration.minusSeconds(_timePieces[SECONDS_INDEX]);
        _timePieces[MILLI_SECONDS_INDEX] = duration.getNano() / 1000000;

        boolean _weFoundTheFirstNonezeroValue = aPrintLeadingZeros;
        int     _indexEnd                     = 0;
        if (!useMilliSeconds) {
            _indexEnd = 1;
        }
        if (!aUseSeconds) {
            _indexEnd = 2;
        }
        for (int _index = WEEK_INDEX; _index >= _indexEnd; _index--) {
            if ((_timePieces[_index] != 0) || _weFoundTheFirstNonezeroValue) {
                if (aUseCharacters) {
                    if (_timePieces[_index] != 0) {
                        String valueString = longToString(_timePieces[_index], false);
                        if (fixedSize) {
                            for (int i = 0; i < _time[_index].width - valueString.length(); i++) {
                                _result += " ";
                            }
                        }
                        _result += valueString;
                        _result += _time[_index].character;
                        if (_index != _indexEnd) {
                            if (_timePieces[_index - 1] != 0) {
                                _result += _time[_index].separator;
                            } else if (fixedSize) {
                                _result += " ";

                            }
                        } else {
                            // ---Do not add a seperator at the end
                            //                            _result += "_";
                        }
                    } else {
                        if (fixedSize) {
                            for (int i = 0; i < _time[_index].width; i++) {
                                _result += " ";
                            }
                            for (int i = 0; i < _time[_index].character.length(); i++) {
                                _result += " ";
                            }
                            //                        _result += _time[_index].character;
                            if (_index != _indexEnd) {
                                _result += " ";
                            } else {
                                // ---Do not add a seperator at the end
                            }
                        }
                    }

                } else {
                    _result += longToString(_timePieces[_index], _weFoundTheFirstNonezeroValue);
                    if (_index != _indexEnd) {
                        _result += _time[_index].separator;
                    } else {
                        // ---Do not add a seperator at the end
                    }
                }
                _weFoundTheFirstNonezeroValue = true;
            } else {
                // ---Ignore all leading zero values
            }
        }
        // ---In case the result is empty
        if (_result.length() == 0) {
            if (aUseCharacters) {
                if (aUseSeconds) {
                    _result = "0s";
                } else {
                    _result = "0m";
                }
            } else {
                _result = "0";
            }
        } else {
            // ---The result is not ampty
        }
        return prefix + _result;
    }

    public static String create24hDurationString(long aTime, final boolean aUseSeconds, final boolean useMilliSeconds, final boolean aUseCharacters,
                                                 final boolean aPrintLeadingZeros, boolean fixedSize) {
        String prefix;
        if (aTime < 0) {
            prefix = "-";
            aTime  = -aTime;
        } else {
            prefix = "";
        }
        String       _result     = "";
        final long[] _timePieces = {0, 0, 0, 0, 0, 0};
        final DateTimeStruct[] _time = {new DateTimeStruct("", "ms", 3), new DateTimeStruct(":", "s", 2), new DateTimeStruct(" ", "m", 2),
                new DateTimeStruct(":", "h", 2), new DateTimeStruct(" ", "d", 1), new DateTimeStruct(" ", "w", 2)};
        _timePieces[WEEK_INDEX]          = aTime / (86400000L * 7L);//assuming 5 day working week
                                           aTime -= _timePieces[WEEK_INDEX] * (86400000L * 7L);//assuming 5 day working week
        _timePieces[DAY_INDEX]           = aTime / 86400000L;//assuming 7.5h day
                                           aTime -= _timePieces[DAY_INDEX] * 86400000L;//assuming 7.5h day
        _timePieces[HOUR_INDEX]          = aTime / 3600000L;
                                           aTime -= _timePieces[HOUR_INDEX] * 3600000L;
        _timePieces[MINUTE_INDEX]        = aTime / 60000L;
                                           aTime -= _timePieces[MINUTE_INDEX] * 60000L;
        _timePieces[SECONDS_INDEX]       = aTime / 1000L;
                                           aTime -= _timePieces[SECONDS_INDEX] * 1000L;
        _timePieces[MILLI_SECONDS_INDEX] = aTime;

        boolean _weFoundTheFirstNonezeroValue = aPrintLeadingZeros;
        int     _indexEnd                     = 0;
        if (!useMilliSeconds) {
            _indexEnd = 1;
        }
        if (!aUseSeconds) {
            _indexEnd = 2;
        }
        for (int _index = WEEK_INDEX; _index >= _indexEnd; _index--) {
            if ((_timePieces[_index] != 0) || _weFoundTheFirstNonezeroValue) {
                if (aUseCharacters) {
                    if (_timePieces[_index] != 0) {
                        String valueString = longToString(_timePieces[_index], false);
                        if (fixedSize) {
                            for (int i = 0; i < _time[_index].width - valueString.length(); i++) {
                                _result += " ";
                            }
                        }
                        _result += valueString;
                        _result += _time[_index].character;
                        if (_index != _indexEnd) {
                            if (_timePieces[_index - 1] != 0) {
                                _result += _time[_index].separator;
                            } else if (fixedSize) {
                                _result += " ";

                            }
                        } else {
                            // ---Do not add a seperator at the end
                            //                            _result += "_";
                        }
                    } else {
                        if (fixedSize) {
                            for (int i = 0; i < _time[_index].width; i++) {
                                _result += " ";
                            }
                            for (int i = 0; i < _time[_index].character.length(); i++) {
                                _result += " ";
                            }
                            //                        _result += _time[_index].character;
                            if (_index != _indexEnd) {
                                _result += " ";
                            } else {
                                // ---Do not add a seperator at the end
                            }
                        }
                    }

                } else {
                    _result += longToString(_timePieces[_index], _weFoundTheFirstNonezeroValue);
                    if (_index != _indexEnd) {
                        _result += _time[_index].separator;
                    } else {
                        // ---Do not add a seperator at the end
                    }
                }
                _weFoundTheFirstNonezeroValue = true;
            } else {
                // ---Ignore all leading zero values
            }
        }
        // ---In case the result is empty
        if (_result.length() == 0) {
            if (aUseCharacters) {
                if (aUseSeconds) {
                    _result = "0s";
                } else {
                    _result = "0m";
                }
            } else {
                _result = "0";
            }
        } else {
            // ---The result is not ampty
        }
        return prefix + _result;
    }

    public static String createDateString(LocalDate date, DateTimeFormatter sdf) {
        if (date == null || sdf == null) {
            return "NA";
        }
        return date.format(sdf);
    }

    public static String createDateString(LocalDateTime date, DateTimeFormatter sdf) {
        if (date == null || sdf == null) {
            return "NA";
        }
        return date.format(sdf);
    }

    public String createDateString(LocalDateTime sprintStartDate) {
        return createDateString(sprintStartDate, dtfymd);
    }

    public String createDateTimeString(LocalDateTime sprintStartDate) {
        if (sprintStartDate == null) {
            return "";
        }
        return dtfymdhm.format(sprintStartDate);
    }

    public static String createDurationString(Duration Duration, final boolean aUseSeconds, final boolean aUseCharacters, final boolean aPrintLeadingZeros) {
        if (Duration == null) {
            return "NA";
        } else {
            return createWorkDayDurationString(Duration, aUseSeconds, aUseCharacters, aPrintLeadingZeros);
        }
    }

    /**
     * If aUseCharacters is true, seconds will be followed with s, hours with h... Result will be xd xh:xm:xs or x x:x:x
     *
     * @param duration
     * @param aUseSeconds
     * @param aUseCharacters
     * @param aPrintLeadingZeros
     * @return String
     */
    public static String createWorkDayDurationString(Duration duration, final boolean aUseSeconds, final boolean aUseCharacters,
                                                     final boolean aPrintLeadingZeros) {
        return createWorkDayDurationString(duration, aUseSeconds, false, aUseCharacters, aPrintLeadingZeros);
    }

    private static String createWorkDayDurationString(Duration duration, final boolean aUseSeconds, final boolean useMilliSeconds, final boolean aUseCharacters,
                                                      final boolean aPrintLeadingZeros) {

        String prefix;
        if (duration.isNegative()) {
            prefix   = "-";
            duration = duration.abs();
        } else {
            prefix = "";
        }
        String       _result     = "";
        final long[] _timePieces = {0, 0, 0, 0, 0, 0};
        final DateTimeStruct[] _time = {//
                new DateTimeStruct(":", "ms", 3),//
                new DateTimeStruct(" ", "s", 2),//
                new DateTimeStruct(" ", "m", 2),//
                new DateTimeStruct(" ", "h", 2),//
                new DateTimeStruct(" ", "d", 2),//
                new DateTimeStruct(" ", "w", 3)//
        };
        _timePieces[WEEK_INDEX]          = duration.getSeconds() / (27000L * 5L);//assuming 5 day working week
        duration                         = duration.minusSeconds(_timePieces[WEEK_INDEX] * (27000L * 5L));//assuming 5 day working week
        _timePieces[DAY_INDEX]           = duration.getSeconds() / 27000L;//assuming 7.5h day
        duration                         = duration.minusSeconds(_timePieces[DAY_INDEX] * 27000L);//assuming 7.5h day
        _timePieces[HOUR_INDEX]          = duration.getSeconds() / 3600L;
        duration                         = duration.minusSeconds(_timePieces[HOUR_INDEX] * 3600L);
        _timePieces[MINUTE_INDEX]        = duration.getSeconds() / 60L;
        duration                         = duration.minusSeconds(_timePieces[MINUTE_INDEX] * 60L);
        _timePieces[SECONDS_INDEX]       = duration.getSeconds();
        duration                         = duration.minusSeconds(_timePieces[SECONDS_INDEX]);
        _timePieces[MILLI_SECONDS_INDEX] = duration.getNano() / 1000000;

        boolean _weFoundTheFirstNonezeroValue = aPrintLeadingZeros;
        int     _indexEnd                     = 0;
        if (!useMilliSeconds) {
            _indexEnd = 1;
        }
        if (!aUseSeconds) {
            _indexEnd = 2;
        }
        for (int _index = WEEK_INDEX; _index >= _indexEnd; _index--) {
            if ((_timePieces[_index] != 0) || _weFoundTheFirstNonezeroValue) {
                if (aUseCharacters) {
                    if (_timePieces[_index] != 0) {
                        _result += longToString(_timePieces[_index], _weFoundTheFirstNonezeroValue);
                        _result += _time[_index].character;
                    }

                    if (_index != _indexEnd) {
                        _result += _time[_index].separator;
                    } else {
                        // ---Do not add a seperator at the end
                        if (_timePieces[_index] == 0) {
                            _result += "0";
                            _result += _time[_index].character;
                        }

                    }
                } else {
                    _result += longToString(_timePieces[_index], _weFoundTheFirstNonezeroValue);
                    if (_index != _indexEnd) {
                        _result += _time[_index].separator;
                    } else {
                        // ---Do not add a seperator at the end
                    }
                }
                _weFoundTheFirstNonezeroValue = true;
            } else {
                // ---Ignore all leading zero values
            }
        }
        // ---In case the result is empty
        if (_result.length() == 0) {
            if (aUseCharacters) {
                if (aUseSeconds) {
                    _result = "0s";
                } else {
                    _result = "0m";
                }
            } else {
                _result = "0";
            }
        } else {
            // ---The result is not empty
        }
        return prefix + _result;
    }

    /**
     * Creates a simple, clean duration string without trailing zeros.
     * For example: 1d 2h 30m, 1d 2h, 1h, 30m, etc.
     *
     * @param duration     The duration to format
     * @param hoursPerDay  Number of hours in a workday
     * @param hoursPerWeek Number of hours in a workweek
     * @return Formatted duration string
     */
    public static String createWorkDayDurationString(Duration duration, double hoursPerDay, double hoursPerWeek) {
        if (duration == null || duration.isZero()) {
            return "0m";
        }

        // Handle negative durations
        boolean isNegative = duration.isNegative();
        if (isNegative) {
            duration = duration.abs();
        }

        StringBuilder result = new StringBuilder();

        // Extract total seconds
        long totalSeconds = duration.getSeconds();

        // Calculate weeks (if applicable)
        long secondsPerWeek = (long) (hoursPerWeek * 3600);
        long weeks          = 0;
        if (secondsPerWeek > 0) {
            weeks = totalSeconds / secondsPerWeek;
            totalSeconds %= secondsPerWeek;
        }

        // Calculate days
        long secondsPerDay = (long) (hoursPerDay * 3600);
        long days          = 0;
        if (secondsPerDay > 0) {
            days = totalSeconds / secondsPerDay;
            totalSeconds %= secondsPerDay;
        }

        // Calculate hours and minutes
        long hours = totalSeconds / 3600;
        totalSeconds %= 3600;
        long minutes = totalSeconds / 60;

        // Append non-zero components
        if (weeks > 0) {
            result.append(weeks).append("w");
        }

        if (days > 0) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(days).append("d");
        }

        if (hours > 0) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(hours).append("h");
        }

        if (minutes > 0 || (weeks == 0 && days == 0 && hours == 0)) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(minutes).append("m");
        }

        return isNegative ? "-" + result : result.toString();
    }

    /**
     * Creates a work duration string with default values of 8 hours per day and 40 hours per week
     *
     * @param duration The duration to format
     * @return Formatted duration string
     */
    public static String createWorkDayDurationString(Duration duration) {
        return createWorkDayDurationString(duration, 7.5, 37.5);
    }

    public static LocalDate dateToLocalDate(Date date) {
        return LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static LocalDate getWeekMonday(LocalDate day) {
        DayOfWeek dayOfTheWeek = day.getDayOfWeek();
        switch (dayOfTheWeek) {
            case MONDAY:
                return day;
            case TUESDAY:
                return day.minusDays(1);
            case WEDNESDAY:
                return day.minusDays(2);
            case THURSDAY:
                return day.minusDays(3);
            case FRIDAY:
                return day.minusDays(4);
            case SATURDAY:
                return day.minusDays(5);
            case SUNDAY:
                return day.minusDays(6);
            default:
                return day;
        }
    }

    public static LocalDate getWeekSunday(LocalDate day) {
        DayOfWeek dayOfTheWeek = day.getDayOfWeek();
        switch (dayOfTheWeek) {
            case MONDAY:
                return day.plusDays(6);
            case TUESDAY:
                return day.plusDays(5);
            case WEDNESDAY:
                return day.plusDays(4);
            case THURSDAY:
                return day.plusDays(3);
            case FRIDAY:
                return day.plusDays(2);
            case SATURDAY:
                return day.plusDays(1);
            case SUNDAY:
                return day;
            default:
                return day;
        }
    }

    /**
     * Resets to last day of the month
     *
     * @param date
     * @return
     */
    public static LocalDateTime incrementToMonthEnd(LocalDateTime date) {
        return date.plusMonths(1).withDayOfMonth(1).minusDays(1);
    }

    public static boolean isAfterOrEqual(LocalDateTime date, LocalDateTime otherDate) {
        return !date.isBefore(otherDate);
    }

    public static boolean isBeforeOrEqual(LocalDateTime date, LocalDateTime otherDate) {
        return !date.isAfter(otherDate);
    }

    /**
     * This checks to see if date represents a point on the
     * local time-line after or equal the start date-time and before or equal the end date-time.
     *
     * @param start
     * @param date
     * @param end
     * @return true if date is located between start and end date-time on the local time-line, this includes start and end dates.
     */
    public static boolean isBetween(LocalDateTime start, LocalDateTime date, LocalDateTime end) {
        return isAfterOrEqual(date, start) && isBeforeOrEqual(date, end);
    }

    public static boolean isOverlapping(LocalDateTime os, LocalDateTime of, boolean overlapping, LocalDateTime ps, LocalDateTime pf) {
        //o   |      |
        //p |      |
        if ((os.isAfter(ps) || os.isEqual(ps)) && (os.isBefore(pf) || os.isEqual(pf))) {
            overlapping = true;
        }
        //o|  |
        //p |      |
        if ((of.isAfter(ps) || of.isEqual(ps)) && (of.isBefore(pf) || of.isEqual(pf))) {
            overlapping = true;
        }
        //o|        |
        //p |      |
        if ((os.isBefore(ps) || os.isEqual(ps)) && (of.isAfter(pf) || of.isEqual(pf))) {
            overlapping = true;
        }
        //o  |    |
        //p |      |
        if (os.isAfter(ps) && of.isBefore(pf)) {
            overlapping = true;
        }
        return overlapping;
    }

    @Deprecated()
    //TODO "use user calendar"
    public static boolean isWorkDay(LocalDate c) {
        return (c.getDayOfWeek() != DayOfWeek.SATURDAY && c.getDayOfWeek() != DayOfWeek.SUNDAY);
    }

    @Deprecated()
    //TODO "use user calendar"
    public static boolean isWorkDay(LocalDateTime c) {
        return (c.getDayOfWeek() != DayOfWeek.SATURDAY && c.getDayOfWeek() != DayOfWeek.SUNDAY);
    }

    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts a LocalDateTime to OffsetDateTime using the system default timezone.
     */
    public static OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public static Date localDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts a LocalDate to OffsetDateTime at the start of day using the system default timezone.
     */
    public static OffsetDateTime localDateToOffsetDateTime(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static String longToString(final Long aValue, final boolean aCreateLeadingZero) {
        if (aValue != null) {
            if (!aCreateLeadingZero || (aValue > 9)) {
                return Long.toString(aValue);
            } else {
                return "0" + aValue;
            }
        } else {
            return "";
        }
    }

    public static Duration max(Duration maxWork, Duration worstEffortEstimation) {
        if (maxWork.compareTo(worstEffortEstimation) > 0) {
            return maxWork;
        }
        if (maxWork.compareTo(worstEffortEstimation) < 0) {
            return worstEffortEstimation;
        }
        return maxWork;
    }

    public static LocalDate max(LocalDate a, LocalDate b) {
        if (!a.isBefore(b)) {
            return a;
        }
        return b;
    }

    public static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (!a.isBefore(b)) {
            return a;
        }
        return b;
    }

    public static LocalDate min(LocalDate a, LocalDate b) {
        if (!a.isAfter(b)) {
            return a;
        }
        return b;
    }

    public static LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        if (!a.isAfter(b)) {
            return a;
        }
        return b;
    }

    /**
     * Converts an OffsetDateTime to LocalDate using the system default timezone.
     */
    public static LocalDate offsetDateTimeToLocalDate(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Converts an OffsetDateTime to LocalDateTime using the system default timezone.
     */
    public static LocalDateTime offsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static Duration parseWorkDayDurationString(String durationString) throws IllegalArgumentException {
        return parseWorkDayDurationString(durationString, 7.5, 37.5);
    }

    public static Duration parseWorkDayDurationString(String durationString, double hoursPerDay, double hoursPerWeek) throws IllegalArgumentException {
        if (durationString == null || durationString.trim().isEmpty()) {
            return Duration.ZERO;
        }

        double   totalHours = 0;
        String[] parts      = durationString.toLowerCase().split("\\s+");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            try {
                // Validate that the part has the correct format (number followed by unit)
                if (!part.matches("^\\d+(\\.\\d+)?[wdhm]$")) {
                    throw new IllegalArgumentException("Invalid duration format: " + part + ". Expected format: number followed by w, d, h, or m (e.g. 2d 4h 30m)");
                }

                double value = Double.parseDouble(part.replaceAll("[wdhm]", ""));
                char   unit  = part.charAt(part.length() - 1);

                switch (unit) {
                    case 'w':
                        totalHours += value * hoursPerWeek;
                        break;
                    case 'd':
                        totalHours += value * hoursPerDay;
                        break;
                    case 'h':
                        totalHours += value;
                        break;
                    case 'm':
                        totalHours += value / 60.0;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid duration unit: " + unit + ". Valid units are w (weeks), d (days), h (hours), m (minutes)");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number in duration: " + part, e);
            }
        }

        long hours   = (long) totalHours;
        long minutes = Math.round((totalHours - hours) * 60);

        return Duration.ofHours(hours).plusMinutes(minutes);
    }

    public static LocalDateTime toDayPrecision(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return date.truncatedTo(ChronoUnit.DAYS);
    }

    public static LocalDateTime toDayPrecision(OffsetDateTime date) {
        if (date == null) {
            return null;
        }
        return DateUtil.offsetDateTimeToLocalDateTime(date).truncatedTo(ChronoUnit.DAYS);
    }
}
