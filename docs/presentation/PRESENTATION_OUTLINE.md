# Outline Thuyết trình Bảo vệ — Nguồn Gốc Số

> Khung slide + gợi ý nội dung cho buổi bảo vệ. Mọi thông tin **bám vào hệ thống thực tế**;
> số liệu cụ thể (số migration, số module, tên công nghệ) đối chiếu từ repository.
> Thời lượng gợi ý tổng: **20–25 phút trình bày + 10–15 phút demo + Q&A**.

- Mã story: **NCL-10-CN-011-CV-05**
- Tham chiếu: [DEMO_SCRIPT.md](./DEMO_SCRIPT.md), [TEST_EVIDENCE.md](./TEST_EVIDENCE.md),
  [ARCHITECTURE.md](../handover/ARCHITECTURE.md), [SECURITY.md](../handover/SECURITY.md)

---

## 0. Ghi chú người trình bày

- Trình bày ngắn gọn, tập trung vào **giá trị nghiệp vụ + kiến trúc + bằng chứng**.
- Mọi slide cho demo dùng đúng **tên màn hình trong UI** và **số liệu thật**
  (12 lô kiểm nghiệm đạt, 15 lô seed, 58 migration…).
- Không nói "feature chưa có"; nếu hệ thống chưa làm E2E hoặc chưa có
  deployment-time tracking → nêu thành **hạn chế/hướng phát triển** (mục 13–14).

---

## 1. Giới thiệu đề tài

- Tên hệ thống: **Nguồn Gốc Số** — nền tảng truy xuất nguồn gốc nông sản.
- Slogan (từ README): *"Minh bạch từ nông trại đến bàn ăn."*
- Vai trò của hệ thống trong bối cảnh: nhu cầu minh bạch chuỗi cung ứng nông sản,
  niềm tin người tiêu dùng, hỗ trợ nhà quản lý ngành.
- Đối tượng sử dụng: HTX/doanh nghiệp nông sản, doanh nghiệp thu mua, cơ quan
  quản lý (VT-01 → VT-06), người tiêu dùng cuối.

## 2. Bài toán

- Hệ thống hiện hữu thiếu minh bạch về nguồn gốc, khó truy vết khi có sự cố.
- Quy trình ghi chép thủ công, dữ liệu phân tán, không có hành trình sản phẩm rõ ràng.
- Người tiêu dùng không biết sản phẩm đến từ đâu; cơ quan quản lý khó tổng hợp, thống kê.
- Doanh nghiệp thu mua/vận chuyển khó đối chiếu tính hợp lệ của lô hàng.

## 3. Mục tiêu

- Số hóa quy trình sản xuất & chuỗi cung ứng (vùng trồng, lô, nhật ký, sự kiện).
- Gắn mã truy xuất (tem QR theo chuẩn GS1 mô phỏng) cho từng lô hàng.
- Cho phép **người tiêu dùng quét mã → xem hành trình công khai**.
- Hỗ trợ kiểm nghiệm/chứng nhận, cảnh báo bất thường, báo cáo & thống kê.
- Đảm bảo phân quyền chi tiết (RBAC), cô lập dữ liệu theo tổ chức (multi-tenant).

## 4. Giải pháp

- Nền tảng web full-stack: **Spring Boot 3.5 + React 19 + MySQL 8.4**.
- Quy trình lõi (luồng TC-01):
  `Tạo lô → Ghi sự kiện → Kiểm nghiệm → Cấp tem/mã → Quét mã → Tra cứu`.
- Tra cứu công khai không cần đăng nhập; tách DTO công khai khỏi dữ liệu nội bộ.
- Sao lưu/phục hồi bằng `mysqldump`, chế độ bảo trì khi restore.
- Triển khai bằng Docker Compose (dev) và Kubernetes/GitHub Actions (staging/production).

## 5. Kiến trúc

- Sơ đồ tổng quan (dùng `ARCHITECTURE.md` §1 — Mermaid).
- Frontend SPA (React + Vite) → Nginx reverse-proxy `/api` → Backend REST → MySQL.
- File system lưu ảnh QR & attachments (uploads).
- Tích hợp ngoài: SMTP (mail/invitation), LocationIQ (reverse geocoding),
  ipwho.is (IP geolocation), partner API (API key gateway).
- Phân tầng backend: Controller → Service → Repository; DTO request/response;
  Spring Events cho tác vụ nội bộ.

## 6. Công nghệ

| Lớp | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.5.16, Spring Security 6, Spring Data JPA, Flyway 11, JWT (jjwt 0.12.6), ZXing (QR), OpenPDF, Apache POI |
| Frontend | React 19, TypeScript, Vite 8, Tailwind CSS 4, shadcn/ui, TanStack Query, React Hook Form + Zod, Recharts, Leaflet |
| Database | MySQL 8.4 (58 migration Flyway, 2 thư mục schema/data) |
| DevOps | Docker, Docker Compose, GitHub Actions (`ci-cd.yml`), GHCR, Kubernetes (k3s), cert-manager |
| Test | JUnit 5 + Spring Boot Test (90 test files backend), Vitest + Testing Library (8 files frontend) |

> Con số "90/8" là số lượng thực tế trong repo tại commit soạn tài liệu.

## 7. Các chức năng chính

- **Quản lý tổ chức & thành viên:** tạo tổ chức, mời thành viên qua email, phân vai, cấp quyền.
- **Vùng trồng & lô sản xuất:** khai báo vùng, tạo/duyệt lô, nhập lô hàng loạt từ Excel.
- **Nhật ký canh tác & chuỗi sự kiện:** farm-log, thu hoạch, sơ chế, đóng gói,
  vận chuyển, thu mua, nhập kho; ghi offline → đồng bộ.
- **Kiểm nghiệm & chứng nhận:** tiêu chuẩn, đơn vị kiểm nghiệm, yêu cầu & kết quả.
- **Tem truy xuất:** dải mã, tạo lô hàng, kích hoạt tem, xuất tem QR, hủy tem, thu hồi.
- **Tra cứu công khai:** quét mã, timeline, chứng nhận, kiểm nghiệm công khai, phản ánh.
- **Báo cáo & thống kê:** thống kê tra cứu, phân tích vùng trồng, so sánh mùa vụ,
  báo cáo ngành, xuất dữ liệu mở.
- **Vận hành hệ thống:** sao lưu/phục hồi, giám sát, lịch sử hoạt động/đăng nhập,
  cảnh báo tem bất thường, theo dõi đăng nhập bất thường.

## 8. Bảo mật

- Authentication: JWT 2 bước (`ORG_SELECTION` → `ACCESS`), BCrypt, reset password an toàn.
- Authorization: `@PreAuthorize` + `PermissionChecker` (role + ghi đè theo tổ chức).
- Cô lập dữ liệu theo tổ chức; báo cáo cán bộ ngành giới hạn theo địa bàn.
- API công khai chỉ phơi bày DTO công khai (không lộ người ghi dữ liệu nội bộ).
- Chuẩn hóa lỗi qua `GlobalExceptionHandler` (không lộ stacktrace).
- Container backend chạy **non-root**; secrets dùng env/K8s Secret (không commit).
- Các cơ chế khác: khóa tài khoản, theo dõi đăng nhập bất thường, IP geolocation
  (chi tiết: `SECURITY.md`).

## 9. Multi-tenant

- Mô hình dữ liệu tổ chức: `organizations`, `users`, `organization_users`; Access JWT chứa `organizationId`.
- Toàn bộ query scope theo tổ chức; phân quyền có thể override từng quyền theo tổ chức.
- Báo cáo nghiệm thu/phân tích cô lập theo địa bàn quản lý (VT-05) — không fallback toàn bộ dữ liệu.
- Shop demo với 4 tổ chức: `SYSTEM`, `DEMO_HTX`, `DEMO_NSV`, `DEMO_GOV`.

## 10. Quy trình truy xuất

- Trình bày vòng đời: lô sản xuất → sự kiện → kiểm nghiệm → lô hàng → tem → consumer.
- Nêu rõ trạng thái tem: `DRAFT`/`CODE_PRINTED` → `ACTIVATED` (tra cứu được) → recall.
- Chi tiết: `ARCHITECTURE.md` §9 (sơ đồ Mermaid TC-01) + `DEMO_SCRIPT.md`.

## 11. Demo

- Chạy theo **`DEMO_SCRIPT.md`** (đã chuẩn bị data trước).
- Tóm tắt 8 bước: login → org → vùng trồng/lô → sự kiện → kiểm nghiệm →
  lô hàng & tem → quét mã consumer → (tùy chọn) VT-04/VT-05.
- Trình bày evidence (screenshot) kèm mã truy xuất thật.

## 12. Kết quả

- Hệ thống đáp ứng luồng truy xuất đầu cuối (TC-01) — xem `TEST_EVIDENCE.md`.
- Có RBAC, multi-tenant, bảo mật JWT, tra cứu công khai không cần đăng nhập.
- Pipeline CI/CD + k8s manifest sẵn sàng cho staging/production.
- **Lưu ý trung thực:** TC-02/TC-03/TC-04 hiện phụ thuộc kết quả kiểm thử runtime
  (một số `NOT VERIFIED`); TC-04 đang là GAP (thiếu cơ chế version/deploy-time).

## 13. Hạn chế (trung thực, có căn cứ)

- Chưa có **E2E test** (Playwright/Cypress) — mới có unit/integration test.
- Chưa có cơ chế hiển thị **version/deployment-time** trong hệ thống (TC-04 GAP).
- Một số phần `NOT VERIFIED` về runtime (empty state, authorization authoring) — sẽ bổ sung bằng kịch bản demo/chạy thực tế.
- CORS allowlist có thể cần siết chặt hơn (hiện `allowedOriginPatterns("*")` + credentials — `SECURITY.md` §11).
- Seed dữ liệu demo chưa đầy đủ lô hàng/tem (cần tạo qua UI — `DEMO_DATA.md` §5).
- Swagger/Actuator chỉ expose `/actuator/health`; chưa expose `/actuator/info` build-info.

## 14. Hướng phát triển

- Bổ sung **E2E** và **deployment version tracking** (endpoint version + build-info).
- Tích hợp AWS Secrets Manager / External Secrets thay base64 trong manifest.
- Mở rộng cảnh báo/warning sớm (mùa vụ, dịch bệnh), app mobile native.
- Tích hợp thanh toán/đặt hàng cho doanh nghiệp thu mua.
- Đa ngôn ngữ (i18n), nâng cao phân tích dữ liệu lớn (dashboard nâng cao).
- Đưa hệ thống lên môi trường production chính thức (đổi secret mặc định, giám sát, log tập trung).

---

## 15. Câu hỏi dự kiến & gợi ý trả lời

| Câu hỏi hội đồng | Gợi ý trả lời (căn cứ thực tế) |
|---|---|
| Làm sao chứng minh quyền truy cập? | JWT Access chứa role + organizationId; mọi API check `@PreAuthorize`/`PermissionChecker`; có test `AuthControllerTest`, roleAccess.test.ts, `SecurityConfig` permitAll chỉ cho public endpoints. |
| Multi-tenant xử lý thế nào? | Organ thông qua `organization_users`; mọi query scope theo `organizationId`; báo cáo VT-05 phân địa bàn không fallback. |
| Mã QR có thật không? | Sinh bằng ZXing (backend), lưu file, có dải mã quản lý; tem chỉ tra cứu khi `ACTIVATED`. |
| Có migration DB không? | Flyway 58 file, schema + data, chạy tự động khi backend khởi động. |
| Bảo mật mật khẩu? | BCrypt, không lưu plaintext; có forgot/reset password bằng token một lần có hạn. |
| Deployment thế nào? | GitHub Actions: test → build/push GHCR → kubectl deploy; có k8s manifest + ingress + cert-manager. |

---

> Sau buổi demo, cập nhật [TEST_EVIDENCE.md](./TEST_EVIDENCE.md) và ghi chú vào
> mục **Lịch sử cập nhật status**.