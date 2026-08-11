## 📘 API Docs: Theo dõi điều kiện bảo quản khi vận chuyển (NCL-05-CN-007)

### 🏷️ Thông tin chung
- **User Story:** NCL-05-CN-007 - Theo dõi điều kiện bảo quản khi vận chuyển
- **Epic:** NCL-05 - Ghi sự kiện chuỗi cung ứng
- **Nhánh Git:** `feature/NCL-05-CN-007-storage-monitoring`
- **Vai trò:** Người ghi sự kiện (VT-03) hoặc Doanh nghiệp thu mua (VT-04) đang vận chuyển lô hàng.
- **Mô tả:** Người dùng nhập tay nhiệt độ và độ ẩm tại các mốc trong hành trình vận chuyển. Hệ thống so sánh với ngưỡng bảo quản của loại nông sản, đánh dấu mốc vượt ngưỡng và hiển thị cảnh báo trên hành trình lô. Dữ liệu nhập tay (mô phỏng), không kết nối cảm biến thật.

---

### 🔧 1. Khai báo ngưỡng bảo quản trong ProductCategory

Trước khi triển khai, cần bổ sung các trường ngưỡng vào bảng `product_categories`:

| Trường | Kiểu | Mô tả |
|---|---|---|
| `temp_min` | DECIMAL(4,1) | Nhiệt độ tối thiểu (°C) |
| `temp_max` | DECIMAL(4,1) | Nhiệt độ tối đa (°C) |
| `humidity_min` | DECIMAL(5,1) | Độ ẩm tối thiểu (%) |
| `humidity_max` | DECIMAL(5,1) | Độ ẩm tối đa (%) |

**Migration:**
```sql
-- V19__add_storage_thresholds_to_product_category.sql
ALTER TABLE product_categories
    ADD COLUMN temp_min DECIMAL(4,1) NULL,
    ADD COLUMN temp_max DECIMAL(4,1) NULL,
    ADD COLUMN humidity_min DECIMAL(5,1) NULL,
    ADD COLUMN humidity_max DECIMAL(5,1) NULL;
```

> **Lưu ý:** Nếu loại nông sản chưa có ngưỡng, hệ thống không thực hiện kiểm tra (bỏ qua).

---

### 🔗 2. Endpoint ghi mốc bảo quản

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `POST` |
| **Endpoint** | `/api/v1/chain-events/storage-condition` |
| **Quyền** | `VT-03` hoặc `VT-04` (có quyền ghi sự kiện trên lô hàng) |
| **Content-Type** | `application/json` |

#### 📥 Request Body
```json
{
  "codeValue": "89300900000006",
  "temperature": 15.5,
  "humidity": 65.2,
  "recordedAt": "2026-08-11T14:30:00Z"
}
```

| Trường | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `codeValue` | string | ✅ Có | Mã truy xuất (tem QR) của lô hàng |
| `temperature` | number | ✅ Có | Nhiệt độ ghi nhận (°C), cho phép giá trị âm. |
| `humidity` | number | ✅ Có | Độ ẩm ghi nhận (%), phải từ 0 đến 100. |
| `recordedAt` | string (ISO 8601) | ❌ Không | Thời điểm ghi nhận (mặc định = thời gian hiện tại). |

#### 📤 Response (201 Created)
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",
    "eventType": "STORAGE_CONDITION",
    "shipmentId": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "shipmentName": "Lô chè Tân Cương T8/2026",
    "temperature": 15.5,
    "humidity": 65.2,
    "thresholds": {
      "tempMin": 2.0,
      "tempMax": 8.0,
      "humidityMin": 40.0,
      "humidityMax": 70.0
    },
    "isTemperatureExceeded": true,
    "isHumidityExceeded": false,
    "alertLevel": "WARNING",
    "recordedAt": "2026-08-11T14:30:00Z",
    "recordedBy": "Nguyễn Văn C"
  },
  "timestamp": "2026-08-11T14:30:05.123Z"
}
```

| Trường | Mô tả |
|---|---|
| `id` | UUID của sự kiện |
| `eventType` | `STORAGE_CONDITION` |
| `shipmentId` | ID lô hàng |
| `shipmentName` | Tên lô hàng |
| `temperature` | Nhiệt độ đã ghi |
| `humidity` | Độ ẩm đã ghi |
| `thresholds` | Ngưỡng của loại nông sản (có thể null nếu chưa khai báo) |
| `isTemperatureExceeded` | `true` nếu nhiệt độ vượt ngưỡng |
| `isHumidityExceeded` | `true` nếu độ ẩm vượt ngưỡng |
| `alertLevel` | `OK`, `WARNING` (vượt 1 trong 2), hoặc `CRITICAL` (vượt cả 2) |
| `recordedAt` | Thời điểm ghi nhận |
| `recordedBy` | Người ghi |

---

### ❌ 3. Error Response

#### 3.1. Độ ẩm không hợp lệ (> 100%) – TC-03
**HTTP Status:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Độ ẩm phải nằm trong khoảng 0 đến 100%.",
  "timestamp": "2026-08-11T14:30:05.123Z"
}
```

#### 3.2. Mã truy xuất không tồn tại
**HTTP Status:** `404 Not Found`
```json
{
  "success": false,
  "status": 404,
  "message": "Mã lô hàng không tồn tại.",
  "timestamp": "2026-08-11T14:30:05.123Z"
}
```

#### 3.3. Lô hàng không hợp lệ (đã thu hồi hoặc chưa kích hoạt)
**HTTP Status:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Lô hàng chưa được kích hoạt hoặc đã bị thu hồi, không thể ghi nhận mốc bảo quản.",
  "timestamp": "2026-08-11T14:30:05.123Z"
}
```

#### 3.4. Người dùng không có quyền – TC-04
**HTTP Status:** `403 Forbidden`
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền ghi nhận điều kiện bảo quản cho lô hàng này.",
  "timestamp": "2026-08-11T14:30:05.123Z"
}
```

#### 3.5. Lô hàng thuộc tổ chức khác (không có quyền)
**HTTP Status:** `403 Forbidden`
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không thuộc tổ chức quản lý của lô hàng này.",
  "timestamp": "2026-08-11T14:30:05.123Z"
}
```

---

### 📊 4. Business Rules

| Rule | Mô tả |
|---|---|
| **Ngưỡng nhiệt độ** | `temp_min ≤ temperature ≤ temp_max` (dựa trên ProductCategory) |
| **Ngưỡng độ ẩm** | `humidity_min ≤ humidity ≤ humidity_max` |
| **Vượt ngưỡng** | Nếu bất kỳ giá trị nào nằm ngoài ngưỡng, mốc đó bị đánh dấu cảnh báo. |
| **Alert Level** | `OK`: không vượt ngưỡng nào. `WARNING`: vượt 1 chỉ số. `CRITICAL`: vượt cả 2 chỉ số. |
| **Lô phải còn hiệu lực** | Lô hàng có trạng thái `ACTIVATED`. |
| **Quyền ghi** | Chỉ VT-03 hoặc VT-04 được ghi. Không cho phép người dùng ẩn danh (tra cứu công khai). |

---

### 📦 5. DTO Schemas

#### 5.1. Request DTO
```java
public class StorageConditionRequest {
    @NotBlank(message = "Mã truy xuất không được để trống")
    private String codeValue;

    @NotNull(message = "Nhiệt độ không được để trống")
    private Double temperature;

    @NotNull(message = "Độ ẩm không được để trống")
    @Min(value = 0, message = "Độ ẩm phải từ 0 đến 100%")
    @Max(value = 100, message = "Độ ẩm phải từ 0 đến 100%")
    private Double humidity;

    private LocalDateTime recordedAt; // mặc định = now
}
```

#### 5.2. Response DTO
```java
public class StorageConditionResponse {
    private UUID id;
    private ChainEventType eventType; // STORAGE_CONDITION
    private UUID shipmentId;
    private String shipmentName;
    private Double temperature;
    private Double humidity;
    private ThresholdInfo thresholds;
    private Boolean isTemperatureExceeded;
    private Boolean isHumidityExceeded;
    private String alertLevel; // OK, WARNING, CRITICAL
    private LocalDateTime recordedAt;
    private String recordedBy;
}

public class ThresholdInfo {
    private Double tempMin;
    private Double tempMax;
    private Double humidityMin;
    private Double humidityMax;
}
```

---

### 🔄 6. Sơ đồ luồng xử lý

```
VT-03 / VT-04
    │
    ├─ POST /api/v1/chain-events/storage-condition
    │   Body: { codeValue, temperature, humidity, recordedAt }
    │
    ▼
Xác thực & phân quyền (VT-03, VT-04) → 401/403 nếu sai
    │
    ▼
Tìm TraceCode → 404 nếu không tồn tại
    │
    ▼
Tìm Shipment → 404 nếu không có lô hàng
    │
    ▼
Kiểm tra Shipment.status == ACTIVATED → 400 nếu không
    │
    ▼
Kiểm tra quyền tổ chức (validateOrganization) → 403 nếu sai
    │
    ▼
Lấy ProductCategory từ ProductionLot → lấy thresholds
    │
    ▼
So sánh nhiệt độ, độ ẩm với ngưỡng:
    - temperature < temp_min || temperature > temp_max → isTemperatureExceeded = true
    - humidity < humidity_min || humidity > humidity_max → isHumidityExceeded = true
    - alertLevel = xác định dựa trên số lượng vượt ngưỡng
    │
    ▼
Lưu ChainEvent với eventType = STORAGE_CONDITION
    eventData chứa tất cả thông tin (nhiệt độ, độ ẩm, ngưỡng, kết quả)
    │
    ▼
Trả về StorageConditionResponse
```

---

### 🧪 7. Test Cases

| TC | Description | Expected |
|---|---|---|
| TC-01 | Nhiệt độ 4, 5, 6°C (ngưỡng 2–8°C) | Thành công, alertLevel = OK |
| TC-02 | Nhiệt độ 15°C (ngưỡng 2–8°C) | Thành công, isTemperatureExceeded = true, alertLevel = WARNING |
| TC-03 | Độ ẩm = 120% | Validation lỗi 400, message: "Độ ẩm phải từ 0 đến 100%" |
| TC-04 | Người tiêu dùng tra cứu (VT-06 / không đăng nhập) | 403 Forbidden |
| TC-05 | Lô hàng bị thu hồi | 400 Bad Request |
| TC-06 | Loại nông sản chưa có ngưỡng | Vẫn lưu được, thresholds = null, alertLevel = OK |

---

### 📁 8. Các file cần tạo/sửa

| File | Hành động |
|---|---|
| `ProductCategory.java` | Thêm `tempMin`, `tempMax`, `humidityMin`, `humidityMax` |
| `ProductCategoryResponse.java` / DTO | Cập nhật để bao gồm ngưỡng |
| `ChainEventType.java` | Thêm `STORAGE_CONDITION` |
| `StorageConditionRequest.java` | Tạo mới |
| `StorageConditionResponse.java` | Tạo mới |
| `ChainEventService.java` | Thêm method `recordStorageCondition()` |
| `ChainEventServiceImpl.java` | Implement |
| `ChainEventController.java` | Thêm endpoint POST `/storage-condition` |
| `EventFormatter.java` (frontend) | Thêm mapping hiển thị cho `STORAGE_CONDITION` |
| `Timeline` (frontend) | Hiển thị cảnh báo nếu vượt ngưỡng |

---

### 🔗 9. Tên nhánh

```bash
git checkout -b feature/NCL-05-CN-007-storage-monitoring
```

---

**Author:** @hienvanla5  
**Date:** 2026-08-11  
**Branch:** `feature/NCL-05-CN-007-storage-monitoring`