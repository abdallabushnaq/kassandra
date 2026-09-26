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
package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bushnaq.abdalla.kassandra.dto.AuditEvent;
import de.bushnaq.abdalla.kassandra.dto.AuditPage;
import de.bushnaq.abdalla.kassandra.rest.api.AuditLogApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Read-only administrator audit history with database-backed filtering and pagination.
 */
@Route(value = AuditLogView.ROUTE, layout = MainLayout.class)
@PageTitle("Audit")
@RolesAllowed("ADMIN")
public class AuditLogView extends VerticalLayout {
    /**
     * Route for administrator audit history.
     */
    public static final  String ROUTE     = "audit";
    private static final int    PAGE_SIZE = 50;

    private final Select<String>   action     = new Select<>();
    private final AuditLogApi      auditLogApi;
    private final DateTimePicker   from       = new DateTimePicker("From");
    private final Grid<AuditEvent> grid       = new Grid<>(AuditEvent.class, false);
    private final Span             pageLabel  = new Span();
    private final IntegerField     pageNumber = new IntegerField("Page number");
    private       long             pageCount  = 1;
    private       int              page;
    private final Button           next       = new Button("Next", event -> {
        page++;
        refresh();
    });
    private final Button           previous   = new Button("Previous", event -> {
        page--;
        refresh();
    });
    private final TextField        search     = new TextField("Search");
    private final DateTimePicker   to         = new DateTimePicker("To (exclusive)");
    private final TextField        user       = new TextField("User name or email");

    /**
     * Creates the admin audit page.
     *
     * @param auditLogApi authenticated audit REST client
     */
    public AuditLogView(AuditLogApi auditLogApi) {
        this.auditLogApi = auditLogApi;
        setSizeFull();
        action.setLabel("Action");
        action.setItems("All", "CREATE", "UPDATE", "DELETE");
        action.setValue("All");
        search.setPlaceholder("Name, email, action, or YYYY-MM-DD");
        Button apply = new Button("Apply", event -> {
            page = 0;
            refresh();
        });
        HorizontalLayout filters = new HorizontalLayout(search, user, action, from, to, apply);
        filters.setAlignItems(Alignment.END);
        filters.setWidthFull();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
        grid.addColumn(event -> formatter.format(event.timestamp())).setHeader("When (UTC)").setAutoWidth(true);
        grid.addColumn(AuditEvent::actor).setHeader("Who").setAutoWidth(true);
        grid.addColumn(AuditEvent::action).setHeader("Action").setAutoWidth(true);
        grid.addColumn(AuditEvent::entityType).setHeader("Type").setAutoWidth(true);
        grid.addColumn(event -> event.label() == null ? event.entityId() : event.label()).setHeader("What").setFlexGrow(1);
        grid.addColumn(event -> event.replay() ? "Undo/redo" : "Change").setHeader("Origin").setAutoWidth(true);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(event -> {
            VerticalLayout details = new VerticalLayout();
            details.setPadding(false);
            if (event.fieldChanges().isEmpty()) {
                details.add(new Span("No changed fields available."));
            } else {
                event.fieldChanges().forEach(change -> details.add(new Span(change)));
            }
            return details;
        }));
        grid.setDetailsVisibleOnClick(false);
        grid.addItemClickListener(event -> {
            if ("UPDATE".equals(event.getItem().action())) {
                grid.setDetailsVisible(event.getItem(), !grid.isDetailsVisible(event.getItem()));
            }
        });
        grid.setSizeFull();
        pageNumber.setMin(1);
        pageNumber.setStepButtonsVisible(false);
        pageNumber.setWidth("8em");
        Button go = new Button("Go", event -> {
            Integer requested = pageNumber.getValue();
            if (requested == null || requested < 1 || requested > pageCount) {
                Notification.show("Enter a page number between 1 and " + pageCount);
                return;
            }
            page = requested - 1;
            refresh();
        });
        HorizontalLayout paging = new HorizontalLayout(previous, pageLabel, next, pageNumber, go);
        paging.setAlignItems(Alignment.CENTER);
        add(new H1("Audit"), filters, grid, paging);
        refresh();
    }

    private void refresh() {
        if (from.getValue() != null && to.getValue() != null && !from.getValue().isBefore(to.getValue())) {
            Notification.show("The end of the timeframe must be after its start");
            return;
        }
        ZoneId zone = ZoneId.systemDefault();
        AuditPage result = auditLogApi.getPage(user.getValue(), "All".equals(action.getValue()) ? null : action.getValue(),
                search.getValue(), from.getValue() == null ? null : from.getValue().atZone(zone).toInstant(),
                to.getValue() == null ? null : to.getValue().atZone(zone).toInstant(), page, PAGE_SIZE);
        grid.setItems(result.items());
        previous.setEnabled(page > 0);
        next.setEnabled((long) (page + 1) * PAGE_SIZE < result.total());
        pageCount = Math.max(1, (result.total() + PAGE_SIZE - 1) / PAGE_SIZE);
        pageNumber.setValue(page + 1);
        pageLabel.setText("Page " + (page + 1) + " of " + pageCount
                + " (" + result.total() + " changes)");
    }
}
