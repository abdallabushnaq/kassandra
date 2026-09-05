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

package de.bushnaq.abdalla.kassandra.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.Lumo;
import de.bushnaq.abdalla.kassandra.config.KassandraProperties;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.UndoRedoApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import de.bushnaq.abdalla.kassandra.ui.component.Breadcrumbs;
import de.bushnaq.abdalla.kassandra.ui.component.ThemeSessionState;
import de.bushnaq.abdalla.kassandra.ui.component.ThemeToggle;
import de.bushnaq.abdalla.kassandra.ui.component.UndoHistoryPanel;
import de.bushnaq.abdalla.kassandra.ui.view.AboutView;
import de.bushnaq.abdalla.kassandra.ui.view.OidcProviderManagementView;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

@RolesAllowed({"ADMIN", "USER"})
//@JsModule("/tooltips.js")
@Slf4j
public final class MainLayout extends AppLayout implements BeforeEnterObserver {

    /**
     * Stable ID for the action-history toggle.
     */
    public static final String           ID_ACTION_HISTORY_BUTTON        = "main-layout-action-history-button";
    public static final String           ID_BREADCRUMBS                  = "main-layout-breadcrumbs";
    public static final String           ID_LOGO                         = "main-layout-logo";
    public static final String           ID_TAB_BASE                     = "main-layout-tab-";
    //    public static final String           ID_TAB_USERS                 = "main-layout-tab-users";
    public static final String           ID_THEME_TOGGLE                 = "main-layout-theme-toggle";
    public static final String           ID_USER_MENU                    = "main-layout-user-menu";
    public static final String           ID_USER_MENU_ABOUT              = "main-layout-user-menu-about";
    public static final String           ID_USER_MENU_AVAILABILITY       = "main-layout-user-menu-availability";
    public static final String           ID_USER_MENU_LOCATION           = "main-layout-user-menu-location";
    public static final String           ID_USER_MENU_LOGOUT             = "main-layout-user-menu-logout";
    public static final String           ID_USER_MENU_MANAGE_SETTINGS    = "main-layout-user-menu-manage-settings";
    public static final String           ID_USER_MENU_MANAGE_USERS       = "main-layout-user-menu-manage-users";
    public static final String           ID_USER_MENU_MANAGE_USER_GROUPS = "main-layout-user-menu-manage-user-groups";
    public static final String           ID_USER_MENU_MANAGE_WORK_WEEKS  = "main-layout-user-menu-manage-work-weeks";
    public static final String           ID_USER_MENU_OFF_DAYS           = "main-layout-user-menu-off-days";
    public static final String           ID_USER_MENU_VIEW_PROFILE       = "main-layout-user-menu-view-profile";
    public static final String           ID_USER_MENU_WORK_WEEK          = "main-layout-user-menu-work-week";
    private final       Collection<UUID> activeProductIds                = new LinkedHashSet<>();
    AuthenticationContext authenticationContext;
    private final Div               breadcrumbContainer;
    @Getter
    private final Breadcrumbs       breadcrumbs               = new Breadcrumbs();
    private       SplitLayout       contentSplit;
    private final Div               historyPane;
    private       boolean           historyPaneOpen;
    private       Image             logoImage;
    private final Map<Tab, String>  tabToPathMap              = new HashMap<>();
    private       Tabs              tabs;
    private final ThemeSessionState themeSessionState;
    private final UndoHistoryPanel  undoHistoryPanel;
    private       boolean           updatingTabFromNavigation = false;
    private final UserApi           userApi;
    private       Image             userAvatarImage;

    MainLayout(UserApi userApi, ThemeSessionState themeSessionState, UndoRedoApi undoRedoApi,
               KassandraProperties kassandraProperties) {
        this.authenticationContext = authenticationContext;
        this.userApi               = userApi;
        this.themeSessionState     = themeSessionState;
        this.undoHistoryPanel      = new UndoHistoryPanel(undoRedoApi, () -> activeProductIds,
                kassandraProperties.getUndoRedo().getHistoryLimit(),
                this::closeHistoryDrawer,
                () -> UI.getCurrent().getPage().reload());
        this.historyPane           = createHistoryPane();
        UI.getCurrent().getPage().addJavaScript("/js/tooltips.js");
        setPrimarySection(Section.NAVBAR);
        addClassName("main-layout"); // scope CSS to this layout

        // Create main navigation bar components
        HorizontalLayout navbarLayout = createNavBar();
        breadcrumbContainer = createBreadcrumbs();

        var navAndBreadcrumbs = new VerticalLayout();
        navAndBreadcrumbs.setPadding(false);
        navAndBreadcrumbs.setSpacing(false);
        navAndBreadcrumbs.setMargin(false);

        navAndBreadcrumbs.add(navbarLayout, breadcrumbContainer);

        // Add the combined layout to the navbar area
        addToNavbar(true, navAndBreadcrumbs);
        // Remove margins from navbar layout and apply padding to the container instead
        navAndBreadcrumbs.getStyle().set("padding-left", "var(--lumo-space-xs)");
        navAndBreadcrumbs.getStyle().set("padding-right", "var(--lumo-space-xs)");
        navAndBreadcrumbs.getStyle().set("padding-bottom", "var(--lumo-space-xs)");
        // Remove these lines that are causing the overflow
        this.getStyle().set("padding-left", "var(--lumo-space-xs)");
        this.getStyle().set("padding-right", "var(--lumo-space-xs)");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        setBreadcrumbsVisible(true);
        final String pathToMatch = event.getLocation().getPath();
        updatingTabFromNavigation = true;
        try {
            tabToPathMap.forEach((tab, path) -> {
                if (("/" + pathToMatch).equals(path)) {
                    tabs.setSelectedTab(tab);
                }
            });
        } finally {
            updatingTabFromNavigation = false;
        }
    }

    /**
     * Deselects all navigation tabs.
     * Called by views that do not correspond to a top-level menu entry (e.g. {@link de.bushnaq.abdalla.kassandra.ui.view.AboutView}).
     */
    public void clearTabSelection() {
        tabs.setSelectedTab(null);
    }

    private void closeHistoryDrawer() {
        historyPane.getStyle().set("width", "0").set("min-width", "0").set("max-width", "0");
        contentSplit.setSplitterPosition(100);
        historyPaneOpen = false;
    }

    private Div createBreadcrumbs() {
        breadcrumbs.setId(ID_BREADCRUMBS);
        Div breadcrumbContainer = new Div(breadcrumbs);
        breadcrumbContainer.addClassNames(
                Padding.Horizontal.MEDIUM,
                Padding.Vertical.XSMALL,
                Width.FULL,
                Background.CONTRAST_5
        );

        return breadcrumbContainer;
    }

    private Div createHistoryPane() {
        VerticalLayout drawer = new VerticalLayout(undoHistoryPanel);
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.setSizeFull();
        drawer.addClassName("planning-history-drawer");
        Div pane = new Div(drawer);
        pane.setSizeFull();
        pane.addClassName("planning-history-pane");
        pane.getStyle().set("width", "0").set("min-width", "0").set("overflow", "hidden");
        return pane;
    }

    private Image createLogo() {
        // Create the logo image component
        logoImage = new Image("images/logo.svg", "Kassandra Logo");
        logoImage.setHeight("24px");
        logoImage.setId(ID_LOGO);

        // Check initial theme and set appropriate logo
        UI      ui          = UI.getCurrent();
        boolean isDarkTheme = ui.getElement().getThemeList().contains(Lumo.DARK);
        updateLogoBasedOnTheme(isDarkTheme);

        return logoImage;
    }

    private HorizontalLayout createNavBar() {
        HorizontalLayout navbarLayout = new HorizontalLayout();
        navbarLayout.setWidthFull();
        navbarLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        navbarLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        navbarLayout.addClassName("navbar-custom");

        // Add logo and app name to the left
        Image logoLayout = createLogo();

        // Add navigation tabs to the center
        tabs = createTabs();
        tabs.addClassNames(Margin.Horizontal.XSMALL);

        // Create theme toggle and register theme change listener
        ThemeToggle themeToggle = createThemeToggle();
        themeToggle.addThemeVariants(ButtonVariant.LUMO_SMALL);

        // Add user menu to the right
        Component userMenu = createUserMenu();

        Button historyButton = new Button(VaadinIcon.TIME_BACKWARD.create(), event -> toggleHistoryDrawer());
        historyButton.setId(ID_ACTION_HISTORY_BUTTON);
        historyButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        historyButton.setTooltipText("Undo history");
        navbarLayout.add(logoLayout, tabs, historyButton, themeToggle, userMenu);
        navbarLayout.expand(tabs);
        return navbarLayout;
    }

    private Tab createTab(MenuEntry menuEntry) {
        Tab tab = new Tab();

        if (menuEntry.icon() != null) {
            Icon icon = new Icon(menuEntry.icon());
            icon.setSize("var(--lumo-icon-size-xs)");
            icon.getStyle().setMarginRight("4px");

            Span label = new Span(menuEntry.title());
            label.getStyle().set("font-size", "var(--lumo-font-size-xs)");

            HorizontalLayout tabLayout = new HorizontalLayout(icon, label);
            tabLayout.setSpacing(false);
            tabLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            tab.add(tabLayout);
        } else {
            tab.add(new Span(menuEntry.title()));
        }
//        tab.setId(menuEntry.title().substring(1));

        return tab;
    }

    private Tabs createTabs() {
        var tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.HORIZONTAL);

        // Create tabs from menu configuration
        MenuConfiguration.getMenuEntries().forEach(entry -> {
            Tab tab = createTab(entry);
            tab.setId(entry.path());
            tabs.add(tab);
            tabToPathMap.put(tab, entry.path());
        });

        // Handle tab selection changes
        tabs.addSelectedChangeListener(event -> {
            if (updatingTabFromNavigation) {
                return; // triggered by beforeEnter — don't navigate again
            }
            Tab selectedTab = event.getSelectedTab();
            if (selectedTab != null) {
                String path = tabToPathMap.get(selectedTab);
                if (path != null) {
                    log.info("Navigating to path '{}' from tab '{}'", path, selectedTab.getId().orElse("unknown"));
                    getUI().ifPresent(ui -> ui.navigate(path));
                }
            }
        });

        return tabs;
    }

    /**
     * Creates a theme toggle button and adds a listener to update the logo when theme changes
     *
     * @return the theme toggle button
     */
    private ThemeToggle createThemeToggle() {
        ThemeToggle themeToggle = new ThemeToggle(themeSessionState);
        themeToggle.setId(ID_THEME_TOGGLE);

        // Add click listener to update logo and user avatar when theme is toggled
        themeToggle.addClickListener(event -> {
            UI      ui          = UI.getCurrent();
            boolean isDarkTheme = ui.getElement().getThemeList().contains(Lumo.DARK);
            updateLogoBasedOnTheme(isDarkTheme);
            updateUserAvatarBasedOnTheme(isDarkTheme);
        });

        return themeToggle;
    }

    private Component createUserMenu() {
        final String userEmail = SecurityUtils.getUserEmail();

        User userFromDb = null;
        // Try to get user from database to check for avatar
        if (!userEmail.equals(SecurityUtils.GUEST)) {
            try {
                userFromDb = userApi.getByEmail(userEmail).get();
            } catch (Exception e) {
                // User not found or error, will use default avatar
                userFromDb = null;
            }
        }
        final User user = userFromDb;

        // Create avatar component
        Component avatarComponent;
        if (user != null) {
            // User has a custom avatar image - use URL-based loading
            userAvatarImage = new com.vaadin.flow.component.html.Image();
            userAvatarImage.setWidth("24px");
            userAvatarImage.setHeight("24px");
            userAvatarImage.getStyle()
                    .set("border-radius", "4px")
                    .set("margin-right", "var(--lumo-space-s)")
                    .set("object-fit", "cover");

            // Use REST API endpoint for avatar with hash-based caching; theme-aware
            boolean isDark = UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
            userAvatarImage.setSrc(user.getAvatarUrl(isDark));
            avatarComponent = userAvatarImage;
        } else {
            userAvatarImage = null;
            // Use default avatar
            var avatar = new Avatar(userEmail);
            avatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
            avatar.setColorIndex(5);
            avatarComponent = avatar;
        }

        var userMenu = new MenuBar();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        userMenu.addClassNames(Margin.Right.MEDIUM);
        userMenu.setId(ID_USER_MENU);

        var userMenuItem = userMenu.addItem(avatarComponent);
        userMenuItem.add(userEmail);
//        userMenuItem.setId(ID_USER_MENU_ITEM);

        var availabilityItem = userMenuItem.getSubMenu().addItem("Manage Availability", e -> navigateToAvailability(userEmail));
        availabilityItem.setId(ID_USER_MENU_AVAILABILITY);

        var locationItem = userMenuItem.getSubMenu().addItem("Manage Location", e -> navigateToLocation(userEmail));
        locationItem.setId(ID_USER_MENU_LOCATION);

        var offDaysItem = userMenuItem.getSubMenu().addItem("Manage Off Days", e -> navigateToOffDays(userEmail));
        offDaysItem.setId(ID_USER_MENU_OFF_DAYS);

        var workWeekItem = userMenuItem.getSubMenu().addItem("Manage Work Week", e -> navigateToUserWorkWeek(userEmail));
        workWeekItem.setId(ID_USER_MENU_WORK_WEEK);

        var viewProfileItem = userMenuItem.getSubMenu().addItem("View Profile", e -> navigateToProfile(userEmail));
        viewProfileItem.setId(ID_USER_MENU_VIEW_PROFILE);

        // Add "Manage User Groups" and "Manage Users" menu items for admins only
        if (SecurityUtils.isAdmin()) {
            userMenuItem.getSubMenu().addSeparator();

            userMenuItem.getSubMenu().addItem("Identity Providers",
                    e -> UI.getCurrent().navigate(OidcProviderManagementView.class));

            var manageUserGroupsItem = userMenuItem.getSubMenu().addItem("Manage User Groups", e -> navigateToUserGroups());
            manageUserGroupsItem.setId(ID_USER_MENU_MANAGE_USER_GROUPS);

            var manageUsersItem = userMenuItem.getSubMenu().addItem("Manage Users", e -> navigateToUsers());
            manageUsersItem.setId(ID_USER_MENU_MANAGE_USERS);

            var manageWorkWeeksItem = userMenuItem.getSubMenu().addItem("Manage Work Weeks", e -> navigateToWorkWeeks());
            manageWorkWeeksItem.setId(ID_USER_MENU_MANAGE_WORK_WEEKS);
        }

        userMenuItem.getSubMenu().addSeparator();
        var manageSettingsItem = userMenuItem.getSubMenu().addItem("Manage Settings");
        manageSettingsItem.setEnabled(false);
        manageSettingsItem.setId(ID_USER_MENU_MANAGE_SETTINGS);

        var aboutItem = userMenuItem.getSubMenu().addItem("About", e -> UI.getCurrent().navigate(AboutView.class));
        aboutItem.setId(ID_USER_MENU_ABOUT);

        var logoutItem = userMenuItem.getSubMenu().addItem("Logout", e -> logout());
        logoutItem.setId(ID_USER_MENU_LOGOUT);

        return userMenu;
    }

    /**
     * Finds this layout from a routed view, including when an intermediate layout component is present.
     *
     * @param component routed view or one of its child components
     * @return the enclosing main layout, when attached
     */
    public static Optional<Component> findParent(Component component) {
        Component parent = component.getParent().orElse(null);
        while (parent != null) {
            if (parent instanceof MainLayout) {
                return Optional.of(parent);
            }
            parent = parent.getParent().orElse(null);
        }
        return Optional.empty();
    }

    /**
     * Gets the tab ID constant for a given menu title.
     * This allows proper identification of tabs in UI tests.
     *
     * @param title the menu title
     * @return the tab ID constant, or null if not found
     */
    private String getTabIdForTitle(String title) {
        return ID_TAB_BASE + title.toLowerCase();
    }

    private void logout() {
        // Redirect to Spring Security's /logout endpoint so that the full logout filter chain runs,
        // including OidcClientInitiatedLogoutSuccessHandler, which calls Keycloak's end_session_endpoint
        // and invalidates the OIDC session.  A plain SecurityContextLogoutHandler only clears the local
        // Spring session; the Keycloak session stays alive and the user would be silently re-authenticated
        // the moment they visit the login page again.
        getUI().ifPresent(ui -> ui.getPage().setLocation("/logout"));
    }

    private void navigateToAvailability(String userEmail) {
        getUI().ifPresent(ui -> ui.navigate("availability/" + userEmail));
    }

    private void navigateToLocation(String userEmail) {
        getUI().ifPresent(ui -> ui.navigate("location/" + userEmail));
    }

    private void navigateToOffDays(String userEmail) {
        getUI().ifPresent(ui -> ui.navigate("offday/" + userEmail));
    }

    private void navigateToProfile(String userEmail) {
        getUI().ifPresent(ui -> ui.navigate("profile/" + userEmail));
    }

    private void navigateToUserGroups() {
        getUI().ifPresent(ui -> ui.navigate("user-group-list"));
    }

    private void navigateToUserWorkWeek(String userEmail) {
        getUI().ifPresent(ui -> ui.navigate("user-work-week/" + userEmail));
    }

    private void navigateToUsers() {
        getUI().ifPresent(ui -> ui.navigate("user-list"));
    }

    private void navigateToWorkWeeks() {
        getUI().ifPresent(ui -> ui.navigate("work-week-list"));
    }

    private void openHistoryDrawer() {
        undoHistoryPanel.refresh();
        historyPane.getStyle().set("width", "35%").set("min-width", "420px").set("max-width", "560px");
        contentSplit.setSplitterPosition(65);
        historyPaneOpen = true;
    }

    /**
     * Invalidates the global planning history after a planning change.
     */
    public void refreshUndoRedoToolbar() {
        undoHistoryPanel.refresh();
    }

    /**
     * Sets the product whose history is controlled by the global undo/redo buttons.
     *
     * @param productId active product ID, or {@code null} when the current view has no product context
     */
    public void setActiveProductId(UUID productId) {
        setActiveProductIds(productId == null ? java.util.List.of() : java.util.List.of(productId));
    }

    /**
     * Sets the products whose histories are controlled by the global undo/redo buttons.
     *
     * @param productIds active product IDs, or an empty collection when the current view has no product context
     */
    public void setActiveProductIds(Collection<UUID> productIds) {
        activeProductIds.clear();
        activeProductIds.addAll(productIds);
    }

    /**
     * Shows or hides the breadcrumb bar below the main navigation.
     * Called by views that do not need breadcrumb context (e.g. {@link de.bushnaq.abdalla.kassandra.ui.view.AboutView}).
     *
     * @param visible {@code true} to show the bar, {@code false} to hide it
     */
    public void setBreadcrumbsVisible(boolean visible) {
        if (breadcrumbContainer != null) {
            breadcrumbContainer.setVisible(visible);
        }
    }

    /**
     * Places routed view content beside the collapsible global planning-history pane.
     *
     * @param content routed view content
     */
    @Override
    public void showRouterLayoutContent(HasElement content) {
        Component target = content == null ? null
                : content.getElement().getComponent()
                .orElseThrow(() -> new IllegalArgumentException("AppLayout content must be a Component"));
        contentSplit = new SplitLayout(target, historyPane);
        contentSplit.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        contentSplit.setSizeFull();
        closeHistoryDrawer();
        setContent(contentSplit);
    }

    private void toggleHistoryDrawer() {
        if (historyPaneOpen) {
            closeHistoryDrawer();
        } else {
            openHistoryDrawer();
        }
    }

    /**
     * Updates the logo source based on the theme
     *
     * @param isDarkTheme true if dark theme is active, false otherwise
     */
    private void updateLogoBasedOnTheme(boolean isDarkTheme) {
        if (logoImage != null) {
            if (isDarkTheme) {
                logoImage.setSrc("images/logo-dark.svg");
            } else {
                logoImage.setSrc("images/logo.svg");
            }
        }
    }

    /**
     * Updates the user-menu avatar source to the light or dark variant after a theme toggle.
     * No-op when no user is logged in or the user has no custom avatar.
     *
     * @param isDarkTheme true if dark theme is now active, false otherwise
     */
    private void updateUserAvatarBasedOnTheme(boolean isDarkTheme) {
        if (userAvatarImage == null) {
            return;
        }
        final String userEmail = SecurityUtils.getUserEmail();
        if (userEmail.equals(SecurityUtils.GUEST)) {
            return;
        }
        try {
            userApi.getByEmail(userEmail).ifPresent(u -> userAvatarImage.setSrc(u.getAvatarUrl(isDarkTheme)));
        } catch (Exception e) {
            log.debug("Could not refresh user avatar after theme toggle: {}", e.getMessage());
        }
    }
}
