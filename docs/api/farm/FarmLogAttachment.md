# API Docs – Quản lý chứng từ nhật ký canh tác

**Tên nhánh:** `feature/farm-logs-attachment`

---

## 1. Upload chứng từ cho nhật ký canh tác

### Thông tin API

| Thuộc tính       | Giá trị                                 |
| ---------------- | --------------------------------------- |
| **Method**       | `POST`                                  |
| **Endpoint**     | `/api/v1/farm-logs/{logId}/attachments` |
| **Quyền**        | `VT-01`, `VT-02`, `VT-03`               |
| **Content-Type** | `multipart/form-data`                   |

### Request

**Path parameter:**

| Parameter | Kiểu dữ liệu | Bắt buộc | Mô tả                   |
| --------- | ------------ | -------- | ----------------------- |
| `logId`   | `UUID`       | Có       | ID của nhật ký canh tác |

**Form-data:**

| Field         | Kiểu dữ liệu | Bắt buộc | Mô tả                                                         |
| ------------- | ------------ | -------- | ------------------------------------------------------------- |
| `file`        | `File`       | Có       | File ảnh hoặc tài liệu. Định dạng hỗ trợ: `JPG`, `PNG`, `PDF` |
| `description` | `String`     | Không    | Mô tả nội dung của chứng từ                                   |

### Response `201 Created`

```json id="1g2o9j"
{
  "success": true,
  "data": {
    "id": "uuid",
    "farmLogId": "uuid",
    "fileName": "image_2026-07-21.jpg",
    "fileSize": 2048576,
    "fileType": "image/jpeg",
    "fileUrl": "/uploads/farm-logs/xxx/yyy.jpg",
    "description": "Ảnh bón phân lót",
    "uploadedBy": "Nguyễn Văn A",
    "uploadedAt": "2026-07-21T10:00:00"
  }
}
```

### Lỗi thường gặp

* `400` – File vượt quá dung lượng cho phép, tối đa `5MB`.
* `400` – Loại file không được hỗ trợ. Chỉ hỗ trợ `JPG`, `PNG`, `PDF`.
* `404` – Không tìm thấy nhật ký canh tác.
* `403` – Nhật ký canh tác không thuộc tổ chức của người dùng hoặc người dùng không có quyền truy cập.

---

## 2. Lấy danh sách chứng từ của nhật ký canh tác

### Thông tin API

| Thuộc tính    | Giá trị                                                                  |
| ------------- | ------------------------------------------------------------------------ |
| **Method**    | `GET`                                                                    |
| **Endpoint**  | `/api/v1/farm-logs/{logId}/attachments`                                  |
| **Quyền**     | `VT-01`, `VT-02`, `VT-03`                                                |
| **Điều kiện** | Chỉ được truy cập nhật ký thuộc lô được phân công hoặc phạm vi được phép |

### Request

**Path parameter:**

| Parameter | Kiểu dữ liệu | Bắt buộc | Mô tả                   |
| --------- | ------------ | -------- | ----------------------- |
| `logId`   | `UUID`       | Có       | ID của nhật ký canh tác |

### Response `200 OK`

```json id="e2g7pz"
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "fileName": "image_2026-07-21.jpg",
      "fileSize": 2048576,
      "fileType": "image/jpeg",
      "fileUrl": "/uploads/farm-logs/xxx/yyy.jpg",
      "description": "Ảnh bón phân lót",
      "uploadedBy": "Nguyễn Văn A",
      "uploadedAt": "2026-07-21T10:00:00"
    }
  ]
}
```

### Lỗi thường gặp

* `403` – Người dùng không có quyền truy cập nhật ký canh tác.
* `404` – Không tìm thấy nhật ký canh tác.

---

## 3. Xóa chứng từ

> **Tùy chọn:** API này chỉ được triển khai nếu nghiệp vụ cho phép người dùng xóa chứng từ đã upload.

### Thông tin API

| Thuộc tính    | Giá trị                                        |
| ------------- | ---------------------------------------------- |
| **Method**    | `DELETE`                                       |
| **Endpoint**  | `/api/v1/farm-logs/attachments/{attachmentId}` |
| **Quyền**     | `VT-01`, `VT-02`, `VT-03`                      |
| **Điều kiện** | Chỉ người dùng đã upload chứng từ mới được xóa |

### Request

**Path parameter:**

| Parameter      | Kiểu dữ liệu | Bắt buộc | Mô tả                   |
| -------------- | ------------ | -------- | ----------------------- |
| `attachmentId` | `UUID`       | Có       | ID của chứng từ cần xóa |

### Response `204 No Content`

Không trả về response body.

### Lỗi thường gặp

* `403` – Người dùng không phải người upload chứng từ hoặc không có quyền xóa.
* `404` – Không tìm thấy chứng từ.
