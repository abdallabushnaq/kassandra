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


import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.awt.*;

public class ColorSerializer extends ValueSerializer<Color> {
    @Override
    public void serialize(Color color, JsonGenerator gen, SerializationContext provider) {
        if (color == null) {
            gen.writeNull();
            return;
        }

        // Format as hex string with alpha
        String colorHex = String.format("#%08X", color.getRGB());
        gen.writeString(colorHex);
    }

}