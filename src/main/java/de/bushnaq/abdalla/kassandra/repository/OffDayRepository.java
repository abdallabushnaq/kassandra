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

package de.bushnaq.abdalla.kassandra.repository;

import de.bushnaq.abdalla.kassandra.dao.OffDayDAO;
import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OffDayRepository extends ListCrudRepository<OffDayDAO, Long> {

    /**
     * Find overlapping OffDays for a specific user.
     * An OffDay overlaps if its date range (firstDay to lastDay) overlaps with the given date range.
     * Two ranges overlap when:
     * - The start of the new range is before or equal to the end of an existing range AND
     * - The end of the new range is after or equal to the start of an existing range
     *
     * @param user     the user to check for
     * @param firstDay the start date of the range to check
     * @param lastDay  the end date of the range to check
     * @param offDayId ID of the current OffDay (to exclude from results when updating)
     * @return list of overlapping OffDays
     */
    @Query("SELECT o FROM OffDayDAO o WHERE o.user = :user " +
            "AND (:offDayId IS NULL OR o.id != :offDayId) " +
            "AND ((o.firstDay <= :firstDay AND o.lastDay >= :firstDay) OR (o.firstDay <= :lastDay AND o.lastDay >= :lastDay) OR (o.firstDay >= :firstDay AND o.lastDay <= :lastDay) )")
    List<OffDayDAO> findOverlappingOffDays(
            @Param("user") UserDAO user,
            @Param("firstDay") LocalDate firstDay,
            @Param("lastDay") LocalDate lastDay,
            @Param("offDayId") Long offDayId
    );
}