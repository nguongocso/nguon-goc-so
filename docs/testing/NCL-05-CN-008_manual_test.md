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
    │      ├─ Tổ chức nhận (dropdown, org ACTIVE — hiển thị TÊN org, không phải UUID)
    │      ├─ Số lượng (kg — hiển thị "còn lại có thể bàn giao" từ API)
    │      ├─ Chứng từ (upload JPG/PNG/PDF ≤ 5MB, thay thế ô nhập tay URL)
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
| `ShipmentHandoverService(Impl)` | `backend/.../trace/service/` | Nghiệp vụ: create / cancel / uploadAttachment (+ accept/reject/getById/sent/received/remaining) |
| `HandoverAttachmentUploadResponse` | `backend/.../dto/response/` | DTO trả về `filePath` sau upload |
| `ShipmentHandoverController` | `backend/.../trace/controller/` | REST: POST /shipment-handovers, POST /attachment, POST /{id}/cancel, GET by-id/sent/received |
| `WebConfig` | `backend/.../config/` | Serve `/uploads/**` → thư mục upload cho việc xem chứng từ |
| `ShipmentController` | `backend/.../trace/controller/ShipmentController.java` | + `GET /{id}/remaining-handover-quantity` |
| `HandoverExpiryScheduler` | `backend/.../trace/scheduler/` | Cron định kỳ: phiếu PENDING quá `expires_at` → EXPIRED |
| `ShipmentHandoverRepository` | `backend/.../trace/repository/` | `sumQuantityByShipmentIdAndStatusIn`, `findExpiredPending`, `existsByShipmentIdAndStatus` |
| `CreateHandoverDialog.tsx` | `frontend/src/components/shipment/` | UI tạo phiếu bàn giao — dropdown hiển thị tên org qua `buildRecipientLabelMap`; UI upload chứng từ (Xem/Gỡ) |
| `handoverApi.ts` | `frontend/src/api/` | Client API functions + `uploadHandoverAttachment` (multipart) |
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
2. (UI) Mở detail lô → nút "Tạo phiếu bàn giao": dropdown "Tổ chức nhận" **hiển thị tên org** (VD "Công ty Nông Sản Việt Demo"), KHÔNG hiển thị UUID — verify: `buildRecipientLabelMap` map id→name, `SelectValue` dùng children function.
3. (UI/API) Đính kèm chứng từ: upload file JPG/PNG/PDF ≤ 5MB → dialog hiện tên file + nút "Xem"/"Gỡ"; "Xem" mở được file (`GET /uploads/handovers/…`).
4. (API) POST `/api/v1/shipment-handovers`:

   ```json
   { "shipmentId": "00000000-0000-0000-0000-001000000001",
     "toOrganizationId": "327a40dc-a396-11f1-aea2-32ec817c7ea4",
     "quantity": 800,
     "attachmentPath": "/app/uploads/handovers/<filePath từ bước 3>" }
   ```

**Kết quác mong dợ**
- HTTP 200, `status = "PENDING_CONFIRMATION"`, `attachmentPath` lưu đúng giá trị từ upload.
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
Set-Location backend; ./mvnw test                    # 742/742 pass (ShipmentHandoverServiceTest 10/10, ShipmentControllerTest 10/10)
Set-Location ..\frontend; npm run lint               # exit 0
npm run test                                          # 141 test, chỉ AreaAssignmentPage "TC-E2 unassign" flaky khi chạy cả suite (pass 14/14 khi chạy riêng file) — KHÔNG liên quan story này
npm run build                                         # tsc -b && vite build, exit 0
git diff --stat
```

**Ghi chú:** `ShipmentControllerTest` yêu cầu `@MockitoBean ShipmentHandoverService` (ShipmentController inject service này) — đã bổ sung để full suite xanh.

---

## 5. Kịch bản bổ sung — Round 09/09/2026 (3 lỗi user báo)

> Gồm: (1) dropdown "Tổ chức nhận" luôn hiển thị placeholder dù đã chọn;
> (2) chưa xem được chi tiết phiếu từ Dashboard trang notification;
> (3) lý do hủy phiếu hiển thị mojibake "D?n d?p d? li?u ki?m th?".
>
> Hạ tầng: backend đã rebuild (migration `V20260909000001`), notification mới có `entityId`,
> JDBC URL có `characterEncoding=UTF-8`. Frontend: route `/shipment-handovers/:id`
> (trang `HandoverDetailPage`), bấm notification điều hướng tới phiếu, nút "Xem phiếu bàn giao"
> trong danh sách lô hàng (VT-04).

### TC-07 (Cao) — Dropdown "Tổ chức nhận" hiển thị tên org sau khi chọn (Issue 1)

Nguyên nhân cũ: Base UI `SelectValue` truyền **raw value** (string) vào children function;
code cũ destructure `{ value }` → luôn undefined → hiển thị placeholder mãi. Fix:
`renderRecipientSelectValue(value, orgLabels)` (export từ `CreateHandoverDialog.tsx`)
nhận raw value, lookup `buildRecipientLabelMap`.

**Bước:**
1. Đăng nhập `orgmanager/admin123` (VT-02) → mở chi tiết lô ACTIVATED → "Tạo phiếu bàn giao".
2. Mở dropdown "Tổ chức nhận", chọn "Công ty Nông Sản Việt Demo".

**Kết quả mong đợi**
- Trigger hiển thị **"Công ty Nông Sản Việt Demo (DEMO_NSV)"**, KHÔNG còn placeholder.
- Đơn vị test tự động: `__tests__/CreateHandoverDialog.test.tsx` — `renderRecipientSelectValue`
  nhận raw value trả về tên org; value falsy trả placeholder; không destructure `{ value }`.

### TC-08 (Cao) — Bấm notification mở trang chi tiết phiếu (Issue 2b)

Điều kiện: tồn **notification có `entityId`** — tạo phiếu bàn giao MỚI sau khi rebuild
(notification cũ trước migration có `entity_id` NULL nên không điều hướng).

**Bước:**
1. Tạo phiếu bàn giao mới (VT-02 → VT-04) qua UI hoặc API.
2. Đăng nhập `procurement` (VT-04, tổ chức nhận).
3. Mở dropdown thông báo (góc phải header) hoặc trang Thông báo; bấm vào thông báo
   "Phiếu bàn giao ... chờ xác nhận".

**Kết quả mong đợi**
- URL = `/shipment-handovers/<id>` hiển thị `HandoverDetailPage`: status "Chờ xác nhận",
  bên giao/nhận, số lượng, thời điểm, thời hạn, ghi chú; notification được đánh dấu đã đọc.
- Truy cập trực tiếp phiếu của org khác → 403 (backend `GET /{id}`).

### TC-09 (Trung) — Nút "Xem phiếu bàn giao" trong danh sách lô hàng (Issue 2a)

**Bước:**
1. Đăng nhập `procurement` (VT-04) → Dashboard thu mua → danh sách lô hàng.
2. Với lô đang có phiếu PENDING của tổ chức nhận → bấm icon bắt tay "Xem phiếu bàn giao".

**Kết quả mong đợi**
- Mở `HandoverDetailPage` của phiếu tương ứng (map `shipmentId → phiếu mới nhất`).
- Lô chưa có phiếu: không hiển thị nút.

### TC-10 (Trung) — Lý do hủy / nội dung nhập tiếng Việt không bị mojibake (Issue 3)

**Bước:**
1. Tạo phiếu bàn giao mới, ghi chú có dấu (VD "Phiếu bàn giao kiểm tra UTF-8: giao hàng đúng hẹn.").
2. Gửi lý do hủy có dấu (VD "Dọn dẹp dữ liệu kiểm thử").
3. Xem lại trang chi tiết (hoặc `GET /{id}`) và kéo notification.

**Kết quả mong đợi**
- Hiển thị đúng dấu tiếng Việt (không còn `?`). Đã chứng minh roundtrip UTF-8:
  - DB `HEX(cancel_reason)` chứa đa-byte UTF-8 (`E1BB8D` = ọ, `E28094` = —), không có byte `3F`.
  - Database utf8mb4; lưu ý KHÔNG gửi dữ liệu tiếng Việt qua PowerShell/vi mà không đúng mã hóa
    (PowerShell 5.1 đổi chuỗi sang ANSI → dấu thành `?` trước khi vào DB).
  - Verify nhanh: `SELECT id, HEX(cancel_reason) FROM shipment_handovers;`

**Dữ liệu test hiện trạng (sau khi dọn ở round này):**
- 1 phiếu PENDING_CONFIRMATION `b5c2463b-0007-4704-8277-a152a525a697`
  (VT-02 → VT-04, 50kg, note UTF-8, có notification `entityId`) — dùng cho TC-08/TC-09/TC-11/TC-12.
- 1 phiếu ACCEPTED `3dd95ecb-978f-42f7-8b09-cf1a966872d0` (200kg) — mẫu "đã xác nhận".
- 1 phiếu REJECTED `f1fa4b12-2a1b-4294-9c52-d99e36bc32d1` (50kg) — mẫu "đã từ chối".

---

## 6. Kịch bản bổ sung — Round 09/09/2026 (xác nhận nhận hàng từ tổ chức bàn giao — NCL-05-CN-009)

> Nội dung: kiểm tra E2E logic accept/reject của bên nhận, bổ sung UI
> "Xác nhận nhận hàng" / "Từ chối nhận hàng" trên `HandoverDetailPage` và
> unit test negative cho backend. Quyền hiển thị nút: **org nhận + status PENDING**
> (mirror đúng backend `ShipmentHandoverServiceImpl.accept/reject`, không gắn role cứng).

### TC-11 (Cao) — Xác nhận nhận hàng từ tổ chức nhận (UI + API)

**Điều kiện:** Phiếu PENDING `b5c2463b-...` (VT-02 → VT-04), backend chạy (8080).

**Bước:**
1. Đăng nhập `procurement/admin123` (VT-04, DEMO_NSV) → Dashboard thu mua →
   danh sách lô → nút "Xem phiếu bàn giao" (hoặc bấm notification).
2. Xem `HandoverDetailPage` phiếu PENDING → xuất hiện card "Xác nhận nhận hàng"
   với 2 nút "Xác nhận nhận hàng" / "Từ chối nhận hàng".
3. Bấm "Xác nhận nhận hàng".

**Kết quả mong đợi**
- Toast "Đã xác nhận nhận hàng lô bàn giao." và trang reload: status → **"Đã xác nhận"**,
  hiển thị "Thời điểm xác nhận". Nút không còn hiển thị.
- API tương đương đã verify:
  - `POST /{id}/accept` (org nhận) → 200 ACCEPTED, `confirmedBy/confirmedAt` set.
  - DB `chain_events`: event `HANDOVER`, `event_data` = `{"action":"ACCEPTED","quantity":200,...}`,
    `recorded_by` = user procurement, `recorded_organization_id` = DEMO_NSV.
  - Notification "Phiếu bàn giao đã được xác nhận" gửi **chỉ user org GIAO** (17 user HTX),
    `entity_id` = phiếu, nội dung UTF-8 sạch (`HEX` không có byte `3F`).
  - `sum(PENDING+ACCEPTED)` giữ nguyên → `remaining` không đổi; `shipments.organization_id` không đổi.

### TC-12 (Cao) — Từ chối nhận hàng (UI + API)

**Bước:**
1. Mở phiếu PENDING khác → bấm "Từ chối nhận hàng" → dialog nhập lý do.
2. Nhập lý do tiếng Việt (VD "Chứng từ kiểm dịch chưa đầy đủ") → "Xác nhận từ chối".

**Kết quả mong đợi**
- Toast "Đã từ chối nhận hàng lô bàn giao."; status → **"Đã từ chối"**, hiển thị
  "Thời điểm từ chối" + lý do. Nút không còn.
- API: `POST /{id}/reject` → 200 REJECTED; notification "đã bị từ chối" gửi org giao kèm lý do,
  `entity_id` = phiếu; **KHÔNG** tạo `chain_events` (chỉ accept mới chuyển trách nhiệm);
  phiếu REJECTED bị loại khỏi `sum(PENDING+ACCEPTED)` → số lượng nhả lại bên giao.

### TC-13 (Trung) — Negative API (đã E2E verify 09/09/2026)

| Test | Hành động | Kết quả |
|---|---|---|
| Accept lại phiếu ACCEPTED | `POST /{id}/accept` (org nhận) | 400 "Chỉ có thể xác nhận phiếu đang chờ" |
| Org giao accept | `POST /{id}/accept` (orgmanager) | 403 "Bạn không có quyền xác nhận phiếu bàn giao" |
| Accept phiếu hết hạn | `UPDATE expires_at` quá khứ → accept | 400 "Phiếu bàn giao đã hết hạn" |
| Org thứ ba xem phiếu | `GET /{id}` (admin VT-01/SYSTEM) | 403 "Bạn không có quyền xem phiếu bàn giao này" |
| Reject sai org / sai trạng thái | `POST /{id}/reject` | 403 / 400 (message tương ứng) |

### TC-14 (Cao) — Phiếu quá hạn → EXPIRED, cả 2 bên nhận notification (AC TC-03)

**Điều kiện:** Backend đã rebuild bản có `HandoverExpiryService`.

**Bước:**
1. Tạo phiếu PENDING mới (VT-02 → VT-04, 30kg).
2. Dời `expires_at` về quá khứ (mô phỏng quá 48h): SQL admin.
3. Bên nhận `GET /{id}` (hoặc cố accept/reject) → **lazy expire**: phiếu chuyển
   `EXPIRED`, accept/reject/cancel trả 400 "Phiếu bàn giao đã hết hạn".
4. Kiểm tra notification: `SELECT * FROM notifications WHERE entity_id='<id>'`
   → có thêm 2 loạt "Phiếu bàn giao đã hết hạn" cho **cả tổ chức giao lẫn tổ chức nhận**.
5. `remaining` loại EXPIRED khỏi tổng PENDING+ACCEPTED → số lượng trả về bên giao.

**Điểm thiết kế:** transition + notification nằm trong `HandoverExpiryService`
(`@Transactional(REQUIRES_NEW)`) nên được lưu bền vững dù action gọi ngoài roll back;
`HandoverExpiryScheduler` (cron `0 0 * * * ?`) gọi chính service này — idempotent vì chỉ
chọn phiếu còn `PENDING_CONFIRMATION`.

### TC-15 (Cao) — Lối vào chủ động xem phiếu bàn giao cho VT-04 (UI)

**Điều kiện:** Frontend bản mới (trang "Phiếu bàn giao nhận" + nút header + mục sidebar).

**Bước:**
1. Đăng nhập `procurement` (org DEMO_NSV) → vào Dashboard thu mua.
2. Mở menu **Thu mua → Phiếu bàn giao nhận** trên sidebar (lối vào duy nhất, không đặt
   nút trên header để tránh trùng).
3. Trang danh sách hiển thị 3 phiếu (ACCEPTED 200kg, PENDING 50kg, REJECTED 50kg
   của lô `Nho đỏ 01 (demo)`) — kể cả khi lô đó nằm ở trang 2 của danh sách thu mua.
4. Bấm **"Xem chi tiết"** phiếu PENDING → vào `HandoverDetailPage`, xác nhận/từ chối bình thường.

**Kết quả mong đợi**
- List gọi `GET /shipment-handovers/received` (proxy dev `:3000/api/v1` → 8080), không phụ thuộc
  vào việc lô hàng tương ứng có nằm trên trang hiện tại của dashboard hay không.
- Mỗi dòng có badge trạng thái tiếng Việt và nút "Xem chi tiết".

### Verify tự động (bổ sung round này)

```powershell
Set-Location backend; ./mvnw test    # 775/775 pass (ShipmentHandoverServiceTest 18/18,
                                     # HandoverExpiryServiceTest 2/2 — thêm 5 negative accept/reject
                                     # + 3 blocked-expire + 1 lazy-expire)
Set-Location ..\frontend; npm run lint               # exit 0
npm run test -- --run                                # 155 test: 154 pass, 1 flaky pre-existing
                                                     # (AreaAssignmentPage TC-E2 timeout) — không liên quan
npm run build                                        # exit 0
```

- FE mới: `__tests__/HandoverDetailPage.test.tsx` (5 test: hiện nút theo org+status, ẩn khi org
  giao / đã ACCEPTED, accept gọi API, reject dialog yêu cầu lý do).
- FE mới (lối vào chủ động): `ShipmentHandoverReceivedListPage.tsx` dùng
  `HandoverStatusBadge.tsx` (component dùng chung cho badge trạng thái phiếu bàn giao);
  route `/shipment-handovers/received` cho VT-04 (`ROLE_ACCESS.handoverReceivedView`),
  mục sidebar "Phiếu bàn giao nhận" (con của nhóm Thu mua);
  `__tests__/ShipmentHandoverReceivedListPage.test.tsx` (5 test: danh sách + badge, navigate
  chi tiết, empty state, tìm kiếm, toast lỗi).

---

## 7. Kịch bản bổ sung — Round 09/09/2026 (nâng cấp trải nghiệm thu mua VT-04)

> Nội dung: (1) nút tắt "Tạo phiếu bàn giao" trong dropdown bảng lô hàng (chi tiết lô sản xuất);
> (2) danh sách Thu mua chỉ hiện lô **liên quan tổ chức VT-04**; (3) nút "Ghi nhận thu mua"
> chỉ hiện với lô đã xác nhận nhận (phiếu ACCEPTED); (4) UI chi tiết phiếu: bỏ nút "Quay lại"
> đầu trang + bỏ card "Lịch sử xử lý phiếu" (các trường xác nhận/từ chối/hủy/lý do dồn vào
> "Thông tin chung"); (5) chuẩn hóa tải lên chứng từ
> bằng component dùng chung `AttachmentUploader` (FE–BE đồng bộ luật file).

### TC-16 (Cao) — Tạo phiếu bàn giao từ danh sách lô hàng (VT-02)

**Điều kiện:** Frontend bản mới; đăng nhập `orgmanager/admin123` (VT-02, DEMO_HTX).

**Bước:**
1. Mở **Chi tiết lô sản xuất** (lô ACTIVATED, không tem khóa) → tab/bảng "Lô hàng".
2. Trên dòng lô ACTIVATED → dropdown hành động (icon 3 chấm) → mục **"Tạo phiếu bàn giao"**.
3. Điền form trong `CreateHandoverDialog` như TC-01 → submit.

**Kết quả mong đợi**
- Dialog mở với `shipment` đúng dòng; submit gọi `POST /shipment-handovers`; `onSuccess`
  reload danh sách lô.
- Mục này chỉ hiển thị khi **VT-02 và lô ACTIVATED** (nhất quán với trang chi tiết đơn lẻ).
- User VT-04 KHÔNG thấy mục này (backend `@PreAuthorize("hasRole('VT-02')")` chặn ở lớp lưu).

### TC-17 (Cao) — Danh sách Thu mua chỉ hiện lô liên quan tổ chức hiện tại

**Điều kiện:** Backend đã rebuild (bản `getEligibleShipments` scope org) — restart backend.

**Bước:**
1. Đăng nhập `procurement/admin123` (VT-04, DEMO_NSV) → Dashboard thu mua → danh sách lô.
2. Verify danh sách chỉ gồm: lô có **phiếu bàn giao MỚI NHẤT** cho DEMO_NSV ở trạng thái
   hoạt động (`PENDING_CONFIRMATION`/`ACCEPTED`), hoặc lô đã ghi sự kiện
   `PROCUREMENT`/`WAREHOUSE_RECEIPT` bởi DEMO_NSV.

**Kết quả mong đợi**
- Trước đây API trả **tất cả** lô ACTIVATED → giờ lọc theo org: `handover.toOrganization`
  với **phiếu mới nhất đang hoạt động** ∪ `chain_events.recorded_organization_id`
  với PROCUREMENT/WAREHOUSE_RECEIPT, `is_correction=false`.
- Lô có phiếu mới nhất bị **TỪ CHỐI (REJECTED)** → **không xuất hiện**, kể cả khi trước
  đó đã có phiếu ACCEPTED cũ (quan hệ hiện tại đã kết thúc — đúng tình huống "Nho đợt 1").
- Empty state: "Chưa có lô hàng nào được thu mua, bàn giao hoặc nhập kho cho tổ chức của bạn."
- `GET /api/v1/shipments/eligible` (VT-04) → 200; user khác + admin → 403 (`@PreAuthorize VT-04`).

### TC-18 (Trung) — Nút "Ghi nhận thu mua" hiện khi phiếu MỚI NHẤT ACCEPTED

**Bước:**
1. VT-04 mở danh sách Thu mua (TC-17): lô có **phiếu mới nhất ACCEPTED** → **có** nút
   "Ghi nhận thu mua";
2. Lô có phiếu mới nhất PENDING / lô chỉ có phiếu REJECTED (đã bị loại khỏi danh sách
   theo TC-17) → **không có** nút.

**Kết quả mong đợi**
- Điều kiện hiển thị: phiếu mới nhất `status === "ACCEPTED"` (map `handoverByShipment`).
  Backend đã loại lô có phiếu mới nhất REJECTED/EXPIRED/CANCELLED khỏi `/eligible`, nên
  FE chỉ cần dựa trên phiếu mới nhất — nhất quán backend ↔ frontend.
- Nút "Xem phiếu bàn giao" trỏ tới **phiếu mới nhất** (giữ nguyên nghiệp vụ).
- Đây là bước thủ công để ghi sự kiện `PROCUREMENT` — **bắt buộc** trước khi tổ chức nhập kho
  (`WarehouseReceiptServiceImpl` validate quan hệ thu mua). Không tự động ghi khi accept.

### TC-19 (Trung) — UI chi tiết phiếu: bỏ "Quay lại", bỏ card "Lịch sử xử lý phiếu"

**Bước:**
1. Mở `HandoverDetailPage` (bất kỳ phiếu hợp lệ) — verify:
2. **Không còn** nút "Quay lại" đầu trang; vẫn còn 2 nút cuối trang "Về trang chủ"/"Xem lô hàng".
3. **Không còn** card "Lịch sử xử lý phiếu" (trước đây đổi tên từ "Xử lý phiếu").

**Kết quả mong đợi**
- Các trường "Thời điểm xác nhận" / "Thời điểm từ chối" / "Đã hủy" / "Lý do hủy/từ chối"
  đã được dồn vào cuối card "Thông tin chung" — thông tin không bị mất, chỉ bỏ card
  hiển thị riêng (vốn chỉ ghi chép trường của chính 1 phiếu, gây nhầm lẫn về "lịch sử").

### TC-20 (Trung) — Chuẩn hóa tải lên chứng từ bằng AttachmentUploader

**Bước:**
1. Mở dialog "Tạo phiếu bàn giao" (TC-01) → vùng "Chứng từ giao hàng" là component
   `AttachmentUploader` (dashed box "Chưa có chứng từ") → chọn file:
   - File JPG/PNG/PDF ≤ 5MB → tile file + nút "Tải lên"→ sau đó "Xem"/"Gỡ chứng từ".
   - File sai loại (VD `.txt`) → báo lỗi loại file, không chọn được.
   - File > 5MB → báo "File vượt quá dung lượng cho phép (5MB)".

**Kết quả mong đợi**
- Luật tập trung tại `frontend/src/components/common/AttachmentUploader.tsx`
  (`ATTACHMENT_MIME_TYPES`, `ATTACHMENT_MAX_SIZE`, `formatFileSize`) — **đồng bộ** với backend:
  `ShipmentHandoverServiceImpl.ALLOWED_ATTACHMENT_TYPES` {jpeg,png,pdf} và
  `app.upload.handover.max-size` (mặc định 5242880, cấu hình qua `UPLOAD_HANDOVER_MAX_SIZE`).
- Reuse: component sử dụng được cho form khác bằng props `selectedFile/onFileChange/onUpload/
  uploadedInfo/onRemoveUploaded/error`.

**Ghi chú hành động (merge develop):**
- Merge trước đó (chưa commit) ghi đè `frontend/vite.config.ts` bản develop **bỏ block test** của
  vitest → mọi test FE fail vì chạy trên env `node`. Đã khôi phục `environment: 'jsdom'` +
  `setupFiles: './src/test/setup.ts'` trong `vite.config.ts` (cần giữ lại khi commit merge).

### Verify tự động (round này)

```powershell
Set-Location backend; ./mvnw -o test "-Dtest=ShipmentServiceImplTest,ShipmentHandoverServiceTest,ShipmentControllerTest"
# 53/53 pass — ShipmentServiceImplTest +2 (org-scope filter, empty khi không có org)
Set-Location ..\frontend
npx eslint .                                     # exit 0
npx tsc -b                                       # exit 0
npx vitest run                                   # 165/166 pass; 1 flaky pre-existing
                                                 # (AreaAssignmentPage TC-E2, 14/14 khi chạy riêng)
npm run build                                    # tsc -b && vite build, exit 0
```

- FE mới: `frontend/src/components/common/AttachmentUploader.tsx` (+ hằng số chuẩn tải lên);
  `CreateHandoverDialog` refactor dùng component chung (test `CreateHandoverDialog.test.tsx` giữ nguyên 12 test xanh).