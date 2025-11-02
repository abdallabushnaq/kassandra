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

package de.bushnaq.abdalla.kassandra.ui.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.ui.HtmlColor;
import de.bushnaq.abdalla.util.date.DateUtil;
import de.bushnaq.abdalla.util.date.ReportUtil;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulates all calculated statistics for a sprint at a given point in time.
 * This class extracts and calculates all the metrics displayed in the Sprint Quality Board.
 */
@Getter
public class SprintStatistics {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    @JsonIgnore
    private final        double            actualProgress;
    private final        String            currentEfficiency;
    private final        String            currentEffortDelay;
    private final        String            currentScheduleDelay;
    private final        double            delayFraction;
    @JsonIgnore
    private final        Duration          effortEstimate;
    private final        String            effortEstimateDisplay;
    @JsonIgnore
    private final        Duration          effortRemaining;
    private final        String            effortRemainingDisplay;
    @JsonIgnore
    private final        Duration          effortSpent;
    private final        String            effortSpentDisplay;
    @JsonIgnore
    private final        double            expectedProgress;
    private final        Double            extrapolatedDelayFraction;
    private final        String            extrapolatedReleaseDate;
    private final        String            extrapolatedScheduleDelay;
    private final        String            extrapolatedStatus;
    private final        boolean           isActualReleaseDate;//- if ture, the sprint has been completed, extrapolatedReleaseDate is actual release date, otherwise extrapolatedReleaseDate is an extrapolated date
    private final        String            optimalEfficiency;
    @JsonIgnore
    private final        Duration          originalEstimation;
    private final        int               remainingWorkDays;
    private final        LocalDateTime     sprintEndDate;
    private final        String            sprintName;
    private final        LocalDateTime     sprintStartDate;
    private final        String            status;
    private final        int               totalWorkDays;

    public SprintStatistics(Sprint sprint, LocalDateTime currentTime) {
        // Basic information
        this.sprintName      = sprint.getName();
        this.sprintStartDate = sprint.getStart();
        this.sprintEndDate   = sprint.getEnd();

        // Effort calculations
        this.effortSpent        = sprint.getWorked();
        this.effortRemaining    = sprint.getRemaining();
        this.effortEstimate     = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        this.originalEstimation = sprint.getOriginalEstimation();

        // Time calculations
        this.totalWorkDays     = DateUtil.calculateWorkingDaysIncluding(sprint.getStart().toLocalDate(), sprint.getEnd().toLocalDate());
        this.remainingWorkDays = DateUtil.calculateWorkingDaysIncluding(currentTime, sprint.getEnd());

        // Progress calculations
        this.expectedProgress = ReportUtil.calcualteExpectedProgress(sprint.getStart(), currentTime, sprint.getEnd(), sprint.getWorked(), sprint.getOriginalEstimation(), effortEstimate, sprint.getRemaining());
        this.actualProgress   = ReportUtil.calcualteProgress(sprint.getWorked(), effortEstimate);

        // Delay calculations
        this.currentEffortDelay   = ReportUtil.calcualteManDelayString(sprint.getStart(), currentTime, sprint.getEnd(), sprint.getWorked(), effortEstimate);
        this.delayFraction        = ReportUtil.calcualteDelayFraction(sprint.getStart(), currentTime, sprint.getEnd(), sprint.getWorked(), effortEstimate);
        this.currentScheduleDelay = DateUtil.createDurationString(ReportUtil.calcualteWorkDaysMiliseconsDelay(sprint.getStart(), currentTime, sprint.getEnd(), sprint.getWorked(), sprint.getOriginalEstimation(), effortEstimate, sprint.getRemaining()), false, true, false);

        this.extrapolatedDelayFraction = ReportUtil.calculateExtrapolatedScheduleDelayFraction(sprint.getStart(), currentTime, sprint.getEnd(), sprint.getWorked(), effortEstimate);
        if (extrapolatedDelayFraction != null)
            this.extrapolatedScheduleDelay = ReportUtil.calculateExtrapolatedScheduleDelayString(sprint.getStart(), currentTime, sprint.getEnd(), sprint.getWorked(), effortEstimate);
        else
            this.extrapolatedScheduleDelay = "NA";
        // Efficiency calculations
        this.optimalEfficiency = ReportUtil.createPersonDayEfficiencyString(ReportUtil.calcualteOptimaleEfficiency(sprint.getStart(), sprint.getEnd(), effortEstimate));
        this.currentEfficiency = ReportUtil.createPersonDayEfficiencyString(ReportUtil.calcualteEfficiency(sprint.getStart(), currentTime, sprint.getEnd(), sprint.getWorked(), sprint.getRemaining()));

        // Status calculations
        this.status             = HtmlColor.calculateStatusColor(delayFraction);
        this.extrapolatedStatus = HtmlColor.calculateStatusColor(extrapolatedDelayFraction);

        // Release date calculation
        this.isActualReleaseDate     = sprint.getRemaining() == null || sprint.getRemaining().equals(Duration.ZERO);
        this.extrapolatedReleaseDate = DateUtil.createDateString(sprint.getReleaseDate(), DATE_FORMATTER);

        // Formatted display strings
        this.effortSpentDisplay     = DateUtil.createDurationString(sprint.getWorked(), false, true, false);
        this.effortEstimateDisplay  = DateUtil.createWorkDayDurationString(effortEstimate, false, true, false);
        this.effortRemainingDisplay = DateUtil.createDurationString(sprint.getRemaining(), false, true, false);
    }

    public String getActualProgressDisplay() {
        return String.format("%.0f%%", 100 * actualProgress);
    }

    public String getCurrentEffortDelayDisplay() {
        return String.format("%s (%.0f%%)", currentEffortDelay, 100 * delayFraction);
    }

    // Formatted display methods for UI
    public String getExpectedProgressDisplay() {
        return String.format("%.0f%%", 100 * expectedProgress);
    }

    public String getExtrapolatedScheduleDelayDisplay() {
        if (extrapolatedDelayFraction != null) {
            return String.format("%s (%.0f%%)", extrapolatedScheduleDelay, 100 * extrapolatedDelayFraction);
        } else {
            return "NA";
        }
    }

    public String getReleaseDateLabel() {
        return isActualReleaseDate ? "Actual Sprint Release Date" : "Extrapolated Sprint Release Date";
    }

    public String getReleaseDateStatus() {
        return isActualReleaseDate ? status : extrapolatedStatus;
    }

}
