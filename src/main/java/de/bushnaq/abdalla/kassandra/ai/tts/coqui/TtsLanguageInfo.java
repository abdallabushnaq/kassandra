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

public class TtsLanguageInfo {
    @JsonProperty("current_model")
    private String currentModel;

    @JsonProperty("is_multi_lingual")
    private boolean      isMultiLingual;
    @JsonProperty("language_count")
    private int          languageCount;
    @JsonProperty("languages")
    private List<String> languages;

    // Default constructor for Jackson
    public TtsLanguageInfo() {
    }

    // Getters
    public String getCurrentModel() {
        return currentModel;
    }

    public int getLanguageCount() {
        return languageCount;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public boolean isMultiLingual() {
        return isMultiLingual;
    }

    // Setters
    public void setCurrentModel(String currentModel) {
        this.currentModel = currentModel;
    }

    public void setLanguageCount(int languageCount) {
        this.languageCount = languageCount;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public void setMultiLingual(boolean multiLingual) {
        isMultiLingual = multiLingual;
    }

    @Override
    public String toString() {
        return "TtsLanguageInfo{" +
                "currentModel='" + currentModel + '\'' +
                ", isMultiLingual=" + isMultiLingual +
                ", languages=" + languages +
                ", languageCount=" + languageCount +
                '}';
    }
}
