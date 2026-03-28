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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.theme.lumo.Lumo;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;

/**
 * A toggle button that switches the Vaadin UI between the Lumo light and dark themes.
 *
 * <p>On attach the stored preference is loaded from browser {@code localStorage}.
 * On every toggle click the new theme is persisted back to {@code localStorage},
 * stored in the session via {@link ThemeSessionState}, and broadcast to all
 * interested views by firing a {@link ThemeChangedEvent} on the current {@link UI}.</p>
 */
public class ThemeToggle extends Button {

    private       boolean            darkTheme          = false; // Default to light theme
    private final ThemeSessionState  themeSessionState;

    /**
     * Creates a new theme-toggle button.
     *
     * @param themeSessionState the session-scoped bean that holds the per-user theme preference
     */
    public ThemeToggle(ThemeSessionState themeSessionState) {
        this.themeSessionState = themeSessionState;
        setIcon(new Icon(VaadinIcon.ADJUST));
        addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY_INLINE);
        addClickListener(e -> toggleTheme());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Load saved preference from browser localStorage
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('theme')")
                .then(String.class, theme -> {
                    ThemeList themeList = UI.getCurrent().getElement().getThemeList();
                    if ("dark".equals(theme)) {
                        themeList.add(Lumo.DARK);
                        darkTheme = true;
                    } else {
                        themeList.remove(Lumo.DARK);
                        darkTheme = false;
                    }
                    ETheme newTheme = darkTheme ? ETheme.dark : ETheme.light;
                    themeSessionState.setTheme(newTheme);
                    ComponentUtil.fireEvent(UI.getCurrent(), new ThemeChangedEvent(UI.getCurrent(), newTheme));
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
        ETheme newTheme = darkTheme ? ETheme.dark : ETheme.light;
        themeSessionState.setTheme(newTheme);
        ComponentUtil.fireEvent(UI.getCurrent(), new ThemeChangedEvent(UI.getCurrent(), newTheme));
        updateTooltip();

        // Persist preference in localStorage
        UI.getCurrent().getPage().executeJs(
                "localStorage.setItem('theme', $0)",
                darkTheme ? "dark" : "light");
    }

    private void updateTooltip() {
        setTooltipText(darkTheme ? "Switch to light theme" : "Switch to dark theme");
    }
}