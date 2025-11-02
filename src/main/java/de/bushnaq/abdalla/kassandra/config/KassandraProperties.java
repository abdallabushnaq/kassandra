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

package de.bushnaq.abdalla.kassandra.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration holder for Kassandra-related settings.
 * Provides static access to configuration values for use in DTOs.
 */
@Component
public class KassandraProperties {

    /**
     * -- GETTER --
     * Get the number of years to look ahead for holidays.
     *
     * @return the number of years to look ahead for holidays
     */
    @Getter
    private static long holidayLookAheadMonths = 2;

    /**
     * Set the look ahead value from application.properties.
     * Spring will inject this value at startup using the @Value annotation.
     *
     * @param value the number of years to look ahead for holidays
     */
    @Value("${kassandra.holidays.look.ahead.months:24}")
    public void setHolidayLookAheadMonths(long value) {
        holidayLookAheadMonths = value;
    }
}


