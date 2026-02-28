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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.user;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.util.ColorUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.time.LocalDate;

/**
 * Simplified User DTO for AI tools.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"userId", "name", "email", "colorHex", "roles", "firstWorkingDay", "lastWorkingDay"})
@Schema(description = "A user in the system")
public class UserDto {
    @Schema(description = "User color in hex format (e.g. '#FF336699')")
    private String    colorHex;
    @Schema(description = "User email address")
    private String    email;
    @Schema(description = "First working day (ISO 8601 date)")
    private LocalDate firstWorkingDay;
    @Schema(description = "Last working day (ISO 8601 date); null means still employed", nullable = true)
    private LocalDate lastWorkingDay;
    @Schema(description = "Unique user name")
    private String    name;
    @Schema(description = "Comma-separated roles: ROLE_USER or ROLE_ADMIN")
    private String    roles;
    @Schema(description = "Unique user identifier; use this ID in subsequent operations")
    private Long      userId;

    /**
     * Custom constructor for UserDto with explicit parameter order.
     */
    public UserDto(Long userId, String name, String email, String colorHex, String roles, LocalDate firstWorkingDay, LocalDate lastWorkingDay) {
        this.userId          = userId;
        this.name            = name;
        this.email           = email;
        this.colorHex        = colorHex;
        this.roles           = roles;
        this.firstWorkingDay = firstWorkingDay;
        this.lastWorkingDay  = lastWorkingDay;
    }

    public static UserDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                ColorUtil.colorToHexString(user.getColor()),
                user.getRoles(),
                user.getFirstWorkingDay(),
                user.getLastWorkingDay()
        );
    }

    public User toUser() {
        User user = new User();
        user.setId(this.userId);
        user.setName(this.name);
        user.setEmail(this.email);
        if (this.colorHex != null && !this.colorHex.isEmpty()) {
            try {
                String hex = this.colorHex.startsWith("#") ? this.colorHex.substring(1) : this.colorHex;
                user.setColor(new Color((int) Long.parseLong(hex, 16), true));
            } catch (NumberFormatException e) {
                user.setColor(new Color(51, 102, 204)); // default blue
            }
        }
        user.setRoles(this.roles);
        user.setFirstWorkingDay(this.firstWorkingDay);
        user.setLastWorkingDay(this.lastWorkingDay);
        return user;
    }
}
