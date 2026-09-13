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

package de.bushnaq.abdalla.kassandra.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory;

import java.util.UUID;
import java.util.function.Function;

/**
 * Displays a planning operation with its affected entities collapsed until the user expands it.
 */
public class UndoHistoryOperationDetails extends Details {

    /**
     * Creates a collapsible planning operation without an action button.
     *
     * @param operation                planning operation to display
     * @param productAvatarUrlResolver resolves product avatar URLs
     * @param userAvatarUrlResolver    resolves actor avatar URLs
     */
    public UndoHistoryOperationDetails(UndoRedoHistory.Operation operation,
                                       Function<UUID, String> productAvatarUrlResolver,
                                       Function<String, String> userAvatarUrlResolver) {
        this(operation, productAvatarUrlResolver, userAvatarUrlResolver, null);
    }

    /**
     * Creates a collapsible planning operation with an action shown after expansion.
     *
     * @param operation                planning operation to display
     * @param productAvatarUrlResolver resolves product avatar URLs
     * @param userAvatarUrlResolver    resolves actor avatar URLs
     * @param actionCaption            action button caption
     * @param actionCallback           action to run when the button is clicked
     */
    public UndoHistoryOperationDetails(UndoRedoHistory.Operation operation,
                                       Function<UUID, String> productAvatarUrlResolver,
                                       Function<String, String> userAvatarUrlResolver, String actionCaption,
                                       Runnable actionCallback) {
        this(operation, productAvatarUrlResolver, userAvatarUrlResolver, createAction(actionCaption, actionCallback));
    }

    private UndoHistoryOperationDetails(UndoRedoHistory.Operation operation,
                                        Function<UUID, String> productAvatarUrlResolver,
                                        Function<String, String> userAvatarUrlResolver, Button action) {
        setSummary(createSummary(operation, productAvatarUrlResolver, userAvatarUrlResolver));
        add(createContent(operation, action));
        addClassName("planning-history-operation");
        setOpened(false);
    }

    private void addAvatar(HorizontalLayout layout, String source, String alt) {
        Image avatar = createAvatar(source, alt);
        if (avatar != null) {
            layout.add(avatar);
        }
    }

    private static Button createAction(String actionCaption, Runnable actionCallback) {
        Button action = new Button(actionCaption, event -> actionCallback.run());
        action.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        return action;
    }

    private Image createAvatar(String source, String alt) {
        if (source == null) {
            return null;
        }
        Image avatar = new Image(source, alt);
        avatar.setWidth("20px");
        avatar.setHeight("20px");
        avatar.getStyle().set("border-radius", "4px").set("object-fit", "cover");
        return avatar;
    }

    private VerticalLayout createContent(UndoRedoHistory.Operation operation, Button action) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setWidthFull();

        var visibleChanges = operation.getEntityChanges().stream().filter(this::isVisible).toList();
        if (visibleChanges.isEmpty()) {
            content.add(new Span("No updates in any fields."));
        } else {
            visibleChanges.forEach(change -> {
                Span entity = new Span(change.getAction() + " " + change.getEntityType() + ": " + change.getDisplayName());
                entity.getStyle().set("font-weight", "600").set("white-space", "normal");
                content.add(entity);
                change.getFieldChanges().forEach(fieldChange -> {
                    Span field = new Span(fieldChange);
                    field.getStyle().set("white-space", "normal");
                    content.add(field);
                });
            });
        }
        if (action != null) {
            content.add(action);
        }
        return content;
    }

    private VerticalLayout createSummary(UndoRedoHistory.Operation operation,
                                         Function<UUID, String> productAvatarUrlResolver,
                                         Function<String, String> userAvatarUrlResolver) {
        VerticalLayout summary = new VerticalLayout();
        summary.setPadding(false);
        summary.setSpacing(false);
        summary.setWidthFull();

        //created
        Span created = new Span(String.valueOf(operation.getCreated()));
        created.getStyle().set("font-weight", "600").set("white-space", "normal");
        summary.add(created);

        HorizontalLayout details = new HorizontalLayout();
        details.setAlignItems(Alignment.CENTER);
        details.setSpacing(true);
        details.setWidthFull();

        addAvatar(details, productAvatarUrlResolver.apply(operation.getProductId()), operation.getProductName());

        //product
        Span productName = new Span(operation.getProductName());
        productName.getStyle().set("font-weight", "600").set("white-space", "normal");
        details.add(productName);

        //actor
        addAvatar(details, userAvatarUrlResolver.apply(operation.getActor()), operation.getActor());
        Span actor = new Span(operation.getActor());
        actor.getStyle().set("font-weight", "600").set("white-space", "normal");
        details.add(actor);

        //summary
        Span summaryText = new Span(operation.getSummary());
        if (operation.getEntityChanges().size() > 1) {
            summaryText.setText(summaryText.getText() + " (" + operation.getEntityChanges().size() + ")");
        }
        summaryText.getStyle().set("font-weight", "600").set("white-space", "normal");
        details.add(summaryText);


        summary.add(details);
        return summary;
    }

    private boolean isVisible(UndoRedoHistory.EntityChange change) {
        return !"Updated".equals(change.getAction()) || !change.getFieldChanges().isEmpty();
    }
}
