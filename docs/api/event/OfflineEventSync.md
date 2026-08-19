# API Docs — Đồng bộ sự kiện ghi nhận khi ngoại tuyến (Offline Event Sync)

*Mã User Story: NCL-10-CN-05 Ghi sự kiện khi ngoại tuyến và đồng bộ sau*

---

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
| :--- | :--- | :--- | :--- |
| 2026-08-01 | v1.0.0 | Khởi tạo tài liệu đặc tả API đồng bộ sự kiện ngoại tuyến và kiểm tra trùng lặp | Antigravity |
| 2026-08-01 | v1.1.0 | Tối ưu hóa cơ sở dữ liệu: Gom nhóm lịch sử đồng bộ vào 1 bảng duy nhất `offline_sync_logs` | Antigravity |

---

## 1. Thông tin chung

**Mục tiêu**
Cho phép ứng dụng di động gửi danh sách các sự kiện được ghi nhận tạm thời trên thiết bị khi không có mạng (ngoại tuyến) lên máy chủ để đồng bộ vào dòng thời gian hệ thống khi có mạng trở lại.

Hệ thống sẽ thực hiện xử lý danh sách sự kiện theo lô (Batch Processing), xác thực quyền của người ghi đối với từng lô sản xuất/lô hàng, kiểm tra tính hợp lệ của trạng thái lô, xử lý các trường hợp ngoại lệ (như lô đã bị thu hồi), kiểm tra trùng lặp dựa trên khóa chống trùng (`offlineEventId` lưu ở bảng log đồng bộ duy nhất `offline_sync_logs`) và ghi nhận lịch sử xử lý.

---

## 2. API: Đồng bộ danh sách sự kiện ngoại tuyến

### 2.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `POST` |
| **Endpoint** | `/api/v1/chain-events/sync` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 2.2 Request Details

**Request Headers**
```http
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body Schema**

| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Body | `syncId` | UUID | Yes | ID duy nhất cho phiên đồng bộ này (do client tự sinh). | `"4a7b9c1d-8e2f-4a3b-b2c1-d0e9f8a7b6c5"` |
| Body | `events` | List\<Object\> | Yes | Danh sách chứa tối thiểu 1 sự kiện ghi nhận khi ngoại tuyến cần đồng bộ. | (Xem cấu trúc chi tiết bên dưới) |

#### Cấu trúc chi tiết của mỗi đối tượng sự kiện trong danh sách `events`
| Field Name | Data Type | Required | Constraints / Validation | Example |
| :--- | :--- | :--- | :--- | :--- |
| `offlineEventId` | UUID | Yes | ID duy nhất của sự kiện được tạo ở client (dùng làm khóa chống trùng - Idempotency Key). | `"9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"` |
| `productionLotId` | UUID | Yes | ID lô sản xuất hoặc lô hàng (Shipment ID) liên quan đến sự kiện. | `"85d91b0c-c3b8-4c1f-bcb0-2b86737d1406"` |
| `eventType` | String | Yes | Loại sự kiện trong chuỗi cung ứng. Hỗ trợ: `HARVEST`, `PACKAGING`, `TRANSPORT`, `PROCUREMENT`. | `"HARVEST"` |
| `recordedAt` | String (ISO LocalDateTime) | Yes | Định dạng: `YYYY-MM-DDTHH:mm:ss`. Không được là ngày ở tương lai so với thời điểm đồng bộ. | `"2026-08-01T08:30:00"` |
| `latitude` | Double | Yes | Vĩ độ GPS của thiết bị khi ghi nhận ngoại tuyến. | `20.985412` |
| `longitude` | Double | Yes | Kinh độ GPS của thiết bị khi ghi nhận ngoại tuyến. | `105.798541` |
| `images` | List\<String\> | Yes | Danh sách đường dẫn ảnh chụp thực địa (tối thiểu 1 ảnh). | `["https://storage.nguongocso.vn/events/offline_harvest_01.jpg"]` |
| `deviceSource` | String | No | Mặc định là `"MOBILE"`. | `"MOBILE"` |
| `eventData` | Map\<String, Object\> | Yes | Dữ liệu chi tiết của sự kiện theo từng `eventType` (Tương đương cấu trúc của API ghi nhận trực tuyến). | (Xem ví dụ bên dưới) |

---

### 2.3 Request Examples

#### Ví dụ Request Body (Đồng bộ lô gồm 2 sự kiện: 1 Thu hoạch và 1 Đóng gói)
```json
{
  "syncId": "4a7b9c1d-8e2f-4a3b-b2c1-d0e9f8a7b6c5",
  "events": [
    {
      "offlineEventId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "eventType": "HARVEST",
      "recordedAt": "2026-08-01T08:30:00",
      "latitude": 20.985412,
      "longitude": 105.798541,
      "images": [
        "https://storage.nguongocso.vn/events/offline_harvest_01.jpg"
      ],
      "deviceSource": "MOBILE",
      "eventData": {
        "quantity": 1500.0,
        "harvestDate": "2026-08-01"
      }
    },
    {
      "offlineEventId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "eventType": "PACKAGING",
      "recordedAt": "2026-08-01T09:45:00",
      "latitude": 20.985412,
      "longitude": 105.798541,
      "images": [
        "https://storage.nguongocso.vn/events/offline_packaging_01.jpg"
      ],
      "deviceSource": "MOBILE",
      "eventData": {
        "packagingSpecification": "Thùng carton 24 hộp 500g",
        "packagingDate": "2026-08-01"
      }
    }
  ]
}
```

---

### 2.4 Response — Success (`200 OK`)

| Status Code | Khi xảy ra |
| :--- | :--- |
| `200 OK` | Yêu cầu đồng bộ được máy chủ tiếp nhận và xử lý hoàn tất (kể cả trường hợp có sự kiện bị lỗi hoặc trùng lặp bên trong danh sách). |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "syncId": "4a7b9c1d-8e2f-4a3b-b2c1-d0e9f8a7b6c5",
    "totalEvents": 3,
    "successCount": 1,
    "duplicateCount": 1,
    "failedCount": 1,
    "results": [
      {
        "offlineEventId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
        "status": "SUCCESS",
        "eventId": "f5b6c7d8-e9a0-4b1c-8d2e-3f4a5b6c7d8e",
        "message": "Đồng bộ sự kiện thành công."
      },
      {
        "offlineEventId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "status": "DUPLICATE",
        "eventId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "message": "Sự kiện đã được đồng bộ trước đó. Hệ thống bỏ qua bản trùng."
      },
      {
        "offlineEventId": "7cb4a2d8-5f91-4cbe-8b29-2d18475c8e31",
        "status": "FAILED",
        "message": "Lô hàng đã bị thu hồi, không thể ghi nhận sự kiện."
      }
    ]
  },
  "timestamp": "2026-08-01T04:20:00Z"
}
```

---

### 2.5 Response — Error

| Status Code | Error Code (nếu có) | Nguyên nhân |
| :--- | :--- | :--- |
| `400 Bad Request` | `VALIDATION_ERROR` | Định dạng payload đồng bộ không hợp lệ, thiếu `syncId` hoặc danh sách `events` rỗng. |
| `401 Unauthorized` | - | Token xác thực không hợp lệ hoặc đã hết hạn. |
| `403 Forbidden` | `ACCESS_DENIED` | Người dùng không có quyền (không phải vai trò `VT-02` hoặc `VT-03`). |

#### Error Response Examples

##### Ví dụ 1: Lỗi định dạng Payload (Thiếu trường bắt buộc)
```json
{
  "success": false,
  "status": 400,
  "message": "Danh sách các sự kiện đồng bộ không được để trống.",
  "path": "/api/v1/chain-events/sync",
  "timestamp": "2026-08-01T04:21:15Z"
}
```

---

## 3. Quy tắc nghiệp vụ & Ràng buộc thiết kế (Business Rules)

### 3.1 Quy tắc chống trùng lặp (Deduplication Check - QTN-16 & NCL-10-CN-005-TC-02)
* Mỗi sự kiện gửi lên đồng bộ bắt buộc phải kèm theo khóa chống trùng `offlineEventId` do client sinh.
* Trước khi xử lý ghi nhận sự kiện, server truy vấn bảng `offline_sync_logs` để tìm xem đã tồn tại bản ghi nào có `offline_event_id` bằng giá trị `offlineEventId` truyền lên và có `status` khác `FAILED` (nghĩa là `SUCCESS` hoặc `DUPLICATE`) hay chưa.
* Nếu **đã tồn tại**:
  - Đánh dấu trạng thái sự kiện trong kết quả đồng bộ là `DUPLICATE`.
  - Bỏ qua, không thực hiện lưu lại sự kiện vào dòng thời gian timeline `chain_events` để đảm bảo tính toàn vẹn dữ liệu.
  - Ghi nhận trạng thái xử lý là `DUPLICATE` vào bảng `offline_sync_logs`.
* Nếu **chưa tồn tại**: Tiến hành các bước kiểm tra nghiệp vụ và ghi nhận mới sự kiện vào bảng timeline `chain_events`, sau đó lưu log trạng thái `SUCCESS` vào bảng `offline_sync_logs`.

### 3.2 Xử lý ngoại lệ lô bị thu hồi (Recall Protection - NCL-10-CN-005-TC-03)
* Đối với mỗi sự kiện cần đồng bộ, hệ thống kiểm tra thực thể liên quan (Lô sản xuất / Lô hàng):
  - Nếu là sự kiện vận chuyển hoặc thu mua liên quan đến lô hàng (`Shipment`): Kiểm tra trạng thái của `Shipment`. Nếu `status == ShipmentStatus.RECALLED` (Lô hàng đã bị thu hồi), hệ thống sẽ chặn ghi nhận, đánh dấu sự kiện là `FAILED`, và phản hồi lý do lỗi: `"Lô hàng đã bị thu hồi, không thể ghi nhận sự kiện."`
  - Đồng thời, thông tin thất bại này cũng được log lại trong bảng `failed_event_logs` tương tự như ghi nhận trực tuyến thông thường và lưu log trạng thái `FAILED` vào `offline_sync_logs`.

### 3.3 Quy trình giao dịch (Transaction Handling)
* Khi xử lý đồng bộ danh sách theo lô (Batch Sync), để tránh trường hợp một lỗi ở sự kiện này làm hủy bỏ toàn bộ phiên đồng bộ của các sự kiện hợp lệ khác, hệ thống áp dụng chiến lược **Partial Commit**:
  - Mỗi sự kiện trong danh sách `events` được xử lý trong một Database Transaction riêng biệt (sử dụng `@Transactional(propagation = Propagation.REQUIRES_NEW)`).
  - Một sự kiện bị thất bại (do lỗi nghiệp vụ hoặc lô bị thu hồi) sẽ rollback transaction của riêng sự kiện đó và ghi log lỗi. Các sự kiện hợp lệ khác vẫn được lưu trữ thành công.

### 3.4 Lưu nhật ký lịch sử đồng bộ (Sync History Logging - NCL-10-CN-005-TC-04)
* Mỗi yêu cầu đồng bộ (dù thành công hay thất bại từng phần) đều ghi nhận chi tiết kết quả xử lý của từng sự kiện vào bảng **`offline_sync_logs`** duy nhất.
* Bảng này chứa các thông tin: `id` (PK), `sync_id` (ID phiên đồng bộ chung), `user_id` (người thực hiện), `offline_event_id` (khóa chống trùng từ client), `production_lot_id`, `shipment_id`, `event_type`, `status` (`SUCCESS`, `DUPLICATE`, `FAILED`), `failure_reason`, và `synced_at`.
* Cách thiết kế này tối giản hóa số lượng bảng cần tạo mới nhưng vẫn bảo đảm đáp ứng đầy đủ yêu cầu audit trail và dễ dàng lấy báo cáo tổng hợp bằng các hàm Gom nhóm (Aggregate functions).

---

## 4. Các Endpoint liên quan (Related Endpoints)

* `POST /api/v1/chain-events/mobile` - Ghi nhận sự kiện trực tuyến từ thiết bị di động (khi có mạng).
* `GET /api/v1/failed-event-logs` - Truy vấn danh sách nhật ký sự kiện bị chặn (lỗi nghiệp vụ).
