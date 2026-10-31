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

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@ToString(callSuper = false)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomCase {
    private final int            maxNumberOfFeatures;
    private final int            maxNumberOfProducts;
    private final int            maxNumberOfSprints;
    private final int            maxNumberOfStories;
    private final int            maxNumberOfTasks;
    private final int            maxNumberOfUsers;
    private final int            maxNumberOfUsersPerSprint;
    private final int            maxNumberOfVersions;
    private final int            maxTaskDurationDays;
    private final int            minNumberOfFeatures;
    private final int            minNumberOfProducts;
    private final int            minNumberOfSprints;
    @Builder.Default
    private final int            minNumberOfStories = 1;
    private final int            minNumberOfVersions;
    private final OffsetDateTime now;
    private final long           seed;
    private final LocalDate      startDate;
    private final int            testCaseIndex;

}
