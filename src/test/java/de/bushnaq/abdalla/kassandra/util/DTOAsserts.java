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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DTOAsserts {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static void assertAvailabilityEquals(Availability expected, Availability actual) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Availability '%d' created date does not match", actual.getId()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Availability '%d' updated date does not match", actual.getId()));
        assertEquals(expected.getId(), actual.getId(), "Availability IDs do not match");
        assertEquals(expected.getAvailability(), actual.getAvailability(), "Availability values do not match");
        assertEquals(expected.getStart(), actual.getStart(), "Availability start dates do not match");
    }

    protected static void assertFeatureEquals(Feature expected, Feature actual) {
        assertFeatureEquals(expected, actual, false);
    }

    protected static void assertFeatureEquals(Feature expected, Feature actual, boolean shallow) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Feature '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Project '%s' updated date does not match", actual.getName()));
        assertEquals(expected.getId(), actual.getId(), "Feature IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Feature names do not match");
        if (!shallow) {
            assertEquals(expected.getSprints().size(), actual.getSprints().size(), String.format("Number of sprints in feature '%s' do not match", actual.getName()));
            for (int i = 0; i < expected.getSprints().size(); i++) {
                assertSprintEquals(expected.getSprints().get(i), actual.getSprints().get(i));
            }
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
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Location '%d' created date does not match", actual.getId()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Location '%d' updated date does not match", actual.getId()));
        assertEquals(expected.getCountry(), actual.getCountry(), "Location countries do not match");
        assertEquals(expected.getId(), actual.getId(), "Location IDs do not match");
        assertEquals(expected.getState(), actual.getState(), "Location states do not match");
        assertEquals(expected.getStart(), actual.getStart());
    }

    private static void assertOffDayEquals(OffDay expected, OffDay actual) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("OffDay '%d' created date does not match", actual.getId()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("OffDay '%d' updated date does not match", actual.getId()));
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getFirstDay(), actual.getFirstDay());
        assertEquals(expected.getLastDay(), actual.getLastDay());
        assertEquals(expected.getType(), actual.getType());
    }

    protected static void assertProductEquals(Product expected, Product actual) {
        assertProductEquals(expected, actual, false);
    }

    protected static void assertProductEquals(Product expected, Product actual, boolean shallow) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Product '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Product '%s' updated date does not match", actual.getName()));
        assertEquals(expected.getId(), actual.getId(), "Product IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Product names do not match");
        if (!shallow) {
            assertEquals(expected.getVersions().size(), actual.getVersions().size(), "Number of versions in product do not match");
            for (int i = 0; i < expected.getVersions().size(); i++) {
                assertVersionEquals(expected.getVersions().get(i), actual.getVersions().get(i));
            }
        }
    }

    protected static void assertRelationEquals(Relation expected, Relation actual) {
        assertEquals(expected.getId(), actual.getId(), "Relation IDs do not match");
        assertEquals(expected.getPredecessorId(), actual.getPredecessorId(), "Relation predecessor IDs do not match");
    }

    protected static void assertSprintEquals(Sprint expected, Sprint actual) {
        assertSprintEquals(expected, actual, false);
    }

    protected static void assertSprintEquals(Sprint expected, Sprint actual, boolean shallow) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Sprint '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Sprint '%s' updated date does not match", actual.getName()));
        assertEquals(expected.getEnd(), actual.getEnd(), "Sprint end dates do not match");
        assertEquals(expected.getId(), actual.getId(), "Sprint IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Sprint names do not match");
        assertEquals(expected.getStart(), actual.getStart(), "Sprint start dates do not match");
        assertEquals(expected.getStatus(), actual.getStatus(), "Sprint status values do not match");

        if (!shallow) {
            assertEquals(expected.getTasks().size(), actual.getTasks().size(), "Number of tasks in sprint do not match");
            for (int i = 0; i < expected.getTasks().size(); i++) {
                assertTaskEquals(expected.getTasks().get(i), actual.getTasks().get(i));
            }
        }
    }

    protected static void assertTaskEquals(Task expected, Task actual) {
        assertEquals(expected.getChildTasks().size(), actual.getChildTasks().size(), "Number of child tasks do not match");
        for (int i = 0; i < expected.getChildTasks().size(); i++) {
            assertTaskEquals(expected.getChildTasks().get(i), actual.getChildTasks().get(i));
        }

        assertEquals(expected.getDuration(), actual.getDuration(), "Task durations do not match");
        assertLocalDateTimeEquals(expected.getFinish(), actual.getFinish(), "Task finish");
        assertEquals(expected.getId(), actual.getId(), "Task IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Task names do not match");
        assertEquals(expected.getParentTask(), actual.getParentTask(), "Task parents do not match");

        assertEquals(expected.getPredecessors().size(), actual.getPredecessors().size(), "Number of task predecessors do not match");
        for (int i = 0; i < expected.getPredecessors().size(); i++) {
            assertRelationEquals(expected.getPredecessors().get(i), actual.getPredecessors().get(i));
        }

        assertEquals(expected.getResourceId(), actual.getResourceId(), "Task resource IDs do not match");
        assertLocalDateTimeEquals(expected.getStart(), actual.getStart(), "Task start");
    }

    protected static void assertUserEquals(User expected, User actual) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("User '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("User '%s' updated date does not match", actual.getName()));

        assertEquals(expected.getAvailabilities().size(), actual.getAvailabilities().size(), "Number of user availabilities do not match");
        for (int i = 0; i < expected.getAvailabilities().size(); i++) {
            assertAvailabilityEquals(expected.getAvailabilities().get(i), actual.getAvailabilities().get(i));
        }

        assertEquals(expected.getEmail(), actual.getEmail(), String.format("User '%s' email do not match", actual.getName()));
        assertEquals(expected.getFirstWorkingDay(), actual.getFirstWorkingDay(), String.format("User '%s' first working days do not match", actual.getName()));
        assertEquals(expected.getId(), actual.getId(), String.format("User '%s' ID dos not match", actual.getName()));
        assertEquals(expected.getLastWorkingDay(), actual.getLastWorkingDay(), String.format("User '%s' last working days do not match", actual.getName()));

        assertEquals(expected.getLocations().size(), actual.getLocations().size(), String.format("Number of user '%s' locations do not match", actual.getName()));
        for (int i = 0; i < expected.getLocations().size(); i++) {
            assertLocationEquals(expected.getLocations().get(i), actual.getLocations().get(i));
        }

        assertEquals(expected.getName(), actual.getName(), String.format("User '%s' name dos not match", actual.getName()));

        assertEquals(expected.getOffDays().size(), actual.getOffDays().size(), String.format("Number of user '%s' off days do not match", actual.getName()));
        for (int i = 0; i < expected.getOffDays().size(); i++) {
            assertOffDayEquals(expected.getOffDays().get(i), actual.getOffDays().get(i));
        }
    }

    protected static void assertVersionEquals(Version expected, Version actual) {
        assertVersionEquals(expected, actual, false);
    }

    protected static void assertVersionEquals(Version expected, Version actual, boolean shallow) {
        assertEquals(expected.getCreated(), actual.getCreated(), String.format("Version '%s' created date does not match", actual.getName()));
//        assertEquals(expected.getUpdated(), actual.getUpdated(), String.format("Version '%s' updated date does not match", actual.getName()));
        assertEquals(expected.getId(), actual.getId(), "Version IDs do not match");
        assertEquals(expected.getName(), actual.getName(), "Version names do not match");
        if (!shallow) {
            assertEquals(expected.getFeatures().size(), actual.getFeatures().size(), "Number of features in version do not match");
            for (int i = 0; i < expected.getFeatures().size(); i++) {
                assertFeatureEquals(expected.getFeatures().get(i), actual.getFeatures().get(i));
            }
        }
    }
}
