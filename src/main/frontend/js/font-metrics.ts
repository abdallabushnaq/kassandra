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


import {FontSpec} from "./font-spec.js";

/**
 * Pixel-based font metrics for a single font specification.
 * Ascent and descent are in CSS pixels at the requested font size.
 */
export class FontMetrics {
    private canvas: HTMLCanvasElement;
    private ctx: CanvasRenderingContext2D;

    constructor(fontSpec: FontSpec) {
        this.canvas = document.createElement("canvas");
        const ctx = this.canvas.getContext("2d");

        if (!ctx) {
            throw new Error("Could not create canvas context");
        }

        this.ctx = ctx;
        this.ctx.font = `${fontSpec.size}px ${fontSpec.family}`;
    }

    stringWidth(text: string): number {
        return this.ctx.measureText(text).width;
    }

}