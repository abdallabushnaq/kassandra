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

package de.bushnaq.abdalla.kassandra.ui.util;

import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.util.AbstractGanttTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

public class AbstractUiTestUtil extends AbstractGanttTestUtil {
    @Autowired
    private HumanizedSeleniumHandler seleniumHandler;

    {
        System.setProperty("java.awt.headless", "false");
    }


//    @BeforeEach
//    public void setupTest(TestInfo testInfo) {
//        seleniumHandler.startRecording(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
//    }

    @AfterEach
    public void tearDownTest() throws InterruptedException {
        if (seleniumHandler.isRecording())
            seleniumHandler.wait(1000); // Wait for any pending actions to complete for the recording
        seleniumHandler.destroy();
    }


}
