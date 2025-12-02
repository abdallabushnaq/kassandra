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
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@JsonIdentityInfo(
        scope = Product.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class Product extends AbstractTimeAware implements Comparable<Product> {

    private String        avatarHash;
    private Long          id;
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
     * Get the avatar URL with hash parameter for proper caching.
     * The hash ensures that when the avatar changes, the URL changes, forcing the browser to fetch the new image.
     *
     * @return The avatar URL with hash parameter if hash is available, otherwise just the base URL
     */
    @JsonIgnore
    public String getAvatarUrl() {
        String url = "/frontend/avatar-proxy/product/" + id;
        if (avatarHash != null && !avatarHash.isEmpty()) {
            url += "?h=" + avatarHash;
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
