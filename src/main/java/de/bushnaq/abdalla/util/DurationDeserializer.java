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

package de.bushnaq.abdalla.util;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.Duration;

public class DurationDeserializer extends ValueDeserializer<Duration> {
    @Override
    public Duration deserialize(JsonParser p, DeserializationContext ctxt) {
        String value = p.getValueAsString();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        long     days  = 0, hours = 0, minutes = 0, seconds = 0;
        String[] parts = value.split("\\s+");

        for (String part : parts) {
            if (part.endsWith("d")) {
                days = Long.parseLong(part.substring(0, part.length() - 1));
            } else if (part.endsWith("h")) {
                hours = Long.parseLong(part.substring(0, part.length() - 1));
            } else if (part.endsWith("m")) {
                minutes = Long.parseLong(part.substring(0, part.length() - 1));
            } else if (part.endsWith("s")) {
                seconds = Long.parseLong(part.substring(0, part.length() - 1));
            }
        }

        return Duration.ofDays(days)
                .plusHours(hours)
                .plusMinutes(minutes)
                .plusSeconds(seconds);
    }
}
