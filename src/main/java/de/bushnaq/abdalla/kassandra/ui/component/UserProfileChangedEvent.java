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

/**
 * Vaadin UI-bus event fired whenever the current user saves changes to their profile
 * (display name, color, or avatar).
 *
 * <p>Subscribe via
 * {@code ComponentUtil.addListener(UI.getCurrent(), UserProfileChangedEvent.class, listener)}
 * in {@code onAttach} and remove the registration in {@code onDetach} to avoid memory leaks.</p>
 */
public class UserProfileChangedEvent extends ComponentEvent<UI> {

    /**
     * Creates a new {@code UserProfileChangedEvent}.
     *
     * @param source the {@link UI} in which the profile change occurred
     */
    public UserProfileChangedEvent(UI source) {
        super(source, false);
    }
}

