package de.bushnaq.abdalla.kassandra.ai.coqui;

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
