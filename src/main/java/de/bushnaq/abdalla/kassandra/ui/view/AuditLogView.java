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
     * Identifier of the audit page title.
     */
    public static final  String PAGE_TITLE_ID      = "audit-page-title";
    /**
     * Identifier of the actor filter.
     */
    public static final  String USER_FILTER_ID     = "audit-user-filter";
    /**
     * Identifier of the inclusive start-time filter.
     */
    public static final  String FROM_FILTER_ID     = "audit-from-filter";
    /**
     * Identifier of the exclusive end-time filter.
     */
    public static final  String TO_FILTER_ID       = "audit-to-filter";
    /**
     * Identifier of the audit grid.
     */
    public static final  String GRID_ID            = "audit-grid";
    /**
     * Identifier of the page-number field.
     */
    public static final  String PAGE_NUMBER_ID     = "audit-page-number";
    /**
     * Identifier of the previous-page button.
     */
    public static final  String PREVIOUS_BUTTON_ID = "audit-previous-button";
    /**
     * Identifier of the next-page button.
     */
    public static final  String NEXT_BUTTON_ID     = "audit-next-button";
    /**
     * Identifier of the go-to-page button.
     */
    public static final  String GO_BUTTON_ID       = "audit-go-button";
    /**
     * Identifier of the audit search field.
     */
    public static final  String SEARCH_FIELD_ID    = "audit-search-field";
    /**
     * Identifier of the action filter.
     */
    public static final  String ACTION_FILTER_ID   = "audit-action-filter";
    /**
     * Identifier of the apply-filters button.
     */
    public static final  String APPLY_BUTTON_ID    = "audit-apply-button";
    /**
     * Prefix for an event label, followed by its revision and entity ID.
     */
    public static final  String EVENT_ID_PREFIX    = "audit-event-";
    /**
     * Prefix for expanded event details, followed by its revision and entity ID.
     */
    public static final  String DETAILS_ID_PREFIX  = "audit-details-";
    /**
     * Route for administrator audit history.
     */
    public static final  String ROUTE              = "audit";
    private static final int    PAGE_SIZE          = 50;

    private final Select<String>   action     = new Select<>();
    private final AuditLogApi      auditLogApi;
    private final DateTimePicker   from       = new DateTimePicker("From");
    private final Grid<AuditEvent> grid       = new Grid<>(AuditEvent.class, false);
    private       AuditEvent       openDetails;
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
        action.setId(ACTION_FILTER_ID);
        search.setId(SEARCH_FIELD_ID);
        user.setId(USER_FILTER_ID);
        from.setId(FROM_FILTER_ID);
        to.setId(TO_FILTER_ID);
        grid.setId(GRID_ID);
        pageNumber.setId(PAGE_NUMBER_ID);
        previous.setId(PREVIOUS_BUTTON_ID);
        next.setId(NEXT_BUTTON_ID);
        action.setLabel("Action");
        action.setItems("All", "CREATE", "UPDATE", "DELETE");
        action.setValue("All");
        search.setPlaceholder("Name, email, action, or YYYY-MM-DD");
        Button apply = new Button("Apply", event -> {
            page = 0;
            refresh();
        });
        apply.setId(APPLY_BUTTON_ID);
        HorizontalLayout filters = new HorizontalLayout(search, user, action, from, to, apply);
        filters.setAlignItems(Alignment.END);
        filters.setWidthFull();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
        grid.addColumn(event -> formatter.format(event.timestamp())).setHeader("When (UTC)").setAutoWidth(true);
        grid.addColumn(AuditEvent::actor).setHeader("Who").setAutoWidth(true);
        grid.addColumn(AuditEvent::action).setHeader("Action").setAutoWidth(true);
        grid.addColumn(AuditEvent::entityType).setHeader("Type").setAutoWidth(true);
        grid.addComponentColumn(event -> {
            Span label = new Span(event.label() == null ? event.entityId() : event.label());
            label.setId(EVENT_ID_PREFIX + event.revision() + "-" + event.entityId());
            return label;
        }).setHeader("What").setFlexGrow(1);
        grid.addColumn(event -> event.replay() ? "Undo/redo" : "Change").setHeader("Origin").setAutoWidth(true);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(event -> {
            VerticalLayout details = new VerticalLayout();
            details.setId(DETAILS_ID_PREFIX + event.revision() + "-" + event.entityId());
            details.setWidthFull();
            details.getStyle().set("box-sizing", "border-box")
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("font-size", "var(--lumo-font-size-m)");
            Span heading = new Span(formatter.format(event.timestamp()) + " UTC | " + event.actor() + " | "
                    + event.action() + " " + event.entityType() + ": "
                    + (event.label() == null ? event.entityId() : event.label()));
            heading.getStyle().set("font-weight", "600").set("overflow-wrap", "anywhere");
            details.add(heading);
            if (event.fieldChanges().isEmpty()) {
                details.add(new Span("No changed fields available."));
            } else {
                event.fieldChanges().forEach(change -> {
                    Span field = new Span(change);
                    field.getStyle().set("overflow-wrap", "anywhere").set("white-space", "pre-wrap");
                    details.add(field);
                });
            }
            return details;
        }));
        grid.setDetailsVisibleOnClick(false);
        grid.addItemClickListener(event -> {
            AuditEvent selected = event.getItem();
            if (openDetails != null) {
                grid.setDetailsVisible(openDetails, false);
            }
            boolean expand = "UPDATE".equals(selected.action()) && !selected.equals(openDetails);
            grid.setDetailsVisible(selected, expand);
            openDetails = expand ? selected : null;
            if (expand) {
                grid.scrollToItem(selected);
            }
        });
        grid.addClassName("audit-log-grid");
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
        go.setId(GO_BUTTON_ID);
        HorizontalLayout paging = new HorizontalLayout(previous, pageLabel, next, pageNumber, go);
        paging.setAlignItems(Alignment.CENTER);
        H1 title = new H1("Audit");
        title.setId(PAGE_TITLE_ID);
        add(title, filters, grid, paging);
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
        if (openDetails != null) {
            grid.setDetailsVisible(openDetails, false);
        }
        openDetails = null;
        grid.setItems(result.items());
        previous.setEnabled(page > 0);
        next.setEnabled((long) (page + 1) * PAGE_SIZE < result.total());
        pageCount = Math.max(1, (result.total() + PAGE_SIZE - 1) / PAGE_SIZE);
        pageNumber.setValue(page + 1);
        pageLabel.setText("Page " + (page + 1) + " of " + pageCount
                + " (" + result.total() + " changes)");
    }
}
