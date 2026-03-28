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

import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Session-scoped bean that stores the UI theme preference for the current user's browser session.
 * A separate instance exists per HTTP session, ensuring that toggling the theme for one user
 * does not affect any other user.
 *
 * <p>This is the canonical server-side owner of the chosen theme. {@link ThemeToggle} writes
 * to it whenever the user clicks the toggle or when the stored preference is restored from
 * {@code localStorage} on attach.  Views read the value indirectly through
 * {@code Context.syncTheme()} before launching background tasks.</p>
 */
@Component
@SessionScope
@Getter
@Setter
public class ThemeSessionState {

    /**
     * The theme currently active for this user's session.
     * Defaults to {@link ETheme#light}, which matches the browser default before any
     * preference is loaded from {@code localStorage}.
     */
    private ETheme theme = ETheme.light;
}


