# API: Tạo tổ chức mới

## Endpoint

| Thuộc tính | Giá trị |
|------------|----------|
| URL | `/api/v1/admin/organizations` |
| Method | `POST` |
| Mô tả | Tạo mới một tổ chức cùng tài khoản quản lý mặc định |
| Quyền | Platform Administrator (VT-01) |
| Content-Type | `application/json` |

---

# Request Body

```json
{
  "organizationName": "Công ty ABC",
  "organizationCode": "TC01",
  "organizationType": "ENTERPRISE",
  "address": "Hà Nội",
  "phone": "0224856765",
  "email": "contact@abc.com",

  "userName": "enterprise_admin12",
  "password": "Ab12345@",

  "fullName": "Trần Văn B",
  "managerPhone": "0395724804",
  "managerEmail": "admin21@abc.com"
}
```

---

# Request Parameters

## Thông tin tổ chức

| Trường | Kiểu | Bắt buộc | Validation | Mô tả |
|---------|------|----------|------------|-------|
| organizationName | String | ✔ | max 255 | Tên tổ chức |
| organizationCode | String | ✔ | `^[A-Z0-9_-]+$` | Mã tổ chức duy nhất |
| organizationType | Enum | ✔ | `COOPERATIVE`, `ENTERPRISE`, `GOVERNMENT`, `SYSTEM` | Loại tổ chức |
| address | String | ✘ | max 255 | Địa chỉ |
| phone | String | ✘ | SĐT Việt Nam | Số điện thoại tổ chức |
| email | String | ✘ | Email | Email tổ chức |

## Thông tin người quản lý

| Trường | Kiểu | Bắt buộc | Validation | Mô tả |
|---------|------|----------|------------|-------|
| fullName | String | ✔ | max 100 | Họ tên người quản lý |
| userName | String | ✔ | 4-30 ký tự | Tên đăng nhập |
| password | String | ✔ | 8-50 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt | Mật khẩu |
| managerPhone | String | ✘ | SĐT Việt Nam | Số điện thoại người quản lý |
| managerEmail | String | ✔ | Email | Email người quản lý |

---

# Enum OrganizationType

| Giá trị | Ý nghĩa |
|----------|----------|
| `COOPERATIVE` | Hợp tác xã |
| `ENTERPRISE` | Doanh nghiệp |
| `GOVERNMENT` | Cơ quan quản lý |
| `SYSTEM` | Tổ chức hệ thống |

---

# Response Success

**HTTP Status**

```http
200 OK
```

```json
{
  "success": true,
  "status": 200,
  "data": {
    "organizationID": "05c43400-8ae5-44c2-ad71-70b80dc98410",
    "organizationName": "Công ty ABC",
    "organizationCode": "TC01",
    "organizationType": "ENTERPRISE",
    "status": "ACTIVE",
    "createdAt": "2026-07-17T10:44:53.9865034"
  },
  "timestamp": "2026-07-17T03:44:54.174351300Z"
}
```

---

# Response Validation Error

**HTTP Status**

```http
400 Bad Request
```

Ví dụ:

```json
{
  "success": false,
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "organizationCode": "Mã tổ chức chỉ được chứa chữ in hoa, số, dấu gạch ngang và gạch dưới",
    "password": "Mật khẩu phải chứa ít nhất một chữ hoa, một chữ thường, một chữ số và một ký tự đặc biệt"
  },
  "path": "/api/v1/admin/organizations",
  "timestamp": "2026-07-17T10:26:05.912Z"
}
```

---

# Response Business Error

Ví dụ: Mã tổ chức đã tồn tại.

**HTTP Status**

```http
409 Conflict
```

```json
{
  "success": false,
  "status": 409,
  "message": "Organization code already exists",
  "timestamp": "2026-07-17T10:30:20.142Z"
}
```

---

# Business Flow

```text
Client
    │
    │ POST /api/v1/admin/organizations
    ▼
OrganizationController
    │
    ▼
OrganizationService
    │
    ├── Kiểm tra organizationCode
    ├── Kiểm tra username
    ├── Tạo Organization
    ├── Tạo User quản lý
    ├── Gán Role mặc định
    └── Trả về OrganizationResponse
    ▼
ApiResult
```

---

# Response Model (`ApiResult<T>`)

| Trường | Kiểu | Mô tả |
|---------|------|-------|
| success | Boolean | Trạng thái xử lý |
| status | Integer | HTTP Status |
| message | String | Thông báo lỗi (nếu có) |
| data | Object | Dữ liệu trả về |
| errors | Object | Danh sách lỗi validation |
| path | String | API gây lỗi |
| timestamp | Instant | Thời điểm trả về |