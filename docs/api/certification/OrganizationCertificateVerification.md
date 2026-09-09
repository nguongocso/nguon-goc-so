# API xác thực chứng nhận của tổ chức

## 1. Thông tin chung

| Thuộc tính | Giá trị |
| --- | --- |
| Jira Story | `NCL-696` – Quản trị viên xác thực chứng nhận của tổ chức |
| Mã backlog | `NCL-09-CN-012` |
| Epic | `NCL-09` – Quản trị danh mục, chứng nhận và thành viên nâng cao |
| Tên nhánh | `feature/organization-certificate-verification` |
| Vai trò xử lý | `VT-01` – Quản trị viên nền tảng |
| Quy tắc | `QTN-34`, `QTN-13` |
| Xác thực | JWT Bearer token |
| Base path quản trị | `/api/v1/admin/certifications` |

Tài liệu này là hợp đồng cho task backend/frontend tiếp theo.

### 1.1. Vị trí và bố cục màn hình quản trị

- Màn hình là một mục riêng **Xác thực chứng nhận** trong nhóm **Quản trị hệ thống** trên sidebar.
- Route frontend: `/admin/certifications`, chỉ hiển thị và cho phép truy cập với vai trò `VT-01`.
- Bố cục desktop gồm danh sách chứng nhận ở bên trái và vùng xem tệp, thông tin đối chiếu ở bên phải; trên màn hình nhỏ hai vùng xếp dọc.
- Danh sách mặc định lọc `PENDING`; cho phép tìm kiếm và chuyển giữa `PENDING`, `VERIFIED`, `REJECTED`.
- Nút **Xác thực** và **Từ chối** chỉ xuất hiện với chứng nhận `PENDING`. Từ chối bắt buộc nhập lý do từ 10 đến 1000 ký tự.

## 2. Quy ước trạng thái

### 2.1. Trạng thái xác thực lưu trong cơ sở dữ liệu

- `PENDING`: đang chờ Quản trị viên nền tảng kiểm tra.
- `VERIFIED`: đã xác thực.
- `REJECTED`: bị từ chối.

### 2.2. Trạng thái hiệu lực tính tại thời điểm truy vấn

- `VALID`: `expiryDate >= ngày hiện tại`.
- `EXPIRED`: `expiryDate < ngày hiện tại`.

Hai trạng thái trên độc lập. Chỉ `verificationStatus = VERIFIED` và `validityStatus = VALID` có
`publicStatus = VERIFIED` với nhãn **Đã đạt chuẩn**.

## 3. Bảo mật và phân quyền

- Tất cả endpoint `/api/v1/admin/certifications/**` yêu cầu `ROLE_VT-01`.
- Người dùng chưa đăng nhập nhận `401 Unauthorized`.
- Người dùng có vai trò khác, bao gồm `VT-02`, nhận `403 Forbidden`.
- Tệp chứng nhận không được phục vụ qua `/uploads/**`. API xem tệp phải kiểm tra JWT và vai trò trước khi
  trả dữ liệu, chuẩn hóa đường dẫn lưu trữ và không để lộ đường dẫn vật lý.
- Backend phải kiểm tra lại vai trò ở service cho thao tác thay đổi trạng thái, ngoài `@PreAuthorize` ở controller.

## 4. Mô hình dữ liệu API

### 4.1. `CertificationVerificationResponse`

| Trường | Kiểu | Mô tả |
| --- | --- | --- |
| `id` | UUID | ID chứng nhận. |
| `organizationId` | UUID | ID tổ chức sở hữu. |
| `organizationName` | String | Tên tổ chức. |
| `standardId` | UUID | ID tiêu chuẩn. |
| `standardName` | String | Tên tiêu chuẩn. |
| `code` | String | Số hiệu chứng nhận. |
| `issuedBy` | String | Cơ quan cấp. |
| `issueDate` | LocalDate | Ngày cấp. |
| `expiryDate` | LocalDate | Ngày hết hạn. |
| `verificationStatus` | Enum | `PENDING`, `VERIFIED`, `REJECTED`. |
| `validityStatus` | Enum | `VALID`, `EXPIRED`, được tính động. |
| `document` | Object | Metadata tệp; không chứa đường dẫn vật lý. |
| `reviewedBy` | Object/null | ID và họ tên người ra quyết định. |
| `reviewedAt` | LocalDateTime/null | Thời điểm ra quyết định. |
| `reviewNote` | String/null | Ghi chú xác thực. |
| `rejectionReason` | String/null | Lý do từ chối. |
| `createdAt` | LocalDateTime | Thời điểm tổ chức nộp chứng nhận. |
| `updatedAt` | LocalDateTime/null | Thời điểm cập nhật cuối. |

`document` có cấu trúc:

```json
{
  "fileName": "vietgap-2026.pdf",
  "contentType": "application/pdf",
  "fileSize": 248320,
  "viewUrl": "/api/v1/admin/certifications/4aa1f56d-9bdf-41cc-93ac-08d42c1fa014/document"
}
```

## 5. Lấy danh sách chứng nhận để duyệt

```http
GET /api/v1/admin/certifications?verificationStatus=PENDING&keyword=&organizationId=&page=0&size=20&sortBy=createdAt&sortDir=desc
Authorization: Bearer <access_token>
```

### Query parameters

| Tham số | Bắt buộc | Ràng buộc |
| --- | --- | --- |
| `verificationStatus` | Không | `PENDING`, `VERIFIED`, `REJECTED`; mặc định `PENDING`. |
| `keyword` | Không | Tìm không phân biệt hoa thường theo số hiệu, cơ quan cấp, tiêu chuẩn hoặc tên tổ chức. |
| `organizationId` | Không | UUID tổ chức. |
| `page` | Không | Số nguyên `>= 0`, mặc định `0`. |
| `size` | Không | Từ `1` đến `100`, mặc định `20`. |
| `sortBy` | Không | `createdAt`, `reviewedAt`, `expiryDate`; mặc định `createdAt`. |
| `sortDir` | Không | `asc` hoặc `desc`; mặc định `desc`. |

### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "items": [
      {
        "id": "4aa1f56d-9bdf-41cc-93ac-08d42c1fa014",
        "organizationId": "13b91fd2-191e-46d9-8145-d0d456937b68",
        "organizationName": "HTX Nông sản Xanh",
        "standardId": "21c4445c-36a4-4103-bf0b-c283339d22b5",
        "standardName": "VietGAP",
        "code": "VGP-2026-00125",
        "issuedBy": "Trung tâm Chứng nhận Chất lượng",
        "issueDate": "2026-01-15",
        "expiryDate": "2027-01-14",
        "verificationStatus": "PENDING",
        "validityStatus": "VALID",
        "document": {
          "fileName": "vietgap-2026.pdf",
          "contentType": "application/pdf",
          "fileSize": 248320,
          "viewUrl": "/api/v1/admin/certifications/4aa1f56d-9bdf-41cc-93ac-08d42c1fa014/document"
        },
        "reviewedBy": null,
        "reviewedAt": null,
        "reviewNote": null,
        "rejectionReason": null,
        "createdAt": "2026-09-09T08:00:00",
        "updatedAt": null
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### Lỗi

- `400 Bad Request`: enum, UUID, phân trang hoặc trường sắp xếp không hợp lệ.
- `401 Unauthorized`: thiếu hoặc hết hạn token.
- `403 Forbidden`: không phải `VT-01`.

## 6. Lấy chi tiết chứng nhận

```http
GET /api/v1/admin/certifications/{certificationId}
Authorization: Bearer <access_token>
```

### Response

- `200 OK`: trả `CertificationVerificationResponse` đầy đủ.
- `401 Unauthorized`: thiếu hoặc hết hạn token.
- `403 Forbidden`: không phải `VT-01`.
- `404 Not Found`: không tìm thấy chứng nhận.

## 7. Xem tệp chứng nhận

```http
GET /api/v1/admin/certifications/{certificationId}/document
Authorization: Bearer <access_token>
```

### Response `200 OK`

- Body là dữ liệu nhị phân của tệp.
- `Content-Type` lấy từ metadata đã kiểm tra, chỉ cho phép `application/pdf`, `image/jpeg`, `image/png`.
- `Content-Disposition: inline; filename*=UTF-8''<encoded-file-name>`.
- `X-Content-Type-Options: nosniff`.

### Lỗi

- `401 Unauthorized`: thiếu hoặc hết hạn token.
- `403 Forbidden`: không phải `VT-01`.
- `404 Not Found`: không có chứng nhận hoặc tệp của chứng nhận.
- `410 Gone`: metadata còn nhưng tệp vật lý không còn; lỗi phải được ghi log vận hành.

## 8. Xác thực chứng nhận

```http
PUT /api/v1/admin/certifications/{certificationId}/verify
Authorization: Bearer <access_token>
Content-Type: application/json
```

### Request body

```json
{
  "reviewNote": "Số hiệu, cơ quan cấp, tiêu chuẩn và thời hạn khớp với tệp chứng nhận."
}
```

| Trường | Bắt buộc | Ràng buộc |
| --- | --- | --- |
| `reviewNote` | Không | Chuỗi sau khi trim, tối đa 1000 ký tự. |

### Điều kiện

1. Chứng nhận tồn tại và có `verificationStatus = PENDING`.
2. Có tệp chứng nhận đọc được.
3. Có đủ `code`, `issuedBy`, `standard`, `issueDate`, `expiryDate` và `issueDate <= expiryDate`.
4. Chứng nhận hết hạn vẫn được phép xác thực về tính chân thực; public API vẫn trả **Đã hết hạn**.

### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "4aa1f56d-9bdf-41cc-93ac-08d42c1fa014",
    "verificationStatus": "VERIFIED",
    "validityStatus": "VALID",
    "reviewedBy": {
      "userId": "bdf1b9a0-672a-4a3e-bdb4-51dc0183b4cf",
      "fullName": "Quản trị viên hệ thống"
    },
    "reviewedAt": "2026-09-09T09:15:00",
    "reviewNote": "Số hiệu, cơ quan cấp, tiêu chuẩn và thời hạn khớp với tệp chứng nhận.",
    "rejectionReason": null
  }
}
```

### Tác động

- Chuyển `PENDING -> VERIFIED` trong cùng transaction.
- Ghi lịch sử `VERIFY_CERTIFICATION`, `entityType = CERTIFICATION`, `entityId = certificationId`.
- Lịch sử được gắn với tổ chức sở hữu chứng nhận, dù người thực hiện là Quản trị viên nền tảng.

### Lỗi

- `400 Bad Request`: `reviewNote` quá dài hoặc dữ liệu ngày không hợp lệ.
- `401 Unauthorized`: thiếu hoặc hết hạn token.
- `403 Forbidden`: không phải `VT-01`.
- `404 Not Found`: không tìm thấy chứng nhận.
- `409 Conflict`: chứng nhận không còn ở `PENDING`, thiếu tệp hoặc thiếu dữ liệu bắt buộc để đối chiếu.

## 9. Từ chối chứng nhận

```http
PUT /api/v1/admin/certifications/{certificationId}/reject
Authorization: Bearer <access_token>
Content-Type: application/json
```

### Request body

```json
{
  "rejectionReason": "Số hiệu trên tệp không khớp với số hiệu đã khai báo."
}
```

| Trường | Bắt buộc | Ràng buộc |
| --- | --- | --- |
| `rejectionReason` | Có | Chuỗi sau khi trim, từ 10 đến 1000 ký tự. |

### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "4aa1f56d-9bdf-41cc-93ac-08d42c1fa014",
    "verificationStatus": "REJECTED",
    "validityStatus": "VALID",
    "reviewedBy": {
      "userId": "bdf1b9a0-672a-4a3e-bdb4-51dc0183b4cf",
      "fullName": "Quản trị viên hệ thống"
    },
    "reviewedAt": "2026-09-09T09:20:00",
    "reviewNote": null,
    "rejectionReason": "Số hiệu trên tệp không khớp với số hiệu đã khai báo.",
    "notifiedCount": 2
  }
}
```

### Tác động

- Chuyển `PENDING -> REJECTED` trong cùng transaction.
- Chặn gắn chứng nhận vào lô mới. Liên kết lịch sử với lô cũ không bị xóa nhưng bị loại khỏi public response.
- Gửi thông báo cho người dùng đang hoạt động của tổ chức có permission `notification:READ`.
- Ghi lịch sử `REJECT_CERTIFICATION` cùng lý do từ chối.

### Lỗi

- `400 Bad Request`: thiếu lý do hoặc lý do ngoài giới hạn.
- `401 Unauthorized`: thiếu hoặc hết hạn token.
- `403 Forbidden`: không phải `VT-01`.
- `404 Not Found`: không tìm thấy chứng nhận.
- `409 Conflict`: chứng nhận không còn ở `PENDING`.

## 10. Thay đổi hợp đồng API hiện có

### 10.1. Tạo chứng nhận

`POST /api/v1/certifications` phải khởi tạo `verificationStatus = PENDING`. Việc tải tệp thuộc luồng
`NCL-09-CN-003`; trước khi triển khai Story này, luồng đó phải cung cấp metadata tệp riêng tư để
Quản trị viên có thể xem. Không tự động xem chứng nhận mới là đã xác thực.

### 10.2. Danh sách chứng nhận có thể gắn vào lô

Endpoint lấy danh sách chọn vẫn cho phép `PENDING` còn hiệu lực, vì AC cho phép chứng nhận chưa xác thực
được gắn vào lô. Phải loại `REJECTED` và `EXPIRED`.

### 10.3. Gắn chứng nhận vào lô

`POST /api/v1/production-lots/{lotId}/certifications` trả `409 Conflict` nếu chứng nhận có
`verificationStatus = REJECTED`, ngoài kiểm tra hết hạn hiện có.

### 10.4. Tra cứu công khai

`GET /api/v1/public/trace/{code}/certifications` mở rộng từng phần tử:

| `verificationStatus` | `validityStatus` | Có trong response | `publicStatus` | `statusLabel` |
| --- | --- | --- | --- | --- |
| `PENDING` | `VALID` hoặc `EXPIRED` | Có | `PENDING_VERIFICATION` | Đang chờ xác thực |
| `VERIFIED` | `VALID` | Có | `VERIFIED` | Đã đạt chuẩn |
| `VERIFIED` | `EXPIRED` | Có | `EXPIRED` | Đã hết hạn |
| `REJECTED` | Bất kỳ | Không | Không áp dụng | Không áp dụng |

Ví dụ phần tử công khai:

```json
{
  "certificationId": "4aa1f56d-9bdf-41cc-93ac-08d42c1fa014",
  "certificationName": "VietGAP",
  "certificationCode": "VGP-2026-00125",
  "issuedBy": "Trung tâm Chứng nhận Chất lượng",
  "issueDate": "2026-01-15",
  "expiryDate": "2027-01-14",
  "verificationStatus": "VERIFIED",
  "validityStatus": "VALID",
  "publicStatus": "VERIFIED",
  "statusLabel": "Đã đạt chuẩn"
}
```

Public API không trả tệp chứng nhận, ghi chú nội bộ, lý do từ chối hoặc danh tính người duyệt.

## 11. Lỗi chuẩn

Các lỗi dùng cấu trúc `ApiResult` hiện có:

```json
{
  "success": false,
  "status": 409,
  "message": "Chứng nhận đã được xử lý trước đó.",
  "path": "/api/v1/admin/certifications/4aa1f56d-9bdf-41cc-93ac-08d42c1fa014/verify",
  "timestamp": "2026-09-09T02:15:00Z"
}
```

## 12. Yêu cầu nhất quán và đồng thời

- Cập nhật trạng thái phải dùng điều kiện trạng thái hiện tại hoặc optimistic locking. Chỉ một request được phép
  chuyển bản ghi từ `PENDING`; request đến sau nhận `409 Conflict`.
- Cập nhật trạng thái, dữ liệu người duyệt và lịch sử phải thành công hoặc rollback cùng nhau.
- Việc gửi thông báo có thể thực hiện sau commit, nhưng lỗi gửi không được hoàn tác quyết định từ chối;
  phải ghi log và trả `notifiedCount` theo số thông báo đã tạo thành công.
- Không dùng `isValid` hiện tại để suy ra đã xác thực. `isValid` chỉ phản ánh ngày hết hạn và nên được thay bằng
  tên rõ nghĩa `validityStatus` ở hợp đồng mới.

## 13. Ánh xạ Acceptance Criteria

| AC | API/Quy tắc |
| --- | --- |
| `NCL-09-CN-012-TC-01` | `PUT .../{id}/verify` và ánh xạ `VERIFIED + VALID -> Đã đạt chuẩn`. |
| `NCL-09-CN-012-TC-02` | Public API ánh xạ `PENDING -> Đang chờ xác thực`. |
| `NCL-09-CN-012-TC-03` | `PUT .../{id}/reject`, chặn attach và gửi notification. |
| `NCL-09-CN-012-TC-04` | Toàn bộ API quản trị chỉ cho `VT-01`; vai trò khác nhận `403`. |

## 14. Thành phần dự kiến khi triển khai

- Migration timestamp bổ sung trường xác thực và metadata tệp vào `certifications` hoặc bảng tệp riêng nếu
  luồng `NCL-09-CN-003` đã tạo bảng đó trước khi backend task bắt đầu.
- Enum trạng thái xác thực độc lập với `CertificationStatus` hiệu lực hiện tại.
- Request DTO xác thực/từ chối và response quản trị.
- Mở rộng `CertificationRepository`, `CertificationService`, controller và public trace mapper.
- Mở rộng `NotificationService` và activity log.
- Test service/controller cho happy path, validation, role, trạng thái cạnh tranh, attach và public mapping.
