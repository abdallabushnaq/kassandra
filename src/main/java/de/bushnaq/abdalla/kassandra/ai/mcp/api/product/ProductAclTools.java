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
import de.bushnaq.abdalla.kassandra.dto.ProductAclEntry;
import de.bushnaq.abdalla.kassandra.rest.api.ProductAclApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI native tool implementations for Product ACL (Access Control List) operations.
 * <p>
 * Product ACL entries control which users and groups can access a product.
 * Each entry grants access to either a user or a group (but not both).
 * <p>
 * Example usage:
 * 1. Use getProductAcl(productId) to list all ACL entries for a product.
 * 2. Use grantUserAccessToProduct(productId, userId) to grant a user access.
 * 3. Use grantGroupAccessToProduct(productId, groupId) to grant a group access.
 * 4. Use revokeUserAccessFromProduct(productId, userId) to revoke a user's access.
 * 5. Use revokeGroupAccessFromProduct(productId, groupId) to revoke a group's access.
 * <p>
 * Uses @Tool annotation for automatic tool registration with ChatClient.
 */
@Component
@Slf4j
public class ProductAclTools {

    @Autowired
    @Qualifier("aiProductAclApi")
    private ProductAclApi productAclApi;

    @Tool(description = "Get all ACL entries for a product (requires admin or existing access).", resultConverter = KassandraToolCallResultConverter.class)
    public List<ProductAclDto> getProductAcl(
            @ToolParam(description = "The productId") Long productId) {
        List<ProductAclEntry> entries = productAclApi.getAcl(productId);
        ToolActivityContextHolder.reportActivity("read " + entries.size() + " ACL entries for product ID: " + productId);
        return entries.stream().map(ProductAclDto::from).collect(Collectors.toList());
    }

    @Tool(description = "Grant a group access to a product (requires admin or existing access).", resultConverter = KassandraToolCallResultConverter.class)
    public ProductAclDto grantGroupAccessToProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The groupId to grant access") Long groupId) {
        ProductAclEntry entry = productAclApi.grantGroupAccess(productId, groupId);
        ToolActivityContextHolder.reportActivity("Granted group " + groupId + " access to product " + productId);
        return ProductAclDto.from(entry);
    }

    @Tool(description = "Grant a user access to a product (requires admin or existing access).", resultConverter = KassandraToolCallResultConverter.class)
    public ProductAclDto grantUserAccessToProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The userId to grant access") Long userId) {
        ProductAclEntry entry = productAclApi.grantUserAccess(productId, userId);
        ToolActivityContextHolder.reportActivity("Granted user " + userId + " access to product " + productId);
        return ProductAclDto.from(entry);
    }

    @Tool(description = "Revoke a group's access from a product (requires admin or existing access).", resultConverter = KassandraToolCallResultConverter.class)
    public void revokeGroupAccessFromProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The groupId to revoke") Long groupId) {
        productAclApi.revokeGroupAccess(productId, groupId);
        ToolActivityContextHolder.reportActivity("Revoked group " + groupId + " access from product " + productId);
    }

    @Tool(description = "Revoke a user's access from a product (requires admin or existing access).", resultConverter = KassandraToolCallResultConverter.class)
    public void revokeUserAccessFromProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The userId to revoke") Long userId) {
        productAclApi.revokeUserAccess(productId, userId);
        ToolActivityContextHolder.reportActivity("Revoked user " + userId + " access from product " + productId);
    }
}
