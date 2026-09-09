# API: Phiếu bàn giao lô hàng (Shipment Handover)

*NCL-05-CN-008 — Epic NCL-05: Quản lý chuỗi cung ứng*

## 1. Thông tin chung

**Mục tiêu**

Cho phép tổ chức giao (owner lô hàng) tạo phiếu bàn giao gửi sang tổ chức nhận. Tổ chức nhận xác nhận (accept) mới chuyển trách nhiệm; từ chối (reject), quá hạn (expired) hoặc bên giao hủy (cancel) thì trách nhiệm giữ nguyên ở bên giao.

**Phạm vi (NCL-05-CN-008):** Tạo phiếu + hủy phiếu. Xác nhận/từ chối thuộc NCL-05-CN-009.

## 2. Endpoints

| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/v1/shipment-handovers/attachment` | Upload chứng từ giao hàng (multipart) |
| POST | `/api/v1/shipment-handovers` | Tạo phiếu bàn giao |
| POST | `/api/v1/shipment-handovers/{id}/cancel` | Hủy phiếu (bên giao) |
| GET | `/api/v1/shipment-handovers/{id}` | Chi tiết phiếu |
| GET | `/api/v1/shipment-handovers/sent` | Danh sách phiếu đã gửi |
| GET | `/api/v1/shipment-handovers/received` | Danh sách phiếu đã nhận |
| GET | `/api/v1/shipments/{id}/remaining-handover-quantity` | Số kg còn lại có thể bàn giao |
| GET | `/api/v1/shipments/{id}/has-pending-handover` | `true` khi lô có phiếu PENDING_CONFIRMATION (nhãn "Đang bàn giao" trên UI) |
| GET | `/api/v1/organizations/recipient-organizations` | Danh sách tổ chức ACTIVE trừ tổ chức hiện tại (cho dropdown Tổ chức nhận, dùng được bởi VT-02) |

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
  "note": "Bàn giao tại kho HTX",
  "attachmentPath": "handover-docs/2026/phieu-xuat-kho-123.pdf"
}
```

| Trường | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| shipmentId | UUID | Có | Lô hàng nguồn, backend bắt buộc trạng thái ACTIVATED (400 nếu DRAFT/CODE_PRINTED). |
| toOrganizationId | UUID | Có | Tổ chức nhận, phải khác tổ chức giao và đang ACTIVE. |
| quantity | number | Có | Số lượng kg bàn giao, > 0 và ≤ remaining. |
| plannedAt | datetime | Không | Thời điểm dự kiến bàn giao. |
| vehicleInfo | string | Không | Phương tiện vận chuyển. |
| carrierName | string | Không | Người áp tải. |
| note | string | Không | Ghi chú. |
| attachmentPath | string | Không | Đường dẫn chứng từ giao hàng. Frontend lấy giá trị `filePath` từ endpoint upload (`POST /attachment`) rồi gửi kèm ở đây; nếu không đính kèm thì bỏ trống/null. |

## 4. POST /api/v1/shipment-handovers/attachment — Upload chứng từ

**Request**: `multipart/form-data`, field `file`.

| Trường | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| file | file | Có | File chứng từ (JPG/PNG/PDF), kích thước ≤ `app.upload.handover.max-size` (mặc định 5MB). |

**Response thành công (200)**

```json
{
  "success": true,
  "status": 200,
  "data": {
    "filePath": "/app/uploads/handovers/2d0cd11eb54a47198671be789384840b.pdf"
  }
}
```

- `filePath` là đường dẫn **tuyệt đối trong container backend** (nằm dưới `app.upload.base-dir`/`handovers`).
- Đường dẫn được trả về đúng dạng `attachmentPath` cần gửi khi gọi `POST /shipment-handovers`.
- File được serve (đọc/xem) qua `GET /uploads/handovers/{tên file}` — `WebConfig` map `/uploads/**` tới `app.upload.base-dir`, nên khi frontend hiển thị cần trích chuỗi sau `/uploads/` rồi ghép với asset base URL (xem `toHandoverAssetUrl` trong `CreateHandoverDialog.tsx`).

**Error cases (400):**

```json
{
  "success": false,
  "status": 400,
  "message": "Chỉ chấp nhận chứng từ định dạng JPG, PNG hoặc PDF"
}
```

```json
{
  "success": false,
  "status": 400,
  "message": "Kích thước chứng từ vượt quá giới hạn cho phép (5MB)"
}
```

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
    "attachmentPath": "handover-docs/2026/phieu-xuat-kho-123.pdf",
    "expiresAt": "2026-09-10T09:00:00",
    "createdAt": "2026-09-08T09:00:00"
  }
}
```

## 5. POST /{id}/cancel — Hủy phiếu (TC-04)

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

## 6. GET endpoints

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

### GET /api/v1/organizations/recipient-organizations — Danh sách tổ chức nhận

Bổ sung để khắc phục lỗi 403 khi VT-02 mở dialog Tạo phiếu bàn giao.
Nguyên nhân cũ: dialog gọi `GET /admin/organizations` (chỉ VT-01) nên VT-02 luôn 403,
`Promise.all` fail kéo `remainingQuantity` về 0 giả và dropdown trống.

Quyền: `VT-01`, `VT-02`. Lọc: `status = ACTIVE` và khác tổ chức hiện tại.

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "name": "Công Ty Trà Việt",
      "code": "CTV",
      "type": "ENTERPRISE",
      "status": "ACTIVE"
    }
  ]
}
```

## 7. Điều kiện & Business Rules

**Người dùng phải:**
- Đăng nhập thành công, thuộc tổ chức giao (owner lô hàng).

**Lô hàng phải:**
- Trạng thái ACTIVATED (TC-01).
- KHÔNG ở trạng thái RECALLED (TC-03).
- Không có mã tem nào LOCKED.

**Ràng buộc số lượng:**
- `quantity ≤ totalQuantity − Σ(PENDING_CONFIRMATION + ACCEPTED)` (TC-02).
- Tổ chức nhận phải khác tổ chức giao.

## 8. Error Cases

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

**400 — lô chưa kích hoạt tem (DRAFT/CODE_PRINTED)**

```json
{
  "success": false,
  "status": 400,
  "message": "Chỉ có thể tạo phiếu bàn giao cho lô hàng đã kích hoạt tem"
}
```

**400 — tổ chức nhận không hoạt động**

```json
{
  "success": false,
  "status": 400,
  "message": "Tổ chức nhận không còn hoạt động, không thể tạo phiếu bàn giao"
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

## 9. Backend xử lý

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
Kiểm tra status = ACTIVATED         -> 400 nếu DRAFT/CODE_PRINTED (tem chưa kích hoạt)
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
Kiểm tra toOrganization ACTIVE      -> 400 nếu INACTIVE
    │
    ▼
remaining = totalQuantity − Σ(PENDING + ACCEPTED)
    │
    ▼
quantity > remaining                -> 409 (TC-02)
    │
    ▼
Tạo phiếu PENDING_CONFIRMATION (kèm attachmentPath nếu có),
expiresAt = now + app.handover.expiry-hours (48h)
    │
    ▼
Gửi notification cho tổ chức nhận
(NotificationService.sendHandoverNotification → user có notification:READ
thuộc tổ chức nhận; tương tự cho cancel/accept/reject về phía còn lại)
    │
    ▼
Trả HandoverResponse (200)
```

### GET /api/v1/shipments/{id}/has-pending-handover

```json
{
  "success": true,
  "status": 200,
  "data": true
}
```

`data = true` khi tồn tại phiếu PENDING_CONFIRMATION của lô. Frontend dùng để hiển
thị nhãn "Đang bàn giao" trên chi tiết/dòng sự kiện của lô trong thời gian chờ
(derived, không thêm cột trạng thái mới — đúng CV-04-1).

## 10. Cấu hình

| Key | Mặc định | Mô tả |
|---|---|---|
| `app.handover.expiry-hours` | 48 | Số giờ phiếu PENDING có hiệu lực trước khi hết hạn. |
| `app.handover.expiry-check-cron` | `0 0 * * * ?` | Cron cho `HandoverExpiryScheduler` quét phiếu quá hạn (EXPIRED). |
| `app.upload.handover.relative-path` | `handovers` | Thư mục con dưới `app.upload.base-dir` chứa chứng từ phiếu bàn giao. |
| `app.upload.handover.max-size` | `5242880` | Giới hạn kích thước file upload chứng từ (bytes). |

## 11. Database

Bảng `shipment_handovers` (migration `V20260908000000__create_shipment_handovers.sql`):

- PK: `id` CHAR(36)
- FK: `shipment_id` → shipments, `from_organization_id`/`to_organization_id` → organizations, `created_by`/`confirmed_by`/`rejected_by`/`cancelled_by` → users
- Status enum: PENDING_CONFIRMATION, ACCEPTED, REJECTED, EXPIRED, CANCELLED (transition chỉ từ PENDING_CONFIRMATION, không đảo chiều)
- `attachment_path` VARCHAR(512) NULL — đường dẫn chứng từ giao hàng (có sẵn từ migration gốc, wire qua request/response)
- Index: shipment_id, to_organization_id, from_organization_id, status, expires_at, (status, expires_at)

**Lưu ý:** `Shipment.organization` KHÔNG bị thay đổi khi tạo/accept phiếu — việc "chuyển trách nhiệm" là trạng thái logic trên phiếu bàn giao, không phải đổi owner vật lý của lô hàng.

## 12. Notification kèm `entityId` + điều hướng UI (Round 09/09/2026)

### 12.1 Liên kết notification → phiếu bàn giao

- Migration `V20260909000001__add_entity_id_to_notifications.sql`: bảng `notifications` thêm
  cột `entity_id CHAR(36) NULL` (+ index `idx_notifications_entity_id`).
- `NotificationService.sendHandoverNotification` đổi signature thành
  `(String title, String content, UUID entityId, UUID organizationId)` — `entityId` chính là
  `handover.getId()`, được lưu vào `notifications.entity_id` qua `NotificationServiceImpl`.
- `NotificationResponse` thêm trường `entityId` (UUID) → frontend dùng để điều hướng.
- Notification tạo **sau** migration có `entity_id` đầy đủ; các notification cũ không có
  `entity_id` (NULL) nên không điều hướng được.

### 12.2 Frontend

- Route mới: `/shipment-handovers/:id` → `frontend/src/pages/shipment-handover/HandoverDetailPage.tsx`
  (view-only, `allowedRoles = AUTHENTICATED_ROLE_CODES`). Backend `GET /{id}` vẫn chặn 403
  nếu người gọi không thuộc sender/receiver org.
- `NotificationBell` + `NotificationsPage`: khi bấm notification có `entityId` → điều hướng
  `/shipment-handovers/{entityId}` (vẫn đánh dấu đã đọc).
- `ProcurementShipmentList` (Dashboard thu mua VT-04): gọi `GET /shipment-handovers/received`,
  map `shipmentId → phiếu bàn giao mới nhất (theo createdAt)`, thêm nút "Xem phiếu bàn giao".
- Tài liệu/việc xem chứng từ tái sử dụng `toHandoverAssetUrl` trong `CreateHandoverDialog.tsx`.

### 12.3 UTF-8

- JDBC URL thêm `characterEncoding=UTF-8` sau khi phát hiện lý do hủy hiển thị mojibake
  ("D?n d?p d? li?u ki?m th?") — nguyên nhân thật: dữ liệu test gửi từ PowerShell (client
  mã hóa Latin-1/ANSI) nên dấu tiếng Việt bị thay bằng `?` trước khi vào DB.
- Database là `utf8mb4`; pipeline Java/Ký tự (Jackson → JDBC → MySQL) đã chứng minh
  roundtrip UTF-8 đúng (tạo/cancel phiếu + notification) qua API.

## 13. Xác nhận / Từ chối nhận hàng từ UI (Round 09/09/2026 — NCL-05-CN-009)

### 13.1 Frontend (`HandoverDetailPage.tsx`)

- Khi `status = PENDING_CONFIRMATION` **và** `user.organizationId === handover.toOrganizationId`,
  hiển thị card hành động "Xác nhận nhận hàng" gồm 2 nút:
  - **"Xác nhận nhận hàng"** → gọi `POST /shipment-handovers/{id}/accept` → toast +
    reload (status ACCEPTED, "Thời điểm xác nhận").
  - **"Từ chối nhận hàng"** → mở dialog bắt buộc nhập lý do (`@NotBlank` backend) →
    `POST /shipment-handovers/{id}/reject` → toast + reload (status REJECTED,
    "Thời điểm từ chối" + lý do hiển thị).
- Ẩn nút khi org giao / org thứ ba / phiếu không còn PENDING — mirror đúng backend
  (org check + status check), không gắn role cứng cục bộ để tránh lệch quyền.
- Đồng bộ Backend ↔ Frontend (~Section 23): gating `organizationId === toOrganizationId`
  khớp `ShipmentHandoverServiceImpl.accept/reject`; message lỗi backend hiển thị qua toast;
  `CancelHandoverRequest.reason` (`@NotBlank`) dùng chung cho cancel và reject.

### 13.2 Unit test bổ sung

- `ShipmentHandoverServiceTest` (18 test): accept/reject sai org (403), accept/reject/cancel
  không PENDING (400), accept/reject/cancel **hết hạn** (400 "Phiếu bàn giao đã hết hạn"),
  reject **không** sinh `chain_events`, notification accept/reject gửi tới org GIAO với
  `entityId` = phiếu, getById quá hạn trả EXPIRED.
- `HandoverExpiryServiceTest` (2 test): chuyển EXPIRED + gửi notification tới **cả 2 tổ chức**;
  giữ nguyên phiếu chưa quá hạn.
- `__tests__/HandoverDetailPage.test.tsx` (5 test): hiện/ẩn nút theo (org, status),
  accept gọi đúng API + toast, reject dialog yêu cầu lý do.

### 13.3 Hết hạn phiếu (NCL-05-CN-009 TC-03)

- Bean mới `HandoverExpiryService` (`HandoverExpiryServiceImpl`): quét các phiếu
  `PENDING_CONFIRMATION` có `expires_at < now` → chuyển `EXPIRED`, gửi notification
  **tới cả tổ chức giao và tổ chức nhận** (`entityId` = phiếu). Chạy
  `@Transactional(propagation = REQUIRES_NEW)` để thay đổi + thông báo được duy trì bền vững
  ngay cả khi action gọi bên ngoài ném ngoại lệ; idempotent (chỉ chọn phiếu còn PENDING).
- `HandoverExpiryScheduler` (cron `app.handover.expiry-check-cron`, mặc định `0 0 * * * ?`)
  ủy quyền xử lý cho service trên.
- **Lazy expire** trong các action: `accept`/`reject`/`cancel` nếu gặp phiếu quá hạn →
  gọi `expireOverdueHandovers()` rồi chặn bằng 400 "Phiếu bàn giao đã hết hạn"
  (không cho reject/cancel tạo REJECTED/CANCELLED thay vì EXPIRED). `getById` quá hạn →
  lazy chuyển EXPIRED rồi trả status EXPIRED để UI hiển thị đúng, không chờ tới giờ cron.
