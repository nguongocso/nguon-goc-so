# NCL-11-CN-007 — Thiết kế giải pháp

## 1. Mục tiêu thiết kế

Thêm một trust boundary công khai, hẹp và có thể kiểm toán để đơn vị kiểm nghiệm nhập kết quả cho đúng một yêu cầu mà không cần tài khoản. Mọi validation, chốt trạng thái, cảnh báo hiệu lực và điều kiện kích hoạt tem vẫn dùng domain logic hiện tại.

## 2. Kiến trúc đề xuất

```text
VT-02 (JWT, đúng tenant)
  └─ POST /inspection-requests/{id}/result-entry-links
       ├─ khóa request theo tenant + trạng thái PENDING_RESULT
       ├─ vô hiệu link ACTIVE cũ
       ├─ sinh 32 byte SecureRandom, chỉ lưu SHA-256
       ├─ lưu link metadata + gửi email async
       └─ trả entryUrl đúng một lần

Đơn vị kiểm nghiệm (không JWT)
  ├─ GET /public/inspection-result-entry/{token}
  │    └─ trả DTO tối thiểu của đúng request
  ├─ POST /public/inspection-result-entry/{token}/criteria/{criterionId}/file
  │    └─ validate token + membership rồi lưu tệp
  └─ PUT /public/inspection-result-entry/{token}/results
       ├─ validate toàn bộ payload trước khi ghi
       ├─ consume token atomic trong cùng transaction
       ├─ gọi shared result domain logic
       ├─ source = TESTING_UNIT_PORTAL, createdBy = null
       ├─ chốt PASSED/FAILED + expiry alert
       └─ lưu used_at/IP/user-agent

Luồng VT-02 nhập tay
  └─ API hiện hữu
       ├─ source = COOPERATIVE_MANUAL
       └─ revoke link ACTIVE của request
```

## 3. Mô hình dữ liệu

### 3.1 Bảng mới `inspection_result_entry_links`

| Cột | Kiểu/ràng buộc | Ý nghĩa |
|---|---|---|
| `id` | `CHAR(36) PK` | ID nội bộ |
| `inspection_request_id` | `CHAR(36) NOT NULL FK` | Request duy nhất được quyền đọc/ghi |
| `organization_id` | `CHAR(36) NOT NULL FK` | Tenant snapshot để kiểm chéo |
| `testing_unit_id` | `CHAR(36) NOT NULL FK` | Đơn vị được cấp link |
| `recipient_email` | `VARCHAR(255) NOT NULL` | Email nhận link, snapshot lúc cấp |
| `token_prefix` | `VARCHAR(16) NOT NULL` | Phục vụ audit/màn hình quản lý, không đủ để xác thực |
| `token_hash` | `CHAR(64) NOT NULL UNIQUE` | SHA-256 của raw token |
| `status` | `VARCHAR(20) NOT NULL` | `ACTIVE`, `USED`, `REVOKED`, `EXPIRED` |
| `expires_at` | `DATETIME NOT NULL` | Hết hạn tuyệt đối |
| `used_at` | `DATETIME NULL` | Thời điểm submit thành công |
| `used_ip` | `VARCHAR(45) NULL` | Audit public |
| `used_user_agent` | `VARCHAR(500) NULL` | Audit public, cắt giới hạn |
| `created_by` | `CHAR(36) NOT NULL FK` | VT-02 cấp link |
| `created_at` | `DATETIME NOT NULL` | Thời điểm cấp |
| `revoked_by` | `CHAR(36) NULL FK` | Người cấp lại/ghi tay làm link cũ mất hiệu lực |
| `revoked_at` | `DATETIME NULL` | Thời điểm vô hiệu |

Indexes: unique `token_hash`; index `(inspection_request_id, status)`; `(organization_id, created_at)`; `(status, expires_at)`. Không cascade delete request/result; lịch sử link phải được giữ.

### 3.2 Thay đổi `inspection_criterion_results`

| Thay đổi | Quy tắc |
|---|---|
| Thêm `entry_source VARCHAR(32) NOT NULL DEFAULT 'COOPERATIVE_MANUAL'` | Backfill dữ liệu cũ là HTX nhập |
| Thêm `portal_link_id CHAR(36) NULL FK` | Chỉ có khi nguồn là portal |
| Cho `created_by` nullable | Public actor không có user; manual vẫn bắt buộc ở service |

Enum Java/API: `COOPERATIVE_MANUAL`, `TESTING_UNIT_PORTAL`. Không lưu label tiếng Việt trong DB enum.

### 3.3 Invariants

1. `TESTING_UNIT_PORTAL` ⇒ `portal_link_id != null`, `created_by == null`.
2. `COOPERATIVE_MANUAL` ⇒ `created_by != null`, `portal_link_id == null`.
3. Link và request phải cùng `organization_id`; request phải trỏ đúng `testing_unit_id`.
4. Chỉ một link `ACTIVE` có hiệu lực theo request; cấp lại chạy trong transaction và khóa các link hiện hành.
5. Một token chỉ chuyển `ACTIVE → USED` đúng một lần bằng conditional update.
6. Link `EXPIRED/USED/REVOKED` không trả dữ liệu request và không upload/submit.

## 4. State model

### 4.1 Link

| Từ | Sang | Tác nhân/điều kiện |
|---|---|---|
| Không có | `ACTIVE` | VT-02 đúng tenant, request `PENDING_RESULT` |
| `ACTIVE` | `USED` | Public submit hợp lệ và atomic consume thành công |
| `ACTIVE` | `REVOKED` | VT-02 cấp lại hoặc bắt đầu ghi kết quả thủ công |
| `ACTIVE` | `EXPIRED` | `now >= expires_at`, lazy update hoặc scheduler tùy chọn |
| `USED/REVOKED/EXPIRED` | — | Terminal; muốn dùng phải cấp link mới |

### 4.2 Request/result

- Giữ nguyên `PENDING_RESULT`, `PASSED`, `FAILED`, `CANCELLED`.
- Public submit chỉ cho request `PENDING_RESULT` và phải đủ toàn bộ chỉ tiêu.
- Manual API tiếp tục theo rule hiện hành (`PENDING_RESULT` hoặc `FAILED`); mọi mutation manual đổi source của result được ghi thành `COOPERATIVE_MANUAL`.
- Không thêm trạng thái “LINK_SENT” vào request; link status thuộc entity riêng.

## 5. API contract đề xuất

Tất cả response JSON dùng `ApiResult<T>`. Tài liệu API chính thức cần được tạo tại `docs/api/certification/inspection-result-entry-portal.md` trước khi code.

### 5.1 Cấp hoặc cấp lại link — authenticated

`POST /api/v1/inspection-requests/{requestId}/result-entry-links`

- Auth: ACCESS JWT, role `VT-02`.
- Tenant: request phải thuộc organization trong JWT.
- Cấp mới và cấp lại dùng cùng endpoint; link active cũ bị revoke.

Request:

```json
{
  "recipientEmail": "lab@example.vn",
  "expiryDays": 7
}
```

Validation: email hợp lệ; `expiryDays` mặc định 7, min 1, max 30; request `PENDING_RESULT`; có `testingUnitId`; testing unit của link trùng request.

Response `201` (raw URL chỉ trả lần này):

```json
{
  "id": "uuid",
  "status": "ACTIVE",
  "recipientEmail": "lab@example.vn",
  "expiresAt": "2026-09-23T10:00:00",
  "entryUrl": "https://frontend/inspection-result-entry/<raw-token>"
}
```

Không trả `tokenHash`; không ghi `entryUrl` vào log.

### 5.2 Xem trạng thái link mới nhất — authenticated

`GET /api/v1/inspection-requests/{requestId}/result-entry-links/latest`

- Auth/tenant như endpoint cấp.
- Response `200` chứa `id`, `status`, email, expiry, usedAt, createdAt; **không có raw token/URL**.
- `404` nếu chưa từng cấp.

### 5.3 Mở cổng public

`GET /api/v1/public/inspection-result-entry/{token}`

- Không JWT.
- Token thô chỉ dùng để hash và lookup.
- Header: `Cache-Control: no-store`, `Pragma: no-cache`, `Referrer-Policy: no-referrer`.
- DTO tối thiểu: tên/mã lô hiển thị, tên đơn vị, ngày gửi mẫu, thời hạn link, các chỉ tiêu snapshot và kết quả nháp hiện có nếu được phép. Không trả organization ID, user ID, creator, testingUnitId, lot UUID hoặc file system path.

```json
{
  "testingUnit": "Trung tâm kiểm nghiệm A",
  "lotCode": "Lô Xoài 09/2026",
  "sampleSentDate": "2026-09-15",
  "expiresAt": "2026-09-23T10:00:00",
  "criteria": [
    {
      "criterionId": "uuid-snapshot",
      "code": "RESIDUE_PESTICIDE",
      "name": "Dư lượng thuốc BVTV",
      "standardName": "VietGAP"
    }
  ]
}
```

### 5.4 Upload phiếu theo token

`POST /api/v1/public/inspection-result-entry/{token}/criteria/{criterionId}/file`

- Multipart field `file`.
- Token active, chưa hết hạn; criterion phải thuộc đúng request của token.
- JPG/PNG/PDF; max 5 MB; tên server sinh ngẫu nhiên; không tin extension/path từ client.
- Response trả `filePath`/opaque handle chỉ được dùng lại trong submit của cùng token/criterion.

### 5.5 Submit public

`PUT /api/v1/public/inspection-result-entry/{token}/results`

Request giữ cấu trúc batch hiện hữu:

```json
{
  "results": [
    {
      "criterionId": "uuid-snapshot",
      "resultDate": "2026-09-16",
      "expiryDate": "2027-09-16",
      "passed": true,
      "filePath": "inspection-results/.../file.pdf"
    }
  ]
}
```

Các bước transaction:

1. Hash token, lookup link/request và lock cần thiết.
2. Kiểm trạng thái/hết hạn/request `PENDING_RESULT`/tenant/testing unit.
3. Validate đủ toàn bộ criterion, không trùng, ngày và file ownership.
4. Atomic update link `ACTIVE → USED` với `expires_at > now`; nếu 0 row thì rollback.
5. Save/update result với `TESTING_UNIT_PORTAL`, `createdBy=null`, `portalLinkId=link.id`.
6. Chốt request status và trigger expiry alert như manual flow.
7. Lưu usedAt/IP/user-agent; commit.

Response `200` dùng `InspectionCriterionResultResponse[]`, bổ sung additive `entrySource` và `createdByName` nullable.

### 5.6 Mở rộng API hiện hữu

- `InspectionCriterionResultResponse`: thêm `entrySource`; `createdByName` nullable/hiển thị tên đơn vị khi portal nếu cần ở presentation layer.
- `PublicInspectionCriterionResultDto`: thêm `entrySource`.
- Không đổi path/payload bắt buộc của manual APIs.

## 6. Error contract

| HTTP | Trường hợp | Thông điệp public/manager |
|---:|---|---|
| 400 | Email/expiry/payload/date/file không hợp lệ | Chi tiết validation theo `ApiResult` |
| 403 | Không phải VT-02 | “Bạn không có quyền thực hiện chức năng này” |
| 404 | Manager request khác tenant/không tồn tại | “Yêu cầu kiểm nghiệm không tồn tại” |
| 404 | Token ngẫu nhiên/không tồn tại | “Liên kết không hợp lệ hoặc không còn hiệu lực” |
| 409 | Request không còn `PENDING_RESULT`, manual/public race | “Yêu cầu kiểm nghiệm không còn ở trạng thái chờ kết quả” |
| 410 | Token hết hạn | “Liên kết đã hết hạn. Vui lòng liên hệ hợp tác xã để được cấp lại.” |
| 410 | Token đã dùng/revoked | “Liên kết đã được sử dụng hoặc đã được thay thế.” |
| 413 | File quá 5 MB | Thông điệp giới hạn file |
| 415 | Loại file không hỗ trợ | Chỉ JPG/PNG/PDF |
| 429 | Vượt hạn mức portal | “Bạn thao tác quá nhanh. Vui lòng thử lại sau.” |

## 7. Authorization và tenant isolation

### Authenticated side

- Repository/service query phải scope bằng `requestId + organizationId`, không `findById` rồi tin client.
- Chỉ `VT-02`; UI hide action nhưng backend là điểm kiểm soát thật.
- Cấp link chỉ cho request có `testingUnitId` và đúng tenant.

### Public side

- Client không truyền organization/request ID để chọn scope; token quyết định toàn bộ scope.
- Sau lookup phải kiểm `link.organizationId == request.productionLot.organizationId` và `link.testingUnitId == request.testingUnitId`.
- Criterion/file phải được xác minh thuộc request token trước mọi đọc/ghi.
- Lỗi token không tồn tại trả generic; không trả tenant/user/internal IDs.

## 8. Bảo mật

- Token 32 byte từ `SecureRandom`, encode URL-safe; SHA-256 lưu DB; so khớp hash.
- Không lưu, log, audit, telemetry raw token hoặc full entry URL.
- Raw URL chỉ trả ở response cấp link và trong email; GET trạng thái không tái tạo được.
- Conditional consume chống replay/race; transaction rollback giữ token active nếu save thất bại.
- Throttle mặc định: 60 request/giờ/valid-token và 30 lần token-invalid/giờ/IP; trả 429. Triển khai ban đầu có thể dùng service in-memory theo pattern hiện có, nhưng phải ghi chú giới hạn multi-instance trong API docs/vận hành.
- Response public `no-store`; không đưa token vào analytics/referrer; trang frontend tránh tải tài nguyên bên thứ ba trước khi token được loại khỏi referrer.
- File validation kiểm MIME cho phép, size, server-generated filename và normalized path.
- Email/log chỉ dùng token prefix hoặc link ID; email lỗi không log URL.

## 9. Frontend design

### Manager UI

- Mở rộng `InspectionRequestActionButtons` cho request `PENDING` với action “Cấp link cho đơn vị”.
- Dialog nhập/xác nhận email, thời hạn; hiển thị trạng thái link mới nhất.
- Sau cấp: báo email đã gửi, cho copy `entryUrl` đúng trong phiên response; cấp lại cảnh báo link cũ mất hiệu lực.

### Public portal

- Route `/inspection-result-entry/:token`, ngoài `PrivateRoute`.
- Các state: loading, invalid, expired, used/revoked, active form, submitting, success.
- Tách editor/form dùng chung từ `RecordInspectionResultPage`; adapter API public khác API JWT nhưng validation UI giống nhau.
- Không hiển thị breadcrumb nội bộ, lot UUID, menu tổ chức hoặc chức năng khác.
- Sau success không cho quay lại chỉnh sửa; refresh hiển thị trạng thái đã dùng.

### Provenance UI

- Màn hình nội bộ và public trace map enum sang badge:
  - `TESTING_UNIT_PORTAL` → “Đơn vị kiểm nghiệm khai”.
  - `COOPERATIVE_MANUAL` → “Hợp tác xã nhập”.
- Không suy nguồn từ `createdByName`; chỉ dùng `entrySource` từ backend.

## 10. Email và audit

- Thêm `EmailService.sendInspectionResultEntryEmail(...)` và template HTML riêng.
- Email chứa tên HTX, đơn vị kiểm nghiệm, mã/tên lô, thời hạn và link; không chứa toàn bộ kết quả.
- Cấp/cấp lại ghi `ActivityLogEvent` bằng actor VT-02, action `ISSUE_INSPECTION_RESULT_LINK`/`REISSUE_INSPECTION_RESULT_LINK`, email được mask.
- Public submit không ghi vào `activity_logs` vì schema bắt buộc user; link lifecycle + result source là audit authoritative. Không nới nullable `activity_logs` trong Story này.

## 11. Tương thích và không hồi quy

- Manual API vẫn hoạt động với payload/path cũ.
- Dữ liệu cũ tự nhận source manual qua default/backfill.
- Status request, QTN-21, expiry warning, failed/reinspection và public history giữ nguyên.
- Field mới là additive; consumer cũ bỏ qua được.
- Không sửa migration cũ, partner API key, invitations, roles/permissions, danh mục testing unit hay cấu trúc activity log.
