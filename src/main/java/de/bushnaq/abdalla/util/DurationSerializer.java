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

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.Duration;

public class DurationSerializer extends ValueSerializer<Duration> {
    @Override
    public void serialize(Duration duration, JsonGenerator gen, SerializationContext provider) {
        if (duration == null) {
            gen.writeNull();
            return;
        }

        StringBuilder sb      = new StringBuilder();
        long          days    = duration.toDays();
        long          hours   = duration.toHoursPart();
        long          minutes = duration.toMinutesPart();
        long          seconds = duration.toSecondsPart();

        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");
        if (duration.toSeconds() == 0) {
            sb.append("0s");
        }

        gen.writeString(sb.toString().trim());
    }

}