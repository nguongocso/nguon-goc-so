**API: Ghi nhật ký canh tác**

# 1. Thông tin chung

**Mục tiêu**

Cho phép Người ghi sự kiện (EVENT_RECODER) ghi nhận một hoạt động canh tác cho Lô sản xuất (ProductionLot).

Mỗi bản ghi nhật ký phản ánh một hoạt động như:

- Gieo trồng
- Bón phân
- Phun thuốc
- Tưới nước
- Thu hoạch
- ...

Nhật ký được lưu vào bảng farm_logs (entity FarmLog).

# 2. Endpoint

| **Thuộc tính** | **Giá trị** |
| --- | --- |
| Method | POST |
| URL | /api/v1/farm-logs |
| Authentication | Bearer Token |
| Điều kiện tổ chức | Người dùng phải cùng Organization với ProductionLot |

**Điều kiện**

Người dùng phải:

- Đăng nhập thành công.
- Có role EVENT_RECODER.
- Thuộc cùng Organization với ProductionLot.
- ProductionLot phải ở trạng thái APPROVED hoặc HARVESTED.

# 3. Request Body

DTO: CreateFarmLogRequest

| **Field** | **Type** | **Required** | **Validation / Description** |
| --- | --- | --- | --- |
| productionLotId | UUID | ✓ | @NotNull – "Vui lòng chọn lô sản xuất" |
| activityType | Enum (FarmActivityType) | ✓ | @NotNull – "Vui lòng chọn loại hoạt động" |
| material | String |  | @Size(max=255) – "Tên vật tư không được vượt quá 255 ký tự" |
| quantity | Double |  | @Positive – "Số lượng phải lớn hơn 0" |
| unit | String |  | @Size(max=50) – "Đơn vị không được vượt quá 50 ký tự" |
| executedDate | Date (LocalDate) | ✓ | @NotNull – "Vui lòng chọn ngày thực hiện" |
| notes | String |  | @Size(max=1000) – "Ghi chú không được vượt quá 1000 ký tự" |

## Ví dụ Request

```json
{
    "productionLotId": "f034eb60-3895-4479-bb23-976008cfc7be",
    "activityType": "FERTILIZING",
    "material": "NPK 16-16-8",
    "quantity": 25.0,
    "unit": "kg",
    "executedDate": "2026-07-21",
    "notes": "Bón phân lần 1 cho lô sản xuất"
}
```

# 4. Enum FarmActivityType

Lưu ý: đoạn mã cung cấp chỉ xác nhận giá trị FERTILIZING qua ví dụ thực tế; các giá trị còn lại giữ theo tài liệu trước đó, cần đối chiếu lại với định nghĩa enum FarmActivityType trong mã nguồn.

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
| productionLotId để trống | 400 Bad Request | Vui lòng chọn lô sản xuất |
| activityType để trống | 400 Bad Request | Vui lòng chọn loại hoạt động |
| executedDate để trống | 400 Bad Request | Vui lòng chọn ngày thực hiện |
| quantity ≤ 0 | 400 Bad Request | Số lượng phải lớn hơn 0 |
| material > 255 ký tự | 400 Bad Request | Tên vật tư không được vượt quá 255 ký tự |
| unit > 50 ký tự | 400 Bad Request | Đơn vị không được vượt quá 50 ký tự |
| notes > 1000 ký tự | 400 Bad Request | Ghi chú không được vượt quá 1000 ký tự |

## 5.2 Kiểm tra Role

Chỉ người dùng có role `ROLE_EVENT_RECODER` được phép ghi nhật ký.

**Nếu không đúng role**

"Bạn không có quyền ghi nhật ký canh tác."

## 5.3 Kiểm tra tồn tại của ProductionLot

Nếu productionLotId không tồn tại, hệ thống ném BusinessException:

"Không tìm thấy lô sản xuất"

## 5.4 Kiểm tra trạng thái lô

Chỉ cho phép ghi nhật ký khi trạng thái là:

- APPROVED
- HARVESTED

**Nếu trạng thái khác**

"Chỉ được ghi nhật ký cho lô đã duyệt hoặc đang thu hoạch."

## 5.5 Kiểm tra quyền theo Organization

Organization của người đăng nhập phải trùng với Organization của ProductionLot.

**Nếu khác**

"Bạn không thuộc tổ chức của lô sản xuất."

## 5.6 Dữ liệu hệ thống tự sinh

Backend tự gán khi lưu (FarmLog entity, @PrePersist):

| **Field** | **Giá trị** |
| --- | --- |
| id | UUID tự sinh nếu chưa có (random UUID) |
| createdBy | User đang đăng nhập (lấy từ SecurityContext) |
| createdAt | Thời gian hệ thống (LocalDateTime.now()) |

Frontend không gửi các trường này.

Riêng productionLotName và createdByName không phải cột lưu trữ — đây là dữ liệu được suy ra (join) từ ProductionLot và User khi build response trả về, không lưu trong bảng farm_logs.

# 6. Response

**HTTP 200 OK**

```json
{
    "success": true,
    "status": 200,
    "data": {
        "id": "a9fcbbac-fe01-4ecf-a97e-2d2c7b4ba5f1",
        "productionLotId": "f034eb60-3895-4479-bb23-976008cfc7be",
        "productionLotName": "Lô chè xuân 2026",
        "activityType": "FERTILIZING",
        "material": "NPK 16-16-8",
        "quantity": 25.0,
        "unit": "kg",
        "executedDate": "2026-07-21",
        "notes": "Bón phân lần 1 cho lô sản xuất",
        "createdByName": "System Administrator",
        "createdAt": "2026-07-21T14:27:34.9674332"
    },
    "timestamp": "2026-07-21T07:27:34.985773800Z"
}
```

Thay đổi so với bản trước: mã trạng thái thành công thực tế là 200 OK (không phải 201 Created); response không có field "message", thay vào đó dùng "status" và "timestamp" ở cấp ngoài, đồng nhất với format của API khai báo vùng trồng. Trường createdBy (UUID) được thay bằng createdByName; đồng thời có thêm productionLotName.

# 7. Error Response

Lưu ý: đoạn mã cung cấp dùng chung một loại BusinessException cho các trường hợp không tìm thấy lô, sai trạng thái, sai role và sai quyền tổ chức. Mã HTTP cụ thể cho từng trường hợp (404 / 409 / 403) phụ thuộc vào cấu hình global exception handler — chưa có trong mã nguồn được cung cấp, nên các mã dưới đây giữ theo thiết kế ban đầu và cần được xác nhận lại với global exception handler thực tế.

## 400 Bad Request

```json
{
    "success": false,
    "status": 400,
    "message": "Vui lòng chọn ngày thực hiện"
}
```

## 403 Forbidden

```json
{
    "success": false,
    "status": 403,
    "message": "Bạn không có quyền ghi nhật ký canh tác."
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
    "message": "Không tìm thấy lô sản xuất"
}
```

## 409 Conflict

```json
{
    "success": false,
    "status": 409,
    "message": "Chỉ được ghi nhật ký cho lô đã duyệt hoặc đang thu hoạch."
}
```

# 8. Backend xử lý

```
Client
    │
    ▼
POST /api/v1/farm-logs
    │
    ▼
Bean Validation
    │
    ▼
Kiểm tra Role (ROLE_EVENT_RECODER)
    │
    ▼
Tìm ProductionLot
    │
    ▼
Kiểm tra trạng thái (APPROVED hoặc HARVESTED)
    │
    ▼
Kiểm tra Organization
    │
    ▼
Tạo FarmLog
    │
    ▼
Lưu Database
    │
    ▼
Map FarmLogResponse
    │
    ▼
Trả Response
```

Thay đổi so với bản trước: bổ sung bước kiểm tra Role (EVENT_RECODER) ngay sau Bean Validation, trước khi tìm ProductionLot; đồng thời trạng thái hợp lệ để ghi nhật ký mở rộng thành APPROVED hoặc HARVESTED (trước đó chỉ APPROVED).

# 9. Phạm vi của Story

**Bao gồm**

- Thiết kế DTO Request/Response.
- API tạo nhật ký canh tác.
- Bean Validation.
- Kiểm tra Role (EVENT_RECODER).
- Kiểm tra trạng thái ProductionLot (APPROVED, HARVESTED).
- Kiểm tra Organization.
- Lưu FarmLog.
- Trả về thông tin nhật ký vừa tạo.

**Không bao gồm**

- Đính kèm ảnh/chứng từ (FarmLogAttachment).
- Danh sách nhật ký canh tác.
- Chỉnh sửa nhật ký.
- Xóa nhật ký.
- Quản lý ProductionLot.