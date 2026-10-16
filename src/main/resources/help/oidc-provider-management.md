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

**Identity Providers** lets administrators configure trusted OIDC sign-in
providers. This controls authentication, not product access or application roles.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Add a sign-in provider

![Provider name](images/help/light/OidcProviderManagementView/identity-provider-name.png)

Choose a recognizable display name for users choosing how to sign in.

![Issuer](images/help/light/OidcProviderManagementView/identity-provider-issuer.png)

Enter the provider's **Issuer URI**, not its sign-in screen URL. Use the issuer
advertised by its OIDC configuration.

![Client ID](images/help/light/OidcProviderManagementView/identity-provider-client-id.png)

Enter the client ID registered for Kassandra at that provider.

![Client secret](images/help/light/OidcProviderManagementView/identity-provider-client-secret.png)

Enter the client secret required by that client registration. Stored client
secrets are encrypted and are never displayed again.

![Scopes](images/help/light/OidcProviderManagementView/identity-provider-scopes.png)

Scopes are comma-separated; the form starts with **openid,profile,email**.
Check the provider's client and redirect configuration as well as these fields.

![Add provider](images/help/light/OidcProviderManagementView/identity-provider-add-button.png)

**Add provider** saves the configuration and refreshes the list. Inspect any
validation or discovery error before retrying. This page's current form adds
providers; it does not load existing rows into an edit form.

## Enable, disable or remove a provider

![Configured providers](images/help/light/OidcProviderManagementView/identity-providers-grid.png)

Check Name, Issuer and Status before acting on a row.

![Provider state](images/help/light/OidcProviderManagementView/identity-provider-state-.png)

**Disable** removes that provider from the enabled sign-in choices without
deleting its configuration. **Enable** makes it available again. The action
saves immediately.

![Delete provider](images/help/light/OidcProviderManagementView/identity-provider-delete-.png)

**Delete acts immediately; there is no confirmation dialog here.** Removing a
provider can disrupt sign-in for users linked to it. Verify an alternative
administrative sign-in/recovery path before disabling or deleting providers.

Provider changes are not recoverable through planning history. Do not delete
and recreate a provider simply to troubleshoot a user's product permissions;
application users, provider identities and product grants are separate concepts.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Identity Providers help](images/help/light/OidcProviderManagementView/identity-provider-help-button.png)

The heading's question mark opens this help. Access remains administrator-only.
