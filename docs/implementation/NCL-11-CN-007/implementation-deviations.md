# Ghi nhận sai khác triển khai — NCL-11-CN-007 (Implementation Deviations)

> **User Story:** NCL-11-CN-007 — Cổng nhập kết quả dành cho đơn vị kiểm nghiệm
>
> **Nhánh phát triển:** `feature/NCL-11-CN-007-inspection-result-entry-portal`
>
> **Ngày cập nhật:** 16/09/2026
> **Trạng thái:** Đã phê duyệt và phản ánh đầy đủ vào mã nguồn, tài liệu API và bộ kiểm thử.

---

## 1. Mục đích tài liệu

Tài liệu này ghi nhận và giải trình toàn bộ các điểm sai khác vật chất (deviations), cải tiến kỹ thuật và điều chỉnh kiến trúc so với tài liệu thiết kế ban đầu (`03-solution-design.md`, `05-implementation-contract.md`). Các thay đổi này nhằm khắc phục triệt để các vấn đề bảo mật (BLOCKER 1, BLOCKER 2), nâng cao tính nhất quán đa người dùng (MAJOR 1, MAJOR 2), đồng bộ hợp đồng kích thước tệp (MAJOR 3, MAJOR 5) và củng cố độ bao phủ kiểm thử (MAJOR 6).

---

## 2. Bảng tổng hợp các sai khác và lý do điều chỉnh

| STT | Khu vực | Thiết kế ban đầu / Contract cũ | Triển khai thực tế | Căn cứ & Lợi ích kỹ thuật |
|:---:|---|---|---|---|
| **1** | **Bảo mật tệp tải lên (BLOCKER 1 & QTN-20)** | Upload trả về đường dẫn server thực tế (`/uploads/...`); submit gửi lại `filePath` mà không xác thực quyền sở hữu. | Server sinh mã che giấu **Opaque File Handle** (`pfh_...`) và lưu bộ liên kết `(tokenHash, requestId, criterionId, realFilePath)`. Client chỉ nhận và gửi mã handle. | Ngăn chặn hoàn toàn việc client công khai biết đường dẫn vật lý trên server và loại trừ tấn công mạo danh tệp giữa các yêu cầu/chỉ tiêu kiểm nghiệm khác nhau. |
| **2** | **Cơ chế cấp/cấp lại link đồng thời (BLOCKER 2 & QTN-14)** | Đọc link `ACTIVE`, revoke và insert link mới bằng các bước JPA thông thường, không có khóa hàng. | Sử dụng Khóa bi quan ghi (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) trên `InspectionRequest` kết hợp câu lệnh update nguyên tử `revokeActiveLinksByRequestId`. | Đảm bảo bất biến QTN-14: Tại mọi thời điểm, mỗi yêu cầu kiểm nghiệm chỉ có duy nhất tối đa 1 liên kết ở trạng thái `ACTIVE`, loại bỏ triệt để race condition khi 2 quản lý cấp cùng lúc. |
| **3** | **Xung đột giữa HTX nhập tay và Cổng nộp (MAJOR 2)** | Chưa có cơ chế giải quyết xung đột khi HTX ghi nhận tay đồng thời với lúc Đơn vị kiểm nghiệm gửi qua portal. | Cả hai luồng đều khóa bi quan `InspectionRequest` và kiểm tra nghiêm ngặt `status == PENDING_RESULT`. Luồng đến sau sẽ bị từ chối `409 CONFLICT` hoặc `410 GONE`. | Bảo toàn ngữ nghĩa một lần (one-time semantics), tránh việc trạng thái hoặc nguồn gốc (`entrySource`) của từng chỉ tiêu bị phân mảnh hoặc ghi đè không nhất quán. |
| **4** | **Cô lập dữ liệu tổ chức (MAJOR 1)** | Truy vấn chi tiết rồi lọc `organizationId` trong bộ nhớ ứng dụng. | Chuyển sang truy vấn trực tiếp có điều kiện tổ chức tại tầng cơ sở dữ liệu (`findByIdAndProductionLot_Organization_OrganizationId`). | Đảm bảo tính cô lập đa người thuê (Multi-tenant Isolation) theo QTN-01 ngay từ câu lệnh SQL. |
| **5** | **Đồng bộ giới hạn kích thước tệp (MAJOR 3)** | Frontend thông báo và cho phép chọn file tối đa 10 MB, trong khi backend giới hạn 5 MB. | Đồng bộ thống nhất 5 MB (5.242.880 bytes) trên toàn hệ thống: Backend config, validator, form dùng chung và form nhập tay nội bộ. | Tránh lỗi người dùng chọn file 5–10 MB thành công ở client nhưng bị server từ chối `413 Payload Too Large`. |
| **6** | **Mã trạng thái HTTP & Endpoint Design (MAJOR 5, 8)** | Cấp link trả `200 OK`, nộp portal ghi nhận dạng `POST`, path file chung `/files`. | - Cấp link trả `201 CREATED`.<br>- Nộp portal dùng `PUT /results` (conditional state transition).<br>- Tải tệp dùng `/criteria/{criterionId}/file` để ràng buộc chỉ tiêu ngay lúc tải. | Tuân thủ nghiêm ngặt chuẩn thiết kế RESTful và tăng cường kiểm soát phạm vi đối tượng. |
| **7** | **Đồng bộ tên trường đơn vị kiểm nghiệm (MAJOR 5)** | Tài liệu API dùng `testingUnitName`, code nội bộ dùng `testingUnit`. | Hỗ trợ song song cả `testingUnitName` và `testingUnit` (alias) trong DTO và TypeScript types. | Tương thích ngược với cả client cũ và các hệ thống đối tác tích hợp theo API doc mới. |
| **8** | **Quy tắc Giới hạn Tần suất Rate Limiting (QTN-20)** | Thiết kế cũ đề xuất 15 requests/phút/IP. | Triển khai theo QTN-20: Tối đa 60 requests/giờ cho mỗi token hợp lệ và tối đa 30 lần thử sai/giờ cho mỗi địa chỉ IP. | Phù hợp với đặc thù phiên làm việc của chuyên viên kiểm nghiệm (thao tác kiểm tra nhiều lần nhưng không dồn dập, đồng thời bảo vệ hệ thống trước tấn công brute-force token). |

---

## 3. Chi tiết triển khai kỹ thuật cho các sai khác

### 3.1. Opaque File Handle Architecture (`InspectionResultPortalFileStorageService`)
- Khi người dùng công khai tải file lên qua `POST /api/v1/public/inspection-result-entry/{token}/criteria/{criterionId}/file`:
  1. Kiểm tra kích thước file <= 5 MB và MIME type thuộc `{image/jpeg, image/png, application/pdf}`.
  2. Lưu file an toàn trên máy chủ với tên ngẫu nhiên UUID.
  3. Sinh mã `fileHandle = "pfh_" + UUID` và lưu vào bộ nhớ cache an toàn kèm metadata: `tokenHash`, `requestId`, `criterionId`, `realFilePath`.
  4. Trả về cho client mã `fileHandle`.
- Khi client submit kết quả qua `PUT /api/v1/public/inspection-result-entry/{token}/results`:
  1. Với mỗi `filePath` (thực chất là handle) gửi lên, gọi `validateAndConsumeHandle(handle, tokenHash, requestId, criterionId)`.
  2. Nếu không khớp token, request hoặc criterion, ném ngay `403 FORBIDDEN`.
  3. Giải mã ra đường dẫn file thực tế trên server và lưu vào thực thể `InspectionCriterionResult`.

### 3.2. Concurrency Control & State Transition Lock
- Khóa bi quan được áp dụng tại `requestRepository.findByIdAndOrganizationIdForUpdate` và `requestRepository.findByIdForUpdate`.
- Câu lệnh SQL nguyên tử đảm bảo việc thu hồi hoặc tiêu thụ liên kết không thể bị can thiệp bởi luồng khác:
  ```sql
  UPDATE inspection_result_entry_links
     SET status = 'USED', used_at = :now, used_ip = :ip, used_user_agent = :ua
   WHERE id = :linkId AND status = 'ACTIVE' AND expires_at > :now;
  ```
- Nếu kết quả trả về `0`, hệ thống phản hồi ngay `410 GONE`, ngăn chặn hoàn toàn việc gửi trùng lặp.

---

## 4. Kết luận

Các thay đổi trên không làm phá vỡ logic nghiệp vụ gốc của User Story NCL-11-CN-007, mà hoàn thiện hệ thống theo đúng các tiêu chuẩn nghiêm ngặt nhất về an toàn dữ liệu, tính toàn vẹn giao dịch và trải nghiệm người dùng. Tất cả các sai khác đã được cập nhật đồng bộ vào:
1. `docs/api/certification/inspection-result-entry-portal.md`
2. `docs/implementation/NCL-11-CN-007/06-test-report.md`
3. Mã nguồn Backend (`vn.nguongocso.certification.*`)
4. Mã nguồn Frontend (`frontend/src/*`)
5. Bộ kiểm thử tự động toàn diện (`InspectionResultPortalConcurrencyAndSecurityTest.java`, `InspectionResultPortalRateLimitServiceTest.java`).
