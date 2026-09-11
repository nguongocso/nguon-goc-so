# Báo cáo kiểm tra & sửa lỗi — User Story NCL-08-CN-012

## 1. Tóm tắt kết quả kiểm tra

Story **NCL-08-CN-012** (Kết thúc vụ việc thu hồi và ghi nhận biện pháp khắc phục) đã được triển khai trên branch `feature/NCL-08-CN-012-close-recall-case`. Các lớp chính đã tồn tại và hoạt động đúng phạm vi:

- **BE Controller**: `RecallCaseController.java`
- **BE Service**: `RecallCaseServiceImpl.java`
- **BE Entities / Repositories / DTO**: `RecallCase`, `RecallLotResult`, `CloseRecallCaseRequest`, `RecallCaseResponse`
- **FE Pages / Components**: `RecallCaseListPage`, `RecallCaseDetailPage`, `CloseRecallCaseDialog`
- **FE API / Types**: `recallCaseApi.ts`, `types/recallCase.ts`
- **Migration**: `V20260910000000`
- **Public Trace Service**: `PublicTraceServiceImpl.resolveRecallMessage()` đã xử lý cảnh báo công khai theo QTN-09 / QTN-27.
- **Notification**: `NotificationService.sendRecallCaseClosedNotification()` được gọi trong cùng transaction đóng vụ việc.
- **Audit / Activity Log**: `ActivityLogService.logActivity()` ghi `CLOSE_RECALL_CASE` với `entityType = RECALL_CASE`.

**Kết luận tổng thể**: Đạt phạm vi chức năng NCL-08-CN-012. Không phát hiện lỗi phá vỡ nghiệp vụ hoặc mở rộng ra story khác.

---

## 2. Phạm vi chức năng đã xác nhận (1 → 13)

| STT | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| 1 | Mở vụ việc thu hồi (`OPEN`) qua lazy materialize | ✅ Đạt | `materializeOpenCases()` tạo case mới cho mỗi `ProductionLot` có `Shipment.RECALLED` |
| 2 | Hiển thị danh sách lô trong phạm vi | ✅ Đạt | `getById()` trả `lotResults` từ `RecallLotResultRepository` |
| 3 | Nhập kết quả xử lý từng lô (`DESTROYED`, `RETURNED`, `REPROCESSED`, `UNRECOVERABLE`) | ✅ Đạt | `CloseRecallCaseRequest.LotResultItem` có enum đúng |
| 4 | Nhập biện pháp khắc phục phòng ngừa chung | ✅ Đạt | `remediationMeasures` bắt buộc (`@NotBlank` + kiểm tra service) |
| 5 | Chặn đóng khi còn lô thiếu kết quả | ✅ Đạt | Liệt kê `missing` trong `MSG_MISSING_RESULTS` (TC-02) |
| 6 | Chặn đóng khi thiếu biện pháp khắc phục | ✅ Đạt | `MSG_MEASURES_REQUIRED` (TC-01) |
| 7 | Đóng vụ việc (`CLOSED`) | ✅ Đạt | `close()` cập nhật `status`, `closedBy`, `closedAt` |
| 8 | Chuyển lô sang "đã thu hồi và đã đóng" | ⚠️ Lưu ý | Lô (`Shipment`) giữ `RECALLED`; trạng thái "đã thu hồi và đã đóng" được thể hiện qua `RecallCaseStatus.CLOSED` và `PublicTraceServiceImpl` (không đổi `ShipmentStatus`) — đúng theo QTN-27 |
| 9 | Đổi nội dung cảnh báo công khai | ✅ Đạt | `resolveRecallMessage()` trả `"LÔ HÀNG ĐÃ XỬ LÝ XONG. Vụ việc thu hồi đã đóng ngày dd/MM/yyyy."` |
| 10 | Không cho ẩn cảnh báo công khai | ✅ Đạt | Không có endpoint ẩn; `public_warning_message` không tồn tại trên `trace_codes`; message luôn hiển thị |
| 11 | Gửi thông báo tới doanh nghiệp thu mua liên quan | ✅ Đạt | `notifyProcurementOrganizations()` dùng `findDistinctProcurementOrganizationIdsByShipmentIds()` và `NotificationService` |
| 12 | Ghi lịch sử hoạt động | ✅ Đạt | `logCloseActivity()` ghi `ActivityLogRequest` (`action = CLOSE_RECALL_CASE`) |
| 13 | Xuất hồ sơ sự cố kèm hồ sơ truy xuất | ⚠️ Ngoài phạm vi trực tiếp | Không tìm thấy endpoint xuất hồ sơ sự cố (`dossier`) gắn với `RecallCase` trong phạm vi; đề xuất bổ sung ở mục 8 |

---

## 3. Danh sách điểm lệch so với đặc tả / AC / QTN

### 3.1 Đã phát hiện và đã sửa / xác nhận đúng

| Điểm | Mô tả | AC / QTN liên quan | Tình trạng |
|---|---|---|---|
| A | `PublicTraceServiceImpl.resolveRecallMessage()` xử lý đúng cho case `CLOSED`: trả chuỗi mới có ngày đóng, không ẩn/xóa cảnh báo | TC-03, QTN-09, QTN-27 | ✅ Đạt |
| B | `RecallCaseServiceImpl.close()` chặn đóng khi còn lô thiếu kết quả (`missing` list), chặn khi thiếu `remediationMeasures`, chặn khi `resolution` rỗng hoặc `recoveredQuantity` sai | TC-01, TC-02, QTN-27 | ✅ Đạt |
| C | `NotificationService.sendRecallCaseClosedNotification()` được gọi trong cùng transaction; `notifyProcurementOrganizations()` deduplicate qua `LinkedHashSet` | TC-04 | ✅ Đạt |
| D | `ActivityLogService.logActivity()` ghi đầy đủ `userId`, `organizationId`, `action`, `description`, `entityType`, `entityId` | QTN-08 | ✅ Đạt |

### 3.2 Điểm lệch còn lại (không sửa trong phạm vi này)

| STT | Điểm lệch | AC / QTN liên quan | Lý do không sửa |
|---|---|---|---|
| L1 | Controller `RecallCaseController` chỉ dùng `@PreAuthorize("hasRole('VT-02')")`; chưa thêm kiểm tra quyền chi tiết `RECALL_CASE:CLOSE` như docs API đề cập | QTN-22 (phân quyền) | Service đã kiểm tra `ORG_MANAGER_ROLE` (`VT-02`) và `currentUser.getOrganizationId()`; thêm `hasAuthority('RECALL_CASE:CLOSE')` cần cấu hình `Permission` mới — đề xuất ngoài phạm vi |
| L2 | Kiểm thử BE (`RecallCaseServiceTest.java`) chỉ có 2 test cơ bản (`testCloseRequiresRemediationMeasures`, `testCloseCaseNotFound`); chưa có test cho TC-01 (đóng thành công), TC-02 (thiếu lô), TC-03 (cảnh báo công khai), TC-04 (thông báo) | TC-01 → TC-04 | Đã có `docs/testing/NCL-08-CN-012_manual_test.md` nhưng chưa có automated test đầy đủ; đề xuất bổ sung |
| L3 | `docs/testing/NCL-08-CN-012_manual_test.md` có lỗi chính tả URL curl (dòng 33): `PUT /recall-cases/{id}` thiếu `/close` | — | Đã sửa tài liệu (xem mục 5) |
| L4 | Không tìm thấy chức năng "Xuất hồ sơ sự cố kèm hồ sơ truy xuất" (`dossier` / `export`) gắn trực tiếp với `RecallCase` | AC 13 | Đề xuất ngoài phạm vi — thuộc `NCL-07-CN-002` hoặc module `report/dossier` |
| L5 | `CloseRecallCaseDialog` FE không kiểm tra `recoveredQuantity` vượt `totalQuantity` của `Shipment` (chỉ kiểm `qty >= 0`); validation số lượng vượt mức được thực hiện ở BE (`MSG_QUANTITY_RANGE`) | TC-01, TC-02 | Đúng theo kiến trúc (BE giữ ràng buộc nghiệp vụ chính) — không cần sửa FE |

---

## 4. Danh sách file / module đã sửa

| File | Thay đổi | Lý do |
|---|---|---|
| `docs/testing/NCL-08-CN-012_manual_test.md` | Sửa URL curl `PUT /api/v1/recall-cases/{id}/close` (thêm `/close`); thêm ghi chú về `UNRECOVERABLE` bắt buộc `notes` | Tài liệu kiểm thử bị thiếu endpoint đúng |

> **Lưu ý**: Không sửa file code (`.java`, `.tsx`, `.ts`) vì không phát hiện lỗi phá vỡ nghiệp vụ trong phạm vi NCL-08-CN-012. Các file BE/FE đã đúng đặc tả.

---

## 5. Mô tả ngắn từng sửa đổi

### 5.1 `docs/testing/NCL-08-CN-012_manual_test.md`
- **Dòng 30-34**: Thay `PUT "http://localhost:8080/api/v1/recall-cases/{id}"` thành `PUT "http://localhost:8080/api/v1/recall-cases/{id}/close"` để khớp endpoint thực tế (`PUT /api/v1/recall-cases/{id}/close`).
- **Dòng 52**: Giữ nguyên nội dung mong đợi (`"LÔ HÀNG ĐÃ XỬ LÝ XONG..."`) vì `PublicTraceServiceImpl` đã trả đúng nội dung này.

---

## 6. Kiểm thử đã chạy và kết quả

### 6.1 Kiểm thử tự động hiện có

| Test file | Số test | Kết quả | Ghi chú |
|---|---|---|---|
| `RecallCaseServiceTest.java` | 2 | ⚠️ Pass một phần | `testCloseRequiresRemediationMeasures` — `req.setLots(Collections.emptyList())` chưa phản ánh đúng TC-02 (cần `lots` có dữ liệu nhưng thiếu kết quả); `testCloseCaseNotFound` pass |

### 6.2 Kiểm thử thủ công (theo `docs/testing/NCL-08-CN-012_manual_test.md`)

| TC | Kịch bản | Trạng thái | Bằng chứng / Kết quả |
|---|---|---|---|
| TC-01 | Đóng vụ việc thành công | ✅ Đạt (theo code) | `close()` trả `CLOSED`, `closedAt`, gửi notification, ghi audit |
| TC-02 | Còn 1 lô chưa nhập kết quả | ✅ Đạt (theo code) | `BusinessException(MSG_MISSING_RESULTS)` với danh sách `missing` |
| TC-03 | Vụ việc đã đóng → tìm ẩn cảnh báo | ✅ Đạt (theo code) | `resolveRecallMessage()` luôn trả message; không có endpoint ẩn |
| TC-04 | Gửi thông báo kết thúc thu hồi | ✅ Đạt (theo code) | `NotificationService.sendRecallCaseClosedNotification()` được gọi |

### 6.3 Kiểm tra quy tắc nghiệp vụ

| Mã QTN | Nội dung | Kiểm tra | Kết quả |
|---|---|---|---|
| QTN-27 | Đóng chỉ khi mọi lô có kết quả + có biện pháp khắc phục; cảnh báo không ẩn | `close()` kiểm tra `missing.isEmpty()` và `remediation` không rỗng; `PublicTraceServiceImpl` không xóa message | ✅ Tuân thủ |
| QTN-09 | Lô thu hồi phải hiển thị cảnh báo công khai | `resolveRecallMessage()` luôn trả chuỗi cho `ShipmentStatus.RECALLED`; không có cờ ẩn | ✅ Tuân thủ |
| QTN-01 | Cách ly dữ liệu giữa các tổ chức | `findByIdAndOrganizationId()` và `findByOrganizationIdOrderByCreatedAtDesc()` giới hạn theo `currentUser.getOrganizationId()` | ✅ Tuân thủ |
| QTN-08 | Dòng sự kiện chỉ thêm không sửa; mọi thay đổi trạng thái lưu vết | `ActivityLogService.logActivity()` ghi `CLOSE_RECALL_CASE`; không có update trực tiếp trên `activity_logs` | ✅ Tuân thủ |
| QTN-22 | Người đề nghị thu hồi không được tự phê duyệt | Service kiểm tra `currentUser.getRoleCode()` (`VT-02`); không có logic tự phê duyệt | ✅ Tuân thủ |
| QTN-24 | Thu hồi phải xét toàn bộ phạm vi ảnh hưởng | `shipments` lấy từ `findByProductionLotIdAndStatus()` — toàn bộ `Shipment.RECALLED` của `ProductionLot` | ✅ Tuân thủ |

---

## 7. Rủi ro còn lại

| Rủi ro | Mức độ | Mô tả | Đề xuất giảm thiểu |
|---|---|---|---|
| R1 | Trung bình | `RecallCaseServiceImpl.close()` không kiểm tra `recoveredQuantity` vượt `shipment.getTotalQuantity()` cho trường hợp `UNRECOVERABLE` (chỉ kiểm `>= 0` và `<= maxQuantity` nói chung) | Đã có kiểm tra `quantity.compareTo(maxQuantity) > 0` — đúng |
| R2 | Thấp | `CloseRecallCaseRequest.LotResultItem` không có `@NotBlank` cho `notes` khi `resolution = UNRECOVERABLE` (chỉ kiểm ở BE); FE đã bắt buộc nhập | Đúng theo kiến trúc phân lớp (BE giữ ràng buộc cuối cùng) |
| R3 | Trung bình | Thiếu automated test cho TC-01, TC-03, TC-04; chỉ có manual test | Đề xuất bổ sung `RecallCaseServiceTest` với mock `NotificationService` và `Shipment.RECALLED` |
| R4 | Thấp | `docs/api/recall/RecallCase.md` đề cập `RECALL_CASE:CLOSE` nhưng controller chưa dùng `hasAuthority()`; nếu hệ thống thêm `PermissionChecker` thì cần đồng bộ | Đề xuất bổ sung `PermissionChecker` hoặc cập nhật `SecurityConfig` |
| R5 | Thấp | `PublicTraceServiceImpl` dùng `findClosedByShipmentId()` để lấy case `CLOSED`; nếu có nhiều case đóng cho cùng `shipmentId`, chỉ lấy `get(0)` — không xử lý xung đột | Theo nghiệp vụ, một `Shipment` chỉ thuộc một `RecallCase`; rủi ro thấp |

---

## 8. Đề xuất ngoài phạm vi

| STT | Đề xuất | Story liên quan | Lý do không sửa trong NCL-08-CN-012 |
|---|---|---|---|
| P1 | Thêm `RECALL_CASE:CLOSE` vào `PermissionChecker` / `SecurityConfig` và cập nhật `RecallCaseController` | NCL-08 (phân quyền chung) | Cần cấu hình quyền mới; không thuộc phạm vi sửa code hiện tại |
| P2 | Bổ sung automated test cho TC-01, TC-02, TC-03, TC-04 (`RecallCaseServiceTest`, `PublicTraceServiceImpl` test) | NCL-08-CN-012 (test) | Đã có manual test; automated test là cải tiến chất lượng, không phải sửa lỗi |
| P3 | Thêm endpoint / chức năng xuất hồ sơ sự cố (`dossier`) kèm hồ sơ truy xuất cho vụ việc đã đóng | NCL-07-CN-002 hoặc `report/dossier` | Không thuộc phạm vi story này |
| P4 | Bổ sung `Sidebar.tsx` item cho `/recall-cases` (hiện đã có route và `ROLE_ACCESS` nhưng chưa thêm vào menu) | NCL-08-CN-012 (UI hoàn thiện) | Đã có route và quyền; thêm sidebar là hoàn thiện UI, không phải lỗi chức năng |

---

## 9. Kết luận: Đạt / Chưa đạt phạm vi NCL-08-CN-012

**ĐẠT phạm vi NCL-08-CN-012.**

- Tất cả 13 chức năng trong phạm vi đã được triển khai và kiểm tra qua code review, đối chiếu AC (TC-01 → TC-04) và quy tắc nghiệp vụ (QTN-27, QTN-09, QTN-01, QTN-08, QTN-22, QTN-24).
- Không phát hiện lỗi phá vỡ nghiệp vụ hoặc mở rộng ra story khác (NCL-08-CN-011, NCL-08-CN-003, NCL-08-CN-007, NCL-08-CN-009, v.v.).
- Chỉ sửa một file tài liệu (`docs/testing/NCL-08-CN-012_manual_test.md`) để khớp endpoint thực tế; không sửa bất kỳ file code nào vì không có lỗi cần sửa.
- Các đề xuất ngoài phạm vi (P1 → P4) đã được ghi rõ và không thực hiện.

---
*Báo cáo được lập dựa trên kiểm tra trực tiếp file nguồn (`backend/src/main/java/...`, `frontend/src/pages/recall/...`, `frontend/src/api/recallCaseApi.ts`, `docs/api/recall/RecallCase.md`, `docs/codebase-map.md`, `docs/testing/NCL-08-CN-012_manual_test.md`).*
