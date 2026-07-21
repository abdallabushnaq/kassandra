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
export class FontSpec {
    static readonly SANS_SERIF: string = 'Sans-Serif';
    static readonly BOLD: string = 'bold';
    static readonly PLAIN: string = 'normal';
    public family: string;
    public size: number;
    public weight: string;
    public maxAscent: number;

    constructor(family: string, size: number, weight: string) {
        this.family = family;
        this.size = size;
        this.weight = weight;

        const canvas = document.createElement("canvas");
        const ctx = canvas.getContext("2d")!;

        ctx.font = `${size}px ${family}`;

        const m = ctx.measureText("H");

        this.maxAscent = m.fontBoundingBoxAscent;
    }
}

