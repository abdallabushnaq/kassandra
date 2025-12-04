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

import de.bushnaq.abdalla.kassandra.dto.AvatarWrapper;
import de.bushnaq.abdalla.kassandra.rest.api.FeatureApi;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import de.bushnaq.abdalla.kassandra.rest.api.SprintApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/frontend/avatar-proxy")
@Slf4j
public class AvatarProxyController {
    private final FeatureApi featureApi;
    private final ProductApi productApi;
    private final SprintApi  sprintApi;
    private final UserApi    userApi;

    @Autowired
    public AvatarProxyController(FeatureApi featureApi, ProductApi productApi, SprintApi sprintApi, UserApi userApi) {
        this.featureApi = featureApi;
        this.productApi = productApi;
        this.sprintApi  = sprintApi;
        this.userApi    = userApi;
    }

    @GetMapping("/feature/{featureId}")
    public ResponseEntity<byte[]> proxyFeatureAvatar(@PathVariable("featureId") Long featureId) {
        AvatarWrapper avatarImage = featureApi.getAvatarImage(featureId);
        if (avatarImage == null || avatarImage.getAvatar() == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("public, max-age=31536000, immutable"); // Cache for 1 year - hash in URL handles versioning
        return ResponseEntity.ok().headers(headers).body(avatarImage.getAvatar());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<byte[]> proxyProductAvatar(@PathVariable("productId") Long productId) {
        AvatarWrapper avatarImage = productApi.getAvatarImage(productId);
        if (avatarImage == null || avatarImage.getAvatar() == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("public, max-age=31536000, immutable"); // Cache for 1 year - hash in URL handles versioning
        return ResponseEntity.ok().headers(headers).body(avatarImage.getAvatar());
    }

    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<byte[]> proxySprintAvatar(@PathVariable("sprintId") Long sprintId) {
        AvatarWrapper avatarImage = sprintApi.getAvatarImage(sprintId);
        if (avatarImage == null || avatarImage.getAvatar() == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("public, max-age=31536000, immutable"); // Cache for 1 year - hash in URL handles versioning
        return ResponseEntity.ok().headers(headers).body(avatarImage.getAvatar());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<byte[]> proxyUserAvatar(@PathVariable("userId") Long userId) {
        AvatarWrapper avatarImage = userApi.getAvatarImage(userId);
        if (avatarImage == null || avatarImage.getAvatar() == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("public, max-age=31536000, immutable"); // Cache for 1 year - hash in URL handles versioning
        return ResponseEntity.ok().headers(headers).body(avatarImage.getAvatar());
    }
}
