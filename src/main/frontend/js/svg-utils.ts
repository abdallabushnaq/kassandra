// svg-utils.ts
// Shared SVG utility functions for chart rendering.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

const SVG_NS = 'http://www.w3.org/2000/svg';

/** Attribute map accepted by all SVG helpers. */
export type SvgAttrs = Record<string, string | number | null | undefined>;

/**
 * Creates an SVG element with attributes and optional text content.
 */
export function createSvgElement<K extends keyof SVGElementTagNameMap>(
    tag: K,
    attrs?: SvgAttrs,
    textContent?: string | null
): SVGElementTagNameMap[K] {
    const element = document.createElementNS(SVG_NS, tag) as SVGElementTagNameMap[K];
    if (attrs) {
        for (const key of Object.keys(attrs)) {
            const val = attrs[key];
            if (val != null) element.setAttribute(key, String(val));
        }
    }
    if (textContent != null) element.textContent = textContent;
    return element;
}

/**
 * Creates an SVG rectangle element with specified dimensions and attributes.
 */
export function createRect(
    x: number, y: number, width: number, height: number,
    additionalAttrs?: SvgAttrs
): SVGRectElement {
    return createSvgElement('rect', {
        x, y,
        width:  Math.max(0, width),
        height: Math.max(0, height),
        ...additionalAttrs,
    });
}

/**
 * Creates an SVG text element with specified position and attributes.
 */
export function createText(
    x: number, y: number, content: string,
    additionalAttrs?: SvgAttrs
): SVGTextElement {
    return createSvgElement('text', { x, y, ...additionalAttrs }, content);
}

/**
 * Creates an SVG line element with specified endpoints and attributes.
 */
export function createLine(
    x1: number, y1: number, x2: number, y2: number,
    additionalAttrs?: SvgAttrs
): SVGLineElement {
    return createSvgElement('line', { x1, y1, x2, y2, ...additionalAttrs });
}

/**
 * Creates an SVG circle element.
 */
export function createCircle(
    cx: number, cy: number, r: number,
    additionalAttrs?: SvgAttrs
): SVGCircleElement {
    return createSvgElement('circle', { cx, cy, r, ...additionalAttrs });
}

/**
 * Creates an SVG clip path definition for rectangular clipping regions.
 */
export function createClipPath(
    id: string, x: number, y: number, width: number, height: number
): SVGDefsElement {
    const defs     = createSvgElement('defs');
    const clipPath = createSvgElement('clipPath', { id });
    clipPath.appendChild(createRect(x, y, width, height, {}));
    defs.appendChild(clipPath);
    return defs;
}

