## 📘 API Docs: Kiểm chứng tính toàn vẹn dòng sự kiện (NCL-08-CN-006)

### 🏷️ Thông tin chung
- **User Story:** NCL-08-CN-006 - Kiểm chứng tính toàn vẹn dòng sự kiện
- **Epic:** NCL-08 - Cảnh báo, thu hồi lô và lịch sử hoạt động
- **Nhánh Git:** `feature/NCL-08-CN-006-event-chain-verification`
- **Vai trò:** Doanh nghiệp thu mua (VT-04), Quản trị viên nền tảng (VT-01), Cán bộ quản lý ngành (VT-05)
- **Mô tả:** Mỗi sự kiện khi ghi nhận được tính một chuỗi băm từ nội dung sự kiện và chuỗi băm của sự kiện liền trước trong cùng lô hàng (Shipment). Trang kiểm chứng tính lại toàn bộ chuỗi và báo dòng sự kiện còn nguyên vẹn hay đã bị can thiệp, chỉ rõ sự kiện đầu tiên bị lệch. Đây là mô phỏng bằng chuỗi băm liên kết, không dùng chuỗi khối.

---

### 🔧 1. Mô hình dữ liệu

#### 1.1. Bổ sung trường vào bảng `chain_events`

| Trường | Kiểu | Mô tả |
|---|---|---|
| `hash` | VARCHAR(64) | SHA-256 hash của sự kiện hiện tại |
| `previous_hash` | VARCHAR(64) | SHA-256 hash của sự kiện trước đó trong cùng lô hàng (null nếu là sự kiện đầu tiên) |

**Migration:**
```sql
-- V19__add_event_hash_fields.sql
ALTER TABLE chain_events
    ADD COLUMN hash VARCHAR(64) NULL,
    ADD COLUMN previous_hash VARCHAR(64) NULL;

-- Tạo index để truy vấn nhanh
CREATE INDEX idx_chain_events_shipment_id_recorded_at 
    ON chain_events(shipment_id, recorded_at);
```

#### 1.2. Cách tính hash

```
hash = SHA-256(
    eventType +
    shipmentId +
    recordedAt +
    recordedBy +
    eventData (JSON) +
    previousHash
)
```

**Trong đó:**
- Tất cả các trường được nối theo thứ tự cố định.
- `eventData` là JSON canonical (sorted keys).
- `previousHash` là hash của sự kiện trước đó (hoặc chuỗi rỗng nếu là sự kiện đầu tiên).
- Kết quả hash là chuỗi hex 64 ký tự.

---

### 🔗 2. Endpoint kiểm chứng toàn vẹn

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `GET` |
| **Endpoint** | `/api/v1/shipments/{shipmentId}/verify-chain` |
| **Quyền** | `VT-01`, `VT-04`, `VT-05` |
| **Content-Type** | `application/json` |

#### 📤 Response (200 OK)

**TC-01: Dòng sự kiện còn nguyên vẹn**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "shipmentId": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "shipmentName": "Lô chè Tân Cương T8/2026",
    "totalEvents": 5,
    "isIntegrityVerified": true,
    "verificationStatus": "INTACT",
    "failedEventIndex": null,
    "failedEventId": null,
    "verifiedAt": "2026-08-11T15:30:00Z",
    "hashAlgorithm": "SHA-256",
    "events": [
      {
        "index": 1,
        "eventId": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "HARVEST",
        "recordedAt": "2026-08-11T10:00:00Z",
        "hash": "abc123...",
        "previousHash": null,
        "isValid": true
      },
      {
        "index": 2,
        "eventId": "b2c3d4e5-2222-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "PACKAGING",
        "recordedAt": "2026-08-11T10:30:00Z",
        "hash": "def456...",
        "previousHash": "abc123...",
        "isValid": true
      }
    ]
  },
  "timestamp": "2026-08-11T15:30:01.123Z"
}
```

**TC-02: Dòng sự kiện bị can thiệp (gãy chuỗi)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "shipmentId": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "shipmentName": "Lô chè Tân Cương T8/2026",
    "totalEvents": 5,
    "isIntegrityVerified": false,
    "verificationStatus": "BROKEN",
    "failedEventIndex": 3,
    "failedEventId": "c3d4e5f6-3333-4a2a-9f3d-1a2b3c4d5e6f",
    "failureReason": "Hash mismatch: expected abc123..., got xyz789...",
    "verifiedAt": "2026-08-11T15:30:00Z",
    "hashAlgorithm": "SHA-256",
    "events": [
      {
        "index": 1,
        "eventId": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "HARVEST",
        "recordedAt": "2026-08-11T10:00:00Z",
        "hash": "abc123...",
        "previousHash": null,
        "isValid": true
      },
      {
        "index": 2,
        "eventId": "b2c3d4e5-2222-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "PACKAGING",
        "recordedAt": "2026-08-11T10:30:00Z",
        "hash": "def456...",
        "previousHash": "abc123...",
        "isValid": true
      },
      {
        "index": 3,
        "eventId": "c3d4e5f6-3333-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "TRANSPORT",
        "recordedAt": "2026-08-11T11:00:00Z",
        "hash": "xyz789...",
        "previousHash": "def456...",
        "isValid": false,
        "expectedHash": "ghi012..."
      }
    ]
  },
  "timestamp": "2026-08-11T15:30:01.123Z"
}
```

---

### ❌ 3. Error Response

#### 3.1. Lô hàng không tồn tại
**HTTP Status:** `404 Not Found`
```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy lô hàng.",
  "timestamp": "2026-08-11T15:30:01.123Z"
}
```

#### 3.2. Chưa có sự kiện nào – TC-04
**HTTP Status:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Lô hàng chưa có sự kiện nào để kiểm chứng.",
  "timestamp": "2026-08-11T15:30:01.123Z"
}
```

#### 3.3. Không có quyền – TC-04 (phiên tra cứu công khai)
**HTTP Status:** `403 Forbidden`
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền kiểm chứng dòng sự kiện của lô này.",
  "timestamp": "2026-08-11T15:30:01.123Z"
}
```

#### 3.4. Lô hàng thuộc tổ chức khác (không có quyền)
**HTTP Status:** `403 Forbidden`
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không thuộc tổ chức quản lý của lô hàng này.",
  "timestamp": "2026-08-11T15:30:01.123Z"
}
```

---

### 📊 4. Business Rules

| Rule | Mô tả |
|---|---|
| **Hash tính khi ghi** | Mỗi sự kiện mới được tính hash ngay khi lưu và lưu cùng bản ghi. |
| **Hash của sự kiện đầu tiên** | `previousHash = null` (chuỗi rỗng khi tính hash). |
| **Hash của sự kiện tiếp theo** | `previousHash = hash của sự kiện trước đó` trong cùng Shipment. |
| **Kiểm chứng** | Tính lại hash từ đầu đến cuối, so sánh với hash đã lưu. |
| **Phát hiện giả mạo** | Nếu hash không khớp, dừng và đánh dấu sự kiện bị lệch đầu tiên. |
| **Lưu lịch sử kiểm chứng** | Mỗi lần kiểm chứng, ghi vào `ActivityLog` với kết quả. |
| **Thứ tự sự kiện** | Sắp xếp theo `recorded_at` tăng dần. |
| **Chỉ kiểm chứng sự kiện không bị sửa** | Sự kiện `is_correction = true` vẫn được kiểm chứng (nhưng lưu ý nó là sự kiện đính chính). |

---

### 📦 5. DTO Schemas

#### 5.1. Response DTO
```java
public class ChainVerificationResponse {
    private UUID shipmentId;
    private String shipmentName;
    private Integer totalEvents;
    private Boolean isIntegrityVerified;
    private String verificationStatus; // "INTACT", "BROKEN"
    private Integer failedEventIndex;
    private UUID failedEventId;
    private String failureReason;
    private LocalDateTime verifiedAt;
    private String hashAlgorithm;
    private List<EventVerificationItem> events;
}

public class EventVerificationItem {
    private Integer index;
    private UUID eventId;
    private String eventType;
    private LocalDateTime recordedAt;
    private String hash;
    private String previousHash;
    private Boolean isValid;
    private String expectedHash; // chỉ có khi isValid = false
}
```

#### 5.2. ActivityLog (ghi lịch sử kiểm chứng)
```java
// Khi kiểm chứng được thực hiện, ghi vào ActivityLog
{
    "action": "VERIFY_CHAIN",
    "description": "Kiểm chứng dòng sự kiện lô hàng: " + shipmentName,
    "entityType": "SHIPMENT",
    "entityId": shipmentId,
    "details": {
        "totalEvents": 5,
        "isIntegrityVerified": true,
        "verificationStatus": "INTACT"
    }
}
```

---

### 🔄 6. Sơ đồ luồng xử lý

```
VT-01 / VT-04 / VT-05
    │
    ├─ GET /api/v1/shipments/{shipmentId}/verify-chain
    │
    ▼
Xác thực & phân quyền → 401/403 nếu sai
    │
    ▼
Tìm Shipment → 404 nếu không tồn tại
    │
    ▼
Lấy tất cả ChainEvent của Shipment, sắp xếp theo recorded_at ASC
    │
    ▼
Nếu không có sự kiện → 400: "Chưa có sự kiện nào để kiểm chứng"
    │
    ▼
Với mỗi sự kiện (theo thứ tự):
    │
    ├─ Lấy eventData, các trường cần thiết
    ├─ Lấy previousHash = hash của sự kiện trước (hoặc "")
    ├─ Tính hash mới: SHA-256(eventType + shipmentId + recordedAt + recordedBy + eventData + previousHash)
    ├─ So sánh với hash đã lưu
    │   ├─ Nếu khớp → tiếp tục
    │   └─ Nếu không khớp → đánh dấu isIntegrityVerified = false, lưu vị trí lỗi, dừng
    │
    ▼
Nếu tất cả khớp → isIntegrityVerified = true, status = "INTACT"
    │
    ▼
Ghi ActivityLog (lịch sử kiểm chứng)
    │
    ▼
Trả về ChainVerificationResponse
```

---

### 🧪 7. Test Cases

| TC | Description | Expected |
|---|---|---|
| TC-01 | Lô có 5 sự kiện, không bị sửa | `isIntegrityVerified = true`, `verificationStatus = "INTACT"` |
| TC-02 | Sự kiện thứ 3 bị sửa trong DB | `isIntegrityVerified = false`, `verificationStatus = "BROKEN"`, `failedEventIndex = 3` |
| TC-03 | Lưu lịch sử kiểm chứng | Mỗi lần kiểm chứng đều có bản ghi `ActivityLog` |
| TC-04 | Lô chưa có sự kiện | 400 Bad Request, message: "Chưa có sự kiện nào" |
| TC-05 | Người dùng tra cứu công khai (VT-06) | 403 Forbidden |
| TC-06 | Sự kiện đính chính (correction) | Vẫn được kiểm chứng và tính hash (tùy vào quy tắc, có thể bỏ qua correction) |

---

### 📁 8. Các file cần tạo/sửa

| File | Hành động |
|---|---|
| `ChainEvent.java` | Thêm `hash` và `previousHash` |
| `ChainEventService.java` | Thêm `verifyChainIntegrity(UUID shipmentId)` |
| `ChainEventServiceImpl.java` | Implement tính toán hash và kiểm chứng |
| `ChainEventController.java` | Thêm endpoint GET `/shipments/{shipmentId}/verify-chain` |
| `ChainVerificationResponse.java` | Tạo mới DTO |
| `EventVerificationItem.java` | Tạo mới DTO |
| `ActivityLog` (đã có) | Ghi log mỗi lần kiểm chứng |
| **Migration** | `V19__add_event_hash_fields.sql` |
| **Frontend** | Thêm trang kiểm chứng toàn vẹn |
| **Frontend** | Hiển thị kết quả kiểm chứng với trạng thái xanh/đỏ |

---

### 📌 9. Lưu ý khi triển khai

1. **Hash tính khi ghi sự kiện**: Khi tạo mới `ChainEvent`, cần tính hash ngay và lưu cùng bản ghi.
2. **Thứ tự sự kiện**: Sắp xếp theo `recorded_at` (có thể dùng `created_at` nếu `recorded_at` không có).
3. **Performance**: Với lô hàng có nhiều sự kiện (hàng trăm), cần tối ưu truy vấn và tính toán (có thể dùng batch).
4. **EventData**: Khi tính hash, cần canonical JSON (sort keys) để đảm bảo nhất quán.
5. **Lịch sử kiểm chứng**: Ghi vào bảng `activity_logs` với action = `VERIFY_CHAIN`.

---

### 🔗 10. Tên nhánh

```bash
git checkout -b feature/NCL-08-CN-006-event-chain-verification
```

---

**Author:** @hienvanla5  
**Date:** 2026-08-11  
**Branch:** `feature/NCL-08-CN-006-event-chain-verification`