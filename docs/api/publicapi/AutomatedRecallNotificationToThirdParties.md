# Tài liệu API: Thông Báo Tự Động Tới Bên Thứ Ba Khi Lô Bị Thu Hồi (Automated Recall Notification To Third Parties)

> **User Story ID:** NCL-12-CN-006  
> **Epic:** NCL-12 — Cổng dữ liệu và hồ sơ theo lược đồ chuẩn  
> **Áp dụng quy tắc nghiệp vụ:** QTN-09 (Cảnh báo công khai lô thu hồi), QTN-20 (Kiểm soát khóa truy cập và hạn mức), QTN-24 (Phạm vi ảnh hưởng thu hồi)  
> **Trạng thái hợp đồng:** Nguồn sự thật hợp đồng API (Single Source of Truth) cho NCL-12-CN-006  

---

## 1. Tổng quan Nghiệp vụ (Business Overview)

### 1.1. Mục đích và đối tượng sử dụng
- **Mục đích:** Cung cấp cơ chế thông báo tự động (Outbound Webhook) và các API quản lý nhận thông báo cho **Doanh nghiệp thu mua / Bên thứ ba** tích hợp qua Cổng dữ liệu đối tác (`Partner Data Portal`). Khi một lô hàng chuyển sang trạng thái đang thu hồi (`RECALLING`) hoặc khi vụ việc thu hồi được đóng (`RECALLED`), hệ thống tự động phát tín hiệu tới các đối tác đã từng lấy dữ liệu của lô đó trong khoảng thời gian cấu hình, giúp đối tác lập tức dừng lưu thông/bán hàng mà không cần chờ liên hệ thủ công qua email hay điện thoại.
- **Đối tượng sử dụng:**
  - **Doanh nghiệp thu mua / Đối tác bên thứ ba:** Đăng ký địa chỉ nhận thông báo (Webhook URL), kiểm tra kết nối và theo dõi lịch sử thông báo đã nhận.
  - **Quản lý Hợp tác xã (VT-02) & Quản trị viên (VT-01):** Quản lý cấu hình địa chỉ nhận thông báo và tra cứu lịch sử gửi trên màn hình quản lý khóa API đối tác.

---

## 2. Quy tắc Nghiệp vụ & Kích hoạt Sự kiện (Business Rules & Event Triggers)

### 2.1. Thời điểm kích hoạt gửi thông báo (Triggers)
Hệ thống kích hoạt thông báo tự động tại 2 thời điểm:
1. **Khi lô chuyển sang đang thu hồi (`RECALLING`):** Kích hoạt ngay khi cấp có thẩm quyền phê duyệt yêu cầu thu hồi hàng loạt (`BulkRecallRequest` được `APPROVED` theo QTN-24).
2. **Khi vụ việc thu hồi được đóng (`RECALLED`):** Kích hoạt khi Quản lý Hợp tác xã hoàn tất đóng vụ việc thu hồi (`RecallCase` chuyển sang `CLOSED` theo QTN-27).

### 2.2. Điều kiện xác định phạm vi đối tác nhận (Scoping Rules)
Hệ thống chỉ gửi thông báo tới các đối tác thỏa mãn toàn bộ các điều kiện:
1. **Khóa API có hiệu lực:** Khóa `partner_api_keys` có `status = 'ACTIVE'` và chưa hết hạn (`expiresAt > NOW()`).
2. **Đã đăng ký địa chỉ nhận an toàn:** `webhook_url` không rỗng và bắt buộc sử dụng giao thức bảo mật `https://` (riêng môi trường local/test cho phép `http://localhost` hoặc `http://127.0.0.1`).
3. **Đã từng lấy dữ liệu của lô:** Đối tác đã từng gọi API lấy dữ liệu của lô sản xuất hoặc lô hàng (ghi nhận trong `partner_lot_access_logs`) trong khoảng thời gian cấu hình `T` ngày qua (mặc định 30 ngày từ `system_configurations`).
4. **Khóa thật (Live Key):** Chỉ gửi cho khóa thật (`is_test = false`), không gửi thông báo nghiệp vụ thật cho khóa thử nghiệm Sandbox (`is_test = true`).

### 2.3. Quy tắc ngoại lệ và chặn gửi (Exclusion Rules)
- **TC-03 (Chưa từng lấy dữ liệu):** Nếu đối tác chưa từng lấy dữ liệu của lô bị thu hồi trong khung thời gian cấu hình, hệ thống **tuyệt đối không gửi** thông báo tới đối tác đó.
- **TC-04 (Khóa đã bị thu hồi):** Nếu khóa truy cập của đối tác có trạng thái `REVOKED`, hệ thống **ngừng gửi thông báo ngay lập tức**, đồng thời hủy bỏ mọi lượt thử lại đang chờ (`CANCELLED`).
- **TC-02 (Đối tác không phản hồi & Lịch giãn dần):** Nếu gửi thất bại (timeout, HTTP 4xx, 5xx, lỗi mạng), hệ thống lưu nhật ký từng lần thử và lên lịch thử lại theo cơ chế Exponential Backoff giãn dần:
  - Lần 1: Ngay khi phát sinh sự kiện.
  - Lần 2: Sau 1 phút.
  - Lần 3: Sau 5 phút.
  - Lần 4: Sau 15 phút.
  - Lần 5: Sau 30 phút.
  - Quá 5 lần không thành công -> Đánh dấu trạng thái cuối cùng là `FAILED`.

---

## 3. Danh sách Endpoints

| Nhóm | Phương thức | Đường dẫn (Path) | Mô tả | Quyền / Xác thực |
|:---|:---:|:---|:---|:---|
| **Quản lý Webhook (Web UI)** | `PUT` | `/api/v1/organization/api-keys/{id}/webhook` | Đăng ký hoặc cập nhật địa chỉ nhận thông báo cho khóa API | `VT-01`, `VT-02` (JWT) |
| **Quản lý Webhook (Web UI)** | `POST` | `/api/v1/organization/api-keys/{id}/webhook/test-ping` | Bắn thử nghiệm webhook kiểm tra kết nối tới máy chủ đối tác | `VT-01`, `VT-02` (JWT) |
| **Lịch sử gửi (Web UI)** | `GET` | `/api/v1/organization/api-keys/{id}/notifications` | Lấy danh sách lịch sử các thông báo thu hồi đã gửi cho khóa | `VT-01`, `VT-02` (JWT) |
| **Cổng đối tác (Partner Portal)** | `PUT` | `/api/v1/partner/webhook` | Đối tác tự đăng ký/cập nhật địa chỉ nhận thông báo qua khóa API | Header `X-API-KEY` |
| **Cổng đối tác (Partner Portal)** | `GET` | `/api/v1/partner/notifications` | Đối tác tra cứu lịch sử các thông báo thu hồi đã gửi tới mình | Header `X-API-KEY` |
| **Outbound Webhook** | `POST` | `{webhookUrl}` | Hệ thống Nguồn Gốc Số gửi thông báo trạng thái thu hồi tới máy chủ đối tác | Chữ ký số `X-Webhook-Signature` |

---

## 4. Đặc tả Chi tiết Các Endpoints

### 4.1. Endpoint 1: Đăng ký / Cập nhật địa chỉ nhận thông báo (Web UI)

- **Method:** `PUT`
- **Path:** `/api/v1/organization/api-keys/{id}/webhook`
- **Xác thực:** Bearer Token JWT (Vai trò `VT-01` hoặc `VT-02`).
- **Mô tả:** Thiết lập địa chỉ nhận thông báo (Webhook URL) gắn với khóa API đối tác. Bắt buộc kiểm tra kết nối an toàn HTTPS.

#### Tham số đường dẫn (Path Variables)
| Tên | Kiểu | Bắt buộc | Mô tả |
|:---|:---|:---:|:---|
| `id` | UUID | Có | Định danh của khóa API đối tác (`PartnerApiKey`) |

#### Payload yêu cầu (Request Body)
```json
{
  "webhookUrl": "https://erp.doanhnghiepthuamua.com/api/v1/webhooks/agri-trace-recalls",
  "isActive": true
}
```

#### Ràng buộc dữ liệu (Validation Rules)
- `webhookUrl`: Không được để trống, độ dài tối đa 500 ký tự. Bắt buộc bắt đầu bằng `https://` (cho phép `http://localhost` hoặc `http://127.0.0.1` khi chạy môi trường dev/test). Nếu không đúng định dạng an toàn -> Phản hồi lỗi `400 Bad Request`.
- `isActive`: Boolean (tùy chọn, mặc định `true`).

#### Phản hồi thành công (HTTP 200 OK)
```json
{
  "success": true,
  "status": 200,
  "message": "Cập nhật địa chỉ nhận thông báo thành công",
  "data": {
    "id": "00000000-0000-0000-0000-000900000001",
    "partnerName": "Doanh Nghiệp Thu Mua Nông Sản Việt",
    "keyPrefix": "nks_live_a1b2c3d4",
    "webhookUrl": "https://erp.doanhnghiepthuamua.com/api/v1/webhooks/agri-trace-recalls",
    "isWebhookActive": true,
    "webhookSecret": "sec_wh_7f8e9d0a1b2c3d4e5f6a7b8c9d0e1f2a",
    "updatedAt": "2026-09-15T09:40:00"
  }
}
```

---

### 4.2. Endpoint 2: Gửi thử nghiệm kết nối Webhook (Test Ping)

- **Method:** `POST`
- **Path:** `/api/v1/organization/api-keys/{id}/webhook/test-ping`
- **Xác thực:** Bearer Token JWT (`VT-01`, `VT-02`).
- **Mô tả:** Gửi một sự kiện mẫu giả lập (`PING`) đến `webhookUrl` của khóa để kiểm tra máy chủ đối tác có tiếp nhận và phản hồi `2xx` thành công hay không.

#### Phản hồi thành công (HTTP 200 OK)
```json
{
  "success": true,
  "status": 200,
  "message": "Gửi thông báo thử nghiệm thành công (Mã phản hồi HTTP: 200 OK)",
  "data": {
    "targetUrl": "https://erp.doanhnghiepthuamua.com/api/v1/webhooks/agri-trace-recalls",
    "httpStatus": 200,
    "durationMs": 312,
    "isSuccess": true,
    "responseBody": "{\"status\":\"received\"}"
  }
}
```

---

### 4.3. Endpoint 3: Tra cứu lịch sử thông báo thu hồi đã gửi (Web UI)

- **Method:** `GET`
- **Path:** `/api/v1/organization/api-keys/{id}/notifications`
- **Xác thực:** Bearer Token JWT (`VT-01`, `VT-02`).
- **Mô tả:** Trả về danh sách phân trang các thông báo thu hồi đã gửi tới khóa API này, bao gồm trạng thái phân phối và chi tiết lịch sử từng lần gửi/thử lại.

#### Tham số truy vấn (Query Parameters)
| Tên | Kiểu | Mặc định | Mô tả |
|:---|:---|:---:|:---|
| `deliveryStatus` | String | null | Lọc theo trạng thái gửi (`SUCCESS`, `PENDING_RETRY`, `FAILED`, `CANCELLED`) |
| `page` | Integer | 0 | Chỉ số trang (0-indexed) |
| `size` | Integer | 10 | Kích thước trang |

#### Phản hồi thành công (HTTP 200 OK)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "id": "e3b0c442-98fc-1c14-9afb-4c7b88719f21",
        "partnerApiKeyId": "00000000-0000-0000-0000-000900000001",
        "shipmentId": "11111111-2222-3333-4444-555555555555",
        "lotCode": "SHIP-20260908-001",
        "eventType": "RECALL_STATUS_CHANGED",
        "newStatus": "RECALLING",
        "targetUrl": "https://erp.doanhnghiepthuamua.com/api/v1/webhooks/agri-trace-recalls",
        "deliveryStatus": "SUCCESS",
        "attemptCount": 1,
        "maxAttempts": 5,
        "nextRetryAt": null,
        "lastHttpStatus": 200,
        "lastErrorMessage": null,
        "createdAt": "2026-09-15T09:30:00",
        "completedAt": "2026-09-15T09:30:01",
        "publicReason": "CẢNH BÁO: Lô hàng đang trong quá trình thu hồi do vượt ngưỡng tồn dư thuốc BVTV.",
        "attempts": [
          {
            "id": "a1a2a3a4-b1b2-c1c2-d1d2-e1e2e3e4e5e6",
            "attemptNumber": 1,
            "attemptedAt": "2026-09-15T09:30:00",
            "httpStatus": 200,
            "responseBody": "{\"received\": true}",
            "errorMessage": null,
            "durationMs": 245
          }
        ]
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### 4.4. Endpoint 4: Đối tác tự đăng ký địa chỉ nhận thông báo qua Cổng dữ liệu

- **Method:** `PUT`
- **Path:** `/api/v1/partner/webhook`
- **Xác thực:** Header `X-API-KEY: nks_live_...` (Kiểm soát hạn mức QTN-20).
- **Mô tả:** Cho phép hệ thống phần mềm của đối tác tự động cấu hình URL nhận thông báo qua kết nối an toàn.

#### Payload yêu cầu (Request Body)
```json
{
  "webhookUrl": "https://erp.doanhnghiepthuamua.com/api/v1/webhooks/agri-trace-recalls"
}
```

#### Phản hồi thành công (HTTP 200 OK)
```json
{
  "success": true,
  "status": 200,
  "message": "Đăng ký địa chỉ nhận thông báo thành công",
  "data": {
    "partnerName": "Doanh Nghiệp Thu Mua Nông Sản Việt",
    "webhookUrl": "https://erp.doanhnghiepthuamua.com/api/v1/webhooks/agri-trace-recalls",
    "isWebhookActive": true,
    "webhookSecret": "sec_wh_7f8e9d0a1b2c3d4e5f6a7b8c9d0e1f2a"
  }
}
```

---

### 4.5. Endpoint 5: Đối tác tra cứu lịch sử thông báo đã nhận qua Cổng dữ liệu

- **Method:** `GET`
- **Path:** `/api/v1/partner/notifications`
- **Xác thực:** Header `X-API-KEY: nks_live_...` (Tính vào 1 lượt gọi theo QTN-20).
- **Mô tả:** Trả về danh sách các thông báo thu hồi mà hệ thống Nguồn Gốc Số đã gửi tới địa chỉ webhook của khóa API đối tác hiện tại.

#### Phản hồi thành công (HTTP 200 OK)
Cấu trúc tương tự mục 4.3, dữ liệu được tự động cô lập theo `partnerApiKey` trong Header `X-API-KEY`.

---

## 5. Đặc tả Outbound Webhook (Hệ thống Nguồn Gốc Số -> Đối Tác)

### 5.1. Định dạng HTTP Request
* **Phương thức:** `POST`
* **Địa chỉ đích:** Giá trị `webhookUrl` đã đăng ký.
* **Tiêu đề (Headers):**
  - `Content-Type: application/json; charset=UTF-8`
  - `User-Agent: NguonGocSo-Webhook/1.0`
  - `X-Webhook-Event: RECALL_STATUS_CHANGED`
  - `X-Webhook-Delivery-Id: <UUID của lượt gửi>`
  - `X-Webhook-Timestamp: <Timestamp tính bằng giây Unix>`
  - `X-Webhook-Signature: t=<Timestamp>,v1=<HMAC-SHA256>`

### 5.2. Công thức sinh chữ ký số bảo mật (`X-Webhook-Signature`)
Để chống giả mạo và chống tấn công phát lại (replay attack), hệ thống ký dữ liệu bằng khóa bí mật `webhookSecret`:
```text
signed_payload = timestamp + "." + json_body
signature = HMAC_SHA256(signed_payload, webhookSecret)
Header Value: t={timestamp},v1={signature}
```

### 5.3. Cấu trúc Payload Webhook Thu hồi
```json
{
  "eventId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "eventType": "RECALL_STATUS_CHANGED",
  "shipmentId": "11111111-2222-3333-4444-555555555555",
  "shipmentCode": "SHIP-20260908-001",
  "productionLotId": "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
  "productionLotCode": "PL-2026-XUAN",
  "productName": "Xoài Cát Chu Cao Lãnh",
  "previousStatus": "ACTIVATED",
  "newStatus": "RECALLING",
  "timestamp": "2026-09-15T09:30:00Z",
  "publicReason": "CẢNH BÁO: Lô hàng đang trong quá trình thu hồi do phát hiện dư lượng thuốc BVTV vượt ngưỡng.",
  "remediationSummary": null
}
```

---

## 6. Bảng Mã Lỗi (Error Matrix)

| Mã HTTP | Tình huống lỗi | Thông báo chi tiết | Khắc phục |
|:---|:---|:---|:---|
| **400 Bad Request** | URL không an toàn | `Địa chỉ nhận thông báo phải sử dụng giao thức bảo mật HTTPS (https://)` | Chuyển sang địa chỉ có chứng chỉ SSL/TLS hợp lệ |
| **400 Bad Request** | URL không hợp lệ | `Định dạng URL địa chỉ nhận thông báo không hợp lệ` | Kiểm tra cú pháp URL |
| **401 Unauthorized** | Thiếu / sai API Key | `Khóa truy cập không hợp lệ hoặc thiếu Header X-API-KEY` | Cung cấp khóa API chính xác |
| **401 Unauthorized** | Khóa đã bị thu hồi (TC-04) | `Khóa truy cập đã bị thu hồi và không còn hiệu lực` | Sử dụng khóa mới còn hiệu lực |
| **403 Forbidden** | Không có quyền (Role) | `Bạn không có quyền quản lý khóa API của tổ chức` | Đăng nhập tài khoản quản lý HTX (VT-02) hoặc Quản trị viên (VT-01) |
| **404 Not Found** | Không tìm thấy khóa | `Không tìm thấy thông tin khóa truy cập đối tác` | Kiểm tra lại ID khóa |
| **429 Too Many Requests** | Vượt hạn mức giờ (QTN-20) | `Khóa truy cập đã vượt quá hạn mức {limit} lượt gọi/giờ` | Chờ sang khung giờ tiếp theo hoặc yêu cầu nâng hạn mức |
