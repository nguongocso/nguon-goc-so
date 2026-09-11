# NCL-08-CN-012 — Kịch bản kiểm thử thủ công (Manual E2E)

> **Nguồn dữ liệu:** `Context.md` §16.8 (DB dev `nguon_goc_so`, port 3307) — đã seed sẵn từ session trước (2026-09-07).
> **Branch kiểm tra:** `feature/NCL-08-CN-012-close-recall-case`
> **Mật khẩu chung:** `admin123`

---

## Chuẩn bị trước khi test

1. Container backend đã rebuild từ branch:
   ```
   docker compose build backend && docker compose up -d backend
   ```
2. Đăng nhập FE bằng tài khoản VT-02:
   - Tài khoản: `orgmanager` (thuộc `DEMO_HTX`)
   - Route: `/login`
3. Đảm bảo trong DB dev có ít nhất **1 `ProductionLot`** thuộc `DEMO_HTX` (`id` = `51c9be50-…` hoặc tương đương) có **≥1 `Shipment` với `status = RECALLED`**. Nếu chưa có, tạo `Shipment` liên kết với `production_lot_id` đó rồi gọi `POST /api/v1/shipments/{id}/recall` (NCL-08-CN-003) trước.

---

## TC-01: Đóng vụ việc thành công (Happy path)

**Điều kiện trước (Given):**
- `RecallCase` đã được lazy materialize khi gọi `GET /api/v1/recall-cases` (case `OPEN`, `production_lot_id` = lô DEMO_HTX).
- `Shipment` liên quan đã `RECALLED`.

**Hành động (When):**
```bash
curl -X PUT "http://localhost:8080/api/v1/recall-cases/{id}/close" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token_vt02>" \
  -d '{
    "lots": [
      {
        "shipmentId": "<shipment_recalled_id>",
        "resolution": "DESTROYED",
        "recoveredQuantity": 0,
        "note": "Tiêu hủy toàn bộ lô thu hồi."
      }
    ],
    "remediationMeasures": "Tăng kiểm nghiệm nguồn nguyên liệu, bổ sung quy trình kiểm soát chất lượng đầu vào.",
    "evidenceFileIds": []
  }'
```

**Kết quả mong đợi (Then):**
- HTTP `200 OK`
- Response `data.status` = `CLOSED`
- `data.closedBy` = `user_id` của `orgmanager`
- `data.remediationMeasures` không rỗng
- `public_warning_message` (qua `PublicTraceServiceImpl.resolveRecallMessage()`) khi tra cứu `TraceCode` của shipment đó phải chứa: `"LÔ HÀNG ĐÃ ĐƯỢC XỬ LÝ. Vụ việc thu hồi đã đóng ngày dd/MM/yyyy."`

---

## TC-02: Thiếu kết quả xử lý cho một lô (Gate validation)

**Điều kiện trước:**
- Case `OPEN` có **2 shipment `RECALLED`** nhưng `req.lots` chỉ cung cấp 1 lô.

**Hành động:**
- Gửi `PUT /close` với `lots` chỉ chứa 1 phần tử, bỏ sót shipment còn lại.

**Kết quả mong đợi:**
- HTTP `400 Bad Request`
- `message` chứa mã lô còn thiếu (ví dụ: `"Còn 1 lô chưa có kết quả xử lý: [shipment_id]"`)
- `RecallCase` vẫn `OPEN` (không đổi trạng thái)

---

## TC-03: Cảnh báo công khai không bị xóa / không bị null (QTN-09, QTN-27)

**Điều kiện trước:**
- Đã đóng case thành công (TC-01 đã pass).

**Hành động:**
1. Gọi `GET /public/trace/{codeValue}` hoặc kiểm tra qua FE (`TraceLookupPage`) cho bất kỳ `TraceCode` thuộc shipment trong case.
2. Kiểm tra `resolveRecallMessage()` trả về nội dung mới (chứa ngày đóng).

**Kết quả mong đợi:**
- `message` **không rỗng**, **không null**, **không bị xóa**.
- `TraceCode.status` vẫn `RECALLED` (không đổi về `ACTIVE`/`CANCELLED`/khác).
- `public_warning_message` trên DB (`trace_codes`) **không tồn tại** và **không bị thêm/xóa** (không `ALTER`).

---

## TC-04: Gửi thông báo cho tổ chức thu mua liên quan (Notification)

**Điều kiện trước:**
- `ChainEvent` liên quan đến các `Shipment` trong case có `recorded_organization_id` = `DEMO_HTX` (hoặc tổ chức thu mua khác nếu có).
- `NotificationService` đã được gọi trong cùng transaction đóng case.

**Hành động:**
- Sau TC-01 thành công, kiểm tra bảng `notifications` (hoặc qua UI `/notifications` với tài khoản `orgmanager` hoặc `admin`):
  ```sql
  SELECT * FROM notifications WHERE user_id IN (
    SELECT user_id FROM organization_users WHERE organization_id = '327a3a0e-...'
  ) AND content LIKE '%thu hồi%';
  ```

**Kết quả mong đợi:**
- Có bản ghi notification mới với `type` = `ALERT` (hoặc tương đương).
- Nội dung chứa thông tin vụ việc thu hồi đã đóng (`CLOSED`).
- Số lượng notification = số tổ chức thu mua liên quan (đã deduplicate qua `findDistinctProcurementOrganizationIdsByShipmentIds`), không trùng lặp.

---

## Kiểm tra FE (UI)

**Bước 1 — Danh sách vụ việc:**
- Đăng nhập `orgmanager` → `/recall-cases`
- Kiểm tra có case `OPEN` (nếu chưa đóng) hoặc `CLOSED` (sau TC-01).
- Kiểm tra `StatusBadge` hiển thị đúng (`OPEN` / `CLOSED`).
- Kiểm tra `Pagination` và `DataTableShell` không bị lỗi.

**Bước 2 — Chi tiết vụ việc:**
- Nhấn vào `id` case → `/recall-cases/{id}`
- Kiểm tra bảng kết quả xử lý từng lô (`RecallLotResult`) hiển thị đúng (`DESTROYED`/`RETURNED`/`REPROCESSED`/`UNRECOVERABLE`).
- Kiểm tra nút "Kết thúc vụ việc" mở dialog.

**Bước 3 — Dialog đóng:**
- Mở `CloseRecallCaseDialog`
- Nhập `remediationMeasures` bắt buộc (validate: rỗng → lỗi).
- Chọn `resolution` từ `Select` (4 giá trị enum).
- Nhấn xác nhận → refresh danh sách, case chuyển `CLOSED`.

---

## Dọn dẹp / Reset dữ liệu test (tùy chọn)

Nếu muốn test lại từ đầu (ví dụ: tạo case mới):
- Xóa bản ghi `RecallCase` và `RecallLotResult` mới tạo trong DB dev:
  ```sql
  DELETE FROM recall_lot_results WHERE recall_case_id = '{new_case_id}';
  DELETE FROM recall_cases WHERE id = '{new_case_id}';
  ```
- Không xóa `Shipment.RECALLED` — giữ nguyên để lazy materialize tạo lại case mới khi `GET /recall-cases`.

---

## Ghi chú cuối

- Nếu phát hiện lỗi compile mới khi rebuild container, báo ngay — tôi chỉ sửa các lỗi import (`RecallCaseStatus`, `ShipmentStatus`, `ApiResult`, DTO getters, notification stub) đã được xác nhận `compile PASS`.
- Nếu `lazy materialize` không tạo case khi `GET` (vì `Shipment.RECALLED` không tồn tại trong DB seed), cần tạo `Shipment` cho `ProductionLot` của `DEMO_HTX` trước bằng `POST /api/v1/shipments` hoặc dữ liệu seed thêm.
