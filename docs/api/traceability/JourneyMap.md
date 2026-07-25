### API Docs

#### 1. Lấy danh sách điểm hành trình của một lô hàng

| Thuộc tính | Giá trị |
|------------|---------|
| **Phương thức** | `GET` |
| **URL** | `/api/v1/public/shipments/{shipmentId}/journey` |
| **Quyền** | Công khai (không cần token) |
| **Mô tả** | Trả về danh sách các điểm sự kiện (có tọa độ) của lô hàng, sắp xếp theo thời gian. |

#### 2. Request

| Loại | Tên | Bắt buộc | Mô tả |
|------|-----|----------|-------|
| Path | `shipmentId` | Có | UUID của lô hàng |

#### 3. Response

**Thành công (200 OK)**:
```json
{
  "success": true,
  "data": {
    "shipmentId": "550e8400-e29b-41d4-a716-446655440000",
    "shipmentName": "Lô hàng lúa vụ hè",
    "totalEvents": 3,
    "points": [
      {
        "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "eventType": "HARVEST",
        "eventName": "Thu hoạch",
        "latitude": 10.823,
        "longitude": 106.629,
        "recordedAt": "2026-07-25T08:00:00",
        "description": "Thu hoạch 1000kg lúa",
        "order": 1
      },
      {
        "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d480",
        "eventType": "TRANSPORT",
        "eventName": "Vận chuyển",
        "latitude": 10.850,
        "longitude": 106.650,
        "recordedAt": "2026-07-25T10:00:00",
        "description": "Vận chuyển từ HTX Xanh đến kho ABC",
        "order": 2
      },
      {
        "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d481",
        "eventType": "PROCUREMENT",
        "eventName": "Thu mua",
        "latitude": 10.870,
        "longitude": 106.680,
        "recordedAt": "2026-07-25T14:00:00",
        "description": "Doanh nghiệp ABC nhận 1000kg",
        "order": 3
      }
    ]
  }
}
```

**Mô tả trường**:

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `shipmentId` | UUID | ID lô hàng |
| `shipmentName` | string | Tên lô hàng |
| `totalEvents` | integer | Tổng số điểm có tọa độ |
| `points` | array | Danh sách điểm hành trình |
| `eventId` | UUID | ID sự kiện |
| `eventType` | string | Loại sự kiện: `HARVEST`, `TRANSPORT`, `PACKAGING`, `PROCUREMENT` |
| `eventName` | string | Tên hiển thị của sự kiện (đã map) |
| `latitude` | double | Vĩ độ |
| `longitude` | double | Kinh độ |
| `recordedAt` | datetime | Thời điểm xảy ra |
| `description` | string | Mô tả ngắn (lấy từ `event_data`) |
| `order` | integer | Thứ tự hiển thị (1, 2, 3...) |

#### Mã lỗi

| Mã | Ý nghĩa | Message |
|----|---------|---------|
| 404 | Không tìm thấy lô hàng | `"Không tìm thấy lô hàng"` |
| 200 (empty) | Không có sự kiện nào có tọa độ | `points: []` |