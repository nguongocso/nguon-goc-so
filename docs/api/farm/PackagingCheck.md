# API Docs – Kiểm tra & Thực hiện đóng gói

**Tên nhánh:** `feature/NCL-03-CN-004-packaging-check`

---

## 1. Kiểm tra điều kiện đóng gói

### Thông tin API

| Thuộc tính   | Giá trị                                           |
| ------------ | ------------------------------------------------- |
| **Method**   | `GET`                                             |
| **Endpoint** | `/api/v1/production-lots/{lotId}/packaging-check` |
| **Quyền**    | `VT-01`, `VT-02`                                  |

### Mục đích

Kiểm tra xem lô sản xuất đã có đủ các nhật ký canh tác bắt buộc để đóng gói hay chưa.

Các loại nhật ký bắt buộc:

- `PLANTING`
- `WATERING`
- `FERTILIZING`
- `PESTICIDE`

### Request

**Path parameter:**

| Parameter | Bắt buộc | Mô tả              |
| --------- | -------- | ------------------ |
| `lotId`   | Có       | UUID của lô sản xuất |

### Response `200 OK` – Sẵn sàng đóng gói

```json
{
  "success": true,
  "data": {
    "lotId": "uuid",
    "status": "HARVESTED",
    "canPackage": true,
    "missingLogs": [],
    "message": "Lô sản xuất đã sẵn sàng để đóng gói"
  }
}
```

### Response `200 OK` – Chưa sẵn sàng (thiếu nhật ký)

```json
{
  "success": true,
  "data": {
    "lotId": "uuid",
    "status": "HARVESTED",
    "canPackage": false,
    "missingLogs": [
      "FERTILIZING",
      "PESTICIDE"
    ],
    "message": "Thiếu nhật ký canh tác bắt buộc: FERTILIZING, PESTICIDE"
  }
}
```

### Lỗi thường gặp

| Mã lỗi | Mô tả                                                              |
| ------ | ------------------------------------------------------------------ |
| `400`  | Lô sản xuất chưa sẵn sàng để đóng gói (trạng thái không phải HARVESTED) |
| `400`  | Không tìm thấy lô sản xuắt                                            |
| `403`  | Không có quyền                                                     |

---

## 2. Thực hiện đóng gói

### Thông tin API

| Thuộc tính   | Giá trị                                    |
| ------------ | ------------------------------------------ |
| **Method**   | `POST`                                     |
| **Endpoint** | `/api/v1/production-lots/{lotId}/package`  |
| **Quyền**    | `VT-02`                                    |

### Mục đích

Chuyển trạng thái lô sản xuất từ `HARVESTED` → `PACKAGED`.

Tự động kiểm tra điều kiện đóng gói trước khi chuyển trạng thái.

### Request

**Path parameter:**

| Parameter | Bắt buộc | Mô tả              |
| --------- | -------- | ------------------ |
| `lotId`   | Có       | UUID của lô sản xuất |

### Response `200 OK` – Đóng gói thành công

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "PACKAGED",
    "name": "Lô lúa vụ hè"
  }
}
```

### Lỗi thường gặp

| Mã lỗi | Mô tả                                                              |
| ------ | ------------------------------------------------------------------ |
| `400`  | Không thể đóng gói. Thiếu nhật ký canh tác bắt buộc                    |
| `400`  | Lô sản xuất chưa sẵn sàng để đóng gói (trạng thái không phải HARVESTED) |
| `400`  | Không tìm thấy lô sản xuắt                                            |
| `403`  | Không có quyền                                                     |