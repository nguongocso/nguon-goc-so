# API Quản lý danh mục vật tư đầu vào kèm thời gian cách ly (Input Material)

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
|---|---|---|---|
| 2026-08-26 | v1.0.0 | Khởi tạo tài liệu đặc tả API Thêm, Sửa, Xóa, Đổi trạng thái và Lọc danh mục vật tư đầu vào (US NCL-09-CN-010) | Senior Backend Engineer |

---

### 1. GET /api/v1/input-materials

**Description:** Lấy và tìm kiếm danh sách vật tư đầu vào theo phân trang.
- Tất cả người dùng đã xác thực (Hợp tác xã, Người ghi nhật ký, Quản trị viên) đều có thể xem để tra cứu vật tư chuẩn khi ghi nhật ký canh tác.

**Authentication:** Yêu cầu Token JWT trong Header `Authorization: Bearer <token>`.
- Vai trò được phép: Tất cả người dùng đã đăng nhập (`isAuthenticated()`).

**Request Query Parameters**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Query | keyword | String | No | Tìm kiếm tương đối theo Tên vật tư hoặc Hoạt chất | "Brightin" |
| Query | group | String | No | Lọc theo nhóm vật tư: `PESTICIDE`, `FERTILIZER`, `BIOLOGICAL`, `OTHER` | "PESTICIDE" |
| Query | isActive | Boolean | No | Lọc theo trạng thái hoạt động: `true` (đang dùng), `false` (ngừng dùng) | true |
| Query | page | Integer | No | Trang hiện tại (0-indexed), mặc định `0` | 0 |
| Query | size | Integer | No | Số bản ghi trên mỗi trang, mặc định `20` | 20 |
| Query | sortBy | String | No | Trường sắp xếp, mặc định `name` | "name" |
| Query | sortDirection | String | No | Hướng sắp xếp: `ASC` hoặc `DESC`, mặc định `ASC` | "ASC" |

**Request Example**
`GET /api/v1/input-materials?keyword=Abamectin&group=PESTICIDE&page=0&size=10`

**Response — Success (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "id": "018f9d00-0001-7000-8000-000000000001",
        "name": "Brightin 4.0EC",
        "materialGroup": "PESTICIDE",
        "materialGroupDisplayName": "Thuốc bảo vệ thực vật",
        "activeIngredient": "Abamectin",
        "unit": "ml",
        "quarantineDays": 7,
        "applyToAllCrops": true,
        "applicableCropTypes": [],
        "referenceSource": "Thông tư 10/2020/TT-BNNPTNT",
        "isActive": true,
        "createdBy": "a5c7f8a1-1234-4a22-8f12-bd12d1b74292",
        "createdAt": "2026-08-26T09:00:00.000Z",
        "updatedAt": null
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2026-08-26T09:40:00.123Z"
}
```

---

### 2. GET /api/v1/input-materials/{id}

**Description:** Lấy chi tiết thông tin vật tư đầu vào theo ID.

**Authentication:** Yêu cầu Token JWT trong Header `Authorization: Bearer <token>`.

**Request**
| Location | Field Name | Data Type | Required | Example |
|---|---|---|---|---|
| Path | id | UUID | Yes | "018f9d00-0001-7000-8000-000000000001" |

**Response — Success (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "018f9d00-0001-7000-8000-000000000001",
    "name": "Brightin 4.0EC",
    "materialGroup": "PESTICIDE",
    "materialGroupDisplayName": "Thuốc bảo vệ thực vật",
    "activeIngredient": "Abamectin",
    "unit": "ml",
    "quarantineDays": 7,
    "applyToAllCrops": true,
    "applicableCropTypes": [],
    "referenceSource": "Thông tư 10/2020/TT-BNNPTNT",
    "isActive": true,
    "createdBy": "a5c7f8a1-1234-4a22-8f12-bd12d1b74292",
    "createdAt": "2026-08-26T09:00:00.000Z",
    "updatedAt": null
  },
  "timestamp": "2026-08-26T09:40:05.123Z"
}
```

---

### 3. POST /api/v1/input-materials

**Description:** Thêm mới vật tư đầu vào vào danh mục chuẩn toàn hệ thống.
- **Ràng buộc QTN-17:** Chỉ Quản trị viên nền tảng (`VT-01`) được phép thêm.
- **Ràng buộc Thuốc BVTV (TC-02):** Nhóm `PESTICIDE` bắt buộc phải nhập `quarantineDays` (\(\ge 0\)). Nhóm `FERTILIZER` có thể để trống.
- **Ràng buộc chống trùng (TC-03):** Chặn nếu trùng cả Tên vật tư (`name`) và Hoạt chất (`activeIngredient`).

**Authentication:** Yêu cầu Token JWT (`VT-01` - Platform Admin).

**Request Body (JSON)**
| Field Name | Data Type | Required | Constraints / Rules | Example |
|---|---|---|---|---|
| name | String | Yes | @NotBlank, max 255 chars | "Brightin 4.0EC" |
| materialGroup | String | Yes | @NotNull. Enums: `PESTICIDE`, `FERTILIZER`, `BIOLOGICAL`, `OTHER` | "PESTICIDE" |
| activeIngredient | String | No | Max 255 chars | "Abamectin" |
| unit | String | Yes | @NotBlank, max 50 chars | "ml" |
| quarantineDays | Integer | Conditional | Mandatory for `PESTICIDE`. Must be integer \(\ge 0\). | 7 |
| applyToAllCrops | Boolean | No | Default `true`. | true |
| applicableCropTypeIds | Array<UUID> | No | Danh sách ID loại nông sản áp dụng khi `applyToAllCrops = false`. | [] |
| referenceSource | String | No | Nguồn quy định tham chiếu | "Thông tư 10/2020/TT-BNNPTNT" |

**Request Body Example**
```json
{
  "name": "Brightin 4.0EC",
  "materialGroup": "PESTICIDE",
  "activeIngredient": "Abamectin",
  "unit": "ml",
  "quarantineDays": 7,
  "applyToAllCrops": true,
  "referenceSource": "Thông tư 10/2020/TT-BNNPTNT"
}
```

**Response — Success (201 Created)**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "018f9d00-0001-7000-8000-000000000001",
    "name": "Brightin 4.0EC",
    "materialGroup": "PESTICIDE",
    "materialGroupDisplayName": "Thuốc bảo vệ thực vật",
    "activeIngredient": "Abamectin",
    "unit": "ml",
    "quarantineDays": 7,
    "applyToAllCrops": true,
    "applicableCropTypes": [],
    "referenceSource": "Thông tư 10/2020/TT-BNNPTNT",
    "isActive": true,
    "createdBy": "a5c7f8a1-1234-4a22-8f12-bd12d1b74292",
    "createdAt": "2026-08-26T09:41:00.123Z",
    "updatedAt": null
  },
  "timestamp": "2026-08-26T09:41:00.123Z"
}
```

**Response — Error (400 Bad Request - Trống thời gian cách ly nhóm Thuốc BVTV - TC-02)**
```json
{
  "success": false,
  "status": 400,
  "message": "Nhóm thuốc bảo vệ thực vật bắt buộc phải có thời gian cách ly",
  "path": "/api/v1/input-materials",
  "timestamp": "2026-08-26T09:41:10.123Z"
}
```

**Response — Error (400 Bad Request - Trùng tên và hoạt chất - TC-03)**
```json
{
  "success": false,
  "status": 400,
  "message": "Vật tư đã tồn tại với cùng tên và hoạt chất này",
  "path": "/api/v1/input-materials",
  "timestamp": "2026-08-26T09:41:15.123Z"
}
```

---

### 4. PUT /api/v1/input-materials/{id}

**Description:** Cập nhật thông tin chi tiết của vật tư đầu vào.

**Authentication:** Yêu cầu Token JWT (`VT-01` - Platform Admin).

**Request Body (JSON)**
```json
{
  "name": "Brightin 4.0EC (Cập nhật)",
  "materialGroup": "PESTICIDE",
  "activeIngredient": "Abamectin",
  "unit": "ml",
  "quarantineDays": 7,
  "applyToAllCrops": true,
  "referenceSource": "Thông tư 10/2020/TT-BNNPTNT",
  "isActive": true
}
```

**Response — Success (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "018f9d00-0001-7000-8000-000000000001",
    "name": "Brightin 4.0EC (Cập nhật)",
    "materialGroup": "PESTICIDE",
    "materialGroupDisplayName": "Thuốc bảo vệ thực vật",
    "activeIngredient": "Abamectin",
    "unit": "ml",
    "quarantineDays": 7,
    "applyToAllCrops": true,
    "applicableCropTypes": [],
    "referenceSource": "Thông tư 10/2020/TT-BNNPTNT",
    "isActive": true,
    "createdBy": "a5c7f8a1-1234-4a22-8f12-bd12d1b74292",
    "createdAt": "2026-08-26T09:41:00.123Z",
    "updatedAt": "2026-08-26T09:42:00.123Z"
  },
  "timestamp": "2026-08-26T09:42:00.123Z"
}
```

---

### 5. PATCH /api/v1/input-materials/{id}/status

**Description:** Chuyển đổi trạng thái kích hoạt (`isActive = true`) hoặc ngừng sử dụng (`isActive = false`) của vật tư.

**Authentication:** Yêu cầu Token JWT (`VT-01` - Platform Admin).

**Request Query Parameter**
| Location | Field Name | Data Type | Required | Example |
|---|---|---|---|---|
| Query | isActive | Boolean | Yes | false |

**Request Example**
`PATCH /api/v1/input-materials/018f9d00-0001-7000-8000-000000000001/status?isActive=false`

**Response — Success (200 OK)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "018f9d00-0001-7000-8000-000000000001",
    "name": "Brightin 4.0EC",
    "isActive": false
  },
  "timestamp": "2026-08-26T09:43:00.123Z"
}
```

---

### 6. DELETE /api/v1/input-materials/{id}

**Description:** Xóa bản ghi vật tư khỏi danh mục.
- **Ràng buộc ngoại lệ (TC-04):** Nếu vật tư đã từng được ghi nhận trong nhật ký canh tác (`farm_logs`), hệ thống **chặn hoàn toàn việc xóa** và yêu cầu chuyển sang trạng thái Ngừng sử dụng.

**Authentication:** Yêu cầu Token JWT (`VT-01` - Platform Admin).

**Response — Success (200 OK - Trường hợp vật tư chưa được dùng trong nhật ký)**
```json
{
  "success": true,
  "status": 200,
  "data": null,
  "timestamp": "2026-08-26T09:44:00.123Z"
}
```

**Response — Error (400 Bad Request - Vật tư đã xuất hiện trong nhật ký canh tác - TC-04)**
```json
{
  "success": false,
  "status": 400,
  "message": "Vật tư đã được dùng trong nhật ký canh tác. Hệ thống chặn xóa và chỉ cho phép ngừng sử dụng.",
  "path": "/api/v1/input-materials/018f9d00-0001-7000-8000-000000000001",
  "timestamp": "2026-08-26T09:44:10.123Z"
}
```

---

## Quy tắc Nghiệp vụ & Ràng buộc (Business Rules & Edge Cases)

1. **Phân quyền chỉnh sửa danh mục dùng chung (QTN-17):**
   - Danh mục vật tư đầu vào là dữ liệu dùng chung toàn hệ thống. Chỉ **Quản trị viên nền tảng** (`VT-01`) được phép gọi các API POST, PUT, PATCH, DELETE.
2. **Kiểm tra điều kiện an toàn trước thu hoạch (QTN-25 & TC-01, TC-02):**
   - Vật tư thuộc nhóm `PESTICIDE` (Thuốc BVTV) bắt buộc khai báo `quarantineDays` \(\ge 0\). Trường này làm cơ sở dữ liệu để hệ thống tính toán khoảng thời gian an toàn kể từ ngày phun thuốc gần nhất tới ngày thu hoạch của Lô sản xuất.
3. **Chống trùng tên và hoạt chất (TC-03):**
   - Hệ thống chặn lưu nếu tồn tại bản ghi có cùng `name` (không phân biệt hoa thường) và cùng `activeIngredient`.
4. **Bảo toàn dữ liệu nhật ký canh tác (TC-04):**
   - Vật tư đã xuất hiện trong nhật ký canh tác chỉ được phép ngừng sử dụng (`isActive = false`), tuyệt đối không cho xóa để bảo đảm toàn vẹn hồ sơ truy xuất nguồn gốc.
