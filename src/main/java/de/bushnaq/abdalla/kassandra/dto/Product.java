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

package de.bushnaq.abdalla.kassandra.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import de.bushnaq.abdalla.kassandra.dto.util.HeaderPrompt;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@JsonIdentityInfo(
        scope = Product.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class Product extends AbstractTimeAware implements Comparable<Product>, HeaderPrompt {

    private String        darkAvatarHash;
    private String        darkHeaderHash;
    private UUID          id;
    private String        lightAvatarHash;
    private String        lightHeaderHash;
    private String        name;
    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private List<Version> versions = new ArrayList<>();

    public void addVersion(Version version) {
        versions.add(version);
        version.setProduct(this);
    }

    @Override
    public int compareTo(Product other) {
        return this.id.compareTo(other.id);
    }

    /**
     * Get the avatar URL for the given theme variant.
     * When {@code dark} is {@code true} and a dark avatar has been generated, the dark variant URL is
     * returned. Falls back to the light variant URL when no dark avatar is available yet.
     *
     * @param dark {@code true} to request the dark-background avatar variant
     * @return The avatar URL with hash parameter for cache-busting
     */
    @JsonIgnore
    public String getAvatarUrl(boolean dark) {
        if (dark && darkAvatarHash != null && !darkAvatarHash.isEmpty()) {
            return "/frontend/dark-avatar-proxy/product/" + id + "?h=" + darkAvatarHash;
        }
        String url = "/frontend/avatar-proxy/product/" + id;
        if (lightAvatarHash != null && !lightAvatarHash.isEmpty()) {
            url += "?h=" + lightAvatarHash;
        }
        return url;
    }

    /**
     * Get the header URL for the given theme variant.
     *
     * @param dark {@code true} to request the dark-background header variant
     * @return The header URL with hash parameter for cache-busting
     */
    @JsonIgnore
    public String getHeaderUrl(boolean dark) {
        if (dark && darkHeaderHash != null && !darkHeaderHash.isEmpty()) {
            return "/frontend/dark-avatar-proxy/product-header/" + id + "?h=" + darkHeaderHash;
        }
        String url = "/frontend/avatar-proxy/product-header/" + id;
        if (lightHeaderHash != null && !lightHeaderHash.isEmpty()) {
            url += "?h=" + lightHeaderHash;
        }
        return url;
    }

    @JsonIgnore
    public String getKey() {
        return "P-" + id;
    }

    public void initialize(GanttContext gc) {
        versions.clear();
        gc.allVersions.forEach(version -> {
            if (Objects.equals(version.getProductId(), id)) {
                addVersion(version);
            }
        });
        versions.forEach(version -> version.initialize(gc));
    }

    public void removeVersion(Version version) {
        versions.remove(version);
    }
}
