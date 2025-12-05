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

package de.bushnaq.abdalla.kassandra.ui.view;

/**
 * Enum representing the grouping modes available in the ActiveSprints view.
 * <p>
 * <ul>
 *     <li>STORIES - Groups tasks by their parent stories (traditional view)</li>
 *     <li>FEATURES - Groups stories by features, with stories shown as cards in lanes</li>
 * </ul>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
public enum GroupingMode {
    STORIES("Stories"),
    FEATURES("Features");

    private final String displayName;

    GroupingMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

