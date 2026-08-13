You are working on the NguonGocSo project.

TASK:
Implement User Story NCL-05-CN-007 — "Theo dõi điều kiện bảo quản khi vận chuyển"
(Storage Monitoring During Transportation).

You MUST first read and understand the API specification:

docs/api/event/NCL-05-CN-007-storage-monitoring.md

The API specification is the primary contract for this implementation.

You must inspect the existing backend, frontend, database entities, Flyway
migrations, event architecture, authorization system, notification system,
traceability journey, and existing transportation/procurement event
implementation before making changes.

Do not blindly create a new module if the existing event architecture can
represent this feature.

==================================================
1. USER STORY
==================================================

NCL-05-CN-007 — Theo dõi điều kiện bảo quản khi vận chuyển

As a Procurement Company (VT-04), I want to see temperature and humidity
recorded during transportation of a lot, so that I can determine whether the
lot exceeded its storage-condition thresholds before receiving the goods.

Business value:

The quality/condition of a lot can be explained using actual storage
measurements instead of assumptions.

The system must support MANUAL data entry only.

IMPORTANT:

This is a demonstration/simulation feature.

DO NOT integrate real IoT sensors.

DO NOT introduce MQTT, Kafka, WebSocket sensor streaming, hardware APIs,
or external sensor services.

Users manually enter temperature and humidity measurements at checkpoints
during the transportation journey.

The system compares each measurement against the storage thresholds defined
for the agricultural product category.

If a measurement exceeds the configured threshold, that checkpoint must be
marked as exceeding the allowed storage condition and the journey must show
a warning.

==================================================
2. ACCEPTANCE CRITERIA
==================================================

Implement the following acceptance criteria.

--------------------------------------------------
TC-01 — SUCCESS
--------------------------------------------------

Given:

- The agricultural product category has a storage temperature range
  from 2°C to 8°C.
- The user records three temperature checkpoints:
  4°C, 5°C, and 6°C.

Expected:

- All three checkpoints are stored successfully.
- None of the checkpoints is marked as exceeding the temperature threshold.
- The lot journey displays all three checkpoints.
- No storage-condition warning is displayed for those checkpoints.

--------------------------------------------------
TC-02 — THRESHOLD EXCEEDED
--------------------------------------------------

Given:

- The agricultural product category has a storage temperature range
  from 2°C to 8°C.
- The user records a checkpoint with temperature 15°C.

Expected:

- The checkpoint is stored.
- The checkpoint is marked as exceeding the configured storage threshold.
- The lot journey displays a storage-condition warning for that checkpoint.
- The warning clearly indicates that the measurement exceeded the allowed
  condition.

Do not merely reject the measurement because it exceeds the threshold.

IMPORTANT:

A temperature outside the allowed range is a VALID observation.

It must be recorded and flagged as an anomaly/warning.

--------------------------------------------------
TC-03 — INVALID DATA
--------------------------------------------------

Given:

- The lot is currently being transported.

When:

- The user enters humidity = 120%.

Expected:

- The request is rejected.
- The system reports that humidity must be between 0% and 100%.
- No invalid checkpoint is stored.

Apply appropriate validation to humidity.

--------------------------------------------------
TC-04 — UNAUTHORIZED USER
--------------------------------------------------

Given:

- The current user is a consumer using public traceability.

When:

- The consumer attempts to add a temperature/humidity checkpoint.

Expected:

- The operation is rejected.
- Public traceability remains read-only.
- Consumers must NOT be able to create storage-monitoring events.

==================================================
3. BUSINESS RULE QTN-05
==================================================

QTN-05:

"Every traceability event must be attached to a valid active lot."

For every storage-monitoring event:

1. The referenced lot must exist.
2. The lot must still be valid/active.
3. The lot must not have been recalled.
4. The event must be attached to the correct lot.
5. Invalid lot references must be rejected.

Follow the project's existing implementation of QTN-05.

Do NOT invent a second or inconsistent definition of "active lot".

Inspect the existing ProductionLot, Shipment, TraceCode, ChainEvent and
traceability validation logic and reuse the established business rules.

==================================================
4. IMPORTANT PRECONDITION
==================================================

The User Story states:

"The lot has at least one transportation event and the agricultural product
category has declared storage thresholds."

Therefore:

A storage-monitoring checkpoint must NOT be recordable for an arbitrary lot.

Before recording the checkpoint, verify:

1. The lot exists and is valid.
2. The lot has at least one valid transportation event.
3. The product category has configured storage thresholds.

If these prerequisites are not satisfied, reject the operation with a clear
business error.

DO NOT silently create a checkpoint without the required threshold data.

==================================================
5. FIRST: READ THE API DOCUMENTATION
==================================================

Before coding, inspect:

docs/api/event/NCL-05-CN-007-storage-monitoring.md

Treat the API documentation as the authoritative API contract.

Extract and understand:

- endpoint paths
- HTTP methods
- request DTOs
- response DTOs
- field names
- required/optional fields
- validation rules
- error responses
- authorization requirements
- event type
- response structure
- storage-threshold structure
- journey/traceability integration
- any examples provided in the API document

DO NOT replace API-document terminology with invented terminology.

If the API document specifies a particular endpoint or DTO design, follow it.

If the API document conflicts with assumptions in this prompt, the API
documentation takes precedence unless the conflict is with the explicit
acceptance criteria above.

==================================================
6. INSPECT THE EXISTING PROJECT ARCHITECTURE
==================================================

Before implementation, inspect the entire relevant architecture.

At minimum inspect:

BACKEND:

- ChainEvent.java
- ChainEventType.java
- ChainEventController.java
- ChainEventService.java
- ChainEventServiceImpl.java
- ChainEventRepository.java
- ProcurementEventController.java
- ProcurementEventService.java
- ProcurementEventServiceImpl.java
- existing transportation event implementation
- RecordProcurementEventRequest.java
- ChainEventResponse.java
- ProductionLot.java
- ProductionLotRepository.java
- Shipment.java
- ShipmentRepository.java
- TraceCode.java
- TraceCodeRepository.java
- ProductCategory.java
- ProductCategoryRepository.java
- RoleCode.java
- OrganizationUser.java
- OrganizationUserRepository.java
- SecurityUtils.java
- CustomUserDetails.java
- PermissionChecker.java
- PermissionCheckerImpl.java
- NotificationService.java
- NotificationServiceImpl.java
- Notification.java
- NotificationRepository.java
- GlobalExceptionHandler.java
- BusinessException.java
- ApiResult.java

Also inspect all existing event types and traceability/journey aggregation
code.

FRONTEND:

Inspect at minimum:

- frontend/src/routes/AppRoutes.tsx
- frontend/src/config/roleAccess.ts
- frontend/src/components/layout/Sidebar.tsx
- frontend/src/types/chainEventType.ts
- frontend/src/types/scan.ts
- frontend/src/utils/eventFormatter.ts
- existing procurement event UI
- existing transportation event UI
- existing traceability journey UI
- existing event timeline/journey components
- existing scan/lookup pages
- existing notification UI
- existing API clients
- existing hooks
- existing form components
- existing Card/Badge/Alert/Table components

Also search the project for:

- TRANSPORT
- PROCUREMENT
- chain event
- journey
- timeline
- temperature
- humidity
- storage
- threshold
- warning
- anomaly
- ProductCategory
- traceability

Use the existing implementation patterns whenever possible.

==================================================
7. SUBTASK CV-01 — STORAGE THRESHOLDS
==================================================

The User Story explicitly requires:

"Declare storage thresholds by agricultural product category."

Inspect the current ProductCategory entity and database schema.

Determine whether the existing model already has:

- minimum temperature
- maximum temperature
- minimum humidity
- maximum humidity

or an equivalent storage-threshold structure.

If the required fields already exist:

- reuse them
- do not duplicate them
- do not create another threshold table unnecessarily

If they do NOT exist:

Modify the ProductCategory model and database schema appropriately.

This may require:

- @Entity field additions
- DTO changes
- service changes
- validation changes
- Flyway migration

If a database schema change is required, create a NEW Flyway migration.

DO NOT edit an already-applied migration.

Example naming:

V19__add_storage_thresholds_to_product_categories.sql

BUT:

First inspect the current migration versions.

Use the actual next version number.

Do not blindly use V19 if a higher version already exists.

==================================================
8. STORAGE THRESHOLD MODEL
==================================================

The implementation must support the threshold concept required by the API
documentation and User Story.

At minimum, the system must be capable of representing:

Temperature:

- minimum allowed temperature
- maximum allowed temperature

Humidity:

- minimum allowed humidity
- maximum allowed humidity

However, do NOT automatically create all four fields if the API contract
or existing domain model uses a different structure.

Follow the existing API documentation and domain model.

The threshold must belong to the agricultural product category.

Example:

Product Category:
Cold-chain vegetables

Temperature:
2°C — 8°C

Humidity:
70% — 90%

The exact fields and representation must follow the API specification.

==================================================
9. SUBTASK CV-03 — RECORD STORAGE CHECKPOINT
==================================================

Implement the backend operation for manually recording:

- lot/trace-code reference
- temperature
- humidity
- checkpoint timestamp
- any additional fields required by the API specification

The event must be associated with the correct lot.

Use the existing ChainEvent architecture if appropriate.

Prefer:

ChainEvent
    eventType = STORAGE_MONITORING / equivalent documented event type
    eventData = JSON

if that is consistent with the existing project architecture.

Do NOT create a separate WarehouseReceipt-style CRUD module unless the API
documentation explicitly requires a persistent entity.

Storage monitoring measurements are events/checkpoints and should follow the
existing immutable event architecture.

==================================================
10. TEMPERATURE VALIDATION
==================================================

Temperature outside the allowed storage range is NOT invalid input.

Example:

Allowed:
2°C — 8°C

Input:
15°C

Expected:

VALID EVENT
+
exceedsTemperatureThreshold = true
+
warning/anomaly information

Do NOT reject 15°C merely because it exceeds the configured threshold.

The system must distinguish:

1. Invalid data
   Example: malformed numeric value or invalid domain constraint.

2. Valid but abnormal measurement
   Example: 15°C when allowed range is 2–8°C.

This distinction is critical.

==================================================
11. HUMIDITY VALIDATION
==================================================

Humidity must be between:

0% and 100%.

Examples:

0%     → valid
50%    → valid
100%   → valid
120%   → invalid

The invalid request must be rejected before persistence.

Use Bean Validation where appropriate.

Do not duplicate validation unnecessarily in multiple layers.

==================================================
12. THRESHOLD COMPARISON
==================================================

For each checkpoint, compare:

temperature against:

minTemperature
maxTemperature

and humidity against:

minHumidity
maxHumidity

The response/event data should make it possible to determine:

- whether temperature exceeded the threshold
- whether humidity exceeded the threshold
- whether the checkpoint has any storage-condition violation
- what the actual values were
- what thresholds were applicable

Do not lose the threshold context required to explain why a checkpoint was
marked as abnormal.

If the API specification defines exact response field names, use those names.

==================================================
13. EVENT DATA DESIGN
==================================================

If the project stores ChainEvent details in JSON, structure the event data
according to the API documentation.

Do not store arbitrary JSON.

The event should contain the information required to reconstruct the
storage-monitoring checkpoint and display it in the journey.

Avoid storing redundant data unless required for historical correctness.

IMPORTANT:

Consider whether threshold values should be snapshotted into the event.

If the product category threshold changes later, historical checkpoints
should still be explainable correctly.

Inspect the existing event-data conventions before deciding.

Follow the API specification if it explicitly defines this behavior.

==================================================
14. TRANSPORTATION PRECONDITION
==================================================

Before creating a storage-monitoring checkpoint:

Verify that the lot has at least one transportation event.

Reuse the existing transportation event type and event lookup logic.

Do not create a new transport-event mechanism.

If there is no transportation event:

Reject the request with a clear business error.

Example meaning:

"The lot must have a transportation event before storage monitoring data
can be recorded."

Use the project's existing error-handling conventions.

==================================================
15. TRACEABILITY / QTN-05
==================================================

The event must be attached to the correct valid lot.

Verify:

- trace code exists
- trace code resolves to the correct shipment/lot
- lot exists
- lot has not been recalled
- lot is still valid according to the project's established rules

Reuse existing lookup and validation services.

Do not duplicate trace-code resolution logic if an existing service already
provides it.

==================================================
16. AUTHORIZATION
==================================================

Only authorized internal users who are allowed to record supply-chain
events may create storage-monitoring checkpoints.

The acceptance criteria explicitly state:

VT-06 / public consumer:
READ ONLY

The consumer must not be able to create checkpoints.

Inspect the project's role model and existing event-recording authorization.

Do NOT automatically assume VT-04 is the only role allowed to record unless
the API documentation and existing business rules confirm it.

The User Story is owned by the Procurement Company, but the phrase
"Người ghi sự kiện" may map to an existing event recorder role.

Determine the correct role(s) from:

- API documentation
- existing event architecture
- RoleCode
- roleAccess
- @PreAuthorize
- PermissionChecker

Follow the project's established authorization model.

==================================================
17. PERMISSION SYSTEM
==================================================

The project uses PermissionChecker and Flyway-managed master data.

If a NEW resource/action permission is required:

1. Add the permission through a NEW Flyway data migration.
2. Add appropriate role-permission mappings through Flyway.
3. Do NOT manually INSERT permissions into MySQL.
4. Do NOT modify an already-applied migration.

Example only:

product_storage_monitoring:CREATE

Do NOT invent this exact resource name without first inspecting the
existing permission naming conventions and API/domain design.

If the existing event permission already covers this operation, reuse it.

==================================================
18. FRONTEND — RECORD CHECKPOINT
==================================================

Implement the frontend UI required by the API documentation and User Story.

The UI must support manual entry of:

- lot/trace code
- temperature
- humidity
- checkpoint timestamp
- any other API-required field

The UI must provide immediate feedback where appropriate.

For example:

Temperature:
[ 15 ]

Humidity:
[ 80 ]

If 15°C exceeds the configured 2–8°C range:

show a clear warning such as:

"Vượt ngưỡng bảo quản"

but DO NOT prevent submission solely because it exceeds the threshold.

If humidity = 120:

prevent submission and show:

"Độ ẩm phải nằm trong khoảng 0–100%."

Use existing project form validation conventions.

==================================================
19. FRONTEND — STORAGE JOURNEY DISPLAY
==================================================

The User Story explicitly requires:

"The lot journey displays temperature and humidity checkpoints with an
exceeded-threshold marker."

Therefore, do NOT build only a data-entry form.

The existing traceability/journey UI must be extended to display storage
monitoring checkpoints.

Inspect the existing journey/timeline implementation first.

Add a dedicated visual representation for storage checkpoints.

Example:

Transportation Journey

● 10:00 — Departed
│
● 10:30 — Storage checkpoint
│   Temperature: 4°C
│   Humidity: 80%
│   ✓ Within threshold
│
● 11:30 — Storage checkpoint
│   Temperature: 15°C
│   Humidity: 85%
│   ⚠ Vượt ngưỡng nhiệt độ
│
● 12:30 — Arrived

The actual design must follow the existing UI style.

Do not create a completely separate traceability system.

Extend the existing event formatter/timeline/journey architecture.

==================================================
20. WARNING DISPLAY
==================================================

A checkpoint that exceeds a threshold must be visually distinguishable.

At minimum show:

- actual temperature
- actual humidity
- threshold information if supported by the API response
- warning state

Possible states:

✓ Within storage threshold

⚠ Temperature exceeded

⚠ Humidity exceeded

⚠ Temperature and humidity exceeded

Use the project's existing Badge/Alert/icon conventions.

Do not invent unsupported severity levels.

==================================================
21. PUBLIC TRACEABILITY
==================================================

The public traceability page may DISPLAY storage-monitoring information if
the existing traceability architecture and API documentation support it.

However:

PUBLIC users MUST NOT be able to create or modify storage checkpoints.

The following must never be exposed publicly:

- POST mutation endpoints
- create buttons
- edit buttons
- delete buttons
- administrative controls

Follow the existing public traceability architecture.

==================================================
22. SIDEBAR / NAVIGATION
==================================================

Inspect the existing Sidebar structure.

Do not automatically create a new top-level menu if the existing UX already
has a suitable "Ghi sự kiện" or "Thu mua" group.

Determine the appropriate placement based on existing event-recording
features.

The navigation must be consistent with:

- procurement event
- transportation event
- warehouse receipt
- other chain-event features

If a new menu item is necessary, make it specific and meaningful.

Example:

"Điều kiện bảo quản"

or

"Giám sát bảo quản"

But only use a label after inspecting the existing UI terminology and API
documentation.

==================================================
23. TRACEABILITY DETAIL / JOURNEY
==================================================

If the project already has a page for:

- production lot journey
- traceability
- chain events
- shipment journey

reuse it.

Do NOT create a duplicate "Storage Monitoring Journey" page if the requirement
is to show checkpoints on the existing lot journey.

The preferred architecture is:

Existing Lot Journey
    ├── Procurement Event
    ├── Transportation Event
    ├── Storage Monitoring Checkpoint
    ├── Warehouse Receipt
    └── Other Chain Events

Storage monitoring should become another event type in the journey.

==================================================
24. API CLIENT / TYPES / HOOKS
==================================================

Follow existing frontend architecture.

Create or modify only what is required:

- API client
- TypeScript types
- React hooks
- page/component
- journey event formatter
- route
- role access

Do not duplicate existing API clients or hooks.

==================================================
25. DATABASE / FLYWAY
==================================================

Inspect:

backend/src/main/resources/db/migration/

before creating migrations.

IMPORTANT:

The project uses:

spring.jpa.hibernate.ddl-auto=validate

and Flyway is the source of truth.

Therefore:

- DO NOT use ddl-auto=update.
- DO NOT manually modify MySQL schema.
- DO NOT manually INSERT master data.
- DO NOT edit an already-applied Flyway migration.
- DO NOT create a migration unless the schema/master data actually requires
  one.

Potential migration cases:

A. ProductCategory needs new storage-threshold columns:
    → create new schema migration.

B. New permission is required:
    → create new data migration.

C. New role-permission mapping is required:
    → create new data migration.

D. ChainEvent already supports arbitrary event_type/event_data:
    → no schema migration may be necessary.

E. Existing schema already supports all required fields:
    → do not create a pointless migration.

Use the actual next Flyway version number.

==================================================
26. MIGRATION SAFETY
==================================================

Before creating a migration:

1. Inspect all existing migration files.
2. Determine the highest applied/defined version.
3. Create the next appropriate version.
4. Never reuse an existing version.
5. Never modify an already-applied migration.

If multiple changes are needed, keep them logically organized.

Follow the project's current:

db/migration/schema/
db/migration/data/

structure.

==================================================
27. BACKEND TESTS
==================================================

Implement tests for at least:

TC-01:
2–8°C threshold
4°C, 5°C, 6°C
→ three valid checkpoints
→ no warning

TC-02:
2–8°C threshold
15°C
→ event accepted
→ checkpoint marked as exceeding threshold
→ warning data returned

TC-03:
humidity = 120%
→ request rejected
→ no event persisted

TC-04:
public/consumer attempt
→ rejected
→ no event persisted

Also test:

- lot not found
- recalled/invalid lot
- no transportation event
- product category without storage threshold
- temperature below minimum
- temperature above maximum
- humidity below minimum
- humidity above maximum
- correct trace-code-to-lot association
- authorization

Use existing test patterns.

Do not weaken existing tests.

==================================================
28. FRONTEND TESTING / VALIDATION
==================================================

At minimum verify:

1. Storage checkpoint form renders.
2. Temperature can be entered.
3. Humidity can be entered.
4. Humidity > 100% is rejected.
5. Temperature outside threshold is warned but can still be submitted.
6. Successful submission refreshes/reloads the relevant journey.
7. Within-threshold checkpoint is visually normal.
8. Exceeded-threshold checkpoint displays a warning.
9. Consumer/public traceability has no create functionality.
10. Route guards work correctly.

Run:

npm run build

and any existing frontend type-check/test command.

==================================================
29. DO NOT CREATE UNNECESSARY CRUD
==================================================

This feature represents supply-chain events/checkpoints.

Do NOT automatically create:

- StorageMonitoring entity
- StorageMonitoringRepository
- StorageMonitoring CRUD module
- PUT endpoint
- PATCH endpoint
- DELETE endpoint

unless the API documentation explicitly requires persistent CRUD semantics.

Prefer the existing ChainEvent architecture.

The likely architecture should be:

POST
    → record storage-monitoring event

GET / existing journey endpoint
    → retrieve storage-monitoring checkpoints as part of the lot journey

GET detail/list endpoints
    → only if explicitly required by the API documentation

Do not invent additional APIs merely because they are convenient.

==================================================
30. DO NOT CHANGE EXISTING BUSINESS LOGIC
==================================================

Do not break or alter unrelated:

- procurement events
- transportation events
- warehouse receipts
- traceability
- public lookup
- notifications
- authorization
- existing ChainEvent behavior

If existing shared code must be modified, ensure all existing consumers remain
compatible.

==================================================
31. GIT BRANCH
==================================================

Before implementation, read:

docs/api/event/NCL-05-CN-007-storage-monitoring.md

and determine the branch name specified by that document.

Create the branch using exactly:

git switch -c <BRANCH_NAME_FROM_DOCUMENT>

IMPORTANT:

The branch name MUST come from the API documentation.

Do NOT invent a different branch name if the document already specifies one.

==================================================
32. GIT PUSH
==================================================

After implementation and validation, push ONLY to the feature branch:

git push -u origin <BRANCH_NAME_FROM_DOCUMENT>

Do NOT push to:

- develop
- main
- master
- any other branch

==================================================
33. ABSOLUTELY NO MERGE
==================================================

DO NOT merge this feature branch into:

- develop
- main
- master
- any other branch

Do not create a pull request unless explicitly requested.

The task ends with the feature branch pushed to origin.

==================================================
34. FINAL IMPLEMENTATION CHECKLIST
==================================================

Before declaring completion, verify:

BACKEND:

[ ] API documentation read and followed
[ ] Existing architecture inspected
[ ] Storage monitoring event implemented
[ ] Correct event type used
[ ] Correct lot association
[ ] QTN-05 enforced
[ ] Transportation-event prerequisite enforced
[ ] Product-category threshold prerequisite enforced
[ ] Temperature threshold comparison implemented
[ ] Humidity threshold comparison implemented
[ ] Humidity 0–100 validation implemented
[ ] Exceeded threshold is recorded, not rejected
[ ] Invalid data is rejected
[ ] Authorization enforced
[ ] Public users cannot create events
[ ] Notification behavior implemented only if required by API/business rules
[ ] Flyway migration created only if necessary
[ ] Existing migrations not modified

FRONTEND:

[ ] Manual checkpoint input UI implemented
[ ] Temperature input
[ ] Humidity input
[ ] Checkpoint timestamp
[ ] Threshold information displayed where appropriate
[ ] Immediate validation feedback
[ ] Exceeded threshold warning
[ ] Existing lot journey extended
[ ] Storage checkpoints appear in journey
[ ] Warning marker appears for exceeded checkpoints
[ ] Public traceability remains read-only
[ ] Existing UI conventions reused
[ ] No duplicate traceability page created unnecessarily

DATABASE:

[ ] ddl-auto remains validate
[ ] Flyway remains enabled
[ ] No manual SQL required
[ ] New migration version is correct if migration is needed
[ ] Master data seeded through migration if required

TESTING:

[ ] TC-01 passes
[ ] TC-02 passes
[ ] TC-03 passes
[ ] TC-04 passes
[ ] Backend compile passes
[ ] Backend tests pass
[ ] Frontend type-check passes
[ ] Frontend build passes

GIT:

[ ] Correct feature branch created from API documentation
[ ] Changes committed
[ ] Feature branch pushed to origin
[ ] No merge into develop
[ ] No merge into main
[ ] No merge into master
[ ] No merge into any other branch

==================================================
35. FINAL REPORT
==================================================

At the end, provide a detailed implementation report containing:

1. API documentation inspected
2. Branch name used
3. Existing architecture inspected
4. Backend files inspected
5. Backend files created
6. Backend files modified
7. Frontend files inspected
8. Frontend files created
9. Frontend files modified
10. Flyway files created/modified
11. ProductCategory threshold implementation
12. Storage-monitoring event implementation
13. Event type used
14. Threshold calculation logic
15. Humidity validation
16. QTN-05 validation
17. Transportation prerequisite
18. Authorization rules
19. Public traceability behavior
20. Journey/timeline integration
21. Warning/anomaly UI
22. Tests executed and results
23. Build/type-check results
24. Git commit hash
25. Push result
26. Confirmation that NO merge was performed
27. Any assumptions or limitations

IMPORTANT:

Do not claim "completed" if the implementation only provides the backend
recording API.

The User Story is NOT complete unless the frontend also displays the
temperature/humidity checkpoints in the lot journey with clear exceeded-
threshold warnings.

Do not claim that Flyway migration was required unless the existing schema
actually requires it.

Do not create unnecessary CRUD architecture.

Follow the existing NguonGocSo architecture and the API documentation first.