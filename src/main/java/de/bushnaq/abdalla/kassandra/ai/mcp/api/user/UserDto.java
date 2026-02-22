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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.time.LocalDate;

/**
 * Simplified User DTO for AI tools.
 * Contains only fields relevant for AI interactions, excluding internal fields like avatarHash, color, calendar, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"userId", "name", "email", "color", "roles", "firstWorkingDay", "lastWorkingDay"})
public class UserDto {
    private Color     color;
    private String    email;
    private LocalDate firstWorkingDay;
    private LocalDate lastWorkingDay;
    private String    name;
    private String    roles;
    private Long      userId;

    /**
     * Custom constructor for UserDto with explicit parameter order.
     */
    public UserDto(Long userId, String name, String email, Color color, String roles, LocalDate firstWorkingDay, LocalDate lastWorkingDay) {
        this.userId          = userId;
        this.name            = name;
        this.email           = email;
        this.color           = color;
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
                user.getColor(),
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
        user.setColor(this.color);
        user.setRoles(this.roles);
        user.setFirstWorkingDay(this.firstWorkingDay);
        user.setLastWorkingDay(this.lastWorkingDay);
        return user;
    }
}
