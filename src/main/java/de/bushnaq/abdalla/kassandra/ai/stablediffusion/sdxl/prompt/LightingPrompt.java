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

package de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt;

public enum LightingPrompt {

    GOLDEN_HOUR(
            "warm golden-hour lighting, soft sunlight, long shadows, warm highlights, atmospheric glow"
    ),

    BLUE_HOUR(
            "blue-hour lighting, cool ambient illumination, deep blue tones, subtle warm highlights, atmospheric mood"
    ),

    SOFT_DIFFUSE(
            "soft diffuse lighting, gentle shadows, evenly illuminated subject, subtle highlights, smooth tonal transitions"
    ),

    DRAMATIC_CHIAROSCURO(
            "dramatic chiaroscuro lighting, strong contrast between light and shadow, deep shadows, focused illumination"
    ),

    VOLUMETRIC(
            "dramatic volumetric lighting, visible rays of light, atmospheric haze, glowing light beams, cinematic illumination"
    ),

    RIM_LIGHT(
            "strong rim lighting, glowing edge highlights around the subject, darker background, cinematic separation"
    ),

    NEON(
            "vibrant neon lighting, colorful artificial lights, magenta and cyan highlights, reflective surfaces, atmospheric glow"
    ),

    MOONLIGHT(
            "cool moonlight, pale blue illumination, deep shadows, subtle highlights, quiet nocturnal atmosphere"
    ),

    CANDLELIGHT(
            "warm candlelight, flickering illumination, soft orange highlights, deep natural shadows, intimate atmosphere"
    ),

    OVERCAST(
            "soft overcast lighting, cloudy diffused daylight, minimal harsh shadows, muted colors, natural atmospheric illumination"
    );

    private final String prompt;

    LightingPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
