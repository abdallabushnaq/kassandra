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

import de.bushnaq.abdalla.kassandra.ai.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.narrator.NarratorAttribute;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("AiUnitTest")
public class TestIndexTTS {

    @Test
    public void compareWithChatterbox() throws Exception {
        String text = "The quick brown fox jumps over the lazy dog.";

        System.out.println("Playing with Chatterbox TTS...");
        Narrator chatterbox = new Narrator("tts/comparison");
        chatterbox.narrate(text);

        Thread.sleep(2000);

        System.out.println("Playing with Index TTS (neutral)...");
        Narrator indexTts = Narrator.withIndexTTS("tts/comparison");
        indexTts.narrate(text);

        Thread.sleep(2000);

        System.out.println("Playing with Index TTS (happy)...");
        indexTts.narrate(new NarratorAttribute().withHappy(0.8f), text);
    }

    @Test
    public void listEmotions() throws Exception {
        System.out.println("=== Available Index TTS Emotions ===");
        for (String emotion : IndexTTS.getEmotions()) {
            System.out.println(emotion);
        }
    }

    @Test
    public void listModels() throws Exception {
        System.out.println("=== Available Index TTS Models ===");
        for (String model : IndexTTS.getModels()) {
            System.out.println(model);
        }
    }

    @Test
    public void listVoices() throws Exception {
        System.out.println("=== Available Index TTS Voice References ===");
        System.out.println("Note: Index TTS uses voice cloning, not predefined voice names.");
        System.out.println("Voice references are WAV files that can be used for cloning.\n");

        IndexTTS.VoiceReference[] refs = IndexTTS.listVoiceReferences();
        if (refs.length == 0) {
            System.out.println("No voice references found on server.");
            System.out.println("Upload one using: IndexTTS.uploadVoiceReference(\"path/to/file.wav\")");
        } else {
            for (IndexTTS.VoiceReference ref : refs) {
                System.out.println(ref);
            }
        }
    }

    @Test
    public void testDifferentVoices() throws Exception {
        String text = "Good morning, my name is Jennifer Holleman. I am the product manager of Kassandra and I will be demonstrating the latest alpha version of the Kassandra project server to you today.";

        IndexTTS.VoiceReference[] refs = IndexTTS.listVoiceReferences();
        for (IndexTTS.VoiceReference ref : refs) {
            System.out.println("Testing voice reference: " + ref.filename());
            Narrator          narrator = Narrator.withIndexTTS("tts/TestIndexTTS/testDifferentVoices");
            NarratorAttribute na       = new NarratorAttribute();
            // Use the convenient withVoice method that takes just the filename
            na.withVoice(ref.filename());
            narrator.narrate(na, text);
            Thread.sleep(1000);
        }
    }

    @Test
    public void testEmotionalRange() throws Exception {
        String   text     = "And we got ourself a new product!";
        Narrator narrator = Narrator.withIndexTTS("tts/TestIndexTTS/testEmotionalRange");
        narrator.narrate(new NarratorAttribute().withVoice("chatterbox").withNeutral(0.3f), text);
        narrator.narrate(new NarratorAttribute().withVoice("chatterbox").withHappy(0.3f), text);
        narrator.narrate(new NarratorAttribute().withVoice("chatterbox").withSad(0.3f), text);
        narrator.narrate(new NarratorAttribute().withVoice("chatterbox").withAngry(0.3f), text);
        narrator.narrate(new NarratorAttribute().withVoice("chatterbox").withSurprise(0.3f), text);
    }

    @Test
    public void testSpeech_03_WithSpeed() throws Exception {
        Narrator          narrator = Narrator.withIndexTTS("tts/TestIndexTTS/testSpeech_03_WithSpeed");
        NarratorAttribute attrs    = new NarratorAttribute().withVoice("chatterbox").withSpeed(.7f);
        narrator.narrate(attrs, "This text is spoken slower than normal speed.");
    }

}

