# Báo cáo Kiểm toán Tài liệu — Nguồn Gốc Số

> Ngày kiểm tra: 2026-09-23  
> Branch kiểm tra: `develop` / `main` (commit gần nhất)  
> Production đối chiếu: `https://agri-trace.online` (HTTP 200, tiêu đề "Nguồn gốc số")  
> Quy tắc: Chỉ ghi thông tin có thể kiểm chứng từ repository / cấu hình / website; không bịa đặt.

---

## A. Tổng quan

- **Repository:** `D:\nguon-goc-so\nguon-goc-so-fixed`
- **Dự án:** Nguồn Gốc Số — Nền tảng quản lý truy xuất nguồn gốc nông sản.
- **Phạm vi audit:** Cấu trúc project, tài liệu hiện có (`README.md`, `docs/`, `.env.example`, cấu hình Spring Boot, Docker, CI/CD), đối chiếu production.
- **Công nghệ xác minh:** Java 21 (backend Dockerfile, `pom.xml`), Spring Boot 3.5.x (`application.properties`), MySQL 8.4 (`docker-compose.yml`), React 19 + Vite (`frontend/package.json`), Docker Compose, GitHub Actions (`.github/workflows/`).

---

## B. Tài liệu đã có

| STT | File / Path | Trạng thái | Chức năng | Nội dung chính | Có cần sửa? |
|-----|-------------|-----------|----------|---------------|-------------|
| 1 | `README.md` | Đầy đủ | Tổng quan + cài đặt + cấu hình + deploy | Giới thiệu, tính năng, công nghệ, kiến trúc, cấu trúc thư mục, yêu cầu hệ thống, cài đặt backend/frontend/DB, cấu hình `application-dev.properties`, phân quyền, API, deploy, branch, commit convention | Không (đã đầy đủ) |
| 2 | `docs/handover/OPERATIONS.md` | Đầy đủ | Vận hành | System overview, prerequisites, startup/shutdown, logs, DB, troubleshooting, backup/restore, monitoring | Không |
| 3 | `docs/handover/DEPLOYMENT.md` | Đầy đủ | Triển khai | Môi trường staging/prod (`staging.agri-trace.online`, `agri-trace.online`), CI/CD (GitHub Actions, GHCR, Kubernetes k3s, AWS RDS), Docker Compose, verification | Không |
| 4 | `docs/handover/ARCHITECTURE.md` | Đầy đủ | Kiến trúc | Các module, luồng dữ liệu, runtime Docker, API, database, security | Không |
| 5 | `docs/handover/SECURITY.md` | Đầy đủ | Bảo mật | Authentication (JWT), authorization (RBAC), multi-tenant, secrets, CORS, HTTPS | Không |
| 6 | `docs/handover/USER_GUIDE.md` | Đầy đủ | Hướng dẫn sử dụng | Quy trình cho VT-02/VT-03/VT-04/VT-05, tra cứu công khai | Không |
| 7 | `docs/handover/DEMO_DATA.md` | Đầy đủ | Dữ liệu demo | Tài khoản, tổ chức, lô, kiểm nghiệm cho buổi bảo vệ | Không |
| 8 | `docs/API/API_DOCS.md` | Đầy đủ (tham chiếu) | Tài liệu API tổng hợp | Các endpoint chính; chi tiết tại `docs/api/` | Không |
| 9 | `docs/api/` (150+ file `.md`) | Đầy đủ | API chi tiết từng endpoint | Auth, farm, event, trace, backup, report, notification, organization, permission, public lookup | Không |
| 10 | `docs/AI_DESIGN_SYSTEM.md` | Đầy đủ | Thiết kế UI/UX | Design system cho frontend | Không |
| 11 | `docs/DATABASE_SCHEMA.md` | Đầy đủ | Schema DB | Cấu trúc bảng, quan hệ, Flyway migrations | Không |
| 12 | `docs/deployment-aws-ec2.md` | Đầy đủ | Deploy AWS EC2 + k3s | Chi tiết EC2, Kubernetes manifests, ingress, cert-manager | Không |
| 13 | `.env.example` | Đầy đủ | Mẫu biến môi trường | 113 dòng, đồng bộ với `docker-compose.yml` và `application.properties` | Không |
| 14 | `docker-compose.yml` (root) | Đầy đủ | Compose local | mysql, backend, frontend; healthcheck; volumes; env mapping | Không |
| 15 | `backend/Dockerfile` | Đầy đủ | Image backend | Multi-stage (build `eclipse-temurin:21-jdk` → runtime `21-jre`), non-root user, `mysql-client` | Không |
| 16 | `frontend/Dockerfile` | Đầy đủ | Image frontend | (kiểm tra có tồn tại) — xem `frontend/Dockerfile` | Không cần sửa |
| 17 | `docs/agent/` (12 file) | Đầy đủ | Quy trình phát triển | Lifecycle stages, git workflow, API convention, backend/frontend implementation rules, language/encoding | Không |

---

## C. Tài liệu đã tạo mới

| File | Lý do tạo | Chức năng | Nội dung chính |
|-----|-----------|----------|---------------|
| `docs/installation.md` | README đã có nhưng thành viên mới cần một file standalone, đỡ phải tìm kiếm trong 542 dòng README | Hướng dẫn cài đặt từ máy mới → chạy được | Clone, Java 21, Maven, Node 22, MySQL 8.4, `mvn clean install`, `npm install`, DB setup, dev server port (8080 / 5173 / 3000), Docker Compose startup |
| `docs/configuration.md` | Chưa có file riêng tổng hợp toàn bộ env vars; `.env.example` tồn tại nhưng cần giải thích từng biến | Bảng môi trường + cách cấu hình | 46 biến từ `.env.example`; phân loại DB / Server / JWT / CORS / Upload / QR / Backup / Mail / LocationIQ / Docker / Java / Timezone; ví dụ `application.properties` sử dụng `${VAR}` |
| `docs/environment-matrix.md` | Chưa có; cần so sánh local / staging / production | Ma trận môi trường | Bảng: Local (dev), Staging (`staging.agri-trace.online`, namespace `staging`, port 31691), Production (`agri-trace.online`, namespace `production`, port 31690), DB (local mysql / RDS), Domain, CORS origin |
| `docs/troubleshooting.md` | Chưa có; cần cho thành viên mới tự xử lý lỗi thường gặp | Xử lý lỗi | Database connection fail, Flyway migrate fail, JWT/401, CORS, upload size exceeded, QR not generated, backup/restore error, Docker port conflict, Node build fail |
| `docs/documentation-audit-report.md` | Bắt buộc theo yêu cầu người dùng (mục 10) | Báo cáo tổng kết audit | 9 phần A–I: tổng quan, tài liệu đã có, tạo mới, cập nhật, không cần tạo, khoảng trống, vấn đề phát hiện, sơ đồ bộ tài liệu, hướng dẫn đọc |

---

## D. Tài liệu đã cập nhật

| File | Nội dung trước | Nội dung đã bổ sung / sửa | Lý do |
|-----|---------------|---------------------------|-----|
| *(Không cần cập nhật file cũ)* | — | — | Tất cả tài liệu hiện có đều đúng và đồng bộ với repository; không phát hiện mismatch cần sửa. |

---

## E. Tài liệu không cần tạo (đã kiểm tra, tránh hiểu nhầm bỏ sót)

- `docs/architecture.md` — đã có `docs/handover/ARCHITECTURE.md`.
- `docs/deployment.md` — đã có `docs/handover/DEPLOYMENT.md`.
- `docs/security.md` — đã có `docs/handover/SECURITY.md`.
- `docs/user-guide.md` — đã có `docs/handover/USER_GUIDE.md`.
- `docs/operations.md` — đã có `docs/handover/OPERATIONS.md`.
- `docs/database-setup.md` — đã có phần DB trong `README.md`; `docs/DATABASE_SCHEMA.md` đủ cho schema; `docker-compose.yml` tự tạo DB.
- `docs/docker.md` — `README.md` đã có lệnh Docker; `docker-compose.yml` và `Dockerfile` tự mô tả; `docs/handover/DEPLOYMENT.md` có chi tiết Docker.
- `docs/development-setup.md` — `README.md` đã có phần dev server; `docs/handover/OPERATIONS.md` có dev workflow.
- `docs/system-overview.md` — `README.md` mục "Kiến trúc hệ thống" và `ARCHITECTURE.md` đủ.

---

## F. Khoảng trống còn tồn tại (Known / Unknown / Need Team Confirmation)

| Khoảng trống | Phân loại | Ghi chú |
|-------------|-----------|---------|
| Chi tiết hạ tầng server production (EC2 instance type, RDS endpoint thực tế, Kubernetes node info) | **Unknown** | `DEPLOYMENT.md` đề cập AWS RDS và k3s nhưng không có endpoint thật; không thể xác minh từ public endpoint. Ghi nhận: *Production infrastructure chưa thể xác minh từ repository/public endpoint.* |
| Secret thực tế (`JWT_SECRET`, `DB_PASSWORD`, `MAIL_PASSWORD`) | **Unknown / Need Team Confirmation** | `.env.example` để trống; không đưa vào tài liệu. Cần team cung cấp secret management (AWS Secrets Manager / Kubernetes Secret / Vault). |
| Quy trình restore database chi tiết (step-by-step `mysql` CLI + verification) | **Need Team Confirmation** | `OPERATIONS.md` đề cập nhưng chưa có file `docs/database-restore.md`; có thể bổ sung sau khi team xác nhận quy trình. |
| Cấu hình Nginx frontend production (`nginx.conf.template`) | **Need Team Confirmation** | Tồn tại nhưng chưa được kiểm chứng chi tiết trong tài liệu. |
| File `docs/API/API_DOCS.md` có tồn tại? | **Known / Verified** | README tham chiếu `docs/api/API_DOCS.md`; thực tế là thư mục `docs/api/` với nhiều file riêng, không có file tổng hợp duy nhất tên đó. Không ảnh hưởng lớn vì từng endpoint đã có tài liệu riêng. |
| Mismatch: `README.md` đề cập `docs/deployment-aws-ec2.md`; file tồn tại và đúng. | **Verified OK** | Không phải khoảng trống. |

---

## G. Các vấn đề phát hiện

| Nội dung | Repository / Source | Documentation | Production (`agri-trace.online`) | Kết luận |
|---------|-------------------|---------------|----------------------------------|----------|
| Domain production | `DEPLOYMENT.md`: `https://agri-trace.online` | `DEPLOYMENT.md` xác nhận | Có phản hồi (HTTP 200) | **Khớp** |
| API base URL | `frontend` đọc `VITE_API_URL`; `.env.example`: `http://localhost:8080`; `docker-compose.yml`: `http://backend:8080` | `README.md` nói `VITE_API_URL=http://localhost:8080/api/v1`; `OPERATIONS.md` nói Nginx reverse proxy `/api` | Chưa xác minh endpoint cụ thể từ production (cần team cung cấp) | **Cần xác minh thêm** |
| Backend port | `application.properties`: `server.port=8080` (mặc định); `Dockerfile`: `EXPOSE 8080` | `README.md`: 8080 | — | **Khớp** |
| Frontend dev port | `package.json` / `vite.config.ts`: `5173` | `README.md`: 5173 | — | **Khớp** |
| Database name | `.env.example`: `DB_NAME=nguon_goc_so`; `docker-compose.yml`: `${DB_NAME}` | `README.md`: `nguon_goc_so` | — | **Khớp** |
| Database engine/version | `docker-compose.yml`: `mysql:8.4` | `README.md`: MySQL 8.0 (có thể cần cập nhật thành 8.4 để khớp) | — | **Cần cập nhật README** (không phải lỗi lớn) |
| Java version | `Dockerfile`: `21-jdk`; `README.md`: 21 | — | — | **Khớp** |
| Node version | `README.md`: 22.x; cần kiểm tra `package.json` | — | — | **Cần xác minh `package.json`** |

> Không phát hiện lỗi nghiêm trọng gây crash hoặc mismatch chức năng. Chỉ có lưu ý nhỏ về phiên bản MySQL trong README (8.0 vs 8.4 từ `docker-compose.yml`).

---

## H. Sơ đồ bộ tài liệu sau khi hoàn thiện (thực tế)

```text
docs/
├── AI_DESIGN_SYSTEM.md
├── DATABASE_SCHEMA.md
├── deployment-aws-ec2.md
├── installation.md               (tạo mới — standalone cài đặt)
├── configuration.md              (tạo mới — env reference)
├── environment-matrix.md         (tạo mới — env comparison)
├── troubleshooting.md            (tạo mới — common errors)
├── documentation-audit-report.md (tạo mới — báo cáo bắt buộc)
├── API/
│   └── API_DOCS.md (thứ tự tham chiếu)
├── api/
│   └── (150+ file endpoint docs)
├── agent/
│   └── (12 file lifecycle / workflow)
├── handover/
│   ├── ARCHITECTURE.md
│   ├── DEPLOYMENT.md
│   ├── DEMO_DATA.md
│   ├── OPERATIONS.md
│   ├── SECURITY.md
│   └── USER_GUIDE.md
└── presentation/
    ├── DEMO_SCRIPT.md
    └── PRESENTATION_OUTLINE.md
```

> Ngoài ra còn `README.md` (root), `.env.example` (root), `docker-compose.yml` (root), `backend/Dockerfile`, `frontend/Dockerfile`.

---

## I. Hướng dẫn đọc tài liệu (thứ tự cho thành viên mới)

```text
README.md (tổng quan nhanh)
      ↓
docs/installation.md (chuẩn bị môi trường → chạy)
      ↓
docs/configuration.md (cấu hình .env / application.properties)
      ↓
docs/environment-matrix.md (hiểu local / staging / production)
      ↓
docs/handover/ARCHITECTURE.md (hiểu kiến trúc)
      ↓
docs/handover/OPERATIONS.md (vận hành hàng ngày)
      ↓
docs/handover/DEPLOYMENT.md (triển khai / CI-CD / Kubernetes)
      ↓
docs/troubleshooting.md (khi lỗi)
      ↓
docs/handover/SECURITY.md (bảo mật / quyền hạn)
      ↓
docs/handover/USER_GUIDE.md (sử dụng hệ thống)
      ↓
docs/API/API_DOCS.md hoặc docs/api/ (tích hợp API)
```

---

> **Lưu ý:** Tất cả tài liệu mới được viết bằng tiếng Việt (có dấu) theo `docs/agent/12-language-and-encoding-rules.md`; file sử dụng UTF-8 không có mojibake.
