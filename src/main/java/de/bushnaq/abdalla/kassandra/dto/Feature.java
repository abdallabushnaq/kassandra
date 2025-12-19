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

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class Feature extends AbstractTimeAware implements Comparable<Feature> {
    private String avatarHash;
    private Long   id;
    private String name;

    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private List<Sprint> sprints = new ArrayList<>();

    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private Version version;

    private Long versionId;

    public void addSprint(Sprint sprint) {
        sprint.setFeature(this);
        sprints.add(sprint);
    }

    @Override
    public int compareTo(Feature other) {
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
        String url = "/frontend/avatar-proxy/feature/" + id;
        if (avatarHash != null && !avatarHash.isEmpty()) {
            url += "?h=" + avatarHash;
        }
        return url;
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This provides a consistent prompt format for feature avatars.
     *
     * @return A default prompt string for generating feature avatar images
     */
    @JsonIgnore
    public String getDefaultAvatarPrompt() {
        return getDefaultAvatarPrompt(name);
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This static method provides a consistent prompt format for feature avatars.
     *
     * @param featureName The name of the feature
     * @return A default prompt string for generating feature avatar images
     */
    public static String getDefaultAvatarPrompt(String featureName) {
        return "Icon representing the feature '" + featureName + "', minimalist, 3D design, white background";
    }

    @JsonIgnore
    public String getKey() {
        return "F-" + id;
    }

    public void initialize(GanttContext gc) {
        sprints.clear();
        gc.allSprints.forEach(sprint -> {
            if (Objects.equals(sprint.getFeatureId(), id)) {
                addSprint(sprint);
            }
        });
        sprints.forEach(sprint -> sprint.initialize(gc));
    }

    public void removePrint(Sprint sprint) {
        sprints.remove(sprint);
    }
}
