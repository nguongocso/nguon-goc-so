# Dữ liệu Demo — Nguồn Gốc Số (chuẩn bị buổi bảo vệ)

> Danh sách dữ liệu demo được **seed tự động** bởi Flyway (migration `data/`)
> khi khởi tạo database mới. Dùng cho buổi bảo vệ / demo, không phải dữ liệu production.

- Mã story: **NCL-10-CN-011-CV-05**
- Tham chiếu: [OPERATIONS.md](./OPERATIONS.md), [USER_GUIDE.md](./USER_GUIDE.md)

---

## 1. Nguồn dữ liệu

Toàn bộ dữ liệu demo nằm trong các migration Flyway `data/`:

| File | Nội dung |
|---|---|
| `V14__seed_roles.sql` | 6 vai trò hệ thống |
| `V15__seed_permissions.sql` | Danh mục permissions |
| `V16__seed_role_permissions.sql` | Gán quyền mặc định cho vai trò |
| `V17__seed_default_admin.sql` | Tổ chức **SYSTEM** + tài khoản **admin** (VT-01) |
| `V18__seed_backup_schedule.sql` | Lịch sao lưu mặc định (hằng ngày 02:00) |
| `V43__seed_administrative_units.sql` | Đơn vị hành chính (địa bàn cho VT-05) |
| `V55__seed_standards.sql` | Danh mục tiêu chuẩn chất lượng (VietGAP, GlobalG.A.P., ISO 22000…) |
| `V56__seed_inspection_criteria.sql` | Danh mục chỉ tiêu kiểm nghiệm (Pb, Cd, E. coli…) |
| `V57__seed_role_test_accounts.sql` | 3 tổ chức demo + 5 tài khoản demo (VT-02…VT-06) |
| `V58__seed_demo_data_vt02.sql` | Dữ liệu nghiệp vụ demo cho VT-02 (đầy đủ luồng truy xuất) |

> ⚠️ Lưu ý: dữ liệu demo hiện **seed qua migration**. Việc bổ sung thêm dữ liệu
> cho buổi bảo vệ nên dùng **API/UI của hệ thống** hoặc file SQL tạm — không tự
> thêm migration mới ngoài phạm vi task.

---

## 2. Tài khoản đăng nhập demo

Mật khẩu mặc định: **`admin123`** (chỉ dùng cho môi trường dev/demo —
**Yêu cầu đổi khi lên production**).

| Username | Vai trò | Tổ chức | Tên hiển thị (seed) | Migration |
|---|---|---|---|---|
| `admin` | **VT-01** — Quản trị viên hệ thống | `SYSTEM` (Hệ thống) | Quản trị viên hệ thống | V17 |
| `orgmanager` | **VT-02** — Quản lý hợp tác xã | `DEMO_HTX` (HTX Nông Sản Demo) | Quản lý HTX Demo (VT-02) | V57 |
| `eventrecorder` | **VT-03** — Người ghi sự kiện | `DEMO_HTX` | Người ghi sự kiện Demo (VT-03) | V57 |
| `procurement` | **VT-04** — Doanh nghiệp thu mua | `DEMO_NSV` (Công ty Nông Sản Việt Demo) | Nhân viên thu mua Demo (VT-04) | V57 |
| `regulator` | **VT-05** — Cán bộ quản lý ngành | `DEMO_GOV` (Chi cục Quản lý Chất lượng Nông Sản) | Cán bộ quản lý nhà nước Demo (VT-05) | V57 |
| `consumer` | **VT-06** — Người tiêu dùng | `SYSTEM` | Người tiêu dùng Demo (VT-06) | V57 |

> `consumer` không có dashboard nội bộ — dùng để trải nghiệm trang tra cứu công khai.

---

## 3. Tổ chức demo

| Mã | Tên | Loại | Trạng thái |
|---|---|---|---|
| `SYSTEM` | Hệ thống | SYSTEM | ACTIVE |
| `DEMO_HTX` | HTX Nông Sản Demo | COOPERATIVE | ACTIVE |
| `DEMO_NSV` | Công ty Nông Sản Việt Demo | ENTERPRISE | ACTIVE |
| `DEMO_GOV` | Chi cục Quản lý Chất lượng Nông Sản | GOVERNMENT | ACTIVE |

---

## 4. Dữ liệu nghiệp vụ demo — VT-02 (V58)

Toàn bộ thuộc tổ chức **`DEMO_HTX`**, người tạo `orgmanager`.

### 4.1 Danh mục sản phẩm (8)

`Nho`, `Chè`, `Xoài`, `Lúa`, `Sầu riêng`, `Hồ tiêu`, `Rau cải`, `Cà phê`
(4.1 ID cố định `…0008xxxx`, `requires_inspection = TRUE`).

### 4.2 Vùng trồng (15)

- ID `00000000-…-0001xxxxxxx`, 1 vùng/lô, diện tích 0.8–6.4 ha, có tọa độ GPS.
- Ví dụ: `Vùng trồng Nho 01`, `Vùng trồng Chè 02`, `Vùng trồng Lúa 04`…

### 4.3 Lô sản xuất (15)

- ID `…0002xxxxxxx`, 1 lô trên 1 vùng trồng.
- Trạng thái lô demo (đủ các trạng thái để trình diễn):
  - **PACKAGED:** Lô 1–5 (Nho, Chè, Xoài, Lúa, Sầu riêng) — sẵn sàng tạo lô hàng/tem.
  - **HARVESTED:** Lô 6–10 (Hồ tiêu, Rau cải, Cà phê, Nho, Chè).
  - **APPROVED:** Lô 11–15 (Xoài, Lúa, Sầu riêng, Hồ tiêu, Rau cải).
- Mẫu: `Lô Nho 01`, `Lô Chè 02`, … (số lượng dự kiến 1800–6000 kg).

### 4.4 Chứng nhận (15) + gắn vào lô

- Mỗi lô có 1 chứng nhận; 15 tiêu chuẩn khác nhau: VietGAP, GlobalG.A.P.,
  HACCP (TCVN 5603), ISO 22000:2018, FSSC 22000, TCVN 11041-2:2017,
  USDA Organic, EU Organic, Rainforest Alliance, Fairtrade, BRCGS Food Safety,
  IFS Food, SQF, ASEAN GAP, Codex Alimentarius.
- Mẫu mã: `CERT-DEMOHTX-001`… hạn hiệu lực 2025–2027.

### 4.5 Yêu cầu kiểm nghiệm (15) + chỉ tiêu + kết quả

- Đơn vị kiểm nghiệm: *Trung tâm Kiểm nghiệm Nông Lâm Thủy sản Miền Bắc*.
- Trạng thái:
  - Lô 1–12: **PASSED** (mẫu gửi 2026-01-15 → …).
  - Lô 13–14: **PENDING_RESULT**.
  - Lô 15: **CANCELLED**.
- Mỗi yêu cầu PASSED (1–12) có 3 chỉ tiêu VietGAP:
  - **Hàm lượng Chì (Pb)** — `HEAVY_METAL_PB`
  - **Hàm lượng Cadmi (Cd)** — `HEAVY_METAL_CD`
  - **E. coli** — `MICROBIO_E_COLI`
  - Toàn bộ chỉ tiêu đều `passed = TRUE`, hạn kết quả 1 năm.

### 4.6 Khóa API bên thứ ba (15)

- Partner tên `Đối tác TTT-01`…`TTT-15`, prefix `nks_live_…`, trạng thái ACTIVE.
- Raw API keys chỉ nằm trong **comment file migration** (không nên dùng ngoài môi trường demo).
- Ví dụ khóa raw của partner 1:
  `nks_live_a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5a2b3c4d5`
  (xem cuối `V58__seed_demo_data_vt02.sql`).

---

## 5. Dữ liệu lô hàng / mã QR / lượt quét

> ⚠️ **NOT VERIFIED / CÓ KHOẢNG TRỐNG:** Migration V58 **chưa seed lô hàng
> (shipments) và mã truy xuất (trace_codes)** cho 15 lô PACKAGED.
> Danh mục trong `docs/sample-data/` là file CSV cho tính năng **import lô**
> (`production_lot_import_template.csv`, `production_lot_valid.csv`…), không phải
> dữ liệu lô hàng/tem.

Hệ quả cho buổi bảo vệ:

- Để trình diễn luồng **tạo lô hàng → cấp tem → người tiêu dùng quét mã**,
  cần **tạo lô hàng thủ công** trên UI (từ lô PACKAGED, sau khi cấu hình dải mã)
  hoặc thực hiện thao tác seed tạm.
- `docs/api/publicapi/PublicLookup.md` chứa ví dụ mã tra cứu mẫu
  (`HX00000029`, `LH-2026-0029` — ví dụ tài liệu, không phải dữ liệu seed thật).

### Đề xuất cho buổi bảo vệ (Không đụng code)

1. Dựng môi trường dev (docker compose hoặc local).
2. Login `orgmanager`, tạo lô hàng từ lô PACKAGED (vd **Lô Nho 01**).
3. Kích hoạt tem → in/xuất QR → quét bằng điện thoại trên trang công khai.
4. Xác nhận timeline/chứng nhận/kết quả kiểm nghiệm hiển thị công khai.

> Chính sự thao tác demo này sẽ tạo dữ liệu trace thật trong DB demo —
> đúng tinh thần "tài liệu phản ánh hệ thống thật, không bịa data".

---

## 6. Migration khác liên quan (giá trị seed khởi tạo)

| Migration | Giá trị |
|---|---|
| `V18__seed_backup_schedule.sql` | Lịch sao lưu: `0 0 2 * * ?` — "Sao lưu dữ liệu tự động hằng ngày lúc 02:00 sáng" |
| `V27/V28/V35…` | Bảng tính năng (không seed) |
| `V43__seed_administrative_units.sql` | Đơn vị hành chính cho phân công địa bàn |
| `V51__seed_input_materials.sql` | Danh mục vật tư đầu vào |
| `V55/V56` | Tiêu chuẩn + chỉ tiêu kiểm nghiệm |

---

## 7. Lưu ý bảo mật khi demo

- **KHÔNG** dùng `production` database hoặc secrets thật khi chạy demo.
- Tài khoản `admin123` chỉ dành cho dev/demo; nếu buổi bảo vệ dùng môi trường
  public, hãy đổi mật khẩu ngay sau khi dựng.
- Raw API keys demo sinh ra tĩnh (trong file migration) — không dùng cho
  môi trường production.

---

## 8. NOT VERIFIED

Trạng thái dữ liệu thực tế trên môi trường demo/staging hiện tại chưa được
kiểm chứng runtime (phase này chỉ audit repository). Các con số trong tài liệu
đối chiếu trực tiếp với nội dung file migration `V57`/`V58`.