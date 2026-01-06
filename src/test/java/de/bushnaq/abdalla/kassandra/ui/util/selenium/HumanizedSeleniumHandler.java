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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.event.InputEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Selenium handler with human-like interactions for improved realism in UI testing and recordings.
 * Never instanciate this class directly, always use dependency injection to get a singleton instance.
 * Includes features like smooth mouse movements, element highlighting,
 * humanized typing delays, and visual overlays.
 *
 * @author Abdalla Bushnaq
 */
@Component
@Log4j2
public class HumanizedSeleniumHandler extends SeleniumHandler {
    public static final int     SHIFTY                   = 6;
    @Getter
    @Setter
    private             boolean highlightEnabled         = true;
    private final       Object  highlightLock            = new Object();// Synchronization for highlight removal
    private             Thread  highlightRemovalThread   = null;
    @Getter
    @Setter
    private static      boolean humanize                 = false;
    /**
     * -- GETTER --
     * Get the current mouse move delay multiplier.
     */
    // --- New configurable mouse movement speed controls ---
    // Multiplies per-step delays during human-like mouse movement. >1 slows down, <1 speeds up. Default 1.0 retains current behavior.
    @Getter
    private             double  mouseMoveDelayMultiplier = 1.0;
    /**
     * -- GETTER --
     * Get the current mouse move steps multiplier.
     */
    // Multiplies the number of movement steps. >1 adds more steps (smoother and slower overall), <1 reduces steps. Default 1.0.
    @Getter
    private             double  mouseMoveStepsMultiplier = 1.0;
    @Setter
    @Getter
    private             boolean moveMouse                = true;// Enable or disable mouse movement entirely
    private final       Random  random                   = new Random(); // For human-like randomness
    private             Robot   robot                    = null; // Lazily initialized
    private final       int     typingDelayMillis        = 50;

    /**
     * Centers the mouse cursor on the browser window.
     * This is useful before starting recordings to ensure the mouse starts
     * in a predictable position rather than wherever it was previously.
     * Only works if mouse movement is enabled and not in headless mode.
     */
    protected void centerMouseOnBrowser() {
        if (!isHumanize()) {
            return;
        }
        // Skip if we're in headless mode
        if (isSeleniumHeadless()) {
            return;
        }

        Robot robotInstance = getRobot();
        if (robotInstance == null) {
            log.trace("Robot not available, cannot center mouse");
            return;
        }

        try {
            // Get browser window position and size
            Point     windowPosition = getDriver().manage().window().getPosition();
            Dimension windowSize     = getDriver().manage().window().getSize();

            // Calculate center of browser window
            int centerX = windowPosition.getX() + (windowSize.getWidth() / 2);
            int centerY = windowPosition.getY() + (windowSize.getHeight() / 2);

            // Move mouse to center (instantly, no smooth movement)
            robotInstance.mouseMove(centerX, centerY);
            log.trace("Centered mouse on browser at ({}, {})", centerX, centerY);

        } catch (Exception e) {
            log.warn("Failed to center mouse on browser: {}", e.getMessage());
        }
    }

    public void closeMultiSelectComboBoxValue(String id) {
//        if (!isHumanize()) {
//            super.setComboBoxValue(id);
//            return;
//        }
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));
        WebElement comboBoxElement = findElement(By.id(id));
        closeMultiSelectComboBoxValue(comboBoxElement);
    }

    public void closeMultiSelectComboBoxValue(WebElement comboBoxElement) {
        WebElement inputElement = comboBoxElement.findElement(By.tagName("input"));
        waitForElementToBeInteractable(inputElement.getAttribute("id"));
        try {
            // Find and click the toggle button to open the dropdown
            // A human would just click the toggle button directly (no need to click input first)
            // Vaadin combobox uses shadow DOM, so we use expandRootElementAndFindElement to access it
            try {
                // Try to find toggle button by ID first
                WebElement toggleButton = expandRootElementAndFindElement(comboBoxElement, "#toggleButton");

                if (toggleButton == null) {
                    // Fallback: try to find by part attribute
                    toggleButton = expandRootElementAndFindElement(comboBoxElement, "[part='toggle-button']");
                }

                if (toggleButton != null) {
                    moveMouseToElement(toggleButton);
                    clickElement(toggleButton);
//                    toggleButton.click();
                    log.trace("Clicked combobox toggle button via shadow DOM");
                } else {
                    // Fallback: click on the input field if toggle button not found
                    log.trace("Toggle button not found in shadow DOM, clicking input field to open dropdown");
                    inputElement.click();
                }
            } catch (Exception ex) {
                // Fallback: click on the input field if shadow DOM access fails
                log.trace("Failed to access toggle button via shadow DOM: {}, clicking input field to open dropdown", ex.getMessage());
                inputElement.click();
            }
//            waitUntil(ExpectedConditions.attributeToBe(By.cssSelector("vaadin-multi-select-combo-box-overlay"), "opened", "true"));
            // Wait for dropdown to close
            wait(200);

        } catch (Exception ex) {
            log.warn("Error during humanized combobox selection: {}. Falling back to keyboard method.", ex.getMessage());
        }
    }

    /**
     * Performs a drag-and-drop operation from the source element to the target element.
     *
     * @param sourceId
     * @param targetId
     */
    public void dragAndDrop(String sourceId, String targetId) {
        dragAndDropShift(sourceId, targetId, 0, 0);
    }

    /**
     * Performs a drag-and-drop operation from the source element to just above the target element.
     *
     * @param sourceId
     * @param targetId
     */
    public void dragAndDropAbove(String sourceId, String targetId) {
        dragAndDropShift(sourceId, targetId, 0, -SHIFTY);
    }

    /**
     * Performs a drag-and-drop operation from the source element to just below the target element.
     *
     * @param sourceId
     * @param targetId
     */
    public void dragAndDropBelow(String sourceId, String targetId) {
        dragAndDropShift(sourceId, targetId, 0, SHIFTY);
    }

    /**
     * Performs a drag-and-drop operation from the source element to the target element,
     * with optional pixel shifts to adjust the drop position.
     * <p>
     * If humanized mode is enabled and not in headless mode, uses Robot for smooth
     * mouse movement and realistic drag-and-drop simulation. Otherwise, falls back
     * to Selenium Actions for reliability.
     * </p>
     *
     * @param sourceId
     * @param targetId
     * @param shiftX
     * @param shiftY
     */
    public void dragAndDropShift(String sourceId, String targetId, int shiftX, int shiftY) {
        // Ensure both elements are ready
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id(sourceId)));
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id(targetId)));
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(sourceId)));
        waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id(targetId)));

        WebElement sourceElement = findElement(By.id(sourceId));
        WebElement targetElement = findElement(By.id(targetId));

        // Visual hint (non-blocking async removal handled by highlight())
//        highlight(800, sourceElement, targetElement);

        // Move mouse to source first for realism
        moveMouseToElement(sourceElement);
        wait(120 + random.nextInt(120));

        boolean canHumanize = isHumanize() && !isSeleniumHeadless() && isMoveMouse();
        Robot   r           = canHumanize ? getRobot() : null;

        try {
            if (canHumanize && r != null) {
                // Compute screen coordinates
                java.awt.Point start = getElementCenterOnScreen(sourceElement);
                java.awt.Point end   = getElementCenterOnScreen(targetElement);

                // Small jitter to avoid pixel-perfect center every time
//                start.translate(0, 0);
                end.translate(shiftX, shiftY);

                // Ensure we are exactly at start
                java.awt.Point current = MouseInfo.getPointerInfo().getLocation();
                if (current.distance(start) > 2) {
                    humanLikeMouseMove(current.x, current.y, start.x, start.y);
                }

                // Press and hold left button
                r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                wait(80 + random.nextInt(120));

                // Human-like move to target
                log.trace("---Dragging from ({}, {}) to ({}, {})", start.x, start.y, end.x, end.y);
                humanLikeMouseMove(start.x, start.y, end.x, end.y);
                wait(60 + random.nextInt(120));

                // Release
                r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                wait(120 + random.nextInt(200));
                log.trace("Drag-and-drop performed (humanized) from '{}' to '{}'", sourceId, targetId);
            } else {
                // Fallback to reliable Selenium Actions
                new org.openqa.selenium.interactions.Actions(getDriver())
                        .moveToElement(sourceElement)
                        .pause(java.time.Duration.ofMillis(150))
                        .clickAndHold(sourceElement)
                        .pause(java.time.Duration.ofMillis(150))
                        .moveToElement(targetElement)
                        .pause(java.time.Duration.ofMillis(150))
                        .release(targetElement)
                        .build()
                        .perform();
                log.trace("Drag-and-drop performed via Selenium Actions from '{}' to '{}'", sourceId, targetId);
            }
        } catch (StaleElementReferenceException e) {
            // Retry once if elements went stale mid-action
            log.trace("Elements went stale during drag-and-drop, retrying once: {}", e.getMessage());
            WebElement src2 = findElement(By.id(sourceId));
            WebElement dst2 = findElement(By.id(targetId));
            new org.openqa.selenium.interactions.Actions(getDriver())
                    .moveToElement(src2)
                    .pause(java.time.Duration.ofMillis(150))
                    .clickAndHold(src2)
                    .pause(java.time.Duration.ofMillis(150))
                    .moveToElement(dst2)
                    .pause(java.time.Duration.ofMillis(150))
                    .release(dst2)
                    .build()
                    .perform();
        } catch (Exception e) {
            log.warn("Drag-and-drop failed using humanized mode, falling back to Actions if possible: {}", e.getMessage());
            try {
                new org.openqa.selenium.interactions.Actions(getDriver())
                        .clickAndHold(sourceElement)
                        .moveToElement(targetElement)
                        .release()
                        .build()
                        .perform();
            } catch (Exception e2) {
                log.error("Drag-and-drop failed via Actions as well: {}", e2.getMessage(), e2);
                throw e2;
            }
        }
        wait(100);
        waitForPageLoaded();
    }

    /**
     * Performs a control+drag-and-drop operation to create or remove a dependency.
     * When holding control during drag, the source task becomes dependent on the target task.
     * If the dependency already exists, it will be removed (toggle behavior).
     *
     * @param sourceId The task that will depend on the target
     * @param targetId The task that will be the predecessor
     */
    public void dragAndDropWithControl(String sourceId, String targetId) {
        Actions a = new Actions(getDriver());
        a.keyDown(Keys.CONTROL);
        showTransientTitle("Ctrl");
        a.build().perform();
        dragAndDrop(sourceId, targetId);
        a.keyUp(Keys.CONTROL);
        a.build().perform();
    }

    /**
     * Performs a control+drag-and-drop operation above the target element.
     *
     * @param sourceId The task that will depend on the target
     * @param targetId The task that will be the predecessor
     */
    public void dragAndDropWithControlAbove(String sourceId, String targetId) {
        Actions a = new Actions(getDriver());
        a.keyDown(Keys.CONTROL);
        showTransientTitle("Ctrl");
        a.build().perform();
        dragAndDropAbove(sourceId, targetId);
        a.keyUp(Keys.CONTROL);
        a.build().perform();
    }

    /**
     * Performs a control+drag-and-drop operation below the target element.
     *
     * @param sourceId The task that will depend on the target
     * @param targetId The task that will be the predecessor
     */
    public void dragAndDropWithControlBelow(String sourceId, String targetId) {
        Actions a = new Actions(getDriver());
        a.keyDown(Keys.CONTROL);
        showTransientTitle("Ctrl");
        a.build().perform();
        dragAndDropBelow(sourceId, targetId);
        a.keyUp(Keys.CONTROL);
        a.build().perform();
    }

    /**
     * Compute the screen center coordinates of a DOM element, accounting for
     * browser window position and chrome height.
     */
    private java.awt.Point getElementCenterOnScreen(WebElement element) {
        org.openqa.selenium.Point     elementLocation = element.getLocation();
        org.openqa.selenium.Dimension elementSize     = element.getSize();
        Point                         windowPosition  = getDriver().manage().window().getPosition();
        int                           chromeHeight    = getBrowserChromeHeight();

        windowPosition = new Point(Math.max(windowPosition.x, 0), Math.max(windowPosition.y, 0));//prevent negative coordinates
        int targetX = windowPosition.getX() + elementLocation.getX() + (elementSize.getWidth() / 2);
        int targetY = windowPosition.getY() + elementLocation.getY() + (elementSize.getHeight() / 2) + chromeHeight;
        log.trace("Element {} center on screen calculated at ({}, {}, {}, {}, {}, {})", element.getTagName(), windowPosition, elementLocation, elementSize, chromeHeight, targetX, targetY);
        return new java.awt.Point(targetX, targetY);
    }

    /**
     * Gets or initializes the Robot instance for mouse movement.
     * Returns null if Robot cannot be initialized (e.g., in headless mode or due to security restrictions).
     *
     * @return the Robot instance, or null if unavailable
     */
    private Robot getRobot() {
        if (robot != null) {
            return robot;
        }

        // Don't even try to create Robot in headless mode
        if (isSeleniumHeadless()) {
            log.trace("Headless mode detected, Robot not available");
            return null;
        }

        try {
            robot = new Robot();
            robot.setAutoDelay(0); // We'll control delays manually
            log.trace("Robot initialized successfully for mouse movement");
        } catch (AWTException e) {
            log.warn("Failed to initialize Robot for mouse movement: {}", e.getMessage());
            robot = null;
        }

        return robot;
    }

    /**
     * Hide the currently displayed overlay with a fade-out animation.
     * The overlay will fade out over 1 second and then be removed from the DOM.
     */
    public void hideOverlay() {
        if (!isHumanize()) {
            return;
        }
        // Skip if we're in headless mode
        if (isSeleniumHeadless()) {
            return;
        }
        try {
            String script =
                    "var overlay = document.getElementById('video-intro-overlay');\n" +
                            "if (overlay) {\n" +
                            "    overlay.style.opacity = '0';\n" +
                            "    setTimeout(function() {\n" +
                            "        overlay.remove();\n" +
                            "    }, 1000);\n" +
                            "}\n";

            executeJavaScript(script);
            log.trace("Hiding overlay with fade-out animation");

            // Wait for fade-out animation to complete
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for overlay fade-out");
            }
        } catch (Exception e) {
            log.error("Failed to hide overlay: {}", e.getMessage(), e);
        }
    }

    /**
     * Explicitly hide the transient title badge if currently shown.
     */
    public void hideTransientTitle() {
        if (!isHumanize()) {
            return;
        }
        try {
            String script =
                    "var el=document.getElementById('video-key-title'); if(el){ el.remove(); }";
            executeJavaScript(script);
            log.trace("Transient title removed");
        } catch (Exception e) {
            log.trace("Failed to remove transient title: {}", e.getMessage());
        }
    }

    /**
     * Highlights one or more elements by their IDs for a short period of time (default 2 seconds).
     * This is a convenience method that finds the elements by ID and then highlights them.
     * <p>
     * All elements are highlighted with a red outline and subtle shadow that don't affect layout.
     * The highlights are applied simultaneously to all elements and removed after the duration.
     *
     * @param ids One or more element IDs to highlight
     */
    public void highlight(String... ids) {
        highlight(2000, ids);
    }

    /**
     * Highlights one or more elements by their IDs for a specified duration.
     * This is a convenience method that finds the elements by ID and then highlights them.
     * <p>
     * All elements are highlighted with a red outline and subtle shadow that don't affect layout.
     * The highlights are applied simultaneously to all elements and removed after the duration.
     *
     * @param durationMillis Duration in milliseconds to show the highlight (e.g., 2000 for 2 seconds)
     * @param ids            One or more element IDs to highlight
     */
    public void highlight(int durationMillis, String... ids) {
        if (!isHumanize()) {
            return;
        }
        if (ids == null || ids.length == 0) {
            log.warn("No element IDs provided to highlight");
            return;
        }

        // Find all elements by their IDs
        WebElement[] elements = new WebElement[ids.length];
        for (int i = 0; i < ids.length; i++) {
            waitUntil(ExpectedConditions.elementToBeClickable(By.id(ids[i])));
            elements[i] = findElement(By.id(ids[i]));
        }

        // Delegate to the WebElement version
        highlight(durationMillis, elements);
    }

    /**
     * Highlights one or more WebElements on the page for a short period of time (default 2 seconds).
     * This is useful for creating instruction videos where you want to draw the viewer's attention
     * to specific elements without needing to describe their exact location.
     * <p>
     * All elements are highlighted with a red outline and subtle shadow that don't affect layout.
     * The highlights are applied simultaneously to all elements and removed after the duration.
     * The original element styles are preserved and restored after highlighting.
     *
     * @param elements One or more WebElements to highlight
     */
    public void highlight(WebElement... elements) {
        highlight(2000, elements);
    }

    /**
     * Highlights one or more WebElements on the page for a specified duration.
     * This is useful for creating instruction videos where you want to draw the viewer's attention
     * to specific elements without needing to describe their exact location.
     * <p>
     * All elements are highlighted with a red outline and subtle shadow that don't affect layout.
     * The highlights are applied simultaneously to all elements and removed after the duration.
     * The original element styles are preserved and restored after highlighting.
     *
     * @param durationMillis Duration in milliseconds to show the highlight (e.g., 2000 for 2 seconds)
     * @param elements       One or more WebElements to highlight
     */
    public void highlight(int durationMillis, WebElement... elements) {
        if (!isHumanize()) {
            return;
        }
        if (!highlightEnabled || elements == null || elements.length == 0) {
            log.warn("No elements provided to highlight");
            return;
        }

        try {
            // Build JavaScript to highlight all elements using outline and box-shadow
            // Using outline instead of border prevents layout shifts because outline doesn't affect the box model

            String script = "var elements = arguments;\n" +
                    "var originals = [];\n" +
                    "\n" +

                    // Add highlights to all elements
                    "for (var i = 0; i < elements.length; i++) {\n" +
                    "  var element = elements[i];\n" +
                    "  if (!element) continue;\n" +
                    "\n" +
                    "  // Use outline and box-shadow instead of border to prevent layout shifts\n" +
                    "  originals.push({\n" +
                    "    element: element,\n" +
                    "    outline: element.style.outline,\n" +
                    "    outlineOffset: element.style.outlineOffset,\n" +
                    "    boxShadow: element.style.boxShadow,\n" +
                    "    transition: element.style.transition\n" +
                    "  });\n" +
                    "  \n" +
                    "  element.style.transition = 'outline 0.3s ease-in-out, box-shadow 0.3s ease-in-out';\n" +
                    "  element.style.outline = '3px solid #ff0000';\n" +
                    "  element.style.outlineOffset = '0px';\n" +
                    "  // Add a subtle shadow for better visibility\n" +
                    "  element.style.boxShadow = '0 0 8px 2px rgba(255, 0, 0, 0.5)';\n" +
                    "}\n" +
                    "\n" +
                    "// Store the cleanup data for later removal\n" +
                    "return { originals: originals };\n";

            // Wait for any previous highlight removal to complete before applying new highlights
            synchronized (highlightLock) {
                if (highlightRemovalThread != null && highlightRemovalThread.isAlive()) {
                    try {
                        log.trace("Waiting for previous highlight removal to complete...");
                        highlightRemovalThread.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while waiting for previous highlight removal");
                    }
                }

                // Execute the highlight script
                Object result = executeJavaScript(script, elements);

                log.trace("Highlighted {} element(s) for {}ms", elements.length, durationMillis);

                // Create cleanup script
                String cleanupScript =
                        "var cleanup = arguments[0];\n" +
                                "if (!cleanup) return;\n" +
                                "\n" +
                                "// Restore original styles for outline highlights\n" +
                                "if (cleanup.originals) {\n" +
                                "  cleanup.originals.forEach(function(original) {\n" +
                                "    if (original.element) {\n" +
                                "      original.element.style.transition = original.transition || '';\n" +
                                "      original.element.style.outline = original.outline || '';\n" +
                                "      original.element.style.outlineOffset = original.outlineOffset || '';\n" +
                                "      original.element.style.boxShadow = original.boxShadow || '';\n" +
                                "    }\n" +
                                "  });\n" +
                                "}\n";

                // Start asynchronous removal thread
                highlightRemovalThread = new Thread(() -> {
                    try {
                        // Wait for the specified duration
                        Thread.sleep(durationMillis);
                        // Remove highlights
                        executeJavaScript(cleanupScript, result);
                        log.trace("Removed highlights from {} element(s)", elements.length);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Highlight removal thread interrupted");
                    } catch (StaleElementReferenceException e) {
                        //ignore, as this happens all the time when the element is no longer in the DOM
//                        logger.trace("Element went stale before highlight removal: {}", e.getMessage());
                    } catch (Exception e) {
                        log.trace("Failed to remove highlights: {}", e.getMessage(), e);
                    }
                }, "HighlightRemovalThread");

                highlightRemovalThread.start();
            }

        } catch (Exception e) {
            log.error("Failed to highlight elements: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to highlight elements: " + e.getMessage(), e);
        }
    }

    /**
     * Performs a human-like mouse movement from one point to another.
     * Uses a B-spline curve for natural arc-like path and variable speed with easing
     * (starts fast, ends slow) to simulate real human mouse movement.
     *
     * @param fromX starting X coordinate
     * @param fromY starting Y coordinate
     * @param toX   target X coordinate
     * @param toY   target Y coordinate
     */
    private void humanLikeMouseMove(int fromX, int fromY, int toX, int toY) {
        Robot robotInstance = getRobot();
        if (robotInstance == null) {
            return;
        }

        int    deltaX   = toX - fromX;
        int    deltaY   = toY - fromY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // If the distance is very small, just move directly
        if (distance < 5) {
            robotInstance.mouseMove(toX, toY);
            return;
        }

        // Generate control points for B-spline to create a shallow arc
        // Add a perpendicular offset to create a curved path
        double midX = (fromX + toX) / 2.0;
        double midY = (fromY + toY) / 2.0;

        // Calculate perpendicular direction for the curve
        double perpX      = -deltaY;
        double perpY      = deltaX;
        double perpLength = Math.sqrt(perpX * perpX + perpY * perpY);

        // Normalize and scale the perpendicular offset (shallow arc, about 5-10% of distance)
        double curveFactor = 0.05 + random.nextDouble() * 0.05; // 5-10% arc
        double offsetX     = (perpX / perpLength) * distance * curveFactor;
        double offsetY     = (perpY / perpLength) * distance * curveFactor;

        // Randomly choose arc direction
        if (random.nextBoolean()) {
            offsetX = -offsetX;
            offsetY = -offsetY;
        }

        // Control point for the curve (offset from midpoint)
        double ctrlX = midX + offsetX;
        double ctrlY = midY + offsetY;

        // Calculate number of steps (more steps for smoother movement)
        int steps = (int) (distance / 3); // Approximately 3 pixels per step
        steps = Math.max(steps, 20); // Minimum steps for smoothness
        steps = Math.min(steps, 200); // Maximum to prevent overly slow movements
        // Apply user-configurable steps multiplier and clamp to a reasonable upper bound
        steps = (int) Math.round(steps * mouseMoveStepsMultiplier);
        steps = Math.max(20, Math.min(steps, 400));

        // Move the mouse along a quadratic B-spline curve with variable speed
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;

            // Apply easing function: starts fast (large steps), ends slow (small steps)
            // Using a cubic ease-out function: 1 - (1-t)^3
            double eased = 1 - Math.pow(1 - t, 3);

            // Quadratic Bezier curve formula
            double x = Math.pow(1 - eased, 2) * fromX +
                    2 * (1 - eased) * eased * ctrlX +
                    Math.pow(eased, 2) * toX;
            double y = Math.pow(1 - eased, 2) * fromY +
                    2 * (1 - eased) * eased * ctrlY +
                    Math.pow(eased, 2) * toY;

            robotInstance.mouseMove((int) x, (int) y);

            // Variable delay: faster at start, slower at end
            // Start with 1-3ms, end with 3-8ms
            int baseDelay;
            if (t < 0.3) {
                // Fast start
                baseDelay = 1 + random.nextInt(3);
            } else if (t < 0.7) {
                // Medium speed
                baseDelay = 2 + random.nextInt(4);
            } else {
                // Slow end for precision
                baseDelay = 3 + random.nextInt(SHIFTY);
            }

            // Apply configurable delay multiplier
            double m             = mouseMoveDelayMultiplier;
            int    adjustedDelay = (int) Math.round(baseDelay * (m <= 0 ? 0 : m));

            if (adjustedDelay > 0) {
                robotInstance.delay(adjustedDelay);
            }
        }

        // Ensure we end exactly at the target position
        robotInstance.mouseMove(toX, toY);

        // Small pause at the end (human-like settling), scaled by the delay multiplier
        int endPause = 20 + random.nextInt(30);
        endPause = (int) Math.round(endPause * (mouseMoveDelayMultiplier <= 0 ? 0 : mouseMoveDelayMultiplier));
        if (endPause > 0) {
            robotInstance.delay(endPause);
        }
    }

    /**
     * Moves the mouse cursor smoothly to the center of the specified element.
     * This method only performs the movement if humanize is enabled and not in headless mode.
     * The actual click should still be performed by Selenium for reliability.
     *
     * @param element the WebElement to move the mouse to
     */
    protected void moveMouseToElement(WebElement element) {
        // Skip if humanize is disabled, or we're in headless mode
        if (!isHumanize() || isSeleniumHeadless() || !isMoveMouse()) {
            return;
        }

        highlight(element);

        // Initialize Robot if needed
        Robot robotInstance = getRobot();
        if (robotInstance == null) {
            log.trace("Robot not available, skipping mouse movement");
            return;
        }

        try {
            java.awt.Point target = getElementCenterOnScreen(element);

//            // Get element location and size relative to the page
//            org.openqa.selenium.Point     elementLocation = element.getLocation();
//            org.openqa.selenium.Dimension elementSize     = element.getSize();
//
//            // Get browser window position on screen
//            Point windowPosition = getDriver().manage().window().getPosition();
//
//            // Calculate browser chrome height (cached after first calculation)
//            int chromeHeight = getBrowserChromeHeight();
//
//            // Calculate target screen coordinates (center of element)
//            int targetX = windowPosition.getX() + elementLocation.getX() + (elementSize.getWidth() / 2);
//            int targetY = windowPosition.getY() + elementLocation.getY() + (elementSize.getHeight() / 2) + chromeHeight;

            // Get current mouse position
            java.awt.Point currentMouse = MouseInfo.getPointerInfo().getLocation();

            // Perform smooth mouse movement with human-like characteristics
            humanLikeMouseMove(currentMouse.x, currentMouse.y, target.x, target.y);

            log.trace("Moved mouse to element '{}' at ({}, {})", element.getText(), target.x, target.y);
            wait(300);

        } catch (Exception e) {
            log.warn("Failed to move mouse to element: {}", e.getMessage());
            // Continue with normal Selenium click even if mouse movement fails
        }
    }

    public void setComboBoxValue(String id, String text) {
        if (!isHumanize()) {
            super.setComboBoxValue(id, text);
            return;
        }
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));
        WebElement comboBoxElement = findElement(By.id(id));
        setComboBoxValue(comboBoxElement, text);
    }

    /**
     * Sets a combobox value in a humanized way by clicking on the dropdown toggle,
     * waiting for the overlay to appear, and clicking on the matching item.
     * This simulates how a real human would interact with a combobox using mouse clicks.
     *
     * @param text the text of the item to select
     */
    public void setComboBoxValue(WebElement comboBoxElement, String text) {
//        if (!isHumanize()) {
//            super.setComboBoxValue(id, text);
//            return;
//        }
//        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));
//        WebElement comboBoxElement = findElement(By.id(id));
        WebElement inputElement = comboBoxElement.findElement(By.tagName("input"));
        waitForElementToBeInteractable(inputElement.getAttribute("id"));
        try {
            // Find and click the toggle button to open the dropdown
            // A human would just click the toggle button directly (no need to click input first)
            // Vaadin combobox uses shadow DOM, so we use expandRootElementAndFindElement to access it
            try {
                // Try to find toggle button by ID first
                WebElement toggleButton = expandRootElementAndFindElement(comboBoxElement, "#toggleButton");

                if (toggleButton == null) {
                    // Fallback: try to find by part attribute
                    toggleButton = expandRootElementAndFindElement(comboBoxElement, "[part='toggle-button']");
                }

                if (toggleButton != null) {
                    moveMouseToElement(toggleButton);
                    clickElement(toggleButton);
//                    toggleButton.click();
                    log.trace("Clicked combobox toggle button via shadow DOM");
                } else {
                    // Fallback: click on the input field if toggle button not found
                    log.trace("Toggle button not found in shadow DOM, clicking input field to open dropdown");
                    inputElement.click();
                }
            } catch (Exception ex) {
                // Fallback: click on the input field if shadow DOM access fails
                log.warn("Failed to access toggle button via shadow DOM: {}, clicking input field to open dropdown", ex.getMessage());
                inputElement.click();
            }

            // Wait for the dropdown overlay to become visible
            // The overlay element exists in the DOM even when closed
            // When opened, the 'opened' attribute is set to "true" (not an empty string)
            waitUntil(ExpectedConditions.attributeToBe(By.cssSelector("vaadin-combo-box-overlay"), "opened", "true"));

            // Find dropdown items
            // Items are in the light DOM as children of vaadin-combo-box-scroller
            // We can query them directly without accessing shadow DOM
            List<WebElement> dropdownItems = getDriver().findElements(By.cssSelector("vaadin-combo-box-item"));

            log.trace("Found {} dropdown items", dropdownItems.size());

            // Find the item that matches the text
            WebElement matchingItem = null;
            for (WebElement item : dropdownItems) {
                String itemText = item.getText();
                if (itemText != null && itemText.trim().equals(text.trim())) {
                    matchingItem = item;
                    break;
                }
            }

            if (matchingItem != null) {
                // Move mouse to the item and click it
                moveMouseToElement(matchingItem);
                wait(100);
                matchingItem.click();
                log.trace("Clicked on dropdown item: {}", text);
            } else {
                log.warn("Could not find dropdown item with text: {}. Falling back to keyboard method.", text);
                // Fallback to typing method if item not found
                setComboBoxValueByTyping(inputElement, text);
            }

            // Wait for dropdown to close
            wait(200);

        } catch (Exception ex) {
            log.warn("Error during humanized combobox selection: {}. Falling back to keyboard method.", ex.getMessage());
            // Fallback to typing method on any error
            setComboBoxValueByTyping(inputElement, text);
        }
    }

    /**
     * Fallback method to set combobox value by typing.
     * Used when humanized click selection fails or item cannot be found.
     *
     * @param inputElement the input element within the combobox
     * @param text         the text to type
     */
    private void setComboBoxValueByTyping(WebElement inputElement, String text) {
        String value = inputElement.getAttribute("value");
        if (!value.isEmpty()) {
            inputElement.sendKeys(Keys.CONTROL + "a");
            inputElement.sendKeys(Keys.DELETE);
        }
        typeText(inputElement, text);
        inputElement.sendKeys(Keys.RETURN);
        wait(500);
        inputElement.sendKeys(Keys.ARROW_DOWN, Keys.TAB);
    }

//    /**
//     * Adjust per-character typing delay in milliseconds for humanized mode.
//     */
//    public void setTypingDelayMillis(int typingDelayMillis) {
//        this.typingDelayMillis = Math.max(0, typingDelayMillis);
//    }

    /**
     * Sets a date picker value in a humanized way by clicking the toggle button,
     * then moving to the input field and typing the date.
     * This simulates how a human would interact with a date picker.
     *
     * @param datePickerId the ID of the date picker component
     * @param date         the LocalDate value to set
     */
    public void setDatePickerValue(String datePickerId, LocalDate date) {
        if (!isHumanize()) {
            super.setDatePickerValue(datePickerId, date);
            return;
        }
        // Find the date picker element
        WebElement datePickerElement = findElement(By.id(datePickerId));
        // Find the toggle button in the shadow DOM to give visual feedback
        WebElement toggleButton = expandRootElementAndFindElement(datePickerElement, "[part='toggle-button']");

        if (toggleButton != null) {
            // Click the toggle button to show the calendar (gives visual feedback)
            moveMouseToElement(toggleButton);
            wait(100);
            toggleButton.click();
            log.trace("Clicked date picker toggle button");
            wait(300); // Wait for calendar to appear
        }

        // Find the input field - it's NOT in shadow DOM, it's a direct child with slot='input'
        // The actual input has an id like "search-input-vaadin-date-picker-20"
        WebElement inputField = datePickerElement.findElement(By.cssSelector("input[slot='input']"));

        log.trace("Found input field, typing date");

        moveMouseToElement(inputField);
        wait(100);
        inputField.click();// Click the input field to focus it
        wait(100);
        inputField.clear();// Clear any existing value
        wait(100);

        // Format the date in US format (M/d/yyyy)
        // Note: This matches the browser's default US locale
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        log.info("Typing date into date picker: {} (formatted as US: {})", date, dateStr);

        typeText(inputField, dateStr);

        wait(500);
        inputField.sendKeys(Keys.ENTER);// Press Enter to confirm the date and close the calendar
        log.trace("Pressed Enter to confirm date");

        // Wait for the calendar overlay to close
        wait(200);
        WebElement overlay = expandRootElementAndFindElement(datePickerElement, "vaadin-date-picker-overlay");
        waitUntil(ExpectedConditions.invisibilityOfAllElements(overlay));
        log.trace("Date picker overlay closed successfully");
        log.trace("Successfully set date: {}", date);
    }

    public void setDateTimePickerValue(String datePickerId, LocalDateTime date) {
        if (!isHumanize()) {
            super.setDateTimePickerValue(datePickerId, date);
            return;
        }
        // Find the date-time picker element
        WebElement dateTimePickerElement = findElement(By.id(datePickerId));

        {
            // find the date picker element
            WebElement datePickerElement = dateTimePickerElement.findElement(By.tagName("vaadin-date-picker"));

            // Find the toggle button in the shadow DOM to give visual feedback
            WebElement toggleButton = expandRootElementAndFindElement(datePickerElement, "[part='toggle-button']");

            if (toggleButton != null) {
                // Click the toggle button to show the calendar (gives visual feedback)
                moveMouseToElement(toggleButton);
                wait(100);
                toggleButton.click();
                log.trace("Clicked date picker toggle button");
                wait(300); // Wait for calendar to appear
            }

            // Find the input field - it's NOT in shadow DOM, it's a direct child with slot='input'
            // The actual input has an id like "search-input-vaadin-date-picker-20"
            WebElement inputField = dateTimePickerElement.findElement(By.cssSelector("input[slot='input']"));

            log.trace("Found input field, typing date-time");

            moveMouseToElement(inputField);
            wait(100);
            inputField.click();// Click the input field to focus it
            wait(100);
            inputField.clear();// Clear any existing value
            wait(100);

            // Format the date in US format (M/d/yyyy)
            // Note: This matches the browser's default US locale
            String dateStr = date.toLocalDate().format(DateTimeFormatter.ofPattern("M/d/yyyy"));
            log.trace("Typing date into date time picker: {} (formatted as US: {})", date, dateStr);

            typeText(inputField, dateStr);

            wait(500);
            inputField.sendKeys(Keys.ENTER);// Press Enter to confirm the date and close the calendar
            log.trace("Pressed Enter to confirm date-time");

            // Wait for the calendar overlay to close
            wait(200);
            WebElement overlay = expandRootElementAndFindElement(datePickerElement, "vaadin-date-picker-overlay");
            waitUntil(ExpectedConditions.invisibilityOfAllElements(overlay));
            log.trace("Date picker overlay closed successfully");
        }
        {
            // find the date picker element
            WebElement timePickerElement = dateTimePickerElement.findElement(By.tagName("vaadin-time-picker"));
            setTimePickerValue(timePickerElement, date.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a")));
        }
        log.trace("Successfully set date-time: {}", date);
    }

    /**
     * Set a multiplier for the per-step delay during human-like mouse movements.
     * Values > 1.0 slow the movement; values between 0.0 and 1.0 speed it up. Negative values are clamped to 0.0.
     */
    public void setMouseMoveDelayMultiplier(double mouseMoveDelayMultiplier) {
        this.mouseMoveDelayMultiplier = Math.max(0.0, mouseMoveDelayMultiplier);
    }

    /**
     * Set a multiplier for the number of steps used during human-like mouse movement.
     * Values > 1.0 increase steps (smoother and slower), values between 0.1 and 1.0 reduce steps. Values < 0.1 are clamped to 0.1.
     */
    public void setMouseMoveStepsMultiplier(double mouseMoveStepsMultiplier) {
        this.mouseMoveStepsMultiplier = Math.max(0.1, mouseMoveStepsMultiplier);
    }

    public void setMultiSelectComboBoxValue(String id, String[] text) {
        //todo fix this method to use humanized selection
//        if (!isHumanize()) {
//            super.setMultiSelectComboBoxValue(id, text);
//            return;
//        }
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));
        WebElement comboBoxElement = findElement(By.id(id));
        setMultiSelectComboBoxValue(comboBoxElement, text);
    }

    public void setMultiSelectComboBoxValue(WebElement comboBoxElement, String[] text) {
        WebElement inputElement = comboBoxElement.findElement(By.tagName("input"));
        waitForElementToBeInteractable(inputElement.getAttribute("id"));
        WebElement toggleButton = null;
        toggleButton = expandRootElementAndFindElement(comboBoxElement, "#toggleButton");
        moveMouseToElement(toggleButton);
        clickElement(toggleButton);
        log.trace("Clicked combobox toggle button via shadow DOM");

        // Wait for the dropdown overlay to become visible
        // The overlay element exists in the DOM even when closed
        // When opened, the 'opened' attribute is set to "true" (not an empty string)
        waitUntil(ExpectedConditions.attributeToBe(comboBoxElement, "opened", "true"));

        // Find dropdown items
        // Items are in the light DOM as children of vaadin-combo-box-scroller
        // We can query them directly without accessing shadow DOM
        List<WebElement> dropdownItems = getDriver().findElements(By.cssSelector("vaadin-multi-select-combo-box-item"));

        log.trace("Found {} dropdown items", dropdownItems.size());

        // Find the item that matches the text
        for (String s : text) {
            WebElement matchingItem = null;
            for (WebElement item : dropdownItems) {
                String itemText = item.getText();
                if (itemText.startsWith(s.trim())) {
                    matchingItem = item;
                    break;
                }
            }

            // Move mouse to the item and click it
            moveMouseToElement(matchingItem);
            wait(100);
            matchingItem.click();
            log.trace("Clicked on dropdown item: {}", s);
            // Wait for selection to be visible
            wait(200);
        }
        // close the dropdown box
        if (toggleButton != null) {
            moveMouseToElement(toggleButton);
            clickElement(toggleButton);
        }
    }

    public void setTimePickerValue(WebElement comboBoxElement, String text) {
//        if (!isHumanize()) {
//            super.setComboBoxValue(id, text);
//            return;
//        }
//        waitUntil(ExpectedConditions.elementToBeInteractable(By.id(id)));
//        WebElement comboBoxElement = findElement(By.id(id));
        WebElement inputElement = comboBoxElement.findElement(By.tagName("input"));
        waitForElementToBeInteractable(inputElement.getAttribute("id"));
        try {
            // Find and click the toggle button to open the dropdown
            // A human would just click the toggle button directly (no need to click input first)
            // Vaadin combobox uses shadow DOM, so we use expandRootElementAndFindElement to access it
            try {
                // Try to find toggle button by ID first
                WebElement toggleButton = expandRootElementAndFindElement(comboBoxElement, "#toggleButton");

                if (toggleButton == null) {
                    // Fallback: try to find by part attribute
                    toggleButton = expandRootElementAndFindElement(comboBoxElement, "[part='toggle-button']");
                }

                if (toggleButton != null) {
                    moveMouseToElement(toggleButton);
                    clickElement(toggleButton);
//                    toggleButton.click();
                    log.trace("Clicked time-picker toggle button via shadow DOM");
                } else {
                    // Fallback: click on the input field if toggle button not found
                    log.warn("Toggle button not found in shadow DOM, clicking input field to open dropdown");
                    inputElement.click();
                }
            } catch (Exception ex) {
                // Fallback: click on the input field if shadow DOM access fails
                log.warn("Failed to access toggle button via shadow DOM: {}, clicking input field to open dropdown", ex.getMessage());
                inputElement.click();
            }

            // Wait for the dropdown overlay to become visible
            // The overlay element exists in the DOM even when closed
            // When opened, the 'opened' attribute is set to "true" (not an empty string)
            waitUntil(ExpectedConditions.attributeToBe(By.cssSelector("vaadin-time-picker-overlay"), "opened", "true"));

            // Find dropdown items
            // Items are in the light DOM as children of vaadin-combo-box-scroller
            // We can query them directly without accessing shadow DOM
            List<WebElement> dropdownItems = getDriver().findElements(By.cssSelector("vaadin-time-picker-item"));

            log.trace("Found {} dropdown items", dropdownItems.size());

            // Find the item that matches the text
            WebElement matchingItem = null;
            for (WebElement item : dropdownItems) {
                String itemText = item.getText();
                if (itemText != null && itemText.trim().equals(text.trim())) {
                    matchingItem = item;
                    break;
                }
            }

            if (matchingItem != null) {
                // Move mouse to the item and click it
                moveMouseToElement(matchingItem);
                wait(100);
                matchingItem.click();
                log.trace("Clicked on dropdown item: {}", text);
            } else {
                log.warn("Could not find dropdown item with text: {}. Falling back to keyboard method.", text);
                // Fallback to typing method if item not found
                setComboBoxValueByTyping(inputElement, text);
            }

            // Wait for dropdown to close
            wait(200);

        } catch (Exception ex) {
            log.warn("Error during humanized time-picker selection: {}. Falling back to keyboard method.", ex.getMessage());
            // Fallback to typing method on any error
            setComboBoxValueByTyping(inputElement, text);
        }
    }

    /**
     * Show a full-screen overlay with title and subtitle.
     * The overlay fades in over 1 second and remains visible until hideOverlay() is called.
     * This is useful for creating intro screens or chapter markers in instruction videos.
     *
     * @param title    Main title text to display
     * @param subtitle Subtitle text (can be null or empty)
     */
    public void showOverlay(String title, String subtitle) {
        if (!isHumanize()) {
            return;
        }
        try {
            log.trace("Preparing to show overlay with title: '{}' and subtitle: '{}'", title, subtitle);
            // Wait for page to be fully loaded
            waitForPageLoaded();

            // Escape special characters for JavaScript
            String escapedTitle    = escapeJavaScript(title);
            String escapedSubtitle = subtitle != null ? escapeJavaScript(subtitle) : "";

            String script =
                    "// Remove existing overlay if present\n" +
                            "var existingOverlay = document.getElementById('video-intro-overlay');\n" +
                            "if (existingOverlay) {\n" +
                            "    existingOverlay.remove();\n" +
                            "}\n" +
                            "\n" +
                            "// Create overlay container\n" +
                            "var overlay = document.createElement('div');\n" +
                            "overlay.id = 'video-intro-overlay';\n" +
                            "overlay.style.cssText = 'position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background-color: #000000; z-index: 999999; display: flex; justify-content: center; align-items: center; opacity: 0; transition: opacity 1s ease-in-out;';\n" +
                            "\n" +
                            "// Create content container\n" +
                            "var content = document.createElement('div');\n" +
                            "content.style.cssText = 'text-align: center;';\n" +
                            "\n" +
                            "// Create title element\n" +
                            "var titleDiv = document.createElement('div');\n" +
                            "titleDiv.style.cssText = 'color: #ffffff; font-size: 48px; font-weight: bold; margin-bottom: 20px; font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);';\n" +
                            "titleDiv.textContent = '" + escapedTitle + "';\n" +
                            "content.appendChild(titleDiv);\n" +
                            "\n" +
                            "// Create subtitle element if provided\n" +
                            "if ('" + escapedSubtitle + "') {\n" +
                            "    var subtitleDiv = document.createElement('div');\n" +
                            "    subtitleDiv.style.cssText = 'color: #cccccc; font-size: 24px; font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);';\n" +
                            "    subtitleDiv.textContent = '" + escapedSubtitle + "';\n" +
                            "    content.appendChild(subtitleDiv);\n" +
                            "}\n" +
                            "\n" +
                            "overlay.appendChild(content);\n" +
                            "document.body.appendChild(overlay);\n" +
                            "\n" +
                            "// Trigger fade-in animation\n" +
                            "setTimeout(function() {\n" +
                            "    overlay.style.opacity = '1';\n" +
                            "}, 10);\n";

            executeJavaScript(script);
            log.trace("Displayed overlay with title: '{}'", title);

            // Wait for fade-in animation to complete
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for overlay fade-in");
            }
        } catch (Exception e) {
            log.error("Failed to show overlay: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to show overlay: " + e.getMessage(), e);
        }
    }

    /**
     * Show overlay, wait for specified duration, then hide it automatically.
     * Total duration will be: 1s (fade-in) + displaySeconds + 1s (fade-out).
     *
     * @param title          Main title text to display
     * @param subtitle       Subtitle text (can be null or empty)
     * @param displaySeconds How long to display the overlay between fade-in and fade-out
     */
    public void showOverlayAndWait(String title, String subtitle, int displaySeconds) {
        if (!isHumanize()) {
            return;
        }
        showOverlay(title, subtitle);

        // Wait for the specified display duration
        if (displaySeconds > 0) {
            try {
                Thread.sleep(displaySeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for overlay display duration");
            }
        }

        hideOverlay();
    }

    /**
     * Show a quick, non-animated title badge and automatically hide it after a short time.
     * No subtitle, no fade-in/out; intended for showing pressed keys or short hints in videos.
     *
     * @param title         text to show inside the badge
     * @param displayMillis how long to keep it visible before removing (milliseconds)
     */
    public void showTransientTitle(String title, int displayMillis) {
        if (!isHumanize()) {
            return;
        }
        try {
            // Ensure page is ready
            waitForPageLoaded();

            String escapedTitle = escapeJavaScript(title == null ? "" : title);
            int    duration     = Math.max(0, displayMillis);

            String script =
                    "(function(){\n" +
                            "  var id='video-key-title';\n" +
                            "  // Remove any existing instance to avoid stacking and timer races\n" +
                            "  var existing=document.getElementById(id);\n" +
                            "  if(existing){ existing.remove(); }\n" +
                            "  // Create a small center badge\n" +
                            "  var el=document.createElement('div');\n" +
                            "  el.id=id;\n" +
                            "  el.textContent='" + escapedTitle + "';\n" +
                            "  el.style.cssText = " +
                            "    'position:fixed; left:50%; top:50%; transform:translate(-50%,-50%);' +\n" +
                            "    'background:rgba(0,0,0,0.65); color:#fff; padding:8px 14px; border-radius:10px;' +\n" +
                            "    'font-size:20px; font-weight:600; z-index:999999; pointer-events:none;' +\n" +
                            "    'box-shadow:0 2px 8px rgba(0,0,0,0.35); font-family:\"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif;';\n" +
                            "  document.body.appendChild(el);\n" +
                            "  // Auto-remove after the requested duration (no fade effects)\n" +
                            "  setTimeout(function(){ var e=document.getElementById(id); if(e){ e.remove(); } }, " + duration + ");\n" +
                            "})();";

            executeJavaScript(script);
            log.trace("Displayed transient title: '{}' for {} ms", title, duration);
        } catch (Exception e) {
            log.error("Failed to show transient title: {}", e.getMessage(), e);
        }
    }

    /**
     * Convenience overload that shows the transient title for a default of 1000 ms.
     */
    public void showTransientTitle(String title) {
        showTransientTitle(title, 1000);
    }

    /**
     * Internal helper to type text into an input/textarea.
     * Respects humanized typing mode if enabled, using variable delays
     * to simulate real human typing patterns (not perfectly rhythmic).
     */
    protected void typeText(WebElement inputElement, String text) {
        if (text == null || text.isEmpty()) return;
        if (!isHumanize()) {
            super.typeText(inputElement, text);
            return;
        }

        for (int idx = 0; idx < text.length(); idx++) {
            String ch = String.valueOf(text.charAt(idx));
            inputElement.sendKeys(ch);

            if (typingDelayMillis > 0) {
                try {
                    // Variable delay: base delay +/- 50% randomness
                    // This creates a more natural typing rhythm
                    int minDelay      = typingDelayMillis / 2;
                    int maxDelay      = typingDelayMillis + (typingDelayMillis / 2);
                    int variableDelay = minDelay + random.nextInt(maxDelay - minDelay + 1);

                    // Occasionally add a longer pause (simulating thinking/hesitation)
                    // About 10% chance of a longer pause
                    if (random.nextInt(10) == 0) {
                        variableDelay += typingDelayMillis * (2 + random.nextInt(3)); // 2-4x longer
                    }

                    Thread.sleep(variableDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
