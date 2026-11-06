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

package de.bushnaq.abdalla.kassandra.report.theme;

import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("UnitTest")
class ColorPaletteTest {
    private static final int BORDER_WIDTH = 2;
    private static final int COLUMNS      = 4;
    private static final int TILE_SIZE    = 64;

    private void writeColorGrid(Path outputPath, Color[] colors) throws IOException {
        int           columns  = Math.min(COLUMNS, Math.max(colors.length, 1));
        int           rows     = Math.max(1, (colors.length + columns - 1) / columns);
        BufferedImage image    = new BufferedImage(columns * TILE_SIZE, rows * TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D    graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            for (int i = 0; i < colors.length; i++) {
                int x = (i % columns) * TILE_SIZE;
                int y = (i / columns) * TILE_SIZE;

                graphics.setColor(Color.WHITE);
                graphics.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                graphics.setColor(colors[i]);
                graphics.fillRect(x + BORDER_WIDTH, y + BORDER_WIDTH,
                        TILE_SIZE - 2 * BORDER_WIDTH, TILE_SIZE - 2 * BORDER_WIDTH);
            }
        } finally {
            graphics.dispose();
        }

        assertTrue(ImageIO.write(image, "png", outputPath.toFile()), "PNG writer is unavailable");
    }

    /**
     * Writes swatch images for the active color palette to {@code references/theme}.
     *
     * @throws IOException if the output directory or a PNG cannot be written
     */
    @Test
    void writeColorPaletteSamples() throws IOException {
        Path outputDirectory = Path.of("references", "theme");
        Files.createDirectories(outputDirectory);

        writeColorGrid(outputDirectory.resolve("light-theme-colors.png"), Theme.COLOR_PALLET);
        writeColorGrid(outputDirectory.resolve("dark-theme-colors.png"), Theme.COLOR_PALLET);
    }
}
