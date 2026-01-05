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
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class OffsetDateTimeSerializer extends ValueSerializer<OffsetDateTime> {
    //    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss XXX");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public void serialize(OffsetDateTime value, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        if (value == null) {
            log.error("OffsetDateTime argument is null.");
        }
        jsonGenerator.writeString(DATE_TIME_FORMATTER.format(value));
    }

}