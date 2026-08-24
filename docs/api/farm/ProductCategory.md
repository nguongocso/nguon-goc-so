# API Quản lý danh mục loại nông sản (Product Category)

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
|---|---|---|---|
| 2026-07-30 | v1.0.0 | Khởi tạo tài liệu đặc tả các API thêm, sửa, ẩn/hiện và lấy danh sách loại nông sản | AI Agent |
| 2026-08-24 | v1.1.0 | Bổ sung 4 trường ngưỡng bảo quản `tempMin`, `tempMax`, `humidityMin`, `humidityMax` vào POST/PUT và các response (fix lỗi không cập nhật được ngưỡng bảo quản) | AI Agent |

---

### GET /api/v1/product-categories

**Description:** Lấy danh sách các loại nông sản trong hệ thống.
- Đối với Người dùng thông thường (Hợp tác xã/Tổ chức): Chỉ trả về danh sách các loại nông sản đang hoạt động (`isActive = true`), sắp xếp theo tên từ A-Z.
- Đối với Quản trị viên (`VT-01`): Mặc định trả về toàn bộ các loại nông sản (cả hoạt động và bị ẩn), hỗ trợ lọc theo trạng thái (`isActive`), tìm kiếm theo tên (`name`), và lọc theo nhóm hàng (`categoryGroup`).

**Authentication:** Yêu cầu Token JWT trong Header `Authorization: Bearer <token>`.
- Vai trò được phép: Tất cả các người dùng đã xác thực (nhưng có sự khác biệt về dữ liệu trả về giữa Quản trị viên và các vai trò khác).

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Query | name | String | No | Tìm kiếm tương đối theo tên loại nông sản | "xoài" |
| Query | categoryGroup | String | No | Lọc theo nhóm hàng nông sản | "Cây ăn quả" |
| Query | isActive | Boolean | No | Chỉ cho phép Admin (`VT-01`) sử dụng tham số lọc trạng thái ẩn/hiện. Với người dùng thông thường, hệ thống luôn mặc định lọc `isActive = true`. | true |

**Request Example**
`GET /api/v1/product-categories?name=xoài&categoryGroup=Cây%20ăn%20quả`

*(Lưu ý: Đối với phương thức GET, không gửi kèm Request Body)*

**Response — Success**
| Status Code | When it occurs |
|---|---|
| 200 OK | Lấy danh sách loại nông sản thành công. |

**Response Example (Success - Người dùng thông thường / Hợp tác xã)**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "bf57bca1-628d-4a11-8f92-bd12d1b74291",
      "name": "Xoài Cát Chu",
      "group": "Cây ăn quả",
      "description": "Xoài cát chu chính hiệu",
      "isActive": true,
      "tempMin": 10.0,
      "tempMax": 30.0,
      "humidityMin": 60.0,
      "humidityMax": 90.0
    }
  ],
  "timestamp": "2026-07-30T10:00:00.123Z"
}
```

**Response Example (Success - Quản trị viên với đầy đủ danh sách kể cả đã ẩn)**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "bf57bca1-628d-4a11-8f92-bd12d1b74291",
      "name": "Xoài Cát Chu",
      "group": "Cây ăn quả",
      "description": "Xoài cát chu chính hiệu",
      "isActive": true,
      "tempMin": 10.0,
      "tempMax": 30.0,
      "humidityMin": 60.0,
      "humidityMax": 90.0
    },
    {
      "id": "a5c7f8a1-1234-4a22-8f12-bd12d1b74292",
      "name": "Cây Cỏ Ngọt",
      "group": "Cây công nghiệp",
      "description": "Cây cỏ ngọt bị ẩn do tạm dừng sản xuất",
      "isActive": false,
      "tempMin": null,
      "tempMax": null,
      "humidityMin": null,
      "humidityMax": null
    }
  ],
  "timestamp": "2026-07-30T10:00:05.123Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|---|---|---|
| 401 Unauthorized | - | Không cung cấp token xác thực hoặc token đã hết hạn. |
| 403 Forbidden | - | Người dùng không phải Admin (`VT-01`) nhưng cố ý lọc danh sách theo `isActive = false`. |
| 500 Internal Error | - | Lỗi hệ thống khi truy cập cơ sở dữ liệu. |

**Error Response Example (401 Unauthorized)**
```json
{
  "success": false,
  "status": 401,
  "message": "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn",
  "path": "/api/v1/product-categories",
  "timestamp": "2026-07-30T09:16:05.123Z"
}
```

---

### POST /api/v1/product-categories

**Description:** Thêm mới một loại nông sản vào danh mục dùng chung của toàn hệ thống.

**Authentication:** Yêu cầu Token JWT trong Header `Authorization: Bearer <token>`.
- Vai trò được phép: Quản trị viên nền tảng (`VT-01` - ADMIN).

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Body | name | String | Yes | @NotBlank, tối đa 255 ký tự. Không được trùng tên (không phân biệt hoa thường) với loại nông sản đã tồn tại. | "Xoài Cát Chu" |
| Body | group | String | Yes | @NotBlank, tối đa 100 ký tự. Tên nhóm hàng dùng để gom nhóm nông sản. | "Cây ăn quả" |
| Body | description | String | No | Tối đa 1000 ký tự. Mô tả thông tin chi tiết về loại nông sản. | "Nông sản sạch chuẩn VietGAP" |
| Body | tempMin | Number | No | Ngưỡng nhiệt độ bảo quản tối thiểu (°C), từ -99.9 đến 999.9. Bỏ trống nếu chưa có ngưỡng. | 10.0 |
| Body | tempMax | Number | No | Ngưỡng nhiệt độ bảo quản tối đa (°C), từ -99.9 đến 999.9, phải ≥ tempMin. | 30.0 |
| Body | humidityMin | Number | No | Ngưỡng độ ẩm bảo quản tối thiểu (%), từ 0 đến 100. | 60.0 |
| Body | humidityMax | Number | No | Ngưỡng độ ẩm bảo quản tối đa (%), từ 0 đến 100, phải ≥ humidityMin. | 90.0 |

**Request Example (JSON)**
```json
{
  "name": "Xoài Cát Chu",
  "group": "Cây ăn quả",
  "description": "Nông sản sạch chuẩn VietGAP",
  "tempMin": 10.0,
  "tempMax": 30.0,
  "humidityMin": 60.0,
  "humidityMax": 90.0
}
```

**Response — Success**
| Status Code | When it occurs |
|---|---|
| 201 Created | Thêm mới loại nông sản thành công, dữ liệu được ghi nhận vào hệ thống. |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "bf57bca1-628d-4a11-8f92-bd12d1b74291",
    "name": "Xoài Cát Chu",
    "group": "Cây ăn quả",
    "description": "Nông sản sạch chuẩn VietGAP",
    "isActive": true,
    "tempMin": 10.0,
    "tempMax": 30.0,
    "humidityMin": 60.0,
    "humidityMax": 90.0
  },
  "timestamp": "2026-07-30T10:05:00.123Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|---|---|---|
| 400 Bad Request | VALIDATION_ERROR | Dữ liệu đầu vào không hợp lệ (ví dụ: name trống, group trống, hoặc vượt quá số ký tự quy định). |
| 401 Unauthorized | - | Không cung cấp token xác thực hoặc token đã hết hạn. |
| 403 Forbidden | - | Người dùng không có vai trò Quản trị viên (`VT-01`). |
| 409 Conflict | DUPLICATE_RESOURCE | Tên loại nông sản đã tồn tại trong hệ thống (không phân biệt hoa thường). |
| 500 Internal Error | - | Lỗi hệ thống khi truy vấn cơ sở dữ liệu. |

**Error Response Example (400 Bad Request - Thiếu thông tin bắt buộc)**
```json
{
  "success": false,
  "status": 400,
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "name": "Tên loại nông sản không được để trống",
    "group": "Nhóm hàng không được để trống"
  },
  "path": "/api/v1/product-categories",
  "timestamp": "2026-07-30T10:06:00.123Z"
}
```

**Error Response Example (409 Conflict - Trùng tên loại nông sản)**
```json
{
  "success": false,
  "status": 409,
  "message": "Loại nông sản với tên 'Xoài Cát Chu' đã tồn tại trong danh mục",
  "path": "/api/v1/product-categories",
  "timestamp": "2026-07-30T10:07:00.123Z"
}
```

---

### PUT /api/v1/product-categories/{id}

**Description:** Cập nhật thông tin chi tiết của loại nông sản, hoặc ẩn/hiện loại nông sản đó bằng thuộc tính `isActive`.

**Authentication:** Yêu cầu Token JWT trong Header `Authorization: Bearer <token>`.
- Vai trò được phép: Quản trị viên nền tảng (`VT-01` - ADMIN).

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Path | id | UUID | Yes | ID định danh của loại nông sản cần sửa | "bf57bca1-628d-4a11-8f92-bd12d1b74291" |
| Body | name | String | Yes | @NotBlank, tối đa 255 ký tự. Không được trùng tên với các loại nông sản khác (không phân biệt hoa thường). | "Xoài Cát Chu Cao Lãnh" |
| Body | group | String | Yes | @NotBlank, tối đa 100 ký tự. | "Cây ăn quả" |
| Body | description | String | No | Tối đa 1000 ký tự. | "Xoài cát chu xuất xứ Cao Lãnh" |
| Body | isActive | Boolean | Yes | @NotNull. Cập nhật trạng thái ẩn/hiện. Nếu đặt là `false`, loại nông sản này bị ẩn và không cho phép tổ chức chọn khi tạo vùng trồng/lô mới. | false |
| Body | tempMin | Number | No | Ngưỡng nhiệt độ bảo quản tối thiểu (°C), từ -99.9 đến 999.9. Gửi `null`/bỏ trống để xóa ngưỡng đã lưu. | 12.5 |
| Body | tempMax | Number | No | Ngưỡng nhiệt độ bảo quản tối đa (°C), từ -99.9 đến 999.9, phải ≥ tempMin. Gửi `null` để xóa ngưỡng. | 28.0 |
| Body | humidityMin | Number | No | Ngưỡng độ ẩm bảo quản tối thiểu (%), từ 0 đến 100. Gửi `null` để xóa ngưỡng. | 65.0 |
| Body | humidityMax | Number | No | Ngưỡng độ ẩm bảo quản tối đa (%), từ 0 đến 100, phải ≥ humidityMin. Gửi `null` để xóa ngưỡng. | 85.0 |

**Request Example (JSON)**
```json
{
  "name": "Xoài Cát Chu Cao Lãnh",
  "group": "Cây ăn quả",
  "description": "Xoài cát chu xuất xứ Cao Lãnh",
  "isActive": false,
  "tempMin": 12.5,
  "tempMax": 28.0,
  "humidityMin": 65.0,
  "humidityMax": 85.0
}
```

**Response — Success**
| Status Code | When it occurs |
|---|---|
| 200 OK | Cập nhật thông tin loại nông sản thành công. |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "bf57bca1-628d-4a11-8f92-bd12d1b74291",
    "name": "Xoài Cát Chu Cao Lãnh",
    "group": "Cây ăn quả",
    "description": "Xoài cát chu xuất xứ Cao Lãnh",
    "isActive": false,
    "tempMin": 12.5,
    "tempMax": 28.0,
    "humidityMin": 65.0,
    "humidityMax": 85.0
  },
  "timestamp": "2026-07-30T10:10:00.123Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|---|---|---|
| 400 Bad Request | VALIDATION_ERROR | Dữ liệu cập nhật không hợp lệ. |
| 401 Unauthorized | - | Không cung cấp token xác thực hoặc token đã hết hạn. |
| 403 Forbidden | - | Người dùng không phải Quản lý hợp tác xã hoặc cố tình thực hiện thao tác sửa danh mục dùng chung (TC-03). |
| 404 Not Found | RESOURCE_NOT_FOUND | Không tìm thấy loại nông sản với ID đã truyền. |
| 409 Conflict | DUPLICATE_RESOURCE | Tên loại nông sản sau khi thay đổi bị trùng với loại nông sản khác đã tồn tại. |
| 500 Internal Error | - | Lỗi hệ thống khi truy vấn cơ sở dữ liệu. |

**Error Response Example (403 Forbidden - Người dùng không phải Admin sửa danh mục dùng chung)**
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện chức năng này",
  "path": "/api/v1/product-categories/bf57bca1-628d-4a11-8f92-bd12d1b74291",
  "timestamp": "2026-07-30T10:11:00.123Z"
}
```

**Error Response Example (404 Not Found - Không tồn tại ID)**
```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy loại nông sản với ID: bf57bca1-628d-4a11-8f92-bd12d1b74291",
  "path": "/api/v1/product-categories/bf57bca1-628d-4a11-8f92-bd12d1b74291",
  "timestamp": "2026-07-30T10:12:00.123Z"
}
```

---

## Quy tắc nghiệp vụ & Ràng buộc (Business Rules & Edge Cases)

1. **Phân quyền thao tác danh mục (Authorization Control):**
   - Chỉ người dùng có vai trò Quản trị viên hệ thống (`VT-01` - ADMIN) mới được phép thực hiện các thao tác ghi (Thêm/Sửa/Ẩn/Hiện) danh mục loại nông sản dùng chung.
   - Bất kỳ vai trò nào khác như Quản lý hợp tác xã (`VT-02` - ORG_MANAGER) cố gắng gửi request POST/PUT để thay đổi danh mục dùng chung sẽ nhận về mã lỗi `403 Forbidden` (NCL-09-CN-001-TC-03).

2. **Ràng buộc tính độc nhất của tên loại nông sản (Unique Product Category Name):**
   - Hệ thống không cho phép lưu hai loại nông sản có cùng một tên (ví dụ: đã có "Lúa" thì không được thêm mới "Lúa" hoặc "lúa").
   - Việc kiểm tra trùng lặp sẽ được thực hiện không phân biệt chữ hoa, chữ thường (case-insensitive) khi thêm mới hoặc chỉnh sửa (NCL-09-CN-001-TC-02).

3. **Cơ chế ẩn danh mục và tác động đến các chức năng khác (Soft Hiding & Reference Integrity):**
   - Khi loại nông sản được cập nhật trạng thái `isActive = false` (bị ẩn):
     - Loại nông sản này sẽ bị lọc bỏ khỏi các API lấy dữ liệu dùng chung cho người dùng thông thường.
     - Các tổ chức/hợp tác xã sẽ **không thể chọn** loại nông sản này khi tạo mới Vùng trồng (`farm_areas`) hoặc Lô sản xuất (`production_lots`).
     - Tuy nhiên, các Vùng trồng và Lô sản xuất **đã tồn tại trước đó** và đang liên kết với loại nông sản này sẽ không bị ảnh hưởng (dữ liệu lịch sử vẫn được hiển thị bình thường).
   - Khi khai báo Lô sản xuất mới (`production_lots`), hệ thống bắt buộc kiểm tra xem ID loại nông sản được truyền lên có tồn tại và đang hoạt động hay không. Nếu không hoạt động, trả về lỗi `400 Bad Request`.

4. **Ghi nhật ký hoạt động (Audit Trail):**
   - Mọi thao tác thay đổi danh mục loại nông sản do Quản trị viên thực hiện phải được ghi nhận tự động vào bảng nhật ký hoạt động (`activity_logs`) qua AOP `@Auditable` hoặc Publisher Event:
     - **Thêm mới loại nông sản**: Action = `CREATE_PRODUCT_CATEGORY`, Entity = `PRODUCT_CATEGORY`, mô tả ghi nhận rõ ID và tên loại nông sản được tạo.
     - **Cập nhật loại nông sản**: Action = `UPDATE_PRODUCT_CATEGORY`, Entity = `PRODUCT_CATEGORY`, mô tả ghi nhận rõ ID, tên và các trường thay đổi (ví dụ: chuyển trạng thái từ Hoạt động sang Ẩn).

---

## Endpoints liên quan (Related Endpoints)
- [Tạo mới vùng trồng (CreateFarmArea)](file:///d:/IntelliJ%20IDEA%202026.1.3/ProjectLocate/nguon-goc-so/docs/api/farm/CreateFarmArea.md)
- [Tạo/Cập nhật lô sản xuất](file:///d:/IntelliJ%20IDEA%202026.1.3/ProjectLocate/nguon-goc-so/docs/api/farm/ApproveProductionLot.md)
