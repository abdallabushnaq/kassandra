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

package de.bushnaq.abdalla.kassandra.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Breadcrumbs extends HorizontalLayout {

    private final List<BreadcrumbItem> items = new ArrayList<>();


    public Breadcrumbs() {
        addClassNames(
                Padding.XSMALL,  // Reduced padding
//                Background.CONTRAST_5,
                BorderRadius.SMALL,  // Smaller radius
                AlignItems.CENTER,
                Gap.XSMALL,      // Smaller gap between items
                FontSize.SMALL   // Smaller font size
        );
        setMinHeight("24px");    // Set explicit min-height
        setHeight("24px");       // Fixed height to make it compact
    }

    public void addItem(String title, Class<? extends Component> view) {
        addItem(title, view, null);
    }

    public void addItem(String title, Class<? extends Component> view, Map<String, String> params) {
        // Check if we're going back in the navigation path
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).view.equals(view)) {
                // Found existing item, truncate list at this point
                while (items.size() > i + 1) {
                    items.remove(items.size() - 1);
                }
                updateUI();
                return;
            }
        }

        items.add(new BreadcrumbItem(title, view, title, params));
        updateUI();
    }

    public void clear() {
        items.clear();
        updateUI();
    }

    private void updateUI() {
        removeAll();

        for (int i = 0; i < items.size(); i++) {
            BreadcrumbItem item = items.get(i);

            if (i > 0) {
                // Add separator
                Span separator = new Span(">");
                separator.addClassNames(TextColor.SECONDARY, Margin.Horizontal.XSMALL);
                add(separator);
            }

            // Last item (current location) is not a link
            if (i == items.size() - 1) {
                Span current = new Span(item.title);
                current.addClassNames(FontWeight.BOLD);
                add(current);
            } else {
                RouterLink link;
                link = new RouterLink(item.title, item.view);
                if (item.id != null) {
                    link.setId(item.id);
                }
                if (item.params != null) {
                    link.setQueryParameters(QueryParameters.simple(item.params));
                }
                link.addClassNames(TextColor.PRIMARY);
                add(link);
            }
        }
    }

    private static class BreadcrumbItem {
        private final String                     id;
        private final Map<String, String>        params;
        private final String                     title;
        private final Class<? extends Component> view;

        public BreadcrumbItem(String title, Class<? extends Component> view, String id, Map<String, String> params) {
            this.title  = title;
            this.view   = view;
            this.params = params;
            this.id     = id;
        }
    }
}