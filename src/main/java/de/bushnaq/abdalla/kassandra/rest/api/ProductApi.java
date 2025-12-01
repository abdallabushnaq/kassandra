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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.dto.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class ProductApi extends AbstractApi {

    public ProductApi(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl) {
        super(restTemplate, objectMapper, baseUrl);
    }

    @Autowired
    public ProductApi(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    public ProductApi() {

    }

    public void deleteById(Long id) {
        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{id}",
                HttpMethod.DELETE,
                createHttpEntity(),
                Void.class,
                id
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
    public AvatarUpdateRequest getAvatarFull(Long productId) {
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
    public AvatarWrapper getAvatarImage(Long productId) {
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

    public Product getById(Long id) {
        ResponseEntity<Product> response = executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{id}",
                HttpMethod.GET,
                createHttpEntity(),
                Product.class,
                id
        ));
        return response.getBody();
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
     * Update product avatar with all fields (resized, original, and prompt).
     *
     * @param productId     The product ID
     * @param resizedImage  The resized avatar image bytes (e.g., 64x64)
     * @param originalImage The original avatar image bytes (e.g., 512x512)
     * @param prompt        The prompt used to generate the avatar
     */
    public void updateAvatarFull(Long productId, byte[] resizedImage, byte[] originalImage, String prompt) {
        AvatarUpdateRequest request = new AvatarUpdateRequest();

        if (resizedImage != null) {
            request.setAvatarImage(resizedImage);
        }

        if (originalImage != null) {
            request.setAvatarImageOriginal(originalImage);
        }

        request.setAvatarPrompt(prompt);

        executeWithErrorHandling(() -> restTemplate.exchange(
                getBaseUrl() + "/product/{id}/avatar/full",
                HttpMethod.PUT,
                createHttpEntity(request),
                Void.class,
                productId
        ));
    }
}
