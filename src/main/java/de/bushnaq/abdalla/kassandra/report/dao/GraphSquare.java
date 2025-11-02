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

package de.bushnaq.abdalla.kassandra.report.dao;

import java.awt.*;

public class GraphSquare {
    public Font    font;
    public Integer height;
    public Font    lableFont;
    public Integer width;
    public Integer x;
    public Integer y;

    public GraphSquare() {
    }

    public GraphSquare(Integer x, Integer y, Integer width, Integer height) {
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
    }

    public GraphSquare(Integer x, Integer y, Integer width, Integer height, Font font, Font lableFont) {
        this.x         = x;
        this.y         = y;
        this.width     = width;
        this.height    = height;
        this.font      = font;
        this.lableFont = lableFont;
    }

    public void initPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void initSize(int w, int h) {
        width  = w;
        height = h;
    }

}