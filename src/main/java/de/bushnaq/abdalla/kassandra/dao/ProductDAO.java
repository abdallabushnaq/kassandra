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

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Proxy;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Proxy(lazy = false)
public class ProductDAO extends AbstractTimeAwareDAO {

    @Column(name = "avatar_hash", length = 16)
    private String avatarHash;
    @Lob
    @Column(name = "avatar_image")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private byte[] avatarImage;
    @Lob
    @Column(name = "avatar_image_original")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private byte[] avatarImageOriginal;
    @Column(name = "avatar_prompt", length = 1000)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String avatarPrompt;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long   id;
    @Column(nullable = false, unique = true)
    private String name;

}
