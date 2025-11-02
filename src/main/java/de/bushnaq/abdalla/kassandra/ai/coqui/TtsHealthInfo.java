package de.bushnaq.abdalla.kassandra.ai.coqui;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TtsHealthInfo {
    @JsonProperty("current_model")
    private String  currentModel;
    @JsonProperty("current_vocoder")
    private String  currentVocoder;
    @JsonProperty("model_loaded")
    private boolean modelLoaded;
    @JsonProperty("status")
    private String  status;

    // Default constructor for Jackson
    public TtsHealthInfo() {
    }

    public String getCurrentModel() {
        return currentModel;
    }

    public String getCurrentVocoder() {
        return currentVocoder;
    }

    // Getters
    public String getStatus() {
        return status;
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public void setCurrentModel(String currentModel) {
        this.currentModel = currentModel;
    }

    public void setCurrentVocoder(String currentVocoder) {
        this.currentVocoder = currentVocoder;
    }

    public void setModelLoaded(boolean modelLoaded) {
        this.modelLoaded = modelLoaded;
    }

    // Setters
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "TtsHealthInfo{" +
                "status='" + status + '\'' +
                ", modelLoaded=" + modelLoaded +
                ", currentModel='" + currentModel + '\'' +
                ", currentVocoder='" + currentVocoder + '\'' +
                '}';
    }
}
