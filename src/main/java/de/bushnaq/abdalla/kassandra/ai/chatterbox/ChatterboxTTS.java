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

package de.bushnaq.abdalla.kassandra.ai.chatterbox;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ChatterboxTTS {
    private static final ObjectMapper objectMapper    = new ObjectMapper();
    private static       String       TTS_SERVICE_URL = "http://localhost:4123";

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
     * Generate speech with voice cloning support
     *
     * @param text            Text to convert to speech
     * @param audioPromptPath Path to audio prompt WAV file for voice cloning (server-side path), null for default voice
     * @param language        Language code (e.g., 'en', 'fr', 'es'), null for English
     * @param temperature     Legacy parameter (unused, kept for compatibility)
     * @param exaggeration    Legacy parameter (unused, kept for compatibility)
     * @param cfgWeight       Legacy parameter (unused, kept for compatibility)
     * @return Audio data as WAV bytes
     */
    public static byte[] generateSpeech(String text, String audioPromptPath, String language,
                                        float temperature, float exaggeration, float cfgWeight) throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/v1/audio/speech");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        java.util.Map<String, Object> jsonMap = new java.util.HashMap<>();
        jsonMap.put("input", text);
        if (audioPromptPath != null) jsonMap.put("audio_prompt_path", audioPromptPath);
        if (language != null) jsonMap.put("language", language);
        jsonMap.put("temperature", temperature);
        jsonMap.put("exaggeration", exaggeration);
        jsonMap.put("cfg_weight", cfgWeight);
        String jsonPayload = objectMapper.writeValueAsString(jsonMap);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Chatterbox TTS service returned error " + responseCode + ": " + error);
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

    public static byte[] generateSpeech(String text, float temperature, float exaggeration, float cfgWeight) throws Exception {
        return generateSpeech(text, null, null, temperature, exaggeration, cfgWeight);
    }

    public static String[] getLanguages() throws Exception {
        URL               url  = new URL(TTS_SERVICE_URL + "/languages");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readProcessOutput(conn.getErrorStream());
            throw new RuntimeException("Failed to get languages " + responseCode + ": " + error);
        }
        String response = readProcessOutput(conn.getInputStream());
        // Parse as Map and extract 'languages' array
        java.util.Map<?, ?> map          = objectMapper.readValue(response, java.util.Map.class);
        Object              languagesObj = map.get("languages");
        if (languagesObj instanceof List<?> languagesList) {
            List<String> codes = new java.util.ArrayList<>();
            for (Object langObj : languagesList) {
                if (langObj instanceof java.util.Map<?, ?> langMap) {
                    Object code = langMap.get("code");
                    if (code != null) codes.add(code.toString());
                }
            }
            return codes.toArray(new String[0]);
        } else {
            throw new RuntimeException("Unexpected response format: 'languages' field missing or not a list");
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

        String              response = readProcessOutput(conn.getInputStream());
        java.util.Map<?, ?> jsonMap  = objectMapper.readValue(response, java.util.Map.class);
        List<?>             refsData = (List<?>) jsonMap.get("voice_references");

        VoiceReference[] refs = new VoiceReference[refsData.size()];
        for (int i = 0; i < refsData.size(); i++) {
            java.util.Map<?, ?> data = (java.util.Map<?, ?>) refsData.get(i);
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
        java.io.File file = new java.io.File(localFilePath);
        if (!file.exists()) {
            throw new Exception("File not found: " + localFilePath);
        }
        if (!localFilePath.toLowerCase().endsWith(".wav")) {
            throw new Exception("Only WAV files are supported");
        }

        String            boundary = "----ChatterboxBoundary" + System.currentTimeMillis();
        URL               url      = new URL(TTS_SERVICE_URL + "/v1/voice-references");
        HttpURLConnection conn     = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream();
             java.io.FileInputStream fileIn = new java.io.FileInputStream(file)) {

            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8), true);

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

        String              response = readProcessOutput(conn.getInputStream());
        java.util.Map<?, ?> jsonMap  = objectMapper.readValue(response, java.util.Map.class);
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
