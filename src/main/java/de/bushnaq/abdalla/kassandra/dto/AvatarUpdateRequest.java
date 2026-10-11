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

import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.CozyStylePrompt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating avatar images and prompt.
 * Used to send avatar data to REST API endpoints since avatar fields are @JsonIgnore.
 * Carries both the light and dark variants in a single request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUpdateRequest {
    /**
     * Theme-independent avatar description.
     */
    private String          avatarPrompt;
    /**
     * Theme-independent header description, without style instructions.
     */
    private String          headerPrompt;
    /**
     * Things to avoid when generating either avatar variant.
     */
    private String          negativePrompt;
    /**
     * Style shared by both header variants; never applied to avatars.
     */
    private CozyStylePrompt style;
    /**
     * Resized dark-mode avatar image (e.g., 64x64).
     */
    private byte[]          darkAvatarImage;
    /**
     * Original dark-mode avatar image (e.g., 512x512).
     */
    private byte[]          darkAvatarImageOriginal;
    /**
     * Header image (1024x48) for the dark theme.
     */
    private byte[]          darkHeaderImage;
    /**
     * Resized avatar image (e.g., 64x64) for the light theme.
     */
    private byte[]          lightAvatarImage;
    /**
     * Original avatar image (e.g., 512x512) for the light theme.
     */
    private byte[]          lightAvatarImageOriginal;
    /**
     * Header image (1024x48) for the light theme.
     */
    private byte[]          lightHeaderImage;
}
