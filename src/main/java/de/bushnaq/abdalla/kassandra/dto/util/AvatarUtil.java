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

package de.bushnaq.abdalla.kassandra.dto.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for avatar-related operations.
 */
public class AvatarUtil {

    private AvatarUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Compute hash of the given data for cache-busting.
     * Uses first 16 characters of SHA-256 hash - provides excellent collision resistance
     * while keeping URLs reasonably short.
     * <p>
     * The hash is used for cache-busting in avatar URLs - when the image changes, the hash changes,
     * forcing the browser to fetch the new image instead of using a cached version.
     *
     * @param data The data to hash (typically the avatar image bytes)
     * @return Hexadecimal string representation of the hash (16 characters)
     * @throws RuntimeException if SHA-256 algorithm is not available
     */
    public static String computeHash(byte[] data) {
        try {
            MessageDigest digest    = MessageDigest.getInstance("SHA-256");
            byte[]        hashBytes = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            // Only use first 8 bytes (16 hex characters) - more than enough for cache-busting
            for (int i = 0; i < 8 && i < hashBytes.length; i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}

