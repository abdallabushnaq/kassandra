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

public class CalendarElement {
    private Font    font;
    private Integer height;
    private Integer width;
    private Integer y;

    public CalendarElement(Font font, Integer y, Integer width, Integer height) {
        this.setFont(font);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);
    }

    public Font getFont() {
        return font;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getY() {
        return y;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setY(Integer y) {
        this.y = y;
    }
}