# API Docs – Tạo lô sản xuất từ mẫu vụ trước (NCL-02-CN-007)

**Quy tắc nghiệp vụ:** QTN-01 (cách ly dữ liệu giữa các tổ chức), QTN-13 (hạn dùng chứng nhận).

---

## 1. Xem trước dữ liệu tạo lô từ mẫu

### Thông tin API

| Thuộc tính   | Giá trị                                              |
| ------------ | ---------------------------------------------------- |
| **Method**   | `GET`                                                |
| **Endpoint** | `/api/v1/production-lots/{sourceLotId}/clone-preview` |
| **Quyền**    | `VT-02` (Quản lý hợp tác xã)                         |

### Request

**Path parameter:**

- `sourceLotId`: UUID của lô sản xuất dùng làm mẫu (phải thuộc tổ chức hiện tại).

### Response `200 OK`

```json
{
  "success": true,
  "message": null,
  "data": {
    "sourceLotId": "uuid-lot-mau",
    "sourceLotName": "Lô lúa vụ hè 2025",
    "farmAreaId": "uuid-vung-trong",
    "farmAreaName": "Vùng trồng số 1",
    "productCategoryId": "uuid-nong-san",
    "productCategoryName": "Lúa",
    "name": "Lô lúa vụ hè 2025",
    "expectedQuantity": 1000,
    "expectedQuantityUnit": "kg",
    "plantingDate": "2025-05-01",
    "activeCertifications": [
      { "id": "uuid", "name": "VietGAP", "code": "VG-001", "expiryDate": "2027-01-01" }
    ],
    "skippedCertifications": [
      { "id": "uuid", "name": "GlobalGAP", "code": "GG-002", "expiryDate": "2024-01-01" }
    ],
    "warnings": [
      "Chứng nhận 'GlobalGAP' đã hết hạn nên không được sao chép sang lô mới."
    ]
  }
}
```

> Response chỉ chứa dữ liệu nền cần cho form; không expose nhật ký canh tác,
> sự kiện chuỗi, lô hàng, mã truy xuất hay lịch sử vận hành của lô mẫu.

### Lỗi thường gặp

| HTTP  | Trường hợp | Ghi chú |
| ----- | ---------- | ------- |
| `400` | Vùng trồng của lô mẫu đã ngừng sử dụng | Không cho tạo clone, không tự chuyển sang vùng trồng khác |
| `400` | Lô mẫu không thuộc tổ chức của bạn (QTN-01) | "Lô sản xuất mẫu không thuộc tổ chức của bạn" |
| `403` | Không có quyền (không phải VT-02) | `@PreAuthorize("hasRole('VT-02')")` |
| `404` | Không tìm thấy lô mẫu | "Không tìm thấy lô sản xuất mẫu" |

---

## 2. Tạo lô sản xuất mới từ mẫu

### Thông tin API

| Thuộc tính   | Giá trị                                        |
| ------------ | ---------------------------------------------- |
| **Method**   | `POST`                                         |
| **Endpoint** | `/api/v1/production-lots/{sourceLotId}/clone`  |
| **Quyền**    | `VT-02` (Quản lý hợp tác xã)                   |

### Request

**Path parameter:**

- `sourceLotId`: UUID của lô sản xuất dùng làm mẫu.

**Request body** (chỉ các trường được phép sửa của vụ mới):

```json
{
  "name": "Lô lúa vụ đông xuân 2026",
  "expectedQuantity": 1200,
  "expectedQuantityUnit": "kg",
  "plantingDate": "2026-01-10"
}
```

> - `name` (bắt buộc), `expectedQuantity` (bắt buộc, > 0),
>   `expectedQuantityUnit` (bắt buộc), `plantingDate` (không bắt buộc).
> - Không nhận `organizationId`, `createdBy`, `farmAreaId`,
>   `productCategoryId`, trạng thái hay danh sách chứng nhận — các giá trị này
>   do backend quyết định từ lô mẫu và tài khoản đăng nhập.

### Response `200 OK`

```json
{
  "success": true,
  "message": null,
  "data": {
    "lot": {
      "id": "uuid-lot-moi",
      "name": "Lô lúa vụ đông xuân 2026",
      "status": "DRAFT",
      "farmAreaName": "Vùng trồng số 1",
      "productCategoryName": "Lúa",
      "expectedQuantity": 1200,
      "expectedQuantityUnit": "kg",
      "createdByName": "Trần Văn A"
    },
    "copiedCertifications": [
      { "id": "uuid", "name": "VietGAP", "code": "VG-001", "expiryDate": "2027-01-01" }
    ],
    "skippedCertifications": [
      { "id": "uuid", "name": "GlobalGAP", "code": "GG-002", "expiryDate": "2024-01-01" }
    ],
    "warnings": [
      "Chứng nhận 'GlobalGAP' đã hết hạn nên không được sao chép sang lô mới."
    ]
  }
}
```

### Quy tắc clone

**Sao chép / kế thừa từ lô mẫu:**

- `farmArea`, `productCategory`, `name`, `expectedQuantity`,
  `expectedQuantityUnit`, `plantingDate` (ba trường sau được prefill nhưng
  user được sửa qua request).
- Các liên kết chứng nhận còn hiệu lực (`expiryDate >= hôm nay` và không bị
  từ chối xác thực — đúng quy tắc gắn chứng nhận QTN-13/QTN-34): tạo
  `ProductionLotCertification` mới với `attachedBy` là user hiện tại, không
  tái sử dụng id cũ, không sửa record của lô mẫu.

**Tuyệt đối không sao chép:**

- `id`, `organization` / `createdBy` của lô mẫu (lấy từ tài khoản đăng nhập),
  `createdAt` / `updatedAt`, `status` (lô mới luôn `DRAFT`), `actualQuantity`,
  `harvestDate`, `approvedBy` / `approvalNotes`, dữ liệu hủy (`cancellation*`),
  dữ liệu loại bỏ (`disposal*` / `handlingMeasure`), nhật ký canh tác, sự
  kiện chuỗi, lịch sử thu hoạch / sơ chế / đóng gói, lô hàng, mã truy xuất /
  QR, lịch sử phê duyệt và mọi trạng thái nghiệp vụ đã phát sinh.

### Lỗi thường gặp

| HTTP  | Trường hợp | Ghi chú |
| ----- | ---------- | ------- |
| `400` | Bỏ trống `name` / `expectedQuantity` không dương / thiếu đơn vị | Validation request |
| `400` | Vùng trồng của lô mẫu đã ngừng sử dụng | Không tạo lot dở dang |
| `400` | Lô mẫu không thuộc tổ chức của bạn (QTN-01) | Không leak dữ liệu tổ chức khác |
| `400` | Loại nông sản của lô mẫu đang ngưng hoạt động | Kế thừa validation của tạo lô |
| `403` | Không có quyền (không phải VT-02) | `@PreAuthorize("hasRole('VT-02')")` |
| `404` | Không tìm thấy lô mẫu | "Không tìm thấy lô sản xuất mẫu" |

### Ghi chú lưu vết

- Ghi nhật ký hoạt động (`activity_logs`) với `action = CREATE`,
  `entityType = ProductionLot`, `entityId` là ID lô mới, mô tả nêu rõ tạo từ
  mẫu lô cũ. Không ghi log giả trên lô mẫu.
- Toàn bộ clone nằm trong một transaction: lỗi khi lưu liên kết chứng nhận
  sẽ rollback cả lô mới.
