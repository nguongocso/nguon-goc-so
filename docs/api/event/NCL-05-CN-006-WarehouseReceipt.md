## 📘 API Docs: Nhập kho và đối chiếu số lượng bên thu mua

### 🏷️ Thông tin chung
- **User Story:** NCL-05-CN-006 - Nhập kho và đối chiếu số lượng bên thu mua
- **Epic:** NCL-05 - Ghi sự kiện chuỗi cung ứng
- **Nhánh Git:** `feature/warehouse-receipt`
- **Vai trò:** Doanh nghiệp thu mua (VT-04)
- **Mô tả:** Doanh nghiệp thu mua quét mã lô hàng, nhập số lượng thực nhận và tình trạng hàng. Hệ thống so sánh với số lượng đã ghi trên lô, tính chênh lệch và bắt nhập lý do nếu chênh lệch vượt ngưỡng cho phép (2%).

---

### 🔗 1. Endpoint

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `POST` |
| **Endpoint** | `/api/v1/chain-events/warehouse-receipt` |
| **Quyền** | `VT-04` (Doanh nghiệp thu mua) |
| **Content-Type** | `application/json` |

---

### 📥 2. Request

#### Headers
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

#### Body (JSON)
```json
{
  "codeValue": "89300900000006",
  "receivedQuantity": 500.0,
  "conditionNote": "Hàng còn nguyên vẹn, bao bì không rách",
  "receiptDate": "2026-08-11"
}
```

#### Field Description
| Trường | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `codeValue` | string | ✅ Có | Mã truy xuất (tem QR) của lô hàng |
| `receivedQuantity` | number | ✅ Có | Số lượng thực nhận (phải > 0) |
| `conditionNote` | string | ❌ Không | Tình trạng hàng hóa khi nhập kho |
| `receiptDate` | string (ISO date) | ❌ Không | Ngày nhập kho (mặc định = ngày hiện tại) |

---

### 📤 3. Response

#### 3.1. Thành công - Chênh lệch bằng 0 (TC-01)
**HTTP Status:** `201 Created`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",
    "eventType": "WAREHOUSE_RECEIPT",
    "shipmentId": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "shipmentName": "Lô chè Tân Cương T8/2026",
    "declaredQuantity": 500.0,
    "receivedQuantity": 500.0,
    "discrepancy": 0.0,
    "discrepancyPercent": 0.0,
    "isDiscrepancyExceeded": false,
    "reasonRequired": false,
    "conditionNote": "Hàng còn nguyên vẹn",
    "receiptDate": "2026-08-11",
    "recordedAt": "2026-08-11T10:30:00Z",
    "recordedBy": "Nguyễn Văn B"
  },
  "timestamp": "2026-08-11T10:30:00.123Z"
}
```

#### 3.2. Thành công - Chênh lệch vượt ngưỡng, có lý do (TC-02)
**HTTP Status:** `201 Created`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "b2c3d4e5-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "eventType": "WAREHOUSE_RECEIPT",
    "shipmentId": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "shipmentName": "Lô chè Tân Cương T8/2026",
    "declaredQuantity": 500.0,
    "receivedQuantity": 400.0,
    "discrepancy": -100.0,
    "discrepancyPercent": -20.0,
    "isDiscrepancyExceeded": true,
    "reasonRequired": true,
    "reason": "Hàng bị hư hỏng trong quá trình vận chuyển",
    "conditionNote": "Hàng bị ẩm ướt, giảm chất lượng",
    "receiptDate": "2026-08-11",
    "recordedAt": "2026-08-11T10:35:00Z",
    "recordedBy": "Nguyễn Văn B",
    "notificationSent": true
  },
  "timestamp": "2026-08-11T10:35:00.123Z"
}
```

---

### ❌ 4. Error Response

#### 4.1. Số lượng thực nhận = 0 (TC-03)
**HTTP Status:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Số lượng thực nhận phải lớn hơn 0.",
  "timestamp": "2026-08-11T10:30:00.123Z"
}
```

#### 4.2. Mã truy xuất không tồn tại
**HTTP Status:** `404 Not Found`
```json
{
  "success": false,
  "status": 404,
  "message": "Mã lô hàng không tồn tại.",
  "timestamp": "2026-08-11T10:30:00.123Z"
}
```

#### 4.3. Lô hàng không thuộc thương vụ của doanh nghiệp (TC-04)
**HTTP Status:** `403 Forbidden`
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền ghi nhận nhập kho cho lô hàng này. Chỉ doanh nghiệp đã thu mua lô hàng mới được thực hiện.",
  "timestamp": "2026-08-11T10:30:00.123Z"
}
```

#### 4.4. Lô hàng chưa kích hoạt hoặc đã bị thu hồi
**HTTP Status:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Lô hàng chưa được kích hoạt hoặc đã bị thu hồi, không thể ghi nhận nhập kho.",
  "timestamp": "2026-08-11T10:30:00.123Z"
}
```

#### 4.5. Chênh lệch vượt ngưỡng nhưng thiếu lý do
**HTTP Status:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Chênh lệch số lượng vượt ngưỡng cho phép (2%). Vui lòng cung cấp lý do chênh lệch.",
  "data": {
    "declaredQuantity": 500.0,
    "receivedQuantity": 400.0,
    "discrepancyPercent": -20.0,
    "threshold": 2.0
  },
  "timestamp": "2026-08-11T10:30:00.123Z"
}
```

#### 4.6. Chưa xác thực
**HTTP Status:** `401 Unauthorized`
```json
{
  "success": false,
  "status": 401,
  "message": "Bạn cần đăng nhập để thực hiện chức năng này.",
  "timestamp": "2026-08-11T10:30:00.123Z"
}
```

---

### 🧮 5. Business Rules

| Rule | Mô tả |
|---|---|
| **Ngưỡng chênh lệch** | Mặc định là **2%** của số lượng khai báo trên lô hàng. |
| **Tính chênh lệch** | `discrepancy = receivedQuantity - declaredQuantity` |
| **Tính % chênh lệch** | `discrepancyPercent = (discrepancy / declaredQuantity) * 100` |
| **Bắt buộc lý do** | Nếu `|discrepancyPercent| > threshold`, request phải có `reason` (không rỗng). |
| **Lô phải còn hiệu lực** | Lô hàng có trạng thái `ACTIVATED` (chưa bị thu hồi). |
| **Quyền ghi** | Chỉ Doanh nghiệp thu mua (VT-04) đã tạo sự kiện `PROCUREMENT` cho lô đó mới được ghi nhận nhập kho. |
| **Notification** | Nếu chênh lệch vượt ngưỡng, hệ thống gửi thông báo cho Quản lý hợp tác xã (VT-02) của lô hàng. |

---

### 📦 6. DTO Schemas

#### 6.1. Request DTO
```java
public class WarehouseReceiptRequest {
    @NotBlank(message = "Mã truy xuất không được để trống")
    private String codeValue;

    @NotNull(message = "Số lượng thực nhận không được để trống")
    @Positive(message = "Số lượng thực nhận phải lớn hơn 0")
    private Double receivedQuantity;

    private String conditionNote;

    private LocalDate receiptDate;

    @Size(max = 500, message = "Lý do chênh lệch không được vượt quá 500 ký tự")
    private String reason;
}
```

#### 6.2. Response DTO
```java
public class WarehouseReceiptResponse {
    private UUID id;
    private ChainEventType eventType; // WAREHOUSE_RECEIPT
    private UUID shipmentId;
    private String shipmentName;
    private Double declaredQuantity;
    private Double receivedQuantity;
    private Double discrepancy;
    private Double discrepancyPercent;
    private Boolean isDiscrepancyExceeded;
    private Boolean reasonRequired;
    private String reason;
    private String conditionNote;
    private LocalDate receiptDate;
    private LocalDateTime recordedAt;
    private String recordedBy;
    private Boolean notificationSent;
}
```

---

### 📊 7. Sơ đồ luồng xử lý

```
Doanh nghiệp thu mua (VT-04)
    │
    ├─ POST /api/v1/chain-events/warehouse-receipt
    │   Body: { codeValue, receivedQuantity, reason, ... }
    │
    ▼
Validate quyền (VT-04) → 403 nếu sai role
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
Kiểm tra doanh nghiệp đã thu mua lô này chưa → 403 nếu chưa
    │
    ▼
Tính chênh lệch:
    declaredQuantity = Shipment.totalQuantity
    receivedQuantity = request.receivedQuantity
    discrepancy = receivedQuantity - declaredQuantity
    discrepancyPercent = (discrepancy / declaredQuantity) * 100
    │
    ▼
if |discrepancyPercent| > threshold (2%)
    │
    ├─ yes → Kiểm tra reason có tồn tại không
    │   ├─ Có → Lưu sự kiện với reason
    │   └─ Không → 400: "Vui lòng cung cấp lý do"
    │
    └─ no → Lưu sự kiện (reason không bắt buộc)
    │
    ▼
Lưu ChainEvent với eventType = WAREHOUSE_RECEIPT
    │
    ▼
Nếu vượt ngưỡng → Gửi Notification đến VT-02 (Quản lý HTX)
    │
    ▼
Trả về WarehouseReceiptResponse
```

---

### 📝 8. Hướng dẫn phát triển

#### 8.1. Tạo nhánh
```bash
git checkout -b feature/NCL-05-CN-006-warehouse-receipt
```

#### 8.2. Các file cần tạo/sửa
| File | Hành động |
|---|---|
| `ChainEventType.java` | Thêm enum `WAREHOUSE_RECEIPT` |
| `WarehouseReceiptRequest.java` | Tạo mới |
| `WarehouseReceiptResponse.java` | Tạo mới |
| `ChainEventService.java` | Thêm method `recordWarehouseReceipt()` |
| `ChainEventServiceImpl.java` | Implement method |
| `ChainEventController.java` | Thêm endpoint POST `/warehouse-receipt` |
| `NotificationService.java` | Gửi thông báo khi vượt ngưỡng |

#### 8.3. Migration
```sql
-- V19__add_warehouse_receipt_event_type.sql
ALTER TABLE chain_events MODIFY COLUMN event_type ENUM(
    'HARVEST', 'PACKAGING', 'TRANSPORT', 'PROCUREMENT',
    'WAREHOUSE_RECEIPT' -- Thêm mới
);
```

---

### ✅ 9. Test Cases

| TC | Description | Expected |
|---|---|---|
| TC-01 | Luồng thành công, chênh lệch = 0 | Status 201, không bắt reason |
| TC-02 | Chênh lệch vượt ngưỡng, có reason | Status 201, lưu lý do, gửi notification |
| TC-03 | receivedQuantity = 0 | Status 400, message lỗi |
| TC-04 | Doanh nghiệp chưa thu mua lô | Status 403, từ chối |
| TC-05 | Lô đã bị thu hồi | Status 400, message lỗi |
| TC-06 | Vượt ngưỡng nhưng thiếu reason | Status 400, bắt nhập lý do |

---

**Author:** @hienvanla5  
**Date:** 2026-08-11  
**Branch:** `feature/warehouse-receipt`