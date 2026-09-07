# Kịch bản kiểm thử thủ công — NCL-04-CN-007 Yêu cầu cấp bổ sung dải mã truy xuất

> Sau thay đổi UI: **không còn trang riêng** `/code-range-supplements/create` và
> mục menu "Yêu cầu bổ sung mã". Chức năng là **dialog** mở từ:
> 1. Nút **"Cấp bổ sung mã"** trong tab **"Lô hàng & Mã QR"** của trang chi tiết lô sản xuất.
> 2. Nút **"Cấp bổ sung"** cạnh hạn mức trên màn hình **Tạo lô hàng** (`/production-lots/{lotId}/shipments/create`):
>    - hạn mức còn < 20% (cảnh báo vàng trong box hạn mức),
>    - hạn mức đã hết (cảnh báo đỏ "Hạn mức đã hết..."),
>    - **số lượng nhập vượt hạn mức còn lại** (cảnh báo đỏ ngay dưới ô số lượng).
> 3. Dialog dùng **danh sách phẳng** sự kiện thu hoạch/sơ chế của tổ chức
>    (`GET /code-range-supplement-requests/evidence-events`) — tick chọn, không
>    cần chọn lô sản xuất → lô hàng.

---

## 1. Chuẩn bị môi trường

```powershell
docker compose up -d frontend backend   # MySQL dùng instance local, host port 3307
```

- FE: `http://localhost` — BE: `http://localhost:8080`.
- Copy `frontend/.env.example` → `frontend/.env` nếu chưa có.

### Tài khoản (seed sẵn trong DB local)

| Tài khoản | Vai trò | Dùng cho |
|---|---|---|
| `admin` | VT-01 Quản trị viên nền tảng | Duyệt / từ chối yêu cầu |
| `manager` | VT-02 Quản lý HTX ABC | Tạo yêu cầu cấp bổ sung |
| `nguoighi` | VT-03 Người ghi sự kiện | Kiểm tra ẩn tùy chọn |

## 2. Dữ liệu kiểm thử (SQL trên MySQL, host port 3307)

Cột `code_ranges`: `from_number, to_number, total_limit, used_count`.
Quota = `total_limit - used_count`; BE lấy **dải mã mới nhất** của tổ chức
(`ORDER BY created_at DESC`) — với seed local là dải `SEED15` của HTX ABC
(`total_limit = 100000`).

Tổ chức HTX ABC: `d74c09f2-a1c6-11f1-9ae2-029fd41577b3`.
Kiểm tra trạng thái hiện tại:

```sql
SELECT id, prefix, total_limit, used_count, total_limit - used_count AS remaining
FROM code_ranges
WHERE organization_id = 'd74c09f2-a1c6-11f1-9ae2-029fd41577b3'
ORDER BY created_at DESC
LIMIT 1;
```

### TC-DATA-1 — Hạn mức bình thường (còn > 20%)

```sql
UPDATE code_ranges
SET used_count = 1000, updated_at = NOW()
WHERE prefix = 'SEED15';
-- remaining = 99000 / 100000 (99%)
```

### TC-DATA-2 — Hạn mức dưới 20% (NEARLY_EXHAUSTED)

```sql
UPDATE code_ranges
SET used_count = 85000, updated_at = NOW()
WHERE prefix = 'SEED15';
-- remaining = 15000 / 100000 (15%)
```

### TC-DATA-3 — Hạn mức đã hết (EXHAUSTED)

```sql
UPDATE code_ranges
SET used_count = 100000, updated_at = NOW()
WHERE prefix = 'SEED15';
-- remaining = 0
```

### Dữ liệu bằng chứng (sự kiện HARVEST / PREPROCESSING)

Cần ≥ 1 lô sản xuất của HTX ABC có lô hàng với sự kiện **Thu hoạch** hoặc
**Sơ chế**. Nếu chưa có: đăng nhập `manager` → ghi nhật ký canh tác (thu hoạch)
cho một lô, tạo lô hàng để phát sinh timeline. Kiểm tra:

```sql
SELECT ce.id, ce.event_type, ce.production_lot_id
FROM chain_events ce
WHERE ce.event_type IN ('HARVEST', 'PREPROCESSING');
```

> Sau mỗi lần duyệt thành công, `total_limit` của `SEED15` tăng thêm số lượng
> thực cấp — chạy lại script của TC-DATA tương ứng trước khi test tiếp.

### Xóa yêu cầu PENDING (reset kịch bản "một yêu cầu duy nhất")

```sql
DELETE FROM code_range_supplement_requests WHERE status = 'PENDING';
```

---

## 3. Kịch bản kiểm thử

| ID | Kịch bản | Bước | Kết quả mong đợi |
|---|---|---|---|
| TC-01 | Tùy chọn trong tab "Lô hàng & Mã QR" | Đăng nhập `manager` → chi tiết lô sản xuất → tab "Lô hàng & Mã QR" | Header có 2 nút: "Cấp bổ sung mã" + "Tạo lô hàng" |
| TC-02 | Mở dialog, hạn mức bình thường (TC-DATA-1) | Bấm "Cấp bổ sung mã" | Dialog mở tại chỗ; hiện "Còn 99.000 / 100.000 mã"; danh sách sự kiện bằng chứng phẳng (checkbox) |
| TC-03 | Cảnh báo hạn mức thấp / hết | Chạy TC-DATA-2 rồi TC-DATA-3, mở lại dialog và trang Tạo lô hàng | <20%: cảnh báo vàng trong box hạn mức + nút "Cấp bổ sung"; =0: form khóa + cảnh báo đỏ "Hạn mức đã hết..." + nút "Cấp bổ sung" |
| TC-04 | Gửi yêu cầu hợp lệ | Trong dialog: SL = 500, lý do, tick ≥1 sự kiện Thu hoạch/Sơ chế → Gửi | Toast thành công; dòng mới trong bảng "Yêu cầu của tổ chức" với trạng thái "Chờ duyệt" |
| TC-05 | Validation | Bỏ trống lý do / SL = 0 / chưa tick sự kiện → Gửi | Chặn gửi, hiện đúng thông báo lỗi zod (lỗi đầu tiên) |
| TC-06 | Trùng yêu cầu PENDING | Có sẵn 1 yêu cầu "Chờ duyệt", gửi thêm 1 yêu cầu nữa | Toast lỗi BE: tổ chức đã có yêu cầu đang chờ duyệt |
| TC-07 | Lối tắt ở trang Tạo lô hàng | Vào Tạo lô hàng khi hạn mức <20% hoặc =0 | Nút "Cấp bổ sung" cạnh hạn mức → mở dialog (không rời trang) |
| TC-08 | Route cũ đã xóa | Truy cập trực tiếp `/code-range-supplements/create` | Không còn trang (router không match — fallback) |
| TC-09 | Duyệt toàn bộ / một phần (VT-01) | Đăng nhập `admin` → `/admin/code-range-supplements` → duyệt 300/500 | Trạng thái "Đã duyệt", SL thực cấp 300; `total_limit` của SEED15 +300 (check SQL); `manager` nhận thông báo |
| TC-10 | Từ chối (VT-01) | Từ chối yêu cầu kèm lý do | Trạng thái "Đã từ chối"; `manager` nhận thông báo |
| TC-11 | Quyền | Đăng nhập `nguoighi` (VT-03) → tab "Lô hàng & Mã QR" | Không thấy nút "Cấp bổ sung mã"; POST API trực tiếp → 403 |
| TC-12 | Ranh giới tổ chức (QTN-01) | `manager` của HTX khác (nếu có) xem `/admin/code-range-supplements` → vào chi tiết yêu cầu của HTX ABC | Bị chặn (400/403, không xem được dữ liệu tổ chức khác) |
| TC-13 | Nhập số lượng vượt hạn mức | TC-DATA-1 (còn 99.000), vào Tạo lô hàng, nhập SL = 100.000 | Ngay khi nhập hiện **cảnh báo đỏ** "Hạn mức đã hết / không đủ mã. Chỉ còn 99.000 mã truy xuất…" + nút "Cấp bổ sung"; nút "Tạo lô hàng" bị khóa |
| TC-14 | Hết hạn mức mở trang tạo lô hàng | TC-DATA-3, mở Tạo lô hàng | Form khóa, cảnh báo đỏ "Hạn mức đã hết..." + nút "Cấp bổ sung" cạnh hạn mức |
| TC-15 | Giảm số lượng về mức hợp lệ | TC-13 → sửa SL xuống 1.000 | Cảnh báo đỏ tự ẩn, tạo lô hàng bình thường |
| TC-16 | Nút trong cảnh báo mở dialog | TC-13 → bấm "Cấp bổ sung" trong cảnh báo đỏ | Dialog mở đúng tại trang, không điều hướng |
| TC-17 | Bằng chứng phẳng (endpoint mới) | Mở dialog → xem danh sách sự kiện | Danh sách phẳng sự kiện Thu hoạch/Sơ chế của tổ chức, mới nhất trước, có tên lô (nếu có); không còn 2 select lô/lô hàng |
| TC-18 | CTA ẩn khi lô bị chặn tạo lô hàng | Lô sản xuất chưa đạt kiểm nghiệm/hủy/loại bỏ → mở trang Tạo lô hàng | Không hiện nút "Cấp bổ sung" (chỉ hiện khối đỏ "Không thể tạo lô hàng") |

## 4. Kết quả chạy tự động

| Kiểm tra | Kết quả |
|---|---|
| `npm run build` (tsc + vite) | PASS |
| `npm run lint` | PASS (0 lỗi) |
