# NCL-11-CN-007 — Kế hoạch triển khai

## 1. Nguyên tắc thực hiện

- Tuân thủ thứ tự lifecycle: API docs → DB/backend → backend runtime/tests → frontend → UI runtime/tests/lint/type-check/build → final validation.
- Mỗi checkpoint phải hoàn thành và có bằng chứng trước khi sang checkpoint sau.
- Không copy logic validation kết quả; public/manual phải hội tụ vào một domain path.
- Mọi file ngoài danh sách dự kiến phải được giải thích và cập nhật Implementation Contract trước khi sửa.

## 2. Checkpoint 0 — Planning Gate

Điều kiện qua cổng:

- [x] Đã đọc parent story, 5 Tasks, 4 AC.
- [x] Đã đọc QTN-14, QTN-20, QTN-21.
- [x] Đã đọc API docs liên quan.
- [x] Đã trace backend/frontend/DB/permission/tenant/tests.
- [x] Đã kiểm tra dependencies trực tiếp đều có trên `develop`.
- [x] Đã khóa API, DB, nguồn nhập, link lifecycle và test matrix trong `05-implementation-contract.md`.
- [ ] Trước khi code, reviewer/PO chấp nhận các quyết định Planner nếu quy trình dự án yêu cầu phê duyệt contract.

## 3. Checkpoint 1 — API documentation (contract-first)

Tạo:

- `docs/api/certification/inspection-result-entry-portal.md`

Nội dung bắt buộc:

- source mapping Story/Tasks/AC/QTN;
- authenticated issue/status endpoints;
- public detail/upload/submit endpoints;
- DTO, validation, errors, state machine;
- tenant/IDOR, token hashing, one-time consume, throttling, cache/logging;
- DB/migration impact;
- frontend flow, test checklist, non-regression.

Đồng bộ additive:

- `docs/api/certification/inspection-result.md`: `entrySource`, `createdByName` nullable, manual mutation revokes active link.
- `docs/api/certification/inspection-request.md`: link issuance chỉ ở `PENDING_RESULT`, cần catalog testing unit.
- Tài liệu public trace/GS1 chỉ cập nhật nếu field `entrySource` được công bố qua contract hiện hành; không đổi schema field cũ.

Exit criteria: docs và implementation contract không mâu thuẫn; API reviewer xác nhận trước migration/code.

## 4. Checkpoint 2 — Database và domain

### 4.1 Migration

Tạo dự kiến:

- `backend/src/main/resources/db/migration/schema/V20260916090000__create_inspection_result_entry_links.sql`

Migration phải:

1. Tạo `inspection_result_entry_links` và indexes/FKs.
2. Thêm `entry_source`, `portal_link_id` vào `inspection_criterion_results`.
3. Backfill/default `COOPERATIVE_MANUAL`.
4. Cho `created_by` nullable, giữ FK.
5. Không sửa V23/V26/V35/V47/V49.

### 4.2 Entity/enums/repositories

Tạo:

- `backend/src/main/java/vn/nguongocso/certification/entity/InspectionResultEntryLink.java`
- `backend/src/main/java/vn/nguongocso/certification/enums/InspectionResultEntryLinkStatus.java`
- `backend/src/main/java/vn/nguongocso/certification/enums/InspectionResultEntrySource.java`
- `backend/src/main/java/vn/nguongocso/certification/repository/InspectionResultEntryLinkRepository.java`

Sửa:

- `backend/src/main/java/vn/nguongocso/certification/entity/InspectionCriterionResult.java`
- Có thể bổ sung tenant-scoped query tại `InspectionRequestRepository.java` thay vì fetch không scope.

Repository cần query lock/conditional update cho:

- latest link theo request;
- revoke active links;
- lookup hash + fetch request/lot/testing unit;
- atomic `ACTIVE → USED` khi chưa hết hạn.

Exit criteria: Flyway chạy trên DB sạch và DB có dữ liệu cũ; schema/entity khớp; không mất result cũ.

## 5. Checkpoint 3 — Backend API và nghiệp vụ

### 5.1 DTO

Tạo dưới `certification/dto`:

- `request/IssueInspectionResultEntryLinkRequest.java`
- `response/InspectionResultEntryLinkResponse.java`
- `response/PublicInspectionResultEntryResponse.java`

Có thể tạo DTO criterion public riêng nếu tránh lộ field nội bộ:

- `response/PublicInspectionResultEntryCriterionResponse.java`

Sửa:

- `InspectionCriterionResultResponse.java`: `entrySource`, null-safe actor.
- Không thay payload `RecordInspectionResultsRequest` nếu contract hiện hữu đủ dùng.

### 5.2 Service

Tạo:

- `certification/service/InspectionResultEntryLinkService.java`
- `certification/service/impl/InspectionResultEntryLinkServiceImpl.java`
- `certification/service/InspectionResultPortalRateLimitService.java` (hoặc tên tương đương, phạm vi hẹp).

Sửa/tái cấu trúc có kiểm soát:

- `InspectionCriterionResultService.java`
- `InspectionCriterionResultServiceImpl.java`

Yêu cầu kỹ thuật:

- shared validation/save/status/expiry path cho manual và portal;
- manual source + revoke link;
- portal source + null user + link relation;
- atomic consume trong transaction;
- null-safe response mapping;
- file authorization dựa token + criterion membership;
- không log token/full URL.

Nếu cần tách lưu file để tái sử dụng, tạo duy nhất:

- `certification/service/InspectionResultFileStorageService.java`

Không sao chép block lưu file vào service portal.

### 5.3 Controllers và security

Tạo:

- `certification/controller/InspectionResultEntryLinkController.java` — authenticated issue/latest.
- `certification/controller/PublicInspectionResultEntryController.java` — public detail/upload/submit.

`SecurityConfig.java` dự kiến **không cần sửa** vì `/api/v1/public/**` đã permit. Chỉ sửa nếu test chứng minh matcher hiện tại không bao phủ endpoint mới.

### 5.4 Email và audit

Sửa:

- `mail/service/EmailService.java`
- `mail/service/impl/EmailServiceImpl.java`

Thêm template/method gửi link, tuyệt đối không log secret. Manager actions phát activity log; public submit dùng link/result metadata làm audit.

### 5.5 Public trace

Sửa:

- `publicapi/dto/response/PublicInspectionCriterionResultDto.java`
- `publicapi/service/impl/PublicTraceServiceImpl.java`

Chỉ thêm `entrySource`, không đổi thuật toán latest/history/status.

Exit criteria: API chạy đúng contract; tenant và one-time guarantees có test; manual flow vẫn pass.

## 6. Checkpoint 4 — Backend tests và runtime validation

### 6.1 Tests mới

Dự kiến:

- `backend/src/test/java/vn/nguongocso/certification/service/InspectionResultEntryLinkServiceImplTest.java`
- `backend/src/test/java/vn/nguongocso/certification/controller/InspectionResultEntryLinkControllerTest.java`
- `backend/src/test/java/vn/nguongocso/certification/controller/PublicInspectionResultEntryControllerTest.java`
- `backend/src/test/java/vn/nguongocso/certification/InspectionResultEntryPortalIntegrationTest.java`

### 6.2 Tests sửa/mở rộng

- `InspectionCriterionResultServiceImplTest.java`
- `InspectionRequestServiceImplTest.java` chỉ khi response/query đổi.
- `PublicTraceServiceImplTest.java`
- `PublicTraceControllerTest.java` nếu public DTO contract được assert.
- Test `EmailService` nếu repository đã có pattern phù hợp.

### 6.3 Ma trận bắt buộc

- happy path issue → GET → upload → submit;
- expired, used, revoked, invalid token;
- double submit/race chỉ một request thành công;
- request/criterion/file không cùng scope;
- wrong role và cross-tenant issue/reissue;
- request không PENDING;
- manual submit revokes link và source manual;
- portal submit source portal, `createdBy=null` không NPE;
- pass/fail/date/file/all-or-nothing/QTN-21/expiry alert;
- 429 throttling và no-store headers;
- token hash only/no raw secret in persisted/logged data.

### 6.4 Commands

Khám phá Maven wrapper trước, sau đó tối thiểu:

```powershell
cd backend
.\mvnw.cmd -Dtest=InspectionResultEntryLinkServiceImplTest,InspectionResultEntryPortalIntegrationTest test
.\mvnw.cmd test
```

Runtime validation: khởi động backend với DB migration thật, chạy curl/Postman toàn bộ flow và kiểm DB status/source/link.

## 7. Checkpoint 5 — Frontend API, types và form dùng chung

### 7.1 API/types

Tạo:

- `frontend/src/api/inspectionResultPortalApi.ts`
- `frontend/src/types/inspectionResultPortal.ts`

Sửa:

- `frontend/src/api/certificationApi.ts` — issue/latest link hoặc tách toàn bộ vào portal API.
- `frontend/src/types/certification.ts` — additive `entrySource`.
- `frontend/src/api/axiosConfig.ts` — prefix portal public không gửi ACCESS JWT và không kích hoạt session-expiry redirect.

### 7.2 Form dùng chung

Tạo/tách:

- `frontend/src/components/certification/InspectionResultEntryForm.tsx`

Sửa:

- `frontend/src/pages/certification/RecordInspectionResultPage.tsx` dùng component chung qua adapter manual.

Component chung chỉ nhận data/callback, không tự biết route/JWT/token.

Exit criteria: màn hình VT-02 giữ hành vi cũ; portal dùng cùng validation presentation.

## 8. Checkpoint 6 — Frontend manager/public UI

Tạo:

- `frontend/src/components/certification/IssueInspectionResultLinkDialog.tsx`
- `frontend/src/pages/public/InspectionResultEntryPage.tsx`

Sửa:

- `frontend/src/components/certification/InspectionRequestActionButtons.tsx`
- `frontend/src/components/certification/InspectionRequestHistoryModal.tsx` và/hoặc nơi truyền callback nếu cần.
- `frontend/src/routes/AppRoutes.tsx`
- `frontend/src/components/public/PublicInspectionSection.tsx`
- `frontend/src/types/publicInspection.ts`

UI states và accessibility:

- form label/error rõ ràng, keyboard accessible;
- expired/used/invalid có trang riêng và hướng dẫn liên hệ HTX;
- submit disable khi upload/validation chưa hoàn tất;
- success terminal;
- source badge ở internal result và public trace;
- không hiển thị token sau khi đóng dialog/cấp mới.

## 9. Checkpoint 7 — Frontend tests, lint, type-check, build và UI runtime

Tests tạo/mở rộng:

- `frontend/src/api/__tests__/inspectionResultPortalApi.test.ts`
- `frontend/src/components/certification/__tests__/IssueInspectionResultLinkDialog.test.tsx`
- `frontend/src/components/certification/__tests__/InspectionResultEntryForm.test.tsx`
- `frontend/src/pages/public/__tests__/InspectionResultEntryPage.test.tsx`
- `frontend/src/components/public/__tests__/PublicInspectionSection.test.tsx`
- mở rộng `InspectionRequestActionButtons.test.tsx` và `certificationApi.test.ts` nếu liên quan.

Commands:

```powershell
cd frontend
npm test
npm run lint
npm run build
```

UI runtime qua browser/Playwright:

1. Login VT-02 org A, cấp link và nhận/copy URL.
2. Mở URL trong incognito, không có JWT, thấy đúng request.
3. Nhập đủ result/upload/submit; kiểm success và refresh thành used.
4. Mở link expired/revoked.
5. Login org B, thử cấp/xem request org A và xác nhận bị chặn.
6. Kiểm public trace hiển thị đúng source.
7. Kiểm console/network không có lỗi và token không xuất hiện trong log/request ngoài endpoint portal.

## 10. Checkpoint 8 — Final validation

- Chạy toàn bộ backend tests, frontend tests/lint/build.
- Kiểm Flyway trên DB sạch và upgrade DB có dữ liệu.
- Đối chiếu từng AC/QTN/API/DB/UI bằng checklist contract.
- `git status`, `git diff`, `git diff --check`.
- Kiểm UTF-8, comments/JavaDoc tiếng Việt.
- Xác nhận không commit secrets, raw token, file upload thử, logs tạm hoặc thay đổi ngoài Story.

## 11. Thứ tự commit đề xuất

1. `docs(NCL-11-CN-007): add inspection result portal API contract`
2. `feat(db): add inspection result entry link and source metadata`
3. `feat(backend): add scoped one-time inspection result portal`
4. `test(backend): cover portal lifecycle tenant and replay protection`
5. `feat(frontend): add manager link flow and public result portal`
6. `test(frontend): cover portal states and provenance display`

Mỗi commit phải chạy checklist git safety và chỉ chứa phạm vi tương ứng.
