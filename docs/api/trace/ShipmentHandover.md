# API: Phiếu bàn giao lô hàng (Shipment Handover)

*NCL-05-CN-008 — Epic NCL-05: Quản lý chuỗi cung ứng*

## 1. Thông tin chung

**Mục tiêu**

Cho phép tổ chức giao (owner lô hàng) tạo phiếu bàn giao gửi sang tổ chức nhận. Tổ chức nhận xác nhận (accept) mới chuyển trách nhiệm; từ chối (reject), quá hạn (expired) hoặc bên giao hủy (cancel) thì trách nhiệm giữ nguyên ở bên giao.

**Phạm vi (NCL-05-CN-008):** Tạo phiếu + hủy phiếu. Xác nhận/từ chối thuộc NCL-05-CN-009.

## 2. Endpoints

| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/v1/shipment-handovers` | Tạo phiếu bàn giao |
| POST | `/api/v1/shipment-handovers/{id}/cancel` | Hủy phiếu (bên giao) |
| GET | `/api/v1/shipment-handovers/{id}` | Chi tiết phiếu |
| GET | `/api/v1/shipment-handovers/sent` | Danh sách phiếu đã gửi |
| GET | `/api/v1/shipment-handovers/received` | Danh sách phiếu đã nhận |
| GET | `/api/v1/shipments/{id}/remaining-handover-quantity` | Số kg còn lại có thể bàn giao |

## 3. POST /api/v1/shipment-handovers — Tạo phiếu

**Request Body**

```json
{
  "shipmentId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
  "toOrganizationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "quantity": 800,
  "plannedAt": "2026-09-10T08:00:00",
  "vehicleInfo": "29A-12345, xe tải 8 tấn",
  "carrierName": "Nguyễn Văn B",
  "note": "Bàn giao tại kho HTX"
}
```

| Trường | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| shipmentId | UUID | Có | Lô hàng nguồn, phải ở trạng thái ACTIVATED. |
| toOrganizationId | UUID | Có | Tổ chức nhận, phải khác tổ chức giao. |
| quantity | number | Có | Số lượng kg bàn giao, > 0 và ≤ remaining. |
| plannedAt | datetime | Không | Thời điểm dự kiến bàn giao. |
| vehicleInfo | string | Không | Phương tiện vận chuyển. |
| carrierName | string | Không | Người áp tải. |
| note | string | Không | Ghi chú. |

**Response thành công (200)**

```json
{
  "success": true,
  "status": 200,
  "message": "Thành công",
  "data": {
    "id": "f0e1d2c3-b4a5-6789-abcd-ef0123456789",
    "shipmentId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
    "shipmentName": "Lô hàng chè Long Cốc T7/2026",
    "fromOrganizationId": "org-a-uuid",
    "fromOrganizationName": "HTX Nông Nghiệp Xanh",
    "toOrganizationId": "org-b-uuid",
    "toOrganizationName": "Công Ty Trà Việt",
    "quantity": 800,
    "status": "PENDING_CONFIRMATION",
    "expiresAt": "2026-09-10T09:00:00",
    "createdAt": "2026-09-08T09:00:00"
  }
}
```

## 4. POST /{id}/cancel — Hủy phiếu (TC-04)

**Điều kiện:** Chỉ bên giao, chỉ khi PENDING_CONFIRMATION, bắt buộc lý do.

**Request Body**

```json
{
  "reason": "Không còn nhu cầu bàn giao"
}
```

**Response thành công (200)**

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "f0e1d2c3-b4a5-6789-abcd-ef0123456789",
    "status": "CANCELLED",
    "cancelReason": "Không còn nhu cầu bàn giao",
    "cancelledAt": "2026-09-08T10:00:00"
  }
}
```

## 5. GET endpoints

### GET /{id} — Chi tiết phiếu

Trả 403 nếu người gọi không thuộc sender/receiver org (TC-04 của NCL-05-CN-009).

### GET /sent — Phiếu đã gửi từ tổ chức hiện tại

### GET /received — Phiếu nhận đến tổ chức hiện tại

### GET /api/v1/shipments/{id}/remaining-handover-quantity

```json
{
  "success": true,
  "status": 200,
  "data": 500
}
```

`remaining = shipment.totalQuantity − Σ(quantity của phiếu PENDING_CONFIRMATION hoặc ACCEPTED)`

## 6. Điều kiện & Business Rules

**Người dùng phải:**
- Đăng nhập thành công, thuộc tổ chức giao (owner lô hàng).

**Lô hàng phải:**
- Trạng thái ACTIVATED (TC-01).
- KHÔNG ở trạng thái RECALLED (TC-03).
- Không có mã tem nào LOCKED.

**Ràng buộc số lượng:**
- `quantity ≤ totalQuantity − Σ(PENDING_CONFIRMATION + ACCEPTED)` (TC-02).
- Tổ chức nhận phải khác tổ chức giao.

## 7. Error Cases

**409 — vượt lượng còn lại của lô (TC-02)**

```json
{
  "success": false,
  "status": 409,
  "message": "Lô hàng chỉ còn 500 kg có thể bàn giao"
}
```

**409 — lô đang bị thu hồi (TC-03)**

```json
{
  "success": false,
  "status": 409,
  "message": "Lô hàng đang bị thu hồi, không thể tạo phiếu bàn giao"
}
```

**409 — có tem bị khóa**

```json
{
  "success": false,
  "status": 409,
  "message": "Lô hàng có tem bị khóa, không thể tạo phiếu bàn giao"
}
```

**403 — không thuộc tổ chức giao**

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền tạo phiếu bàn giao"
}
```

**400 — lý do hủy trống (TC-04)**

```json
{
  "success": false,
  "status": 400,
  "message": "Lý do hủy không được để trống"
}
```

## 8. Backend xử lý

```
POST /api/v1/shipment-handovers
    │
    ▼
Lấy currentUser (SecurityContext)
    │
    ▼
Tìm Shipment                        -> 404 nếu không có
    │
    ▼
Kiểm tra status = RECALLED          -> 409 nếu thu hồi (TC-03)
    │
    ▼
Kiểm tra tem LOCKED (TraceCodeRepository) -> 409 nếu có
    │
    ▼
Kiểm tra organization = owner       -> 403 nếu khác
    │
    ▼
Tìm toOrganization                  -> 404 nếu không có
    │
    ▼
Kiểm tra khác tổ chức giao          -> 400 nếu trùng
    │
    ▼
remaining = totalQuantity − Σ(PENDING + ACCEPTED)
    │
    ▼
quantity > remaining                -> 409 (TC-02)
    │
    ▼
Tạo phiếu PENDING_CONFIRMATION, expiresAt = now + app.handover.expiry-hours (48h)
    │
    ▼
Gửi notification cho tổ chức nhận
    │
    ▼
Trả HandoverResponse (200)
```

## 9. Cấu hình

| Key | Mặc định | Mô tả |
|---|---|---|
| `app.handover.expiry-hours` | 48 | Số giờ phiếu PENDING có hiệu lực trước khi hết hạn. |
| `app.handover.expiry-check-cron` | `0 0 * * * ?` | Cron cho `HandoverExpiryScheduler` quét phiếu quá hạn (EXPIRED). |

## 10. Database

Bảng `shipment_handovers` (migration `V20260908000000__create_shipment_handovers.sql`):

- PK: `id` CHAR(36)
- FK: `shipment_id` → shipments, `from_organization_id`/`to_organization_id` → organizations, `created_by`/`confirmed_by`/`rejected_by`/`cancelled_by` → users
- Status enum: PENDING_CONFIRMATION, ACCEPTED, REJECTED, EXPIRED, CANCELLED (transition chỉ từ PENDING_CONFIRMATION, không đảo chiều)
- Index: shipment_id, to_organization_id, from_organization_id, status, expires_at, (status, expires_at)

**Lưu ý:** `Shipment.organization` KHÔNG bị thay đổi khi tạo/accept phiếu — việc "chuyển trách nhiệm" là trạng thái logic trên phiếu bàn giao, không phải đổi owner vật lý của lô hàng.
