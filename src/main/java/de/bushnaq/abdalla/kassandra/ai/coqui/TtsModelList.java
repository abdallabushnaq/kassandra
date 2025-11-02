package de.bushnaq.abdalla.kassandra.ai.coqui;

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
