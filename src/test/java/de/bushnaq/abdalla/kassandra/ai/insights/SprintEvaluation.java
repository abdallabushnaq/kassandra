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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.ai.insights;

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.service.SickLeaveImpactContributor;

import java.util.Map;

/**
 * Holds the sprint, its sick-leave impact, and tasks indexed by task key for simulation comparisons.
 *
 * @param sprint    evaluated sprint
 * @param impact    sick-leave impact calculated for the sprint
 * @param tasksByKey sprint tasks indexed by their stable task keys
 */
public record SprintEvaluation(Sprint sprint, SickLeaveImpactContributor.SprintImpact impact,
                               Map<String, Task> tasksByKey) {
}
