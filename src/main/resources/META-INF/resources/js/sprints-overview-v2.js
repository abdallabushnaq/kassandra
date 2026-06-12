/*
  Minimal React + D3 based sprints overview v2 starter.
  This script loads React/ReactDOM and D3 from CDN, mounts a small component into
  the Vaadin view container with id 'sprints-overview-v2-container'.

  This is intentionally dependency-light for an initial MVP. Later we can
  replace this with a proper TypeScript/React component bundled by Vite.
*/
(function () {
    function loadScript(src) {
        return new Promise((resolve, reject) => {
            if (document.querySelector(`script[src="${src}"]`)) {
                // already present
                resolve();
                return;
            }
            const s = document.createElement('script');
            s.src = src;
            s.async = true;
            s.onload = () => resolve();
            s.onerror = (e) => reject(e);
            document.head.appendChild(s);
        });
    }

    async function ensureDeps() {
        // Use UMD builds from unpkg; switch to local bundling later
        await loadScript('https://unpkg.com/react@18/umd/react.development.js');
        await loadScript('https://unpkg.com/react-dom@18/umd/react-dom.development.js');
        await loadScript('https://unpkg.com/d3@7/dist/d3.min.js');
        // load CalendarXAxes implementation served from META-INF/resources so it is available at runtime
        try {
            await loadScript('/js/CalendarXAxes.js');
        } catch (e) {
            // not critical; we have an embedded fallback in this script
        }
        // Calendar helper: CalendarXAxesSVG loaded from /js/CalendarXAxes.js (if available).
        // A fallback embedded implementation existed here previously; we now prefer the external script so
        // the same calendar code can be shared between views.
        return {
            React: window.React,
            ReactDOM: window.ReactDOM,
            d3: window.d3
        };
    }

    function formatDate(iso) {
        try {
            return new Date(iso).toLocaleDateString();
        } catch (e) {
            return iso;
        }
    }

    function buildScales(meta, width) {
        const start = new Date(meta.chartStart);
        const end = new Date(meta.chartEnd);
        const scaleX = window.d3.scaleTime().domain([start, end]).range([0, width]);
        return {scaleX};
    }

    function SprintsOverview(props) {
        const {React, d3} = props;
        const [data, setData] = React.useState(null);
        const ref = React.useRef(null);

        React.useEffect(() => {
            let mounted = true;
            fetch('/api/overview/sprints')
                .then(r => r.json())
                .then(json => {
                    if (mounted) setData(json);
                })
                .catch(e => {
                    console.error('Failed to load overview data', e);
                });
            return () => {
                mounted = false;
            };
        }, []);

        React.useEffect(() => {
            if (!data || !ref.current) return;
            const container = ref.current;
            container.innerHTML = '';
            const lanes = data.lanes || [];
            const meta = data.meta || {};
            const width = Math.min(container.clientWidth || 1200, 1400);
            // Geometry tuned to resemble the server-side renderer
            const captionHeight = 36; // title area
            const footerHeight = 20;
            const laneHeight = 56; // corresponds roughly to server getTaskHeight() when numberOfLines=3
            const axisAreaHeight = 28; // space reserved for month/week axis
            const chartBodyHeight = Math.max(120, lanes.length * laneHeight + 8);
            const height = captionHeight + axisAreaHeight + chartBodyHeight + footerHeight;

            // compute discrete day width to match server renderer behaviour
            const startDate = meta.chartStart ? new Date(meta.chartStart) : new Date();
            // canonical chart domain start at UTC midnight for the startDate to avoid
            // timezone-related day shifts when converting dates to pixel positions
            const domainStart = new Date(Date.UTC(startDate.getFullYear(), startDate.getMonth(), startDate.getDate()));
            const endDate = meta.chartEnd ? new Date(meta.chartEnd) : new Date(startDate.getTime() + 30 * 24 * 3600 * 1000);
            // Canonical domain end at UTC midnight to match domainStart
            const domainEnd = new Date(Date.UTC(endDate.getFullYear(), endDate.getMonth(), endDate.getDate()));
            const msPerDay = 24 * 60 * 60 * 1000;
            // inclusive days count
            const days = Math.max(1, Math.floor((Date.UTC(endDate.getFullYear(), endDate.getMonth(), endDate.getDate()) - Date.UTC(startDate.getFullYear(), startDate.getMonth(), startDate.getDate())) / msPerDay) + 1);
            // server MAX_DAY_WIDTH = 20
            const maxDayWidth = 20;
            const dayWidth = Math.max(1, Math.min(maxDayWidth, Math.floor((width - 40) / days)));
            const chartPixelWidth = dayWidth * days;
            // make the svg wide enough to allow horizontal scrolling initially
            const baseSvgWidth = Math.max(width, chartPixelWidth + 40);

            const svg = d3.create('svg')
                .attr('viewBox', `0 0 ${baseSvgWidth} ${height}`)
                .attr('width', baseSvgWidth)
                .style('max-width', 'none')
                .attr('height', height);

            function dateToX(dt) {
                if (!dt) return 0;
                const d = new Date(dt);
                // compute day index relative to domainStart (UTC midnight)
                const dayIndex = Math.floor((Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()) - Date.UTC(domainStart.getFullYear(), domainStart.getMonth(), domainStart.getDate())) / msPerDay);
                const timeOfDay = (d.getHours() * 3600 + d.getMinutes() * 60 + d.getSeconds()) / (24 * 3600);
                // firstDayX offset (left padding)
                const firstDayX = 20;
                const x = firstDayX + Math.round(dayWidth / 2) + Math.round((dayIndex + timeOfDay) * dayWidth);
                return x;
            }

            // prepare discrete scale that matches dayWidth grid
            const firstDayX = 20;
            const firstX = firstDayX + Math.round(dayWidth / 2);
            const lastX = firstX + (days - 1) * dayWidth;
            const scaleX = d3.scaleTime().domain([domainStart, new Date(domainStart.getTime() + (days - 1) * msPerDay)]).range([firstX, lastX]);

            // top-level caption / title
            svg.append('text')
                .attr('x', baseSvgWidth / 2)
                .attr('y', captionHeight / 2 + 6)
                .attr('text-anchor', 'middle')
                .attr('font-size', 16)
                .attr('fill', '#222')
                .text('Sprints overview');

            // axes: top timeline with month ticks (position under caption)
            const axisY = captionHeight + 8;
            const axisG = svg.append('g').attr('transform', `translate(0,${axisY})`);
            // month labels and grid are rendered by the CalendarXAxesSVG; axisG is retained as a placeholder for
            // any additional annotations but we intentionally avoid d3.axisTop to prevent drawing a duplicate x-axis.

            // calendar group must be rendered below (before) the sprint rectangles so
            // weekend/day stripes appear beneath sprints. Create calendarG first,
            // then the scaled/unscaled content groups are appended so they render on top.
            const calendarG = svg.append('g').attr('class', 'calendarG').attr('transform', `translate(0,${axisY})`);

            // content groups: scaled visuals (rects, grids) and unscaled labels/text
            const contentScaledG = svg.append('g').attr('class', 'content-scaled').attr('transform', `translate(0,0)`);
            const contentUnscaledG = svg.append('g').attr('class', 'content-unscaled');

            // instantiate calendar implementation from loaded script (fallback to window.CalendarXAxesSVG)
            const CalendarCtor = window.CalendarXAxesSVG || (typeof CalendarXAxesSVG !== 'undefined' ? CalendarXAxesSVG : null);
            const calendar = new CalendarCtor(meta.themeColors || meta.xAxesTheme || {});
            // baseDayWidth is discrete from server-like calculation above
            const baseDayWidth = dayWidth;
            // initial draw (k=1) to obtain calendar header height; we'll redraw with bodyY/bodyHeight next
            let currentK = 1;
            let currentTx = 0;
            let calendarInfo = calendar.drawCalendar(calendarG, domainStart, domainEnd, baseDayWidth * currentK, currentK, { width: baseSvgWidth });

            // render lanes (positioned beneath the calendar) inside contentScaledG so they pan/zoom
            let lanesStartY = captionHeight + (calendarInfo && calendarInfo.calendarHeight ? calendarInfo.calendarHeight : axisAreaHeight);
            // remember the initial lanesStartY so we can compute deltas when the calendar height
            // changes (for example when day-of-week row appears). Sprint rects/labels store
            // their original base Y and are repositioned relative to this initial value.
            const initialLanesStartY = lanesStartY;
            // Redraw calendar with explicit body placement/height so weekend stripes are drawn beneath sprints
            calendarInfo = calendar.drawCalendar(calendarG, domainStart, domainEnd, baseDayWidth * currentK, currentK, { width: baseSvgWidth, bodyY: (lanesStartY - axisY), bodyHeight: chartBodyHeight });
            // ensure lanesStartY matches any recalculated calendarHeight
            lanesStartY = captionHeight + (calendarInfo && calendarInfo.calendarHeight ? calendarInfo.calendarHeight : axisAreaHeight);

            // day boundary grid lines
            // Draw these into the unscaled layer so the stroke remains a single device pixel
            // regardless of zoom scale. We compute the screen X using the same rescaled
            // time scale used for unscaled labels/now-line so positions stay consistent.
            const gridG = contentUnscaledG.append('g').attr('class', 'day-grid');
            const gridColor = (typeof calendar !== 'undefined' && calendar.color) ? calendar.color(['ganttTheme.gridColor','xAxesTheme.gridColor'], '#e0e0e0') : '#e0e0e0';
            // precompute day date array so we can update positions on zoom easily
            const dayDates = [];
            for (let i = 0; i < days; i++) {
                const day = new Date(domainStart.getTime() + i * msPerDay);
                dayDates.push(day);
            }
            // create 1px lines (use stroke, not rect, and shape-rendering crispEdges to avoid anti-aliasing)
            gridG.selectAll('.day-grid-line').data(dayDates).enter().append('line')
                .attr('class', 'day-grid-line')
                .attr('x1', d => scaleX(d) - dayWidth / 2)
                .attr('x2', d => scaleX(d) - dayWidth / 2)
                .attr('y1', lanesStartY)
                .attr('y2', lanesStartY + chartBodyHeight)
                .attr('stroke', gridColor)
                .attr('stroke-width', 1)
                .attr('shape-rendering', 'crispEdges')
                .style('pointer-events', 'none');

            const sprintRects = [];
            const labels = [];
            lanes.forEach((lane, li) => {
                const y = lanesStartY + li * laneHeight;
                lane.sprints.forEach(s => {
                    if (!s.start || !s.end) return;
                    const dayX1 = dateToX(s.start);
                    const dayX2 = dateToX(s.end);
                    const rectX = dayX1 - (Math.floor(dayWidth / 2) - 1);
                    const rectW = Math.max(2, (dayX2 - dayX1) + dayWidth - 1);
                    const g = contentScaledG.append('g');

                    // normalize color: support 8-digit hex (#rrggbbaa) by converting to rgba if needed
                    function normalizeColor(hex) {
                        if (!hex) return '#1f8fff80';
                        if (/^#[0-9a-fA-F]{8}$/.test(hex)) {
                            const rr = parseInt(hex.substr(1, 2), 16);
                            const gg = parseInt(hex.substr(3, 2), 16);
                            const bb = parseInt(hex.substr(5, 2), 16);
                            const aa = parseInt(hex.substr(7, 2), 16) / 255;
                            return `rgba(${rr},${gg},${bb},${aa.toFixed(2)})`;
                        }
                        return hex;
                    }

                    const rect = g.append('rect')
                        .attr('x', rectX).attr('y', y).attr('width', rectW).attr('height', laneHeight - 16)
                        .attr('fill', normalizeColor(s.color)).attr('stroke', '#00000020').attr('rx', 3).attr('ry', 3);

                    // store original base Y so we can shift rects down/up when calendar header height changes
                    sprintRects.push({rect, s, laneIndex: li, originalBaseY: y});
                    // label when space: create unscaled text element so it remains crisp when zooming
                    if (rectW > 40) {
                        const tx = rectX + 6;
                        const ty = y + (laneHeight - 16) / 2 + 6;
                        const tEl = contentUnscaledG.append('text').attr('x', tx).attr('y', ty).attr('fill', '#000').attr('font-size', 12).text(s.name || '');
                        labels.push({
                            el: tEl,
                            start: new Date(s.start),
                            end: new Date(s.end),
                            laneIndex: li,
                            offsetX: 6,
                            offsetY: ty,
                            originalOffsetY: ty
                        });
                    }
                });
            });

            // Debug text: show current dayWidthScaled for troubleshooting
            const debugText = svg.append('text')
                .attr('class', 'debug-daywidth')
                .attr('x', 8)
                .attr('y', captionHeight - 8)
                .attr('fill', '#666')
                .attr('font-size', 11)
                .text('dayWidth: ' + (baseDayWidth).toFixed(2));

            // Debug now text: show now timestamp and computed X positions (dateToX, scaleX, and rescaled newScale)
            const nowDebugText = svg.append('text')
                .attr('class', 'debug-now')
                .attr('x', 8)
                .attr('y', captionHeight - 22)
                .attr('fill', '#666')
                .attr('font-size', 11)
                .text(() => {
                    if (!meta || !meta.now) return 'now: n/a';
                    try {
                        const nd = new Date(meta.now);
                        const dtX = dateToX(nd);
                        const scX = scaleX(nd);
                        return 'now: ' + nd.toISOString() + ' dateToX:' + dtX.toFixed(2) + ' scaleX:' + scX.toFixed(2);
                    } catch (e) { return 'now: invalid'; }
                });

            // now-line layer: create after sprints so it renders on top
            const nowLayer = svg.append('g').attr('class', 'now-layer');
            if (meta.now) {
                // draw initial now-line at the correct screen x using the discrete scale
                // (we'll update its position on zoom using the same rescaled transform)
                const nowDate = new Date(meta.now);
                // use UTC-midnight to match domainStart and avoid timezone shifts
                const nowStartOfDay = new Date(Date.UTC(nowDate.getFullYear(), nowDate.getMonth(), nowDate.getDate()));
                // initial nowX uses scaleX(startOfDay) - half day width so it aligns with the day's start (left edge)
                const nowX = scaleX(nowStartOfDay) - dayWidth / 2;
                // normalize now-line color via calendar color resolver (handles numeric theme values)
                const nowColor = (typeof calendar !== 'undefined' && calendar.color) ? calendar.color(['xAxesTheme.todayBarColor','todayBarColor','todayBar'],'#ff0000') : (meta.themeColors && meta.themeColors['xAxesTheme.todayBarColor'] ? meta.themeColors['xAxesTheme.todayBarColor'] : '#ff0000');
                nowLayer.append('line').attr('x1', nowX).attr('x2', nowX).attr('y1', captionHeight).attr('y2', height - footerHeight).attr('stroke', nowColor).attr('stroke-width', 1).attr('class', 'now-line');
            }

            // day-of-week / day-of-month / week numbers are rendered by calendar.drawCalendar above

            // footer
            svg.append('text')
                .attr('x', baseSvgWidth / 2)
                .attr('y', height - footerHeight / 2 + 6)
                .attr('text-anchor', 'middle')
                .attr('font-size', 11)
                .attr('fill', '#666')
                .text(meta.now ? ('Generated: ' + new Date(meta.now).toLocaleString()) : 'Generated by Kassandra');

            // Zoom behaviour: horizontal only (scaleX and translateX)
            const zoom = d3.zoom()
                .scaleExtent([1, 8])
                .translateExtent([[0, 0], [baseSvgWidth * 8, height]])
                .on('zoom', (event) => {
                    const t = event.transform;
                    // If zooming via wheel, center zoom about the viewport center so content grows left and right.
                    // If panning (drag), use the raw transform.x so panning behaves naturally.
                    try {
                        const srcType = event.sourceEvent && event.sourceEvent.type;
                        if (srcType === 'wheel') {
                            const newK = t.k;
                            const containerWidth = container.clientWidth || width;
                            const viewportCenter = containerWidth / 2;
                            const oldK = currentK || 1;
                            const oldTx = currentTx || 0;
                            // dataX is the logical data coordinate under the viewport center before scaling
                            const dataX = (viewportCenter - oldTx) / oldK;
                            // compute precise translate so we do not introduce integer rounding jitter
                            const newTx = (viewportCenter - newK * dataX);
                            currentK = newK;
                            currentTx = newTx;
                        } else {
                            // panning or other input: use transform values directly
                            currentK = t.k;
                            currentTx = t.x;
                        }
                    } catch (e) {
                        // fallback to raw transform values
                        currentK = t.k;
                        currentTx = t.x;
                    }

                    // apply transform to scaled visuals (shapes still use original coordinates)
                    contentScaledG.attr('transform', `translate(${currentTx},0) scale(${currentK},1)`);

                    // redraw calendar without CSS scaling so text stays crisp
                    // use precise fractional day width to avoid stepping/jumps
                    const dayWidthScaled = Math.max(1, baseDayWidth * currentK);
                    // redraw and capture returned calendar info (height may change when day-of-week row appears)
                    const newCalendarInfo = calendar.drawCalendar(calendarG, domainStart, domainEnd, dayWidthScaled, currentK, {width: baseSvgWidth, bodyY: (lanesStartY - axisY), bodyHeight: chartBodyHeight});
                    // update debug display for day width
                    try { if (debugText && typeof debugText.text === 'function') debugText.text('dayWidth: ' + (dayWidthScaled).toFixed(2)); } catch (e) {}
                    // update now debug display (show dateToX, scaleX and newScale positions)
                    try {
                        if (nowDebugText && meta && meta.now) {
                            const nd = new Date(meta.now);
                            const ndStart = new Date(Date.UTC(nd.getFullYear(), nd.getMonth(), nd.getDate()));
                            const dtX = dateToX(ndStart);
                            const scX = scaleX(ndStart);
                            const nsX = (typeof newScale === 'function') ? (newScale(ndStart) - (dayWidth * currentK) / 2) : scX;
                            nowDebugText.text('now: ' + nd.toISOString() + ' startDate:' + ndStart.toISOString() + ' dateToX:' + dtX.toFixed(2) + ' scaleX:' + scX.toFixed(2) + ' newScale:' + nsX.toFixed(2));
                         }
                     } catch (e) { /* ignore */ }
                    // compute new lanes start Y from returned calendar height and shift sprint rects/labels accordingly
                    const newLanesStartY = captionHeight + (newCalendarInfo && newCalendarInfo.calendarHeight ? newCalendarInfo.calendarHeight : axisAreaHeight);
                    const deltaY = newLanesStartY - initialLanesStartY;
                    // compute rescaled time scale matching the applied transform so X positions are consistent
                    const appliedTransform = d3.zoomIdentity.translate(currentTx, 0).scale(currentK);
                    const newScale = appliedTransform.rescaleX(scaleX);
                    // update grid lines vertical position/height when the calendar header height changes
                    try {
                        if (gridG) {
                            // update vertical extents and recompute X using rescaled time scale so lines remain 1px
                            gridG.selectAll('.day-grid-line').each(function(d) {
                                const x = newScale(d) - (dayWidth * currentK) / 2;
                                d3.select(this)
                                    .attr('x1', x).attr('x2', x)
                                    .attr('y1', newLanesStartY).attr('y2', newLanesStartY + chartBodyHeight);
                            });
                        }
                    } catch (e) { /* ignore */ }
                    // reposition sprint rects based on their original base Y
                    sprintRects.forEach(entry => {
                        try { entry.rect.attr('y', entry.originalBaseY + deltaY); } catch (e) {}
                    });
                    // reposition unscaled labels vertically so they remain aligned with their lanes and update X from newScale
                    labels.forEach(l => {
                        const x = newScale(l.start) + l.offsetX;
                        const newY = (typeof l.originalOffsetY !== 'undefined') ? (l.originalOffsetY + deltaY) : l.offsetY;
                        l.el.attr('x', x).attr('y', newY);
                    });
                    // move calendar group horizontally with the same translate (no scale)
                    calendarG.attr('transform', `translate(${currentTx},${axisY})`);
                    // keep lanesStartY updated for subsequent interactions
                    lanesStartY = newLanesStartY;

                    // position now-line on top (keep it unscaled so it stays crisp).
                    // Compute its screen X using the same rescaled x-scale we used for labels above.
                    const nowLine = (meta && meta.now) ? nowLayer.select('.now-line') : null;
                    if (nowLine && typeof nowLine.attr === 'function' && meta && meta.now) {
                                const nd = new Date(meta.now);
                                // use UTC-midnight to match domainStart and avoid timezone-related shifts
                                const nowStart = new Date(Date.UTC(nd.getFullYear(), nd.getMonth(), nd.getDate()));
                        // compute screen X for start of day and offset by half a day cell so it aligns with cell left
                        const nowScreenX = newScale(nowStart) - (dayWidth * currentK) / 2;
                        nowLine.attr('x1', nowScreenX).attr('x2', nowScreenX);
                    }
                    // expand svg width visually so scroll area grows when zooming
                    try {
                        const node = svg.node();
                        if (node && node.style) node.style.width = (baseSvgWidth * currentK) + 'px';
                    } catch (e) { /* ignore */
                    }

                    // compute a transform that matches the transform we applied to contentScaledG so label positions
                    // are computed from the same translate/scale (avoid using the raw event.transform which may differ
                    // from our adjusted currentTx/currentK when centering on wheel zoom)
                    // (we already computed `newScale` above when updating the now-line)
                    // reposition unscaled labels so they remain crisp (compute x from newScale)
                    labels.forEach(l => {
                        const x = newScale(l.start) + l.offsetX;
                        // only update X here; Y was updated above to account for calendar header height
                        l.el.attr('x', x);
                    });
                });

            svg.call(zoom);
            // double-click resets zoom
            svg.on('dblclick', () => {
                svg.transition().duration(400).call(zoom.transform, d3.zoomIdentity);
                // also reset rendered width
                try {
                    svg.node().style.width = baseSvgWidth + 'px';
                } catch (e) {
                }
            });

            container.appendChild(svg.node());
        }, [data, React, d3]);

        return React.createElement('div', {
            ref: ref,
            style: {width: '100%', minHeight: '120px', overflowY: 'auto', overflowX: 'auto', maxHeight: '640px'}
        }, null);
    }

    // Initialize and mount into container
    (async function mount() {
        try {
            const deps = await ensureDeps();
            const container = document.getElementById('sprints-overview-v2-container');
            if (!container) {
                console.warn('SprintsOverviewViewV2 container not found');
                return;
            }
            const root = deps.ReactDOM.createRoot(container);
            root.render(deps.React.createElement(SprintsOverview, deps));
        } catch (e) {
            console.error('Failed to mount SprintsOverview v2', e);
        }
    })();

})();





