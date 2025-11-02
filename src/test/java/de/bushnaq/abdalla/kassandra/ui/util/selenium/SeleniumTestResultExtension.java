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

package de.bushnaq.abdalla.kassandra.ui.util.selenium;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class SeleniumTestResultExtension implements TestWatcher {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private void deleteScreenshot(ExtensionContext context) throws IOException {
        Files.deleteIfExists(Paths.get(ScreenShotCreator.fileNameGenerator(context.getDisplayName(), context.getTestMethod().get().getName())));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        try {
            deleteScreenshot(context);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        try {
            deleteScreenshot(context);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        //do not delete
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        try {
            deleteScreenshot(context);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}