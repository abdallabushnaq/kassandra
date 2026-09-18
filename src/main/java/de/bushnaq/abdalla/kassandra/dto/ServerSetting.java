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

package de.bushnaq.abdalla.kassandra.dto;

import lombok.Data;

import java.util.List;

/**
 * Safe setting metadata and value returned to an administrator.
 */
@Data
public class ServerSetting {

    private String       categoryIcon;
    private boolean      categoryAllowDisable;
    private boolean      categoryEnabled;
    private String       categoryKey;
    private String       categoryLabel;
    private boolean      configured;
    private String       description;
    private String       key;
    private String       label;
    private Double       maximum;
    private Double       minimum;
    private List<String> options;
    private boolean      restartRequired;
    private boolean      secret;
    private boolean      testable;
    private String       type;
    private String       value;
}
