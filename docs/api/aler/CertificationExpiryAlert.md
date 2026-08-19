# API Docs — Cảnh báo chứng nhận sắp hết hạn (Certification Expiry Alert)

*Mã User Story: NCL-09-CN-005 Cảnh báo chứng nhận sắp hết hạn*

---

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
| :--- | :--- | :--- | :--- |
| 2026-08-02 | v1.0.0 | Khởi tạo tài liệu đặc tả API cảnh báo hết hạn chứng nhận, tích hợp in-app notifications | Antigravity |

---

## 1. Thông tin chung

### 1.1 Mục tiêu
Hệ thống tự động theo dõi ngày hết hạn (`expiryDate`) của các chứng nhận (`Certification`) thuộc các tổ chức/hợp tác xã. Khi chứng nhận tiến gần đến ngày hết hạn (dưới ngưỡng cảnh báo cấu hình) hoặc đã chính thức hết hạn, hệ thống sẽ:
1. Tự động tạo cảnh báo (`Alert`) lưu vào cơ sở dữ liệu.
2. Tự động gửi thông báo (`Notification`) vào hộp thư thông báo của các tài khoản có vai trò Quản lý hợp tác xã (`VT-02`) thuộc tổ chức đó và Quản trị viên hệ thống (`VT-01`).
3. Đánh dấu và ngăn chặn việc gắn các chứng nhận hết hạn này cho các lô sản xuất mới theo Quy tắc nghiệp vụ `QTN-13`.

### 1.2 Vai trò & Phân quyền
- **Quản lý hợp tác xã (VT-02):**
  - Nhận thông báo trong hộp thông báo cá nhân khi chứng nhận của tổ chức mình sắp hết hạn/đã hết hạn.
  - Xem danh sách cảnh báo chứng nhận thuộc tổ chức của mình.
  - Thực hiện xác nhận/xử lý cảnh báo sau khi đã gia hạn chứng nhận.
- **Quản trị viên nền tảng (VT-01):**
  - Xem và xử lý toàn bộ cảnh báo chứng nhận trên hệ thống.
  - Có quyền cấu hình ngưỡng cảnh báo hoặc kích hoạt thủ công tiến trình quét kiểm tra.

### 1.3 Quy tắc nghiệp vụ (Business Rules)
- **QTN-13 (Chỉ gắn và hiển thị chứng nhận còn hiệu lực):**
  - Chứng nhận đã hết hạn (`expiryDate < LocalDate.now()`) sẽ KHÔNG được phép gắn vào bất kỳ lô sản xuất (`ProductionLot`) mới nào.
  - Trên trang tra cứu công khai, chứng nhận hết hạn sẽ không hiển thị là đang đạt hoặc hiển thị rõ trạng thái đã hết hạn.
- **Ngưỡng cảnh báo sắp hết hạn (Threshold):**
  - Ngưỡng cảnh báo được cấu hình thông qua thuộc tính hệ thống: `app.certification.expiry-warning-threshold-days` (mặc định là `30` ngày).
  - Khi `0 < expiryDate - LocalDate.now() <= threshold`, hệ thống tạo Alert với loại `CERT_EXPIRING` và mức độ nghiêm trọng `MEDIUM`.
  - Khi `expiryDate < LocalDate.now()`, hệ thống tạo Alert với loại `CERT_EXPIRED` và mức độ nghiêm trọng `HIGH`.
- **Tần suất quét (Batch Check):**
  - Một tiến trình nền (Scheduled Job/Cron) chạy định kỳ mỗi ngày lúc 01:00 AM để quét và cập nhật trạng thái thời hạn của tất cả các chứng nhận.

---

## 2. Đặc tả các Endpoints

### 2.1 GET /api/v1/alerts

**Description:** Lấy danh sách cảnh báo (bao gồm cảnh báo quét bất thường và cảnh báo hết hạn chứng nhận). Đối với Quản lý HTX (`VT-02`), chỉ trả về cảnh báo liên quan đến chứng nhận hoặc mã truy xuất của tổ chức mình.

**Authentication:** Yêu cầu Token. Quyền truy cập: `VT-01` (Xem toàn hệ thống), `VT-02` (Xem trong phạm vi tổ chức).

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Query | `type` | String | No | Lọc theo loại cảnh báo. Giá trị hỗ trợ: `SCAN_ANOMALY`, `CERT_EXPIRING`, `CERT_EXPIRED` | `"CERT_EXPIRING"` |
| Query | `status` | String | No | Lọc theo trạng thái xử lý: `PENDING`, `RESOLVED` | `"PENDING"` |
| Query | `fromDate` | String | No | Định dạng ngày `YYYY-MM-DD`. Lọc cảnh báo tạo ra từ ngày này | `"2026-08-01"` |
| Query | `toDate` | String | No | Định dạng ngày `YYYY-MM-DD`. Lọc cảnh báo tạo ra đến ngày này | `"2026-08-31"` |
| Query | `page` | int | No | Trang số, mặc định = `0` | `0` |
| Query | `size` | int | No | Số bản ghi trên mỗi trang, mặc định = `10` | `10` |

**Request Example (JSON)**
*Không có Body cho phương thức GET.*
```http
GET /api/v1/alerts?type=CERT_EXPIRING&status=PENDING&page=0&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response — Success**
| Status Code | When it occurs |
|-------------|-------------------------------------|
| 200 OK      | Lấy danh sách cảnh báo thành công |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "id": "e2a3b4c5-5555-4a2a-9f3d-1a2b3c4d5e6f",
        "type": "CERT_EXPIRING",
        "relatedEntityType": "Certification",
        "relatedEntityId": "7d6c5b4a-1234-4a2a-9f3d-1a2b3c4d5e6f",
        "severity": "MEDIUM",
        "details": {
          "certificationName": "Chứng nhận VietGAP Sầu Riêng Ri6",
          "certificationCode": "VG-SR-2026-001",
          "issuedBy": "Trung tâm Kiểm định Chất lượng Nông nghiệp",
          "issueDate": "2025-08-15",
          "expiryDate": "2026-08-15",
          "daysRemaining": 13,
          "thresholdConfigured": 30
        },
        "status": "PENDING",
        "createdAt": "2026-08-02T01:00:00Z",
        "resolvedAt": null,
        "resolvedBy": null
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10
  },
  "timestamp": "2026-08-02T10:30:00.123456Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|-------------|---------------------|-------|
| 401 Unauthorized | - | Token bị thiếu hoặc hết hạn |
| 403 Forbidden | - | Người dùng không phải `VT-01` hoặc `VT-02` |

**Error Response Example**
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền xem cảnh báo."
}
```

---

### 2.2 PATCH /api/v1/alerts/{alertId}/resolve

**Description:** Đánh dấu một cảnh báo đã được xử lý (ví dụ: sau khi Quản lý HTX đã gia hạn chứng nhận thành công ngoài đời thực). Hành động này ghi nhận lại thông tin người giải quyết và tự động tạo nhật ký hoạt động (`AuditLog`).

**Authentication:** Yêu cầu Token. Quyền truy cập: `VT-01` (Giải quyết mọi cảnh báo), `VT-02` (Chỉ giải quyết cảnh báo thuộc tổ chức của mình).

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Path | `alertId` | UUID | Yes | ID của cảnh báo cần xử lý | `"e2a3b4c5-5555-4a2a-9f3d-1a2b3c4d5e6f"` |
| Body | `resolutionNote` | String | Yes | Ghi chú về cách giải quyết (không trống, tối đa 500 ký tự) | `"Đã tiến hành gia hạn chứng nhận VietGAP mới có giá trị đến 2027-08-15."` |

**Request Example (JSON)**
```json
{
  "resolutionNote": "Đã tiến hành gia hạn chứng nhận VietGAP mới có giá trị đến 2027-08-15."
}
```

**Response — Success**
| Status Code | When it occurs |
|-------------|-------------------------------------|
| 200 OK      | Xử lý cảnh báo thành công và lưu vết |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "e2a3b4c5-5555-4a2a-9f3d-1a2b3c4d5e6f",
    "status": "RESOLVED",
    "resolvedAt": "2026-08-02T10:35:10Z",
    "resolvedBy": "7f6e5d4c-3333-4a2a-9f3d-1a2b3c4d5e6f",
    "auditLogId": "b3f1a2e0-6c1a-4e2a-9f3d-1a2b3c4d5e6f"
  },
  "timestamp": "2026-08-02T10:35:10.987654Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|-------------|---------------------|-------|
| 400 Bad Request | - | Cảnh báo đã được giải quyết từ trước, hoặc dữ liệu ghi chú không hợp lệ |
| 401 Unauthorized | - | Token bị thiếu hoặc hết hạn |
| 403 Forbidden | - | Người dùng không có quyền xử lý cảnh báo này |
| 404 Not Found | - | Cảnh báo không tồn tại trên hệ thống |

**Error Response Example**
```json
{
  "success": false,
  "status": 400,
  "message": "Cảnh báo không thể xử lý."
}
```

---

### 2.3 POST /api/v1/certifications/check-expiry

**Description:** Kích hoạt thủ công tiến trình quét kiểm tra hạn của tất cả các chứng nhận và tạo cảnh báo nếu thỏa mãn điều kiện. Endpoint này phục vụ việc kiểm thử, vận hành và quản trị hệ thống.

**Authentication:** Yêu cầu Token. Quyền truy cập: Chỉ `VT-01` (Quản trị viên nền tảng).

**Request**
*Không có Body và tham số truy vấn cho API này.*

**Request Example (JSON)**
```http
POST /api/v1/certifications/check-expiry
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response — Success**
| Status Code | When it occurs |
|-------------|-------------------------------------|
| 200 OK      | Kích hoạt tiến trình thành công và trả về số lượng chứng nhận được phát hiện sắp/đã hết hạn |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "totalScanned": 150,
    "newExpiringAlertsCreated": 2,
    "newExpiredAlertsCreated": 1
  },
  "timestamp": "2026-08-02T10:32:00.123456Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|-------------|---------------------|-------|
| 401 Unauthorized | - | Token bị thiếu hoặc hết hạn |
| 403 Forbidden | - | Người dùng không phải là `VT-01` |

**Error Response Example**
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện hành động này."
}
```

---

### 2.4 GET /api/v1/notifications

**Description:** Lấy danh sách các thông báo cá nhân thuộc về người dùng đang đăng nhập (bao gồm thông báo hệ thống, thông báo cảnh báo chứng nhận hết hạn/quét bất thường).

**Authentication:** Yêu cầu Token. Quyền truy cập: Mọi người dùng đã đăng nhập.

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Query | `isRead` | Boolean | No | Lọc thông báo đã đọc (`true`) hoặc chưa đọc (`false`) | `false` |
| Query | `page` | int | No | Trang số, mặc định = `0` | `0` |
| Query | `size` | int | No | Số bản ghi trên mỗi trang, mặc định = `10` | `10` |

**Request Example (JSON)**
```http
GET /api/v1/notifications?isRead=false&page=0&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response — Success**
| Status Code | When it occurs |
|-------------|-------------------------------------|
| 200 OK      | Lấy danh sách thông báo thành công |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "id": "c1d2e3f4-6666-4a2a-9f3d-1a2b3c4d5e6f",
        "type": "ALERT",
        "title": "Chứng nhận sắp hết hiệu lực",
        "content": "Chứng nhận 'VietGAP Sầu Riêng Ri6' (mã VG-SR-2026-001) sắp hết hạn vào ngày 2026-08-15 (còn 13 ngày). Vui lòng gia hạn.",
        "isRead": false,
        "readAt": null,
        "createdAt": "2026-08-02T01:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10
  },
  "timestamp": "2026-08-02T10:30:15.111222Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|-------------|---------------------|-------|
| 401 Unauthorized | - | Token bị thiếu hoặc hết hạn |

---

### 2.5 PATCH /api/v1/notifications/{notificationId}/read

**Description:** Đánh dấu một thông báo cụ thể là đã đọc.

**Authentication:** Yêu cầu Token. Quyền truy cập: Chỉ chủ sở hữu thông báo.

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Path | `notificationId` | UUID | Yes | ID của thông báo cần đánh dấu đã đọc | `"c1d2e3f4-6666-4a2a-9f3d-1a2b3c4d5e6f"` |

**Request Example (JSON)**
*Không có Body cho phương thức PATCH này.*
```http
PATCH /api/v1/notifications/c1d2e3f4-6666-4a2a-9f3d-1a2b3c4d5e6f/read
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response — Success**
| Status Code | When it occurs |
|-------------|-------------------------------------|
| 200 OK      | Đánh dấu đã đọc thành công |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "c1d2e3f4-6666-4a2a-9f3d-1a2b3c4d5e6f",
    "isRead": true,
    "readAt": "2026-08-02T10:36:00Z"
  },
  "timestamp": "2026-08-02T10:36:00.123456Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|-------------|---------------------|-------|
| 401 Unauthorized | - | Token bị thiếu hoặc hết hạn |
| 403 Forbidden | - | Người dùng không phải là chủ sở hữu của thông báo này |
| 404 Not Found | - | Không tìm thấy thông báo tương ứng |

---

### 2.6 PATCH /api/v1/notifications/read-all

**Description:** Đánh dấu tất cả thông báo chưa đọc của người dùng đang đăng nhập là đã đọc.

**Authentication:** Yêu cầu Token. Quyền truy cập: Mọi người dùng đã đăng nhập.

**Request**
*Không có Body và tham số cho API này.*

**Request Example (JSON)**
```http
PATCH /api/v1/notifications/read-all
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response — Success**
| Status Code | When it occurs |
|-------------|-------------------------------------|
| 200 OK      | Đánh dấu đã đọc tất cả thành công |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "markedReadCount": 5
  },
  "timestamp": "2026-08-02T10:37:00.123456Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|-------------|---------------------|-------|
| 401 Unauthorized | - | Token bị thiếu hoặc hết hạn |

---

## 3. Luồng Xử Lý Phía Máy Chủ (Backend Flow)

```
[ Scheduled Job chạy lúc 01:00 AM ] Hoặc [ Admin gọi POST /api/v1/certifications/check-expiry ]
                     │
                     ▼
       Lấy danh sách tất cả các Chứng nhận (Certifications)
                     │
                     ▼
       Lặp qua từng chứng nhận:
                     │
                     ├─► TH1: Hết hạn (expiryDate < Today)
                     │    │
                     │    ├─► Cập nhật trạng thái chứng nhận (hoặc tự tính toán hết hiệu lực)
                     │    ├─► Tạo Alert (CERT_EXPIRED, Severity: HIGH) nếu chưa tồn tại alert PENDING cùng loại
                     │    └─► Gửi Notification tới Admin (VT-01) & Quản lý HTX sở hữu (VT-02)
                     │
                     └─► TH2: Sắp hết hạn (0 < expiryDate - Today <= Threshold)
                          │
                          ├─► Tạo Alert (CERT_EXPIRING, Severity: MEDIUM) nếu chưa tồn tại alert PENDING cùng loại
                          └─► Gửi Notification tới Admin (VT-01) & Quản lý HTX sở hữu (VT-02)
```

### 3.1 Quy tắc ngăn chặn đính kèm chứng nhận hết hạn (QTN-13)
Tại logic của API `POST /api/v1/production-lots/{lotId}/certifications` (gắn chứng nhận vào lô):
1. Hệ thống lấy thông tin chứng nhận từ `CertificationRepository`.
2. Kiểm tra hạn dùng:
   ```java
   if (certification.getExpiryDate().isBefore(LocalDate.now())) {
       throw new BusinessException("Chứng nhận đã hết hạn hiệu lực, không thể gắn cho lô sản xuất mới.");
   }
   ```
3. Đồng thời, API lấy danh sách chứng nhận còn hiệu lực (`GET /api/v1/certifications/valid`) sẽ tự động loại trừ các chứng nhận có `expiryDate < LocalDate.now()`.

---

## 4. Đặc tả Cấu trúc DTO & Lớp Dữ liệu

### 4.1 Chi tiết Alert Details DTO cho Chứng nhận
```java
public class CertificationAlertDetails {
    private String certificationName;
    private String certificationCode;
    private String issuedBy;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private Long daysRemaining; // null nếu đã hết hạn
    private Long daysOverdue;   // null nếu chưa hết hạn
    private Integer thresholdConfigured;
}
```

### 4.2 Lớp Request / Response DTO cho Thông báo
```java
public class NotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
```

---

## 5. Danh sách các Test Cases kiểm thử nghiệp vụ

### 5.1 NCL-09-CN-005-TC-01: Quét chứng nhận sắp hết hiệu lực (Luồng thành công)
- ** Given:** Chứng nhận A có `expiryDate` cách ngày hiện tại 15 ngày, ngưỡng cấu hình cảnh báo là 30 ngày.
- **When:** Hệ thống chạy quét định kỳ hoặc gọi POST `/check-expiry`.
- **Then:** 
  - Tạo Alert với loại `CERT_EXPIRING`, độ nghiêm trọng `MEDIUM`.
  - Tạo Notification gửi tới hòm thư Quản lý HTX.
  - Phản hồi của chứng nhận A khi xem chi tiết vẫn có `isValid = true` nhưng có cảnh báo đính kèm.

### 5.2 NCL-09-CN-005-TC-02: Chứng nhận đã hết hạn
- **Given:** Chứng nhận B có `expiryDate` trước ngày hiện tại (đã hết hạn).
- **When:** Hệ thống quét thời hạn.
- **Then:**
  - Tạo Alert với loại `CERT_EXPIRED`, mức độ nghiêm trọng `HIGH`.
  - Tạo Notification gửi tới Quản lý HTX và Admin nền tảng.
  - Trạng thái `isValid` của chứng nhận B chuyển thành `false`.
  - Khi người dùng cố gắng gọi POST `/production-lots/{lotId}/certifications` để gắn chứng nhận B, hệ thống trả về lỗi `400 Bad Request` với thông điệp: *"Chứng nhận đã hết hạn hiệu lực, không thể gắn cho lô sản xuất mới."*

### 5.3 NCL-09-CN-005-TC-03: Nhận thông báo trong hộp thư
- **Given:** Cảnh báo hết hạn đã được hệ thống tạo và Notification tương ứng đã được lưu cho user có vai trò `VT-02`.
- **When:** User đăng nhập và gọi GET `/api/v1/notifications?isRead=false`.
- **Then:** Nhận được bản ghi thông báo mô tả chi tiết chứng nhận sắp hết hạn.
