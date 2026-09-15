# Chuẩn bị kiểm thử thủ công — NCL-07-CN-008

> Phạm vi: chỉ NCL-07-CN-008 (Dashboard mức độ sử dụng nền tảng theo tổ chức). Không sửa mã nguồn, không đổi API contract. Chỉ chuẩn bị dữ liệu, kịch bản, môi trường.
> Branch kiểm thử: `feature/NCL-07-CN-008-organization-usage`
> Môi trường: Backend Spring Boot standalone (MySQL 8.4 local), Frontend `npm run dev`. (Máy không có Docker → không dùng `docker compose`.)
> Ngày tham chiếu của bộ dữ liệu: **2026-09-14**. Nếu kiểm thử sau ngày này, kỳ mặc định (30 ngày gần nhất) sẽ trượt — khi đó **truyền ngày cố định `startDate=2026-08-16&endDate=2026-09-14`** để tái lập đúng số liệu dưới đây.

---

## Phần 1 — Dữ liệu kiểm thử

### A. Tổ chức và người dùng

| Mã | Tên | UUID | Vai trò mẫu | Mô tả kịch bản |
|---|---|---|---|---|
| `HTXA` | HTX Chè Tân Cương | `aaa00001-0000-0000-0000-000000000001` | Tổ chức (COOPERATIVE) | Hoạt động mạnh, 6 metric đều có số, tăng trưởng ở kỳ hiện tại |
| `HTXB` | HTX Rau Sạch | `bbb00002-0000-0000-0000-000000000002` | Tổ chức (COOPERATIVE) | Ngừng hoạt động từ 2026-06-01 → "Cần liên hệ hỗ trợ", 2 kỳ đều 0 |
| `HTXC` | HTX Mới Thành Lập | `ccc00003-0000-0000-0000-000000000003` | Tổ chức (COOPERATIVE) | Mới tạo đúng ngày 2026-09-14 → "Chưa có dữ liệu" |
| `SYSTEM` | Hệ thống | `963ff448-aff1-11f1-8bac-6018952b881a` | SYSTEM | Không có dữ liệu nghiệp vụ → "Chưa có dữ liệu" |

| Tài khoản | Mật khẩu | Vai trò | Tổ chức | Dùng cho |
|---|---|---|---|---|
| `admin` | `admin123` | VT-01 (quản trị) | SYSTEM | Toàn bộ kịch bản (VT-01 là vai trò duy nhất được phép) |
| `managerA` | `admin123` | VT-02 | HTXA | Kịch bản phân quyền (phải bị từ chối 403) |

### B. Số liệu kỳ vọng (lọc `startDate=2026-08-16&endDate=2026-09-14`)

Kỳ hiện tại = 2026-08-16 → 2026-09-14; kỳ trước = 2026-07-17 → 2026-08-15.

| Metric | HTXA (current / previous / change / %) | HTXB | HTXC | SYSTEM |
|---|---|---|---|---|
| Lô sản xuất mới | 3 / 2 / +1 / **+50.0%** | 0 | 0 | 0 |
| Nhật ký sản xuất | 4 / 2 / +2 / **+100.0%** | 0 | 0 | 0 |
| Sự kiện chuỗi | 3 / 1 / +2 / **+200.0%** | 0 | 0 | 0 |
| Tem đã kích hoạt | 2 / 1 / +1 / **+100.0%** | 0 | 0 | 0 |
| Lượt tra cứu công khai | 3 / 1 / +2 / **+200.0%** | 0 | 0 | 0 |
| Người dùng hoạt động | 1 / 1 / 0 / **+0.0%** | 0 | 0 | 0 |
| Hoạt động gần nhất | 2026-09-12 | 2026-06-01 | — (trống) | — (trống) |
| `hasData` | **true** | false | false | false |
| `needsSupport` | **false** | **true** | true | true |

> Ghi chú đọc kết quả:
> - HTXA: org đang vận hành bình thường, các chỉ số tăng → tag không hiển thị.
> - HTXB: không có hoạt động >= 30 ngày → tag **"Cần liên hệ hỗ trợ"**; tổng column vẫn 0 (vì 2 kỳ đều không có dữ liệu) nhưng dòng vẫn có `lastActivityAt`.
> - HTXC: org tạo sau khi bắt đầu kỳ hiện tại (2026-08-16) → mọi cột 0 và tag **"Chưa có dữ liệu"** (không bị đánh nhầm là "cần hỗ trợ").
> - Khi `previous = 0` nhưng `current > 0` (ví dụ lọc `2026-09-01..2026-09-14`), ô metric hiển thị giá trị kèm mũi tên lên và "`+100.0%`" (quy ước: tăng từ 0 = 100%).

### C. Dữ liệu đã seed (file `docs/testing/NCL-07-CN-008-seed.sql`, idempotent)

- 3 tổ chức demo; user `managerA` (bang hash của `admin` = `admin123`) + liên kết `organization_users` VT-02.
- 6 lô sản xuất: HTXA 5 lô (3 kỳ hiện tại, 2 kỳ trước), HTXB 1 lô cũ (2026-06-01).
- 5 lô hàng (shipment, status `ACTIVATED`) gắn 4 sự kiện chuỗi (`HARVEST/PACKAGING/TRANSPORT`, `is_correction=0`).
- 3 tem đã kích hoạt (`status=ACTIVE`, có `activated_at`) + 4 lượt tra cứu công khai.
- 6 dòng nhật ký sản xuất (4 hiện tại, 2 trước); 6 dòng `activity_logs` (HTXA 5: 3 hiện tại + 2 trước; HTXB 1 cũ).
- Tài khoản `admin`/`admin123` (VT-01) có sẵn từ seed hệ thống, tổ chức SYSTEM.

### D. Dữ liệu ngoại lệ (tạo khi cần)

| Tình huống | Cách chuẩn bị |
|---|---|
| Metric previous = 0, current > 0 | Lọc kỳ `2026-09-01 → 2026-09-14` (tự áp dụng ngay) → mọi metric của HTXA đều previous = 0 → hiển thị giá trị kèm mũi tên lên và "`+100.0%`" |
| Ngày trống (không truyền startDate/endDate) | Xóa 2 ô ngày → bảng tự tải lại, backend dùng 30 ngày gần nhất; kết quả phụ thuộc ngày chạy |
| Kỳ nghịch đảo (start > end) | Nhập `startDate > endDate` → frontend hiển thị thông báo lỗi, không gọi API |

---

## Phần 2 — Môi trường kiểm thử

### A. Thông tin môi trường

| Hạng mục | Giá trị / Hướng dẫn |
|---|---|
| Branch cần kiểm thử | `feature/NCL-07-CN-008-organization-usage` |
| Backend | Spring Boot chạy standalone bằng `mvnw.cmd spring-boot:run`, cổng `8080`. Khởi động lại: xem mục "Cách khởi động lại" bên dưới |
| Frontend | `npm run dev` trong `frontend/`, cổng `3000` (`vite.config.ts`). URL: `http://localhost:3000` |
| MySQL | MySQL 8.4 local (`C:\Program Files\MySQL\MySQL Server 8.4`), database `nguon_goc_so`, user `nguongocso`/`nguongocso`, cổng `3306`. Khởi động bằng `mysqld --datadir=C:\MySQLData --port=3306` |
| Múi giờ | Backend dùng `Asia/Ho_Chi_Minh` (đồng bộ DB và hiển thị ngày) |
| API kiểm tra trực tiếp | `GET /api/v1/reports/organization-usage?startDate=...&endDate=...` với header `Authorization: Bearer <access_token>` |

### B. Cách khởi động lại (khi cần)

```powershell
# 1) MySQL (nếu chưa chạy)
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --datadir=C:\MySQLData --port=3306

# 2) Backend (cần đủ biến môi trường; shell mới phải set lại)
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
$env:DB_HOST="localhost"; $env:DB_PORT="3306"; $env:DB_NAME="nguon_goc_so"
$env:DB_USERNAME="nguongocso"; $env:DB_PASSWORD="nguongocso"; $env:PORT="8080"
$env:JWT_SECRET="local-dev-secret-key-nguongocso-2026-min-32-chars-xxxx"; $env:JWT_EXPIRATION="86400000"
$env:ALLOWED_ORIGINS="http://localhost:3000,http://localhost:5173,http://localhost"
$env:UPLOAD_BASE_DIR="./uploads"; $env:UPLOAD_FARM_LOG_RELATIVE_PATH="farm-logs"; $env:UPLOAD_FARM_LOG_MAX_SIZE="5242880"
$env:QR_IMAGE_STORAGE_PATH="./files/qr"; $env:FRONTEND_URL="http://localhost:3000"
$env:MAIL_HOST="localhost"; $env:MAIL_PORT="2525"; $env:MAIL_USERNAME="test"; $env:MAIL_PASSWORD="test"
$env:LOCATIONIQ_API_KEY="test-key"; $env:LOCATIONIQ_BASE_URL="https://us1.locationiq.com/v1"
cd D:\nguon-goc-so\nguon-goc-so-fixed\backend; .\mvnw.cmd spring-boot:run

# 3) Frontend
cd D:\nguon-goc-so\nguon-goc-so-fixed\frontend; npm run dev   # http://localhost:3000
```

### C. Nạp lại dữ liệu seed (nếu bị xóa / cần reset)

```powershell
$env:MYSQL_PWD="nguongocso"
Get-Content docs\testing\NCL-07-CN-008-seed.sql -Raw | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u nguongocso nguon_goc_so
```

### D. Công cụ hỗ trợ

| Công cụ | Mục đích |
|---|---|
| Trình duyệt Chrome/Edge (DevTools) | Kiểm tra Network (401/403), Console |
| `mysql` CLI / DBeaver/HeidiSQL | Kiểm tra dữ liệu nguồn 6 metric trực tiếp trên DB |
| Postman / PowerShell `Invoke-WebRequest` | Gọi API trực tiếp để đối chiếu số liệu frontend |

### E. Checklist trước khi test

- [ ] `git status` → đang ở `feature/NCL-07-CN-008-organization-usage`.
- [ ] MySQL đang chạy: `mysqladmin -u root ping` trả `mysqld is alive`; `SELECT COUNT(*) FROM organizations` có >= 4 dòng (SYSTEM + HTXA/HTXB/HTXC).
- [ ] Backend chạy: mở `http://localhost:8080/api/v1/auth/login` (POST admin/admin123) → 200; `netstat` thấy cổng 8080 LISTENING.
- [ ] Frontend chạy: `http://localhost:3000` trả 200; đăng nhập `admin`/`admin123` vào được.
- [ ] Đã chạy seed `docs/testing/NCL-07-CN-008-seed.sql` (kiểm tra bảng đếm: 6 lô, 6 nhật ký, 6 activity, 4 event, 3 tem, 4 scan, 5 shipment).
- [ ] Vào được trang: menu trái → nhóm báo cáo → **"Mức độ sử dụng nền tảng"** (route `/reports/organization-usage`); hoặc mở thẳng `http://localhost:3000/reports/organization-usage`.
- [ ] Login `managerA`/`admin123` hoạt động (để test phân quyền 403).

---

## Tóm tắt

- **Dữ liệu**: 3 tổ chức demo phủ 3 trạng thái (hoạt động / ngừng hoạt động / mới) — seed idempotent tại `docs/testing/NCL-07-CN-008-seed.sql`.
- **Môi trường**: MySQL 8.4 local + Backend `8080` + Frontend `3000`; lệnh khởi động lại đầy đủ ở Phần 2.B.
- **Kỳ vọng số liệu**: bảng Phần 1.B — đã xác minh bằng API trực tiếp trước khi viết tài liệu này.
- **Kịch bản thủ công chi tiết**: xem `docs/testing/NCL-07-CN-008_manual_test.md`.