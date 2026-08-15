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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Stores the singleton security setup and recovery state of a Kassandra instance.
 */
@Entity
@Table(name = "security_configuration")
@Getter
@Setter
@ToString(callSuper = true, exclude = "recoveryPasswordHash")
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class SecurityConfigurationDAO extends AbstractTimeAwareDAO {

    /**
     * The fixed identifier of the sole security configuration row.
     */
    public static final UUID CONFIGURATION_ID = UUID.fromString("fa7ce2dc-cdd1-4c25-928d-5bbd94274e67");

    @Id
    @Column(name = "id")
    private UUID           id = CONFIGURATION_ID;

    @Column(name = "recovery_password_hash", length = 100)
    private String         recoveryPasswordHash;

    @Column(name = "setup_completed", nullable = false)
    private boolean        setupCompleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "setup_state", nullable = false, length = 32)
    private SetupState     setupState = SetupState.SETUP_REQUIRED;

    @Version
    @Column(name = "version", nullable = false)
    private long           version;

    /**
     * Represents the security configuration lifecycle.
     */
    public enum SetupState {
        SETUP_REQUIRED,
        SETUP_IN_PROGRESS,
        READY
    }
}
