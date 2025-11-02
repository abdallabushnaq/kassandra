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

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class Version extends AbstractTimeAware implements Comparable<Version> {

    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private List<Feature> features = new ArrayList<>();
    private Long          id;
    private String        name;
    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private Product       product;
    private Long          productId;

    public Feature addFeature(Feature feature) {
        features.add(feature);
        feature.setVersion(this);
        return feature;
    }

    @Override
    public int compareTo(Version other) {
        return this.id.compareTo(other.id);
    }

    @JsonIgnore
    public String getKey() {
        return "V-" + id;
    }

    public void initialize(GanttContext gc) {
        features.clear();
        gc.allFeatures.forEach(project -> {
            if (project.getVersionId() == id) {
                addFeature(project);
            }
        });
        features.forEach(project -> project.initialize(gc));
    }

    public void removeProject(Feature feature) {
        features.remove(feature);
    }
}
