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

import de.bushnaq.abdalla.kassandra.ai.mcp.ToolActivityContextHolder;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring AI native tool implementations for Product operations.
 * <p>
 * Products contain Versions, and each Version can have multiple Features.
 * To list all features for a product, first use VersionTools to get all versions for the product,
 * then use FeatureTools to get features for each version.
 * <p>
 * Example traversal:
 * 1. Use ProductTools.getAllProducts() to list products.
 * 2. Use VersionTools.getAllVersionsByProductId(productId) to list versions for a product.
 * 3. Use FeatureTools.getAllFeaturesByVersionId(versionId) to list features for a version.
 * <p>
 * Uses @Tool annotation for automatic tool registration with ChatClient.
 */
@Component
@Slf4j
public class ProductTools {

    /**
     * Schema description for Product objects returned by tools.
     * Describes the JSON structure and field meanings.
     * Used in @Tool annotations - must be a compile-time constant.
     */
    private static final String     PRODUCT_FIELDS             =
            "id (number): Unique identifier of the product, used to map versions to a product, " +
                    "name (string): The product name, " +
                    "created (ISO 8601 datetime string): Timestamp when the product was created, " +
                    "updated (ISO 8601 datetime string): Timestamp when the product was last updated";
    private static final String     RETURNS_PRODUCT_ARRAY_JSON = "Returns: JSON array of Product objects. Each Product contains: " + PRODUCT_FIELDS;
    private static final String     RETURNS_PRODUCT_JSON       = "Returns: JSON Product object with fields: " + PRODUCT_FIELDS;
    @Autowired
    private              JsonMapper jsonMapper;

    @Autowired
    @Qualifier("aiProductApi")
    private ProductApi productApi;

    @Tool(description = "Create a new product (requires USER or ADMIN role). " + RETURNS_PRODUCT_JSON)
    public String createProduct(
            @ToolParam(description = "The product name (must be unique)") String name) {
        ToolActivityContextHolder.reportActivity("Creating product with name: " + name);
        try {
            Product product = new Product();
            product.setName(name);
            Product    savedProduct = productApi.persist(product);
            ProductDto productDto   = ProductDto.from(savedProduct);
            ToolActivityContextHolder.reportActivity("Product created: " + productDto.getName());
            return jsonMapper.writeValueAsString(productDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error creating product: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a product by ID (requires access or admin role). " +
            "Returns: Success message (string) confirming deletion")
    public String deleteProduct(
            @ToolParam(description = "The product ID") Long id) {
        ToolActivityContextHolder.reportActivity("Deleting product with ID: " + id);
        try {
            productApi.deleteById(id);
            ToolActivityContextHolder.reportActivity("Product deleted: " + id);
            return "Product with ID " + id + " deleted successfully";
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error deleting product: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all products accessible to the current user (Admin sees all). " + RETURNS_PRODUCT_ARRAY_JSON)
    public String getAllProducts() {
        ToolActivityContextHolder.reportActivity("Getting all products...");
        try {
            List<Product> products = productApi.getAll();
            ToolActivityContextHolder.reportActivity("Found " + products.size() + " products.");
            List<ProductDto> productDtos = products.stream()
                    .map(ProductDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(productDtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting all products: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific product by its ID (requires access or admin role). " + RETURNS_PRODUCT_JSON)
    public String getProductById(
            @ToolParam(description = "The product ID") Long id) {
        ToolActivityContextHolder.reportActivity("Getting product with ID: " + id);
        try {
            Product product = productApi.getById(id);
            if (product != null) {
                ToolActivityContextHolder.reportActivity("Product found: " + product.getName());
                ProductDto productDto = ProductDto.from(product);
                return jsonMapper.writeValueAsString(productDto);
            }
            ToolActivityContextHolder.reportActivity("Product not found with ID: " + id);
            return "Product not found with ID: " + id;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting product by ID: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public String getProductByName(String name) {
        ToolActivityContextHolder.reportActivity("Getting product with name: " + name);
        try {
            Optional<Product> product = productApi.getByName(name);
            if (product.isPresent()) {
                ToolActivityContextHolder.reportActivity("Product found: " + product.get().getName());
                ProductDto productDto = ProductDto.from(product.get());
                return jsonMapper.writeValueAsString(productDto);
            }
            ToolActivityContextHolder.reportActivity("Product not found with name: " + name);
            return "Product not found with name: " + name;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting product by name: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing product (requires access or admin role). " + RETURNS_PRODUCT_JSON)
    public String updateProduct(
            @ToolParam(description = "The product ID") Long id,
            @ToolParam(description = "The new product name") String name) {
        ToolActivityContextHolder.reportActivity("Updating product " + id + " with name: " + name);
        try {
            Product product = productApi.getById(id);
            if (product == null) {
                ToolActivityContextHolder.reportActivity("Product not found with ID: " + id);
                return "Product not found with ID: " + id;
            }
            product.setName(name);
            productApi.update(product);
            ProductDto productDto = ProductDto.from(product);
            ToolActivityContextHolder.reportActivity("Product updated: " + productDto.getName());
            return jsonMapper.writeValueAsString(productDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error updating product: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
