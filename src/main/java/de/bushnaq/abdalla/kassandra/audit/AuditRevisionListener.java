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

import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import org.hibernate.envers.RevisionListener;

/**
 * Populates Envers revision metadata from the current authenticated request.
 */
public class AuditRevisionListener implements RevisionListener {

    /**
     * Populates the revision entity before Envers persists it.
     *
     * @param revisionEntity the custom Envers revision entity
     */
    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionEntity auditRevision = (AuditRevisionEntity) revisionEntity;
        auditRevision.setActor(SecurityUtils.getUserEmail());
        auditRevision.setOperationId(AuditOperationContextHolder.getOperationId());
    }
}
