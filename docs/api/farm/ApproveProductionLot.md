# API Docs – Duyệt lô sản xuất

**Tên nhánh:** `feature/approve-production-lot`

---

## 1. Gửi duyệt lô (`DRAFT → PENDING`)

### Thông tin API

| Thuộc tính   | Giá trị                               |
| ------------ | ------------------------------------- |
| **Method**   | `POST`                                |
| **Endpoint** | `/api/v1/production-lots/{id}/submit` |
| **Quyền**    | `VT-01`, `VT-02`                      |

### Request

**Path parameter:**

* `id`: UUID của lô sản xuất.

### Response `200 OK`

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Lô lúa vụ hè",
    "status": "PENDING"
  }
}
```

### Lỗi thường gặp

* `400` – Lô không ở trạng thái `DRAFT`.
* `400` – Thiếu thông tin bắt buộc như vùng trồng, sản lượng.
* `403` – Không có quyền.
* `404` – Không tìm thấy lô.

---

## 2. Duyệt / Từ chối lô (`PENDING → APPROVED / DRAFT`)

### Thông tin API

| Thuộc tính   | Giá trị                                |
| ------------ | -------------------------------------- |
| **Method**   | `POST`                                 |
| **Endpoint** | `/api/v1/production-lots/{id}/approve` |
| **Quyền**    | `VT-02`                                |

### Request

**Path parameter:**

* `id`: UUID của lô sản xuất.

**Request body:**

```json
{
  "approved": true,
  "reason": "Lý do từ chối (bắt buộc nếu approved = false)"
}
```

> `approved = true`: Duyệt lô sản xuất.
> `approved = false`: Từ chối lô sản xuất. Khi từ chối, `reason` là bắt buộc.

### Response `200 OK` – Khi duyệt

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "APPROVED",
    "approvedByName": "Trần Thị B"
  }
}
```

### Response `200 OK` – Khi từ chối

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "DRAFT",
    "approvalNotes": "Vui lòng bổ sung nhật ký canh tác"
  }
}
```

### Lỗi thường gặp

* `400` – Lô không ở trạng thái `PENDING`.
* `400` – Lô không thuộc tổ chức của bạn.
* `403` – Không có quyền.
* `404` – Không tìm thấy lô.

---

## 3. Lấy danh sách lô theo trạng thái

### Thông tin API

| Thuộc tính   | Giá trị                                  |
| ------------ | ---------------------------------------- |
| **Method**   | `GET`                                    |
| **Endpoint** | `/api/v1/production-lots?status=PENDING` |
| **Quyền**    | `VT-01`, `VT-02`                         |

### Query Parameter

| Parameter | Bắt buộc | Giá trị                                                           |
| --------- | -------- | ----------------------------------------------------------------- |
| `status`  | Không    | `DRAFT`, `PENDING`, `APPROVED`, `HARVESTED`, `PACKAGED`, `CLOSED` |

### Response `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Lô lúa vụ hè",
      "status": "PENDING"
    }
  ]
}
```
