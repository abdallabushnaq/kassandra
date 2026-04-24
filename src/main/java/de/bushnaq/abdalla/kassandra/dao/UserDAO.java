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

package de.bushnaq.abdalla.kassandra.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Supports client side id generation.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@BatchSize(size = 10)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserDAO extends AbstractTimeAwareDAO {

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<AvailabilityDAO> availabilities = new ArrayList<>();
    @Column(nullable = false)
    private Color                 color;
    @Column(name = "dark_avatar_hash", length = 16)
    private String                darkAvatarHash;
    @Column(nullable = false, unique = true)
    private String                email;
    @Column(nullable = false)
    private LocalDate             firstWorkingDay;//first working day
    @Id
    @Column(name = "id")
    private UUID                  id;
    @Column(nullable = true)
    private LocalDate             lastWorkingDay;//last working day
    @Column(name = "light_avatar_hash", length = 16)
    private String                lightAvatarHash;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("start ASC")
    @JsonManagedReference
    private List<LocationDAO>     locations      = new ArrayList<>();
    @Column(nullable = false, unique = true)
    private String                name;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<OffDayDAO>       offDays        = new ArrayList<>();
    @Column(nullable = false)
    private String                roles          = "USER"; // Default role for new users
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("start ASC")
    @JsonManagedReference
    private List<UserWorkWeekDAO> userWorkWeeks  = new ArrayList<>();

    public UserDAO() {
        setId(UUID.randomUUID());
    }

    /**
     * Add a role to this user
     *
     * @param role the role to add (e.g., "ADMIN", "USER")
     */
    @JsonIgnore
    public void addRole(String role) {
        List<String> roleList = getRoleList();
        if (!roleList.contains(role)) {
            roleList.add(role);
            setRoleList(roleList);
        }
    }

    /**
     * Get roles as a list
     *
     * @return list of role names
     */
    @JsonIgnore
    public List<String> getRoleList() {
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(roles.split(",")));
    }

    /**
     * Check if user has a specific role
     *
     * @param role the role to check
     * @return true if user has the role
     */
    @JsonIgnore
    public boolean hasRole(String role) {
        return getRoleList().contains(role);
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (firstWorkingDay == null)
            firstWorkingDay = LocalDate.now();
    }

    /**
     * Remove a role from this user
     *
     * @param role the role to remove
     */
    @JsonIgnore
    public void removeRole(String role) {
        List<String> roleList = getRoleList();
        roleList.remove(role);
        setRoleList(roleList);
    }

    /**
     * Set roles from a list
     *
     * @param roleList list of role names
     */
    @JsonIgnore
    public void setRoleList(List<String> roleList) {
        this.roles = roleList.stream()
                .filter(r -> r != null && !r.isEmpty())
                .collect(Collectors.joining(","));
    }

}
