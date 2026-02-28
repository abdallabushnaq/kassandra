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

import de.bushnaq.abdalla.kassandra.ai.mcp.KassandraToolCallResultConverter;
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

import java.util.List;
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
    @Autowired
    @Qualifier("aiProductApi")
    private   ProductApi             productApi;
    @Autowired
    protected StableDiffusionService stableDiffusionService;

    @Tool(description = "Create a new product. Returns the created Product including its productId.", resultConverter = KassandraToolCallResultConverter.class)
    public ProductDto createProduct(
            @ToolParam(description = "Unique product name") String name,
            @ToolParam(description = "Stable-diffusion prompt for the avatar", required = false) String avatarPrompt) {
        Product product = new Product();
        product.setName(name);
        if (avatarPrompt == null || avatarPrompt.isEmpty()) {
            avatarPrompt = product.getDefaultAvatarPrompt();
        }
        GeneratedImageResult image;
        if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
            try {
                image = generateProductAvatar(name);
            } catch (StableDiffusionException e) {
                log.warn("Failed to generate image for product {}: {}", name, e.getMessage());
                image = stableDiffusionService.generateDefaultAvatar("cube");
            }
        } else {
            log.warn("Stable Diffusion not available, using default avatar for product: {}", name);
            image = stableDiffusionService.generateDefaultAvatar("cube");
        }
        product.setAvatarHash(AvatarUtil.computeHash(image.getResizedImage()));
        Product savedProduct = productApi.persist(product);
        ToolActivityContextHolder.reportActivity("created product '" + savedProduct.getName() + "' with ID: " + savedProduct.getId());
        return ProductDto.from(savedProduct);
    }

    @Tool(description = "Delete a product and all its ACL entries by productId.", resultConverter = KassandraToolCallResultConverter.class)
    public void deleteProductById(
            @ToolParam(description = "The productId") Long id) {
        Product product = productApi.getById(id);
        if (product == null) {
            throw new IllegalArgumentException("Product not found with ID: " + id);
        }
        ToolActivityContextHolder.reportActivity("Deleting product '" + product.getName() + "' (ID: " + id + ")");
        productApi.deleteById(id);
        ToolActivityContextHolder.reportActivity("Deleted product '" + product.getName() + "' (ID: " + id + ")");
    }

    @Tool(description = "Delete a product and all its ACL entries by name.", resultConverter = KassandraToolCallResultConverter.class)
    public void deleteProductByName(
            @ToolParam(description = "The product name") String name) {
        productApi.getByName(name).orElseThrow(() -> new IllegalArgumentException("Product not found with name: " + name));
        ToolActivityContextHolder.reportActivity("Deleting product: " + name);
        productApi.deleteByName(name);
        ToolActivityContextHolder.reportActivity("Deleted product: " + name);
    }

    private @NonNull GeneratedImageResult generateProductAvatar(String name) throws StableDiffusionException {
        String prompt = Product.getDefaultAvatarPrompt(name);
        log.trace("Generating image for product: {} with prompt: {}", name, prompt);
        return stableDiffusionService.generateImageWithOriginal(prompt);
    }

    @Tool(description = "Get all products accessible to the current user.", resultConverter = KassandraToolCallResultConverter.class)
    public List<ProductDto> getAllProducts() {
        List<Product> products = productApi.getAll();
        ToolActivityContextHolder.reportActivity("read " + products.size() + " products.");
        return products.stream().map(ProductDto::from).collect(Collectors.toList());
    }

    @Tool(description = "Get a product by its productId.", resultConverter = KassandraToolCallResultConverter.class)
    public ProductDto getProductById(
            @ToolParam(description = "The productId") Long productId) {
        Product product = productApi.getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }
        ToolActivityContextHolder.reportActivity("read product: " + product.getName());
        return ProductDto.from(product);
    }

    @Tool(description = "Get a product by its name.", resultConverter = KassandraToolCallResultConverter.class)
    public ProductDto getProductByName(
            @ToolParam(description = "The product name") String name) {
        Product product = productApi.getByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with name: " + name));
        ToolActivityContextHolder.reportActivity("found product: " + product.getName());
        return ProductDto.from(product);
    }

    @Tool(description = "Update an existing product by its productId.", resultConverter = KassandraToolCallResultConverter.class)
    public ProductDto updateProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The new product name") String name) {
        Product product = productApi.getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }
        product.setName(name);
        productApi.update(product);
        ToolActivityContextHolder.reportActivity("updated product: " + name);
        return ProductDto.from(product);
    }
}
