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

/** Contract implemented by timeline charts that support interactive navigation. */
export interface TimelineChart {
    chartHeight: number;
    verticalScrollEnabled: boolean;

    updateViewState(dayWidth: number, scrollOffset: number, containerWidth: number, containerHeight: number): void;

    render(container: HTMLElement): void;

    updateContentTransform(tx: number, ty: number, scale: number): void;

    updateSvgHeight(height: number): void;
}

/** Minimal renderer data needed to constrain the timeline viewport. */
export interface TimelineRenderer {
    days: number;
}

/** Public lifecycle operations returned by chart bundle factories. */
export interface ChartHandle {
    render(): void;

    schedule(): void;

    destroy(): void;
}

interface ViewState {
    dayWidth: number;
    scrollOffset: number;
}

/**
 * Configures the reusable interaction layer around one timeline chart.
 *
 * The chart-specific bundle supplies chart construction, the renderer timeline
 * properties, and the initial-scroll calculation. This class owns all mutable
 * browser interaction state and its cleanup.
 */
export interface InteractiveTimelineChartOptions<TChart extends TimelineChart> {
    container: HTMLElement;
    containerId: string;
    chart: TChart;
    renderer: TimelineRenderer;
    defaultDayWidth: number;
    minDayWidth: number;
    maxDayWidth: number;
    dayWidthZoomStep: number;
    initialScrollOffset: (dayWidth: number, containerWidth: number) => number;
    getContainerHeight?: () => number;
    beforeRender?: () => void;
    afterRender?: () => void;
}

interface RegisteredEventListener {
    type: string;
    listener: EventListener;
    options?: boolean | AddEventListenerOptions;
}

interface DragState {
    startX: number;
    startOffset: number;
    startY: number;
    startYOffset: number;
}

const VISUAL_ZOOM_STEP = 1.1;
const VISUAL_ZOOM_MIN = 0.1;
const VISUAL_ZOOM_MAX = 10.0;
const ZOOM_INDICATOR_DURATION_MS = 1500;
const LAZY_REDRAW_DURATION_MS = 100;

function viewStateKey(containerId: string): string {
    return 'kassandra.chart.' + containerId.replace(/-container$/, '') + '.view';
}

function loadViewState(containerId: string): ViewState | null {
    try {
        const raw = localStorage.getItem(viewStateKey(containerId));
        if (raw) {
            const state = JSON.parse(raw) as ViewState;
            if (typeof state.dayWidth === 'number' && typeof state.scrollOffset === 'number')
                return state;
        }
    } catch { /* unavailable */
    }
    return null;
}

function saveViewState(containerId: string, dayWidth: number, scrollOffset: number): void {
    try {
        localStorage.setItem(viewStateKey(containerId), JSON.stringify({dayWidth, scrollOffset}));
    } catch { /* quota */
    }
}

/**
 * Adds scrolling, panning, day-width zooming, visual zooming, persistence, and
 * lifecycle management to a rendered timeline chart.
 */
export class InteractiveTimelineChart<TChart extends TimelineChart> implements ChartHandle {
    private readonly registeredEventListeners: RegisteredEventListener[] = [];
    private dayWidth: number;
    private scrollOffset = 0;
    private renderedScrollOffset = 0;
    private lazyRedrawTimerId: ReturnType<typeof setTimeout> | null = null;
    private visualScale = 1.0;
    private visualPanX = 0;
    private visualPanY = 0;
    private scrollYOffset = 0;
    private saveTimerId: ReturnType<typeof setTimeout> | null = null;
    private animationFrameId: number | null = null;
    private dragState: DragState | null = null;
    private resizeObserver: ResizeObserver | null = null;
    private zoomIndicator: HTMLDivElement | null = null;
    private zoomIndicatorHideTimerId: ReturnType<typeof setTimeout> | null = null;
    private zoomIndicatorExpiresAt = 0;

    constructor(private readonly options: InteractiveTimelineChartOptions<TChart>) {
        this.dayWidth = options.defaultDayWidth;
        this.restoreViewState();
        this.registerEventListeners();
        this.observeResize();
    }

    /** Renders the chart immediately. */
    public render(): void {
        this.redrawChart();
    }

    /** Schedules a chart render on the next animation frame. */
    public schedule(): void {
        this.scheduleRender();
    }

    /** Registers a chart-specific event listener that will be removed on destroy. */
    public addEventListener(
        type: string,
        listener: EventListener,
        options?: boolean | AddEventListenerOptions,
    ): void {
        this.options.container.addEventListener(type, listener, options);
        this.registeredEventListeners.push({type, listener, options});
    }

    /**
     * Converts client coordinates to the chart content coordinates used by
     * chart-specific hit testing.
     */
    public toContentCoordinates(clientX: number, clientY: number): { x: number; y: number } {
        const rect = this.options.container.getBoundingClientRect();
        const scrollTxGroup = -(this.scrollOffset - this.renderedScrollOffset) * this.dayWidth;
        const tx = this.visualPanX + this.visualScale * scrollTxGroup;
        return {
            x: (clientX - rect.left - tx) / this.visualScale,
            y: (clientY - rect.top - this.visualPanY) / this.visualScale,
        };
    }

    /** Removes all listeners, observers, queued work, and rendered chart content. */
    public destroy(): void {
        for (const {type, listener, options} of this.registeredEventListeners)
            this.options.container.removeEventListener(type, listener, options);
        this.resizeObserver?.disconnect();
        if (this.animationFrameId)
            cancelAnimationFrame(this.animationFrameId);
        if (this.saveTimerId)
            clearTimeout(this.saveTimerId);
        if (this.lazyRedrawTimerId)
            clearTimeout(this.lazyRedrawTimerId);
        if (this.zoomIndicatorHideTimerId)
            clearTimeout(this.zoomIndicatorHideTimerId);
        this.options.container.innerHTML = '';
    }

    private restoreViewState(): void {
        const saved = loadViewState(this.options.containerId);
        if (saved) {
            this.dayWidth = this.constrainDayWidth(saved.dayWidth);
            this.scrollOffset = saved.scrollOffset;
            this.constrainScrollOffset();
            return;
        }
        this.scrollOffset = this.options.initialScrollOffset(this.dayWidth, this.getContainerWidth());
    }

    private getContainerWidth(): number {
        return Math.max(200, this.options.container.clientWidth || 800);
    }

    private getContainerHeight(): number {
        return this.options.getContainerHeight?.() ?? Math.max(200, this.options.container.clientHeight || 600);
    }

    private constrainDayWidth(dayWidth: number): number {
        return Math.max(this.options.minDayWidth, Math.min(this.options.maxDayWidth, dayWidth));
    }

    private constrainScrollOffset(): void {
        this.scrollOffset = Math.max(0, Math.min(
            Math.max(0, this.options.renderer.days - this.getContainerWidth() / (this.dayWidth * this.visualScale)),
            this.scrollOffset,
        ));
    }

    private constrainScrollYOffset(): void {
        if (!this.options.chart.verticalScrollEnabled)
            return;
        this.scrollYOffset = Math.max(0, Math.min(
            Math.max(0, this.options.chart.chartHeight - this.getContainerHeight() / this.visualScale),
            this.scrollYOffset,
        ));
    }

    private scheduleSave(): void {
        if (this.saveTimerId)
            clearTimeout(this.saveTimerId);
        this.saveTimerId = setTimeout(
            () => saveViewState(this.options.containerId, this.dayWidth, this.scrollOffset),
            250,
        );
    }

    private redrawChart(): void {
        this.renderedScrollOffset = this.scrollOffset;
        this.options.beforeRender?.();
        this.options.chart.updateViewState(
            this.dayWidth,
            this.scrollOffset,
            this.getContainerWidth(),
            this.getContainerHeight(),
        );
        this.zoomIndicator = null;
        this.options.chart.render(this.options.container);
        this.options.afterRender?.();
        this.renderZoomIndicator();
        this.applyContentTransform();
    }

    private applyContentTransform(): void {
        const scrollTxGroup = -(this.scrollOffset - this.renderedScrollOffset) * this.dayWidth;
        const tx = this.visualPanX + this.visualScale * scrollTxGroup;
        const scrollTyGroup = this.options.chart.verticalScrollEnabled ? -this.scrollYOffset : 0;
        const ty = this.visualPanY + this.visualScale * scrollTyGroup;
        this.options.chart.updateContentTransform(tx, ty, this.visualScale);
        this.options.chart.updateSvgHeight(this.options.chart.chartHeight * this.visualScale);
    }

    private showZoomIndicator(): void {
        this.zoomIndicatorExpiresAt = Date.now() + ZOOM_INDICATOR_DURATION_MS;
        this.renderZoomIndicator();
    }

    private renderZoomIndicator(): void {
        const remainingDuration = this.zoomIndicatorExpiresAt - Date.now();
        if (remainingDuration <= 0)
            return;
        if (!this.zoomIndicator) {
            if (getComputedStyle(this.options.container).position === 'static')
                this.options.container.style.position = 'relative';
            this.zoomIndicator = document.createElement('div');
            this.zoomIndicator.className = 'chart-zoom-indicator';
            this.zoomIndicator.style.position = 'absolute';
            this.zoomIndicator.style.top = '12px';
            this.zoomIndicator.style.right = '12px';
            this.zoomIndicator.style.zIndex = '1000';
            this.zoomIndicator.style.padding = '6px 10px';
            this.zoomIndicator.style.borderRadius = '4px';
            this.zoomIndicator.style.background = 'rgba(15,23,42,0.88)';
            this.zoomIndicator.style.color = '#fff';
            this.zoomIndicator.style.fontFamily = 'sans-serif';
            this.zoomIndicator.style.fontSize = '12px';
            this.zoomIndicator.style.lineHeight = '1.4';
            this.zoomIndicator.style.textAlign = 'center';
            this.zoomIndicator.style.whiteSpace = 'pre-line';
            this.zoomIndicator.style.pointerEvents = 'none';
            this.options.container.appendChild(this.zoomIndicator);
        }
        this.zoomIndicator.textContent = `Day width: ${this.dayWidth.toFixed(1)} px\nZoom: ${Math.round(this.visualScale * 100)}%`;
        if (this.zoomIndicatorHideTimerId)
            clearTimeout(this.zoomIndicatorHideTimerId);
        this.zoomIndicatorHideTimerId = setTimeout(() => {
            this.zoomIndicator?.remove();
            this.zoomIndicator = null;
            this.zoomIndicatorHideTimerId = null;
        }, remainingDuration);
    }

    private scheduleLazyRedraw(): void {
        if (this.lazyRedrawTimerId)
            clearTimeout(this.lazyRedrawTimerId);
        this.lazyRedrawTimerId = setTimeout(() => {
            this.lazyRedrawTimerId = null;
            this.scheduleRender();
        }, LAZY_REDRAW_DURATION_MS);
    }

    private scheduleRender(): void {
        if (this.animationFrameId)
            cancelAnimationFrame(this.animationFrameId);
        this.animationFrameId = requestAnimationFrame(() => this.redrawChart());
    }

    private handleWheelEvent = (event: WheelEvent): void => {
        if (event.altKey) {
            event.preventDefault();
            const rect = this.options.container.getBoundingClientRect();
            const cursorX = event.clientX - rect.left;
            const delta = event.deltaY < 0 ? VISUAL_ZOOM_STEP : 1 / VISUAL_ZOOM_STEP;
            const newScale = Math.max(VISUAL_ZOOM_MIN, Math.min(VISUAL_ZOOM_MAX, this.visualScale * delta));
            const scaleFactor = newScale / this.visualScale;
            const newVisualPanX = cursorX * (1 - scaleFactor) + this.visualPanX * scaleFactor;
            this.visualScale = newScale;
            this.scrollOffset -= newVisualPanX / (this.visualScale * this.dayWidth);
            this.visualPanX = 0;
            this.constrainScrollOffset();
            this.constrainScrollYOffset();
            this.applyContentTransform();
            this.showZoomIndicator();
            this.scheduleLazyRedraw();
        } else if (event.shiftKey) {
            event.preventDefault();
            const rect = this.options.container.getBoundingClientRect();
            const mouseX = event.clientX != null ? event.clientX - rect.left : this.getContainerWidth() / 2;
            const dayUnderCursor = this.scrollOffset + mouseX / this.dayWidth;
            const wheelDelta = event.deltaY || event.deltaX;
            const factor = wheelDelta < 0 ? this.options.dayWidthZoomStep : 1 / this.options.dayWidthZoomStep;
            this.dayWidth = this.constrainDayWidth(this.dayWidth * factor);
            this.scrollOffset = dayUnderCursor - mouseX / this.dayWidth;
            this.constrainScrollOffset();
            this.visualScale = 1.0;
            this.visualPanX = 0;
            this.visualPanY = 0;
            this.showZoomIndicator();
            this.scheduleRender();
            this.scheduleSave();
        } else if (event.deltaX !== 0) {
            event.preventDefault();
            this.scrollOffset += event.deltaX / this.dayWidth;
            this.constrainScrollOffset();
            this.applyContentTransform();
            this.scheduleLazyRedraw();
            this.scheduleSave();
        } else if (this.options.chart.verticalScrollEnabled) {
            event.preventDefault();
            this.scrollYOffset += event.deltaY / this.visualScale;
            this.constrainScrollYOffset();
            this.applyContentTransform();
            this.scheduleLazyRedraw();
        }
    };

    private handlePointerDown = (event: PointerEvent): void => {
        if (event.button !== 0)
            return;
        this.dragState = {
            startX: event.clientX,
            startOffset: this.scrollOffset,
            startY: event.clientY,
            startYOffset: this.scrollYOffset,
        };
        this.options.container.setPointerCapture(event.pointerId);
        event.preventDefault();
    };

    private handlePointerMove = (event: PointerEvent): void => {
        if (!this.dragState)
            return;
        this.scrollOffset = this.dragState.startOffset
            - (event.clientX - this.dragState.startX) / (this.dayWidth * this.visualScale);
        this.constrainScrollOffset();
        if (this.options.chart.verticalScrollEnabled) {
            this.scrollYOffset = this.dragState.startYOffset - (event.clientY - this.dragState.startY) / this.visualScale;
            this.constrainScrollYOffset();
        }
        this.applyContentTransform();
        this.scheduleLazyRedraw();
    };

    private handlePointerUp = (): void => {
        if (this.dragState) {
            this.dragState = null;
            this.scheduleSave();
        }
    };

    private registerEventListeners(): void {
        this.addEventListener('wheel', this.handleWheelEvent as EventListener, {passive: false});
        this.addEventListener('pointerdown', this.handlePointerDown as EventListener, {passive: false});
        this.addEventListener('pointermove', this.handlePointerMove as EventListener, {passive: true});
        this.addEventListener('pointerup', this.handlePointerUp);
        this.addEventListener('pointercancel', this.handlePointerUp);
    }

    private observeResize(): void {
        if (typeof ResizeObserver === 'undefined')
            return;
        this.resizeObserver = new ResizeObserver(() => this.scheduleRender());
        this.resizeObserver.observe(this.options.container);
    }
}
