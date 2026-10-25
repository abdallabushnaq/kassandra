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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

/**
 * Shared page header with ordered identity, filter, summary, action and help slots.
 */
public class SmartHeader extends HorizontalLayout {
    private final Div       actions   = slot("smart-header-actions");
    private final Div       filters   = slot("smart-header-filters");
    private final Div       help      = slot("smart-header-help");
    private final Div       icon      = slot("smart-header-icon");
    private final Div       search    = slot("smart-header-search");
    private final Div       summary   = slot("smart-header-summary");
    private       Component fallbackIcon;
    /**
     * Page heading, also used by views with selection-dependent titles.
     */
    @Getter
    private final H2        pageTitle = new H2();

    /**
     * Creates a page header without optional controls.
     *
     * @param title     page title
     * @param titleId   existing title identifier, or null
     * @param titleIcon page icon or avatar, or null for the default icon
     */
    public SmartHeader(String title, String titleId, Component titleIcon) {
        addClassName("smart-header");
        setWidthFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("display", "grid");
        pageTitle.addClassName("smart-header-title");
        if (titleId != null) {
            pageTitle.setId(titleId);
        }
        setTitle(title);
        setTitleIcon(titleIcon);
        Div identity = slot("smart-header-identity");
        identity.add(icon, pageTitle);
        Div controls = slot("smart-header-controls");
        controls.add(search, filters, summary);
        Div right = slot("smart-header-right");
        right.add(actions, help);
        add(identity, controls, right);
    }

    /**
     * Appends actions before the dedicated help slot.
     *
     * @param components actions in display order
     */
    public void addActions(Component... components) {
        actions.add(components);
    }

    /**
     * Appends filters after search and before the summary.
     *
     * @param components filters in display order
     */
    public void addFilters(Component... components) {
        filters.add(components);
    }

    /**
     * Sets the final page-help control.
     *
     * @param helpButton help button, or null to clear it
     */
    public void setHelpButton(Button helpButton) {
        replace(help, helpButton);
    }

    /**
     * Sets the smart or plain-text search control.
     *
     * @param component search control, or null to clear it
     */
    public void setSearch(Component component) {
        replace(search, component);
    }

    /**
     * Sets the row count or other compact summary.
     *
     * @param component summary, or null to clear it
     */
    public void setSummary(Component component) {
        replace(summary, component);
    }

    /**
     * Updates the page heading.
     *
     * @param title page title
     */
    public void setTitle(String title) {
        pageTitle.setText(title);
    }

    /**
     * Replaces the page icon or avatar.
     *
     * @param component icon or avatar, or null for the default icon
     */
    public void setTitleIcon(Component component) {
        icon.removeAll();
        if (fallbackIcon == null) {
            setFallbackIcon(VaadinIcon.INFO_CIRCLE);
        } else {
            icon.add(fallbackIcon);
        }
        if (component != null) {
            if (component instanceof Image image) {
                image.setAlt("");
            }
            icon.add(component);
        }
    }

    /**
     * Sets the page icon shown when its avatar is absent or hidden.
     *
     * @param pageIcon fallback page icon
     */
    public void setFallbackIcon(VaadinIcon pageIcon) {
        if (fallbackIcon != null) {
            icon.remove(fallbackIcon);
        }
        fallbackIcon = pageIcon.create();
        fallbackIcon.addClassName("smart-header-fallback");
        fallbackIcon.getElement().setAttribute("aria-hidden", "true");
        icon.getElement().insertChild(0, fallbackIcon.getElement());
    }

    private static void replace(Div slot, Component component) {
        slot.removeAll();
        if (component != null) {
            slot.add(component);
        }
    }

    private static Div slot(String className) {
        Div slot = new Div();
        slot.addClassName(className);
        return slot;
    }
}
