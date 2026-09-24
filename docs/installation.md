# Hướng dẫn Cài đặt & Tiếp nhận dự án — Nguồn Gốc Số

> **Đối tượng:** thành viên mới (dev backend/frontend) lần đầu cài dự án trên máy cá nhân.
> **Sau khi làm xong tài liệu này bạn sẽ có:** hệ thống chạy ở `http://localhost:3000`, đăng nhập được bằng `admin/admin123`, và biết đọc tài liệu nào + lệnh nào dùng hằng ngày.
> **Thời gian ước tính:** ~15 phút với Docker Compose • ~25 phút chạy từ source.
> **Source of truth:** `https://github.com/nguongocso/nguon-goc-so.git` (nhánh mặc định `develop`).
> **Quy ước lệnh:** mặc định cho **Windows PowerShell** (team dùng Windows); nếu có cách khác cho Linux/macOS, tôi ghi ngay dòng bên dưới.

**Mục lục**

- **0.** Cài nhanh trong 3 lệnh (Docker Compose)
- **PHẦN A.** Chuẩn bị môi trường — A1 công cụ • A2 chọn cách chạy • A3 kiểm tra môi trường • A4 lấy mã nguồn (clone) • A5 biến môi trường • A6 database
- **PHẦN B.** Cài đặt dự án — B1 Cách 1: Docker Compose • B2 Cách 2: chạy từ source • B3 nghiệm thu • B4 đăng nhập & dữ liệu • B5 dừng / chạy lại / reset
- **PHẦN C.** Tiếp nhận dự án để tiếp tục làm việc — C1 đọc gì trước • C2 bản đồ code • C3 quy tắc Git & nhánh • C4 những lệnh thường dùng
- **PHẦN D.** Sự cố thường gặp & Phụ lục — D1 bảng lỗi • phụ lục cấu trúc thư mục / port / biến / tài liệu liên quan

---

## 0. Cài nhanh trong 3 lệnh (Docker Compose)

> Dành cho người **chỉ muốn chạy thử dự án** ngay. Muốn hiểu vì sao, làm tiếp **Phần A → Phần B**.

```powershell
# 1) Tạo file .env từ mẫu (nếu chưa clone, xem A4)
Copy-Item .env.example .env
#    → Mở file .env, điền 3 giá trị: DB_PASSWORD, MYSQL_ROOT_PASSWORD, JWT_SECRET
#      (bí mật tùy ý, KHÔNG dùng secret thật của hệ thống, KHÔNG commit file này)

# 2) Dựng và chạy toàn bộ (MySQL + backend + frontend)
docker compose up -d --build

# 3) Kiểm tra
docker compose ps
```

**Kết quả kỳ vọng**

| Thành phần | Địa chỉ | Kỳ vọng |
|---|---|---|
| Frontend | http://localhost:3000 | Mở được trang đăng nhập |
| Backend health | `curl -s http://localhost:8080/actuator/health` | Body JSON chứa `"status":"UP"` |
| Đăng nhập | `admin` / `admin123` | Vào được Dashboard |

- Lần đầu `docker compose up -d --build` mất vài phút (build ảnh backend Maven + frontend npm).
- Gặp lỗi → xem **Phần D — bảng sự cố**.

---

## PHẦN A. CHUẨN BỊ MÔI TRƯỜNG

### A1. Công cụ cần cài trên máy

| Công cụ | Phiên bản | Bắt buộc? | Lệnh kiểm tra | Kết quả đúng |
|---|---|---|---|---|
| JDK | **21** | Bắt buộc (cả 2 flow) | `java -version` | `openjdk version "21…"` |
| Maven Wrapper | kèm trong repo (Maven **3.9.16**) | Bắt buộc với Flow A | `cd backend; .\mvnw.cmd -v` | In ra `Apache Maven 3.9.x` — **không cần cài Maven riêng** |
| Node.js | **22.x** | Bắt buộc (dev frontend) | `node -v` | `v22.x` |
| npm | 10+ | Bắt buộc | `npm -v` | `10.x` trở lên (đi kèm Node 22) |
| Git | 2.x | Bắt buộc | `git --version` | `git version 2.x` |
| Docker Desktop | 24+ | **Bắt buộc với Flow B** | `docker version` và `docker compose version` | `Docker version 24+`; `Docker Compose version v2.24` **trở lên** |
| MySQL Server | 8.4 | Tuỳ chọn — cần cho Flow A nếu không dùng Docker cho DB; **hoặc dùng MySQL của XAMPP** (xem A6.3) | `mysql -V` | `mysql Ver 8.4…` (XAMPP: mở Control Panel → bấm **Start** module MySQL) |
| Trình duyệt | Chrome / Edge / Firefox | Bắt buộc | — | Dùng đăng nhập, quét QR, tra cứu công khai |

Ghi chú:

- **Docker Compose phải ≥ v2.24** vì `docker-compose.yml` dùng `additional_contexts` (tận dụng cache `~/.m2` khi build ảnh backend). Kiểm tra bằng `docker compose version`.
- Node cũ hơn 22 có thể build lỗi → dùng nvm-windows / volta / fnm để đổi phiên bản Node.
- Không cài được Docker → chọn **Flow A** (xem A2).

### A2. Chọn cách chạy dự án (Flow A hay Flow B)

| Hạng mục | **Flow B — Docker Compose** ⭐ | **Flow A — Chạy từ source** |
|---|---|---|
| Bạn được gì | Chạy đủ MySQL + backend + frontend bằng **1 lệnh** | Chạy trực tiếp Java/Node trên máy (debug nhanh, sửa code thấy ngay) |
| Cần cài gì | Docker Desktop | JDK 21 + Node 22 + MySQL 8.4 (bản cài riêng / chạy Docker / **dùng MySQL của XAMPP** — xem A6) |
| File `.env` dùng | **root** `.env` | `backend/.env` (+ `frontend/.env` tuỳ chọn) |
| Phù hợp với | Người mới, demo, kiểm thử hệ thống | Dev viết code, cần debug/xem log nhanh |
| Chi tiết các bước | Xem **B1** | Xem **B2** |

**Khuyến nghị:** lần đầu chọn **Flow B** để chắc chắn hệ thống chạy được; khi bắt đầu task development chuyển sang **Flow A**. Kết hợp cũng được: MySQL chạy Docker + backend/frontend chạy source (xem A6).

> 🔀 **Ánh xạ thuật ngữ dùng trong tài liệu này:** **Cách 1 = Flow B (Docker Compose)** · **Cách 2 = Flow A (chạy source)**. Riêng mục **A6** liệt kê 4 cách *chuẩn bị database* (gồm cả XAMPP).

> 💡 **Windows đã có XAMPP?** Bỏ qua việc cài MySQL riêng — dùng luôn MySQL của XAMPP cho Flow A (xem **A6.3**). Lúc đó bạn chỉ cần cài thêm **JDK 21 + Node 22** là đủ.

---

### A3. Kiểm tra môi trường (chạy 1 lần)

```powershell
java -version                  # → openjdk version "21..."
node -v                        # → v22.x
npm -v                         # → 10.x
git --version                  # → git version 2.x
docker compose version         # → Docker Compose version v2.2x (≥ v2.24) — cần cho Flow B
cd backend; .\mvnw.cmd -v; cd ..   # → Apache Maven 3.9.16 (wrapper nằm trong backend/)
mysql -V                       # → mysql Ver 8.4… — chỉ cần với Flow A dùng MySQL đã cài/XAMPP
```

- Linux/macOS: dùng `./mvnw -v` sau khi `cd backend`; dùng `cp` thay cho `Copy-Item`.
- Chạy được dòng nào thì chạy; bỏ qua dòng không liên quan (ví dụ không có Docker nếu bạn chọn Flow A + XAMPP).
- Dòng nào sai phiên bản → quay lại **A1** cài/cập nhật trước khi đi tiếp.

### A4. Lấy mã nguồn (clone)

```powershell
git clone https://github.com/nguongocso/nguon-goc-so.git
cd nguon-goc-so
git checkout develop                  # nhánh mặc định của repo (origin/HEAD -> develop)
git branch --show-current             # kỳ vọng: develop
```

- Chưa có quyền truy cập repo trên GitHub? Liên hệ Team Lead cấp quyền trước khi clone.
- Bắt đầu mỗi task: tạo nhánh mới **từ `develop`** — xem **C3** (git flow).
- Linux/macOS: lệnh clone giống hệt.

### A5. Chuẩn bị biến môi trường

Repo có **3 file mẫu** `.env.example`; các file `.env` thật được tạo từ mẫu và nằm trong `.gitignore` (**không được commit**):

| File | Dùng khi | Tạo bằng (Windows / Linux) | Giá trị cần sửa |
|---|---|---|---|
| root `.env` | **Flow B** — Docker Compose | `Copy-Item .env.example .env` / `cp .env.example .env` | `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET` |
| `backend/.env` | **Flow A** — backend chạy bằng `mvnw` | `Copy-Item backend\.env.example backend\.env` | `DB_HOST=localhost` (MySQL máy chủ/XAMPP/Docker — xem A6), `DB_PASSWORD`, `JWT_SECRET`; nên đặt thêm `FRONTEND_URL=http://localhost:3000` |
| `frontend/.env` | **Tuỳ chọn** khi dev frontend | `Copy-Item frontend\.env.example frontend\.env` | Giữ nguyên `VITE_API_BASE_URL` — **không có file này vẫn chạy được** (frontend mặc định same-origin `/api/v1` qua Vite proxy `/api` → `localhost:8080`) |

**Biến bắt buộc** — nhóm này **không có giá trị mặc định** trong `backend/src/main/resources/application.properties`; thiếu *key* là backend dừng ngay với lỗi `Could not resolve placeholder …`:

| Nhóm | Biến |
|---|---|
| **Luôn bắt buộc** (cả 2 flow) | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `ALLOWED_ORIGINS`, `UPLOAD_BASE_DIR`, `UPLOAD_FARM_LOG_RELATIVE_PATH`, `UPLOAD_FARM_LOG_MAX_SIZE`, `QR_IMAGE_STORAGE_PATH`, `FRONTEND_URL`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `LOCATIONIQ_API_KEY`, `LOCATIONIQ_BASE_URL` |
| **Chỉ Flow B** (root `.env`) | `MYSQL_ROOT_PASSWORD`, `VITE_API_URL`, `MYSQL_VOLUME_NAME` (các biến port Docker đã có sẵn trong mẫu) |

> 💡 Cách an toàn nhất: **copy đủ `.env.example` → `.env` rồi chỉ đổi password/secret**. Một số biến (`MAIL_*`, `LOCATIONIQ_*`) có thể để **giá trị rỗng** nếu chưa dùng chức năng tương ứng, nhưng **key phải tồn tại**.
> ⚠️ Không ghi secret thật vào file được Git track. `.env` nằm trong `.gitignore`; chỉ `.env.example` được commit.

### A6. Chuẩn bị database

Mục tiêu của bước này rất đơn giản — làm sao để có **database tên `nguon_goc_so`** và **user tên `nguongocso`** (cổng `3306`). Có **4 cách** — bạn chọn **1 cách** phù hợp với cách chạy đã chọn ở A2:

**Cách 1 — Dùng Docker Compose (Flow B):** **không cần làm gì cả.** Docker tự tạo database và user giúp bạn, dựa vào file `.env` ở thư mục gốc.

**Cách 2 — Máy đã cài MySQL riêng (Flow A):** mở MySQL, đăng nhập bằng quyền `root`, rồi chạy sẵn **4 câu lệnh SQL** để tạo database, tạo user và cấp quyền.

**Cách 3 — Máy đã có sẵn XAMPP (Windows, Flow A):** mở **XAMPP Control Panel** → bấm **Start** cho module **MySQL** → mở phpMyAdmin (`http://localhost/phpmyadmin`) → chạy **4 câu lệnh SQL** giống Cách 2.
- Để ý 1 chút về cổng: XAMPP phải chạy ở cổng `3306` (mặc định là vậy). Nếu XAMPP đang để `3307` thì thêm `DB_PORT=3307` vào file `backend/.env`.

**Cách 4 — Chạy MySQL bằng Docker, phần code vẫn chạy trên máy như bình thường:** tạo 1 container MySQL 8.4 và trong `backend/.env` đặt `DB_HOST=localhost`.

👉 **4 câu lệnh SQL và hướng dẫn chi tiết cho cả 4 cách nằm ở [`docs/guide_db.md`](guide_db.md) (mục 2 và mục 3).** Ở đây chỉ ghi tóm tắt, tránh trùng lặp nội dung.

---

## PHẦN B. CÀI ĐẶT DỰ ÁN

### B1. Cách 1 — Chạy bằng Docker Compose (khuyến nghị)

**Bước 1 — Tạo file cấu hình**

```powershell
# mở PowerShell tại thư mục gốc của repo (đã clone ở A4)
Copy-Item .env.example .env
notepad .env     # điền 3 dòng: DB_PASSWORD, MYSQL_ROOT_PASSWORD, JWT_SECRET
```

**Bước 2 — Khởi động**

```powershell
docker compose up -d --build
```

(Lần đầu mất vài phút để build hình backend + frontend. Linux/macOS: lệnh giống hệt.)

**Bước 3 — Kiểm tra**

```powershell
docker compose ps          # mysql phải có chữ (healthy); backend, frontend phải Up
docker compose logs backend   # tìm dòng "Started BackendApplication" là backend đã chạy xong
```

- Mở trình duyệt vào http://localhost:3000 → thấy trang đăng nhập.
- Kiểm tra riêng backend: `curl -s http://localhost:8080/actuator/health` → thân phản hồi có `"status":"UP"`.
- Đăng nhập thử bằng `admin` / `admin123` (xem **B4**).

> Nếu `mysql` chưa có chữ `healthy` → gõ `docker compose logs mysql` để xem lỗi (thường là thiếu `MYSQL_ROOT_PASSWORD` trong `.env`). Xem **D1**.



---

### B2. Cách 2 — Chạy trực tiếp từ mã nguồn (Flow A)

Dành cho lúc bạn **viết code**. Làm theo đúng 3 bước:

**Bước 1 — Chuẩn bị database**

Chọn **1 trong 4 cách ở A6** (đã có database rồi thì bỏ qua).

**Bước 2 — Chạy backend** (mở terminal số 1):

```powershell
cd backend
Copy-Item .env.example .env      # tạo file cấu hình riêng cho backend
notepad .env                     # sửa: DB_HOST=localhost, DB_PASSWORD, JWT_SECRET
                                  # (XAMPP / MySQL trên máy thì để DB_HOST=localhost — xem A6)

.\mvnw.cmd clean install         # lần đầu: tải thư viện + build + chạy test
.\mvnw.cmd spring-boot:run       # khởi động backend
```

*Kỳ vọng thấy trong log:*

- dòng có chữ **Flyway** (migration đang chạy), ví dụ `Successfully applied ...`
- dòng **`Started BackendApplication ... port(s): 8080`**

*Ghi chú:*

- `clean install` chỉ cần chạy **lần đầu** (hoặc khi thêm/thay thư viện). Về sau chỉ cần `.\mvnw.cmd spring-boot:run`.
- Lệnh `spring-boot:run` sẽ “treo” terminal đó → mở **terminal thứ 2** (hoặc tab mới) cho frontend.
- Chạy test riêng: `.\mvnw.cmd test` — test dùng bộ nhớ trong (H2), **không cần** MySQL, **không cần** file `.env`.
- Linux/macOS: `./mvnw clean install` rồi `./mvnw spring-boot:run`.

**Bước 3 — Chạy frontend** (terminal số 2, ở thư mục gốc của repo):

```powershell
cd frontend
npm install
npm run dev
```

*Kỳ vọng:* terminal in ra `Local: http://localhost:3000/` → mở trình duyệt vào http://localhost:3000.

*Ghi chú:*

- Khi dev, frontend tự chuyển các request `/api`, `/uploads`, `/files` sang backend ở cổng 8080 (cấu hình trong `vite.config.ts`) → **không gặp lỗi CORS** và **không bắt buộc** phải có file `frontend/.env`.
- Linux/macOS: lệnh giống hệt.

---

### B3. Kiểm tra sau khi cài đặt (nghệm thu)

Chạy hết bảng này để chắc chắn mọi thứ ổn:

| # | Kiểm tra | Thao tác / lệnh | Kết quả đúng |
|---|---|---|---|
| 1 | Backend sống | `curl -s http://localhost:8080/actuator/health` | Thân phản hồi có `"status":"UP"` |
| 2 | Frontend mở được | Mở http://localhost:3000 | Thấy trang đăng nhập |
| 3 | Đăng nhập | `admin` / `admin123` | Vào được Dashboard |
| 4 | Log backend sạch | `docker compose logs backend` (hoặc nhìn terminal đang chạy) | Thấy `Started BackendApplication`, không có dòng lỗi đỏ |
| 5 | Test (tuỳ chọn) | `cd backend; .\mvnw.cmd test` và `cd frontend; npm run test` | Test xanh, không lỗi |

Kiểm tra thêm xem database đã có bảng chưa (chọn lệnh theo cách bạn đã làm ở A6):

```powershell
# Cách 1 (Docker Compose):
docker compose exec mysql mysql -uroot -p -e "SELECT COUNT(*) FROM nguon_goc_so.flyway_schema_history;"

# Cách 2, 3, 4 (MySQL máy / XAMPP / MySQL Docker):
mysql -u nguongocso -p -e "SELECT COUNT(*) FROM flyway_schema_history;" nguon_goc_so
```

Kỳ vọng: số dòng **> 0** và cột `success` đều là `1`.

### B4. Đăng nhập lần đầu và dữ liệu có sẵn

**Các bước đăng nhập**

1. Mở http://localhost:3000 → trang đăng nhập (hoặc vào thẳng http://localhost:3000/login).
2. Nhập **tên đăng nhập** `admin`, **mật khẩu** `admin123` → **Đăng nhập**.
3. Nếu màn hình hỏi chọn tổ chức → chọn tổ chức **Hệ thống (SYSTEM)**.

**Bảng dữ liệu bạn sẽ thấy**

| Thành phần | Local mặc định (cả Cách 1 và Cách 2) | Khi bật profile `staging` |
|---|---|---|
| Tổ chức | `SYSTEM` | thêm 3 tổ chức demo (`DEMO_HTX`, `DEMO_NSV`, `DEMO_GOV`) |
| Tài khoản | `admin` | thêm `orgmanager`, `eventrecorder`, `procurement`, `regulator`, `consumer` |
| Mật khẩu các tài khoản demo | `admin123` | `admin123` |
| Nguồn seed | `backend/src/main/resources/db/migration/` | thêm `backend/src/main/resources/db/migration-test/` |

💡 **Muốn có sẵn dữ liệu demo để tập làm quen** (nhiều tổ chức, tài khoản theo từng vai trò)? Chạy backend với profile `staging`:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "staging"
.\mvnw.cmd spring-boot:run
```

(Để trở lại bình thường: mở terminal mới, hoặc `$env:SPRING_PROFILES_ACTIVE = ""`.)

> ⚠️ Tài khoản + mật khẩu trên **chỉ dành cho dev/demo**. Lên production **PHẢI đổi**. Danh sách đầy đủ dữ liệu demo: [`docs/handover/DEMO_DATA.md`](handover/DEMO_DATA.md).

### B5. Dừng, chạy lại và reset

| Việc | Cách 2 — chạy source (Flow A) | Cách 1 — Docker Compose (Flow B) |
|---|---|---|
| **Dừng** | Bấm `Ctrl+C` ở terminal backend và terminal frontend | `docker compose down` (giữ nguyên dữ liệu DB) |
| **Chạy lại** | Chạy lại 2 lệnh ở B2 | `docker compose up -d` |
| **Reset database** (xóa sạch, làm lại từ đầu) | Xóa rồi tạo lại database theo `docs/guide_db.md` mục 8 → chạy lại backend | `docker compose down -v` (xóa cả volume DB) → `docker compose up -d --build` |

⚠️ Reset = **mất toàn bộ dữ liệu local**. Chỉ làm khi thật sự cần.

---

## PHẦN C. TIẾP NHẬN DỰ ÁN ĐỂ TIẾP TỤC LÀM VIỆC

Phần này dành cho lúc bạn **đã cài xong** và chuẩn bị bắt tay vào task thật.

### C1. Nên đọc tài liệu nào, theo thứ tự nào

| Thứ tự | Bạn muốn biết gì | Đọc tài liệu nào |
|---|---|---|
| 1 | Dự án làm gì, dùng công nghệ gì | `README.md` |
| 2 | Cách cài & cách chạy (tài liệu này) | `docs/installation.md` |
| 3 | Ý nghĩa từng biến cấu hình | `docs/configuration.md` + `.env.example` |
| 4 | Database, migration mới, reset DB | `docs/guide_db.md` |
| 5 | Chức năng này nằm ở file nào? | `docs/codebase-map.md` |
| 6 | Kiến trúc tổng thể (sơ đồ) | `docs/handover/ARCHITECTURE.md` |
| 7 | Vận hành: log, backup, port, sự cố | `docs/handover/OPERATIONS.md` |
| 8 | API cho từng lĩnh vực | `docs/api/` (theo domain: `auth`, `farm`, `trace`…) |
| 9 | Quy trình làm task bằng AI (nếu dùng) | `AGENTS.md` + `docs/agent/00-index.md` |
| 10 | Người dùng thao tác gì trên giao diện | `docs/handover/USER_GUIDE.md` |

💡 Gợi ý ngày đầu: đọc mục **1 → 2 → 5** (≈ 30 phút) là đủ để bắt tay vào task.

### C2. Bản đồ code — khi muốn sửa một chức năng

Không cần đọc hết source — hãy tra trong `docs/codebase-map.md` (chức năng → danh sách file). Tóm tắt cấu trúc:

**Backend** — `backend/src/main/java/vn/nguongocso/`:

```text
Controller → Service/Impl → Repository → Entity → MySQL
```

- Dữ liệu qua lại bọc trong `ApiResult<T>`; lỗi trả về **tiếng Việt**.
- Quyền truy cập kiểm tra bằng `@PreAuthorize` theo vai trò `VT-01` … `VT-06`.

**Frontend** — `frontend/src/`:

```text
src/api/*Api.ts (gọi Axios) → src/pages/*/*.tsx (trang) → src/routes/AppRoutes.tsx (điều hướng)
```

- Alias `@` trỏ tới `frontend/src/`.
- Chặn trang theo vai trò: `<RoleRoute allowedRoles={…}>`.

### C3. Quy tắc Git & nhánh

| Nội dung | Quy tắc |
|---|---|
| Nhánh `develop` | Là nhánh chính (code merge vào đây sẽ được deploy lên **staging**). **Không làm việc / commit trực tiếp trên `develop`** — chỉ dùng để tạo nhánh con và nhận code về, giữ cho `develop` luôn sạch |
| Nhánh làm việc | Mọi task làm ở **nhánh con**: `feature/*` (chức năng mới), `fix/*` (sửa lỗi), `docs/*` (tài liệu)… |
| Tạo nhánh cho task | `git checkout develop` → `git pull` → `git checkout -b feature/NCL-xx-cn-yyy-mo-ta` |
| Tên commit | `feat:`, `fix:`, `docs:`, `test:`, `refactor:` + mô tả ngắn |
| Gửi code | Push **nhánh con** → mở PR vào `develop` → CI chạy → verify trên staging |
| Lên production | Merge nhánh `release/vX.Y.Z` hoặc `hotfix/vX.Y.Z` vào `main` (hoặc chạy workflow thủ công) |
| Trước mỗi commit | `git status` + `git diff` — **không** commit secret, file tạm, file sinh không cần |

### C4. Những lệnh thường dùng

**a) Git**

```powershell
git status                                    # đang ở nhánh nào, có gì thay đổi
git diff                                      # xem chi tiết thay đổi trước khi commit
git fetch --all                               # lấy thông tin mới nhất từ máy chủ
git checkout develop                          # sang develop (chỉ để pull / tạo nhánh con)
git pull                                      # lấy code mới nhất
git checkout -b feature/NCL-xx-cn-yyy-mo-ta   # tạo nhánh con cho task
git add <tên-file>                            # chọn file cần commit
git commit -m "feat: mô tả ngắn"              # commit theo quy ước
git push -u origin feature/NCL-xx-cn-yyy-mo-ta # đẩy nhánh con lên máy chủ
git log --oneline -5                          # xem 5 commit gần nhất
```

**b) Docker (Cách 1 — dùng Docker)**

```powershell
docker compose up -d --build     # dựng + chạy toàn bộ (lần đầu build image)
docker compose ps                # xem trạng thái các service
docker compose logs backend      # xem log backend
docker compose logs -f backend   # xem log realtime (thoát bằng Ctrl+C)
docker compose restart backend   # khởi động lại backend
docker compose down              # dừng (giữ nguyên dữ liệu DB)
docker compose down -v           # dừng + xóa dữ liệu DB (reset)
```

**c) Nếu không dùng Docker (Cách 2 — chạy source)**

```powershell
# --- Terminal 1: Backend ---
cd backend
.\mvnw.cmd spring-boot:run       # chạy backend
.\mvnw.cmd test                  # chạy test (không cần DB)
.\mvnw.cmd clean package -Pprod  # build file sản phẩm (bỏ qua test)

# --- Terminal 2: Frontend ---
cd frontend
npm install                      # cài thư viện (lần đầu, hoặc khi đổi package.json)
npm run dev                      # chạy dev server
npm run test                     # chạy test
npm run lint                     # kiểm tra code
npm run build                    # build production (kiểm tra kiểu TypeScript + build)
```

---

## PHẦN D. SỰ CỐ THƯỜNG GẶP & PHỤ LỤC

### D1. Bảng lỗi thường gặp

> Bảng tóm tắt bên dưới. Danh sách lỗi đầy đủ hơn dành cho thành viên mới: [`docs/troubleshooting.md`](troubleshooting.md).

| Triệu chứng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `Port 8080 already in use` | Có tiến trình cũ đang chiếm cổng | `netstat -ano \| findstr 8080` → tắt tiến trình đó. **Cách 2 (source):** đổi `PORT=8081` trong `backend/.env`. **Cách 1 (Docker):** đổi `BACKEND_HOST_PORT` trong root `.env` — **không** đổi riêng `PORT`, vì nginx proxy đi theo `BACKEND_CONTAINER_PORT` |
| Backend không kết nối được DB | Sai `DB_HOST` | Cách 1: `DB_HOST=mysql`. Cách 2/3/4: `DB_HOST=localhost` |
| `Access denied for user 'nguongocso'@'localhost'` | Chưa tạo user / sai mật khẩu / thiếu `GRANT` | Chạy lại đoạn SQL ở `docs/guide_db.md` mục 2.1; đối chiếu `DB_PASSWORD` trong `backend/.env` |
| `Unknown database 'nguon_goc_so'` | Chưa tạo database hoặc tên sai | Làm lại bước ở `docs/guide_db.md` mục 2 |
| `Cannot resolve placeholder ${DB_HOST}` (hoặc biến khác) | Thiếu key trong `.env`, hoặc sai thư mục tạo file | Cách 1: `.env` phải ở **thư mục gốc**. Cách 2: `backend/.env`. Cách an toàn: copy lại từ `.env.example` |
| Lỗi JWT / `401 Unauthorized` khi gọi API | Thiếu hoặc sai `JWT_SECRET` | Điền `JWT_SECRET` (bí mật đủ dài) vào đúng file `.env` |
| Frontend không gọi được API | Backend chưa chạy / sai `VITE_API_BASE_URL` / CORS | Mở DevTools → Network xem request lỗi gì; chắc backend đang chạy; nếu gọi thẳng `:8080` thì thêm `http://localhost:3000` vào `ALLOWED_ORIGINS` |
| Container `mysql` không `healthy` | Thiếu `MYSQL_ROOT_PASSWORD` | `docker compose logs mysql` → điền biến trong root `.env` rồi `docker compose up -d` lại |
| Docker Compose build fail | Thiếu file `.env` / `mvnw` không có quyền thực thi | `docker compose logs backend`; kiểm tra `.env` ở gốc; Linux: `chmod +x backend/mvnw` |
| `Flyway checksum mismatch` | Đã sửa file migration đã chạy rồi | Repo bật `repair-on-migrate=true` nên thường **tự sửa**; nếu vẫn lỗi → tạo migration mới hoặc reset DB (`docs/guide_db.md` mục 8) |
| `java -version` không phải 21 / lỗi build Java | Cài nhầm JDK 17/23 | Cài JDK **21** (ví dụ Eclipse Temurin 21), kiểm tra lại ở **A3** |
| Build frontend lỗi / `npm ci` lỗi | Sai phiên bản Node, hoặc `node_modules` hỏng | `node -v` phải là `v22.x` (dùng nvm/volta nếu cần); xoá `node_modules` rồi `npm install` lại (dùng **npm**, không yarn/pnpm) |
| Không thấy tài khoản `orgmanager`, `procurement`… | Local mặc định **chỉ seed `admin`** | Bật profile `staging` như hướng dẫn ở **B4** |
| Backend báo lỗi kết nối DB dù XAMPP/MySQL đã có | **MySQL của XAMPP chưa Start** / sai port | Vào XAMPP Control Panel bấm **Start**; kiểm tra `netstat -ano \| findstr 3306`; nếu XAMPP dùng `3307` thì đặt `DB_PORT=3307` |

### Phụ lục

**Phụ lục 1 — Cấu trúc thư mục liên quan đến cài đặt**

```text
nguon-goc-so/
├── backend/
│   ├── .env.example          # mẫu cấu hình cho cách chạy source (Flow A)
│   ├── mvnw / mvnw.cmd       # Maven Wrapper (chỉ ở đây, không ở thư mục gốc)
│   ├── Dockerfile            # eclipse-temurin:21-jdk
│   └── src/main/resources/
│       ├── application.properties      # cấu hình Spring Boot (ddl-auto=validate, flyway bật)
│       └── db/migration/{schema,data}/ # migration nền (luôn chạy)
│       └── db/migration-test/          # seed demo (chỉ profile staging)
├── frontend/
│   ├── .env.example          # VITE_API_BASE_URL (tuỳ chọn khi dev)
│   ├── vite.config.ts        # port 3000; proxy /api, /uploads, /files → :8080
│   └── Dockerfile            # node:22-alpine + nginx:1.27-alpine (container nghe cổng 80)
├── docker-compose.yml        # mysql:8.4 + backend + frontend (cần Docker Compose ≥ 2.24)
├── .env.example              # mẫu cấu hình cho Docker Compose (Flow B)
└── docs/
    ├── installation.md       # tài liệu này
    ├── configuration.md      # bảng biến môi trường
    ├── guide_db.md           # database, SQL tạo DB/user, XAMPP, reset
    └── troubleshooting.md    # sự cố thường gặp cho thành viên mới
```

**Phụ lục 2 — Cổng & địa chỉ (môi trường local)**

| Thành phần | Địa chỉ |
|---|---|
| Frontend (dev & Docker) | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| API base của frontend | `/api/v1` (same-origin, tự proxy) |
| MySQL | `127.0.0.1:3306` |
| Tra cứu công khai | http://localhost:3000 (menu tra cứu) |

> Staging / production: xem `docs/environment-matrix.md`.

**Phụ lục 3 — Biến môi trường**

- Bảng đầy đủ: `docs/configuration.md`
- File mẫu (nguồn chính): `.env.example`, `backend/.env.example`, `frontend/.env.example`
- Nhóm biến bắt buộc: xem **A5**

**Phụ lục 4 — Tài liệu liên quan**

- Cài đặt / cấu hình: `docs/installation.md`, `docs/configuration.md`, `docs/environment-matrix.md`
- Database / Flyway / reset: `docs/guide_db.md`
- Sự cố: mục **D1** ở trên và `docs/troubleshooting.md`
- Vận hành / triển khai: `docs/handover/OPERATIONS.md`, `docs/handover/DEPLOYMENT.md`
- Kiến trúc: `docs/handover/ARCHITECTURE.md`
- API theo domain: `docs/api/`
- Bản đồ code: `docs/codebase-map.md`


---


