// File: 'src/main/java/de/bushnaq/abdalla/kassandra/ui/view/GridRowReordering.java'
package de.bushnaq.abdalla.kassandra.ui.view;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Route("grid-row-reordering")
@AnonymousAllowed
@Log4j2
@Deprecated
@JsModule("./styles/vaadin-grid-styles.js")
public class GridRowReordering extends Div {

    private User draggedItem;

    public GridRowReordering(UserApi userApi) {
        addClassName("jira-backlog-view");
        setSizeFull();

        Grid<User> grid = setupGrid();

        List<User>             people   = new ArrayList<>(userApi.getAll());
        GridListDataView<User> dataView = grid.setItems(people);

        grid.setRowsDraggable(true);

        grid.addDragStartListener(e -> {
            draggedItem = e.getDraggedItems().get(0);
            grid.setDropMode(GridDropMode.BETWEEN);
        });

        // Keep your existing drop filter/logic if needed
        grid.setDropFilter(e -> false);

        grid.addDropListener(e -> {
            User             targetPerson = e.getDropTargetItem().orElse(null);
            GridDropLocation dropLocation = e.getDropLocation();

            log.warn("DropListener: draggedItem={}, targetPerson={}, dropLocation={}", draggedItem, targetPerson, dropLocation);
            boolean personWasDroppedOntoItself = draggedItem != null && draggedItem.equals(targetPerson);
            if (targetPerson == null || personWasDroppedOntoItself) return;

            dataView.removeItem(draggedItem);
            if (dropLocation == GridDropLocation.BELOW) {
                dataView.addItemAfter(draggedItem, targetPerson);
            } else {
                dataView.addItemBefore(draggedItem, targetPerson);
            }
        });

        grid.addDragEndListener(e -> {
            draggedItem = null;
            grid.setDropMode(null);
        });

        add(grid);
    }

    private static Grid<User> setupGrid() {
        Grid<User> grid = new Grid<>(User.class, false);
        grid.addClassName("jira-backlog");
        grid.setHeightFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);

        // Keep multiple columns
        grid.addColumn(User::getName).setHeader("First name").setAutoWidth(true);
        grid.addColumn(User::getEmail).setHeader("Last name").setAutoWidth(true);
        grid.addColumn(User::getEmail).setHeader("Email").setAutoWidth(true);

        return grid;
    }
}
