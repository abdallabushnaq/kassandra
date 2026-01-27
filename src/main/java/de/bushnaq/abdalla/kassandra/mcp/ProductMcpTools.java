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

package de.bushnaq.abdalla.kassandra.mcp;

import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.mcp.dto.McpProduct;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers Product API endpoints as MCP tools
 */
@Component
@Slf4j
public class ProductMcpTools {

    /**
     * Schema description for Product objects returned by MCP tools.
     * Describes the JSON structure and field meanings.
     */
    private static final String PRODUCT_SCHEMA_DESCRIPTION = """
            id (number): Unique identifier of the product, \
            name (string): The product name, \
            created (ISO 8601 datetime string): Timestamp when the product was created, \
            updated (ISO 8601 datetime string): Timestamp when the product was last updated\
            """;

    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    private McpServer  mcpServer;
    @Autowired
    @Qualifier("mcpProductApi")
    private ProductApi productApi;

    private void registerDeleteProduct() {
        McpTool tool = McpTool.create(
                "delete_product",
                """
                        Delete a product by ID (requires access or admin role). \
                        Returns: Success message (string) confirming deletion\
                        """,
                List.of(
                        new McpTool.McpToolParameter("id", "integer", "The product ID", true)
                )
        );

        mcpServer.registerTool(tool, args -> {
            try {
                Long id = Long.valueOf(args.get("id").toString());

                // Delete via API - security is enforced at the controller level
                productApi.deleteById(id);

                return "Product with ID " + id + " deleted successfully";
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    private void registerGetAllProducts() {
        McpTool tool = McpTool.create(
                "get_all_products",
                """
                        Get a list of all products accessible to the current user (Admin sees all). \
                        Returns: JSON array of Product objects. Each Product contains: \
                        """ + PRODUCT_SCHEMA_DESCRIPTION,
                List.of()
        );

        mcpServer.registerTool(tool, _args -> {
            try {
                // Get products via API - security filtering is done at the controller level
                List<Product> products = productApi.getAll();
                // Convert to MCP DTOs
                List<McpProduct> mcpProducts = products.stream()
                        .map(McpProduct::from)
                        .collect(Collectors.toList());
                return jsonMapper.writeValueAsString(mcpProducts);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    private void registerGetProductById() {
        McpTool tool = McpTool.create(
                "get_product_by_id",
                """
                        Get a specific product by its ID (requires access or admin role). \
                        Returns: JSON Product object with fields: \
                        """ + PRODUCT_SCHEMA_DESCRIPTION,
                List.of(
                        new McpTool.McpToolParameter("id", "integer", "The product ID", true)
                )
        );

        mcpServer.registerTool(tool, args -> {
            try {
                Long    id      = Long.valueOf(args.get("id").toString());
                Product product = productApi.getById(id);
                if (product != null) {
                    McpProduct mcpProduct = McpProduct.from(product);
                    return jsonMapper.writeValueAsString(mcpProduct);
                }
                return "Product not found with ID: " + id;
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    private void registerSaveProduct() {
        McpTool tool = McpTool.create(
                "save_product",
                """
                        Create a new product (requires USER or ADMIN role). \
                        Returns: JSON Product object with fields: \
                        """ + PRODUCT_SCHEMA_DESCRIPTION + " (id is auto-generated)",
                List.of(
                        new McpTool.McpToolParameter("name", "string", "The product name (must be unique)", true)
                )
        );

        mcpServer.registerTool(tool, args -> {
            try {
                String name = args.get("name").toString();

                // Create and save the product via API - security and access control are enforced at the controller level
                Product product = new Product();
                product.setName(name);
                Product savedProduct = productApi.persist(product);

                McpProduct mcpProduct = McpProduct.from(savedProduct);
                return jsonMapper.writeValueAsString(mcpProduct);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }

    @PostConstruct
    public void registerTools() {
        registerGetAllProducts();
        registerGetProductById();
        registerSaveProduct();
        registerUpdateProduct();
        registerDeleteProduct();
    }

    private void registerUpdateProduct() {
        McpTool tool = McpTool.create(
                "update_product",
                """
                        Update an existing product (requires access or admin role). \
                        Returns: JSON Product object with fields: \
                        """ + PRODUCT_SCHEMA_DESCRIPTION + " (name and updated are refreshed)",
                List.of(
                        new McpTool.McpToolParameter("id", "integer", "The product ID", true),
                        new McpTool.McpToolParameter("name", "string", "The new product name", true)
                )
        );

        mcpServer.registerTool(tool, args -> {
            try {
                Long   id   = Long.valueOf(args.get("id").toString());
                String name = args.get("name").toString();

                // Get the existing product via API
                Product product = productApi.getById(id);
                if (product == null) {
                    return "Product not found with ID: " + id;
                }

                // Update the product name
                product.setName(name);

                // Update via API - security and name uniqueness checks are enforced at the controller level
                productApi.update(product);

                McpProduct mcpProduct = McpProduct.from(product);
                return jsonMapper.writeValueAsString(mcpProduct);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        });
    }
}
