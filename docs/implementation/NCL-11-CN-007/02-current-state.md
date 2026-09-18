# NCL-11-CN-007 — Hiện trạng hệ thống

## 1. Luồng hiện tại

1. VT-02 tạo yêu cầu qua `POST /api/v1/production-lots/{lotId}/test-requests`.
2. Backend tạo `InspectionRequest(PENDING_RESULT)` và snapshot các `InspectionCriterion`.
3. VT-02 mở trang nội bộ `RecordInspectionResultPage`.
4. UI tải `GET /api/v1/inspection-requests/{requestId}`, upload phiếu theo chỉ tiêu, rồi gọi batch `PUT /api/v1/inspection-requests/{requestId}/results`.
5. `InspectionCriterionResultServiceImpl` validate toàn bộ trước khi lưu, cập nhật status, quét cảnh báo hiệu lực và ghi activity log.
6. Public trace lấy các request/result để hiển thị lịch sử; người nhập hiện được suy ra từ `createdBy`, nhưng API công khai chưa hiển thị nguồn nhập.

Khoảng trống: chỉ VT-02 có JWT mới đọc/ghi/upload được; chưa có link public, token một lần, lifecycle cấp lại, hoặc trường provenance.

## 2. Backend

### 2.1 Thành phần có thể tái sử dụng

| Thành phần | Hiện trạng | Giá trị tái sử dụng |
|---|---|---|
| `InspectionRequestController/ServiceImpl` | Tenant-scope theo `productionLot.organization` | Kiểm tra request thuộc HTX cấp link |
| `InspectionCriterionResultServiceImpl` | Batch all-or-nothing, kiểm ngày, trạng thái, cảnh báo | Một nguồn logic duy nhất cho manual/public submit |
| `InspectionResultFile` flow | JPG/PNG/PDF, 5 MB, path dưới base dir | Dùng chung lưu file sau khi đổi cơ chế authorization |
| `TestingUnit` + `AccreditationScope` | Đã merge, request có `testingUnitId` nullable | Ràng link đúng đơn vị đã chọn |
| `PartnerApiKeyService` | SecureRandom, SHA-256, raw secret chỉ trả một lần, expiry/rate limit | Pattern bảo mật QTN-20 |
| `PasswordResetTokenRepository.consumeToken` | Atomic update chống double-submit | Pattern tiêu thụ token một lần |
| `InvitationService` | Link public, expiry, email, reissue làm link cũ hết hiệu lực | Pattern QTN-14 và UI route public |
| `EmailService` | Gửi HTML async, có `app.frontend-url` | Thêm template gửi link kiểm nghiệm |
| `ApiResult` + `GlobalExceptionHandler` | Wrapper thống nhất, hỗ trợ status tùy chỉnh | Giữ contract lỗi hiện hành |

### 2.2 Mô hình dữ liệu hiện có

- `inspection_requests`: request, lot, tên đơn vị snapshot, `testing_unit_id`, status, creator, scope warning.
- `inspection_criteria`: snapshot các chỉ tiêu thuộc request.
- `inspection_criterion_results`: một result/criterion, ngày, pass/fail, file, `created_by NOT NULL`.
- `testing_units`: danh mục dùng chung, contact chỉ là `VARCHAR(500)` tự do.
- Không có token/link chuyên dụng; không có nguồn nhập trên request/result.

Ràng buộc cần xử lý:

- Public actor không có `User`, trong khi `inspection_criterion_results.created_by` đang `NOT NULL` và mapper gọi thẳng `result.getCreatedBy().getFullName()`.
- Unique `inspection_criterion_id` khiến public/manual cùng ghi phải dùng chung chiến lược update và concurrency control.
- `InspectionRequestStatus` chỉ gồm `PENDING_RESULT`, `PASSED`, `FAILED`, `CANCELLED`; không được thêm trạng thái chỉ để biểu diễn link.

### 2.3 Phân quyền và tenant isolation hiện tại

- Controller result và request dùng `@PreAuthorize("hasRole('VT-02')")`.
- Service kiểm organization của lot; cross-tenant được trả như “không tồn tại”.
- `SecurityConfig` permit `/api/v1/public/**`, vì vậy endpoint portal có thể đặt dưới prefix này mà không mở thêm vùng URL.
- Public request hiện không có actor/tenant context; tenant phải được suy ra duy nhất từ token hash và kiểm chéo với request.

### 2.4 Token hiện có

- `invitations` lưu token thô; phù hợp tham khảo state/expiry nhưng không đủ an toàn để sao chép nguyên trạng.
- `partner_api_keys` và `password_reset_tokens` lưu SHA-256; password reset có atomic consume.
- Không có generic public-link service. Tạo entity chuyên dụng là cần thiết để bảo đảm phạm vi hẹp và audit.

### 2.5 Audit và thông báo

- Manual result phát `ActivityLogEvent` với user và organization.
- `activity_logs.user_id/username/full_name` đều bắt buộc; không phù hợp cho actor ngoài hệ thống.
- Link table cần giữ audit public (`used_at`, IP, user-agent, nguồn), còn activity log vẫn dùng cho VT-02 cấp/cấp lại link.
- `EmailServiceImpl` hiện có fallback/error log chứa URL đầy đủ. Method mới không được log raw token hoặc full URL.

## 3. Frontend

### 3.1 Luồng nội bộ

- `InspectionRequestActionButtons` hiển thị “Ghi nhận kết quả” cho `PENDING`.
- `RecordInspectionResultPage` chứa đầy đủ editor: danh sách chỉ tiêu, pass/fail, ngày, upload và batch submit.
- Route hiện tại nằm trong `PrivateRoute` và `RoleRoute(VT-02)`.
- `certificationApi.ts` và `types/certification.ts` phản ánh contract hiện hữu.

### 3.2 Pattern public có thể tái sử dụng

- `JoinOrganizationPage` là public route dùng token query.
- `invitationApi.ts` gọi `/public/**` qua Axios wrapper.
- `AppRoutes.tsx` tách public route khỏi `MainLayout`.

Khoảng trống frontend:

- Chưa có route/trang nhập kết quả public.
- Editor hiện tại gắn chặt với API nội bộ, breadcrumb và điều hướng lot; cần tách phần form trình bày để tái sử dụng thay vì copy toàn bộ trang.
- `axiosConfig.ts` chỉ coi `/auth/login` là request không gửi access token. Portal cần được thêm vào danh sách public/no-access-token để không gửi JWT cũ và không kích hoạt logout toàn cục.
- Chưa có UI cấp/cấp lại link hoặc hiển thị status link.

## 4. Public trace và hồ sơ

- Backend `PublicTraceServiceImpl` trả kết quả mới nhất và lịch sử các lần kiểm nghiệm.
- `PublicInspectionCriterionResultDto` hiện có `inspectorName`, `laboratoryName` nhưng không có `entrySource`.
- `PublicInspectionSection` hiển thị đơn vị, kết luận, ngày và lịch sử; chưa có badge “Đơn vị kiểm nghiệm khai”/“HTX nhập”.
- GS1/dossier hiện đọc cùng entities. Trường nguồn phải được thêm additive khi tài liệu/DTO đó công bố kết quả; không đổi tên trường hiện hữu.

## 5. Database và migration

- Flyway tách `schema/` và `data/`; migration đã chạy không được sửa.
- Migration schema mới nhất có timestamp `V20260915102000`; Story nên dùng migration timestamp mới, ví dụ `V20260916090000__create_inspection_result_entry_links.sql`, sau khi kiểm tra collision lúc triển khai.
- Dữ liệu cũ có `created_by`, nên backfill `entry_source='COOPERATIVE_MANUAL'` là an toàn và tương thích ngược.

## 6. Tests hiện có

| Lớp test | Bao phủ hiện tại | Khoảng trống cần bổ sung |
|---|---|---|
| `InspectionRequestServiceImplTest` | Tạo/list/detail, tenant, testing unit/scope | Cấp link đúng request/tenant |
| `InspectionCriterionResultServiceImplTest` | Single/batch, dates, status, failed/retry, expiry | Actor public, source, revoke link khi manual |
| `PublicTraceServiceImplTest` | Mapping inspections/history/i18n | Mapping `entrySource`, null `createdBy` |
| Invitation/password reset/API key tests | Token lifecycle/hash/rate limit | Dedicated link expiry/use/concurrency |
| `certificationApi.test.ts` | Ít test API inspection | API portal/issue link |
| `InspectionRequestActionButtons.test.tsx` | Ma trận action hiện hữu | Nút cấp/cấp lại và callback |
| Public component tests | Chưa có test riêng cho source badge | Portal form + trace badge |

## 7. Dependency merge audit

- `origin/feature/NCL-11-CN-006_testing-units` → ancestor của `develop`.
- `origin/feature/NCL-11-CN-003-inspection-result` → ancestor của `develop`.
- `origin/feature/NCL-11-CN-004`, `origin/feature/NCL-11-CN-005-handle-failed-inspection`, `origin/feature/NCL-09-CN-009` → ancestor của `develop`.
- Không có branch `NCL-11-CN-007` hoặc implementation sẵn trong code/docs.
- Không có tài liệu API riêng cho `NCL-11-CN-007`; đây là artifact bắt buộc ở checkpoint API documentation.

## 8. Rủi ro hồi quy

1. Làm `created_by` nullable nhưng không sửa mapper có thể gây NPE ở API nội bộ/public trace.
2. Viết logic public riêng có thể lệch QTN-21, không chạy expiry alert hoặc activity behavior.
3. Submit song song có thể dùng token hai lần hoặc ghi đè unique result nếu không consume atomic.
4. Link bị cấp bởi org A cho request org B là IDOR nghiêm trọng.
5. Ghi raw token/full URL vào log, email fallback hoặc response sau lần cấp đầu làm lộ secret.
6. Upload qua token không kiểm criterion thuộc request có thể ghi file chéo tenant.
7. Manual submit và public submit cạnh tranh có thể đổi nguồn sai hoặc ghi đè dữ liệu.
8. Thêm field bắt buộc không có default sẽ làm migration dữ liệu cũ thất bại.
9. Refactor form lớn có thể làm hỏng màn hình VT-02 hiện hữu.
10. Public endpoint dùng Axios interceptor hiện tại có thể vô tình gửi JWT hoặc redirect `/login`.
11. Public trace/GS1 consumers có thể lỗi nếu thay đổi field hiện hữu thay vì bổ sung additive.

## 9. Kết luận hiện trạng

Khoảng 70–80% năng lực nghiệp vụ đã tồn tại: request, criteria snapshot, batch result, upload, status, expiry, testing unit, email và token patterns. Phần cần xây mới là trust boundary công khai: link lifecycle, token-scoped authorization, provenance, manager UI cấp link và public page. Thiết kế phải mở rộng additive và dùng lại domain logic hiện hành.
