## 📘 API Docs – Cấp dải mã truy xuất

### 1. Giới thiệu
API này dành cho **Quản trị viên nền tảng (VT-01)** để cấp một dải mã truy xuất cho một tổ chức (HTX, doanh nghiệp). Dải mã sẽ xác định tiền tố và số lượng mã tối đa có thể sinh sau này, giúp kiểm soát số lượng tem truy xuất theo sản lượng khai báo.

**Môi trường**:  
- Development: `http://localhost:8080`  
- Staging/Production: Theo cấu hình của từng môi trường.

**Xác thực**:  
Tất cả request đều yêu cầu **JWT token** được gửi trong header `Authorization: Bearer <token>`. Token phải có role `VT-01` (Quản trị viên nền tảng).

---

### 2. Endpoint

| Thuộc tính | Giá trị |
|------------|---------|
| **Phương thức** | `POST` |
| **URL** | `/api/v1/admin/code-ranges` |
| **Quyền** | `VT-01` |

---

### 3. Tham số yêu cầu (Request)

#### Headers
| Tên | Giá trị | Bắt buộc |
|-----|---------|----------|
| `Authorization` | `Bearer <token>` | Có |
| `Content-Type` | `application/json` | Có |

#### Body (JSON)
```json
{
  "organizationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "prefix": "893001",
  "totalLimit": 1000
}
```

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| `organizationId` | UUID | Có | ID của tổ chức được cấp mã |
| `prefix` | string | Có | Tiền tố mã (tối đa 50 ký tự), phải là duy nhất trên toàn hệ thống |
| `totalLimit` | integer | Có | Số lượng tem tối đa có thể sinh từ dải mã này (phải > 0) |

---

### 4. Phản hồi (Response)

#### Thành công (201 Created)
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "organizationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "organizationName": "Hợp tác xã Nông sản Xanh",
    "prefix": "893001",
    "totalLimit": 1000,
    "usedCount": 0,
    "createdAt": "2026-07-22T10:00:00Z"
  }
}
```

#### Lỗi thường gặp

| Mã | Ý nghĩa | Ví dụ message |
|----|---------|---------------|
| 400 | Dữ liệu không hợp lệ | `"Tiền tố mã đã tồn tại"`<br>`"Tổ chức không tồn tại"`<br>`"totalLimit phải lớn hơn 0"` |
| 401 | Chưa đăng nhập hoặc token hết hạn | `"Unauthorized"` |
| 403 | Không có quyền | `"Access Denied"` (chỉ VT-01 mới được gọi) |
| 500 | Lỗi hệ thống | `"Đã xảy ra lỗi hệ thống"` |

---

### 5. Ví dụ minh họa

#### cURL
```bash
curl -X POST http://localhost:8080/api/v1/admin/code-ranges \
  -H "Authorization: Bearer <your_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "prefix": "893001",
    "totalLimit": 1000
  }'
```

#### JavaScript (Fetch)
```javascript
fetch('http://localhost:8080/api/v1/admin/code-ranges', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer <your_jwt_token>',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    organizationId: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    prefix: '893001',
    totalLimit: 1000
  })
})
.then(res => res.json())
.then(data => console.log(data));
```

#### Python (requests)
```python
import requests

url = "http://localhost:8080/api/v1/admin/code-ranges"
headers = {
    "Authorization": "Bearer <your_jwt_token>",
    "Content-Type": "application/json"
}
payload = {
    "organizationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "prefix": "893001",
    "totalLimit": 1000
}
response = requests.post(url, json=payload, headers=headers)
print(response.json())
```

---

**Ghi chú**:  
- Dải mã sau khi tạo sẽ có `usedCount = 0`.  
- Khi sinh mã truy xuất thực tế (tem), số lượng sẽ được tăng dần và không được vượt quá `totalLimit`.