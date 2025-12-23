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
import de.bushnaq.abdalla.kassandra.ui.view.ActiveSprints;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Test helper class for interacting with the Product UI components.
 * <p>
 * This class provides methods to test product-related operations in the UI such as
 * creating, editing, deleting products and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 */
@Component
@Lazy
@Log4j2
public class ActiveSprintsTester extends AbstractViewTester {

    /**
     * Constructs a new ProductViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public ActiveSprintsTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    /**
     * Navigates to the UserListView.
     * <p>
     * Opens the user list URL directly and waits for the page to load
     * by checking for the presence of the page title element.
     */
    public void switchToActiveSprints() {
        seleniumHandler.click("/" + ActiveSprints.ROUTE);
        seleniumHandler.waitForUrl("http://localhost:" + port + "/ui/" + ActiveSprints.ROUTE);
    }
}
