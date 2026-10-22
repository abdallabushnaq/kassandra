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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package de.bushnaq.abdalla.kassandra.util;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.AbstractTimeAware;
import de.bushnaq.abdalla.kassandra.dto.Availability;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Location;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.rest.api.UndoRedoApi;
import de.bushnaq.abdalla.kassandra.service.HistoricalFieldChanges;
import de.bushnaq.abdalla.kassandra.ui.component.UndoHistoryOperationDetails;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies deterministic fixture and persistence timestamps without generating images or snapshots.
 */
@Tag("UnitTest")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
class EntityTimestampTest extends AbstractUiTestUtil {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-22T08:00:00+01:00");
    private OffsetDateTime previousTime;
    @Autowired
    private UndoRedoApi undoRedoApi;

    @BeforeEach
    void configureTimestampSequence() {
        previousTime = ParameterOptions.getNow();
        ParameterOptions.setNow(NOW);
    }

    @AfterEach
    void restoreTime() {
        ParameterOptions.setNow(previousTime);
    }

    @Test
    void sequencesPersistenceTimeWithoutAdvancingPlanningTime() {
        assertEquals(NOW, ParameterOptions.getPersistenceNow());
        assertEquals(NOW.plusSeconds(1), ParameterOptions.getPersistenceNow());
        assertEquals(NOW, ParameterOptions.getNow());
        OffsetDateTime historical = NOW.plusHours(3).plusNanos(123);
        assertEquals(NOW.plusHours(3).plusSeconds(1), ParameterOptions.getPersistenceNow(historical));
        assertEquals(NOW.plusHours(3).plusSeconds(2), ParameterOptions.getPersistenceNow());
        ParameterOptions.setNow(NOW);
        assertEquals(NOW, ParameterOptions.getPersistenceNow());
    }

    @Test
    void usesRealTimeOutsideFixedTimeMode() {
        ParameterOptions.setNow(null);
        OffsetDateTime before = OffsetDateTime.now();
        OffsetDateTime timestamp = ParameterOptions.getPersistenceNow(NOW.plusYears(10));
        OffsetDateTime after = OffsetDateTime.now();
        assertFalse(timestamp.isBefore(before));
        assertFalse(timestamp.isAfter(after));
    }

    @Test
    void generatesDistinctRepeatableTimestampsForTheWholeHierarchy() {
        EntityGenerator generator = new EntityGenerator();
        List<OffsetDateTime> first = hierarchy(generator).stream().map(AbstractTimeAware::getCreated).toList();
        assertEquals(first.size(), first.stream().distinct().count());
        for (int index = 0; index < first.size(); index++) {
            assertEquals(NOW.plusSeconds(index), first.get(index));
        }
        generator.init();
        ParameterOptions.setNow(NOW);
        assertEquals(first, hierarchy(generator).stream().map(AbstractTimeAware::getCreated).toList());
    }

    @Test
    void preservesHistoricalUserDatesWithDistinctRelatedRecordTimes() {
        EntityGenerator generator = new EntityGenerator();
        LocalDate start = NOW.toLocalDate().minusYears(2);
        User first = generator.addUser("Historical", "historical@example.org", "USER", start, Color.BLUE, user -> user);
        User second = generator.addUser("Historical two", "historical-two@example.org", "USER", start, Color.RED, user -> user);
        Availability availability = generator.addAvailability(first, 1, start);
        Location location = generator.addLocation(first, "de", "nw", start);
        User third = generator.addUser("Historical three", "historical-three@example.org", "USER", start, Color.GREEN, user -> user);
        List<AbstractTimeAware> entities = List.of(first, second, availability, location, third);
        assertEquals(5, entities.stream().map(AbstractTimeAware::getCreated).distinct().count());
        assertTrue(entities.stream().allMatch(entity -> entity.getCreated().toLocalDate().equals(start)));
        assertTrue(entities.stream().allMatch(entity -> entity.getCreated().equals(entity.getUpdated())));
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void persistsFixtureTimesAndSequencesUpdatesOperationsAndAuditRevisions() {
        EntityGenerator generator = new EntityGenerator();
        Product product = generator.addProduct("Timestamp product", peg.productApi::persist);
        OffsetDateTime created = product.getCreated();
        assertEquals(NOW, created);
        assertEquals(created, product.getUpdated());
        assertEquals(created, peg.productApi.getById(product.getId()).getCreated());

        product.setName("Timestamp product updated");
        peg.productApi.update(product);
        Product updated = peg.productApi.getById(product.getId());
        assertEquals(created, updated.getCreated());
        assertTrue(updated.getUpdated().isAfter(created));
        var operation = undoRedoApi.history(product.getId()).getOperations().getFirst();
        assertEquals(0, operation.getCreated().getNano());
        assertTrue(operation.getCreated().isBefore(updated.getUpdated()));
        Number revisionTime = (Number) entityManager.createNativeQuery(
                        "SELECT MAX(r.timestamp) FROM audit_revisions r JOIN products_aud p ON p.rev = r.id WHERE p.id = :id")
                .setParameter("id", product.getId())
                .getSingleResult();
        assertTrue(revisionTime.longValue() > created.toInstant().toEpochMilli());
        assertTrue(revisionTime.longValue() < NOW.plusHours(1).toInstant().toEpochMilli());

        ParameterOptions.setNow(NOW);
        ParameterOptions.getPersistenceNow(updated.getUpdated());
        product.setName("Timestamp product updated again");
        peg.productApi.update(product);
        assertTrue(peg.productApi.getById(product.getId()).getUpdated().isAfter(updated.getUpdated()));
        assertEquals(NOW, ParameterOptions.getNow());
    }

    @Test
    void hidesFractionalSecondsWithoutDiscardingHistoricalDifferences() {
        String before = "2026-08-22T08:00:00.123456+01:00";
        String after = "2026-08-22T08:00:00.987654+01:00";
        assertEquals(List.of("updated: 2026-08-22T08:00:00+01:00 -> 2026-08-22T08:00:00+01:00"),
                HistoricalFieldChanges.between(Map.of("updated", before), Map.of("updated", after)));
        assertEquals(List.of("notes: " + before + " -> " + after),
                HistoricalFieldChanges.between(Map.of("notes", before), Map.of("notes", after)));
        assertEquals(List.of("updated: 2026-08-22T08:00:00+01:00 -> 2026-08-22T08:00:01+01:00"),
                HistoricalFieldChanges.between(Map.of("updated", NOW.plusNanos(123)), Map.of("updated", NOW.plusSeconds(1))));
    }

    @Test
    void formatsHistorySummaryToWholeSeconds() {
        var operation = new UndoRedoHistory.Operation();
        operation.setCreated(NOW.plusNanos(123456));
        operation.setSummary("Updated product");
        operation.setProductName("Product");
        operation.setActor("admin-user");
        operation.setEntityChanges(List.of());
        UndoHistoryOperationDetails details = new UndoHistoryOperationDetails(operation, id -> null, actor -> null);
        VerticalLayout summary = (VerticalLayout) details.getSummary();
        assertEquals("2026-08-22 08:00:00+01:00", ((Span) summary.getComponentAt(0)).getText());
    }

    private List<AbstractTimeAware> hierarchy(EntityGenerator generator) {
        Product product = generator.addProduct("Product");
        Version version = generator.addVersion(product, "Version");
        Feature feature = generator.addFeature(version, "Feature");
        Sprint sprint = generator.addSprint(feature, "Sprint");
        Task task = generator.addTask(sprint, null, "Task", NOW.toLocalDateTime(), Duration.ofHours(4),
                Duration.ofHours(8), null, null);
        User user = generator.addUser("User", "user@example.org", "USER", null, Color.BLUE, generated -> generated);
        Worklog worklog = generator.addWorklog(task, user, NOW.toLocalDateTime(), Duration.ofHours(1), "Work");
        List<AbstractTimeAware> entities = List.of(product, version, feature, sprint, user, worklog);
        assertTrue(entities.stream().allMatch(entity -> entity.getCreated().equals(entity.getUpdated())));
        assertTrue(entities.stream().allMatch(entity -> entity.getCreated().getNano() == 0));
        return entities;
    }
}
