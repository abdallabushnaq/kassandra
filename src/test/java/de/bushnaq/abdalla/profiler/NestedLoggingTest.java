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

/**
 * calc(200)
 * net(300)
 * sql(500)
 * net(200)
 * smb(300)
 * calc(200)
 * <p>
 * expected result
 * net = 500ms
 * calc = 400ms
 * sql = 500ms
 * smb = 300ms
 *
 * @author abdalla
 */

/**
 * Profiler should support logging intermediate results
 *
 * @author abdalla
 */
@Tag("UnitTest")
class NestedLoggingTest extends BaseUtils {

    private void method1() throws Exception {
        try (Profiler timeKeeping = new Profiler("method1", SampleType.CPU)) {
            try (Profiler pc = new Profiler(SampleType.TCP)) {
                mockupNetAccess(300);
                sqlMethod();
                mockupNetAccess(200);
            }
            Profiler.log("NestedLoggingTest");
            test(new TestResult[]{//
                    new TestResult(SampleType.JPA, 500),//
                    new TestResult(SampleType.TCP, 500)//
            });

            mockupCpuAccess(100);
            try (Profiler pc = new Profiler(SampleType.SMB)) {
                mockupSmbAccess(300);
            }
            test(new TestResult[]{//
                    new TestResult(SampleType.SMB, 300)//
            });
        }
        test(new TestResult[]{//
                new TestResult(SampleType.JPA, 500),//
                new TestResult(SampleType.CPU, 100),//
                new TestResult(SampleType.TCP, 500),//
                new TestResult(SampleType.SMB, 300)//
        });
    }

    private void method2() throws Exception {
        try (Profiler timeKeeping = new Profiler("method2", SampleType.CPU)) {
            try (Profiler pc = new Profiler(SampleType.TCP)) {
                mockupNetAccess(300);
                sqlMethod();
                mockupNetAccess(200);
            }
            mockupCpuAccess(100);
            try (Profiler pc = new Profiler(SampleType.SMB)) {
                mockupSmbAccess(300);
            }
        }
    }

    @Test
    void nestedLogsTest() throws Exception {
        //        Profiler.enableTraceLogger = true;
        try (Profiler timeKeeping = new Profiler(this.getClass().getSimpleName(), SampleType.CPU)) {
            mockupCpuAccess(200);
            method1();
            mockupCpuAccess(200);
            method2();
            mockupCpuAccess(100);
        }
    }

    private void sqlMethod() throws Exception {
        try (Profiler pc = new Profiler(SampleType.JPA)) {
            mockupSqlAccess(500);
        }
    }

}
