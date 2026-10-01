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

public enum CameraPrompt {

    WIDE_ESTABLISHING(
            "wide establishing shot, expansive environment, strong sense of scale, cinematic composition"
    ),

    LOW_ANGLE(
            "low-angle camera, looking upward, imposing perspective, dramatic sense of scale"
    ),

    HIGH_ANGLE(
            "high-angle camera, looking downward, expansive view of the environment, dynamic perspective"
    ),

    EYE_LEVEL(
            "eye-level camera, natural perspective, balanced composition, immersive viewpoint"
    ),

    CLOSE_UP(
            "close-up shot, tightly framed subject, intimate composition, strong facial and surface detail"
    ),

    MEDIUM_SHOT(
            "medium shot, subject framed from the waist up, balanced composition, natural perspective"
    ),

    FULL_BODY(
            "full-body shot, entire subject visible, balanced framing, clear silhouette"
    ),

    OVER_THE_SHOULDER(
            "over-the-shoulder camera angle, foreground subject partially visible, cinematic depth and perspective"
    ),

    DUTCH_ANGLE(
            "Dutch angle, tilted camera, dynamic diagonal composition, dramatic and unsettling perspective"
    ),

    AERIAL(
            "aerial camera perspective, viewed from high above, expansive environment, cinematic spatial composition"
    );

    private final String prompt;

    CameraPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
