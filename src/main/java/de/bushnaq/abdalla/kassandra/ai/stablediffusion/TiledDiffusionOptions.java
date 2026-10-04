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

package de.bushnaq.abdalla.kassandra.ai.stablediffusion;

import lombok.Builder;
import lombok.Getter;

/**
 * Settings for the A1111 Tiled Diffusion extension. Tile dimensions and overlap
 * are latent units, not image pixels. Region Prompt Control is always disabled.
 */
@Getter
@Builder
public class TiledDiffusionOptions {
    @Builder.Default
    private final boolean enabled             = false;
    @Builder.Default
    private final int     imageHeight         = 1024;
    @Builder.Default
    private final int     imageWidth          = 1024;
    @Builder.Default
    private final int     latentTileBatchSize = 4;
    @Builder.Default
    private final int     latentTileHeight    = 32;
    @Builder.Default
    private final int     latentTileOverlap   = 48;
    @Builder.Default
    private final int     latentTileWidth     = 128;
    @Builder.Default
    private final boolean overwriteImageSize  = false;

    /**
     * Calculates a header canvas with a minimum height of 256 image pixels.
     * Width is rounded up to a multiple of 16; the browser handles minor cropping.
     *
     * @param displayWidth  rendered header width in CSS pixels
     * @param displayHeight rendered header height in CSS pixels
     * @return enabled tiled settings matching the rendered header aspect ratio
     * @throws IllegalArgumentException if dimensions are invalid or exceed the extension's limits
     */
    public static TiledDiffusionOptions forHeader(double displayWidth, double displayHeight) {
        if (!Double.isFinite(displayWidth) || !Double.isFinite(displayHeight) || displayWidth <= 0 || displayHeight <= 0) {
            throw new IllegalArgumentException("Header dimensions must be finite and positive");
        }
        double width = Math.max(256, Math.ceil(displayWidth * 256 / displayHeight / 16) * 16);
        if (width > 16384) {
            throw new IllegalArgumentException("Header aspect ratio exceeds the Tiled Diffusion maximum width of 16384 pixels");
        }
        return TiledDiffusionOptions.builder().enabled(true).overwriteImageSize(true)
                .imageWidth((int) width).imageHeight(256).build();
    }
}
