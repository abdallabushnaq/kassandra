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

package de.bushnaq.abdalla.kassandra.ui.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.*;
import de.bushnaq.abdalla.kassandra.ai.AiFilterService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.rest.api.FeatureApi;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import de.bushnaq.abdalla.kassandra.rest.api.VersionApi;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@Route("test-list")
@PageTitle("Test List Page")
@PermitAll // When security is enabled, allow all authenticated users
@RolesAllowed({"USER", "ADMIN"}) // Restrict access to users with specific roles
@Menu(order = 0, icon = "vaadin:calendar", title = "TestListView")
@Slf4j
public class TestListView extends Main implements AfterNavigationObserver {
    public static final String                 CREATE_FEATURE_BUTTON_ID          = "create-feature-button";
    public static final String                 FEATURE_GLOBAL_FILTER             = "feature-global-filter";
    public static final String                 FEATURE_GRID                      = "feature-grid";
    public static final String                 FEATURE_GRID_DELETE_BUTTON_PREFIX = "feature-grid-delete-button-prefix-";
    public static final String                 FEATURE_GRID_EDIT_BUTTON_PREFIX   = "feature-grid-edit-button-prefix-";
    public static final String                 FEATURE_GRID_NAME_PREFIX          = "feature-grid-name-";
    public static final String                 FEATURE_LIST_PAGE_TITLE           = "feature-list-page-title";
    public static final String                 FEATURE_ROW_COUNTER               = "feature-row-counter";
    private final       FeatureApi             featureApi;
    private final       ProductApi             productApi;
    private             Long                   productId;
    private final       StableDiffusionService stableDiffusionService;
    private final       VersionApi             versionApi;
    private             Long                   versionId;

    public TestListView(FeatureApi featureApi, ProductApi productApi, VersionApi versionApi, Clock clock, AiFilterService aiFilterService, ObjectMapper mapper, StableDiffusionService stableDiffusionService) {
        this.featureApi             = featureApi;
        this.productApi             = productApi;
        this.versionApi             = versionApi;
        this.stableDiffusionService = stableDiffusionService;

    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        log.info("== AfterNavigationEvent start");
        //- Get query parameters
        Location        location        = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        if (queryParameters.getParameters().containsKey("product")) {
            this.productId = Long.parseLong(queryParameters.getParameters().get("product").getFirst());
        }
        if (queryParameters.getParameters().containsKey("version")) {
            this.versionId = Long.parseLong(queryParameters.getParameters().get("version").getFirst());
        }
        //- update breadcrumbs

//        refreshGrid();
        log.info("== AfterNavigationEvent end");
    }


}
