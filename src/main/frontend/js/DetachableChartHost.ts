// detachable-chart-host.ts
// Shared browser-side host for timeline charts that can be shown in a popup window.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

import type {ChartHandle} from './InteractiveTimelineChart.js';

export interface DetachableChartOptions<T> {
    bundleUrl: string;
    containerId: string;
    factoryExportName: string;
    factory: (container: HTMLElement, data: T, options: { containerId: string }) => ChartHandle;
    title: string;
}

interface PopupMessage {
    type: 'kassandra-chart-ready' | 'kassandra-chart-return' | 'kassandra-chart-failed';
    detail?: string;
}

interface PopupRenderMessage<T> {
    type: 'kassandra-chart-render';
    bundleUrl: string;
    containerId: string;
    data: T;
    factoryExportName: string;
    title: string;
}

interface PopupClearMessage {
    type: 'kassandra-chart-clear';
}

class DetachableChartHost<T> {
    private container: HTMLElement | null = null;
    private data: T | null = null;
    private normalHandle: ChartHandle | null = null;
    private originalDisplay = '';
    private popup: Window | null = null;
    private popupReady = false;
    private popupReadyTimer: ReturnType<typeof setTimeout> | null = null;
    private popupCheckTimer: ReturnType<typeof setInterval> | null = null;

    public constructor(private readonly options: DetachableChartOptions<T>) {
        window.addEventListener('message', this.handlePopupMessage);
    }

    public mount(container: HTMLElement, data: T, title: string): void {
        if (activeDetachedHost && activeDetachedHost !== this)
            activeDetachedHost.transferTo(this);

        const containerChanged = this.container !== container;
        this.container = container;
        this.data = data;
        this.options.title = title;
        if (this.isDetached()) {
            if (containerChanged) {
                this.originalDisplay = container.style.display;
                container.style.display = 'none';
            }
            this.sendPopupRender();
            return;
        }
        this.renderOnPage();
    }

    public detach(): void {
        if (!this.container || !this.data)
            return;
        if (this.isDetached()) {
            this.popup?.focus();
            return;
        }

        const popup = window.open(
            '',
            '_blank',
            'popup=yes,width=1280,height=900,resizable=yes,scrollbars=yes,toolbar=yes,location=yes,menubar=yes',
        );
        if (!popup) {
            this.showPageError('The browser blocked opening the chart tab. Allow popups for this site and try again.');
            return;
        }

        this.popup = popup;
        activeDetachedHost = this as DetachableChartHost<unknown>;
        this.popupReady = false;
        this.writePopupDocument(popup);
        this.normalHandle?.destroy();
        this.normalHandle = null;
        this.originalDisplay = this.container.style.display;
        this.container.style.display = 'none';
        this.startPopupMonitor();
        this.popupReadyTimer = setTimeout(() => {
            if (this.popup === popup && !this.popupReady) {
                this.restoreToPage(true);
                this.showPageError('The chart tab did not initialize. Close it and try opening the chart again.');
            }
        }, 5_000);
    }

    public dispose(): void {
        const popup = this.popup;
        this.popup = null;
        this.popupReady = false;
        if (this.popupReadyTimer) {
            clearTimeout(this.popupReadyTimer);
            this.popupReadyTimer = null;
        }
        if (this.popupCheckTimer) {
            clearInterval(this.popupCheckTimer);
            this.popupCheckTimer = null;
        }
        this.normalHandle?.destroy();
        this.normalHandle = null;
        if (activeDetachedHost === this)
            activeDetachedHost = null;
        if (popup && !popup.closed)
            popup.close();
        window.removeEventListener('message', this.handlePopupMessage);
    }

    public release(): boolean {
        this.container = null;
        this.normalHandle?.destroy();
        this.normalHandle = null;
        if (activeDetachedHost === this && this.isDetached()) {
            this.sendPopupClear();
            return true;
        }
        window.removeEventListener('message', this.handlePopupMessage);
        return false;
    }

    public clear(): void {
        this.sendPopupClear();
    }

    private readonly handlePopupMessage = (event: MessageEvent<PopupMessage>): void => {
        if (event.origin !== window.location.origin || event.source !== this.popup)
            return;

        switch (event.data?.type) {
            case 'kassandra-chart-ready':
                this.popupReady = true;
                if (this.popupReadyTimer) {
                    clearTimeout(this.popupReadyTimer);
                    this.popupReadyTimer = null;
                }
                this.sendPopupRender();
                break;
            case 'kassandra-chart-return':
                this.restoreToPage(true);
                break;
            case 'kassandra-chart-failed':
                this.restoreToPage(true);
                this.showPageError(event.data.detail || 'The chart could not be rendered in the new tab.');
                break;
            default:
                break;
        }
    };

    private isDetached(): boolean {
        return this.popup != null && !this.popup.closed;
    }

    private transferTo(nextHost: DetachableChartHost<unknown>): void {
        const popup = this.popup;
        if (!popup || popup.closed)
            return;

        this.popup = null;
        this.popupReady = false;
        if (this.popupReadyTimer) {
            clearTimeout(this.popupReadyTimer);
            this.popupReadyTimer = null;
        }
        if (this.popupCheckTimer) {
            clearInterval(this.popupCheckTimer);
            this.popupCheckTimer = null;
        }
        nextHost.popup = popup;
        nextHost.popupReady = true;
        activeDetachedHost = nextHost;
    }

    private renderOnPage(): void {
        if (!this.container || !this.data)
            return;
        this.normalHandle?.destroy();
        this.normalHandle = this.options.factory(this.container, this.data, {containerId: this.options.containerId});
    }

    private sendPopupRender(): void {
        if (!this.popupReady || !this.popup || this.popup.closed || !this.data)
            return;
        const message: PopupRenderMessage<T> = {
            type: 'kassandra-chart-render',
            bundleUrl: this.options.bundleUrl,
            containerId: this.options.containerId,
            data: this.data,
            factoryExportName: this.options.factoryExportName,
            title: this.options.title,
        };
        this.popup.postMessage(message, window.location.origin);
    }

    private sendPopupClear(): void {
        if (!this.popupReady || !this.popup || this.popup.closed)
            return;
        const message: PopupClearMessage = {type: 'kassandra-chart-clear'};
        this.popup.postMessage(message, window.location.origin);
    }

    private restoreToPage(closePopup: boolean): void {
        const popup = this.popup;
        this.popup = null;
        this.popupReady = false;
        if (activeDetachedHost === this)
            activeDetachedHost = null;
        if (this.popupReadyTimer) {
            clearTimeout(this.popupReadyTimer);
            this.popupReadyTimer = null;
        }
        if (this.popupCheckTimer) {
            clearInterval(this.popupCheckTimer);
            this.popupCheckTimer = null;
        }
        if (closePopup && popup && !popup.closed)
            popup.close();
        if (this.container) {
            this.container.style.display = this.originalDisplay;
            this.renderOnPage();
        }
    }

    private startPopupMonitor(): void {
        if (this.popupCheckTimer)
            clearInterval(this.popupCheckTimer);
        this.popupCheckTimer = setInterval(() => {
            if (!this.popup || this.popup.closed)
                this.restoreToPage(false);
        }, 500);
    }

    private showPageError(message: string): void {
        if (!this.container)
            return;
        this.container.innerHTML = '';
        const error = document.createElement('div');
        error.style.color = 'var(--lumo-error-text-color, #b91c1c)';
        error.style.fontFamily = 'sans-serif';
        error.style.padding = '16px';
        error.textContent = message;
        this.container.appendChild(error);
    }

    private writePopupDocument(popup: Window): void {
        const expectedOrigin = JSON.stringify(window.location.origin);
        popup.document.open();
        popup.document.write(`<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Chart</title>
<style>
body { margin: 0; background: var(--lumo-base-color, #fff); color: var(--lumo-body-text-color, #1f2937); font-family: sans-serif; }
#toolbar { align-items: center; border-bottom: 1px solid #d1d5db; display: flex; gap: 12px; padding: 10px 16px; }
#return-chart { background: #2563eb; border: 0; border-radius: 4px; color: #fff; cursor: pointer; font: inherit; padding: 8px 12px; }
#chart { min-height: 200px; width: 100%; }
</style>
</head>
<body>
<div id="toolbar"><button id="return-chart" type="button">Return chart to page</button><span id="title"></span></div>
<div id="chart"></div>
<script type="module">
const expectedOrigin = ${expectedOrigin};
let chartHandle = null;
let returning = false;
const notifyReturn = () => {
    if (returning)
        return;
    returning = true;
    if (window.opener && !window.opener.closed)
        window.opener.postMessage({type: 'kassandra-chart-return'}, expectedOrigin);
};
document.getElementById('return-chart').addEventListener('click', () => {
    notifyReturn();
    window.close();
});
window.addEventListener('pagehide', notifyReturn);
window.addEventListener('message', async (event) => {
    if (event.origin !== expectedOrigin || event.source !== window.opener)
        return;
    if (event.data?.type === 'kassandra-chart-clear') {
        chartHandle?.destroy();
        chartHandle = null;
        document.title = 'No Chart Data Available';
        document.getElementById('title').textContent = 'No Chart Data Available';
        const chart = document.getElementById('chart');
        chart.textContent = 'No Chart Data Available';
        chart.style.padding = '24px';
        chart.style.textAlign = 'center';
        return;
    }
    if (event.data?.type !== 'kassandra-chart-render')
        return;
    try {
        const message = event.data;
        const chartModule = await import(message.bundleUrl);
        const factory = chartModule[message.factoryExportName] || window[message.factoryExportName];
        if (typeof factory !== 'function') {
            if (window.opener && !window.opener.closed)
                window.opener.postMessage({type: 'kassandra-chart-failed', detail: 'Chart factory is unavailable.'}, expectedOrigin);
            return;
        }
        chartHandle?.destroy();
        document.title = message.title;
        document.getElementById('title').textContent = message.title;
        const chart = document.getElementById('chart');
        chart.textContent = '';
        chart.style.padding = '';
        chart.style.textAlign = '';
        chartHandle = factory(chart, message.data, {containerId: message.containerId});
    } catch (error) {
        const detail = error instanceof Error ? error.message : 'Unknown chart rendering error.';
        if (window.opener && !window.opener.closed)
            window.opener.postMessage({type: 'kassandra-chart-failed', detail}, expectedOrigin);
    }
});
if (window.opener && !window.opener.closed)
    window.opener.postMessage({type: 'kassandra-chart-ready'}, expectedOrigin);
</script>
</body>
</html>`);
        popup.document.close();
    }
}

const hosts = new Map<string, DetachableChartHost<unknown>>();
let activeDetachedHost: DetachableChartHost<unknown> | null = null;

export function mountDetachableChart<T>(
    options: DetachableChartOptions<T>,
    container: HTMLElement,
    data: T,
    title: string,
): void {
    let host = hosts.get(options.containerId) as DetachableChartHost<T> | undefined;
    if (!host) {
        host = new DetachableChartHost(options);
        hosts.set(options.containerId, host as DetachableChartHost<unknown>);
    }
    host.mount(container, data, title);
}

export function detachChart(containerId: string): void {
    hosts.get(containerId)?.detach();
}

export function disposeDetachableChart(containerId: string): void {
    const host = hosts.get(containerId);
    if (!host)
        return;
    host.dispose();
    hosts.delete(containerId);
}

export function releaseDetachableChart(containerId: string): void {
    const host = hosts.get(containerId);
    if (!host)
        return;
    if (!host.release())
        hosts.delete(containerId);
}

export function clearDetachedChart(): void {
    activeDetachedHost?.clear();
}

declare global {
    interface Window {
        clearKassandraChart: typeof clearDetachedChart;
        detachKassandraChart: typeof detachChart;
        disposeKassandraChart: typeof disposeDetachableChart;
        releaseKassandraChart: typeof releaseDetachableChart;
    }
}

window.clearKassandraChart = clearDetachedChart;
window.detachKassandraChart = detachChart;
window.disposeKassandraChart = disposeDetachableChart;
window.releaseKassandraChart = releaseDetachableChart;
