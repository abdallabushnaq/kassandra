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

package de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl;


public enum Lora {
    WATERCOLORS("<lora:- SDXL - ptrn-no1_style_V1.0:1> ptrn-no1"),
    MOSAIC("<lora:Abstract_Mosaic_Pattern:0.5> Abstract_Mosaic_Pattern"),
    ABSTRACT("<lora:AbstractPatternStyleXL:1> AbstractPatternStyle"),
    CIRCUIT("<lora:Circuit_pattern:1> circuitpattern"),
    NOVUSCHROMA66("<lora:novuschroma66 style_:1.3> novuschroma66 style swirl patterns"),
    DISINTEGRATING("<lora:ral-dstgrtptrn-sdxl:1> ral-dstgrtptrn"),
    GOLDEN("<lora:Test_Golden_Patterns.:1> gold patterns, gold and black spirit, liquid gold explosion, golden smoke magic, star dust, golden milky way, black background, magic fog."),
    ZARABI("<lora:Zarabi:1> adrr-zrb, patterns, intricate weavings, rich colors"),
    HARDWOOD(" <lora:myststyle-hardwood-universe-xl-v2:1> everything made of wood grains design, living hardwood objects, varied grain patterns surfaces, sharp edges, earth-tone color palette, glassy reflective textures, glossy sheen, every surface covered in varied grain patterns, hardwood theme, all objects covered in varied grain patterns, wooden art, wooden, entire scene is hardwood || w0_o8_w2-enhanced-style || w0_o8_w2 || hardwood theme, hardwood universe, hardwood universe style, hardwood universe theme, hardwood universe design, hardwood universe aesthetic, hardwood universe concept, hardwood universe motif, hardwood universe pattern, hardwood universe texture, hardwood universe finish, hardwood universe surface, hardwood universe detail, hardwood universe element, hardwood universe feature, hardwood universe characteristic, hardwood universe quality, hardwood universe attribute, hardwood universe trait, hardwood universe aspect, hardwood universe property || w0_o8_w2-enhanced-style || w0_o8_w2 || hardwood theme || w0_o8_w2-enhanced-style || w0_o8_w2 || hardwood theme || w0_o8_w2-enhanced-style || w0_o8_w2 || hardwood theme || w0_o8_w2-enhanced-style || w0_o8_w2 || hardwood theme || w0_o8_w2-enhanced-style || w0_o8_w2 || hardwood theme || w0_o8_w2-enhanced-style || w0_o8_w2 || hardwood theme || w0_o8_w2-enhanced-style || w0_o8_w2 || hardwood theme");


    private final String lor;

    Lora(String lor) {
        this.lor = lor;
    }

    public String getLora() {
        return lor;
    }

}

