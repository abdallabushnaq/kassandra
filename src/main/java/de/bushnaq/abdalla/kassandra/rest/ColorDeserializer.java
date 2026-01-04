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

package de.bushnaq.abdalla.kassandra.rest;


import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.awt.*;

@Slf4j
public class ColorDeserializer extends ValueDeserializer<Color> {
    @Override
    public Color deserialize(JsonParser p, DeserializationContext ctx) {
        String json = p.getText();
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            // If color string has # prefix, use Color.decode
            if (json.startsWith("#")) {
                return new Color((int) Long.parseLong(json.substring(1), 16), true);
            } else {
                // Parse integer RGB value
                return new Color((int) Long.parseLong(json, 16), true);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid color format: " + json, e);
        }
        return null;
    }
}
