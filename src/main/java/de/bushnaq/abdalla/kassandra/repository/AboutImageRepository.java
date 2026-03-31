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

package de.bushnaq.abdalla.kassandra.repository;

import de.bushnaq.abdalla.kassandra.dao.AboutImageDAO;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Spring Data repository for {@link AboutImageDAO}.
 * The table holds a single row ({@code id = 1}); only {@code findById} and {@code save} are needed.
 */
public interface AboutImageRepository extends ListCrudRepository<AboutImageDAO, Long> {
}

