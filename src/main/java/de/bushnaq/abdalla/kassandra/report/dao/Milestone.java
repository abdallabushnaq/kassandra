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
import java.time.LocalDate;

public class Milestone implements Comparable<Milestone> {
    public Color     color;
    public boolean   hidden;
    public String    name;
    public boolean   nowLine = false;
    public String    symbol;
    public LocalDate time;

    public Milestone(LocalDate time, String symbol, String name, Color color) {
        this.time   = time;
        this.symbol = symbol;
        this.name   = name;
        this.color  = color;
    }

    public Milestone(LocalDate time, String symbol, String name, Color color, boolean nowLine) {
        this.time    = time;
        this.symbol  = symbol;
        this.name    = name;
        this.color   = color;
        this.nowLine = nowLine;
    }

    @Override
    public int compareTo(Milestone o) {
        return time.compareTo(o.time);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Milestone) {
            return time.equals(((Milestone) obj).time);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return time.hashCode();
    }
}
