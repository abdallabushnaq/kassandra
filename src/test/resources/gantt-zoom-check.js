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

const taskId = '00000000-0000-0000-0000-000000000001';
const milestoneId = '00000000-0000-0000-0000-000000000002';
const shortTaskId = '00000000-0000-0000-0000-000000000003';
const storyId = '00000000-0000-0000-0000-000000000004';
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

function formatValue(value) {
    if (typeof value === 'string')
        return JSON.stringify(value);
    if (value && typeof value.tagName === 'string' && typeof value.getAttribute === 'function')
        return `<${value.tagName.toLowerCase()} class="${value.getAttribute('class') || ''}">`;
    if (value !== null && typeof value === 'object')
        return JSON.stringify(value);
    return String(value);
}

function assertEqual(actual, expected, message) {
    if (actual !== expected)
        throw new Error(`${message}: expected ${formatValue(expected)}, found ${formatValue(actual)}`);
}

function assertCondition(condition, message, expected, actual) {
    if (!condition)
        throw new Error(`${message}: expected ${formatValue(expected)}, found ${formatValue(actual)}`);
}

function assertPresent(element, message) {
    assertCondition(!!element, message, 'element present', element || 'no element');
}

function assertAbsent(element, message) {
    assertCondition(!element, message, 'no element', element || 'no element');
}

async function waitFor(condition, message) {
    for (let attempt = 0; attempt < 50; attempt++) {
        if (condition())
            return;
        await delay(100);
    }
    throw new Error(`${message}: expected condition to become true, found false after 50 attempts`);
}

function state(dayWidth, visualScale = 1) {
    localStorage.setItem('kassandra.chart.chart.view', JSON.stringify({
        dayWidth, visualScale, scrollOffset: 0, scrollYOffset: 0,
    }));
}

function group(id) {
    const element = document.querySelector(`[data-task-id="${id}"]`);
    assertPresent(element, `task group ${id}`);
    return element;
}

function dependency(predecessorId, successorId) {
    return document.querySelector(`.task-dependency[data-predecessor-id="${predecessorId}"][data-successor-id="${successorId}"]`);
}

function incomingDependencies(successorId) {
    return document.querySelectorAll(`.task-dependency[data-successor-id="${successorId}"]`);
}

function milestoneLabel() {
    const label = group(milestoneId).querySelector('.milestone-label');
    assertPresent(label, 'milestone label element');
    assertPresent(label.firstChild, 'milestone label text node');
    return label.firstChild.textContent;
}

function labels(id) {
    return [...group(id).querySelectorAll('text')].map(element => element.firstChild.textContent);
}

const features = {
    renderTaskProgressBarAtDayWidth: () => !!group(taskId).querySelector('.task-progress-bar'),
    renderTaskProgressTextAtDayWidth: () => !!group(taskId).querySelector('.task-progress-text'),
    renderTaskKeyAtDayWidth: () => labels(taskId).some(label => label.includes('K-1') && label.includes('Alpha')),
    renderTaskNonWorkingDaysAtDayWidth: () => group(taskId).querySelectorAll('.task-body').length > 1,
    renderTaskBorderAtDayWidth: () => !!group(taskId).querySelector('.task-border'),
    renderMilestoneStartDateAtDayWidth: () => milestoneLabel().includes('2025'),
    renderMilestoneStartTimeAtDayWidth: () => milestoneLabel().includes('08:00'),
    renderMilestoneBorderAtDayWidth: () => {
        const body = group(milestoneId).querySelector('.milestone-body');
        return !!body && body.getAttribute('stroke') !== 'none';
    },
    renderDependenciesAtDayWidth: () => incomingDependencies(milestoneId).length > 0,
};

function settings(value) {
    return Object.fromEntries(Object.keys(features).map(key => [key, value]));
}

function taskEdge(id, side) {
    const task = group(id);
    const milestone = task.querySelector('.milestone-body');
    if (milestone) {
        const points = milestone.points;
        return {x: points.getItem(side === 'right' ? 1 : 3).x, y: points.getItem(1).y};
    }
    const body = task.querySelector('.task-body');
    const rect = body || (id === storyId ? task.querySelector(':scope > rect') : null);
    assertPresent(rect, `task body for ${id}`);
    const x = Number(rect.getAttribute('x'));
    const width = Number(rect.getAttribute('width'));
    const y = Number(rect.getAttribute('y'));
    const storySide = body ? null : task.querySelectorAll(':scope > rect')[1];
    if (!body)
        assertPresent(storySide, `story side for ${id}`);
    return {
        x: side === 'right' ? x + width - (body ? 0 : 1) : x,
        y: body ? y + Number(rect.getAttribute('height')) / 2
            : (y + Number(storySide.getAttribute('y')) + Number(storySide.getAttribute('height'))) / 2,
    };
}

function assertRelation(type, predecessorId, successorId, color) {
    const start = taskEdge(predecessorId, 'right');
    const end = taskEdge(successorId, 'left');
    const relation = dependency(predecessorId, successorId);
    assertPresent(relation, `${type} dependency group for successor ${successorId}`);
    const path = relation.querySelector('path');
    const arrow = relation.querySelector('polygon');
    assertPresent(path, `${type} dependency connector path`);
    assertPresent(arrow, `${type} dependency arrowhead`);
    const commands = [...path.getAttribute('d').matchAll(/([MHVQ])\s+([^MHVQ]+)/g)]
        .map(match => [match[1], ...match[2].trim().split(/\s+/).map(Number)]);
    assertEqual(commands.map(command => command[0]).join(''), 'MHQVQHQVQH', `${type} dependency route commands`);
    const near = (a, b) => Math.abs(a - b) < 0.01;
    assertCondition(near(commands[0][1], start.x) && near(commands[0][2], start.y),
        `${type} predecessor edge`, start, {x: commands[0][1], y: commands[0][2]});
    const tip = arrow.points.getItem(2);
    assertCondition(near(tip.x, end.x) && near(tip.y, end.y),
        `${type} successor edge`, end, {x: tip.x, y: tip.y});
    assertCondition(near(commands[9][1], end.x - 5), `${type} path meets arrow base`, end.x - 5, commands[9][1]);
    const arrowBaseX = end.x - 5;
    const arrowBasePoints = [arrow.points.getItem(0).x, arrow.points.getItem(1).x];
    assertCondition(arrowBasePoints.every(x => near(x, arrowBaseX)), `${type} arrow points toward successor`,
        [arrowBaseX, arrowBaseX], arrowBasePoints);
    const gap = end.y - Math.sign(end.y - start.y) * 9.5;
    const rowGapY = [commands[4][2], commands[4][4]];
    assertCondition(rowGapY.every(y => near(y, gap)), `${type} return in row gap`, [gap, gap], rowGapY);
    assertCondition(commands[5][1] < commands[4][3], `${type} horizontal return moves left`,
        `< ${commands[4][3]}`, commands[5][1]);
    const radius = commands[2][1] - commands[1][1];
    const roundedExit = {
        radius,
        horizontalY: commands[2][2],
        verticalX: commands[2][3],
        verticalDistance: Math.abs(commands[2][4] - start.y),
    };
    assertCondition(radius > 0 && near(commands[2][2], start.y)
        && near(commands[2][3], commands[2][1])
        && near(Math.abs(commands[2][4] - start.y), radius), `${type} rounded exit`,
        {radius: '> 0', horizontalY: start.y, verticalX: commands[2][1], verticalDistance: radius}, roundedExit);
    const roundedReturn = {
        rowGapX: commands[4][1],
        rowGapWidth: commands[4][1] - commands[4][3],
        verticalX: commands[6][1],
        returnWidth: commands[5][1] - commands[6][1],
        entryX: commands[8][1],
        entryWidth: commands[8][3] - commands[8][1],
        entryStartY: commands[8][2],
        entryEndY: commands[8][4],
    };
    assertCondition(near(commands[4][1], commands[2][1])
        && near(commands[4][1] - commands[4][3], radius)
        && near(commands[6][1], commands[6][3])
        && near(commands[5][1] - commands[6][1], radius)
        && near(commands[8][1], commands[6][1])
        && near(commands[8][3] - commands[8][1], radius)
        && near(commands[8][2], end.y) && near(commands[8][4], end.y), `${type} rounded return and entry`,
        {
            rowGapX: commands[2][1], rowGapWidth: radius, verticalX: commands[6][1], returnWidth: radius,
            entryX: commands[6][1], entryWidth: radius, entryY: end.y
        }, roundedReturn);
    assertCondition(path.getAttribute('stroke') === color && arrow.getAttribute('fill') === color,
        `${type} relation color`, {pathStroke: color, arrowFill: color},
        {pathStroke: path.getAttribute('stroke'), arrowFill: arrow.getAttribute('fill')});
}

function verifyRelations(type, data, render) {
    const originalTasks = structuredClone(data.tasks);
    const originalTheme = structuredClone(data.meta.theme.ganttTheme);
    try {
        const task = data.tasks.find(item => item.id === taskId);
        const milestone = data.tasks.find(item => item.id === milestoneId);
        const shortTask = data.tasks.find(item => item.id === shortTaskId);
        const story = data.tasks.find(item => item.id === storyId);
        const relation = predecessorId => ({predecessorId, visible: true});
        const thresholds = {...settings(0), renderTaskNonWorkingDaysAtDayWidth: 1000};
        data.meta.theme.ganttTheme.relationColor = 0x112233;
        data.meta.theme.ganttTheme.criticalRelationColor = 0xcc3344;

        for (const width of [2, 20, 30]) {
            render(thresholds, width);
            assertRelation(`${type} milestone successor at ${width}`, taskId, milestoneId, '#112233ff');
            assertRelation(`${type} milestone predecessor at ${width}`, milestoneId, storyId, '#112233ff');
        }

        shortTask.predecessors = [relation(taskId)];
        task.rowIndex = 0;
        shortTask.rowIndex = 2;
        task.critical = shortTask.critical = true;
        for (const [start, finish] of [
            ['2025-06-11T08:00:00', '2025-06-11T16:30:00'],
            ['2025-06-07T08:00:00', '2025-06-07T16:30:00'],
            ['2025-06-09T16:30:00', '2025-06-10T14:00:00'],
        ]) {
            shortTask.start = start;
            shortTask.finish = finish;
            render(thresholds);
            assertRelation(`${type} separated, overlapping or adjacent dates`, taskId, shortTaskId, '#cc3344ff');
        }

        task.rowIndex = 2;
        shortTask.rowIndex = 0;
        render(thresholds);
        assertRelation(`${type} predecessor below successor`, taskId, shortTaskId, '#cc3344ff');

        story.predecessors = [relation(shortTaskId)];
        render(thresholds);
        assertRelation(`${type} story successor`, shortTaskId, storyId, '#112233ff');

        milestone.predecessors = [relation(storyId)];
        render(thresholds);
        assertRelation(`${type} story predecessor`, storyId, milestoneId, '#112233ff');

        task.start = '2024-01-01T08:00:00';
        task.finish = '2024-01-02T16:30:00';
        render(thresholds);
        assertPresent(dependency(taskId, shortTaskId)?.querySelector('path'), `${type} partially clipped relation`);
        shortTask.start = '2024-01-03T08:00:00';
        shortTask.finish = '2024-01-03T16:30:00';
        render(thresholds);
        assertAbsent(dependency(taskId, shortTaskId)?.querySelector('path'), `${type} fully clipped relation`);
        task.finish = null;
        render(thresholds);
        assertAbsent(dependency(taskId, shortTaskId)?.querySelector('path'), `${type} unscheduled predecessor`);
    } finally {
        data.tasks = originalTasks;
        data.meta.theme.ganttTheme = originalTheme;
    }
}

async function selectTask(id, key = 'Enter', target = group(id)) {
    group(id).focus();
    target.dispatchEvent(new KeyboardEvent('keydown', {key, bubbles: true, cancelable: true}));
    await waitFor(() => group(id).classList.contains('is-selected'), `select task ${id}`);
}

async function clearTaskSelection() {
    document.querySelector('.gantt-inspection-viewport').dispatchEvent(
        new KeyboardEvent('keydown', {key: 'Escape', bubbles: true, cancelable: true}));
    await waitFor(() => !document.querySelector('.gantt-task.is-selected'), 'clear task selection');
}

async function verifyTaskInspection(type, data, render) {
    const originalTasks = structuredClone(data.tasks);
    const host = document.getElementById('chart');
    const originalWidth = host.style.width;
    const hiddenId = '00000000-0000-0000-0000-000000000005';
    const branchId = '00000000-0000-0000-0000-000000000006';
    const isolatedId = '00000000-0000-0000-0000-000000000007';
    const missingId = '00000000-0000-0000-0000-000000000008';
    const relation = (predecessorId, visible = true) => ({predecessorId, visible});
    const details = () => document.querySelector('.gantt-task-details');
    const field = name => [...details().querySelectorAll('dt')]
        .find(term => term.textContent === name)?.nextElementSibling?.textContent;
    try {
        host.style.width = '1600px';
        const [task, milestone, shortTask, story] = data.tasks;
        shortTask.predecessors = [relation(taskId)];
        story.predecessors = [relation(milestoneId), relation(shortTaskId), relation(hiddenId, false), relation(missingId)];
        task.critical = milestone.critical = true;
        const notes = '<img src=x onerror="window.inspectorInjected=true">';
        story.notes = notes;
        data.tasks.push(
            {
                ...structuredClone(shortTask),
                id: hiddenId,
                name: 'Hidden resource predecessor',
                rowIndex: 4,
                predecessors: [relation(isolatedId)]
            },
            {...structuredClone(shortTask), id: branchId, name: 'Unrelated successor', rowIndex: 5},
            {
                ...structuredClone(task),
                id: isolatedId,
                name: 'Isolated task',
                rowIndex: 6,
                progress: 0,
                predecessors: []
            },
        );
        render(settings(0));
        await waitFor(() => document.querySelector('svg').getAttribute('width') === '1600', `${type} initial inspection width`);
        assertEqual(details().hidden, true, `${type} details initially hidden`);
        await selectTask(storyId);
        assertEqual(group(storyId).getAttribute('aria-pressed'), 'true', `${type} accessible selection`);
        assertEqual(document.querySelectorAll('.gantt-task.is-predecessor, .gantt-task.is-muted').length, 0,
            `${type} other task styling unchanged`);
        assertEqual(getComputedStyle(group(milestoneId)).opacity, '1', `${type} direct predecessor not faded`);
        assertEqual(group(storyId).querySelector('.gantt-task-outline').tagName.toLowerCase(), 'path',
            `${type} story outline follows story shape`);
        assertEqual(group(milestoneId).querySelector('.gantt-task-outline').getAttribute('points'),
            group(milestoneId).querySelector('.milestone-body').getAttribute('points').replace(/ (\S+)$/, ''),
            `${type} milestone outline follows diamond`);
        const body = group(taskId).querySelector('.task-body');
        const outline = group(taskId).querySelector('.gantt-task-outline');
        assertEqual(outline.getAttribute('x'), body.getAttribute('x'), `${type} task outline starts at task`);
        assertEqual(outline.getAttribute('y'), body.getAttribute('y'), `${type} task outline top matches task`);
        assertEqual(outline.getAttribute('height'), body.getAttribute('height'), `${type} task outline height matches task`);
        assertCondition(Number(outline.getAttribute('width')) < Number(document.querySelector('svg').getAttribute('width')),
            `${type} task outline is not row-wide`, 'less than chart width', outline.getAttribute('width'));
        assertEqual(dependency(hiddenId, storyId), null, `${type} hidden arrow excluded`);
        assertEqual(dependency(taskId, branchId).classList.contains('is-muted'), true, `${type} unrelated arrow faded`);
        assertEqual(document.querySelectorAll('.task-dependency.is-emphasized').length, 2, `${type} direct edges only`);
        assertEqual(dependency(taskId, milestoneId).classList.contains('is-emphasized'), false,
            `${type} indirect edge not emphasized`);
        const emphasized = dependency(milestoneId, storyId);
        assertEqual(getComputedStyle(emphasized.querySelector('path')).strokeWidth, '3px', `${type} highlighted connector`);
        assertEqual(getComputedStyle(emphasized.querySelector('polygon')).strokeWidth, '1px', `${type} highlighted arrowhead`);
        assertEqual(emphasized.querySelector('path').getAttribute('stroke'),
            emphasized.querySelector('polygon').getAttribute('fill'), `${type} arrow colours preserved`);
        assertEqual(field('Notes'), notes, `${type} notes rendered literally`);
        assertEqual(details().querySelector('img'), null, `${type} notes do not create HTML`);
        assertEqual(details().querySelectorAll('li').length, 3, `${type} direct predecessors including missing reference`);
        assertCondition(details().textContent.includes(`Unavailable task (${missingId})`),
            `${type} unresolved predecessor`, 'unavailable reference displayed', details().textContent);
        assertEqual(details().querySelector(`[data-predecessor-id="${taskId}"]`), null,
            `${type} indirect predecessor not listed`);
        await waitFor(() => details().getBoundingClientRect().left
            >= document.querySelector('.gantt-inspection-viewport').getBoundingClientRect().right, `${type} right docking`);
        assertEqual(document.activeElement.getAttribute('data-task-id'), storyId, `${type} selection focus restored`);

        host.style.width = '700px';
        await waitFor(() => document.querySelector('.gantt-inspection-layout').classList.contains('is-narrow')
                && details().getBoundingClientRect().top >= document.querySelector('.gantt-inspection-viewport').getBoundingClientRect().bottom,
            `${type} below-chart inspector`);
        assertEqual(group(storyId).classList.contains('is-selected'), true, `${type} selection survives responsive redraw`);
        host.style.width = '1600px';
        await waitFor(() => !document.querySelector('.gantt-inspection-layout').classList.contains('is-narrow'), `${type} wide layout restored`);
        const beforePanelWheel = document.querySelector('[contentGroup]').getAttribute('transform');
        details().dispatchEvent(new WheelEvent('wheel', {
            bubbles: true,
            cancelable: true,
            shiftKey: true,
            deltaY: 100
        }));
        await delay(150);
        assertEqual(document.querySelector('[contentGroup]').getAttribute('transform'), beforePanelWheel, `${type} panel wheel does not pan chart`);
        details().querySelector('button').click();
        await waitFor(() => details().hidden, `${type} close details`);

        render({...settings(0), renderDependenciesAtDayWidth: 1000});
        assertEqual(document.querySelectorAll('.task-dependency').length, 0, `${type} low-zoom dependencies initially hidden`);
        await selectTask(storyId);
        assertEqual(document.querySelectorAll('.task-dependency').length, 2, `${type} only direct edges override zoom threshold`);
        for (const modifiers of [{shiftKey: true}, {altKey: true}, {}]) {
            const viewport = document.querySelector('.gantt-inspection-viewport');
            const svg = viewport.querySelector('svg');
            viewport.dispatchEvent(new WheelEvent('wheel', {
                bubbles: true, cancelable: true, deltaY: -1, deltaX: Object.keys(modifiers).length ? 0 : 20,
                clientX: viewport.getBoundingClientRect().left + 200, ...modifiers,
            }));
            await waitFor(() => viewport.querySelector('svg') !== svg, `${type} navigation redraw`);
            assertEqual(group(storyId).classList.contains('is-selected'), true, `${type} selection survives navigation`);
            assertEqual(document.querySelectorAll('.task-dependency.is-emphasized').length, 2, `${type} direct edges survive navigation`);
        }
        await clearTaskSelection();
        assertEqual(document.querySelectorAll('.task-dependency').length, 0, `${type} clearing restores zoom threshold`);
        assertEqual(document.querySelectorAll('.gantt-task.is-muted').length, 0, `${type} clearing restores task opacity`);
        await selectTask(milestoneId, ' ');
        assertEqual(field('Type'), 'Milestone', `${type} milestone inspection`);
        await selectTask(taskId, 'Enter', group(taskId).querySelector('text'));
        assertEqual(field('Type'), 'Task', `${type} task label inspection`);
        await selectTask(isolatedId);
        assertEqual(field('Progress'), '0%', `${type} zero progress displayed`);
        assertEqual(details().querySelectorAll('li').length, 0, `${type} isolated task has no predecessors`);

        if (type === 'combined') {
            const viewport = document.querySelector('.gantt-inspection-viewport');
            const hitTarget = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            hitTarget.setAttribute('data-tooltip-html', '<b>Existing burndown tooltip</b>');
            viewport.querySelector('svg').appendChild(hitTarget);
            const rect = viewport.getBoundingClientRect();
            hitTarget.dispatchEvent(new MouseEvent('mousemove', {
                bubbles: true,
                clientX: rect.left + 50,
                clientY: rect.top + 50
            }));
            const tooltip = viewport.querySelector('.gantt-burndown-tooltip');
            assertEqual(tooltip.style.display, 'block', `${type} existing tooltip still visible`);
            assertEqual(tooltip.textContent, 'Existing burndown tooltip', `${type} existing tooltip content`);
            viewport.dispatchEvent(new MouseEvent('mouseleave'));
            assertEqual(tooltip.style.display, 'none', `${type} tooltip dismissed`);
            hitTarget.remove();
        }

        task.predecessors = [relation(storyId)];
        render({...settings(0), renderDependenciesAtDayWidth: 1000});
        assertEqual(details().hidden, true, `${type} remount starts unselected`);
        await selectTask(storyId);
        assertEqual(document.querySelectorAll('.task-dependency.is-emphasized').length, 2, `${type} cycle does not add indirect edges`);
        assertEqual(details().querySelector(`[data-predecessor-id="${taskId}"]`), null, `${type} cycle does not add indirect tasks`);
        assertEqual(group(storyId).classList.contains('is-selected'), true, `${type} cycle preserves selected task`);
        assertEqual(document.querySelectorAll('.gantt-task-details').length, 1, `${type} remount cleans up inspector`);
        const theme = data.meta.theme;
        try {
            data.meta.theme = {...theme, themeVariance: 'dark'};
            render(settings(0));
            await selectTask(isolatedId);
            assertEqual(getComputedStyle(details()).backgroundColor, 'rgb(30, 41, 59)', `${type} dark inspector background`);
            assertEqual(getComputedStyle(details()).color, 'rgb(241, 245, 249)', `${type} dark inspector text`);
        } finally {
            data.meta.theme = theme;
        }
    } finally {
        data.tasks = originalTasks;
        host.style.width = originalWidth;
        render(settings(0));
    }
}

/** Mounts a known chart for real WebDriver pointer and keyboard gestures. */
export async function mountGanttPointerInspection(type, gantt, combined) {
    const source = structuredClone(type === 'gantt' ? gantt : combined);
    const bundleUrl = type === 'gantt' ? '/js/generated/gantt/gantt-bundle.js' : '/js/generated/burndown/gantt-burndown-bundle.js';
    const mountName = type === 'gantt' ? 'mountGanttChart' : 'mountGanttBurndownChart';
    await import(bundleUrl);
    state(40);
    source.meta.ganttZoom = settings(0);
    document.getElementById('chart').style.width = '1600px';
    window[mountName]('chart', source, 'Pointer inspection test');
    await waitFor(() => document.querySelector('.task-body') && document.querySelector('svg').getAttribute('width') === '1600',
        `${type} pointer fixture`);
    await delay(200);
}

/**
 * Renders both chart types and verifies independent threshold behavior.
 * @param {object} gantt standalone chart data
 * @param {object} combined combined chart data
 */
export async function verifyGanttZoom(gantt, combined) {
    await document.fonts.ready;

    for (const [type, source, bundleUrl, mountName] of [
        ['gantt', gantt, '/js/generated/gantt/gantt-bundle.js', 'mountGanttChart'],
        ['combined', combined, '/js/generated/burndown/gantt-burndown-bundle.js', 'mountGanttBurndownChart'],
    ]) {
        await import(bundleUrl);
        const mount = window[mountName];
        const data = structuredClone(source);
        const render = (thresholds, dayWidth = 20, visualScale = 1) => {
            data.meta.ganttZoom = thresholds;
            state(dayWidth, visualScale);
            mount('chart', data, 'Zoom test');
            assertPresent(document.querySelector('#chart svg'), `${type} chart SVG`);
        };

        render({...settings(0), renderTaskBorderAtDayWidth: 13.5, renderMilestoneBorderAtDayWidth: 16.5}, 20, 1.5);
        const meter = document.querySelector('.zoom-meter');
        const track = meter?.querySelector('.zoom-meter-track');
        const currentMarker = meter?.querySelector('.zoom-meter-current');
        const svg = document.querySelector('#chart svg');
        const captionTitle = svg.querySelector('text');
        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d');
        context.font = 'normal 18px sans-serif';
        const leftTextEnd = 3 + context.measureText(captionTitle.textContent).width + 8;
        const availableGap = Number(svg.getAttribute('width')) - 4 - 350 - leftTextEnd;
        if (availableGap >= 256)
            assertPresent(track, `${type} zoom meter track with available gap ${availableGap}`);
        else
            assertAbsent(meter, `${type} meter omitted with available gap ${availableGap}`);
        if (track) {
            assertEqual(track.getAttribute('width'), '256', `${type} zoom meter width`);
            assertEqual(track.getAttribute('height'), '14', `${type} zoom meter height`);
            assertEqual(Number(track.getAttribute('data-max-effective-day-width')), 32,
                `${type} maximum effective day width`);
            const isDarkTheme = source.meta.theme.themeVariance.toLowerCase() === 'dark';
            assertEqual(track.getAttribute('stroke'), isDarkTheme ? '#64748b' : '#94a3b8',
                `${type} zoom meter border color`);
            assertPresent(currentMarker, `${type} current zoom marker`);
            assertEqual(currentMarker.getAttribute('stroke'), isDarkTheme ? '#fb7185' : '#e11d48',
                `${type} current zoom color`);
            assertEqual(Number(currentMarker.getAttribute('data-effective-day-width')), 30,
                `${type} current zoom includes visual scale`);
            const timelineHint = [...svg.querySelectorAll('text')]
                .find(element => element.textContent.includes('Shift + mouse wheel:'));
            assertPresent(timelineHint, `${type} timeline zoom hint`);
            assertCondition(timelineHint.textContent.includes('(30px)'), `${type} hint reports effective day width`,
                '(30px)', timelineHint.textContent);
            const minWidth = Number(track.getAttribute('data-min-effective-day-width'));
            const maxWidth = Number(track.getAttribute('data-max-effective-day-width'));
            const expectedMarkerX = Number(track.getAttribute('x'))
                + ((30 - minWidth) / (maxWidth - minWidth)) * Number(track.getAttribute('width'));
            assertCondition(Math.abs(Number(currentMarker.getAttribute('x1')) - expectedMarkerX) < 1e-7,
                `${type} current zoom marker position`, expectedMarkerX, currentMarker.getAttribute('x1'));
            const borderThreshold = [...meter.querySelectorAll('.zoom-meter-threshold')]
                .find(line => Number(line.getAttribute('data-effective-day-width')) === 13.5);
            assertPresent(borderThreshold, `${type} task border threshold marker`);
            assertEqual(borderThreshold.getAttribute('stroke'), isDarkTheme ? '#cbd5e1' : '#475569',
                `${type} threshold color`);
            assertEqual(borderThreshold.querySelector('title').textContent, 'Task borders',
                `${type} task border threshold tooltip`);
            assertCondition(!borderThreshold.querySelector('title').textContent.includes('appear'),
                `${type} task border threshold tooltip omits "appear"`);
            const borderHitArea = [...meter.querySelectorAll('.zoom-meter-threshold-hit-area')]
                .find(area => Number(area.getAttribute('data-effective-day-width')) === 13.5);
            assertPresent(borderHitArea, `${type} task border threshold hover area`);
            assertEqual(borderHitArea.getAttribute('width'), '6', `${type} threshold hover target width`);
            assertEqual(borderHitArea.getAttribute('pointer-events'), 'all', `${type} threshold hover target`);
            assertEqual(borderHitArea.querySelector('title').textContent, 'Task borders',
                `${type} task border threshold hover tooltip`);
            const sharedThreshold = [...meter.querySelectorAll('.zoom-meter-threshold')]
                .find(line => Number(line.getAttribute('data-effective-day-width')) === 16.5);
            assertPresent(sharedThreshold, `${type} shared calendar/detail threshold marker`);
            assertEqual(sharedThreshold.querySelector('title').textContent,
                'Milestone borders\nDay-of-week labels and grid', `${type} multiline threshold tooltip`);
            assertEqual(sharedThreshold.querySelector('title').textContent.includes('px'), false,
                `${type} threshold tooltip omits width`);
        }

        render(settings(0), 20, 10);
        const cappedTrack = document.querySelector('.zoom-meter-track');
        const cappedMarker = document.querySelector('.zoom-meter-current');
        const cappedTimelineHint = [...document.querySelectorAll('#chart svg text')]
            .find(element => element.textContent.includes('Shift + mouse wheel:'));
        assertPresent(cappedTimelineHint, `${type} capped timeline zoom hint`);
        assertCondition(cappedTimelineHint.textContent.includes('(32px)'), `${type} zoom capped at 32 effective px/day`,
            '(32px)', cappedTimelineHint.textContent);
        if (cappedTrack) {
            assertEqual(Number(cappedTrack.getAttribute('data-max-effective-day-width')), 32,
                `${type} capped meter maximum`);
            assertEqual(Number(cappedMarker.getAttribute('data-effective-day-width')), 32,
                `${type} capped marker position`);
        }

        const host = document.getElementById('chart');
        const originalWidth = host.style.width;
        try {
            host.style.width = '600px';
            render(settings(0));
            assertAbsent(document.querySelector('.zoom-meter'), `${type} meter omitted when caption gap is too narrow`);
        } finally {
            host.style.width = originalWidth;
        }

        for (const [key, visible] of Object.entries(features)) {
            const configuredThreshold = source.meta.ganttZoom[key];
            for (const [threshold, expected] of [
                [configuredThreshold + 0.01, false],
                [configuredThreshold, true],
                [Math.max(0, configuredThreshold - 0.01), true],
            ]) {
                render({...settings(0), [key]: threshold}, configuredThreshold);
                const expectedVisibility = key === 'renderTaskProgressBarAtDayWidth' && type === 'gantt' ? false : expected;
                assertEqual(visible(), expectedVisibility, `${type} ${key} visibility at threshold ${threshold}`);
            }
        }

        render({...settings(0), renderTaskProgressBarAtDayWidth: 30, renderTaskProgressTextAtDayWidth: 0});
        const progressIndependence = {
            progressBar: features.renderTaskProgressBarAtDayWidth(),
            progressText: features.renderTaskProgressTextAtDayWidth(),
        };
        assertCondition(!progressIndependence.progressBar && progressIndependence.progressText,
            `${type} progress text independent of progress bar`,
            {progressBar: false, progressText: true}, progressIndependence);

        for (const [showDate, showTime] of [[false, false], [true, false], [false, true], [true, true]]) {
            render({
                ...settings(0),
                renderMilestoneStartDateAtDayWidth: showDate ? 0 : 30,
                renderMilestoneStartTimeAtDayWidth: showTime ? 0 : 30,
            });
            const label = milestoneLabel();
            assertEqual(features.renderMilestoneStartDateAtDayWidth(), showDate,
                `${type} milestone start-date visibility for label ${JSON.stringify(label)}`);
            assertEqual(features.renderMilestoneStartTimeAtDayWidth(), showTime,
                `${type} milestone start-time visibility for label ${JSON.stringify(label)}`);
            assertCondition(label.startsWith('Milestone'), `${type} milestone name`, 'starts with "Milestone"', label);
            const hasEmptySuffix = label.includes('(') && !showDate && !showTime;
            assertCondition(!hasEmptySuffix, `${type} milestone label has no empty date/time suffix`,
                'no empty suffix', label);
        }

        render(settings(0));
        const detailedBody = group(taskId).querySelectorAll('.task-body');
        assertEqual(detailedBody.length, 2, `${type} working-day segment count`);
        const firstSegmentEnd = Number(detailedBody[0].getAttribute('x')) + Number(detailedBody[0].getAttribute('width'));
        const secondSegmentStart = Number(detailedBody[1].getAttribute('x'));
        assertCondition(secondSegmentStart > firstSegmentEnd, `${type} non-working-day gap`,
            `greater than ${firstSegmentEnd}`, secondSegmentStart);
        assertPresent(group(taskId).querySelector('.task-border[stroke-dasharray]'),
            `${type} dotted off-day border`);
        const chartHeight = document.querySelector('#chart svg').getAttribute('height');

        render({...settings(0), renderTaskNonWorkingDaysAtDayWidth: 30});
        const body = group(taskId).querySelectorAll('.task-body');
        assertEqual(body.length, 1, `${type} continuous task-body segment count`);
        assertEqual(Number(body[0].getAttribute('height')), 12, `${type} continuous task-body height`);
        const taskTitle = body[0].querySelector('title');
        assertPresent(taskTitle, `${type} task tooltip element`);
        const taskTooltip = taskTitle.textContent;
        assertCondition(taskTooltip.includes('Alpha'), `${type} task tooltip includes task name`,
            'contains "Alpha"', taskTooltip);
        if (type === 'combined')
            assertPresent(group(taskId).querySelector('.task-progress-bar'), `${type} progress bar`);
        else
            assertAbsent(group(taskId).querySelector('.task-progress-bar'), `${type} progress bar`);
        assertAbsent(group(shortTaskId).querySelector('.task-progress-bar'), `${type} zero-progress task bar`);
        assertAbsent(group(taskId).querySelector('rect[fill="#000000"]'), `${type} hidden progress marker`);
        assertEqual(group(taskId).querySelectorAll('.task-border').length, 4, `${type} solid task-border count`);
        assertAbsent(group(taskId).querySelector('.task-border[stroke-dasharray]'), `${type} dotted task border`);
        const taskLabels = labels(taskId);
        assertCondition(taskLabels.includes('Alice') && taskLabels.some(label => label.includes('Alpha')),
            `${type} task labels`, 'includes "Alice" and a label containing "Alpha"', taskLabels);
        const storyLabels = labels(storyId);
        assertCondition(storyLabels.includes('Story'), `${type} story label`, 'includes "Story"', storyLabels);
        assertPresent(dependency(milestoneId, storyId), `${type} story dependency connector`);
        assertEqual(document.querySelector('#chart svg').getAttribute('height'), chartHeight,
            `${type} chart height after task rendering`);

        render({...settings(0), renderTaskNonWorkingDaysAtDayWidth: 30}, 2);
        const shortTaskBody = group(shortTaskId).querySelector('.task-body');
        assertPresent(shortTaskBody, `${type} short task body`);
        const shortTaskBodyWidth = Number(shortTaskBody.getAttribute('width'));
        assertCondition(shortTaskBodyWidth > 0, `${type} short task body width`, 'greater than 0', shortTaskBodyWidth);
        assertEqual(Number(shortTaskBody.getAttribute('height')), 12, `${type} short task-body height`);
        render({...settings(0), renderDependenciesAtDayWidth: 30});
        assertEqual(incomingDependencies(storyId).length, 0, `${type} hidden story dependency`);

        verifyRelations(type, data, render);

        render({...settings(0), renderTaskProgressTextAtDayWidth: 21}, 20, 1);
        assertEqual(features.renderTaskProgressTextAtDayWidth(), false, `${type} progress text visibility at initial zoom`);
        document.querySelector('.gantt-inspection-viewport').dispatchEvent(new WheelEvent('wheel', {
            bubbles: true, cancelable: true, altKey: true, deltaY: -1, clientX: 400,
        }));
        await waitFor(features.renderTaskProgressTextAtDayWidth, `${type}: full-chart zoom`);

        render({...settings(0), renderTaskProgressTextAtDayWidth: 21}, 20, 1);
        assertEqual(features.renderTaskProgressTextAtDayWidth(), false, `${type} progress text visibility before day-width zoom`);
        document.querySelector('.gantt-inspection-viewport').dispatchEvent(new WheelEvent('wheel', {
            bubbles: true, cancelable: true, shiftKey: true, deltaY: -1, clientX: 400,
        }));
        await waitFor(features.renderTaskProgressTextAtDayWidth, `${type}: day-width zoom`);

        render({...settings(0), renderTaskProgressTextAtDayWidth: 21}, 20, 1);
        const open = window.open;
        let popup = null;
        window.open = function (...args) {
            popup = open.apply(window, args);
            return popup;
        };
        try {
            window.detachKassandraChart('chart');
            assertPresent(popup, `${type} detached-chart popup`);
            await waitFor(() => popup.document.querySelector('.task-body'), `${type}: detached chart did not render`);
            assertAbsent(popup.document.querySelector('.task-progress-text'), `${type} detached-chart progress text before zoom`);
            popup.document.querySelector('.gantt-inspection-viewport').dispatchEvent(new popup.WheelEvent('wheel', {
                bubbles: true, cancelable: true, altKey: true, deltaY: -1, clientX: 400,
            }));
            await waitFor(() => popup.document.querySelector('.task-progress-text'), `${type}: detached zoom`);
        } finally {
            window.open = open;
            window.disposeKassandraChart('chart');
        }
    }
}

/** Verifies read-only task inspection without assumptions about progress-bar rendering. */
export async function verifyGanttInspection(gantt, combined) {
    await document.fonts.ready;
    for (const [type, source, bundleUrl, mountName] of [
        ['gantt', gantt, '/js/generated/gantt/gantt-bundle.js', 'mountGanttChart'],
        ['combined', combined, '/js/generated/burndown/gantt-burndown-bundle.js', 'mountGanttBurndownChart'],
    ]) {
        await import(bundleUrl);
        const data = structuredClone(source);
        const render = (thresholds, dayWidth = 20, visualScale = 1) => {
            data.meta.ganttZoom = thresholds;
            state(dayWidth, visualScale);
            window[mountName]('chart', data, 'Task inspection test');
        };
        try {
            await verifyTaskInspection(type, data, render);
            const open = window.open;
            let popup = null;
            window.open = function (...args) {
                popup = open.apply(window, args);
                return popup;
            };
            try {
                window.detachKassandraChart('chart');
                await waitFor(() => popup?.document.querySelector('.task-body'), `${type} detached inspection`);
                const task = popup.document.querySelector(`[data-task-id="${storyId}"]`);
                task.focus();
                task.dispatchEvent(new popup.KeyboardEvent('keydown', {key: 'Enter', bubbles: true, cancelable: true}));
                await waitFor(() => popup.document.querySelector('.gantt-task.is-selected'), `${type} detached task selected`);
                assertEqual(popup.document.querySelector('.gantt-task-details').hidden, false, `${type} detached inspector visible`);
                assertEqual(popup.document.querySelectorAll('.task-dependency.is-emphasized').length, 1, `${type} detached direct relation`);
                popup.document.getElementById('return-chart').click();
                await waitFor(() => document.querySelector('.task-body'), `${type} return to embedded chart`);
                assertEqual(document.querySelector('.gantt-task-details').hidden, true, `${type} returned instance starts unselected`);
                await selectTask(milestoneId);
            } finally {
                window.open = open;
            }
        } finally {
            window.disposeKassandraChart('chart');
        }
    }
}

            /** Exercises actual daily burn-down inspection using known stacked values. */
            export async function verifyBurndownPoints(gantt, source) {
                await import('/js/generated/burndown/gantt-burndown-bundle.js');
                const data = structuredClone(source);
                data.meta.maxWorkedSeconds = 40500;
                data.meta.estimatedBestWorkSeconds = 40500;
                data.authors = [
                    {userId: 'alice', userName: 'Alice', color: 0xff336699, totalWorkedSeconds: 18000, totalRemainingSeconds: 9000,
                        workPerDay: [0, 3600, 3600, 0, 3600, 3600, 3600, ...Array(20).fill(0)], tooltipPerDay: []},
                    {userId: 'bob', userName: 'Bob', color: 0xffaa6633, totalWorkedSeconds: 7200, totalRemainingSeconds: 6300,
                        workPerDay: [0, 1800, 0, 1800, 0, 1800, 1800, ...Array(20).fill(0)], tooltipPerDay: []},
                    {userId: 'carol', userName: 'Carol', color: 0xff448855, totalWorkedSeconds: 3600, totalRemainingSeconds: 0,
                        workPerDay: [0, 3600, ...Array(25).fill(0)], tooltipPerDay: []},
                ];
                data.replayTasks = data.authors.map((author, index) => ({
                    taskId: [taskId, shortTaskId, storyId][index],
                    userId: author.userId,
                    impactOnCost: true,
                    initialEstimateSeconds: author.totalWorkedSeconds + author.totalRemainingSeconds,
                    worklogs: [{start: '2025-06-06T08:00:00', timeSpentSeconds: author.totalWorkedSeconds,
                        remainingEstimateSeconds: author.totalRemainingSeconds}],
                }));
                const mount = (dayWidth = 40, visualScale = 1) => {
                    state(dayWidth, visualScale);
                    document.getElementById('chart').style.width = '1600px';
                    window.mountGanttBurndownChart('chart', data, 'Burn-down inspection');
                };
                const hover = point => {
                    const svg = point.ownerSVGElement;
                    const screen = svg.createSVGPoint();
                    screen.x = Number(point.getAttribute('cx'));
                    screen.y = Number(point.getAttribute('cy'));
                    const location = screen.matrixTransform(point.getScreenCTM());
                    document.querySelector('.gantt-inspection-viewport').dispatchEvent(new PointerEvent('pointermove', {
                        bubbles: true, clientX: location.x, clientY: location.y, pointerType: 'mouse',
                    }));
                };
                mount();
                const point = document.querySelector(".burndown-actual-point[data-date='2025-06-01'][data-author='Alice']");
                const upper = document.querySelector(".burndown-actual-point[data-date='2025-06-01'][data-author='Bob']");
                const overlapping = document.querySelector(".burndown-actual-point[data-date='2025-06-01'][data-author='Carol']");
                assertPresent(point, 'first actual end-of-day point');
                assertPresent(upper, 'upper author point');
                assertPresent(overlapping, 'overlapping author point');
                assertEqual(point.dataset.seconds, '35100', 'stacked actual remaining');
                assertEqual(point.dataset.authorSeconds, '23400', 'lower author remaining');
                assertEqual(upper.dataset.authorSeconds, '11700', 'upper author remaining');
                assertEqual(overlapping.dataset.authorSeconds, '0', 'zero author remaining');
                assertCondition(Number(point.getAttribute('cy')) > Number(upper.getAttribute('cy')),
                    'author boundaries follow the stack', 'lower boundary below upper boundary', point.getAttribute('cy'));
                assertCondition(Math.abs(Number(overlapping.getAttribute('cy')) - Number(upper.getAttribute('cy'))) < 1e-7,
                    'zero-height boundaries overlap', upper.getAttribute('cy'), overlapping.getAttribute('cy'));
                assertEqual(document.querySelectorAll('.burndown-actual-point[data-date="2025-06-07"]').length, 0,
                    'future actual points excluded');
                assertEqual(document.querySelectorAll('[data-tooltip-html]').length, 0, 'old worklog tooltip removed');
                const svg = document.querySelector('svg');
                hover(point);
                const readout = document.querySelector('.burndown-point-readout');
                assertEqual(readout.style.display, 'block', 'point hover shows readout');
                assertCondition(readout.textContent.includes('2025-06-01 (end of day)')
                    && readout.textContent.includes('Total remaining: 1d 02h 15m')
                    && readout.textContent.includes('Alice: 6h 30m')
                    && !readout.textContent.includes('Bob:'), 'lower boundary readout', 'total and Alice', readout.textContent);
                assertEqual(document.querySelector('.burndown-point-guide').getAttribute('display'), null, 'vertical guide shown');
                assertEqual(document.querySelector('.burndown-point-marker').getAttribute('display'), null, 'active marker shown');
                assertEqual(document.querySelector('.burndown-point-marker').getAttribute('fill'), 'red', 'active marker is red');
                assertEqual(document.querySelector('.burndown-point-marker').getAttribute('r'), '6', 'active marker diameter is 12');
                hover(upper);
                assertCondition(readout.textContent.includes('Total remaining: 1d 02h 15m')
                    && readout.textContent.includes('Bob: 3h 15m')
                    && readout.textContent.includes('Carol: 0m')
                    && !readout.textContent.includes('Alice:'), 'overlapping boundary readout', 'total, Bob and Carol', readout.textContent);
                assertCondition(Math.abs(Number(document.querySelector('.burndown-point-marker').getAttribute('cy'))
                    - Number(upper.getAttribute('cy'))) < 1e-7, 'marker follows selected boundary',
                    upper.getAttribute('cy'), document.querySelector('.burndown-point-marker').getAttribute('cy'));
                assertEqual(document.querySelector('svg'), svg, 'hover does not redraw SVG');
                document.querySelector('.gantt-inspection-viewport').dispatchEvent(new PointerEvent('pointermove', {
                    bubbles: true, clientX: 0, clientY: 0, pointerType: 'mouse',
                }));
                assertEqual(readout.style.display, 'none', 'moving away clears readout');
                hover(point);
                document.querySelector('.gantt-inspection-viewport').dispatchEvent(new PointerEvent('pointermove', {
                    bubbles: true, buttons: 1, pointerId: 99, pointerType: 'mouse', isPrimary: true,
                }));
                assertEqual(readout.style.display, 'none', 'drag start clears readout');
                window.disposeKassandraChart('chart');
                assertEqual(document.querySelector('.burndown-point-readout'), null, 'destroy removes readout');
                mount(20, 1.4);
                const zoomedPoint = document.querySelector(".burndown-actual-point[data-date='2025-06-01'][data-author='Alice']");
                assertPresent(zoomedPoint, 'author point after zoom');
                hover(zoomedPoint);
                assertCondition(document.querySelector('.burndown-point-readout').textContent.includes('Alice: 6h 30m'),
                    'author inspection after zoom', 'Alice: 6h 30m', document.querySelector('.burndown-point-readout').textContent);
                window.disposeKassandraChart('chart');
                data.authors[2].workPerDay[1] = 3599;
                mount();
                hover(document.querySelector(".burndown-actual-point[data-date='2025-06-01'][data-author='Bob']"));
                assertCondition(document.querySelector('.burndown-point-readout').textContent.includes('Carol: 0m'),
                    'subpixel-separated authors remain inspectable', 'Carol: 0m', document.querySelector('.burndown-point-readout').textContent);
                window.disposeKassandraChart('chart');
                data.authors[2].workPerDay[1] = 3600;
                data.authors[1].workPerDay[1] = undefined;
                mount();
                assertEqual(document.querySelectorAll(".burndown-actual-point[data-date='2025-06-01']").length, 0,
                    'incomplete day has no misleading total or author points');
                window.disposeKassandraChart('chart');
                data.authors[1].workPerDay[1] = 1800;
                mount();
                const slider = () => document.querySelector('[data-replay-slider]');
                assertPresent(slider(), 'replay slider visible with weekdays');
                const endDay = Number(slider().getAttribute('aria-valuemax'));
                assertCondition(endDay >= 2, 'replay slider has several days', 'at least two', endDay);
                window.disposeKassandraChart('chart');

                const normalAuthors = data.authors;
                const normalReplayTasks = data.replayTasks;
                const normalGuideWithoutBuffer = data.ganttGuideWithoutBuffer;
                const normalGuideWithBuffer = data.ganttGuideWithBuffer;
                data.ganttGuideWithoutBuffer = null;
                data.ganttGuideWithBuffer = null;
                data.authors = [{
                    userId: 'replay', userName: 'Replay', color: 0xff336699,
                    totalWorkedSeconds: 36000, totalRemainingSeconds: 9000,
                    workPerDay: [0, 27000, 0, 9000, ...Array(24).fill(0)], tooltipPerDay: [],
                }];
                data.replayTasks = [{
                    taskId, userId: 'replay', impactOnCost: true, initialEstimateSeconds: 54000,
                    worklogs: [
                        {authorId: 'replay', start: '2025-06-01T08:00:00', timeSpentSeconds: 27000, remainingEstimateSeconds: 54000},
                        {authorId: 'replay', start: '2025-06-03T08:00:00', timeSpentSeconds: 9000, remainingEstimateSeconds: 9000},
                        {authorId: 'replay', start: '2025-06-03T08:00:00', timeSpentSeconds: 0, remainingEstimateSeconds: 1000},
                    ],
                }];
                const replayWorklogs = data.replayTasks[0].worklogs;
                data.replayTasks[0].worklogs = [];
                data.authors[0].workPerDay.fill(0);
                mount();
                assertPresent(slider(), 'unstarted sprint renders replay slider');
                assertAbsent(document.querySelector('.burndown-actual-work'), 'unstarted sprint has no actual-work bars');
                assertAbsent(document.querySelector('.burndown-actual-point'), 'unstarted sprint has no actual points');
                assertCondition([...document.querySelectorAll('svg text')].some(element => /^\d+pd$/.test(element.textContent.trim())),
                    'unstarted sprint retains y-axis', 'person-day tick', 'no person-day tick');
                assertPresent(document.querySelector('svg line[stroke-dasharray="3"]'), 'unstarted sprint retains guide');
                window.disposeKassandraChart('chart');
                data.replayTasks[0].worklogs = replayWorklogs;
                data.authors[0].workPerDay[1] = 27000;
                data.authors[0].workPerDay[3] = 9000;
                data.replayTasks.push({
                    taskId: shortTaskId, impactOnCost: false, initialEstimateSeconds: 3600,
                    worklogs: [
                        {start: '2025-06-01T08:00:00', timeSpentSeconds: 1800, remainingEstimateSeconds: 1800},
                        {start: '2025-06-02T08:00:00', timeSpentSeconds: 0, remainingEstimateSeconds: 900},
                        {start: '2025-06-03T08:00:00', timeSpentSeconds: 1800, remainingEstimateSeconds: 0},
                    ],
                });
                mount();
                const progressText = () => group(taskId).querySelector('.task-progress-text')?.textContent;
                const progressBar = () => group(taskId).querySelector('.task-progress-bar');
                const detailsProgress = () => [...document.querySelectorAll('.gantt-task-details dt')]
                    .find(term => term.textContent === 'Progress')?.nextElementSibling?.textContent;
                assertEqual(progressText(), '80%', 'initial Gantt progress uses worklogs through current day');
                const completedOtherWidth = Number(group(shortTaskId).querySelector('.task-progress-bar').getAttribute('width'));
                const finalBarWidth = Number(progressBar().getAttribute('width'));
                await selectTask(taskId);
                assertEqual(detailsProgress(), '80%', 'selected task displays replay progress');
                slider().dispatchEvent(new KeyboardEvent('keydown', {key: 'Home', bubbles: true, cancelable: true}));
                await waitFor(() => Number(slider()?.getAttribute('aria-valuenow')) === -1, 'replay selects day before first milestone');
                assertEqual(progressText(), undefined, 'pre-work Gantt progress text is hidden');
                assertAbsent(progressBar(), 'pre-work Gantt progress bar is hidden');
                assertAbsent(group(shortTaskId).querySelector('.task-progress-bar'), 'pre-work non-cost task has no progress bar');
                assertEqual(detailsProgress(), '0%', 'selected task details update to pre-work progress');
                assertEqual(slider().getAttribute('aria-valuetext'), 'Saturday 31 May 2025', 'pre-work replay date');
                assertAbsent(document.querySelector('.burndown-actual-work'), 'pre-work replay has no actual-work bars');
                assertAbsent(document.querySelector(".burndown-actual-point[data-date='2025-05-31']"),
                    'pre-work replay has no actual point');
                assertPresent(document.querySelector('svg line[stroke-dasharray="3"]'), 'pre-work replay retains guide');
                assertAbsent(document.querySelector(".burndown-actual-point[data-date='2025-06-01']"),
                    'pre-work replay excludes first-day work');
                assertCondition(![...document.querySelectorAll('svg title')]
                    .some(title => title.textContent.startsWith('R = Release (Estimated release date)')),
                    'no release forecast without work', 'no release milestone', 'release milestone');
                slider().dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowRight', bubbles: true, cancelable: true}));
                await waitFor(() => Number(slider()?.getAttribute('aria-valuenow')) === 0, 'replay selects first day');
                const firstPoint = document.querySelector(".burndown-actual-point[data-date='2025-06-01']");
                assertPresent(firstPoint, 'first replay day actual point');
                assertEqual(progressText(), '33%', 'first-day Gantt progress uses worked and remaining');
                assertEqual(detailsProgress(), '33%', 'selected task details update to first-day progress');
                assertCondition(Number(progressBar().getAttribute('width')) < finalBarWidth,
                    'first-day progress bar is shorter than final bar', `less than ${finalBarWidth}`, progressBar().getAttribute('width'));
                assertCondition(Math.abs(Number(group(shortTaskId).querySelector('.task-progress-bar').getAttribute('width'))
                    - completedOtherWidth / 2) < 1e-7, 'non-cost task progress is replayed independently',
                    completedOtherWidth / 2, group(shortTaskId).querySelector('.task-progress-bar').getAttribute('width'));
                assertCondition(group(taskId).querySelector('.task-body title').textContent.includes('Progress: 33%'),
                    'task tooltip uses replay progress', 'Progress: 33%', group(taskId).querySelector('.task-body title').textContent);
                assertPresent(document.querySelector('.burndown-actual-work polygon'), 'first worklog restores actual-work bars');
                assertEqual(firstPoint.dataset.seconds, '54000', 'first replay day uses its remaining estimate');
                assertAbsent(document.querySelector(".burndown-actual-point[data-date='2025-06-02']"),
                    'future points excluded from replay');
                const maxTick = () => Math.max(...[...document.querySelectorAll('svg text')]
                    .map(element => element.textContent.trim()).filter(text => /^\d+pd$/.test(text))
                    .map(text => Number.parseInt(text, 10)));
                const firstMaxTick = maxTick();
                const releaseX = () => [...document.querySelectorAll('svg title')]
                    .filter(title => title.textContent.startsWith('R = Release (Estimated release date)'))
                    .map(title => title.parentElement?.getAttribute('x')).join(',');
                const firstRelease = releaseX();
                for (let day = 1; day <= 2; day++) {
                    slider().dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowRight', bubbles: true, cancelable: true}));
                    await waitFor(() => Number(slider()?.getAttribute('aria-valuenow')) === day, 'replay advances one day');
                    if (day === 1) {
                        assertEqual(document.querySelector(".burndown-actual-point[data-date='2025-06-02']").dataset.seconds,
                            '54000', 'replay retains the latest estimate on days without worklogs');
                        assertCondition(Math.abs(Number(group(shortTaskId).querySelector('.task-progress-bar').getAttribute('width'))
                            - completedOtherWidth * 2 / 3) < 1e-7, 'estimate-only worklog updates non-cost task progress',
                            completedOtherWidth * 2 / 3, group(shortTaskId).querySelector('.task-progress-bar').getAttribute('width'));
                    }
                }
                assertEqual(document.querySelector(".burndown-actual-point[data-date='2025-06-03']").dataset.seconds,
                    '9000', 'replay uses the latest task estimate');
                assertEqual(progressText(), '80%', 'Gantt progress reflects latest estimate');
                assertEqual(detailsProgress(), '80%', 'selected task details follow latest estimate');
                assertEqual(Number(progressBar().getAttribute('width')), finalBarWidth, 'progress bar returns to final width');
                assertCondition(maxTick() < firstMaxTick, 'y-axis uses selected-day worked plus remaining', firstMaxTick, maxTick());
                assertCondition(releaseX() !== firstRelease, 'release forecast changes with replay', firstRelease, releaseX());
                slider().dispatchEvent(new KeyboardEvent('keydown', {key: 'Home', bubbles: true, cancelable: true}));
                await waitFor(() => Number(slider()?.getAttribute('aria-valuenow')) === -1, 'replay returns to pre-work day');
                assertAbsent(document.querySelector('.burndown-actual-work'), 'returning to pre-work removes actual-work bars');
                assertAbsent(document.querySelector('.burndown-actual-point'), 'returning to pre-work removes actual points');
                assertEqual(detailsProgress(), '0%', 'selected task details return to pre-work progress');
                window.disposeKassandraChart('chart');

                data.replayTasks[0].worklogs.unshift(
                    {authorId: 'replay', start: '2025-05-31T08:00:00', timeSpentSeconds: 0, remainingEstimateSeconds: 54000});
                mount();
                slider().dispatchEvent(new KeyboardEvent('keydown', {key: 'Home', bubbles: true, cancelable: true}));
                await waitFor(() => Number(slider()?.getAttribute('aria-valuenow')) === -1, 'replay selects zero-duration worklog');
                assertPresent(document.querySelector(".burndown-actual-point[data-date='2025-05-31']"),
                    'zero-duration worklog still produces an actual point');
                window.disposeKassandraChart('chart');
                data.replayTasks[0].worklogs.shift();

                data.authors.push({
                    userId: 'other', userName: 'Other', color: 0xffaa6633,
                    totalWorkedSeconds: 9000, totalRemainingSeconds: 0,
                    workPerDay: [0, 9000, ...Array(26).fill(0)], tooltipPerDay: [],
                });
                data.replayTasks[0].worklogs.unshift(
                    {authorId: 'other', start: '2025-05-31T08:00:00', timeSpentSeconds: 9000, remainingEstimateSeconds: 36000});
                mount();
                slider().dispatchEvent(new KeyboardEvent('keydown', {key: 'Home', bubbles: true, cancelable: true}));
                await waitFor(() => Number(slider()?.getAttribute('aria-valuenow')) === -1, 'replay selects earlier worklog');
                assertEqual(document.querySelector(".burndown-actual-point[data-date='2025-05-31']").dataset.seconds,
                    '36000', 'pre-milestone replay includes earlier work and its remaining estimate');
                window.disposeKassandraChart('chart');

                const normalPreRun = data.meta.preRun;
                data.meta.preRun = 0;
                mount();
                slider().dispatchEvent(new KeyboardEvent('keydown', {key: 'Home', bubbles: true, cancelable: true}));
                await waitFor(() => Number(slider()?.getAttribute('aria-valuenow')) === -1, 'pre-work replay without padding');
                const replayClip = document.querySelector(slider().getAttribute('clip-path').slice(4, -1) + ' rect');
                assertCondition(Number(slider().querySelector('circle').getAttribute('cx'))
                    >= Number(replayClip.getAttribute('x')) + 6, 'pre-work slider knob is visible without padding',
                    'inside the plot', slider().querySelector('circle').getAttribute('cx'));
                window.disposeKassandraChart('chart');
                data.meta.preRun = normalPreRun;
                mount(10);
                assertAbsent(slider(), 'replay slider hidden without weekdays');
                window.disposeKassandraChart('chart');
                data.authors = normalAuthors;
                data.replayTasks = normalReplayTasks;
                data.ganttGuideWithoutBuffer = normalGuideWithoutBuffer;
                data.ganttGuideWithBuffer = normalGuideWithBuffer;
                mount();
            }
