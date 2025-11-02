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

public class CalendarMilestoneElement {
    public Font    flagFont;
    public Integer flagHeight;
    public Integer flagY;
    public Font    font;
    public Integer height;
    public Integer width;
    public Integer y;

    public CalendarMilestoneElement(Integer x, Integer y, Integer width, Integer height, Font font, Integer flagY, Integer flagHeight, Font flagFont) {
        this.y          = y;
        this.width      = width;
        this.height     = height;
        this.font       = font;
        this.flagY      = flagY;
        this.flagHeight = flagHeight;
        this.flagFont   = flagFont;
    }
}