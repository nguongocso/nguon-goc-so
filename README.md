# Nguồn Gốc Số — Nông sản truy xuất nguồn gốc

> Dự án nguồn gốc số là nền tảng quản lí truy xuất nguồn gốc các mặt hàng nông sản. Giúp các tổ chức minh bạch quá trình sản xuất và vận chuyển chuỗi cung ứng.

**Website triển khai:** [https://agri-trace.online](https://agri-trace.online)

---

## Tính năng hiện có

### 1. Đối với các quản trị viên của hệ thống
- Quản lý thông tin và quyền các tổ chức, doanh nghiệp và cơ quan quản lí
- Quản lý thông tin và quyền các thành viên của từng tổ chức
- Quản lý các thông tin, dữ liệu nội bộ

### 2. Đối với các hợp tác xã
I. Đối với người quản lí
- Cho phép khai báo và quản lý thông tin của các vùng trồng, lô sản xuất, lô hàng
- Quản lý thông tin, quyền sử dụng của các thành viên trong nội bộ hợp tác xã

II. Đối với thành viên khác
- Ghi sự kiện thu hoạch, đóng gói, vận chuyển, thu mua để minh bạch quá trình sản xuất và vận chuyển
- Tạo lô hàng và mã QR / tem truy xuất
- Kích hoạt, thu hồi các lô hàng
- Tạo hồ sơ, báo cáo dưới dạng PDF hoặc CSV

### 3. Đối với các doanh nghiệp
- Ghi sự kiện thu mua, xuất hồ sơ, tạo báo cáo dưới dạng PDF hoặc CSV
- Quản lý chuỗi cung ứng và sự kiện vận chuyển liên quan

### 4. Đối với người tiêu dùng
- Truy xuất / tra cứu công khai thông tin các lô hàng, lô sản xuất thông qua QR hoặc mã tem

---

## Công nghệ

### Backend
- Ngôn ngữ: Java 21
- Framework: Spring Boot 3.5.x + Spring Security 6.x + JWT
- ORM: Spring Data JPA (Hibernate 6.x)
- Database: MySQL 8.4
- Migration: Flyway 11.x
- Build: Maven 3.9+ (có `.mvn/wrapper`)
- Logging: SLF4J + Logback

### Frontend
- Ngôn ngữ: TypeScript 6.x
- Framework: React 19.x
- Build: Vite 8.x
- UI: Tailwind CSS 4.x + shadcn/ui + Base UI (`@base-ui/react`)
- HTTP Client: Axios; Routing: React Router 7
- State / Query: TanStack React Query (`@tanstack/react-query`)
- QR: `@zxing/browser`; Charts: Recharts; Maps: Leaflet

### Database
- MySQL 8.4 (`mysql:8.4` qua Docker Compose)
- Schema: Flyway (`db/migration/`)
- Migration tự động khi khởi động backend (`spring.flyway.enabled=true`)
- Database name: `nguon_goc_so`

### DevOps
- Container: Docker + Docker Compose (v2)
- CI/CD: GitHub Actions (`.github/workflows/`)
- Monitoring: Spring Boot Actuator
- Deployment thực tế: Kubernetes (k3s) + GHCR + AWS RDS; staging (`staging.agri-trace.online`) và production (`https://agri-trace.online`)

---

## Cấu trúc thư mục nhanh

```text
nguongocso/
├── backend/          # Spring Boot (Java 21, Maven)
├── frontend/         # React + Vite (TypeScript)
├── docs/             # Tài liệu (API, handover, agent workflow)
├── docker-compose.yml# Compose v2
├── .env.example      # Mẫu biến môi trường (113 dòng)
├── k8s/              # Kubernetes manifests
└── README.md         # Trang vào chính
```

---

## Yêu cầu hệ thống

| Thành phần | Phiên bản tối thiểu | Ghi chú |
|------------|---------------------|---------|
| OS | Windows / Linux / macOS | — |
| Java | 21 | `backend/Dockerfile`: `eclipse-temurin:21-jdk` |
| Maven | 3.9.x | Có `.mvn/wrapper` (`./mvnw`, `.\mvnw.cmd`) |
| Node.js | 22.x | `frontend/package.json`: React 19 / Vite 8 |
| MySQL | 8.4 | `docker-compose.yml`: `mysql:8.4` |
| Docker (optional) | 24+ / Compose v2 | — |
| Git | 2.x | — |

---

## Cài đặt & Chạy dự án

### 1. Clone

```bash
git clone https://github.com/nguongocso/nguon-goc-so.git
cd nguon-goc-so
```

### 2. Cấu hình môi trường

**Docker Compose (đơn giản nhất):**
```bash
cp .env.example .env
# Điền DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD, JWT_SECRET, v.v.
```

**Development từ source:**
- Root `.env` cho Compose; `backend/.env` và `frontend/.env` nếu tách riêng.
- Chi tiết: `docs/configuration.md`, `.env.example`.

> **Không ghi secret thật**; dùng placeholder.

### 3. Database

MySQL 8.4. Schema & migration qua Flyway (`db/migration/`). Chi tiết: `docs/installation.md`, `docs/guide_db.md`.

### 4. Backend

```bash
cd backend
# Linux / macOS / Git Bash
./mvnw clean install
./mvnw spring-boot:run
# Windows PowerShell:
# .\mvnw.cmd clean install
# .\mvnw.cmd spring-boot:run
# Port: 8080
```

> **DB sẵn sàng:** `mysql` có `healthcheck`; backend có `depends_on` + `condition: service_healthy`. Nếu chạy thủ công (không Compose), phải đợi `mysqladmin ping` trước khi `mvn spring-boot:run`.

### 5. Frontend

```bash
cd frontend
npm install
npm run dev
# Port: 3000 (khớp vite.config.ts)
# API base: VITE_API_URL=http://localhost:8080/api/v1 (khớp runtimeConfig.ts)
```

### 6. Docker Compose (toàn bộ)

```bash
docker compose up -d --build
docker compose down
docker compose down -v
```

### 7. Kiểm tra hệ thống

- Backend: `curl -I http://localhost:8080`
- Frontend: `http://localhost:3000` → DevTools → Network → API trả 200/JSON (không CORS / connection refused)
- Database: `mysql -h localhost -P 3306 -u nguongocso -p` → xem DB `nguon_goc_so`

---

## Cấu hình quan trọng

| Biến | Mục đích | Ví dụ / Mặc định |
|------|----------|----------------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | DB kết nối | `mysql` / `3306` / `nguon_goc_so` |
| `DB_USERNAME` / `DB_PASSWORD` | DB auth | `nguongocso` / (placeholder) |
| `JWT_SECRET` / `JWT_EXPIRATION` | JWT ký / hạn | (placeholder) / `86400000` |
| `ALLOWED_ORIGINS` | CORS | `http://localhost:3000` |
| `VITE_API_URL` | FE → API | `http://localhost:8080/api/v1` |
| `UPLOAD_BASE_DIR` / `QR_IMAGE_STORAGE_PATH` | File lưu | `/app/uploads` / `/app/files/qr` |
| `APP_TIMEZONE` | Múi giờ nghiệp vụ | `Asia/Ho_Chi_Minh` |

Chi tiết đầy đủ tại `docs/configuration.md` và `.env.example`.

---

## Kiểm thử

```bash
# Backend
cd backend && ./mvnw test
# Frontend
cd frontend && npm run test
# E2E không có trong dự án hiện tại; sử dụng `npm run test` nếu cần kiểm thử tự động.
```

---

## Triển khai

- Docker Compose: `docker compose up -d --build` (v2)
- Production build: `cd backend && ./mvnw clean package -Pprod`; `cd frontend && npm run build`
- CI/CD: GitHub Actions → GHCR → Kubernetes (`k8s/`) → `staging.agri-trace.online` / `agri-trace.online`
- Tài liệu deploy chi tiết: `docs/handover/DEPLOYMENT.md`, `docs/deployment-aws-ec2.md`

---

## Tài liệu liên quan

- Cài đặt / cấu hình: `docs/installation.md`, `docs/configuration.md`, `docs/environment-matrix.md`
- Vận hành / troubleshoot: `docs/handover/OPERATIONS.md`, `docs/troubleshooting.md`
- Kiến trúc: `docs/handover/ARCHITECTURE.md`
- API docs (theo domain): [`docs/api/`](docs/api/)
- Quản lý agent / lifecycle: `docs/agent/`

---

## Quy trình phát triển

- Branch: `main` → production; `develop` → staging; `feature/*`, `fix/*` cho phát triển.
- Commit convention: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`
- CI/CD chi tiết: xem `docs/handover/DEPLOYMENT.md` và `docs/deployment-aws-ec2.md`

---

## Đóng góp

Fork → branch `feature/*` → commit → PR → `develop` → CI → staging verify → `main` → tag → production.

---

## Giấy phép

MIT License.

---

## Tác giả

- Trần Phương Đoàn — Team Lead / Backend
- La Văn Hiến — Backend Developer
- Triệu Văn Đại — Backend Developer
- Trần Văn Nhu — Frontend Developer
- Lê Xuân Dương — Frontend Developer

> Liên hệ chính thức: xem `docs/handover/USER_GUIDE.md`; không đưa email cá nhân vào tài liệu kỹ thuật công khai.

---
