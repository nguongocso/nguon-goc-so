# Xuất nhật ký hoạt động phục vụ kiểm tra

> Jira Story: NCL-706 (`NCL-08-CN-015`)
>
> Nhánh triển khai: `feature/NCL-08-CN-015-export-activity-log`
>
> Epic: NCL-79 · Task tài liệu/thiết kế: NCL-854 · Liên quan: NCL-857, NCL-860, NCL-862
>
> Phụ thuộc: NCL-101 và `NCL-08-CN-004` (xem lịch sử hoạt động)
>
> Loại tài liệu: API Contract-first · Trạng thái: **Triển khai một phần (NCL-857)** · Phiên bản: v1
>
> Lưu ý: preview và export CSV trực tiếp đã được triển khai trong NCL-857. Export nền/job/notification, bảng mới và các trường Activity Log bổ sung vẫn là **đề xuất**, chưa được source hiện tại triển khai.

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
- **Giới hạn xuất trực tiếp (DIRECT export limit):** Chế độ DIRECT export trong Story v1 giới hạn tối đa 10.000 bản ghi (`MAX_DIRECT_EXPORT_RECORDS = 10_000`). Nếu số lượng bản ghi vượt quá 10.000 (truy vấn snapshot lấy được từ 10.001 bản ghi trở lên), yêu cầu export bị từ chối với HTTP 400 (`BusinessException`), không tạo CSV, không ghi audit `EXPORT_ACTIVITY_LOG`, và thông báo người dùng thu hẹp khoảng thời gian hoặc điều kiện lọc. Đây là giới hạn kỹ thuật an toàn của DIRECT mode v1; ASYNC export không thuộc phạm vi Story hiện tại.
- Không có dữ liệu khớp bộ lọc: trả `400`, không sinh file/job và không ghi audit export.
- Cùng một DTO/bộ đặc tả lọc được dùng cho preview, snapshot và export để tránh sai lệch số lượng.
- CSV là định dạng duy nhất của v1: `text/csv;charset=UTF-8`, có BOM UTF-8 để tương thích Excel. Giá trị phải escape đúng CSV; mọi giá trị mà sau khi loại bỏ khoảng trắng đầu dòng (`stripLeading()`) bắt đầu bằng `=`, `+`, `-`, `@` phải được bảo vệ khỏi CSV formula injection bằng cách thêm tiền tố dấu nháy đơn (`'`) vào đầu toàn bộ giá trị xuất ra.
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

| Method | Path | Mục đích | Trạng thái source |
|---|---|---|---|
| `POST` | `/api/v1/organizations/activity-logs/exports/preview` | Đếm và gợi ý mode | Đã triển khai NCL-857 |
| `POST` | `/api/v1/organizations/activity-logs/exports` | Tạo export trực tiếp hoặc nền | Đã triển khai nhánh `DIRECT`; `ASYNC` chưa triển khai |
| `GET` | `/api/v1/organizations/activity-logs/exports/{exportId}` | Xem job async | Đề xuất, chưa triển khai |
| `GET` | `/api/v1/organizations/activity-logs/exports/{exportId}/download` | Tải CSV job hoàn tất | Đề xuất, chưa triển khai |

Hai endpoint POST đã được triển khai trong NCL-857. Hai endpoint GET phụ thuộc thiết kế job nền và vẫn là **đề xuất**.

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

- Trong phạm vi Story v1, `mode` luôn là `DIRECT`. Hợp đồng API và response schema được giữ nguyên (không tự ý thêm trường mới).
- `count` phản ánh tổng số bản ghi thực tế theo bộ lọc tại thời điểm truy vấn và **có thể vượt quá 10.000 bản ghi** (ví dụ: `15.000`).
- Nếu `count > 10.000`:
  - Preview vẫn trả về số lượng thực tế để người dùng biết quy mô dữ liệu.
  - Frontend dựa vào `count` và ngưỡng DIRECT (`10.000`) để hiển thị cảnh báo: *"Có {count} bản ghi. Xuất trực tiếp hỗ trợ tối đa 10.000 bản ghi. Vui lòng thu hẹp khoảng thời gian hoặc điều kiện lọc."*
  - Frontend vô hiệu hóa (disable) nút "Tải tệp CSV", ngăn người dùng gửi request chắc chắn sẽ bị từ chối.
- Preview không tạo snapshot. Nếu có Activity Log mới giữa preview và lúc xác nhận, `recordCount` của export có thể thay đổi; đây là thay đổi dữ liệu tự nhiên, không phải sai khác logic.

## 9. Export trực tiếp

### `POST /api/v1/organizations/activity-logs/exports`

Body giống preview. Trong Story v1, backend xử lý mode `DIRECT`; `ASYNC` export chưa thuộc phạm vi triển khai của Story này.

#### Giới hạn và an toàn bộ nhớ (OOM Protection):
- Backend không bao giờ thực hiện `findAll(specification, sort)` không giới hạn vào RAM.
- Query snapshot được giới hạn chặt chẽ ở database layer thông qua `PageRequest.of(0, 10001, sort)` (`MAX_DIRECT_EXPORT_RECORDS + 1`).
- Thứ tự sắp xếp giữ nguyên: `createdAt ASC`, `id ASC`.

#### Xử lý kết quả theo ngưỡng:
- **Trường hợp kết quả <= 10.000 bản ghi:**
  - `200 OK`, `Content-Type: text/csv;charset=UTF-8`, `Content-Disposition: attachment; filename="activity-logs-<timestamp>.csv"`; response là nội dung CSV nhị phân có UTF-8 BOM.
  - Sau khi sinh file thành công, ghi audit log `EXPORT_ACTIVITY_LOG` với trạng thái `SUCCESS`.
- **Trường hợp kết quả > 10.000 bản ghi (query DB trả đủ 10.001 bản ghi):**
  - Từ chối export, trả về HTTP `400 Bad Request` dạng JSON (`ApiResult`).
  - **Không** tạo file CSV.
  - **Không** ghi audit log `EXPORT_ACTIVITY_LOG`.
  - Message phản hồi nghiệp vụ rõ ràng:
  
```json
{
  "success": false,
  "status": 400,
  "message": "Số lượng bản ghi vượt quá giới hạn xuất trực tiếp (tối đa 10.000 bản ghi). Vui lòng thu hẹp khoảng thời gian hoặc điều kiện lọc.",
  "path": "/api/v1/organizations/activity-logs/exports",
  "timestamp": "2026-09-14T02:05:00Z"
}
```

- Không tạo file rỗng. Direct không trả URL tải riêng.

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
| API hiện hữu | `GET /api/v1/organizations/activity-logs`, `POST .../exports/preview`, `POST .../exports` | Hai POST đã hỗ trợ preview và direct CSV trong NCL-857; async chưa có |
| `CustomUserDetails` | Có `roleCode`, `roleName` | Không phải lịch sử role trong Event; không dùng để khôi phục role quá khứ |
| Before/after | Không tồn tại; chỉ có `description` text | Không đáp ứng đầy đủ TC-01 |
| Notification | `user,type,title,content,entityId` | Dùng `INFO`, `entityId=exportId` |
| Nền | `TaskExecutor`; backup dùng `IN_PROGRESS/SUCCESS/FAILED` | Reuse pattern, không tạo status mới |

Đề xuất DB **chưa implement migration**: `activity_log_export_job` và `activity_log_export_job_item` lưu metadata/job và ordered snapshot IDs; bổ sung nullable vào ActivityLog: `actor_role_code`, `before_value JSON`, `after_value JSON`, `metadata JSON`. Tương thích dữ liệu cũ bằng `null`; không backfill từ `description`.

## 16. Acceptance Criteria Mapping

| AC/TC | Contract/logic đáp ứng | Trạng thái tài liệu |
|---|---|---|
| TC-01 – export thành công | CSV schema mục 6; direct/async mục 9–10 | Direct đã triển khai; chưa đạt đầy đủ do thiếu role/before/after |
| TC-02 – không có dữ liệu | `400`, không file/job/audit | Đã triển khai cho direct |
| TC-03 – tenant isolation | Current-user scope, 404 chung, mục 3/14 | Đã áp dụng cho hai POST; job/download chưa triển khai |
| TC-04 – ghi lịch sử export | `EXPORT_ACTIVITY_LOG` sau snapshot, mục 13 | Đã triển khai cho direct |
| TC-05 – preview count | Endpoint preview dùng cùng filter/query, mục 8 | Đã triển khai, mode hiện là `DIRECT` |
| TC-06 – dữ liệu lớn | Chặn OOM bằng query limit 10.001 ở DB; từ chối khi > 10.000; async hoãn | Đã áp dụng safety guard cho direct v1; async chưa triển khai |

Ma trận yêu cầu:

| Yêu cầu | Nguồn | Hành vi | API/DB tác động | Bằng chứng/validation |
|---|---|---|---|---|
| Chốt trường file | NCL-854 | Chín cột cố định ở mục 6; thiếu dữ liệu phải ghi GAP | ActivityLog capture/model additive | Review schema/source; chưa implement |
| Xuất theo bộ lọc | NCL-857 | Preview và export dùng cùng DTO/specification | Hai POST endpoint, mở rộng specification | Đã triển khai direct; test service/controller đạt |
| Bảo đảm phạm vi | NCL-860 | JWT/current tenant ở query, job và file | Tenant-scoped repository/service | TC-03 + negative cross-tenant |
| Kiểm thử export | NCL-862 | Bao phủ direct, boundary, security và lỗi | Test plan cuối mục 16 | Đã bổ sung unit/boundary tests |
| Xem lịch sử hiện có | NCL-101 / NCL-08-CN-004-TC-01 | Giữ `GET /activity-logs`; export dựa cùng dữ liệu | Không phá contract hiện có | Regression test GET |
| Trạng thái rỗng | NCL-08-CN-004-TC-02 | GET hiện tại vẫn trả items rỗng; preview trả count 0 | Không đổi GET | Existing controller/service + regression test |
| Chỉ lịch sử tổ chức mình | NCL-08-CN-004-TC-03 | Scope từ JWT/current user | Reuse `hasOrganizationId` | Cross-tenant test |
| Ghi thao tác quan trọng | NCL-08-CN-004-TC-04 | Export thành công/chấp nhận phải có audit | `EXPORT_ACTIVITY_LOG` bền vững | TC-04 |
| Export thành công | Baseline NCL-08-CN-015-TC-01 | CSV đủ field theo mục 6 | Direct/async + model GAP | File-content test; đang bị chặn bởi GAP role/before/after |
| Không có dữ liệu | Baseline NCL-08-CN-015-TC-02 | `400`, không file/job/audit | Shared query/count | Negative test |
| Tenant isolation | Baseline NCL-08-CN-015-TC-03, QTN-01 | Không rò count/job/file/metadata | Scope từ `CustomUserDetails` | Permission/cross-tenant tests |
| Ghi lịch sử export | Baseline NCL-08-CN-015-TC-04, QTN-08 | Audit sau snapshot, không sửa log cũ | Durable append-only audit | Snapshot/audit ordering test |
| Preview count | Baseline NCL-08-CN-015-TC-05 | Cùng filter/query với snapshot | Preview endpoint | Count parity test khi dữ liệu không đổi |
| Dữ liệu lớn | Baseline NCL-08-CN-015-TC-06 | Query DB giới hạn 10.001; từ chối khi > 10.000; FE cảnh báo và disable tải | Bounded query Pageable, Http 400 | Boundary test 9.999 / 10.000 / 10.001 |

Kiểm thử bắt buộc khi triển khai: TC01–TC06; sai khoảng ngày; 401/403; actor/object/job khác tenant; `download` khi `IN_PROGRESS`; file thiếu; lỗi worker `FAILED`; CSV escape/formula injection; denylist before/after; event export không nằm trong snapshot; và hồi quy `GET /activity-logs`.

## 17. GAP / quyết định cần xác nhận

1. Jira NCL-706/NCL-854/NCL-857/NCL-860/NCL-862 hiện không có description/AC live; TC01–TC06 từ baseline attachment là evidence nghiệp vụ. Excel mới ngày 2026-08-14 có QTN-01, QTN-08, `NCL-08-CN-004` và TC/task liên quan, nhưng không có `NCL-08-CN-015`/NCL-706/NCL-854; workbook cũ có nội dung liên quan tương tự.
2. Source thiếu role lịch sử và before/after. Vì vậy không thể khẳng định TC-01 pass cho dữ liệu cũ cho đến khi có thay đổi additive về capture/model; các cột hiện là `null`.
3. **Scope Adaptation so với mô tả Excel gốc của NCL-08-CN-015:** Trong file Excel nghiệp vụ gốc, chức năng xuất nhật ký nêu yêu cầu xử lý nền (ASYNC export) đối với khoảng thời gian lớn, gửi thông báo hệ thống kèm liên kết tải tệp và chặn nếu khoảng thời gian vượt giới hạn cấu hình. Trong hợp đồng v1 này, phạm vi đã được chủ động thu hẹp (scope adaptation đã chốt) sang DIRECT Export với giới hạn kỹ thuật tối đa 10.000 bản ghi (sử dụng `PageRequest.of(0, 10001, sort)` ở database layer để chặn nguy cơ tràn bộ nhớ OOM). Toàn bộ cơ chế ASYNC export (job nền, `TaskExecutor`, bảng quản lý job/item, lưu trữ file tạm, notification tải file) được chủ động hoãn cho giai đoạn sau. Đây là quyết định phân kỳ phạm vi có chủ đích và có cơ sở an toàn kỹ thuật, không phải phần bị bỏ sót trong quá trình triển khai.
4. Bảng `job`/`job_item` và các cột ActivityLog là proposal DB, không phải migration đã tồn tại. Cần thiết kế khóa/index, quyền file storage và transaction cụ thể trước backend implementation.
5. Audit hiện tại qua `ActivityLogListener` là bất đồng bộ và nuốt lỗi sau khi log; chưa đủ bảo đảm TC-04 cho export. Task backend phải chọn cơ chế ghi bền vững/transaction hoặc outbox trước khi trả thành công, không làm yếu tính nhất quán chỉ để tái sử dụng annotation hiện tại.
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

**Trạng thái validation NCL-857:** backend preview/direct CSV và test service/controller đã được triển khai. Export nền/job/notification, migration bổ sung Activity Log, runtime với database thật và toàn bộ TC01–TC06 chưa hoàn tất; không tuyên bố toàn bộ Story đã pass.
