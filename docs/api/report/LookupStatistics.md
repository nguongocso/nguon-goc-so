# API Docs – Thống kê lượt tra cứu

**Tên nhánh:** `feature/lookup-statistics`

---

## 1. Tra cứu công khai mã truy xuất nguồn gốc (Quét mã QR)

API này cho phép người tiêu dùng thực hiện quét mã QR (tra cứu mã truy xuất) mà không cần đăng nhập. Khi API này được gọi, hệ thống sẽ thực hiện đồng thời:
1. Trả về thông tin chi tiết của mã truy xuất, lô hàng, lô sản xuất, nhật ký canh tác và lịch sử sự kiện của chuỗi cung ứng.
2. Tự động ghi lại một bản ghi nhật ký quét mã vào bảng `trace_code_scan_logs`.
3. Kiểm tra quy tắc phát hiện bất thường (QTN-10). Nếu phát hiện bất thường, đánh dấu bản ghi quét đó và gửi cảnh báo đến quản trị viên/hợp tác xã.

### Thông tin API

| Thuộc tính   | Giá trị                                                 |
|--------------|---------------------------------------------------------|
| **Method**   | `GET`                                                   |
| **Endpoint** | `/public/api/v1/trace-codes/{codeValue}`                |
| **Quyền**    | Public (Không cần đăng nhập)                            |

---

### Request

**Path Parameter:**

| Parameter   | Kiểu   | Bắt buộc | Mô tả                                      |
|-------------|--------|----------|--------------------------------------------|
| `codeValue` | String | Có       | Giá trị mã truy xuất cần tra cứu (VD: `NCL00000001`). |

**Query Parameters:**

| Parameter   | Kiểu   | Bắt buộc | Mặc định | Mô tả                                                               |
|-------------|--------|----------|----------|--------------------------------------------------------------------|
| `latitude`  | Double | Không    | Không    | Vĩ độ GPS của vị trí quét (nếu trình duyệt/app di động được cấp quyền GPS).|
| `longitude` | Double | Không    | Không    | Kinh độ GPS của vị trí quét (nếu trình duyệt/app di động được cấp quyền GPS).|
| `location`  | String | Không    | Không    | Tên vị trí (Ví dụ: `Hà Nội`, `Thái Nguyên`) do client xác định.     |

*Hệ thống sẽ tự động trích xuất `ip_address` từ request client và `user_agent` từ HTTP Header.*

---

### Response `200 OK` – Thành công

```json
{
  "success": true,
  "status": 200,
  "data": {
    "codeValue": "NCL00000001",
    "status": "ACTIVE",
    "activatedAt": "2026-07-28T09:00:00.000Z",
    "shipment": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Lô hàng Chè Tân Cương xuất khẩu số 1",
      "packagingInfo": "Hộp giấy 500g",
      "totalQuantity": 1000
    },
    "productionLot": {
      "id": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "name": "Lô Chè Tân Cương chất lượng cao 2026",
      "plantingDate": "2026-03-15",
      "harvestDate": "2026-07-10",
      "cropType": "Chè Tân Cương",
      "organization": {
        "id": "a9f8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4d",
        "name": "Hợp tác xã Chè Tân Cương Thái Nguyên"
      }
    },
    "farmLogs": [
      {
        "id": "95d91b0c-c3b8-4c1f-bcb0-2b86737d1407",
        "logDate": "2026-03-15",
        "activityType": "PLANTING",
        "description": "Xuống giống chè vụ xuân",
        "attachments": [
          {
            "id": "b1d91b0c-c3b8-4c1f-bcb0-2b86737d1401",
            "fileName": "chung_tu_xuong_giong.pdf"
          }
        ]
      }
    ],
    "chainEvents": [
      {
        "id": "75d91b0c-c3b8-4c1f-bcb0-2b86737d1408",
        "eventType": "PACKAGING",
        "eventDate": "2026-07-15T08:00:00.000Z",
        "description": "Đóng gói hoàn tất lô chè"
      }
    ]
  },
  "timestamp": "2026-07-29T08:00:00.000Z"
}
```

### Response `404 Not Found` – Không tìm thấy mã truy xuất

```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy thông tin mã truy xuất hoặc mã chưa được kích hoạt.",
  "path": "/public/api/v1/trace-codes/NCL99999999",
  "timestamp": "2026-07-29T08:05:00.000Z"
}
```

---

## 2. Lấy thống kê lượt tra cứu (Dành cho Quản lý HTX & Admin)

API này tổng hợp số lượt quét theo lô hàng/lô sản xuất, thời gian và vị trí địa lý.

### Thông tin API

| Thuộc tính   | Giá trị                                                 |
|--------------|---------------------------------------------------------|
| **Method**   | `GET`                                                   |
| **Endpoint** | `/api/v1/reports/lookup-statistics`                     |
| **Quyền**    | `VT-01`, `VT-02`                                        |

* `VT-01`: Quản trị viên hệ thống (Admin) - Có quyền xem thống kê của bất kỳ tổ chức nào.
* `VT-02`: Quản lý hợp tác xã - Chỉ được phép xem thống kê của tổ chức của mình.

---

### Request

**Query Parameters:**

| Parameter        | Kiểu   | Bắt buộc | Mặc định | Mô tả                                                                                           |
|------------------|--------|----------|----------|-------------------------------------------------------------------------------------------------|
| `startDate`      | Date   | Không    | Không    | Định dạng `yyyy-MM-dd`. Lọc lượt quét từ thời điểm này (00:00:00).                               |
| `endDate`        | Date   | Không    | Không    | Định dạng `yyyy-MM-dd`. Lọc lượt quét đến thời điểm này (23:59:59).                             |
| `productionLotId` | UUID   | Không    | Không    | Lọc theo Lô sản xuất.                                                                           |
| `shipmentId`     | UUID   | Không    | Không    | Lọc theo Lô hàng.                                                                               |
| `organizationId` | UUID   | Không    | Không    | ID của tổ chức cần lấy báo cáo. Nếu trống, hệ thống tự động dùng ID tổ chức của user đăng nhập. |
| `groupBy`        | String | Không    | `MONTH`  | Nhóm dữ liệu theo thời gian: `DAY` (ngày), `WEEK` (tuần), `MONTH` (tháng), `YEAR` (năm).        |

---

### Response `200 OK` – Thành công

```json
{
  "success": true,
  "status": 200,
  "data": {
    "summary": {
      "totalScans": 1520,
      "totalUniqueCodes": 450,
      "abnormalScansCount": 15
    },
    "byLocation": [
      {
        "location": "Hà Nội",
        "scanCount": 850
      },
      {
        "location": "TP. Hồ Chí Minh",
        "scanCount": 420
      },
      {
        "location": "Đà Nẵng",
        "scanCount": 150
      },
      {
        "location": "Khác/Không xác định",
        "scanCount": 100
      }
    ],
    "byProductionLot": [
      {
        "lotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
        "lotName": "Lô Chè Tân Cương chất lượng cao 2026",
        "scanCount": 1200,
        "abnormalScansCount": 10
      },
      {
        "lotId": "12d91b0c-c3b8-4c1f-bcb0-2b86737d1499",
        "lotName": "Lô Chè Ô Long xuất khẩu 2026",
        "scanCount": 320,
        "abnormalScansCount": 5
      }
    ],
    "timeSeries": [
      {
        "period": "2026-07-28",
        "scanCount": 520
      },
      {
        "period": "2026-07-29",
        "scanCount": 1000
      }
    ]
  },
  "timestamp": "2026-07-29T08:10:00.000Z"
}
```

### Response `200 OK` – Trường hợp chưa có dữ liệu quét (NCL-07-CN-004-TC-02)

```json
{
  "success": true,
  "status": 200,
  "data": {
    "summary": {
      "totalScans": 0,
      "totalUniqueCodes": 0,
      "abnormalScansCount": 0
    },
    "byLocation": [],
    "byProductionLot": [],
    "timeSeries": []
  },
  "timestamp": "2026-07-29T08:10:00.000Z"
}
```

### Response `403 Forbidden` – Từ chối truy cập tổ chức khác

```json
{
  "success": false,
  "status": 403,
  "message": "Từ chối truy cập: Bạn không có quyền xem thống kê của tổ chức này.",
  "path": "/api/v1/reports/lookup-statistics",
  "timestamp": "2026-07-29T08:11:00.000Z"
}
```

---

## 3. Danh sách các lượt quét bất thường (Dành cho Quản lý HTX & Admin)

API trả về danh sách các lượt quét bị đánh dấu bất thường để người quản trị kiểm tra và xử lý.

### Thông tin API

| Thuộc tính   | Giá trị                                                 |
|--------------|---------------------------------------------------------|
| **Method**   | `GET`                                                   |
| **Endpoint** | `/api/v1/reports/lookup-statistics/abnormal`            |
| **Quyền**    | `VT-01`, `VT-02`                                        |

---

### Request

**Query Parameters:**

| Parameter        | Kiểu   | Bắt buộc | Mặc định | Mô tả                                                                                           |
|------------------|--------|----------|----------|-------------------------------------------------------------------------------------------------|
| `startDate`      | Date   | Không    | Không    | Lọc theo ngày bắt đầu.                                                                          |
| `endDate`        | Date   | Không    | Không    | Lọc theo ngày kết thúc.                                                                         |
| `productionLotId` | UUID   | Không    | Không    | Lọc theo Lô sản xuất.                                                                           |
| `organizationId` | UUID   | Không    | Không    | Lọc theo ID tổ chức cần lấy báo cáo. Mặc định tự động lấy tổ chức của user hiện tại.            |
| `page`           | Integer| Không    | `0`      | Chỉ số trang hiển thị (0-indexed).                                                              |
| `size`           | Integer| Không    | `10`     | Số lượng bản ghi trên một trang.                                                                |

---

### Response `200 OK` – Thành công

```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "scanId": "35d91b0c-c3b8-4c1f-bcb0-2b86737d1409",
        "codeValue": "NCL00000001",
        "lotName": "Lô Chè Tân Cương chất lượng cao 2026",
        "scannedAt": "2026-07-29T07:45:00.000Z",
        "ipAddress": "171.244.10.15",
        "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X)",
        "location": "TP. Hồ Chí Minh",
        "latitude": 10.8231,
        "longitude": 106.6297,
        "reason": "Mã bị quét ở nhiều vị trí địa lý khác nhau trong thời gian ngắn (Thái Nguyên, TP. HCM trong vòng 30 phút)."
      },
      {
        "scanId": "45d91b0c-c3b8-4c1f-bcb0-2b86737d1410",
        "codeValue": "NCL00000001",
        "lotName": "Lô Chè Tân Cương chất lượng cao 2026",
        "scannedAt": "2026-07-29T07:40:00.000Z",
        "ipAddress": "1.53.16.88",
        "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
        "location": "Thái Nguyên",
        "latitude": 21.5925,
        "longitude": 105.8442,
        "reason": "Mã bị quét ở nhiều vị trí địa lý khác nhau trong thời gian ngắn (Thái Nguyên, TP. HCM trong vòng 30 phút)."
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 2,
      "totalPages": 1,
      "last": true
    }
  },
  "timestamp": "2026-07-29T08:15:00.000Z"
}
```

---

## 4. Quy tắc nghiệp vụ (Business Rules)

### 4.1 Quy tắc QTN-10 – Phát hiện quét bất thường

Hệ thống tự động phân tích và đánh dấu các lượt quét nghi ngờ là bất thường dựa trên 2 tiêu chí sau:

1. **Vượt ngưỡng tần suất quét (Scan Rate Limit):** 
   - Một mã truy xuất cụ thể (`TraceCode`) được quét quá **10 lần** trong vòng **24 giờ**.
   - Phân tích: Ngăn chặn hành vi giả mạo, sao chép tem hàng loạt để dán lên sản phẩm kém chất lượng.

2. **Bất thường khoảng cách địa lý (Impossible Travel):**
   - Một mã truy xuất cụ thể (`TraceCode`) được quét tại **2 vị trí địa lý khác nhau** (theo tỉnh/thành phố hoặc có khoảng cách xa hơn 100km) trong vòng **1 giờ**.
   - Phân tích: Một tem thật chỉ có thể ở một nơi tại một thời điểm. Việc quét ở hai nơi quá xa nhau trong thời gian ngắn chứng tỏ mã tem đã bị sao chép.

### 4.2 Hành động khi phát hiện bất thường

* **Không tự động khóa mã:** Theo quy định nghiệp vụ, hệ thống **không tự động khóa** mã truy xuất khi phát hiện bất thường để tránh ảnh hưởng đến người tiêu dùng mua hàng thật, mà chỉ đánh dấu `is_abnormal = 1` và lưu lý do.
* **Gửi cảnh báo:** Hệ thống tự động kích hoạt gửi cảnh báo/thông báo đến cho Quản trị viên hệ thống (`VT-01`) và Quản lý Hợp tác xã (`VT-02`) sở hữu lô hàng thông qua `NotificationService.sendAlert()`.

### 4.3 Phân quyền cách ly dữ liệu

* **Admin hệ thống (VT-01):** Có quyền xem báo cáo thống kê và danh sách bất thường của tất cả các tổ chức trên hệ thống.
* **Quản lý hợp tác xã (VT-02):** Chỉ được xem thống kê và danh sách bất thường thuộc về tổ chức của mình. Nếu truyền vào `organizationId` của tổ chức khác, hệ thống sẽ trả về lỗi `403 Forbidden`.

---

## 5. Thiết kế Cơ sở dữ liệu

### Bảng `trace_code_scan_logs` (Nhật ký quét mã truy xuất)

| Tên trường           | Kiểu dữ liệu   | Nullable | Khóa | Mô tả                                                                    |
|----------------------|----------------|----------|------|--------------------------------------------------------------------------|
| `id`                 | `CHAR(36)`     | NO       | PK   | Khóa chính (UUID tự sinh)                                                |
| `trace_code_id`      | `CHAR(36)`     | NO       | FK   | ID của mã truy xuất bị quét (Khóa ngoại đến `trace_codes.id`)             |
| `scanned_at`         | `DATETIME`     | NO       | –    | Thời điểm thực hiện quét                                                 |
| `ip_address`         | `VARCHAR(45)`  | YES      | –    | Địa chỉ IP của client thực hiện quét                                     |
| `user_agent`         | `VARCHAR(500)` | YES      | –    | Thông tin trình duyệt/thiết bị của client                                |
| `latitude`           | `DECIMAL(10,8)`| YES      | –    | Vĩ độ GPS từ thiết bị quét                                               |
| `longitude`          | `DECIMAL(11,8)`| YES      | –    | Kinh độ GPS từ thiết bị quét                                               |
| `location`           | `VARCHAR(255)` | YES      | –    | Địa danh hành chính (Ví dụ: "Hà Nội", "TP. Hồ Chí Minh")                 |
| `is_abnormal`        | `TINYINT(1)`   | NO       | –    | Trạng thái bất thường (1: Bất thường, 0: Bình thường)                    |
| `abnormal_reason`    | `VARCHAR(255)` | YES      | –    | Lý do đánh dấu bất thường (nếu có)                                       |

#### Mã SQL Migration (`V12__create_trace_code_scan_logs.sql`)

```sql
CREATE TABLE trace_code_scan_logs (
    id CHAR(36) NOT NULL,
    trace_code_id CHAR(36) NOT NULL,
    scanned_at DATETIME NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    location VARCHAR(255) NULL,
    is_abnormal TINYINT(1) NOT NULL DEFAULT 0,
    abnormal_reason VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_scan_logs_trace_code FOREIGN KEY (trace_code_id) REFERENCES trace_codes(id)
);

CREATE INDEX idx_scan_logs_trace_code ON trace_code_scan_logs(trace_code_id);
CREATE INDEX idx_scan_logs_scanned_at ON trace_code_scan_logs(scanned_at);
```

---

## 6. Tiêu chí nghiệm thu (Acceptance Criteria)

* **AC-01 (Ghi nhận quét mã):** Khi truy cập endpoint `/public/api/v1/trace-codes/{codeValue}`, hệ thống ghi nhận đúng thông tin lượt quét bao gồm IP, User-Agent, toạ độ GPS, thời gian quét vào bảng `trace_code_scan_logs`.
* **AC-02 (Thống kê lượt quét):** API `/api/v1/reports/lookup-statistics` tính toán đúng:
  - Tổng số lượt quét, số mã quét không trùng lặp và số lượt quét bất thường.
  - Phân tích số lượt quét theo từng vị trí (location).
  - Phân tích số lượt quét theo từng Lô sản xuất (`production_lot`).
  - Phân rã dữ liệu theo chuỗi thời gian (timeSeries) được nhóm theo `DAY`, `WEEK`, `MONTH`, `YEAR`.
* **AC-03 (Xử lý dữ liệu rỗng):** Nếu chưa có lượt quét nào, API thống kê trả về kết quả rỗng (các chỉ số bằng `0`, danh sách trống) thay vì gây lỗi.
* **AC-04 (Cách ly và phân quyền):** 
  - Chỉ cho phép `VT-01` (Admin) và `VT-02` (Quản lý HTX) truy cập các API thống kê và danh sách bất thường.
  - Người dùng thuộc tổ chức nào chỉ được xem thống kê thuộc tổ chức của mình (ngoại trừ Admin).
* **AC-05 (Quy tắc QTN-10 & Cảnh báo):** 
  - Khi một mã bị quét > 10 lần trong 24 giờ, hoặc quét ở 2 nơi cách xa nhau trong vòng 1 giờ, hệ thống ghi nhận `is_abnormal = 1` cùng lý do chính xác.
  - Hệ thống gọi `NotificationService.sendAlert()` để in log cảnh báo dạng `🚨 CẢNH BÁO: ...`.
  - Không tự động khoá mã hay đổi trạng thái `TraceCode.status` của mã đó.
