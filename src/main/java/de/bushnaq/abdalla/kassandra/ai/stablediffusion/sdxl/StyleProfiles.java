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


import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.CameraPrompt;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.LightingPrompt;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.QualityPrompt;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.StylePrompt;

import java.util.List;

public final class StyleProfiles {

    private StyleProfiles() {
    }

    private static WeightedRandom.WeightedOption<CameraPrompt> camera(
            CameraPrompt value,
            double weight
    ) {
        return new WeightedRandom.WeightedOption<>(value, weight);
    }

    @SafeVarargs
    private static List<WeightedRandom.WeightedOption<CameraPrompt>> cameras(
            WeightedRandom.WeightedOption<CameraPrompt>... options
    ) {
        return List.of(options);
    }

    public static StyleProfile get(StylePrompt style) {

        return switch (style) {

            case GHIBLI_INSPIRED -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.WIDE_ESTABLISHING, 30),
                            camera(CameraPrompt.FULL_BODY, 20),
                            camera(CameraPrompt.EYE_LEVEL, 30),
                            camera(CameraPrompt.MEDIUM_SHOT, 20)
                    ),

                    lighting(
                            light(LightingPrompt.SOFT_DIFFUSE, 35),
                            light(LightingPrompt.GOLDEN_HOUR, 30),
                            light(LightingPrompt.OVERCAST, 20),
                            light(LightingPrompt.VOLUMETRIC, 15)
                    ),

                    quality(
                            quality(QualityPrompt.PAINTERLY, 45),
                            quality(QualityPrompt.ILLUSTRATION, 35),
                            quality(QualityPrompt.DREAMLIKE, 20)
                    )
            );

            case DARK_FANTASY -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.LOW_ANGLE, 30),
                            camera(CameraPrompt.MEDIUM_SHOT, 25),
                            camera(CameraPrompt.CLOSE_UP, 20),
                            camera(CameraPrompt.DUTCH_ANGLE, 15),
                            camera(CameraPrompt.WIDE_ESTABLISHING, 10)
                    ),

                    lighting(
                            light(LightingPrompt.DRAMATIC_CHIAROSCURO, 35),
                            light(LightingPrompt.MOONLIGHT, 30),
                            light(LightingPrompt.CANDLELIGHT, 20),
                            light(LightingPrompt.RIM_LIGHT, 15)
                    ),

                    quality(
                            quality(QualityPrompt.PAINTERLY, 40),
                            quality(QualityPrompt.CONCEPT_ART, 35),
                            quality(QualityPrompt.HIGHLY_DETAILED, 25)
                    )
            );

            case EPIC_FANTASY -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.WIDE_ESTABLISHING, 35),
                            camera(CameraPrompt.LOW_ANGLE, 25),
                            camera(CameraPrompt.FULL_BODY, 20),
                            camera(CameraPrompt.AERIAL, 20)
                    ),

                    lighting(
                            light(LightingPrompt.GOLDEN_HOUR, 35),
                            light(LightingPrompt.VOLUMETRIC, 30),
                            light(LightingPrompt.RIM_LIGHT, 20),
                            light(LightingPrompt.SOFT_DIFFUSE, 15)
                    ),

                    quality(
                            quality(QualityPrompt.CONCEPT_ART, 40),
                            quality(QualityPrompt.PAINTERLY, 35),
                            quality(QualityPrompt.HIGHLY_DETAILED, 25)
                    )
            );

            case SCI_FI -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.WIDE_ESTABLISHING, 35),
                            camera(CameraPrompt.LOW_ANGLE, 25),
                            camera(CameraPrompt.AERIAL, 20),
                            camera(CameraPrompt.MEDIUM_SHOT, 20)
                    ),

                    lighting(
                            light(LightingPrompt.VOLUMETRIC, 35),
                            light(LightingPrompt.NEON, 30),
                            light(LightingPrompt.RIM_LIGHT, 25),
                            light(LightingPrompt.BLUE_HOUR, 10)
                    ),

                    quality(
                            quality(QualityPrompt.CONCEPT_ART, 40),
                            quality(QualityPrompt.CINEMATIC, 35),
                            quality(QualityPrompt.PHOTOREALISTIC, 25)
                    )
            );

            case RETRO_SCI_FI -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.WIDE_ESTABLISHING, 30),
                            camera(CameraPrompt.MEDIUM_SHOT, 30),
                            camera(CameraPrompt.LOW_ANGLE, 20),
                            camera(CameraPrompt.FULL_BODY, 20)
                    ),

                    lighting(
                            light(LightingPrompt.NEON, 40),
                            light(LightingPrompt.RIM_LIGHT, 25),
                            light(LightingPrompt.VOLUMETRIC, 20),
                            light(LightingPrompt.BLUE_HOUR, 15)
                    ),

                    quality(
                            quality(QualityPrompt.FILM_STILL, 40),
                            quality(QualityPrompt.CINEMATIC, 35),
                            quality(QualityPrompt.ILLUSTRATION, 25)
                    )
            );

            case GOTHIC -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.LOW_ANGLE, 30),
                            camera(CameraPrompt.DUTCH_ANGLE, 25),
                            camera(CameraPrompt.CLOSE_UP, 20),
                            camera(CameraPrompt.WIDE_ESTABLISHING, 25)
                    ),

                    lighting(
                            light(LightingPrompt.DRAMATIC_CHIAROSCURO, 40),
                            light(LightingPrompt.CANDLELIGHT, 30),
                            light(LightingPrompt.MOONLIGHT, 25),
                            light(LightingPrompt.RIM_LIGHT, 5)
                    ),

                    quality(
                            quality(QualityPrompt.PAINTERLY, 45),
                            quality(QualityPrompt.HIGHLY_DETAILED, 30),
                            quality(QualityPrompt.CONCEPT_ART, 25)
                    )
            );

            case GRAPHIC_NOVEL -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.LOW_ANGLE, 25),
                            camera(CameraPrompt.DUTCH_ANGLE, 25),
                            camera(CameraPrompt.CLOSE_UP, 25),
                            camera(CameraPrompt.MEDIUM_SHOT, 25)
                    ),

                    lighting(
                            light(LightingPrompt.DRAMATIC_CHIAROSCURO, 40),
                            light(LightingPrompt.RIM_LIGHT, 25),
                            light(LightingPrompt.NEON, 20),
                            light(LightingPrompt.SOFT_DIFFUSE, 15)
                    ),

                    quality(
                            quality(QualityPrompt.ILLUSTRATION, 50),
                            quality(QualityPrompt.HIGHLY_DETAILED, 30),
                            quality(QualityPrompt.CONCEPT_ART, 20)
                    )
            );

            case SURREAL -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.AERIAL, 30),
                            camera(CameraPrompt.DUTCH_ANGLE, 25),
                            camera(CameraPrompt.WIDE_ESTABLISHING, 25),
                            camera(CameraPrompt.HIGH_ANGLE, 20)
                    ),

                    lighting(
                            light(LightingPrompt.VOLUMETRIC, 35),
                            light(LightingPrompt.SOFT_DIFFUSE, 25),
                            light(LightingPrompt.BLUE_HOUR, 20),
                            light(LightingPrompt.GOLDEN_HOUR, 20)
                    ),

                    quality(
                            quality(QualityPrompt.DREAMLIKE, 50),
                            quality(QualityPrompt.PAINTERLY, 30),
                            quality(QualityPrompt.ILLUSTRATION, 20)
                    )
            );

            case CINEMATIC_SCI_FI -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.WIDE_ESTABLISHING, 30),
                            camera(CameraPrompt.LOW_ANGLE, 25),
                            camera(CameraPrompt.MEDIUM_SHOT, 20),
                            camera(CameraPrompt.AERIAL, 15),
                            camera(CameraPrompt.CLOSE_UP, 10)
                    ),

                    lighting(
                            light(LightingPrompt.VOLUMETRIC, 35),
                            light(LightingPrompt.RIM_LIGHT, 30),
                            light(LightingPrompt.NEON, 20),
                            light(LightingPrompt.BLUE_HOUR, 15)
                    ),

                    quality(
                            quality(QualityPrompt.PHOTOREALISTIC, 45),
                            quality(QualityPrompt.CINEMATIC, 35),
                            quality(QualityPrompt.FILM_STILL, 20)
                    )
            );

            case PAINTERLY_FANTASY -> profile(
                    style,

                    cameras(
                            camera(CameraPrompt.WIDE_ESTABLISHING, 30),
                            camera(CameraPrompt.FULL_BODY, 25),
                            camera(CameraPrompt.MEDIUM_SHOT, 25),
                            camera(CameraPrompt.LOW_ANGLE, 20)
                    ),

                    lighting(
                            light(LightingPrompt.DRAMATIC_CHIAROSCURO, 40),
                            light(LightingPrompt.GOLDEN_HOUR, 25),
                            light(LightingPrompt.CANDLELIGHT, 20),
                            light(LightingPrompt.MOONLIGHT, 15)
                    ),

                    quality(
                            quality(QualityPrompt.PAINTERLY, 60),
                            quality(QualityPrompt.HIGHLY_DETAILED, 25),
                            quality(QualityPrompt.CONCEPT_ART, 15)
                    )
            );
        };
    }

    private static WeightedRandom.WeightedOption<LightingPrompt> light(
            LightingPrompt value,
            double weight
    ) {
        return new WeightedRandom.WeightedOption<>(value, weight);
    }

    @SafeVarargs
    private static List<WeightedRandom.WeightedOption<LightingPrompt>> lighting(
            WeightedRandom.WeightedOption<LightingPrompt>... options
    ) {
        return List.of(options);
    }

    private static StyleProfile profile(
            StylePrompt style,
            List<WeightedRandom.WeightedOption<CameraPrompt>> cameras,
            List<WeightedRandom.WeightedOption<LightingPrompt>> lighting,
            List<WeightedRandom.WeightedOption<QualityPrompt>> quality
    ) {
        return new StyleProfile(style, cameras, lighting, quality);
    }

    private static WeightedRandom.WeightedOption<QualityPrompt> quality(
            QualityPrompt value,
            double weight
    ) {
        return new WeightedRandom.WeightedOption<>(value, weight);
    }

    @SafeVarargs
    private static List<WeightedRandom.WeightedOption<QualityPrompt>> quality(
            WeightedRandom.WeightedOption<QualityPrompt>... options
    ) {
        return List.of(options);
    }
}
