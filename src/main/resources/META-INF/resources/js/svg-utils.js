// svg-utils.js
// Shared SVG utility functions for chart rendering.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    const SVG_NS = 'http://www.w3.org/2000/svg';

    /**
     * Creates an SVG element with attributes and optional text content.
     * @param {string} tag The SVG element tag name (e.g., 'rect', 'text', 'g')
     * @param {Object} attrs Optional object of attribute key-value pairs to apply
     * @param {string} textContent Optional text content for the element
     * @returns {SVGElement} The created SVG element
     */
    function createSvgElement(tag, attrs, textContent) {
        const element = document.createElementNS(SVG_NS, tag);
        if (attrs) {
            for (const key of Object.keys(attrs)) {
                if (attrs[key])
                    element.setAttribute(key, String(attrs[key]));
            }
        }
        if (textContent != null) element.textContent = textContent;
        return element;
    }

    /**
     * Creates an SVG rectangle element with specified dimensions and attributes.
     * Note: crispEdges was previously applied for pixel-sharp borders but disabled.
     * @param {number} x The x coordinate of the rectangle
     * @param {number} y The y coordinate of the rectangle
     * @param {number} width The width of the rectangle
     * @param {number} height The height of the rectangle
     * @param {Object} additionalAttrs Optional additional SVG attributes to apply
     * @returns {SVGRectElement} The created rectangle element
     */
    function createRect(x, y, width, height, additionalAttrs) {
        return createSvgElement('rect', Object.assign({
            x,
            y,
            width: Math.max(0, width),
            height: Math.max(0, height)
        }, additionalAttrs));
    }

    /**
     * Creates an SVG text element with specified position and attributes.
     * @param {number} x The x coordinate for the text baseline
     * @param {number} y The y coordinate for the text baseline
     * @param {string} content The text content to display
     * @param {Object} additionalAttrs Optional additional SVG attributes to apply
     * @returns {SVGTextElement} The created text element
     */
    function createText(x, y, content, additionalAttrs) {
        return createSvgElement('text', Object.assign({x, y}, additionalAttrs), content);
    }

    /**
     * Creates an SVG line element with specified endpoints and attributes.
     * @param {number} x1 The x coordinate of the line start
     * @param {number} y1 The y coordinate of the line start
     * @param {number} x2 The x coordinate of the line end
     * @param {number} y2 The y coordinate of the line end
     * @param {Object} additionalAttrs Optional additional SVG attributes
     * @returns {SVGLineElement} The created line element
     */
    function createLine(x1, y1, x2, y2, additionalAttrs) {
        return createSvgElement('line', Object.assign({x1, y1, x2, y2}, additionalAttrs));
    }

    function createCircle(x1, y1, r, additionalAttrs) {
        return createSvgElement('circle', Object.assign({x1, y1, r}, additionalAttrs));
    }

    /**
     * Creates an SVG clip path definition for rectangular clipping regions.
     * @param {string} id The unique identifier for the clip path
     * @param {number} x The x coordinate of the clipping region
     * @param {number} y The y coordinate of the clipping region
     * @param {number} width The width of the clipping region
     * @param {number} height The height of the clipping region
     * @returns {SVGDefsElement} A defs element containing the clip path definition
     */
    function createClipPath(id, x, y, width, height) {
        const defsElement = createSvgElement('defs');
        const clipPathElement = createSvgElement('clipPath', {id});
        clipPathElement.appendChild(createRect(x, y, width, height, {}));
        defsElement.appendChild(clipPathElement);
        return defsElement;
    }

    // Export to global scope
    window.SvgUtils = {
        createSvgElement,
        createRect,
        createText,
        createLine,
        createCircle,
        createClipPath
    };
})();
