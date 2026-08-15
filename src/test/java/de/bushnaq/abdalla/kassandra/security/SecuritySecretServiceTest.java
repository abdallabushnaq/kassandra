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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
class SecuritySecretServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsWithDifferentInitializationVectors() {
        SecuritySecretService service = serviceWithKey(KEY);

        String first = service.encrypt("client-secret");
        String second = service.encrypt("client-secret");

        assertNotEquals(first, second);
        assertEquals("client-secret", service.decrypt(first));
        assertEquals("client-secret", service.decrypt(second));
    }

    @Test
    void rejectsAnAbsentMasterKey() {
        SecuritySecretService service = serviceWithKey("");

        assertThrows(IllegalStateException.class, () -> service.encrypt("client-secret"));
    }

    @Test
    void rejectsAnInvalidMasterKey() {
        SecuritySecretService service = serviceWithKey(Base64.getEncoder().encodeToString(new byte[16]));

        assertThrows(IllegalStateException.class, () -> service.encrypt("client-secret"));
    }

    private SecuritySecretService serviceWithKey(String encryptionKey) {
        SecuritySecretService service = new SecuritySecretService();
        ReflectionTestUtils.setField(service, "encryptionKey", encryptionKey);
        return service;
    }
}
