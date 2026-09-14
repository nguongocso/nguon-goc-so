# API Docs — Trang cảnh báo tổng hợp (Aggregate Alerts)

*Mã User Story: NCL-08-CN-016 Trang cảnh báo tổng hợp cho quản trị viên và quản lý hợp tác xã*

---

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
| :--- | :--- | :--- | :--- |
| 2026-09-14 | v1.0.0 | Khởi tạo tài liệu đặc tả API gom 7 nguồn cảnh báo, thống kê mức độ khẩn cấp, bộ đếm cho thanh điều hướng và cơ chế tự đóng | Antigravity |

---

## 1. Thông tin chung

### 1.1 Mục tiêu
Hệ thống cung cấp giao diện và dịch vụ cảnh báo tập trung (Single Pane of Glass) giúp gom toàn bộ các cảnh báo đang mở từ **ít nhất 7 nguồn** khác nhau của tổ chức vào một nơi duy nhất. Giúp Quản lý hợp tác xã (`VT-02`) và Quản trị viên nền tảng (`VT-01`) không bỏ sót các việc cấp bách cần xử lý, với khả năng điều hướng thẳng tới màn hình xử lý qua lối tắt, cùng cơ chế tự đóng thông minh khi nguyên nhân đã được khắc phục.

### 1.2 Vai trò & Phân quyền (Role Access)
- **Quản lý hợp tác xã (`VT-02`):**
  - Xem danh sách cảnh báo tổng hợp đang mở thuộc tổ chức của mình.
  - Xem bộ đếm cảnh báo chưa xem / đang mở trên thanh điều hướng (`Header`).
  - Sử dụng các lối tắt trực tiếp để thực hiện xử lý hoặc khắc phục cảnh báo.
  - **Phạm vi dữ liệu:** Nghiêm ngặt tuân thủ **QTN-01** (chỉ thấy dữ liệu của tổ chức hiện tại).
- **Quản trị viên nền tảng (`VT-01`):**
  - Xem danh sách cảnh báo tổng hợp toàn nền tảng (toàn bộ các tổ chức).
  - Có thêm cột "Tổ chức" trên danh sách và bộ lọc theo từng tổ chức.
  - Sử dụng lối tắt để kiểm tra chi tiết sự cố tại tổ chức liên quan.
- **Các vai trò khác (`VT-03`, `VT-04`, `VT-05`, v.v.):** Không có quyền truy cập endpoint cảnh báo tổng hợp (trả về `403 Forbidden`).

### 1.3 Danh mục 7 nguồn cảnh báo và điều kiện tự đóng

| STT | Mã loại (`type`) | Tên hiển thị tiếng Việt | Thực thể liên quan (`relatedEntityType`) | Mức khẩn cấp (`severity`) | Màn hình lối tắt (`actionUrl`) | Điều kiện tự đóng (Auto-resolve) |
|:---:|:---|:---|:---|:---:|:---|:---|
| **1** | `SCAN_ANOMALY` | Tem quét bất thường | `TraceCode` / `Shipment` | `HIGH` / `MEDIUM` | `/alerts/scan-anomaly` | Khi cán bộ quản lý/admin xác minh hoặc xử lý mã tem (`status -> RESOLVED`). |
| **2** | `CERT_EXPIRING`<br>`CERT_EXPIRED` | Chứng nhận sắp hết hạn / đã hết hạn | `Certification` | `MEDIUM` (`EXPIRING`)<br>`HIGH` (`EXPIRED`) | `/certifications` | Khi chứng nhận được gia hạn với ngày hết hạn mới ở tương lai (`autoResolveExpiringAlert`). |
| **3** | `INSPECTION_EXPIRING`<br>`INSPECTION_EXPIRED` | Kết quả kiểm nghiệm sắp / đã hết hiệu lực | `ProductionLot` | `MEDIUM` (`EXPIRING`)<br>`HIGH` (`EXPIRED`) | `/production-lots/:id` | Khi lô có kết quả kiểm nghiệm mới đạt chuẩn còn hiệu lực (`autoResolveAllPendingInspectionAlerts`). |
| **4** | `UNPROCESSED_FEEDBACK` | Phản ánh chưa xử lý | `ProductFeedback` | `HIGH` (khi CRITICAL/WARNING)<br>`MEDIUM` (khi INFO) | `/product-feedbacks` | Khi phản ánh được xử lý và chuyển trạng thái sang `CLOSED` hoặc `ESCALATED_TO_RECALL`. |
| **5** | `CODE_RANGE_QUOTA` | Hạn mức dải mã sắp hết | `CodeRange` | `HIGH` ($\ge 100\%$ `EXHAUSTED`)<br>`MEDIUM` ($\ge 80\%$ `NEARLY_EXHAUSTED`) | `/code-range-supplements/create` | Khi tổ chức được admin duyệt cấp bổ sung hạn mức khiến tỷ lệ sử dụng giảm dưới $80\%$. |
| **6** | `OVERDUE_MILESTONE` | Mốc canh tác quá hạn | `MilestoneReminder` / `ProductionLot` | `HIGH` (quá hạn $\ge 7$ ngày)<br>`MEDIUM` (quá hạn $1 - 6$ ngày) | `/farm-logs/create?lotId=:lotId` | Khi người dùng nhập nhật ký canh tác cho mốc đó (`MilestoneReminder.status -> COMPLETED`). |
| **7** | `OPEN_RECALL_CASE` | Vụ việc thu hồi đang mở | `RecallCase` / `ProductionLot` | `HIGH` (Tất cả vụ việc thu hồi mở) | `/recall-cases/:id` | Khi mọi lô hàng được xử lý và vụ việc được đóng (`RecallCase.status -> CLOSED`). |

### 1.4 Quy tắc nghiệp vụ liên quan
- **QTN-01 (Cách ly dữ liệu giữa các tổ chức):** Người dùng thuộc tổ chức nào chỉ thấy dữ liệu cảnh báo của tổ chức đó. Cảnh báo của tổ chức khác bị chặn và từ chối truy cập. Quản trị viên `VT-01` xem được toàn bộ.
- **QTN-10 (Phát hiện quét bất thường):** Mã tem quét vượt ngưỡng tạo cảnh báo `SCAN_ANOMALY` để xác minh, không tự động khóa mã khi chưa có chỉ định.
- **QTN-13 (Chỉ gắn và hiển thị chứng nhận còn hiệu lực):** Cảnh báo chứng nhận hết hiệu lực nhắc nhở gia hạn kịp thời để không làm tắc nghẽn hoạt động cấp chứng nhận cho các lô mới.
- **QTN-21 (Lô phải có kết quả kiểm nghiệm đạt còn hiệu lực):** Cảnh báo kiểm nghiệm nhắc nhở Quản lý HTX thực hiện kiểm nghiệm bổ sung trước khi dán và kích hoạt tem.

---

## 2. Đặc tả các Endpoints

### 2.1 GET /api/v1/alerts/aggregate

**Description:** Lấy danh sách cảnh báo tổng hợp gom từ cả 7 nguồn dữ liệu, có hỗ trợ tìm kiếm, lọc theo loại, lọc theo mức khẩn cấp, lọc trạng thái, lọc theo khoảng thời gian và phân trang.

**Authentication:** Bắt buộc Bearer JWT Token.  
**Quyền truy cập (PreAuthorize):** `hasAnyRole('VT-01', 'VT-02')`.

#### Tham số truy vấn (Query Parameters)
| Tên tham số | Kiểu dữ liệu | Bắt buộc | Mô tả & Ràng buộc | Giá trị ví dụ |
|:---|:---|:---:|:---|:---|
| `type` | String | Không | Lọc theo loại cảnh báo. Các giá trị hợp lệ: `SCAN_ANOMALY`, `CERT_EXPIRING`, `CERT_EXPIRED`, `INSPECTION_EXPIRING`, `INSPECTION_EXPIRED`, `UNPROCESSED_FEEDBACK`, `CODE_RANGE_QUOTA`, `OVERDUE_MILESTONE`, `OPEN_RECALL_CASE`. | `"CERT_EXPIRING"` |
| `severity` | String | Không | Lọc theo mức khẩn cấp: `HIGH`, `MEDIUM`. | `"HIGH"` |
| `status` | String | Không | Trạng thái cảnh báo: `OPEN` (mặc định), `RESOLVED`, `ALL`. | `"OPEN"` |
| `organizationId` | UUID | Không | Lọc theo tổ chức. **Chỉ `VT-01` mới được chỉ định tham số này.** Với `VT-02`, hệ thống luôn ép dùng `organizationId` từ phiên đăng nhập theo QTN-01. | `"48398188-75c1-4b13-8fcb-cbb1b1cbe3e7"` |
| `keyword` | String | Không | Từ khóa tìm kiếm theo nội dung, tiêu đề hoặc đối tượng liên quan. | `"Bón thúc"` |
| `fromDate` | LocalDate | Không | Định dạng `YYYY-MM-DD`. Lọc từ ngày phát sinh cảnh báo. | `"2026-09-01"` |
| `toDate` | LocalDate | Không | Định dạng `YYYY-MM-DD`. Lọc đến ngày phát sinh cảnh báo. | `"2026-09-14"` |
| `page` | Integer | Không | Chỉ số trang (bắt đầu từ `0`), mặc định `0`. | `0` |
| `size` | Integer | Không | Số phần tử mỗi trang, mặc định `10`. | `10` |

#### Phản hồi thành công (HTTP 200 OK)
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy danh sách cảnh báo tổng hợp thành công",
  "data": {
    "items": [
      {
        "id": "e4b6c310-901b-4f9e-a89c-0971b3e8c001",
        "type": "CERT_EXPIRING",
        "typeName": "Chứng nhận sắp hết hạn",
        "severity": "MEDIUM",
        "title": "Chứng nhận VietGAP sắp hết hạn",
        "message": "Chứng nhận VietGAP số VG-2025-09 còn 5 ngày nữa sẽ hết hiệu lực (hết hạn ngày 19/09/2026).",
        "relatedEntityType": "CERTIFICATION",
        "relatedEntityId": "73c68a41-2b63-45c1-97b0-13f569b9f011",
        "relatedEntityName": "VietGAP Trồng trọt",
        "createdAt": "2026-09-14T08:30:00",
        "actionUrl": "/certifications",
        "organizationId": "48398188-75c1-4b13-8fcb-cbb1b1cbe3e7",
        "organizationName": "Hợp tác xã Nông nghiệp Xanh",
        "status": "OPEN"
      },
      {
        "id": "f8a12d34-712c-4912-9c12-3498bfe12345",
        "type": "OPEN_RECALL_CASE",
        "typeName": "Vụ việc thu hồi đang mở",
        "severity": "HIGH",
        "title": "Vụ việc thu hồi đang mở cho Lô bưởi da xanh",
        "message": "Vụ việc thu hồi RC-20260910-001 đang mở và cần xử lý dứt điểm các lô hàng liên quan.",
        "relatedEntityType": "RECALL_CASE",
        "relatedEntityId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "relatedEntityName": "RC-20260910-001",
        "createdAt": "2026-09-10T14:15:00",
        "actionUrl": "/recall-cases/a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "organizationId": "48398188-75c1-4b13-8fcb-cbb1b1cbe3e7",
        "organizationName": "Hợp tác xã Nông nghiệp Xanh",
        "status": "OPEN"
      }
    ],
    "totalElements": 2,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 10,
    "summaryCounts": {
      "totalOpen": 2,
      "highSeverityCount": 1,
      "mediumSeverityCount": 1,
      "byTypeCounts": {
        "SCAN_ANOMALY": 0,
        "CERT_EXPIRING": 1,
        "CERT_EXPIRED": 0,
        "INSPECTION_EXPIRING": 0,
        "INSPECTION_EXPIRED": 0,
        "UNPROCESSED_FEEDBACK": 0,
        "CODE_RANGE_QUOTA": 0,
        "OVERDUE_MILESTONE": 0,
        "OPEN_RECALL_CASE": 1
      }
    }
  },
  "errors": null,
  "path": "/api/v1/alerts/aggregate",
  "timestamp": "2026-09-14T09:30:00"
}
```

---

### 2.2 GET /api/v1/alerts/aggregate/counts

**Description:** Lấy nhanh số liệu thống kê tổng hợp số lượng cảnh báo đang mở phân theo mức khẩn cấp và theo 7 loại cảnh báo.

**Authentication:** Bắt buộc Bearer JWT Token.  
**Quyền truy cập (PreAuthorize):** `hasAnyRole('VT-01', 'VT-02')`.

#### Phản hồi thành công (HTTP 200 OK)
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy số liệu thống kê cảnh báo thành công",
  "data": {
    "totalOpen": 4,
    "highSeverityCount": 2,
    "mediumSeverityCount": 2,
    "byTypeCounts": {
      "SCAN_ANOMALY": 1,
      "CERT_EXPIRING": 1,
      "CERT_EXPIRED": 0,
      "INSPECTION_EXPIRING": 0,
      "INSPECTION_EXPIRED": 0,
      "UNPROCESSED_FEEDBACK": 1,
      "CODE_RANGE_QUOTA": 0,
      "OVERDUE_MILESTONE": 1,
      "OPEN_RECALL_CASE": 0
    }
  },
  "errors": null,
  "path": "/api/v1/alerts/aggregate/counts",
  "timestamp": "2026-09-14T09:30:00"
}
```

---

### 2.3 GET /api/v1/alerts/unviewed-count

**Description:** Endpoint siêu nhẹ (lightweight) dành riêng cho thanh điều hướng (`Header`), trả về số lượng cảnh báo đang mở cần xử lý của tổ chức người dùng đăng nhập để hiển thị huy hiệu (Badge) số đếm.

**Authentication:** Bắt buộc Bearer JWT Token.  
**Quyền truy cập (PreAuthorize):** `hasAnyRole('VT-01', 'VT-02')`.

#### Phản hồi thành công (HTTP 200 OK)
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy số cảnh báo chưa xử lý thành công",
  "data": {
    "unviewedCount": 4,
    "hasHighSeverity": true
  },
  "errors": null,
  "path": "/api/v1/alerts/unviewed-count",
  "timestamp": "2026-09-14T09:30:00"
}
```

---

## 3. Mã lỗi & Xử lý ngoại lệ

| HTTP Code | Error Message Tiếng Việt | Mô tả / Nguyên nhân |
|:---|:---|:---|
| `401 Unauthorized` | "Người dùng chưa đăng nhập hoặc phiên làm việc đã hết hạn." | Thiếu token hoặc token không hợp lệ. |
| `403 Forbidden` | "Bạn không có quyền xem cảnh báo tổng hợp." | Người dùng có vai trò không phải `VT-01` hoặc `VT-02`. |
| `403 Forbidden` | "Bạn không có quyền xem cảnh báo của tổ chức khác." | `VT-02` cố tình gửi `organizationId` khác với tổ chức của mình (**QTN-01**). |
| `400 Bad Request` | "Khoảng thời gian tìm kiếm không hợp lệ." | `fromDate` sau `toDate` hoặc định dạng ngày sai. |
| `500 Internal Server Error` | "Lỗi hệ thống khi tổng hợp cảnh báo." | Ngoại lệ máy chủ chưa được xử lý. |

---

## 4. Đặc tả kịch bản kiểm thử API (AC Mapping)

- **NCL-08-CN-016-TC-01 (Luồng thành công 4 loại cảnh báo mở):**
  - Given: Tổ chức có 4 loại cảnh báo đang mở (quét bất thường, chứng nhận sắp hết hạn, phản ánh chưa xử lý, mốc canh tác quá hạn).
  - When: Gọi `GET /api/v1/alerts/aggregate`.
  - Then: Trả về HTTP 200, danh sách chứa đủ 4 loại cảnh báo, nhóm theo mức khẩn cấp (`HIGH`, `MEDIUM`), kèm đường dẫn `actionUrl` tương ứng.
- **NCL-08-CN-016-TC-02 (Cảnh báo tự đóng khi gia hạn chứng nhận):**
  - Given: Đang có cảnh báo `CERT_EXPIRING` cho chứng nhận ID `X`.
  - When: Quản lý HTX gọi API cập nhật gia hạn chứng nhận `X` với ngày hết hạn mới.
  - Then: Cảnh báo `CERT_EXPIRING` của chứng nhận `X` tự động chuyển trạng thái sang `RESOLVED`. Khi gọi lại `GET /api/v1/alerts/aggregate?status=OPEN`, cảnh báo không còn xuất hiện.
- **NCL-08-CN-016-TC-03 (Cách ly dữ liệu QTN-01):**
  - Given: Cảnh báo thuộc Tổ chức B. Người dùng `VT-02` thuộc Tổ chức A đăng nhập.
  - When: Gọi `GET /api/v1/alerts/aggregate`.
  - Then: Hệ thống chỉ trả về cảnh báo của Tổ chức A, tuyệt đối không trả về cảnh báo của Tổ chức B.
- **NCL-08-CN-016-TC-04 (Dữ liệu rỗng khi không còn cảnh báo mở):**
  - Given: Tổ chức đã xử lý hết mọi cảnh báo.
  - When: Gọi `GET /api/v1/alerts/aggregate?status=OPEN`.
  - Then: Trả về HTTP 200 với `items: []`, `totalElements: 0`, `summaryCounts.totalOpen: 0` (hỗ trợ hiển thị Empty State trên UI).
