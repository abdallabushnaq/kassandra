/*
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import test from 'node:test';
import vm from 'node:vm';

const runtime = await readFile(new URL('./frontend-coverage-runtime.js', import.meta.url), 'utf8');

function browser(storage, coverage) {
    const listeners = {};
    const window = {
        __coverage__: coverage,
        addEventListener(name, listener) { listeners[name] = listener; },
    };
    const localStorage = {
        getItem(key) { return storage.get(key) ?? null; },
        setItem(key, value) { storage.set(key, value); },
    };
    vm.runInNewContext(runtime, {window, localStorage});
    return {window, listeners};
}

test('merges page unloads and detached windows without counting flushed executions twice', () => {
    const storage = new Map();
    const first = browser(storage, {chart: {s: {0: 2}, f: {0: 1}, b: {0: [1, 0]}}});
    first.listeners.pagehide();
    first.listeners.pagehide();
    const second = browser(storage, {chart: {s: {0: 3}, f: {0: 2}, b: {0: [0, 2]}}});
    const popup = browser(storage, {chart: {s: {0: 4}, f: {0: 1}, b: {0: [1, 1]}}});
    popup.listeners.pagehide();
    second.window.__kassandraFrontendCoverage.flush();
    assert.deepEqual(JSON.parse(storage.get('__kassandra_frontend_coverage__')), {
        chart: {s: {0: 9}, f: {0: 4}, b: {0: [2, 3]}},
    });
});

test('does not discard counters when storage fails', () => {
    const storage = {get() { return null; }, set() { throw new Error('storage full'); }};
    const page = browser(storage, {chart: {s: {0: 2}, f: {}, b: {}}});
    assert.throws(() => page.window.__kassandraFrontendCoverage.flush(), /storage full/);
    assert.equal(page.window.__coverage__.chart.s[0], 2);
});
