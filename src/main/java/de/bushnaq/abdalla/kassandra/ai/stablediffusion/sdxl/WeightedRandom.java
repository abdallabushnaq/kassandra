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


import java.util.List;
import java.util.Random;

public final class WeightedRandom {

    private WeightedRandom() {
    }

    public static <T> T choose(
            List<WeightedOption<T>> options,
            Random random
    ) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options cannot be empty");
        }

        double totalWeight = options.stream()
                .mapToDouble(WeightedOption::weight)
                .sum();

        if (totalWeight <= 0) {
            throw new IllegalArgumentException(
                    "Total weight must be greater than zero"
            );
        }

        double value = random.nextDouble() * totalWeight;

        for (WeightedOption<T> option : options) {
            value -= option.weight();

            if (value <= 0) {
                return option.value();
            }
        }

        // Protect against floating-point rounding.
        return options.get(options.size() - 1).value();
    }

    public record WeightedOption<T>(
            T value,
            double weight
    ) {
        public WeightedOption {
            if (value == null) {
                throw new IllegalArgumentException("Value cannot be null");
            }

            if (weight < 0) {
                throw new IllegalArgumentException(
                        "Weight cannot be negative"
                );
            }
        }
    }
}
