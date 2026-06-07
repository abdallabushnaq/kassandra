package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "overview-v2", layout = MainLayout.class)
@PermitAll
@RolesAllowed({"USER", "ADMIN"})
public class SprintsOverviewViewV2 extends Div {

    public SprintsOverviewViewV2() {
        setWidthFull();
        setId("sprints-overview-v2-container");
        getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");

        // Fallback: show existing server-side generated chart SVG (will be replaced by JS when ready)
//        Image fallback = new Image("/ui/report/sprints-overview.svg", "Sprints overview (static)");
//        fallback.setId("sprints-overview-fallback");
//        fallback.getStyle().set("max-width", "100%").set("height", "auto");
//        add(fallback);

        // Load client script that will fetch /api/overview/sprints and render interactive chart
        UI.getCurrent().getPage().addJavaScript("/js/sprints-overview-v2.js");
    }
}

