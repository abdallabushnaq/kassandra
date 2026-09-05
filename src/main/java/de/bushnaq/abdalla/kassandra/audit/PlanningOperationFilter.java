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

package de.bushnaq.abdalla.kassandra.audit;

import de.bushnaq.abdalla.kassandra.rest.api.PlanningOperationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Associates a UI operation header with the current server request.
 */
@Component
public class PlanningOperationFilter extends OncePerRequestFilter {

    /**
     * Binds the supplied operation ID only for the lifetime of the request.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param filterChain remaining filter chain
     * @throws ServletException when filter processing fails
     * @throws IOException when response processing fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String operationId = request.getHeader(PlanningOperationContext.HEADER_NAME);
        try {
            if (operationId != null && !operationId.isBlank()) {
                AuditOperationContextHolder.setOperationId(UUID.fromString(operationId));
            }
            filterChain.doFilter(request, response);
        } finally {
            AuditOperationContextHolder.clear();
        }
    }
}
