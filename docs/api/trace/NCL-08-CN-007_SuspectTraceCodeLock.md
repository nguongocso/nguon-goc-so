## 📘 API Docs: Khóa mã tem nghi vấn theo dấu hiệu quét bất thường (NCL-08-CN-007)

### 🏷️ Thông tin chung
- **User Story:** NCL-08-CN-007 - Khóa mã tem nghi vấn theo dấu hiệu quét bất thường
- **Epic:** NCL-08 - Cảnh báo, thu hồi lô và lịch sử hoạt động
- **Nhánh Git:** `feature/NCL-08-CN-007-suspect-trace-code-lock`
- **Vai trò:** Quản trị viên nền tảng (VT-01) – xem danh sách nghi vấn và khóa mã.
- **Vị trí file API docs:** `docs/api/trace/SuspectTraceCodeLock.md`
- **Mô tả:** Hệ thống tự động chấm mức nghi vấn cho mã tem dựa trên số lượt quét vượt ngưỡng, khoảng cách địa lý giữa hai lượt quét liên tiếp so với thời gian giữa chúng. Quản trị viên xem danh sách nghi vấn, khóa mã kèm lý do, và mã bị khóa sẽ hiện cảnh báo trên trang tra cứu công khai.

---

### 🔧 1. Mô hình dữ liệu

#### 1.1. Bổ sung trạng thái cho `trace_codes`

| Trường | Kiểu | Mô tả |
|---|---|---|
| `status` | VARCHAR(20) | `ACTIVE`, `SUSPECT`, `LOCKED` (thay đổi từ `ACTIVATED` / `INACTIVE` nếu cần) |
| `suspicion_score` | INTEGER | Điểm nghi vấn (0-100) tính từ các lượt quét bất thường |
| `suspicion_reason` | TEXT | Lý do bị đánh dấu nghi vấn (tự động từ hệ thống) |
| `locked_at` | TIMESTAMP | Thời điểm khóa mã (null nếu chưa khóa) |
| `locked_by` | UUID | ID của người khóa (VT-01) |
| `lock_reason` | TEXT | Lý do khóa (do admin nhập) |

**Migration:**
```sql
-- V19__add_suspect_fields_to_trace_codes.sql
ALTER TABLE trace_codes
    ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE',
    ADD COLUMN suspicion_score INTEGER DEFAULT 0,
    ADD COLUMN suspicion_reason TEXT NULL,
    ADD COLUMN locked_at TIMESTAMP NULL,
    ADD COLUMN locked_by CHAR(36) NULL,
    ADD COLUMN lock_reason TEXT NULL;

-- Index cho truy vấn nhanh
CREATE INDEX idx_trace_codes_status ON trace_codes(status);
CREATE INDEX idx_trace_codes_suspicion_score ON trace_codes(suspicion_score);
```

---

### 🔗 2. Endpoints

#### 2.1. Lấy danh sách mã tem nghi vấn

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `GET` |
| **Endpoint** | `/api/v1/admin/trace-codes/suspect` |
| **Quyền** | `VT-01` |
| **Content-Type** | `application/json` |

**Query Parameters:**
| Tham số | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `minScore` | integer | ❌ | Điểm nghi vấn tối thiểu (mặc định = 30) |
| `status` | string | ❌ | Lọc theo trạng thái (`SUSPECT`, `LOCKED`) |
| `page` | integer | ❌ | Số trang (mặc định = 0) |
| `size` | integer | ❌ | Số bản ghi trên trang (mặc định = 20) |

**📤 Response (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "id": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
        "codeValue": "89300900000006",
        "shipmentName": "Lô chè Tân Cương T8/2026",
        "status": "SUSPECT",
        "suspicionScore": 85,
        "suspicionReason": "2 lượt quét cách nhau 500km trong 10 phút - không thể di chuyển hợp lý",
        "scanCount": 12,
        "uniqueLocations": 5,
        "firstScannedAt": "2026-08-10T08:00:00Z",
        "lastScannedAt": "2026-08-11T14:30:00Z",
        "lockedAt": null,
        "lockedBy": null,
        "lockReason": null
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 20
  },
  "timestamp": "2026-08-11T16:00:00.123Z"
}
```

---

#### 2.2. Chi tiết mã tem nghi vấn

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `GET` |
| **Endpoint** | `/api/v1/admin/trace-codes/{traceCodeId}/suspect-detail` |
| **Quyền** | `VT-01` |

**📤 Response (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "codeValue": "89300900000006",
    "shipmentId": "8c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "shipmentName": "Lô chè Tân Cương T8/2026",
    "status": "SUSPECT",
    "suspicionScore": 85,
    "suspicionReason": "2 lượt quét cách nhau 500km trong 10 phút - không thể di chuyển hợp lý",
    "scanLogs": [
      {
        "scannedAt": "2026-08-10T08:00:00Z",
        "latitude": 21.0285,
        "longitude": 105.8542,
        "location": "Hà Nội, Việt Nam",
        "userAgent": "Mozilla/5.0 ..."
      },
      {
        "scannedAt": "2026-08-10T08:10:00Z",
        "latitude": 16.0544,
        "longitude": 108.2022,
        "location": "Đà Nẵng, Việt Nam",
        "userAgent": "Mozilla/5.0 ..."
      }
    ],
    "anomalyDetails": {
      "totalScans": 12,
      "uniqueLocations": 5,
      "impossibleTravelCount": 2,
      "scoreBreakdown": {
        "highFrequency": 30,
        "impossibleTravel": 40,
        "multipleLocations": 15
      }
    },
    "lockedAt": null,
    "lockedBy": null,
    "lockReason": null
  },
  "timestamp": "2026-08-11T16:00:00.123Z"
}
```

---

#### 2.3. Khóa mã tem nghi vấn

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `POST` |
| **Endpoint** | `/api/v1/admin/trace-codes/{traceCodeId}/lock` |
| **Quyền** | `VT-01` |
| **Content-Type** | `application/json` |

**📥 Request Body**
```json
{
  "reason": "Mã tem bị nghi ngờ làm giả do quét bất thường ở nhiều địa điểm khác nhau trong thời gian ngắn."
}
```

| Trường | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `reason` | string | ✅ Có | Lý do khóa mã (tối thiểu 10 ký tự, tối đa 500) |

**📤 Response (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "codeValue": "89300900000006",
    "status": "LOCKED",
    "lockedAt": "2026-08-11T16:05:00Z",
    "lockedBy": "7f6e5d4c-3333-4a2a-9f3d-1a2b3c4d5e6f",
    "lockedByName": "Nguyễn Văn A",
    "lockReason": "Mã tem bị nghi ngờ làm giả do quét bất thường ở nhiều địa điểm khác nhau trong thời gian ngắn.",
    "notificationSent": true
  },
  "timestamp": "2026-08-11T16:05:01.123Z"
}
```

---

#### 2.4. Mở khóa mã tem (nếu cần)

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `POST` |
| **Endpoint** | `/api/v1/admin/trace-codes/{traceCodeId}/unlock` |
| **Quyền** | `VT-01` |

**📥 Request Body**
```json
{
  "reason": "Đã xác minh, mã tem hợp lệ. Quét bất thường do lỗi kỹ thuật."
}
```

**📤 Response (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "codeValue": "89300900000006",
    "status": "ACTIVE",
    "unlockedAt": "2026-08-11T17:00:00Z",
    "unlockedBy": "7f6e5d4c-3333-4a2a-9f3d-1a2b3c4d5e6f",
    "unlockReason": "Đã xác minh, mã tem hợp lệ."
  },
  "timestamp": "2026-08-11T17:00:01.123Z"
}
```

---

### ❌ 3. Error Response

#### 3.1. Lý do khóa rỗng hoặc quá ngắn – TC-04
**HTTP Status:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Vui lòng nhập lý do khóa (tối thiểu 10 ký tự).",
  "timestamp": "2026-08-11T16:05:01.123Z"
}
```

#### 3.2. Không có quyền – TC-03
**HTTP Status:** `403 Forbidden`
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền khóa mã tem. Chỉ Quản trị viên nền tảng mới được thực hiện.",
  "timestamp": "2026-08-11T16:05:01.123Z"
}
```

#### 3.3. Mã tem không tồn tại
**HTTP Status:** `404 Not Found`
```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy mã tem.",
  "timestamp": "2026-08-11T16:05:01.123Z"
}
```

#### 3.4. Mã tem đã bị khóa
**HTTP Status:** `409 Conflict`
```json
{
  "success": false,
  "status": 409,
  "message": "Mã tem đã bị khóa bởi Quản trị viên khác.",
  "timestamp": "2026-08-11T16:05:01.123Z"
}
```

#### 3.5. Không có dữ liệu quét để đánh giá
**HTTP Status:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Mã tem chưa có đủ lượt quét để đánh giá nghi vấn (cần ít nhất 2 lượt quét).",
  "timestamp": "2026-08-11T16:05:01.123Z"
}
```

---

### 📊 4. Business Rules

| Rule | Mô tả |
|---|---|
| **Phát hiện nghi vấn** | Hệ thống chạy định kỳ (cron job) hoặc real-time sau mỗi lượt quét để tính điểm nghi vấn. |
| **Ngưỡng số lượt quét** | `≥ 10` lượt quét trong 24 giờ → +30 điểm. |
| **Khoảng cách không hợp lý** | Khoảng cách > 50km mà thời gian giữa 2 lượt quét < 30 phút → +40 điểm. |
| **Nhiều địa điểm khác nhau** | `≥ 5` địa điểm khác nhau trong 24 giờ → +15 điểm. |
| **Điểm nghi vấn** | Tổng điểm tối đa = 100. |
| **Ngưỡng nghi vấn** | `≥ 50` điểm → tự động đánh dấu `SUSPECT` và gửi thông báo cho VT-01. |
| **Khóa mã** | Chỉ VT-01 mới có quyền khóa/mở khóa. |
| **Cảnh báo công khai** | Khi mã bị khóa (`LOCKED`), trang tra cứu công khai hiển thị cảnh báo thay vì hành trình. |
| **Lý do khóa** | Bắt buộc, tối thiểu 10 ký tự. |
| **Lịch sử** | Ghi log mỗi lần khóa/mở khóa vào `ActivityLog`. |

---

### 📦 5. DTO Schemas

#### 5.1. SuspectTraceCodeResponse
```java
public class SuspectTraceCodeResponse {
    private UUID id;
    private String codeValue;
    private String shipmentName;
    private String status; // SUSPECT, LOCKED
    private Integer suspicionScore;
    private String suspicionReason;
    private Integer scanCount;
    private Integer uniqueLocations;
    private LocalDateTime firstScannedAt;
    private LocalDateTime lastScannedAt;
    private LocalDateTime lockedAt;
    private UUID lockedBy;
    private String lockReason;
}
```

#### 5.2. SuspectTraceCodeDetailResponse
```java
public class SuspectTraceCodeDetailResponse {
    // ... extends SuspectTraceCodeResponse
    private List<ScanLogDetail> scanLogs;
    private AnomalyDetails anomalyDetails;
}
```

#### 5.3. LockTraceCodeRequest
```java
public class LockTraceCodeRequest {
    @NotBlank(message = "Vui lòng nhập lý do khóa")
    @Size(min = 10, max = 500, message = "Lý do khóa phải từ 10 đến 500 ký tự")
    private String reason;
}
```

#### 5.4. LockTraceCodeResponse
```java
public class LockTraceCodeResponse {
    private UUID id;
    private String codeValue;
    private String status; // LOCKED
    private LocalDateTime lockedAt;
    private UUID lockedBy;
    private String lockedByName;
    private String lockReason;
    private Boolean notificationSent;
}
```

---

### 🔄 6. Sơ đồ luồng xử lý

```
Quét mã (ScanLog mới)
    │
    ▼
Hệ thống kiểm tra số lượt quét của mã
    │
    ├─ < 2 lượt → không đánh giá
    └─ ≥ 2 lượt → tính điểm nghi vấn
        │
        ├─ Số lượt quét ≥ 10 trong 24h → +30 điểm
        ├─ Khoảng cách > 50km & thời gian < 30p → +40 điểm
        ├─ ≥ 5 địa điểm khác nhau → +15 điểm
        │
        ▼
Tổng điểm ≥ 50?
    │
    ├─ Không → giữ nguyên
    └─ Có → status = SUSPECT, gửi Notification đến VT-01
        │
        ▼
VT-01 xem danh sách nghi vấn (GET /admin/trace-codes/suspect)
    │
    ▼
VT-01 xem chi tiết (GET /admin/trace-codes/{id}/suspect-detail)
    │
    ▼
VT-01 khóa mã (POST /admin/trace-codes/{id}/lock)
    ├─ reason không rỗng → status = LOCKED, ghi ActivityLog
    └─ reason rỗng → 400 Bad Request
        │
        ▼
Trang tra cứu công khai hiển thị cảnh báo khi mã bị LOCKED
```

---

### 🧪 7. Test Cases

| TC | Description | Expected |
|---|---|---|
| TC-01 | 2 lượt quét cách nhau 500km trong 10 phút | Điểm = 40, status = SUSPECT (nếu ≥ 50 điểm) |
| TC-02 | Admin khóa mã kèm lý do | status = LOCKED, trang tra cứu hiển thị cảnh báo |
| TC-03 | VT-02 (Quản lý HTX) khóa mã | 403 Forbidden |
| TC-04 | Khóa mã với lý do rỗng | 400 Bad Request |
| TC-05 | Mã tem có 10 lượt quét trong 24h | +30 điểm nghi vấn |
| TC-06 | Mã tem có 5 địa điểm khác nhau | +15 điểm nghi vấn |
| TC-07 | Mã tem đã bị khóa, khóa lần nữa | 409 Conflict |

---

### 📁 8. Các file cần tạo/sửa

| File | Hành động |
|---|---|
| `TraceCode.java` | Thêm `status`, `suspicionScore`, `suspicionReason`, `lockedAt`, `lockedBy`, `lockReason` |
| `TraceCodeStatus.java` | Tạo enum mới (`ACTIVE`, `SUSPECT`, `LOCKED`) |
| `ScanLogRepository.java` | Thêm method đếm lượt quét và vị trí |
| `SuspectDetectionService.java` | Tạo mới service tính điểm nghi vấn |
| `SuspectDetectionServiceImpl.java` | Implement |
| `TraceCodeAdminController.java` | Tạo mới controller cho admin |
| `SuspectTraceCodeResponse.java` | Tạo mới DTO |
| `LockTraceCodeRequest.java` | Tạo mới DTO |
| `LockTraceCodeResponse.java` | Tạo mới DTO |
| **Migration** | `V19__add_suspect_fields_to_trace_codes.sql` |
| **Frontend** | Trang danh sách nghi vấn (admin) |
| **Frontend** | Dialog khóa mã |
| **Frontend** | Cảnh báo trên trang tra cứu công khai |

---

### 🔗 9. Tên nhánh

```bash
git checkout -b feature/NCL-08-CN-007-suspect-trace-code-lock
```

---

### 📌 10. Lưu ý khi triển khai

1. **Cron job chạy định kỳ**: Nên chạy real-time (sau mỗi lượt quét) để phát hiện nhanh, nhưng có thể đặt cron job 5 phút/lần nếu load cao.
2. **Tính toán khoảng cách**: Dùng Haversine formula để tính khoảng cách giữa 2 tọa độ.
3. **Thời gian di chuyển hợp lý**: Giả định vận tốc tối đa 100km/h → thời gian tối thiểu = khoảng cách / 100.
4. **Notification**: Gửi thông báo đến tất cả VT-01 khi có mã mới bị đánh dấu `SUSPECT`.
5. **Cảnh báo công khai**: Khi mã `LOCKED`, trang tra cứu hiển thị thông báo "Mã tem này bị nghi ngờ giả mạo. Vui lòng liên hệ với cơ quan chức năng để kiểm tra."

---

**Author:** @hienvanla5  
**Date:** 2026-08-11  
**Branch:** `feature/NCL-08-CN-007-suspect-trace-code-lock`  
**File:** `docs/api/trace/SuspectTraceCodeLock.md`