# Kịch bản kiểm thử thủ công — NCL-05-CN-008 Phiếu Bàn Giao Lô Hàng

> **Story:** NCL-05-CN-008 — Tạo và hủy phiếu bàn giao lô hàng.
> **Branch:** `feature/NCL-05-CN-008-009-shipment-handover`
> **Phạm vi:** Tạo phiếu (`POST /api/v1/shipment-handovers`) + Hủy phiếu (`POST /{id}/cancel`).
> Xác nhận/từ chối/hết hạn thuộc **NCL-05-CN-009** (ngoài scope tài liệu này).

---

## 1. Luồng đi của chức năng

### 1.1 Luồng nghiệp vụ (bàn giao giữa 2 tổ chức)

```text
[Tổ chức GIAO — HTX, VT-02]
    │
    ▼ 1. Mở chi tiết lô hàng (trạng thái ACTIVATED)
    │
    ▼ 2. Nút "Tạo phiếu bàn giao" → dialog (form)
    │      ├─ Tổ chức nhận (dropdown, org ACTIVE)
    │      ├─ Số lượng (kg — hiển thị "còn lại có thể bàn giao" từ API)
    │      ├─ Thời điểm dự kiến (optional)
    │      ├─ Phương tiện (optional)
    │      ├─ Người áp tải (optional)
    │      └─ Ghi chú (optional)
    │
    ▼ 3. Submit → POST /api/v1/shipment-handovers
    │      BE: kiểm lô (không thu hồi, không tem khóa)
    │      BE: kiểm owner org = org giao
    │      BE: remaining = totalQuantity − Σ(PENDING+ACCEPTED)
    │      BE: quantity ≤ remaining?
    │      ↓ có → 200 PENDING_CONFIRMATION, notification org nhận
    │      ↓ không → 400/409 message "Lô hàng chỉ còn X kg…"
    │
    ▼ 4. Phiếu đang chờ xác nhán (status PENDING_CONFIRMATION)
              │
              ├─ [NCL-009] Org nhận xác nhán → ACCEPTED (+ChainEvent HANDOVER)
              ├─ [NCL-009] Org nhận từ chối  → REJECTED
              ├─ [NCL-009] Quá expires_at (scheduler) → EXPIRED
              └─ [NCL-008] Org giao hủy kèm lý do → CANCELLED  ← phạm tầng này
```

### 1.2 Transfer trách nhiệm — nguyên lí

- **KHÔNG** đổi `shipments.organization_id`. Phiếu bàn giao là trạng thái logic:
  trách nhiệm chuyển **chỉ khi** tổ chức nhận ghi xác nhán (accept) — thuộc NCL-009.
- `Shipment.organization` giữ giá gốc; nhãn "đang bàn giao" **derived** dari tồn
  phiếu PENDING_CONFIRMATION (query), không thêm cột trên Shipment.
- Số lượng "còn lại" = `totalQuantity − Σ(phiếu PENDING_CONFIRMATION + ACCEPTED)` —
  tính động dari 1 query SUM, không bảng allocation riêng.

---

## 2. Nguyên lí và cách làm việc

### 2.1 Các thành phần chính

| Thành phần | Vị trí | Vai trò |
|---|---|---|
| `ShipmentHandover` entity | `backend/.../trace/entity/ShipmentHandover.java` | Bảng `shipment_handovers` — phiếu bàn giao |
| `ShipmentHandoverStatus` enum | `backend/.../trace/enums/ShipmentHandoverStatus.java` | PENDING_CONFIRMATION / ACCEPTED / REJECTED / EXPIRED / CANCELLED |
| `ShipmentHandoverService(Impl)` | `backend/.../trace/service/` | Nghiệp vụ: create / cancel (+ accept/reject/getById/sent/received/remaining) |
| `ShipmentHandoverController` | `backend/.../trace/controller/` | REST: POST /shipment-handovers, POST /{id}/cancel, GET by-id/sent/received |
| `ShipmentController` | `backend/.../trace/controller/ShipmentController.java` | + `GET /{id}/remaining-handover-quantity` |
| `HandoverExpiryScheduler` | `backend/.../trace/scheduler/` | Cron định kỳ: phiếu PENDING quá `expires_at` → EXPIRED |
| `ShipmentHandoverRepository` | `backend/.../trace/repository/` | `sumQuantityByShipmentIdAndStatusIn`, `findExpiredPending`, `existsByShipmentIdAndStatus` |
| `CreateHandoverDialog.tsx` | `frontend/src/components/shipment/` | UI tạo phiếu bàn giao |
| `handoverApi.ts` | `frontend/src/api/` | Client API functions |
| Migration | `backend/src/main/resources/db/migration/schema/V20260908000000__create_shipment_handovers.sql` | Bảng + index + FK |

### 2.2 Nguyên lí nghiệp vụ (QTN-31)

| Lựa chọn | Hệ quả |
|---|---|
| **Tạo phiếu** | Phiếu PENDING_CONFIRMATION, `expires_at = now + app.handover.expiry-hours` (48h default). Trách nhiệm **chưa** chuyển. |
| **Hủy phiếu** (bên giao) | CANCELLED + lý do bắt buộce. Remaining tự giải fàng (query động). Nhãn "đang bàn giao" biàn mất. |
| Xác nhán (NCL-009) | ACCEPTED, ChainEvent HANDOVER, trách nhiệm → org nhận |
| Từ chối (NCL-009) | REJECTED, trách nhiệm giữ nguyên bên giao |
| Quá hạn (scheduler, NCL-009) | EXPIRED, trách nhiệm giữ nguyên bên giao |

### 2.3 Dữ liệu test chuẩn bị (chạy 1 lần)

Bên giao: `orgmanager/admin123` (VT-02, DEMO_HTX)
Bên nhận: `procurement/...` (VT-04, DEMO_NSV) — hoặc chỉ so sán bằng org IDs DB.

```powershell
# 1. Đăng login + chọn org
$st = (Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/login' -Method Post `
  -Body '{"username":"orgmanager","password":"admin123"}' -ContentType 'application/json').data.selectionToken
$r = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/select-organization' -Method Post `
  -Body '{"organizationId":"327a3a0e-a396-11f1-aea2-32ec817c7ea4"}' -ContentType 'application/json' `
  -Headers @{Authorization="Bearer $st"}
$token = $r.data.accessToken

# 2. Dữ liệu test đã so sán kịch bán dưới (DB):
#    Lô hàng 00000000-0000-0000-0000-001000000001  totalQuantity = 1000, ACTIVATED
#    Org nhận 327a40dc-a396-11f1-aea2-32ec817c7ea4 (DEMO_NSV)
```

---

## 3. Kịch bán kiểm thử thủ công

### TC-01 (Cao) — Tạo phiếu khi lô còn 1 tấn

**Điều kiến:** Lô `001000000001` totalQuantity = 1000kg, ACTIVATED, không phiếu PENDING/ACCEPTED tồn.

**Bước:**
1. (API) POST `/api/v1/auth/login` orgmanager → POST select-organization → token.
2. POST `/api/v1/shipment-handovers`:
   ```json
   { "shipmentId": "00000000-0000-0000-0000-001000000001",
     "toOrganizationId": "327a40dc-a396-11f1-aea2-32ec817c7ea4",
     "quantity": 800 }
   ```

**Kết quác mong dợ**
- HTTP 200, `status = "PENDING_CONFIRMATION"`.
- `fromOrganizationId` = DEMO_HTX, `toOrganizationId` = DEMO_NSV.
- `expiresAt` ≈ now + 48h.
- Nhân "đang bàn giao" xuẽ hiển thị (derived từ tồn phiếu PENDING).
- Notification gửi org nhán (xem bảng notifications/UI người nhán).

### TC-02 (Cao) — Tạo phiếu vượt lượng còn lại

**Điều kiến:** Đã tồn phiếu PENDING 800kg trên lô 1000kg → remaining = 200kg.

**Bước:**
1. POST `/api/v1/shipment-handovers` quantity = 800 (còn lại 200kg).

**Kết quác mong dợ**
- HTTP 400 (BusinessException), message: `Lô hàng chỉ còn 200 kg có thể bàn giao`.
- Không tạo phiếu mới (COUNT phiếu PENDING không đổi).

### TC-03 (Cao) — Chặn bàn giao lô thu hồi

**Điều kién:** Lô `001000000003` trạng thái `RECALLED`.

**Bước (chuẩn bí):**
```sql
UPDATE shipments SET status='RECALLED' WHERE id='00000000-0000-0000-0000-001000000003';
```
1. POST `/api/v1/shipment-handovers` trên lô đó (quantity bất kỳ > 0).
2. Hoàn hio khôi phục:
```sql
UPDATE shipments SET status='ACTIVATED' WHERE id='00000000-0000-0000-0000-001000000003';
```

**Kết quác mong dợ**
- HTTP 400, message: `Lô hàng đang bị thu hồi, không thể tạo phiếu bàn giao`.

### TC-04 (Trung) — Hủy phiếu chưa xác nhán kèm lý do

**Điều kién:** Phiếu PENDING tồn (dari TC-01), id = `<handoverId>`.

**Bước:**
1. POST `/api/v1/shipment-handovers/{id}/cancel`:
   ```json
   { "reason": "Không còn nhu càu bàn giao" }
   ```

**Kết quác mong dợ**
- HTTP 200, `status = "CANCELLED"`, `cancelReason` lưu đúng.
- Nhãn "đang bàn giao" **biàn mất** — verify:
  - GET `/api/v1/shipments/00000000-0000-0000-0000-001000000001/remaining-handover-quantity` → `1000`.
  - SQL: `SELECT status, COUNT(*) FROM shipment_handovers WHERE shipment_id='...' GROUP BY status;` → chỉ CANCELLED, 0 PENDING.

### TC-05 (phụ trợ) — Authorization (bên non-giao bị chặn)

**Điều kién:** Phiếu PENDING tồn.

**Bước:**
1. Đăng login `procurement` (VT-04, org nhán) → token.
2. POST `/api/v1/shipment-handovers/{id}/cancel`.

**Kết quác mong dợ**
- HTTP 403, `Bạn không có quyền hủy phiếu bàn giao`.

### TC-06 (phụ trợ) — Hủy phiếu đã quá PENDING

**Điều kién:** Phiếu đã CANCELLED dari TC-04.

**Bước:**
1. POST cancel lại phiếu đó.

**Kết quác mong dợ**
- HTTP 400, `Chỉ có thể hủy phiếu đang chờ xác nhán`.

---

## 4. Verify toàn (so that branch)

```powershell
Set-Location backend; ./mvnw -q test -Dtest=ShipmentHandoverServiceTest   # 7/7 pass
Set-Location ..\frontend; npm run build                                     # exit 0
git diff --stat
```