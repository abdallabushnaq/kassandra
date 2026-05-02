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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;

public class ReportUtil {
    private static final Logger logger                  = LoggerFactory.getLogger(ReportUtil.class);
    static               long   ONE_WORKING_DAY_MINUTES = 75 * 6;

    public static double calcualteDelayFraction(LocalDateTime startDate, LocalDateTime now, LocalDateTime endDate, Duration worked, Duration estimated) {
        return (double) calcualteManDelay(startDate, now, endDate, worked, estimated).getSeconds() / (double) (estimated.getSeconds());
    }

    /**
     * Hardened
     *
     * @param start
     * @param now
     * @param end
     * @param worked
     * @param remaining
     * @return
     */
    public static Double calcualteEfficiency(LocalDateTime start, LocalDateTime now, LocalDateTime end, Duration worked, Duration remaining) {
        Duration duration = Duration.ofHours(DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), now.toLocalDate()) * ONE_WORKING_DAY_MINUTES);
        if (!duration.isZero() && remaining != null && !worked.isZero()) {
            double efficiency = ((double) worked.getSeconds()) / duration.getSeconds();
            return efficiency;
        }
        return 0d;
    }

    public static double calcualteExpectedProgress(LocalDateTime start, LocalDateTime now, LocalDateTime end) {
        //        logger.trace(String.format("start=%s now=%s end=%s", DateUtil.createDateString(start, DateUtil.sdfymdhm), DateUtil.createDateString(now, DateUtil.sdfymdhm), DateUtil.createDateString(end, DateUtil.sdfymdhm)));
        double   timeProgress   = 0;
        Duration sprintDuration = Duration.ofHours(DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), end.toLocalDate()) * ONE_WORKING_DAY_MINUTES);
        if (!sprintDuration.isZero()) {
            Duration timePast = Duration.ofHours(DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), now.toLocalDate()) * ONE_WORKING_DAY_MINUTES);

            if (now.isAfter(end)) {
                timeProgress = 1.0d;
            } else {
                timeProgress = (double) (timePast.getSeconds()) / (double) sprintDuration.getSeconds();
            }
        }
        return timeProgress;
    }

    /**
     * Hardened
     *
     * @param start
     * @param now
     * @param end
     * @param worked
     * @param originalEstimation
     * @param estimation
     * @param remaining
     * @return
     */
    public static double calcualteExpectedProgress(LocalDateTime start, LocalDateTime now, LocalDateTime end, Duration worked, Duration originalEstimation,
                                                   Duration estimation, Duration remaining) {
        double   timeProgress   = 0;
        Duration sprintDuration = Duration.ofHours(DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), end.toLocalDate()) * ONE_WORKING_DAY_MINUTES);
        if (!sprintDuration.isZero() && remaining != null && !worked.isZero()) {
            Duration timePast = Duration.ofHours(DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), now.toLocalDate()) * ONE_WORKING_DAY_MINUTES);

            if (now.isAfter(end)) {
                timeProgress = 1.0d;
            } else {
                timeProgress = (double) (timePast.getSeconds()) / (double) sprintDuration.getSeconds();
            }
        }
        return timeProgress;
    }

    /**
     * Hardened
     *
     * @param start
     * @param now
     * @param end
     * @param worked
     * @param remaining
     * @return
     */
    public static Duration calcualteManDelay(LocalDateTime start, LocalDateTime now, LocalDateTime end, Duration worked, Duration total) {
        long totalSprintWorkingDays = DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), end.toLocalDate());
        if (totalSprintWorkingDays != 0 && total != null) {
            double timeProgress;
            if (now.isAfter(end)) {
                timeProgress = 1.0d;
            } else {
                long workingDaysPast = DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), now.toLocalDate());
                timeProgress = ((double) (workingDaysPast)) / ((double) totalSprintWorkingDays);
            }
            Duration expectedWork = Duration.ofSeconds((long) (timeProgress * (total.getSeconds())));
            Duration manDaysDelay = expectedWork.minus(worked);
            return manDaysDelay;
        }
        return Duration.ZERO;
    }

    public static String calcualteManDelayString(LocalDateTime start, LocalDateTime now, LocalDateTime end, Duration worked, Duration estimated) {
        Duration duration = Duration.between(start, end);
        if (!duration.isZero() && estimated != null) {
            Duration manDaysDelay = calcualteManDelay(start, now, end, worked, estimated);
            String   delay        = DateUtil.createWorkDayDurationString(manDaysDelay, false, true, false);
            return delay;
        }
        return "";
    }

    /**
     * Hardened
     *
     * @param start
     * @param end
     * @param estimated
     * @return
     */
    public static Double calcualteOptimaleEfficiency(LocalDateTime start, LocalDateTime end, Duration estimated) {
        Duration duration = Duration.ofMinutes(DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), end.toLocalDate()) * ONE_WORKING_DAY_MINUTES);
        if (!duration.isZero() && estimated != null /*&& worked.longValue() != 0*/) {
            double efficiency = ((double) estimated.getSeconds()) / duration.getSeconds();
            return efficiency;
        }
        return 0d;
    }

    /**
     * Hardened
     *
     * @param worked
     * @param estimated
     * @return
     */
    public static double calcualteProgress(Duration worked, Duration estimated) {
        if ((estimated == null) || (worked == null)) {
            return -1f;
        }
        //        if (worked.longValue() > (worked.longValue() + remaining.longValue())) {
        //            return 1.d;
        //        }
        return (double) worked.getSeconds() / (double) (estimated.getSeconds());
    }

    public static Duration calcualteTestCaseDelay(LocalDateTime start, LocalDateTime now, LocalDateTime end, Duration worked, Duration remaining) {
        long totalSprintWorkingDays = DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), end.toLocalDate());
        //        long duration = end - start;
        if (totalSprintWorkingDays != 0 && remaining != null) {
            double timeProgress;
            //            if(now < start)
            //                return  0;
            if (now.isAfter(end)) {
                timeProgress = 1.0d;
                //                long expectedWork = (long) timeProgress * (worked.longValue() + remaining.longValue());
                //                long manDaysDelay = expectedWork - worked.longValue();//in man days
                //                String delay = Util.createDurationString(manDaysDelay, false, true, false);
                //                return delay;
            } else {
                long workingDaysPast = DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), now.toLocalDate());
                timeProgress = ((double) (workingDaysPast)) / ((double) totalSprintWorkingDays);
            }
            Duration expectedWork = Duration.ofSeconds((long) (timeProgress * (worked.getSeconds() + remaining.getSeconds())));
            Duration manDaysDelay = expectedWork.minus(worked);
            return manDaysDelay;
        }
        return Duration.ZERO;
    }

    public static Duration calcualteWorkDaysMiliseconsDelay(LocalDateTime start, LocalDateTime now, LocalDateTime end, Duration worked,
                                                            Duration originalEstimation, Duration estimation, Duration remaining) {

        int      sprintDays     = DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), end.toLocalDate());
        Duration sprintDuration = Duration.ofMinutes(sprintDays * ONE_WORKING_DAY_MINUTES);
        //        logger.info(DateUtil.createWorkDayDurationString(sprintDuration, false, true, false));
        if (!sprintDuration.isZero() && remaining != null && !worked.isZero()) {
            int      daysSpent = DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), now.toLocalDate());
            Duration timePast  = Duration.ofMinutes(daysSpent * ONE_WORKING_DAY_MINUTES);
            double   workforce = ((double) worked.getSeconds()) / timePast.getSeconds();//how much work per working days duration

            double timeProgress;
            if (now.isAfter(end)) {
                timeProgress = 1.0d;
            } else {
                timeProgress = (double) (timePast.getSeconds()) / (double) sprintDuration.getSeconds();
            }
            Duration expectedWork = Duration.ofSeconds((long) (timeProgress * (worked.getSeconds() + remaining.getSeconds())));
            if (expectedWork.compareTo(remaining.plus(worked)) > 0) {
                logger.error("Exception!!!!!");
            }

            //            logger.trace(String.format("past working days=%d Efficency=%.1fPD timeProgress=%02.0f%% expectedwork=%s work=%s", timePast / ONE_WORKING_DAY_MILLIS, workforce, timeProgress * 100, Util.createDurationString(expectedWork, false, true, true), Util.createDurationString(worked, false, true, true)));
            Duration workDayMillisecondsDelay = Duration.ofSeconds((long) ((expectedWork.minus(worked).getSeconds() / workforce)));
            return workDayMillisecondsDelay;
        }
        return null;
    }

    public static Double calculateExtrapolatedScheduleDelayFraction(LocalDateTime start, LocalDateTime now, LocalDateTime end, Duration worked,
                                                                    Duration estimated) {
        Duration duration = Duration.between(start, end);
        if (!duration.isZero() && estimated != null) {
            LocalDateTime releaseDate = calculateReleaseDate(start, now, worked, estimated);
            //            String debug1 = DateUtil.createDateString(releaseDate);
            if (releaseDate == null) {
                return null;
            }
            int scheduledWorkingDays = DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), end.toLocalDate());
            int workingDays          = DateUtil.calculateWorkingDaysIncluding(end.toLocalDate(), releaseDate.toLocalDate());
            return (double) workingDays / (double) scheduledWorkingDays;
        }
        return null;
    }

    public static String calculateExtrapolatedScheduleDelayString(LocalDateTime start, LocalDateTime now, LocalDateTime end, Duration worked, Duration estimated) {
        Duration duration = Duration.between(start, end);
        if (!duration.isZero() && estimated != null) {
            LocalDateTime releaseDate = calculateReleaseDate(start, now, worked, estimated);
            if (releaseDate == null) {
                return null;
            }
            int    workingDays = DateUtil.calculateWorkingDaysIncluding(end.toLocalDate(), releaseDate.toLocalDate());
            String delay       = DateUtil.createWorkDayDurationString(Duration.ofMinutes(workingDays * ONE_WORKING_DAY_MINUTES), false, true, false);
            return delay;
        }
        return null;
    }

    public static Duration calculateExtrapolatedTimeSpent(LocalDateTime start, LocalDateTime now, LocalDateTime end, Duration spent, Duration planned) {

        Duration duration               = Duration.between(start, end);
        Duration past                   = Duration.between(start, now);
        Duration extrapolcatedTimeSpent = Duration.ofSeconds(duration.getSeconds() * spent.getSeconds() / past.getSeconds());
        //        long extrapolcatedTimeSpent = (end - start) * spent / (now - start);
        //        String debug1 = DateUtil.createDurationString(extrapolcatedTimeSpent, false, true, true);
        return extrapolcatedTimeSpent;
    }

    /**
     * Hardened
     *
     * @param start
     * @param now
     * @param worked
     * @param estimated
     * @return
     */
    public static LocalDateTime calculateReleaseDate(LocalDateTime start, LocalDateTime now, Duration worked, Duration estimated) {
        //        logger.trace(String.format("start=%s now=%s end=%s worked=%s remaining=%s", Util.createDateString(start, sdtf), Util.createDateString(now, sdtf), Util.createDateString(end, sdtf), Util.createDurationString(worked, false, true, true), Util.createDurationString(remaining, false, true, true)));
        if ((worked == null) || worked.isZero()) {
            return null;
        }
        LocalDateTime releaseDate = LocalDateTime.parse("1900-01-01T08:00:00");
        if (worked != null && estimated != null && !worked.isZero()) {
            long workingDaysSpent = DateUtil.calculateWorkingDaysIncluding(start.toLocalDate(), now.toLocalDate());
            //You spent workingDaysSpent to get worked done. Now how many days will you need to get remaining done?
            long expectedTotalWorkingDaysToSpend = (long) Math.ceil((((double) estimated.getSeconds()) * (workingDaysSpent)) / (worked.getSeconds()));
            //            logger.trace(String.format("total expected working days=%d", expectedTotalWorkingDaysToSpend));
            releaseDate = DateUtil.addNoneWorkingDays(start, expectedTotalWorkingDaysToSpend);
        } else {
        }
        return releaseDate;

    }

    public static String createPersonDayEfficiencyString(Double efficiency) {
        if (efficiency == null) {
            return "";
        } else {
            return String.format("%d%% Person", Math.round(efficiency.doubleValue() * 100));
        }

    }

    //    public static String createTestCaseEfficiencyString(Double efficiency) {
    //        if (efficiency == null) {
    //            return "";
    //        } else {
    //            return String.format("%d Test Cases", Math.round(efficiency.doubleValue() * Util.ONE_WORKING_DAY_MILLIS));
    //        }
    //
    //    }

}
