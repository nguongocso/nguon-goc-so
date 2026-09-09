# Phân tích nghiệp vụ xác thực chứng nhận của tổ chức

## 1. Phạm vi

| Thuộc tính | Giá trị |
| --- | --- |
| Jira Story | `NCL-696` – Quản trị viên xác thực chứng nhận của tổ chức |
| Mã backlog | `NCL-09-CN-012` |
| Task đầu tiên | `NCL-731` / `NCL-09-CN-012-CV-01` – Chốt tiêu chí xác thực giấy chứng nhận |
| Vai trò chính | `VT-01` – Quản trị viên nền tảng |
| Quy tắc | `QTN-34`, đồng thời tiếp tục áp dụng `QTN-13` |
| Phụ thuộc | `NCL-09-CN-003` (tải chứng nhận), `NCL-09-CN-006` (hiển thị công khai) |

Tài liệu này chốt đầu ra của task phân tích nghiệp vụ. Hợp đồng API tương ứng được mô tả tại
`docs/api/certification/NCL-09-CN-012_CertificateVerification.md`.

## 2. Nguồn yêu cầu đã đối chiếu

- Jira `NCL-696` có tiêu đề và bốn subtask nhưng chưa có mô tả nghiệp vụ.
- Excel dự án, dòng 93 của sheet `Product Backlog (User Stories)`, xác định mục tiêu, tiền điều kiện,
  hậu điều kiện, phụ thuộc và quy tắc của Story.
- Excel, các dòng 380–383 của sheet `Acceptance Criteria`, xác định bốn tiêu chí nghiệm thu.
- Excel, dòng 35 của sheet `Business Rules (Quy tắc)`, định nghĩa `QTN-34`.
- Mã nguồn hiện tại đã có `Certification`, quan hệ gắn chứng nhận vào lô, tra cứu công khai,
  thông báo và lịch sử hoạt động. Tuy nhiên mô hình chưa có trạng thái xác thực, dữ liệu người duyệt,
  lý do từ chối hoặc tệp chứng nhận.

## 3. Mục tiêu nghiệp vụ

Hệ thống phải ngăn việc một chứng nhận do tổ chức tự khai được trình bày với người tiêu dùng như
đã đạt chuẩn trước khi Quản trị viên nền tảng kiểm tra. Việc xác thực tính chân thực và việc xác định
hiệu lực theo ngày là hai khái niệm độc lập:

- `verificationStatus` trả lời chứng nhận đã được nền tảng kiểm tra hay chưa.
- `validityStatus` được tính theo `expiryDate` tại thời điểm truy vấn và trả lời chứng nhận còn hạn hay không.

Chỉ tổ hợp `VERIFIED` và `VALID` được hiển thị công khai là **Đã đạt chuẩn**.

## 4. Thông tin Quản trị viên phải đối chiếu

| Nhóm | Dữ liệu | Tiêu chí chấp nhận |
| --- | --- | --- |
| Tài liệu | Tệp chứng nhận | Có tệp, đọc được, không sai định dạng và không có dấu hiệu bị cắt mất nội dung chính. |
| Định danh | `code` | Số hiệu trên tệp trùng hoàn toàn với dữ liệu tổ chức khai báo. |
| Cơ quan cấp | `issuedBy` | Tên cơ quan cấp trên tệp phù hợp với dữ liệu khai báo. |
| Tiêu chuẩn | `standardId`, tên tiêu chuẩn | Tiêu chuẩn trên tệp phù hợp với tiêu chuẩn được chọn trong hệ thống. |
| Thời hạn | `issueDate`, `expiryDate` | Ngày trên tệp khớp dữ liệu khai báo và `issueDate <= expiryDate`. |
| Chủ thể | Tên tổ chức | Tổ chức được cấp trên tệp phù hợp với tổ chức sở hữu chứng nhận, nếu tệp có thông tin này. |

Chứng nhận đã hết hạn vẫn có thể được xác thực về tính chân thực. Sau khi xác thực, `QTN-13` vẫn
đánh dấu chứng nhận là hết hạn, không cho gắn mới và không hiển thị là đang đạt chuẩn.

## 5. Mô hình trạng thái đã chốt

### 5.1. Trạng thái xác thực được lưu

| Trạng thái | Ý nghĩa | Có thể gắn vào lô mới | Hiển thị công khai |
| --- | --- | --- | --- |
| `PENDING` | Mới nộp hoặc đã nộp lại, đang chờ kiểm tra | Có, nếu còn hiệu lực | Hiển thị **Đang chờ xác thực** |
| `VERIFIED` | Đã được `VT-01` xác thực | Có, nếu còn hiệu lực | Hiển thị **Đã đạt chuẩn** khi còn hiệu lực; **Đã hết hạn** khi hết hạn |
| `REJECTED` | Không đạt tiêu chí xác thực | Không | Không hiển thị trong danh sách chứng nhận công khai |

### 5.2. Chuyển trạng thái

```text
Tạo/tải chứng nhận
        |
        v
     PENDING -------- xác thực bởi VT-01 --------> VERIFIED
        |
        +---------- từ chối bởi VT-01 ----------> REJECTED
                                                       |
                                                       +-- tổ chức sửa dữ liệu hoặc nộp tệp mới --> PENDING
```

Quy tắc chuyển trạng thái:

1. Chỉ `VT-01` được xác thực hoặc từ chối.
2. Chỉ bản ghi `PENDING` được duyệt hoặc từ chối. Yêu cầu lặp hoặc cạnh tranh sau khi trạng thái đã đổi
   trả về `409 Conflict`.
3. Từ chối bắt buộc có lý do từ 10 đến 1000 ký tự; ghi chú khi xác thực là tùy chọn, tối đa 1000 ký tự.
4. Khi tổ chức thay tệp, số hiệu, cơ quan cấp, tiêu chuẩn, ngày cấp hoặc ngày hết hạn của bản ghi
   `REJECTED`, trạng thái trở lại `PENDING` và xóa quyết định duyệt cũ khỏi trạng thái hiện hành.
   Lịch sử hoạt động vẫn giữ nguyên quyết định trước đó.
5. Không cho chỉnh sửa trực tiếp nội dung cốt lõi của bản ghi `VERIFIED`. Tổ chức phải tạo lần nộp mới
   hoặc dùng luồng nộp lại được thiết kế ở task triển khai sau để tránh làm mất giá trị của quyết định xác thực.

## 6. Luồng nghiệp vụ

### 6.1. Luồng xác thực

1. `VT-01` mở danh sách chứng nhận, mặc định lọc `PENDING`, mới nộp trước.
2. Quản trị viên mở chi tiết và xem tệp qua endpoint có xác thực; không dùng đường dẫn `/uploads/**`
   công khai cho tài liệu chứng nhận.
3. Quản trị viên đối chiếu các mục tại phần 4.
4. Nếu phù hợp, hệ thống chuyển `PENDING -> VERIFIED`, lưu người duyệt, thời gian và ghi chú.
5. Hệ thống ghi lịch sử `VERIFY_CERTIFICATION` cho tổ chức sở hữu chứng nhận.
6. Trang công khai tính nhãn từ trạng thái xác thực và hạn hiệu lực.

### 6.2. Luồng từ chối

1. Quản trị viên nhập lý do cụ thể và xác nhận từ chối.
2. Hệ thống chuyển `PENDING -> REJECTED`, lưu người duyệt, thời gian và lý do.
3. Hệ thống chặn gắn chứng nhận này vào lô mới, không trả chứng nhận trong danh sách công khai.
4. Các liên kết với lô đã tồn tại được giữ để bảo toàn lịch sử nhưng không được hiển thị công khai.
5. Hệ thống gửi thông báo cho các thành viên đang hoạt động của tổ chức có quyền đọc thông báo,
   trong đó có lý do và mã chứng nhận.
6. Hệ thống ghi lịch sử `REJECT_CERTIFICATION`.

## 7. Ánh xạ tiêu chí nghiệm thu

| AC | Hành vi đã chốt | Điểm kiểm soát |
| --- | --- | --- |
| `TC-01` | Xác thực bản ghi `PENDING` làm trang công khai hiển thị **Đã đạt chuẩn** khi chứng nhận còn hạn. | Trạng thái lưu + ánh xạ public API |
| `TC-02` | Chứng nhận `PENDING` đã gắn với lô hiển thị **Đang chờ xác thực**, không hiển thị **Đã đạt chuẩn**. | Public API |
| `TC-03` | Từ chối bắt buộc có lý do; không cho gắn mới; tổ chức nhận thông báo. | Service, attach validation, notification |
| `TC-04` | `VT-02` và mọi vai trò ngoài `VT-01` nhận `403` khi gọi API duyệt/từ chối. | `@PreAuthorize` + kiểm tra phía service |

## 8. Tác động kiến trúc dự kiến cho task phát triển sau

- Mở rộng `certifications` bằng trạng thái xác thực, người/thời gian duyệt, ghi chú/lý do và thông tin tệp riêng tư.
- Không tái sử dụng `CertificationStatus` hiện tại cho trạng thái xác thực. Enum hiện tại chỉ biểu diễn
  `VALID/EXPIRED`; cần một enum độc lập để tránh trộn hai trục trạng thái.
- Mở rộng `CertificationController`/`CertificationService` hoặc bổ sung controller quản trị trong package
  `certification`; không tạo module mới.
- Chặn `REJECTED` tại `CertificationServiceImpl.attachCertification` và các truy vấn chọn chứng nhận hợp lệ.
- Mở rộng `PublicCertificationResponse` và `PublicTraceServiceImpl` theo bảng hiển thị ở phần 5.
- Tái sử dụng `ActivityLogEvent`/`@Auditable`, `NotificationService` và truy vấn người nhận theo permission.

## 9. Khoảng trống và rủi ro đã phát hiện

1. Mã nguồn hiện tại chưa lưu tệp chứng nhận, trong khi nghiệp vụ yêu cầu Quản trị viên mở tệp để đối chiếu.
   Task backend không được coi là hoàn tất nếu chỉ thêm nút duyệt mà không có tài liệu nguồn để xem.
2. `/uploads/**` hiện là tài nguyên tĩnh công khai. Tệp chứng nhận phải được phục vụ qua endpoint kiểm tra quyền,
   không trả đường dẫn vật lý và không đặt ở URL công khai đoán được.
3. `CertificationStatus` hiện chỉ có `VALID/EXPIRED`. Ghi đè enum này bằng trạng thái duyệt sẽ làm hỏng `QTN-13`.
4. Public service hiện tính nhãn chỉ từ ngày hết hạn. Nếu không sửa đồng bộ, `PENDING` vẫn bị hiển thị như còn hiệu lực.
5. Luồng chọn chứng nhận hiện lọc theo ngày hết hạn nhưng chưa lọc `REJECTED`; đây là điểm bắt buộc phải chặn ở backend.
6. Cần khóa cạnh tranh khi hai quản trị viên cùng xử lý một bản ghi `PENDING` để chỉ một quyết định thành công.

## 10. Ngoài phạm vi task đầu tiên

- Chưa triển khai migration, backend, frontend hoặc test.
- Chưa thay đổi trạng thái Jira hay nội dung issue.
- Không sửa các lỗi không liên quan trong luồng chứng nhận hiện tại.
