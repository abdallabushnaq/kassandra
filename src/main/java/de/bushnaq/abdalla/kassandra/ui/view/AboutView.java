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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import de.bushnaq.abdalla.kassandra.service.AboutBoxService;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;
import java.time.Year;

@Route(value = "", layout = MainLayout.class)
@PageTitle("About Kassandra")
@PermitAll
public class AboutView extends Main implements AfterNavigationObserver {

    public static final String ABOUT_PAGE_TITLE = "about-page-title";
    /**
     * Vaadin component ID for this view.
     */
    public static final String ABOUT_VIEW       = "about-view";

    /**
     * Route path constant.
     */
    public static final String ROUTE = "";

    /**
     * Constructs the About view and starts the async image-loading thread.
     *
     * @param aboutBoxService service that provides the cached SD image and version string
     */
    AboutView(AboutBoxService aboutBoxService) {
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
        center.setMaxWidth("560px");
        center.setWidth("100%");

        // Loading placeholder
        Span loadingText = new Span("Generating image…");
        loadingText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        // Banner image – max 512 px wide, hidden until the background thread delivers the bytes
        com.vaadin.flow.component.html.Image bannerImage = new com.vaadin.flow.component.html.Image();
        bannerImage.setMaxWidth("512px");
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
        licenseLink.setTarget("_blank");

        Anchor githubLink = new Anchor("https://github.com/abdallabushnaq/kassandra", "GitHub");
        githubLink.setTarget("_blank");

        center.add(loadingText, bannerImage, appName, versionSpan, copyright, licenseLink, githubLink);
        add(center);

        // Async image load — capture UI reference while still on the Vaadin thread
        UI ui = UI.getCurrent();
        if (ui != null) {
            new Thread(() -> {
                byte[] imageBytes = aboutBoxService.getOrGenerateImage();
                ui.access(() -> {
                    loadingText.setVisible(false);
                    if (imageBytes != null && imageBytes.length > 0) {
                        StreamResource resource = new StreamResource(
                                "kassandra-about.png",
                                () -> new ByteArrayInputStream(imageBytes));
                        bannerImage.setSrc(resource);
                        bannerImage.setVisible(true);
                    }
                    ui.push();
                });
            }, "about-view-image-loader").start();
        }
    }

    /**
     * Clears the breadcrumb trail when navigating to this view.
     *
     * @param event the navigation event
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getElement().getParent().getComponent()
                .ifPresent(component -> {
                    if (component instanceof MainLayout layout) {
                        layout.getBreadcrumbs().clear();
                        layout.setBreadcrumbsVisible(false);
                        layout.clearTabSelection();
                    }
                });
    }
}

