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

import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.AboutView;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;

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
     * @param username the username to use for OIDC authentication
     * @param password the password to use for OIDC authentication
     */
    public void login(String username, String password, String screenshotFileName, String recordingFolderName, String testName) throws Exception {
        try {
            // Navigate to the application login page
//            System.out.println("OIDC Login: Navigating to login page");
            seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + LoginView.ROUTE);
            seleniumHandler.startRecording(recordingFolderName, testName);
//            System.out.println("OIDC Login: Current URL after navigation: " + seleniumHandler.getCurrentUrl());
            // Check if the OIDC login button is present
//            System.out.println("OIDC Login: Checking for OIDC login button with ID: " + LoginView.OIDC_LOGIN_BUTTON);
            if (seleniumHandler.isElementPresent(By.id(LoginView.OIDC_LOGIN_BUTTON))) {
//                System.out.println("OIDC Login: OIDC login button found, clicking it now");
                if (screenshotFileName != null) {
                    seleniumHandler.takeElementScreenShot(seleniumHandler.findElement(By.id(LoginView.LOGIN_VIEW)), LoginView.LOGIN_VIEW, screenshotFileName);
                }

                // Click with SeleniumHandler for mouse movement and humanization
                seleniumHandler.click(LoginView.OIDC_LOGIN_BUTTON);
//                System.out.println("OIDC Login: Clicked login button with mouse movement");

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
                    if (isReAuthentication) {
                        // Username is pre-filled; only the password is required.
                        WebElement passwordField = seleniumHandler.findElement(By.id("password"));
                        seleniumHandler.typeIntoElement(passwordField, password);
                    } else {
                        // Normal login: fill in both username and password.
                        WebElement usernameField = seleniumHandler.findElement(By.id("username"));
                        WebElement passwordField = seleniumHandler.findElement(By.id("password"));
                        seleniumHandler.typeIntoElement(usernameField, username);
                        seleniumHandler.typeIntoElement(passwordField, password);
                    }

                    // Click login button with mouse movement
                    WebElement loginButton = seleniumHandler.findElement(By.id("kc-login"));
                    seleniumHandler.clickElement(loginButton);

                    // Wait for redirect back
//                    System.out.println("OIDC Login: Waiting for redirect back to application");
                    seleniumHandler.waitForElementToBeClickable(AboutView.ABOUT_PAGE_TITLE);
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

    public void logout() {
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_LOGOUT);
        seleniumHandler.waitForElementToBeClickable(LoginView.OIDC_LOGIN_BUTTON);
    }
}
