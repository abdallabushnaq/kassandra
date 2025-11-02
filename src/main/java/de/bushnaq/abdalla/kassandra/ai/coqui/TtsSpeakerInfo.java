package de.bushnaq.abdalla.kassandra.ai.coqui;

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
