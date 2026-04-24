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
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "user_avatar_generation_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class UserAvatarGenerationDataDAO extends AbstractTimeAwareDAO {

    @Lob
    @Column(name = "light_avatar_image_original")
    private byte[] lightAvatarImageOriginal;
    @Column(name = "light_avatar_negative_prompt", length = 1000)
    private String lightAvatarNegativePrompt;
    @Column(name = "light_avatar_prompt", length = 1000)
    private String lightAvatarPrompt;
    @Lob
    @Column(name = "dark_avatar_image_original")
    private byte[] darkAvatarImageOriginal;
    @Column(name = "dark_avatar_negative_prompt", length = 1000)
    private String darkAvatarNegativePrompt;
    @Column(name = "dark_avatar_prompt", length = 1000)
    private String darkAvatarPrompt;
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id")
    private UUID   id;
    @Column(name = "user_id", unique = true, nullable = false)
    private UUID   userId;
}

