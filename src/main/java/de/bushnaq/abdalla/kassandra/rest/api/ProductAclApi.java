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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.ProductAclEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ProductAclApi extends AbstractApi {

    public ProductAclApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    @Autowired
    public ProductAclApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    public ProductAclApi() {
    }

    public List<ProductAclEntry> getAcl(Long productId) {
        return executeWithErrorHandling(() -> {
            ResponseEntity<ProductAclEntry[]> response = restTemplate.exchange(
                    getBaseUrl() + "/product/{productId}/acl",
                    HttpMethod.GET,
                    createHttpEntity(),
                    ProductAclEntry[].class,
                    productId
            );
            ProductAclEntry[] entries = response.getBody();
            return entries != null ? Arrays.asList(entries) : new ArrayList<>();
        });
    }

    public ProductAclEntry grantGroupAccess(Long productId, Long groupId) {
        return executeWithErrorHandling(() -> {
            ResponseEntity<ProductAclEntry> response = restTemplate.exchange(
                    getBaseUrl() + "/product/{productId}/acl/group/{groupId}",
                    HttpMethod.POST,
                    createHttpEntity(),
                    ProductAclEntry.class,
                    productId,
                    groupId
            );
            return response.getBody();
        });
    }

    public ProductAclEntry grantUserAccess(Long productId, Long userId) {
        return executeWithErrorHandling(() -> {
            ResponseEntity<ProductAclEntry> response = restTemplate.exchange(
                    getBaseUrl() + "/product/{productId}/acl/user/{userId}",
                    HttpMethod.POST,
                    createHttpEntity(),
                    ProductAclEntry.class,
                    productId,
                    userId
            );
            return response.getBody();
        });
    }

    public void revokeGroupAccess(Long productId, Long groupId) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{productId}/acl/group/{groupId}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                productId,
                groupId
        ));
    }

    public void revokeUserAccess(Long productId, Long userId) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{productId}/acl/user/{userId}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                productId,
                userId
        ));
    }
}

