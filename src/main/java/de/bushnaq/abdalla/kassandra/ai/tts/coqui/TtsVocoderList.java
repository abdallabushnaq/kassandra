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

public class TtsVocoderList {
    @JsonProperty("vocoder_count")
    private int          vocoderCount;
    @JsonProperty("vocoders")
    private List<String> vocoders;

    // Default constructor for Jackson
    public TtsVocoderList() {
    }

    public int getVocoderCount() {
        return vocoderCount;
    }

    // Getters
    public List<String> getVocoders() {
        return vocoders;
    }

    public void setVocoderCount(int vocoderCount) {
        this.vocoderCount = vocoderCount;
    }

    // Setters
    public void setVocoders(List<String> vocoders) {
        this.vocoders = vocoders;
    }

    @Override
    public String toString() {
        return "TtsVocoderList{" +
                "vocoders=" + vocoders +
                ", vocoderCount=" + vocoderCount +
                '}';
    }
}
