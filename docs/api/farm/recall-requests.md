# API Docs – Yêu cầu thu hồi lô sản xuất (2 bước)

**Tên nhánh:** `feature/NCL-08-CN-008-two-step-recall`

**Tóm tắt:** Quy trình thu hồi lô sản xuất 2 bước:
1. Người ghi sự kiện (`VT-03`) tạo yêu cầu thu hồi.
2. Quản lý hợp tác xã (`VT-02`) duyệt hoặc từ chối. Duyệt sẽ chuyển
   `ProductionLot → RECALLED`, kéo theo toàn bộ `Shipment` và `TraceCode`
   chuyển sang `RECALLED`, bật cảnh báo công khai trên trang truy xuất và
   gửi thông báo tới các doanh nghiệp thu mua (người mua).

**Quy tắc QTN-22:** Người tạo yêu cầu **không được** tự duyệt yêu cầu của chính mình.

---

## Bảo mật (JWT + Roles)

- Toàn bộ endpoint yêu cầu `Authorization: Bearer <access_token>`.
- `VT-03` (Người ghi sự kiện) – tạo yêu cầu.
- `VT-02` (Quản lý hợp tác xã) – xem danh sách, chi tiết, duyệt, từ chối.
- Không thuộc vai trò yêu cầu → trả về `403`.

---

## 1. Tạo yêu cầu thu hồi

### Thông tin API

| Thuộc tính   | Giá trị                        |
| ------------ | ------------------------------ |
| **Method**   | `POST`                         |
| **Endpoint** | `/api/v1/recall-requests`      |
| **Quyền**    | `VT-03`                        |

### Request body

```json
{
  "lotId": "uuid",
  "reason": "Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép",
  "evidence": "Kết quả xét nghiệm mẫu NGS-2024-012"
}
```

| Trường    | Bắt buộc | Mô tả                        |
| --------- | -------- | ---------------------------- |
| `lotId`   | Có       | ID lô sản xuất (UUID).       |
| `reason`  | Có       | Lý do thu hồi (≤ 1000 ký tự).|
| `evidence`| Không    | Bằng chứng (≤ 2000 ký tự).   |

### Response `201 Created`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "uuid",
    "lotId": "uuid",
    "lotName": "Lô lúa vụ hè 2024",
    "requestedBy": { "userId": "uuid", "fullName": "Nguyễn Văn A" },
    "requestedAt": "2024-06-01T10:00:00",
    "status": "PENDING",
    "reason": "Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép",
    "evidence": "Kết quả xét nghiệm mẫu NGS-2024-012",
    "approvedBy": null,
    "approvedAt": null,
    "approvalRemarks": null,
    "rejectedBy": null,
    "rejectedAt": null,
    "rejectionReason": null,
    "notifiedBuyerCount": 0
  }
}
```

### Lỗi thường gặp

- `400` – Lô không tồn tại / lô đã thu hồi / lô không ở trạng thái `APPROVED`, `HARVESTED`, `PACKAGED` / đã có yêu cầu `PENDING` trùng.
- `403` – Không có quyền (không phải `VT-03`).
- `404` – Không tìm thấy lô sản xuất.

---

## 2. Lấy danh sách yêu cầu thu hồi

### Thông tin API

| Thuộc tính   | Giá trị                                  |
| ------------ | ---------------------------------------- |
| **Method**   | `GET`                                    |
| **Endpoint** | `/api/v1/recall-requests`                |
| **Quyền**    | `VT-02`                                  |

### Query Parameter

| Parameter | Bắt buộc | Giá trị                                          |
| --------- | -------- | ------------------------------------------------ |
| `status`  | Không    | `PENDING`, `APPROVED`, `REJECTED`                |
| `page`    | Không    | Trang (bắt đầu `0`, mặc định `0`)                |
| `size`    | Không    | Kích thước trang (mặc định `20`)                 |

### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "items": [
      {
        "id": "uuid",
        "lotId": "uuid",
        "lotName": "Lô lúa vụ hè 2024",
        "requestedBy": { "userId": "uuid", "fullName": "Nguyễn Văn A" },
        "requestedAt": "2024-06-01T10:00:00",
        "status": "PENDING",
        "reason": "Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép",
        "evidence": null,
        "approvedBy": null,
        "approvedAt": null,
        "approvalRemarks": null,
        "rejectedBy": null,
        "rejectedAt": null,
        "rejectionReason": null,
        "notifiedBuyerCount": 0
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

> Phân trang dùng cấu trúc `PageResponse` chuẩn của hệ thống.

### Lỗi thường gặp

- `400` – Trạng thái lọc không hợp lệ.
- `403` – Không có quyền (không phải `VT-02`).

---

## 3. Lấy chi tiết một yêu cầu

### Thông tin API

| Thuộc tính   | Giá trị                        |
| ------------ | ------------------------------ |
| **Method**   | `GET`                          |
| **Endpoint** | `/api/v1/recall-requests/{id}` |
| **Quyền**    | `VT-02`                        |

### Path Parameter

* `id`: UUID của yêu cầu thu hồi.

### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "uuid",
    "lotId": "uuid",
    "lotName": "Lô lúa vụ hè 2024",
    "requestedBy": { "userId": "uuid", "fullName": "Nguyễn Văn A" },
    "requestedAt": "2024-06-01T10:00:00",
    "status": "PENDING",
    "reason": "Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép",
    "evidence": "Kết quả xét nghiệm mẫu NGS-2024-012",
    "approvedBy": null,
    "approvedAt": null,
    "approvalRemarks": null,
    "rejectedBy": null,
    "rejectedAt": null,
    "rejectionReason": null,
    "notifiedBuyerCount": 0
  }
}
```

### Lỗi thường gặp

- `403` – Không có quyền.
- `404` – Không tìm thấy yêu cầu.

---

## 4. Duyệt yêu cầu thu hồi

### Thông tin API

| Thuộc tính   | Giá trị                                |
| ------------ | -------------------------------------- |
| **Method**   | `PUT`                                  |
| **Endpoint** | `/api/v1/recall-requests/{id}/approve` |
| **Quyền**    | `VT-02`                                |

### Request body (tùy chọn)

```json
{
  "remarks": "Đã kiểm tra và xác nhận lô cần thu hồi"
}
```

| Trường     | Bắt buộc | Mô tả                        |
| ---------- | -------- | ---------------------------- |
| `remarks`  | Không    | Ghi chú khi duyệt (tùy chọn).|

> Body có thể bỏ trống (`{}` hoặc không gửi body) để duyệt không kèm ghi chú.

### Hành động khi duyệt

- Yêu cầu phải ở trạng thái `PENDING`.
- Người duyệt phải **khác** người tạo (QTN-22). Vi phạm → `409`.
- Chuyển `ProductionLot → RECALLED`.
- Chuyển toàn bộ `Shipment` thuộc lô → `RECALLED`.
- Chuyển toàn bộ `TraceCode` của các shipment đó → `RECALLED`.
- Bật cảnh báo công khai trên trang truy xuất (nội dung = `reason` của yêu cầu).
- Gửi thông báo tới các doanh nghiệp thu mua (`VT-04` đã ghi sự kiện `PROCUREMENT`)
  + người dùng nội bộ có quyền `notification:READ`.
- Ghi audit log `APPROVE_RECALL_REQUEST`.

### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "uuid",
    "lotId": "uuid",
    "lotName": "Lô lúa vụ hè 2024",
    "requestedBy": { "userId": "uuid", "fullName": "Nguyễn Văn A" },
    "requestedAt": "2024-06-01T10:00:00",
    "status": "APPROVED",
    "reason": "Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép",
    "evidence": "Kết quả xét nghiệm mẫu NGS-2024-012",
    "approvedBy": { "userId": "uuid", "fullName": "Trần Thị B" },
    "approvedAt": "2024-06-02T09:30:00",
    "approvalRemarks": "Đã kiểm tra và xác nhận lô cần thu hồi",
    "rejectedBy": null,
    "rejectedAt": null,
    "rejectionReason": null,
    "notifiedBuyerCount": 3
  }
}
```

### Lỗi thường gặp

- `400` – Yêu cầu không ở trạng thái `PENDING`.
- `403` – Không có quyền.
- `404` – Không tìm thấy yêu cầu.
- `409` – Người duyệt trùng người tạo (QTN-22).

---

## 5. Từ chối yêu cầu thu hồi

### Thông tin API

| Thuộc tính   | Giá trị                               |
| ------------ | ------------------------------------- |
| **Method**   | `PUT`                                 |
| **Endpoint** | `/api/v1/recall-requests/{id}/reject` |
| **Quyền**    | `VT-02`                               |

### Request body

```json
{
  "rejectionReason": "Chưa đủ bằng chứng xác thực"
}
```

| Trường            | Bắt buộc | Mô tả                          |
| ----------------- | -------- | ------------------------------ |
| `rejectionReason` | Có       | Lý do từ chối (≤ 1000 ký tự).  |

### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "uuid",
    "lotId": "uuid",
    "lotName": "Lô lúa vụ hè 2024",
    "requestedBy": { "userId": "uuid", "fullName": "Nguyễn Văn A" },
    "requestedAt": "2024-06-01T10:00:00",
    "status": "REJECTED",
    "reason": "Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép",
    "evidence": "Kết quả xét nghiệm mẫu NGS-2024-012",
    "approvedBy": null,
    "approvedAt": null,
    "approvalRemarks": null,
    "rejectedBy": { "userId": "uuid", "fullName": "Trần Thị B" },
    "rejectedAt": "2024-06-02T09:45:00",
    "rejectionReason": "Chưa đủ bằng chứng xác thực",
    "notifiedBuyerCount": 0
  }
}
```

### Lỗi thường gặp

- `400` – Yêu cầu không ở trạng thái `PENDING` / thiếu `rejectionReason`.
- `403` – Không có quyền.
- `404` – Không tìm thấy yêu cầu.

---

## Ghi chú tích hợp

### Thông báo người mua

Khi duyệt, hệ thống:
1. Lấy tất cả `Shipment` của lô sản xuất bị thu hồi.
2. Truy vấn các user đã ghi sự kiện `PROCUREMENT` trên các shipment đó
   (qua `ChainEventRepository.findDistinctProcurementRecorderIdsByShipmentIds`).
3. Xác định tổ chức của các user đó và gửi thông báo tới **mọi user đang hoạt động**
   thuộc các tổ chức này (cộng thêm người dùng nội bộ có quyền `notification:READ`).

Tiêu đề thông báo: `Thông báo thu hồi lô sản xuất`.
Nội dung: `Lô sản xuất "<tên lô>" đã bị thu hồi. Lý do: <reason>`.

### Trang truy xuất công khai

Khi `Shipment.status == RECALLED`, `PublicTraceResponse.recallMessage` ưu tiên lấy
`reason` từ yêu cầu thu hồi lô sản xuất **đã duyệt** (`APPROVED`); fallback về lý do
từ bản ghi thu hồi cũ hoặc thông điệp mặc định.