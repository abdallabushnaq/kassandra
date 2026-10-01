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

package de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.pompt;

import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.PromptGenerator;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.*;
import org.junit.jupiter.api.Test;

public class PromptGeneratorTest {

    @Test
    public void subject() {
        PromptGenerator generator = new PromptGenerator();
        String          endPrompt = generator.generate("a lone warrior standing before an ancient castle");
        System.out.println(endPrompt);
    }

    @Test
    public void subject1() {
        PromptGenerator generator = new PromptGenerator();
        Prompt prompt = Prompt.builder()
                .camera(CameraPrompt.AERIAL)
                .lighting(LightingPrompt.DRAMATIC_CHIAROSCURO)
                .style(StylePrompt.CINEMATIC_SCI_FI)
                .quality(QualityPrompt.CINEMATIC)
                .build();
        String endPrompt = generator.generate("a space ship standing before an ancient base", prompt);
        System.out.println(endPrompt);
    }

    @Test
    public void subject_x_5() {
        PromptGenerator generator = new PromptGenerator();
        for (int i = 0; i < 5; i++) {
            String endPrompt = generator.generate("a space ship standing before an ancient base");
            System.out.println();
            System.out.println(endPrompt);
            System.out.println();
        }
    }

}
