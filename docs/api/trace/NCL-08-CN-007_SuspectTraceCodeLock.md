# 📘 API Docs: Khóa mã tem nghi vấn theo dấu hiệu quét bất thường

## 1. Thông tin chung

| Thuộc tính                      | Giá trị                                                            |
| ------------------------------- | ------------------------------------------------------------------ |
| **User Story**                  | NCL-08-CN-007 - Khóa mã tem nghi vấn theo dấu hiệu quét bất thường |
| **Epic**                        | NCL-08 - Cảnh báo, thu hồi lô và lịch sử hoạt động                 |
| **Git Branch**                  | `feature/NCL-08-CN-007-suspect-trace-code-lock`                    |
| **Vai trò thực hiện**           | VT-01 - Quản trị viên nền tảng                                     |
| **API Docs**                    | `docs/api/trace/SuspectTraceCodeLock.md`                           |
| **Detection source**            | `TraceCodeScanLog` được tạo từ **POST /public/trace/{codeValue}/scan** (luồng quét QR thực tế) |
| **Detection mode**              | Real-time, sau mỗi lượt quét QR (POST /scan)                       |
| **Automatic status**            | `ACTIVE → SUSPECT`                                                 |
| **Manual status**               | `SUSPECT → LOCKED`                                                 |
| **Automatic LOCK**              | Không                                                              |
| **Shipment status propagation** | Không                                                              |

### 1.1. Phạm vi chức năng

NCL-08-CN-007 phát hiện mã tem có dấu hiệu bất thường dựa trên **lịch sử quét public của chính mã tem**.

Luồng chính (chỉ luồng quét QR tạo ScanLog):

```text
QR Scanner (frontend, sau khi giải mã QR)
        │
        ▼
POST /api/v1/public/trace/{codeValue}/scan
        │
        ▼
TraceCodeScanLog được tạo
        │
        ▼
SuspectDetectionService.evaluateSuspicion()
        │
        ├── ≥ 10 lượt quét / 24h       → +30
        ├── > 50 km / < 30 phút        → +40
        └── ≥ 5 địa điểm khác nhau     → +15
        │
        ▼
Tổng điểm ≥ 50?
        │
        ├── Không → giữ nguyên ACTIVE
        │
        └── Có
             │
             ▼
          SUSPECT
             │
             ▼
       VT-01 xem danh sách
             │
             ▼
       VT-01 khóa thủ công
             │
             ▼
           LOCKED
```

---

# 2. Nguồn dữ liệu phát hiện nghi vấn

## 2.1. Chỉ `TraceCodeScanLog` được sử dụng

NCL-08-CN-007 sử dụng dữ liệu từ:

```text
TraceCodeScanLog
```

Các trường phục vụ phát hiện:

| Trường        | Ý nghĩa                    |
| ------------- | -------------------------- |
| `traceCodeId` | Mã tem được quét           |
| `scannedAt`   | Thời điểm quét             |
| `latitude`    | Vĩ độ tại thời điểm quét   |
| `longitude`   | Kinh độ tại thời điểm quét |
| `location`    | Thông tin địa điểm         |
| `ipAddress`   | IP của request             |
| `userAgent`   | User-Agent của client      |

### 2.2. Tách biệt TRA CỨU (GET) và QUÉT QR (POST)

**Tra cứu công khai** (_manual lookup, mở URL, reload, chia sẻ liên kết_) là **đọc thuần túy**:

```text
GET /api/v1/public/trace/{codeValue}
    → KHÔNG tạo TraceCodeScanLog
    → KHÔNG tăng lượt quét
    → KHÔNG kích hoạt đánh giá nghi vấn
```

**Quét mã QR thực tế** (_luồng QR scanner của frontend sau khi giải mã payload_) sử dụng endpoint riêng:

```text
POST /api/v1/public/trace/{codeValue}/scan
    → tạo TraceCodeScanLog
    → kích hoạt đánh giá nghi vấn NCL-08-CN-007
    → trả về thông tin truy xuất công khai
```

Cả hai endpoint đều public và nhận các query parameters tùy chọn:

```text
latitude
longitude
```

Ví dụ quét QR:

```http
POST /api/v1/public/trace/89300900000006?latitude=21.0285&longitude=105.8542
```

Khi POST /scan hợp lệ:

```text
PublicTraceService.recordPublicScan()
    ↓
TraceCodeScanLog.save()
    ↓
ScanAnomalyDetectionService.onScanRecorded()
    ↓
SuspectDetectionService.evaluateSuspicion()
```

### 2.3. Không tính sự kiện nghiệp vụ là lượt quét

Các API nghiệp vụ sau **không tạo lượt quét cho NCL-08-CN-007**:

```text
Ghi sự kiện thu mua
Ghi sự kiện vận chuyển
Ghi sự kiện sản xuất
Ghi sự kiện kho
Ghi các ChainEvent khác
```

Đặc biệt:

```text
PROCUREMENT ≠ ScanLog
TRANSPORT  ≠ ScanLog
ChainEvent  ≠ ScanLog
```

Do đó, việc một người dùng ghi sự kiện thu mua hoặc vận chuyển **không làm tăng `scanCount` và không trực tiếp kích hoạt suspect detection**.

> **Lưu ý:** Backend hiện không có cơ chế xác minh rằng request public thực sự được tạo bằng camera QR. Vì vậy, trong phạm vi API, "lượt quét" được hiểu là một public trace request hợp lệ tạo `TraceCodeScanLog`.

---

# 3. Điều kiện bắt đầu đánh giá

Mã tem phải:

* tồn tại;
* đang ở trạng thái cho phép tra cứu;
* không bị `LOCKED`;
* không thuộc Shipment đã `RECALLED`;
* có ít nhất **2 lượt quét** để thực hiện đánh giá.

Nếu:

```text
recentScans.size() < 2
```

hệ thống không thực hiện tính điểm nghi vấn.

---

# 4. Mô hình trạng thái TraceCode

NCL-08-CN-007 sử dụng các trạng thái:

```text
INACTIVE
ACTIVE
SUSPECT
LOCKED
RECALLED
```

Trong đó:

| Status     | Ý nghĩa                        |
| ---------- | ------------------------------ |
| `INACTIVE` | Mã chưa được kích hoạt         |
| `ACTIVE`   | Mã đang hoạt động bình thường  |
| `SUSPECT`  | Mã có dấu hiệu quét bất thường |
| `LOCKED`   | Mã đã bị VT-01 khóa            |
| `RECALLED` | Mã thuộc lô đã bị thu hồi      |

### 4.1. Chuyển trạng thái tự động

```text
ACTIVE
   │
   │ score >= 50
   ▼
SUSPECT
```

Hệ thống **không tự động chuyển**:

```text
SUSPECT → LOCKED
```

### 4.2. Chuyển sang LOCKED

Chỉ VT-01 được phép thực hiện:

```text
SUSPECT
   │
   │ POST /lock
   ▼
LOCKED
```

---

# 5. Thuật toán chấm điểm nghi vấn

Điểm được tính lại dựa trên các ScanLog trong **24 giờ gần nhất**.

```text
totalScore =
    highFrequencyScore
    + impossibleTravelScore
    + multipleLocationsScore
```

Điểm tối đa:

```text
100
```

---

## 5.1. Quét quá nhiều lần

Nếu mã có:

```text
>= 10 lượt quét / 24 giờ
```

cộng:

```text
+30 điểm
```

Ví dụ:

```text
9 scans  → +0
10 scans → +30
15 scans → +30
```

---

## 5.2. Khoảng cách di chuyển không hợp lý

Xét **hai lượt quét liên tiếp** theo `scannedAt`.

Nếu:

```text
distance > 50 km
AND
time difference < 30 minutes
```

thì cộng:

```text
+40 điểm
```

Khoảng cách được tính bằng **Haversine formula**.

Chỉ những scan có đầy đủ:

```text
latitude != null
longitude != null
```

mới được sử dụng cho tính khoảng cách.

### Ví dụ

```text
Scan #1
Hà Nội
21.0285, 105.8542
08:00

        ↓ 10 phút

Scan #2
Đà Nẵng
16.0544, 108.2022
08:10
```

Khoảng cách khoảng:

```text
620 km
```

Thời gian:

```text
10 phút
```

Kết quả:

```text
distance > 50 km
time < 30 phút
→ +40 điểm
```

---

## 5.3. Nhiều địa điểm

Trong 24 giờ, nếu có:

```text
>= 5 địa điểm khác nhau
```

cộng:

```text
+15 điểm
```

Hai vị trí chỉ được xem là khác nhau khi khoảng cách giữa chúng lớn hơn:

```text
0.5 km
```

Các ScanLog không có tọa độ hợp lệ không được tính vào số địa điểm.

---

# 6. Ngưỡng SUSPECT

Sau khi tính tất cả các rule:

```text
totalScore >= 50
```

thì:

```text
TraceCode.status = SUSPECT
```

Đồng thời lưu:

```text
suspicionScore
suspicionReason
```

và gửi notification cho VT-01.

### Ví dụ

```text
Impossible travel       +40
Multiple locations      +15
----------------------------
Total                   55
```

Kết quả:

```text
ACTIVE → SUSPECT
```

### Ví dụ chỉ có hai lượt quét bất thường

```text
Impossible travel       +40
----------------------------
Total                   40
```

Kết quả:

```text
ACTIVE
```

**Chưa đủ điều kiện để chuyển thành `SUSPECT`.**

Điểm này rất quan trọng đối với TC-01: tiêu chí "2 lượt quét cách nhau 500 km trong 10 phút" **chỉ tạo +40 điểm**, không tự nó đạt ngưỡng SUSPECT = 50.

---

# 7. Persistence khi phát hiện SUSPECT

Khi:

```text
totalScore >= 50
```

và TraceCode hiện tại là:

```text
ACTIVE
```

hệ thống cập nhật:

```text
status = SUSPECT
suspicion_score = totalScore
suspicion_reason = ...
```

Sau đó gửi notification tới VT-01.

Nếu TraceCode đã:

```text
SUSPECT
```

hệ thống tiếp tục tính lại và cập nhật:

```text
suspicionScore
suspicionReason
```

nhưng không tạo lại trạng thái.

Các trạng thái sau không bị detector tự động thay đổi:

```text
LOCKED
INACTIVE
RECALLED
```

---

# 8. Shipment không bị chuyển SUSPECT/LOCKED

NCL-08-CN-007 **không thay đổi `ShipmentStatus`**.

Ví dụ:

```text
Shipment
    ├── TraceCode A → SUSPECT
    ├── TraceCode B → ACTIVE
    └── TraceCode C → ACTIVE
```

Không thực hiện:

```text
Shipment → SUSPECT
```

và cũng không tự động:

```text
TraceCode B → SUSPECT
TraceCode C → SUSPECT
```

Chỉ TraceCode có điểm nghi vấn đạt ngưỡng mới được đánh dấu `SUSPECT`.

---

# 9. API Endpoints

## 9.1. Public trace — TRA CỨU (đọc thuần túy)

### Request

```http
GET /api/v1/public/trace/{codeValue}
```

### Query parameters

| Parameter   | Type     | Required | Description                 |
| ----------- | -------- | -------: | --------------------------- |
| `latitude`  | `Double` |       No | Vĩ độ tại vị trí người xem  |
| `longitude` | `Double` |       No | Kinh độ tại vị trí người xem |

### Example

```http
GET /api/v1/public/trace/89300900000006?latitude=21.0285&longitude=105.8542
```

Khi request hợp lệ, hệ thống:

```text
1. Tra cứu TraceCode
2. Trả public trace response
3. KHÔNG tạo TraceCodeScanLog
4. KHÔNG tăng lượt quét
5. KHÔNG kích hoạt suspect detection
```

## 9.2. Public scan — QUÉT QR (tạo ScanLog + kích hoạt đánh giá nghi vấn)

### Request

```http
POST /api/v1/public/trace/{codeValue}/scan
```

### Query parameters

| Parameter   | Type     | Required | Description                 |
| ----------- | -------- | -------: | --------------------------- |
| `latitude`  | `Double` |       No | Vĩ độ tại vị trí quét       |
| `longitude` | `Double` |       No | Kinh độ tại vị trí quét     |

### Example

```http
POST /api/v1/public/trace/89300900000006?latitude=21.0285&longitude=105.8542
```

Khi request hợp lệ, hệ thống:

```text
1. Tra cứu TraceCode (cùng quy tắc validation với GET lookup)
2. Tạo TraceCodeScanLog
3. Lưu thời điểm scannedAt
4. Lưu latitude/longitude nếu được cung cấp
5. Gọi ScanAnomalyDetectionService.onScanRecorded()
   → SuspectDetectionService.evaluateSuspicion()
6. Trả public trace response
```

> **Lưu ý bảo mật:** POST /scan không phải bằng chứng mật mã xác thực việc quét mã vật lý. Nó đại diện cho hợp đồng ứng dụng giữa luồng QR scanner (frontend) và backend. Không mở các chức năng quản trị qua endpoint public này.

---

# 10. Danh sách mã nghi vấn

### Request

```http
GET /api/v1/admin/trace-codes/suspect
```

### Authorization

```text
VT-01
```

### Query parameters

| Parameter  | Type    | Required | Default | Description             |
| ---------- | ------- | -------: | ------: | ----------------------- |
| `minScore` | Integer |       No |    `30` | Điểm nghi vấn tối thiểu |
| `status`   | String  |       No |       — | `SUSPECT` hoặc `LOCKED` |
| `page`     | Integer |       No |     `0` | Trang                   |
| `size`     | Integer |       No |    `20` | Số bản ghi              |

### Example

```http
GET /api/v1/admin/trace-codes/suspect?minScore=50&status=SUSPECT&page=0&size=20
```

### Response

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
        "suspicionScore": 55,
        "suspicionReason": "Khoảng cách không hợp lý; 5 địa điểm khác nhau",
        "scanCount": 5,
        "uniqueLocations": 5,
        "firstScannedAt": "2026-08-10T08:00:00",
        "lastScannedAt": "2026-08-10T11:00:00",
        "lockedAt": null,
        "lockedBy": null,
        "lockReason": null
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 20
  }
}
```

---

# 11. Chi tiết mã nghi vấn

### Request

```http
GET /api/v1/admin/trace-codes/{traceCodeId}/suspect-detail
```

### Authorization

```text
VT-01
```

### Response

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
    "suspicionScore": 55,
    "suspicionReason": "Khoảng cách không hợp lý; 5 địa điểm khác nhau",
    "scanLogs": [],
    "anomalyDetails": {
      "totalScans": 5,
      "uniqueLocations": 5,
      "impossibleTravelCount": 1,
      "scoreBreakdown": {
        "highFrequency": 0,
        "impossibleTravel": 40,
        "multipleLocations": 15
      }
    },
    "lockedAt": null,
    "lockedBy": null,
    "lockReason": null
  }
}
```

---

# 12. Khóa mã tem

### Request

```http
POST /api/v1/admin/trace-codes/{traceCodeId}/lock
```

### Authorization

```text
VT-01
```

### Request body

```json
{
  "reason": "Mã tem có dấu hiệu quét bất thường tại nhiều địa điểm."
}
```

### Validation

| Rule     | Requirement |
| -------- | ----------- |
| `reason` | bắt buộc    |
| Minimum  | 10 ký tự    |
| Maximum  | 500 ký tự   |

### Thành công

```text
SUSPECT → LOCKED
```

### Response

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "codeValue": "89300900000006",
    "status": "LOCKED",
    "lockedAt": "2026-08-11T16:05:00",
    "lockedBy": "7f6e5d4c-3333-4a2a-9f3d-1a2b3c4d5e6f",
    "lockedByName": "Nguyễn Văn A",
    "lockReason": "Mã tem có dấu hiệu quét bất thường tại nhiều địa điểm.",
    "notificationSent": true
  }
}
```

---

# 13. Quy tắc khóa

Chỉ mã:

```text
SUSPECT
```

mới được VT-01 khóa.

Không cho phép:

```text
ACTIVE → LOCKED
```

thông qua API này.

Không tự động khóa khi:

```text
score >= 50
```

Mà phải:

```text
score >= 50
      ↓
   SUSPECT
      ↓
VT-01 xác minh
      ↓
   LOCKED
```

---

# 14. Public trace khi mã bị LOCKED

Khi người tiêu dùng tra cứu mã:

```text
GET /api/v1/public/trace/{codeValue}
```

và TraceCode có:

```text
status = LOCKED
```

hệ thống phải hiển thị cảnh báo mã bị khóa/nghi vấn thay vì cho người dùng thấy hành trình bình thường theo contract của User Story.

Thông điệp nghiệp vụ:

> **Mã tem này đang bị nghi ngờ giả mạo hoặc có dấu hiệu bất thường. Vui lòng liên hệ đơn vị quản lý để được kiểm tra.**

---

# 15. Error responses

## 15.1. Không có quyền

```http
403 Forbidden
```

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền khóa mã tem."
}
```

Áp dụng cho người dùng không có role:

```text
VT-01
```

---

## 15.2. Lý do khóa không hợp lệ

```http
400 Bad Request
```

```json
{
  "success": false,
  "status": 400,
  "message": "Lý do khóa phải từ 10 đến 500 ký tự."
}
```

---

## 15.3. Không tìm thấy TraceCode

```http
404 Not Found
```

```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy mã tem."
}
```

---

## 15.4. TraceCode đã LOCKED

```http
409 Conflict
```

```json
{
  "success": false,
  "status": 409,
  "message": "Mã tem đã bị khóa."
}
```

---

# 16. Business Rules

| Rule                             | Giá trị                               |
| -------------------------------- | ------------------------------------- |
| Detection source                 | `TraceCodeScanLog`                    |
| Trigger                          | Sau mỗi public scan tạo ScanLog       |
| Minimum scans                    | 2                                     |
| High frequency                   | ≥ 10 scans / 24h                      |
| High frequency score             | +30                                   |
| Impossible travel                | >50 km và <30 phút                    |
| Impossible travel score          | +40                                   |
| Unique locations                 | ≥5 / 24h                              |
| Multiple-location score          | +15                                   |
| SUSPECT threshold                | ≥50                                   |
| Maximum score                    | 100                                   |
| Automatic SUSPECT                | Có                                    |
| Automatic LOCK                   | Không                                 |
| Manual LOCK                      | VT-01                                 |
| Shipment SUSPECT                 | Không                                 |
| Shipment LOCKED                  | Không                                 |
| ChainEvent detection             | Không                                 |
| Procurement event counts as scan | Không                                 |
| Transport event counts as scan   | Không                                 |
| GET /public/trace lookup         | Đọc thuần túy — KHÔNG tạo ScanLog      |
| POST /public/trace/{code}/scan   | Tạo ScanLog + kích hoạt đánh giá       |
| QR-camera verification           | Không thuộc backend contract hiện tại |

---

# 17. Acceptance Criteria Mapping

| Acceptance Criteria                            | API/Logic                                           |
| ---------------------------------------------- | --------------------------------------------------- |
| **TC-01** – Hai lượt quét >500km trong 10 phút | +40 điểm; chưa đủ SUSPECT nếu không có điểm bổ sung |
| **TC-02** – VT-01 khóa mã nghi vấn             | `POST /admin/trace-codes/{id}/lock`                 |
| **TC-03** – VT-02 khóa mã                      | `403 Forbidden`                                     |
| **TC-04** – Lý do rỗng                         | `400 Bad Request`                                   |

### Lưu ý TC-01

Theo thuật toán hiện tại:

```text
2 scans
+ impossible travel = 40
```

không đạt:

```text
SUSPECT_THRESHOLD = 50
```

Vì vậy TC-01 **không thể đồng thời giữ nguyên "chỉ 2 lượt quét" và yêu cầu status = SUSPECT** nếu không thay đổi business rule.

Để kiểm thử trạng thái `SUSPECT`, cần bổ sung một điều kiện khác, ví dụ:

```text
Impossible travel +40
+
5 unique locations +15
=
55 → SUSPECT
```

hoặc:

```text
Impossible travel +40
+
10 scans +30
=
70 → SUSPECT
```

**Không tự ý thay đổi threshold chỉ để TC-01 pass.**

---

# 18. Test scenario khuyến nghị

## Scenario A — Impossible travel

```text
Scan 1:
Hà Nội
08:00

Scan 2:
Đà Nẵng
08:10
```

Kết quả:

```text
+40
status = ACTIVE
```

---

## Scenario B — Impossible travel + 5 locations

```text
Scan 1 → Hà Nội
Scan 2 → Đà Nẵng (<30 phút)
Scan 3 → Huế
Scan 4 → Nha Trang
Scan 5 → Cần Thơ
```

Kết quả:

```text
Impossible travel = +40
Unique locations = +15
----------------------
Total = 55

ACTIVE → SUSPECT
```

---

## Scenario C — Admin lock

```text
SUSPECT
   ↓
VT-01 POST /lock
   ↓
LOCKED
```

Public trace:

```text
LOCKED
   ↓
hiển thị cảnh báo
```

---

# 19. Migration

Việc thay đổi database phải tuân theo migration hiện có của repository.

Các field NCL-08-CN-007 cần có trên `trace_codes`:

```text
suspicion_score
suspicion_reason
locked_at
locked_by
lock_reason
```

Status phải hỗ trợ:

```text
SUSPECT
LOCKED
```

**Không thêm `SUSPECT` hoặc `LOCKED` vào `ShipmentStatus` cho NCL-08-CN-007.**

> Tên migration phải được xác định theo migration version thực tế hiện có trong repository. Không sử dụng lại `V19` nếu version đó đã tồn tại.

---

# 20. Activity Log

Khi VT-01 khóa mã, hệ thống ghi nhận hoạt động:

```text
ActivityLog
```

Thông tin tối thiểu:

```text
actor
action
target TraceCode
timestamp
reason
```

Việc mở khóa **không thuộc acceptance criteria hiện tại** và không được xem là requirement bắt buộc của NCL-08-CN-007.

Nếu product bổ sung chức năng unlock sau này, cần tạo requirement/API contract riêng.

---

# 21. Không thuộc phạm vi NCL-08-CN-007

Các nội dung sau **không thuộc contract hiện tại**:

```text
ChainEvent geographic detection
        ↓
PROCUREMENT location detection
        ↓
TRANSPORT location detection
        ↓
Shipment SUSPECT
        ↓
Shipment LOCKED
        ↓
Propagation SUSPECT → tất cả TraceCode
        ↓
Transport latitude/longitude contract
```

Cụ thể:

```text
Ghi sự kiện thu mua
Ghi sự kiện vận chuyển
```

**không được tính là lượt quét và không làm tăng suspicion score.**

---

# 22. Git Branch

```bash
git checkout -b feature/NCL-08-CN-007-suspect-trace-code-lock
```

---

# 23. Files dự kiến

Các file cần kiểm tra/chỉnh sửa theo implementation thực tế:

```text
backend/
└── .../
    ├── trace/
    │   ├── entity/
    │   │   └── TraceCode.java
    │   ├── enum/
    │   │   └── TraceCodeStatus.java
    │   ├── repository/
    │   │   └── TraceCodeRepository.java
    │   ├── service/
    │   │   ├── SuspectDetectionService.java
    │   │   └── SuspectDetectionServiceImpl.java
    │   └── controller/
    │       └── TraceCodeAdminController.java
    │
    └── ...

frontend/
└── .../
    ├── suspect-trace-codes/
    └── public-trace/

docs/
└── api/
    └── trace/
        └── SuspectTraceCodeLock.md
```

Các tên package/file phải được đối chiếu với cấu trúc thực tế của repository trước khi tạo mới.

---

# 24. Summary

NCL-08-CN-007 hoạt động theo mô hình:

```text
        QR SCAN (POST /scan)
                  │
                  ▼
        ┌───────────────────┐
        │ TraceCodeScanLog  │
        └─────────┬─────────┘
                  │
                  ▼
        Suspect Detection
                  │
       ┌──────────┼──────────┐
       ▼          ▼          ▼
     ≥10/24h   >50km/<30m   ≥5 locations
       +30        +40          +15
       └──────────┼────────────┘
                  ▼
             score >= 50
                  │
                  ▼
               SUSPECT
                  │
                  │ VT-01
                  ▼
               LOCKED
                  │
                  ▼
          Public warning
```

**Điểm mấu chốt của contract:**

> **NCL-08-CN-007 phát hiện bất thường dựa trên `TraceCodeScanLog` được tạo bởi `POST /public/trace/{codeValue}/scan` (luồng quét QR). `GET /public/trace/{codeValue}` là đọc thuần túy — không tạo ScanLog, không tăng lượt quét. Ghi sự kiện thu mua hoặc vận chuyển không phải là lượt quét và không tham gia chấm điểm nghi vấn.**
