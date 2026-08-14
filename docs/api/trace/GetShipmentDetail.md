# API: Lấy chi tiết lô hàng theo ID

*Epic NCL-04: Cấp mã truy xuất và kiểm soát tem*

---

## 1. Thông tin chung

**Mục tiêu**

Cho phép người dùng (Quản lý HTX, Người ghi sự kiện, Doanh nghiệp thu mua, Cơ quan quản lý) lấy thông tin chi tiết của một lô hàng (Shipment) cụ thể dựa trên ID của lô hàng đó.

---

## 2. Endpoint

**GET /api/v1/shipments/{id}**

### Request Path Parameter

| Tham số | Kiểu dữ liệu | Bắt buộc | Mô tả | Ví dụ |
| :--- | :--- | :--- | :--- | :--- |
| `id` | UUID (String) | Có | ID của lô hàng cần tra cứu | `e2f89c6d-5b3a-4db1-9e23-7fa5293d48bb` |

### Request Example
```http
GET /api/v1/shipments/e2f89c6d-5b3a-4db1-9e23-7fa5293d48bb
Authorization: Bearer <token>
```

---

## 3. Response

### Response — Success (200 OK)

| Status Code | Ý nghĩa |
| :--- | :--- |
| `200 OK` | Lấy chi tiết lô hàng thành công |

#### Response Example (Success)
```json
{
  "code": 200,
  "message": "Thành công",
  "data": {
    "id": "e2f89c6d-5b3a-4db1-9e23-7fa5293d48bb",
    "organizationId": "11111111-1111-1111-1111-111111111111",
    "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
    "name": "Lô hàng chè Long Cốc T7/2026",
    "totalQuantity": 200,
    "packagingInfo": "Túi 500g, đóng thùng 20 túi/thùng",
    "status": "ACTIVATED",
    "createdAt": "2026-08-04T10:00:00",
    "updatedAt": "2026-08-04T10:15:00"
  }
}
```

---

### Response — Error

| Status Code | Error Code / Message | Nguyên nhân |
| :--- | :--- | :--- |
| `400 Bad Request` | `VALIDATION_ERROR` | Định dạng UUID của tham số `id` không hợp lệ |
| `401 Unauthorized` | `-` | Thiếu hoặc token xác thực hết hạn |
| `403 Forbidden` | `Forbidden` | Bị từ chối quyền truy cập (bởi Spring Security hoặc do `PermissionChecker` chặn) |
| `404 Not Found` | `Không tìm thấy lô hàng` | Lô hàng với ID yêu cầu không tồn tại trong hệ thống |

#### Error Response Example (404 Not Found)
```json
{
  "code": 404,
  "message": "Không tìm thấy lô hàng",
  "data": null
}
```

---

## 4. Điều kiện & Phân quyền (Security & Business Rules)

### 4.1 Bảo mật và Xác thực
- Người dùng phải đăng nhập thành công.
- Có vai trò hệ thống thuộc nhóm: `VT-01` (Admin), `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện), `VT-04` (Doanh nghiệp thu mua), hoặc `VT-05` (Cán bộ quản lý).
- Phải vượt qua kiểm tra phân quyền chi tiết động bằng `PermissionChecker`:
  ```java
  permissionChecker.check("shipment", "READ");
  ```

### 4.2 Giới hạn Dữ liệu (Data Boundary)
- **Tài khoản thuộc HTX (VT-02, VT-03):** Chỉ được phép xem chi tiết các lô hàng thuộc sở hữu của HTX mình.
- **Tài khoản ngoài HTX (VT-01, VT-04, VT-05):** Được phép xem chi tiết lô hàng (phục vụ giám sát, thu mua và quản trị hệ thống).

---

## 5. Các Endpoint liên quan
- `POST /api/v1/shipments`: Tạo lô hàng.
- `GET /api/v1/shipments/production-lots/{productionLotId}`: Lấy danh sách lô hàng theo lô sản xuất.
