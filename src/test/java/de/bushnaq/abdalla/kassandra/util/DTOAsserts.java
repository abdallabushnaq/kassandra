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

import de.bushnaq.abdalla.kassandra.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Compares sprints without comparing their IDs.
 * Supports shallow compare, ignoring any list elements.
 */
public class DTOAsserts {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static void assertAvailabilityEquals(Availability expected, Availability actual) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Availability '%s' created date does not match", actual.getId()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Availability '%d' updated date does not match", actual.getId()));
//        assertEquals(expected.getId(), actual.getId(), "Availability IDs do not match");
        assertEquals(expected.getAvailability(), actual.getAvailability(), "Availability values do not match");
        assertEquals(expected.getStart(), actual.getStart(), "Availability start dates do not match");
    }

    protected static void assertFeatureEquals(Feature expected, Feature actual) {
        assertFeatureEquals(expected, actual, false);
    }

    protected static void assertFeatureEquals(Feature expected, Feature actual, boolean shallow) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Feature '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Project '%s' updated date does not match", actual.getName()));
//        assertEquals(expected.getId(), actual.getId(), "Feature IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Feature names do not match");
        if (!shallow) {
            assertUnorderedListEquals(expected.getSprints(), actual.getSprints(), Comparator.comparing(Sprint::getName), "sprints", DTOAsserts::assertSprintEquals);
        }
    }

    protected static void assertLocalDateTimeEquals(LocalDateTime expected, LocalDateTime actual, String name) {
        if (expected == null && actual == null) {
            return;
        }
        assertEquals(expected, actual, String.format("%s LocalDateTime mismatch.", name));
//        assertTrue(Math.abs(ChronoUnit.MICROS.between(expected, actual)) < 1, () -> String.format("Expected %s but was %s", expected, actual));
    }

    private static void assertLocationEquals(Location expected, Location actual) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Location '%s' created date does not match", actual.getId()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Location '%s' updated date does not match", actual.getId()));
        assertEquals(expected.getCountry(), actual.getCountry(), "Location countries do not match");
//        assertEquals(expected.getId(), actual.getId(), "Location IDs do not match");
        assertEquals(expected.getState(), actual.getState(), "Location states do not match");
        assertEquals(expected.getStart(), actual.getStart());
    }

    private static void assertOffDayEquals(OffDay expected, OffDay actual) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("OffDay '%s' created date does not match", actual.getId()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("OffDay '%s' updated date does not match", actual.getId()));
//        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getFirstDay(), actual.getFirstDay(), "OffDays fist day do not match");
        assertEquals(expected.getLastDay(), actual.getLastDay(), "OffDays last day do not match");
        assertEquals(expected.getType(), actual.getType(), "OffDays type do not match");
    }

    protected static void assertProductEquals(Product expected, Product actual) {
        assertProductEquals(expected, actual, false);
    }

    protected static void assertProductEquals(Product expected, Product actual, boolean shallow) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Product '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Product '%s' updated date does not match", actual.getName()));
//        assertEquals(expected.getId(), actual.getId(), "Product IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Product names do not match");
        if (!shallow) {
            assertUnorderedListEquals(expected.getVersions(), actual.getVersions(), Comparator.comparing(Version::getName), "versions", DTOAsserts::assertVersionEquals);
        }
    }

    protected static void assertRelationEquals(Relation expected, Relation actual) {
//        assertEquals(expected.getId(), actual.getId(), "Relation IDs do not match");
        assertEquals(expected.getPredecessorId(), actual.getPredecessorId(), "Relation predecessor IDs do not match");
    }

    protected static void assertSprintEquals(Sprint expected, Sprint actual) {

        assertSprintEquals(expected, actual, false);
    }

    protected static void assertSprintEquals(Sprint expected, Sprint actual, boolean shallow) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Sprint '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Sprint '%s' updated date does not match", actual.getName()));
        assertEquals(expected.getEnd(), actual.getEnd(), "Sprint end dates do not match");
//        assertEquals(expected.getId(), actual.getId(), "Sprint IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Sprint names do not match");
        assertEquals(expected.getStart(), actual.getStart(), "Sprint start dates do not match");
        assertEquals(expected.getStatus(), actual.getStatus(), "Sprint status values do not match");

        if (!shallow) {
            assertUnorderedListEquals(expected.getTasks(), actual.getTasks(), Comparator.comparing(Task::getName), "tasks", DTOAsserts::assertTaskEquals);
        }
    }

    protected static void assertTaskEquals(Task expected, Task actual) {
        if (expected == null && actual == null)
            return;
        assertUnorderedListEquals(expected.getChildTasks(), actual.getChildTasks(), Comparator.comparing(Task::getName), "child tasks", DTOAsserts::assertTaskEquals);

        assertEquals(expected.getDuration(), actual.getDuration(), "Task durations do not match");
        assertLocalDateTimeEquals(expected.getFinish(), actual.getFinish(), "Task finish");
//        assertEquals(expected.getId(), actual.getId(), "Task IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Task names do not match");
//        assertTaskEquals(expected.getParentTask(), actual.getParentTask());

        //TODO find a way to compare relations.
//        assertUnorderedListEquals(expected.getPredecessors(), actual.getPredecessors(), Comparator.comparing(Relation::getId), "features", DTOAsserts::assertRelationEquals);

        assertUserEquals(expected.getSprint().getUser(expected.getResourceId()), actual.getSprint().getUser(actual.getResourceId()), true);
        assertLocalDateTimeEquals(expected.getStart(), actual.getStart(), "Task start");
    }

    /**
     * Compares two collections independent of order by sorting both by the given comparator and then
     * applying {@code elementAssert} to each pair of corresponding elements.
     *
     * @param <T>           element type
     * @param expected      expected collection
     * @param actual        actual collection
     * @param comparator    comparator used to sort both collections before comparison
     * @param label         human-readable label used in the size-mismatch message
     * @param elementAssert per-element assertion; receives (expected, actual)
     */
    protected static <T> void assertUnorderedListEquals(Collection<T> expected, Collection<T> actual, Comparator<T> comparator, String label, BiConsumer<T, T> elementAssert) {
        assertEquals(expected.size(), actual.size(), String.format("Number of %s do not match", label));
        List<T> sortedExpected = expected.stream().sorted(comparator).toList();
        List<T> sortedActual   = actual.stream().sorted(comparator).toList();
        for (int i = 0; i < sortedExpected.size(); i++) {
            elementAssert.accept(sortedExpected.get(i), sortedActual.get(i));
        }
    }

    protected static void assertUserEquals(User expected, User actual) {
        assertUserEquals(expected, actual, false);
    }

    protected static void assertUserEquals(User expected, User actual, boolean shallow) {
        if (expected == null && actual == null)
            return;
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("User '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("User '%s' updated date does not match", actual.getName()));

        if (!shallow)
            assertUnorderedListEquals(expected.getAvailabilities(), actual.getAvailabilities(),
                    Comparator.comparing(Availability::getStart), "user availabilities",
                    DTOAsserts::assertAvailabilityEquals);

        assertEquals(expected.getEmail(), actual.getEmail(), String.format("User '%s' email do not match", actual.getName()));
        assertEquals(expected.getFirstWorkingDay(), actual.getFirstWorkingDay(), String.format("User '%s' first working days do not match", actual.getName()));
//        assertEquals(expected.getId(), actual.getId(), String.format("User '%s' ID dos not match", actual.getName()));
        assertEquals(expected.getLastWorkingDay(), actual.getLastWorkingDay(), String.format("User '%s' last working days do not match", actual.getName()));

        if (!shallow)
            assertUnorderedListEquals(expected.getLocations(), actual.getLocations(),
                    Comparator.comparing(Location::getStart), "user locations",
                    DTOAsserts::assertLocationEquals);

        assertEquals(expected.getName(), actual.getName(), String.format("User '%s' name dos not match", actual.getName()));

        if (!shallow)
            assertUnorderedListEquals(expected.getOffDays(), actual.getOffDays(),
                    Comparator.comparing(OffDay::getFirstDay), "user off days",
                    DTOAsserts::assertOffDayEquals);

        if (!shallow)
            assertUnorderedListEquals(expected.getUserWorkWeeks(), actual.getUserWorkWeeks(),
                    Comparator.comparing(UserWorkWeek::getStart), "user work-week assignments",
                    DTOAsserts::assertUserWorkWeekEquals);
    }

    private static void assertUserWorkWeekEquals(UserWorkWeek expected, UserWorkWeek actual) {
        assertEquals(expected.getCreated(), actual.getCreated(),
                String.format("UserWorkWeek '%s' created date does not match", actual.getId()));
//        assertEquals(expected.getId(), actual.getId(), "UserWorkWeek IDs do not match");
        assertEquals(expected.getStart(), actual.getStart(), "UserWorkWeek start dates do not match");
        assertNotNull(actual.getWorkWeek(), "UserWorkWeek work-week reference must not be null");
        assertEquals(expected.getWorkWeek().getName(), actual.getWorkWeek().getName(), "UserWorkWeek work-week IDs do not match");
    }

    protected static void assertVersionEquals(Version expected, Version actual) {
        assertVersionEquals(expected, actual, false);
    }

    protected static void assertVersionEquals(Version expected, Version actual, boolean shallow) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Version '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Version '%s' updated date does not match", actual.getName()));
//        assertEquals(expected.getId(), actual.getId(), "Version IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Version names do not match");
        if (!shallow) {
            assertUnorderedListEquals(expected.getFeatures(), actual.getFeatures(), Comparator.comparing(Feature::getName), "features", DTOAsserts::assertFeatureEquals);
        }
    }
}
