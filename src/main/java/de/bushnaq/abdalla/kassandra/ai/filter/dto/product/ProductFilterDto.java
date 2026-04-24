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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.product;

import java.util.UUID;
import de.bushnaq.abdalla.kassandra.ai.filter.dto.availability.AvailabilityFilterDto;
import de.bushnaq.abdalla.kassandra.dto.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Filter-only DTO for {@link Product}.
 *
 * <p>Exposes only the fields the LLM needs to evaluate a filter predicate
 * ({@code name}, {@code created}, {@code updated}).  Using a dedicated DTO
 * keeps the filter layer decoupled from the full entity and makes every AI
 * filter test look the same (parallel to
 * {@link AvailabilityFilterDto}).</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductFilterDto {

    private OffsetDateTime created;
    private UUID           id;
    private String         name;
    private OffsetDateTime updated;

    /**
     * Creates a {@code ProductFilterDto} from a {@link Product} entity.
     */
    public static ProductFilterDto from(Product product) {
        ProductFilterDto dto = new ProductFilterDto();
        dto.id      = product.getId();
        dto.name    = product.getName();
        dto.created = product.getCreated();
        dto.updated = product.getUpdated();
        return dto;
    }
}

