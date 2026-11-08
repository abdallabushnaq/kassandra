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
    renderDependenciesAtDayWidth: () => !!group(milestoneId).querySelector('.task-dependency'),
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
    return {
        x: side === 'right' ? x + width - (body ? 0 : 1) : x,
        y: body ? y + Number(rect.getAttribute('height')) / 2 : y + 8,
    };
}

function assertRelation(type, predecessorId, successorId, color) {
    const start = taskEdge(predecessorId, 'right');
    const end = taskEdge(successorId, 'left');
    const relation = group(successorId).querySelector('.task-dependency');
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
        assertPresent(group(shortTaskId).querySelector('.task-dependency path'), `${type} partially clipped relation`);
        shortTask.start = '2024-01-03T08:00:00';
        shortTask.finish = '2024-01-03T16:30:00';
        render(thresholds);
        assertAbsent(group(shortTaskId).querySelector('.task-dependency path'), `${type} fully clipped relation`);
        task.finish = null;
        render(thresholds);
        assertAbsent(group(shortTaskId).querySelector('.task-dependency path'), `${type} unscheduled predecessor`);
    } finally {
        data.tasks = originalTasks;
        data.meta.theme.ganttTheme = originalTheme;
    }
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

        for (const [key, visible] of Object.entries(features)) {
            const configuredThreshold = source.meta.ganttZoom[key];
            for (const [threshold, expected] of [
                [configuredThreshold + 0.01, false],
                [configuredThreshold, true],
                [Math.max(0, configuredThreshold - 0.01), true],
            ]) {
                render({...settings(0), [key]: threshold}, configuredThreshold);
                assertEqual(visible(), expected, `${type} ${key} visibility at threshold ${threshold}`);
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
        const distinctFills = [...new Set([...detailedBody].map(element => element.getAttribute('fill')))];
        assertCondition(distinctFills.length > 1, `${type} non-working-day segment fills`,
            'more than 1 distinct fill', distinctFills);
        assertPresent(group(taskId).querySelector('.task-border[stroke-dasharray]'),
            `${type} dotted off-day border`);
        const chartHeight = document.querySelector('#chart svg').getAttribute('height');

        render({...settings(0), renderTaskNonWorkingDaysAtDayWidth: 30});
        const body = group(taskId).querySelectorAll('.task-body');
        assertEqual(body.length, 1, `${type} continuous task-body segment count`);
        assertEqual(Number(body[0].getAttribute('height')), 16, `${type} continuous task-body height`);
        const taskTitle = body[0].querySelector('title');
        assertPresent(taskTitle, `${type} task tooltip element`);
        const taskTooltip = taskTitle.textContent;
        assertCondition(taskTooltip.includes('Alpha'), `${type} task tooltip includes task name`,
            'contains "Alpha"', taskTooltip);
        const progress = group(taskId).querySelector('.task-progress-bar');
        assertPresent(progress, `${type} progress bar`);
        assertEqual(progress.getAttribute('fill'), '#0000ffff', `${type} default progress-bar fill`);
        assertEqual(Number(progress.getAttribute('height')), 1, `${type} progress-bar height`);
        const progressBottomGap = Number(body[0].getAttribute('y')) + Number(body[0].getAttribute('height'))
            - Number(progress.getAttribute('y')) - Number(progress.getAttribute('height'));
        assertEqual(progressBottomGap, 1, `${type} progress-bar bottom gap`);
        const expectedProgressWidth = Number(body[0].getAttribute('width')) / 2;
        assertEqual(Number(progress.getAttribute('width')), expectedProgressWidth, `${type} progress-bar width`);
        const progressTitle = progress.querySelector('title');
        assertPresent(progressTitle, `${type} progress-bar tooltip element`);
        const progressTooltip = progressTitle.textContent;
        assertCondition(progressTooltip.includes('Alpha'), `${type} progress tooltip includes task name`,
            'contains "Alpha"', progressTooltip);
        assertAbsent(group(shortTaskId).querySelector('.task-progress-bar'), `${type} zero-progress task bar`);
        assertAbsent(group(taskId).querySelector('rect[fill="#000000"]'), `${type} hidden progress marker`);
        assertEqual(group(taskId).querySelectorAll('.task-border').length, 4, `${type} solid task-border count`);
        assertAbsent(group(taskId).querySelector('.task-border[stroke-dasharray]'), `${type} dotted task border`);
        const taskLabels = labels(taskId);
        assertCondition(taskLabels.includes('Alice') && taskLabels.some(label => label.includes('Alpha')),
            `${type} task labels`, 'includes "Alice" and a label containing "Alpha"', taskLabels);
        const storyLabels = labels(storyId);
        assertCondition(storyLabels.includes('Story'), `${type} story label`, 'includes "Story"', storyLabels);
        assertPresent(group(storyId).querySelector('.task-dependency'), `${type} story dependency connector`);
        assertEqual(document.querySelector('#chart svg').getAttribute('height'), chartHeight,
            `${type} chart height after task rendering`);

        data.meta.theme.ganttTheme.progressColor = 0xff8000;
        render(settings(0));
        assertEqual(group(taskId).querySelector('.task-progress-bar').getAttribute('fill'), '#ff8000ff',
            `${type} configured progress-bar fill`);
        data.meta.theme.ganttTheme.progressColor = 0x0000ff;

        render({...settings(0), renderTaskNonWorkingDaysAtDayWidth: 30}, 2);
        const shortTaskBody = group(shortTaskId).querySelector('.task-body');
        assertPresent(shortTaskBody, `${type} short task body`);
        const shortTaskBodyWidth = Number(shortTaskBody.getAttribute('width'));
        assertCondition(shortTaskBodyWidth > 0, `${type} short task body width`, 'greater than 0', shortTaskBodyWidth);
        assertEqual(Number(shortTaskBody.getAttribute('height')), 16, `${type} short task-body height`);
        render({...settings(0), renderDependenciesAtDayWidth: 30});
        assertAbsent(group(storyId).querySelector('.task-dependency'), `${type} hidden story dependency`);

        verifyRelations(type, data, render);

        render({...settings(0), renderTaskProgressBarAtDayWidth: 21}, 20, 1);
        assertEqual(features.renderTaskProgressBarAtDayWidth(), false, `${type} progress bar visibility at initial zoom`);
        document.getElementById('chart').dispatchEvent(new WheelEvent('wheel', {
            bubbles: true, cancelable: true, altKey: true, deltaY: -1, clientX: 400,
        }));
        await waitFor(features.renderTaskProgressBarAtDayWidth, `${type}: full-chart zoom`);

        render({...settings(0), renderTaskProgressBarAtDayWidth: 21}, 20, 1);
        document.getElementById('chart').dispatchEvent(new WheelEvent('wheel', {
            bubbles: true, cancelable: true, shiftKey: true, deltaY: -1, clientX: 400,
        }));
        await waitFor(features.renderTaskProgressBarAtDayWidth, `${type}: day-width zoom`);

        render({...settings(0), renderTaskProgressBarAtDayWidth: 21}, 20, 1);
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
            assertAbsent(popup.document.querySelector('.task-progress-bar'), `${type} detached-chart progress bar before zoom`);
            popup.document.getElementById('chart').dispatchEvent(new popup.WheelEvent('wheel', {
                bubbles: true, cancelable: true, altKey: true, deltaY: -1, clientX: 400,
            }));
            await waitFor(() => popup.document.querySelector('.task-progress-bar'), `${type}: detached zoom`);
        } finally {
            window.open = open;
            window.disposeKassandraChart('chart');
        }
    }
}
