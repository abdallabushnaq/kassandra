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

public class TtsSpeakerInfo {
    @JsonProperty("current_model")
    private String currentModel;

    @JsonProperty("is_multi_speaker")
    private boolean      isMultiSpeaker;
    @JsonProperty("speaker_count")
    private int          speakerCount;
    @JsonProperty("speakers")
    private List<String> speakers;

    // Default constructor for Jackson
    public TtsSpeakerInfo() {
    }

    // Getters
    public String getCurrentModel() {
        return currentModel;
    }

    public int getSpeakerCount() {
        return speakerCount;
    }

    public List<String> getSpeakers() {
        return speakers;
    }

    public boolean isMultiSpeaker() {
        return isMultiSpeaker;
    }

    // Setters
    public void setCurrentModel(String currentModel) {
        this.currentModel = currentModel;
    }

    public void setMultiSpeaker(boolean multiSpeaker) {
        isMultiSpeaker = multiSpeaker;
    }

    public void setSpeakerCount(int speakerCount) {
        this.speakerCount = speakerCount;
    }

    public void setSpeakers(List<String> speakers) {
        this.speakers = speakers;
    }

    @Override
    public String toString() {
        return "TtsSpeakerInfo{" +
                "currentModel='" + currentModel + '\'' +
                ", isMultiSpeaker=" + isMultiSpeaker +
                ", speakers=" + speakers +
                ", speakerCount=" + speakerCount +
                '}';
    }
}
