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

import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.AboutView;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test helper class for interacting with the Product UI components.
 * <p>
 * This class provides methods to test product-related operations in the UI such as
 * creating, editing, deleting products and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 */
@Component
@Lazy
@Slf4j
public class AboutViewTester extends AbstractViewTester {

    /**
     * Constructs a new ProductViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public AboutViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    /**
     * Navigates to the ProductListView using OIDC Authentication with Keycloak.
     * <p>
     * Opens the product list URL and handles the OIDC login redirect to Keycloak.
     *
     * @param username            the username to use for OIDC authentication
     * @param password            the password to use for OIDC authentication
     * @param screenshotFileName  optional login screenshot destination
     * @param recordingFolderName destination folder for the recording
     * @param testName            name used for the recording
     * @throws Exception when login or screenshot capture fails
     */
    public void login(String username, String password, String screenshotFileName, String recordingFolderName, String testName) throws Exception {
        try {
            // Navigate to the application login page.
            // If a previous Keycloak session is still active, log out first so a different user can
            // actually complete a fresh OIDC login instead of silently re-authenticating.
            final String userEmail = SecurityUtils.getUserEmail();
            if (userEmail.equals(username)) {
                //already logged in as the requested user, no need to log in again
                log.trace("OIDC Login: Already logged in as the requested user: " + username);
                return;
            }
            if (!SecurityUtils.isOidcLogin()) {
                log.trace("OIDC Login: No user is currently logged in, proceeding with login for: " + username);
            } else {
                log.trace("OIDC Login: Currently logged in as: " + userEmail + ", logging out to switch to: " + username);
                logout();
            }
            seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + LoginView.ROUTE);
            if (!seleniumHandler.isRecording()) {
                seleniumHandler.startRecording(recordingFolderName, testName);
            }
            log.trace("OIDC Login: Current URL after navigation: " + seleniumHandler.getCurrentUrl());
            // Check if the OIDC login button is present
//            System.out.println("OIDC Login: Checking for OIDC login button with ID: " + LoginView.OIDC_LOGIN_BUTTON);
            if (seleniumHandler.isElementPresent(By.id(LoginView.OIDC_LOGIN_BUTTON))) {
                log.trace("OIDC Login: OIDC login button found, clicking it now");
                if (screenshotFileName != null) {
                    seleniumHandler.takeElementScreenShot(seleniumHandler.findElement(By.id(LoginView.LOGIN_VIEW)), LoginView.LOGIN_VIEW, screenshotFileName);
                }

                // Click with SeleniumHandler for mouse movement and humanization
                seleniumHandler.click(LoginView.OIDC_LOGIN_BUTTON);
                log.trace("OIDC Login: Clicked login button with mouse movement");

                // Wait longer for Keycloak redirect (up to 10 seconds)
//                System.out.println("OIDC Login: Waiting for Keycloak login page");
//                System.out.println("OIDC Login: Current URL after waiting: " + seleniumHandler.getCurrentUrl());
                // Check for username field
//                System.out.println("OIDC Login: Looking for username field");
                try {
                    seleniumHandler.waitForElementToBeClickable("kc-login");

                    // Keycloak shows one of two form variants:
                    //   1. Normal login   – both "username" and "password" fields are present and editable.
                    //   2. Re-authentication – the session was not fully cleared; Keycloak pre-fills the
                    //      username (read-only element id "kc-attempted-username") and only asks for the
                    //      password.  This can happen when prompt=login is sent while a Keycloak SSO
                    //      cookie is still alive (e.g., post_logout_redirect_uri not yet accepted).
                    seleniumHandler.pushImplicitWait(Duration.ofSeconds(1));
                    boolean isReAuthentication = seleniumHandler.isElementPresent(By.id("kc-attempted-username"));
                    seleniumHandler.popImplicitWait();
                    if (isReAuthentication
                            && !username.equals(seleniumHandler.findElement(By.id("kc-attempted-username")).getDomProperty("value"))) {
                        // Keycloak's remembered username is read-only; restart login to switch accounts.
                        seleniumHandler.click("reset-login");
                        seleniumHandler.waitForElementToBeClickable("username");
                        isReAuthentication = false;
                    }
                    if (!isReAuthentication) {
                        WebElement usernameField = seleniumHandler.findElement(By.id("username"));
                        seleniumHandler.typeIntoElement(usernameField, username);
                    }
                    WebElement passwordField = seleniumHandler.findElement(By.id("password"));
                    seleniumHandler.typeIntoElement(passwordField, password);

                    // Click login button with mouse movement
                    WebElement loginButton = seleniumHandler.findElement(By.id("kc-login"));
                    seleniumHandler.clickElement(loginButton);

                    // Wait for redirect back
//                    System.out.println("OIDC Login: Waiting for redirect back to application");
                    seleniumHandler.waitForElementToBeClickable(AboutView.ABOUT_PAGE_TITLE);
                    assertEquals(username, seleniumHandler.findElement(By.id(MainLayout.ID_USER_MENU))
                            .getText().lines().findFirst().orElseThrow(), "OIDC login must authenticate the requested user");
//                    System.out.println("OIDC Login: Successfully logged in with OIDC");
                } catch (Exception e) {
                    log.error("OIDC Login: Error during Keycloak login: " + e.getMessage());
                    throw e;
                }
            } else {
                throw new Exception("OIDC Login: Fatal error in login process: ");
            }
        } catch (Exception e) {
            log.error("OIDC Login: Fatal error in login process: " + e.getMessage(), e);
//            e.printStackTrace();
//            seleniumHandler.takeScreenshot("fatal-login-error.png");
            throw e;
        }
    }

    /**
     * Logs out of the application and waits for the provider sign-in button.
     */
    public void logout() {
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_LOGOUT);
        seleniumHandler.waitForElementToBeClickable(LoginView.OIDC_LOGIN_BUTTON);
    }
}
