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

package de.bushnaq.abdalla.kassandra.mcp.dto;

import de.bushnaq.abdalla.kassandra.dto.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Simplified Product DTO for MCP (Model Context Protocol) server.
 * Contains only fields relevant for AI interactions, excluding internal fields like avatarHash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpProduct {
    private OffsetDateTime created;
    private Long           id;
    private String         name;
    private OffsetDateTime updated;

    /**
     * Convert from full Product DTO to MCP Product DTO
     *
     * @param product the full Product object
     * @return McpProduct with only AI-relevant fields
     */
    public static McpProduct from(Product product) {
        if (product == null) {
            return null;
        }
        return new McpProduct(
                product.getCreated(),
                product.getId(),
                product.getName(),
                product.getUpdated()
        );
    }

    /**
     * Convert to full Product DTO (for API calls)
     *
     * @return Product object that can be used with ProductApi
     */
    public Product toProduct() {
        Product product = new Product();
        product.setId(this.id);
        product.setName(this.name);
        product.setCreated(this.created);
        product.setUpdated(this.updated);
        return product;
    }
}
