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

import java.util.*;

/**
 * Session-scoped bean that survives F5 browser refreshes within the same HTTP session.
 * Stores a per-route-key conversation slot (conversationId + message snapshot) so that
 * each page retains its own independent chat history. Navigating away and back restores
 * the exact conversation the user left.
 */
@Component
@SessionScope
@Getter
@Setter
public class ChatPanelSessionState {

    /**
     * Whether the AI chat panel is open. Shared across all views so navigating
     * between pages preserves the open/closed state.
     */
    private boolean panelOpen = false;

    /**
     * One slot per route key. Each slot owns its conversationId (used by the
     * server-side Spring AI ChatMemory) and the message snapshot for UI replay.
     */
    private final Map<String, ConversationSlot> slots = new LinkedHashMap<>();

    /**
     * Returns the existing slot for the given route key, or creates a fresh one.
     */
    public ConversationSlot getOrCreateSlot(String routeKey) {
        return slots.computeIfAbsent(routeKey, k -> new ConversationSlot());
    }

    /**
     * Removes the slot for the given route key.
     * Call this when the user explicitly clears a conversation.
     */
    public void removeSlot(String routeKey) {
        slots.remove(routeKey);
    }

    /**
     * Simple record representing a single rendered chat message.
     *
     * @param role one of "user", "ai", "system", "error"
     * @param text the message text
     */
    public record ChatMessageRecord(String role, String text) {
    }

    /**
     * Holds the server-side conversationId and the UI message snapshot for one page.
     */
    public static class ConversationSlot {
        /**
         * Reused across reloads so server-side ChatMemory is not abandoned.
         */
        public       String                  conversationId = UUID.randomUUID().toString();
        /**
         * Snapshot of all rendered messages (user, ai, system, tool, error), capped so that
         * the number of user+ai entries never exceeds MAX_MESSAGES.
         */
        public final List<ChatMessageRecord> messages       = new ArrayList<>();
    }
}
