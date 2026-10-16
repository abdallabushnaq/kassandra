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

**Server Settings** manages persisted configuration for the whole server.
Administrator changes here affect other users, not just the current browser.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Choose a settings category

![General category](images/help/light/ServerSettingsView/server-settings-category-general.png)

The left-hand category list selects the settings shown on the right. Categories
include general settings, AI models, service connections and logging. Read each
setting's description and any restart notice rather than guessing from its key.

![AI model category](images/help/light/ServerSettingsView/server-settings-category-ai-models.png)

AI model choices and service connections are separate: selecting a model does
not by itself make an unavailable service reachable.

## Save one setting at a time

![History limit field](images/help/light/ServerSettingsView/server-setting-field-kassandra.undo-redo.history-limit.png)

Enter or select the setting's new value using its offered editor.

![Save setting](images/help/light/ServerSettingsView/server-setting-save-kassandra.undo-redo.history-limit.png)

Each row has its own **Save**. There is no page-wide Save; saving refreshes the
form, so save a setting before editing another row or leaving the category.
Unsaved input in other editors can be lost during a refresh.

Most changes apply immediately. If the setting or success notification says
**restart required**, restart Kassandra before expecting the new behavior.
Invalid values produce a notification; inspect it rather than assuming a save
succeeded. Planning undo/redo does not reverse server configuration changes.

## Enable optional services and test connections

![LM Studio category](images/help/light/ServerSettingsView/server-settings-category-lm-studio.png)

![Enable LM Studio](images/help/light/ServerSettingsView/server-settings-category-enabled-lm-studio.png)

Optional service categories have an enable checkbox. Toggling it saves
immediately and refreshes the form. Disabled categories retain their settings,
but their editors and actions are disabled.

![Stable Diffusion enablement](images/help/light/ServerSettingsView/server-settings-category-enabled-stable-diffusion.png)

Stable Diffusion availability is separate from the chat/filter service.
Disabling a service intentionally makes its dependent features unavailable.

![Service URL](images/help/light/ServerSettingsView/server-setting-field-kassandra.lm-studio.api-url.png)

![Test connection](images/help/light/ServerSettingsView/server-setting-test-kassandra.lm-studio.api-url.png)

**Test connection** checks the supported connection using the current field
value; it is not Save. Read the result, then Save separately when you want to
persist an edited value. Not every setting offers a test.

## Handle stored secrets

![Secret editor](images/help/light/ServerSettingsView/server-setting-field-kassandra.openai.api-key.png)

Secret editors do not reveal stored values. A configured secret can be retained
by leaving its replacement field empty. Enter a replacement and Save to change
it. **Clear stored secret** appears only when a stored value exists and removes
it explicitly; an empty replacement is not the same action.

Do not paste secrets into help, chat, screenshots or bug reports. A hidden
secret field is not proof that no value is configured.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Settings help](images/help/light/ServerSettingsView/server-settings-help-button.png)

The heading's question mark opens this help and remains available after saves
and category refreshes. Access remains administrator-only.
