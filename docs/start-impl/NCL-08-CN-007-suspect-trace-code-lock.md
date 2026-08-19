You are working on the Nguon Goc So project.

Your task is to fully implement the following User Story end-to-end across the existing backend and frontend.

==================================================
USER STORY
==================================================

ID:
NCL-08-CN-007

Title:
Khóa mã tem nghi vấn theo dấu hiệu quét bất thường

User Story:
Là Quản trị viên nền tảng, tôi muốn khóa những mã tem có dấu hiệu bị làm giả, để người tiêu dùng quét trúng tem giả được cảnh báo ngay thay vì thấy hành trình bình thường.

Actor:
Quản trị viên nền tảng (VT-01)

Business Value:
Chuyển từ chỉ cảnh báo sang chặn được tem nghi giả.

Description:
Hệ thống chấm mức nghi vấn cho mã tem dựa trên:
- số lượt quét vượt ngưỡng;
- khoảng cách địa lý giữa hai lượt quét liên tiếp;
- thời gian giữa hai lượt quét.

Quản trị viên:
1. Xem danh sách mã tem nghi vấn.
2. Xem thông tin/dấu hiệu khiến mã tem bị nghi vấn.
3. Khóa mã tem nghi vấn.
4. Bắt buộc nhập lý do khóa.
5. Sau khi khóa, mã tem phải chuyển sang trạng thái bị khóa.
6. Khi người tiêu dùng tra cứu công khai mã tem bị khóa, phải hiển thị cảnh báo nghi vấn thay vì hiển thị hành trình bình thường như một mã tem hợp lệ.

IMPORTANT:
- Đây là chức năng dành cho QUẢN TRỊ VIÊN NỀN TẢNG.
- Không được cho VT-02 hoặc các role khác khóa mã tem.
- Phát hiện nghi vấn KHÔNG được tự động khóa mã tem.
- Chỉ VT-01 mới được xác minh và thực hiện khóa.
- Không tạo một module CRUD TraceCode mới nếu kiến trúc hiện tại đã có TraceCode/scan/traceability flow. Hãy tích hợp vào kiến trúc hiện có.

==================================================
ACCEPTANCE CRITERIA
==================================================

TC-01 — Luồng thành công

Given:
- Một mã tem có ít nhất hai lượt quét.
- Hai lượt quét cách nhau 500 km trong 10 phút.

When:
- Hệ thống chấm mức nghi vấn.

Then:
- Mã tem được đưa vào danh sách nghi vấn.
- Hệ thống hiển thị lý do/dấu hiệu:
  "Khoảng cách không hợp lý" hoặc thông tin tương đương phù hợp với terminology hiện tại của project.

Priority:
HIGH

--------------------------------------------------

TC-02 — Luồng thành công

Given:
- Một mã tem đang nằm trong danh sách nghi vấn.

When:
- VT-01 mở thông tin mã tem.
- VT-01 nhập lý do khóa.
- VT-01 xác nhận khóa.

Then:
- Mã tem chuyển sang trạng thái bị khóa.
- Lý do khóa được lưu lại.
- Trang tra cứu công khai của mã tem phải hiển thị cảnh báo nghi vấn.

Priority:
HIGH

--------------------------------------------------

TC-03 — Không có quyền

Given:
- Người dùng là VT-02 (Quản lý hợp tác xã).

When:
- Người dùng cố gắng khóa một mã tem.

Then:
- Backend phải từ chối request.
- HTTP status phải phù hợp với security convention hiện tại.
- Không được thay đổi trạng thái mã tem.
- Thông báo phải thể hiện rằng chỉ Quản trị viên nền tảng mới được khóa mã.

Priority:
HIGH

--------------------------------------------------

TC-04 — Dữ liệu không hợp lệ

Given:
- VT-01 đang khóa mã tem.

When:
- VT-01 để trống lý do khóa và xác nhận.

Then:
- Backend phải từ chối request.
- Frontend phải validate.
- Phải yêu cầu nhập lý do khóa.
- Không được thay đổi trạng thái mã tem.

Priority:
HIGH

==================================================
BUSINESS RULE
==================================================

QTN-10 — Phát hiện quét bất thường

Một mã bị quét quá nhiều lần ở nhiều nơi bị đánh dấu bất thường.

Actor:
- Quản trị viên nền tảng

Subject:
- Lô hàng và tem

Condition:
- Số lượt quét của một mã vượt ngưỡng theo thời gian và vị trí.

Expected behavior:
- Hệ thống đánh dấu mã nghi ngờ.
- Hệ thống gửi cảnh báo.
- KHÔNG tự động khóa mã khi chưa được VT-01 xác minh.

IMPORTANT:
The implementation must preserve this distinction:

ANOMALY DETECTION
    ↓
SUSPECT TRACE CODE
    ↓
VT-01 REVIEW
    ↓
MANUAL LOCK
    ↓
PUBLIC WARNING

Do NOT implement:

ANOMALY DETECTION
    ↓
AUTOMATIC LOCK

==================================================
SUBTASKS
==================================================

NCL-08-CN-007-CV-01
Chốt luật chấm mức nghi vấn
- Xác định ngưỡng số lượt quét.
- Xác định ngưỡng khoảng cách.
- Xác định ngưỡng thời gian.
- Kiểm tra project hiện tại đã có cấu hình/constant/business rule nào liên quan hay chưa.
- Không tự ý tạo một cơ chế cấu hình mới nếu project đã có cơ chế phù hợp.

NCL-08-CN-007-CV-02
Thiết kế màn hình danh sách nghi vấn
- Dựng giao diện xem danh sách trace code nghi vấn.
- Hiển thị thông tin cần thiết để VT-01 đánh giá.
- Có thể lọc/tìm kiếm nếu phù hợp với kiến trúc hiện tại.
- Có action để xem chi tiết.
- Có action để khóa mã khi đủ điều kiện.

NCL-08-CN-007-CV-03
Phát triển chấm nghi vấn và khóa mã
- Backend tính mức nghi vấn.
- Backend xác định lý do/dấu hiệu.
- Backend cho phép VT-01 khóa mã.
- Bắt buộc lý do khóa.
- Lưu trạng thái và lý do khóa.
- Không cho role khác thực hiện khóa.

NCL-08-CN-007-CV-04
Hiển thị cảnh báo mã bị khóa khi tra cứu
- Tích hợp với public trace/lookup flow hiện tại.
- Khi trace code bị khóa, public user phải thấy cảnh báo nghi vấn.
- Không được làm hỏng flow tra cứu mã hợp lệ hiện tại.

NCL-08-CN-007-CV-05
Kiểm thử
- Kiểm thử phát hiện quét bất thường.
- Kiểm thử khoảng cách/thời gian bất hợp lý.
- Kiểm thử khóa bởi VT-01.
- Kiểm thử VT-02 không thể khóa.
- Kiểm thử lý do khóa bắt buộc.
- Kiểm thử public lookup của mã đã bị khóa.
- Kiểm thử mã bình thường không bị ảnh hưởng.

==================================================
STEP 1 — READ API DOCUMENTATION FIRST
==================================================

BEFORE modifying ANY source code, you MUST read and understand:

docs/api/trace/NCL-08-CN-007_SuspectTraceCodeLock.md

Treat this file as the API contract for this feature.

Do NOT blindly implement the User Story above without reading the API documentation.

Extract and understand:
- API endpoints
- HTTP methods
- request DTOs
- response DTOs
- validation rules
- authorization requirements
- trace-code status behavior
- anomaly scoring rules
- error responses
- public lookup behavior
- pagination/filtering requirements
- existing naming conventions
- expected frontend/backend integration

If the API documentation conflicts with the User Story:
1. Identify the conflict.
2. Prefer the explicit API contract where appropriate.
3. Do not silently invent behavior.
4. Preserve the business intent of the User Story.
5. Document any unavoidable assumption in the final report.

==================================================
STEP 2 — READ THE EXISTING PROJECT
==================================================

Before coding, inspect the existing architecture.

You MUST search the entire project for relevant existing implementations.

Backend areas to inspect include, but are not limited to:

- TraceCode entity
- TraceCode repository
- TraceCode service
- TraceCode controller
- public trace/lookup controller
- public trace/lookup service
- scan-related entities
- scan-related repositories
- scan-related services
- scan statistics
- anomaly detection
- alert system
- notification system
- chain events
- Shipment
- ProductionLot
- organization/user/role security
- PermissionChecker
- RoleCode
- SecurityUtils
- existing status enums
- existing exception handling
- existing pagination
- existing migration files

Search for concepts such as:

- TraceCode
- trace code
- scan
- scan history
- scan statistics
- suspicious
- anomaly
- abnormal
- alert
- warning
- locked
- blocked
- status
- public lookup
- traceability
- distance
- latitude
- longitude
- timestamp
- QR
- code activation

Frontend areas to inspect include, but are not limited to:

- Sidebar
- roleAccess
- AppRoutes
- TraceCode pages
- Scan pages
- Scan statistics
- Alert pages
- public trace page
- public lookup components
- existing detail pages
- existing list/table pages
- Dialog/Sheet patterns
- API clients
- hooks
- types
- notification/toast patterns

IMPORTANT:
Reuse existing architecture whenever possible.

Do NOT create duplicate:
- TraceCode entity
- scan entity
- scan service
- alert system
- notification system
- authentication mechanism
- permission mechanism
- public lookup mechanism

unless the existing project genuinely lacks the required capability.

==================================================
STEP 3 — DATABASE / ENTITY ANALYSIS
==================================================

The project uses:

spring.jpa.hibernate.ddl-auto=validate

and Flyway.

Therefore:

NEVER use Hibernate schema auto-update.

Before creating a migration, determine whether the existing schema can support the feature.

The feature may require changes such as:
- TraceCode status
- lockedAt
- lockedBy
- lockReason
- suspicion/anomaly information
- scan-related fields
- indexes
- audit/history information

BUT DO NOT add fields blindly.

First inspect existing entities and migrations.

If the existing schema already supports the required behavior:
- DO NOT create unnecessary migration files.

If schema changes are genuinely required:
- Modify the appropriate @Entity.
- Create a NEW Flyway migration.
- Never modify an already-applied migration.
- Follow the project's current migration naming/versioning convention.
- Make sure Hibernate validation passes.

Master data:
- If a new permission/resource/action is required, add it through Flyway master-data migration.
- Do NOT manually INSERT permissions into MySQL.
- Do NOT put runtime business data into migration files.

Runtime data such as:
- suspicious trace codes
- scan records
- lock records
- lock reasons

must NOT be seeded into migration files unless explicitly required as master/demo data by the existing project architecture.

==================================================
STEP 4 — DESIGN THE BACKEND
==================================================

Implement the API defined by:

docs/api/trace/NCL-08-CN-007_SuspectTraceCodeLock.md

Follow existing project conventions.

Potential responsibilities include:

1. Detect suspicious trace codes.
2. Calculate suspicion indicators.
3. Return suspicious trace-code list.
4. Return trace-code suspicion details.
5. Allow VT-01 to manually lock a suspicious trace code.
6. Require lock reason.
7. Persist lock metadata.
8. Prevent unauthorized roles from locking.
9. Update public trace lookup behavior.
10. Emit alert/notification using existing infrastructure if required.

Do NOT automatically lock when anomaly detection occurs.

The detection mechanism must only mark the trace code as suspicious.

Only explicit VT-01 action may transition it to LOCKED.

==================================================
STEP 5 — ANOMALY / SUSPICION LOGIC
==================================================

Read the API documentation carefully to determine exact thresholds.

Do NOT arbitrarily invent thresholds if they are defined in the API documentation or existing project.

The system must consider the relevant scan history.

At minimum, the logic must support the business scenario:

Example:

Scan A:
- location A

Scan B:
- location B
- approximately 500 km away
- approximately 10 minutes later

This must be recognized as suspicious when the documented threshold/rule says so.

The implementation should identify:
- suspicious trace code
- first/most relevant suspicious scan pair
- reason
- relevant timestamps
- geographic information where available
- calculated distance
- elapsed time
- any score/threshold required by API documentation

Avoid unnecessary floating-point/geospatial complexity if the existing project already has a utility for this.

Reuse existing distance/location utilities if present.

==================================================
STEP 6 — TRACE CODE LOCKING
==================================================

Implement manual locking according to the API contract.

Rules:

- Only VT-01 can lock.
- Lock reason is mandatory.
- Blank reason must be rejected.
- A normal/non-suspicious trace code should not be lockable unless API documentation explicitly permits it.
- Locking must be idempotent or safely reject already locked codes according to existing project conventions.
- Store:
  - locked status
  - lock reason
  - locking user
  - lock timestamp
  if these fields are required by the existing design/API contract.

Do NOT delete the trace code.

Do NOT delete scan history.

Do NOT modify historical chain events just to implement locking.

==================================================
STEP 7 — PUBLIC TRACE / LOOKUP
==================================================

This is a critical requirement.

Inspect the existing public trace page and backend public lookup flow.

When a trace code is LOCKED:

The public user must immediately see a clear warning that the trace code is suspected/fraudulent/locked according to the terminology defined by the API documentation.

The public lookup must NOT incorrectly present the locked trace code as a normal trustworthy traceability result.

However:

Do NOT break normal lookup behavior for:
- valid active trace codes
- valid unlocked trace codes
- invalid trace codes
- other existing statuses

Only modify the minimum required parts of the public lookup flow.

==================================================
STEP 8 — FRONTEND UX
==================================================

Build the UI according to the existing project design system.

The primary management screen should be appropriate for VT-01.

Prefer an architecture such as:

Sidebar
  ↓
"Quản lý mã tem nghi vấn"
  ↓
Suspicious Trace Code List
  ├── suspicious indicators
  ├── scan count
  ├── suspicious reason
  ├── latest scan
  ├── status
  └── actions
        ├── View detail
        └── Lock

When VT-01 clicks "Lock":
- open a confirmation dialog
- show trace-code information
- show suspicion reason
- require lock reason
- disable/avoid submission when reason is empty
- submit to backend
- refresh list after success
- show success/error toast

Do NOT build a giant CRUD module if the domain is event/status driven.

The UI should reflect the actual business flow:
Detection → Review → Manual Lock.

==================================================
STEP 9 — SIDEBAR / ROUTING / ROLE ACCESS
==================================================

Inspect the current Sidebar architecture.

Add a suitable menu item for VT-01 only if the project does not already have an appropriate screen.

The menu item should clearly communicate its purpose, for example:

"Tem nghi vấn"

or an equivalent name consistent with the existing terminology.

Requirements:
- Visible to VT-01.
- Not visible to VT-02 through VT-06 unless explicitly required by existing access rules/API documentation.
- Route must use the existing RoleRoute/security mechanism.
- Do not bypass the existing authorization architecture.

If an existing TraceCode management/alert page is suitable, integrate into it instead of creating a duplicate menu item.

==================================================
STEP 10 — AUTHORIZATION
==================================================

Follow existing security architecture.

Backend authorization MUST be enforced.

Frontend hiding the button/menu is NOT sufficient.

Verify:

VT-01:
- can view suspicious trace codes
- can view details
- can lock

VT-02:
- cannot lock

VT-03:
- cannot lock

VT-04:
- cannot lock unless explicitly specified by the API contract

VT-05:
- cannot lock unless explicitly specified

VT-06:
- public consumer cannot lock

If a permission-based authorization mechanism is already used for TraceCode management:
- reuse it.

If a new permission is genuinely required:
- add it through Flyway master data migration.
- update the appropriate role-permission mapping.
- do NOT manually modify MySQL.

==================================================
STEP 11 — MIGRATION
==================================================

Inspect:

backend/src/main/resources/db/migration

before creating anything.

Remember:

DDL:
- schema changes
- ALTER TABLE
- indexes
- constraints

Master data:
- permissions
- role-permission mappings

Runtime data:
- scan records
- suspicious trace codes
- lock records

Do NOT seed runtime records into Flyway.

If migration is necessary:
- create a new V__ migration.
- do not edit existing applied migrations.
- ensure compatibility with MySQL.
- ensure Hibernate ddl-auto=validate passes.

If no migration is required:
- explicitly state:
  "No Flyway migration required."

==================================================
STEP 12 — TESTING
==================================================

Implement tests for backend business logic and controller/security behavior.

At minimum verify:

1. Suspicious trace code is detected.
2. Two scans with abnormal distance/time produce suspicion.
3. Normal scans do not produce false positive suspicion.
4. Suspicious list returns expected records.
5. VT-01 can view suspicious records.
6. VT-01 can lock.
7. Lock reason is mandatory.
8. Empty lock reason is rejected.
9. VT-02 cannot lock.
10. Other unauthorized roles cannot lock.
11. Locked status is persisted.
12. Public lookup of locked trace code returns warning.
13. Public lookup of normal trace code remains unchanged.
14. Already locked trace code follows documented behavior.
15. Missing/invalid trace code follows existing error conventions.

Use the project's existing:
- JUnit
- Mockito
- MockMvc
- integration test
- security test
patterns.

Do not weaken existing tests or remove unrelated tests to make the build pass.

==================================================
STEP 13 — FRONTEND VALIDATION
==================================================

Run the project's normal frontend checks.

At minimum:

- TypeScript type-check
- production build
- relevant lint/check commands if configured

Verify:
- route works
- sidebar highlighting works
- list loads
- detail works
- lock dialog works
- validation works
- API errors are displayed correctly
- public trace warning works

==================================================
STEP 14 — BACKEND VALIDATION
==================================================

Run:

- compilation
- relevant unit tests
- relevant integration/controller tests
- full test suite if practical

Confirm:

spring.jpa.hibernate.ddl-auto=validate

still works.

Confirm Flyway migrations execute successfully if a new migration was created.

==================================================
STEP 15 — GIT BRANCH
==================================================

Before making changes, inspect the API documentation file:

docs/api/trace/NCL-08-CN-007_SuspectTraceCodeLock.md

Use the branch name specified by that documentation.

Create the branch exactly using:

git switch -c <BRANCH_NAME_FROM_API_DOC>

Do NOT invent another branch name if the API documentation already specifies one.

At the end, push ONLY this feature branch:

git push -u origin <BRANCH_NAME_FROM_API_DOC>

ABSOLUTELY FORBIDDEN:

- Do NOT merge into develop.
- Do NOT merge into main.
- Do NOT merge into master.
- Do NOT merge into any other branch.
- Do NOT rebase this feature onto another branch unless explicitly required.
- Do NOT push changes to develop/main/master.
- Do NOT force push unless absolutely necessary and explicitly instructed.

==================================================
STEP 16 — CODE QUALITY
==================================================

Follow the existing project conventions.

Do not:
- duplicate existing services
- duplicate existing entities
- duplicate public lookup logic
- bypass PermissionChecker/security
- hardcode credentials
- hardcode database records
- manually modify database
- modify applied Flyway migrations
- introduce unnecessary dependencies
- create unnecessary CRUD endpoints
- create unnecessary CRUD pages

Prefer:
- existing services
- existing repositories
- existing DTO conventions
- existing ApiResult
- existing PageResponse
- existing exception handling
- existing notification/alert infrastructure
- existing frontend API/hook patterns
- existing UI components
- existing role/permission architecture

==================================================
STEP 17 — FINAL VERIFICATION
==================================================

Before finishing, verify the complete flow:

1. Scan history exists.
2. System analyzes scan behavior.
3. Suspicious trace code appears in VT-01 management UI.
4. VT-01 opens detail.
5. VT-01 enters lock reason.
6. Backend validates authorization and reason.
7. Trace code becomes LOCKED.
8. Lock metadata is persisted.
9. Public lookup detects LOCKED status.
10. Public user sees warning.
11. Normal trace codes still work.
12. VT-02 cannot lock.
13. No automatic lock occurs merely because a code is suspicious.
14. No unrelated functionality is broken.

==================================================
FINAL REPORT
==================================================

After implementation, provide a concise but complete implementation report containing:

1. Branch name
2. API documentation findings
3. Existing architecture reused
4. Files inspected
5. Files created
6. Files modified
7. Files deleted, if any
8. Backend endpoints
9. Frontend routes
10. Sidebar changes
11. Authorization/permission changes
12. Suspicion scoring/detection logic
13. Locking behavior
14. Public lookup behavior
15. Flyway migrations created, or explicitly state none were required
16. Tests executed and results
17. Backend build result
18. Frontend build/type-check result
19. Git commit hash(es)
20. Push result
21. Confirmation that NO merge was performed
22. Any assumptions or limitations

IMPORTANT:
Do not claim a feature is complete merely because the backend compiles.

The feature is considered complete only when:
- Backend API works.
- Frontend management UI works.
- VT-01 can review and lock suspicious trace codes.
- Unauthorized users cannot lock.
- Lock reason is mandatory.
- Public lookup displays the required warning for locked trace codes.
- Tests pass.
- Build passes.
- Changes are pushed only to the specified feature branch.