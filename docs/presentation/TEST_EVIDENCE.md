# Bằng chứng Kiểm thử — Nguồn Gốc Số (Traceability Matrix)

> Traceability matrix giữa **Acceptance Criteria (TC-01 → TC-04)** và bằng chứng
> kiểm thử. Status chỉ nhận: **PASS / FAIL / NOT VERIFIED**.
>
> ⚠️ **Nguyên tắc:** Không đánh `PASS` nếu không có bằng chứng kiểm chứng thực tế
> (runtime/API/UI).
>
> ✅ **Cập nhật 02/09/2026 — Phase 4 Final Verification:** TC-01 và TC-03 đã được
> **kiểm chứng runtime thực tế** (API live trên backend đang chạy). TC-02 có bằng
> chứng mức unit-test nhưng chưa verify UI đầy đủ → giữ `NOT VERIFIED`. TC-04 xác
> nhận thêm bằng runtime → giữ `FAIL` (hiện trạng điều tra ngày đó: mục 6.6).
> Chi tiết: mục 3.4, 5.4, 6.6; lịch sử mục 8.
>
> 🔧 **Cập nhật 03/09/2026 — TC-04 implementation hoàn tất:** gap TC-04 đã được
> triển khai (build-info + `/actuator/info` JWT-only + deploy image commit-SHA +
> CI evidence step — chi tiết mục 6). Local verify PASS — backend test 578/578,
> `package -Pprod` OK (mục 6.2). Status chuyển `FAIL` → `NOT VERIFIED`, chờ
> evidence runtime từ cluster sau lần deploy CI tiếp theo (mục 6.5) trước khi
> đánh `PASS`.

- Mã story: **NCL-10-CN-011-CV-05**
- Tham chiếu: [DEMO_SCRIPT.md](./DEMO_SCRIPT.md),
  [DEMO_DATA.md](../handover/DEMO_DATA.md)

---

## 1. Bảng tổng hợp

| Acceptance Criterion | Trạng thái | Bằng chứng |
|---|---|---|
| **TC-01** — Luồng truy xuất đầy đủ (lô → sự kiện → kiểm nghiệm → tem → quét → tra cứu) | `PASS` ✅ | **E2E sống 02/09/2026** — chạy đủ chuỗi API thật trên 1 lô mới: tạo lô → duyệt → thu hoạch (201) → đóng gói (201) → lô hàng mã `HX00000011` → kích hoạt → tra cứu public (200, 2 sự kiện) → scan (200). Chi tiết mục 3.4 |
| **TC-02** — Không dữ liệu → Dashboard/List → Empty state, không lộ lỗi kỹ thuật | `NOT VERIFIED` | UI code có empty-state pattern (mục 4) + unit test empty-state PASS (`AreaAssignmentPage.test.tsx` TC-A; Vitest 46/46 PASS 02/09/2026); **API-level đã kiểm chứng 02/09/2026**: org `DEMO_NSV` (VT-04, không có lô) → `GET /production-lots` trả **HTTP 200 `{"data":[]}`** (không 500, không lộ lỗi kỹ thuật); chưa verify UI walkthrough đầy đủ với DB trống |
| **TC-03** — Unauthenticated → Protected page/API → Login/Unauthorized, không lộ dữ liệu | `PASS` ✅ (API) | **Runtime 02/09/2026:** 8/8 endpoint protected không token → **403 với body rỗng** (không lộ dữ liệu/stacktrace); token giả → 403; `GET /public/trace/HX00000001` không token → 200 (đúng thiết kế public). Chi tiết mục 5.4 |
| **TC-04** — Deployment → Version → Deployment timestamp | `NOT VERIFIED` (implemented 03/09/2026) | **Gap đã được implement** (mục 6): `build-info` → `/actuator/info` (vẫn yêu cầu JWT), K8s deploy image `:<commit-sha>`, deployment time = pod `creationTimestamp`, CI evidence step. Local verify PASS — test 578/578 + `package -Pprod` OK (mục 6.2); **chờ evidence runtime từ cluster** sau lần deploy CI tiếp theo (mục 6.5) trước khi đánh `PASS` |

---

## 2. Định nghĩa status

- **PASS:** có bằng chứng kiểm thử thực tế (screenshot, log API, kết quả run test).
- **FAIL:** kiểm thử/điều tra xác định hệ thống **không đáp ứng** tiêu chí.
- **NOT VERIFIED:** chưa có bằng chứng hoặc chưa chạy kiểm thử phù hợp.

---

## 3. TC-01 — Bằng chứng (Luồng truy xuất nguồn gốc)

### 3.1 Flow thực tế (đối chiếu code)

```text
Tạo lô sản xuất
   → POST /api/v1/production-lots                     (ProductionLotController)
   ↓
Ghi sự kiện chuỗi
   → ChainEvent (harvest/packaging/preprocessing/transport), ProcurementEvent,
     WarehouseReceipt
   ↓
Kiểm nghiệm & chứng nhận
   → inspection-requests + results; certifications gắn lô
   ↓
Cấp mã/tem truy xuất
   → Tạo dải mã (admin) → Tạo lô hàng (POST /shipments) → sinh trace codes
   → Kích hoạt tem (POST /shipments/{id}/activate)
   ↓
Người tiêu dùng quét mã
   → POST /api/v1/public/trace/{code}/scan             (PublicTraceController)
   ↓
Tra cứu công khai
   → GET /api/v1/public/trace/{code}
   → Timeline + chứng nhận + kết quả kiểm nghiệm công khai
```

### 3.2 Test hiện có (evidence từ repository)

| Tầng | Test file (backend) | Nội dung kiểm thử |
|---|---|---|
| Tạo lô | `ProductionLotControllerTest`, `ProductionLotServiceTest`, `UpdateProductionLotServiceTest` | Tạo lô, submit, approve |
| Sự kiện | `ChainEventControllerTest`, `ChainEventServiceImplTest`, `ProcurementEventControllerTest`, `WarehouseReceiptControllerTest`, `OfflineSyncServiceImplTest` | Ghi sự kiện, timeline, offline sync |
| Kiểm nghiệm | `InspectionRequestServiceImplTest`, `InspectionCriterionResultServiceImplTest`, `DossierServiceTest` | Yêu cầu kiểm nghiệm, ghi kết quả |
| Cấp tem | `ShipmentControllerTest`, `ShipmentServiceImplTest`, `ShipmentServiceTest`, `CodeRangeControllerTest`, `CodeRangeServiceTest`, `LabelExportServiceImplTest` | Tạo lô hàng, sinh mã, kích hoạt, xuất tem |
| Consumer | `PublicTraceControllerTest`, `PublicTraceServiceImplTest` | Tra cứu công khai, scan, recall |

### 3.3 Cách verify để đạt PASS (chạy theo DEMO_SCRIPT.md)

1. Chạy `docker compose up -d --build` và login `orgmanager/admin123`.
2. Thực hiện đủ **Step 1 → Step 8** trong `DEMO_SCRIPT.md`.
3. Ghi screenshot mỗi bước; ghi mã truy xuất đã dùng ở bước quét.
4. Cập nhật bảng này: TC-01 → `PASS` (kèm evidence path).

### 3.4 ✅ Bằng chứng E2E sống (02/09/2026 — Phase 4 Final Verification)

Chạy trực tiếp API trên backend thật (port 8080, DB đã seed V57/V58), **đủ chuỗi
đầu-cuối trên một lô mới tạo** ("Lo Demo Bao Ve 01" — HTX Nông Sản Demo / `DEMO_HTX`):

| # | Mắt xích | API (thật) | Kết quả |
|---|---|---|---|
| 1 | Đăng nhập 2 bước | `POST /auth/login` → `GET /auth/organizations` → `POST /auth/select-organization` | ✅ 200 — accessToken VT-02, tổ chức `DEMO_HTX` |
| 2 | **Tạo lô** | `POST /production-lots` (name, farmAreaId, productCategoryId, expectedQuantity 500 kg) | ✅ 200 — `Lo Demo Bao Ve 01` |
| 3 | Trình duyệt | `POST /production-lots/{id}/submit` → `POST /production-lots/{id}/approve` body `{"approved":true}` | ✅ 200/200 — trạng thái `APPROVED` |
| 4 | **Ghi sự kiện — thu hoạch** | `POST /chain-events/harvest` (480 kg, 2026-09-02) | ✅ **201** — `HARVEST`, người ghi "Quản lý HTX Demo (VT-02)" |
| 5 | **Ghi sự kiện — đóng gói** | `POST /chain-events/packaging` ("Tui zip 1kg") | ✅ **201** — `PACKAGING` |
| 6 | **Cấp tem/mã truy xuất** | `POST /shipments` (lô đã đóng gói) → sinh mã **`HX00000011`** từ dải mã đã cấp, trạng thái `CODE_PRINTED` | ✅ 200 |
| 7 | Kích hoạt tem | `POST /shipments/{id}/activate` | ✅ 200 — `ACTIVATED` |
| 8 | **Consumer tra cứu (không đăng nhập)** | `GET /public/trace/HX00000011` | ✅ 200 — `{lot: "Lo Demo Bao Ve 01", events: 2, shipment: "ACTIVATED", recalled: false}` |
| 9 | **Consumer quét mã** | `POST /public/trace/HX00000011/scan` | ✅ 200 — ghi nhận lượt quét |

> **Ghi chú nghiệp vụ phát hiện khi verify (đúng thiết kế, không phải lỗi):**
> - Lô phải **`APPROVED`** mới ghi được sự kiện thu hoạch (thử trên lô `HARVESTED`
>   seed → 400 "Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.").
> - Lô phải có sự kiện **đóng gói** mới tạo được lô hàng (thử trên lô chỉ thu hoạch
>   → 400 "Chỉ có thể tạo lô hàng từ lô sản xuất đã đóng gói.").
> - `approve` yêu cầu body `{"approved": true}` (validation `ApproveProductionLotRequest`).
>
> Mắt xích **Kiểm nghiệm** được minh chứng bằng seed V58 (15 yêu cầu kiểm nghiệm /
> 42 kết quả chỉ tiêu, hiển thị ở trang public — `DEMO_SCRIPT.md` Step 8) và backend
> tests (`InspectionRequestServiceImplTest`, `InspectionCriterionResultServiceImplTest`
> — PASS 575/575 ngày 02/09/2026); không tạo yêu cầu kiểm nghiệm mới trong lần chạy E2E này.

**Kết luận: TC-01 = `PASS`** (bằng chứng API-level toàn chuỗi). UI walkthrough theo
`DEMO_SCRIPT.md` Step 1→8 trước buổi demo sẽ bổ sung screenshot.

---

## 4. TC-02 — Bằng chứng (Empty state)

### 4.1 Hiện trạng code (UI)

Danh sách chính dùng chung pattern empty-state (component `DataTableShell` /
`ListCard`, props `empty` + `loadingMessage`/`emptyMessage`):

- `frontend/src/pages/certification/CertificationListPage.tsx`
  (`empty={!loading && items.length === 0}`)
- `frontend/src/pages/farm-area/FarmAreaListPage.tsx`
- `frontend/src/pages/admin/CodeRangeListPage.tsx`
- `frontend/src/pages/recall-request/RecallRequestListPage.tsx`
- `frontend/src/pages/warehouse-receipt/WarehouseReceiptPage.tsx`
- Dashboard không khớp role → message mặc định thân thiện
  (`DashboardContent.tsx`: "Không có Dashboard phù hợp với vai trò hiện tại.")

→ UI **được thiết kế** không hiển thị lỗi kỹ thuật khi trống dữ liệu.

### 4.2 Trạng thái

```text
TC-02 = NOT VERIFIED
```

> Bằng chứng mức **unit** (02/09/2026, Vitest — 8 file / 46 test PASS):
> `AreaAssignmentPage.test.tsx` — *"TC-A: hiển thị empty-state đúng chuỗi khi cán bộ
> đầu tiên chưa được gán địa bàn"* ✅. Đây là empty-state ở mức component với dữ liệu
> mock rỗng; chưa verify **toàn app với DB trống** (mục 4.3) → giữ `NOT VERIFIED`.

### 4.3 Cách verify

1. Dựng môi trường **không seed dữ liệu nghiệp vụ** (tạm thời tắt V58 trong
   `spring.flyway.locations` hoặc dùng DB mới không chạy migrations data —
   **chỉ làm trên môi trường test, không sửa source**).
2. Login một tài khoản thuộc tổ chức không có dữ liệu (hoặc org mới tạo).
3. Mở **Dashboard**, **Lô sản xuất**, **Vùng trồng**, **Chứng nhận** → chụp screenshot
   empty state.
4. Xác nhận: thông báo tiếng Việt thân thiện, **không có stacktrace/error kỹ thuật**,
   không có lỗi JS trong console.
5. Cập nhật TC-02 → `PASS`.

---

## 5. TC-03 — Bằng chứng (Protected page/API)

### 5.1 Hiện trạng code

**Backend — `SecurityConfig.java`:**

- `permitAll` chỉ cho: login/forgot/reset-password, `GET|POST /auth/organizations`,
  `POST /auth/select-organization`, `/auth/organizations`, `/api/v1/public/**`,
  `/api/v1/partner/**`, `/actuator/health`, `/files/qr/**`, `OPTIONS`.
- Toàn bộ API còn lại: `anyRequest().authenticated()` → 401/403 nếu thiếu token.

**Frontend:**

- `PrivateRoute` (`AppRoutes.tsx`): chưa đăng nhập → `Navigate("/login")`.
- `RoleRoute` (`components/auth/RoleBasedRoute.tsx`): không đủ role → `Navigate("/dashboard")`.
- `AuthContext` + `utils/session.ts`: phát hiện token hết hạn/bị xóa → logout tập trung → `/login`.

**Test liên quan:** `AuthControllerTest`, `PublicTraceControllerTest`
(`@Import(SecurityConfig.class)`), frontend `AuthContext.test.tsx`, `session.test.ts`.

### 5.2 Trạng thái

```text
TC-03 = PASS  (API đã kiểm chứng runtime 02/09/2026 — xem mục 5.4)
```

> **Phạm vi:** backend API (ranh giới lộ dữ liệu) đã verify sống. Frontend route guard
> được bảo vệ bởi unit test PASS 02/09/2026 (`AuthContext.test.tsx`, `session.test.ts`,
> `roleAccess.test.ts`, `axiosConfig.test.ts`). Bước redirect UI thủ công (mục 5.3,
> browser) nên thực hiện nhanh trước buổi demo để có screenshot.

### 5.3 Cách verify (cả frontend & backend)

**Backend (curl):**

```bash
# 1. Gọi API protected khi chưa có token → kỳ vọng 401/403, response KHÔNG chứa dữ liệu
curl -i http://localhost:8080/api/v1/production-lots

# 2. Gọi API protected với token giả → kỳ vọng 401/403
curl -i http://localhost:8080/api/v1/production-lots \
  -H "Authorization: Bearer invalid.token.here"
```

**Frontend (browser):**

1. Truy cập trực tiếp `/dashboard` khi chưa đăng nhập → bị redirect `/login`.
2. Truy cập trực tiếp `/members` với tài khoản VT-03 (không có quyền) → redirect `/dashboard`.
3. Xác nhận trang nào cũng **không lộ dữ liệu** cho người chưa xác thực.
4. Chụp screenshot, ghi log response status. Cập nhật TC-03 → `PASS`.

### 5.4 ✅ Bằng chứng runtime (02/09/2026 — Phase 4 Final Verification)

Thực hiện đúng các lệnh ở mục 5.3 trên backend đang chạy (port 8080):

| # | Request (không có token) | Kết quả | Đánh giá |
|---|---|---|---|
| 1 | `GET /api/v1/production-lots` | **403**, body rỗng | ✅ Chặn, không lộ dữ liệu |
| 2 | `GET /api/v1/shipments` | **403**, body rỗng | ✅ |
| 3 | `GET /api/v1/farm-areas` | **403**, body rỗng | ✅ |
| 4 | `GET /api/v1/certifications` | **403**, body rỗng | ✅ |
| 5 | `GET /api/v1/inspections` | **403**, body rỗng | ✅ |
| 6 | `GET /api/v1/admin/code-ranges` | **403**, body rỗng | ✅ |
| 7 | `GET /api/v1/users` | **403**, body rỗng | ✅ |
| 8 | `GET /api/v1/organizations` | **403**, body rỗng | ✅ |
| 9 | `GET /api/v1/production-lots` + `Bearer invalid.token.here` | **403** | ✅ Token giả bị từ chối |
| 10 | `GET /actuator/info` | **403** | ✅ Không public (chỉ `/actuator/health`) |
| 11 | `GET /api/v1/public/trace/HX00000001` | **200** | ✅ Đúng thiết kế — endpoint public, chỉ dữ liệu công khai |
| 12 | `GET /actuator/health` | **200** `{"status":"UP"}` | ✅ Đúng thiết kế |

> Không có response 403 nào chứa stacktrace/internals (body rỗng hoặc ApiResult chuẩn
> — `GlobalExceptionHandlerTest` PASS trong 575/575 ngày 02/09/2026).

**Kết luận: TC-03 = `PASS` (API).** Bước redirect UI (mục 5.3 — browser) khuyến nghị
làm nhanh trước demo để có screenshot.

---

## 6. TC-04 — Bằng chứng (Version & Deployment time)

### 6.1 Hiện trạng sau khi implement (03/09/2026)

> Trước 03/09/2026: ❌ không có build-info, `/actuator/info` → 403 (và chưa expose
> qua web), K8s deploy `latest`/`edge`, không có deployment time — xem lịch sử ở
> mục 6.6 và `DEPLOYMENT.md` §10.

| Cơ chế | Trước | Sau khi implement (03/09/2026) |
|---|---|---|
| Build info | ❌ Không có | ✅ `spring-boot-maven-plugin:build-info` → `META-INF/build-info.properties`: `build.version=01`, `build.time`, `build.git.commit` |
| `/actuator/info` | ❌ 403 + chưa expose | ✅ Expose qua web (`management.endpoints.web.exposure.include=health,info`); **vẫn yêu cầu JWT** — security giữ nguyên, chỉ `/actuator/health` public (không vi phạm TC-03) |
| Git commit ↔ image | ⚠️ GHCR có tag `github.sha` nhưng deploy không dùng | ✅ CI truyền `GIT_COMMIT` vào Docker build → JAR build-info `git.commit` + OCI label `org.opencontainers.image.revision` |
| K8s deploy image | ❌ `latest`/`edge` (mutable) | ✅ CI deploy `ghcr.io/nguongocso/nguongocso-{backend,frontend}:<commit-sha>` (immutable; `latest`/`edge` vẫn được push cho convenience) |
| Deployment time | ❌ Không có | ✅ Pod `creationTimestamp` (= thời điểm rollout thực tế, UTC) + annotation `kubernetes.io/change-cause` (commit + run URL) + CI evidence log |
| CI evidence | ❌ Không có | ✅ Step **"Collect deployment evidence (TC-04)"** sau `rollout status`: in Version/Commit/Image/Environment/Deployment Time (không in secret) |

### 6.2 Bằng chứng local (03/09/2026) — application-level

**a) Backend test suite** — `./mvnw clean test -Dgit.commit=c5c516f3f176693e3fb018b2602e533632d2a5c4`
(SPRING_PROFILES_ACTIVE=test):

```text
[INFO] Tests run: 578, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- 575 test cũ PASS + **3 test mới `BuildInfoActuatorTest` PASS**:
  1. build-info được sinh với `version=01`, `build.time`, `git.commit`;
  2. `GET /actuator/info` (có xác thực) → **200**, body chứa `build.version=01`,
     `build.time`, commit — content thực tế qua security filter chain thật;
  3. `GET /actuator/info` (không token) → **403** — xác nhận endpoint **không public**,
     security policy không bị thay đổi vì TC-04.

**b) Build production JAR** — `./mvnw clean package -Pprod -Dgit.commit=c5c516f3...`
(cùng lệnh với Dockerfile, test đã chạy riêng ở CI):

```text
[INFO] BUILD SUCCESS
target/backend-01.jar
```

`backend/target/classes/META-INF/build-info.properties` (nội dung thực tế):

```properties
build.artifact=backend
build.git.commit=c5c516f3f176693e3fb018b2602e533632d2a5c4
build.group=vn.nguongocso
build.name=backend
build.time=2026-09-03T03\:37\:14.460Z
build.version=01
```

**c) Docker build với `--build-arg GIT_COMMIT=<commit>`:**

- Backend — cơ chế đã được chứng minh: Dockerfile chạy `./mvnw clean package -Pprod -B
  -Dgit.commit=${GIT_COMMIT}` (chính là lệnh đã verify ở (b) với commit thật), CI truyền
  `build-args: GIT_COMMIT=${{ github.sha }}` và gắn OCI label
  `org.opencontainers.image.revision=<sha>`. Build+push chính thức do CI thực hiện
  (step *"Build & push backend image"* + log *"Log build traceability"*).
- Frontend — **build local thành công** và kiểm chứng runtime OCI label:

  ```text
  $ docker build --build-arg GIT_COMMIT=c5c516f3f176693e3fb018b2602e533632d2a5c4 -t nguongocso-frontend:tc04-verify ./frontend   → OK
  $ docker inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' nguongocso-frontend:tc04-verify
  c5c516f3f176693e3fb018b2602e533632d2a5c4
  ```

### 6.3 Chuỗi truy vết Version → Commit → Image → Deployment → Time

```text
Version "01"  (pom.xml)
   ↓  spring-boot-maven-plugin:build-info
META-INF/build-info.properties  →  /actuator/info (yêu cầu JWT)
   { "build": { "version": "01", "time": "...", "git": { "commit": "<sha>" } } }
   ↓  CI: -Dgit.commit + --build-arg GIT_COMMIT + tag :<sha> + OCI label revision
Docker Image  ghcr.io/nguongocso/nguongocso-backend:<commit-sha>
   ↓  CI step "Set immutable commit-SHA images" → kubectl apply
Kubernetes Deployment "backend" / "frontend" (staging | production)
   ↓  kubectl rollout status → pod mới được tạo
Deployment Time (UTC) = pod metadata.creationTimestamp = thời điểm rollout thực tế
   (+ deployment annotation kubernetes.io/change-cause = commit + run URL)
```

> Deployment time **không giả lập, không hard-code** — lấy trực tiếp từ metadata
> Kubernetes (pod `creationTimestamp`), đại diện thời điểm rollout/deployment thực tế.

### 6.4 Cách verify sau mỗi lần deploy (runtime trên cluster)

```bash
# 1. Image đang chạy có phải commit-SHA image? (mục tiêu: image = backend:<sha>)
kubectl -n <namespace> get deployment backend \
  -o jsonpath='{.spec.template.spec.containers[0].image}'
# kỳ vọng: ghcr.io/nguongocso/nguongocso-backend:<commit-sha>

# 2. Deployment time = pod creationTimestamp (thời điểm rollout thực tế, UTC):
kubectl -n <namespace> get pods -l app=backend \
  -o custom-columns=NAME:.metadata.name,CREATED:.metadata.creationTimestamp,IMAGE:.spec.containers[0].image

# 3. Commit + run URL của lần deploy (change-cause):
kubectl -n <namespace> rollout history deployment/backend

# 4. Version / Build Time / Git Commit từ app.
#    Endpoint YÊU CẦU JWT (không token → 403, đúng security TC-03) — dùng token hợp lệ:
kubectl -n <namespace> exec deploy/backend -- \
  curl -s -H "Authorization: Bearer <JWT>" localhost:8080/actuator/info
# kỳ vọng: {"build":{"version":"01","time":"...","git":{"commit":"<sha>"},...}}

# 5. Đối chiếu image tag ↔ commit trên GHCR (tag <sha> tồn tại, immutable):
#    https://github.com/<owner>/nguongocso-backend/pkgs/container/nguongocso-backend
```

> ⚠️ Không làm `/actuator/info` thành public endpoint (không thêm permitAll).
> Không expose bất kỳ secret nào (JWT_SECRET, DB password, KUBECONFIG_B64,
> MAIL_PASSWORD, LOCATIONIQ_API_KEY) trong evidence.

### 6.5 Evidence runtime từ CI (điền sau lần deploy tiếp theo)

Sau khi push nhánh, CI sẽ tự in khối evidence ở step
**"Collect deployment evidence (TC-04)"** (sau `rollout status`). Copy output thật vào đây:

```text
Version          : 01
Git Commit       : <điền từ CI log — GITHUB_SHA>
Docker Image     : ghcr.io/nguongocso/nguongocso-backend:<commit-sha>
Environment      : staging | production
Deployment Time  : <điền pod creationTimestamp, UTC>
Workflow Run     : <URL GitHub Actions run>
```

> 🚦 **Điều kiện chuyển TC-04 = `PASS`:** mục 6.5 được điền bằng output thật từ CI
> (hoặc bằng cách chạy tay các lệnh 6.4 trên cluster). Không đánh `PASS` khi chưa có
> runtime evidence từ cluster; không tạo dữ liệu giả.

### 6.6 Hiện trạng TRƯỚC khi implement (lưu giữ — điều tra 02/09/2026)

| Cơ chế | Kết quả trước 03/09/2026 |
|---|---|
| Git tag | ❌ Không có tag nào trong repository |
| Docker image tag | ⚠️ Có `latest`/`edge` + tag `github.sha` trên GHCR (CI `ci-cd.yml`) nhưng deploy **không dùng** tag SHA |
| GitHub Actions / CI/CD | ✅ Pipeline deploy tồn tại (chỉ chứng minh được "đã deploy bởi workflow", không cung cấp version hiển thị) |
| Application version | ⚠️ `pom.xml` = `01`, `package.json` = `0.0.0` — không phải release version |
| Deployment timestamp/audit | ❌ Không có bảng/UI/API deployment history |
| Actuator `/actuator/info` | ❌ Không cấu hình build-info/git commit — **xác nhận runtime 02/09/2026: `curl /actuator/info` → `403`** |

---

## 7. Test hiện có trong repository (evidence tham khảo)

### 7.1 Backend (90 file test, `backend/src/test/java`)

Một số test tiêu biểu cho 4 TC:

- `ProductionLotControllerTest`, `ProductionLotServiceTest` — TC-01 (lô)
- `ChainEventControllerTest`, `ChainEventServiceImplTest` — TC-01 (sự kiện)
- `ShipmentControllerTest`, `ShipmentServiceImplTest`, `CodeRangeServiceTest` — TC-01 (tem)
- `PublicTraceControllerTest`, `PublicTraceServiceImplTest` — TC-01 (tra cứu công khai)
- `AuthControllerTest`, controller tests dùng `@Import(SecurityConfig.class)` — TC-03
- `GlobalExceptionHandlerTest` — lỗi chuẩn hóa, không lộ internals (liên quan TC-02/03)

### 7.2 Frontend (8 file test, Vitest)

- `AuthContext.test.tsx`, `utils/__tests__/session.test.ts` — TC-03 (session/login)
- `config/__tests__/roleAccess.test.ts` — phân quyền (TC-03)
- `api/__tests__/axiosConfig.test.ts` — interceptor (401 → session expiry)
- `components/organization/__tests__/DeactivateMemberDialog.test.tsx` — CRUD thành viên

> Riêng **E2E (end-to-end)** chưa có (không có Playwright/Cypress) — ghi nhận
> trong `DEPLOYMENT.md` §4.3.

---

## 8. Lịch sử cập nhật status

| Ngày | TC | Thay đổi | Người thực hiện  |
|---|---|---|------------------|
| 02/09/2026 | TC-01 | `NOT VERIFIED` → `PASS` — E2E sống đủ 9 mắt xích, mã `HX00000011` (mục 3.4) | Trần Phương Đoàn |
| 02/09/2026 | TC-02 | Giữ `NOT VERIFIED` — thêm bằng chứng unit empty-state PASS (46/46 Vitest) | Trần Phương Đoàn |
| 02/09/2026 | TC-03 | `NOT VERIFIED` → `PASS` (API) — 8/8 protected → 403 body rỗng, public → 200 (mục 5.4) | Trần Phương Đoàn |
| 02/09/2026 | TC-04 | Giữ `FAIL` — xác nhận runtime `/actuator/info` → 403, không có Git tag | Trần Phương Đoàn |
| 02/09/2026 | — | Build backend `clean verify` PASS 575/575; frontend build OK; Vitest 46/46 PASS | Trần Phương Đoàn |
| 03/09/2026 | TC-04 | `FAIL` → `NOT VERIFIED` (implemented) — build-info + `/actuator/info` (JWT-only) + deploy image commit-SHA + change-cause + CI evidence step; local verify PASS: test 578/578 (3 test mới `BuildInfoActuatorTest`), `package -Pprod` OK, build-info chứa commit `c5c516f3` (mục 6) | Trần Phương Đoàn |