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

package de.bushnaq.abdalla.kassandra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts persisted security credentials using an operator-provided AES-256 key.
 */
@Service
public class SecuritySecretService {

    private static final String ALGORITHM          = "AES/GCM/NoPadding";
    private static final int    GCM_TAG_LENGTH     = 128;
    private static final int    INITIALIZATION_LEN = 12;
    private static final String VERSION            = "v1";

    @Value("${kassandra.security.encryption-key:}")
    private String              encryptionKey;

    /**
     * Decrypts a value produced by {@link #encrypt(String)}.
     *
     * @param encryptedValue encrypted value
     * @return plaintext value
     * @throws IllegalStateException when the configured master key is invalid or decryption fails
     */
    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return null;
        }

        String[] parts = encryptedValue.split(":", -1);
        if (parts.length != 3 || !VERSION.equals(parts[0])) {
            throw new IllegalStateException("Stored security secret has an unsupported format");
        }

        try {
            byte[] initializationVector = Base64.getUrlDecoder().decode(parts[1]);
            byte[] ciphertext           = Base64.getUrlDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_LENGTH, initializationVector));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to decrypt security secret with the configured encryption key", e);
        }
    }

    /**
     * Encrypts a secret for database storage.
     *
     * @param plaintext plaintext value
     * @return versioned encrypted value, or null when plaintext is null
     * @throws IllegalStateException when the configured master key is absent or invalid
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }

        byte[] initializationVector = new byte[INITIALIZATION_LEN];
        new SecureRandom().nextBytes(initializationVector);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_LENGTH, initializationVector));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(initializationVector) + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt security secret", e);
        }
    }

    private SecretKey key() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalStateException("KASSANDRA_SECURITY_ENCRYPTION_KEY must be configured before storing OIDC client secrets");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            if (keyBytes.length != 32) {
                throw new IllegalStateException("KASSANDRA_SECURITY_ENCRYPTION_KEY must contain a Base64-encoded 256-bit key");
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("KASSANDRA_SECURITY_ENCRYPTION_KEY must be Base64-encoded", e);
        }
    }
}
