**API: Đính chính nhật ký canh tác (NCL-03-CN-006)**

# 1. Thông tin chung

Cho phép Người ghi sự kiện (VT-03) hoặc Quản lý hợp tác xã (VT-02) **đính chính** một nhật ký canh tác đã ghi sai mà không làm mất dấu vết của bản gốc:

- Bản gốc **giữ nguyên** và được đánh dấu là "đã bị đính chính" (`isCorrected = true`).
- Hệ thống tạo một **bản ghi đính chính mới**, liên kết về bản gốc qua `originalFarmLogId`.
- Lý do đính chính là **bắt buộc** và được lưu vào `correctionReason`.
- Bản ghi đính chính vẫn hiển thị trong lịch sử Nhật ký canh tác (có nhãn "Đính chính") và xuất hiện trong hồ sơ truy xuất xuất khẩu (traceability dossier).
- Giá trị hiệu lực dùng cho nghiệp vụ (ví dụ kiểm tra thời gian cách ly thuốc, độ chín thu hoạch — tích hợp tương lai với NCL-03-CN-005) là giá trị của **bản ghi đính chính mới nhất**.

Nghiệp vụ tham chiếu theo mẫu đã có của hệ thống: đính chính sự kiện sơ chế/đóng gói (ChainEvent với `is_correction = true`, `parent_event_id`).

# 2. Endpoint

| **Thuộc tính** | **Giá trị** |
| --- | --- |
| Method | POST |
| URL | /api/v1/farm-logs/{id}/correct |
| Authentication | Bearer Token (JWT) |
| Authorization | Role `VT-03` (chỉ khi là người ghi gốc của bản ghi đang đính chính) hoặc `VT-02` |
| Permission | FARM_LOG:UPDATE (via PermissionChecker) |
| Điều kiện tổ chức | Người dùng phải cùng Organization với ProductionLot của nhật ký |

**Điều kiện**

Người dùng phải:

- Đăng nhập thành công (JWT).
- Có role VT-03 hoặc VT-02 (`@PreAuthorize("hasAnyRole('VT-02', 'VT-03')")`).
- Với VT-03: chỉ được đính chính nhật ký do **chính mình** ghi (`createdBy == currentUser`).
- Với VT-02: được đính chính mọi nhật ký của lô sản xuất thuộc tổ chức mình.
- Thuộc cùng Organization với ProductionLot.

# 3. Request Body

DTO: CorrectFarmLogRequest

| **Field** | **Type** | **Required** | **Validation / Description** |
| --- | --- | --- | --- |
| correctionData | Object | ✓ | Các trường cần đính chính. Ít nhất một trường phải khác giá trị bản gốc |
| correctionData.activityType | Enum (FarmActivityType) |  | Nếu gửi, phải là giá trị enum hợp lệ |
| correctionData.material | String |  | @Size(max=255) – "Tên vật tư không được vượt quá 255 ký tự" |
| correctionData.quantity | Double |  | @Positive – "Số lượng phải lớn hơn 0" |
| correctionData.unit | String |  | @Size(max=50) – "Đơn vị không được vượt quá 50 ký tự" |
| correctionData.executedDate | Date (LocalDate) |  | Nếu gửi, phải là ngày hợp lệ; không được là ngày tương lai |
| correctionData.notes | String |  | @Size(max=1000) – "Ghi chú không được vượt quá 1000 ký tự" |
| reason | String | ✓ | @NotBlank + @Size(max=500) – "Lý do đính chính không được để trống" / "Lý do đính chính không được vượt quá 500 ký tự" |

**Lưu ý**

- Không chấp nhận `productionLotId` và `createdBy`: bản ghi đính chính luôn thuộc cùng lô sản xuất với bản gốc và do người đính chính tạo (createdBy/correctedBy = người gọi API). Không có trường này trong DTO.
- Chỉ các trường được gửi trong `correctionData` mới thay đổi giá trị; trường không gửi sẽ giữ nguyên giá trị hiệu lực hiện tại (của bản ghi gốc hoặc bản đính chính gần nhất).

## Ví dụ Request

```json
POST /api/v1/farm-logs/a9fcbbac-fe01-4ecf-a97e-2d2c7b4ba5f1/correct
{
    "correctionData": {
        "quantity": 30.0,
        "material": "NPK 16-16-8",
        "executedDate": "2026-07-22"
    },
    "reason": "Ghi sai số lượng và ngày thực hiện trong lần ghi ban đầu"
}
```

# 4. Enum FarmActivityType

| **Value** | **Hiển thị** |
| --- | --- |
| PLANTING | Gieo trồng |
| WATERING | Tưới nước |
| FERTILIZING | Bón phân |
| PESTICIDE | Phun thuốc |
| WEEDING | Làm cỏ |
| HARVESTING | Thu hoạch |
| OTHER | Khác |

# 5. Business Rules

## 5.1 Kiểm tra dữ liệu (Bean Validation)

| **Điều kiện** | **Kết quả** | **Message** |
| --- | --- | --- |
| reason để trống / rỗng | 400 Bad Request | Lý do đính chính không được để trống |
| reason > 500 ký tự | 400 Bad Request | Lý do đính chính không được vượt quá 500 ký tự |
| material > 255 ký tự | 400 Bad Request | Tên vật tư không được vượt quá 255 ký tự |
| quantity ≤ 0 | 400 Bad Request | Số lượng phải lớn hơn 0 |
| unit > 50 ký tự | 400 Bad Request | Đơn vị không được vượt quá 50 ký tự |
| notes > 1000 ký tự | 400 Bad Request | Ghi chú không được vượt quá 1000 ký tự |

## 5.2 Kiểm tra Role / Quyền sở hữu

Controller yêu cầu role VT-02 hoặc VT-03 (`@PreAuthorize`) và permission `FARM_LOG:UPDATE`.

Service kiểm tra quyền đính chính theo vai trò:

- VT-03: chỉ được đính chính nhật ký do chính mình tạo. Nếu không: 403 Forbidden.

  "Bạn chỉ được đính chính nhật ký do bạn ghi."

- VT-02: được đính chính mọi nhật ký trong tổ chức.
- Người dùng khác role: 403 Forbidden.

  "Bạn không có quyền đính chính nhật ký canh tác."

## 5.3 Kiểm tra tồn tại của FarmLog

Nếu `{id}` không tồn tại: 404 Not Found.

"Không tìm thấy nhật ký canh tác"

## 5.4 Kiểm tra quyền theo Organization

Organization của người đăng nhập phải trùng với Organization của ProductionLot chứa nhật ký.

**Nếu khác:** 403 Forbidden.

"Bạn không thuộc tổ chức của lô sản xuất."

## 5.5 Kiểm tra có ít nhất một trường thay đổi

So sánh `correctionData` với giá trị hiệu lực hiện tại của bản ghi (bản gốc nếu chưa có đính chính, hoặc bản đính chính gần nhất nếu đã có).

- Không có trường nào khác: 400 Bad Request.

  "Phải có ít nhất một trường được đính chính so với bản gốc."

## 5.6 Ràng buộc mã truy xuất đã kích hoạt

Lô sản xuất được coi là **đã kích hoạt mã truy xuất** nếu tồn tại ít nhất một TraceCode thuộc bất kỳ Shipment nào của lô đó với trạng thái khác `INACTIVE` (tức ACTIVE/SUSPECT/LOCKED/RECALLED — mã đã rời trạng thái dự thảo).

Trường hợp:

| **Kịch bản** | **Kết quả** |
| --- | --- |
| Lot đã kích hoạt mã truy xuất + người dùng là VT-03 (kể cả người ghi gốc) | 409 Conflict – chặn đính chính |
| Lot đã kích hoạt mã truy xuất + người dùng là VT-02 | Cho phép (reason vẫn bắt buộc theo 5.1) |
| Lot chưa kích hoạt mã truy xuất | Cho phép theo đúng điều kiện 5.2 |

**Nếu bị chặn:**

"Lô sản xuất đã kích hoạt mã truy xuất. Bạn không thể đính chính nhật ký này."

## 5.7 Chuỗi đính chính (correction chain)

- Mọi bản ghi đính chính đều liên kết trực tiếp tới **bản gốc** (`originalFarmLogId = id của bản gốc`), kể cả khi đính chính lên một bản đính chính trước đó — tuân thủ mô tả use case "liên kết tới mục gốc".
- Khi đính chính một bản ghi mà đã có bản đính chính: bản mới được tạo từ **giá trị hiệu lực mới nhất** (merge của chuỗi trước đó + dữ liệu gửi lần này); bản đứng trước được đánh dấu `isCorrected = true`.
- Bản ghi gốc giữ nguyên toàn bộ dữ liệu ban đầu, chỉ cập nhật cờ `isCorrected = true`.

## 5.8 Dữ liệu hệ thống tự sinh

Backend tự gán khi lưu (entity FarmLog):

| **Field** | **Giá trị** |
| --- | --- |
| id | UUID tự sinh cho bản ghi đính chính mới |
| createdBy | Người thực hiện đính chính (lấy từ SecurityContext) |
| correctedBy | Người thực hiện đính chính |
| originalFarmLogId | ID của bản gốc |
| isCorrection | true |
| correctionReason | Lý do từ request |
| createdAt | Thời gian hệ thống theo múi giờ nghiệp vụ (app.timezone) |

Sau khi lưu, hệ thống phát sinh ActivityLogEvent ("CORRECT" nhật ký canh tác) phục vụ lịch sử hoạt động.

# 6. Response

**HTTP 200 OK** — trả về thông tin bản ghi đính chính vừa tạo (DTO: FarmLogResponse mở rộng).

```json
{
    "success": true,
    "status": 200,
    "data": {
        "id": "c1d8a52e-71b4-46ff-9ac2-39b95ebaa9f2",
        "productionLotId": "f034eb60-3895-4479-bb23-976008cfc7be",
        "productionLotName": "Lô chè xuân 2026",
        "activityType": "FERTILIZING",
        "material": "NPK 16-16-8",
        "quantity": 30.0,
        "unit": "kg",
        "executedDate": "2026-07-22",
        "notes": "Bón phân lần 1 cho lô sản xuất",
        "createdByName": "Quản lý HTX Trần B",
        "createdAt": "2026-07-25T09:10:22.341000",
        "attachmentCount": 0,
        "originalFarmLogId": "a9fcbbac-fe01-4ecf-a97e-2d2c7b4ba5f1",
        "isCorrection": true,
        "correctionReason": "Ghi sai số lượng và ngày thực hiện trong lần ghi ban đầu",
        "correctedByName": "Quản lý HTX Trần B",
        "isCorrected": false
    },
    "timestamp": "2026-07-25T02:10:22.352773800Z"
}
```

Các trường mới so với CreateFarmLog:

| **Field** | **Ý nghĩa** |
| --- | --- |
| createdById | UUID của người ghi bản ghi (phục vụ kiểm tra quyền hiển thị nút Đính chính ở frontend) |
| originalFarmLogId | ID bản gốc (null nếu là bản ghi thường) |
| isCorrection | true nếu đây là bản ghi đính chính |
| correctionReason | Lý do đính chính (null nếu bản ghi thường) |
| correctedByName | Tên người đính chính (null nếu bản ghi thường) |
| isCorrected | true nếu bản ghi này đã bị thay thế bằng một bản đính chính (áp dụng cả bản gốc) |

Danh sách nhật ký (`GET /api/v1/farm-logs`) cũng trả về các trường trên, giúp frontend hiển thị trạng thái bản gốc "Đã đính chính" và bản ghi đính chính mới.

# 7. Error Response

## 400 Bad Request

```json
{
    "success": false,
    "status": 400,
    "message": "Lý do đính chính không được để trống"
}
```

Hoặc

```json
{
    "success": false,
    "status": 400,
    "message": "Phải có ít nhất một trường được đính chính so với bản gốc."
}
```

## 403 Forbidden

```json
{
    "success": false,
    "status": 403,
    "message": "Bạn chỉ được đính chính nhật ký do bạn ghi."
}
```

Hoặc

```json
{
    "success": false,
    "status": 403,
    "message": "Bạn không thuộc tổ chức của lô sản xuất."
}
```

## 404 Not Found

```json
{
    "success": false,
    "status": 404,
    "message": "Không tìm thấy nhật ký canh tác"
}
```

## 409 Conflict

```json
{
    "success": false,
    "status": 409,
    "message": "Lô sản xuất đã kích hoạt mã truy xuất. Bạn không thể đính chính nhật ký này."
}
```

# 8. Backend xử lý

```
Client
    │
    ▼
POST /api/v1/farm-logs/{id}/correct
    │
    ▼
Bean Validation (reason bắt buộc, giới hạn ký tự)
    │
    ▼
Kiểm tra Role @PreAuthorize VT-02/VT-03 + PermissionChecker FARM_LOG:UPDATE
    │
    ▼
Tìm FarmLog theo {id}                       ──► 404 nếu không thấy
    │
    ▼
Quyết về bản gốc của chuỗi (root original)
    │
    ▼
Kiểm tra Organization                       ──► 403 nếu khác tổ chức
    │
    ▼
Kiểm tra quyền: VT-02 | VT-03&createdBy==me ──► 403 nếu vi phạm
    │
    ▼
Kiểm tra ít nhất một trường khác            ──► 400 nếu không có gì đổi
    │
    ▼
Kiểm tra mã truy xuất đã kích hoạt          ──► 409 nếu VT-03 bị chặn
    │
    ▼
Tạo FarmLog đính chính (link tới bản gốc, base = giá trị hiệu lực mới nhất)
    │
    ▼
Đánh dấu bản bị thay thế isCorrected=true
    │
    ▼
Publish ActivityLogEvent
    │
    ▼
Map FarmLogResponse
    │
    ▼
Trả Response 200
```

# 9. Phạm vi của Story

- Trong phạm vi NCL-03-CN-006: đính chính 6 trường activityType, material, quantity, unit, executedDate, notes.
- Hệ thống sử dụng giá trị hiệu lực mới nhất cho các nghiệp vụ kiểm tra (thời gian cách ly, độ chín thu hoạch) — sẵn sàng tích hợp với NCL-03-CN-005.
- Bản đính chính xuất hiện trong hồ sơ truy xuất xuất khẩu: các dịch vụ export đọc danh sách farm_logs theo lô, nên bản ghi đính chính được đưa vào tự động kèm metadata (isCorrection, correctionReason, originalFarmLogId).

