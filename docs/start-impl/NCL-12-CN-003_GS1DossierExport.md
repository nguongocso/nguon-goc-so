# NCL-12-CN-003 — Implement GS1 Dossier Export

You are implementing **NCL-12-CN-003: GS1 Dossier Export** in the NGUONGOCSO repository.

## 1. Source of truth

Before making ANY code changes, you MUST read and understand:

```text
docs/api/report/NCL-12-CN-003_GS1DossierExport.md
````

Treat this API document as the primary functional contract for this feature.

Do NOT silently change the business requirements defined in the document.

However, the API document may describe DTOs, entities, field names, event types, statuses, or services that differ from the actual repository. Therefore:

* Inspect the existing implementation before coding.
* Reuse existing domain models, services, repositories, DTO conventions,
  response wrappers, exception handling, authorization patterns, and
  ChainEvent infrastructure wherever possible.
* Do NOT invent duplicate domain concepts when an equivalent already exists.
* Do NOT modify unrelated features.
* Do NOT modify database schema unless repository inspection proves that it
  is absolutely required by the documented contract.

If the documentation conflicts with the existing code, STOP and report the
conflict before making architectural changes that would alter existing
business behavior.

---

# 2. Feature objective

Implement an API that exports the traceability dossier of a Shipment using
the project's simulated GS1-oriented schema.

This is explicitly a **simulation of a GS1-oriented schema**, NOT a claim of
official GS1 compliance or certification.

The exported dossier must represent events using four dimensions:

* who — actor responsible for the event
* when — event timestamp
* where — event location when available
* why — event type and event-specific data

The export must include the mapping between the project's internal fields
and the simulated schema fields.

---

# 3. Required endpoint

Implement:

```http
GET /api/v1/shipments/{shipmentId}/export-gs1-dossier
```

Authorized roles:

```text
VT-02
VT-04
```

Supported query parameters:

```text
format=json|xml
includeMapping=true|false
```

Defaults:

```text
format=json
includeMapping=true
```

First inspect how existing controllers implement:

* role authorization
* UUID/path-variable handling
* query parameter validation
* standard API response wrappers
* exception handling

Follow those existing conventions instead of introducing a new style.

---

# 4. IMPORTANT: inspect the repository first

Before implementing anything, inspect at minimum:

## Shipment/domain

Find the actual implementation of:

```text
Shipment
ProductionLot
ChainEvent
ShipmentStatus
```

and determine:

* how Shipment is identified
* how Shipment relates to ProductionLot
* how ChainEvent relates to Shipment
* how event types are represented
* how event data is stored
* how actor/user information is represented
* how location is represented
* whether location/address already exists
* what statuses actually exist

## Existing ChainEvent implementation

Search for:

```text
ChainEvent
PROCUREMENT
TRANSPORT
HARVEST
PACKAGING
PLANTING
FERTILIZING
PESTICIDE
```

Determine the actual event model and reuse it.

Do NOT create a second event model merely for this export.

## Existing services

Search for existing:

```text
ShipmentService
DossierService
ReportService
ExportService
ChainEventService
```

If `DossierService` already exists, determine whether extending it is
appropriate.

If it does not exist, create the smallest service structure consistent
with the repository.

## Existing authorization

Inspect existing:

```text
@PreAuthorize
SecurityConfig
VT-02
VT-04
```

Reuse the project's authorization conventions.

## Existing ActivityLog

Search for:

```text
ActivityLog
activity log
audit log
export
```

Determine the correct existing mechanism for recording dossier exports.

Do NOT create a second audit mechanism.

---

# 5. Business rules

Implement the rules documented in:

```text
NCL-12-CN-003_GS1DossierExport.md
```

## QTN-11 eligibility

A Shipment must satisfy the documented completion requirements.

The API documentation specifies:

```text
CLOSED or PACKAGED
```

and requires the documented mandatory production records:

```text
PLANTING
FERTILIZING
PESTICIDE
HARVESTING
```

BUT before implementing this rule, inspect the actual repository to determine:

* the real Shipment/ProductionLot status enum
* the actual farming-log event/activity representation
* whether HARVESTING is named HARVEST in the code
* whether PACKAGED/CLOSED are actual statuses
* how required farming activities are currently validated elsewhere

Reuse existing business validation logic if available.

Do not introduce string-based checks when an existing enum/domain abstraction
exists.

If the documentation uses terminology that differs from the repository,
report the mapping and implement against the actual domain model.

---

# 6. Organization-level authorization

The API documentation specifies:

1. authenticate the user
2. require VT-02 or VT-04
3. load the Shipment
4. verify that the user is authorized to access that Shipment/organization
5. reject unauthorized access with HTTP 403

Inspect existing organization ownership/access-control logic and reuse it.

Do NOT implement authorization based only on a role check if the existing
system also enforces organization-level access.

---

# 7. Event export

Load all relevant ChainEvents belonging to the Shipment.

Sort events by:

```text
recordedAt ASC
```

Map each event into the simulated GS1 event representation.

Expected conceptual mapping:

```text
ChainEvent.id
    -> eventIdentifier

ChainEvent.eventType
    -> eventTypeCode

ChainEvent.recordedAt
    -> eventDateTime

ChainEvent.recordedBy.fullName
    -> actorName

ChainEvent.location.latitude
    -> eventLocation.latitude

ChainEvent.location.longitude
    -> eventLocation.longitude

ChainEvent.location.address
    -> eventLocation.address

ChainEvent.eventData
    -> eventData
```

Use the actual repository fields where their names differ.

---

# 8. Four-dimensional event representation

Each exported event should represent:

### WHO

The actor who recorded the event.

Use the actual user/actor relationship from the repository.

### WHEN

The actual event timestamp.

Do not generate artificial timestamps.

### WHERE

Use the existing event location if available.

If location is unavailable:

```text
location = null
```

and add a warning.

Do NOT fabricate coordinates or addresses.

### WHY

Represent the event type and its event-specific data.

Use the existing `eventType` and `eventData` rather than inventing a new
reason field unless the domain already provides one.

---

# 9. Warning behavior

If an event has no location:

The export must still succeed.

Add a warning containing at least:

```text
eventId
field = "location"
message
```

Do not reject the entire export solely because one event lacks location.

Do not invent location data.

---

# 10. Empty-event behavior

If the Shipment has no ChainEvents:

Return:

```http
400 Bad Request
```

with the project's standard error response structure.

The semantic error should indicate:

```text
Lô chưa có sự kiện nào để xuất hồ sơ.
```

Adapt the exact wording only if the repository has a standardized error-message
convention.

---

# 11. Response DTO

Implement DTOs only if equivalent existing DTOs do not already exist.

Conceptually the successful response should contain:

```text
shipment
events
mapping
warnings
exportedAt
exportedBy
schemaVersion
schemaDescription
```

Event:

```text
eventId
eventType
eventTypeLabel
recordedAt
recordedBy
location
details
```

Location:

```text
latitude
longitude
address
```

Warning:

```text
eventId
field
message
```

Use the project's existing DTO naming and serialization conventions.

Do NOT expose JPA entities directly from the controller.

---

# 12. Mapping table

The response should support the documented mapping:

```text
eventId
    -> eventIdentifier

eventType
    -> eventTypeCode

recordedAt
    -> eventDateTime

recordedBy
    -> actorName

location.latitude
    -> eventLocation.latitude

location.longitude
    -> eventLocation.longitude

location.address
    -> eventLocation.address

details
    -> eventData

Shipment.name
    -> shipmentName

Shipment.totalQuantity
    -> declaredQuantity

Shipment.status
    -> shipmentStatus
```

If:

```text
includeMapping=false
```

do not include the mapping section in the response, assuming this behavior is
compatible with the documented API contract.

If the API documentation requires mapping to always be present despite the
query parameter, STOP and report the inconsistency rather than silently
choosing one behavior.

---

# 13. JSON export

For:

```text
format=json
```

return the dossier using the project's standard JSON response mechanism.

Do not manually serialize JSON unless necessary.

---

# 14. XML export

For:

```text
format=xml
```

inspect the repository first for existing XML serialization support.

Reuse existing dependencies and conventions if available.

Do NOT add a large XML framework unnecessarily.

If XML support is not currently available and adding it would require a
dependency change, STOP and report the dependency requirement before making
the change.

The XML response must preserve the same semantic data as the JSON response.

---

# 15. Content type

Ensure the HTTP response has the appropriate content type:

JSON:

```text
application/json
```

XML:

```text
application/xml
```

Follow the repository's existing response/download conventions.

Do not use `application/octet-stream` unless the implementation actually
returns a downloadable binary file.

---

# 16. ActivityLog

A successful dossier export must record the export action using the existing
ActivityLog/audit mechanism.

Before implementing this:

* inspect ActivityLog
* inspect existing audit operations
* inspect how actor, organization, entity ID, action, timestamp and metadata
  are stored

Reuse the existing mechanism.

Do not create a new logging table.

If ActivityLog is asynchronous or handled through an existing service, follow
that architecture.

The log should identify the exported Shipment and the exporting user.

---

# 17. Error handling

Implement at minimum:

### 403 Forbidden

User does not have the required role or organization access.

### 404 Not Found

Shipment does not exist.

### 400 Bad Request

Shipment has no events.

### 400 Bad Request

Shipment fails QTN-11 eligibility requirements.

### Invalid format

For example:

```text
format=pdf
```

must be rejected according to the project's standard validation/error
handling.

Do not silently fall back to JSON for unsupported formats.

---

# 18. Security requirements

Do NOT:

* trust a client-provided organization ID
* expose another organization's Shipment
* bypass existing authorization
* expose internal entity relationships unnecessarily
* introduce unauthenticated access
* weaken Spring Security configuration
* disable security for testing

Follow the project's current security architecture.

---

# 19. Files

The API document proposes files such as:

```text
GS1DossierExportRequest.java
GS1DossierExportResponse.java
GS1Event.java
EventLocation.java
Warning.java
GS1DossierExportService.java
GS1DossierExportServiceImpl.java
GS1DossierExportController.java
DossierService.java
DossierServiceImpl.java
```

DO NOT blindly create all of these files.

First inspect the repository.

If existing classes provide the same responsibility, extend/reuse them.

Create only the minimum required files.

---

# 20. Database migration

The API documentation states:

```text
Migration: not required
```

Respect this unless repository inspection proves that an existing required
field/table is missing.

Do NOT modify the database schema simply to make the feature easier.

If a migration unexpectedly becomes necessary, STOP and report it before
creating the migration.

---

# 21. Tests

Create focused automated tests.

At minimum cover:

### TC-01

Shipment with multiple events:

* export succeeds
* events are returned
* events are ordered by recordedAt ASC
* four-dimensional event information is mapped correctly

### TC-02

Event without location:

* export succeeds
* location is null
* warning is generated

### TC-03

VT-06/unauthorized consumer:

* returns 403

### TC-04

Shipment without events:

* returns 400

### TC-05

Shipment failing QTN-11:

* returns 400
* missing conditions are reported

### TC-06

XML format:

* returns successful XML response
* correct content type
* equivalent semantic data

### TC-07

Missing location and address:

* location remains null
* warning is present

Also test:

* Shipment not found → 404
* invalid format → 400
* organization access violation → 403
* `includeMapping=false`
* `includeMapping=true`
* ActivityLog is recorded after successful export

Follow existing project testing patterns.

Do not weaken existing tests.

Do not delete unrelated failing tests.

---

# 22. Important testing rule

Before declaring the feature complete:

Run the focused tests first.

Then run the full backend test suite:

```powershell
cd backend
.\mvnw.cmd test
```

Then build the frontend:

```powershell
cd frontend
npm run build
```

If the full suite contains unrelated failures:

* do NOT modify unrelated code
* do NOT remove or weaken tests
* identify the exact failing test
* identify the commit/change causing it if possible
* report it clearly

---

# 23. Git safety

Before modifying anything:

```powershell
git status --short
git branch --show-current
git log --oneline -5
```

The expected feature branch is:

```text
feature/NCL-12-CN-003-gs1-dossier-export
```

If currently on another branch, STOP and report before switching branches.

Do NOT:

* reset user changes
* checkout away user changes
* restore files without understanding their origin
* commit unrelated changes
* modify unrelated files

After implementation:

```powershell
git status --short
git diff --stat
git diff
```

Review every changed file.

---

# 24. Implementation workflow

Follow this exact workflow:

## Phase 1 — Read and inspect

1. Read:

```text
docs/api/report/NCL-12-CN-003_GS1DossierExport.md
```

2. Inspect the current branch/status.
3. Inspect Shipment.
4. Inspect ChainEvent.
5. Inspect ProductionLot/farming logs.
6. Inspect event types.
7. Inspect organization authorization.
8. Inspect existing service/controller patterns.
9. Inspect ActivityLog.
10. Inspect existing DTO/response conventions.
11. Inspect XML support.

DO NOT modify code during this phase.

## Phase 2 — Report architecture

Before implementing, summarize:

* relevant existing classes
* relevant existing services
* actual event model
* actual status model
* authorization mechanism
* ActivityLog mechanism
* whether XML support already exists
* any documentation/code conflicts
* exact files you intend to modify

If there is a blocking architectural conflict, STOP here.

Otherwise continue.

## Phase 3 — Implement

Implement the smallest solution satisfying the API document and repository
architecture.

Avoid unrelated refactoring.

## Phase 4 — Test

Run focused tests.

Fix only failures caused by this implementation.

Then run:

```powershell
cd backend
.\mvnw.cmd test
```

Then:

```powershell
cd frontend
npm run build
```

## Phase 5 — Review

Verify:

* authorization
* organization isolation
* QTN-11
* event ordering
* four-dimensional event mapping
* missing-location warnings
* empty-event handling
* JSON
* XML
* mapping inclusion
* ActivityLog
* error handling
* no schema changes
* no unrelated modifications

## Phase 6 — Final report

Return a concise implementation report containing:

1. Files changed
2. Architecture used
3. Endpoint behavior
4. Business rules implemented
5. Authorization behavior
6. JSON/XML behavior
7. ActivityLog behavior
8. Tests executed and results
9. Frontend build result
10. Any unrelated failures
11. Any remaining limitations
12. Git status

DO NOT claim a requirement is implemented unless it was actually verified.

---

# 25. Critical stop conditions

STOP and ask for clarification if:

1. The API document conflicts materially with the existing domain model.
2. QTN-11 cannot be implemented without inventing business rules.
3. Shipment ownership/organization authorization is unclear.
4. XML requires a new dependency that is not already present.
5. A database migration appears necessary despite the API document saying
   no migration is required.
6. Existing ChainEvent semantics conflict with the documented event model.
7. The required endpoint conflicts with an existing endpoint.
8. The current branch is not the expected feature branch and switching would
   risk user changes.

Do not make unilateral architectural decisions in these cases.

---

## Final instruction

Start with **Phase 1 only**.

Read:

```text
docs/api/report/NCL-12-CN-003_GS1DossierExport.md
```

and inspect the repository.

Do NOT modify any files yet.

After inspection, report the actual architecture, relevant existing
implementations, conflicts/gaps, and the exact implementation plan.

Only proceed to Phase 3 implementation after the architecture is confirmed
to be compatible with the API contract.