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

import {readFile, readdir, writeFile} from 'node:fs/promises';
import {spawnSync} from 'node:child_process';
import path from 'node:path';

const root = path.resolve('target/frontend-coverage');
const raw = path.join(root, 'raw');
// The baseline supplies zero counts and source maps for all instrumented modules, including unvisited ones.
const baseline = JSON.parse(await readFile(path.join(raw, 'baseline.json'), 'utf8'));
const snapshots = (await readdir(root)).filter(name => name.startsWith('browser-') && name.endsWith('.json'));
if (snapshots.length === 0)
    throw new Error('No browser coverage snapshots were collected');

let hasCoverage = false;
for (const snapshot of snapshots) {
    const counters = JSON.parse(await readFile(path.join(root, snapshot), 'utf8'));
    const coverage = {};
    // Reattach the instrumentation metadata omitted from the compact browser snapshots.
    for (const [file, counts] of Object.entries(counters)) {
        if (!baseline[file])
            throw new Error(`No coverage metadata for ${file}`);
        coverage[file] = {...baseline[file], ...counts};
        hasCoverage ||= Object.values(counts.s).some(count => count > 0);
    }
    if (!hasCoverage)
        throw new Error('Browser coverage snapshots contain no executed statements');
    await writeFile(path.join(raw, snapshot), JSON.stringify(coverage));
}

// Merge the baseline and snapshots, remap coverage to TypeScript, and write LCOV, HTML and summaries.
const result = spawnSync(process.execPath, [
    'node_modules/nyc/bin/nyc.js', 'report',
    '--temp-dir', raw,
    '--report-dir', path.join(root, 'report'),
    '--reporter', 'lcov',
    '--reporter', 'text-summary',
    '--reporter', 'json-summary',
], {stdio: 'inherit'});
if (result.error)
    throw result.error;
if (result.status !== 0)
    process.exit(result.status ?? 1);
