# NCL-11-CN-007 — Implementation Contract

> Trạng thái: **PLANNING COMPLETE — READY FOR REVIEW**
> Baseline: `develop@bfc19573cb74c12adce2020c1d244d0e7a90726d`
> Contract này khóa phạm vi triển khai. Mọi thay đổi có ảnh hưởng API/DB/quyền/tenant ngoài nội dung dưới đây phải quay lại Planning Gate.

## 1. IN SCOPE

1. VT-02 cấp/cấp lại link nhập kết quả cho đúng một yêu cầu kiểm nghiệm `PENDING_RESULT` thuộc tenant hiện tại.
2. Link dùng một lần, có hạn 1–30 ngày, mặc định 7 ngày; link mới vô hiệu link active cũ.
3. Gửi link qua email được VT-02 nhập/xác nhận khi cấp; trả URL thô đúng một lần để copy dự phòng.
4. Cổng public không cần tài khoản/JWT để xem đúng request, upload phiếu và submit toàn bộ kết quả.
5. Token secure random, lưu SHA-256, atomic consume, throttling và no-store.
6. Lưu nguồn theo từng result: `TESTING_UNIT_PORTAL` hoặc `COOPERATIVE_MANUAL`.
7. HTX nhập tay vẫn dùng API hiện hữu; mọi manual mutation gắn nguồn manual và revoke active link.
8. Mở rộng additive DTO nội bộ/public trace để hiển thị nguồn.
9. Dùng chung validation, status, QTN-21, expiry alert, file rules và audit hiện hữu.
10. Migration, backend/frontend tests, runtime/UI validation và API docs tương ứng.

## 2. OUT OF SCOPE

- Tạo account/role/login workspace cho đơn vị kiểm nghiệm.
- Dashboard nhiều yêu cầu cho đơn vị kiểm nghiệm.
- Sửa danh mục đơn vị kiểm nghiệm, tách `contact_info`, tự động lấy email từ chuỗi tự do.
- Ký số, OCR, xác minh phiếu, chứng thư số, mã công nhận hoặc chống giả tài liệu.
- Thay đổi bộ chỉ tiêu, accreditation scope hoặc cách tạo request.
- Thay đổi enum trạng thái request hoặc thuật toán QTN-21/inspection validity/reinspection.
- Mở rộng/đổi schema `activity_logs` để có anonymous actor.
- Refactor partner API key, invitation, password reset hoặc hệ thống permission chung.
- Sửa GS1 schema ngoài việc bổ sung source nếu contract đang công bố result tương ứng.
- Scheduler dọn file orphan hoặc chiến lược distributed rate-limit; ghi nhận là cải tiến vận hành sau Story nếu chạy đa instance.

## 3. Acceptance Criteria bị khóa

| AC | Contract nghiệm thu |
|---|---|
| `TC-01` | Link active + request pending; public GET thấy đúng criteria; submit đủ batch thành công; link thành `USED`; mọi result có `entrySource=TESTING_UNIT_PORTAL`; status/expiry alert được cập nhật bằng logic hiện hữu. |
| `TC-02` | `now >= expiresAt`: mọi GET/upload/submit trả `410`; không trả dữ liệu request; UI hướng dẫn liên hệ HTX để cấp lại; manager có thể cấp link mới nếu request vẫn pending. |
| `TC-03` | Sau submit thành công, refresh/replay trả `410`; hai submit song song chỉ một request consume/save thành công. |
| `TC-04` | Manual API ghi thành công như hiện tại; result có `COOPERATIVE_MANUAL`; public/internal trace hiển thị nguồn HTX; link active của request bị revoke. |

## 4. Business Rules bị khóa

### QTN-14

- Link có expiry, terminal after use, reissue bắt buộc tạo token mới.
- Không dùng lại bảng `invitations`; chỉ dùng lifecycle pattern.

### QTN-20

- Link gắn `organization + request + testing unit`, không cấp scope tổ chức rộng.
- Token chỉ lưu hash, raw chỉ xuất hiện trong email/response cấp đầu.
- Rate limit: 60 request/giờ/valid token; 30 invalid-token request/giờ/IP; trả 429.
- Không log token/full URL; public DTO tối thiểu; generic invalid-token error.

### QTN-21

- Portal dùng chung batch validation/chốt status/cảnh báo hiệu lực với manual flow.
- Source không tác động pass/fail, expiry hoặc seal eligibility.
- Không tạo endpoint/logic kích hoạt tem mới.

## 5. API contract bị khóa

### Authenticated VT-02

| Method | Path | Mục đích | Success |
|---|---|---|---:|
| POST | `/api/v1/inspection-requests/{requestId}/result-entry-links` | Cấp/cấp lại, gửi email, trả URL một lần | 201 |
| GET | `/api/v1/inspection-requests/{requestId}/result-entry-links/latest` | Xem metadata link mới nhất, không trả secret | 200 |

POST body:

```json
{ "recipientEmail": "lab@example.vn", "expiryDays": 7 }
```

POST response data bắt buộc: `id`, `status`, `recipientEmail`, `expiresAt`, `entryUrl`. GET response không có `entryUrl`, raw token hoặc hash.

### Public, không JWT

| Method | Path | Mục đích | Success |
|---|---|---|---:|
| GET | `/api/v1/public/inspection-result-entry/{token}` | DTO tối thiểu của request/criteria | 200 |
| POST | `/api/v1/public/inspection-result-entry/{token}/criteria/{criterionId}/file` | Upload JPG/PNG/PDF ≤ 5 MB | 200 |
| PUT | `/api/v1/public/inspection-result-entry/{token}/results` | Submit batch all-or-nothing và consume token | 200 |

Public detail không trả organization ID, lot UUID, request UUID, user ID, testing unit ID, creator hoặc raw file system path không cần thiết. `criterionId` là snapshot UUID cần cho submit và luôn được kiểm membership ở server.

### Additive contract hiện hữu

- `InspectionCriterionResultResponse.entrySource` — enum string.
- `InspectionCriterionResultResponse.createdByName` — nullable.
- `PublicInspectionCriterionResultDto.entrySource` — enum string.
- Không rename/remove field; manual request payload/path giữ nguyên.

### Error semantics

| HTTP | Contract |
|---:|---|
| 400 | Validation email/expiry/result/date/file |
| 403 | Wrong role authenticated side |
| 404 | Request không tồn tại/khác tenant; token invalid dùng thông điệp generic |
| 409 | Request không pending hoặc xung đột manual/public |
| 410 | Link expired/used/revoked; expired phải hướng dẫn liên hệ HTX |
| 413 | File quá giới hạn |
| 415 | MIME không hỗ trợ |
| 429 | Vượt throttling |

Mọi response public có `Cache-Control: no-store`; không dựa vào frontend để thực thi authorization.

## 6. Database changes bị khóa

### Bảng mới

`inspection_result_entry_links` với các cột:

`id`, `inspection_request_id`, `organization_id`, `testing_unit_id`, `recipient_email`, `token_prefix`, `token_hash`, `status`, `expires_at`, `used_at`, `used_ip`, `used_user_agent`, `created_by`, `created_at`, `revoked_by`, `revoked_at`.

Status: `ACTIVE`, `USED`, `REVOKED`, `EXPIRED`.

### Bảng sửa

`inspection_criterion_results`:

- `entry_source VARCHAR(32) NOT NULL DEFAULT 'COOPERATIVE_MANUAL'`;
- `portal_link_id CHAR(36) NULL FK`;
- `created_by` cho phép null nhưng giữ FK.

### Migration rules

- Migration additive mới; không sửa migration cũ.
- Backfill dữ liệu cũ thành `COOPERATIVE_MANUAL`.
- FK/index theo `03-solution-design.md`.
- Upgrade phải giữ đủ số lượng/nội dung result hiện hữu.

## 7. Permission rules bị khóa

| Actor | Issue/reissue/status | Public detail/upload/submit | Manual result APIs |
|---|---|---|---|
| VT-02 đúng tenant | Cho phép | Qua token nếu có | Cho phép theo rule hiện hữu |
| VT-02 khác tenant | 404/deny | Chỉ token scope, không dựa JWT | 404/deny |
| VT-01/VT-03/VT-04/VT-05 | 403 | Qua token nếu được nhận link, không theo role | Giữ nguyên hiện tại |
| Anonymous không token hợp lệ | Không | 404/410 | Không |
| Anonymous có token active | Không | Chỉ đúng request/criteria của token | Không |

Không thêm role hoặc permission code mới.

## 8. Tenant isolation bị khóa

1. Authenticated queries dùng `requestId + currentOrganizationId`.
2. Link lưu organization snapshot và testing unit; service kiểm chéo với request trước khi trả/ghi.
3. Client public không được chọn organization/request bằng tham số riêng.
4. Criterion, result và upload path phải thuộc đúng request token.
5. Cross-tenant/guessed ID không tiết lộ sự tồn tại; không trả dữ liệu partial trước khi token validation hoàn tất.
6. Public source label không làm lộ user nội bộ hoặc email người nhận.

## 9. Expected files to change

### Documentation

- `docs/api/certification/inspection-result-entry-portal.md` (new)
- `docs/api/certification/inspection-result.md`
- `docs/api/certification/inspection-request.md`
- Tài liệu public trace/GS1 tương ứng chỉ khi DTO public bổ sung field được mô tả tại đó.

### Backend — new

- `backend/src/main/resources/db/migration/schema/V20260916090000__create_inspection_result_entry_links.sql`
- `backend/src/main/java/vn/nguongocso/certification/entity/InspectionResultEntryLink.java`
- `backend/src/main/java/vn/nguongocso/certification/enums/InspectionResultEntryLinkStatus.java`
- `backend/src/main/java/vn/nguongocso/certification/enums/InspectionResultEntrySource.java`
- `backend/src/main/java/vn/nguongocso/certification/repository/InspectionResultEntryLinkRepository.java`
- `backend/src/main/java/vn/nguongocso/certification/service/InspectionResultEntryLinkService.java`
- `backend/src/main/java/vn/nguongocso/certification/service/impl/InspectionResultEntryLinkServiceImpl.java`
- `backend/src/main/java/vn/nguongocso/certification/service/InspectionResultPortalRateLimitService.java`
- `backend/src/main/java/vn/nguongocso/certification/controller/InspectionResultEntryLinkController.java`
- `backend/src/main/java/vn/nguongocso/certification/controller/PublicInspectionResultEntryController.java`
- `backend/src/main/java/vn/nguongocso/certification/dto/request/IssueInspectionResultEntryLinkRequest.java`
- `backend/src/main/java/vn/nguongocso/certification/dto/response/InspectionResultEntryLinkResponse.java`
- `backend/src/main/java/vn/nguongocso/certification/dto/response/PublicInspectionResultEntryResponse.java`
- DTO criterion public riêng nếu cần để tránh dùng DTO nội bộ.
- Backend test files liệt kê tại `04-implementation-plan.md`.

### Backend — modify

- `InspectionCriterionResult.java`
- `InspectionRequestRepository.java`
- `InspectionCriterionResultService.java`
- `InspectionCriterionResultServiceImpl.java`
- `InspectionCriterionResultResponse.java`
- `EmailService.java`, `EmailServiceImpl.java`
- `PublicInspectionCriterionResultDto.java`
- `PublicTraceServiceImpl.java`
- Các test hiện hữu tương ứng.
- `SecurityConfig.java` chỉ khi test chứng minh prefix `/api/v1/public/**` chưa đủ; mặc định không sửa.

### Frontend — new

- `frontend/src/api/inspectionResultPortalApi.ts`
- `frontend/src/types/inspectionResultPortal.ts`
- `frontend/src/components/certification/InspectionResultEntryForm.tsx`
- `frontend/src/components/certification/IssueInspectionResultLinkDialog.tsx`
- `frontend/src/pages/public/InspectionResultEntryPage.tsx`
- Các test mới liệt kê tại `04-implementation-plan.md`.

### Frontend — modify

- `frontend/src/api/axiosConfig.ts`
- `frontend/src/api/certificationApi.ts` nếu authenticated link API không đặt chung portal API
- `frontend/src/types/certification.ts`
- `frontend/src/routes/AppRoutes.tsx`
- `frontend/src/pages/certification/RecordInspectionResultPage.tsx`
- `frontend/src/components/certification/InspectionRequestActionButtons.tsx`
- Nơi render action buttons nếu cần truyền callback/dialog state
- `frontend/src/components/public/PublicInspectionSection.tsx`
- `frontend/src/types/publicInspection.ts`
- Các test hiện hữu tương ứng.

## 10. Modules that should not change

- Auth login/JWT/organization selection và role catalog.
- `partner_api_keys`, `ApiKeyAuthenticationFilter` và partner APIs.
- `invitations`, membership join flow và password reset tables/APIs.
- Testing unit/accreditation CRUD và DB schema.
- Inspection criterion catalog/category assignment.
- Production lot/shipment/trace-code state machines và seal activation logic.
- Inspection expiry scheduler/threshold semantics, ngoài việc được gọi như hiện tại.
- Failed-lot/reinspection semantics.
- `activity_logs` schema/listener.
- Các migration V1–V65 và migration timestamp đã tồn tại.
- Các module farm, warehouse, recall, feedback, monitoring không liên quan.
- File đang có thay đổi ngoài Story `docs/api/farm/DrawFarmAreaBoundary.md` phải được giữ nguyên và không đưa vào commit Story.

## 11. Test matrix bị khóa

| Nhóm | Ca kiểm thử | Expected |
|---|---|---|
| AC-01 | Issue valid link, public GET, complete submit | 201/200/200, results source portal, link USED |
| AC-02 | Clock sau expiry | 410, không lộ detail, hướng dẫn cấp lại |
| AC-03 | Replay + concurrent double submit | Chỉ một success, các lần còn lại 410/conflict |
| AC-04 | Manual batch/single result | Source manual, active link revoked |
| Auth | VT-01/03/04/05 issue link | 403 |
| Tenant | VT-02 org B issue/status request org A | 404/deny |
| Scope | Token A + criterion/file của request B | 404/deny, không ghi |
| Token | invalid hash, revoked, used, expired | Generic 404 hoặc 410 đúng contract |
| State | issue/submit khi request PASSED/FAILED/CANCELLED | 409 |
| Validation | empty/duplicate/missing criterion | 400, no partial writes, token remains active |
| Date | pass thiếu ngày, expiry trước result/past | 400 |
| Fail | `passed=false` theo semantics backend hiện tại | Lưu hợp lệ theo rule hiện hành |
| File | allowed types/size/path traversal/foreign handle | Đúng 200/413/415/deny |
| QTN-21 | all pass, one fail, expiry | Status/seal eligibility như manual |
| Alert | portal submit gần hết hạn | Trigger expiry scan như manual |
| Source | old data/manual/portal, manual correction | Enum và UI badge đúng |
| Null actor | Portal response/public trace | Không NPE; actor nullable |
| Rate limit | vượt token/IP limit | 429 |
| Secret | DB/log/error/status endpoint | Không có raw token/full URL |
| Email | gửi đúng recipient/expiry/link | Async call; log không lộ secret |
| UI | active/invalid/expired/used/success/loading/error | Đúng state và accessible |
| Regression | Existing request/result/history/reinspection/public trace | Không đổi hành vi ngoài additive field |
| Migration | clean DB + upgrade DB có result | Pass, dữ liệu cũ source manual |

## 12. Regression requirements bị khóa

1. Existing manual endpoints giữ path, method, required payload và HTTP success code.
2. `PENDING_RESULT/PASSED/FAILED/CANCELLED` mapping giữ nguyên.
3. Batch vẫn all-or-nothing và đủ toàn bộ criteria.
4. Failed result/null dates theo logic hiện hành không bị siết khác bởi portal.
5. QTN-21, expiry warnings, seal gate và reinspection history không đổi.
6. Public trace current/latest/history và i18n fallback không đổi ngoài source badge.
7. Testing unit dropdown/scope warning không đổi.
8. Activity log manual vẫn có user/organization; public không tạo bản ghi invalid.
9. File read/upload internal giữ ownership và path normalization.
10. Frontend VT-02 result page vẫn hoạt động sau khi tách component.

## 13. Definition of Done

- [ ] API contract được tạo trước code và khớp implementation.
- [ ] Migration chạy thành công trên DB sạch và upgrade; dữ liệu cũ được bảo toàn/backfill.
- [ ] Token hash-only, expiry, revoke/reissue, atomic one-time consume hoạt động.
- [ ] Backend role + tenant + object scope được kiểm bằng automated tests.
- [ ] Đủ 4 AC và 3 QTN được trace/test.
- [ ] Manual/public cùng dùng validation/status/expiry logic; không duplicate domain rules.
- [ ] Source được lưu và hiển thị đúng ở internal/public trace.
- [ ] Email hoạt động và secret không xuất hiện trong DB/log/status endpoint.
- [ ] Public portal có đủ UI states và không phụ thuộc login/JWT.
- [ ] Backend full tests pass.
- [ ] Frontend tests, lint và build pass.
- [ ] UI runtime/Playwright flow pass, không có console error liên quan.
- [ ] API docs, comments/JavaDoc tiếng Việt, UTF-8 hợp lệ.
- [ ] `git diff --check` pass; commit chỉ chứa file trong scope; không gồm thay đổi farm boundary có sẵn.
- [ ] Final validation xác nhận không hồi quy request/result/reinspection/public trace/QTN-21.

## 14. Planning Gate và điểm cần theo dõi

Không còn blocker kỹ thuật ngăn lập kế hoạch. Các quyết định sau đã được khóa ở mức implementation contract để tránh dừng giữa chừng:

- expiry mặc định 7 ngày, min 1/max 30;
- email nhập/xác nhận lúc cấp link;
- per-result provenance;
- public actor không có user;
- upload optional theo từng criterion như contract hiện hữu;
- rate limit in-memory giai đoạn đầu, có ghi chú hạn chế multi-instance.

Nếu Product Owner thay đổi bất kỳ quyết định nào trên, phải cập nhật `01`, `03`, API docs và Contract trước khi triển khai. Planner dừng tại đây; chưa được phép sửa production code trong giai đoạn hiện tại.
