# API Docs – Yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007)

**Tóm tắt:** Khi hạn mức dải mã còn lại dưới ngưỡng cảnh báo (dưới 20%,
trạng thái `NEARLY_EXHAUSTED` của `CodeRangeService`) hoặc đã hết
(`EXHAUSTED`), Quản lý hợp tác xã (`VT-02`) tạo yêu cầu cấp bổ sung kèm số
lượng đề nghị, lý do và bằng chứng sản lượng thực (sự kiện thu hoạch/sơ chế).
Quản trị viên nền tảng (`VT-01`) duyệt toàn bộ / duyệt một phần / từ chối kèm
lý do. Khi duyệt, `totalLimit` của dải mã hiện có của tổ chức tăng ngay theo số
lượng thực cấp (không tạo dải mã mới vì `prefix` UNIQUE toàn hệ thống) và thông
báo được gửi cho người tạo + quản lý HTX của tổ chức.

**Quy tắc:**
- QTN-01: `VT-02` chỉ thao tác yêu cầu của tổ chức mình.
- Mỗi tổ chức chỉ được có tối đa **một** yêu cầu `PENDING` tại một thời điểm.

---

## Bảo mật (JWT + Roles)

- Toàn bộ endpoint yêu cầu `Authorization: Bearer <access_token>`.
- `VT-02` (Quản lý hợp tác xã) – tạo yêu cầu, xem yêu cầu của tổ chức mình.
- `VT-01` (Quản trị viên nền tảng) – xem tất cả, duyệt, từ chối.
- Không thuộc vai trò yêu cầu → trả về `403`.

---

## 1. Tạo yêu cầu cấp bổ sung

### Thông tin API

| Thuộc tính   | Giá trị                               |
| ------------ | ------------------------------------- |
| **Method**   | `POST`                                |
| **Endpoint** | `/api/v1/code-range-supplement-requests` |
| **Quyền**    | `VT-02`                               |

### Request body

```json
{
  "requestedQuantity": 500,
  "reason": "Vụ thu đông sản lượng cao, cần thêm tem truy xuất",
  "evidenceEventIds": ["uuid-su-kien-thu-hoach", "uuid-su-kien-so-che"]
}
```

| Trường             | Bắt buộc | Mô tả                                                        |
| ------------------ | -------- | ------------------------------------------------------------ |
| `requestedQuantity`| Có       | Số lượng đề nghị, số nguyên > 0.                             |
| `reason`           | Có       | Lý do đề nghị (≤ 1000 ký tự).                                |
| `evidenceEventIds` | Có       | ≥ 1 ID sự kiện thu hoạch (`HARVEST`) / sơ chế (`PREPROCESSING`) thuộc tổ chức. |

### Response `201 Created`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "uuid",
    "organizationId": "uuid",
    "organizationName": "HTX Nông Sản Xanh",
    "requestedBy": { "userId": "uuid", "fullName": "Nguyễn Văn A" },
    "requestedAt": "2026-09-07T10:00:00",
    "requestedQuantity": 500,
    "approvedQuantity": null,
    "status": "PENDING",
    "reason": "Vụ thu đông sản lượng cao, cần thêm tem truy xuất",
    "evidenceEventIds": ["uuid-su-kien-thu-hoach"],
    "approvedBy": null,
    "approvedAt": null,
    "approvalRemarks": null,
    "rejectedBy": null,
    "rejectedAt": null,
    "rejectionReason": null,
    "notifiedCount": 0
  }
}
```

### Lỗi thường gặp

- `400` – Tổ chức đã có yêu cầu đang chờ duyệt / tổ chức chưa được cấp dải mã / bằng chứng không hợp lệ (sai loại, của tổ chức khác, không tồn tại).
- `403` – Không có quyền (không phải `VT-02`).

---

## 2. Lấy danh sách yêu cầu của tổ chức mình

### Thông tin API

| Thuộc tính   | Giá trị                                        |
| ------------ | ---------------------------------------------- |
| **Method**   | `GET`                                          |
| **Endpoint** | `/api/v1/code-range-supplement-requests/my`    |
| **Quyền**    | `VT-02`                                        |

### Query Parameter

| Parameter | Bắt buộc | Giá trị                                          |
| --------- | -------- | ------------------------------------------------ |
| `status`  | Không    | `PENDING`, `APPROVED`, `REJECTED`                |
| `page`    | Không    | Trang (bắt đầu `0`, mặc định `0`)                |
| `size`    | Không    | Kích thước trang (mặc định `20`)                 |

### Response `200 OK` – `PageResponse` chuẩn của hệ thống (`items`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`).

---

## 2.1 Lấy danh sách sự kiện bằng chứng sản lượng thực

### Thông tin API

| Thuộc tính   | Giá trị                                                      |
| ------------ | ------------------------------------------------------------ |
| **Method**   | `GET`                                                        |
| **Endpoint** | `/api/v1/code-range-supplement-requests/evidence-events`     |
| **Quyền**    | `VT-02`                                                      |

### Mô tả

Trả về danh sách **phẳng** các sự kiện thu hoạch (`HARVEST`) / sơ chế
(`PREPROCESSING`) của tổ chức, sắp xếp mới nhất trước, tối đa 200 sự kiện.
FE hiển thị dạng checkbox để VT-02 tick chọn khi tạo yêu cầu — không cần đi
qua lô sản xuất → lô hàng.

Bao gồm cả sự kiện đã gắn lô hàng (tra tổ chức qua lô hàng) và sự kiện tự do
lưu `productionLotId` trong `eventData`.

### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "eventId": "uuid",
      "eventType": "HARVEST",
      "recordedAt": "2026-09-07T10:00:00",
      "recordedByName": "Nguyễn Văn A",
      "shipmentId": null,
      "productionLotId": "uuid",
      "productionLotName": "Lô chè Long Cốc T7/2026"
    }
  ]
}
```

### Lỗi thường gặp

- `403` – Không có quyền (không phải `VT-02`).

---

## 3. Lấy danh sách tất cả yêu cầu

### Thông tin API

| Thuộc tính   | Giá trị                                     |
| ------------ | ------------------------------------------- |
| **Method**   | `GET`                                       |
| **Endpoint** | `/api/v1/code-range-supplement-requests`    |
| **Quyền**    | `VT-01`                                     |

Query Parameter và Response giống mục 2.

### Lỗi thường gặp

- `400` – Trạng thái lọc không hợp lệ.
- `403` – Không có quyền (không phải `VT-01`).

---

## 4. Lấy chi tiết một yêu cầu

### Thông tin API

| Thuộc tính   | Giá trị                                          |
| ------------ | ------------------------------------------------ |
| **Method**   | `GET`                                            |
| **Endpoint** | `/api/v1/code-range-supplement-requests/{id}`    |
| **Quyền**    | `VT-01` (tất cả), `VT-02` (chỉ của tổ chức mình) |

### Lỗi thường gặp

- `400` – `VT-02` xem yêu cầu của tổ chức khác.
- `403` – Không có quyền.
- `404` – Không tìm thấy yêu cầu.

---

## 5. Duyệt yêu cầu (toàn bộ / một phần)

### Thông tin API

| Thuộc tính   | Giá trị                                                  |
| ------------ | -------------------------------------------------------- |
| **Method**   | `PUT`                                                    |
| **Endpoint** | `/api/v1/code-range-supplement-requests/{id}/approve`    |
| **Quyền**    | `VT-01`                                                  |

### Request body

```json
{
  "approvedQuantity": 300,
  "remarks": "Cấp trước 300 mã theo sản lượng đã đối chiếu"
}
```

| Trường             | Bắt buộc | Mô tả                                                        |
| ------------------ | -------- | ------------------------------------------------------------ |
| `approvedQuantity` | Có       | Số lượng thực cấp: số nguyên > 0, ≤ số lượng đề nghị (bằng = duyệt toàn bộ, nhỏ hơn = duyệt một phần). |
| `remarks`          | Không    | Ghi chú khi duyệt (≤ 2000 ký tự).                            |

### Hành động khi duyệt

- Yêu cầu phải ở trạng thái `PENDING`.
- `totalLimit` của dải mã mới nhất của tổ chức tăng thêm `approvedQuantity` (cùng transaction).
- Gửi thông báo cho người tạo yêu cầu + quản lý HTX (`VT-02`) của tổ chức (`notifiedCount`).
- Ghi audit log `APPROVE_CODE_RANGE_SUPPLEMENT`.

### Lỗi thường gặp

- `400` – Yêu cầu không ở trạng thái `PENDING` / số lượng thực cấp ≤ 0 hoặc vượt quá đề nghị / tổ chức chưa có dải mã.
- `403` – Không có quyền.
- `404` – Không tìm thấy yêu cầu.

---

## 6. Từ chối yêu cầu

### Thông tin API

| Thuộc tính   | Giá trị                                                 |
| ------------ | ------------------------------------------------------- |
| **Method**   | `PUT`                                                   |
| **Endpoint** | `/api/v1/code-range-supplement-requests/{id}/reject`    |
| **Quyền**    | `VT-01`                                                 |

### Request body

```json
{
  "rejectionReason": "Chưa đủ cơ sở sản lượng thực"
}
```

| Trường            | Bắt buộc | Mô tả                          |
| ----------------- | -------- | ------------------------------ |
| `rejectionReason` | Có       | Lý do từ chối (≤ 1000 ký tự).  |

Từ chối cũng gửi thông báo cho người tạo + quản lý HTX và ghi audit log `REJECT_CODE_RANGE_SUPPLEMENT`.

### Lỗi thường gặp

- `400` – Yêu cầu không ở trạng thái `PENDING` / thiếu `rejectionReason`.
- `403` – Không có quyền.
- `404` – Không tìm thấy yêu cầu.

---

## Ghi chú tích hợp (frontend)

- **Không có trang riêng** cho VT-02 tạo yêu cầu. Form tạo là dialog dùng chung
  `components/shipment/CodeRangeSupplementDialog.tsx`, được mở từ:
  - Tab **"Lô hàng & Mã QR"** của trang chi tiết lô sản xuất
    (`ShipmentList` — nút "Cấp bổ sung mã", chỉ `VT-02`).
  - Màn hình sinh mã (`CreateShipmentPage`): khi `remainingCount/totalLimit < 20%`
    (`NEARLY_EXHAUSTED`) hiện cảnh báo vàng, khi hết (`EXHAUSTED`) chặn sinh mã,
    và khi số lượng nhập vượt hạn mức còn lại hiện cảnh báo đỏ ngay dưới ô
    số lượng — cả ba đều có nút "Cấp bổ sung" (chỉ `VT-02`) mở dialog. CTA ẩn
    khi lô bị chặn tạo lô hàng (kiểm nghiệm/hủy/loại bỏ).
- Bằng chứng sản lượng thực: dialog gọi `GET .../evidence-events` lấy danh sách
  phẳng sự kiện thu hoạch/sơ chế của tổ chức, VT-02 chỉ cần tick chọn.
- Kết quả duyệt/từ chối hiển thị qua `NotificationBell` sẵn có (không cần UI riêng).
- Kịch bản kiểm thử: `docs/testing/NCL-04-CN-007_manual_test.md`.
