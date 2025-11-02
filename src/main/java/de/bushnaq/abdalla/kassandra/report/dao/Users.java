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

import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Users /*implements Iterable<User>*/ {
    protected Map<String, User> emailMap = new HashMap<>();
    protected Map<Long, User>   idMap    = new HashMap<>();
    protected List<User>        list     = new ArrayList<>();
    protected Map<String, User> nameMap  = new HashMap<>();

    public void add(User user) {
        list.add(user);
        emailMap.put(user.getEmail(), user);
        nameMap.put(user.getName(), user);
        idMap.put(user.getId(), user);
//        for (User user : list) {
//            map.put(login, user);
//        }
    }

//    @Override
//    public Iterator<User> iterator() {
//        return list.iterator();
//    }

//    public int size() {
//        return list.size();
//    }
}
