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

package de.bushnaq.abdalla.kassandra.report.dao;

import de.bushnaq.abdalla.kassandra.dto.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConflictingTasks {
    private final Map<Task, List<Conflict>> conflictings = new HashMap<>();

    public void add(Task task, Conflict conflict) {
        List<Conflict> list = conflictings.get(task);
        if (list == null) {
            list = new ArrayList<>();
            conflictings.put(task, list);
        }
        list.add(conflict);
    }

    public List<Conflict> get(Task task) {
        return conflictings.get(task);
    }
}
