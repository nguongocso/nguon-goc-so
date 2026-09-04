# Kịch bản Demo — Nguồn Gốc Số (Buổi bảo vệ)

> Kịch bản demo **deterministic** — một thành viên khác trong team có thể mở
> tài liệu này và trình diễn mà **không cần hỏi người viết code**.
> Tên màn hình lấy chính xác từ UI (`frontend/src/components/layout/Sidebar.tsx`),
> route lấy từ `frontend/src/routes/AppRoutes.tsx`, API từ các Controller backend.

- Mã story: **NCL-10-CN-011-CV-05**
- Tham chiếu: [TEST_EVIDENCE.md](./TEST_EVIDENCE.md), [PRESENTATION_OUTLINE.md](./PRESENTATION_OUTLINE.md),
  [DEMO_DATA.md](../handover/DEMO_DATA.md), [OPERATIONS.md](../handover/OPERATIONS.md)

---

## 0. Chuẩn bị trước buổi demo (10–15 phút)

> Thực hiện trước; mục tiêu là khi bắt đầu demo, dữ liệu đã **sẵn sàng** để
> trình diễn đúng luồng đầu cuối mà không phải chờ đợi hay tạo mới giữa chừng.

### 0.1 Dựng môi trường

```bash
cd /path/to/nguon-goc-so
cp .env.example .env        # điền đủ: DB_PASSWORD, MYSQL_ROOT_PASSWORD, JWT_SECRET, MAIL_*, LOCATIONIQ_API_KEY
docker compose up -d --build
docker compose ps           # mysql healthy, backend Up, frontend Up
curl -s http://localhost:8080/actuator/health   # kỳ vọng {"status":"UP"}
```

### 0.2 Chuẩn bị dữ liệu demo

Seed tự động khi DB mới (Flyway `data/`):
- Tài khoản: `admin/admin123`, `orgmanager/admin123`, `eventrecorder/admin123`, …
- Tổ chức: `SYSTEM`, `DEMO_HTX`, `DEMO_NSV`, `DEMO_GOV`.
- Dữ liệu VT-02: 15 vùng trồng, 15 lô sản xuất (5 `PACKAGED`), 15 chứng nhận,
  15 yêu cầu kiểm nghiệm (12 `PASSED`), 42 kết quả chỉ tiêu.

**Bước chuẩn bị bắt buộc (tạo dữ liệu còn thiếu bằng UI — chưa có trong seed):**

> ⚠️ Seed **chưa có** `code_range`, `shipments`, `trace_codes`. Cần làm 2 thao tác:

1. **Đăng nhập `admin/admin123`** → menu **Quản lý → Quản lý dải mã**
   → **Tạo dải mã**: chọn tổ chức `HTX Nông Sản Demo (DEMO_HTX)`,
   prefix `DEMO`, tổng hạn mức `100`. *(endpoint: `POST /api/v1/admin/code-ranges`)*
2. **Đăng nhập `orgmanager/admin123`** → menu **Vận hành sản xuất → Lô sản xuất**
   → mở **Lô Nho 01** (trạng thái `PACKAGED`) → **Tạo lô hàng**
   (`/production-lots/:id/shipments/create`) → **Kích hoạt tem**
   (`POST /api/v1/shipments/{id}/activate`) → ghi lại tên lô hàng và **1 mã truy xuất**.
3. (Khuyến nghị) **Xuất tem QR** từ chi tiết lô hàng → in/giữ file PDF để quét trên màn hình.

> Sau bước 2, luồng "quét mã → tra cứu" chỉ còn là trình diễn trên 1 mã có thật.

---

## 1. Luồng demo chính (đề xuất ~20–25 phút)

Luồng đi theo đúng chuỗi giá trị của hệ thống:

```text
Login → Organization → Production Lot → Production Event
      → Inspection → Traceability → QR / Consumer Lookup
```

---

### Step 1 — Đăng nhập & chọn tổ chức

- **Actor:** Quản lý hợp tác xã (`orgmanager`)
- **Screen:** Đăng nhập → Chọn tổ chức
- **Action:**
  1. Mở `http://localhost:3000/`.
  2. Nhập `orgmanager` / `admin123`.
  3. Chọn tổ chức **HTX Nông Sản Demo**.
- **Expected Result:** Vào **Dashboard** theo vai trò VT-02 (màn hình hiển thị
  thống kê tổng quan của HTX).
- **Evidence:** Screenshot Dashboard + (tùy chọn) log API
  `POST /api/v1/auth/login` → `200`, `POST /api/v1/auth/select-organization` → `200`.

---

### Step 2 — Xem tổ chức & thành viên

- **Actor:** Quản lý hợp tác xã
- **Screen:** **Hệ thống → Hồ sơ tổ chức** và **Quản lý → Quản lý thành viên**
- **Action:** Mở hồ sơ tổ chức (thấy `DEMO_HTX`); mở danh sách thành viên.
- **Expected Result:** Thông tin tổ chức hiển thị; danh sách thành viên có
  `orgmanager` (VT-02) và `eventrecorder` (VT-03) với trạng thái ACTIVE.
- **Evidence:** Screenshot.

---

### Step 3 — Vùng trồng

- **Actor:** Quản lý hợp tác xã
- **Screen:** **Vận hành sản xuất → Vùng trồng**
- **Action:** Mở danh sách **Vùng trồng**; tìm **Vùng trồng Nho 01** (do seed V58).
- **Expected Result:** 15 vùng trồng hiển thị với loại cây trồng, diện tích, tọa độ.
- **Evidence:** Screenshot.

---

### Step 4 — Lô sản xuất & duyệt lô

- **Actor:** Quản lý hợp tác xã
- **Screen:** **Vận hành sản xuất → Lô sản xuất**
- **Action:**
  1. Mở danh sách **Lô sản xuất**, lọc/bấm vào **Lô Nho 01** (`PACKAGED`).
  2. Trình bày trạng thái lô: 5 lô `PACKAGED`, 5 lô `HARVESTED`, 5 lô `APPROVED`.
  3. (Tùy chọn) Minh họa **Gửi duyệt → Duyệt** trên một lô `APPROVED` hoặc nhắc
     `submit`/`approve` (`POST /api/v1/production-lots/{id}/submit`, `/{id}/approve`).
- **Expected Result:** Chi tiết lô hiển thị vùng trồng, loại nông sản, số lượng,
  trạng thái, người tạo/duyệt.
- **Evidence:** Screenshot danh sách + chi tiết lô.

---

### Step 5 — Ghi sự kiện chuỗi cung ứng (Production Event)

- **Actor:** Người ghi sự kiện (`eventrecorder`)
- **Screen:** **Vận hành sản xuất → Lô sản xuất** → chi tiết lô → nhật ký/sự kiện
  (route: `/farm-logs/create`, `/packaging-events/create`, `/transport-events/record`,
  `/chain-events/scan`)
- **Action:**
  1. Đăng xuất `orgmanager` → đăng nhập `eventrecorder/admin123` → chọn **HTX Nông Sản Demo**.
  2. Mở **Lô Nho 02** (đã `PACKAGED`) → ghi **nhật ký canh tác** (ví dụ hoạt động tưới nước) kèm ghi chú.
  3. Trình bày **Quét mã ghi sự kiện nhanh** (`/chain-events/scan`) — quét mã lô/QR.
  4. Nhắc **Sự kiện chờ đồng bộ** (`/offline-events`) — tính năng offline cho ngoài đồng.
- **Expected Result:** Sự kiện/nhật ký xuất hiện trong dòng sự kiện; nếu mô phỏng
  mất mạng, sự kiện nằm ở **Sự kiện chờ đồng bộ** rồi đồng bộ lại.
- **Evidence:** Screenshot dòng sự kiện (timeline) + screenshot danh sách đã đồng bộ.

> Không cần tạo sự kiện mới nếu muốn demo nhanh — có thể dùng dữ liệu đã tồn tại.

---

### Step 6 — Kiểm nghiệm & chứng nhận (Inspection)

- **Actor:** Quản lý hợp tác xã
- **Screen:** **Quản lý → Chứng nhận** + chi tiết lô → Yêu cầu kiểm nghiệm
  (route: `/certifications`, `production-lots/:lotId/inspection-requests/create`,
  `inspection-requests/:requestId/results`)
- **Action:**
  1. Đăng nhập lại `orgmanager`.
  2. Mở **Chứng nhận** → tìm **Chứng nhận VietGAP - Lô 01** (đã gắn).
  3. Mở yêu cầu kiểm nghiệm của **Lô Nho 01** → xem 3 chỉ tiêu VietGAP
     (Chì Pb, Cadmi Cd, E. coli) đều **Đạt**.
- **Expected Result:** Lô có chứng nhận + kết quả kiểm nghiệm `passed = TRUE`
  (dữ liệu V58). Kết quả này sẽ xuất hiện công khai ở bước Consumer Lookup.
- **Evidence:** Screenshot chứng nhận + kết quả kiểm nghiệm.

---

### Step 7 — Traceability: Tạo lô hàng & cấp tem

- **Actor:** Quản lý hợp tác xã
- **Screen:** Chi tiết **Lô sản xuất** → **Tạo lô hàng**
  (route: `/production-lots/:productionLotId/shipments/create`)
- **Action:**
  1. Mở **Lô Nho 01** (`PACKAGED`, đã có dải mã + kiểm nghiệm đạt).
  2. **Tạo lô hàng** → hệ thống sinh mã truy xuất từ dải mã `DEMO…`.
  3. Trong chi tiết lô hàng → **Kích hoạt tem** (`POST /api/v1/shipments/{id}/activate`).
  4. **Xuất tem QR**: menu chi tiết lô hàng → **Xuất tem QR** → tải file PDF
     (`POST /api/v1/shipments/{shipmentId}/labels/export`).
- **Expected Result:** Lô hàng có trạng thái `ACTIVATED`; danh sách mã truy xuất
  hiển thị; file PDF chứa mã QR để trình chiếu/quét.
- **Evidence:** Screenshot lô hàng + mã truy xuất + file PDF tem (không cần in, hiện màn hình).

> Nếu làm đúng ở **mục 0.2**, lô hàng & tem đã sẵn sàng → chỉ cần mở và trình bày.

---

### Step 8 — QR / Consumer Lookup (Người tiêu dùng)

- **Actor:** Người tiêu dùng (không cần đăng nhập)
- **Screen:** Trang công khai `/` (trang chủ) → `/public/trace/:codeValue`
- **Action:**
  1. Mở trang chủ `http://localhost:3000/` (hoặc dùng **window/incognito** để rõ là public).
  2. **Quét mã QR** từ file PDF bằng camera máy laptop, **hoặc nhập mã truy xuất**
     của lô hàng ở bước 7 vào ô tìm kiếm.
  3. Xem kết quả tra cứu.
- **Expected Result:**
  - Hiển thị thông tin sản phẩm (tên sản phẩm khớp lô), mã lô hàng.
  - **Timeline** dòng sự kiện (bản đồ / danh sách).
  - **Chứng nhận** + **kết quả kiểm nghiệm** công khai (từ bước 6).
- **Evidence:** Screenshot trang tra cứu + ghi rõ mã đã dùng.
  (Endpoint: `POST /api/v1/public/trace/{code}/scan` hoặc `GET /api/v1/public/trace/{code}`.)

---

### Step 9 — (Tùy chọn) Vai trò Doanh nghiệp thu mua & Cán bộ ngành

- **Actor:** `procurement` (VT-04) hoặc `regulator` (VT-05)
- **Screen:** **Thu mua → Nhập kho & đối chiếu** (VT-04); **Thống kê & Báo cáo** (VT-05)
- **Action:** Đăng nhập `procurement/admin123` → mở danh sách lô hàng đủ điều kiện
  thu mua (`GET /api/v1/shipments/eligible`). Hoặc `regulator/admin123` → mở
  **Báo cáo ngành** / **Xuất dữ liệu mở**.
- **Expected Result:** Hiển thị đúng dữ liệu trong phạm vi vai trò/tổ chức.
- **Evidence:** Screenshot.

---

## 2. Kịch bản backup khẩn cấp (nếu hệ thống lỗi giữa chừng)

Nếu có sự cố không sửa được ngay:

1. Bình tĩnh, đóng modal/lỗi, reload trang (`F5`).
2. Nếu backend chết → kiểm tra:
   ```bash
   docker compose logs -f backend
   docker compose restart backend
   curl -s http://localhost:8080/actuator/health
   ```
3. Nếu database chết → `docker compose restart mysql`, đợi healthy, rồi restart backend.
4. Nếu vẫn không khôi phục → **demo dữ liệu đã seed** bằng tài khoản fresh
   (login lại, các bước demo vẫn đi được tới bước 6; bước 7–8 có thể trình bày
   bằng ảnh/screenshot chuẩn bị sẵn).
5. Luôn giữ sẵn **ảnh chụp màn hình** các bước chính trước buổi demo để dự phòng.

---

## 3. Defense Checklist (check trước khi demo T-30 phút)

```text
[ ] Environment accessible (http://localhost:3000 mở được)
[ ] Demo account available (admin123 hoạt động)
[ ] Demo data available (15 lô, chứng nhận, kiểm nghiệm đã seed)
[ ] Backend running (curl /actuator/health = UP)
[ ] Frontend running (trang chủ hiển thị)
[ ] Database running (mysql healthy)
[ ] Login verified (orgmanager + eventrecorder)
[ ] Main E2E flow verified (lô → lô hàng → tem → tra cứu)
[ ] QR/traceability verified (mã đã kích hoạt có thể tra cứu)
[ ] Consumer lookup verified (trang public hiển thị đầy đủ)
[ ] Security verified (trang nội bộ redirect login khi chưa đăng nhập)
[ ] Backup/demo recovery plan checked (xem mục 2 — kịch bản backup)
```

---

## 4. Ghi chép evidence

- Trong lúc demo, ghi lại **mã lô hàng/mã truy xuất** đã dùng (ưu tiên trùng với bước 0.2).
- Lưu screenshot theo từng bước vào thư mục demo evidence (ngoài phạm vi repo,
  hoặc thư mục tạm, không commit nếu chưa được phép).
- Sau demo, cập nhật [TEST_EVIDENCE.md](./TEST_EVIDENCE.md): chuyển các dòng
  `NOT VERIFIED` sang `PASS` kèm bằng chứng cụ thể.