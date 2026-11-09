/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.ui;

import de.bushnaq.abdalla.kassandra.util.RandomCase;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public class RandomCases {
    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                RandomCase.builder()
                        .testCaseIndex(4)
                        .now(OffsetDateTime.parse("2026-03-23T08:00:00+01:00"))
                        .startDate(LocalDate.parse("2026-02-01"))
                        .numberOfProducts(8, 8)
                        .numberOfVersions(1, 2)
                        .numberOfFeatures(1, 2)
                        .numberOfSprints(1, 2)
                        .taskDurationPerDay(1, 5)
                        .numberOfStories(1, 4)
                        .numberOfUsers(1, 8)
                        .numberOfUsersPerSprint(2, 4)
                        .numberOfTasks(2, 4)
                        .seed(7)
                        .build()//official screenshot data
        };
        return Arrays.stream(randomCases).toList();
    }


}
