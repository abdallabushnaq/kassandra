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

public enum StylePrompt {

    GHIBLI_INSPIRED(
            "hand-painted Japanese fantasy animation aesthetic, "
                    + "inspired by the visual language of Hayao Miyazaki and Studio Ghibli, "
                    + "lush watercolor backgrounds, soft cel shading, expressive characters, "
                    + "whimsical atmosphere, warm natural colors, detailed environmental art, "
                    + "gentle diffuse lighting, painterly textures, nostalgic cinematic feel"
    ),

    DARK_FANTASY(
            "dark gothic fantasy, inspired by the dark fantasy artwork of Frank Frazetta "
                    + "and the dramatic fantasy painting of Gerald Brom, "
                    + "towering medieval architecture, ancient ruins, dramatic atmosphere, "
                    + "mysterious mood, intricate ornamental details, rich earthy colors, "
                    + "painterly realism"
    ),

    EPIC_FANTASY(
            "epic high fantasy illustration, inspired by the fantasy artwork of "
                    + "Frank Frazetta, John Howe and Alan Lee, "
                    + "majestic landscapes, ancient forests, grand castles, mythical atmosphere, "
                    + "richly detailed environments, dramatic clouds, saturated natural colors, "
                    + "intricate costume and architecture design"
    ),

    SCI_FI(
            "premium science fiction concept art, inspired by the futuristic artwork of "
                    + "Syd Mead and Chris Foss, "
                    + "futuristic architecture, advanced technology, enormous structures, "
                    + "sophisticated production design, intricate mechanical details, "
                    + "epic sense of scale"
    ),

    RETRO_SCI_FI(
            "retro science fiction aesthetic, inspired by the artwork of Chris Foss, "
                    + "Moebius and Syd Mead, "
                    + "vintage analog technology, chunky futuristic machinery, "
                    + "retro-futuristic design, neon accents, practical effects aesthetic, "
                    + "nostalgic cinematic atmosphere"
    ),

    GOTHIC(
            "gothic dark fantasy, inspired by the macabre artwork of Zdzisław Beksiński "
                    + "and the gothic fantasy art of Gerald Brom, "
                    + "ornate stone architecture, ancient ruins, dramatic gothic atmosphere, "
                    + "elaborate details, mysterious and haunting mood, dark romantic aesthetic"
    ),

    GRAPHIC_NOVEL(
            "stylized graphic novel aesthetic, inspired by the sequential art of "
                    + "Moebius, Frank Miller and Mike Mignola, "
                    + "bold ink outlines, dynamic composition, dramatic shadows, "
                    + "graphic shapes, expressive lighting, detailed linework, textured paper"
    ),

    SURREAL(
            "surreal dreamscape, inspired by the surrealist artwork of Salvador Dalí, "
                    + "René Magritte and Zdzisław Beksiński, "
                    + "impossible architecture, floating landscapes, strange organic forms, "
                    + "dreamlike atmosphere, unusual perspective, mysterious symbolism, "
                    + "otherworldly colors"
    ),

    CINEMATIC_SCI_FI(
            "photorealistic cinematic science fiction, inspired by the production design "
                    + "of Syd Mead and the cinematic visual language of Ridley Scott's science fiction, "
                    + "sophisticated production design, realistic materials, advanced technology, "
                    + "atmospheric environments, blockbuster movie aesthetic"
    ),

    PAINTERLY_FANTASY(
            "dark painterly fantasy, inspired by the classical fantasy paintings of "
                    + "Frank Frazetta, Boris Vallejo and Gerald Brom, "
                    + "classical oil painting technique, visible brushwork, rich textured surfaces, "
                    + "dramatic chiaroscuro, muted jewel tones, ancient mythology, "
                    + "dramatic and mysterious mood"
    );

    private final String prompt;

    StylePrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
