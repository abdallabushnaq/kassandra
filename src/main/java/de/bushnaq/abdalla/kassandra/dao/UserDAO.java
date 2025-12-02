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
import lombok.*;
import org.hibernate.annotations.Proxy;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Proxy(lazy = false)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserDAO extends AbstractTimeAwareDAO {

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<AvailabilityDAO> availabilities = new ArrayList<>();
    @Column(name = "avatar_hash", length = 16)
    private String                avatarHash;
    @Lob
    @Column(name = "avatar_image")
    @JsonIgnore
    private byte[]                avatarImage;
    @Lob
    @Column(name = "avatar_image_original")
    @JsonIgnore
    private byte[]                avatarImageOriginal;
    @Column(name = "avatar_prompt", length = 1000)
    @JsonIgnore
    private String                avatarPrompt;
    @Column(nullable = false)
    private Color                 color;
    @Column(nullable = false)
    private String                email;
    @Column(nullable = false)
    private LocalDate             firstWorkingDay;//first working day
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long                  id;
    @Column(nullable = true)
    private LocalDate             lastWorkingDay;//last working day
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<LocationDAO>     locations      = new ArrayList<>();
    @Column(nullable = false)
    private String                name;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<OffDayDAO>       offDays        = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (firstWorkingDay == null)
            firstWorkingDay = LocalDate.now();
    }

}
