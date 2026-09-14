# Xuất nhật ký hoạt động phục vụ kiểm tra

> Jira Story: NCL-706 (`NCL-08-CN-015`)
>
> Nhánh triển khai: `feature/NCL-08-CN-015-export-activity-log`
>
> Epic: NCL-79 · Task tài liệu/thiết kế: NCL-854 · Liên quan: NCL-857, NCL-860, NCL-862
>
> Phụ thuộc: NCL-101 và `NCL-08-CN-004` (xem lịch sử hoạt động)
>
> Loại tài liệu: API Contract-first · Trạng thái: **Đã triển khai direct/async, đang hoàn thiện validation** · Phiên bản: v1
>
> Lưu ý: preview, export CSV trực tiếp, export nền/job/notification và mô hình dữ liệu audit bổ sung đã được triển khai trên nhánh nêu trên. Dữ liệu lịch sử trước migration không được suy diễn ngược `actorRole`, `beforeValue`, `afterValue`.

## 1. Mục tiêu

Cho phép Quản lý tổ chức xuất các bản ghi nhật ký hoạt động thuộc **chính tổ chức hiện tại** để phục vụ kiểm tra. Người dùng có thể lọc, xem trước số lượng, yêu cầu xuất CSV trực tiếp hoặc nền; lần xuất thành công phải tự ghi vết `EXPORT_ACTIVITY_LOG` mà không tự lọt vào file của chính lần xuất đó.

## 2. Actor và quyền

| Actor | Quyền | Phạm vi |
|---|---|---|
| Quản lý tổ chức (`VT-02` / `ORG_MANAGER`) | Preview, yêu cầu export, xem trạng thái và tải file export của mình/tổ chức mình | `organizationId` chỉ lấy từ `CustomUserDetails` của JWT; không nhận từ client |

Mọi endpoint yêu cầu `Authorization: Bearer <token>`. Không có quyền trả `403`; chưa xác thực trả `401`.

## 3. Business Rules

- **QTN-01 – cách ly dữ liệu:** truy vấn Activity Log và Export Job luôn ràng buộc `organizationId` của người dùng hiện tại trong service/query. Không nhận `organizationId`, `userId` hay `objectId` làm bộ lọc client; không tiết lộ sự tồn tại của dữ liệu/job/file khác tổ chức.
- **QTN-08 – dòng sự kiện chỉ thêm không sửa:** không sửa/xóa Activity Log để phục vụ export. Yêu cầu ghi audit khi xuất nhật ký bắt nguồn từ chính User Story `NCL-08-CN-015` (`TC-04`), còn nguyên tắc `QTN-08` quy định việc lưu vết này phải theo cơ chế append-only: sau khi snapshot được xác định và tệp tạo thành công, thêm một bản ghi mới `EXPORT_ACTIVITY_LOG` vào chuỗi nhật ký mà không sửa đổi lịch sử cũ và không tự lọt vào snapshot của chính lần xuất đó.
- **Ngưỡng chuyển chế độ:** Ngưỡng direct mặc định là 10.000 bản ghi và có thể cấu hình bằng `ACTIVITY_LOG_EXPORT_DIRECT_LIMIT`. Đây là technical configurable threshold (ngưỡng kỹ thuật an toàn có thể cấu hình, không phải Business Rule cố định của BA/PO). Kết quả không vượt ngưỡng dùng `DIRECT`; kết quả vượt ngưỡng phải tạo snapshot và `ASYNC` export job, không được từ chối chỉ vì dữ liệu lớn.
- **Giới hạn khoảng thời gian xuất:** Khoảng thời gian giữa `startDate` và `endDate` không được vượt quá 365 ngày (cấu hình kỹ thuật qua `app.activity-log-export.max-range-days`, mặc định 365 ngày cho v1). Nếu vượt quá giới hạn cấu hình, request preview và export đều bị từ chối với HTTP 400 Bad Request, không tạo snapshot/job/file và không ghi audit.
- Không có dữ liệu khớp bộ lọc: trả `400`, không sinh file/job và không ghi audit export.
- Cùng một DTO/bộ đặc tả lọc được dùng cho preview, snapshot và export để tránh sai lệch số lượng.
- CSV là định dạng duy nhất của v1: `text/csv;charset=UTF-8`, có BOM UTF-8 để tương thích Excel. Giá trị phải escape đúng CSV; mọi giá trị mà sau khi loại bỏ khoảng trắng đầu dòng (`stripLeading()`) bắt đầu bằng `=`, `+`, `-`, `@` phải được bảo vệ khỏi CSV formula injection bằng cách thêm tiền tố dấu nháy đơn (`'`) vào đầu toàn bộ giá trị xuất ra.
- Không xuất `userId`, `organizationId`, `ipAddress`, `description`, mật khẩu, token, secret hoặc credential. `description` chỉ là tóm tắt tự do, không được dùng để suy ra before/after. `beforeValue`/`afterValue` đi qua sanitizer dùng chung trước khi ghi CSV.

## 4. Luồng nghiệp vụ

1. `VT-02` chọn bộ lọc trên màn hình Lịch sử hoạt động.
2. Frontend gọi preview bằng cùng bộ lọc; backend áp dụng tenant scope và trả `count`, `mode`.
3. Người dùng xác nhận export; backend trong transaction xác định tập bản ghi, cố định snapshot ID theo `(createdAt ASC, id ASC)`.
4. Nếu thuộc ngưỡng trực tiếp, backend tạo CSV từ snapshot. Chỉ sau khi tạo file thành công mới ghi bền vững `EXPORT_ACTIVITY_LOG` với trạng thái `SUCCESS`, rồi trả binary `200`.
5. Nếu vượt ngưỡng, backend đóng snapshot bằng một câu `INSERT … SELECT` tại database, tạo job `IN_PROGRESS` và giao việc cho `TaskExecutor`. Request không tải/copy toàn bộ tập lớn qua JVM. Sau khi yêu cầu được chấp nhận, backend ghi bền vững `EXPORT_ACTIVITY_LOG` với trạng thái `IN_PROGRESS`, rồi trả `202`.
6. Job nền thành công tạo file, đổi `SUCCESS`, gửi notification `ACTIVITY_LOG_EXPORT_READY` với `entityId=exportId`. Người dùng dùng endpoint download, không dùng URL trực tiếp trong notification. Nếu worker lỗi, job đổi `FAILED`; audit đã ghi ở lúc chấp nhận không bị sửa.

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

Header CSV gồm 9 cột kỹ thuật cố định, theo đúng thứ tự:

```text
occurredAt,actorName,actorUsername,actorRole,actionType,objectType,objectIdentifier,beforeValue,afterValue
```

| Cột nghiệp vụ | Nguồn/mapping hiện tại | Có sẵn | Cách export / gap |
|---|---|---:|---|
| `occurredAt` | `ActivityLog.createdAt` | Có | ISO-8601 theo múi giờ nghiệp vụ `Asia/Ho_Chi_Minh`; không tự gắn nhãn UTC cho `LocalDateTime` hiện tại |
| `actorName` | `fullName`, fallback `username` | Có | Dùng `fullName` khi không rỗng, nếu không dùng `username`, đúng mapping API hiện tại |
| `actorUsername` | `username` | Có | Định danh phù hợp, được phép xuất |
| `actorRole` | `ActivityLog.actorRole` / `ActivityLogEvent.actorRole` | Có từ migration mới | Role tại thời điểm ghi event; literal `null` cho dữ liệu lịch sử cũ |
| `actionType` | `action` | Có | Xuất trực tiếp |
| `objectType` | `entityType` | Có | Ánh xạ 1:1 |
| `objectIdentifier` | `entityId` | Có một phần | Xuất ID hiện có; chưa có code/tên nghiệp vụ ổn định |
| `beforeValue` | `ActivityLog.beforeValue` | Có từ migration mới | JSON compact đã che khóa nhạy cảm hoặc literal `null` |
| `afterValue` | `ActivityLog.afterValue` | Có từ migration mới | JSON compact đã che khóa nhạy cảm hoặc literal `null` |
| Tenant scope | `organizationId` | Có | Chỉ dùng nội bộ để ràng buộc query, **không xuất** |

`beforeValue`/`afterValue` được parse JSON khi có thể và áp dụng denylist đệ quy với `password`, `token`, `secret`, `credential`, API/private/access key, authorization, cookie và biến thể tương đương. Chuỗi không phải JSON được che theo mẫu key-value phổ biến. Khi không có dữ liệu, ô CSV là literal `null`, không thay bằng `description`.

Object JSON được serialize compact thành một giá trị CSV và escape dấu nháy kép theo quy tắc CSV. `actorName`, `actorUsername`, `objectIdentifier` và mọi giá trị text cũng phải qua bước chống formula injection trước khi ghi file.

## 7. API Endpoints

Mọi response JSON, gồm lỗi, dùng wrapper `ApiResult` hiện hữu. Binary CSV trả raw response, không bọc `ApiResult`.

| Method | Path | Mục đích | Trạng thái source |
|---|---|---|---|
| `POST` | `/api/v1/organizations/activity-logs/exports/preview` | Đếm và gợi ý mode | Đã triển khai NCL-857 |
| `POST` | `/api/v1/organizations/activity-logs/exports` | Tạo export trực tiếp hoặc nền | Đã triển khai `DIRECT` và `ASYNC` |
| `GET` | `/api/v1/organizations/activity-logs/exports/{exportId}` | Xem job async | Đã triển khai, tenant-scoped |
| `GET` | `/api/v1/organizations/activity-logs/exports/{exportId}/download` | Tải CSV job hoàn tất | Đã triển khai, tenant-scoped |

Bốn endpoint đã được triển khai theo cùng contract và đều giới hạn quyền `VT-02` trong tổ chức hiện tại.

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
  "data": { "count": 248, "mode": "DIRECT" }
}
```

- `mode` là `DIRECT` khi `count` không vượt ngưỡng cấu hình và là `ASYNC` khi vượt ngưỡng.
- `count` phản ánh tổng số bản ghi thực tế theo bộ lọc tại thời điểm truy vấn và **có thể vượt quá 10.000 bản ghi** (ví dụ: `15.000`).
- Nếu `count > 10.000`:
  - Preview vẫn trả về số lượng thực tế để người dùng biết quy mô dữ liệu.
  - Frontend hiển thị chế độ xử lý nền và cho phép người dùng tạo yêu cầu export job.
  - Người dùng không phải giữ request HTTP trong lúc hệ thống sinh tệp.
- Preview không tạo snapshot. Nếu có Activity Log mới giữa preview và lúc xác nhận, `recordCount` của export có thể thay đổi; đây là thay đổi dữ liệu tự nhiên, không phải sai khác logic.

## 9. Export trực tiếp

### `POST /api/v1/organizations/activity-logs/exports`

Body giống preview. Backend tự chọn `DIRECT` hoặc `ASYNC` từ số lượng snapshot và ngưỡng cấu hình; client không được tự ép mode.

#### Giới hạn và an toàn bộ nhớ (OOM Protection):
- Backend không bao giờ thực hiện `findAll(specification, sort)` không giới hạn vào RAM.
- Query direct được giới hạn chặt chẽ ở database layer thông qua `PageRequest.of(0, directLimit + 1, sort)`; nhánh async dùng một câu `INSERT … SELECT` có `ROW_NUMBER()` để đóng ordered snapshot tại database, không lặp page qua JVM trong request.
- Thứ tự sắp xếp giữ nguyên: `createdAt ASC`, `id ASC`.

#### Xử lý kết quả theo ngưỡng:
- **Trường hợp kết quả <= 10.000 bản ghi:**
  - `200 OK`, `Content-Type: text/csv;charset=UTF-8`, `Content-Disposition: attachment; filename="activity-logs-<timestamp>.csv"`; response là nội dung CSV nhị phân có UTF-8 BOM.
  - Sau khi sinh file thành công, ghi audit log `EXPORT_ACTIVITY_LOG` với trạng thái `SUCCESS`.
- **Trường hợp kết quả vượt ngưỡng direct:** tạo snapshot bất biến, trả `202 Accepted` với `ApiResult<ActivityLogExportJobResponse>`, ghi audit `EXPORT_ACTIVITY_LOG` trạng thái `IN_PROGRESS`, rồi sinh file bằng `TaskExecutor`.

- Không tạo file rỗng. Direct không trả URL tải riêng.

## 10. Background Export

Mode `ASYNC` dùng `TaskExecutor` hiện có và các trạng thái `IN_PROGRESS`, `SUCCESS`, `FAILED`. Snapshot lưu từng dòng tại `activity_log_export_items` bằng một thao tác database trước khi response được commit; worker đọc snapshot theo trang, sinh CSV và lưu đường dẫn nội bộ. Khi thành công, hệ thống gửi notification cho đúng người tạo yêu cầu với `entityId = exportJobId`.

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
    "mode": "ASYNC",
    "status": "SUCCESS",
    "recordCount": 1248,
    "fileName": "activity-logs-b6b05fbc-6f2a-4d8f-a6e4-4ddc0fa0af31-20260914_090000.csv",
    "fileSize": 48126,
    "createdAt": "2026-09-14T09:00:00",
    "completedAt": "2026-09-14T09:00:03",
    "downloadUrl": "/api/v1/organizations/activity-logs/exports/b6b05fbc-6f2a-4d8f-a6e4-4ddc0fa0af31/download"
  }
}
```

## 12. Notification / Download

Khi job `SUCCESS`, tạo Notification với `type=ACTIVITY_LOG_EXPORT_READY`, `entityId=exportId`, tiêu đề/nội dung thông báo file đã sẵn sàng. Không đưa direct URL vào notification; frontend điều hướng qua trang Activity Log rồi gọi endpoint download có xác thực.

### `GET /api/v1/organizations/activity-logs/exports/{exportId}/download`

Chỉ cho đúng `VT-02` trong đúng tổ chức, job `SUCCESS`, file còn tồn tại. Trả raw CSV với `200 OK` và header như export trực tiếp. Job tenant khác hoặc không tồn tại đều trả `404` chung để tránh lộ dữ liệu; file bị thiếu trả `404`.

## 13. Activity Log của chính lần export

Event `EXPORT_ACTIVITY_LOG` được ghi **sau** khi transaction cố định snapshot `(createdAt ASC, id ASC)` và chỉ khi direct file đã tạo thành công hoặc async request đã được chấp nhận. Event lưu người xuất, role tại thời điểm xuất, filter, `recordCount`, `exportJobId` khi có và trạng thái (`SUCCESS` cho direct, `IN_PROGRESS` khi nhận async); không lưu nội dung file. Event đó bị loại khỏi file hiện tại, nhưng có thể được xuất ở lần sau nếu khớp filter.

Để đáp ứng TC-04, ghi audit của export phải hoàn tất bền vững trước khi trả `200`/`202`; không được chỉ dựa vào listener `@Async` hiện tại vốn bắt lỗi và tiếp tục. Nếu ghi audit thất bại, request direct/acceptance async không được báo thành công giả.

## 14. Tenant Isolation

`ActivityLogSpecification` và service hiện tại đã lấy `organizationId` từ current user cho danh sách Activity Log; hợp đồng mới phải tái sử dụng nguyên tắc này cho count, snapshot, status và download. Actor/object được nêu trong filter chỉ giới hạn kết quả đã tenant-scope, không cho phép dò dữ liệu tenant khác. Với `{exportId}` sai tenant, trả `404` chung, không trả count, metadata hay lý do sở hữu.

- `actorName` chỉ khớp người ở tenant khác: preview trả `count=0`; export trả lỗi không có dữ liệu chung `400`, không trả tên hay xác nhận người đó tồn tại.
- Contract không nhận `objectId`; `objectType` chỉ được áp dụng sau điều kiện tenant. Vì vậy client không thể tham chiếu trực tiếp object của tenant khác qua API này.
- Nếu dữ liệu Activity Log cũ có `entityId` không nhất quán, export không join sang object để lấy tên/metadata. Chỉ xuất identifier đã nằm trong bản ghi log thuộc tenant hiện tại, tránh mở thêm đường IDOR.

## 15. Mapping source code hiện tại

| Thành phần | Evidence hiện tại | Tác động hợp đồng |
|---|---|---|
| Activity Log | Các trường cũ cùng `actorRole,beforeValue,afterValue` nullable | Dùng trực tiếp các trường mapping ở mục 6; không lộ ID/IP/description; dữ liệu cũ giữ `null` |
| Filter/scoping | `ActivityLogSpecification` + service lấy organization từ current user | Tái sử dụng cho preview/export/snapshot |
| API | GET danh sách, preview, request export, job status và download | Direct/async dùng chung contract và tenant scope |
| `CustomUserDetails` | Có `roleCode`, `roleName` | Capture `roleCode` lúc tạo event mới; không khôi phục role cho dữ liệu cũ |
| Before/after | Nullable trên event/log, sanitizer trước CSV | Annotation/request có thể cung cấp snapshot có cấu trúc; dữ liệu cũ giữ `null` |
| Notification | `user,type,title,content,entityId` | Dùng `ACTIVITY_LOG_EXPORT_READY`, `entityId=exportId` |
| Nền | `TaskExecutor`; backup dùng `IN_PROGRESS/SUCCESS/FAILED` | Reuse pattern, không tạo status mới |

Migration `V20260914150000__add_activity_log_export_jobs.sql` đã bổ sung `actor_role`, `before_value`, `after_value`, bảng `activity_log_export_jobs` và `activity_log_export_items`. Item lưu bản sao bất biến của đủ chín trường CSV theo `sequence_no`, không chỉ giữ ID trỏ về Activity Log; vì vậy thay đổi dữ liệu sau khi nhận job không làm đổi file. Dữ liệu cũ giữ `null`, không backfill từ `description`.

## 16. Acceptance Criteria Mapping

| AC/TC | Contract/logic đáp ứng | Trạng thái tài liệu |
|---|---|---|
| TC-01 – export thành công | CSV schema mục 6; direct/async mục 9–10 | Đã triển khai đủ chín cột cho dữ liệu mới; dữ liệu cũ tương thích bằng `null` |
| TC-02 – không có dữ liệu | `400`, không file/job/audit | Đã triển khai cho cả logic chọn mode |
| TC-03 – tenant isolation | Current-user scope, 404 chung, mục 3/14 | Đã áp dụng cho preview/request/status/download |
| TC-04 – ghi lịch sử export | `EXPORT_ACTIVITY_LOG` sau snapshot, mục 13 | Đã triển khai direct và lúc chấp nhận async |
| TC-05 – preview count | Endpoint preview dùng cùng filter/query, mục 8 | Đã triển khai `DIRECT`/`ASYNC` theo ngưỡng |
| TC-06 – dữ liệu lớn | Vượt ngưỡng tạo snapshot + job nền, notification và link tải | Đã triển khai; cần runtime/UI validation |

Ma trận yêu cầu:

| Yêu cầu | Nguồn | Hành vi | API/DB tác động | Bằng chứng/validation |
|---|---|---|---|---|
| Chốt trường file | NCL-854 | Chín cột cố định ở mục 6 | ActivityLog capture/model additive | Migration + file-content test |
| Xuất theo bộ lọc | NCL-857 | Preview và export dùng cùng DTO/specification | Hai POST endpoint, mở rộng specification | Đã triển khai direct; test service/controller đạt |
| Bảo đảm phạm vi | NCL-860 | JWT/current tenant ở query, job và file | Tenant-scoped repository/service | TC-03 + negative cross-tenant |
| Kiểm thử export | NCL-862 | Bao phủ direct, boundary, security và lỗi | Test plan cuối mục 16 | Đã bổ sung unit/boundary tests |
| Xem lịch sử hiện có | NCL-101 / NCL-08-CN-004-TC-01 | Giữ `GET /activity-logs`; export dựa cùng dữ liệu | Không phá contract hiện có | Regression test GET |
| Trạng thái rỗng | NCL-08-CN-004-TC-02 | GET hiện tại vẫn trả items rỗng; preview trả count 0 | Không đổi GET | Existing controller/service + regression test |
| Chỉ lịch sử tổ chức mình | NCL-08-CN-004-TC-03 | Scope từ JWT/current user | Reuse `hasOrganizationId` | Cross-tenant test |
| Ghi thao tác quan trọng | NCL-08-CN-004-TC-04 | Export thành công/chấp nhận phải có audit | `EXPORT_ACTIVITY_LOG` bền vững | TC-04 |
| Export thành công | Baseline NCL-08-CN-015-TC-01 | CSV đủ field theo mục 6 | Direct/async + model additive | File-content test direct và worker |
| Không có dữ liệu | Baseline NCL-08-CN-015-TC-02 | `400`, không file/job/audit | Shared query/count | Negative test |
| Tenant isolation | Baseline NCL-08-CN-015-TC-03, QTN-01 | Không rò count/job/file/metadata | Scope từ `CustomUserDetails` | Permission/cross-tenant tests |
| Ghi lịch sử export | Baseline NCL-08-CN-015-TC-04, QTN-08 | Audit sau snapshot, không sửa log cũ | Durable append-only audit | Snapshot/audit ordering test |
| Preview count | Baseline NCL-08-CN-015-TC-05 | Cùng filter/query với snapshot | Preview endpoint | Count parity test khi dữ liệu không đổi |
| Dữ liệu lớn | Baseline NCL-08-CN-015-TC-06 | Vượt ngưỡng tạo snapshot và job nền; hoàn tất gửi notification | Job/item, `TaskExecutor`, HTTP 202 | Boundary direct/async + status/download test |

Kiểm thử bắt buộc khi triển khai: TC01–TC06; sai khoảng ngày; 401/403; actor/object/job khác tenant; `download` khi `IN_PROGRESS`; file thiếu; lỗi worker `FAILED`; CSV escape/formula injection; denylist before/after; event export không nằm trong snapshot; và hồi quy `GET /activity-logs`.

## 17. GAP / quyết định cần xác nhận

1. Jira NCL-706/NCL-854/NCL-857/NCL-860/NCL-862 hiện không có description/AC live; TC01–TC06 từ baseline attachment là evidence nghiệp vụ. Excel mới ngày 2026-08-14 có QTN-01, QTN-08, `NCL-08-CN-004` và TC/task liên quan, nhưng không có `NCL-08-CN-015`/NCL-706/NCL-854; workbook cũ có nội dung liên quan tương tự.
2. Dữ liệu lịch sử trước migration không có role/before/after và tiếp tục xuất literal `null`; hệ thống không suy diễn từ role hiện tại hoặc `description`. Event mới hỗ trợ capture ba trường additive này.
3. Ngưỡng direct mặc định 10.000 là technical configurable threshold, không phải Business Rule cố định của BA/PO. Giới hạn khoảng thời gian xuất mặc định 365 ngày là configurable max range v1 (`app.activity-log-export.max-range-days=365`). Khi vượt ngưỡng direct, hệ thống chuyển sang job nền đúng baseline; khi vượt khoảng thời gian, hệ thống từ chối HTTP 400 và không ghi audit.
4. **Operational GAP – Lưu trữ file và Retention/Cleanup Policy:** Job và snapshot đã có migration, khóa ngoại/index và tenant-scoped service. File được lưu tạm thời tại thư mục cấu hình `ACTIVITY_LOG_EXPORT_STORAGE_DIR`. Môi trường production cần gắn volume lưu trữ bền vững (persistent storage). Việc xây dựng chính sách hết hạn tệp (retention policy), thời hạn tải và tiến trình định kỳ dọn dẹp file cũ (cleanup scheduler) là một Operational GAP cần task vận hành và xác nhận nghiệp vụ riêng từ BA/PO, không thuộc phạm vi User Story hiện tại.
5. Export không dùng listener audit bất đồng bộ: service ghi `EXPORT_ACTIVITY_LOG` bằng `saveAndFlush` trong transaction trước khi trả `200/202`; worker chỉ xử lý file sau commit.
6. Mã lỗi chuẩn dùng `ApiResult`: khoảng ngày không hợp lệ `400`; không dữ liệu `400`; không xác thực `401`; không đủ quyền `403`; job tenant khác/không tồn tại `404`; job chưa hoàn tất `409`; file thiếu `404`; tạo file/lỗi nền: job `FAILED`, lỗi đồng bộ `500` và notification theo quyết định nghiệp vụ. Không công bố chi tiết tenant khác.
7. **GAP – Ghi nhật ký truy cập trái phép / từ chối truy cập theo QTN-01:** Hệ thống hiện tại **chưa có cơ chế global Security/Audit** tự động ghi nhận các request bị từ chối do sai vai trò (403 Forbidden), thiếu tenant context (không có `organizationId`), hoặc dò quét dữ liệu chéo tenant. Cơ chế `ACCESS_DENIED` duy nhất hiện có trong hệ thống đang được hardcode cục bộ tại `GlobalExceptionHandler.publishAccessDeniedAudit()` chỉ dành riêng cho endpoint `/api/v1/admin/monitoring`. `AuditAspect` hiện hữu chỉ bắt `@AfterReturning` trên các method `@Auditable` thành công của người dùng hợp lệ. Do đó, việc ghi vết từ chối truy cập cho `NCL-08-CN-015` được xác định là một GAP thực sự so với QTN-01 của toàn hệ thống và cần task kiến trúc riêng để thiết lập cơ chế global security audit chung, không tự ý mở rộng cục bộ trong User Story này.

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

**Trạng thái validation:** direct/async, job status/download, notification, migration audit và frontend integration đã có trong source. Chỉ được tuyên bố Story hoàn tất sau khi runtime, UI và toàn bộ TC01–TC06 được kiểm tra thành công.
