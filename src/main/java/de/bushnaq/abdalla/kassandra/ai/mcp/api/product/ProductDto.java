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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.product;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.bushnaq.abdalla.kassandra.dto.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Simplified Product DTO for AI tools.
 * Contains only fields relevant for AI interactions, excluding internal fields like avatarHash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"productId", "name", "created", "updated", "avatarPrompt"})
@Schema(description = "A product in the system")
public class ProductDto {
    @Schema(description = "Stable-diffusion prompt used to generate the product avatar")
    private String         avatarPrompt;
    @Schema(description = "Timestamp when the product was created (ISO 8601)")
    private OffsetDateTime created;
    @Schema(description = "Unique product name")
    private String         name;
    @Schema(description = "Unique product identifier; use this ID in subsequent operations")
    private Long           productId;
    @Schema(description = "Timestamp when the product was last updated (ISO 8601)")
    private OffsetDateTime updated;


    public ProductDto(Long productId, String name, OffsetDateTime updated, OffsetDateTime created, String avatarPrompt) {
        this.productId    = productId;
        this.name         = name;
        this.updated      = updated;
        this.created      = created;
        this.avatarPrompt = avatarPrompt;
    }

    public static ProductDto from(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getCreated(),
                product.getUpdated(),
                product.getDefaultAvatarPrompt()
        );
    }

    public Product toProduct() {
        Product product = new Product();
        product.setId(this.productId);
        product.setName(this.name);
        product.setCreated(this.created);
        product.setUpdated(this.updated);
        return product;
    }
}
