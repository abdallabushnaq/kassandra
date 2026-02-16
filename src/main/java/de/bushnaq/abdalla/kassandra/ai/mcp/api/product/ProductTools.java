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
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionException;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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
    private static final String                 PRODUCT_FIELDS             = """
            productId (number): Unique identifier of the product, used to map versions to a product,
            name (string): The product name,
            created (ISO 8601 datetime string): Timestamp when the product was created,
            updated (ISO 8601 datetime string): Timestamp when the product was last updated,
            avatarPrompt (string): Default avatar prompt for stable-diffusion to generate.
            """;
    private static final String                 RETURNS_PRODUCT_ARRAY_JSON = "Returns: JSON array of Product objects. Each Product contains: " + PRODUCT_FIELDS;
    private static final String                 RETURNS_PRODUCT_JSON       = "Returns: JSON Product object with fields: " + PRODUCT_FIELDS;
    @Autowired
    private              JsonMapper             jsonMapper;
    @Autowired
    @Qualifier("aiProductApi")
    private              ProductApi             productApi;
    @Autowired
    protected            StableDiffusionService stableDiffusionService;

    @Tool(description = "Create a new product. " +
            "IMPORTANT: The returned JSON includes an 'productId' field - you MUST extract and use this ID for subsequent operations (like deleting this product). " +
            RETURNS_PRODUCT_JSON)
    public String createProduct(
            @ToolParam(description = "The product name (must be unique)") String name,
            @ToolParam(description = "(Optional) The product avatar stable-diffusion prompt. If null or empty, a default prompt will be generated.") String avatarPrompt) {
        ToolActivityContextHolder.reportActivity("Creating product with name: " + name);
        try {
            Product product = new Product();
            product.setName(name);

            if (avatarPrompt == null || avatarPrompt.isEmpty()) {
                avatarPrompt = product.getDefaultAvatarPrompt();
            }
            {
                GeneratedImageResult image     = null;
                long                 startTime = System.currentTimeMillis();
                if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
                    try {
                        image = generateProductAvatar(name);
                    } catch (StableDiffusionException e) {
                        System.err.println("Failed to generate image for product " + name + ": " + e.getMessage());
                        image = stableDiffusionService.generateDefaultAvatar("cube");
                    }
                } else {
                    log.warn("Stable Diffusion not available, using default avatar for product: " + name);
                    image = stableDiffusionService.generateDefaultAvatar("cube");
                }
                product.setAvatarHash(AvatarUtil.computeHash(image.getResizedImage()));
            }
            Product    savedProduct = productApi.persist(product);
            ProductDto productDto   = ProductDto.from(savedProduct);
            ToolActivityContextHolder.reportActivity("created product '" + productDto.getName() + "' with ID: " + savedProduct.getId());
            return jsonMapper.writeValueAsString(productDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error creating product: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a product by productId. " +
            "IMPORTANT: You must provide the exact productId. If you just created a product, use the 'productId' field from the createProduct response. " +
            "Do NOT guess or use a different productId. " +
            "Returns: Success message (string) confirming deletion")
    public String deleteProduct(
            @ToolParam(description = "The productId") Long id) {
        try {
            // First, get the product details to log what we're about to delete
            Product productToDelete = productApi.getById(id);
            if (productToDelete != null) {
                ToolActivityContextHolder.reportActivity("Deleting product '" + productToDelete.getName() + "' (ID: " + id + ")");
            } else {
                ToolActivityContextHolder.reportActivity("Attempting to delete product with ID: " + id + " (product not found)");
            }

            productApi.deleteById(id);
            ToolActivityContextHolder.reportActivity("Successfully deleted product with ID: " + id);
            return "Product with ID " + id + " deleted successfully";
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error deleting product: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private @NonNull GeneratedImageResult generateProductAvatar(String name) throws StableDiffusionException {
        String prompt = Product.getDefaultAvatarPrompt(name);
        log.trace("Generating image for product: " + name + " with prompt: " + prompt);
        GeneratedImageResult image = stableDiffusionService.generateImageWithOriginal(prompt);
        return image;
    }

    @Tool(description = "Get a list of all products accessible to the current user. " + RETURNS_PRODUCT_ARRAY_JSON)
    public String getAllProducts() {
        try {
            List<Product> products = productApi.getAll();
            ToolActivityContextHolder.reportActivity("read " + products.size() + " products.");
            List<ProductDto> productDtos = products.stream()
                    .map(ProductDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(productDtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting all products: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific product by its productId. " + RETURNS_PRODUCT_JSON)
    public String getProductById(
            @ToolParam(description = "The productId") Long productId) {
//        ToolActivityContextHolder.reportActivity("Getting product with ID: " + id);
        try {
            Product product = productApi.getById(productId);
            if (product != null) {
                ToolActivityContextHolder.reportActivity("read product : " + product.getName() + ".");
                ProductDto productDto = ProductDto.from(product);
                return jsonMapper.writeValueAsString(productDto);
            }
            ToolActivityContextHolder.reportActivity("failed to find product by ID: " + productId + ".");
            return "Product not found with ID: " + productId;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting product by ID: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific product by its name. " + RETURNS_PRODUCT_JSON)
    public String getProductByName(String name) {
//        ToolActivityContextHolder.reportActivity("Getting product with name: " + name);
        try {
            Optional<Product> product = productApi.getByName(name);
            if (product.isPresent()) {
                ToolActivityContextHolder.reportActivity("found product: " + product.get().getName() + ".");
                ProductDto productDto = ProductDto.from(product.get());
                return jsonMapper.writeValueAsString(productDto);
            }
            ToolActivityContextHolder.reportActivity("failed to find product by name: " + name + ".");
            return "Product not found with name: " + name;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting product by name: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing product by its productId. " + RETURNS_PRODUCT_JSON)
    public String updateProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The new product name") String name) {
//        ToolActivityContextHolder.reportActivity("Updating product " + id + " with name: " + name);
        try {
            Product product = productApi.getById(productId);
            if (product == null) {
                ToolActivityContextHolder.reportActivity("failed to update product by ID: " + productId + ".");
                return "Product not found with ID: " + productId;
            }
            product.setName(name);
            productApi.update(product);
            ProductDto productDto = ProductDto.from(product);
            ToolActivityContextHolder.reportActivity("updated product: " + productDto.getName() + ".");
            return jsonMapper.writeValueAsString(productDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error updating product: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
