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
import de.bushnaq.abdalla.kassandra.dto.ProductAclEntry;
import de.bushnaq.abdalla.kassandra.rest.api.ProductAclApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

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
    private JsonMapper jsonMapper;

    @Autowired
    @Qualifier("aiProductAclApi")
    private ProductAclApi productAclApi;

    @Tool(description = "Get all ACL entries for a product (requires admin or existing access).")
    public String getProductAcl(
            @ToolParam(description = "The productId") Long productId) {
        try {
            List<ProductAclEntry> entries = productAclApi.getAcl(productId);
            ToolActivityContextHolder.reportActivity("read " + entries.size() + " ACL entries for product ID: " + productId);
            List<ProductAclDto> dtos = entries.stream()
                    .map(ProductAclDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(dtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting ACL for product " + productId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Grant a group access to a product (requires admin or existing access).")
    public String grantGroupAccessToProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The groupId to grant access") Long groupId) {
        try {
            ProductAclEntry entry = productAclApi.grantGroupAccess(productId, groupId);
            ToolActivityContextHolder.reportActivity("Granted group " + groupId + " access to product " + productId);
            ProductAclDto dto = ProductAclDto.from(entry);
            return jsonMapper.writeValueAsString(dto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error granting group access: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Grant a user access to a product (requires admin or existing access).")
    public String grantUserAccessToProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The userId to grant access") Long userId) {
        try {
            ProductAclEntry entry = productAclApi.grantUserAccess(productId, userId);
            ToolActivityContextHolder.reportActivity("Granted user " + userId + " access to product " + productId);
            ProductAclDto dto = ProductAclDto.from(entry);
            return jsonMapper.writeValueAsString(dto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error granting user access: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Revoke a group's access from a product (requires admin or existing access).")
    public String revokeGroupAccessFromProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The groupId to revoke") Long groupId) {
        try {
            productAclApi.revokeGroupAccess(productId, groupId);
            ToolActivityContextHolder.reportActivity("Revoked group " + groupId + " access from product " + productId);
            return "Group " + groupId + " access to product " + productId + " revoked successfully";
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error revoking group access: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Revoke a user's access from a product (requires admin or existing access).")
    public String revokeUserAccessFromProduct(
            @ToolParam(description = "The productId") Long productId,
            @ToolParam(description = "The userId to revoke") Long userId) {
        try {
            productAclApi.revokeUserAccess(productId, userId);
            ToolActivityContextHolder.reportActivity("Revoked user " + userId + " access from product " + productId);
            return "User " + userId + " access to product " + productId + " revoked successfully";
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error revoking user access: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
