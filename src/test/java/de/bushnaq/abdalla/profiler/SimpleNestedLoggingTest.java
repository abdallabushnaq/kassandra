/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.profiler;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class SimpleNestedLoggingTest extends BaseUtils {

    private void method1() throws Exception {
        try (Profiler timeKeeping = new Profiler("method1", SampleType.TCP)) {
            mockupNetAccess(300);
        }
        test(new TestResult[]{new TestResult(SampleType.TCP, 300)});
    }

    @Test
    void simpleNestedLogsTest() throws Exception {
        Profiler.enableTraceLogger = true;
        try (Profiler timeKeeping = new Profiler(this.getClass().getSimpleName(), SampleType.CPU)) {
            mockupCpuAccess(200);
            method1();
            mockupCpuAccess(200);
        }
        test(new TestResult[]{new TestResult(SampleType.TCP, 300), new TestResult(SampleType.CPU, 400)});
    }

}
