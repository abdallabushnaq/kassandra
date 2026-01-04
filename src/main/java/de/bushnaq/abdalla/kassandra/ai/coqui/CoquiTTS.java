package de.bushnaq.abdalla.kassandra.ai.coqui;


import tools.jackson.databind.json.JsonMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CoquiTTS {
    private static final int             MAX_STARTUP_WAIT_SECONDS = 300; // Increased to 5 minutes for build time
    private static final String          TTS_SERVICE_URL          = "http://localhost:5000";
    private static final JsonMapper      jsonMapper               = new JsonMapper();
    private static final TtsLanguageInfo languageInfo;
    private static final TtsSpeakerInfo  speakerInfo;

    static {
        try {
            speakerInfo  = CoquiTTS.listSpeakers();
            languageInfo = CoquiTTS.listLanguages();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] generateMinionSpeech(String text, int speakerId, Float pitchShift, Float speedFactor, Float formantShift) throws Exception {
        String speaker = null;
        if (speakerInfo.isMultiSpeaker() && speakerInfo.getSpeakerCount() > 1) {
            speaker = speakerInfo.getSpeakers().get(speakerId % speakerInfo.getSpeakerCount());
        }
        String language = null;
        if (languageInfo.getLanguageCount() > 0) {
            language = languageInfo.getLanguages().getFirst();
        }

        return generateMinionSpeech(text, speaker, language, pitchShift, speedFactor, formantShift);
    }

    // Basic speech generation methods
    public static byte[] generateMinionSpeech(String text, Float pitchShift, Float speedFactor, Float formantShift) throws Exception {
        String speaker = null;
        if (speakerInfo.isMultiSpeaker() && speakerInfo.getSpeakerCount() > 1) {
            speaker = speakerInfo.getSpeakers().getFirst();
        }
        String language = null;
        if (languageInfo.getLanguageCount() > 0) {
            language = languageInfo.getLanguages().getFirst();
        }

        return generateMinionSpeech(text, speaker, language, pitchShift, speedFactor, formantShift);
    }

    public static byte[] generateMinionSpeech(String text, String speaker, String language, Float pitchShift, Float speedFactor, Float formantShift) throws Exception {
//        Float             pitchShift   = 1f;
//        Float             speedFactor  = 1f;
//        Float             formantShift = 1f;
        URL               url  = new URL(TTS_SERVICE_URL + "/speak_minion_memory");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set up the request
//        System.out.println("Connecting to TTS service at " + TTS_SERVICE_URL);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Build JSON payload using Jackson for proper escaping
        java.util.Map<String, Object> jsonMap = new java.util.HashMap<>();
        jsonMap.put("text", text);

        if (speaker != null && !speaker.trim().isEmpty()) {
            jsonMap.put("speaker", speaker);
        }

        if (language != null && !language.trim().isEmpty()) {
            jsonMap.put("language", language);
        }

        if (pitchShift != null) {
            jsonMap.put("pitch_shift", pitchShift);
        }
        if (speedFactor != null) {
            jsonMap.put("speed_factor", speedFactor);
        }
        if (formantShift != null) {
            jsonMap.put("formant_shift", formantShift);
        }

        String jsonPayload = jsonMapper.writeValueAsString(jsonMap);
//        System.out.println("JSON payload: " + jsonPayload);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Check response code
        int responseCode = conn.getResponseCode();
//        System.out.println("TTS service response code: " + responseCode);
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("TTS service returned error " + responseCode + ": " + error + " for text: '" + text + "'.");
        }

        // Read the audio data
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int    bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            byte[] byteArray = baos.toByteArray();
//            System.out.println("TTS audio data received successfully " + byteArray.length + " bytes.");
            return byteArray;
        }
    }

    public static byte[] generateSpeech(String text, int speakerId) throws Exception {
        String speaker = null;
        if (speakerInfo.isMultiSpeaker() && speakerInfo.getSpeakerCount() > 1) {
            speaker = speakerInfo.getSpeakers().get(speakerId % speakerInfo.getSpeakerCount());
        }
        String language = null;
        if (languageInfo.getLanguageCount() > 0) {
            language = languageInfo.getLanguages().getFirst();
        }

        return generateSpeech(text, speaker, language);
    }

    // Basic speech generation methods
    public static byte[] generateSpeech(String text) throws Exception {
        String speaker = null;
        if (speakerInfo.isMultiSpeaker() && speakerInfo.getSpeakerCount() > 1) {
            speaker = speakerInfo.getSpeakers().getFirst();
        }
        String language = null;
        if (languageInfo.getLanguageCount() > 0) {
            language = languageInfo.getLanguages().getFirst();
        }

        return generateSpeech(text, speaker, language);
    }

    public static byte[] generateSpeech(String text, String speaker, String language) throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/speak");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set up the request
//        System.out.println("Connecting to TTS service at " + TTS_SERVICE_URL);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Build JSON payload using Jackson for proper escaping
        java.util.Map<String, Object> jsonMap = new java.util.HashMap<>();
        jsonMap.put("text", text);

        if (speaker != null && !speaker.trim().isEmpty()) {
            jsonMap.put("speaker", speaker);
        }

        if (language != null && !language.trim().isEmpty()) {
            jsonMap.put("language", language);
        }


        String jsonPayload = jsonMapper.writeValueAsString(jsonMap);
//        System.out.println("JSON payload: " + jsonPayload);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Check response code
        int responseCode = conn.getResponseCode();
//        System.out.println("TTS service response code: " + responseCode);
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("TTS service returned error " + responseCode + ": " + error);
        }

        // Read the audio data
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int    bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            byte[] byteArray = baos.toByteArray();
//            System.out.println("TTS audio data received successfully " + byteArray.length + " bytes.");
            return byteArray;
        }
    }

    public static TtsHealthInfo getHealth() throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/health");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set up the request
        System.out.println("Checking TTS service health");
        conn.setRequestMethod("GET");

        // Check response code
        int responseCode = conn.getResponseCode();
        System.out.println("Health check response code: " + responseCode);
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Health check failed " + responseCode + ": " + error);
        }

        // Read and parse JSON response
        String        response   = readProcessOutput(conn.getInputStream());
        TtsHealthInfo healthInfo = jsonMapper.readValue(response, TtsHealthInfo.class);
        System.out.println("TTS service health: " + healthInfo);
        return healthInfo;
    }

    public static TtsLanguageInfo listLanguages() throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/languages");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set up the request
        System.out.println("Listing available TTS languages");
        conn.setRequestMethod("GET");

        // Check response code
        int responseCode = conn.getResponseCode();
        System.out.println("List languages response code: " + responseCode);
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to list languages " + responseCode + ": " + error);
        }

        // Read and parse JSON response
        String          response     = readProcessOutput(conn.getInputStream());
        TtsLanguageInfo languageInfo = jsonMapper.readValue(response, TtsLanguageInfo.class);
        System.out.println("Available languages: " + languageInfo);
        return languageInfo;
    }

    public static TtsModelList listModels() throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/models");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set up the request
        System.out.println("Listing available TTS models");
        conn.setRequestMethod("GET");

        // Check response code
        int responseCode = conn.getResponseCode();
        System.out.println("List models response code: " + responseCode);
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to list models " + responseCode + ": " + error);
        }

        // Read and parse JSON response
        String       response  = readProcessOutput(conn.getInputStream());
        TtsModelList modelList = jsonMapper.readValue(response, TtsModelList.class);
        System.out.println("Available models: " + modelList.getModels().size() + " models found");
        return modelList;
    }

    public static TtsSpeakerInfo listSpeakers() throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/speakers");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set up the request
        System.out.println("Listing available TTS speakers");
        conn.setRequestMethod("GET");

        // Check response code
        int responseCode = conn.getResponseCode();
        System.out.println("List speakers response code: " + responseCode);
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to list speakers " + responseCode + ": " + error);
        }

        // Read and parse JSON response
        String         response    = readProcessOutput(conn.getInputStream());
        TtsSpeakerInfo speakerInfo = jsonMapper.readValue(response, TtsSpeakerInfo.class);
        System.out.println("Available speakers: " + speakerInfo);
        return speakerInfo;
    }

    public static TtsVocoderList listVocoders() throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/vocoders");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set up the request
        System.out.println("Listing available TTS vocoders");
        conn.setRequestMethod("GET");

        // Check response code
        int responseCode = conn.getResponseCode();
        System.out.println("List vocoders response code: " + responseCode);
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to list vocoders " + responseCode + ": " + error);
        }

        // Read and parse JSON response
        String         response    = readProcessOutput(conn.getInputStream());
        TtsVocoderList vocoderList = jsonMapper.readValue(response, TtsVocoderList.class);
        System.out.println("Available vocoders: " + vocoderList.getVocoderCount() + " vocoders found");
        return vocoderList;
    }

    public static String loadModel(String modelName, String vocoderName, Boolean useGpu) throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/load_model");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Set up the request
        System.out.println("Loading TTS model: " + modelName + (vocoderName != null ? " with vocoder: " + vocoderName : ""));
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Build JSON payload using Jackson for proper escaping
        java.util.Map<String, Object> jsonMap = new java.util.HashMap<>();
        jsonMap.put("model_name", modelName);

        if (vocoderName != null && !vocoderName.trim().isEmpty()) {
            jsonMap.put("vocoder_name", vocoderName);
        }

        if (useGpu != null) {
            jsonMap.put("gpu", useGpu);
        }

        String jsonPayload = jsonMapper.writeValueAsString(jsonMap);
        System.out.println("Load model JSON payload: " + jsonPayload);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Check response code
        int responseCode = conn.getResponseCode();
        System.out.println("Load model response code: " + responseCode);
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to load model " + responseCode + ": " + error);
        }

        // Read response
        String response = readProcessOutput(conn.getInputStream());
        System.out.println("Model loaded successfully: " + response);
        return response;
    }

    // Model management methods
    public static String loadModel(String modelName) throws Exception {
        return loadModel(modelName, null, null);
    }

    public static String loadModel(String modelName, String vocoderName) throws Exception {
        return loadModel(modelName, vocoderName, null);
    }

    private static String readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().reduce("", (a, b) -> a + "\n" + b);
        }
    }

    public static void writeWav(byte[] audioData, String fileName) throws IOException {
        Path outputPath = Paths.get(fileName);
        Files.write(outputPath, audioData);
        System.out.printf("Audio saved to '%s'.\n", outputPath.toAbsolutePath());
    }

}
