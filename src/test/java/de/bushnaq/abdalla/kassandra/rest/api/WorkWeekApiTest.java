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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.WorkDaySchedule;
import de.bushnaq.abdalla.kassandra.dto.WorkWeek;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;

/**
 * API tests for {@link de.bushnaq.abdalla.kassandra.rest.controller.WorkWeekController}.
 * <p>
 * Covers full CRUD, 404 edge-cases (fake IDs), and all three security tiers:
 * anonymous (401), regular USER (403 for writes), and ADMIN (full access).
 * </p>
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class WorkWeekApiTest extends AbstractUiTestUtil {

    private static final long FAKE_ID = 999999L;

    // -----------------------------------------------------------------------
    // CRUD
    // -----------------------------------------------------------------------

    /**
     * Verifies that an unauthenticated caller is rejected (401) for all five
     * operations: getAll, getById, create, update, and delete.
     */
    @Test
    public void anonymousSecurity() {
        // Obtain an existing work week ID while authenticated, then drop the context.
        final WorkWeek existingWorkWeek;
        {
            setUser("admin-user", "ROLE_ADMIN");
            List<WorkWeek> all = workWeekApi.getAll();
            assertFalse(all.isEmpty());
            existingWorkWeek = all.getFirst();
            SecurityContextHolder.clearContext();
        }

        // Read operations require at least USER role.
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> workWeekApi.getAll());

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> workWeekApi.getById(existingWorkWeek.getId()));

        // Write operations require ADMIN role; anonymous is rejected even earlier.
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> workWeekApi.persist(buildWorkWeek("Anon create", "Should fail")));

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> workWeekApi.update(existingWorkWeek));

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> workWeekApi.deleteById(existingWorkWeek.getId()));
    }

    /**
     * Builds a {@link WorkWeek} DTO with Mon–Thu as working days (08:00–17:00,
     * lunch 12:00–13:00) and Fri–Sun as non-working.
     *
     * @param name        unique name for the work week
     * @param description human-readable description
     * @return a fully populated, unsaved {@link WorkWeek}
     */
    private WorkWeek buildWorkWeek(String name, String description) {
        WorkDaySchedule workingDay = new WorkDaySchedule(
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0));

        WorkWeek ww = new WorkWeek();
        ww.setName(name);
        ww.setDescription(description);
        ww.setMonday(workingDay);
        ww.setTuesday(new WorkDaySchedule(
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0)));
        ww.setWednesday(new WorkDaySchedule(
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0)));
        ww.setThursday(new WorkDaySchedule(
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0)));
        // Friday, Saturday, Sunday default to non-working (new WorkDaySchedule())
        return ww;
    }

    /**
     * Admin creates a new work week and verifies the returned entity has the
     * correct fields and a server-assigned ID.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void create() {
        WorkWeek created = workWeekApi.persist(buildWorkWeek("Custom 4x8", "Four-day 8-hour work week"));

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Custom 4x8", created.getName());
        assertEquals("Four-day 8-hour work week", created.getDescription());
        assertTrue(created.getMonday().isWorkingDay());
        assertTrue(created.getTuesday().isWorkingDay());
        assertTrue(created.getWednesday().isWorkingDay());
        assertTrue(created.getThursday().isWorkingDay());
        assertFalse(created.getSaturday().isWorkingDay());
        assertFalse(created.getSunday().isWorkingDay());
    }

    /**
     * Admin creates a work week, deletes it, and confirms that a subsequent
     * {@code getById} returns 404.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void delete() {
        WorkWeek created = workWeekApi.persist(buildWorkWeek("To Delete", "Will be deleted"));
        Long     id      = created.getId();

        workWeekApi.deleteById(id);

        // Confirm the entry is gone
        assertThrows(ResponseStatusException.class, () -> workWeekApi.getById(id));
    }

    /**
     * Admin tries to delete a work week with a non-existent ID; expects a 404-backed
     * {@link ResponseStatusException}.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeId() {
        assertThrows(ResponseStatusException.class, () -> workWeekApi.deleteById(FAKE_ID));
    }

    /**
     * Admin retrieves all work weeks and verifies the default "Western 5x8"
     * entry seeded by {@link DefaultEntitiesInitializer} is present.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void getAll() {
        List<WorkWeek> all = workWeekApi.getAll();

        assertFalse(all.isEmpty());
        assertTrue(all.stream().anyMatch(ww -> DefaultEntitiesInitializer.WORK_WEEK_5X8.equals(ww.getName())),
                "Default 'Western 5x8' work week must be present");
    }

    /**
     * Admin creates a work week and retrieves it by ID; verifies field equality.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void getById() {
        WorkWeek created = workWeekApi.persist(buildWorkWeek("Get-by-ID test", "Description"));

        WorkWeek fetched = workWeekApi.getById(created.getId());

        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals("Get-by-ID test", fetched.getName());
        assertEquals("Description", fetched.getDescription());
    }

    /**
     * Admin requests a work week using a non-existent ID; expects a 404-backed
     * {@link ResponseStatusException}.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void getByIdNotFound() {
        assertThrows(ResponseStatusException.class, () -> workWeekApi.getById(FAKE_ID));
    }

    // -----------------------------------------------------------------------
    // Security – anonymous
    // -----------------------------------------------------------------------

    /**
     * Admin updates the name and description of a work week, then re-fetches it
     * to confirm the changes were persisted.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void update() {
        WorkWeek created = workWeekApi.persist(buildWorkWeek("Old name", "Old description"));

        created.setName("New name");
        created.setDescription("New description");
        // Mark Friday as a working day in the updated schedule
        created.setFriday(new WorkDaySchedule(
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0)));
        workWeekApi.update(created);

        WorkWeek fetched = workWeekApi.getById(created.getId());
        assertEquals("New name", fetched.getName());
        assertEquals("New description", fetched.getDescription());
        assertTrue(fetched.getFriday().isWorkingDay());
    }

    // -----------------------------------------------------------------------
    // Security – regular USER
    // -----------------------------------------------------------------------

    /**
     * Admin tries to update a work week whose ID does not exist in the database;
     * expects a 404-backed {@link ResponseStatusException}.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeId() {
        WorkWeek workWeek = buildWorkWeek("Fake update", "Should fail");
        workWeek.setId(FAKE_ID);

        assertThrows(ResponseStatusException.class, () -> workWeekApi.update(workWeek));
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Verifies that a user with only the USER role can read work weeks but
     * receives {@link AccessDeniedException} for any mutating operation.
     */
    @Test
    @WithMockUser(username = "user", roles = "USER")
    public void userSecurity() {
        // USER can read all work weeks
        List<WorkWeek> all = workWeekApi.getAll();
        assertFalse(all.isEmpty());

        // USER can read a single work week by ID
        WorkWeek existing = all.getFirst();
        WorkWeek fetched  = workWeekApi.getById(existing.getId());
        assertNotNull(fetched);

        // USER cannot create a new work week
        assertThrows(AccessDeniedException.class,
                () -> workWeekApi.persist(buildWorkWeek("User created", "Should fail")));

        // USER cannot update an existing work week
        String originalName = existing.getName();
        try {
            existing.setName("Hacked");
            workWeekApi.update(existing);
            fail("USER should not be able to update a work week");
        } catch (AccessDeniedException e) {
            // expected – restore name so the object stays consistent
        } finally {
            existing.setName(originalName);
        }

        // USER cannot delete a work week
        assertThrows(AccessDeniedException.class,
                () -> workWeekApi.deleteById(existing.getId()));
    }
}

