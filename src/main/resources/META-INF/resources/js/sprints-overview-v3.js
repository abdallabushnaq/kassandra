// sprints-overview-v3.js
// Virtual-canvas Sprints Overview chart.
// Depends on: CalendarXAxes.js  (must be loaded first)
//
// Interaction:
//   plain wheel      → zoom (change dayWidth, keep day-under-cursor stable)
//   horizontal swipe → pan (trackpad)
//   right-click      → context menu with Backlog / Active Sprint / Quality Board links
//
// View state (dayWidth + scrollOffset) is persisted in localStorage using the key
//   kassandra.chart.<chartName>.view
// so each chart instance keeps its own position across theme toggles and page reloads.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0
(function () {
    'use strict';

    // ── Java SprintsOverviewRenderer constants (numberOfLines = 3) ────────────
    const LINE_HEIGHT = 13;
    const SPRINT_H = LINE_HEIGHT * 3;        // 39 px – height of sprint rect
    const TASK_H = LINE_HEIGHT * 3 + 17;   // 56 px – getTaskHeight()
    const LANE_H = TASK_H + 2;             // 58 px – lane spacing

    // ── Threshold below which per-day stripes become week-boundary lines ──────
    const MIN_DAY_BARS = 4;   // Java: isDayBarsVisible() = dayWidth >= 4

    // ── Zoom / scroll config ──────────────────────────────────────────────────
    const DEFAULT_DW = 8;     // matches Java CalendarXAxes.calculateDayWidth() → 8
    const MIN_DW = 1;
    const MAX_DW = 80;
    const ZOOM_STEP = 1.25;

    const MS = 86400000;
    const SVG_NS = 'http://www.w3.org/2000/svg';

    // ── SVG helpers ───────────────────────────────────────────────────────────
    function el(tag, attrs, txt) {
        const e = document.createElementNS(SVG_NS, tag);
        if (attrs) for (const k of Object.keys(attrs)) e.setAttribute(k, String(attrs[k]));
        if (txt != null) e.textContent = txt;
        return e;
    }

    function rct(x, y, w, h, a) {
        return el('rect', Object.assign({x, y, width: Math.max(0, w), height: Math.max(0, h)}, a));
    }

    function ln(x1, y1, x2, y2, a) {
        return el('line', Object.assign({x1, y1, x2, y2}, a));
    }

    function tx(x, y, c, a) {
        return el('text', Object.assign({x, y}, a), c);
    }

    function mkClip(id, x, y, w, h) {
        const d = el('defs'), cp = el('clipPath', {id});
        cp.appendChild(rct(x, y, w, h, {}));
        d.appendChild(cp);
        return d;
    }

    // ── Color helpers ─────────────────────────────────────────────────────────
    function numToHex(v) {
        if (v == null) return null;
        if (typeof v === 'string') return v;
        if (typeof v === 'number') return '#' + (v >>> 0).toString(16).padStart(6, '0').slice(-6);
        return null;
    }

    function tc(theme, key, fb) {
        return (theme && theme[key] != null) ? (numToHex(theme[key]) || fb) : fb;
    }

    function utcDay(d) {
        const dt = (typeof d === 'string') ? new Date(d) : d;
        return new Date(Date.UTC(dt.getFullYear(), dt.getMonth(), dt.getDate()));
    }

    function dayIdxOf(date, start) {
        return Math.round((utcDay(date).getTime() - start.getTime()) / MS);
    }

    function dayCount(s, e) {
        return Math.round((utcDay(e).getTime() - utcDay(s).getTime()) / MS) + 1;
    }

    // Java encodes sprint color as #rrggbbaa → rgba()
    function sprintFill(hex) {
        if (!hex) return 'rgba(31,143,255,0.31)';
        if (/^#[0-9a-fA-F]{8}$/.test(hex)) {
            const r = parseInt(hex.slice(1, 3), 16), g = parseInt(hex.slice(3, 5), 16),
                b = parseInt(hex.slice(5, 7), 16), a = (parseInt(hex.slice(7, 9), 16) / 255).toFixed(3);
            return 'rgba(' + r + ',' + g + ',' + b + ',' + a + ')';
        }
        return hex;
    }

    // ── localStorage view-state helpers ───────────────────────────────────────
    // Key format: kassandra.chart.<chartName>.view
    // where <chartName> is derived from the container ID by stripping a trailing "-container".
    // Example: "sprints-overview-v3-container" → "kassandra.chart.sprints-overview-v3.view"

    function viewStateKey(containerId) {
        const chartName = (containerId || 'chart').replace(/-container$/, '');
        return 'kassandra.chart.' + chartName + '.view';
    }

    function loadViewState(containerId) {
        try {
            const raw = localStorage.getItem(viewStateKey(containerId));
            if (raw) {
                const s = JSON.parse(raw);
                if (typeof s.dayWidth === 'number' && typeof s.scrollOffset === 'number') return s;
            }
        } catch (e) { /* storage may be unavailable */
        }
        return null;
    }

    function saveViewState(containerId, dayWidth, scrollOffset) {
        try {
            localStorage.setItem(viewStateKey(containerId), JSON.stringify({dayWidth, scrollOffset}));
        } catch (e) { /* quota exceeded or restricted – silently ignore */
        }
    }

    // ── Context menu (one singleton per page) ─────────────────────────────────
    var _ctxMenu = null;

    function ensureContextMenu() {
        if (_ctxMenu) return _ctxMenu;
        const m = document.createElement('div');
        m.style.cssText = [
            'position:fixed',
            'z-index:99999',
            'background:var(--lumo-base-color,#fff)',
            'color:var(--lumo-body-text-color,#333)',
            'border:1px solid var(--lumo-contrast-20pct,#ccc)',
            'border-radius:var(--lumo-border-radius-m,4px)',
            'box-shadow:0 4px 16px rgba(0,0,0,.18)',
            'padding:4px 0',
            'font-family:var(--lumo-font-family,Arial,sans-serif)',
            'font-size:13px',
            'min-width:168px',
            'display:none',
            'user-select:none',
        ].join(';');
        document.body.appendChild(m);
        _ctxMenu = m;
        // Dismiss on any click or scroll outside the menu
        document.addEventListener('click', function (e) {
            if (!_ctxMenu.contains(e.target)) hideContextMenu();
        });
        document.addEventListener('scroll', hideContextMenu, true);
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') hideContextMenu();
        });
        return m;
    }

    function hideContextMenu() {
        if (_ctxMenu) _ctxMenu.style.display = 'none';
    }

    function showContextMenu(clientX, clientY, sprint) {
        const m = ensureContextMenu();
        m.innerHTML = '';

        const items = [
            {label: '\u{1F4CB} Backlog', url: 'backlog?sprint=' + sprint.id},
            {label: '\u25B6 Active Sprint', url: 'active-sprints?sprints=' + sprint.id},
            {label: '\u{1F4CB} Quality Board', url: 'quality-board?sprint=' + sprint.id},
        ];

        // Optional header showing sprint name
        const header = document.createElement('div');
        header.textContent = sprint.name || 'Sprint';
        header.style.cssText = [
            'padding:4px 14px 4px 14px',
            'font-weight:bold',
            'font-size:12px',
            'color:var(--lumo-secondary-text-color,#666)',
            'border-bottom:1px solid var(--lumo-contrast-10pct,#eee)',
            'margin-bottom:4px',
            'white-space:nowrap',
            'overflow:hidden',
            'text-overflow:ellipsis',
            'max-width:220px',
        ].join(';');
        m.appendChild(header);

        items.forEach(function (item) {
            const row = document.createElement('div');
            row.textContent = item.label;
            row.style.cssText = [
                'padding:6px 16px',
                'cursor:pointer',
                'white-space:nowrap',
                'transition:background 0.1s',
            ].join(';');
            row.addEventListener('mouseenter', function () {
                row.style.background = 'var(--lumo-primary-color-10pct,#e8f0fe)';
            });
            row.addEventListener('mouseleave', function () {
                row.style.background = '';
            });
            row.addEventListener('click', function (e) {
                e.stopPropagation();
                hideContextMenu();
                window.location.href = item.url;
            });
            m.appendChild(row);
        });

        // Position: prefer right-and-below cursor; flip if it would overflow viewport
        m.style.display = 'block';
        const mw = m.offsetWidth || 168;
        const mh = m.offsetHeight || 120;
        const vw = window.innerWidth;
        const vh = window.innerHeight;
        const left = (clientX + mw + 2 > vw) ? (clientX - mw) : (clientX + 2);
        const top = (clientY + mh + 2 > vh) ? (clientY - mh) : (clientY + 2);
        m.style.left = Math.max(0, left) + 'px';
        m.style.top = Math.max(0, top) + 'px';
    }

    // ── Chart factory ─────────────────────────────────────────────────────────
    function createChart(container, data, opts) {
        opts = opts || {};
        const containerId = opts.containerId || container.id || 'chart';
        const meta = data.meta || {};
        const lanes = data.lanes || [];
        const theme = meta.xAxesTheme || {};
        const chartStart = utcDay(new Date(meta.chartStart || Date.now()));
        const chartEnd = utcDay(new Date(meta.chartEnd || Date.now()));
        const nowDate = meta.now ? utcDay(new Date(meta.now)) : utcDay(new Date());
        const totalDays = dayCount(chartStart, chartEnd);

        // CalendarXAxes class from CalendarXAxes.js
        const calendar = new window.CalendarXAxes(theme);

        let dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, opts.initialDayWidth || DEFAULT_DW));
        let scrollOffset = 0;  // fractional day index of the left viewport edge

        // Hit areas for context menu – rebuilt on every render()
        let _hitAreas = [];  // [{ sp, x, y, w, h }]

        const dX = di => (di - scrollOffset) * dayWidth;
        const cw = () => Math.max(200, container.clientWidth || 800);
        const calH = () => calendar.getHeight(dayWidth);

        // ── Restore or default scroll/zoom position ────────────────────────────
        function initScroll() {
            const saved = loadViewState(containerId);
            if (saved) {
                dayWidth = Math.min(MAX_DW, Math.max(MIN_DW, saved.dayWidth));
                scrollOffset = saved.scrollOffset;
                clampScroll();  // clampScroll needs dayWidth set first
            } else {
                // Default: put "today" at 30% from the left
                const ni = dayIdxOf(nowDate, chartStart);
                const v = cw() / dayWidth;
                scrollOffset = Math.max(0, Math.min(totalDays - v, ni - v * 0.3));
            }
        }

        // ── Debounced save after interactions ─────────────────────────────────
        let _saveTimer = null;

        function scheduleSave() {
            if (_saveTimer) clearTimeout(_saveTimer);
            _saveTimer = setTimeout(function () {
                saveViewState(containerId, dayWidth, scrollOffset);
            }, 250);
        }

        // ── Weekend / day stripes (drawn BELOW sprints) ────────────────────────
        function renderStripes(bY, bH) {
            const g = el('g', {'class': 'stripes'});
            const W = cw();
            const satC = tc(theme, 'dayOfweekSaturdayBgColor', '#d7d7d7');
            const sunC = tc(theme, 'dayOfweekSundayBgColor', '#d7d7d7');

            if (dayWidth >= MIN_DAY_BARS) {
                // Per-day coloured stripes (weekends coloured, weekdays transparent)
                const fv = Math.max(0, Math.floor(scrollOffset) - 1);
                const lv = Math.min(totalDays - 1, fv + Math.ceil(W / dayWidth) + 2);
                for (let i = fv; i <= lv; i++) {
                    const dw = new Date(chartStart.getTime() + i * MS).getUTCDay();
                    const bg = dw === 6 ? satC : (dw === 0 ? sunC : null);
                    if (!bg) continue;
                    const x = dX(i);
                    if (x + dayWidth < 0 || x > W) continue;
                    g.appendChild(rct(x, bY, dayWidth, bH, {fill: bg}));
                }
            } else {
                // Week-boundary lines only (one Monday line per week)
                const gc = tc(theme, 'gridColor', '#e4e8f3');
                const fv = Math.max(0, Math.floor(scrollOffset));
                const lv = Math.min(totalDays - 1, fv + Math.ceil(W / dayWidth) + 8);
                for (let i = fv; i <= lv; i++) {
                    if (new Date(chartStart.getTime() + i * MS).getUTCDay() !== 1) continue;
                    const x = dX(i);
                    if (x < 0 || x > W) continue;
                    g.appendChild(ln(x, bY, x, bY + bH, {
                        stroke: gc,
                        'stroke-width': '1'
                    }));
                }
            }
            return g;
        }

        // ── Vertical grid lines ────────────────────────────────────────────────
        function renderGrid(bY, bH) {
            const g = el('g', {'class': 'grid'});
            if (dayWidth < MIN_DAY_BARS) return g;
            const gc = tc(theme, 'gridColor', '#e4e8f3');
            const W = cw();
            const fv = Math.max(0, Math.floor(scrollOffset) - 1);
            const lv = Math.min(totalDays, fv + Math.ceil(W / dayWidth) + 2);
            for (let i = fv; i <= lv; i++) {
                const x = dX(i);
                if (x < 0 || x > W) continue;
                g.appendChild(ln(x, bY, x, bY + bH, {
                    stroke: gc,
                    'stroke-width': '1'
                }));
            }
            return g;
        }

        // ── Sprint rectangles + name label ─────────────────────────────────────
        function renderSprints(sY) {
            _hitAreas = [];  // rebuild on every render
            const g = el('g', {'class': 'sprints'});
            const W = cw();
            lanes.forEach((lane, li) => {
                const lY = sY + li * LANE_H;
                (lane.sprints || []).forEach(sp => {
                    if (!sp.start || !sp.end) return;
                    const si = dayIdxOf(sp.start, chartStart);
                    const ei = dayIdxOf(sp.end, chartStart);
                    // Sprint rect: starts at left edge of start-day, ends 1px before right edge of end-day
                    const sx = dX(si);
                    const sw = (ei - si + 1) * dayWidth - 1;
                    if (sx + sw < 0 || sx > W) return;

                    // Register hit area for context menu (uses raw SVG coords)
                    _hitAreas.push({sp, x: sx, y: lY, w: Math.max(0, sw), h: SPRINT_H});

                    const rect = rct(sx, lY, sw, SPRINT_H, {fill: sprintFill(sp.color)});
                    rect.appendChild(el('title', {}, buildTooltip(sp)));
                    g.appendChild(rect);

                    if (sw > 20) {
                        const cid = 'sp' + li + '_' + sp.id;
                        g.appendChild(mkClip(cid, sx, lY, sw, SPRINT_H));
                        // Java: drawString at x1+1, y1+(0+1)*LINE_HEIGHT-2 = y1+11
                        g.appendChild(tx(sx + 1, lY + LINE_HEIGHT - 2, sp.name || '', {
                            fill: '#000000', 'font-size': '12', 'font-family': 'Arial,sans-serif',
                            'font-weight': 'bold', 'clip-path': 'url(#' + cid + ')'
                        }));
                    }
                });
            });
            return g;
        }

        function buildTooltip(s) {
            let out = s.name || '';
            if (s.key) out += '\nKey: ' + s.key;
            if (s.status) out += '\nStatus: ' + s.status;
            if (s.start) out += '\nStart: ' + new Date(s.start).toLocaleDateString();
            if (s.end) out += '\nEnd:   ' + new Date(s.end).toLocaleDateString();
            if (s.delay) out += '\n(DELAYED)';
            return out;
        }

        // ── Now-line (Java: 2px width, COLOR_DARK_RED = #cc0000) ──────────────
        function renderNowLine(H) {
            const g = el('g', {'class': 'now-line'});
            const W = cw();
            // Java: center of day cell = dayLeft + dayWidth/2
            const x = dX(dayIdxOf(nowDate, chartStart)) + dayWidth / 2;
            if (x < 0 || x > W) return g;
            g.appendChild(ln(x, 0, x, H, {stroke: '#cc0000', 'stroke-width': '2'}));
            return g;
        }

        // ── Full redraw ────────────────────────────────────────────────────────
        let _raf = null;

        function render() {
            const W = cw();
            const ch = calH();
            const bH = lanes.length * LANE_H + 8;
            const H = ch + bH;

            const svg = el('svg', {
                width: W,
                height: H,
                style: 'display:block;user-select:none;shape-rendering: crispEdges'
            });

            // Draw order: stripes (bottom) → grid → sprints → calendar header → now-line (top)
            svg.appendChild(renderStripes(ch, bH));
            svg.appendChild(renderGrid(ch, bH));
            svg.appendChild(renderSprints(ch));
            calendar.draw(svg, chartStart, totalDays, dayWidth, scrollOffset, W);
            svg.appendChild(renderNowLine(H));

            container.innerHTML = '';
            container.appendChild(svg);
        }

        function schedule() {
            if (_raf) cancelAnimationFrame(_raf);
            _raf = requestAnimationFrame(render);
        }

        function clampScroll() {
            scrollOffset = Math.max(0, Math.min(Math.max(0, totalDays - cw() / dayWidth), scrollOffset));
        }

        // ── Mouse-wheel:  vertical  = zoom (change dayWidth)
        //                 horizontal = pan  (trackpad swipe left/right)
        function onWheel(e) {
            e.preventDefault();
            if (e.deltaX !== 0) {
                // Horizontal trackpad swipe → pan
                scrollOffset += e.deltaX / dayWidth;
            } else {
                // Vertical wheel → zoom about the cursor position
                const br = container.getBoundingClientRect();
                const mouseX = (e.clientX != null) ? (e.clientX - br.left) : (cw() / 2);
                const dayUnderCursor = scrollOffset + mouseX / dayWidth;
                const factor = e.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
                dayWidth = Math.max(MIN_DW, Math.min(MAX_DW, dayWidth * factor));
                scrollOffset = dayUnderCursor - mouseX / dayWidth;
            }
            clampScroll();
            schedule();
            scheduleSave();
        }

        // ── Click-and-drag panning ─────────────────────────────────────────────
        let _drag = null;  // { startX, startOffset }

        function onPointerDown(e) {
            if (e.button !== 0) return;
            _drag = {startX: e.clientX, startOffset: scrollOffset};
            container.setPointerCapture(e.pointerId);
            container.style.cursor = 'grabbing';
            e.preventDefault();
        }

        function onPointerMove(e) {
            if (!_drag) return;
            const dx = e.clientX - _drag.startX;
            scrollOffset = _drag.startOffset - dx / dayWidth;
            clampScroll();
            schedule();
        }

        function onPointerUp() {
            if (_drag) {
                _drag = null;
                scheduleSave();
            }
            container.style.cursor = 'grab';
        }

        // ── Context menu (right-click on a sprint) ─────────────────────────────
        function onContextMenu(e) {
            e.preventDefault();
            hideContextMenu();
            const br = container.getBoundingClientRect();
            const mx = e.clientX - br.left;
            const my = e.clientY - br.top;
            for (let i = 0; i < _hitAreas.length; i++) {
                const ha = _hitAreas[i];
                if (mx >= ha.x && mx <= ha.x + ha.w && my >= ha.y && my <= ha.y + ha.h) {
                    showContextMenu(e.clientX, e.clientY, ha.sp);
                    return;
                }
            }
        }

        // ── ResizeObserver: redraw when container resizes ──────────────────────
        let _ro = null;
        if (typeof ResizeObserver !== 'undefined') {
            _ro = new ResizeObserver(schedule);
            _ro.observe(container);
        }

        // ── Cleanup ────────────────────────────────────────────────────────────
        function destroy() {
            container.removeEventListener('wheel', onWheel);
            container.removeEventListener('pointerdown', onPointerDown);
            container.removeEventListener('pointermove', onPointerMove);
            container.removeEventListener('pointerup', onPointerUp);
            container.removeEventListener('pointercancel', onPointerUp);
            container.removeEventListener('contextmenu', onContextMenu);
            if (_ro) _ro.disconnect();
            if (_raf) cancelAnimationFrame(_raf);
            if (_saveTimer) clearTimeout(_saveTimer);
            container.innerHTML = '';
        }

        container.style.cursor = 'grab';
        container.addEventListener('wheel', onWheel, {passive: false});
        container.addEventListener('pointerdown', onPointerDown, {passive: false});
        container.addEventListener('pointermove', onPointerMove, {passive: true});
        container.addEventListener('pointerup', onPointerUp);
        container.addEventListener('pointercancel', onPointerUp);
        container.addEventListener('contextmenu', onContextMenu);
        initScroll();
        render();

        return {render, schedule, destroy};
    }

    // ── Public mount function ─────────────────────────────────────────────────
    // Called by:
    //   1. Script IIFE on first load (may be a no-op if container not yet in DOM)
    //   2. Vaadin executeJs on every navigation (container is guaranteed to exist)
    //   3. Test page with injectedData to skip the fetch
    var _instance = null;
    var _themeWatcher = null;  // MutationObserver watching <html theme="...">

    /**
     * True when Vaadin's Lumo dark theme is active.
     * Checks the body[theme] attribute set by Vaadin after theme is applied.
     */
    function isDarkMode() {
        return (document.body.getAttribute('theme') || '').includes('dark');
    }

    function mountSprintsOverviewV3(containerId, injectedData) {
        const id = containerId || 'sprints-overview-v3-container';
        const c = document.getElementById(id);
        if (!c) return;  // not in DOM yet; Vaadin will call us again via executeJs

        if (_instance && typeof _instance.destroy === 'function') {
            _instance.destroy();
            _instance = null;
        }

        if (injectedData) {
            _instance = createChart(c, injectedData, {containerId: id});
            return;
        }

        // Watch for Vaadin dark/light mode toggle and re-fetch when it changes
        if (_themeWatcher) {
            _themeWatcher.disconnect();
            _themeWatcher = null;
        }
        _themeWatcher = new MutationObserver(function () {
            mountSprintsOverviewV3(id);
        });
        _themeWatcher.observe(document.documentElement, {attributes: true, attributeFilter: ['theme']});

        const themeParam = isDarkMode() ? 'dark' : 'light';
        c.innerHTML = '<div style="padding:16px;color:#666;font-family:sans-serif;">Loading\u2026</div>';
        fetch('/api/overview/sprints?theme=' + themeParam)
            .then(function (r) {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(function (data) {
                c.innerHTML = '';
                _instance = createChart(c, data, {containerId: id});
            })
            .catch(function (err) {
                console.error('SprintsOverviewV3:', err);
                c.innerHTML = '<div style="padding:16px;color:red;font-family:sans-serif;">Error: ' + err.message + '</div>';
            });
    }

    window.mountSprintsOverviewV3 = mountSprintsOverviewV3;
    window.createSprintsOverviewV3 = createChart;  // used by test page

    // Auto-mount on script load (first-ever page load)
    mountSprintsOverviewV3();
})();
