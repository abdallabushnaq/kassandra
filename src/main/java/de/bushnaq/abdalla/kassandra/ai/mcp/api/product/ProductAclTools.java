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

    /**
     * Schema description for ProductAclEntry objects returned by tools.
     * Describes the JSON structure and field meanings.
     * Used in @Tool annotations - must be a compile-time constant.
     */
    private static final String ACL_ENTRY_FIELDS             = """
            id (number): Unique identifier of the ACL entry,
            productId (number): The product this ACL entry belongs to,
            userId (number or null): The user granted access (null if this is a group entry),
            groupId (number or null): The group granted access (null if this is a user entry),
            type (string): Either 'USER' or 'GROUP',
            displayName (string): Human-readable name of the user or group,
            created (ISO 8601 datetime string): Timestamp when the entry was created,
            updated (ISO 8601 datetime string): Timestamp when the entry was last updated.
            """;
    private static final String RETURNS_ACL_ENTRY_ARRAY_JSON = "Returns: JSON array of ProductAclEntry objects. Each entry contains: " + ACL_ENTRY_FIELDS;
    private static final String RETURNS_ACL_ENTRY_JSON       = "Returns: JSON ProductAclEntry object with fields: " + ACL_ENTRY_FIELDS;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    @Qualifier("aiProductAclApi")
    private ProductAclApi productAclApi;

    @Tool(description = "Get all ACL (Access Control List) entries for a product. " +
            "Requires admin role or existing access to the product. " +
            RETURNS_ACL_ENTRY_ARRAY_JSON)
    public String getProductAcl(
            @ToolParam(description = "The productId to retrieve ACL entries for") Long productId) {
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

    @Tool(description = "Grant a group access to a product by adding a GROUP ACL entry. " +
            "Requires admin role or existing access to the product. " +
            "IMPORTANT: The returned JSON includes the new ACL entry's 'id' field. " +
            RETURNS_ACL_ENTRY_JSON)
    public String grantGroupAccessToProduct(
            @ToolParam(description = "The productId to grant access to.") Long productId,
            @ToolParam(description = "The groupId of the group to grant access.") Long groupId) {
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

    @Tool(description = "Grant a user access to a product by adding a USER ACL entry. " +
            "Requires admin role or existing access to the product. " +
            "IMPORTANT: The returned JSON includes the new ACL entry's 'id' field. " +
            RETURNS_ACL_ENTRY_JSON)
    public String grantUserAccessToProduct(
            @ToolParam(description = "The productId to grant access to") Long productId,
            @ToolParam(description = "The userId of the user to grant access") Long userId) {
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

    @Tool(description = "Revoke a group's access from a product by removing the GROUP ACL entry. " +
            "Requires admin role or existing access to the product. " +
            "Returns: Success message (string) confirming revocation")
    public String revokeGroupAccessFromProduct(
            @ToolParam(description = "The productId to revoke access from") Long productId,
            @ToolParam(description = "The groupId of the group whose access should be revoked") Long groupId) {
        try {
            productAclApi.revokeGroupAccess(productId, groupId);
            ToolActivityContextHolder.reportActivity("Revoked group " + groupId + " access from product " + productId);
            return "Group " + groupId + " access to product " + productId + " revoked successfully";
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error revoking group access: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Revoke a user's access from a product by removing the USER ACL entry. " +
            "Requires admin role or existing access to the product. " +
            "Returns: Success message (string) confirming revocation")
    public String revokeUserAccessFromProduct(
            @ToolParam(description = "The productId to revoke access from") Long productId,
            @ToolParam(description = "The userId of the user whose access should be revoked") Long userId) {
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
