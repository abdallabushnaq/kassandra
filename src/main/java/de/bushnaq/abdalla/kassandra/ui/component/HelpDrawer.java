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

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;

import java.io.UncheckedIOException;

/**
 * View-owned, non-modal help drawer that leaves the underlying page interactive.
 */
@CssImport("./help/help.css")
public class HelpDrawer extends Dialog {
    private final Div     content = new Div();
    private final boolean collapsibleChapters;
    private final Button  closeButton;
    private final String  resourcePath;
    private final String  title;
    private       Button  toggleButton;
    private       boolean loaded;

    /**
     * Creates a closed drawer; help content is loaded on first opening.
     *
     * @param title        the page name shown in the drawer
     * @param resourcePath the classpath help Markdown resource
     */
    public HelpDrawer(String title, String resourcePath) {
        this(title, resourcePath, false);
    }

    /**
     * Creates a closed drawer with optional collapsible Markdown chapters.
     *
     * @param title               the page name shown in the drawer
     * @param resourcePath        the classpath help Markdown resource
     * @param collapsibleChapters whether level-two headings become expandable chapters
     */
    public HelpDrawer(String title, String resourcePath, boolean collapsibleChapters) {
        this.title               = title;
        this.resourcePath        = resourcePath;
        this.collapsibleChapters = collapsibleChapters;
        addClassName("help-drawer");
        setHeaderTitle("Help: " + title);
        setModality(ModalityMode.MODELESS);
        setCloseOnOutsideClick(false);
        setWidth("min(72rem, 100vw)");
        closeButton = new Button(VaadinIcon.CLOSE.create(), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.setAriaLabel("Close help");
        closeButton.setTooltipText("Close help (Escape)");
        getHeader().add(closeButton);
        content.addClassName("help-drawer-content");
        add(content);
        addOpenedChangeListener(event -> {
            if (toggleButton != null) {
                toggleButton.getElement().setAttribute("aria-expanded", String.valueOf(event.isOpened()));
            }
        });
    }

    /**
     * Closes help and returns focus to its toolbar button.
     */
    @Override
    public void close() {
        super.close();
    }

    /**
     * Creates the accessible toolbar button controlling this drawer.
     *
     * @param id a stable, page-specific button ID
     * @return the button to add to the view header
     * @throws IllegalStateException if a button has already been created
     */
    public Button createToggleButton(String id) {
        if (toggleButton != null) {
            throw new IllegalStateException("The help drawer already has a toggle button");
        }
        setId(id + "-drawer");
        toggleButton = new Button(VaadinIcon.QUESTION_CIRCLE_O.create(), event -> {
            if (isOpened()) {
                close();
            } else {
                open();
            }
        });
        toggleButton.setId(id);
        toggleButton.setAriaLabel("Help: " + title);
        toggleButton.setTooltipText("Help: " + title);
        toggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toggleButton.getElement().setAttribute("aria-controls", id + "-drawer");
        toggleButton.getElement().setAttribute("aria-expanded", "false");
        return toggleButton;
    }

    @Override
    protected void onDetach(DetachEvent event) {
        super.close();
        super.onDetach(event);
    }

    /**
     * Opens the help drawer, reporting resource failures without showing empty help.
     */
    @Override
    public void open() {
        if (!loaded) {
            try {
                content.add(new HelpMarkdown(resourcePath, collapsibleChapters));
                loaded = true;
            } catch (UncheckedIOException e) {
                Notification.show("Help could not be loaded. Please contact your administrator.", 5000,
                        Notification.Position.BOTTOM_START);
                return;
            }
        }
        super.open();
        closeButton.focus();
    }
}
