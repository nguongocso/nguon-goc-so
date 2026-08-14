# API: Tạo vùng trồng

## Endpoint

| Thuộc tính | Giá trị |
|---|---|
| Method | POST |
| URL | `/api/v1/farm-areas` |
| Authentication | Bearer JWT |
| Quyền | Quản lý tổ chức (hoặc người dùng có quyền tạo vùng trồng) |

## Mô tả

Cho phép người dùng có quyền khai báo một vùng trồng thuộc tổ chức của mình.

Thông tin tổ chức không gửi từ client, backend tự xác định từ JWT của người dùng đăng nhập.

## Header

| Tên | Bắt buộc | Giá trị |
|---|---|---|
| Authorization | Có | `Bearer <access_token>` |
| Content-Type | Có | `application/json` |

## Request Body

| Trường | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| name | String | Có | Tên vùng trồng |
| cropType | UUID | Có | ID loại cây trồng |
| latitude | Double | Có | Vĩ độ |
| longitude | Double | Có | Kinh độ |
| area | Decimal | Có | Diện tích (ha), phải lớn hơn 0 |

### Ví dụ Request

```json
{
    "name": "Vùng chè Tân Cương",
    "cropType": "cd16698a-8384-11f1-a80c-e89c251cc2ec",
    "latitude": 21.587568,
    "longitude": 105.826176,
    "area": 6.69
}
```

## Xử lý nghiệp vụ

Backend thực hiện các bước sau:

1. Xác thực JWT.
2. Lấy `organizationId` từ người dùng đang đăng nhập.
3. Kiểm tra tổ chức tồn tại.
4. Kiểm tra loại cây trồng tồn tại.
5. Chuyển `latitude` và `longitude` thành Point (SRID 4326).
6. Lưu vùng trồng.
7. Trả về thông tin vùng trồng vừa tạo.

## Response thành công

**HTTP Status: 201 Created**

```json
{
    "success": true,
    "status": 201,
    "data": {
        "id": "19001664-577e-4b3b-beda-7218066e2f23",
        "name": "Vùng chè Tân Cương",
        "organizationId": "11111111-1111-1111-1111-111111111111",
        "organizationName": "Nguồn gốc số",
        "cropTypeId": "cd16698a-8384-11f1-a80c-e89c251cc2ec",
        "cropTypeName": "Chè",
        "latitude": 21.587568,
        "longitude": 105.826176,
        "area": 6.69,
        "createdAt": "2026-07-19T22:24:53.1475245",
        "updatedAt": "2026-07-19T22:24:53.1475245"
    },
    "timestamp": "2026-07-19T15:24:53.157029200Z"
}
```

## Response lỗi

### 1. Dữ liệu không hợp lệ

**HTTP Status: 400 Bad Request**

```json
{
    "success": false,
    "status": 400,
    "message": "Dữ liệu không hợp lệ",
    "errors": {
        "area": "Diện tích phải lớn hơn 0"
    },
    "path": "/api/v1/farm-areas",
    "timestamp": "2026-07-19T15:24:11.754489600Z"
}
```

Có thể xảy ra khi:

- Tên vùng trồng để trống.
- Loại cây trồng không được chọn.
- Latitude hoặc Longitude bị thiếu.
- Diện tích nhỏ hơn hoặc bằng 0.

### 2. Không tìm thấy loại cây trồng

**HTTP Status: 404 Not Found**

```json
{
    "success": false,
    "status": 404,
    "message": "Không tìm thấy loại cây trồng"
}
```

### 3. Không tìm thấy tổ chức

**HTTP Status: 404 Not Found**

```json
{
    "success": false,
    "status": 404,
    "message": "Không tìm thấy tổ chức"
}
```

### 4. Chưa đăng nhập hoặc Token không hợp lệ

**HTTP Status: 401 Unauthorized**

```json
{
    "success": false,
    "status": 401,
    "message": "Unauthorized"
}
```

## Quy tắc nghiệp vụ

- Chỉ người dùng đã xác thực mới được tạo vùng trồng.
- `organizationId` được lấy từ JWT, client không được phép truyền lên.
- `cropType` phải tồn tại trong bảng `product_categories`.
- `latitude` và `longitude` được backend chuyển thành Point (SRID 4326) trước khi lưu.
- Diện tích phải lớn hơn 0.
- `createdAt`, `updatedAt` và `id` được hệ thống tự sinh.

## Cơ sở dữ liệu

| Cột | Nguồn dữ liệu |
|---|---|
| id | Backend sinh UUID |
| organization_id | JWT |
| crop_type | Request |
| name | Request |
| location | Backend tạo từ latitude và longitude |
| area | Request |
| created_at | Backend |
| updated_at | Backend |

## Acceptance Criteria

- Người dùng có JWT hợp lệ có thể tạo vùng trồng thuộc tổ chức của mình.
- Tên vùng trồng, loại cây trồng, vị trí và diện tích là bắt buộc.
- Diện tích phải lớn hơn 0.
- Loại cây trồng phải tồn tại trong hệ thống.
- Thông tin tổ chức được lấy từ JWT, không cho phép sửa từ phía client.
- Backend lưu vị trí dưới dạng Point (SRID 4326).
- Sau khi tạo thành công, API trả về đầy đủ thông tin vùng trồng vừa được tạo.
