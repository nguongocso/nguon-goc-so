# Hướng dẫn Vận hành — Nguồn Gốc Số

> Tài liệu vận hành hệ thống **Nguồn Gốc Số** (nền tảng truy xuất nguồn gốc nông sản).
> Mọi lệnh trong tài liệu này được kiểm chứng với repository tại commit
> `4a8d7417` (branch `develop`).

- Mã story: **NCL-10-CN-011-CV-05**
- Tài liệu liên quan: [DEPLOYMENT.md](./DEPLOYMENT.md), [ARCHITECTURE.md](./ARCHITECTURE.md),
  [SECURITY.md](./SECURITY.md), [USER_GUIDE.md](./USER_GUIDE.md), [DEMO_DATA.md](./DEMO_DATA.md)

---

## 1. System Overview

### 1.1 Mục đích hệ thống

**Nguồn Gốc Số** là nền tảng quản lý truy xuất nguồn gốc nông sản, cho phép:

- Quản lý tổ chức (hợp tác xã, doanh nghiệp, cơ quan quản lý) và thành viên, phân quyền chi tiết theo vai trò.
- Khai báo vùng trồng, lô sản xuất và nhật ký canh tác.
- Ghi chuỗi sự kiện cung ứng (thu hoạch, sơ chế, đóng gói, vận chuyển, thu mua, nhập kho).
- Sinh mã QR / tem truy xuất theo chuẩn GS1 mô phỏng.
- Người tiêu dùng quét mã QR để tra cứu hành trình sản phẩm công khai.
- Báo cáo, thống kê, cảnh báo bất thường, sao lưu & phục hồi dữ liệu.

### 1.2 Các thành phần chính

| Thành phần | Công nghệ | Ghi chú |
|---|---|---|
| **Backend** | Spring Boot 3.5.16, Java 21, Maven | API REST tại cổng `8080`, chạy Flyway khi khởi động |
| **Frontend** | React 19, TypeScript, Vite 8, Tailwind CSS 4 | SPA; dev server cổng `3000`; production phục vụ bởi Nginx |
| **Database** | MySQL 8.4 | Migration bằng Flyway |

### 1.3 Kiến trúc runtime (Docker Compose)

```text
[Trình duyệt]
    │  http://localhost:3000
    ▼
[frontend] (Nginx, cổng 3000 → 80)
    │  reverse proxy /api → http://backend:8080
    ▼
[backend] (Spring Boot, cổng 8080)
    │
    ├──► [mysql] (MySQL 8.4, cổng 3306, volume `mysql_data`)
    │
    ├──► File system: /app/uploads (farm log attachments)
    └──► File system: /app/files/qr (ảnh QR)
```

- Nginx của frontend reverse-proxy `/api/` và `/files/` về backend service
  (`nginx.conf.template`), SPA đọc cấu hình runtime tại `/config.js`.
- Backend đọc toàn bộ cấu hình từ biến môi trường (`application.properties` dùng `${VAR}`).

---

## 2. Prerequisites

Yêu cầu tối thiểu để vận hành (đối chiếu README & config đã xác minh):

| Thành phần | Phiên bản | Mục đích |
|---|---|---|
| Docker | 24.x+ | Chạy `docker-compose.yml` ở **thư mục gốc repository** |
| Docker Compose | V2 (lệnh `docker compose`) | Orchestrate 3 service: mysql, backend, frontend |
| Java | **21** | Build backend (`backend/pom.xml` yêu cầu Java 21) |
| Maven | 3.9.x (hoặc dùng `./mvnw`) | Build backend; wrapper được commit trong repo |
| Node.js | **22** | Build frontend (CI dùng node 22, `frontend/package.json`) |
| npm | đi kèm Node 22 | Cài dependency frontend |
| MySQL | 8.x | Nếu không dùng container MySQL (dùng RDS / MySQL host) |

> **Lưu ý:** `docker-compose.yml` nằm tại **root repository** — dùng `docker compose`
> trực tiếp trong thư mục gốc, không phải `docker/docker-compose.yml`.

---

## 3. Startup

### 3.1 Bước 0 — Chuẩn bị môi trường

```bash
# Tạo file .env từ mẫu (bắt buộc, docker-compose đọc .env ở root)
cp .env.example .env
```

Điền các giá trị bắt buộc trong `.env` (xem [mục 6. Configuration](#6-configuration)) —
tối thiểu cần:

```bash
DB_PASSWORD=...            # password cho user DB_NAME=nguongocso
MYSQL_ROOT_PASSWORD=...    # password root MySQL
JWT_SECRET=...             # chuỗi bí mật ký JWT (không được bỏ trống trong production)
MAIL_USERNAME=...          # email SMTP (Gmail app password)
MAIL_PASSWORD=...          # app password SMTP
LOCATIONIQ_API_KEY=...     # key geolocation ngược (nếu dùng)
```

### 3.2 Chạy toàn bộ hệ thống bằng Docker Compose

```bash
cd /path/to/nguon-goc-so
docker compose up -d --build
```

Sau khi khởi động:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- Swagger UI (cần backend chạy): `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/actuator/health`

> Backend sẽ tự chạy Flyway migration khi khởi động (xem [mục 7 — Database](#7-database)).

### 3.3 Chạy bằng mã nguồn (development)

```bash
# Backend (cổng 8080) — cần MySQL đang chạy sẵn
cd backend
./mvnw spring-boot:run

# Frontend (cổng 3000, proxy /api → localhost:8080)
cd frontend
npm install       # lần đầu
npm run dev
```

> Cấu hình MySQL cho chế độ dev lấy từ `backend/.env.example`
> (mặc định `DB_HOST=host.docker.internal`, `DB_NAME=nguon_goc_so`).

### 3.4 Tài khoản khởi tạo

Flyway seed sẵn (xem [DEMO_DATA.md](./DEMO_DATA.md)):

- `admin / admin123` — VT-01 Quản trị viên hệ thống (**phải đổi mật khẩu khi lên production**).
- `orgmanager / admin123`, `eventrecorder / admin123`, `procurement / admin123`,
  `regulator / admin123`, `consumer / admin123` — tài khoản demo cho từng vai trò.

---

## 4. Shutdown

```bash
# Tắt 3 service nhưng GIỮ volume dữ liệu MySQL
docker compose down

# Xóa luôn volume dữ liệu MySQL (DỮ LIỆU SẼ MẤT)
docker compose down -v
```

Dừng gọn mà vẫn giữ network/container để khởi động lại nhanh:

```bash
docker compose stop
docker compose start
```

Với chế độ dev (không dùng compose): dừng bằng `Ctrl+C` trên từng terminal
(`./mvnw spring-boot:run` và `npm run dev`).

---

## 5. Status

### 5.1 Trạng thái container

```bash
docker compose ps
```

Kỳ vọng 3 service: `mysql` (healthy), `backend` (Up), `frontend` (Up).

### 5.2 Health check backend

```bash
curl -s http://localhost:8080/actuator/health
# {"status":"UP"}
```

> **Lưu ý (đã xác minh thực tế):** `/actuator/health` tổng hợp nhiều indicator. Nếu
> SMTP credentials sai/thiếu, `MailHealthIndicator` fail → tổng thể trả `"DOWN"`
> (log có WARN `Mail health check failed`) **dù** MySQL/API vẫn hoạt động bình
> thường. Khi thấy `DOWN`, kiểm tra log backend trước khi kết luận.
>
> Chỉ `/actuator/health` được public (`SecurityConfig`); các endpoint actuator
> khác không được bật/cấu hình trong repository hiện tại (VD `/actuator/info`
> trả 403).

### 5.3 Kiểm tra nhanh frontend

```bash
curl -I http://localhost:3000            # kỳ vọng HTTP 200
curl -s http://localhost:3000/config.js  # kỳ vọng thấy window.__RUNTIME_CONFIG__
```

---

## 6. Configuration

Backend đọc toàn bộ cấu hình từ biến môi trường. Danh sách đầy đủ trong
`.env.example` (root) và `backend/.env.example`. Dưới đây là các biến quan trọng
**(không ghi secret thật ở đây — xem `SECURITY.md`)**.

| Nhóm | Biến | Mô tả | Mặc định |
|---|---|---|---|
| **Database** | `DB_HOST` | Host MySQL | `mysql` (compose) / `host.docker.internal` (dev) |
| | `DB_PORT` | Cổng MySQL | `3306` |
| | `DB_NAME` | Tên database | `nguon_goc_so` |
| | `DB_USERNAME` | User MySQL | `nguongocso` |
| | `DB_PASSWORD` | Password MySQL | *(bắt buộc điền)* |
| | `MYSQL_ROOT_PASSWORD` | Password root MySQL (compose) | *(bắt buộc điền)* |
| **Server** | `PORT` | Cổng backend | `8080` |
| | `VITE_API_URL` | Base URL API cho frontend dev | `http://localhost:8080` |
| **JWT** | `JWT_SECRET` | Bí mật ký JWT (**bắt buộc production**) | *(bắt buộc điền)* |
| | `JWT_EXPIRATION` | Thời hạn token (ms) | `86400000` |
| **CORS** | `ALLOWED_ORIGINS` | Danh sách origin bổ sung (phân cách `,`) | `http://localhost:3000` |
| **Upload** | `UPLOAD_BASE_DIR` | Thư mục upload backend | `/app/uploads` |
| | `UPLOAD_FARM_LOG_RELATIVE_PATH` | Thư mục con farm-log attachments | `farm-logs` |
| | `UPLOAD_FARM_LOG_MAX_SIZE` | Dung lượng tối đa file (byte) | `5242880` |
| | `UPLOAD_INSPECTION_RESULT_RELATIVE_PATH` | Thư mục con inspection-result | `inspection-results` |
| **QR** | `QR_IMAGE_STORAGE_PATH` | Thư mục lưu ảnh QR | `/app/files/qr` |
| **Backup** | `MYSQL_DUMP_PATH` | Đường dẫn `mysqldump` | `mysqldump` (tự tìm PATH) |
| | `MYSQL_PATH` | Đường dẫn `mysql` | `mysql` |
| **Frontend** | `FRONTEND_URL` | URL frontend (dùng trong CORS/link) | `http://localhost:3000` |
| **Mail** | `MAIL_HOST` / `MAIL_PORT` | SMTP host/port | `smtp.gmail.com` / `587` |
| | `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP credentials (Gmail app password) | *(bắt buộc điền)* |
| **Geolocation** | `IP_GEOLOCATION_ENABLED` | Bật/tắt geolocation IP | `true` |
| | `IP_GEOLOCATION_BASE_URL` | Dịch vụ GeoIP | `https://ipwho.is` |
| | `LOCATIONIQ_API_KEY` | Key LocationIQ reverse geocoding | *(bắt buộc nếu dùng)* |
| | `LOCATIONIQ_BASE_URL` | Endpoint LocationIQ | `https://us1.locationiq.com/v1/reverse` |
| **Timezone** | `APP_TIMEZONE` | Múi giờ nghiệp vụ (JVM + Clock) | `Asia/Ho_Chi_Minh` |
| **JVM** | `JAVA_OPTS` | Tham số JVM | `-Xms256m -Xmx768m` |
| **Docker ports** | `FRONTEND_HOST_PORT` / `BACKEND_HOST_PORT` / `MYSQL_HOST_PORT` | Cổng host | `3000` / `8080` / `3306` |
| | `FRONTEND_CONTAINER_PORT` / `BACKEND_CONTAINER_PORT` / `MYSQL_CONTAINER_PORT` | Cổng container | `80` / `8080` / `3306` |
| **Volume** | `MYSQL_VOLUME_NAME` | Tên volume MySQL | `nguongocso_mysql_data` |

> Frontend production (container) không build sẵn API URL — nó đọc `/config.js`
> lúc runtime, do entrypoint `frontend/docker-entrypoint.d/40-runtime-config.sh`
> sinh ra từ biến `API_BASE_URL` (mặc định `/api/v1`).

---

## 7. Database

### 7.1 Khởi động database

Docker Compose dựng MySQL 8.4 với healthcheck (`mysqladmin ping`):
`backend` chỉ khởi động sau khi `mysql` đạt trạng thái healthy.

Database tên `nguongocso`, user `nguongocso`, được tạo tự động bởi biến môi
trường của container (`MYSQL_DATABASE`, `MYSQL_USER`, ...).

### 7.2 Migration (Flyway)

- Flyway được bật: `spring.flyway.enabled=true`.
- Locations: `classpath:db/migration/schema` và `classpath:db/migration/data`.
- Chạy **tự động** mỗi khi backend khởi động. Không cần lệnh riêng.
- Cấu hình an toàn: `repair-on-migrate=true`, `validate-on-migrate=false`,
  `out-of-order=true` (`application.properties`).
- Có **58 file migration** (`V1__...` → `V59__...`, V-number có khoảng trống chủ ý).
- Hibernate dùng `ddl-auto=validate` (schema do Flyway quản lý).

### 7.3 Seed data (khi database mới)

Các migration `data/` tự chèn dữ liệu khởi tạo:

| Migration | Nội dung |
|---|---|
| `V14__seed_roles.sql` | 6 vai trò `VT-01`…`VT-06` |
| `V15__seed_permissions.sql` | Danh mục permissions (resource × action) |
| `V16__seed_role_permissions.sql` | Gán permission mặc định cho từng role |
| `V17__seed_default_admin.sql` | Tổ chức `SYSTEM` + tài khoản `admin/admin123` (VT-01) |
| `V18__seed_backup_schedule.sql` | Lịch sao lưu mặc định |
| `V29/V34/V37/V46/V52` | Nội dung hướng dẫn sử dụng (in-app help) |
| `V43__seed_administrative_units.sql` | Đơn vị hành chính (phân công địa bàn VT-05) |
| `V57__seed_role_test_accounts.sql` | 3 tổ chức demo + 5 tài khoản demo theo role |
| `V58__seed_demo_data_vt02.sql` | Dữ liệu demo VT-02 (15 vùng trồng, 15 lô, chứng nhận, kiểm nghiệm…) |

### 7.4 Sao lưu & phục hồi (Backup / Restore)

Hệ thống hỗ trợ sao lưu/phục hồi **qua API và UI** (chỉ VT-01 qua menu
**Hệ thống → Sao lưu & Phục hồi dữ liệu**), dùng CLI `mysqldump` / `mysql`
(từ image backend hoặc cấu hình `MYSQL_DUMP_PATH` / `MYSQL_PATH`):

| Action | Endpoint / nơi thực hiện |
|---|---|
| Xem lịch sao lưu | `GET /api/v1/backups/schedules` |
| Cấu hình lịch | `POST /api/v1/backups/schedules` |
| Sao lưu thủ công | `POST /api/v1/backups/trigger` |
| Lịch sử | `GET /api/v1/backups/history?page=&size=` |
| Tải file | `GET /api/v1/backups/history/{id}/download` |
| Xóa bản sao lưu | `DELETE /api/v1/backups/history/{id}` |
| Phục hồi | `POST /api/v1/backups/history/{id}/restore` |

> Khi phục hồi, hệ thống tự bật **maintenance mode** (toàn bộ request khác bị
> chặn với 503) cho đến khi hoàn tất (`RestoreService` / `MaintenanceFilter`).
> Chi tiết: `docs/api/backup/backup-restore-api.md`.

---

## 8. Logs

```bash
# Theo dõi log tất cả service
docker compose logs -f

# Log từng service
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

Với chế độ dev:

```bash
# Backend log ra console của terminal ./mvnw spring-boot:run
# Frontend log ra console của terminal npm run dev
```

Backend dùng SLF4J/Logback; mức log cấu hình trong `application.properties`
(`logging.level.*`, mặc định root = INFO).

---

## 9. Troubleshooting

Các lỗi dưới đây đều có căn cứ từ cấu hình/behaviour thực tế trong repository.

| Hiện tượng | Nguyên nhân có thể | Cách xử lý |
|---|---|---|
| `/actuator/health` trả `"DOWN"` nhưng API vẫn hoạt động | Health tổng hợp nhiều indicator; SMTP credentials sai/thiếu làm `MailHealthIndicator` fail (WARN `Mail health check failed` trong log) → tổng thể DOWN, DB/API không bị ảnh hưởng. *Đã gặp thực tế trên môi trường local.* | Sửa SMTP username/password trong `.env` cho đúng rồi restart backend; hoặc chấp nhận DOWN nếu không dùng tính năng mail. |
| Backend không khởi động, lỗi kết nối MySQL | `DB_HOST` trỏ sai. Trong compose phải là `mysql`; khi chạy dev phải là host/localhost/RDS. | Sửa `.env` (`DB_HOST`, `DB_PORT`, `DB_NAME`, credentials) rồi `docker compose up -d --build` lại. |
| 401/403 khi gọi API dù đã đăng nhập | Token thiếu/hết hạn; hoặc chưa hoàn tất bước chọn tổ chức (cần **Access JWT**, không phải Selection JWT). | Đăng nhập lại qua `POST /api/v1/auth/login` → chọn org → dùng Access JWT mới. |
| Lỗi CORS khi frontend gọi API từ origin lạ | Origin chưa nằm trong CORS allowlist. | Thêm origin vào `ALLOWED_ORIGINS` trong `.env`. Lưu ý: `SecurityConfig` hiện set allowedOriginPatterns `*` kèm credentials — xem [SECURITY.md](./SECURITY.md) mục risk. |
| Flyway migration lỗi `checksum mismatch` / duplicate version trên database đã có | Schema cũ từng chạy trước đó. | Project đã bật `repair-on-migrate=true`; để tự sửa các bản record cũ. Nếu vẫn lỗi, cần kiểm tra lại chuỗi version (cách đổi tên bulk đã thực hiện trong commit `d0c78e51`). |
| App frontend không gọi đúng API khi deploy | `/config.js` chưa được thay thế placeholder. | Đảm bảo entrypoint `40-runtime-config.sh` chạy với biến `API_BASE_URL` đúng (mặc định `/api/v1` same-origin). |
| Sao lưu thất bại vì thiếu `mysqldump` | Môi trường không có binary MySQL. | Cài mysql-client hoặc cấu hình `MYSQL_DUMP_PATH` / `MYSQL_PATH` (image backend Docker đã cài `mysql-client`). |
| Thời gian hiển thị lệch giờ | Múi giờ JVM/container mặc định UTC. | Hệ thống đã set `APP_TIMEZONE=Asia/Ho_Chi_Minh` mặc định; nếu bị lệch, kiểm tra biến `APP_TIMEZONE` và `-Duser.timezone`. |
| `docker compose` báo thiếu biến môi trường | `.env` chưa tồn tại hoặc thiếu biến. | `cp .env.example .env` và điền đủ biến bắt buộc. |

---

## 10. NOT VERIFIED

Phần dưới đây **chưa được kiểm chứng runtime** tại thời điểm viết tài liệu
(phase audit chỉ đọc repository):

- Trạng thái `UP` của `/actuator/health` trên môi trường thật.
- Hành vi backup/restore end-to-end trên dữ liệu thực.
- Log lỗi cụ thể theo từng môi trường (staging/production) ngoài CI artifact hiện có.