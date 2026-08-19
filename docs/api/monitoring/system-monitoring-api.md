# Changelog
- **2026-08-18 (v1.0.0):** Khởi tạo tài liệu API Giám sát tình trạng hệ thống trước buổi trình diễn (NCL-10-CN-010).

---

# API Giám sát Tình trạng Hệ thống (System Monitoring API)

Tài liệu này cung cấp chi tiết danh sách các API phục vụ chức năng **Giám sát tình trạng hệ thống thời gian thực trước buổi trình diễn** (Story **NCL-10-CN-010**). 

> [!IMPORTANT]
> **Quy định chung về bảo mật & phân quyền (QTN-01 & TC-03):**
> - Tất cả các API trong tài liệu này yêu cầu Header xác thực: `Authorization: Bearer <JWT_TOKEN>`.
> - Chỉ người dùng có vai trò **Quản trị viên nền tảng (VT-01 - PLATFORM_ADMIN)** mới được quyền gọi các API này.
> - Bất kỳ vai trò nào khác (ví dụ: `VT-02` - Quản lý hợp tác xã, `VT-03` - Người ghi sự kiện, `VT-04` - Doanh nghiệp thu mua, `VT-05` - Cán bộ quản lý) khi truy cập sẽ bị hệ thống từ chối ngay lập tức với mã HTTP `403 Forbidden` và tự động lưu nhật ký truy cập trái phép.

---

## 📊 Bảng Ngưỡng Cảnh Báo Hệ Thống (Threshold Specifications)

| Mã Chỉ Số | Tên Chỉ Số Giám Sát | Ngưỡng Cảnh Báo (Threshold) | Đơn Vị | Hành Động Khi Vượt Ngưỡng |
| :--- | :--- | :--- | :--- | :--- |
| **`DB_CONNECTION`** | Trạng thái kết nối Cơ sở dữ liệu | `UP` (Nối thành công) | Status Enum (`UP`/`DOWN`) | Chuyển trạng thái sang **CRITICAL** |
| **`SERVER_ERRORS`** | Số lỗi máy chủ (HTTP 5xx) trong 1 giờ | `> 20` lỗi / giờ | Lỗi | Chuyển trạng thái sang **WARNING** |
| **`PUBLIC_TRACE_LATENCY`**| Thời gian phản hồi TB trang tra cứu công khai | `> 2000` ms | Milliseconds (ms) | Chuyển trạng thái sang **WARNING** |
| **`DATA_GATEWAY_CALLS`** | Số lượt gọi Cổng dữ liệu / API tích hợp | `> 1000` lượt / giờ | Lượt gọi | Chuyển trạng thái sang **WARNING** |

---

## 🛠️ Danh Sách Endpoints

### 1. GET `/api/v1/admin/monitoring/system-status`

**Description:** Lấy thông tin tổng hợp tình trạng sức khỏe hệ thống thời gian thực trong 1 giờ gần nhất. API này tổng hợp 4 chỉ số trọng yếu, so sánh với ngưỡng cảnh báo và trả về trạng thái tổng thể (`HEALTHY`, `WARNING`, `CRITICAL`, `INSUFFICIENT_DATA`).

**Authentication:** Yêu cầu đăng nhập, Role bắt buộc: `VT-01` (Quản trị viên nền tảng).

**Request Parameters:** Không có.

---

### Response — Success (200 OK)

| Status Code | Conditions | Expected Outcome |
| :--- | :--- | :--- |
| **200 OK** | 4 chỉ số đều nằm trong ngưỡng an toàn | Status: `HEALTHY`, hiển thị đầy đủ 4 chỉ số ở mức bình thường (TC-01) |
| **200 OK** | 1 hoặc nhiều chỉ số vượt ngưỡng cảnh báo | Status: `WARNING`/`CRITICAL`, đổi màu cảnh báo & chỉ rõ chỉ số vượt ngưỡng (TC-02) |
| **200 OK** | Hệ thống mới khởi động lại / chưa có dữ liệu | Status: `INSUFFICIENT_DATA`, `hasSufficientData: false` (TC-04) |

---

### Response Examples (Mẫu Kết Quả Trả Về)

#### 🔹 Trường hợp 1: Luồng thành công — Hệ thống chạy bình thường (TC-01)

```json
{
  "success": true,
  "status": 200,
  "data": {
    "overallStatus": "HEALTHY",
    "hasSufficientData": true,
    "uptimeSeconds": 86400,
    "lastUpdated": "2026-08-18T08:54:00Z",
    "breachedMetricsCount": 0,
    "summaryMessage": "Hệ thống hoạt động bình thường và sẵn sàng cho buổi trình diễn.",
    "metrics": {
      "dbConnection": {
        "metricCode": "DB_CONNECTION",
        "metricName": "Trạng thái kết nối CSDL",
        "value": "UP",
        "numericValue": 1.0,
        "threshold": "UP",
        "status": "NORMAL",
        "unit": "STATUS",
        "message": "Kết nối CSDL MySQL ổn định."
      },
      "serverErrorCount": {
        "metricCode": "SERVER_ERRORS",
        "metricName": "Số lỗi máy chủ (1 giờ gần nhất)",
        "value": "3",
        "numericValue": 3.0,
        "threshold": "20.0",
        "status": "NORMAL",
        "unit": "lỗi/giờ",
        "message": "Số lỗi máy chủ nằm trong giới hạn an toàn (3/20)."
      },
      "publicTraceAvgResponseTime": {
        "metricCode": "PUBLIC_TRACE_LATENCY",
        "metricName": "Thời gian phản hồi TB tra cứu công khai",
        "value": "180 ms",
        "numericValue": 180.0,
        "threshold": "2000.0",
        "status": "NORMAL",
        "unit": "ms",
        "message": "Thời gian phản hồi trang tra cứu nhanh (180 ms)."
      },
      "dataGatewayCallCount": {
        "metricCode": "DATA_GATEWAY_CALLS",
        "metricName": "Số lượt gọi Cổng dữ liệu (1 giờ)",
        "value": "120",
        "numericValue": 120.0,
        "threshold": "1000.0",
        "status": "NORMAL",
        "unit": "lượt/giờ",
        "message": "Lượt gọi Cổng dữ liệu bình thường (120/1000)."
      }
    }
  },
  "timestamp": "2026-08-18T08:54:00Z"
}
```

---

#### 🔹 Trường hợp 2: Ngoại lệ — Số lỗi máy chủ vượt ngưỡng cảnh báo (TC-02)

*(Dữ liệu thử nghiệm: 25 lỗi máy chủ HTTP 5xx xuất hiện trong 1 giờ gần nhất)*

```json
{
  "success": true,
  "status": 200,
  "data": {
    "overallStatus": "WARNING",
    "hasSufficientData": true,
    "uptimeSeconds": 14400,
    "lastUpdated": "2026-08-18T08:54:00Z",
    "breachedMetricsCount": 1,
    "summaryMessage": "CẢNH BÁO: Phát hiện 1 chỉ số vượt ngưỡng cho phép trước buổi trình diễn!",
    "metrics": {
      "dbConnection": {
        "metricCode": "DB_CONNECTION",
        "metricName": "Trạng thái kết nối CSDL",
        "value": "UP",
        "numericValue": 1.0,
        "threshold": "UP",
        "status": "NORMAL",
        "unit": "STATUS",
        "message": "Kết nối CSDL MySQL ổn định."
      },
      "serverErrorCount": {
        "metricCode": "SERVER_ERRORS",
        "metricName": "Số lỗi máy chủ (1 giờ gần nhất)",
        "value": "25",
        "numericValue": 25.0,
        "threshold": "20.0",
        "status": "WARNING",
        "unit": "lỗi/giờ",
        "message": "Số lỗi máy chủ trong 1 giờ qua (25 lỗi) đã vượt ngưỡng cảnh báo quy định (20 lỗi)."
      },
      "publicTraceAvgResponseTime": {
        "metricCode": "PUBLIC_TRACE_LATENCY",
        "metricName": "Thời gian phản hồi TB tra cứu công khai",
        "value": "210 ms",
        "numericValue": 210.0,
        "threshold": "2000.0",
        "status": "NORMAL",
        "unit": "ms",
        "message": "Thời gian phản hồi nằm trong giới hạn an toàn."
      },
      "dataGatewayCallCount": {
        "metricCode": "DATA_GATEWAY_CALLS",
        "metricName": "Số lượt gọi Cổng dữ liệu (1 giờ)",
        "value": "450",
        "numericValue": 450.0,
        "threshold": "1000.0",
        "status": "NORMAL",
        "unit": "lượt/giờ",
        "message": "Lượt gọi Cổng dữ liệu bình thường."
      }
    }
  },
  "timestamp": "2026-08-18T08:54:00Z"
}
```

---

#### 🔹 Trường hợp 3: Chưa đủ số liệu — Khởi động lại hệ thống (TC-04)

*(Dữ liệu thử nghiệm: Máy chủ vừa restart < 1 phút hoặc chưa thu thập đủ mẫu dữ liệu)*

```json
{
  "success": true,
  "status": 200,
  "data": {
    "overallStatus": "INSUFFICIENT_DATA",
    "hasSufficientData": false,
    "uptimeSeconds": 45,
    "lastUpdated": "2026-08-18T08:54:00Z",
    "breachedMetricsCount": 0,
    "summaryMessage": "Hệ thống vừa khởi động lại, đang thu thập số liệu giám sát...",
    "metrics": {
      "dbConnection": {
        "metricCode": "DB_CONNECTION",
        "metricName": "Trạng thái kết nối CSDL",
        "value": "UP",
        "numericValue": 1.0,
        "threshold": "UP",
        "status": "NORMAL",
        "unit": "STATUS",
        "message": "Kết nối CSDL MySQL ổn định."
      },
      "serverErrorCount": {
        "metricCode": "SERVER_ERRORS",
        "metricName": "Số lỗi máy chủ (1 giờ gần nhất)",
        "value": "N/A",
        "numericValue": null,
        "threshold": "20.0",
        "status": "INSUFFICIENT_DATA",
        "unit": "lỗi/giờ",
        "message": "Chưa đủ số liệu quan sát trong 1 giờ gần nhất."
      },
      "publicTraceAvgResponseTime": {
        "metricCode": "PUBLIC_TRACE_LATENCY",
        "metricName": "Thời gian phản hồi TB tra cứu công khai",
        "value": "N/A",
        "numericValue": null,
        "threshold": "2000.0",
        "status": "INSUFFICIENT_DATA",
        "unit": "ms",
        "message": "Chưa đủ lượt truy cập để tính thời gian phản hồi trung bình."
      },
      "dataGatewayCallCount": {
        "metricCode": "DATA_GATEWAY_CALLS",
        "metricName": "Số lượt gọi Cổng dữ liệu (1 giờ)",
        "value": "N/A",
        "numericValue": null,
        "threshold": "1000.0",
        "status": "INSUFFICIENT_DATA",
        "unit": "lượt/giờ",
        "message": "Chưa đủ số liệu lượt gọi Cổng dữ liệu."
      }
    }
  },
  "timestamp": "2026-08-18T08:54:00Z"
}
```

---

### Response — Error (Lỗi & Phân quyền)

| Status Code | Cause | Exception / Standard Message |
| :--- | :--- | :--- |
| **401 Unauthorized** | Chưa truyền JWT Token hoặc Token không hợp lệ / đã hết hạn | Token không hợp lệ hoặc thiếu thông tin xác thực. |
| **403 Forbidden** | Tài khoản thuộc vai trò Quản lý HTX (`VT-02`) hoặc vai trò khác không phải `VT-01` (TC-03) | Bạn không có quyền truy cập thông tin giám sát hệ thống. |

#### 🔹 Ví dụ Response lỗi 403 Forbidden (TC-03)

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện chức năng này (Yêu cầu vai trò Quản trị viên nền tảng - VT-01)",
  "errors": "ACCESS_DENIED",
  "path": "/api/v1/admin/monitoring/system-status",
  "timestamp": "2026-08-18T08:54:00Z"
}
```

---

### 2. GET `/api/v1/admin/monitoring/thresholds`

**Description:** Lấy danh sách thông tin cài đặt ngưỡng cảnh báo hiện tại của hệ thống.

**Authentication:** Yêu cầu đăng nhập, Role bắt buộc: `VT-01`.

**Request Parameters:** Không có.

**Response Example (200 OK):**

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "metricCode": "DB_CONNECTION",
      "metricName": "Trạng thái kết nối CSDL",
      "thresholdValue": "UP",
      "unit": "STATUS",
      "description": "Yêu cầu kết nối CSDL phải sẵn sàng (UP)"
    },
    {
      "metricCode": "SERVER_ERRORS",
      "metricName": "Số lỗi máy chủ (1 giờ gần nhất)",
      "thresholdValue": "20.0",
      "unit": "lỗi/giờ",
      "description": "Tối đa 20 lỗi 5xx trong 60 phút"
    },
    {
      "metricCode": "PUBLIC_TRACE_LATENCY",
      "metricName": "Thời gian phản hồi TB tra cứu công khai",
      "thresholdValue": "2000.0",
      "unit": "ms",
      "description": "Tối đa 2000ms latency trung bình"
    },
    {
      "metricCode": "DATA_GATEWAY_CALLS",
      "metricName": "Số lượt gọi Cổng dữ liệu (1 giờ)",
      "thresholdValue": "1000.0",
      "unit": "lượt/giờ",
      "description": "Tối đa 1000 lượt gọi vào Cổng dữ liệu trong 60 phút"
    }
  ],
  "timestamp": "2026-08-18T08:54:00Z"
}
```
