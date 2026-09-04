# 📘 API Docs: Cấu hình ngưỡng phát hiện quét bất thường (NCL-08-CN-014)

## 1. Thông tin chung

| Thuộc tính | Giá trị |
| --- | --- |
| **User Story** | NCL-08-CN-014 - Cấu hình ngưỡng phát hiện quét bất thường |
| **Epic** | NCL-08 - Cảnh báo, thu hồi lô và lịch sử hoạt động |
| **Git Branch** | `feature/NCL-08-CN-014_configure-anomaly-thresholds` |
| **Vai trò thực hiện** | `VT-01` - Quản trị viên nền tảng |
| **Tài liệu tham khảo** | QTN-10, QTN-17, NCL-08-CN-001, NCL-08-CN-007 |
| **Phạm vi áp dụng** | Áp dụng cấu hình cho các lượt quét trong tương lai (không tính lại quá khứ) |

---

## 2. Tổng quan kiến trúc & Quy tắc nghiệp vụ

### 2.1. Phân cấp cấu hình ngưỡng (Threshold Resolution)
Hệ thống hỗ trợ 2 tầng cấu hình ngưỡng:
1. **Cấu hình mặc định toàn cục (Global Default Thresholds):** Áp dụng cho tất cả các loại nông sản chưa có cấu hình ghi đè riêng.
2. **Cấu hình ghi đè theo danh mục nông sản (Per-Product-Category Overrides):** Áp dụng ưu tiên cho các mã tem thuộc danh mục nông sản tương ứng (`ProductCategory`).

**Thứ tự ưu tiên khi đánh giá quét tem:**
```text
Lượt quét mới (Scan)
        │
        ▼
Xác định danh mục nông sản (ProductCategory từ Shipment/ProductionLot)
        │
        ├── Đã có cấu hình ghi đè theo danh mục đang Active?
        │       ├── Có  ──> Sử dụng cấu hình ghi đè của danh mục
        │       └── Không ─> Sử dụng cấu hình mặc định toàn cục (Global)
        ▼
Đánh giá quét bất thường & chấm điểm nghi vấn (Suspect Detection / Anomaly Alert)
```

### 2.2. Các thông số ngưỡng (Threshold Parameters)
| Tham số | Kiểu dữ liệu | Ràng buộc | Đơn vị | Ý nghĩa nghiệp vụ |
| --- | --- | --- | --- | --- |
| `maxScansPerHour` | Integer | >= 1 | Lượt | Số lượt quét tối đa cho phép trên 1 mã tem trong vòng 1 giờ. |
| `maxScansPerDay` | Integer | >= 1 | Lượt | Số lượt quét tối đa cho phép trên 1 mã tem trong vòng 24 giờ. |
| `maxDistanceKmPer30Min` | Decimal | >= 0.0 | Kilomet (km) | Khoảng cách di chuyển tối đa cho phép trong khung thời gian quy định (`minTimeBetweenScansMinutes`). |
| `minTimeBetweenScansMinutes` | Integer | >= 0 | Phút | Khung thời gian tối thiểu / cửa sổ xét di chuyển giữa các lần quét. |
| `activationAgeDays` | Integer | >= 0 | Ngày | Số ngày kể từ thời điểm kích hoạt tem mà việc quét được coi là bình thường trong vòng đời sản phẩm. |

### 2.3. Ước lượng tác động (Impact Estimation - Dry-run)
- Cho phép Quản trị viên (`VT-01`) mô phỏng và xem trước số lượng quét bất thường / mã nghi vấn sẽ phát sinh trong **30 ngày gần nhất** nếu áp dụng bộ tham số dự thảo.
- **Quan trọng:** Đây là thao tác tính toán mô phỏng (dry-run), hoàn toàn không ghi đè hay thay đổi dữ liệu cảnh báo/mã tem trong quá khứ.

### 2.4. Tính độc lập & Không tính lại quá khứ
- Thay đổi cấu hình chỉ có hiệu lực với các lượt quét phát sinh **sau thời điểm lưu cấu hình**.
- Không tự động tính toán lại điểm nghi vấn hay trạng thái của các mã tem/lượt quét đã ghi nhận trước đó.

---

## 3. Chi tiết API Endpoints

Tất cả các endpoint đều yêu cầu Header:
```http
Authorization: Bearer <JWT_TOKEN_VT_01>
```

---

### 3.1. Lấy toàn bộ cấu hình ngưỡng (Global + Overrides)

- **Method:** `GET`
- **Path:** `/api/v1/admin/anomaly-thresholds`
- **Quyền truy cập:** `VT-01`
- **Mô tả:** Trả về cấu hình mặc định toàn cục kèm danh sách tất cả các cấu hình ghi đè theo danh mục nông sản.

#### Response: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "data": {
    "global": {
      "id": "11111111-2222-3333-4444-555555555555",
      "productCategoryId": null,
      "productCategoryName": null,
      "maxScansPerHour": 5,
      "maxScansPerDay": 10,
      "maxDistanceKmPer30Min": 50.0,
      "minTimeBetweenScansMinutes": 30,
      "activationAgeDays": 365,
      "isActive": true,
      "createdAt": "2026-08-30T08:00:00",
      "updatedAt": "2026-08-30T08:00:00",
      "createdByName": "Quản trị viên hệ thống",
      "updatedByName": "Quản trị viên hệ thống"
    },
    "categoryOverrides": [
      {
        "id": "22222222-3333-4444-5555-666666666666",
        "productCategoryId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
        "productCategoryName": "Sầu riêng Ri6",
        "maxScansPerHour": 3,
        "maxScansPerDay": 8,
        "maxDistanceKmPer30Min": 40.0,
        "minTimeBetweenScansMinutes": 20,
        "activationAgeDays": 180,
        "isActive": true,
        "createdAt": "2026-08-30T09:00:00",
        "updatedAt": "2026-08-30T09:00:00",
        "createdByName": "Quản trị viên hệ thống",
        "updatedByName": "Quản trị viên hệ thống"
      }
    ]
  },
  "timestamp": "2026-08-30T09:15:00.000Z"
}
```

---

### 3.2. Lấy cấu hình mặc định toàn cục

- **Method:** `GET`
- **Path:** `/api/v1/admin/anomaly-thresholds/global`
- **Quyền truy cập:** `VT-01`

#### Response: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "11111111-2222-3333-4444-555555555555",
    "productCategoryId": null,
    "productCategoryName": null,
    "maxScansPerHour": 5,
    "maxScansPerDay": 10,
    "maxDistanceKmPer30Min": 50.0,
    "minTimeBetweenScansMinutes": 30,
    "activationAgeDays": 365,
    "isActive": true,
    "createdAt": "2026-08-30T08:00:00",
    "updatedAt": "2026-08-30T08:00:00",
    "createdByName": "Quản trị viên hệ thống",
    "updatedByName": "Quản trị viên hệ thống"
  },
  "timestamp": "2026-08-30T09:15:00.000Z"
}
```

---

### 3.3. Cập nhật cấu hình mặc định toàn cục

- **Method:** `PUT`
- **Path:** `/api/v1/admin/anomaly-thresholds/global`
- **Quyền truy cập:** `VT-01`

#### Request Body:
```json
{
  "maxScansPerHour": 6,
  "maxScansPerDay": 12,
  "maxDistanceKmPer30Min": 60.0,
  "minTimeBetweenScansMinutes": 30,
  "activationAgeDays": 365
}
```

#### Response: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Cập nhật cấu hình ngưỡng toàn cục thành công",
  "data": {
    "id": "11111111-2222-3333-4444-555555555555",
    "productCategoryId": null,
    "productCategoryName": null,
    "maxScansPerHour": 6,
    "maxScansPerDay": 12,
    "maxDistanceKmPer30Min": 60.0,
    "minTimeBetweenScansMinutes": 30,
    "activationAgeDays": 365,
    "isActive": true,
    "createdAt": "2026-08-30T08:00:00",
    "updatedAt": "2026-08-30T09:30:00",
    "createdByName": "Quản trị viên hệ thống",
    "updatedByName": "Quản trị viên hệ thống"
  },
  "timestamp": "2026-08-30T09:30:00.000Z"
}
```

---

### 3.4. Lấy danh sách cấu hình ghi đè theo danh mục

- **Method:** `GET`
- **Path:** `/api/v1/admin/anomaly-thresholds/categories`
- **Quyền truy cập:** `VT-01`

#### Response: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "22222222-3333-4444-5555-666666666666",
      "productCategoryId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
      "productCategoryName": "Sầu riêng Ri6",
      "maxScansPerHour": 3,
      "maxScansPerDay": 8,
      "maxDistanceKmPer30Min": 40.0,
      "minTimeBetweenScansMinutes": 20,
      "activationAgeDays": 180,
      "isActive": true,
      "createdAt": "2026-08-30T09:00:00",
      "updatedAt": "2026-08-30T09:00:00",
      "createdByName": "Quản trị viên hệ thống",
      "updatedByName": "Quản trị viên hệ thống"
    }
  ],
  "timestamp": "2026-08-30T09:15:00.000Z"
}
```

---

### 3.5. Tạo mới hoặc cập nhật cấu hình ghi đè theo danh mục

- **Method:** `POST`
- **Path:** `/api/v1/admin/anomaly-thresholds/categories`
- **Quyền truy cập:** `VT-01`

#### Request Body:
```json
{
  "productCategoryId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "maxScansPerHour": 4,
  "maxScansPerDay": 8,
  "maxDistanceKmPer30Min": 45.0,
  "minTimeBetweenScansMinutes": 25,
  "activationAgeDays": 180
}
```

#### Response: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Lưu cấu hình ghi đè danh mục thành công",
  "data": {
    "id": "22222222-3333-4444-5555-666666666666",
    "productCategoryId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
    "productCategoryName": "Sầu riêng Ri6",
    "maxScansPerHour": 4,
    "maxScansPerDay": 8,
    "maxDistanceKmPer30Min": 45.0,
    "minTimeBetweenScansMinutes": 25,
    "activationAgeDays": 180,
    "isActive": true,
    "createdAt": "2026-08-30T09:00:00",
    "updatedAt": "2026-08-30T09:45:00",
    "createdByName": "Quản trị viên hệ thống",
    "updatedByName": "Quản trị viên hệ thống"
  },
  "timestamp": "2026-08-30T09:45:00.000Z"
}
```

---

### 3.6. Xóa cấu hình ghi đè danh mục (Quay về dùng Global)

- **Method:** `DELETE`
- **Path:** `/api/v1/admin/anomaly-thresholds/categories/{id}`
- **Tham số đường dẫn (`id`):** ID của bản ghi cấu hình hoặc ID của danh mục nông sản.
- **Quyền truy cập:** `VT-01`

#### Response: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Đã xóa cấu hình ghi đè danh mục thành công. Danh mục này sẽ sử dụng cấu hình toàn cục.",
  "timestamp": "2026-08-30T09:50:00.000Z"
}
```

---

### 3.7. Ước lượng tác động ngưỡng dự thảo (Impact Estimation)

- **Method:** `POST`
- **Path:** `/api/v1/admin/anomaly-thresholds/estimate`
- **Quyền truy cập:** `VT-01`
- **Mô tả:** Mô phỏng số lượng bất thường sẽ phát sinh trên tập dữ liệu quét 30 ngày gần nhất với các tham số dự thảo.

#### Request Body:
```json
{
  "productCategoryId": null,
  "maxScansPerHour": 4,
  "maxScansPerDay": 8,
  "maxDistanceKmPer30Min": 40.0,
  "minTimeBetweenScansMinutes": 20,
  "activationAgeDays": 180
}
```

#### Response: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Ước lượng tác động thành công",
  "data": {
    "estimatedAnomaliesCount": 12,
    "totalScansAnalyzed": 1450,
    "totalTraceCodesAnalyzed": 320,
    "highFrequencyCount": 5,
    "impossibleTravelCount": 7,
    "activationAgeCount": 2,
    "analysisPeriodDays": 30,
    "message": "Dự kiến có 12 mã tem/lượt quét sẽ bị gắn cờ bất thường trong 30 ngày qua nếu áp dụng ngưỡng này."
  },
  "timestamp": "2026-08-30T09:55:00.000Z"
}
```

---

## 4. Bảng mã lỗi (Error Codes)

| Mã HTTP | Tình huống phát sinh |
| --- | --- |
| `400 Bad Request` | Dữ liệu đầu vào không hợp lệ (số âm, chuỗi rỗng, không đúng định dạng UUID,...). |
| `401 Unauthorized` | Chưa truyền Token hoặc Token đã hết hạn. |
| `403 Forbidden` | Người dùng không có vai trò `VT-01`. |
| `404 Not Found` | Không tìm thấy danh mục nông sản hoặc cấu hình ghi đè cần thao tác. |
| `409 Conflict` | Danh mục nông sản đã có cấu hình ghi đè và xảy ra xung đột dữ liệu. |


