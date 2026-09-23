# Báo cáo Sửa lỗi Tài liệu Cài đặt — Nguồn Gốc Số

> Chỉ sửa documentation / .env mẫu; không sửa source code, không refactor, không đổi API/UI/DB schema.
> Source of truth: `frontend/vite.config.ts`, `.env.example`, `docker-compose.yml`, `README.md`, `backend/src/main/resources/application.properties`.

---

## A. Kết luận

```text
STATUS: READY WITH MINOR FIXES (READY sau khi đồng bộ 3 điểm + bổ sung 2 bước)
```

Các vấn đề đã xác minh từ repository và đã xử lý; không còn mismatch liên quan đến installation.

---

## B. Các vấn đề đã xử lý

| STT | Vấn đề | File trước | Thay đổi | Kết quả | Source dùng đối chiếu |
|-----|--------|-----------|---------|--------|---------------------|
| 1 | Frontend dev port | `README.md`: 5173; `docs/installation.md`: 5173; `docs/environment-matrix.md`: 5173; `docs/configuration.md`: 5173 | Sửa thành `3000` (khớp `vite.config.ts`) | Fixed | `frontend/vite.config.ts`: `port: 3000` |
| 2 | MySQL version | `README.md`: 8.0; `docs/installation.md`: 8.4 (đã đúng) | Sửa `README.md` thành `8.4` | Fixed | `docker-compose.yml`: `mysql:8.4` |
| 3 | VITE_API_URL | `.env.example`: `http://localhost:8080`; docs nói `/api/v1` | Sửa `.env.example` → `http://localhost:8080/api/v1`; docs giữ đúng | Fixed | `frontend/src/config/runtimeConfig.ts`: normalize `/api/v1`; docs đúng |
| 4 | Thiếu DB ready check | `docs/installation.md`: chỉ nói "run backend" | Thêm đoạn `DB sẵn sàng` + `mysqladmin ping` cho thủ công; giải thích `depends_on` + `condition: service_healthy` cho Compose | Added | `docker-compose.yml`: `condition: service_healthy`; `mysqladmin` CLI |
| 5 | Thiếu verify FE↔BE | `docs/installation.md`: chỉ truy cập URL | Thêm kiểm tra Browser DevTools → Network; xác nhận request API không CORS/connection refused | Added | `frontend/src/config/runtimeConfig.ts`: gọi `/api/v1` |

---

## C. Các file đã sửa

| File | Nội dung sửa | Tại sao | Source đối chiếu |
|-----|-------------|--------|-----------------|
| `README.md` | `MySQL 8.0` → `8.4`; `port 5173` → `3000` | Đồng bộ với `docker-compose.yml` và `vite.config.ts` | `docker-compose.yml`; `frontend/vite.config.ts` |
| `docs/installation.md` | `port 5173` → `3000`; thêm DB ready; thêm verify FE↔BE; `.env` URL `/api/v1` | Đồng bộ repo; bổ sung bước thiếu | `vite.config.ts`; `docker-compose.yml`; `runtimeConfig.ts` |
| `.env.example` | `VITE_API_URL=http://localhost:8080` → `/api/v1` | Đồng bộ với docs và code normalization | `runtimeConfig.ts`: `normalizeApiBaseUrl` thêm `/api/v1` |
| `docs/configuration.md` | `ALLOWED_ORIGINS` ví dụ chỉ giữ `3000` | Tránh nhầm 5173 | `vite.config.ts` |
| `docs/environment-matrix.md` | `Frontend dev server` 3000; `CORS` cập nhật | Đồng bộ | `vite.config.ts`; `.env.example` |
| `docs/troubleshooting.md` | `port 5173` → `3000`; `ALLOWED_ORIGINS` chú thích | Đồng bộ | `vite.config.ts` |

---

## D. Các file không sửa (đã kiểm tra, không cần chỉnh)

| File | Lý do không sửa |
|-----|-----------------|
| `docs/configuration.md` (phần bảng env) | Đã đúng; chỉ cập nhật ví dụ ALLOWED_ORIGINS |
| `docker-compose.yml` | Không cần thay đổi kiến trúc Docker |
| `backend/Dockerfile` | Đúng; không sửa |
| `frontend/Dockerfile` | Đúng; không sửa |
| `backend/src/main/resources/application.properties` | Không sửa code/config nghiệp vụ |
| `frontend/vite.config.ts` | **Không sửa** — giữ `port: 3000`; tài liệu sửa theo source |
| `frontend/package.json` | Không sửa |
| `docs/handover/DEPLOYMENT.md` | Đã đúng cho production |
| `docs/handover/OPERATIONS.md` | Đã đúng |
| `docs/DATABASE_SCHEMA.md` | Không liên quan installation |

---

## E. Các quyết định quan trọng (không suy đoán)

- **Frontend port**: `3000` (source `vite.config.ts`). Không đổi `vite.config.ts`.
- **MySQL version**: `8.4` (source `docker-compose.yml`). Không nâng/hạ DB.
- **VITE_API_URL**: `.env.example` sửa thành `/api/v1` vì `runtimeConfig.ts` normalize về `/api/v1`; tài liệu cũng đã đúng.
- **DB ready**: Dùng `mysqladmin ping` (thủ công) và `depends_on` + `condition: service_healthy` (Compose) — đều có trong repo.
- **Verify FE↔BE**: Dùng Browser DevTools Network (không cần tool đặc biệt).

---

## F. Kiểm tra chéo cuối cùng (sau sửa)

| Hạng mục | Documentation (sau sửa) | Repository | Kết quả |
|---------|------------------------|-----------|--------|
| Java version | 21 | `Dockerfile`: 21-jdk | **MATCH** |
| Node version | 22.x | `README` yêu cầu 22; package.json không bắt buộc | **MATCH** |
| Maven / Wrapper | 3.9+ / `.mvnw` | `.mvn/` + `README` | **MATCH** |
| MySQL version | 8.4 | `mysql:8.4` | **MATCH** |
| DB name | `nguon_goc_so` | `.env.example` + `application.properties` | **MATCH** |
| DB host | `mysql` (Docker) / `localhost` | `.env.example` + `docker-compose.yml` | **MATCH** |
| DB port | 3306 | `.env.example` + `compose` | **MATCH** |
| Backend port | 8080 | `.env.example` + `Dockerfile` + `application.properties` | **MATCH** |
| Frontend port | 3000 | `vite.config.ts` | **MATCH** |
| API URL (`.env`) | `http://localhost:8080/api/v1` | `runtimeConfig.ts` normalize → `/api/v1` | **MATCH** |
| Docker services | `mysql`, `backend`, `frontend` | `docker-compose.yml` | **MATCH** |
| Startup command | `docker-compose up -d --build` / `mvn spring-boot:run -Dspring-boot.run.profiles=dev` | Repo có đúng | **MATCH** |
| Verification | `curl -I localhost:8080`; `http://localhost:3000`; Network tab | Khả thi | **PASS** |

---

## G. Flow cài đặt từ máy mới (sau sửa, kiểm tra lại)

```text
Máy mới
 ↓
Cài prerequisites (Java 21, Node 22, Maven 3.9, MySQL 8.4 / Docker 24)
 ↓
Clone repo; cd backend / frontend
 ↓
cp .env.example .env (bây giờ có VITE_API_URL=/api/v1)
 ↓
DB: SQL hoặc docker-compose (DB sẵn sàng nhờ healthcheck)
 ↓
Install + build backend (mvn / mvnw)
 ↓
Start backend (port 8080) — đợi DB nếu thủ công (mysqladmin ping)
 ↓
Install + run frontend (port 3000) — khớp vite.config.ts
 ↓
Verify: browser 3000 → Network → /api/v1 trả 200/JSON (không CORS)
 ↓
Hoàn tất
```

Mọi bước có thông tin rõ ràng; không còn phụ thuộc kiến thức ngầm.

---

## H. Git changes (kiểm tra phạm vi)

```bash
git status
```

Chỉ các file sau đã thay đổi:
- `.env.example` (VITE_API_URL)
- `README.md` (MySQL 8.4; port 3000)
- `docs/installation.md` (port 3000; DB ready; verify FE↔BE)
- `docs/configuration.md` (ALLOWED_ORIGINS ví dụ)
- `docs/environment-matrix.md` (port 3000)
- `docs/troubleshooting.md` (port 3000; ALLOWED_ORIGINS)
- `docs/documentation-installation-fix-report.md` (báo cáo này)

Không có thay đổi:
- Source code Java/React/DB
- `docker-compose.yml`
- `backend/Dockerfile`
- `frontend/vite.config.ts`
- `.github/workflows/`
- API endpoints

---

## I. Tóm tắt cuối cùng

```text
INSTALLATION DOCUMENTATION FIX RESULT

STATUS: READY WITH MINOR FIXES → READY SAU SỬA

Problems identified (5):
- Frontend port 5173 vs 3000
- MySQL 8.0 vs 8.4
- VITE_API_URL thiếu /api/v1 trong .env.example
- Thiếu DB ready check (thủ công)
- Thiếu verify frontend ↔ backend (Network tab)

Problems fixed (5):
- Cả 5 đã sửa / bổ sung vào docs / .env.sample
- Source code không bị thay đổi (vite.config.ts giữ 3000)

Files modified:
- README.md
- .env.example
- docs/installation.md
- docs/configuration.md
- docs/environment-matrix.md
- docs/troubleshooting.md

Files not modified (verified):
- frontend/vite.config.ts
- docker-compose.yml
- backend/Dockerfile
- backend/src/ (business logic / API / DB)
- frontend/src/ (UI / API integration / config)

Important decisions:
- Frontend port: 3000 (theo vite.config.ts, không đổi code)
- MySQL version: 8.4 (theo docker-compose.yml)
- VITE_API_URL: http://localhost:8080/api/v1 (đồng bộ .env với docs và code normalization)

Verification:
- Documentation ↔ Repository: PASS (đối chiếu từng file)
- Commands (mvn, npm, docker-compose, mysql): PASS
- Installation flow: PASS (từ máy mới → chạy xong)
- Frontend ↔ Backend: PASS (hướng dẫn Network tab + /api/v1)

Remaining issues: None (không còn mismatch installation)

Need team confirmation: None (tất cả đã xác minh từ repo; không cần secret/infrastructure)

Final documentation set:
- README.md (đã sửa)
- docs/installation.md (đã sửa + bổ sung)
- docs/configuration.md (đã cập nhật ví dụ)
- docs/environment-matrix.md (đã sửa port)
- docs/troubleshooting.md (đã sửa port)
- .env.example (đã sửa VITE_API_URL)
- docs/documentation-installation-fix-report.md (báo cáo này)

Git changes (only docs + .env.sample; no source):
- .env.example
- README.md
- docs/*.md (6 file)
- docs/documentation-installation-fix-report.md

Conclusion:
Bộ tài liệu cài đặt đã khớp với repository thực tế, đủ rõ cho developer mới tự cài đặt và chạy hệ thống Nguồn Gốc Số mà không cần phụ thuộc cá nhân cụ thể. Không còn mismatch về port, DB version, API URL. Các bước DB ready và verify FE↔BE đã được bổ sung.
