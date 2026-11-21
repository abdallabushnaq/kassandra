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

// Every instrumented module includes this hook, but each browser window installs it only once.
if (!window.__kassandraFrontendCoverage) {
    window.__kassandraFrontendCoverage = {
        flush() {
            const key = '__kassandra_frontend_coverage__';
            const saved = JSON.parse(localStorage.getItem(key) || '{}');
            // Merge statement, function and branch counters into storage shared by same-origin windows.
            for (const [file, coverage] of Object.entries(window.__coverage__ || {})) {
                const counters = saved[file] ||= {s: {}, f: {}, b: {}};
                for (const type of ['s', 'f', 'b']) {
                    for (const [id, value] of Object.entries(coverage[type])) {
                        if (Array.isArray(value)) {
                            const previous = counters[type][id] || value.map(() => 0);
                            counters[type][id] = value.map((count, i) => previous[i] + count);
                        } else {
                            counters[type][id] = (counters[type][id] || 0) + value;
                        }
                    }
                }
            }
            localStorage.setItem(key, JSON.stringify(saved));
            // Reset only after saving, so a later flush does not count the same executions twice.
            for (const coverage of Object.values(window.__coverage__ || {})) {
                for (const type of ['s', 'f', 'b']) {
                    for (const [id, value] of Object.entries(coverage[type])) {
                        coverage[type][id] = Array.isArray(value) ? value.map(() => 0) : 0;
                    }
                }
            }
        },
    };
    // Preserve the counters before a refresh, navigation or detached chart window closes.
    window.addEventListener('pagehide', () => window.__kassandraFrontendCoverage.flush());
}
