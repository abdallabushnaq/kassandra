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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.UserGroup;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for UserGroupApi - verifies user group management functionality
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class UserGroupApiTest extends AbstractUiTestUtil {
    private User admin1;
    private User user1;
    private User user2;
    private User user3;

    private void init(RandomCase randomCase, TestInfo testInfo) throws Exception {
        Authentication roleAdmin = setUser("admin-user", "ROLE_ADMIN");
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);// creates "Team" group in addition to the "All" default group
        admin1 = userApi.getByEmail("christopher.paul@kassandra.org").get();
        user1  = userApi.getByEmail("kristen.hubbell@kassandra.org").get();
        user2  = userApi.getByEmail("claudine.fick@kassandra.org").get();
        user3  = userApi.getByEmail("randy.asmus@kassandra.org").get();

        setUser(roleAdmin);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testAdminCanAddMemberToGroup(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Create group with all users
        UserGroup group = userGroupApi.getAll().getFirst();
        assertEquals(4, group.getMemberCount());

    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testAdminCanCreateGroup(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Admin creates users

        // Admin creates a group
        Set<Long> memberIds = Set.of(user1.getId(), user2.getId());
        UserGroup group     = userGroupApi.create("Developers", "Development team", memberIds);

        assertNotNull(group);
        assertNotNull(group.getId());
        assertEquals("Developers", group.getName());
        assertEquals("Development team", group.getDescription());
        assertEquals(2, group.getMemberCount());
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testAdminCanDeleteGroup(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        UserGroup group = userGroupApi.create("To Delete", "Will be deleted", Set.of(user1.getId()));
        assertNotNull(group.getId());

        assertEquals(1 + 2, userGroupApi.getAll().size());
        // Delete the group
        userGroupApi.deleteById(group.getId());

        // Verify it's gone
        assertEquals(1 + 1, userGroupApi.getAll().size());//  default "All" group
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testAdminCanGetAllGroups(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Create multiple groups
        userGroupApi.create("Group 1", "First group", Set.of(user1.getId()));
        userGroupApi.create("Group 2", "Second group", new HashSet<>());
        userGroupApi.create("Group 3", "Third group", Set.of(user1.getId()));
        printTables();
        List<UserGroup> groups = userGroupApi.getAll();
        assertEquals(1 + 4, groups.size());//default Team group + 3 created
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testAdminCanRemoveMemberFromGroup(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Create group with both users
        UserGroup group = userGroupApi.getAll().getFirst();
        assertEquals(4, group.getMemberCount());

        // Remove user1
        userGroupApi.removeMember(group.getId(), user1.getId());

        // Verify user1 was removed
        UserGroup updated = userGroupApi.getById(group.getId());
        assertEquals(3, updated.getMemberCount());
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testAdminCanUpdateGroup(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Create group with user1
        UserGroup group = userGroupApi.create("Original", "Original desc", Set.of(user1.getId()));

        // Update group
        UserGroup updated = userGroupApi.update(
                group.getId(),
                "Updated Name",
                "Updated description",
                Set.of(user2.getId(), user3.getId())
        );

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(2, updated.getMemberCount());
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testAnonymousCannotCreateGroup(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Anonymous user tries to create a group - should fail
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            userGroupApi.create("Anonymous Group", "No auth", new HashSet<>());
        });
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCannotCreateGroupWithDuplicateName(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Create first group
        userGroupApi.create("Developers", "Dev team", Set.of(user1.getId()));

        // Try to create second group with same name - should fail
        try {
            userGroupApi.create("Developers", "Another dev team", new HashSet<>());
            fail("Should not be able to create group with duplicate name");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("already exists") || e.getMessage().contains("Developers"));
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateEmptyGroup(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Create group with no members
        UserGroup group = userGroupApi.create("Empty Group", "No members yet", new HashSet<>());

        assertNotNull(group);
        assertEquals("Empty Group", group.getName());
        assertEquals(0, group.getMemberCount());
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "user", roles = "USER")
    public void testRegularUserCannotCreateGroup(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Regular user tries to create a group - should fail
        assertThrows(AccessDeniedException.class, () -> {
            userGroupApi.create("Hackers", "Trying to create group", new HashSet<>());
        });
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "user", roles = "USER")
    public void testRegularUserCannotGetAllGroups(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        assertThrows(AccessDeniedException.class, () -> {
            userGroupApi.getAll();
        });
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testUpdateGroupName(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        UserGroup group = userGroupApi.create("Old Name", "Description", Set.of(user1.getId()));

        // Update only the name
        UserGroup updated = userGroupApi.update(
                group.getId(),
                "New Name",
                group.getDescription(),
                Set.of(user1.getId())
        );

        assertEquals("New Name", updated.getName());
        assertEquals("Description", updated.getDescription());
    }
}

