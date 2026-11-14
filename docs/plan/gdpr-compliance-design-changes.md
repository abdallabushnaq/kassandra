# GDPR Compliance: Required Design Changes

Kassandra needs a privacy lifecycle, not just a "delete user" feature. The biggest gaps are sick-leave
confidentiality, overly broad personal-data access, permanent audit/undo history, and incomplete erasure.

This is a source-level assessment as of 14 November 2026, not a production compliance audit. "100% GDPR
compliant" cannot be guaranteed by software: compliance also depends on the organisation's purposes, lawful
bases, deployment, contracts, and procedures.

## What Kassandra Stores

| Category | Data found |
| --- | --- |
| Identity | Name, email, UUID, roles, OIDC provider/subject links |
| Employment and working patterns | First/last working day, availability history, assigned working schedules |
| Location | Country/state and effective dates, not precise GPS location |
| Absence | Vacation, sickness, trips, holidays, date ranges |
| Individual activity | Assignments, worklog authors/editors, timestamps, hours, estimates, progress |
| Images and free text | Avatars, original images, generation prompts, task notes, worklog comments |
| Secondary copies | Envers revisions, undo history, AI conversations/tool results, diagnostic output, JSON/XML/Excel exports |

Names are not the only personal data. Identifiable work patterns and performance are personal data too.
**Identified sick-leave records are health data**, requiring additional protection under Article 9.

## Required Design Changes

### 1. Separate Planning Availability from Private Absence Reasons

`UserController.getAll()` returns `UserDAO`, which includes eagerly loaded off-days, locations, availability,
and working schedules. General `USER` access therefore exposes substantially more than a colleague directory
needs. `OffDayController.getById()` also permits general user access.

Change the read model:

- A minimal colleague DTO: identifier, display name, approved avatar.
- A planning DTO: capacity and unavailable periods, without absence reasons.
- A restricted personal-data DTO: self-access and specifically authorised personnel.

**For planning, "unavailable" should normally suffice; colleagues should not need to know "sick".** If sickness
tracking is necessary, isolate its permissions, retention, exports, and processing purpose. Being a technical
administrator should not automatically mean unrestricted access to health records.

This must be enforced server-side across REST, charts, downloads, and AI, not merely hidden in Vaadin.

### 2. Fix Existing Security Issues Before Adding Privacy Features

Two concrete production blockers surfaced:

- `SecurityConfig` creates fixed test accounts without a test/development profile restriction; the OIDC API
  chain also enables HTTP Basic.
- `UserController.update()` checks ownership against the **submitted email**, then saves the entire submitted
  `UserDAO`, including security-sensitive fields such as roles.

Use the authenticated, server-resolved user identity for ownership checks, load the persisted target before
authorisation, and accept allowlisted profile-update DTOs. Keep role administration separate. Child-record
updates/deletes must verify that the persisted record belongs to the authorised owner.

Also review CSRF protection: the API accepts session authentication while disabling CSRF. That needs an
authentication-aware design, rather than assuming every API call is bearer-token-only.

### 3. Introduce Explicit Account and Erasure Lifecycles

Distinguish these operations:

| Operation | Meaning |
| --- | --- |
| Deactivate | Stop access while retaining justified records |
| Restrict processing | Retain data but block uses covered by a restriction |
| Erase | Permanently remove data without a continuing lawful retention requirement |
| Retain under exception | Keep only necessary records, with a documented reason, access restriction, and review/expiry date |

Currently, `UserController.delete()` removes avatars and the user, but is not a complete erasure workflow.
It does not explicitly address OIDC links, group memberships, product ACLs, assignments, sprint ownership,
worklog attribution, history, conversations, or caches. OIDC foreign keys can also prevent deletion.

Introduce a `PrivacyService` that inventories these references, produces an erasure preview, and executes an
idempotent, non-undoable operation. Revoke active sessions and remove applicable authentication state as part
of deactivation/erasure.

Preserve useful project totals where possible, but reconsider retaining individually attributable historical
activity. **Replacing a name with "Former user" or a UUID is not necessarily anonymisation**: dates, tasks,
and colleagues may still identify the person.

### 4. Make Retention Apply to History, Not Just Current Tables

Planning entities and worklogs use soft deletion. Envers retains historical values, and
`store_data_at_delete=true` deliberately preserves deletion states. Audit revisions and undo operations also
copy actor email addresses.

The "Planning history limit" setting limits **displayed operations**, not retention.

Add purpose-based retention policies for profiles, absences, worklogs, audit records, undo history,
conversations, logs, and generated files. There is no single GDPR-mandated retention period for these
categories.

A scheduled `RetentionService` must handle live rows, tombstones, historical revisions, associations, and
dependent undo entries consistently. Do not delete shared revision metadata blindly, or leave replay paths
capable of restoring erased data. Prefer opaque actor references over copying emails throughout history,
while recognising those references can remain personal data.

Existing data needs migration and cleanup, not only safer handling of future records.

### 5. Support Data-Subject Requests

Existing sprint exports are not a subject-access export: they mix multiple people's information and omit
other relevant stores.

Add a protected privacy workflow covering access, correction, erasure, restriction, and objection.
Portability can reuse export machinery **where that right applies**; it is not universal.

A small initial API could expose:

```text
GET  /api/privacy/me/export
POST /api/privacy/requests
POST /api/privacy/users/{id}/erasure-preview
POST /api/privacy/users/{id}/erase
```

Self-service identity must come from authentication; administrator actions need explicit permissions and
confirmation. Exports must protect other people's rights and include relevant historical data, with human
review for mixed free text. Requests should track status, the applicable deadline, normally one month, and
reasons for any exception or refusal.

People mentioned in notes may be data subjects without having a Kassandra account.

### 6. Put Privacy Controls Around Every AI Data Path

`UserDto` sends names, emails, roles, and employment dates to the configured model. Assistant queries/responses
and model-generated reasoning are logged; sprint-insight prompts/responses are also printed to stdout.

Additionally, `AiAssistantService` holds application-wide conversation maps. Its 20-message window bounds each
conversation, **not the number or lifetime of conversations**. Cleanup currently relies on explicit clearing.

Recommended design:

- Administrator-controlled enablement for assistant, insights, filters, and image generation.
- Minimal, purpose-specific outgoing DTOs; exclude health information and unnecessary identifiers.
- Conversation ownership, expiry, logout/session cleanup, and erasure integration.
- No prompt, response, tool-result, or generated-reasoning content in routine production logs.
- Explicit approval of external endpoints and their retention/training terms.

Local inference reduces third-party disclosure but does not remove GDPR obligations. External services
require the applicable processor arrangements and international-transfer assessment.

### 7. Include Caches, Exports, Logs, and Backups

User avatar responses currently specify **public, immutable caching for one year**. Identifiable user images
should instead have authenticated delivery and an appropriate private caching policy.

Production logging should favour event metadata over names, emails, comments, and payloads, with bounded
retention. Apply equivalent access/minimisation rules to JSON, Microsoft Project, and Excel exports.

Deployment requirements should cover TLS, protected database storage, encrypted backups where appropriate to
risk, secret management, and restore procedures. Existing secret encryption does not encrypt all personal
records.

For backups, define justified expiry and restricted use; reapply erasures before restored data returns to
service. Document responsibilities for downloaded exports and notify recipients of corrections/erasures
where required. Kassandra cannot remotely revoke an already downloaded file.

## What Software Cannot Provide Alone

Assuming workplace self-hosting, the deploying organisation will usually be the controller. It must establish
processing purposes and Article 6 lawful bases, an Article 9 condition for sickness data, appropriate privacy
notices, processor contracts, retention justifications, and request/breach procedures. Employee consent is
often unsuitable because it may not be freely given.

Health processing and employee-monitoring capabilities warrant a DPIA screening; national employment rules
may impose additional obligations. Breach procedures must support supervisory-authority notification within
72 hours where required.

## Implementation Order

Close the security and health-disclosure gaps first; then add erasure/retention with undo-history safeguards;
then deliver the request/export UI and deployment documentation. Keep this within the existing application:
a focused `PrivacyService`, `PrivacyController`, and scheduled `RetentionService`, rather than a separate
compliance platform.
