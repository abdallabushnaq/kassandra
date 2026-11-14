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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

import {ColorUtils} from '../ColorUtils.js';
import {ActualInspectionPoint, BurndownRenderer} from './BurndownRenderer.js';

/** Matches DateUtil.createWorkDayDurationString with seconds and leading zeroes disabled. */
export function formatWorkingDuration(seconds: number): string {
    const negative = seconds < 0;
    let remaining = Math.abs(Math.trunc(seconds));
    const parts = [135000, 27000, 3600, 60].map(unit => {
        const value = Math.floor(remaining / unit);
        remaining %= unit;
        return value;
    });
    const suffixes = ['w', 'd', 'h', 'm'];
    let result = '';
    let started = false;
    parts.forEach((value, index) => {
        if (value || started) {
            if (value)
                result += (started && value < 10 ? '0' : '') + value + suffixes[index];
            if (index < parts.length - 1)
                result += ' ';
            else if (!value)
                result += '0m';
            started = true;
        }
    });
    return (negative ? '-' : '') + (result || '0m');
}

/** Transient hover inspection of the rendered actual-work curve. */
export class BurndownPointInteraction {
    private readonly readout: HTMLDivElement;
    private pointer: {x: number; y: number} | null = null;
    private frame: number | null = null;
    private guide: SVGLineElement | null = null;
    private marker: SVGCircleElement | null = null;

    constructor(private readonly viewport: HTMLElement, private readonly renderer: BurndownRenderer) {
        this.readout = viewport.ownerDocument.createElement('div');
        this.readout.className = 'burndown-point-readout';
        this.readout.setAttribute('role', 'tooltip');
        Object.assign(this.readout.style, {
            position: 'absolute', pointerEvents: 'none', zIndex: '1000', display: 'none',
            padding: '8px 10px', borderRadius: '6px', background: 'rgba(15,23,42,0.94)',
            color: '#fff', font: '12px/1.4 sans-serif', whiteSpace: 'pre-line',
        });
    }

    afterRender(): void {
        const layer = this.renderer.inspectionLayer;
        if (!layer)
            return;
        const doc = this.viewport.ownerDocument;
        const color = ColorUtils.intToHex(this.renderer.theme.burndownTheme.borderColor);
        for (const point of this.renderer.actualInspectionPoints) {
            const circle = doc.createElementNS('http://www.w3.org/2000/svg', 'circle');
            circle.setAttribute('class', 'burndown-actual-point');
            circle.setAttribute('cx', String(point.x));
            circle.setAttribute('cy', String(point.y));
            circle.setAttribute('r', '2');
            circle.setAttribute('fill', color);
            circle.dataset.date = this.dateKey(point.date);
            circle.dataset.seconds = String(point.seconds);
            circle.dataset.author = point.authorName;
            circle.dataset.authorSeconds = String(point.authorSeconds);
            layer.appendChild(circle);
        }
        this.guide = doc.createElementNS('http://www.w3.org/2000/svg', 'line');
        this.guide.setAttribute('class', 'burndown-point-guide');
        this.guide.setAttribute('stroke', color);
        this.guide.setAttribute('stroke-dasharray', '4 3');
        this.guide.setAttribute('vector-effect', 'non-scaling-stroke');
        this.marker = doc.createElementNS('http://www.w3.org/2000/svg', 'circle');
        this.marker.setAttribute('class', 'burndown-point-marker');
        this.marker.setAttribute('r', '6');
        this.marker.setAttribute('fill', 'red');
        layer.append(this.guide, this.marker);
        this.viewport.appendChild(this.readout);
        this.hide();
        if (this.pointer) {
            const win = doc.defaultView;
            if (win) {
                if (this.frame !== null)
                    win.cancelAnimationFrame(this.frame);
                this.frame = win.requestAnimationFrame(() => {
                    this.frame = null;
                    this.update();
                });
            }
        }
    }

    handlePointerMove(event: PointerEvent): void {
        if (event.buttons !== 0 || event.pointerType === 'touch') {
            this.clear();
            return;
        }
        this.pointer = {x: event.clientX, y: event.clientY};
        this.update();
    }

    clear(): void {
        this.pointer = null;
        this.hide();
    }

    destroy(): void {
        this.clear();
        if (this.frame !== null)
            this.viewport.ownerDocument.defaultView?.cancelAnimationFrame(this.frame);
        this.readout.remove();
    }

    private hide(): void {
        this.guide?.setAttribute('display', 'none');
        this.marker?.setAttribute('display', 'none');
        this.readout.style.display = 'none';
    }

    private update(): void {
        const layer = this.renderer.inspectionLayer;
        const matrix = layer?.getScreenCTM();
        const pointer = this.pointer;
        if (!layer || !matrix || !pointer || !this.guide || !this.marker) {
            this.hide();
            return;
        }
        let closest: ActualInspectionPoint | null = null;
        let distance = 10;
        for (const point of this.renderer.actualInspectionPoints) {
            const x = matrix.a * point.x + matrix.c * point.y + matrix.e;
            const y = matrix.b * point.x + matrix.d * point.y + matrix.f;
            const candidateDistance = Math.hypot(pointer.x - x, pointer.y - y);
            if (candidateDistance < distance) {
                closest = point;
                distance = candidateDistance;
            }
        }
        if (!closest) {
            this.hide();
            return;
        }
        this.guide.setAttribute('x1', String(closest.x));
        this.guide.setAttribute('x2', String(closest.x));
        this.guide.setAttribute('y1', String(this.renderer.diagram.y));
        this.guide.setAttribute('y2', String(this.renderer.diagram.y + this.renderer.diagram.height));
        this.guide.removeAttribute('display');
        this.marker.setAttribute('cx', String(closest.x));
        this.marker.setAttribute('cy', String(closest.y));
        this.marker.removeAttribute('display');
        const selected = closest;
        const overlapping = this.renderer.actualInspectionPoints.filter(point =>
            point.x === selected.x && Math.abs(point.y - selected.y) * Math.hypot(matrix.c, matrix.d) < 1);
        const authors = overlapping.map(point => `${point.authorName}: ${formatWorkingDuration(point.authorSeconds)}`);
        this.readout.textContent = `${this.dateKey(closest.date)} (end of day)\nTotal remaining: ${formatWorkingDuration(closest.seconds)}\n${authors.join('\n')}`;
        this.readout.dataset.date = this.dateKey(closest.date);
        this.readout.style.display = 'block';
        const rect = this.viewport.getBoundingClientRect();
        const left = Math.max(0, Math.min(pointer.x - rect.left + 12, rect.width - this.readout.offsetWidth));
        const top = Math.max(0, Math.min(pointer.y - rect.top + 12, rect.height - this.readout.offsetHeight));
        this.readout.style.left = `${left}px`;
        this.readout.style.top = `${top}px`;
    }

    private dateKey(date: Date): string {
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    }
}
