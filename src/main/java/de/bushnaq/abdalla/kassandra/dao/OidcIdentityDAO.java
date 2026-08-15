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
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Links one immutable OIDC subject at a configured provider to a Kassandra user.
 */
@Entity
@Table(name = "oidc_identities", uniqueConstraints = @UniqueConstraint(
        name = "uk_oidc_identity_provider_subject", columnNames = {"provider_id", "subject"}))
@Getter
@Setter
@ToString(callSuper = true, exclude = "user")
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class OidcIdentityDAO extends AbstractTimeAwareDAO {

    @Id
    @Column(name = "id")
    private UUID            id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_oidc_identity_provider"))
    private OidcProviderDAO provider;

    @Column(nullable = false, length = 2048)
    private String          subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_oidc_identity_user"))
    private UserDAO         user;

    /**
     * Creates an identity with a generated persistent identifier.
     */
    public OidcIdentityDAO() {
        id = UUID.randomUUID();
    }
}
