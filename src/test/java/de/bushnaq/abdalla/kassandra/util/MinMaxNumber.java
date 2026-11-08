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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.util;

/**
 * Represents an integer range used when generating randomized test data.
 *
 * @param min the minimum value
 * @param max the inclusive maximum value
 */
public record MinMaxNumber(int min, int max) {
    /**
     * Creates a non-negative inclusive range.
     *
     * @param min the minimum value
     * @param max the inclusive maximum value
     * @throws IllegalArgumentException if either bound is negative or {@code max} is less than {@code min}
     */
    public MinMaxNumber {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Min/max range must satisfy 0 <= min <= max");
        }
    }
}
