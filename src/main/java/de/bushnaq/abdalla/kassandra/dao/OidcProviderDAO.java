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

package de.bushnaq.abdalla.kassandra.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * A configured OpenID Connect provider trusted by this Kassandra instance.
 */
@Entity
@Table(name = "oidc_providers")
@Getter
@Setter
@ToString(callSuper = true, exclude = "clientSecretEncrypted")
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class OidcProviderDAO extends AbstractTimeAwareDAO {

    @Column(name = "client_id", nullable = false)
    private String  clientId;

    @Column(name = "client_secret_encrypted", length = 4096)
    private String  clientSecretEncrypted;

    @Column(nullable = false)
    private String  displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Id
    @Column(name = "id")
    private UUID    id;

    @Column(name = "issuer_uri", nullable = false, unique = true, length = 2048)
    private String  issuerUri;

    @Column(name = "registration_id", nullable = false, unique = true, updatable = false, length = 100)
    private String  registrationId;

    @Column(nullable = false, length = 1000)
    private String  scopes = "openid,profile,email";

    /**
     * Creates a provider with a generated persistent identifier.
     */
    public OidcProviderDAO() {
        id = UUID.randomUUID();
    }
}
