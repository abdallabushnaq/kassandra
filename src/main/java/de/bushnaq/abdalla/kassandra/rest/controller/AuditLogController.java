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
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package de.bushnaq.abdalla.kassandra.rest.controller;

import de.bushnaq.abdalla.kassandra.dto.AuditPage;
import de.bushnaq.abdalla.kassandra.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Administrator-only audit history endpoint.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {
    @Autowired
    private AuditLogService auditLogService;

    /**
     * Returns one filtered page of changes.
     *
     * @param user   actor name or email
     * @param action change action
     * @param search free-text search
     * @param from   inclusive timestamp
     * @param to     exclusive timestamp
     * @param page   zero-based page
     * @param size   page size
     * @return matching changes and total count
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AuditPage list(@RequestParam(required = false) String user, @RequestParam(required = false) String action,
                          @RequestParam(required = false) String search, @RequestParam(required = false) Instant from,
                          @RequestParam(required = false) Instant to, @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "50") int size) {
        return auditLogService.find(user, action, search, from, to, page, size);
    }
}
