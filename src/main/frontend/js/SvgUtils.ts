// svg-utils.ts
// Shared SVG utility functions for chart rendering.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

const SVG_NS = 'http://www.w3.org/2000/svg';

/** Attribute map accepted by all SVG helpers. */
export type SvgAttrs = Record<string, string | number | null | undefined>;

/**
 * Utility class for SVG element creation.
 */
export class SvgUtils {
    /**
     * Creates an SVG element with attributes and optional text content.
     */
    static createSvgElement<K extends keyof SVGElementTagNameMap>(
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
    static createRect(
        x: number, y: number, width: number, height: number,
        additionalAttrs?: SvgAttrs
    ): SVGRectElement {
        return SvgUtils.createSvgElement('rect', {
            x, y,
            width: Math.max(0, width),
            height: Math.max(0, height),
            ...additionalAttrs,
        });
    }

    /**
     * Creates an SVG group element with specified attributes.
     */
    static createGroup(
        x: number, y: number,
        additionalAttrs?: SvgAttrs
    ): SVGGElement {
        const group = document.createElementNS(SVG_NS, 'g');
        group.setAttribute('transform', `translate(${x}, ${y})`);
        if (additionalAttrs) {
            for (const key of Object.keys(additionalAttrs)) {
                const val = additionalAttrs[key];
                if (val != null) group.setAttribute(key, String(val));
            }
        }
        return group;
    }

    /**
     * Creates an SVG text element with specified position and attributes.
     */
    static createText(
        x: number, y: number, content: string,
        additionalAttrs?: SvgAttrs
    ): SVGTextElement {
        return SvgUtils.createSvgElement('text', {x, y, ...additionalAttrs}, content);
    }

    /**
     * Creates an SVG line element with specified endpoints and attributes.
     */
    static createLine(
        x1: number, y1: number, x2: number, y2: number,
        additionalAttrs?: SvgAttrs
    ): SVGLineElement {
        return SvgUtils.createSvgElement('line', {x1, y1, x2, y2, ...additionalAttrs});
    }

    /**
     * Creates an SVG circle element.
     */
    static createCircle(
        cx: number, cy: number, r: number,
        additionalAttrs?: SvgAttrs
    ): SVGCircleElement {
        return SvgUtils.createSvgElement('circle', {cx, cy, r, ...additionalAttrs});
    }

    /**
     * Creates an SVG clip path definition for rectangular clipping regions.
     */
    static createClipPath(
        id: string, x: number, y: number, width: number, height: number
    ): SVGDefsElement {
        const defs = SvgUtils.createSvgElement('defs');
        const clipPath = SvgUtils.createSvgElement('clipPath', {id});
        clipPath.appendChild(SvgUtils.createRect(x, y, width, height, {}));
        defs.appendChild(clipPath);
        return defs;
    }
}

