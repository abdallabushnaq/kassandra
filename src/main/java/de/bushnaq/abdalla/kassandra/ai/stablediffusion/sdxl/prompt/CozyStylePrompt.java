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

import java.util.concurrent.ThreadLocalRandom;

public enum CozyStylePrompt {

    GHIBLI_INSPIRED(
            ", Studio Ghibli"
//            """
//                    hand-painted Japanese fantasy animation aesthetic,
//                    inspired by the visual language of Hayao Miyazaki and Studio Ghibli,
//                    lush watercolor backgrounds, soft cel shading, expressive characters,
//                    whimsical atmosphere, warm natural colors, detailed environmental art,
//                    gentle diffuse lighting, painterly textures, nostalgic cinematic feel
//                    """
    ),

    BEATRIX_POTTER(", Beatrix Potter"
//            "delicate English storybook illustration aesthetic, inspired by the watercolor illustrations "
//                    + "of Beatrix Potter, "
//                    + "finely detailed natural environments, charming woodland animals, wildflowers, "
//                    + "small cottages, gardens, winding paths, intricate botanical details, "
//                    + "soft watercolor washes, fine ink linework, intimate handcrafted appearance, "
//                    + "whimsical countryside atmosphere"
    ),

    RICHARD_SCARRY(", Richard Scarry"
//            "playful vintage children's book illustration aesthetic, inspired by the illustrations "
//                    + "of Richard Scarry, "
//                    + "busy miniature environments filled with charming characters and small activities, "
//                    + "colorful buildings, vehicles, shops, animals and everyday objects, "
//                    + "expressive simplified forms, clean hand-drawn outlines, cheerful colors, "
//                    + "high visual density, delightful storytelling details"
    ),

    TOVE_JANSSON("Tove Jansson"
//            "whimsical Scandinavian children's illustration aesthetic, inspired by the artwork "
//                    + "of Tove Jansson, "
//                    + "charming fantastical landscapes, expressive small characters, unusual plants, "
//                    + "cozy architecture, winding paths, forests and seaside environments, "
//                    + "delicate ink linework, restrained watercolor colors, organic shapes, "
//                    + "quietly magical atmosphere, sophisticated simplicity"
    ),

    MARY_BLAIR(
            "Mary Blair"
//            "stylized mid-century storybook illustration aesthetic, inspired by the color design "
//                    + "and visual language of Mary Blair, "
//                    + "simplified whimsical environments, imaginative architecture, playful landscapes, "
//                    + "colorful trees and plants, charming small characters, geometric organic shapes, "
//                    + "bold harmonious color relationships, decorative composition, "
//                    + "dreamlike children's book atmosphere"
    ),

    EYVIND_EARLE(
            "Eyvind Earle"
//            "highly stylized storybook landscape illustration aesthetic, inspired by the artwork "
//                    + "of Eyvind Earle, "
//                    + "layered forests, elegant trees, rolling hills, distant villages, "
//                    + "ornamental foliage, stylized architecture, intricate repeating shapes, "
//                    + "decorative silhouettes, rich natural colors, simplified perspective, "
                    + "beautifully composed panoramic scenery"
    ),

    MOEBIUS_ADVENTURE(
            "Moebius"
//            "bright imaginative European graphic illustration aesthetic, inspired by the world-building "
//                    + "and linework of Moebius, "
//                    + "vast fantastical landscapes, unusual architecture, strange but friendly creatures, "
//                    + "small settlements, curious machines, winding roads and distant explorers, "
//                    + "precise delicate linework, clean color areas, imaginative environmental details, "
//                    + "sense of discovery and boundless exploration"
    ),

//    PYOETIC_PICTUREBOOK(
//            "ornamental contemporary picture-book illustration aesthetic, "
//                    + "inspired by richly detailed European children's books, "
//                    + "panoramic storybook landscapes filled with many small scenes, "
//                    + "tiny characters, houses, gardens, animals, plants and decorative objects, "
//                    + "hand-drawn linework, textured painted surfaces, harmonious colors, "
//                    + "layered visual storytelling, playful handcrafted detail"
//    ),

    //    FANTASTICAL_WATERCOLOR(
//            "classic European fantasy picture-book illustration aesthetic, "
//                    + "inspired by detailed watercolor and ink children's book illustrations, "
//                    + "wide enchanted landscapes populated with many small characters, "
//                    + "villages, forests, flowers, animals, cottages and whimsical objects, "
//                    + "fine pen details, transparent watercolor washes, delicate textures, "
//                    + "gentle colors, intricate environmental storytelling, charming handcrafted feel"
//    ),
    BRAMBLY_HEDGE(", Brambly Hedge"
//            "detailed English woodland storybook illustration aesthetic, inspired by the "
//                    + "illustrations of Jill Barklem and Brambly Hedge, "
//                    + "charming miniature woodland villages, cozy cottages, winding paths, "
//                    + "lush gardens, flowering plants, woodland animals, tiny everyday objects "
//                    + "and many small environmental details, "
//                    + "delicate pen and watercolor illustration, finely rendered textures, "
//                    + "warm natural colors, handcrafted children's book aesthetic, "
//                    + "rich panoramic environmental storytelling, charming and inviting atmosphere"
    );

    private final String prompt;

    CozyStylePrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Returns the style's image-generation prompt block.
     *
     * @return the style prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Selects a Cozy style with uniform probability.
     *
     * @return a randomly selected style
     */
    public static CozyStylePrompt randomStyle() {
        CozyStylePrompt[] styles = values();
        return styles[ThreadLocalRandom.current().nextInt(styles.length)];
    }

    public enum StylePrompt {

    }
}
