/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.rest.controller;

import de.bushnaq.abdalla.kassandra.dao.ProductAclEntryDAO;
import de.bushnaq.abdalla.kassandra.service.AclSecurityService;
import de.bushnaq.abdalla.kassandra.service.ProductAclService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Product Access Control Lists.
 * Access requires either ADMIN role or existing access to the product.
 */
@RestController
@RequestMapping("/api/product/{productId}/acl")
@Slf4j
public class ProductAclController {

    @Autowired
    private AclSecurityService aclSecurityService;
    @Autowired
    private ProductAclService productAclService;

    /**
     * Get all ACL entries for a product
     *
     * @param productId the product ID
     * @return list of ACL entries
     */
    @GetMapping
    @PreAuthorize("@aclSecurityService.canManageProductAcl(#productId)")
    public List<ProductAclEntryDAO> getAcl(@PathVariable Long productId) {
        log.info("Getting ACL for product: {}", productId);
        return productAclService.getProductAcl(productId);
    }

    /**
     * Grant access to a group for a product
     *
     * @param productId the product ID
     * @param groupId   the group ID
     * @return the created ACL entry
     */
    @PostMapping("/group/{groupId}")
    @PreAuthorize("@aclSecurityService.canManageProductAcl(#productId)")
    @Transactional
    public ProductAclEntryDAO grantGroupAccess(
            @PathVariable Long productId,
            @PathVariable Long groupId
    ) {
        log.info("Granting group {} access to product {}", groupId, productId);
        return productAclService.grantGroupAccess(productId, groupId);
    }

    /**
     * Grant access to a user for a product
     *
     * @param productId the product ID
     * @param userId    the user ID
     * @return the created ACL entry
     */
    @PostMapping("/user/{userId}")
    @PreAuthorize("@aclSecurityService.canManageProductAcl(#productId)")
    @Transactional
    public ProductAclEntryDAO grantUserAccess(
            @PathVariable Long productId,
            @PathVariable Long userId
    ) {
        log.info("Granting user {} access to product {}", userId, productId);
        return productAclService.grantUserAccess(productId, userId);
    }

    /**
     * Revoke a group's access to a product
     *
     * @param productId the product ID
     * @param groupId   the group ID
     */
    @DeleteMapping("/group/{groupId}")
    @PreAuthorize("@aclSecurityService.canManageProductAcl(#productId)")
    @Transactional
    public void revokeGroupAccess(
            @PathVariable Long productId,
            @PathVariable Long groupId
    ) {
        log.info("Revoking group {} access to product {}", groupId, productId);
        productAclService.revokeGroupAccess(productId, groupId);
    }

    /**
     * Revoke a user's access to a product
     *
     * @param productId the product ID
     * @param userId    the user ID
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("@aclSecurityService.canManageProductAcl(#productId)")
    @Transactional
    public void revokeUserAccess(
            @PathVariable Long productId,
            @PathVariable Long userId
    ) {
        log.info("Revoking user {} access to product {}", userId, productId);
        productAclService.revokeUserAccess(productId, userId);
    }
}

