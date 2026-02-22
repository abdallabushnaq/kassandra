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

package de.bushnaq.abdalla.kassandra.ui.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Session-scoped bean that survives F5 browser refreshes within the same HTTP session.
 * Stores the AI chat panel's conversation ID and a capped snapshot of displayed messages
 * so they can be replayed into the UI after a reload.
 */
@Component
@SessionScope
@Getter
@Setter
public class ChatPanelSessionState {

    /**
     * Reused across reloads so server-side ChatMemory is not abandoned.
     */
    private       String                  conversationId = UUID.randomUUID().toString();
    /**
     * The route key of the last view that used this session state (including any
     * context-discriminating URL parameters). Used to detect navigation changes
     * on F5 (same-page reload after a route change) and within-view param changes.
     */
    private       String                  lastViewRoute  = null;
    /**
     * Snapshot of all rendered messages (user, ai, system, tool, error), capped so that
     * the number of user+ai entries never exceeds MAX_MESSAGES.
     */
    private final List<ChatMessageRecord> messages       = new ArrayList<>();
    /**
     * Whether the AI chat panel is open. Shared across all views so navigating
     * between pages preserves the open/closed state.
     */
    private       boolean                 panelOpen      = false;

    /**
     * Clears history and assigns a fresh conversation ID.
     * Must be called on the request thread only.
     */
    public void reset(String newConversationId) {
        messages.clear();
        this.conversationId = newConversationId;
        this.lastViewRoute  = null;
    }

    /**
     * Simple record representing a single rendered chat message.
     *
     * @param role one of "user", "ai", "system", "error"
     * @param text the message text
     */
    public record ChatMessageRecord(String role, String text) {
    }
}

