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

package de.bushnaq.abdalla.kassandra.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.bushnaq.abdalla.kassandra.util.AbstractTestUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression coverage for shared header slots and grid-specific integration.
 */
@Tag("UnitTest")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
class SmartHeaderTest extends AbstractTestUtil {
    @Test
    void optionalSlotsStartEmpty() {
        SmartHeader header = new SmartHeader("Products", "title", null);
        assertEquals("Products", header.getPageTitle().getText());
        assertEquals("title", header.getPageTitle().getId().orElseThrow());
        for (String slot : List.of("search", "filters", "summary", "actions", "help")) {
            assertEquals(0, findSlot(header, slot).getChildren().count(), slot);
        }
    }

    @Test
    void slotsKeepSearchFiltersSummaryAndHelpInOrder() {
        SmartHeader header  = new SmartHeader("Products", "title", null);
        Span        search  = new Span("Search");
        Span        filter  = new Span("Filter");
        Span        summary = new Span("Summary");
        Button      help    = new Button("Help");
        Button      create  = new Button("Create");
        Button      ai      = new Button("AI");
        header.setHelpButton(help);
        header.addActions(create);
        header.setSummary(summary);
        header.addFilters(filter);
        header.setSearch(search);
        header.addActions(ai);

        assertEquals(List.of(search, filter, summary), findSlot(header, "controls").getChildren()
                .flatMap(Component::getChildren).toList());
        assertEquals(List.of(create, ai, help), findSlot(header, "right").getChildren()
                .flatMap(Component::getChildren).toList());
    }

    @Test
    void titleIconAndOptionalControlsCanBeReplaced() {
        Image       first  = new Image();
        Image       second = new Image();
        SmartHeader header = new SmartHeader("First", "title", first);
        header.setTitle("Second");
        header.setTitleIcon(second);
        header.setFallbackIcon(VaadinIcon.CALENDAR);
        assertEquals("Second", header.getPageTitle().getText());
        assertTrue(first.getParent().isEmpty());
        assertEquals(findSlot(header, "icon"), second.getParent().orElseThrow());
        assertEquals(2, findSlot(header, "icon").getChildren().count());
        header.setTitleIcon(null);
        assertEquals(1, findSlot(header, "icon").getChildren().count());
        assertTrue(second.getParent().isEmpty());
        header.setSearch(new Span("Search"));
        header.setSummary(new Span("Summary"));
        header.setHelpButton(new Button("Help"));
        header.setSearch(null);
        header.setSummary(null);
        header.setHelpButton(null);
        for (String slot : List.of("search", "summary", "help")) {
            assertEquals(0, findSlot(header, slot).getChildren().count());
        }
    }

    @Test
    void rebuiltGridHeaderReplacesItsCounterListener() {
        AbstractMainGrid<String> view = new AbstractMainGrid<>(Clock.systemUTC()) {
            @Override
            protected void initGrid(Clock clock) {
            }
        };
        SmartHeader first      = gridHeader(view);
        Span        oldCounter = (Span) findSlot(first, "summary").getChildren().findFirst().orElseThrow();
        view.getDataProvider().getItems().addAll(List.of("Alpha", "Beta"));
        view.getDataProvider().refreshAll();
        assertEquals("Showing 2 of 2 rows", oldCounter.getText());
        SmartHeader second  = gridHeader(view);
        Span        counter = (Span) findSlot(second, "summary").getChildren().findFirst().orElseThrow();
        view.getDataProvider().setFilter(value -> value.startsWith("A"));
        assertEquals("Showing 1 of 2 rows", counter.getText());
        assertEquals("Showing 2 of 2 rows", oldCounter.getText());
        assertSame(second, view.getSmartHeader());
    }

    private static Component findSlot(Component root, String name) {
        return root.getChildren().filter(child -> child.getElement().getClassList().contains("smart-header-" + name))
                .findFirst().orElseGet(() -> root.getChildren().flatMap(child -> child.getChildren())
                        .filter(child -> child.getElement().getClassList().contains("smart-header-" + name))
                        .findFirst().orElseThrow());
    }

    private static SmartHeader gridHeader(AbstractMainGrid<String> view) {
        return view.createSmartHeader("Items", "title", VaadinIcon.GRID, "create", () -> {
        }, "counter", null, null, null, "Product");
    }
}
