# Hướng dẫn Cài đặt — Nguồn Gốc Số

> **Mô tả:** Tài liệu hướng dẫn cài đặt và chạy hệ thống đúng theo repository hiện tại (`develop`).
> **Source of truth:** `https://github.com/nguongocso/nguon-goc-so.git`
> **Không sửa source code:** chỉ cập nhật tài liệu này.

---

## 1. Mục đích

Giúp thành viên mới trong team chuẩn bị môi trường cục bộ, clone repo, cấu hình biến môi trường, khởi động backend/frontend hoặc toàn bộ qua Docker Compose, kiểm tra hệ thống và reset DB khi cần.

---

## 2. Yêu cầu hệ thống

| Thành phần | Phiên bản bắt buộc | Ghi chú (từ repo) |
|------------|-------------------|-------------------|
| OS | Windows / Linux / macOS | Team dùng Windows + PowerShell |
| Java | 21 | `backend/Dockerfile`: `eclipse-temurin:21-jdk` |
| Maven / Wrapper | 3.9.x | `.mvn/wrapper` tồn tại (`mvnw`, `mvnw.cmd`) |
| Node.js | 22.x | `frontend/package.json`: React 19 / Vite 8 |
| npm | 10+ | `package-lock.json` tồn tại |
| Database | MySQL 8.4 | `docker-compose.yml`: `mysql:8.4` |
| Docker (optional) | 24+ / Compose v2 | `docker compose` (v2) |
| Git | 2.x | — |
| Browser | Chrome / Edge / Firefox | Để quét QR test |

Kiểm tra nhanh trước khi bắt đầu (PowerShell / Bash đều chạy):

```powershell
java -version        # phải có 21
.\mvnw.cmd -v         # hoặc `mvn -v`
node -v              # phải có 22
npm -v
docker compose version
mysql -V             # nếu không dùng Docker Compose
```

---

## 3. Cấu trúc repository (chỉ phần liên quan đến cài đặt)

```text
nguon-goc-so/
├── backend/
│   ├── .env.example          # cho local development (DB_HOST=host.docker.internal ...)
│   ├── mvnw / mvnw.cmd       # Maven Wrapper
│   ├── Dockerfile            # eclipse-temurin:21-jdk
│   └── src/main/resources/
│       ├── application.properties   # cấu hình Spring Boot; ddl-auto=validate; flyway.enabled=true
│       └── db/migration/
│           ├── schema/      # Flyway schema migrations
│           └── data/        # Flyway data migrations (seed, admin ...)
├── frontend/
│   ├── .env.example         # VITE_API_BASE_URL=http://localhost:8080/api/v1; VITE_USE_MOCK_INSPECTION_RESULT=false
│   ├── package.json         # React 19 / Vite 8 / TypeScript
│   ├── vite.config.ts       # port 3000; proxy /api -> localhost:8080
│   ├── Dockerfile           # node:22-alpine + nginx:1.27-alpine (port 80 container)
│   ├── nginx.conf.template  # reverse proxy /api /files /uploads
│   └── docker-entrypoint.d/
├── docker-compose.yml       # mysql:8.4 + backend + frontend; depends_on với healthcheck
├── .env.example             # root env cho Docker Compose (DB_HOST=mysql, PORT=8080, VITE_API_URL=...)
├── README.md
└── docs/
    ├── configuration.md     # bảng biến môi trường đầy đủ
    ├── troubleshooting.md
    └── guide_db.md          # Flyway, reset DB
```

---

## 4. Cấu hình môi trường

**Quan trọng:** repository có 3 file `.env` khác nhau, dùng cho 2 flow riêng.

| File | Trường hợp sử dụng | Ghi chú |
|------|-------------------|---------|
| `root .env` (tạo từ `.env.example`) | **Docker Compose** hoặc khi chạy backend trực tiếp từ root | `DB_HOST=mysql` (tên service), `DB_NAME=nguon_goc_so` |
| `backend/.env` (tạo từ `backend/.env.example`) | **Chạy backend local bằng source** (không Docker mysql) | `DB_HOST=host.docker.internal` (mặc định cho Docker mysql); `ALLOWED_ORIGINS=http://localhost`; `FRONTEND_URL=http://localhost` |
| `frontend/.env` (tạo từ `frontend/.env.example`) | **Chạy frontend local bằng `npm run dev`** | `VITE_API_BASE_URL=http://localhost:8080/api/v1` |
| `root .env`, `backend/.env`, `frontend/.env` | **Không được Git track** (`*.env` trong `.gitignore`) | Chỉ `.env.example` được commit |

### 4.1 Biến bắt buộc để khởi động (tối thiểu)

```env
DB_HOST=mysql           # Docker Compose: mysql; Local + MySQL ngoài: localhost / 127.0.0.1
DB_PORT=3306
DB_NAME=nguon_goc_so
DB_USERNAME=nguongocso
DB_PASSWORD=your_password   # placeholder — không ghi secret thật

MYSQL_ROOT_PASSWORD=your_root_password  # bắt buộc cho Docker Compose mysql

JWT_SECRET=your_jwt_secret
JWT_EXPIRATION=86400000

PORT=8080
ALLOWED_ORIGINS=http://localhost:3000

VITE_API_URL=http://localhost:8080/api/v1   # dùng cho root .env / Docker frontend
```

> Xem chi tiết đầy đủ biến tại `docs/configuration.md` (đã xác minh từ `.env.example` + `docker-compose.yml` + `application.properties`).

---

## 5. Chạy bằng source code (local development — Flow A)

### 5.1 Chuẩn bị

```powershell
# 1. Clone từ repo thật
git clone https://github.com/nguongocso/nguon-goc-so.git
cd nguon-goc-so

# 2. Chọn env phù hợp
# Nếu bạn dùng MySQL riêng trên máy (không Docker mysql):
#   - Sao chép backend/.env.example -> backend/.env; sửa DB_HOST=localhost, DB_PASSWORD
#   - Sao chép frontend/.env.example -> frontend/.env; giữ VITE_API_BASE_URL
# Nếu bạn dùng Docker mysql (hoặc muốn chạy cả backend qua Compose sau):
#   - Sao chép root .env.example -> .env; điền DB_PASSWORD, MYSQL_ROOT_PASSWORD, JWT_SECRET
```

> **Windows:** dùng `Copy-Item`. Ví dụ `Copy-Item backend\.env.example backend\.env`.

### 5.2 Database (nếu dùng MySQL ngoài Docker)

```sql
CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Flyway (`spring.flyway.enabled=true`) sẽ tự chạy khi backend khởi động; không cần chạy migration tay.

### 5.3 Chạy Backend

```powershell
cd backend

# Cài dependency
.\mvnw.cmd clean install

# Chạy (mặc định port 8080 — từ ${PORT:8080})
.\mvnw.cmd spring-boot:run
```

> **Chú ý:** file `application-dev.properties` **không tồn tại** trong repository hiện tại; nếu muốn chạy với profile `dev` hãy tạo file đó hoặc chỉ dùng lệnh trên (Spring sẽ dùng `application.properties`). Không ghi `-Dspring-boot.run.profiles=dev` nếu bạn chưa tạo file đó.

Kiểm tra backend đã chạy:

```powershell
curl -s http://localhost:8080/actuator/health
# Kỳ vọng: {"status":"UP"}
```

### 5.4 Chạy Frontend

```powershell
cd frontend
npm install
npm run dev
# Mặc định port 3000 (và proxy /api -> http://localhost:8080 qua vite.config.ts)
```

Kiểm tra frontend:

```powershell
curl -I http://localhost:3000
# Kỳ vọng: HTTP 200
```

### 5.5 Kiểm tra hệ thống (local flow)

- Backend health: `curl -s http://localhost:8080/actuator/health`
- Backend (nếu có bảo vệ): `/actuator/info` **yêu cầu JWT** (theo `SecurityConfig`), không public.
- Frontend: `http://localhost:3000` → DevTools → Network → xác nhận request `/api/v1` trả 200/JSON.
- Database: `mysql -h localhost -P 3306 -u nguongocso -p nguon_goc_so`

---

## 6. Chạy bằng Docker Compose (Flow B — khuyến nghị cho môi trường đồng nhất)

### 6.1 Chuẩn bị

```powershell
# Tạo root .env từ mẫu
Copy-Item .env.example .env

# Điền tối thiểu các biến (không ghi secret thật)
# DB_PASSWORD=your_password
# MYSQL_ROOT_PASSWORD=your_root_password
# JWT_SECRET=your_jwt_secret
```

### 6.2 Khởi động

```powershell
# Từ thư mục root (nơi có docker-compose.yml)
docker compose -f docker-compose.yml up -d --build
```

Các service:

- `mysql`: `mysql:8.4`; port host `3306`; healthcheck `mysqladmin ping`.
- `backend`: build từ `backend/Dockerfile`; port host `8080`; `depends_on` mysql với `condition: service_healthy`.
- `frontend`: build từ `frontend/Dockerfile`; port host `3000`; `depends_on` backend.

> **Lưu ý:** `docker-compose.yml` đọc `.env` ở root; nếu thiếu `MYSQL_ROOT_PASSWORD`, `DB_PASSWORD` hoặc `JWT_SECRET`, container sẽ fail hoặc backend không kết nối DB / không ký JWT.

### 6.3 Kiểm tra

```powershell
docker compose ps
# Kỳ vọng: mysql healthy; backend up (port 8080); frontend up (port 3000)

curl -s http://localhost:8080/actuator/health
curl -I http://localhost:3000
```

---

## 7. Database, Flyway và reset

### 7.1 Flyway

- Đã bật (`spring.flyway.enabled=true`) trong `application.properties`.
- Location: `classpath:db/migration/schema`, `classpath:db/migration/data`.
- Migration tự động khi backend start.
- Không hard-code số lượng migration; kiểm tra trực tiếp thư mục nếu cần.

### 7.2 Kiểm tra Flyway

Nếu backend chạy OK nhưng DB chưa có bảng, xem log: tìm dòng `Flyway` hoặc `Schema validated`.

### 7.3 Reset database (development only)

**Nếu dùng Docker Compose:**

```powershell
docker compose -f docker-compose.yml down -v
# Rồi khởi động lại để tạo DB mới từ .env
```

**Nếu dùng MySQL local (không Docker):**

```sql
DROP DATABASE nguon_goc_so;
CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Sau đó khởi động lại backend để Flyway chạy lại từ đầu.

---

## 8. Tài khoản development / demo

Theo seed data `backend/src/main/resources/db/migration/data/V17__seed_default_admin.sql`:

- **Username:** `admin`
- **Password:** `admin123`

> **Chỉ dùng cho dev/demo.** Phải đổi trước khi triển khai production.

---

## 9. Troubleshooting — Lỗi phổ biến và cách xử lý

| Triệu chứng | Nguyên nhân thường gặp | Cách kiểm tra | Cách xử lý |
|-------------|------------------------|---------------|------------|
| `Port 8080 already in use` | Có tiến trình cũ chiếm port | `netstat -ano \| findstr 8080` | Tắt tiến trình cũ hoặc đổi `PORT=8081` trong `.env` |
| Backend không kết nối DB | Sai `DB_HOST` (dùng `localhost` khi DB trong Docker là `mysql`) | Kiểm tra `.env`; `mysql -h localhost -P 3306 -u nguongocso -p` | Đặt đúng `DB_HOST=mysql` (Compose) hoặc `localhost` (local) |
| `Access denied for user` | Thiếu / sai `DB_PASSWORD` hoặc `DB_USERNAME` | Kiểm tra `.env`; thử `mysql -h ...` | Điền đúng `DB_PASSWORD`; tạo lại user nếu cần |
| `Unknown database` | Chưa tạo DB hoặc DB tên sai | `mysql -h ... -e "SHOW DATABASES;"` | Tạo `CREATE DATABASE nguon_goc_so ...` hoặc kiểm tra `DB_NAME` |
| `JWT_SECRET missing` / lỗi JWT | Thiếu `JWT_SECRET` trong `.env` | Kiểm tra log `Cannot resolve placeholder`; kiểm tra `.env` | Điền `JWT_SECRET=...` (placeholer cho dev) |
| `Flyway checksum mismatch` | Đã sửa file migration đã áp dụng | Log Flyway báo `validateOnMigrate` | Tạo migration mới (`V...__...`) hoặc reset DB (xem §7.3) |
| `Cannot resolve placeholder ${DB_HOST}` | `.env` chưa được đọc | Kiểm tra file `.env` tại đúng thư mục chạy lệnh | Đảm bảo `cp .env.example .env` đúng chỗ; với Compose phải ở root |
| Frontend không kết nối backend | Sai `VITE_API_BASE_URL`; backend chưa chạy; CORS | Browser DevTools → Network / Console | Đặt `VITE_API_BASE_URL=http://localhost:8080/api/v1`; kiểm tra `ALLOWED_ORIGINS`; đảm bảo backend chạy ở `8080` |
| Docker Compose build fail | Thiếu `package-lock.json` / `mvnw` không executable / thiếu `.env` | `docker compose logs backend` / `docker compose logs mysql` | `npm ci` trước build; `chmod +x mvnw`; điền `.env` đầy đủ; kiểm tra `MYSQL_ROOT_PASSWORD` |
| `mysql:8.4` container không healthy | Thiếu `MYSQL_ROOT_PASSWORD`; đợi quá lâu | `docker compose ps`; `docker compose logs mysql` | Điền `MYSQL_ROOT_PASSWORD`; kiểm tra `healthcheck` trong `docker-compose.yml` |

---

## 10. Dừng và reset hệ thống

**Flow A (source):**

```powershell
# Backend
Ctrl+C (trên terminal chạy mvnw)
# Frontend
Ctrl+C (trên terminal npm run dev)
```

**Flow B (Docker Compose):**

```powershell
docker compose -f docker-compose.yml down         # dừng, giữ volume DB
docker compose -f docker-compose.yml down -v      # dừng + xóa volume DB (reset)
```

---

## 11. Tài liệu liên quan (đã tồn tại trong repo)

- Cấu hình chi tiết biến môi trường: `docs/configuration.md`
- Database / Flyway / reset: `docs/guide_db.md`
- Vận hành / troubleshooting nâng cao: `docs/handover/OPERATIONS.md`
- Triển khai / CI-CD: `docs/handover/DEPLOYMENT.md`
- Kiến trúc: `docs/handover/ARCHITECTURE.md`
- API endpoint docs: `docs/api/`
- Handover / người dùng: `docs/handover/`
