# API Docs — Ghi nhận kết quả kiểm nghiệm và hiệu lực

Cập nhật theo code hiện tại  
Ngày cập nhật: 2026-08-16  
Nguồn đối chiếu: backend certification module hiện hành  

Tài liệu này phản ánh đúng luồng đang được triển khai trong code hiện tại, không dựa trên thiết kế mô tả cũ hoặc quy trình chưa tồn tại trong backend.

Mục tiêu: mô tả cách ghi nhận kết quả kiểm nghiệm cho từng chỉ tiêu, cách kiểm tra hiệu lực và cách xác định khả năng kích hoạt tem theo logic hiện hành của hệ thống.

## 1. Luồng nghiệp vụ thực tế

1) Quản lý hợp tác xã (VT-02) tạo yêu cầu kiểm nghiệm cho lô qua endpoint `POST /api/v1/production-lots/{lotId}/test-requests`.
2) Khi tạo yêu cầu, hệ thống lưu từng chỉ tiêu kiểm nghiệm thành snapshot `InspectionCriterion`; request được tạo với `status = PENDING_RESULT`.
3) Dựa trên yêu cầu đã tạo, đơn vị kiểm nghiệm thực hiện kiểm nghiệm và trả kết quả cho từng chỉ tiêu riêng lẻ.
4) Ghi nhận kết quả thực tế được thực hiện qua endpoint `POST /api/v1/inspection-criteria/{criterionId}/results`.
5) DTO kết quả kiểm nghiệm một chỉ tiêu gồm: `criterionId`, `resultDate`, `expiryDate`, `passed`, `filePath`.
6) Hệ thống không có endpoint ghi nhận kết quả cho cả request cùng lúc; không có API `PUT /api/v1/inspection-requests/{requestId}/result` trong code hiện tại.
7) Sau khi có kết quả, service kiểm tra điều kiện: tất cả chỉ tiêu của request phải có `passed = true` và `expiryDate >= ngày hiện tại`; nếu đủ điều kiện, request được cập nhật `status = PASSED`.
8) Dùng endpoint `POST /api/v1/production-lots/{lotId}/can-activate-seal` để kiểm tra lô có đủ điều kiện kích hoạt tem hay không.

## 2. Trạng thái hiện có trong code

Enum `InspectionRequestStatus` hiện có trong code: `PENDING_RESULT`, `PASSED`, `FAILED`, `CANCELLED`.

Không tồn tại enum `RESULTED` trong code hiện hành.

Yêu cầu chỉ được phép ghi nhận kết quả khi `status == PENDING_RESULT`.

Nếu tất cả chỉ tiêu đều đạt và còn hiệu lực thì service set status = `PASSED`.

`FAILED` không được set bởi logic ghi nhận kết quả hiện tại; nó chỉ được tính toán/đánh giá qua kiểm tra `can-activate-seal` và các tiêu chí không đạt.

## 3. Ràng buộc thực tế trong service

### 3.1. Chỉ Quản lý HTX (VT-02) được phép gọi các API ghi nhận kết quả và kiểm tra kích hoạt tem.

### 3.2. `inspectionRequest` phải ở trạng thái `PENDING_RESULT` tại thời điểm ghi nhận kết quả.

### 3.3. `resultDate` không được null; `expiryDate` không được null.

### 3.4. `expiryDate` phải >= `resultDate`, và `expiryDate` phải >= ngày hiện tại.

### 3.5. Mỗi chỉ tiêu chỉ có tối đa một kết quả kiểm nghiệm do unique constraint trên `inspection_criterion_id`.

### 3.6. Khi đã tồn tại result, service thực hiện UPDATE thay vì tạo bản ghi mới.

### 3.7. Mỗi chỉ tiêu được lưu dưới dạng `InspectionCriterionResult` với các trường: `resultDate`, `expiryDate`, `passed`, `filePath`, `createdBy`.

## 4. API thực tế trong hệ thống

| Method | Endpoint | Mục đích | Role | Logic thực tế | Ghi chú |
|---|---|---|---|---|---|
| POST | `/api/v1/production-lots/{lotId}/test-requests` | Tạo yêu cầu kiểm nghiệm cho lô | VT-02 | `status = PENDING_RESULT` | API tạo request, không phải ghi kết quả |
| GET | `/api/v1/test-requests` | Danh sách yêu cầu kiểm nghiệm | VT-02 | Hỗ trợ lọc `lotId/status/page/size` | Dùng `PageRequest` |
| POST | `/api/v1/inspection-criteria/{criterionId}/results` | Ghi nhận hoặc cập nhật kết quả cho 1 chỉ tiêu | VT-02 | Khi request đang ở `PENDING_RESULT`; validate `resultDate`, `expiryDate`, `passed` | Đây là API chính đang có trong code |
| GET | `/api/v1/inspection-requests/{requestId}/results` | Lấy toàn bộ kết quả thuộc một request | VT-02 | Trả danh sách `InspectionCriterionResultResponse` | Không chứa logic chốt status ở đây |
| GET | `/api/v1/inspection-criteria/{criterionId}/result` | Lấy kết quả của 1 chỉ tiêu | VT-02 | Dựa trên `criterionId` | Tìm theo `InspectionCriterionResult` |
| DELETE | `/api/v1/inspection-results/{resultId}` | Xóa kết quả kiểm nghiệm | VT-02 | Xóa rồi gọi `checkAndUpdateRequestStatus()` | Dùng để xoá lại kết quả nếu cần |
| POST | `/api/v1/production-lots/{lotId}/can-activate-seal` | Kiểm tra lô có đủ điều kiện kích hoạt tem | VT-02 | `canActivate`, `reason`, `earliestExpiryDate`, `totalCriteria`, `passedCriteria` | Logic thực sự quyết định tem có thể kích hoạt hay không |

## 5. DTO thực tế

`InspectionCriterionResultRequest` hiện có trong code gồm các trường sau:

- `criterionId: String` — ID của chỉ tiêu cần ghi nhận kết quả
- `resultDate: LocalDate` — ngày cấp kết quả
- `expiryDate: LocalDate` — ngày hết hiệu lực
- `passed: Boolean` — true = đạt, false = không đạt
- `filePath: String` — đường dẫn file phiếu kết quả (tùy chọn)

Không tồn tại các trường `requestId`, `overallResult`, `resultFileUrl`, `criteriaResults` trong DTO hiện tại.

Không có logic ghi nhận cả request bằng một payload tổng theo kiểu `PUT /inspection-requests/{requestId}/result`.

## 6. Logic kiểm tra hiệu lực và kích hoạt tem

Repository `InspectionCriterionResultRepository` định nghĩa các điều kiện sau:

- `areAllCriteriaPassedAndValid(requestId, today)`: true nếu `COUNT(r) = COUNT(c)` với các bản ghi có `r.passed = true` và `r.expiryDate >= today`.
- `countPassedAndValidCriteria(requestId, today)`: đếm số chỉ tiêu đạt và còn hiệu lực.
- `countTotalCriteria(requestId)`: đếm tổng số chỉ tiêu thuộc request.

Trong service `checkCanActivateSeal()`:

- nếu `requestTotal > 0` và `requestTotal != requestPassed` => `canActivate = false`, reason = `"Lô chưa có kết quả kiểm nghiệm đạt cho tất cả chỉ tiêu"`.
- nếu `expiryDate` của bất kỳ request nào < today => `canActivate = false`, reason = `"Kết quả kiểm nghiệm đã quá hạn"`.
- cuối cùng, response trả về `canActivate`, `reason`, `earliestExpiryDate`, `totalCriteria`, `passedCriteria`, `failedOrExpiredCriteria`.

## 7. Kết luận cập nhật

Đến thời điểm hiện tại, luồng chính xác trong hệ thống là: Tạo yêu cầu -> Ghi nhận kết quả cho từng chỉ tiêu riêng lẻ -> Kiểm tra hiệu lực -> Kiểm tra khả năng kích hoạt tem.

Điểm khác biệt quan trọng so với tài liệu cũ: không có trạng thái `RESULTED`, không có request-level result API, không có `overallResult` trong request.

Các API hiện có thực sự đang hoạt động theo code là:

- `POST /api/v1/production-lots/{lotId}/test-requests`
- `POST /api/v1/inspection-criteria/{criterionId}/results`
- `POST /api/v1/production-lots/{lotId}/can-activate-seal`

Nếu cần thống nhất API contract cho tương lai, phải cập nhật lại doc này dựa trên code hiện hành, không dựa trên mô hình thiết kế chưa implement.

## Nguồn code đối chiếu

- `backend/src/main/java/vn/nguongocso/certification/controller/InspectionCriterionResultController.java`
- `backend/src/main/java/vn/nguongocso/certification/service/impl/InspectionCriterionResultServiceImpl.java`
- `backend/src/main/java/vn/nguongocso/certification/repository/InspectionCriterionResultRepository.java`
- `backend/src/main/java/vn/nguongocso/certification/enums/InspectionRequestStatus.java`
- `backend/src/main/java/vn/nguongocso/certification/controller/InspectionRequestController.java`
