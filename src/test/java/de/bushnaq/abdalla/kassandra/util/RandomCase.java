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

package de.bushnaq.abdalla.kassandra.util;

import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@ToString(callSuper = false)
public class RandomCase {
    private final int            maxDurationDays;
    private final int            maxNumberOfFeatures;
    private final int            maxNumberOfProducts;
    private final int            maxNumberOfSprints;
    private final int            maxNumberOfStories;
    private final int            maxNumberOfTasks;
    private final int            maxNumberOfUsers;
    private final int            maxNumberOfVersions;
    private final Duration       maxStartDateShift;
    private final int            minNumberOfFeatures;
    private final int            minNumberOfProducts;
    private final int            minNumberOfSprints;
    private final int            minNumberOfVersions;
    private final LocalDate      minStartDate;
    private final OffsetDateTime now;
    private final long           seed;
    private final int            testCaseIndex;

    public RandomCase(int testCaseIndex, int maxDurationDays, int maxNumberOfStories, int maxNumberOfUsers, int maxNumberOfTasks, int seed) {
        this.testCaseIndex       = testCaseIndex;
        this.minStartDate        = LocalDate.parse("2024-12-15");
        this.maxStartDateShift   = Duration.ofDays(1);
        this.minNumberOfProducts = 1;
        this.maxNumberOfProducts = 1;
        this.minNumberOfVersions = 1;
        this.maxNumberOfVersions = 1;
        this.minNumberOfFeatures = 1;
        this.maxNumberOfFeatures = 1;
        this.minNumberOfSprints  = 1;
        this.maxNumberOfSprints  = 1;
        this.maxDurationDays     = maxDurationDays;
        this.maxNumberOfStories  = maxNumberOfStories;
        this.maxNumberOfUsers    = maxNumberOfUsers;
        this.maxNumberOfTasks    = maxNumberOfTasks;
        this.seed                = seed;
        this.now                 = OffsetDateTime.parse("2025-05-05T08:00:00+01:00");
    }

    public RandomCase(int testCaseIndex, OffsetDateTime now, LocalDate minStartDate, Duration maxStartDateShift, int minNumberOfProducts, int maxNumberOfProducts, int minNumberOfVersions, int maxNumberOfVersions, int minNumberOfFeatures, int maxNumberOfFeatures, int minNumberOfSprints, int maxNumberOfSprints, int maxDurationDays, int maxNumberOfStories, int maxNumberOfUsers, int maxNumberOfTasks, int seed) {
        this.testCaseIndex       = testCaseIndex;
        this.minStartDate        = minStartDate;
        this.maxStartDateShift   = maxStartDateShift;
        this.minNumberOfProducts = minNumberOfProducts;
        this.maxNumberOfProducts = maxNumberOfProducts;
        this.minNumberOfVersions = minNumberOfVersions;
        this.maxNumberOfVersions = maxNumberOfVersions;
        this.minNumberOfFeatures = minNumberOfFeatures;
        this.maxNumberOfFeatures = maxNumberOfFeatures;
        this.minNumberOfSprints  = minNumberOfSprints;
        this.maxNumberOfSprints  = maxNumberOfSprints;
        this.maxDurationDays     = maxDurationDays;
        this.maxNumberOfStories  = maxNumberOfStories;
        this.maxNumberOfUsers    = maxNumberOfUsers;
        this.maxNumberOfTasks    = maxNumberOfTasks;
        this.seed                = seed;
        this.now                 = now;
    }

}
