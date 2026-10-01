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

package de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl;


import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.*;

import java.util.Random;

public class PromptGenerator {

    private final Random random;

    public PromptGenerator() {
        this(new Random());
    }

    public PromptGenerator(long seed) {
        this(new Random(seed));
    }

    public PromptGenerator(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random cannot be null");
        }

        this.random = random;
    }

    /**
     * Generate a prompt using a random style.
     */
    public String generate(String subject) {
        return generate(subject, randomStyle());
    }

    public String generate(String subject, Prompt prompt) {
        return generate(subject, prompt.getCamera(), prompt.getStyle(), prompt.getLighting(), prompt.getQuality());
    }

    /**
     * Generate a prompt using the specified components.
     * The components are combined in the following order:
     * [YOUR SUBJECT],
     * [YOUR ACTION / ENVIRONMENT],
     * [COMPOSITION / CAMERA],
     * [STYLE BLOCK],
     * [LIGHTING],
     * [QUALITY / DETAIL]
     *
     */
    public String generate(
            String subject,
            CameraPrompt camera,
            StylePrompt style,
            LightingPrompt lighting,
            QualityPrompt quality
    ) {
        return String.join(
                ", ",
                subject.trim(),
                camera.getPrompt(),
                style.getPrompt(),
                lighting.getPrompt(),
                quality.getPrompt()
        );

    }


    /**
     * Generate a prompt using the specified style.
     * Camera, lighting and quality are selected according
     * to the style's weighted preferences.
     */
    public String generate(
            String subject,
            StylePrompt style
    ) {
        validateSubject(subject);

        if (style == null) {
            throw new IllegalArgumentException("Style cannot be null");
        }

        StyleProfile profile = StyleProfiles.get(style);

        CameraPrompt camera = WeightedRandom.choose(
                profile.cameras(),
                random
        );

        LightingPrompt lighting = WeightedRandom.choose(
                profile.lighting(),
                random
        );

        QualityPrompt quality = WeightedRandom.choose(
                profile.quality(),
                random
        );

        return generate(subject, camera, style, lighting, quality);

    }

    /**
     * Returns a random style.
     */
    public StylePrompt randomStyle() {
        StylePrompt[] styles = StylePrompt.values();

        return styles[random.nextInt(styles.length)];
    }

    private void validateSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "Subject cannot be null or blank"
            );
        }
    }
}
