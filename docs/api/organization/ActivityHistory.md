# API Lịch sử hoạt động (Activity History)

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
|---|---|---|---|
| 2026-07-30 | v1.0.0 | Khởi tạo tài liệu đặc tả API xem lịch sử hoạt động | AI Agent |

---

### GET /api/v1/organizations/activity-logs

**Description:** Lấy danh sách lịch sử hoạt động của tổ chức hiện tại. API hỗ trợ phân trang, lọc theo thời gian (startDate, endDate), lọc theo loại thao tác (action) và lọc theo người thực hiện (actorName).

**Authentication:** Yêu cầu Token JWT trong Header `Authorization: Bearer <token>`.
- Vai trò được phép: Quản lý tổ chức (`VT-02` - ORG_MANAGER).

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|---|---|---|---|---|---|
| Query | page | int | No | >= 0, default = 0 | 0 |
| Query | size | int | No | > 0, default = 10 | 10 |
| Query | action | String | No | Tên mã hóa của thao tác | "CREATE_PRODUCTION_LOT" |
| Query | actorName | String | No | Tìm kiếm tương đối theo tên hoặc username | "Nguyễn Văn" |
| Query | startDate | String | No | Định dạng `yyyy-MM-dd` | "2026-07-01" |
| Query | endDate | String | No | Định dạng `yyyy-MM-dd` | "2026-07-30" |

**Request Example**
`GET /api/v1/organizations/activity-logs?page=0&size=10&startDate=2026-07-01&endDate=2026-07-30`

*(Lưu ý: Đối với phương thức GET, không gửi kèm Request Body)*

**Response — Success**
| Status Code | When it occurs |
|---|---|
| 200 OK | Lấy lịch sử hoạt động thành công (trả về danh sách phân trang, có thể rỗng nếu chưa có hoạt động nào). |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "items": [
      {
        "id": "e4f8d6b1-4f8d-97e7-fedc-a6b6c68da760",
        "userId": "3c907154-1b15-46c8-bc4a-93df383a8b27",
        "username": "manager_coop1",
        "fullName": "Nguyễn Văn A",
        "action": "CREATE_PRODUCTION_LOT",
        "description": "Tạo lô sản xuất mới: Lô xoài cát chu Cát Tường",
        "entityType": "PRODUCTION_LOT",
        "entityId": "bf57bca1-628d-4a11-8f92-bd12d1b74291",
        "ipAddress": "192.168.1.15",
        "createdAt": "2026-07-30T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-07-30T10:05:00.123Z"
}
```

**Response — Error**
| Status Code | Error Code (if any) | Cause |
|---|---|---|
| 400 Bad Request | VALIDATION_ERROR | Định dạng ngày `startDate` hoặc `endDate` không hợp lệ. |
| 401 Unauthorized | - | Không cung cấp token xác thực hoặc token đã hết hạn. |
| 403 Forbidden | - | Người dùng không có vai trò Quản lý tổ chức (`VT-02`). |
| 500 Internal Error | - | Lỗi hệ thống khi truy vấn cơ sở dữ liệu. |

**Error Response Example (401 Unauthorized)**
```json
{
  "success": false,
  "status": 401,
  "message": "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn",
  "path": "/api/v1/organizations/activity-logs",
  "timestamp": "2026-07-30T09:16:05.123Z"
}
```

**Error Response Example (403 Forbidden)**
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện chức năng này",
  "path": "/api/v1/organizations/activity-logs",
  "timestamp": "2026-07-30T09:16:10.123Z"
}
```

**Error Response Example (400 Bad Request - Sai định dạng ngày)**
```json
{
  "success": false,
  "status": 400,
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "startDate": "Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'"
  },
  "path": "/api/v1/organizations/activity-logs",
  "timestamp": "2026-07-30T09:16:15.123Z"
}
```

---

## Quy tắc nghiệp vụ & Ràng buộc (Business Rules & Edge Cases)

1. **Cách ly dữ liệu nghiêm ngặt (Data Isolation - QTN-01):**
   - Hệ thống **bắt buộc** lấy `organizationId` từ JWT Token của người dùng đang đăng nhập thông qua đối tượng `CustomUserDetails` được xác thực.
   - Tuyệt đối không nhận `organizationId` dưới dạng tham số truyền lên từ client (như Query Param hay Request Header) để ngăn chặn hành vi khai thác, xem lén lịch sử hoạt động của tổ chức khác (NCL-08-CN-004-TC-03).
   - Nếu phát hiện truy cập trái phép hoặc không hợp lệ, trả về mã lỗi `403 Forbidden`.

2. **Cơ chế ghi nhật ký hoạt động (Audit Trail CV-01 / CV-02):**
   - Mọi hoạt động thay đổi dữ liệu hoặc xuất hồ sơ quan trọng trong tổ chức đều phải được ghi nhận tự động vào bảng `activity_logs`.
   - Danh sách các thao tác cần lưu vết bao gồm:

| Đối tượng dữ liệu (Entity) | Thao tác (Action) | Ý nghĩa nghiệp vụ | Dữ liệu cần ghi | Mức độ |
|---|---|---|---|---|
| **Organization** | `UPDATE_PROFILE` | Cập nhật hồ sơ tổ chức | Người thực hiện, thông tin thay đổi | Cao |
| **Farm Area** | `CREATE_FARM_AREA`<br>`UPDATE_FARM_AREA`<br>`DELETE_FARM_AREA` | Tạo/Cập nhật/Xóa vùng trồng | ID vùng trồng, tên vùng trồng | Cao |
| **Production Lot** | `CREATE_PRODUCTION_LOT`<br>`UPDATE_PRODUCTION_LOT`<br>`SUBMIT_FOR_APPROVAL`<br>`APPROVE_PRODUCTION_LOT` | Tạo/Cập nhật/Gửi duyệt/Phê duyệt lô sản xuất | ID lô, trạng thái, ghi chú | Cao |
| **Farm Log** | `CREATE_FARM_LOG`<br>`UPDATE_FARM_LOG`<br>`DELETE_FARM_LOG` | Tạo/Cập nhật/Xóa nhật ký canh tác | ID nhật ký, loại hoạt động (activity_type) | Trung bình |
| **Shipment** | `CREATE_SHIPMENT`<br>`UPDATE_SHIPMENT_STATUS` | Tạo/Cập nhật trạng thái lô hàng | ID lô hàng, trạng thái mới | Cao |
| **Trace Code** | `ACTIVATE_TRACE_CODE` | Kích hoạt mã QR truy xuất | ID lô hàng, số lượng mã | Cao |
| **Dossier** | `EXPORT_DOSSIER` | Xuất và tải hồ sơ truy xuất PDF | ID lô hàng, tên file, kích thước | Cao |

3. **Cơ chế hoạt động của phân trang & tìm kiếm:**
   - Trường hợp không có bản ghi lịch sử thỏa mãn bộ lọc, hệ thống trả về danh sách `items` rỗng `[]` kèm HTTP status `200 OK` (NCL-08-CN-004-TC-02).
   - Danh sách trả về được sắp xếp theo thời gian giảm dần (`createdAt DESC`) để đảm bảo các hoạt động mới nhất luôn được hiển thị trước tiên.

---

## Endpoints liên quan (Related Endpoints)
- [Hồ sơ tổ chức](file:///d:/IntelliJ%20IDEA%202026.1.3/ProjectLocate/nguon-goc-so/docs/api/organization/CreateOrganization.md)
