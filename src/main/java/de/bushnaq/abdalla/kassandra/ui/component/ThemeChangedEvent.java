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

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import lombok.Getter;

/**
 * Vaadin UI-bus event fired whenever the user toggles the application theme.
 *
 * <p>Subscribe via
 * {@code ComponentUtil.addListener(UI.getCurrent(), ThemeChangedEvent.class, listener)}
 * in {@code onAttach} and remove the registration in {@code onDetach} to avoid memory leaks.</p>
 */
@Getter
public class ThemeChangedEvent extends ComponentEvent<UI> {

    /**
     * The theme that has just become active.
     */
    private final ETheme newTheme;

    /**
     * Creates a new {@code ThemeChangedEvent}.
     *
     * @param source   the {@link UI} on which the theme changed
     * @param newTheme the theme that is now active
     */
    public ThemeChangedEvent(UI source, ETheme newTheme) {
        super(source, false);
        this.newTheme = newTheme;
    }
}

