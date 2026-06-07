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
    // Provide small aliases
    return {
      React: window.React,
      ReactDOM: window.ReactDOM,
      d3: window.d3
    };
  }

  function formatDate(iso) {
    try { return new Date(iso).toLocaleDateString(); } catch (e) { return iso; }
  }

  function buildScales(meta, width) {
    const start = new Date(meta.chartStart);
    const end = new Date(meta.chartEnd);
    const scaleX = window.d3.scaleTime().domain([start, end]).range([0, width]);
    return { scaleX };
  }

  function SprintsOverview(props) {
    const { React, d3 } = props;
    const [data, setData] = React.useState(null);
    const ref = React.useRef(null);

    React.useEffect(() => {
      let mounted = true;
      fetch('/api/overview/sprints')
        .then(r => r.json())
        .then(json => { if (mounted) setData(json); })
        .catch(e => { console.error('Failed to load overview data', e); });
      return () => { mounted = false; };
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
      const endDate = meta.chartEnd ? new Date(meta.chartEnd) : new Date(startDate.getTime() + 30 * 24 * 3600 * 1000);
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
        const dayIndex = Math.floor((Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()) - Date.UTC(startDate.getFullYear(), startDate.getMonth(), startDate.getDate())) / msPerDay);
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
      const scaleX = d3.scaleTime().domain([startDate, new Date(Date.UTC(startDate.getFullYear(), startDate.getMonth(), startDate.getDate()) + (days - 1) * msPerDay)]).range([firstX, lastX]);

      // top-level caption / title
      svg.append('text')
        .attr('x', baseSvgWidth / 2)
        .attr('y', captionHeight / 2 + 6)
        .attr('text-anchor', 'middle')
        .attr('font-size', 16)
        .attr('fill', '#222')
        .text('Sprints overview');

      // content groups: scaled visuals (rects, grids) and unscaled labels/text
      const contentScaledG = svg.append('g').attr('class', 'content-scaled').attr('transform', `translate(0,0)`);
      const contentUnscaledG = svg.append('g').attr('class', 'content-unscaled');

      // (now line will be drawn last so it's on top)

      // axes: top timeline with month ticks (position under caption)
      const axisY = captionHeight + 8;
      const axisG = svg.append('g').attr('transform', `translate(0,${axisY})`);
      const axisBottom = d3.axisTop(scaleX).ticks(d3.timeMonth.every(1)).tickFormat(d3.timeFormat('%b %Y'));
      axisG.call(axisBottom).selectAll('text').attr('font-size', 11).attr('fill', '#444');

      // draw month backgrounds, weekend background and week borders BEFORE drawing sprints so they appear behind
      const monthBgColors = ['#ffffff', '#f7fbff'];
      let monthIndex = 0;
      for (let m = new Date(startDate.getFullYear(), startDate.getMonth(), 1); m <= endDate; m = new Date(m.getFullYear(), m.getMonth() + 1, 1)) {
        const monthStart = new Date(m.getFullYear(), m.getMonth(), 1);
        const monthEnd = new Date(m.getFullYear(), m.getMonth() + 1, 0);
        const left = dateToX(monthStart) - (Math.floor(dayWidth / 2) - 1);
        const right = dateToX(monthEnd) - (Math.floor(dayWidth / 2) - 1) + (dayWidth - 1);
        const col = monthBgColors[monthIndex % monthBgColors.length];
        contentScaledG.append('rect').attr('x', left).attr('y', axisY).attr('width', Math.max(0, right - left)).attr('height', axisAreaHeight + 4).attr('fill', col).attr('opacity', 1);
        monthIndex++;
      }
      const bgG = contentScaledG.append('g').attr('class', 'backgrounds');
      // weekend shading: Saturday (6) and Sunday (0)
      for (let d = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate()); d <= endDate; d.setDate(d.getDate() + 1)) {
        const day = new Date(d);
        const dow = day.getDay();
        if (dow === 6 || dow === 0) {
          const left = dateToX(new Date(day)) - (Math.floor(dayWidth / 2) - 1);
          bgG.append('rect').attr('x', left).attr('y', captionHeight + axisAreaHeight).attr('width', dayWidth).attr('height', chartBodyHeight + 4).attr('fill', '#f5f7fa');
        }
      }
      // week boundary lines (draw at Mondays) so border between weekdays only
      const weekLineG = contentScaledG.append('g').attr('class', 'week-lines');
      for (let d = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate()); d <= endDate; d.setDate(d.getDate() + 1)) {
        const day = new Date(d);
        if (day.getDay() === 1) { // Monday
          const x = dateToX(day);
          weekLineG.append('line').attr('x1', x).attr('x2', x).attr('y1', axisY).attr('y2', axisY + chartBodyHeight + 4).attr('stroke', '#e6eef8').attr('stroke-width', 1);
        }
      }

      // render lanes (positioned beneath the axis) inside contentScaledG so they pan/zoom
      const lanesStartY = captionHeight + axisAreaHeight;
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
              const rr = parseInt(hex.substr(1,2),16);
              const gg = parseInt(hex.substr(3,2),16);
              const bb = parseInt(hex.substr(5,2),16);
              const aa = parseInt(hex.substr(7,2),16)/255;
              return `rgba(${rr},${gg},${bb},${aa.toFixed(2)})`;
            }
            return hex;
          }

          const rect = g.append('rect')
            .attr('x', rectX).attr('y', y).attr('width', rectW).attr('height', laneHeight - 16)
            .attr('fill', normalizeColor(s.color)).attr('stroke', '#00000020').attr('rx', 3).attr('ry', 3);

          rect.on('mouseenter', (event) => {
              const tip = document.getElementById('kassandra-sprint-tooltip') || (() => {
                const t = document.createElement('div'); t.id = 'kassandra-sprint-tooltip';
                t.style.position = 'fixed'; t.style.background = 'var(--lumo-base-color, white)';
                t.style.color = 'var(--lumo-body-text-color, #111)';
                t.style.border = '1px solid #ccc'; t.style.padding = '6px 8px'; t.style.zIndex = 9999; t.style.boxShadow = '0 2px 6px rgba(0,0,0,0.15)';
                document.body.appendChild(t); return t; })();
              tip.innerHTML = `<b>${s.name}</b><br/>${formatDate(s.start)} — ${formatDate(s.end)}<br/>Status: ${s.status || ''}`;
              tip.style.left = (event.clientX + 12) + 'px'; tip.style.top = (event.clientY + 12) + 'px'; tip.style.display = 'block';
            })
            .on('mouseleave', () => {
              const t = document.getElementById('kassandra-sprint-tooltip'); if (t) t.style.display = 'none';
            }).on('click', () => {
              if (s.jiraUrl) window.open(s.jiraUrl, '_blank');
            });

          // label when space: create unscaled text element so it remains crisp when zooming
          const label = s.name || '';
          if (rectW > 40) {
            const tx = rectX + 6;
            const ty = y + (laneHeight - 16) / 2 + 6;
            const tEl = contentUnscaledG.append('text').attr('x', tx).attr('y', ty).attr('fill', '#000').attr('font-size', 12).text(label);
            labels.push({ el: tEl, start: new Date(s.start), end: new Date(s.end), laneIndex: li, offsetX: 6, offsetY: ty });
          }

          sprintRects.push({ rect, s, laneIndex: li });
        });
      });

      // now-line layer: create after sprints so it renders on top; we'll apply same transform as scaled content
      const nowLayer = svg.append('g').attr('class', 'now-layer');
      if (meta.now) {
        const nowX = dateToX(meta.now);
        nowLayer.append('line').attr('x1', nowX).attr('x2', nowX).attr('y1', captionHeight).attr('y2', height - footerHeight).attr('stroke', '#ff0000').attr('stroke-width', 1).attr('class', 'now-line');
      }

      // week numbers row below the months (ISO week number using Monday-based %W) - axisG is already translated to axisY
      const weekYOffset = 16;
      const weekG = axisG.append('g').attr('transform', `translate(0,${weekYOffset})`);
      const weekTicks = d3.timeWeeks(startDate, new Date(Date.UTC(startDate.getFullYear(), startDate.getMonth(), startDate.getDate()) + days * msPerDay));
      const weekFormat = d3.timeFormat('%W');
      weekG.selectAll('text').data(weekTicks).enter().append('text')
        .attr('x', d => scaleX(d))
        .attr('y', 0)
        .attr('text-anchor', 'middle')
        .attr('font-size', 10)
        .attr('fill', '#666')
        .text(d => 'W' + weekFormat(d));

      // day-of-week and day-of-month labels when dayWidth allows (put in axisG to update positions on zoom)
      const dowG = axisG.append('g').attr('transform', `translate(0,${12})`);
      const domG = axisG.append('g').attr('transform', `translate(0,${26})`);
      const dayDates = [];
      for (let d = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate()); d <= endDate; d.setDate(d.getDate() + 1)) {
        dayDates.push(new Date(d));
      }
      if (dayWidth >= 10) {
        dowG.selectAll('text').data(dayDates).enter().append('text')
          .attr('x', d => scaleX(d)).attr('y', 0).attr('text-anchor', 'middle').attr('font-size', 10).attr('fill', '#444')
          .text(d => ['M','T','W','T','F','S','S'][new Date(d).getDay() === 0 ? 6 : new Date(d).getDay()-1]);
      }
      if (dayWidth >= 16) {
        domG.selectAll('text').data(dayDates).enter().append('text')
          .attr('x', d => scaleX(d)).attr('y', 0).attr('text-anchor', 'middle').attr('font-size', 10).attr('fill', '#222')
          .text(d => d.getDate());
      }

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
          // If user used wheel to zoom, center the content so it grows both left and right
          let tx = t.x;
          try {
            const srcType = event.sourceEvent && event.sourceEvent.type;
            if (srcType === 'wheel') {
              const containerWidth = container.clientWidth || width;
              tx = Math.round((containerWidth - baseSvgWidth * t.k) / 2);
            }
          } catch (e) { /* ignore */ }
          // apply horizontal scale + translate to scaled visuals and now-layer
          contentScaledG.attr('transform', `translate(${tx},0) scale(${t.k},1)`);
          nowLayer.attr('transform', `translate(${tx},0) scale(${t.k},1)`);
          // expand svg width visually so scroll area grows when zooming
          try {
            const node = svg.node();
            if (node && node.style) node.style.width = (baseSvgWidth * t.k) + 'px';
          } catch (e) { /* ignore */ }
          const newScale = t.rescaleX(scaleX);
          // update axis ticks and week/day labels positions using rescaled scale
          axisG.call(axisBottom.scale(newScale)).selectAll('text').attr('font-size', 11).attr('fill', '#444');
          weekG.selectAll('text').attr('x', d => newScale(d));
          dowG.selectAll('text').attr('x', d => newScale(d));
          domG.selectAll('text').attr('x', d => newScale(d));
          // reposition unscaled labels so they remain crisp (compute x from newScale)
          labels.forEach(l => {
            const x = newScale(l.start) + l.offsetX;
            l.el.attr('x', x).attr('y', l.offsetY);
          });
        });

      svg.call(zoom);
      // double-click resets zoom
      svg.on('dblclick', () => {
        svg.transition().duration(400).call(zoom.transform, d3.zoomIdentity);
        // also reset rendered width
        try { svg.node().style.width = baseSvgWidth + 'px'; } catch (e) {}
      });

      container.appendChild(svg.node());
    }, [data, React, d3]);

    return React.createElement('div', { ref: ref, style: { width: '100%', minHeight: '120px', overflowY: 'auto', overflowX: 'auto', maxHeight: '640px' } }, null);
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



