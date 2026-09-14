# Xuất nhật ký hoạt động phục vụ kiểm tra

> Jira Story: NCL-706 (`NCL-08-CN-015`)
>
> Nhánh triển khai: `feature/NCL-08-CN-015-export-activity-log`
>
> Epic: NCL-79 · Task tài liệu/thiết kế: NCL-854 · Liên quan: NCL-857, NCL-860, NCL-862
>
> Phụ thuộc: NCL-101 và `NCL-08-CN-004` (xem lịch sử hoạt động)
>
> Loại tài liệu: API Contract-first · Trạng thái: **Proposed** · Phiên bản: v1
>
> Lưu ý: các API, bảng và trường được đánh dấu **đề xuất** trong tài liệu này chưa được source hiện tại triển khai.

## 1. Mục tiêu

Cho phép Quản lý tổ chức xuất các bản ghi nhật ký hoạt động thuộc **chính tổ chức hiện tại** để phục vụ kiểm tra. Người dùng có thể lọc, xem trước số lượng, yêu cầu xuất CSV trực tiếp hoặc nền; lần xuất thành công phải tự ghi vết `EXPORT_ACTIVITY_LOG` mà không tự lọt vào file của chính lần xuất đó.

## 2. Actor và quyền

| Actor | Quyền | Phạm vi |
|---|---|---|
| Quản lý tổ chức (`VT-02` / `ORG_MANAGER`) | Preview, yêu cầu export, xem trạng thái và tải file export của mình/tổ chức mình | `organizationId` chỉ lấy từ `CustomUserDetails` của JWT; không nhận từ client |

Mọi endpoint yêu cầu `Authorization: Bearer <token>`. Không có quyền trả `403`; chưa xác thực trả `401`.

## 3. Business Rules

- **QTN-01 – cách ly dữ liệu:** truy vấn Activity Log và Export Job luôn ràng buộc `organizationId` của người dùng hiện tại trong service/query. Không nhận `organizationId`, `userId` hay `objectId` làm bộ lọc client; không tiết lộ sự tồn tại của dữ liệu/job/file khác tổ chức.
- **QTN-08 – dòng sự kiện chỉ thêm:** không sửa/xóa Activity Log để phục vụ export. Sau khi snapshot được chấp nhận/tạo thành công, thêm một event `EXPORT_ACTIVITY_LOG`; event này không thuộc snapshot hiện tại.
- Không có dữ liệu khớp bộ lọc: trả `400`, không sinh file/job và không ghi audit export.
- Cùng một DTO/bộ đặc tả lọc được dùng cho preview, snapshot và export để tránh sai lệch số lượng.
- CSV là định dạng duy nhất của v1: `text/csv;charset=UTF-8`. Giá trị phải escape đúng CSV; mọi giá trị bắt đầu bằng `=`, `+`, `-`, `@` phải được bảo vệ khỏi CSV formula injection (ví dụ tiền tố dấu nháy đơn).
- Không xuất `userId`, `organizationId`, `ipAddress`, `description`, mật khẩu, token, secret hoặc credential. `description` chỉ là tóm tắt tự do, không đủ để suy ra before/after có cấu trúc và hiện chưa có sanitizer.

## 4. Luồng nghiệp vụ

1. `VT-02` chọn bộ lọc trên màn hình Lịch sử hoạt động.
2. Frontend gọi preview bằng cùng bộ lọc; backend áp dụng tenant scope và trả `count`, `mode`.
3. Người dùng xác nhận export; backend trong transaction xác định tập bản ghi, cố định snapshot ID theo `(createdAt ASC, id ASC)`.
4. Nếu thuộc ngưỡng trực tiếp, backend tạo CSV từ snapshot. Chỉ sau khi tạo file thành công mới ghi bền vững `EXPORT_ACTIVITY_LOG` với trạng thái `SUCCESS`, rồi trả binary `200`.
5. Nếu vượt ngưỡng, backend tạo job `IN_PROGRESS` và giao việc thành công cho `TaskExecutor`. Sau khi yêu cầu được chấp nhận, backend ghi bền vững `EXPORT_ACTIVITY_LOG` với trạng thái `IN_PROGRESS`, rồi trả `202`.
6. Job nền thành công tạo file, đổi `SUCCESS`, gửi notification `INFO` với `entityId=exportId`. Người dùng dùng endpoint download, không dùng URL trực tiếp trong notification. Nếu worker lỗi, job đổi `FAILED`; audit đã ghi ở lúc chấp nhận không bị sửa.

## 5. Bộ lọc

DTO dùng chung: `ActivityLogExportFilterRequest`.

| Field | Kiểu | Bắt buộc | Quy tắc |
|---|---|---:|---|
| `startDate` | string | Không | `yyyy-MM-dd`, bao gồm toàn bộ ngày bắt đầu |
| `endDate` | string | Không | `yyyy-MM-dd`, bao gồm toàn bộ ngày kết thúc; không nhỏ hơn `startDate` |
| `action` | string | Không | Mã action Activity Log |
| `actorName` | string | Không | Tìm gần đúng trên `username` hoặc `fullName` trong tenant |
| `objectType` | string | Không | Ánh xạ sang `entityType` |

Không có `organizationId`, `objectId`, `userId`. Danh sách action/object type dùng các giá trị đã biết từ màn hình/lịch sử hiện tại; v1 không có metadata endpoint. Nếu sau này cần selector giá trị phân biệt chính xác theo tenant, cần task riêng.

Ví dụ body:

```json
{
  "startDate": "2026-08-01",
  "endDate": "2026-08-31",
  "action": "UPDATE_PRODUCTION_LOT",
  "actorName": "nguyen van",
  "objectType": "PRODUCTION_LOT"
}
```

## 6. Schema dữ liệu file export

Header CSV cố định, theo đúng thứ tự:

```text
occurredAt,actorName,actorUsername,actorRole,actionType,objectType,objectIdentifier,beforeValue,afterValue
```

| Cột nghiệp vụ | Nguồn/mapping hiện tại | Có sẵn | Cách export / gap |
|---|---|---:|---|
| `occurredAt` | `ActivityLog.createdAt` | Có | ISO-8601 theo múi giờ nghiệp vụ `Asia/Ho_Chi_Minh`; không tự gắn nhãn UTC cho `LocalDateTime` hiện tại |
| `actorName` | `fullName`, fallback `username` | Có | Dùng `fullName` khi không rỗng, nếu không dùng `username`, đúng mapping API hiện tại |
| `actorUsername` | `username` | Có | Định danh phù hợp, được phép xuất |
| `actorRole` | Không có trên ActivityLog/Event | Không | `null` cho dữ liệu cũ; đề xuất `actor_role_code` khi ghi event mới |
| `actionType` | `action` | Có | Xuất trực tiếp |
| `objectType` | `entityType` | Có | Ánh xạ 1:1 |
| `objectIdentifier` | `entityId` | Có một phần | Xuất ID hiện có; chưa có code/tên nghiệp vụ ổn định |
| `beforeValue` | Không có | Không | Chuỗi JSON compact hoặc literal `null`; đề xuất `before_value` JSON |
| `afterValue` | Không có | Không | Chuỗi JSON compact hoặc literal `null`; đề xuất `after_value` JSON |
| Tenant scope | `organizationId` | Có | Chỉ dùng nội bộ để ràng buộc query, **không xuất** |

`beforeValue`/`afterValue` chỉ chứa field được phép theo allowlist từng action; áp dụng denylist đệ quy với `password`, `token`, `secret`, `credential` và dữ liệu nhạy cảm tương đương. Khi không có dữ liệu, ô CSV là literal `null`, không thay bằng `description`.

Object JSON được serialize compact thành một giá trị CSV và escape dấu nháy kép theo quy tắc CSV. `actorName`, `actorUsername`, `objectIdentifier` và mọi giá trị text cũng phải qua bước chống formula injection trước khi ghi file.

## 7. API Endpoints

Mọi response JSON, gồm lỗi, dùng wrapper `ApiResult` hiện hữu. Binary CSV trả raw response, không bọc `ApiResult`.

| Method | Path | Mục đích |
|---|---|---|
| `POST` | `/api/v1/organizations/activity-logs/exports/preview` | Đếm và gợi ý mode |
| `POST` | `/api/v1/organizations/activity-logs/exports` | Tạo export trực tiếp hoặc nền |
| `GET` | `/api/v1/organizations/activity-logs/exports/{exportId}` | Xem job async |
| `GET` | `/api/v1/organizations/activity-logs/exports/{exportId}/download` | Tải CSV job hoàn tất |

Đây là các endpoint **đề xuất**; API hiện có chỉ là `GET /api/v1/organizations/activity-logs`.

| Endpoint | Quyền | Request/validation | Success | Lỗi chính |
|---|---|---|---|---|
| `POST .../exports/preview` | `VT-02` | JSON `ActivityLogExportFilterRequest`; ngày đúng định dạng, `startDate <= endDate` | `200 ApiResult<Preview>` | `400`, `401`, `403` |
| `POST .../exports` | `VT-02` | Cùng DTO và validation với preview | `200` raw CSV hoặc `202 ApiResult<ExportJob>` | `400` không dữ liệu/validation, `401`, `403`, `500` tạo file trực tiếp |
| `GET .../exports/{exportId}` | `VT-02` cùng tenant | Không body; `exportId` là UUID | `200 ApiResult<ExportJob>` | `401`, `403`, `404` chung cho không tồn tại/khác tenant |
| `GET .../exports/{exportId}/download` | `VT-02` cùng tenant | Không body; job phải `SUCCESS`, file phải tồn tại | `200` raw CSV | `401`, `403`, `404`, `409` chưa hoàn tất |

## 8. Preview Count

### `POST /api/v1/organizations/activity-logs/exports/preview`

Body là `ActivityLogExportFilterRequest`. Backend dùng đúng truy vấn filter + tenant scope của export, không tạo snapshot, job, file hay audit.

```json
{
  "success": true,
  "status": 200,
  "data": { "count": 1248, "mode": "ASYNC" }
}
```

`mode` là `DIRECT` hoặc `ASYNC`, do server quyết định. Ngưỡng số bản ghi/chỉ số tài nguyên chưa được chốt.

Preview không tạo snapshot. Nếu có Activity Log mới giữa preview và lúc xác nhận, `recordCount` của export có thể tăng; đây là thay đổi dữ liệu hợp lệ, không phải sai khác logic. `recordCount` trả từ export/job là số lượng snapshot có thẩm quyền.

## 9. Export trực tiếp

### `POST /api/v1/organizations/activity-logs/exports`

Body giống preview. Server tự chọn mode sau khi tạo snapshot.

- **DIRECT:** `200 OK`, `Content-Type: text/csv;charset=UTF-8`, `Content-Disposition: attachment; filename="activity-logs-<timestamp>.csv"`; response là bytes CSV. Snapshot/job nội bộ hoàn tất đồng bộ.
- **ASYNC:** `202 Accepted`, trả `ApiResult`:

```json
{
  "success": true,
  "status": 202,
  "data": {
    "exportId": "b6b05fbc-6f2a-4d8f-a6e4-4ddc0fa0af31",
    "status": "IN_PROGRESS",
    "recordCount": 1248,
    "createdAt": "2026-09-14T09:00:00+07:00"
  }
}
```

Không tạo file rỗng. Direct không trả URL tải riêng.

## 10. Background Export

Với mode `ASYNC`, dùng pattern `TaskExecutor` hiện có và trạng thái backup hiện hữu: `IN_PROGRESS`, `SUCCESS`, `FAILED`; không tạo framework job mới. Worker đọc các ID snapshot đã cố định, sinh CSV và lưu tham chiếu file nội bộ. Lỗi nền đổi job sang `FAILED`; notification lỗi chỉ được bổ sung khi yêu cầu nghiệp vụ chốt.

## 11. Export Job lifecycle

```text
snapshot tạo xong → IN_PROGRESS → SUCCESS
                           └→ FAILED
```

- `IN_PROGRESS`: file chưa tải được.
- `SUCCESS`: file tồn tại và được phép tải bởi đúng tenant.
- `FAILED`: không có file hợp lệ; không tự retry.

### `GET /api/v1/organizations/activity-logs/exports/{exportId}`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "exportId": "b6b05fbc-6f2a-4d8f-a6e4-4ddc0fa0af31",
    "status": "SUCCESS",
    "recordCount": 1248,
    "createdAt": "2026-09-14T09:00:00+07:00"
  }
}
```

## 12. Notification / Download

Khi job `SUCCESS`, tạo Notification hiện hữu với `type=INFO`, `entityId=exportId`, tiêu đề/nội dung thông báo file đã sẵn sàng. Không đưa direct URL vào notification.

### `GET /api/v1/organizations/activity-logs/exports/{exportId}/download`

Chỉ cho đúng `VT-02` trong đúng tổ chức, job `SUCCESS`, file còn tồn tại. Trả raw CSV với `200 OK` và header như export trực tiếp. Job tenant khác hoặc không tồn tại đều trả `404` chung để tránh lộ dữ liệu; file bị thiếu trả `404`.

## 13. Activity Log của chính lần export

Event `EXPORT_ACTIVITY_LOG` được ghi **sau** khi transaction cố định snapshot `(createdAt ASC, id ASC)` và chỉ khi direct file đã tạo thành công hoặc async request đã được chấp nhận. Metadata tối thiểu đề xuất: người xuất, role tại thời điểm xuất, thời điểm, filter, `recordCount`, `exportJobId`/file tham chiếu và trạng thái (`SUCCESS` cho direct, `IN_PROGRESS` khi nhận async); không lưu nội dung file. Event đó bị loại khỏi file hiện tại, nhưng có thể được xuất ở lần sau nếu khớp filter.

Để đáp ứng TC-04, ghi audit của export phải hoàn tất bền vững trước khi trả `200`/`202`; không được chỉ dựa vào listener `@Async` hiện tại vốn bắt lỗi và tiếp tục. Nếu ghi audit thất bại, request direct/acceptance async không được báo thành công giả.

## 14. Tenant Isolation

`ActivityLogSpecification` và service hiện tại đã lấy `organizationId` từ current user cho danh sách Activity Log; hợp đồng mới phải tái sử dụng nguyên tắc này cho count, snapshot, status và download. Actor/object được nêu trong filter chỉ giới hạn kết quả đã tenant-scope, không cho phép dò dữ liệu tenant khác. Với `{exportId}` sai tenant, trả `404` chung, không trả count, metadata hay lý do sở hữu.

- `actorName` chỉ khớp người ở tenant khác: preview trả `count=0`; export trả lỗi không có dữ liệu chung `400`, không trả tên hay xác nhận người đó tồn tại.
- Contract không nhận `objectId`; `objectType` chỉ được áp dụng sau điều kiện tenant. Vì vậy client không thể tham chiếu trực tiếp object của tenant khác qua API này.
- Nếu dữ liệu Activity Log cũ có `entityId` không nhất quán, export không join sang object để lấy tên/metadata. Chỉ xuất identifier đã nằm trong bản ghi log thuộc tenant hiện tại, tránh mở thêm đường IDOR.

## 15. Mapping source code hiện tại

| Thành phần | Evidence hiện tại | Tác động hợp đồng |
|---|---|---|
| Activity Log | `organizationId,userId,username,fullName,action,description,entityType,entityId,ipAddress,createdAt` | Dùng trực tiếp các trường mapping ở mục 6; không lộ ID/IP/description |
| Filter/scoping | `ActivityLogSpecification` + service lấy organization từ current user | Tái sử dụng cho preview/export/snapshot |
| API hiện hữu | `GET /api/v1/organizations/activity-logs` | Là nguồn filter/role `VT-02`; chưa có export API |
| `CustomUserDetails` | Có `roleCode`, `roleName` | Không phải lịch sử role trong Event; không dùng để khôi phục role quá khứ |
| Before/after | Không tồn tại; chỉ có `description` text | Không đáp ứng đầy đủ TC-01 |
| Notification | `user,type,title,content,entityId` | Dùng `INFO`, `entityId=exportId` |
| Nền | `TaskExecutor`; backup dùng `IN_PROGRESS/SUCCESS/FAILED` | Reuse pattern, không tạo status mới |

Đề xuất DB **chưa implement migration**: `activity_log_export_job` và `activity_log_export_job_item` lưu metadata/job và ordered snapshot IDs; bổ sung nullable vào ActivityLog: `actor_role_code`, `before_value JSON`, `after_value JSON`, `metadata JSON`. Tương thích dữ liệu cũ bằng `null`; không backfill từ `description`.

## 16. Acceptance Criteria Mapping

| AC/TC | Contract/logic đáp ứng | Trạng thái tài liệu |
|---|---|---|
| TC-01 – export thành công | CSV schema mục 6; direct/async mục 9–10 | Chưa đạt đầy đủ với source cũ do thiếu role/before/after |
| TC-02 – không có dữ liệu | `400`, không file/job/audit | Proposed |
| TC-03 – tenant isolation | Current-user scope, 404 chung, mục 3/14 | Proposed |
| TC-04 – ghi lịch sử export | `EXPORT_ACTIVITY_LOG` sau snapshot, mục 13 | Proposed |
| TC-05 – preview count | Endpoint preview dùng cùng filter/query, mục 8 | Proposed |
| TC-06 – dữ liệu lớn | Server chọn `ASYNC`, TaskExecutor, notification/download | Proposed |

Ma trận yêu cầu:

| Yêu cầu | Nguồn | Hành vi | API/DB tác động | Bằng chứng/validation |
|---|---|---|---|---|
| Chốt trường file | NCL-854 | Chín cột cố định ở mục 6; thiếu dữ liệu phải ghi GAP | ActivityLog capture/model additive | Review schema/source; chưa implement |
| Xuất theo bộ lọc | NCL-857 | Preview và export dùng cùng DTO/specification | Hai POST endpoint, mở rộng specification | TC-01, TC-02, TC-05 |
| Bảo đảm phạm vi | NCL-860 | JWT/current tenant ở query, job và file | Tenant-scoped repository/service | TC-03 + negative cross-tenant |
| Kiểm thử export | NCL-862 | Bao phủ direct, async, security và lỗi | Test plan cuối mục 16 | Chưa chạy vì task docs-only |
| Xem lịch sử hiện có | NCL-101 / NCL-08-CN-004-TC-01 | Giữ `GET /activity-logs`; export dựa cùng dữ liệu | Không phá contract hiện có | Regression test GET |
| Trạng thái rỗng | NCL-08-CN-004-TC-02 | GET hiện tại vẫn trả items rỗng; preview trả count 0 | Không đổi GET | Existing controller/service + regression test |
| Chỉ lịch sử tổ chức mình | NCL-08-CN-004-TC-03 | Scope từ JWT/current user | Reuse `hasOrganizationId` | Cross-tenant test |
| Ghi thao tác quan trọng | NCL-08-CN-004-TC-04 | Export thành công/chấp nhận phải có audit | `EXPORT_ACTIVITY_LOG` bền vững | TC-04 |
| Export thành công | Baseline NCL-08-CN-015-TC-01 | CSV đủ field theo mục 6 | Direct/async + model GAP | File-content test; đang bị chặn bởi GAP role/before/after |
| Không có dữ liệu | Baseline NCL-08-CN-015-TC-02 | `400`, không file/job/audit | Shared query/count | Negative test |
| Tenant isolation | Baseline NCL-08-CN-015-TC-03, QTN-01 | Không rò count/job/file/metadata | Scope từ `CustomUserDetails` | Permission/cross-tenant tests |
| Ghi lịch sử export | Baseline NCL-08-CN-015-TC-04, QTN-08 | Audit sau snapshot, không sửa log cũ | Durable append-only audit | Snapshot/audit ordering test |
| Preview count | Baseline NCL-08-CN-015-TC-05 | Cùng filter/query với snapshot | Preview endpoint | Count parity test khi dữ liệu không đổi |
| Dữ liệu lớn | Baseline NCL-08-CN-015-TC-06 | `202`, job nền, notification, download | Job/items + TaskExecutor + Notification | Async integration test |

Kiểm thử bắt buộc khi triển khai: TC01–TC06; sai khoảng ngày; 401/403; actor/object/job khác tenant; `download` khi `IN_PROGRESS`; file thiếu; lỗi worker `FAILED`; CSV escape/formula injection; denylist before/after; event export không nằm trong snapshot; và hồi quy `GET /activity-logs`.

## 17. GAP / quyết định cần xác nhận

1. Jira NCL-706/NCL-854/NCL-857/NCL-860/NCL-862 hiện không có description/AC live; TC01–TC06 từ baseline attachment là evidence nghiệp vụ. Excel mới ngày 2026-08-14 có QTN-01, QTN-08, `NCL-08-CN-004` và TC/task liên quan, nhưng không có `NCL-08-CN-015`/NCL-706/NCL-854; workbook cũ có nội dung liên quan tương tự.
2. Source thiếu role lịch sử và before/after. Vì vậy không thể khẳng định TC-01 pass cho dữ liệu cũ cho đến khi có thay đổi additive về capture/model; các cột hiện là `null`.
3. Cần BA/PO chốt ngưỡng direct/async dạng số, thời gian giữ file, expiry/cleanup, retry và kích thước file tối đa. Chỉ dùng `410` nếu chính sách retention tương lai xác định file hết hạn; v1 dùng `404` khi file thiếu.
4. Bảng `job`/`job_item` và các cột ActivityLog là proposal DB, không phải migration đã tồn tại. Cần thiết kế khóa/index, quyền file storage và transaction cụ thể trước backend implementation.
5. Audit hiện tại qua `ActivityLogListener` là bất đồng bộ và nuốt lỗi sau khi log; chưa đủ bảo đảm TC-04 cho export. Task backend phải chọn cơ chế ghi bền vững/transaction hoặc outbox trước khi trả thành công, không làm yếu tính nhất quán chỉ để tái sử dụng annotation hiện tại.
6. Mã lỗi chuẩn dùng `ApiResult`: khoảng ngày không hợp lệ `400`; không dữ liệu `400`; không xác thực `401`; không đủ quyền `403`; job tenant khác/không tồn tại `404`; job chưa hoàn tất `409`; file thiếu `404`; tạo file/lỗi nền: job `FAILED`, lỗi đồng bộ `500` và notification theo quyết định nghiệp vụ. Không công bố chi tiết tenant khác.

Ví dụ lỗi dùng chung, trong đó `path` thay đổi theo endpoint thực tế:

```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy yêu cầu xuất nhật ký.",
  "path": "/api/v1/organizations/activity-logs/exports/b6b05fbc-6f2a-4d8f-a6e4-4ddc0fa0af31",
  "timestamp": "2026-09-14T02:05:00Z"
}
```

**Trạng thái validation:** chỉ kiểm tra tính nhất quán contract-first với nguồn được cung cấp; chưa có backend/frontend/runtime/test nào được thực thi và không tuyên bố AC đã pass.
