# NCL-08-CN-009 — API xử lý phản ánh của người tiêu dùng

## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| User Story | `NCL-08-CN-009` — Xem và xử lý phản ánh của người tiêu dùng |
| Nhánh triển khai | `feature/NCL-08-CN-009-feedback-processing` |
| Epic | `NCL-08` — Cảnh báo, thu hồi lô và lịch sử hoạt động |
| Business Rules | `QTN-01`, `QTN-12`, `QTN-26` |
| Phụ thuộc | `NCL-06-CN-003`, `NCL-08-CN-007`, `NCL-08-CN-008` |
| Trạng thái API | Đã triển khai (`IMPLEMENTED`) — chờ review giao diện và kiểm thử tích hợp trên local |
| Phạm vi | API nội bộ xử lý phản ánh và phần mở rộng cần thiết của API gửi phản ánh công khai |

Tài liệu này là contract đã dùng để triển khai backend, frontend và kiểm thử cho story. Các endpoint và
trường ở mục 8–10 đã có trong code; trạng thái hoàn tất cuối cùng vẫn phụ thuộc migration và kiểm thử
tích hợp trên môi trường local của dự án.

API gửi phản ánh công khai hiện hữu được mô tả tại
[`SendProductFeedback.md`](./SendProductFeedback.md). Việc sinh mã tra cứu và trang công khai để người
gửi xem trạng thái thuộc `NCL-06-CN-005`, không nằm trong phạm vi triển khai giao diện nội bộ của story
này.

Giao diện nội bộ sử dụng hai route tách biệt và cùng áp dụng quyền `VT-01`, `VT-02`:

- `/product-feedbacks`: danh sách, bộ lọc và phân trang phản ánh.
- `/product-feedbacks/{feedbackId}`: trang chi tiết toàn màn hình để xem, phân công, phân loại, lưu
  nội dung xử lý, tạo yêu cầu thu hồi và đóng phản ánh. Nút **Xem chi tiết** trên danh sách điều hướng
  sang route này; không xử lý nghiệp vụ trong modal.

## 2. Mục tiêu nghiệp vụ

Sau khi người tiêu dùng gửi phản ánh từ trang tra cứu công khai, hệ thống phải cho phép hợp tác xã:

1. Xem phản ánh đúng phạm vi tổ chức.
2. Phân loại mức độ.
3. Gán người chịu trách nhiệm xử lý.
4. Lưu nội dung xử lý nội bộ và câu trả lời được phép công khai.
5. Đóng phản ánh khi đủ điều kiện.
6. Tạo yêu cầu thu hồi có liên kết ngược về phản ánh gốc.
7. Đưa phản ánh nghi ngờ tem giả vào nguồn dữ liệu của danh sách mã tem nghi vấn.

Phản ánh không có API xóa và phải giữ được người thực hiện cùng các mốc thời gian quan trọng.

## 3. Hiện trạng và phạm vi thay đổi

### 3.1 Đã có trong hệ thống

- `POST /api/v1/public/production-lots/{productionLotId}/feedbacks` để gửi phản ánh công khai.
- `GET /api/v1/product-feedbacks` để lấy danh sách.
- `GET /api/v1/product-feedbacks/{feedbackId}` để lấy chi tiết.
- Phân trang theo `PageResponse<T>`.
- Giới hạn danh sách của `VT-02` theo tổ chức; `VT-01` có thể xem toàn bộ.
- API lấy thành viên đang hoạt động của tổ chức:
  `GET /api/v1/organization/members?status=ACTIVE`.
- API lấy lô hàng và mã tem theo lô sản xuất:
  `GET /api/v1/shipments/production-lots/{productionLotId}`.
- Luồng yêu cầu thu hồi nhiều bước tại `/api/v1/recall-requests`.
- Danh sách mã tem nghi vấn tại `/api/v1/admin/trace-codes/suspect`.

### 3.2 Cần triển khai

- Trạng thái, mức độ, người xử lý và các mốc xử lý của phản ánh.
- Bộ lọc danh sách theo trạng thái, mức độ, lô và người xử lý.
- API gán người xử lý, cập nhật nội dung, đóng phản ánh và tạo yêu cầu thu hồi.
- Liên kết phản ánh với mã tem nguồn.
- Liên kết hai chiều giữa phản ánh và yêu cầu thu hồi.
- Quyền `product_feedback:UPDATE` và kiểm tra vai trò tại controller/service.
- Audit log cho các thao tác thay đổi.

## 4. Quyết định nghiệp vụ dùng cho implementation

### D-01. Phạm vi vai trò

- `VT-01` được xem danh sách và chi tiết trên toàn nền tảng.
- `VT-02` được xem và cập nhật phản ánh của tổ chức hiện tại.
- `VT-01` chưa được xử lý thay `VT-02` trong contract này. Nếu nghiệp vụ muốn cho phép, phải thay đổi
  Permission Matrix và bổ sung test riêng trước khi code.
- Các vai trò khác không được truy cập API nội bộ của phản ánh, kể cả khi seed cũ đang có
  `product_feedback:READ`.

### D-02. Người được gán xử lý

- Phải là thành viên `ACTIVE` của tổ chức sở hữu lô sản xuất.
- Phải có vai trò `VT-03` — Người ghi sự kiện trong chính tổ chức đó.
- Danh sách chọn trên giao diện lấy từ `GET /api/v1/organization/members?status=ACTIVE` và chỉ giữ
  các membership có `roleCode=VT-03`.
- Việc được gán là thông tin phân công trách nhiệm, không tự cấp quyền API. Trong phạm vi story này,
  `VT-02` vẫn là vai trò gọi các API gán, cập nhật xử lý, yêu cầu thu hồi và đóng phản ánh.
- Có thể gán lại khi phản ánh chưa đóng.
- Không được gán hoặc gán lại phản ánh `CLOSED`.
- Gán lần đầu cho phản ánh `NEW` tự động chuyển trạng thái sang `IN_PROGRESS`.
- Mọi thao tác ghi nội dung xử lý, đóng hoặc tạo yêu cầu thu hồi đều yêu cầu phản ánh đã có người
  xử lý; backend không tự gán ngầm người gọi.

### D-03. Trạng thái đóng

- `CLOSED` là trạng thái cuối; story không có chức năng mở lại.
- Đóng bắt buộc có người xử lý, nội dung xử lý và lý do đóng.
- `publicResponse` không bắt buộc để đóng, nhưng nếu có phải được lưu tách khỏi nội dung nội bộ.
- Không được đóng khi tồn tại yêu cầu thu hồi liên kết ở trạng thái `PENDING`.

### D-04. Liên kết yêu cầu thu hồi

- Một phản ánh có thể có nhiều yêu cầu thu hồi trong lịch sử.
- Tại một thời điểm chỉ được có tối đa một yêu cầu `PENDING`.
- Khi tạo yêu cầu, hệ thống lấy `productionLotId` từ phản ánh; client không được truyền một lô khác.
- Người tạo yêu cầu không được tự phê duyệt theo `QTN-22`.
- API hiện tại chỉ cho `VT-03` tạo yêu cầu. Story này bổ sung trường hợp `VT-02` tạo từ phản ánh nhưng
  không được làm yếu quy tắc người tạo khác người duyệt.
- Giao diện chỉ hiển thị và cho tạo yêu cầu theo `severity` đã lưu. Nếu mức độ, mã tem hoặc nội dung
  xử lý còn là bản nháp, nút tạo yêu cầu bị khóa cho tới khi người dùng bấm **Lưu xử lý**.

### D-05. Phản ánh nghi ngờ tem giả

- Phân loại `COUNTERFEIT_SUSPECTED` bắt buộc phản ánh phải liên kết một `traceCodeId` cụ thể.
- Mã tem phải thuộc lô sản xuất của phản ánh.
- Giao diện chỉ hiển thị mã nghiệp vụ `traceCodeValue` (ví dụ `NGS-2026-000001`), không hiển thị hoặc
  yêu cầu người dùng nhập UUID `traceCodeId`.
- Nếu phản ánh đã có mã tem từ lần gửi công khai, mã nghiệp vụ được hiển thị ở trạng thái chỉ đọc.
- Nếu phản ánh chưa có mã tem, giao diện lấy danh sách mã thuộc lô qua
  `GET /api/v1/shipments/production-lots/{productionLotId}`, cho phép tìm/chọn theo `traceCodeValue`,
  sau đó ánh xạ sang `traceCodeId` để gửi nội bộ trong request xử lý.
- Việc phân loại chỉ đánh dấu nguồn nghi vấn; không tự động khóa mã.
- Quyết định khóa mã vẫn thuộc `NCL-08-CN-007` và chỉ `VT-01` thực hiện.
- Danh sách mã nghi vấn phải lấy cả nguồn phát hiện tự động và nguồn phản ánh của người tiêu dùng;
  không được ghi đè điểm nghi vấn tự động bằng một điểm giả lập.

### D-06. Không xóa dữ liệu

- Không thiết kế endpoint `DELETE`.
- Không xóa vật lý hoặc xóa mềm phản ánh trong phạm vi story này.

## 5. Trạng thái và mức độ

### 5.1 Trạng thái phản ánh

| Giá trị API | Nhãn giao diện | Ý nghĩa |
|---|---|---|
| `NEW` | Mới | Vừa được tiếp nhận, chưa bắt đầu xử lý |
| `IN_PROGRESS` | Đang xử lý | Đã có người xử lý hoặc đã lưu hoạt động xử lý |
| `ESCALATED_TO_RECALL` | Đã chuyển thu hồi | Đã tạo ít nhất một yêu cầu thu hồi từ phản ánh |
| `CLOSED` | Đã đóng | Đã có kết luận, nội dung xử lý, lý do và mốc đóng |

Chuyển trạng thái hợp lệ:

```text
NEW -> IN_PROGRESS
IN_PROGRESS -> ESCALATED_TO_RECALL
IN_PROGRESS -> CLOSED
ESCALATED_TO_RECALL -> IN_PROGRESS   (yêu cầu gần nhất bị từ chối và tiếp tục xử lý)
ESCALATED_TO_RECALL -> CLOSED        (không còn yêu cầu PENDING và đủ dữ liệu đóng)
```

Không cho phép:

- `NEW -> CLOSED` khi chưa gán người xử lý.
- Mọi chuyển trạng thái ra khỏi `CLOSED`.
- Client đặt trạng thái trực tiếp qua một endpoint cập nhật tổng quát.

### 5.2 Mức độ phản ánh

| Giá trị API | Nhãn giao diện | Quy tắc |
|---|---|---|
| `INFORMATION` | Thông tin | Giá trị mặc định khi tiếp nhận |
| `QUALITY_SUSPECTED` | Nghi ngờ chất lượng | Có thể tạo yêu cầu thu hồi |
| `COUNTERFEIT_SUSPECTED` | Nghi ngờ tem giả | Bắt buộc có mã tem; đưa vào nguồn danh sách nghi vấn |

## 6. Mô hình dữ liệu đề xuất

### 6.1 Bổ sung bảng `product_feedbacks`

| Cột | Kiểu đề xuất | Null | Quy tắc |
|---|---|---:|---|
| `status` | `VARCHAR(32)` | Không | Mặc định `NEW` |
| `severity` | `VARCHAR(32)` | Không | Mặc định `INFORMATION` |
| `trace_code_id` | `CHAR(36)` | Có | FK `trace_codes.id`; phải thuộc lô của phản ánh |
| `assigned_to` | `CHAR(36)` | Có | FK `users.user_id` |
| `assigned_at` | `DATETIME` | Có | Ghi khi gán người xử lý |
| `processing_content` | `TEXT` | Có | Ghi chú/nội dung xử lý nội bộ |
| `public_response` | `TEXT` | Có | Nội dung được phép trả về API công khai sau này |
| `close_reason` | `TEXT` | Có | Bắt buộc tại thời điểm đóng |
| `closed_by` | `CHAR(36)` | Có | FK `users.user_id` |
| `closed_at` | `DATETIME` | Có | Ghi khi đóng |
| `updated_at` | `DATETIME` | Không | Mốc cập nhật gần nhất |

Chỉ mục đề xuất:

- `(status, created_at)`
- `(severity, created_at)`
- `(assigned_to, status)`
- `(trace_code_id)`

### 6.2 Liên kết yêu cầu thu hồi

Bổ sung `source_feedback_id CHAR(36) NULL` vào `recall_requests`:

- FK tới `product_feedbacks.id`.
- Không đặt `UNIQUE`, vì cần giữ lịch sử nhiều lần yêu cầu sau khi một yêu cầu bị từ chối.
- Service phải chặn nhiều hơn một bản ghi `PENDING` cho cùng phản ánh.
- Thêm index `(source_feedback_id, status)`.

### 6.3 Audit log

Các hành động sau phải ghi lịch sử hoạt động:

- `ASSIGN_PRODUCT_FEEDBACK`
- `UPDATE_PRODUCT_FEEDBACK_PROCESSING`
- `ESCALATE_PRODUCT_FEEDBACK_TO_RECALL`
- `CLOSE_PRODUCT_FEEDBACK`

Audit hiện có `feedbackId`, tổ chức, người thực hiện, thời điểm và giá trị mới quan trọng trong mô tả.
Hạ tầng `@Auditable` hiện hành chưa lưu snapshot giá trị cũ; nếu team yêu cầu diff trước/sau có cấu trúc,
phần đó cần một thay đổi riêng cho audit framework.

## 7. Quy ước API chung

- Base path: `/api/v1`.
- Xác thực nội bộ: `Authorization: Bearer <JWT>`.
- Response wrapper: `ApiResult<T>` gồm
  `{ success, status, message?, data?, errors?, path?, timestamp }`.
- Phân trang: `PageResponse<T>` gồm
  `{ items, page, size, totalElements, totalPages, first, last }`.
- `page` bắt đầu từ `0`.
- Thời gian trả về theo ISO-8601.
- ID dùng UUID.
- Đối tượng không tồn tại và đối tượng ngoài phạm vi tổ chức đều trả `404` để tránh dò ID.

## 8. Response model nội bộ

`ProductFeedbackResponse`:

```json
{
  "id": "90fe86ba-596e-4709-b4d5-11f8b9445304",
  "productionLotId": "e28a83cf-3b36-4fd8-b10e-d8d12688f244",
  "productionLotName": "Lô chè xuân 2026",
  "organizationId": "bf85df50-a6b4-49b7-aa1b-f133ab209b66",
  "organizationName": "Hợp tác xã chè Phú Thịnh",
  "productCategoryName": "Chè",
  "traceCodeId": "88ed17be-2ceb-4541-97f2-c204683b978d",
  "traceCodeValue": "NGS-2026-000001",
  "content": "Thông tin trên tem không khớp với bao bì.",
  "status": "IN_PROGRESS",
  "severity": "QUALITY_SUSPECTED",
  "assignedToUserId": "68dfa52d-f371-4af7-bddd-9df09c875cd9",
  "assignedToName": "Nguyễn Văn An",
  "assignedAt": "2026-09-07T09:10:00",
  "processingContent": "Đã đối chiếu hồ sơ lô và liên hệ cơ sở sản xuất.",
  "publicResponse": "Hợp tác xã đã tiếp nhận và đang xác minh.",
  "closeReason": null,
  "closedByUserId": null,
  "closedByName": null,
  "closedAt": null,
  "latestRecallRequestId": null,
  "latestRecallRequestStatus": null,
  "hasPendingRecallRequest": false,
  "createdAt": "2026-09-07T08:30:00",
  "updatedAt": "2026-09-07T09:10:00"
}
```

Không dùng response model này cho API công khai vì có trường nội bộ.

## 9. Đặc tả endpoint nội bộ

### 9.1 Lấy danh sách phản ánh

```http
GET /api/v1/product-feedbacks
```

Phân quyền: `VT-01`, `VT-02` và `product_feedback:READ`.

Query parameters:

| Param | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---|---|---:|---|---|
| `keyword` | string | Không | — | Tìm theo nội dung, tên lô hoặc mã tem |
| `status` | enum | Không | — | `NEW`, `IN_PROGRESS`, `ESCALATED_TO_RECALL`, `CLOSED` |
| `severity` | enum | Không | — | Ba mức tại mục 5.2 |
| `productionLotId` | UUID | Không | — | Lọc theo lô sản xuất |
| `assignedToUserId` | UUID | Không | — | Lọc theo người xử lý |
| `page` | int | Không | `0` | Phải lớn hơn hoặc bằng 0 |
| `size` | int | Không | `20` | Từ 1 đến 100 |
| `sort` | string | Không | `createdAt,desc` | Chỉ cho phép `createdAt`, `updatedAt`, `status`, `severity` |

Quy tắc phạm vi:

- `VT-01`: toàn nền tảng.
- `VT-02`: backend luôn thêm điều kiện tổ chức hiện tại; client không truyền `organizationId` để đổi
  phạm vi.

Response `200 OK`: `ApiResult<PageResponse<ProductFeedbackResponse>>`.

### 9.2 Lấy chi tiết phản ánh

```http
GET /api/v1/product-feedbacks/{feedbackId}
```

Phân quyền: `VT-01`, `VT-02` và `product_feedback:READ`.

Response `200 OK`: `ApiResult<ProductFeedbackResponse>`.

Nếu `VT-02` mở phản ánh của tổ chức khác, trả `404 Not Found` với thông điệp
`"Không tìm thấy phản ánh"`.

### 9.3 Gán người xử lý

```http
PUT /api/v1/product-feedbacks/{feedbackId}/assignment
```

Phân quyền: chỉ `VT-02` và `product_feedback:UPDATE`.

Request:

```json
{
  "assignedToUserId": "68dfa52d-f371-4af7-bddd-9df09c875cd9"
}
```

Validation:

- `assignedToUserId` bắt buộc.
- Người được gán phải là membership `ACTIVE` của tổ chức sở hữu phản ánh.
- Người được gán phải có vai trò `VT-03` — Người ghi sự kiện.
- Người thuộc tổ chức khác, membership `INACTIVE` hoặc có vai trò khác `VT-03` đều bị từ chối.
- Không được gán phản ánh `CLOSED`.

Side effects:

- Ghi `assignedAt` bằng thời gian hiện tại.
- Nếu trạng thái là `NEW`, chuyển sang `IN_PROGRESS`.
- Ghi audit log.

Response `200 OK`: `ApiResult<ProductFeedbackResponse>`.

### 9.4 Cập nhật phân loại và nội dung xử lý

```http
PUT /api/v1/product-feedbacks/{feedbackId}/processing
```

Phân quyền: chỉ `VT-02` và `product_feedback:UPDATE`.

Request:

```json
{
  "severity": "QUALITY_SUSPECTED",
  "traceCodeId": null,
  "processingContent": "Đã kiểm tra hồ sơ lô và liên hệ cơ sở sản xuất.",
  "publicResponse": "Hợp tác xã đã tiếp nhận và đang xác minh."
}
```

Ràng buộc đề xuất:

| Field | Bắt buộc | Ràng buộc |
|---|---:|---|
| `severity` | Có | Thuộc enum tại mục 5.2 |
| `traceCodeId` | Có điều kiện | Bắt buộc nếu `severity=COUNTERFEIT_SUSPECTED` |
| `processingContent` | Không | Sau trim tối đa 4.000 ký tự |
| `publicResponse` | Không | Sau trim tối đa 2.000 ký tự |

Validation bổ sung:

- `traceCodeId` phải thuộc lô sản xuất của phản ánh.
- Không cập nhật phản ánh `CLOSED`.
- Phản ánh phải có `assignedTo`; nếu chưa có, client phải gọi API gán người xử lý trước.
- Trạng thái phải là `IN_PROGRESS` hoặc `ESCALATED_TO_RECALL`.
- Nếu chuyển sang `COUNTERFEIT_SUSPECTED`, cập nhật nguồn dữ liệu danh sách mã nghi vấn trong cùng
  transaction.

Quy ước giao diện:

- `traceCodeId` là khóa kỹ thuật chỉ dùng trong payload; không hiển thị cho người xử lý.
- Người xử lý nhìn và tìm theo `traceCodeValue`. Frontend chỉ bật nút lưu khi giá trị nhập khớp một
  mã thuộc lô và đã ánh xạ được sang `traceCodeId`.
- Mã đã liên kết được khóa chỉ đọc trên trang chi tiết. Thay đổi liên kết đã lưu không thuộc thao tác cập nhật
  thông thường của story này.
- Nếu mức độ hoặc mã tem đang khác dữ liệu đã lưu, nút **Đóng phản ánh** bị khóa. Nội dung xử lý và
  phản hồi công khai vẫn có thể được lưu nguyên tử qua API đóng theo mục 9.5.

Response `200 OK`: `ApiResult<ProductFeedbackResponse>`.

### 9.5 Đóng phản ánh

```http
PUT /api/v1/product-feedbacks/{feedbackId}/close
```

Phân quyền: chỉ `VT-02` và `product_feedback:UPDATE`.

Request:

```json
{
  "processingContent": "Đã xác minh và cập nhật lại thông tin của lô.",
  "publicResponse": "Thông tin đã được kiểm tra và điều chỉnh.",
  "closeReason": "Đã xử lý xong"
}
```

Ràng buộc đề xuất:

- `processingContent`: có thể bỏ qua nếu phản ánh đã lưu nội dung trước đó; giá trị hiệu lực sau cùng
  phải khác rỗng và tối đa 4.000 ký tự.
- `publicResponse`: không bắt buộc, tối đa 2.000 ký tự.
- `closeReason`: bắt buộc, tối đa 1.000 ký tự.
- Phải có `assignedTo`.
- Không có yêu cầu thu hồi liên kết `PENDING`.
- Không được đóng lại phản ánh đã `CLOSED`.

Side effects:

- Ghi `status=CLOSED`.
- Ghi `closedBy` từ người dùng hiện tại và `closedAt` bằng thời gian hiện tại.
- Ghi audit log.

Response `200 OK`: `ApiResult<ProductFeedbackResponse>`.

### 9.6 Tạo yêu cầu thu hồi từ phản ánh

```http
POST /api/v1/product-feedbacks/{feedbackId}/recall-requests
```

Phân quyền: chỉ `VT-02`, `product_feedback:UPDATE` và `recall:CREATE`.

Request:

```json
{
  "shipmentId": "9f488b4b-d15d-4701-af9d-c2d240ed316c",
  "reason": "Nghi ngờ chất lượng sản phẩm từ phản ánh người tiêu dùng.",
  "evidence": "Kết quả đối chiếu hồ sơ và nội dung phản ánh."
}
```

Ràng buộc:

- `shipmentId`: bắt buộc nếu phản ánh chưa liên kết mã tem; phải thuộc lô sản xuất của phản ánh.
- Nếu phản ánh đã có mã tem, backend tự xác định shipment chứa mã đó. `shipmentId` gửi lên (nếu có)
  phải trùng shipment đã xác định và giao diện không cho thay đổi.
- `reason`: bắt buộc, tối đa 1.000 ký tự.
- `evidence`: không bắt buộc, tối đa 2.000 ký tự.
- Mức độ phải là `QUALITY_SUSPECTED` hoặc `COUNTERFEIT_SUSPECTED`.
- Phản ánh phải có người xử lý và đang ở trạng thái `IN_PROGRESS`.
- Không tồn tại yêu cầu `PENDING` khác liên kết cùng phản ánh.
- `productionLotId` lấy từ phản ánh và chỉ dùng làm ngữ cảnh; phạm vi thu hồi thực tế là `shipmentId`.

Transaction phải thực hiện nguyên tử:

1. Tạo `RecallRequest` ở trạng thái `PENDING` với `sourceFeedbackId`.
2. Gắn yêu cầu với đúng shipment chứa mã tem hoặc shipment được chọn hợp lệ.
3. Ghi người tạo là người dùng hiện tại.
4. Chuyển phản ánh sang `ESCALATED_TO_RECALL`.
5. Ghi audit log.

Response `201 Created`: `ApiResult<RecallRequestResponse>`; response thu hồi bổ sung
`sourceFeedbackId`.

Khi yêu cầu được `APPROVED`, chỉ shipment liên kết và toàn bộ mã tem của shipment đó chuyển
`RECALLED`. Lô sản xuất và các shipment khác trong cùng lô sản xuất giữ nguyên trạng thái.

Khi yêu cầu liên kết được xử lý:

- `REJECTED`: chuyển phản ánh từ `ESCALATED_TO_RECALL` về `IN_PROGRESS` để tiếp tục xử lý.
- `APPROVED`: giữ `ESCALATED_TO_RECALL`; có thể đóng phản ánh vì không còn yêu cầu `PENDING`, nếu
  đồng thời thỏa các điều kiện đóng khác.

### 9.7 API không tồn tại

Không triển khai:

```http
DELETE /api/v1/product-feedbacks/{feedbackId}
```

## 10. Mở rộng API gửi phản ánh công khai

Giữ nguyên endpoint hiện hữu:

```http
POST /api/v1/public/production-lots/{productionLotId}/feedbacks
```

Request mở rộng tương thích ngược:

```json
{
  "content": "Tôi nghi ngờ mã tem này không hợp lệ.",
  "traceCodeValue": "NGS-2026-000001"
}
```

| Field | Bắt buộc | Ràng buộc |
|---|---:|---|
| `content` | Có | Không rỗng, tối đa 1.000 ký tự |
| `traceCodeValue` | Không | Nếu có phải tồn tại và thuộc lô trong path |

Quy tắc:

- Frontend gửi `traceCodeValue` khi form được mở từ một lần tra cứu QR/mã cụ thể.
- Backend tra mã và lưu `traceCodeId`; không tin trực tiếp quan hệ lô do client cung cấp.
- Phản ánh mới luôn có `status=NEW`, `severity=INFORMATION`.
- Không trả `ProductFeedbackResponse` nội bộ từ endpoint public.

Response public đề xuất:

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "90fe86ba-596e-4709-b4d5-11f8b9445304",
    "productionLotId": "e28a83cf-3b36-4fd8-b10e-d8d12688f244",
    "status": "NEW",
    "createdAt": "2026-09-07T08:30:00"
  },
  "timestamp": "2026-09-07T01:30:00Z"
}
```

`NCL-06-CN-005` sẽ bổ sung `lookupCode` và API công khai chỉ trả trạng thái cùng `publicResponse`;
không được trả `processingContent`, người xử lý hoặc dữ liệu nội bộ của tổ chức.

## 11. Permission Matrix

| API | VT-01 | VT-02 | Vai trò khác |
|---|---:|---:|---:|
| `GET /product-feedbacks` | Cho phép toàn nền tảng | Cho phép trong tổ chức | Từ chối |
| `GET /product-feedbacks/{id}` | Cho phép toàn nền tảng | Cho phép trong tổ chức | Từ chối |
| `PUT /{id}/assignment` | Từ chối | Cho phép trong tổ chức | Từ chối |
| `PUT /{id}/processing` | Từ chối | Cho phép trong tổ chức | Từ chối |
| `PUT /{id}/close` | Từ chối | Cho phép trong tổ chức | Từ chối |
| `POST /{id}/recall-requests` | Từ chối | Cho phép trong tổ chức | Từ chối |

Implementation phải kiểm tra đồng thời:

1. Role guard ở controller.
2. Permission `READ` hoặc `UPDATE`.
3. Tenant scope ở service/repository.

Không dựa riêng vào việc frontend ẩn nút.

## 12. Error contract

| HTTP | Trường hợp | Thông điệp đề xuất |
|---:|---|---|
| `400` | Request/enum/độ dài không hợp lệ | Theo lỗi validation của trường |
| `400` | Người được gán không phải VT-03 ACTIVE của tổ chức sở hữu phản ánh | `Người được chọn không đủ điều kiện xử lý phản ánh` |
| `400` | Thiếu nội dung xử lý | `Vui lòng nhập nội dung xử lý trước khi đóng phản ánh` |
| `400` | Thiếu lý do đóng | `Vui lòng nhập lý do đóng phản ánh` |
| `400` | Tem không thuộc lô phản ánh | `Mã tem không thuộc lô sản xuất của phản ánh` |
| `401` | Chưa xác thực | Theo SecurityConfig hiện hành |
| `403` | Sai vai trò hoặc thiếu permission | `Bạn không có quyền thực hiện thao tác này` |
| `404` | Không tồn tại hoặc ngoài tenant scope | `Không tìm thấy phản ánh` |
| `409` | Phản ánh đã đóng | `Phản ánh đã được đóng` |
| `409` | Đóng khi recall còn PENDING | `Phải xử lý xong yêu cầu thu hồi trước khi đóng phản ánh` |
| `409` | Tạo recall khi đã có recall PENDING | `Phản ánh đã có yêu cầu thu hồi đang chờ duyệt` |
| `409` | Nghi ngờ tem giả nhưng chưa có mã tem | `Phản ánh phải được liên kết với mã tem cụ thể` |

Ví dụ lỗi:

```json
{
  "success": false,
  "status": 409,
  "message": "Phải xử lý xong yêu cầu thu hồi trước khi đóng phản ánh",
  "path": "/api/v1/product-feedbacks/90fe86ba-596e-4709-b4d5-11f8b9445304/close",
  "timestamp": "2026-09-07T02:00:00Z"
}
```

## 13. Transaction và tính nhất quán

- Gán người: cập nhật assignee, trạng thái và audit trong một transaction.
- Phân loại tem giả: cập nhật feedback, liên kết trace code và nguồn danh sách nghi vấn trong một
  transaction.
- Tạo yêu cầu thu hồi: tạo recall, liên kết feedback, cập nhật trạng thái và audit trong một transaction.
- Khi tạo yêu cầu, backend khóa pessimistic bản ghi shipment và database dùng unique key có điều kiện
  để bảo đảm mỗi shipment chỉ có tối đa một yêu cầu `PENDING`, kể cả khi có request đồng thời.
- Đóng: kiểm tra lại điều kiện tại thời điểm ghi; không chỉ dựa trên trạng thái frontend đã tải trước đó.
- Query cập nhật phải áp dụng tenant scope để tránh lỗ hổng kiểm tra rồi cập nhật chéo tổ chức.

## 14. Ánh xạ Acceptance Criteria gốc

| AC | API/logic chịu trách nhiệm | Kết quả cần kiểm chứng |
|---|---|---|
| `TC-01` | `PUT /{id}/close` | Đóng thành công, lưu người xử lý và thời điểm |
| `TC-02` | `PUT /{id}/close` | Thiếu nội dung xử lý trả `400`, trạng thái không đổi |
| `TC-03` | `POST /{id}/recall-requests` | Tạo recall có `sourceFeedbackId` và lý do |
| `TC-04` | `PUT /{id}/close` | Recall `PENDING` trả `409`, phản ánh không đóng |
| `TC-05` | Mọi GET/PUT/POST nội bộ | `VT-02` khác tổ chức nhận `404` |

## 15. Test bổ sung bắt buộc ngoài năm AC gốc

1. `VT-01` xem được toàn nền tảng nhưng không gọi được API cập nhật.
2. `VT-02` chỉ nhận danh sách phản ánh của tổ chức hiện tại.
3. Gán thành viên `VT-03` ACTIVE cùng tổ chức thành công và chuyển `NEW -> IN_PROGRESS`.
4. Gán thành viên `VT-03` INACTIVE, khác tổ chức hoặc thành viên có vai trò khác `VT-03` bị từ chối.
5. Không cập nhật hoặc gán lại phản ánh `CLOSED`.
6. Phân loại `COUNTERFEIT_SUSPECTED` thiếu mã tem trả `409`.
7. Mã tem khác lô trả `400` và không tạo liên kết.
8. Tem được gắn từ phản ánh xuất hiện trong danh sách nghi vấn nhưng chưa tự động bị khóa.
9. Không tạo hai recall `PENDING` cho một phản ánh.
10. Người tạo recall không tự phê duyệt recall đó.
11. `processingContent` không xuất hiện trong response công khai.
12. Không tồn tại endpoint xóa phản ánh.
13. Mọi thao tác thay đổi đều ghi audit log.
14. Giao diện không hiển thị UUID mã tem; mã đã liên kết hiển thị `traceCodeValue` chỉ đọc, còn phản
    ánh chưa có mã chỉ cho lưu sau khi chọn đúng mã thuộc lô sản xuất.

## 16. Thứ tự triển khai đã thực hiện

1. Migration và permission.
2. Entity, enum, repository và tenant-scoped queries.
3. Request/response DTO tách public và internal.
4. Service xử lý assignment, processing, close.
5. Tích hợp recall request.
6. Tích hợp trace code và nguồn danh sách nghi vấn.
7. Controller và API tests.
8. Frontend API client, danh sách, bộ lọc và trang chi tiết xử lý riêng tại
   `/product-feedbacks/{feedbackId}`.
9. Integration test theo mục 14 và 15.

## 17. Các quyết định đã áp dụng khi triển khai

1. `VT-01` chỉ xem, không xử lý thay tổ chức.
2. Chỉ membership `ACTIVE` có vai trò `VT-03` của tổ chức sở hữu phản ánh được gán xử lý; `VT-02`
   vẫn là vai trò thao tác các API quản lý phản ánh.
3. `publicResponse` không bắt buộc khi đóng.
4. Giữ nguyên `QTN-22`: người tạo yêu cầu thu hồi không được tự phê duyệt. Trường hợp tổ chức chỉ có
   một quản lý cần điều phối một quản lý hợp lệ khác; không nới quyền trong story này.
