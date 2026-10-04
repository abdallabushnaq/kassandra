/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.Lumo;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.service.AboutBoxService;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.ThemeChangedEvent;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import jakarta.annotation.security.PermitAll;

import java.time.Year;

@Route(value = "", layout = MainLayout.class)
@PageTitle("About Kassandra")
@PermitAll
public class AboutView extends Main implements AfterNavigationObserver {

    /**
     * Vaadin component ID for the page title.
     */
    public static final String ABOUT_PAGE_TITLE = "about-page-title";
    /**
     * Vaadin component ID for this view.
     */
    public static final String ABOUT_VIEW       = "about-view";
    /**
     * Identifier of the Apache license link.
     */
    public static final String LICENSE_LINK_ID = "about-license-link";
    /**
     * Identifier of the project repository link.
     */
    public static final String GITHUB_LINK_ID = "about-github-link";

    /**
     * Route path constant.
     */
    public static final String ROUTE = "";

    private final AboutBoxService aboutBoxService;
    private final Image           bannerImage = new Image();
    private final Span            loadingText = new Span("Generating image…");
    private       int             imageRequest;
    private       Registration    themeChangedRegistration;

    /**
     * Constructs the About view; its themed image is loaded asynchronously on attach.
     *
     * @param aboutBoxService service that provides the cached SD image and version string
     */
    AboutView(AboutBoxService aboutBoxService) {
        this.aboutBoxService = aboutBoxService;
        setId(ABOUT_VIEW);

        // Full-height centred column
        setSizeFull();
        getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");

        VerticalLayout center = new VerticalLayout();
        center.setPadding(true);
        center.setSpacing(true);
        center.setAlignItems(FlexComponent.Alignment.CENTER);
        center.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        center.setWidth("100%");

        // Loading placeholder
        loadingText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        // Banner image – max 1024 px wide, hidden until the background thread delivers the bytes
        bannerImage.setMaxWidth("1024px");
        bannerImage.setWidth("100%");
        bannerImage.getStyle()
                .set("height", "auto")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("object-fit", "contain");
        bannerImage.setVisible(false);

        H2 appName = new H2("Kassandra");
        appName.setId(ABOUT_PAGE_TITLE);
        appName.getStyle().set("margin", "var(--lumo-space-s) 0 0 0");

        Span versionSpan = new Span("Version " + aboutBoxService.getVersion());
        versionSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        int startYear = 2025;
        int endYear   = Year.now().getValue();
        String copyrightText = (startYear == endYear)
                ? "© " + startYear + " Abdalla Bushnaq"
                : "© " + startYear + "–" + endYear + " Abdalla Bushnaq";
        Span copyright = new Span(copyrightText);
        copyright.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Anchor licenseLink = new Anchor("https://www.apache.org/licenses/LICENSE-2.0", "Apache License 2.0");
        licenseLink.setId(LICENSE_LINK_ID);
        licenseLink.setTarget("_blank");

        Anchor githubLink = new Anchor("https://github.com/abdallabushnaq/kassandra", "GitHub");
        githubLink.setId(GITHUB_LINK_ID);
        githubLink.setTarget("_blank");

        center.add(loadingText, bannerImage, appName, versionSpan, copyright, licenseLink, githubLink);
        add(center);
    }

    private void loadImage(UI ui, ETheme theme) {
        int request = ++imageRequest;
        loadingText.setVisible(true);
        bannerImage.setVisible(false);
        new Thread(() -> {
            byte[] imageBytes = aboutBoxService.getOrGenerateImage(theme);
            ui.accessLater(() -> {
                // A theme change or navigation may have superseded this background load.
                if (!isAttached() || request != imageRequest) {
                    return;
                }
                loadingText.setVisible(false);
                String imageUrl = VaadinUtil.dataUriFromBytes(imageBytes);
                if (imageUrl != null) {
                    bannerImage.setSrc(imageUrl);
                    bannerImage.setVisible(true);
                }
                ui.push();
            }, null).run();
        }, "about-view-image-loader").start();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        themeChangedRegistration = ComponentUtil.addListener(ui, ThemeChangedEvent.class,
                event -> loadImage(ui, event.getNewTheme()));
        loadImage(ui, ui.getElement().getThemeList().contains(Lumo.DARK) ? ETheme.dark : ETheme.light);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (themeChangedRegistration != null) {
            themeChangedRegistration.remove();
            themeChangedRegistration = null;
        }
        imageRequest++;
        super.onDetach(detachEvent);
    }

    /**
     * Clears the breadcrumb trail when navigating to this view.
     *
     * @param event the navigation event
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        MainLayout.findParent(this)
                .ifPresent(component -> {
                    if (component instanceof MainLayout layout) {
                        layout.getBreadcrumbs().clear();
                        layout.setBreadcrumbsVisible(false);
                        layout.clearTabSelection();
                    }
                });
    }
}
