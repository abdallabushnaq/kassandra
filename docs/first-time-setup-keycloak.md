# First-Time Setup with the Test Keycloak Realm

This guide starts Kassandra with the Keycloak realm used by the UI tests. It is intended for local development and
evaluation only: the imported realm contains public test credentials and the Keycloak administrator password shown
below.

## Prerequisites

- Docker Desktop
- Java 25 and Maven
- A clone of this repository

Run all commands from the repository root.

## 1. Start Keycloak with the test realm

The UI tests use Keycloak `26.5.6` and import
`src/test/resources/keycloak/project-hub-realm-realm.json`. Start the same configuration locally:

```powershell
docker run --rm --name kassandra-test-keycloak `
  -p 8081:8080 `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin `
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin `
  -v "${PWD}\src\test\resources\keycloak\project-hub-realm-realm.json:/opt/keycloak/data/import/project-hub-realm-realm.json" `
  quay.io/keycloak/keycloak:26.5.6 start-dev --import-realm
```

Wait until Keycloak reports that it has started. Its administration console is then available at
`http://localhost:8081/admin/`; use `admin` / `admin` only for this local test instance.

The imported realm is `project-hub-realm`. Its Kassandra client configuration is:

| Setting | Value |
| --- | --- |
| Issuer URI | `http://localhost:8081/realms/project-hub-realm` |
| Client ID | `kassandra-client` |
| Client secret | `test-client-secret` |
| Redirect URI | `http://localhost:8080/*` |
| Scopes | `openid,profile,email` |

## 2. Supply Kassandra bootstrap secrets

The setup wizard requires two deployment-provided secrets. Generate a 256-bit encryption key once, then choose a
long, random setup token:

```powershell
$env:KASSANDRA_SECURITY_ENCRYPTION_KEY = [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
$env:KASSANDRA_SETUP_TOKEN = 'replace-this-with-a-long-random-token'
```

Persist the encryption key in a secret manager before using this outside local development. Losing or replacing it
prevents Kassandra from decrypting stored OIDC client secrets. Do not put either value in `application.properties`.

## 3. Start Kassandra

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080/ui/setup`.

1. Enter `KASSANDRA_SETUP_TOKEN` and choose the local recovery-account password.
2. Enter the Keycloak provider values from the table above.
3. Select **Validate provider**.
4. Return to the login page and select the configured Keycloak provider.
5. Sign in as `christopher.paul@kassandra.org` with password `password`.

Because this is the first OIDC sign-in while setup is in progress, Kassandra creates or links the user and grants
`ADMIN` and `USER`. It then marks setup complete. Later OIDC identities are not matched by email: an administrator
must explicitly link their `(issuer, subject)` identity to a Kassandra user through **Link Identity**.

Other imported test users include:

| Username | Password |
| --- | --- |
| `admin` | `admin` |
| `jennifer.holleman@kassandra.org` | `password` |
| `grace.martin@kassandra.org` | `password` |
| `testuser` | `password` |

## Recovery account

The setup wizard creates the restricted local account `setup-recovery`. It is available at
`http://localhost:8080/ui/recovery` and can be used to return to `/ui/setup` for identity-provider recovery.
It intentionally cannot access normal Kassandra UI routes or REST APIs.

## Production differences

For production, use HTTPS, configure Keycloak with the public Kassandra callback
`https://kassandra.example/login/oauth2/code/<registration-id>`, and inject both secrets through the container or
orchestrator secret facility. Do not use the test realm, Keycloak `start-dev`, wildcard redirects, or the credentials
in this document.
