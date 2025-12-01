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

package de.bushnaq.abdalla.kassandra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating avatar images and prompt.
 * Used to send avatar data to REST API endpoints since avatar fields are @JsonIgnore.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUpdateRequest {
    /**
     * Resized avatar image (e.g., 64x64) as base64 encoded string
     */
    private byte[] avatarImage;

    /**
     * Original avatar image (e.g., 512x512) as base64 encoded string
     */
    private byte[] avatarImageOriginal;

    /**
     * The prompt used to generate the avatar
     */
    private String avatarPrompt;
}

