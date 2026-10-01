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

public enum QualityPrompt {

    PHOTOREALISTIC(
            "photorealistic rendering, physically plausible materials, realistic textures, natural surface detail, lifelike appearance"
    ),

    CINEMATIC(
            "cinematic rendering, sophisticated visual composition, realistic materials, subtle film grain, polished movie-quality imagery"
    ),

    HIGHLY_DETAILED(
            "highly detailed rendering, intricate textures, fine surface details, complex environmental detail, crisp definition"
    ),

    PAINTERLY(
            "detailed painterly rendering, visible brushwork, rich textures, nuanced colors, traditional painted artwork"
    ),

    CONCEPT_ART(
            "high-end concept art, detailed production design, sophisticated environment detail, polished professional illustration"
    ),

    ILLUSTRATION(
            "high-quality digital illustration, clean rendering, refined shapes, detailed textures, polished artwork"
    ),

    PHOTOGRAPHIC(
            "professional photography, realistic textures, natural depth of field, detailed surfaces, photographic rendering"
    ),

    FILM_STILL(
            "cinematic film still, realistic visual treatment, subtle film grain, natural imperfections, professional cinematography"
    ),

    SHARP(
            "crisp detailed rendering, sharp subject definition, clear textures, precise edges, well-defined fine details"
    ),

    DREAMLIKE(
            "dreamlike high-detail rendering, soft atmospheric textures, ethereal color transitions, polished finish"
    );

    private final String prompt;

    QualityPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
