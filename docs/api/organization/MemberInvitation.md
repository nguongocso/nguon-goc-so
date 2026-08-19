# Tài liệu API: Mời thành viên tham gia tổ chức qua thư mời

## Changelog
| Ngày | Phiên bản | Người thực hiện | Mô tả thay đổi |
| :--- | :--- | :--- | :--- |
| 2026-08-02 | v1.0.0 | AI Agent (Antigravity) | Thiết kế ban đầu cho User Story NCL-09-CN-007 |
| 2026-08-02 | v1.0.1 | AI Agent (Antigravity) | Cập nhật cấu trúc bảng Invitation thực tế theo yêu cầu của user |
| 2026-08-02 | v1.0.2 | AI Agent (Antigravity) | Thống nhất chọn Phương án 1: Lưu cột status cứng trong Database |

---

## 🎯 Kiến trúc & Chiến lược thiết kế (Architecture & Strategy Summary)
Hệ thống sử dụng cơ chế **Token-based Invitation** để mời người dùng tham gia tổ chức một cách bảo mật và có thời hạn. Khi Quản lý hợp tác xã gửi lời mời, một bản ghi thư mời được tạo kèm một Token ngẫu nhiên (UUID hoặc Secure Token) có thời hạn hiệu lực cụ thể. Thư mời được gửi trực tiếp đến liên hệ của người nhận (Email). Người nhận sử dụng token này để đăng ký tài khoản (nếu chưa có) hoặc liên kết tài khoản đã có, tự động gia nhập tổ chức với vai trò được cấu hình trước. Để tối ưu hóa hiệu năng truy vấn, phân trang và lọc danh sách thư mời, trạng thái của thư mời (`PENDING`, `ACCEPTED`, `EXPIRED`) được lưu trữ cứng dưới cột `status` trong database, kết hợp cơ chế kiểm tra thời hạn thực tế `expiry_date` khi sử dụng token nhằm đảm bảo an toàn tuyệt đối.

---

## 🗄️ Mô hình dữ liệu & Cấu trúc bảng (Data Model)

### Bảng: `invitations`

Dùng để lưu thông tin thư mời gửi đi. Mối quan hệ: `Organization (1) <--- (N) Invitation`.

| Tên trường (Database Column) | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(36)` | `PRIMARY KEY` (UUID) | ID duy nhất của thư mời. |
| `organization_id` | `VARCHAR(36)` | `NOT NULL`, `FOREIGN KEY` | Tham chiếu tới bảng `organizations(id)`. |
| `email` | `VARCHAR(255)` | `NOT NULL` | Email liên hệ của người được mời. |
| `role_id` | `INT` | `NOT NULL`, `FOREIGN KEY` | Tham chiếu tới bảng `roles(role_id)` (Vai trò sẽ gán khi đồng ý). |
| `token` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | Token ngẫu nhiên duy nhất để xác định thư mời. |
| `status` | `VARCHAR(50)` | `NOT NULL` | Trạng thái thư mời: `PENDING`, `ACCEPTED`, `EXPIRED`. |
| `expiry_date` | `TIMESTAMP` | `NOT NULL` | Thời điểm hết hạn của thư mời. |
| `used_at` | `TIMESTAMP` | `NULL` | Thời điểm người được mời đồng ý tham gia (nếu đã sử dụng). |
| `created_by` | `VARCHAR(36)` | `NOT NULL`, `FOREIGN KEY` | Tham chiếu tới bảng `users(user_id)` (Người tạo thư mời). |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Thời điểm tạo thư mời. |

#### Chiến lược lập chỉ mục (Indexes):
*   `idx_invitations_token` trên cột `token` (Unique Index) để truy xuất nhanh khi kiểm tra và xác thực thư mời.
*   `idx_invitations_email_org` trên cặp cột `(email, organization_id)` để kiểm tra nhanh thư mời trùng lặp trong cùng một tổ chức.
*   `idx_invitations_status` trên cột `status` hỗ trợ tối ưu việc lọc và thống kê thư mời trên Dashboard quản trị.

---

## 🚦 Quản lý Trạng thái & Quy tắc Nghiệp vụ

### 1. Máy trạng thái thư mời (State Machine)

| Trạng thái hiện tại | Trạng thái tiếp theo | Tác nhân kích hoạt | Điều kiện áp dụng | Tác động phụ (Side Effects) |
| :--- | :--- | :--- | :--- | :--- |
| `None` | `PENDING` | Quản lý hợp tác xã | Email được mời chưa là thành viên hoạt động (`ACTIVE`) của tổ chức. | 1. Tạo bản ghi `invitations` với `status = 'PENDING'`, `used_at = NULL`. <br>2. Gửi Email thông báo chứa link đính kèm token. <br>3. Ghi log hoạt động: `CREATE_INVITATION`. |
| `PENDING` | `ACCEPTED` | Người được mời | Token hợp lệ, `status = 'PENDING'`, `used_at IS NULL`, thời gian hiện tại `< expiry_date`. | 1. Đăng ký tài khoản (nếu chưa có). <br>2. Tạo bản ghi liên kết `OrganizationUser` với vai trò cấu hình. <br>3. Cập nhật `status = 'ACCEPTED'`, `used_at = LocalDateTime.now()`. <br>4. Ghi log hoạt động: `ACCEPT_INVITATION`. |
| `PENDING` | `EXPIRED` | Hệ thống / Tác vụ quét / Lazy Update | Thời gian hiện tại >= `expiry_date` và `status` vẫn là `PENDING`. | Cập nhật `status = 'EXPIRED'`. Từ chối các thao tác chấp nhận tiếp theo. |

### 2. Nhật ký hoạt động (Audit Trail)
Mọi thay đổi liên quan đến thư mời sẽ được ghi nhận vào bảng `activity_logs` thông qua cơ chế `ActivityLogEvent`.

| Đối tượng (Entity) | Thao tác (Action) | Mức độ | Lý do ghi nhận | Dữ liệu ghi log chi tiết (Description) |
| :--- | :--- | :--- | :--- | :--- |
| `MEMBER_INVITATION` | `CREATE` | **Required** | Quản lý HTX tạo thư mời mới | `"Người dùng [username] đã gửi thư mời tham gia tổ chức cho email [email] với vai trò [role_name]"` |
| `MEMBER_INVITATION` | `ACCEPT` | **Required** | Người dùng chấp nhận thư mời | `"Người dùng [username] chấp nhận thư mời tham gia tổ chức bằng email [email]"` |

### 3. Cấu hình thông báo (Notification)
Hệ thống sẽ gửi email tự động khi có thao tác mời thành viên.

| Sự kiện kích hoạt | Người nhận | Kênh nhận | Tóm tắt nội dung | Loại gửi |
| :--- | :--- | :--- | :--- | :--- |
| Tạo thư mời mới thành công | Người được mời | Email | *"Chào mừng bạn tham gia [Tên tổ chức]. Vui lòng nhấn vào liên kết bên dưới để xác nhận tài khoản và tham gia tổ chức của chúng tôi. Liên kết có hiệu lực đến ngày [expiry_date]."* | Thời gian thực (Real-time) |

---

## 💻 Danh sách APIs đặc tả

### 1. API: Gửi thư mời thành viên mới

*   **Endpoint:** `/api/v1/organization/invitations`
*   **Method:** `POST`
*   **Mô tả:** Quản lý hợp tác xã gửi thư mời tham gia tổ chức cho một thành viên mới qua email và chỉ định vai trò của họ.
*   **Authentication:** Yêu cầu đăng nhập. Quyền: Quản lý hợp tác xã (`ROLE_COOPERATIVE_MANAGER`).
*   **Content-Type:** `application/json`

#### Request Body
```json
{
  "email": "member.new@gmail.com",
  "roleId": 3,
  "expiryDays": 7
}
```

#### Request Parameters
| Vị trí | Tên trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Validation | Ví dụ | Mô tả |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Body | `email` | String | ✔ | `@NotBlank`, `@Email`, max 255 | `"member.new@gmail.com"` | Email nhận thư mời |
| Body | `roleId` | Integer | ✔ | `@NotNull`, `> 0` | `3` | ID của vai trò (Role ID) gán cho người dùng |
| Body | `expiryDays` | Integer | ✘ | Mặc định: 7 ngày, khoảng [1 - 30] | `7` | Số ngày thư mời có hiệu lực |

#### Response — Success (201 Created)
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "email": "member.new@gmail.com",
    "organizationId": "05c43400-8ae5-44c2-ad71-70b80dc98410",
    "organizationName": "Hợp tác xã Nông sản Sạch",
    "roleId": 3,
    "roleName": "Người ghi sự kiện",
    "status": "PENDING",
    "token": "inv_tok_e5f43a2b1c0d9e8f7a6b",
    "expiryDate": "2026-08-09T13:00:00Z",
    "createdBy": "d7b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "createdAt": "2026-08-02T13:00:00Z"
  },
  "timestamp": "2026-08-02T13:00:01.120Z"
}
```

#### Response — Error

| Status Code | Message / Error Code | Nguyên nhân | Chi tiết phản hồi lỗi mẫu |
| :--- | :--- | :--- | :--- |
| **400 Bad Request** | `"Dữ liệu không hợp lệ"` | Dữ liệu đầu vào sai định dạng hoặc thiếu thông tin | `{ "success": false, "status": 400, "message": "Dữ liệu không hợp lệ", "errors": { "email": "Phải là địa chỉ email hợp lệ" }, "path": "/api/v1/organization/invitations", "timestamp": "..." }` |
| **403 Forbidden** | `"Bạn không có quyền thực hiện chức năng này"` | Tài khoản đăng nhập không phải là Quản lý hợp tác xã | `{ "success": false, "status": 403, "message": "Bạn không có quyền thực hiện chức năng này", "path": "/api/v1/organization/invitations", "timestamp": "..." }` |
| **404 Not Found** | `"Vai trò không tồn tại trong hệ thống"` | `roleId` gửi lên không tồn tại | `{ "success": false, "status": 404, "message": "Vai trò không tồn tại trong hệ thống", "path": "/api/v1/organization/invitations", "timestamp": "..." }` |
| **409 Conflict** | `"Người dùng có email này đã là thành viên của tổ chức"` | Người được mời đã là thành viên trong tổ chức này | `{ "success": false, "status": 409, "message": "Người dùng có email này đã là thành viên của tổ chức", "path": "/api/v1/organization/invitations", "timestamp": "..." }` |

---

### 2. API: Lấy thông tin chi tiết thư mời từ Token

*   **Endpoint:** `/api/v1/public/organization/invitations/{token}`
*   **Method:** `GET`
*   **Mô tả:** Lấy thông tin cơ bản của thư mời (tên tổ chức, email người nhận, vai trò) trước khi người dùng đồng ý tham gia.
*   **Authentication:** Không yêu cầu đăng nhập (Public API).
*   **Content-Type:** `application/json`

#### Request Parameters
| Vị trí | Tên trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Validation | Ví dụ | Mô tả |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Path | `token` | String | ✔ | `@NotBlank` | `"inv_tok_e5f43a2b1c0d9e8f7a6b"` | Token thư mời đính kèm trên Link |

#### Response — Success (200 OK)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "email": "member.new@gmail.com",
    "organizationName": "Hợp tác xã Nông sản Sạch",
    "roleName": "Người ghi sự kiện",
    "status": "PENDING",
    "expiryDate": "2026-08-09T13:00:00Z"
  },
  "timestamp": "2026-08-02T13:10:00.540Z"
}
```

#### Response — Error

| Status Code | Message / Error Code | Nguyên nhân | Chi tiết phản hồi lỗi mẫu |
| :--- | :--- | :--- | :--- |
| **404 Not Found** | `"Thư mời không tồn tại hoặc mã token không hợp lệ"` | Token gửi lên không có trong database | `{ "success": false, "status": 404, "message": "Thư mời không tồn tại hoặc mã token không hợp lệ", "path": "/api/v1/public/organization/invitations/invalid-token", "timestamp": "..." }` |
| **400 Bad Request** | `"Thư mời đã quá hạn hoặc đã được sử dụng"` | Thư mời có trạng thái không phải `PENDING` hoặc quá hạn sử dụng | `{ "success": false, "status": 400, "message": "Thư mời đã quá hạn hoặc đã được sử dụng", "path": "/api/v1/public/organization/invitations/exp-token", "timestamp": "..." }` |

---

### 3. API: Chấp nhận thư mời và tạo tài khoản mới

*   **Endpoint:** `/api/v1/public/organization/invitations/{token}/accept`
*   **Method:** `POST`
*   **Mô tả:** Đăng ký tài khoản cho thành viên mới được mời và tự động liên kết thành viên đó vào tổ chức với vai trò được thiết lập sẵn trong thư mời.
*   **Authentication:** Không yêu cầu đăng nhập (Public API).
*   **Content-Type:** `application/json`

#### Request Parameters
| Vị trí | Tên trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Validation | Ví dụ | Mô tả |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Path | `token` | String | ✔ | `@NotBlank` | `"inv_tok_e5f43a2b1c0d9e8f7a6b"` | Token thư mời |
| Body | `userName` | String | ✔ | 4-30 ký tự, `^[a-zA-Z0-9_-]+$` | `"nguyenvancontrib"` | Tên đăng nhập mới của người dùng |
| Body | `password` | String | ✔ | 8-50 ký tự, chứa chữ hoa, chữ thường, số, ký tự đặc biệt | `"P@ssword123"` | Mật khẩu tài khoản |
| Body | `fullName` | String | ✔ | Max 100 ký tự | `"Nguyễn Văn Contrib"` | Họ và tên của thành viên |
| Body | `phone` | String | ✘ | Số điện thoại Việt Nam hợp lệ | `"0987654321"` | Số điện thoại liên hệ |

#### Request Body
```json
{
  "userName": "nguyenvancontrib",
  "password": "SecurePassword123!",
  "fullName": "Nguyễn Văn Contrib",
  "phone": "0987654321"
}
```

#### Response — Success (200 OK)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "userId": "d7b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "userName": "nguyenvancontrib",
    "fullName": "Nguyễn Văn Contrib",
    "organizationId": "05c43400-8ae5-44c2-ad71-70b80dc98410",
    "organizationName": "Hợp tác xã Nông sản Sạch",
    "roleCode": "ROLE_CONTRIBUTOR"
  },
  "timestamp": "2026-08-02T13:15:22.091Z"
}
```

#### Response — Error

| Status Code | Message / Error Code | Nguyên nhân | Chi tiết phản hồi lỗi mẫu |
| :--- | :--- | :--- | :--- |
| **400 Bad Request** | `"Dữ liệu không hợp lệ"` | Lỗi validation trường thông tin tài khoản | `{ "success": false, "status": 400, "message": "Dữ liệu không hợp lệ", "errors": { "password": "Mật khẩu phải chứa ít nhất 8 ký tự..." }, "path": "...", "timestamp": "..." }` |
| **400 Bad Request** | `"Thư mời đã quá hạn hoặc đã được sử dụng"` | Thư mời hết hạn hoặc đã được sử dụng trước đó | `{ "success": false, "status": 400, "message": "Thư mời đã quá hạn hoặc đã được sử dụng", "path": "...", "timestamp": "..." }` |
| **409 Conflict** | `"Tên đăng nhập đã tồn tại trong hệ thống"` | Tên đăng nhập `userName` bị trùng lặp | `{ "success": false, "status": 409, "message": "Tên đăng nhập đã tồn tại trong hệ thống", "path": "...", "timestamp": "..." }` |

---

## 🔄 Luồng Nghiệp Vụ (Business Flow)

### 1. Luồng Tạo Thư Mời (Quản lý gửi thư mời)
```text
Quản lý HTX (Client)
    │
    │ POST /api/v1/organization/invitations
    ▼
InvitationController
    │
    ▼
InvitationService
    │
    ├── 1. Kiểm tra Email xem đã có tài khoản và là thành viên ACTIVE trong tổ chức chưa (Nếu rồi -> Báo lỗi 409)
    ├── 2. Xác thực RoleId được mời có tồn tại trong hệ thống không (Nếu không -> Báo lỗi 404)
    ├── 3. Sinh mã Token ngẫu nhiên (UUID) + Xác định ngày hết hạn (createdAt + expiryDays)
    ├── 4. Lưu bản ghi Invitation với status = 'PENDING', used_at = NULL vào Database
    ├── 5. Kích hoạt sự kiện gửi Email mời thành viên (Email chứa link và Token)
    ├── 6. Gửi ActivityLogEvent: Ghi log tạo thư mời thành công
    ▼
ApiResult<InvitationResponse>
```

### 2. Luồng Chấp Nhận Thư Mời (Người nhận tham gia tổ chức)
```text
Người được mời (Client)
    │
    │ POST /api/v1/public/organization/invitations/{token}/accept
    ▼
InvitationController
    │
    ▼
InvitationService
    │
    ├── 1. Lấy thông tin thư mời bằng token (Nếu không tìm thấy -> Báo lỗi 404)
    ├── 2. Kiểm tra nếu expiry_date <= NOW và status == 'PENDING':
    │      ├── Cập nhật status = 'EXPIRED' (Lazy Update)
    │      └── Trả về lỗi 400 (Thư mời đã hết hạn)
    ├── 3. Kiểm tra status == 'PENDING' và used_at IS NULL (Nếu sai -> Báo lỗi 400)
    ├── 4. Kiểm tra userName đăng ký mới có bị trùng không (Nếu trùng -> Báo lỗi 409)
    ├── 5. Tạo User mới và lưu vào bảng `users` (password được băm qua BCrypt)
    ├── 6. Tạo OrganizationUser liên kết User mới với Organization + Gán vai trò
    ├── 7. Cập nhật status = 'ACCEPTED', used_at = LocalDateTime.now() cho bản ghi thư mời
    ├── 8. Gửi ActivityLogEvent: Ghi log người dùng chấp nhận thư mời tham gia tổ chức
    ▼
ApiResult<AcceptInvitationResponse>
```

---

## ⚡ Các trường hợp đặc biệt & Bảo mật (Performance & Security Edge Cases)

1.  **Chống tấn công Brute Force Token:** Do API chấp nhận thư mời là API Public, đối tượng tấn công có thể spam đoán mã token. Cần áp dụng **Rate Limiting** (giới hạn số lượt gọi từ 1 IP) trên các endpoint public `/api/v1/public/organization/invitations/**`.
2.  **Xử lý bất đồng bộ (Async Notifications & Audit logs):** Việc gửi Email và lưu `ActivityLog` phải được thực hiện bất đồng bộ (`@Async`) qua Spring Event Publisher để giảm thời gian phản hồi (Response Latency) cho Client và tránh làm tắc nghẽn DB Transaction.
3.  **Xử lý tranh chấp luồng (Concurrency Race Condition):** Có thể xảy ra trường hợp người dùng nhấp đúp vào nút đăng ký, khiến API `/accept` bị gọi song song 2 lần cho cùng 1 token.
    *   **Giải pháp:** Cần sử dụng cơ chế transaction cô lập và khoá hoặc cập nhật trạng thái thư mời một cách nguyên tử (Atomic Update):
        ```sql
        UPDATE invitations 
        SET status = 'ACCEPTED', used_at = :usedAt 
        WHERE token = :token AND status = 'PENDING' AND expiry_date > :now
        ```
        Nếu số lượng dòng cập nhật bằng 0, chứng tỏ token đã bị sử dụng hoặc quá hạn bởi luồng song song khác -> Rollback transaction và báo lỗi ngay.
4.  **Bảo vệ Dữ liệu Nhạy cảm:** Khi ghi nhật ký `activity_logs`, tuyệt đối không ghi nhận các thông tin bảo mật như mật khẩu của người dùng mới (`password` phải bị loại bỏ trước khi truyền vào payload nhật ký).
