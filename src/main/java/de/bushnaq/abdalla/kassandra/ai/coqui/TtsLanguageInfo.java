package de.bushnaq.abdalla.kassandra.ai.coqui;

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
