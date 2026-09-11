# Chuẩn bị kiểm thử thủ công — NCL-08-CN-012

> Phạm vi: chỉ NCL-08-CN-012 (Kết thúc vụ việc thu hồi). Không sửa mã nguồn, không đổi API contract. Chỉ chuẩn bị dữ liệu, kịch bản, môi trường.
> Môi trường bắt buộc: **Backend chạy bằng Docker** (build + container), **Frontend chạy bằng `npm run dev`** (không dùng build production, không dùng pm2, không dùng Vercel).
> Branch kiểm thử: `feature/NCL-08-CN-012-close-recall-case`
> Migration cần có: `V20260910000000` (`recall_cases`, `recall_lot_results`)

---

## Phần 1 — Dữ liệu kiểm thử

### A. Tổ chức và người dùng

| Nhóm dữ liệu | Mã / Tên | Thuộc tính | Giá trị / Ghi chú |
|---|---|---|---|
| Tổ chức A (HTX sở hữu vụ việc) | `ORG-A` — HTX DEMO | Loại | Hợp tác xã (VT-02) |
| Tổ chức B (doanh nghiệp thu mua) | `ORG-B` — NitroFresh | Loại | Doanh nghiệp thu mua (VT-04) |
| Tổ chức C (tổ chức khác — test cách ly) | `ORG-C` — GreenCoop | Loại | Hợp tác xã khác (VT-02) |
| Tài khoản QL HTX A | `orgmanager` / `admin123` | Vai trò | VT-02; Tổ chức `ORG-A` |
| Tài khoản Ghi sự kiện A | `eventrec` / `admin123` | Vai trò | VT-03; Tổ chức `ORG-A` |
| Tài khoản QL HTX C | `orgmanager_c` / `admin123` | Vai trò | VT-02; Tổ chức `ORG-C` |
| Tài khoản Thu mua B | `procurement_b` / `admin123` | Vai trò | VT-04; Tổ chức `ORG-B` |

### B. Dữ liệu nền (phải nạp trước vụ việc)

| Nhóm | Mã | Tên / Nội dung | Thuộc tính chính | Giá trị mẫu | Ghi chú |
|---|---|---|---|---|---|
| Vùng trồng | `FARM-A-01` | Ruộng số 1 — HTX DEMO | Tổ chức | `ORG-A`; Loại cây | Nông sản chung |
| Lô sản xuất | `LOT-A-01` | Lô SX-2026-08-A | Vùng trồng | `FARM-A-01`; Trạng thái | Hoạt động |
| Lô hàng (Shipment) | `SHP-A-01` | Lô hàng A-01 | Lô sản xuất | `LOT-A-01`; Status | `RECALLED` (đã thu hồi) |
| Lô hàng (Shipment) | `SHP-A-02` | Lô hàng A-02 | Lô sản xuất | `LOT-A-01`; Status | `RECALLED` |
| Lô hàng (Shipment) | `SHP-A-03` | Lô hàng A-03 | Lô sản xuất | `LOT-A-01`; Status | `RECALLED` |
| Tem đã kích hoạt | `TRC-A-01` | Mã tem A-01 | Lô hàng | `SHP-A-01`; Trạng thái | `ACTIVE` |
| Sự kiện chuỗi | `EVT-H1` | Harvest | Lô sản xuất | `LOT-A-01`; Loại | `HARVEST` |
| Sự kiện chuỗi | `EVT-P1` | Packaging | Lô sản xuất | `LOT-A-01`; Loại | `PACKAGING` |
| Sự kiện chuỗi | `EVT-T1` | Transport | Lô hàng | `SHP-A-01`; Tổ chức thu mua | `ORG-B` |
| Sự kiện chuỗi | `EVT-PR1` | Procurement | Lô hàng | `SHP-A-01`; Bên thu mua | `ORG-B` |

**Thứ tự nạp qua màn hình frontend `npm run dev` (theo đúng đường dẫn trong docs/codebase-map.md):**
1. Đăng nhập `orgmanager` → `/organizations` (kiểm tra `ORG-A`, `ORG-B`, `ORG-C` đã có).
2. `/farm-areas` → tạo `FARM-A-01` thuộc `ORG-A`.
3. `/production-lots` → tạo `LOT-A-01` thuộc `FARM-A-01`.
4. `/farm-logs/create` → ghi nhật ký tối thiểu cho `LOT-A-01` (để lô hợp lệ).
5. `/shipments` → tạo `SHP-A-01`, `SHP-A-02`, `SHP-A-03` từ `LOT-A-01`; kích hoạt tem `TRC-A-01`; đặt status `RECALLED` (hoặc dùng endpoint thu hồi nếu có `POST /shipments/{id}/recall`).
6. `/chain-events` (hoặc qua `transport-event`, `packaging-event`) → ghi sự kiện cho `SHP-A-01` với `recordedOrganizationId = ORG-B`.

**Nếu dùng DB trực tiếp (check danh sách đã có trong seed `data/`):**
- Kiểm tra bảng `organizations`, `users`, `production_lots`, `shipments`, `trace_codes`, `chain_events` trong `nguon_goc_so` (MySQL port 3307 theo docs/testing).
- Nếu chưa có `RECALLED`, thực hiện qua API `POST /api/v1/shipments/{id}/recall` (NCL-08-CN-003) hoặc insert trực tiếp `UPDATE shipments SET status = 'RECALLED' ...` trong transaction.

### C. Vụ việc thu hồi đang mở (điều kiện tiên quyết)

| Mã vụ việc | Loại kiểm thử | Trạng thái hiện tại | Lý do thu hồi | Ngày mở | Người mở | Lô trong phạm vi (3 lô cho V1) |
|---|---|---|---|---|---|---|
| `CASE-V1` | TC-01 (thành công) | `OPEN` | "Phát hiện dư lượng hóa chất vượt ngưỡng" | 2026-09-09 | `orgmanager` | `SHP-A-01`, `SHP-A-02`, `SHP-A-03` |
| `CASE-V2` | TC-02 (thiếu kết quả) | `OPEN` | "Nghi ngờ nhiễm khuẩn" | 2026-09-08 | `orgmanager` | `SHP-B-01`, `SHP-B-02`, `SHP-B-03` |
| `CASE-V3` | TC-03 (đã đóng) | `CLOSED` (đã đóng) | "Kết quả kiểm nghiệm không đạt" | 2026-09-07 | `orgmanager` | `SHP-C-01` |
| `CASE-V4` | TC-04 (thông báo) | `OPEN` | "Thu hồi theo yêu cầu siêu thị" | 2026-09-10 | `orgmanager` | `SHP-A-04`, `SHP-A-05` |

**Kết quả xử lý đã nhập cho V1 (để test TC-01):**

| Lô | Kết quả | Số lượng thu hồi được | Ghi chú | Tệp biên bản (mã mẫu) |
|---|---|---|---|---|
| `SHP-A-01` | `DESTROYED` | 1000 (bằng tổng) | "Tiêu hủy toàn bộ theo quy định" | `FILE-B01` |
| `SHP-A-02` | `RETURNED` | 800 | "Trả lại cho nhà sản xuất" | `FILE-B02` |
| `SHP-A-03` | `REPROCESSED` | 600 | "Xử lý lại tại nhà máy" | — |

**Biện pháp khắc phục đã nhập cho V1:** `"Tăng tần suất kiểm nghiệm nguồn nguyên liệu; bổ sung quy trình kiểm soát chất lượng đầu vào; đào tạo nhân viên nhận diện sớm."`

**Kết quả xử lý cho V2 (để test TC-02 — còn thiếu 1 lô):**
- `SHP-B-01`: `DESTROYED`, qty = 500, ghi chú = "...", file = `FILE-B05`
- `SHP-B-02`: `RETURNED`, qty = 300, ghi chú = "..."
- `SHP-B-03`: **chưa nhập** (resolution = null, qty = null)

**Kết quả xử lý cho V3 (đã đóng):**
- `SHP-C-01`: `UNRECOVERABLE`, qty = 0, ghi chú = "Không thu hồi được do lô đã phân phối hết", file = `FILE-B10`
- Biện pháp khắc phục: `"Rà soát chuỗi phân phối; tăng kiểm soát tại điểm bán."`
- `closedAt` = 2026-09-07 14:00; `status` = `CLOSED`; cảnh báo công khai đang hiển thị `"LÔ HÀNG ĐÃ XỬ LÝ XONG. Vụ việc thu hồi đã đóng ngày 07/09/2026."` (theo `PublicTraceServiceImpl.resolveRecallMessage()`)

**Dữ liệu V4 (TC-04):**
- `SHP-A-04`: `RECALLED`, tên = "Lô hàng A-04", số lượng = 1200
- `SHP-A-05`: `RECALLED`, tên = "Lô hàng A-05", số lượng = 900
- `ChainEvent` liên quan có `recordedOrganizationId = ORG-B` (để `NotificationService` tìm được người nhận)
- `remediationMeasures` đã nhập; 2 lô đã có kết quả → có thể đóng để test thông báo

### D. Dữ liệu ngoại lệ (âm)

| Mã vụ việc | Trạng thái | Mục đích kịch bản | Chi tiết lỗi cần tạo / kiểm tra |
|---|---|---|---|
| `CASE-V5` | `OPEN` | Chặn đóng: thiếu lô + thiếu biện pháp | 3 lô `RECALLED`; chỉ 1 lô có kết quả; `remediationMeasures` = rỗng → cả 2 lỗi |
| `CASE-V6` | `OPEN` | Chặn khi "không thu hồi được" thiếu lý do | `SHP-V6-01`: resolution = `UNRECOVERABLE`; `notes` = rỗng → phải chặn |
| `CASE-V7` | `OPEN` | Chặn khi qty vượt số lượng lô | `SHP-V7-01`: resolution = `DESTROYED`; `recoveredQuantity` = 5000 (lớn hơn `totalQuantity` = 1000) → chặn |
| `CASE-V8` | `OPEN` | Test cách ly tổ chức | Thuộc `ORG-C`; `orgmanager_c` cố đóng → từ chối (403 / không thấy) |
| `CASE-V9` | `CLOSED` | Chặn sửa kết quả sau khi đóng | Đã đóng; cố gọi `PUT .../close` lại → `MSG_CASE_ALREADY_CLOSED` |
| `CASE-V10` | `CLOSED` | Chặn ẩn cảnh báo công khai | Đã đóng; tìm cách ẩn (không có endpoint ẩn) nhưng kiểm tra `PublicTraceServiceImpl` vẫn trả message |

---

## Phần 2 — Kịch bản kiểm thử thủ công

> Mỗi kịch bản dùng tài khoản đã nêu ở Phần 1. Giao diện kiểm thử: frontend chạy `npm run dev` (URL ví dụ `http://localhost:5173`, xác nhận bằng terminal). Backend: container Docker expose port `8080` (ví dụ `http://localhost:8080`). Trang tra cứu công khai: `http://localhost:3080` hoặc đường dẫn công khai từ `PublicTraceServiceImpl`.
> Không viết test tự động; chỉ mô tả thao tác trên màn hình.

### Nhóm 1 — Luồng thành công (Cao)

| Mã | Tên | Mục tiêu | Given | When (từng bước) | Then | Dữ liệu | Ưu tiên | AC / QTN |
|---|---|---|---|---|---|---|---|---|
| MTC-01 | Đóng vụ việc khi đủ kết quả (TC-01) | Xác nhận đóng thành công khi 3 lô có kết quả | Đăng nhập `orgmanager` (`ORG-A`). Mở `/recall-cases`. Tìm `CASE-V1`. Trạng thái `OPEN` | 1. Nhấn vào `CASE-V1` → `/recall-cases/CASE-V1`. 2. Nhấn nút "Kết thúc vụ việc" → `CloseRecallCaseDialog`. 3. Chọn `DESTROYED` cho `SHP-A-01`; nhập qty = 1000. 4. Chọn `RETURNED` cho `SHP-A-02`; qty = 800. 5. Chọn `REPROCESSED` cho `SHP-A-03`; qty = 600. 6. Nhập `remediationMeasures`. 7. Nhấn "Xác nhận kết thúc vụ việc" | Trả `200`. Trạng thái vụ việc `CLOSED`. `closedBy` = `orgmanager`. `lotResults` đầy đủ. Không còn `pendingLotCount`. | `CASE-V1`, `SHP-A-01/02/03`, `orgmanager` | Cao | TC-01, QTN-27 |
| MTC-02 | Đổi cảnh báo công khai sau đóng (TC-01) | Kiểm tra thông điệp công khai đổi sang "đã xử lý xong" | Sau MTC-01 thành công | 1. Mở trang tra cứu công khai (từ `TraceLookupPage` hoặc URL công khai). 2. Nhập mã `TRC-A-01` (tem của `SHP-A-01`). 3. Quan sát phần cảnh báo | Nội dung chứa `"LÔ HÀNG ĐÃ XỬ LÝ XONG. Vụ việc thu hồi đã đóng ngày 09/09/2026."` (hoặc ngày đóng thực tế). Không bị ẩn. `TraceCode.status` vẫn `RECALLED`. | `CASE-V1` (đã đóng), `TRC-A-01` | Cao | TC-01, TC-03, QTN-09 |
| MTC-03 | Gửi thông báo kết thúc cho thu mua (TC-04) | Kiểm tra doanh nghiệp B nhận thông báo | Sau MTC-01; `ChainEvent` cho `SHP-A-04/05` liên quan `ORG-B` | 1. Đăng nhập `procurement_b` (`ORG-B`). 2. Mở `/notifications`. 3. Quan sát thông báo mới | Có thông báo với nội dung liên quan đến `CASE-V4` (hoặc `CASE-V1` nếu có liên kết `ORG-B`). Nếu thiếu `ChainEvent` cho `ORG-B`, cần tạo trước (xem Phần 1). | `CASE-V4`, `ORG-B`, `procurement_b` | Cao | TC-04 |
| MTC-04 | Xuất hồ sơ sự cố sau đóng | Xuất hồ sơ kèm truy xuất | `CASE-V1` đã đóng (`CLOSED`) | 1. Từ `/recall-cases/CASE-V1`, tìm nút/ chức năng xuất hồ sơ (nếu có). Nếu chưa có UI, kiểm tra qua `DossierController` hoặc `ReportService` với `resource_type = RECALL_CASE`. 2. Nếu không có sẵn, ghi nhận "Cần xác nhận" và kiểm tra DB trực tiếp: `SELECT * FROM recall_cases WHERE id = 'CASE-V1'` + `SELECT * FROM recall_lot_results WHERE recall_case_id = 'CASE-V1'` | Hồ sơ chứa đủ: vụ việc, danh sách lô, kết quả xử lý, biện pháp khắc phục, biên bản (`FILE-B01/02`), dòng sự kiện liên quan. Nếu không có chức năng xuất → ghi "Cần xác nhận". | `CASE-V1`, `FILE-B01/02` | Cao | AC 13 |
| MTC-05 | Ghi biện pháp khắc phục vào hồ sơ | Xác nhận biện pháp hiển thị | `CASE-V1` đã đóng | 1. Mở chi tiết `CASE-V1`. 2. Quan sát phần `remediationMeasures`. | Nội dung đúng: `"Tăng tần suất kiểm nghiệm..."`. Không bị cắt bỏ. | `CASE-V1` | Cao | QTN-27 |
| MTC-06 | Ghi kết quả "không thu hồi được" kèm lý do | Xác nhận `UNRECOVERABLE` + lý do | Tạo `CASE-V6` hoặc dùng `SHP-V6-01` trong `CASE-V6` | 1. Đăng nhập `orgmanager`. 2. Mở `CASE-V6`. 3. Chọn `UNRECOVERABLE` cho `SHP-V6-01`; nhập `notes` = "Lô đã phân phối hết"; qty = 0. 4. Nhập `remediationMeasures`. 5. Nhấn đóng. | Đóng thành công; `resolution` = `UNRECOVERABLE`; `notes` không rỗng. | `CASE-V6`, `SHP-V6-01` | Cao | TC-01, QTN-27 |

### Nhóm 2 — Ngoại lệ (Trung bình / Cao)

| Mã | Tên | Mục tiêu | Given | When | Then | Dữ liệu | Ưu tiên | AC / QTN |
|---|---|---|---|---|---|---|---|---|
| MTC-07 | Chặn đóng khi còn lô thiếu kết quả (TC-02) | Liệt kê lô còn thiếu | Đăng nhập `orgmanager`; `CASE-V2` `OPEN`; 2 lô có kết quả (`SHP-B-01`, `SHP-B-02`); `SHP-B-03` chưa nhập | 1. Mở `CASE-V2`. 2. Nhấn "Kết thúc vụ việc". 3. Nhập đủ 2 lô đã có + biện pháp khắc phục. 4. Nhấn xác nhận. | Chặn; `400 Bad Request`; `message` chứa `"Còn 1 lô chưa có kết quả xử lý: SHP-B-03"` (hoặc tên lô). `CASE-V2` vẫn `OPEN`. | `CASE-V2`, `SHP-B-03` | Cao | TC-02, QTN-27 |
| MTC-08 | Chặn khi thiếu biện pháp khắc phục | Từ chối đóng nếu `remediationMeasures` rỗng | `CASE-V5`; 2 lô có kết quả; `remediationMeasures` = rỗng | 1. Mở `CASE-V5`. 2. Nhập kết quả cho đủ lô. 3. Để trống `remediationMeasures`. 4. Nhấn xác nhận. | Chặn; thông báo `"Biện pháp khắc phục phòng ngừa là bắt buộc trước khi đóng vụ việc."`; không đổi `status`. | `CASE-V5` | Cao | TC-01, QTN-27 |
| MTC-09 | Chặn khi qty âm | Đồng ý chỉ khi `qty >= 0` | `CASE-V1` (đang mở lại thử) | 1. Nhập `recoveredQuantity` = `-10` cho một lô. 2. Nhấn xác nhận. | Chặn; `message` nói qty không được âm (`@DecimalMin(0.0)`). | `CASE-V1` | Trung bình | QTN-27 |
| MTC-10 | Chặn khi qty vượt số lượng lô | Đồng ý chỉ khi `qty <= totalQuantity` | Dùng `SHP-A-01` (tổng = 1000) | 1. Nhập `recoveredQuantity` = `9999`. 2. Nhấn xác nhận. | Chặn; `MSG_QUANTITY_RANGE`; không đóng. | `CASE-V1`, `SHP-A-01` | Trung bình | QTN-27 |
| MTC-11 | Chặn khi `UNRECOVERABLE` thiếu lý do | Bắt buộc `notes` | Dùng `CASE-V6`; chọn `UNRECOVERABLE` | 1. Nhận kết quả `UNRECOVERABLE`; để `notes` = rỗng. 2. Nhấn xác nhận. | Chặn; `MSG_UNRECOVERABLE_REASON_REQUIRED`. | `CASE-V6`, `SHP-V6-01` | Cao | QTN-27 |
| MTC-12 | Chặn sửa kết quả sau khi đã đóng | Không cho cập nhật `RecallLotResult` | `CASE-V9` đã `CLOSED` | 1. Đăng nhập `orgmanager`. 2. Mở `CASE-V9`. 3. Thử sửa `recoveredQuantity` hoặc `resolution`. 4. Nếu có nút sửa, nhấn; nếu không có, kiểm tra API `PUT` trả lỗi. | Không cho sửa; hoặc API trả `400` / `BusinessException`. `status` vẫn `CLOSED`. | `CASE-V9` | Trung bình | QTN-27 |
| MTC-13 | Chặn ẩn cảnh báo công khai (TC-03) | Không cho ẩn; message vẫn hiển thị | `CASE-V3` đã đóng; `TRC-C-01` của `SHP-C-01` | 1. Mở trang tra cứu công khai; nhập `TRC-C-01`. 2. Kiểm tra phần cảnh báo. 3. Tìm xem có nút "Ẩn cảnh báo" hay không. | Không có nút ẩn. Message vẫn hiển thị `"LÔ HÀNG ĐÃ XỬ LÝ XONG..."`. `TraceCode.status` không đổi về `ACTIVE`. | `CASE-V3`, `TRC-C-01` | Cao | TC-03, QTN-09 |

### Nhóm 3 — Phân quyền và cách ly dữ liệu (Trung bình)

| Mã | Tên | Mục tiêu | Given | When | Then | Dữ liệu | Ưu tiên | QTN |
|---|---|---|---|---|---|---|---|---|
| MTC-14 | Cách ly: người C không thấy vụ việc A | Từ chối truy cập | Đăng nhập `orgmanager_c` (`ORG-C`) | 1. Mở `/recall-cases`. 2. Tìm `CASE-V1`. | Không thấy `CASE-V1` (hoặc danh sách rỗng). Nếu cố truy cập `/recall-cases/CASE-V1` → `404` / `MSG_CASE_NOT_FOUND`. | `orgmanager_c`, `CASE-V1` | Cao | QTN-01 |
| MTC-15 | Ghi sự kiện không được đóng | Từ chối vì thiếu quyền | Đăng nhập `eventrec` (`ORG-A`, VT-03) | 1. Mở `/recall-cases/CASE-V1`. 2. Thử nhấn "Kết thúc vụ việc". | Nút không hiển thị hoặc API trả `403` (`MSG_NO_PERMISSION`). | `eventrec`, `CASE-V1` | Trung bình | QTN-22 |
| MTC-16 | Doanh nghiệp thu mua chỉ xem / nhận thông báo | Không được đóng | Đăng nhập `procurement_b` (`ORG-B`, VT-04) | 1. Mở `/recall-cases`. 2. Thử đóng `CASE-V4`. | Không có quyền; hoặc danh sách chỉ hiển thị các case liên quan `ORG-B` (nếu có). Không thể đóng. | `procurement_b`, `CASE-V4` | Trung bình | QTN-01 |

### Nhóm 4 — Lưu vết (Trung bình)

| Mã | Tên | Mục tiêu | Given | When | Then | Dữ liệu | Ưu tiên | QTN |
|---|---|---|---|---|---|---|---|---|
| MTC-17 | Ghi lịch sử đóng vụ việc | `CLOSE_RECALL_CASE` trong `activity_logs` | Sau MTC-01 | 1. Đăng nhập `orgmanager`. 2. Mở `/activity-logs`. 3. Tìm hành động gần nhất. | Có dòng `action = CLOSE_RECALL_CASE`; `entityType = RECALL_CASE`; `entityId = CASE-V1`; `username = orgmanager`. | `CASE-V1`, `orgmanager` | Trung bình | QTN-08 |
| MTC-18 | Lịch sử đổi trạng thái lô | Lô chuyển "đã thu hồi và đã đóng" | `CASE-V1` đã đóng | Kiểm tra DB / màn hình chi tiết: `RecallLotResult` cho `SHP-A-01/02/03` tồn tại; `Shipment.status` giữ `RECALLED` (không đổi về `ACTIVE`). | `RecallLotResult` đầy đủ; `RecallCaseStatus = CLOSED`. Không có sửa `RecallLotResult` sau đóng. | `CASE-V1` | Trung bình | QTN-08 |
| MTC-19 | Lịch sử đổi nội dung cảnh báo | `PublicTraceServiceImpl` lưu vết qua `closedAt` | `CASE-V3` đã đóng | Kiểm tra `PublicTraceResponse.recallMessage` qua trang tra cứu công khai cho `TRC-C-01`. | Message chứa ngày `07/09/2026`. Không có bản ghi sửa message trực tiếp. | `CASE-V3`, `TRC-C-01` | Trung bình | QTN-08, QTN-09 |

### Nhóm 5 — Tích hợp (Cao / Trung bình)

| Mã | Tên | Mục tiêu | Given | When | Then | Dữ liệu | Ưu tiên | AC |
|---|---|---|---|---|---|---|---|---|
| MTC-20 | Trang tra cứu công khai hiển thị đúng sau đóng | Quan sát `recallMessage` | `CASE-V3` đã đóng | 1. Mở trang tra cứu công khai; nhập `TRC-C-01`. 2. Quan sát phần cảnh báo. | Nội dung `"LÔ HÀNG ĐÃ XỬ LÝ XONG. Vụ việc thu hồi đã đóng ngày 07/09/2026."`; không null; không rỗng. | `TRC-C-01`, `CASE-V3` | Cao | TC-03 |
| MTC-21 | Thông báo hiển thị đúng cho thu mua | Kiểm tra `NotificationService` | `CASE-V4` đã đóng (sau MTC-03) | 1. Đăng nhập `procurement_b`. 2. Mở `/notifications`. | Có thông báo mới liên quan `CASE-V4`; nội dung chứa thông tin đóng vụ việc. | `CASE-V4`, `procurement_b` | Cao | TC-04 |
| MTC-22 | Hồ sơ sự cố xuất đủ thông tin | Kiểm tra output / DB | `CASE-V1` đã đóng | 1. Từ `/recall-cases/CASE-V1`, tìm chức năng xuất (nếu có UI). Nếu không có UI sẵn, kiểm tra DB / API `GET /api/v1/recall-cases/CASE-V1` và so sánh với yêu cầu. | Response chứa: `id`, `status`, `lotResults[]`, `remediationMeasures`, `evidenceFileIds[]`, `closedAt`, `closedBy`. Nếu thiếu phần xuất file → ghi "Cần xác nhận / đề xuất ngoài phạm vi". | `CASE-V1` | Cao | AC 13 |

---

## Phần 3 — Môi trường kiểm thử (theo đúng 2 cơ chế)

### A. Thông tin môi trường

| Hạng mục | Giá trị / Hướng dẫn |
|---|---|
| **Branch cần kiểm thử** | `feature/NCL-08-CN-012-close-recall-case` (từ repo `nguon-goc-so`). Kiểm tra `git branch` và `git log --oneline -3` để xác nhận. |
| **Backend — Docker** | `docker build -t nguongocso-backend ./backend` → `docker run -d --name ngs-backend --env-file .env -p 8080:8080 nguongocso-backend` (hoặc dùng `docker-compose up -d backend`). Xác nhận `docker ps`; gọi `curl http://localhost:8080/actuator/health` (nếu có) hoặc kiểm tra log `docker logs -f ngs-backend`. |
| **Frontend — `npm run dev`** | `cd frontend` → `npm install` → `npm run dev`. Cổng mặc định Vite: `http://localhost:5173` (xác nhận từ terminal). Nếu khác, xem dòng `Local: http://localhost:...`. |
| **API Base (frontend → backend)** | `VITE_API_URL` trong `.env` / `frontend/.env` phải trỏ `http://localhost:8080` (hoặc `http://backend:8080` nếu frontend cũng chạy trong Docker — nhưng yêu cầu là `npm run dev` nên dùng `localhost`). |
| **DB trực tiếp (nếu cần kiểm tra)** | MySQL host `localhost`, port theo `.env` (`MYSQL_HOST_PORT`, ví dụ `3307`). DB name `nguon_goc_so`. User/password từ `.env`. Công cụ: DBeaver, MySQL Workbench, hoặc `mysql -h localhost -P 3307 -u ... -p`. |
| **Trang tra cứu công khai** | URL công khai từ `PublicTraceServiceImpl.getPublicTrace()` (thường qua `frontend/src/pages/public/TraceLookupPage.tsx` hoặc endpoint `/public/trace/{codeValue}`). Kiểm tra qua `http://localhost:5173` → tìm trang tra cứu; hoặc kiểm tra trực tiếp API public từ backend. |
| **Trình duyệt đề xuất** | Chromium/Chrome hoặc Edge (để dev tools dễ kiểm tra Network / Console). Mữi giờ hệ thống: `Asia/Ho_Chi_Minh` (`APP_TIMEZONE` từ `.env`). |
| **Thời gian kiểm thử** | Nên thực hiện trong khung giờ có thể kiểm tra `closedAt` rõ ràng; không cần đặc biệt. |

### B. Tài khoản và quyền cho từng kịch bản

| Tài khoản | Vai trò | Tổ chức | Dùng cho kịch bản | Mật khẩu |
|---|---|---|---|---|
| `orgmanager` | VT-02 | `ORG-A` | MTC-01, 02, 07, 08, 09, 10, 11, 12, 17, 18 | `admin123` |
| `procurement_b` | VT-04 | `ORG-B` | MTC-03, MTC-21 | `admin123` |
| `orgmanager_c` | VT-02 | `ORG-C` | MTC-14 | `admin123` |
| `eventrec` | VT-03 | `ORG-A` | MTC-15 | `admin123` |

### C. Dữ liệu nạp sẵn — thứ tự tối ưu qua frontend dev

> Thực hiện trên `http://localhost:5173` (frontend `npm run dev`) sau khi backend Docker đã up và DB đã sẵn sàng.

| Bước | Màn hình / Route | Thao tác | Dữ liệu cần tạo (tham chiếu Phần 1) |
|---|---|---|---|
| 1 | Đăng nhập `/login` | `orgmanager` / `admin123` | Kiểm tra vào `ORG-A` |
| 2 | `/organizations` (nếu cần) | Xác ít nhất `ORG-A`, `ORG-B`, `ORG-C` | Đã có từ seed |
| 3 | `/farm-areas` | Tạo `FARM-A-01` | Thuộc `ORG-A` |
| 4 | `/production-lots` | Tạo `LOT-A-01`; chọn `FARM-A-01`; nhập loại nông sản; trạng thái = Hoạt động | Loại cây theo `FARM-A-01` |
| 5 | `/farm-logs/create` | Ghi nhật ký cho `LOT-A-01` (tối thiểu 1 dòng: thu hoạch) | Để lô hợp lệ cho các event tiếp |
| 6 | `/shipments` | Tạo `SHP-A-01`, `SHP-A-02`, `SHP-A-03`; liên kết `LOT-A-01`; số lượng = 1000/900/800; kích hoạt tem; sau đó đặt/đẩy `RECALLED` (qua API hoặc màn hình thu hồi nếu có) | Để lazy materialize tạo `CASE-V1` |
| 7 | `/chain-events` (hoặc các tab con) | Ghi `HARVEST` (`EVT-H1`), `PACKAGING` (`EVT-P1`), `TRANSPORT` (`EVT-T1` với `ORG-B`) cho các lô | Để `NotificationService` tìm được `ORG-B` |
| 8 | `/recall-cases` | Kiểm tra `CASE-V1` đã xuất hiện (lazy materialize). Nếu chưa, refresh. | Xác nhận trạng thái `OPEN`; 3 lô trong `lotResults`; `pendingLotCount` = 3 (nếu chưa nhập kết quả) |
| 9 | `CloseRecallCaseDialog` (from `/recall-cases/CASE-V1`) | Nhập kết quả cho 3 lô + `remediationMeasures` (MTC-01) | Để tạo dữ liệu V1 hoàn chỉnh |

**Nếu dùng seed sẵn / DB trực tiếp:**
- Kiểm tra `SELECT * FROM recall_cases WHERE case_code LIKE 'RC-%';` để xem `CASE-V1` đã tồn tại chưa.
- Nếu chưa, gọi `GET /api/v1/recall-cases` vài lần để lazy materialize tạo.
- Nếu cần tạo nhanh: `INSERT INTO recall_cases (id, case_code, production_lot_id, organization_id, status, created_at, updated_at) VALUES (...)` + `INSERT INTO recall_lot_results ...` (chỉ dùng khi không có UI seed).

### D. Cấu hình cần thiết

| Cấu hình | Giá trị cần kiểm tra / thiết lập | Cách kiểm tra |
|---|---|---|
| DB connection | `.env` hoặc `docker-compose` phải có `DB_HOST=localhost`, `DB_PORT=3307`, `DB_NAME=nguon_goc_so` | `cat .env \| grep DB_`; `docker-compose config` |
| JWT | `JWT_SECRET`, `JWT_EXPIRATION` phải hợp lệ | Đăng nhập được → token hoạt động |
| Thông báo | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`; nhưng `NotificationService` cũng gửi trong hệ thống (`notifications` bảng) | Kiểm tra `NotificationService` dùng cả email và in-app; không bắt buộc email phải gửi thành công cho MTC-03 |
| Upload / Tệp biên bản | `UPLOAD_BASE_DIR`, `UPLOAD_FARM_LOG_MAX_SIZE`, `UPLOAD_CERTIFICATION_MAX_SIZE`; cần thư mục `uploads/` tồn tại trong container và được mount (`volumes` trong docker-compose) | `ls -la backend/uploads/`; kiểm tra `evidenceFileIds` lưu đúng UUID |
| Múi giờ | `APP_TIMEZONE=Asia/Ho_Chi_Minh`; `TimeConfig` dùng `Asia/Ho_Chi_Minh` | Kiểm tra `closedAt` hiển thị đúng định dạng `dd/MM/yyyy` |
| Cổng frontend | `npm run dev` thường `5173`; nếu `FRONTEND_HOST_PORT` khác (ví dụ `3080`) thì dùng theo `docker-compose` hoặc terminal | Xem dòng `Local:` sau `npm run dev` |

### E. Công cụ hỗ trợ cho kiểm thử

| Công cụ | Mục đích | Chuẩn bị |
|---|---|---|
| Tệp biên bản mẫu | `FILE-B01.pdf` / `FILE-B01.jpg` | Tạo 2 tệp nhỏ (< 5MB, định dạng PDF/JPG) để nhập `evidenceFileIds`; tạo 1 tệp sai định dạng (`test.exe`) để test chặn |
| Tệp vượt dung lượng | `huge.pdf` (> 5MB hoặc vượt `UPLOAD_CERTIFICATION_MAX_SIZE`) | Để test chặn tải lên (nếu có UI kiểm tra) |
| Email test | `test-notify@example.com` (nếu cần nhận thông báo qua email) | Không bắt buộc; `NotificationService` gửi trong hệ thống đã đủ cho TC-04 |
| Mã QR / tem | `TRC-A-01`, `TRC-C-01` | In ra hoặc lưu ảnh để quét trên trình duyệt công khai (hoặc nhập thủ công) |

### F. Checklist trước khi test

- [ ] `git status` → đang ở branch `feature/NCL-08-CN-012-close-recall-case`; không có thay đổi chưa commit liên quan story khác.
- [ ] `docker ps` → container backend (`nguongocso-backend` hoặc từ compose) chạy, port `8080` listen.
- [ ] `curl -s http://localhost:8080/api/v1/recall-cases -H "Authorization: Bearer <token>"` → trả `200` (hoặc `401` nếu thiếu token, nhưng backend chấp nhận).
- [ ] `npm run dev` đã chạy; terminal hiển thị `Local: http://localhost:5173` (hoặc tương đương); mở được `http://localhost:5173/login`.
- [ ] Đăng nhập `orgmanager` / `admin123` thành công; vào được `/recall-cases`; thấy danh sách.
- [ ] Dữ liệu Phần 1 đã nạp: có `ORG-A`, `FARM-A-01`, `LOT-A-01`, `SHP-A-01/02/03`, `TRC-A-01`, `CASE-V1` (hoặc đã tạo qua lazy materialize).
- [ ] `CASE-V2`, `CASE-V3`, `CASE-V4`, `CASE-V5` → `CASE-V10` đã tồn tại đúng trạng thái (`OPEN` / `CLOSED`).
- [ ] `NotificationService` hoạt động (kiểm tra `/notifications` sau khi đóng `CASE-V4`).
- [ ] Trang tra cứu công khai hoạt động: nhập `TRC-A-01` / `TRC-C-01` trả kết quả có `recallMessage` đúng.
- [ ] Hồ sơ sự cố: kiểm tra `GET /api/v1/recall-cases/CASE-V1` trả đủ dữ liệu; nếu có chức năng xuất, thử xuất; nếu không có → ghi "Cần xác nhận".

---

## Câu hỏi mở cần xác nhận trước khi test

1. **Hồ sơ sự cố xuất ra**: Story nói "Cho phép xuất hồ sơ sự cố kèm hồ sơ truy xuất" nhưng thiếu endpoint / UI cụ thể trong `docs/codebase-map.md` và `docs/api/recall/RecallCase.md`. Cần xác nhận: có màn hình "Xuất hồ sơ" trên `RecallCaseDetailPage` không? Nếu không, kiểm thử MTC-04 chỉ kiểm tra dữ liệu API / DB.
2. **Tệp biên bản bắt buộc**: `CloseRecallCaseRequest` có `evidenceFileIds` tùy chọn; nhưng `docs/api/recall/RecallCase.md` nói "tệp biên bản nếu bắt buộc" — cần xác nhận có bắt buộc cho từng lô hay chỉ cấp vụ việc.
3. **Quyền đóng (`RECALL_CASE:CLOSE`)**: Controller chỉ có `@PreAuthorize("hasRole('VT-02')")`; cần xác nhận có cần thêm `hasAuthority('RECALL_CASE:CLOSE')` trong `SecurityConfig` không trước khi test phân quyền MTC-14/15.
4. **Kênh thông báo**: `NotificationService.sendRecallCaseClosedNotification()` gửi qua điều nào (trong hệ thống / email / cả hai)? Cần xác nhận để test MTC-03 chính xác.

---

## Tóm tắt khớp 3 phần

- **Phần 1 (Dữ liệu)**: `ORGANIZATION` (`ORG-A/B/C`), `USER` (`orgmanager`, `procurement_b`, `orgmanager_c`, `eventrec`), `PRODUCTION_LOT` (`LOT-A-01`), `SHIPMENT` (`SHP-A-01/02/03`, `SHP-B-01/02/03`, `SHP-C-01`), `TRACE_CODE` (`TRC-A-01`, `TRC-C-01`), `RECALL_CASE` (`CASE-V1` → `V10`), `RECALL_LOT_RESULT` cho V1/V2/V3/V6.
- **Phần 2 (Kịch bản)**: 23 kịch bản (`MTC-01` → `MTC-22`) tham chiếu rõ mã dữ liệu từ Phần 1; dùng tài khoản đúng; thao tác trên màn hình frontend `npm run dev`; kết quả quan sát được.
- **Phần 3 (Môi trường)**: Backend `docker build` + `docker run` (port `8080`); Frontend `npm run dev` (port `5173`); DB `localhost:3307`; kiểm tra bằng `curl`, trình duyệt, `docker logs`; checklist trước test đầy đủ.
