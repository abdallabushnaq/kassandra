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

package de.bushnaq.abdalla.kassandra.ai.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.bushnaq.abdalla.kassandra.rest.ColorSerializer;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.lang.reflect.Type;

/**
 * A {@link ToolCallResultConverter} that uses a {@code com.fasterxml} {@link ObjectMapper}
 * with the project's custom serializers (e.g. {@link ColorSerializer}) registered.
 *
 * <p>Spring AI instantiates the converter class via no-arg reflection
 * ({@code type.getDeclaredConstructor().newInstance()}), so the mapper cannot be injected
 * as a Spring bean directly. Instead this class holds the mapper in a {@code static} field
 * that is populated once at startup by the {@link Initializer} inner {@link Component}.
 *
 * <p>Usage in {@code @Tool} methods:
 * <pre>
 *     &#64;Tool(description = "...", resultConverter = KassandraToolCallResultConverter.class)
 *     public List&lt;UserDto&gt; getAllUsers() { ... }
 * </pre>
 */
public class KassandraToolCallResultConverter implements ToolCallResultConverter {

    /**
     * Lazily populated by {@link Initializer} on application startup.
     * Falls back to a plain ObjectMapper if called before the context is ready (e.g. in tests).
     */
    private static volatile ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        SimpleModule colorModule = new SimpleModule();
        colorModule.addSerializer(Color.class, new FasterxmlColorSerializer());
        colorModule.addDeserializer(Color.class, new FasterxmlColorDeserializer());
        return new ObjectMapper()
                .registerModule(colorModule)
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String convert(Object result, Type returnType) {
        if (returnType == Void.TYPE) {
            return "\"Done\"";
        }
        try {
            return MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            return "\"Error serializing tool result: " + e.getMessage() + "\"";
        }
    }

    // ---------------------------------------------------------------------------
    // com.fasterxml equivalents of the tools.jackson Color serializer/deserializer
    // ---------------------------------------------------------------------------

    static class FasterxmlColorDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Color> {
        @Override
        public Color deserialize(com.fasterxml.jackson.core.JsonParser p,
                                 com.fasterxml.jackson.databind.DeserializationContext ctx) throws java.io.IOException {
            String text = p.getText();
            if (text == null || text.isEmpty()) return null;
            try {
                String hex = text.startsWith("#") ? text.substring(1) : text;
                return new Color((int) Long.parseLong(hex, 16), true);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    static class FasterxmlColorSerializer extends com.fasterxml.jackson.databind.JsonSerializer<Color> {
        @Override
        public void serialize(Color color, com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
            if (color == null) {
                gen.writeNull();
            } else {
                gen.writeString(String.format("#%08X", color.getRGB()));
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Spring component that initialises the static mapper once the context is up
    // ---------------------------------------------------------------------------

    /**
     * Eagerly initialises the static {@link #MAPPER} from the Spring context.
     * Declared as a nested {@link Component} so it is picked up by component scanning
     * without needing a separate file.
     */
    @Component
    public static class Initializer implements InitializingBean {
        @Override
        public void afterPropertiesSet() {
            // Re-build here if you later want to inject additional Spring-managed modules.
            // For now the static buildMapper() already covers all needed custom types.
            KassandraToolCallResultConverter.MAPPER = buildMapper();
        }
    }
}

