You are working on the Nguon Goc So traceability system.

Implement the complete user story:

NCL-08-CN-006 — Event Chain Integrity Verification

IMPORTANT:
Before writing or modifying ANY code, thoroughly inspect the entire project and read the API specification:

docs/api/event/NCL-08-CN-006-event-chain-verification.md

The API specification is the primary contract for this feature.

Do NOT blindly implement the specification from this prompt if the existing project architecture or API documentation defines a more precise behavior. Follow the existing project's conventions and the API contract.

==================================================
1. USER STORY
==================================================

NCL-08 — Cảnh báo, thu hồi lô và lịch sử hoạt động

NCL-08-CN-006 — Kiểm chứng tính toàn vẹn dòng sự kiện

As a Procurement Company (VT-04), I want to independently verify that the event chain of a production lot has never been modified, so that I can trust the traceability record without relying on the seller's promise.

Business value:
Turn the "append-only" event-chain rule into something that users can independently verify.

Business behavior:

Every event recorded for a production lot must contain a linked hash generated from:

1. The canonical content of the current event.
2. The hash of the immediately preceding event in the same lot.

This creates a hash-linked event chain.

The system must NOT implement blockchain.

This is only a simulation of an append-only integrity mechanism using linked cryptographic hashes.

When a user opens the verification page for a production lot:

1. Load all events belonging to the lot in deterministic chronological order.
2. Recalculate the hash of every event.
3. Compare the recalculated values with the stored hash values.
4. Verify that each event references the correct previous-event hash.
5. Determine whether the entire chain is intact.
6. If the chain is broken, identify the FIRST event where the mismatch occurs.
7. Display the integrity result to the user.
8. Record the verification attempt in the activity/history system according to the existing project architecture.

==================================================
2. ACCEPTANCE CRITERIA
==================================================

TC-01 — Successful verification

Given:
- A production lot has five events.
- None of the events has been modified.

When:
- The user opens the event-chain verification page.

Then:
- The backend recalculates the complete chain.
- The system reports that the event chain is intact.
- The frontend clearly displays a successful integrity result.
- The verified events and their chain information should be inspectable according to the API contract.

TC-02 — Tampered event

Given:
- The third event of the lot has been directly modified in the database.

When:
- The user opens the verification page.

Then:
- The system detects that the chain is broken.
- The system identifies event #3 as the FIRST inconsistent event.
- The frontend clearly displays that the chain has been tampered with.
- The user can identify the first invalid event.

Important:
Do NOT simply check whether stored hashes exist.

The verification must actually recalculate the hashes from the event content and previous hash and compare them with the stored values.

TC-03 — Verification history

Given:
- The user performs a verification.

Then:
- The system records the verification attempt in the project's existing activity/history mechanism.
- The record must contain at least the verification time and verification conclusion, according to the existing project architecture/API contract.

Do NOT create a new history mechanism if the project already has an appropriate activity/audit/history mechanism.

TC-04 — Empty event chain

Given:
- A production lot has no events.

When:
- The user opens the verification page.

Then:
- The system must NOT report the chain as valid.
- The system reports that there are no events to verify.
- The frontend displays an appropriate empty-state message.

==================================================
3. BUSINESS RULE
==================================================

QTN-19 — Event chain is sealed using linked hashes

Every event recorded for a production lot must calculate a cryptographic hash from:

- canonical current-event content
- previous event hash

The first event in a chain has no previous event hash and must use the project's defined representation for the genesis/first event according to the API documentation.

For every subsequent event:

currentHash =
    HASH(canonicalCurrentEventContent + previousEventHash)

The exact canonicalization, hash algorithm, encoding, field selection, delimiter, and representation MUST follow:

docs/api/event/NCL-08-CN-006-event-chain-verification.md

Do not invent a different hashing scheme if the API documentation already specifies one.

==================================================
4. VERY IMPORTANT — UNDERSTAND EXISTING EVENT ARCHITECTURE
==================================================

Before implementation, inspect how events currently work.

At minimum inspect:

Backend:

- ChainEvent.java
- ChainEventType.java
- ChainEventController.java
- ChainEventService.java
- ChainEventServiceImpl.java
- ChainEventRepository.java
- all existing event request/response DTOs
- all existing event recording services
- all existing event controllers
- all existing event creation flows
- event_data JSON structure
- ProductionLot.java
- Shipment.java
- TraceCode.java
- related repositories
- authentication/security utilities
- RoleCode.java
- PermissionChecker
- activity/audit/history entities and services
- notification infrastructure if relevant
- exception handling
- ApiResult
- PageResponse
- existing Flyway migrations

Frontend:

- AppRoutes.tsx
- Sidebar.tsx
- roleAccess.ts
- ChainEvent-related pages
- eventFormatter.ts
- existing traceability/event timeline components
- existing verification/audit/history UI
- API clients
- hooks
- TypeScript types
- UI components
- existing detail pages and navigation patterns

Also search the entire project for:

- ChainEvent
- eventType
- event_data
- recordedAt
- recordedBy
- productionLot
- activity log
- audit log
- verification
- integrity
- hash
- SHA-256
- checksum

Do not assume that ChainEvent is the only place where event information is stored.

==================================================
5. HASH STORAGE DESIGN
==================================================

Determine whether the existing ChainEvent table/entity can support:

- previous event hash
- current event hash

If fields do not exist, modify the existing ChainEvent model rather than creating a separate Warehouse/EventHash/etc. entity unless the existing architecture explicitly requires otherwise.

Preferred conceptual model:

ChainEvent
    previousHash
    eventHash

The exact field names should follow the project's existing naming conventions and API contract.

Do NOT create a separate event-chain table merely for this feature unless the API documentation/project architecture requires it.

Do NOT create a blockchain implementation.

Do NOT introduce unnecessary entities.

==================================================
6. EVENT CREATION MUST BE INTEGRATED
==================================================

This is critical.

Do not implement verification only.

Every NEW event that is recorded after this feature is implemented must receive the correct linked hash.

Inspect ALL existing event creation paths.

Examples may include:

- PROCUREMENT
- TRANSPORT
- WAREHOUSE_RECEIPT
- FARMING events
- other ChainEvent types

Determine the correct central point where ChainEvent records are persisted.

Prefer implementing hash generation in a centralized ChainEvent creation/persistence service so that every event type automatically receives:

- previousHash
- eventHash

This avoids having each individual event service duplicate hashing logic.

Do not modify every event type independently if a safe centralized implementation is possible.

However, ensure that existing event flows continue to work.

==================================================
7. HASH CANONICALIZATION
==================================================

This is a security/integrity-sensitive part of the feature.

The hash must be deterministic.

Do NOT hash a Java object's default toString().

Do NOT hash a Map directly without deterministic serialization.

Do NOT rely on database JSON serialization order unless the API contract explicitly defines it.

Create a deterministic canonical representation of the event content according to the API documentation.

The canonical content must produce the exact same hash:

- when the event is originally recorded
- when the event is later verified

The implementation must ensure:

same event content + same previous hash
        =>
same calculated hash

Any modification to relevant event content must result in a different calculated hash.

If the API specification defines SHA-256 or another specific algorithm, use exactly that algorithm.

==================================================
8. FIRST EVENT / GENESIS EVENT
==================================================

Determine from the API documentation how the first event should be handled.

For the first event of a production lot:

previousHash should follow the documented convention.

Do not use arbitrary values such as:

- "null"
- "NONE"
- "GENESIS"
- empty string

unless the API documentation explicitly specifies that representation.

The same convention must be used consistently during:

- event creation
- event verification

==================================================
9. EVENT ORDER
==================================================

Verification must use a deterministic event ordering.

Determine the correct ordering from the API documentation and existing model.

Prefer the project's authoritative event ordering field, such as:

- recordedAt
- createdAt
- sequence number
- database identifier

If timestamps alone can produce ambiguous ordering, implement a deterministic secondary ordering.

Do not rely on unspecified database row order.

The exact ordering must be consistent between event creation and verification.

==================================================
10. VERIFICATION ALGORITHM
==================================================

Implement verification approximately as follows:

1. Load all events for the production lot.
2. Sort them deterministically.
3. If no events exist:
   - return EMPTY / NO_EVENTS according to API contract.
4. Set previousHash to the genesis value.
5. For each event:

   calculatedHash =
       calculateHash(canonicalEventContent, previousHash)

   Compare:

   event.previousHash
   calculated previousHash

   event.eventHash
   calculatedHash

6. The FIRST mismatch becomes the first corrupted event.
7. Continue only if needed to generate the complete verification response.
8. Return:

   - overall integrity status
   - total event count
   - verified event count
   - first invalid event
   - reason/error type
   - per-event verification information if specified by API contract

Do not stop at the first mismatch if the API contract requires the complete verification result.

==================================================
11. DETECT BOTH TYPES OF TAMPERING
==================================================

The verification must detect at least:

A. Event content modification

Example:

Original:

receivedQuantity = 500

Database is manually changed to:

receivedQuantity = 450

The recalculated hash must no longer match eventHash.

B. Previous hash modification

Example:

Event #3 has:

previousHash = manipulated-value

The verification must detect the mismatch.

C. Historical event modification

If event #2 is modified:

- event #2 should fail
- event #3 and all subsequent linked hashes should also become inconsistent

The system must identify event #2 as the FIRST invalid event.

==================================================
12. API IMPLEMENTATION
==================================================

Read the complete API documentation first.

Implement exactly the endpoints defined there.

At minimum, the architecture is expected to contain an operation conceptually equivalent to:

GET
/api/v1/.../production-lots/{productionLotId}/event-chain/verify

However:

DO NOT invent the final endpoint path.

Use the exact endpoint defined in:

docs/api/event/NCL-08-CN-006-event-chain-verification.md

Implement:

- Controller
- Service
- Repository queries if needed
- Request/Response DTOs
- authorization
- validation
- exception handling

Follow existing API conventions.

Do not create CRUD endpoints for event-chain verification.

Verification is a read/compute operation.

==================================================
13. AUTHORIZATION
==================================================

The user story is for:

- VT-04 — Procurement Company

QTN-19 also mentions:

- Platform Administrator
- Procurement Company
- Industry Regulator

Determine the exact authorization rules from the API documentation and existing permission model.

Do not automatically assume that only VT-04 should have access if the API contract explicitly supports other roles.

Use the project's existing:

- RoleCode
- @PreAuthorize
- PermissionChecker
- permission/resource/action model

If a new permission is required, add it as MASTER DATA through Flyway.

Do NOT manually insert permissions into MySQL.

==================================================
14. PRODUCTION LOT VALIDATION
==================================================

Verification must be attached to the correct production lot.

Respect QTN-19 and the project's existing traceability/security rules.

Validate:

- production lot exists
- user is authorized to inspect it
- events belong to the requested production lot
- events are ordered correctly
- no cross-lot events are accidentally included

Do not allow a user to verify an arbitrary event chain belonging to another organization without authorization.

==================================================
15. ACTIVITY / VERIFICATION HISTORY
==================================================

Acceptance criterion TC-03 requires verification history.

Before creating anything new, inspect the existing activity/audit infrastructure.

Search for:

- ActivityLog
- ActivityLogService
- AuditLog
- history
- action logging
- system activity

If an existing mechanism can represent:

"EVENT_CHAIN_VERIFICATION"

reuse it.

Only create a new entity/table if the existing architecture cannot support this requirement.

If a new master-data action/resource is needed, add it through Flyway.

The verification history should record at least:

- who performed verification
- production lot
- timestamp
- result
- first invalid event if applicable

Follow the existing project's audit model and API contract.

==================================================
16. FRONTEND DESIGN
==================================================

Build an actual user-facing event-chain verification experience.

Do NOT implement backend-only functionality.

First inspect existing traceability/event pages and navigation patterns.

The UI should provide:

1. A clear entry point to verify a production lot.
2. A verification page/view.
3. A loading state.
4. An empty state when no events exist.
5. A successful integrity state.
6. A tampered/broken-chain state.
7. The first invalid event clearly highlighted.
8. A readable event-chain visualization/list.
9. Hash information where appropriate without overwhelming the user.
10. Verification timestamp/result.
11. Clear explanation of what was verified.

Suggested conceptual UI:

--------------------------------------------------
Event Chain Integrity Verification
--------------------------------------------------

Production Lot: XXXXX

[ Verify Integrity ]

Result:

✓ EVENT CHAIN INTACT

or

⚠ EVENT CHAIN COMPROMISED

First inconsistent event:
Event #3
Event ID: ...
Recorded at: ...
Reason: Event hash mismatch

--------------------------------------------------
Event Chain
--------------------------------------------------

✓ Event #1
  Hash: ...

✓ Event #2
  Hash: ...

✗ Event #3
  Hash mismatch
  Expected: ...
  Stored: ...

⚠ Event #4
  Depends on invalid previous event

...

Do not blindly copy this layout.

Follow the project's existing UI design system.

==================================================
17. NAVIGATION / SIDEBAR
==================================================

Determine the most appropriate navigation based on the existing project.

If this feature should be a standalone menu item, implement it consistently.

If it belongs under an existing traceability/event menu, follow that architecture.

The route must be protected by the correct role/permission.

Do not create duplicate navigation entries.

The browser URL should remain meaningful and support direct navigation.

==================================================
18. FRONTEND API / STATE
==================================================

Create or modify:

- API client
- TypeScript types
- hooks
- page/components

according to the project's architecture.

Handle:

- loading
- success
- tampered chain
- empty event list
- API errors
- unauthorized access

Do not put business verification logic only in the frontend.

The backend must be the source of truth for integrity verification.

==================================================
19. FLYWAY / DATABASE MIGRATION
==================================================

The project uses:

spring.jpa.hibernate.ddl-auto=validate

and Flyway as the database source of truth.

Migration structure:

backend/src/main/resources/db/migration/

with the existing schema/data organization.

Before creating a migration, inspect the current migration history.

Only create a new V__ migration if database schema/master data changes are actually required.

Potential schema changes may include:

- previous_hash column
- event_hash column

Potential data changes may include:

- new permission
- new resource/action
- new activity action

If required:

1. Create the next appropriate migration version.
2. NEVER modify an already-applied migration.
3. NEVER create duplicate versions.
4. NEVER manually INSERT data into MySQL.
5. Ensure migration works on a clean database.
6. Ensure Hibernate ddl-auto=validate passes.

If the current schema already supports the required fields, DO NOT create unnecessary migrations.

==================================================
20. IMPORTANT FLYWAY RULE
==================================================

Do NOT create migration files just because the task mentions migrations.

First inspect:

- ChainEvent entity
- current schema migrations
- current database design

Then determine whether migration is required.

If migration is required, create it.

If not required, explicitly document why no migration was created.

==================================================
21. TESTING
==================================================

Implement comprehensive backend tests.

At minimum cover:

TC-01:
5 valid events → integrity VALID.

TC-02:
Modify event #3 directly in the database/test fixture → event #3 identified as first invalid event.

TC-03:
Verification creates an activity/history record.

TC-04:
No events → EMPTY / NO_EVENTS.

Also test:

- first event/genesis handling
- previousHash mismatch
- eventHash mismatch
- multiple subsequent events after corruption
- deterministic hashing
- authorization
- production lot not found
- unauthorized production lot
- cross-lot isolation
- invalid/missing hash values
- event ordering

Frontend tests or appropriate validation should cover:

- valid state
- compromised state
- empty state
- loading state
- API error
- first invalid event highlighting

Run:

Backend:

.\mvnw.cmd test

Frontend:

npm run build

and the project's TypeScript/type-check command if applicable.

Fix errors caused by this implementation.

Do not silently ignore failures introduced by your changes.

==================================================
22. BACKWARD COMPATIBILITY
==================================================

This project may already contain events created before hash-chain support was implemented.

Determine from the API documentation and existing project behavior how legacy events should be handled.

DO NOT invent a migration strategy blindly.

If legacy events require:

- backfilling hashes
- treating old events specially
- rejecting verification
- recalculating a baseline

follow the API contract.

If the API documentation does not define this behavior, inspect the project and clearly document the assumption instead of silently making a destructive decision.

==================================================
23. CODE QUALITY
==================================================

Follow existing project conventions.

Do not:

- create unnecessary abstractions
- duplicate hashing logic
- create unnecessary entities
- create CRUD for immutable events
- implement blockchain
- store verification result as authoritative chain state
- trust frontend calculations
- modify unrelated features
- rewrite unrelated files
- modify existing applied Flyway migrations

Prefer:

- centralized hash calculation
- deterministic canonicalization
- immutable event records
- clear DTOs
- service-layer business logic
- existing exception handling
- existing authorization
- existing UI components
- existing activity/audit infrastructure

==================================================
24. GIT REQUIREMENTS
==================================================

The branch name MUST be taken from:

docs/api/event/NCL-08-CN-006-event-chain-verification.md

Read the API document and use the branch name specified there.

Create the branch using exactly:

git switch -c <BRANCH_NAME_FROM_API_DOC>

All implementation work must happen on this feature branch.

When finished:

git status
git diff
git log --oneline -n 10

Commit all feature changes with a clear commit message, for example:

feat: implement event chain integrity verification

Then push ONLY this feature branch:

git push -u origin <BRANCH_NAME_FROM_API_DOC>

ABSOLUTE GIT RESTRICTIONS:

DO NOT merge into:

- develop
- main
- master
- any other branch

DO NOT checkout another branch to perform the implementation.

DO NOT rebase this feature into another branch.

DO NOT force push unless explicitly required.

DO NOT delete the feature branch.

==================================================
25. FINAL VALIDATION
==================================================

Before declaring completion, verify:

Backend:
- compilation passes
- tests pass
- Flyway migrations pass if added
- Hibernate ddl-auto=validate passes
- authorization works
- hash creation works for new events
- verification correctly detects tampering

Frontend:
- TypeScript passes
- production build passes
- verification UI is accessible
- valid chain UI works
- invalid chain UI works
- empty chain UI works
- first invalid event is clearly shown

Git:
- correct feature branch
- commit created
- pushed to origin
- no merge into develop/main/master/other branches

==================================================
26. FINAL IMPLEMENTATION REPORT
==================================================

At the end, provide a concise but complete implementation report containing:

1. API contract inspected
2. Branch name
3. Files inspected
4. Files created
5. Files modified
6. Files deleted, if any
7. Backend endpoints
8. Frontend routes
9. Authorization/permissions
10. Hash algorithm and canonicalization
11. Genesis/first-event handling
12. Event ordering strategy
13. How new events receive hashes
14. Verification algorithm
15. First-invalid-event detection
16. Activity/history implementation
17. Flyway migrations created, or explicit reason why none were needed
18. Frontend UX implemented
19. Tests executed and results
20. Build/type-check results
21. Commit hash
22. Push result
23. Explicit confirmation that NO merge was performed
24. Any assumptions or limitations

IMPORTANT FINAL RULE:

Do not report the feature as complete merely because compilation succeeds.

The feature is complete only when:

- new events are actually hash-linked,
- verification actually recalculates the chain,
- tampering is detected,
- the first invalid event is identified,
- verification history is recorded,
- the frontend exposes the functionality,
- authorization is enforced,
- tests cover the acceptance criteria,
- and the feature branch has been pushed without merging anywhere.