# API Phân tích theo Vùng trồng và Mùa vụ

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
|---|---|---|---|
| 2026-07-30 | v1.0.0 | Khởi tạo tài liệu đặc tả API phân tích sản lượng và số lô theo vùng trồng và mùa vụ cho cán bộ quản lý ngành | AI Agent |

---

### GET /api/v1/reports/crop-area-analysis

**Description:** Tổng hợp sản lượng (dự kiến và thực tế), số lô sản xuất và diện tích theo vùng trồng và mùa vụ nông nghiệp, hỗ trợ lọc theo thời gian, loại nông sản, vùng trồng và tổ chức.

**Authentication:** Yêu cầu Token JWT trong Header `Authorization: Bearer <token>`.
- Vai trò được phép: 
  - `VT-01` (ADMIN): Quản trị viên hệ thống (có quyền truy cập tất cả dữ liệu).
  - `VT-05` (REGULATOR): Cán bộ quản lý ngành (có quyền xem toàn bộ báo cáo phân tích trên địa bàn).
- Các vai trò khác (như `VT-02` - ORG_MANAGER, `VT-03` - EVENT_RECORDER,...) không được phép truy cập.

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Query | year | Integer | No | Mặc định là năm hiện tại nếu không truyền. Lọc các lô xuống giống trong năm này. | 2026 |
| Query | farmAreaId | UUID | No | Lọc phân tích theo một vùng trồng cụ thể | "e12f60a2-2735-430b-93ff-18306df679e0" |
| Query | productCategoryId | UUID | No | Lọc phân tích theo loại nông sản | "bf57bca1-628d-4a11-8f92-bd12d1b74291" |
| Query | organizationId | UUID | No | Lọc phân tích theo một tổ chức / hợp tác xã cụ thể | "a9f8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4d" |

**Request Example**
`GET /api/v1/reports/crop-area-analysis?year=2026&farmAreaId=e12f60a2-2735-430b-93ff-18306df679e0`

*(Lưu ý: Đối với phương thức GET, không gửi kèm Request Body)*

---

**Response — Success**
| Status Code | When it occurs |
|---|---|
| 200 OK | Tổng hợp và phân tích số liệu thành công. |

**Response Example (Success - Có dữ liệu)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "summary": {
      "totalLots": 12,
      "totalExpectedYield": 25000.0,
      "totalActualYield": 23500.0,
      "totalArea": 15.5
    },
    "byArea": [
      {
        "farmAreaId": "e12f60a2-2735-430b-93ff-18306df679e0",
        "farmAreaName": "Vùng trồng Chè Tân Cương A",
        "areaSize": 5.5,
        "organizationName": "Hợp tác xã Chè Tân Cương Thái Nguyên",
        "totalLots": 6,
        "expectedYield": 12000.0,
        "actualYield": 11500.0,
        "seasons": [
          {
            "seasonCode": "DONG_XUAN",
            "seasonName": "Vụ Đông Xuân",
            "year": 2026,
            "lotCount": 3,
            "expectedYield": 6000.0,
            "actualYield": 5800.0
          },
          {
            "seasonCode": "HE_THU",
            "seasonName": "Vụ Hè Thu",
            "year": 2026,
            "lotCount": 3,
            "expectedYield": 6000.0,
            "actualYield": 5700.0
          }
        ]
      }
    ],
    "bySeason": [
      {
        "seasonCode": "DONG_XUAN",
        "seasonName": "Vụ Đông Xuân",
        "year": 2026,
        "totalLots": 6,
        "expectedYield": 13000.0,
        "actualYield": 12200.0,
        "areas": [
          {
            "farmAreaId": "e12f60a2-2735-430b-93ff-18306df679e0",
            "farmAreaName": "Vùng trồng Chè Tân Cương A",
            "lotCount": 3,
            "expectedYield": 6000.0,
            "actualYield": 5800.0
          }
        ]
      },
      {
        "seasonCode": "HE_THU",
        "seasonName": "Vụ Hè Thu",
        "year": 2026,
        "totalLots": 6,
        "expectedYield": 12000.0,
        "actualYield": 11300.0,
        "areas": [
          {
            "farmAreaId": "e12f60a2-2735-430b-93ff-18306df679e0",
            "farmAreaName": "Vùng trồng Chè Tân Cương A",
            "lotCount": 3,
            "expectedYield": 6000.0,
            "actualYield": 5700.0
          }
        ]
      }
    ]
  },
  "timestamp": "2026-07-30T10:00:00.000Z"
}
```

**Response Example (Success - Khi chưa có dữ liệu/Dữ liệu rỗng)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "summary": {
      "totalLots": 0,
      "totalExpectedYield": 0.0,
      "totalActualYield": 0.0,
      "totalArea": 0.0
    },
    "byArea": [],
    "bySeason": []
  },
  "timestamp": "2026-07-30T10:00:00.000Z"
}
```

---

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|---|---|---|
| 401 Unauthorized | - | Không cung cấp token xác thực hoặc token đã hết hạn. |
| 403 Forbidden | - | Người dùng không thuộc vai trò được phép (`VT-01`, `VT-05`). |
| 500 Internal Error | - | Lỗi hệ thống khi thực hiện truy vấn hoặc tính toán dữ liệu. |

**Error Response Example (403 Forbidden)**
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện chức năng này",
  "path": "/api/v1/reports/crop-area-analysis",
  "timestamp": "2026-07-30T10:05:00.123Z"
}
```

**Error Response Example (401 Unauthorized)**
```json
{
  "success": false,
  "status": 401,
  "message": "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn",
  "path": "/api/v1/reports/crop-area-analysis",
  "timestamp": "2026-07-30T10:05:00.123Z"
}
```

---

## Business Rules / Edge Cases

* **QTN-01 – Giới hạn phân quyền truy cập:**
  - Chỉ có người dùng mang vai trò `VT-05` (Cán bộ quản lý ngành) hoặc `VT-01` (Admin) mới có quyền gọi API này.
  - Mọi nỗ lực truy cập từ các vai trò khác (`VT-02`, `VT-03`, `VT-04`, `VT-06`) đều sẽ bị hệ thống từ chối bằng mã lỗi `403 Forbidden`.
* **QTN-02 – Nhật ký truy cập báo cáo (Audit Log):**
  - Mọi lượt truy cập vào API bảng phân tích này (dù thành công hay bị lỗi 403) đều phải được hệ thống ghi nhận vào bảng `report_access_log`.
  - Tên báo cáo được ghi nhận là: `CROP_AREA_ANALYSIS`.
  - Trạng thái `success` lưu là `1` (thành công) hoặc `0` (thất bại/bị chặn).
* **QTN-03 – Quy tắc tính toán mùa vụ (Seasoning Logic):**
  - Mùa vụ được xác định dựa trên ngày xuống giống (`plantingDate`) của từng lô sản xuất (`ProductionLot`):
    - **Vụ Đông Xuân (`DONG_XUAN`):** Lô sản xuất có ngày xuống giống từ ngày `01/11` của năm trước đến hết ngày `30/04` của năm lọc hiện tại.
    - **Vụ Hè Thu (`HE_THU`):** Lô sản xuất có ngày xuống giống từ ngày `01/05` đến hết ngày `31/08` của năm lọc hiện tại.
    - **Vụ Thu Đông / Vụ Mùa (`THU_DONG`):** Lô sản xuất có ngày xuống giống từ ngày `01/09` đến hết ngày `31/10` của năm lọc hiện tại.
  - Các lô sản xuất không có ngày xuống giống (`planting_date IS NULL`) hoặc nằm ngoài phạm vi lọc năm sẽ không được đưa vào phân tích tổng hợp.
* **QTN-04 – Cách tính tổng diện tích (`totalArea`):**
  - `totalArea` trong phần `summary` là tổng diện tích của tất cả các vùng trồng (`FarmArea`) có lô sản xuất được đưa vào phân tích trong năm lọc hiện tại. Mỗi vùng trồng chỉ được cộng diện tích một lần (không cộng lặp theo số lô).

---

## Liên kết liên quan (Related Endpoints)
- [Bảng điều khiển sản lượng và lô (ProductionAndLotDashboard)](file:///d:/IntelliJ%20IDEA%202026.1.3/ProjectLocate/nguon-goc-so/docs/api/report%20&%20dossier%20export/ProductionAndLotDashboard.md)
