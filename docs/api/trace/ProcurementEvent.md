## 📘 API Docs – Ghi sự kiện thu mua

### 1. Giới thiệu
API này cho phép **Doanh nghiệp thu mua (VT-04)** ghi nhận sự kiện nhận hàng cho một lô hàng cụ thể. Sự kiện được thêm vào dòng thời gian của lô hàng, hoàn thiện chuỗi truy xuất nguồn gốc.

**Xác thực**:  
Yêu cầu **JWT token** hợp lệ với role `VT-04`.

---

### 2. Endpoint

| Thuộc tính | Giá trị |
|------------|---------|
| **Phương thức** | `POST` |
| **URL** | `/api/v1/chain-events/procurement` |
| **Quyền** | `VT-04` (Doanh nghiệp thu mua) |

---

### 3. Tham số yêu cầu (Request)

| Loại | Tên | Bắt buộc | Mô tả |
|------|-----|----------|-------|
| Header | `Authorization` | Có | `Bearer <token>` |
| Header | `Content-Type` | Có | `application/json` |

**Body (JSON)**:
```json
{
  "shipmentId": "550e8400-e29b-41d4-a716-446655440000",
  "receivedQuantity": 1000,
  "notes": "Đã nhận đủ hàng, chất lượng tốt",
  "latitude": 10.823,
  "longitude": 106.629
}
```

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| `shipmentId` | UUID | Có | ID của lô hàng cần ghi nhận |
| `receivedQuantity` | long | Có | Số lượng thực nhận (> 0) |
| `notes` | string | Không | Ghi chú thêm (tối đa 500 ký tự) |
| `latitude` | double | Không | Vĩ độ (nếu có vị trí) |
| `longitude` | double | Không | Kinh độ (nếu có vị trí) |

---

### 4. Phản hồi (Response)

#### Thành công (201 Created)
```json
{
  "success": true,
  "data": {
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "shipmentId": "550e8400-e29b-41d4-a716-446655440000",
    "eventType": "PROCUREMENT",
    "eventData": {
      "shipmentId": "550e8400-e29b-41d4-a716-446655440000",
      "shipmentName": "Lô hàng lúa vụ hè",
      "receivedQuantity": 1000,
      "notes": "Đã nhận đủ hàng, chất lượng tốt"
    },
    "latitude": 10.823,
    "longitude": 106.629,
    "recordedAt": "2026-07-25T10:00:00",
    "recordedByName": "Công ty Thu mua ABC",
    "createdAt": "2026-07-25T10:00:00"
  }
}
```

#### Mô tả trường response

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `id` | UUID | ID của sự kiện |
| `shipmentId` | UUID | ID lô hàng |
| `eventType` | string | Luôn là `PROCUREMENT` |
| `eventData` | object | Dữ liệu chi tiết của sự kiện |
| `latitude` | double | Vĩ độ (nếu có) |
| `longitude` | double | Kinh độ (nếu có) |
| `recordedAt` | datetime | Thời điểm sự kiện xảy ra |
| `recordedByName` | string | Tên người ghi nhận |
| `createdAt` | datetime | Thời điểm tạo bản ghi |

---

### 5. Mã lỗi thường gặp

| Mã | Ý nghĩa | Message |
|----|---------|---------|
| 400 | Dữ liệu không hợp lệ | `"receivedQuantity phải lớn hơn 0"` |
| 400 | Lô hàng không tồn tại | `"Không tìm thấy lô hàng."` |
| 400 | Lô hàng đã thu hồi | `"Lô hàng đã bị thu hồi, không thể ghi sự kiện."` |
| 403 | Không có quyền | `"Access Denied"` (nếu không phải VT-04) |
| 400 | Sai role | `"Chỉ Doanh nghiệp thu mua mới được ghi sự kiện này"` |

---

### 6. Ví dụ minh họa

#### cURL
```bash
curl -X POST http://localhost:8080/api/v1/chain-events/procurement \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentId": "550e8400-e29b-41d4-a716-446655440000",
    "receivedQuantity": 1000,
    "notes": "Đã nhận đủ hàng",
    "latitude": 10.823,
    "longitude": 106.629
  }'
```

#### JavaScript (Fetch)
```javascript
fetch('http://localhost:8080/api/v1/chain-events/procurement', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer <token>',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    shipmentId: '550e8400-e29b-41d4-a716-446655440000',
    receivedQuantity: 1000,
    notes: 'Đã nhận đủ hàng',
    latitude: 10.823,
    longitude: 106.629
  })
})
.then(res => res.json())
.then(data => console.log(data));
```

#### Python (requests)
```python
import requests

url = "http://localhost:8080/api/v1/chain-events/procurement"
headers = {
    "Authorization": "Bearer <token>",
    "Content-Type": "application/json"
}
payload = {
    "shipmentId": "550e8400-e29b-41d4-a716-446655440000",
    "receivedQuantity": 1000,
    "notes": "Đã nhận đủ hàng",
    "latitude": 10.823,
    "longitude": 106.629
}
response = requests.post(url, json=payload, headers=headers)
print(response.json())
```

---

### 7. Lưu ý
- **Doanh nghiệp thu mua không cần thuộc cùng tổ chức với lô hàng** – có thể ghi sự kiện cho bất kỳ lô hàng hợp lệ nào.
- Chỉ ghi được khi lô hàng **chưa bị thu hồi** (`status != RECALLED`).
- Sự kiện được lưu với loại `PROCUREMENT` trong bảng `chain_events`, có thể xem trong dòng thời gian của lô hàng.