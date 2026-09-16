# Báo cáo kiểm thử cuối — NCL-11-CN-007

> **Phạm vi:** Cổng nhập kết quả kiểm nghiệm qua liên kết có thời hạn
>
> **Nhánh:** `feature/NCL-11-CN-007-inspection-result-entry-portal`
>
> **Ngày xác minh cuối:** 16/09/2026, sau khi merge `origin/develop`
> **Kết luận:** Tính năng NCL-11-CN-007 đã được xác minh; full regression còn lỗi baseline ngoài phạm vi được nêu tại Mục 6.

## 1. Phạm vi thay đổi

- Backend: cấp/cấp lại link, token SHA-256, tenant scope, one-time consume, rate limit, upload file qua opaque handle, email và audit.
- Database: bảng `inspection_result_entry_links`; provenance cho `inspection_criterion_results`.
- Frontend: dialog cấp link, portal công khai, form nhập kết quả, trạng thái link và provenance badge.
- Public trace: hiển thị nguồn nhập kết quả mà không lộ người dùng nội bộ.

## 2. Acceptance Criteria

| AC | Kết quả | Bằng chứng |
|---|---|---|
| NCL-11-CN-007-TC-01 | verified | Integration test E2E trả 201/200; portal lưu nguồn `TESTING_UNIT_PORTAL`; Playwright render form hợp lệ. |
| NCL-11-CN-007-TC-02 | verified | Link hết hạn trả 410 và có hướng dẫn liên hệ HTX. |
| NCL-11-CN-007-TC-03 | verified | Replay và concurrent double-submit chỉ cho phép một lần thành công. |
| NCL-11-CN-007-TC-04 | verified | Luồng HTX nhập tay lưu `COOPERATIVE_MANUAL` và thu hồi link ACTIVE. |

## 3. Business Rules và bảo mật

| Rule | Kết quả | Bằng chứng |
|---|---|---|
| QTN-14 | verified | Khóa bi quan trên `InspectionRequest` và atomic revoke/consume; concurrency tests PASS. |
| QTN-20 | verified | Token chỉ lưu hash, generic public errors, rate limit 429, opaque file handle kiểm token/request/criterion. |
| QTN-21 | verified | Dùng chung logic chốt `PASSED`/`FAILED` và quét cảnh báo hiệu lực với luồng manual. |
| Tenant isolation | verified | Authenticated query bắt buộc `requestId + organizationId`; không fallback sang query unscoped. |

## 4. Backend validation

### Test theo phạm vi inspection sau merge develop

```powershell
mvn test "-Dtest=*Inspection*Test"
```

- 137 tests; 0 failures; 0 errors; 0 skipped.
- `BUILD SUCCESS`.

### Runtime

- Backend khởi động bằng profile `test` và test classpath tại cổng 8080.
- `GET /actuator/health` trả HTTP 200, `status=UP`, database H2 `UP`.
- Profile test hiện có cảnh báo Hibernate DDL cho một số bảng ngoài NCL-11 nhưng ứng dụng vẫn đạt readiness và health UP.
- Docker/MySQL không khả dụng trên máy (`docker` command không được cài), nên chưa có bằng chứng Flyway clean/upgrade trên MySQL thật.

## 5. Frontend validation

```powershell
npm run lint
npx vitest run <4 test files NCL-11>
npm run build
```

- ESLint: PASS.
- NCL-11 frontend: 4 files, 19/19 tests PASS.
- TypeScript + Vite production build: PASS.
- Cảnh báo chunk > 500 kB là cảnh báo hiện hữu, không chặn build.

### Runtime/UI bằng Playwright

- Route `/inspection-result-entry/invalid-token`: API 404; UI hiển thị “Liên kết không hợp lệ” và hướng dẫn phù hợp.
- Response hợp lệ được mock ở network boundary: hiển thị đơn vị kiểm nghiệm, lô, ngày gửi mẫu, hạn link và danh sách chỉ tiêu.
- Chọn “Đạt”: form hiện ngày cấp/ngày hết hiệu lực, bộ đếm cập nhật và nút nộp được bật.
- Console của trạng thái hợp lệ: 0 errors, 0 warnings; request portal: HTTP 200.

## 6. Full regression sau merge develop

### Backend

- Tổng: 1.176 tests; 0 failures; 5 errors.
- 2 errors: `CodeRangeRepositoryTest`.
- 3 errors: `ShipmentSplitRelationshipRepositoryTest`.
- Nguyên nhân: H2 test schema không có các bảng `organizations`/`product_categories`; hai nhóm test và source liên quan không nằm trong diff NCL-11.

### Frontend

- Tổng: 404 tests; 403 PASS; 1 FAIL.
- Lỗi: `OrganizationUsagePage.test.tsx`, TC-06 của NCL-07-CN-008 phân biệt chữ hoa/thường giữa expected `từ` và actual `Từ`.
- File test/component liên quan không nằm trong diff NCL-11.

Các lỗi trên là baseline được đưa vào nhánh khi merge `origin/develop`; không được sửa trong NCL-11 để tránh mở rộng phạm vi.

## 7. Git và chất lượng

- `origin/develop` là ancestor của nhánh sau merge; nhánh không còn commit behind.
- `git diff --check`: PASS.
- Thay đổi ngoài Story `docs/api/farm/DrawFarmAreaBoundary.md` được giữ nguyên trong worktree và không đưa vào commit.
- Maven Wrapper của repo lỗi `NullArray` trên PowerShell; validation dùng đúng Maven 3.9.16 đã được wrapper tải vào `.m2`.

## 8. Kết luận

- **NCL-11 feature validation:** PASS.
- **Full repository regression gate:** NOT PASS do 6 lỗi baseline ngoài phạm vi (5 backend, 1 frontend).
- **MySQL/Flyway runtime migration:** UNVERIFIED vì môi trường không có Docker/MySQL.
- **Trạng thái:** sẵn sàng push và mở PR để human review với các giới hạn trên; chưa được mô tả là full-gate PASS hoặc deployment-ready.
