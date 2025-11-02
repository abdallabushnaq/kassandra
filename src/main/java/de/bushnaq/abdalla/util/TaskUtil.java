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

package de.bushnaq.abdalla.util;

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.report.dao.MetaData;

import java.util.HashMap;


public class TaskUtil {

    public static HashMap<Task, MetaData> createMetaData(Sprint projectFile) {
        //TODO fix creating metadata
//        ProjectProperties properties = projectFile.getProjectProperties();
//        if (properties.getCustomProperties() == null || properties.getCustomProperties().get(MetaData.METADATA) == null) {
//            Map<String, Object>     customProperties = new HashMap<>();
//            HashMap<Task, MetaData> mdMap            = new HashMap<>();
//            customProperties.put(MetaData.METADATA, mdMap);
//            properties.setCustomProperties(customProperties);
//            return mdMap;
//        } else {
//            return (HashMap<Task, MetaData>) projectFile.getProjectProperties().getCustomProperties().get(MetaData.METADATA);
//        }
        return null;
    }

    public static MetaData getTaskMetaData(Task task) {
//        HashMap<Task, MetaData> mdMap = createMetaData(task.getParentFile());
//        return mdMap.get(task);
        return null;
    }

    public static void putTaskMetaData(Task task, MetaData md) {
//        HashMap<Task, MetaData> mdMap = createMetaData(task.getParentFile());
//        mdMap.put(task, md);
    }
}
