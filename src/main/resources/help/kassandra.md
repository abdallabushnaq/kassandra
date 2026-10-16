<!--
Copyright (C) 2025-2026 Abdalla Bushnaq
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

**Kassandra** is the administrator's standalone AI assistant. Unlike the AI pane
on a planning page, this conversation has no selected-product/version creation
context. State the intended work explicitly.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Ask about the intended work

![Query input](images/help/light/Kassandra/ai-query-input.png)

Enter a question or request. Specify product, version, feature and sprint when
those determine the target; similarly named work can exist in different places.
Do not include passwords, access tokens or client secrets.

![Send request](images/help/light/Kassandra/ai-submit-button.png)

**Send** or **Enter** submits the query. **Ctrl+Enter** inserts a new line.
The assistant requires configured, available AI services. Opening the page or
writing a draft does not submit a request.

Requests can invoke tools and change saved data with your permissions; do not
assume every request is read-only or that an additional confirmation is always
required. Review the request before sending.

## Read replies and manage the conversation

![Response area](images/help/light/Kassandra/ai-response-area.png)

The response area shows conversation messages and tool activity. Check the
result against the actual planning pages when a change matters. After an error,
inspect saved data before repeating a request that could already have applied
some operations.

![Clear conversation](images/help/light/Kassandra/ai-clear-button.png)

**Clear** starts a fresh conversation. It does not undo tool actions or delete
the work they created. Returning to this route can restore its session
conversation; it is separate from the contextual conversations on other pages.

The left-hand **Audit Log** area is currently a placeholder, not a live audit
viewer. Use **Audit** from the account menu to inspect recorded changes, and
planning history on supported pages to recover recorded planning operations.

## Troubleshoot unavailable AI

If a request fails, inspect its displayed error and ask an administrator to
check the configured service, model and connection. Do not change server
credentials in a chat message. Help itself is bundled locally and does not
require an AI query.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Kassandra help](images/help/light/Kassandra/kassandra-help-button.png)

The compact **Kassandra** header's question mark opens this help without
resetting the conversation or submitting the draft.
