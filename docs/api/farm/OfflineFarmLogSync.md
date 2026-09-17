# API Docs — Đồng bộ nhật ký canh tác ghi khi ngoại tuyến (Offline Farm Log Sync)

*Mã User Story: NCL-10-CN-012 Ghi nhật ký canh tác khi ngoại tuyến (MVP)*

---

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
| :--- | :--- | :--- | :--- |
| 2026-09-16 | v1.0.0 | Mở rộng `POST /chain-events/sync` với `eventType=FARM_LOG`, tái dùng `offline_sync_logs` chống trùng | Agent |
| 2026-09-17 | v2.0.0 | Offline trở thành chế độ của màn hình ghi nhật ký hiện có; gửi lần lượt từng bản ghi; ảnh nén gửi sau dữ liệu qua endpoint đính kèm; bản ghi lỗi giữ lại (dead-letter); kiểm tra quyền như ghi trực tuyến | Agent |

---

## 1. Thông tin chung

**Mục tiêu**
Cho phép Người ghi sự kiện ghi nhật ký canh tác khi không có mạng, lưu tạm trong IndexedDB ở trình duyệt, sau đó đồng bộ lên máy chủ khi có mạng trở lại.

**Nguyên tắc thiết kế (không tạo endpoint mới)**
- Tái dùng endpoint đồng bộ chung `POST /api/v1/chain-events/sync` (đã có từ NCL-10-CN-005).
- Chỉ mở rộng `ChainEventType` thêm giá trị `FARM_LOG` và nhánh xử lý trong `OfflineSyncEventProcessor`.
- Logic nghiệp vụ khi sync delegate về `FarmLogService.create()` nên kiểm tra quyền, trạng thái lô,
  tổ chức hoàn toàn giống ghi trực tuyến (`POST /api/v1/farm-logs`).
- **Kiểm tra quyền chi tiết**: trước khi tạo nhật ký, nhánh `FARM_LOG` gọi
  `PermissionChecker.check("FARM_LOG", "CREATE")` — cùng ma trận quyền với đường trực tuyến (QTN-07).
- Chống trùng bằng `offlineEventId` tra trong bảng `offline_sync_logs` (QTN-16).
- Mỗi sự kiện xử lý trong transaction riêng `REQUIRES_NEW` (partial commit): một bản ghi lỗi không
  làm hỏng cả batch. Client **gửi lần lượt từng bản ghi** nên mỗi request thường chỉ có 1 phần tử.
- Không lưu giờ tạo bản ghi trên thiết bị: `farm_logs.created_at` là giờ máy chủ tại thời điểm đồng
  bộ; ngày nghiệp vụ (`executed_date`) giữ đúng giá trị người dùng đã nhập.
- Ảnh không đi trong `images` của payload sync: client lưu blob trên thiết bị, **nén** rồi tải lên
  endpoint đính kèm (`POST /api/v1/farm-logs/{logId}/attachments`) **sau** khi nội dung đã được ghi.

---

## 2. API: Đồng bộ danh sách nhật ký ngoại tuyến

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

| Field Name | Data Type | Required | Constraints / Validation | Example |
| :--- | :--- | :--- | :--- | :--- |
| `syncId` | UUID | Yes | ID phiên đồng bộ do client sinh. | `"4a7b9c1d-8e2f-4a3b-b2c1-d0e9f8a7b6c5"` |
| `events` | List\<Object\> | Yes | Tối thiểu 1 sự kiện. | (xem dưới) |

**Cấu trúc một sự kiện `eventType=FARM_LOG`**

| Field Name | Data Type | Required | Constraints / Validation | Example |
| :--- | :--- | :--- | :--- | :--- |
| `offlineEventId` | UUID | Yes | Khóa chống trùng do client sinh (`crypto.randomUUID()`). | `"9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"` |
| `productionLotId` | UUID | Yes | Lô sản xuất ghi nhật ký. | `"85d91b0c-c3b8-4c1f-bcb0-2b86737d1406"` |
| `eventType` | String | Yes | Phải là `"FARM_LOG"`. | `"FARM_LOG"` |
| `recordedAt` | String (ISO LocalDateTime) | Yes | Thời điểm ghi trên thiết bị (giờ local dạng `YYYY-MM-DDTHH:mm:ss`). Máy chủ chỉ lưu giờ đồng bộ, không lưu giá trị này cho nhật ký. | `"2026-09-16T08:30:00"` |
| `latitude` | Double | No | Vĩ độ GPS (nếu có). | `20.985412` |
| `longitude` | Double | No | Kinh độ GPS (nếu có). | `105.798541` |
| `images` | List\<String\> | No | Với `FARM_LOG`: luôn để trống `[]`; ảnh gửi qua endpoint đính kèm sau khi ghi. | `[]` |
| `deviceSource` | String | No | Mặc định `"MOBILE"` / `"WEB"`. | `"WEB"` |
| `eventData` | Map\<String, Object\> | Yes | Khớp `CreateFarmLogRequest` (xem dưới). | (xem ví dụ) |

**Cấu trúc `eventData` cho `FARM_LOG`**

| Field Name | Data Type | Required | Constraints / Validation |
| :--- | :--- | :--- | :--- |
| `activityType` | Enum | Yes | Một trong `PLANTING/WATERING/FERTILIZING/PESTICIDE/WEEDING/HARVESTING/OTHER`. |
| `material` | String | No | Tối đa 255 ký tự. |
| `quantity` | Double | No | Phải > 0 nếu có. |
| `unit` | String | No | Tối đa 50 ký tự. |
| `executedDate` | Date | Yes | `YYYY-MM-DD`, không ở tương lai. |
| `notes` | String | No | Tối đa 1000 ký tự. |
| `milestoneId` | Long | No | ID mốc canh tác (nếu có, để tự đóng nhắc việc). |

---

### 2.3 Request Example

```json
{
  "syncId": "4a7b9c1d-8e2f-4a3b-b2c1-d0e9f8a7b6c5",
  "events": [
    {
      "offlineEventId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "eventType": "FARM_LOG",
      "recordedAt": "2026-09-16T08:30:00",
      "latitude": 20.985412,
      "longitude": 105.798541,
      "images": [],
      "deviceSource": "WEB",
      "eventData": {
        "activityType": "FERTILIZING",
        "material": "NPK 16-16-8",
        "quantity": 25.0,
        "unit": "kg",
        "executedDate": "2026-09-15",
        "notes": "Bón phân lần 1, ghi khi ngoại tuyến"
      }
    }
  ]
}
```

---

### 2.4 Response — Success (`200 OK`)

Cấu trúc giống API sync chung (`OfflineEventSyncResponse` bọc trong `ApiResult`).

```json
{
  "success": true,
  "status": 200,
  "data": {
    "syncId": "4a7b9c1d-8e2f-4a3b-b2c1-d0e9f8a7b6c5",
    "totalEvents": 1,
    "successCount": 1,
    "duplicateCount": 0,
    "failedCount": 0,
    "results": [
      {
        "offlineEventId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
        "status": "SUCCESS",
        "eventId": "a9fcbbac-fe01-4ecf-a97e-2d2c7b4ba5f1",
        "message": "Đồng bộ sự kiện thành công."
      }
    ]
  },
  "timestamp": "2026-09-16T04:20:00Z"
}
```

**Ý nghĩa từng `status` trong `results[]`:**

| Status | Khi xảy ra | Hành vi client |
| :--- | :--- | :--- |
| `SUCCESS` | Tạo `farm_logs` thành công, đã ghi `offline_sync_logs`. `eventId` là ID bản ghi `farm_logs`. | Xóa bản ghi khỏi IndexedDB. |
| `DUPLICATE` | `offlineEventId` đã có log `SUCCESS` trước đó (QTN-16). Gửi lại 5 lần vẫn 1 bản ghi. | Xóa bản ghi khỏi IndexedDB. |
| `FAILED` | Lỗi nghiệp vụ (sai quyền, sai lô, sai ngày...). `message` tiếng Việt. | Giữ lại, hiển thị lý do, cho thử lại/xóa tay. |

---

### 2.5 Response — Error

| Status Code | Nguyên nhân |
| :--- | :--- |
| `400 Bad Request` | Payload sai (thiếu `syncId`/`events` rỗng/thiếu `eventData`). |
| `401 Unauthorized` | Token hết hạn/không hợp lệ. |
| `403 Forbidden` | Không phải `VT-02`/`VT-03`. |

```json
{
  "success": false,
  "status": 400,
  "message": "Danh sách các sự kiện đồng bộ không được để trống.",
  "path": "/api/v1/chain-events/sync",
  "timestamp": "2026-09-16T04:21:15Z"
}
```

---

## 3. Quy tắc nghiệp vụ (tái dùng `FarmLogService.create`)

| Quy tắc | Hành vi khi sync | Message (`FAILED`) |
| :--- | :--- | :--- |
| QTN-07 Quyền ghi | Chỉ `VT-02`/`VT-03`. | `"Bạn không có quyền ghi nhật ký canh tác."` |
| QTN-01 Cách ly tổ chức | Người sync phải cùng tổ chức với lô. | `"Bạn không thuộc tổ chức của lô sản xuất."` |
| Trạng thái lô | Lô phải `APPROVED`/`HARVESTED`; lô đã hủy bị chặn. | `"Chỉ được ghi nhật ký cho lô đã duyệt hoặc đang thu hoạch."` / `"Lô sản xuất đã bị hủy, không thể thao tác nhật ký canh tác."` |
| QTN-25 Ngày thực hiện | `executedDate` bắt buộc, theo Bean Validation. | `"Vui lòng chọn ngày thực hiện"` |
| QTN-16 Chống trùng | Tra `offline_sync_logs.offline_event_id`. | `"Sự kiện đã được đồng bộ trước đó."` (`DUPLICATE`) |
| Audit | Mỗi bản ghi thành công publish `ActivityLogEvent` như ghi trực tuyến. | — |
| Nhắc mốc (NCL-03-CN-007) | Nếu `eventData.milestoneId` có hoặc khớp `activityType`, tự đóng nhắc việc. | — |
| Quyền chi tiết (QTN-07) | `PermissionChecker.check("FARM_LOG", "CREATE")` — giống `POST /farm-logs`. | `"Bạn không có quyền thực hiện chức năng này."` |
| Ngày thực hiện ở tương lai | `executedDate` > ngày hiện tại bị chặn (đồng bộ với validate client). | `"Ngày thực hiện không được ở tương lai."` |
| Lô không tồn tại | Trả lỗi nghiệp vụ, không tạo bản ghi. | `"Không tìm thấy lô sản xuất"` |

---

## 4. Ảnh đính kèm khi ngoại tuyến (2 pha)

1. **Pha 1 — dữ liệu**: client lưu ảnh dạng `Blob` trong IndexedDB (`tep-dinh-kem`), **nén
   client-side** (cạnh dài ≤ 1280 px, JPEG chất lượng giảm dần tới khi ≤ 5 MB) rồi gửi nội dung
   nhật ký qua `POST /chain-events/sync`.
2. **Pha 2 — ảnh**: sau khi nội dung đã ghi thành công (`SUCCESS`, có `eventId` = ID `farm_logs`),
   client tải từng ảnh lên `POST /api/v1/farm-logs/{logId}/attachments` (multipart, `file`).

Quy tắc:
- Ảnh lỗi **không** làm mất nhật ký: bản ghi chờ ở trạng thái `da-ghi` kèm `farmLogId` để lần sau chỉ
  tải ảnh, không gửi lại nội dung.
- Giới hạn: tối đa 5 tệp/nhật ký, mỗi tệp ≤ 5 MB, định dạng JPG/PNG/PDF (theo `AttachmentService`).
- Endpoint đính kèm yêu cầu quyền `FARM_LOG/UPDATE` (VT-02, VT-03 có mặc định).

---

## 5. Giới hạn và trạng thái phía client

**Trạng thái bản ghi chờ** (cửa hàng IndexedDB `nhat-ky-cho`):

| Trạng thái | Khi nào | Hành vi tiếp theo |
| :--- | :--- | :--- |
| `pending` | Vừa lưu tạm | Chờ mạng/backoff để gửi |
| `syncing` | Đang gửi | Quá 2 phút vẫn `syncing` (đóng tab) ⇒ tự thu hồi về `pending` |
| `da-ghi` | Nội dung đã lên máy chủ, còn ảnh chờ tải | Chỉ tải ảnh còn lại rồi xoá bản ghi |
| `failed` | Lỗi mạng | Tăng lượt thử, thử lại theo backoff 5s/15s/30s |
| `invalid` | Lỗi nghiệp vụ (sai quyền, lô đã hủy...) | **Giữ nguyên** kèm lý do; người dùng tự thử lại / xuất CSV / xoá |

**Giới hạn tải sẵn và hàng chờ:**
- Danh mục tải sẵn gồm: **danh sách lô** (APPROVED/HARVESTED), **danh mục vật tư đang hoạt động**,
  **loại hoạt động** — chi tiết `docs/NCL-10-CN-012/data-scope.md`.
- TTL **7 ngày** cho cả ba danh mục; chưa tải/hết hạn ⇒ chặn ghi ngoại tuyến mới (banner đỏ).
- Hàng chờ tối đa **100** bản ghi/thiết bị (bản ghi 101 bị chặn + cảnh báo, **không** xoá bản ghi cũ).
- Tự động thử lại tối đa **3 lần** cho lỗi mạng; hết lượt **không** xoá bản ghi.
- Không chống trùng theo nội dung: chỉ chống trùng theo `offlineEventId` (QTN-16).

---

## 6. Các Endpoint liên quan

- `POST /api/v1/farm-logs` — Ghi nhật ký trực tuyến (logic gốc được tái dùng).
- `POST /api/v1/chain-events/sync` — Endpoint sync chung (tài liệu này mở rộng thêm `FARM_LOG`).
- `POST /api/v1/farm-logs/{logId}/attachments` — Tải ảnh/đính kèm của nhật ký (pha 2 khi ngoại tuyến).
- `GET /api/v1/production-lots` — Nguồn danh sách lô tải sẵn.
- `GET /api/v1/input-materials?isActive=true` — Nguồn danh mục vật tư tải sẵn.
- `GET /api/v1/farm-logs?productionLotId=&page=&size=` — Kiểm tra kết quả sau sync.
- Màn hình ghi nhật ký (kiêm chế độ ngoại tuyến): `/farm-logs/create`.
