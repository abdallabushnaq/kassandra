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

import {build} from 'esbuild';
import {transformAsync} from '@babel/core';
import istanbul from 'babel-plugin-istanbul';
import {mkdir, readFile, readdir, rm, writeFile} from 'node:fs/promises';
import path from 'node:path';

const output = path.resolve('target/classes/META-INF/resources/js/generated');
const raw = path.resolve('target/frontend-coverage/raw');
const runtime = await readFile('scripts/frontend-coverage-runtime.js', 'utf8');
const baseline = {};

// Start with a clean coverage run and retain source maps from generated JavaScript back to TypeScript.
await rm(path.dirname(raw), {recursive: true, force: true});
await rm(output, {recursive: true, force: true});
await build({
    entryPoints: ['src/main/frontend/js/**/*.ts'],
    outdir: output,
    format: 'esm',
    target: 'es2020',
    sourcemap: 'inline',
});

async function instrument(directory) {
    for (const entry of await readdir(directory, {withFileTypes: true})) {
        const file = path.join(directory, entry.name);
        if (entry.isDirectory()) {
            await instrument(file);
        } else if (entry.isFile() && file.endsWith('.js')) {
            // Instrument each module separately so its counters and source map remain identifiable after bundling.
            const result = await transformAsync(await readFile(file, 'utf8'), {
                filename: file,
                babelrc: false,
                configFile: false,
                sourceMaps: 'inline',
                plugins: [[istanbul, {
                    include: ['**/*.js'],
                    exclude: [],
                    onCover: (name, coverage) => { baseline[name] = coverage; },
                }]],
            });
            if (!baseline[file])
                throw new Error(`No coverage instrumentation generated for ${file}`);
            await writeFile(file, runtime + '\n' + result.code);
        }
    }
}

await instrument(output);
// Recreate the production-style entry bundles from the instrumented modules used by the browser tests.
const bundles = await build({
    entryPoints: [
        path.join(output, 'sprints-overview/sprints-overview-bundle.js'),
        path.join(output, 'gantt/gantt-bundle.js'),
        path.join(output, 'burndown/gantt-burndown-bundle.js'),
    ],
    outbase: output,
    outdir: output,
    bundle: true,
    format: 'iife',
    platform: 'browser',
    target: 'es2020',
    minify: true,
    write: false,
});
for (const bundle of bundles.outputFiles)
    await writeFile(bundle.path, bundle.contents);
// Zero-count metadata also includes executable modules that no browser loaded.
await mkdir(raw, {recursive: true});
await writeFile(path.join(raw, 'baseline.json'), JSON.stringify(baseline));
