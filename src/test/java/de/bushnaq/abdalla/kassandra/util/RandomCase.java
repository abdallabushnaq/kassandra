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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Holds the randomized generation settings for one test case.
 */
@Getter
@ToString(callSuper = false)
public class RandomCase {
    private final OffsetDateTime now;
    @Getter(AccessLevel.NONE)
    private final MinMaxNumber   numberOfFeatures;
    @Getter(AccessLevel.NONE)
    private final MinMaxNumber   numberOfProducts;
    @Getter(AccessLevel.NONE)
    private final MinMaxNumber   numberOfSprints;
    @Getter(AccessLevel.NONE)
    private final MinMaxNumber   numberOfStories;
    @Getter(AccessLevel.NONE)
    private final MinMaxNumber   numberOfTasks;
    @Getter(AccessLevel.NONE)
    private final MinMaxNumber   numberOfUsers;
    @Getter(AccessLevel.NONE)
    private final MinMaxNumber   numberOfUsersPerSprint;
    @Getter(AccessLevel.NONE)
    private final MinMaxNumber   numberOfVersions;
    private final long           seed;
    private final LocalDate      startDate;
    @Getter(AccessLevel.NONE)
    private final MinMaxNumber   taskDurationDays;
    private final int            testCaseIndex;

    private RandomCase(RandomCaseBuilder builder) {
        numberOfFeatures       = builder.numberOfFeatures;
        numberOfProducts       = builder.numberOfProducts;
        numberOfSprints        = builder.numberOfSprints;
        numberOfStories        = builder.numberOfStories;
        numberOfTasks          = builder.numberOfTasks;
        numberOfUsers          = builder.numberOfUsers;
        numberOfUsersPerSprint = builder.numberOfUsersPerSprint;
        numberOfVersions       = builder.numberOfVersions;
        taskDurationDays       = builder.taskDurationPerDay;
        now                    = builder.now;
        seed                   = builder.seed;
        startDate              = builder.startDate;
        testCaseIndex          = builder.testCaseIndex;
    }

    /**
     * Creates a builder for a randomized test case.
     *
     * @return a new builder
     */
    public static RandomCaseBuilder builder() {
        return new RandomCaseBuilder();
    }

    /**
     * Returns the feature-count range.
     *
     * @return the feature-count range
     */
    public MinMaxNumber features() {
        return numberOfFeatures;
    }

    /**
     * Returns the product-count range.
     *
     * @return the product-count range
     */
    public MinMaxNumber products() {
        return numberOfProducts;
    }

    /**
     * Returns the sprint-count range.
     *
     * @return the sprint-count range
     */
    public MinMaxNumber sprints() {
        return numberOfSprints;
    }

    /**
     * Returns the story-count range.
     *
     * @return the story-count range
     */
    public MinMaxNumber stories() {
        return numberOfStories;
    }

    /**
     * Returns the task-duration-per-day range.
     *
     * @return the task-duration-per-day range
     */
    public MinMaxNumber taskDurationDays() {
        return taskDurationDays;
    }

    /**
     * Returns the task-count range.
     *
     * @return the task-count range
     */
    public MinMaxNumber tasks() {
        return numberOfTasks;
    }

    /**
     * Returns the user-count range.
     *
     * @return the user-count range
     */
    public MinMaxNumber users() {
        return numberOfUsers;
    }

    /**
     * Returns the users-per-sprint range.
     *
     * @return the users-per-sprint range
     */
    public MinMaxNumber usersPerSprint() {
        return numberOfUsersPerSprint;
    }

    /**
     * Returns the version-count range.
     *
     * @return the version-count range
     */
    public MinMaxNumber versions() {
        return numberOfVersions;
    }

    /**
     * Builds {@link RandomCase} instances.
     */
    public static class RandomCaseBuilder {
        private OffsetDateTime now;
        private MinMaxNumber   numberOfFeatures       = new MinMaxNumber(0, 0);
        private MinMaxNumber   numberOfProducts       = new MinMaxNumber(0, 0);
        private MinMaxNumber   numberOfSprints        = new MinMaxNumber(0, 0);
        private MinMaxNumber   numberOfStories        = new MinMaxNumber(0, 0);
        private MinMaxNumber   numberOfTasks          = new MinMaxNumber(0, 0);
        private MinMaxNumber   numberOfUsers          = new MinMaxNumber(0, 0);
        private MinMaxNumber   numberOfUsersPerSprint = new MinMaxNumber(0, 0);
        private MinMaxNumber   numberOfVersions       = new MinMaxNumber(0, 0);
        private long           seed;
        private LocalDate      startDate;
        private MinMaxNumber   taskDurationPerDay     = new MinMaxNumber(0, 0);
        private int            testCaseIndex;

        /**
         * Creates a randomized test case using the configured values.
         *
         * @return the new randomized test case
         */
        public RandomCase build() {
            return new RandomCase(this);
        }

        /**
         * Sets the current timestamp.
         *
         * @param now the current timestamp
         * @return this builder
         */
        public RandomCaseBuilder now(OffsetDateTime now) {
            this.now = now;
            return this;
        }

        /**
         * Sets the feature-count range.
         *
         * @param min the minimum feature count
         * @param max the maximum feature count
         * @return this builder
         */
        public RandomCaseBuilder numberOfFeatures(int min, int max) {
            numberOfFeatures = new MinMaxNumber(min, max);
            return this;
        }

        /**
         * Sets the product-count range.
         *
         * @param min the minimum product count
         * @param max the maximum product count
         * @return this builder
         */
        public RandomCaseBuilder numberOfProducts(int min, int max) {
            numberOfProducts = new MinMaxNumber(min, max);
            return this;
        }

        /**
         * Sets the sprint-count range.
         *
         * @param min the minimum sprint count
         * @param max the maximum sprint count
         * @return this builder
         */
        public RandomCaseBuilder numberOfSprints(int min, int max) {
            numberOfSprints = new MinMaxNumber(min, max);
            return this;
        }

        /**
         * Sets the story-count range.
         *
         * @param min the minimum story count
         * @param max the maximum story count
         * @return this builder
         */
        public RandomCaseBuilder numberOfStories(int min, int max) {
            numberOfStories = new MinMaxNumber(min, max);
            return this;
        }

        /**
         * Sets the task-count range.
         *
         * @param min the minimum task count
         * @param max the maximum task count
         * @return this builder
         */
        public RandomCaseBuilder numberOfTasks(int min, int max) {
            numberOfTasks = new MinMaxNumber(min, max);
            return this;
        }

        /**
         * Sets the user-count range.
         *
         * @param min the minimum user count
         * @param max the maximum user count
         * @return this builder
         */
        public RandomCaseBuilder numberOfUsers(int min, int max) {
            numberOfUsers = new MinMaxNumber(min, max);
            return this;
        }

        /**
         * Sets the users-per-sprint range.
         *
         * @param min the minimum users per sprint
         * @param max the maximum users per sprint
         * @return this builder
         */
        public RandomCaseBuilder numberOfUsersPerSprint(int min, int max) {
            numberOfUsersPerSprint = new MinMaxNumber(min, max);
            return this;
        }

        /**
         * Sets the version-count range.
         *
         * @param min the minimum version count
         * @param max the maximum version count
         * @return this builder
         */
        public RandomCaseBuilder numberOfVersions(int min, int max) {
            numberOfVersions = new MinMaxNumber(min, max);
            return this;
        }

        /**
         * Sets the random seed.
         *
         * @param seed the random seed
         * @return this builder
         */
        public RandomCaseBuilder seed(long seed) {
            this.seed = seed;
            return this;
        }

        /**
         * Sets the generated-data start date.
         *
         * @param startDate the start date
         * @return this builder
         */
        public RandomCaseBuilder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * Sets the task-duration-per-day range.
         *
         * @param min the minimum duration in days
         * @param max the maximum duration in days
         * @return this builder
         */
        public RandomCaseBuilder taskDurationPerDay(int min, int max) {
            taskDurationPerDay = new MinMaxNumber(min, max);
            return this;
        }

        /**
         * Sets the test-case index.
         *
         * @param testCaseIndex the test-case index
         * @return this builder
         */
        public RandomCaseBuilder testCaseIndex(int testCaseIndex) {
            this.testCaseIndex = testCaseIndex;
            return this;
        }
    }
}
