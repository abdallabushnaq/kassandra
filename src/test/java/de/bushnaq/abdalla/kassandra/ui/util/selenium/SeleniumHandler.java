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

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * handles vaadin ui tests using selenium, please always use HumanizedSeleniumHandler
 */
@Component
@Getter
@Slf4j
class SeleniumHandler {
    private static final int             DEFAULT_BROWSER_CHROME_HEIGHT = 130; // Typical Chrome window chrome height in pixels
    private              Integer         browserChromeHeight           = null; // Cached browser chrome height
    private              WebDriver       driver;
    private final        Duration        implicitWaitDuration;
    private final        Stack<Duration> implicitWaitStack             = new Stack<>();
    private final        VideoRecorder   videoRecorder;
    private              WebDriverWait   wait;
    private              Duration        waitDuration;
    private final        Stack<Duration> waitDurationStack             = new Stack<>();
    private              Dimension       windowSize;                     // Added field to store custom window size

    public SeleniumHandler() {
        this(Duration.ofSeconds(10), Duration.ofSeconds(30));
    }

    public SeleniumHandler(Duration implicitWaitDuration, Duration waitDuration) {
        this.implicitWaitDuration = implicitWaitDuration;
        this.waitDuration         = waitDuration;
        this.videoRecorder        = new VideoRecorder();
    }

    protected void centerMouseOnBrowser() {
        //dummy implementation
    }

    public void click(String id) {
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));
        WebElement element = findElement(By.id(id));
        moveMouseToElement(element);
        element.click();
        log.trace("Clicked element with ID: " + id);
    }

    /**
     * Clicks the clear button (X) in a Vaadin text field or combo box.
     * This method finds the clear button within the field's shadow DOM and clicks it.
     *
     * @param fieldId the ID of the text field or combo box
     */
    public void clickClearButton(String fieldId) {
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(fieldId)));
        WebElement field = findElement(By.id(fieldId));

        // Vaadin text fields have a clear button in their shadow DOM
        // Access it via shadow root and click it
        WebElement clearButton = expandRootElementAndFindElement(field, "[part='clear-button']");

        if (clearButton != null && clearButton.isDisplayed()) {
            moveMouseToElement(clearButton);
            clearButton.click();
            log.trace("Clicked clear button for field with ID: " + fieldId);
        } else {
            log.warn("Clear button not found or not visible for field with ID: " + fieldId);
            throw new RuntimeException("Clear button not found for field: " + fieldId);
        }
    }

    /**
     * Clicks a WebElement directly with mouse movement support.
     * This is useful for elements that aren't part of the standard Vaadin component structure,
     * such as Keycloak login buttons or other external pages.
     *
     * @param element the WebElement to click
     */
    public void clickElement(WebElement element) {
        moveMouseToElement(element);
        element.click();
        log.trace("Clicked element with mouse movement");
    }

    public void copy() {
        showTransientTitle("Ctrl+C");
        Actions a = new Actions(getDriver());
        a.keyDown(Keys.CONTROL);
        a.sendKeys("c");
        a.keyUp(Keys.CONTROL);
        a.build().perform();
    }

    @PreDestroy
    public void destroy() {
        if (videoRecorder.isRecording()) {
            stopRecording();
        }

        if (driver != null) {
//            ScreenShotCreator.takeScreenShot(driver, testInfo.getDisplayName(), testInfo.getTestMethod().get().getName());
//            driver.close();//closing the browser will fail in headless mode
            driver.quit();//quit the driver and close all windows
            driver = null;
        }
        log.trace("quit selenium driver");
    }

    /**
     * Ensures that a grid contains exactly the expected count of elements with the specified name.
     * <p>
     * This method is useful for verifying that a specific element appears exactly the expected
     * number of times in a grid, such as checking that no duplicate products were created.
     *
     * @param gridId         the ID of the grid WebElement to check
     * @param gridNamePrefix the prefix used in the element IDs within the grid
     * @param name           the name suffix to search for in the grid element IDs
     * @param expectedCount  the expected number of occurrences of the element in the grid
     */
    public void ensureElementCountInGrid(String gridId, String gridNamePrefix, String name, int expectedCount) {
        {
            waitForElementToBeLocated(gridId);
            WebElement grid = getDriver().findElement(By.id(gridId));
            // Wait a moment for any UI updates to complete
            wait(500);
            // Check for the div containing the product name
            List<WebElement> matchingElements = grid.findElements(By.cssSelector("[id^='" + gridNamePrefix + name + "']"));
            assertEquals(expectedCount, matchingElements.size(), "Expected 1 element with name '" + name + "' but found " + matchingElements.size());
        }
    }

    public void ensureIsInList(String id, String userName) {
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id + userName)));
        log.trace("Element with ID: " + id + userName + " is in grid");
    }

    public void ensureIsNotInList(String id, String name) {
        Duration implicitTime = getImplicitWaitDuration();
        setImplicitWaitDuration(Duration.ofSeconds(1));
        waitUntil(ExpectedConditions.not(ExpectedConditions.elementToBeClickable(By.id(id + name))));
        setImplicitWaitDuration(implicitTime);
        log.trace("Element with ID: " + id + name + " is not in grid");
    }

    public void ensureIsSelected(String id, String userName) {
        WebElement label = findElement(By.id(id));
        waitUntil(ExpectedConditions.textToBePresentInElement(label, userName));
    }

    /**
     * Escape special characters in a string for safe use in JavaScript.
     *
     * @param input The string to escape
     * @return The escaped string safe for JavaScript injection
     */
    protected String escapeJavaScript(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Executes JavaScript in the current browser window.
     * <p>
     * This method allows executing arbitrary JavaScript code in the browser, which is useful for
     * interacting with complex components that may be difficult to access through standard Selenium methods.
     * It's particularly useful for setting values on custom components like color pickers and date pickers.
     *
     * @param script the JavaScript to execute
     * @param args   the arguments to pass to the script (will be available in the script as arguments[0], arguments[1], etc.)
     * @return the value returned by the script, which may be null
     */
    public Object executeJavaScript(String script, Object... args) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) getDriver();
        return jsExecutor.executeScript(script, args);
    }

    public WebElement expandRootElementAndFindElement(WebElement element, String elementName) {
        // Escape single quotes in the selector to prevent JavaScript syntax errors
        String     escapedElementName = elementName.replace("'", "\\'");
        String     script             = String.format("return arguments[0].shadowRoot.querySelector('%s')", escapedElementName);
        WebElement ele                = (WebElement) ((JavascriptExecutor) getDriver()).executeScript(script, element);
        return ele;
    }

    public WebElement findDialogOverlayElement(String dialogId) {
        WebElement    rootHost   = getDriver().findElement(By.tagName("vaadin-dialog-overlay"));
        SearchContext shadowRoot = rootHost.getShadowRoot();
        return shadowRoot.findElement(By.cssSelector("div[part=\"overlay\"]"));
    }

    public WebElement findElement(By by) {
        return getDriver().findElement(by);
    }

    public List<WebElement> findElements(By by) {
        return getDriver().findElements(by);
    }

    public WebElement findLoginOverlayElement(String dialogId) {
        WebElement    rootHost   = getDriver().findElement(By.tagName("vaadin-login-form-wrapper"));
        SearchContext shadowRoot = rootHost.getShadowRoot();
        return shadowRoot.findElement(By.cssSelector("section[part=\"form\"]"));
    }

    public void get(String url) {
        getDriver().get(url);
        log.trace("Navigated to URL: " + url);
    }

    public void getAndCheck(String url) {
        get(url);
        waitUntil(ExpectedConditions.urlContains(url));
        testForAnyError();
    }

    /**
     * Calculates and caches the browser chrome height (window decorations, address bar, toolbar).
     * Uses JavaScript to determine the actual height by comparing outer and inner window heights.
     *
     * @return the browser chrome height in pixels
     */
    protected int getBrowserChromeHeight() {
        if (browserChromeHeight != null) {
            return browserChromeHeight;
        }

        try {
            // Use JavaScript to calculate the chrome height
            Long outerHeight = (Long) executeJavaScript("return window.outerHeight;");
            Long innerHeight = (Long) executeJavaScript("return window.innerHeight;");

            if (outerHeight != null && innerHeight != null) {
                browserChromeHeight = (int) (outerHeight - innerHeight);
                log.trace("Calculated browser chrome height: {} pixels", browserChromeHeight);
            } else {
                browserChromeHeight = DEFAULT_BROWSER_CHROME_HEIGHT;
                log.warn("Using default browser chrome height: {} pixels", browserChromeHeight);
            }
        } catch (Exception e) {
            browserChromeHeight = DEFAULT_BROWSER_CHROME_HEIGHT;
            log.warn("Failed to calculate chrome height, using default: {} pixels", browserChromeHeight);
        }

        return browserChromeHeight;
    }

    public boolean getCheckbox(String id) {
        WebElement element = findElement(By.id(id));
        return element.getAttribute("checked") != null;
    }

    /**
     * Gets the current value from a color picker component.
     * <p>
     * This method retrieves the color value from a Vaadin ColorPicker component
     * using JavaScript. It accesses the component's shadow DOM to get the color
     * input element's value. Returns the hex color code with # prefix.
     *
     * @param colorPickerId the ID of the color picker component
     * @return the current color value as a hex string (e.g. "#FF5733"), or null if not available
     */
    public String getColorPickerValue(String colorPickerId) {
        // Wait for the color picker element to be present
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id(colorPickerId)));

        // Access the color input element through the shadow DOM, similar to the set method
        String colorScript =//
                "var picker = document.getElementById('" + colorPickerId + "');" +//
                        "if (picker) {" +//
                        "  var inputElement = picker.shadowRoot.querySelector('input[type=\"color\"]');" +//
                        "  if (inputElement) {" +//
                        "    return inputElement.value;" +//
                        "  }" +//
                        "  return picker.value;" + // Fallback to component's value if input not found
                        "}" +//
                        "return null;";

        return (String) executeJavaScript(colorScript);
    }

    /**
     * Gets the selected text from a Vaadin ComboBox component.
     * <p>
     * This method retrieves the visible text of the selected item in a ComboBox.
     * It uses JavaScript to access the `selectedItemLabel` property of the component,
     * which is a reliable way to get the display value, as the `value` attribute
     * might contain an internal ID.
     *
     * @param id the ID of the vaadin-combo-box element
     * @return the selected text as a String, or null if no item is selected or the element is not found.
     */
    public String getComboBoxValue(String id) {
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id(id)));
        WebElement e = findElement(By.id(id));
        WebElement i = e.findElement(By.tagName("input"));
        return i.getAttribute("value");
    }

    public String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    /**
     * Gets the current browser window size.
     *
     * @return the current window size as a Dimension object, or null if the browser is not yet initialized
     */
    public Dimension getCurrentWindowSize() {
        if (driver != null) {
            return driver.manage().window().getSize();
        }
        return windowSize;
    }

    /**
     * Gets the current value from a date picker component.
     * <p>
     * This method retrieves the date value from a Vaadin DatePicker component
     * using JavaScript and converts it to a LocalDate object.
     *
     * @param datePickerId the ID of the date picker component
     * @return the current date value as LocalDate, or null if no date is set or component not found
     */
    public LocalDate getDatePickerValue(String datePickerId) {
        // Wait for the date picker element to be present
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id(datePickerId)));

        String dateScript =//
                "var datePicker = document.getElementById('" + datePickerId + "');" +//
                        "if (datePicker && datePicker.value) {" +//
                        "  return datePicker.value;" +//
                        "}" +//
                        "return null;";

        String dateStr = (String) executeJavaScript(dateScript);

        if (dateStr != null && !dateStr.isEmpty()) {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return null;
    }

    public WebDriver getDriver() {
        if (driver != null) {
            return driver;
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        // Remove the "Chrome is being controlled by automated test software" banner
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        // Check if we're running in headless mode (for CI environment)
        boolean headlessMode = isSeleniumHeadless();
        if (headlessMode) {
            log.info("creating selenium driver, Running Chrome in headless mode");
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-browser-side-navigation");
            options.addArguments("--disable-web-security");
            options.addArguments("--dns-prefetch-disable");
            // Add a longer timeout for the page load
            options.setPageLoadTimeout(Duration.ofSeconds(60));
            // Disable the "Save password?" prompt and grant clipboard permissions
            options.setExperimentalOption("prefs", Map.of(
                    "credentials_enable_service", false,
                    "profile.password_manager_enabled", false,
                    "profile.content_settings.exceptions.clipboard", Map.of(
                            "*", Map.of("setting", 1)
                    )
            ));
        } else {
            log.info("creating selenium driver");
            // Disable the "Save password?" prompt and grant clipboard permissions
            options.setExperimentalOption("prefs", Map.of(
                    "credentials_enable_service", false,
                    "profile.password_manager_enabled", false,
                    "profile.content_settings.exceptions.clipboard", Map.of(
                            "*", Map.of("setting", 1)
                    )
            ));
        }

        options.addArguments("--remote-allow-origins=*");
        // Grant clipboard permissions without prompting
        options.addArguments("--disable-features=ClipboardContentSetting");
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

        // Set browser locale if specified via system property (e.g., -Dtest.locale=de-DE)
        String testLocale = System.getProperty("test.locale");
        if (testLocale != null && !testLocale.isEmpty()) {
            log.info("Setting browser locale to: " + testLocale);
            options.addArguments("--lang=" + testLocale);
        }

        // Enable browser console logging to capture JavaScript console.log messages
        // Use W3C-compliant logging preferences for modern Chrome/Selenium
        options.setCapability("goog:loggingPrefs", Map.of(
                "browser", "ALL",
                "driver", "ALL",
                "performance", "ALL"
        ));


        // Set a higher script timeout to prevent connection issues
        driver = new ChromeDriver(options);
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        setImplicitWaitDuration(implicitWaitDuration);
        wait = new WebDriverWait(driver, waitDuration);

        // Set default window size if not in headless mode
        if (!headlessMode) {
            if (windowSize != null) {
                getDriver().manage().window().setSize(windowSize);
                getDriver().manage().window().setPosition(new Point(33, 22));
            } else {
                //maximize window by default
                getDriver().manage().window().maximize();
            }
        }
        return driver;
    }

    /**
     * Gets the error message from a Vaadin text field element by accessing the error-message slot.
     * <p>
     * This method retrieves the error message that appears below a form field when validation fails.
     * It waits for the field to become invalid and for an error message to appear before returning it.
     * It uses JavaScript to access the shadow DOM and find the error message slot content.
     *
     * @param fieldId the ID of the field element
     * @return the error message text, or null if no error message is present after waiting
     */
    public String getFieldErrorMessage(String fieldId) {
        pushWaitDuration(Duration.ofSeconds(3));
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id(fieldId)));

        // Wait for the field to become invalid (with a timeout)
        try {
            // Use waitUntil with a custom condition to wait for field to become invalid and have an error message
            waitUntil(driver -> {
                Boolean isInvalid = (Boolean) executeJavaScript(
                        "var field = document.getElementById('" + fieldId + "');" +//
                                "return field && field.invalid;");

                if (Boolean.TRUE.equals(isInvalid)) {
                    // Now check if the error message element exists
                    String errorMsg = (String) executeJavaScript(//
                            "var field = document.getElementById('" + fieldId + "');" +//
                                    "var errorElement = field.querySelector('[slot=\"error-message\"]');" +//
                                    "return errorElement ? errorElement.textContent : null;");

                    return errorMsg != null && !errorMsg.isEmpty();
                }
                return false;
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            log.warn("Timed out waiting for error message on field: {}", fieldId);
            // We'll continue and try to get whatever message might be there
        }
        popWaitDuration();
        // Now that we've waited, get the actual error message
        String script =
                "var field = document.getElementById('" + fieldId + "');" +//
                        "if (field && field.invalid) {" +//
                        "  var errorElement = field.querySelector('[slot=\"error-message\"]');" +//
                        "  return errorElement ? errorElement.textContent : null;" +//
                        "}" +//
                        "return null;";

        return (String) executeJavaScript(script);
    }

    public String getIntegerField(String id) {
        WebElement e     = findElement(By.id(id));
        String     value = e.getAttribute("value");
        return value;
    }

    public String getRouteValue(Class<?> viewClass) {
        if (viewClass.isAnnotationPresent(com.vaadin.flow.router.Route.class)) {
            com.vaadin.flow.router.Route route = viewClass.getAnnotation(com.vaadin.flow.router.Route.class);
            if (route != null)
                return route.value();
        }
        return null;
    }

    public String getTextField(String id) {
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));
        WebElement e     = findElement(By.id(id));
        WebElement i     = e.findElement(By.tagName("input"));
        String     value = i.getAttribute("value");
        return value;
    }

    public String getTitle() {
        return getDriver().getTitle();
    }

    /**
     * Helper method to initialize WebDriverWait if it hasn't been initialized yet.
     * This is used by methods that need to wait for elements or conditions.
     */
    private void initWait() {
        if (wait == null) {
            wait = new WebDriverWait(getDriver(), waitDuration);
        }
    }

    /**
     * Checks if an element is present on the page
     *
     * @param locator the By locator for the element to check
     * @return true if the element is present, false otherwise
     */
    public boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Checks if an element with the specified ID is present on the page
     *
     * @param id the ID of the element to check
     * @return true if the element is present, false otherwise
     */
    public boolean isElementPresent(String id) {
        return isElementPresent(By.id(id));
    }

    public boolean isRecording() {
        return videoRecorder.isRecording();
    }

    public static boolean isSeleniumHeadless() {
        return Boolean.parseBoolean(System.getProperty("selenium.headless", System.getenv("SELENIUM_HEADLESS")));
    }

//    public void loginSubmit() {
//        click(LoginView.LOGIN_VIEW_SUBMIT_BUTTON);
//        log.trace("Clicked login submit button");
//        waitForPageLoaded();
//    }

    protected void moveMouseToElement(WebElement element) {
        //dummy implementation
    }

    public void past() throws Exception {
        showTransientTitle("Ctrl+V");
        {
            Actions a = new Actions(getDriver());
            a.keyDown(Keys.CONTROL);
            a.sendKeys("v");
            a.keyUp(Keys.CONTROL);
            a.build().perform();
        }
    }

    /**
     * Pops the last implicit wait duration from the stack and restores it.
     * If the stack is empty, nothing happens.
     */
    public void popImplicitWait() {
        if (!implicitWaitStack.isEmpty()) {
            setImplicitWaitDuration(implicitWaitStack.pop());
        }
    }

    /**
     * Pops the last wait duration from the stack and restores it.
     * If the stack is empty, nothing happens.
     */
    public void popWaitDuration() {
        if (!waitDurationStack.isEmpty()) {
            setWaitDuration(waitDurationStack.pop());
        }
    }

    /**
     * Pushes the current implicit wait duration onto a stack and sets a new value.
     *
     * @param newDuration the new implicit wait duration to set
     */
    public void pushImplicitWait(Duration newDuration) {
        implicitWaitStack.push(getImplicitWaitDuration());
        setImplicitWaitDuration(newDuration);
    }

    /**
     * Pushes the current wait duration onto a stack and sets a new value.
     *
     * @param newDuration the new wait duration to set
     */
    public void pushWaitDuration(Duration newDuration) {
        waitDurationStack.push(getWaitDuration());
        setWaitDuration(newDuration);
    }

    /**
     * Resets the window size setting to use default maximized window.
     * If the browser is already running, it will maximize the current window.
     */
    public void resetWindowSize() {
        windowSize = null;

        // If driver is already initialized, maximize the current window
        if (driver != null) {
            driver.manage().window().maximize();
        }
    }

    public void selectGridRow(String gridRowBaseId, Class<?> viewClass, String rowName) {
        String  url             = getRouteValue(viewClass);
        boolean outerTesting    = true;
        int     outerIterations = 12;//5 seconds
        boolean weClicked       = false;
        //try several times
        do {
            if (!weClicked) {
                WebElement row = findElement(By.id(gridRowBaseId + rowName));
                try {
                    moveMouseToElement(row);
                    row.click();
                    weClicked = true;
                    log.trace("Clicked row: " + gridRowBaseId + rowName);
                } catch (StaleElementReferenceException e) {
                    //ignore and retry
                }
            }
            //check several times
            int     innerIterations = 10;//5 seconds
            boolean innerTesting    = true;
            do {
                if (ExpectedConditions.urlContains(url).apply(getDriver())) {
                    outerTesting = false;
                }
                if (--innerIterations == 0) {
                    innerTesting = false;
                }
                if (outerTesting) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            } while (innerTesting && outerTesting);
            if (--outerIterations == 0) {
                outerTesting = false;
            }
            if (outerTesting) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        } while (outerTesting);
    }

    public void selectGridRow(String gridRowBaseId, String labelId, String rowName) {
        boolean    outerTesting    = true;
        int        outerIterations = 12;//5 seconds
        WebElement label           = findElement(By.id(labelId));
        //try several times
        do {
            WebElement row = findElement(By.id(gridRowBaseId + rowName));
            try {
                moveMouseToElement(row);
                row.click();
            } catch (StaleElementReferenceException e) {
                //ignore and retry
            }
            //check several times
            int     innerIterations = 10;//5 seconds
            boolean innerTesting    = true;
            do {
                if (ExpectedConditions.textToBePresentInElement(label, rowName).apply(getDriver())) {
                    outerTesting = false;
                }
                if (--innerIterations == 0) {
                    innerTesting = false;
                }
                if (outerTesting) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            } while (innerTesting && outerTesting);
            if (--outerIterations == 0) {
                outerTesting = false;
            }
            if (outerTesting) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        } while (outerTesting);
        assertEquals(rowName, label.getText());
    }

    public void sendKeys(String id, CharSequence... keysToSend) {
        WebElement e = findElement(By.id(id));
        WebElement i = e.findElement(By.tagName("input"));
        moveMouseToElement(i);  // Move mouse to input field before sending keys
        i.sendKeys(keysToSend);
    }

    public void setCheckCheckbox(String id, boolean value) {
        boolean old = getCheckbox(id);
        if (old != value) {
            WebElement e = findElement(By.id(id));
            WebElement i = e.findElement(By.tagName("input"));
            moveMouseToElement(i);
            if (value) {
                if (!i.isSelected()) {
                    i.click();
                }
            } else {
                if (i.isSelected()) {
                    i.click();
                }
            }
        }
    }

    /**
     * Sets a value to a color picker component.
     * <p>
     * This method provides a reliable way to set color values on Vaadin ColorPicker
     * components. It uses JavaScript to set the value directly and ensures the component
     * properly registers the color change by triggering necessary events.
     *
     * @param colorPickerId the ID of the color picker component
     * @param colorValue    the color value to set (hex format with # prefix, e.g., "#FF5733")
     * @return true if the color was successfully set, false otherwise
     */
    public boolean setColorPickerValue(String colorPickerId, String colorValue) {
        // Wait for the color picker element to be present
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id(colorPickerId)));

        // First attempt: set the value and trigger necessary events
        String colorScript =//
                "var picker = document.getElementById('" + colorPickerId + "');" +//
                        "if (picker) {" +//
                        "  picker.value = '" + colorValue + "';" +//
                        "  if (typeof picker._valueChanged === 'function') {" +//
                        "    picker._valueChanged('" + colorValue + "');" +//
                        "  }" +//
                        "  var inputElement = picker.shadowRoot.querySelector('input[type=\"color\"]');" +//
                        "  if (inputElement) {" +//
                        "    inputElement.value = '" + colorValue + "';" +//
                        "    inputElement.dispatchEvent(new Event('input', {bubbles: true}));" +//
                        "    inputElement.dispatchEvent(new Event('change', {bubbles: true}));" +//
                        "  }" +//
                        "  return picker.value === '" + colorValue + "';" +//
                        "}" +//
                        "return false;";

        Boolean result = (Boolean) executeJavaScript(colorScript);

        // If first attempt failed, try a simpler direct approach
        //TODO do we ned this?
        if (result == null || !result) {
            // Direct method as fallback: use the component's public API
            executeJavaScript(//
                    "document.getElementById('" + colorPickerId + "').value = '" + colorValue + "';" +//
                            "document.getElementById('" + colorPickerId + "').dispatchEvent(new Event('change'));"
            );

            // Verify the color was set correctly
            String verifyScript = "return document.getElementById('" + colorPickerId + "').value;";
            String verifyResult = (String) executeJavaScript(verifyScript);

            return verifyResult != null && verifyResult.equalsIgnoreCase(colorValue);
        }

        return result;
    }

    public void setComboBoxValue(String id, String text) {
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));
        WebElement e = findElement(By.id(id));
        WebElement i = e.findElement(By.tagName("input"));
        waitForElementToBeInteractable(i.getAttribute("id"));

        // Original fast method: type and select
        moveMouseToElement(i);  // Move mouse to combobox field before typing
        String value = i.getAttribute("value");
        if (value.isEmpty()) {
        } else {
            i.sendKeys(Keys.CONTROL + "a");
            i.sendKeys(Keys.DELETE);
        }
        // Humanized typing into the combobox
        typeText(i, text);
        sendKeys(id, Keys.RETURN);
        wait(500);
        sendKeys(id, Keys.ARROW_DOWN, Keys.TAB);
        log.trace("set ComboBox value=" + text);
    }


    /**
     * Sets a date picker value by directly setting it via JavaScript.
     * This is the fast method used when humanize mode is disabled.
     *
     * @param datePickerId the ID of the date picker component
     * @param date         the LocalDate value to set, can be null to clear
     */
    public void setDatePickerValue(String datePickerId, LocalDate date) {
        // Find the date picker element
        WebElement datePickerElement = findElement(By.id(datePickerId));
        // Find the input field - it's NOT in shadow DOM, it's a direct child with slot='input'
        // The actual input has an id like "search-input-vaadin-date-picker-20"
        WebElement inputField = datePickerElement.findElement(By.cssSelector("input[slot='input']"));

        log.trace("Found input field, typing date");

        inputField.clear();// Clear any existing value

        // Format the date in US format (M/d/yyyy)
        // Note: This matches the browser's default US locale
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        log.trace("Typing date into date picker: {} (formatted as US: {})", date, dateStr);

        typeText(inputField, dateStr);

        inputField.sendKeys(Keys.ENTER);// Press Enter to confirm the date and close the calendar
        log.trace("Pressed Enter to confirm date");

        log.trace("Successfully set date: {}", date);
    }

    public void setDateTimePickerValue(String datePickerId, LocalDateTime date) {
        // Wait for the date picker element to be present
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id(datePickerId)));
        if (date == null) {
            String clearScript =
                    "var datePicker = document.getElementById('" + datePickerId + "');" +
                            "if (datePicker) {" +
                            "  datePicker.value = '';" +
                            "  datePicker.dispatchEvent(new CustomEvent('change', { bubbles: true }));" +
                            "  return true;" +
                            "}" +
                            "return false;";
            executeJavaScript(clearScript);
        } else {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String dateScript =
                    "var datePicker = document.getElementById('" + datePickerId + "');" +
                            "if (datePicker) {" +
                            "  datePicker.value = '" + dateStr + "';" +
                            "  datePicker.dispatchEvent(new CustomEvent('change', { bubbles: true }));" +
                            "  return true;" +
                            "}" +
                            "return false;";
            log.trace("set DatePicker value=" + dateStr);
            executeJavaScript(dateScript);
        }
    }


    public void setImplicitWaitDuration(Duration duration) {
        getDriver().manage().timeouts().implicitlyWait(duration);
    }

    public void setIntegerField(String id, String userName) {
        WebElement e = findElement(By.id(id));
        WebElement i = e.findElement(By.tagName("input"));
        moveMouseToElement(i);  // Move mouse to integer field before typing
        String value = i.getAttribute("value");
        if (value.isEmpty()) {
        } else {
            i.sendKeys(Keys.CONTROL + "a");
            i.sendKeys(Keys.DELETE);
        }
        // Humanized typing
        typeText(i, userName);
    }

//    public void setLoginPassword(String loginPassword) {
//        WebElement passwordElement = findElement(By.id(LoginView.LOGIN_VIEW_PASSWORD));
//        moveMouseToElement(passwordElement);  // Move mouse to password field before typing
//        log.trace("sent loginPassword='{}' to element with name '{}}'%n", loginPassword, LoginView.LOGIN_VIEW_PASSWORD);
//        // Humanized typing
//        typeText(passwordElement, loginPassword);
//    }

//    public void setLoginUser(String loginUser) {
//        waitForElementToBeLocated(LoginView.LOGIN_VIEW_USERNAME);
//        WebElement usernameElement = findElement(By.id(LoginView.LOGIN_VIEW_USERNAME));
//        moveMouseToElement(usernameElement);  // Move mouse to username field before typing
//        log.trace("sent loginUser='{}' to element with id '{}'%n", loginUser, LoginView.LOGIN_VIEW_USERNAME);
//        // Humanized typing
//        typeText(usernameElement, loginUser);
//    }

    public void setTextArea(String id, String userName) {
        WebElement e = findElement(By.id(id));
        WebElement i = e.findElement(By.tagName("textarea"));
        moveMouseToElement(i);  // Move mouse to text area before typing
        String value = i.getAttribute("value");
        if (value.isEmpty()) {
        } else {
            i.sendKeys(Keys.CONTROL + "a");
            i.sendKeys(Keys.DELETE);
        }
        // Humanized typing
        typeText(i, userName);
    }

    public void setTextField(String id, String text) {
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));
        WebElement e = findElement(By.id(id));
        WebElement i = e.findElement(By.tagName("input"));
        moveMouseToElement(i);  // Move mouse to text field before typing
        String value = i.getAttribute("value");
        if (value.isEmpty()) {
        } else {
            i.sendKeys(Keys.CONTROL + "a");
            i.sendKeys(Keys.DELETE);
        }
        log.trace("set TextField " + id + " to '" + text + "'\n");
        // Humanized typing
        typeText(i, text);
    }

    public void setWaitDuration(Duration waitDuration) {
        this.waitDuration = waitDuration;
        wait.withTimeout(waitDuration);
    }

    /**
     * Sets the browser window to a specific size.
     * This must be called before the browser is initialized.
     * If the browser is already running, it will resize the current window.
     *
     * @param width  the width of the browser window in pixels
     * @param height the height of the browser window in pixels
     */
    public void setWindowSize(int width, int height) {
        windowSize = new Dimension(width, height);

        // If driver is already initialized, resize the current window
        if (driver != null) {
            driver.manage().window().setSize(windowSize);
        }
    }

    /**
     * Show a quick, non-animated title badge and automatically hide it after a short time.
     * No subtitle, no fade-in/out; intended for showing pressed keys or short hints in videos.
     *
     * @param title         text to show inside the badge
     * @param displayMillis how long to keep it visible before removing (milliseconds)
     */
    public void showTransientTitle(String title, int displayMillis) {
        if (!HumanizedSeleniumHandler.isHumanize()) {
        }
    }

    /**
     * Convenience overload that shows the transient title for a default of 1000 ms.
     */
    public void showTransientTitle(String title) {
        showTransientTitle(title, 1000);
    }

    /**
     * Start recording screen activities for the current test
     *
     * @param folderName Subfolder name for storing the video
     * @param testName   Name of the test for the video file name
     */
    public void startRecording(String folderName, String testName) {
        if (!videoRecorder.isRecording() && folderName != null && testName != null) {
            try {
                getDriver(); // Ensure the driver is initialized and browser is open

                // Center the mouse on the browser window before starting recording
                // This prevents the mouse from starting far away (e.g., on a second monitor)
                centerMouseOnBrowser();

                // Pass the WebDriver to VideoRecorder to access browser content
                videoRecorder.setWebDriver(driver);

                // Option 1: Record the entire browser window
                Point     position = driver.manage().window().getPosition();
                Dimension size     = driver.manage().window().getSize();
                videoRecorder.setCaptureArea(position.getX(), position.getY(), size.getWidth(), size.getHeight());

                // Start the recording with content-only mode, which will use JavaScript to determine
                // the exact content area within the browser window
                if (videoRecorder.startRecording(folderName, testName, true)) {
                    log.trace("Started video recording for test: {}", testName);
                }
            } catch (IOException | AWTException e) {
                log.error("Failed to start video recording: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Stop the active recording and save the video file
     *
     * @return File object pointing to the recorded video, or null if recording failed
     */
    public File stopRecording() {
        if (videoRecorder.isRecording()) {
            try {
                File videoFile = videoRecorder.stopRecording();
                if (videoFile != null) {
                    log.info("Video recording saved to: {}", videoFile.getAbsolutePath());
                }
                return videoFile;
            } catch (IOException e) {
                log.error("Failed to stop video recording: {}", e.getMessage(), e);
            }
        }
        return null;
    }

    public void takeElementScreenShot(WebElement overlayElement, String dialogId, String fileName) {
        ScreenShotCreator.takeElementScreenshot(getDriver(), overlayElement, dialogId, fileName);
    }

    public void takeScreenShot(String fileName) {
        ScreenShotCreator.takeScreenShot(getDriver(), fileName);
    }

    private void testForAnyError() {
        assertEquals(-1, getTitle().indexOf("HTTP Status"), String.format("HTTP error detected '%s'.", getTitle()));
    }

    private void testForError(String errorTitle) {
        //        String errorMessage = String.format("HTTP Status %d – Not Found", errorCode);
        assertTrue(getTitle().indexOf(errorTitle) != -1, String.format("Expected '%s', but got '%s'.", errorTitle, getTitle()));
    }

    public void testUrl(String expectedUrl) {
        String currentUrl = getCurrentUrl();
        waitUntil(ExpectedConditions.urlContains(expectedUrl));
        log.trace(String.format("expectedUrl=%s, actualUrl=%s", expectedUrl, currentUrl));
        assertTrue(currentUrl.contains(expectedUrl));
    }

    public void toggleCheckbox(String id) {
        WebElement e = findElement(By.id(id));
        WebElement i = e.findElement(By.tagName("input"));
        moveMouseToElement(i);
        i.click();
    }

    /**
     * Types text into a WebElement directly with humanized typing support.
     * This is useful for input fields that aren't part of the standard Vaadin component structure,
     * such as Keycloak login fields or other external pages.
     *
     * @param element the WebElement (input or textarea) to type into
     * @param text    the text to type
     */
    public void typeIntoElement(WebElement element, String text) {
        moveMouseToElement(element);
        // Clear any existing text first
        String value = element.getAttribute("value");
        if (value != null && !value.isEmpty()) {
            element.sendKeys(Keys.CONTROL + "a");
            element.sendKeys(Keys.DELETE);
        }
        // Type with humanization
        typeText(element, text);
        log.trace("Typed text into element with humanization");
    }

    /**
     * Internal helper to type text into an input/textarea.
     * Respects humanized typing mode if enabled, using variable delays
     * to simulate real human typing patterns (not perfectly rhythmic).
     */
    protected void typeText(WebElement inputElement, String text) {
        if (text == null || text.isEmpty()) return;
        inputElement.sendKeys(text);
    }

    public void wait(int milliseconds) {
        try {
            log.trace("Waiting for {}ms.", milliseconds);
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting", e);
        }
    }

    public void waitForElementInvisibility(String id) {
        log.trace("Waiting for {} to become invisible.", id);
        waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(id)));
    }

    public void waitForElementToBeClickable(String id) {
        log.trace("Waiting for {} to become Clickable.", id);
        waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));
    }

    public void waitForElementToBeDisabled(String id) {
        log.trace("Waiting for {} to become disabled.", id);
        waitUntil(driver -> {
            try {
                WebElement element = driver.findElement(By.id(id));
                return element.getAttribute("disabled") != null ||
                        "true".equals(element.getAttribute("aria-disabled"));
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Wait for an element to become interactable (enabled and visible)
     * This is useful for elements like ComboBoxes that may be disabled initially
     * and only become enabled after other UI actions.
     *
     * @param id The ID of the element to wait for
     */
    public void waitForElementToBeInteractable(String id) {
        log.trace("Waiting for {} to become Interactable.", id);
        try {
            // Wait for the element to be visible first
            waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id(id)));

            // Wait for the element to not have the 'disabled' attribute
            waitUntil(driver -> {
                WebElement element = driver.findElement(By.id(id));
                return element.getAttribute("disabled") == null &&
                        !"true".equals(element.getAttribute("aria-disabled"));
            });

            // Finally, wait for it to be clickable
            waitUntil(ExpectedConditions.elementToBeClickable(By.id(id)));

            // Small additional delay to ensure UI is fully ready
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            log.error("Error waiting for element to be interactable: " + id, e);
            throw e;
        }
    }

    public void waitForElementToBeLocated(String id) {
        log.trace("Waiting for {} to become Located.", id);
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id(id)));
    }

    public void waitForElementToBeVisible(By locator) {
        log.trace("Waiting for element located by {} to become visible.", locator);
        waitUntil(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for a notification of a specific type to appear.
     * <p>
     * This method waits for a Vaadin notification to appear with the specified type
     * (such as "error", "success", "warning", or "info").
     *
     * @param notificationType the type of notification to wait for (e.g., "error")
     */
    public void waitForNotification(String notificationType) {
        log.trace("Waiting for {} notification.", notificationType);
        // Wait for notification overlay to appear
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.cssSelector("vaadin-notification-container vaadin-notification-card")));
        // Wait specifically for notification with the given type
        waitUntil(ExpectedConditions.presenceOfElementLocated(By.cssSelector("vaadin-notification-container vaadin-notification-card[theme~='" + notificationType + "']")));
    }

    public void waitForPageLoaded() {
        waitForPageLoaded(30);
    }

    public void waitForPageLoaded(long seconds) {
        log.trace("Waiting for page loaded with {} seconds.", seconds);
        ExpectedCondition<Boolean> expectation = new ExpectedCondition<>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
            }
        };
        Wait<WebDriver> wait = new WebDriverWait(getDriver(), Duration.ofSeconds(seconds));
        try {
            wait.until(expectation);
        } catch (Throwable error) {
            fail("Timeout waiting for Page Load Request to complete.");
        }
    }

    public void waitForUrl(String url) {
        log.trace("Waiting for {} url.", url);
        waitUntil(ExpectedConditions.urlContains(url));
        testForAnyError();
    }

//    public void waitForUrl(String url, String errortitle) {
//        waitUntil(ExpectedConditions.urlContains(url));
//        testForError(errortitle);
//    }

//    public void waitForUrlNot(String url) {
//        waitUntil(ExpectedConditions.not(ExpectedConditions.urlToBe(url)));
//        //        waitUntil(ExpectedConditions.urlToBe(url));
//        testForAnyError();
//    }

    public void waitUntil(ExpectedCondition<?> condition) {
        wait.until(condition);
    }

    /**
     * Waits until the browser window is closed either manually by the user
     * or by another process.
     *
     * @param timeoutMillis Maximum time to wait in milliseconds, or 0 for infinite wait
     * @return true if browser was closed, false if timeout occurred
     */
    public boolean waitUntilBrowserClosed(long timeoutMillis) {

        if (isSeleniumHeadless()) {
            log.warn("Browser is running in headless mode; waitUntilBrowserClosed will return immediately.");
            return false;
        }

        log.error("Waiting {}s for browser to be closed.", timeoutMillis / 1000);
        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                // Try to execute a simple script or get window handles to verify browser is open
                getDriver().getWindowHandles();
                // Short sleep to prevent high CPU usage
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (videoRecorder.isRecording()) {
                        stopRecording();
                    }
                    driver.quit();//quit the driver
                    driver = null;
                    return false;
                }

                // Check for timeout if specified
                if (timeoutMillis > 0 && System.currentTimeMillis() - startTime > timeoutMillis) {
                    if (videoRecorder.isRecording()) {
                        stopRecording();
                    }
                    driver.quit();//quit the driver
                    driver = null;
                    return false;
                }
            } catch (WebDriverException e) {
                if (e instanceof NoSuchSessionException) {
                    // Browser has been closed if we get a NoSuchSessionException
                } else {
                    throw e;
                }
                if (videoRecorder.isRecording()) {
                    stopRecording();
                }
                driver.quit();//quit the driver
                driver = null;
                return true;
            }
        }
    }

}
