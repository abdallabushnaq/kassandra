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

function assert(condition, message) {
    if (!condition)
        throw new Error(message);
}

async function waitFor(condition, message) {
    for (let attempt = 0; attempt < 50; attempt++) {
        if (condition())
            return;
        await delay(100);
    }
    throw new Error(message);
}

function state(dayWidth, visualScale = 1) {
    localStorage.setItem('kassandra.chart.chart.view', JSON.stringify({
        dayWidth, visualScale, scrollOffset: 0, scrollYOffset: 0,
    }));
}

function group(id) {
    const element = document.querySelector(`[data-task-id="${id}"]`);
    assert(element, `missing task group ${id}`);
    return element;
}

function milestoneLabel() {
    return group(milestoneId).querySelector('.milestone-label').firstChild.textContent;
}

function labels(id) {
    return [...group(id).querySelectorAll('text')].map(element => element.firstChild.textContent);
}

const features = {
    renderTaskProgressBarAtDayWidth: () => !!group(taskId).querySelector('.task-progress-bar'),
    renderTaskProgressTextAtDayWidth: () => !!group(taskId).querySelector('.task-progress-text'),
    renderTaskNonWorkingDaysAtDayWidth: () => group(taskId).querySelectorAll('.task-body').length > 1,
    renderTaskBorderAtDayWidth: () => !!group(taskId).querySelector('.task-border'),
    renderMilestoneStartDateAtDayWidth: () => milestoneLabel().includes('2025'),
    renderMilestoneStartTimeAtDayWidth: () => milestoneLabel().includes('08:00'),
    renderMilestoneBorderAtDayWidth: () => group(milestoneId).querySelector('.milestone-body').getAttribute('stroke') !== 'none',
    renderDependenciesAtDayWidth: () => !!group(milestoneId).querySelector('.task-dependency'),
};

function settings(value) {
    return Object.fromEntries(Object.keys(features).map(key => [key, value]));
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
            assert(document.querySelector('#chart svg'), `${type} did not render`);
        };

        for (const [key, visible] of Object.entries(features)) {
            for (const [threshold, expected] of [[20.01, false], [20, true], [19.99, true]]) {
                render({...settings(0), [key]: threshold});
                assert(visible() === expected, `${type}: ${key} threshold ${threshold}`);
            }
        }

        render({...settings(0), renderTaskProgressBarAtDayWidth: 30, renderTaskProgressTextAtDayWidth: 0});
        assert(!features.renderTaskProgressBarAtDayWidth() && features.renderTaskProgressTextAtDayWidth(),
            `${type}: progress text must not depend on the bar`);

        for (const [showDate, showTime] of [[false, false], [true, false], [false, true], [true, true]]) {
            render({...settings(0),
                renderMilestoneStartDateAtDayWidth: showDate ? 0 : 30,
                renderMilestoneStartTimeAtDayWidth: showTime ? 0 : 30,
            });
            assert(features.renderMilestoneStartDateAtDayWidth() === showDate, `${type}: milestone date`);
            assert(features.renderMilestoneStartTimeAtDayWidth() === showTime, `${type}: milestone time`);
            assert(milestoneLabel().startsWith('Milestone'), `${type}: milestone name`);
            assert(showDate || showTime || !milestoneLabel().includes('('), `${type}: empty milestone suffix`);
        }

        render(settings(0));
        const detailedBody = group(taskId).querySelectorAll('.task-body');
        assert(new Set([...detailedBody].map(element => element.getAttribute('fill'))).size > 1,
            `${type}: non-working day fill`);
        assert(group(taskId).querySelector('.task-border[stroke-dasharray]'), `${type}: dotted off-day border`);
        const chartHeight = document.querySelector('#chart svg').getAttribute('height');

        render({...settings(0), renderTaskNonWorkingDaysAtDayWidth: 30});
        const body = group(taskId).querySelectorAll('.task-body');
        assert(body.length === 1 && Number(body[0].getAttribute('height')) === 18, `${type}: continuous body`);
        assert(body[0].querySelector('title').textContent.includes('Alpha'), `${type}: task tooltip`);
        assert(group(taskId).querySelectorAll('.task-border').length === 4, `${type}: solid border`);
        assert(!group(taskId).querySelector('.task-border[stroke-dasharray]'), `${type}: no dotted border`);
        assert(labels(taskId).includes('Alice') && labels(taskId).includes('Alpha'), `${type}: task labels`);
        assert(labels(storyId).includes('Story'), `${type}: story label`);
        assert(group(storyId).querySelector('.task-dependency'), `${type}: story dependency`);
        assert(document.querySelector('#chart svg').getAttribute('height') === chartHeight, `${type}: unchanged chart height`);

        render({...settings(0), renderTaskNonWorkingDaysAtDayWidth: 30}, 2);
        assert(Number(group(shortTaskId).querySelector('.task-body').getAttribute('width')) > 0,
            `${type}: short task body`);
        assert(Number(group(shortTaskId).querySelector('.task-body').getAttribute('height')) === 18,
            `${type}: unchanged body height`);
        render({...settings(0), renderDependenciesAtDayWidth: 30});
        assert(!group(storyId).querySelector('.task-dependency'), `${type}: hidden story dependency`);

        render({...settings(0), renderTaskProgressBarAtDayWidth: 21}, 20, 1);
        assert(!features.renderTaskProgressBarAtDayWidth(), `${type}: initial zoom`);
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
            assert(popup, `${type}: popup was blocked`);
            await waitFor(() => popup.document.querySelector('.task-body'), `${type}: detached chart did not render`);
            assert(!popup.document.querySelector('.task-progress-bar'), `${type}: detached threshold`);
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
