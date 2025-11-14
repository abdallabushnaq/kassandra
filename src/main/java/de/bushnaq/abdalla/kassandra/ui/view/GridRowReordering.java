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

import com.vaadin.flow.component.grid.Grid;
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
public class GridRowReordering extends Div {

    private User draggedItem;

    public GridRowReordering(UserApi userApi) {
        Grid<User> grid = setupGrid();

        // Modifying the data view requires a mutable collection
        List<User>             people   = new ArrayList<>(userApi.getAll());
        GridListDataView<User> dataView = grid.setItems(people);

        grid.setRowsDraggable(true);

        grid.addDragStartListener(e -> {
            draggedItem = e.getDraggedItems().get(0);
            grid.setDropMode(GridDropMode.BETWEEN);
        });

        grid.setDropFilter(e -> {
            return false;
        });
        grid.addDropListener(e -> {
            User             targetPerson = e.getDropTargetItem().orElse(null);
            GridDropLocation dropLocation = e.getDropLocation();

            log.warn("DropListener: draggedItem={}, targetPerson={}, dropLocation={}", draggedItem, targetPerson, dropLocation);
            boolean personWasDroppedOntoItself = draggedItem
                    .equals(targetPerson);

            if (targetPerson == null || personWasDroppedOntoItself)
                return;

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
        grid.addColumn(User::getName).setHeader("First name");
        grid.addColumn(User::getEmail).setHeader("Last name");
        grid.addColumn(User::getEmail).setHeader("Email");

        return grid;
    }

}
