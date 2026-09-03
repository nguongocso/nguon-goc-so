# Bảo mật Hệ thống — Nguồn Gốc Số

> Tài liệu bảo mật dựa trên triển khai **thực tế** (đọc source/config).
> Mọi mục được phân biệt rõ `Implemented` / `Recommended` — không biến
> recommendation thành tính năng triển khai.

- Mã story: **NCL-10-CN-011-CV-05**
- Tham chiếu: [ARCHITECTURE.md](./ARCHITECTURE.md), [DEPLOYMENT.md](./DEPLOYMENT.md)

---

## 1. Authentication — Implemented

- **JWT stateless**, SessionCreationPolicy.STATELESS (`SecurityConfig`).
- 2 loại token:
  - `ORG_SELECTION` — sau khi nhập đúng username/password, dùng để chọn tổ chức.
  - `ACCESS` — sau khi chọn tổ chức, dùng cho toàn bộ API; chứa role + organizationId.
- Password hash **BCrypt** (`BCryptPasswordEncoder`).
- Password reset an toàn hóa:
  - `POST /api/v1/auth/forgot-password`
  - `POST /api/v1/auth/reset-password/validate`
  - `POST /api/v1/auth/reset-password`
  - Token reset một lần, có hạn (`password_reset_tokens`).
- `JwtAuthenticationFilter` + `ApiKeyAuthenticationFilter` đặt trước
  `UsernamePasswordAuthenticationFilter`.
- Timezone nghiệp vụ thống nhất (`APP_TIMEZONE`, mặc định Asia/Ho_Chi_Minh) để
  tránh lệch thời gian ghi log/business.

### Account protection — Implemented

Cơ chế bảo vệ tài khoản đã triển khai (package `auth`):

| Cơ chế | Mô tả | Bằng chứng |
|---|---|---|
| Khóa tài khoản tạm thời | Tự khóa sau nhiều lần đăng nhập sai, có `lock_until`, hỗ trợ khóa vĩnh viễn | `AccountLock`, `AccountLockService`, `AccountLockExpiryScheduler` |
| Theo dõi đăng nhập bất thường | Phát hiện bất thường dựa trên IP / thiết bị / thời gian | `LoginMonitoringService`, `LoginAnomaly`, `suspicious_cases` |
| IP geolocation | Nhận diện vùng địa lý của IP đăng nhập (mặc định ipwho.is) | `app.ip-geolocation.*`, `IpUtils` (chống spoof X-Forwarded-For bằng TRUSTED_PROXY_IPS) |
| Lịch sử đăng nhập | Lưu lịch sử để thanh tra | `LoginHistoryPage`, API /login-history |

### Password policy — Implemented (một phần)

- Độ dài/tối thiểu theo Jakarta Validation trên DTO đăng nhập/đổi mật khẩu.
- Không ghi password plaintext vào log (chỉ hash).

---

## 2. Authorization & RBAC — Implemented

- **`@PreAuthorize("hasRole('VT-xx')")`** trên controller.
- **`PermissionChecker.check(resource, action)`**:
  - Permission mặc định theo role: `role_permissions`.
  - Ghi đè theo tổ chức: `organization_role_permissions` (HTX bật/tắt quyền
    cho vai trò trong tổ chức của mình).
  - Không có quyền → `403` `BusinessException("Bạn không có quyền thực hiện chức năng này.")`.

### Vai trò hệ thống

| Code | Tên | Quyền điển hình |
|---|---|---|
| VT-01 | ADMIN | Mọi quyền; quản lý tổ chức, dải mã, backup, giám sát, phân công địa bàn |
| VT-02 | ORG_MANAGER | Quản lý vùng trồng, lô, thành viên, chứng nhận, duyệt lô, phân quyền |
| VT-03 | EVENT_RECORDER | Ghi nhật ký & sự kiện chuỗi, quét mã nhanh, yêu cầu thu hồi |
| VT-04 | PROCUREMENT | Ghi sự kiện thu mua, nhập kho & đối chiếu, xuất hồ sơ GS1 |
| VT-05 | REGULATOR | Báo cáo ngành, phân tích vùng trồng, xuất dữ liệu mở (theo địa bàn) |
| VT-06 | CONSUMER | Tra cứu công khai (không vào dashboard nội bộ) |

---

## 3. Multi-tenant isolation — Implemented

- Mỗi user thuộc tổ chức qua `organization_users`; Access JWT chứa `organizationId`.
- Query dữ liệu luôn scope theo tổ chức (repository/service) — ví dụ
  `ProductionLotRepository.findByIdAndOrganization_OrganizationId(...)`.
- Báo cáo VT-05 giới hạn theo **phân công địa bàn** (`area_assignments`);
  không fallback sang toàn bộ dữ liệu khi chưa phân công.
- Public API chỉ trả **dữ liệu công khai** (xem mục 6).

---

## 4. Protected API — Implemented

### Công khai (permitAll, từ `SecurityConfig`)

```
POST /api/v1/auth/login
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
POST /api/v1/auth/reset-password/validate
GET  /api/v1/auth/organizations
POST /api/v1/auth/select-organization
/api/v1/public/**
/api/v1/partner/**          (chú ý: permitAll + tự kiểm tra API key nội bộ)
/actuator/health
/files/qr/**
OPTIONS /**
```

### Bảo vệ (cần ACCESS JWT)

```text
Mọi endpoint còn lại (/api/v1/**) → anyRequest().authenticated()
```

- Tài nguyên công khai **chỉ ở mức xem**: `PublicTraceController` trả
  `PublicTraceResponse` đã lọc — không lộ `recorded_by`, timestamp hệ thống,
  thông tin nội bộ.
- Maintenance mode: khi restore dữ liệu đang chạy, tất cả request (trừ
  `/actuator/health`, `/api/v1/backups*`, `/api/v1/admin/monitoring`) bị chặn
  với **503** (`MaintenanceFilter`).

---

## 5. Protected Frontend Routes — Implemented

- `PrivateRoute`: chưa đăng nhập → redirect `/login`.
- `RoleRoute` (`RoleBasedRoute`): role không nằm trong `allowedRoles` → redirect `/dashboard`.
- Window/session watchdog trong `AuthContext` + `utils/session.ts`: phát hiện token
  bị xóa/hết hạn → logout tập trung → redirect `/login` + thông báo.
- Route công khai chỉ: `/` (trang chủ quét mã) và `/public/trace/:codeValue`.

---

## 6. Public Data Exposure — Implemented

`PublicTraceResponse` (backend) chỉ gồm:

```
codeValue, productName, shipmentCode, shipmentStatus,
recalled, recallMessage, events[{eventType, eventData, recordedAt}]
```

- `eventData` chỉ chứa các trường được phép công khai (địa điểm, thời gian,
  mô tả sự kiện).
- Dữ liệu nội bộ (người ghi, attachment, chi tiết kinh doanh) **không** nằm
  trong response công khai.
- Lô thu hồi vẫn hiển thị nhưng kèm cảnh báo `recallMessage`.

---

## 7. Validation — Implemented

- Jakarta Bean Validation (`@Valid`) trên toàn bộ `@RequestBody` request DTO.
- Cấu hình multipart: max file 10MB (`spring.servlet.multipart.*`);
  upload farm log giới hạn `UPLOAD_FARM_LOG_MAX_SIZE` (mặc định 5MB).
- Kiểm tra nghiệp vụ trong service (BusinessException với message tiếng Việt
  mang tính người dùng, không lộ stacktrace).

---

## 8. Error Handling — Implemented

- `GlobalExceptionHandler` thống nhất:
  - `BusinessException` → HTTP code tương ứng (vd 403 khi thiếu quyền, 409 xung đột).
  - `ResourceNotFoundException` → 404.
  - `DuplicateResourceException` → 409.
  - Exception khác → 500 chung, **không lộ nội bộ**.
- Response format chuẩn `ApiResult`: `{ success, status, data, message, timestamp }`.

---

## 9. Secret Management — Implemented

| Kênh | Trạng thái |
|---|---|
| `.env.example` (git-tracked) | Chỉ chứa placeholder, không có secret thật |
| `.env` (root, `backend/`, `frontend/`) | Bị gitignore (`*.env`, `!.env.example`) |
| `k8s/secrets.yaml` | Dùng **base64 placeholder**; phải thay bằng giá trị thật khi deploy thật (hoặc dùng AWS Secrets Manager / External Secrets) |
| `k8s/configmap.yaml` | Chỉ chứa cấu hình không bí mật |
| SMTP credentials | `MAIL_USERNAME`/`MAIL_PASSWORD` từ env, không hardcode |

### Khuyến nghị (Recommended Future Improvements)

- Dùng **AWS Secrets Manager + External Secrets Operator** cho k8s (không cần
  base64 trong git), như suggestion trong `docs/deployment-aws-ec2.md` mục 3.
- Xoay vòng `JWT_SECRET` định kỳ; sử dụng secret đủ mạnh.
- Không dùng giá trị ưu tiên mặc định khi production (DV đã ghi rõ `admin123`
  phải đổi).

---

## 10. Audit Logging — Implemented

| Nguồn | Mô tả |
|---|---|
| `ActivityLogPage` / `ActivityLogService` | Lịch sử hoạt động của tổ chức (`/api/v1/organizations/activity-logs`) |
| `LoginHistoryPage` / `LoginMonitoringService` | Lịch sử đăng nhập |
| `LoginAnomalyTrackingPage` | Bất thường đăng nhập (VT-01) |
| `FailedEventLogsPage` | Nhật ký lỗi khi ghi sự kiện chain |
| `scan_logs` | Lượt quét mã truy xuất (cho thống kê & phát hiện nghi vấn) |
| Metrics | `MetricsCollectorFilter` + buffer (độ trễ request public/gateway) |

### Khuyến nghị (Recommended)

- Chưa có audit log cho toàn bộ thao tác admin CRUD (hiện chủ yếu hoạt động
  nghiệp vụ chính); có thể bổ sung nếu cần thanh tra sâu.
- Chưa có firewall WAF/rate-limiting cấp ingress (mặc định ingress-nginx).

---

## 11. CORS — Implemented + Risk

- Default origins hardcode: `http://localhost:3000`, `:5173`, `:63342`, `
  localhost`, `:5500`, `127.0.0.1:5500/5501`.
- Bổ sung orgins từ `ALLOWED_ORIGINS` (CI override theo env: staging/prod domains).
- **Risk được ghi nhận:** code hiện set `setAllowedOriginPatterns(List.of("*"))`
  **kết hợp** `setAllowCredentials(true)` (SecurityConfig). Khi dùng pattern `*`
  thì allowlist origins không có hiệu lực thực sự — kiến nghị thay bằng
  danh sách origin cụ thể trong production. **(Recommended fix — không tự sửa code trong phase này)**

---

## 12. Framework & Dependency Security — Implemented

- Backend Dockerfile chạy **non-root** user `spring` (runtime stage).
- Frontend Nginx không expose danh sách file (SPA fallback duy nhất `index.html`).
- `spring.flyway.repair-on-migrate=true`, `validate-on-migrate=false` — linh hoạt
  trong dev; cân nhắc `validate-on-migrate=true` cho production. **(Recommended)**

### Khuyến nghị (Recommended Future Improvements)

- Bổ sung dependency scanning (OSV/OWASP) vào CI.
- Bật security headers (CSP, HSTS) trong Nginx (hiện nginx.conf.template chưa có).
- Kiểm tra secret scan trong CI trước khi push.

---

## 13. NOT VERIFIED

- Kết quả quét bảo mật tự động / penetration test — chưa có evidence trong repo.
- Trạng thái cấu hình cluster thật (TLS, ingress policy) ngoài manifest đã đọc.
- `admin123` có bị đổi trên môi trường production thật hay không — chưa kiểm
  chứng được từ repository.
- Chính sách sử dụng email SMTP thật (app password) — chỉ thấy placeholder.