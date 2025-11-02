/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.ai.indextts;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for Index TTS service
 * https://github.com/index-tts/index-tts
 * <p>
 * Index TTS provides high-quality text-to-speech with emotional expression support.
 *
 * <p><strong>IMPORTANT: Voice Selection</strong></p>
 * <p>Index TTS uses voice cloning via audio reference files, NOT predefined voice names.
 * The 'voice' parameter should be a path to a WAV file containing a sample of the voice
 * you want to clone. The getVoices() method returns placeholder names that are not actually
 * supported by the underlying service.</p>
 *
 * <p><strong>How to use voices:</strong></p>
 * <ul>
 *   <li>Provide a path to a WAV file as the voice parameter (e.g., "/voices/my_voice.wav")</li>
 *   <li>The server will use this audio file as a reference to clone that voice</li>
 *   <li>Pass null to use the server's default voice reference</li>
 * </ul>
 */
public class IndexTTS {
    private static final ObjectMapper objectMapper    = new ObjectMapper();
    private static       String       TTS_SERVICE_URL = "http://localhost:5124";

    /**
     * Delete a voice reference file from the server
     *
     * @param filename The filename to delete (e.g., "my_voice.wav")
     */
    public static void deleteVoiceReference(String filename) throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/v1/voice-references/" + filename);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to delete voice reference " + responseCode + ": " + error);
        }
    }

    /**
     * Generate speech with default parameters and default voice reference
     */
    public static byte[] generateSpeech(String text) throws Exception {
        return generateSpeech(text, null, null, null, null, null, null, null, null);
    }

    /**
     * Generate speech with full parameter control including emotional parameters
     *
     * @param text            Input text
     * @param voiceReference  Path to voice reference WAV file for cloning (server-side path), null for default.
     *                        NOTE: This is NOT a predefined voice name - Index TTS uses voice cloning.
     * @param speed           Speech speed (0.5 to 2.0), null for default (1.0)
     * @param emotionAngry    Anger emotion level (0.0 to 1.0), null for none
     * @param emotionHappy    Happiness emotion level (0.0 to 1.0), null for none
     * @param emotionSad      Sadness emotion level (0.0 to 1.0), null for none
     * @param emotionSurprise Surprise emotion level (0.0 to 1.0), null for none
     * @param emotionNeutral  Neutral emotion level (0.0 to 1.0), null for default
     * @param temperature     Sampling temperature for variation, null for default
     */
    public static byte[] generateSpeech(String text, String voiceReference, Float speed,
                                        Float emotionAngry, Float emotionHappy, Float emotionSad,
                                        Float emotionSurprise, Float emotionNeutral, Float temperature) throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/v1/audio/speech");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("input", text);

        // Add optional parameters only if provided
        if (voiceReference != null) jsonMap.put("voice_reference", voiceReference);
        if (speed != null) jsonMap.put("speed", speed);
        if (temperature != null) jsonMap.put("temperature", temperature);

        // Add emotional parameters if any are provided
        Map<String, Float> emotions = new HashMap<>();
        if (emotionAngry != null) emotions.put("angry", emotionAngry);
        if (emotionHappy != null) emotions.put("happy", emotionHappy);
        if (emotionSad != null) emotions.put("sad", emotionSad);
        if (emotionSurprise != null) emotions.put("surprise", emotionSurprise);
        if (emotionNeutral != null) emotions.put("neutral", emotionNeutral);

        if (!emotions.isEmpty()) {
            jsonMap.put("emotions", emotions);
        }

        String jsonPayload = objectMapper.writeValueAsString(jsonMap);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Index TTS service returned error " + responseCode + ": " + error);
        }

        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int    bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Get available emotions supported by the service
     */
    public static String[] getEmotions() throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/emotions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to get emotions " + responseCode + ": " + error);
        }

        String    response    = readProcessOutput(conn.getInputStream());
        Map<?, ?> map         = objectMapper.readValue(response, Map.class);
        Object    emotionsObj = map.get("emotions");

        if (emotionsObj instanceof List<?> emotionsList) {
            return emotionsList.stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
        } else {
            throw new RuntimeException("Unexpected response format: 'emotions' field missing or not a list");
        }
    }

    /**
     * Get list of available models
     */
    public static String[] getModels() throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/models");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to get models " + responseCode + ": " + error);
        }

        String    response  = readProcessOutput(conn.getInputStream());
        Map<?, ?> map       = objectMapper.readValue(response, Map.class);
        Object    modelsObj = map.get("models");

        if (modelsObj instanceof List<?> modelsList) {
            return modelsList.stream()
                    .map(m -> {
                        if (m instanceof Map<?, ?> modelMap) {
                            return modelMap.get("id").toString();
                        }
                        return m.toString();
                    })
                    .toArray(String[]::new);
        } else {
            throw new RuntimeException("Unexpected response format: 'models' field missing or not a list");
        }
    }

    /**
     * List available voice reference files on the server
     * <p>
     * Returns information about WAV files that can be used for voice cloning.
     */
    public static VoiceReference[] listVoiceReferences() throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/v1/voice-references");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to list voice references " + responseCode + ": " + error);
        }

        String    response = readProcessOutput(conn.getInputStream());
        Map<?, ?> jsonMap  = objectMapper.readValue(response, Map.class);
        List<?>   refsData = (List<?>) jsonMap.get("voice_references");

        VoiceReference[] refs = new VoiceReference[refsData.size()];
        for (int i = 0; i < refsData.size(); i++) {
            Map<?, ?> data = (Map<?, ?>) refsData.get(i);
            refs[i] = new VoiceReference(
                    (String) data.get("filename"),
                    (String) data.get("path"),
                    ((Number) data.get("size_bytes")).longValue(),
                    ((Number) data.get("modified_timestamp")).doubleValue()
            );
        }

        return refs;
    }

    private static String readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().reduce("", (a, b) -> a + "\n" + b);
        }
    }

    public static void setServiceUrl(String url) {
        TTS_SERVICE_URL = url;
    }

    /**
     * Upload a voice reference WAV file to the server
     *
     * @param localFilePath Path to the local WAV file to upload
     * @return Information about the uploaded file
     */
    public static VoiceReference uploadVoiceReference(String localFilePath) throws Exception {
        File file = new File(localFilePath);
        if (!file.exists()) {
            throw new Exception("File not found: " + localFilePath);
        }
        if (!localFilePath.toLowerCase().endsWith(".wav")) {
            throw new Exception("Only WAV files are supported");
        }

        String            boundary = "----IndexTTSBoundary" + System.currentTimeMillis();
        URL               url      = new URL(TTS_SERVICE_URL + "/v1/voice-references");
        HttpURLConnection conn     = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream();
             FileInputStream fileIn = new FileInputStream(file)) {

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);

            // Write file part
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(file.getName()).append("\"\r\n");
            writer.append("Content-Type: audio/wav\r\n\r\n");
            writer.flush();

            // Copy file data
            byte[] buffer = new byte[4096];
            int    bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

            writer.append("\r\n");
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to upload voice reference " + responseCode + ": " + error);
        }

        String    response = readProcessOutput(conn.getInputStream());
        Map<?, ?> jsonMap  = objectMapper.readValue(response, Map.class);
        return new VoiceReference(
                (String) jsonMap.get("filename"),
                (String) jsonMap.get("path"),
                ((Number) jsonMap.get("size_bytes")).longValue(),
                0.0 // timestamp not provided in upload response
        );
    }

    public static void writeWav(byte[] audioData, String fileName) throws IOException {
        Path outputPath = Paths.get(fileName);
        Files.write(outputPath, audioData);
        System.out.printf("Audio saved to '%s'.\n", outputPath.toAbsolutePath());
    }

    /**
     * Represents a voice reference file on the server
     */
    public record VoiceReference(String filename, String path, long sizeBytes, double modifiedTimestamp) {

        @Override
        public String toString() {
            return String.format("VoiceReference{filename='%s', path='%s', size=%d bytes}",
                    filename, path, sizeBytes);
        }
    }
}

