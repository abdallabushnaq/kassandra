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

import org.junit.jupiter.api.TestInfo;

/**
 * Utility class for managing test-related metadata through JUnit 5's TestInfo tags.
 * <p>
 * This class provides methods to store and retrieve tag information for JUnit tests,
 * which can be used to name test-related files or output to console to distinguish
 * different test cases. This is especially useful for parameterized tests where
 * multiple test executions with different parameters need to be tracked separately.
 * <p>
 * The utility supports storing metadata such as test case indices, method names,
 * start times, and days after start for time-based testing.
 */
public class TestInfoUtil {

    /**
     * Tag key for storing the number of days after the test start date
     */
    public static final String DAYS_AFTER_START = "daysAfterStart";

    /**
     * Tag key for storing the test case index for parameterized tests
     */
    public static final String TEST_CASE_INDEX = "testCaseIndex";

    /**
     * Tag key for storing the test method name
     */
    public static final String TEST_METHOD_NAME = "testMethodName";

    /**
     * Tag key for storing the test start time/date
     */
    public static final String TEST_START = "testStart";

    /**
     * Retrieves the number of days after start from test tags.
     *
     * @param testInfo The TestInfo object containing test metadata
     * @return The number of days after start, or null if not set
     */
    public static int getDaysAfterStart(TestInfo testInfo) {
        return testInfo.getTags().stream()
                .filter(tag -> tag.startsWith(DAYS_AFTER_START + "="))
                .map(tag -> Integer.parseInt(tag.substring(DAYS_AFTER_START.length() + 1)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves the test case index from test tags.
     * <p>
     * This is particularly useful for parameterized tests to identify
     * which parameter set is being used in a specific test execution.
     *
     * @param testInfo The TestInfo object containing test metadata
     * @return The test case index, or null if not set
     */
    public static Integer getTestCaseIndex(TestInfo testInfo) {
        return testInfo.getTags().stream()
                .filter(tag -> tag.startsWith(TEST_CASE_INDEX + "="))
                .map(tag -> tag.substring(TEST_CASE_INDEX.length() + 1))
                .map(Integer::parseInt)
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves the test method name from test tags.
     * <p>
     * If no tags are present, falls back to the display name of the test.
     *
     * @param testInfo The TestInfo object containing test metadata
     * @return The test method name, or the display name if no tags are present, or null if not found
     */
    public static String getTestMethodName(TestInfo testInfo) {
        if (testInfo.getTags().isEmpty())
            return testInfo.getDisplayName();
        return testInfo.getTags().stream()
                .filter(tag -> tag.startsWith(TEST_METHOD_NAME + "="))
                .map(tag -> tag.substring(TEST_METHOD_NAME.length() + 1))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves the test start time/date from test tags.
     *
     * @param testInfo The TestInfo object containing test metadata
     * @return The test start time/date as a string, or null if not set
     */
    public static String getTestStart(TestInfo testInfo) {
        return testInfo.getTags().stream()
                .filter(tag -> tag.startsWith(TEST_START + "="))
                .map(tag -> tag.substring(TEST_START.length() + 1))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets the days after start tag for a test.
     * <p>
     * This can be used for time-based testing to simulate different points in time.
     *
     * @param testInfo The TestInfo object to update
     * @param days     The number of days after the start date
     */
    public static void setDaysAfterStart(TestInfo testInfo, int days) {
        testInfo.getTags().add(String.format("%s=%d", DAYS_AFTER_START, days));
    }

    /**
     * Sets the test case index tag for a test.
     * <p>
     * This is useful for identifying specific iterations in parameterized tests.
     *
     * @param testInfo      The TestInfo object to update
     * @param testCaseIndex The index of the test case
     */
    public static void setTestCaseIndex(TestInfo testInfo, int testCaseIndex) {
        testInfo.getTags().add(String.format("%s=%s", TEST_CASE_INDEX, testCaseIndex));
    }

    /**
     * Sets the test method name tag for a test.
     * <p>
     * This can be used to provide a custom name for the test method,
     * which is helpful for distinguishing test cases in reports and logs.
     *
     * @param testInfo       The TestInfo object to update
     * @param testMethodName The name to set for the test method
     */
    public static void setTestMethod(TestInfo testInfo, String testMethodName) {
        testInfo.getTags().add(String.format("%s=%s", TEST_METHOD_NAME, testMethodName));
    }

    /**
     * Sets the test start time/date tag for a test.
     * <p>
     * This can be used to track when a test was started or to set a reference
     * point for time-based testing.
     *
     * @param testInfo      The TestInfo object to update
     * @param localDateTime The start time/date as a string
     */
    public static void setTestStart(TestInfo testInfo, String localDateTime) {
        testInfo.getTags().add(String.format("%s=%s", TEST_START, localDateTime));
    }
}
