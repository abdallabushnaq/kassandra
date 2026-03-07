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

package de.bushnaq.abdalla.kassandra.ai.tts.coqui;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TtsModelList {
    @JsonProperty("models")
    private List<String> models;

    // Default constructor for Jackson
    public TtsModelList() {
    }

    // Getters and setters
    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    @Override
    public String toString() {
        return "TtsModelList{" +
                "models=" + models +
                '}';
    }
}
