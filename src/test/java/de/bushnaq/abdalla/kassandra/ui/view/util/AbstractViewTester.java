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

package de.bushnaq.abdalla.kassandra.ui.view.util;

import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

public abstract class AbstractViewTester {

    protected final DateTimeFormatter        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    @Getter
    protected final int                      port;
    protected final HumanizedSeleniumHandler seleniumHandler;

    public AbstractViewTester(HumanizedSeleniumHandler seleniumHandler, int port) {
        this.seleniumHandler = seleniumHandler;
        this.port            = port;
    }

    protected void closeConfirmDialog(String button, String waitForElementId) {
        seleniumHandler.wait(300);
        seleniumHandler.click(button);
        seleniumHandler.waitForElementToBeClickable(waitForElementId);
    }


}
