/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
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
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Stores one administrator-managed server setting value.
 */
@Entity
@Table(name = "server_settings")
@Audited
@Getter
@Setter
@ToString(callSuper = true, exclude = "value")
@EqualsAndHashCode(of = {"key"}, callSuper = false)
public class ServerSettingDAO extends AbstractTimeAwareDAO {

    @Column(nullable = false)
    private boolean encrypted;
    @Id
    @Column(name = "setting_key", nullable = false, updatable = false)
    private String  key;
    @Column(name = "setting_value", nullable = false, length = 8192)
    @NotAudited
    private String  value;
    @Version
    @Column(nullable = false)
    private long    version;
}
