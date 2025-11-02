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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.theme.lumo.Lumo;

public class ThemeToggle extends Button {
    private boolean darkTheme = false; // Default to light theme

    public ThemeToggle() {
        setIcon(new Icon(VaadinIcon.ADJUST));
        addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY_INLINE);
        addClickListener(e -> toggleTheme());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Load saved preference
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('theme')")
                .then(String.class, theme -> {
                    ThemeList themeList = UI.getCurrent().getElement().getThemeList();
                    if ("dark".equals(theme)) {
                        // Apply dark theme if preference is set to dark
                        themeList.add(Lumo.DARK);
                        darkTheme = true;
                    } else {
                        // Default to light theme
                        themeList.remove(Lumo.DARK);
                        darkTheme = false;
                    }
                    updateTooltip();
                });
    }

    private void toggleTheme() {
        ThemeList themeList = UI.getCurrent().getElement().getThemeList();
        if (themeList.contains(Lumo.DARK)) {
            themeList.remove(Lumo.DARK);
            darkTheme = false;
        } else {
            themeList.add(Lumo.DARK);
            darkTheme = true;
        }
        updateTooltip();

        // Store preference in localStorage
        UI.getCurrent().getPage().executeJs(
                "localStorage.setItem('theme', $0)",
                darkTheme ? "dark" : "light");
    }

    private void updateTooltip() {
        setTooltipText(darkTheme ? "Switch to light theme" : "Switch to dark theme");
    }
}