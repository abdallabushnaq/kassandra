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

import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.dto.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductApi extends AbstractApi {

    public ProductApi(RestTemplate restTemplate, JsonMapper jsonMapper, String baseUrl) {
        super(restTemplate, jsonMapper, baseUrl);
    }

    @Autowired
    public ProductApi(RestTemplate restTemplate, JsonMapper jsonMapper) {
        super(restTemplate, jsonMapper);
    }

    public ProductApi() {

    }

    public void deleteById(UUID id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
        ));
    }

    public void deleteByName(String name) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/by-name/{name}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                name
        ));
    }

    public List<Product> getAll() {
        ResponseEntity<Product[]> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product",
                HttpMethod.GET,
                createHttpEntity(),
                Product[].class
        ));
        return Arrays.asList(response.getBody());
    }

    /**
     * Get all avatar info (resized, original, and prompt) for a product.
     *
     * @param productId The product ID
     * @return AvatarUpdateRequest containing avatarImage, avatarImageOriginal, and avatarPrompt, or null if not found
     */
    public AvatarUpdateRequest getAvatarFull(UUID productId) {
        try {
            ResponseEntity<AvatarUpdateRequest> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/product/{id}/avatar/full",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarUpdateRequest.class,
                    productId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get avatar image bytes for a product.
     *
     * @param productId The product ID
     * @return The avatar image as byte array, or null if not found
     */
    public AvatarWrapper getAvatarImage(UUID productId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/product/{id}/avatar",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    productId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Product getById(UUID id) {
        ResponseEntity<Product> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                Product.class,
                id
        ));
        return response.getBody();
    }

    /**
     * Get a product by its name (case-insensitive, exact match).
     * Returns null if not found.
     */
    public Optional<Product> getByName(String name) {
        try {
            ResponseEntity<Product> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/product/by-name/{name}",
                    HttpMethod.GET,
                    createHttpEntity(),
                    Product.class,
                    name
            ));
            return Optional.of(response.getBody());
        } catch (ErrorResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Get the dark-mode avatar image bytes for a product.
     * The server falls back to the light image automatically if no dark variant has been stored yet.
     *
     * @param productId The product ID
     * @return The dark avatar image (or light fallback), or null if not found
     */
    public AvatarWrapper getDarkAvatarImage(UUID productId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/product/{id}/dark-avatar",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    productId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the dark-mode header image bytes for a product.
     *
     * @param productId The product ID
     * @return The dark header image, or its light fallback, or null if not found
     */
    public AvatarWrapper getDarkHeaderImage(UUID productId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/product/{id}/dark-header",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    productId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the light-mode header image bytes for a product.
     *
     * @param productId The product ID
     * @return The header image, or null if not found
     */
    public AvatarWrapper getHeaderImage(UUID productId) {
        try {
            ResponseEntity<AvatarWrapper> response = executeWithErrorHandling(() -> restTemplate.exchange(
                    getBaseUrl() + "/product/{id}/header",
                    HttpMethod.GET,
                    createHttpEntity(),
                    AvatarWrapper.class,
                    productId
            ));
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Product persist(Product product) {
        ResponseEntity<Product> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product",
                HttpMethod.POST,
                createHttpEntity(product),
                Product.class
        ));
        return response.getBody();
    }

    public void update(Product product) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product",
                HttpMethod.PUT,
                createHttpEntity(product),
                Void.class
        ));
    }

    /**
     * Updates image variants and theme-independent generation metadata.
     * Null fields leave stored values unchanged.
     *
     * @param productId the entity ID
     * @param request   images and editable generation inputs
     */
    public void updateAvatarFull(UUID productId, AvatarUpdateRequest request) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{id}/avatar/full",
                HttpMethod.PUT,
                createHttpEntity(request),
                Void.class,
                productId
        ));
    }
}