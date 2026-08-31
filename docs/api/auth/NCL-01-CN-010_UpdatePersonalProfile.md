# TÀI LIỆU THIẾT KẾ VÀ ĐẶC TẢ API (API DOCS)
## USER STORY: NCL-01-CN-010 — CẬP NHẬT HỒ SƠ CÁ NHÂN & CHỦ ĐỘNG ĐỔI MẬT KHẨU

---

## 📝 Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nhánh Git (Git Branch) | Nội dung thay đổi | Người thực hiện |
| :--- | :---: | :--- | :--- | :--- |
| **2026-08-31** | `v1.0.0` | `feature/NCL-01-CN-010_update-personal-profile` | Khởi tạo tài liệu đặc tả API cập nhật thông tin cá nhân, chủ động đổi mật khẩu và tải lên ảnh đại diện | Fullstack Development Team |

---

## 1. Tổng quan User Story & Nghiệp vụ (Business Overview)

| Thuộc tính | Chi tiết |
| :--- | :--- |
| **Mã User Story** | `NCL-01-CN-010` (Thuộc Epic `NCL-01`: Quản lý tài khoản và phân quyền đa tổ chức) |
| **Nhánh Git (Git Branch)** | `feature/NCL-01-CN-010_update-personal-profile` |
| **Tên tính năng** | Cập nhật hồ sơ cá nhân và chủ động đổi mật khẩu |
| **Actor (Tác nhân)** | Tất cả người dùng đã xác thực: Quản trị viên hệ thống (`VT-01`), Quản lý hợp tác xã (`VT-02`), Người ghi sự kiện (`VT-03`), Doanh nghiệp thu mua (`VT-04`), Cơ quan kiểm định / Quản lý ngành (`VT-05`) |
| **Mô tả Story** | *Là người dùng đã đăng nhập vào hệ thống, tôi muốn cập nhật thông tin liên hệ (số điện thoại, email, họ tên, ảnh đại diện) và chủ động thay đổi mật khẩu tài khoản của mình mà không cần yêu cầu quản trị viên can thiệp.* |
| **Quy tắc nghiệp vụ** | `QTN-01` (Cô lập dữ liệu: Người dùng chỉ được xem/sửa hồ sơ của chính mình). `QTN-33` (Quy chuẩn mật khẩu & bảo mật thông tin định danh). Việc đổi họ tên chỉ áp dụng cho các sự kiện/hoạt động mới trong tương lai, bảo toàn họ tên lịch sử trong các sự kiện đã ghi nhận trước đó (`ChainEvent`, `FarmLog`, `ActivityLog`). |
| **Tiêu chí nghiệm thu** | `TC-01`: Cập nhật số điện thoại/email/họ tên → Thông tin hồ sơ được cập nhật thành công.<br>`TC-02`: Đổi mật khẩu với mật khẩu hiện tại không chính xác → Hệ thống từ chối và báo lỗi.<br>`TC-03`: Thay đổi họ tên hiển thị → Các sự kiện cũ giữ nguyên tên cũ, sự kiện mới hiển thị tên mới. |

---

## 2. Phân tích Cơ sở Dữ liệu (Database Schema Analysis)

Chức năng cập nhật trường dữ liệu trên bảng **`users`** và ghi nhận nhật ký vào bảng **`activity_logs`**.

### 2.1. Cấu trúc bảng `users` sau khi bổ sung cột `avatar_url`

```mermaid
erDiagram
    users ||--o{ organization_users : "belongs to"
    users ||--o{ activity_logs : "creates"
    users {
        char(36) user_id PK "Khóa chính UUID"
        varchar(100) user_name UK "Tên đăng nhập"
        varchar(255) password_hash "Mật khẩu mã hóa BCrypt"
        varchar(255) full_name "Họ và tên hiển thị"
        varchar(30) phone "Số điện thoại liên hệ"
        varchar(255) email UK "Địa chỉ email (duy nhất)"
        varchar(500) avatar_url "Đường dẫn ảnh đại diện"
        varchar(50) status "Trạng thái tài khoản"
        datetime created_at "Thời gian tạo"
        datetime updated_at "Thời gian cập nhật gần nhất"

---

## 3. Đặc tả Danh sách API Endpoints

### 3.1. Lấy thông tin hồ sơ cá nhân hiện tại

- **Endpoint:** `GET /api/v1/users/profile`
- **Xác thực:** Yêu cầu `Bearer Access JWT` (`@PreAuthorize("isAuthenticated()")`)
- **Phân quyền:** Dành cho tất cả vai trò (`VT-01`, `VT-02`, `VT-03`, `VT-04`, `VT-05`)

#### Response Success (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Thành công",
  "data": {
    "id": "0198e3b1-8409-7711-b0de-7c38c035626b",
    "userId": "0198e3b1-8409-7711-b0de-7c38c035626b",
    "username": "farmer01",
    "fullName": "Nguyễn Văn Nông Dân",
    "phone": "0987654321",
    "email": "farmer01@example.com",
    "avatarUrl": "/uploads/avatars/avatar_farmer01.jpg",
    "roleCode": "VT-03",
    "roleName": "Người ghi sự kiện",
    "organizationId": "0198e3b1-7a6e-7c5b-8610-18ebad9fb6f8",
    "organizationCode": "HTX_XANH",
    "organizationName": "Hợp tác xã Nông nghiệp Xanh",
    "organizationType": "COOPERATIVE",
    "permissions": [
      "farm_log.CREATE",
      "farm_log.READ"
    ],
    "createdAt": "2026-08-01T08:00:00",
    "updatedAt": "2026-08-31T09:00:00"
  },
  "timestamp": "2026-08-31T09:30:00.000Z"
}
```

---

### 3.2. Cập nhật thông tin hồ sơ cá nhân

- **Endpoint:** `PUT /api/v1/users/profile`
- **Xác thực:** Yêu cầu `Bearer Access JWT` (`@PreAuthorize("isAuthenticated()")`)
- **Phân quyền:** Dành cho tất cả vai trò (`VT-01`, `VT-02`, `VT-03`, `VT-04`, `VT-05`)

#### Request Body
```json
{
  "fullName": "Nguyễn Văn Nông Dân Mới",
  "phone": "0912345678",
  "email": "farmer.new@example.com",
  "avatarUrl": "/uploads/avatars/0198e3b1_1725091200000.png"
}
```

#### Response Success (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Cập nhật hồ sơ cá nhân thành công",
  "data": {
    "id": "0198e3b1-8409-7711-b0de-7c38c035626b",
    "userId": "0198e3b1-8409-7711-b0de-7c38c035626b",
    "username": "farmer01",
    "fullName": "Nguyễn Văn Nông Dân Mới",
    "phone": "0912345678",
    "email": "farmer.new@example.com",
    "avatarUrl": "/uploads/avatars/0198e3b1_1725091200000.png",
    "roleCode": "VT-03",
    "roleName": "Người ghi sự kiện",
    "organizationId": "0198e3b1-7a6e-7c5b-8610-18ebad9fb6f8",
    "organizationCode": "HTX_XANH",
    "organizationName": "Hợp tác xã Nông nghiệp Xanh",
    "organizationType": "COOPERATIVE",
    "permissions": ["farm_log.CREATE", "farm_log.READ"],
    "createdAt": "2026-08-01T08:00:00",
    "updatedAt": "2026-08-31T09:35:00"
  },
  "timestamp": "2026-08-31T09:35:00.000Z"
}
```

---

### 3.3. Đổi mật khẩu chủ động

- **Endpoint:** `POST /api/v1/users/change-password`
- **Xác thực:** Yêu cầu `Bearer Access JWT` (`@PreAuthorize("isAuthenticated()")`)

#### Request Body
```json
{
  "currentPassword": "OldPassword@123",
  "newPassword": "NewSecurePassword@456",
  "confirmNewPassword": "NewSecurePassword@456"
}
```

#### Response Success (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Đổi mật khẩu thành công",
  "data": null,
  "timestamp": "2026-08-31T09:40:00.000Z"
}
```

---

### 3.4. Tải lên ảnh đại diện (Avatar Upload)

- **Endpoint:** `POST /api/v1/users/avatar`
- **Xác thực:** Yêu cầu `Bearer Access JWT` (`@PreAuthorize("isAuthenticated()")`)
- **Content-Type:** `multipart/form-data`
- **Request Param:** `file` (MultipartFile)

#### Response Success (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "message": "Tải lên ảnh đại diện thành công",
  "data": {
    "avatarUrl": "/uploads/avatars/0198e3b1-8409-7711-b0de-7c38c035626b_1725091200000.png"
  },
  "timestamp": "2026-08-31T09:45:00.000Z"
}
```

---

## 4. Nhật ký hoạt động (Audit Log / Activity Log)

| Hoạt động | `action` | `entity_type` | `description` |
| :--- | :--- | :--- | :--- |
| Cập nhật hồ sơ cá nhân | `UPDATE_PROFILE` | `USER` | `Người dùng {username} đã cập nhật thông tin hồ sơ cá nhân` |
| Đổi mật khẩu thành công | `CHANGE_PASSWORD` | `USER` | `Người dùng {username} đã đổi mật khẩu thành công` |
| Tải lên ảnh đại diện | `UPLOAD_AVATAR` | `USER` | `Người dùng {username} đã tải lên ảnh đại diện mới` |

    }
```
