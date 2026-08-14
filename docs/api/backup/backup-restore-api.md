# Changelog
- **2026-08-03 (v1.0.0):** Khởi tạo tài liệu API sao lưu và phục hồi dữ liệu hệ thống (NCL-10-CN-008).

---

# API Sao lưu và Phục hồi Dữ liệu Nền tảng (Backup & Restore)

Tài liệu này cung cấp chi tiết các API dành cho chức năng sao lưu (Backup) và phục hồi (Restore) dữ liệu nền tảng. 

> [!IMPORTANT]
> **Quy định chung về phân quyền:**
> - Tất cả các API trong tài liệu này yêu cầu Header xác thực `Authorization: Bearer <JWT_TOKEN>`.
> - Chỉ người dùng có vai trò **Quản trị viên nền tảng (VT-01 - ADMIN)** mới được quyền thực hiện các API này. Tất cả các vai trò khác (như VT-02 - ORG_MANAGER, VT-06 - CONSUMER...) khi gọi API sẽ nhận về mã lỗi `403 Forbidden`.

---

### 1. GET /api/v1/backups/schedules

**Description:** Lấy danh sách cấu hình lịch sao lưu tự động của hệ thống.

**Authentication:** Yêu cầu đăng nhập, Role: `VT-01` (ADMIN).

**Request**
Không yêu cầu Request Body hoặc Query Parameters.

**Response — Success**
| Status Code | When it occurs |
|-------------|----------------|
| 200 OK      | Truy vấn thành công |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": 1,
      "cronExpression": "0 0 2 * * ?",
      "description": "Sao lưu toàn bộ Database lúc 02:00 sáng hàng ngày",
      "isActive": true,
      "createdAt": "2026-08-03T02:00:00Z",
      "updatedAt": "2026-08-03T09:00:00Z",
      "updatedBy": "Quản trị viên hệ thống"
    }
  ],
  "timestamp": "2026-08-03T02:45:00Z"
}
```

**Response — Error**
| Status Code | Error Code / Message | Cause |
|-------------|---|---|
| 401 Unauthorized | Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn | Token không hợp lệ hoặc thiếu Token |
| 403 Forbidden | Bạn không có quyền thực hiện chức năng này | Tài khoản không có vai trò VT-01 |

**Response Example (Error - 403)**
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện chức năng này",
  "path": "/api/v1/backups/schedules",
  "timestamp": "2026-08-03T02:45:05Z"
}
```

---

### 2. POST /api/v1/backups/schedules

**Description:** Thiết lập hoặc cập nhật lịch sao lưu tự động. Nếu đã tồn tại lịch, hệ thống sẽ ghi đè cấu hình cũ và tự động tải lại Scheduler mà không cần restart server.

**Authentication:** Yêu cầu đăng nhập, Role: `VT-01` (ADMIN).

**Request**
| Location | Field Name     | Data Type | Required | Constraints / Validation | Example |
|----------|----------------|-----------|----------|---------------------------|---------|
| Body     | cronExpression | String    | Yes      | Phải là Cron Expression hợp lệ, không trống | "0 0 2 * * ?" |
| Body     | description    | String    | No       | Tối đa 255 ký tự | "Backup lúc 2h sáng" |
| Body     | isActive       | Boolean   | Yes      | Không được NULL | true |

**Request Example (JSON)**
```json
{
  "cronExpression": "0 0 2 * * ?",
  "description": "Sao lưu tự động hàng ngày lúc 2 giờ sáng",
  "isActive": true
}
```

**Response — Success**
| Status Code | When it occurs |
|-------------|----------------|
| 200 OK      | Thiết lập/Cập nhật lịch thành công |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 1,
    "cronExpression": "0 0 2 * * ?",
    "description": "Sao lưu tự động hàng ngày lúc 2 giờ sáng",
    "isActive": true,
    "createdAt": "2026-08-03T02:00:00Z",
    "updatedAt": "2026-08-03T02:46:00Z",
    "updatedBy": "admin"
  },
  "timestamp": "2026-08-03T02:46:00Z"
}
```

**Response — Error**
| Status Code | Error Code / Message | Cause |
|-------------|---|---|
| 400 Bad Request | Dữ liệu không hợp lệ | Định dạng Cron Expression sai hoặc thiếu trường bắt buộc |
| 401 Unauthorized | Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn | Token không hợp lệ |
| 403 Forbidden | Bạn không có quyền thực hiện chức năng này | Tài khoản không có vai trò VT-01 |

**Response Example (Error - 400)**
```json
{
  "success": false,
  "status": 400,
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "cronExpression": "Cron expression không đúng định dạng Spring Schedule"
  },
  "path": "/api/v1/backups/schedules",
  "timestamp": "2026-08-03T02:46:05Z"
}
```

---

### 3. POST /api/v1/backups/trigger

**Description:** Kích hoạt quá trình sao lưu Database ngay lập tức (sao lưu thủ công). API sẽ trả về trạng thái ngay khi tiến hành chạy ngầm (Asynchronous).

**Authentication:** Yêu cầu đăng nhập, Role: `VT-01` (ADMIN).

**Request**
Không có tham số đầu vào.

**Response — Success**
| Status Code | When it occurs |
|-------------|----------------|
| 202 Accepted| Yêu cầu sao lưu được chấp nhận và đang chạy ngầm |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 202,
  "data": {
    "id": 12,
    "operationType": "BACKUP",
    "fileName": "backup_20260803_094600.sql.gz",
    "backupType": "MANUAL",
    "status": "IN_PROGRESS",
    "createdAt": "2026-08-03T02:46:00Z",
    "createdBy": "admin"
  },
  "timestamp": "2026-08-03T02:46:00Z"
}
```

**Response — Error**
| Status Code | Error Code / Message | Cause |
|-------------|---|---|
| 401 Unauthorized | Chưa đăng nhập | Thiếu/sai token |
| 403 Forbidden | Không có quyền | Không phải role VT-01 |
| 409 Conflict | Đang có một tiến trình sao lưu hoặc khôi phục khác đang diễn ra | Hệ thống không cho chạy song song |

---

### 4. GET /api/v1/backups/history

**Description:** Truy vấn lịch sử các hoạt động sao lưu và phục hồi dữ liệu trong hệ thống (Hỗ trợ phân trang, lọc theo loại thao tác và trạng thái).

**Authentication:** Yêu cầu đăng nhập, Role: `VT-01` (ADMIN).

**Request**
| Location | Field Name    | Data Type | Required | Constraints / Validation | Example |
|----------|---------------|-----------|----------|---------------------------|---------|
| Query    | page          | int       | No       | default = 0, >= 0         | 0       |
| Query    | size          | int       | No       | default = 10, > 0         | 10      |
| Query    | operationType | String    | No       | Giá trị: `BACKUP`, `RESTORE` | "BACKUP"|
| Query    | status        | String    | No       | Giá trị: `IN_PROGRESS`, `SUCCESS`, `FAILED` | "SUCCESS"|

**Request Example (Query URL)**
`/api/v1/backups/history?page=0&size=5&operationType=BACKUP`

**Response — Success**
| Status Code | When it occurs |
|-------------|----------------|
| 200 OK      | Truy vấn dữ liệu lịch sử phân trang thành công |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "id": 12,
        "operationType": "BACKUP",
        "fileName": "backup_20260803_094600.sql.gz",
        "fileSize": 1048576,
        "backupType": "MANUAL",
        "status": "SUCCESS",
        "errorMessage": null,
        "referenceId": null,
        "createdAt": "2026-08-03T02:46:00Z",
        "createdBy": "admin"
      },
      {
        "id": 11,
        "operationType": "RESTORE",
        "fileName": null,
        "fileSize": null,
        "backupType": null,
        "status": "SUCCESS",
        "errorMessage": null,
        "referenceId": 10,
        "createdAt": "2026-08-02T15:00:00Z",
        "createdBy": "admin"
      }
    ],
    "page": {
      "size": 5,
      "number": 0,
      "totalElements": 12,
      "totalPages": 3
    }
  },
  "timestamp": "2026-08-03T02:47:00Z"
}
```

---

### 5. GET /api/v1/backups/history/{id}/download

**Description:** Tải trực tiếp file sao lưu vật lý `.sql.gz` từ server về máy khách.

**Authentication:** Yêu cầu đăng nhập, Role: `VT-01` (ADMIN).

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|----------|------------|-----------|----------|---------------------------|---------|
| Path     | id         | Long      | Yes      | Phải là ID bản ghi tồn tại và là BACKUP thành công | 12 |

**Response — Success**
| Status Code | When it occurs | Content-Type |
|-------------|----------------|--------------|
| 200 OK      | Tải xuống file thành công | `application/octet-stream` |

**Headers:**
- `Content-Disposition: attachment; filename="backup_20260803_094600.sql.gz"`

**Response — Error**
| Status Code | Error Code / Message | Cause |
|-------------|---|---|
| 404 Not Found | Bản ghi lịch sử không tồn tại hoặc file vật lý đã bị xóa | ID sai hoặc file đã bị xóa bởi chính sách dọn dẹp |
| 400 Bad Request | Thao tác không hợp lệ | ID truyền vào là bản ghi RESTORE không có file |

---

### 6. DELETE /api/v1/backups/history/{id}

**Description:** Xóa bản sao lưu vật lý trên đĩa cứng máy chủ và cập nhật trạng thái bản ghi lịch sử tương ứng hoặc xóa bản ghi log.

**Authentication:** Yêu cầu đăng nhập, Role: `VT-01` (ADMIN).

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|----------|------------|-----------|----------|---------------------------|---------|
| Path     | id         | Long      | Yes      | ID bản ghi sao lưu hiện có | 12 |

**Response — Success**
| Status Code | When it occurs |
|-------------|----------------|
| 200 OK      | Xóa file và bản ghi lịch sử thành công |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 200,
  "message": "Đã xóa bản sao lưu vật lý thành công",
  "timestamp": "2026-08-03T02:48:00Z"
}
```

---

### 7. POST /api/v1/backups/history/{id}/restore

**Description:** Kích hoạt quá trình phục hồi (Restore) cơ sở dữ liệu của nền tảng dựa trên file của bản sao lưu được chỉ định.

**Authentication:** Yêu cầu đăng nhập, Role: `VT-01` (ADMIN).

**Request**
| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
|----------|------------|-----------|----------|---------------------------|---------|
| Path     | id         | Long      | Yes      | ID của bản ghi sao lưu có trạng thái `SUCCESS` | 12 |

**Response — Success**
| Status Code | When it occurs |
|-------------|----------------|
| 202 Accepted| Yêu cầu phục hồi dữ liệu được chấp nhận và hệ thống bắt đầu chế độ Bảo trì |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 202,
  "data": {
    "id": 13,
    "operationType": "RESTORE",
    "status": "IN_PROGRESS",
    "referenceId": 12,
    "createdAt": "2026-08-03T02:50:00Z",
    "createdBy": "admin"
  },
  "timestamp": "2026-08-03T02:50:00Z"
}
```

**Response — Error**
| Status Code | Error Code / Message | Cause |
|-------------|---|---|
| 404 Not Found | Bản ghi sao lưu không tồn tại hoặc file không còn trên ổ đĩa | ID sai hoặc file đã bị xóa |
| 409 Conflict | Đang có một tiến trình sao lưu/khôi phục khác chạy ngầm | Xung đột luồng thực thi |

---

## Business Rules / Edge Cases

1. **Khóa an toàn tiến trình (Process Locking):**
   - Tại một thời điểm, chỉ cho phép duy nhất 1 tiến trình `BACKUP` hoặc `RESTORE` được hoạt động. Nếu quản trị viên gọi API sao lưu/phục hồi trong khi tiến trình trước đó đang chạy (trạng thái `IN_PROGRESS`), hệ thống sẽ lập tức trả về lỗi `409 Conflict`.
2. **Chế độ bảo trì hệ thống (Maintenance Mode):**
   - Ngay khi quá trình `RESTORE` bắt đầu (trạng thái `IN_PROGRESS`), hệ thống sẽ tự động kích hoạt **Maintenance Mode**.
   - Trong chế độ này, tất cả các request đến các API khác trong hệ thống (ngoại trừ các endpoint check health như `/actuator/health` và các API thuộc `/api/v1/backups/**` dành riêng cho ADMIN) sẽ bị Filter chặn lại và trả về mã lỗi:
     - **Status Code:** `503 Service Unavailable`
     - **Message:** `"Hệ thống đang tiến hành bảo trì phục hồi dữ liệu. Vui lòng quay lại sau."`
   - Chế độ bảo trì sẽ tự động kết thúc khi tiến trình `RESTORE` hoàn thành (chuyển sang trạng thái `SUCCESS` hoặc `FAILED`).
3. **Bảo toàn dữ liệu khi khôi phục thất bại (Restore Rollback / Backup before Restore):**
   - Trước khi thực hiện Restore từ một bản sao lưu cũ, hệ thống sẽ **tự động kích hoạt một luồng sao lưu nhanh (Quick Backup)** để lưu trạng thái dữ liệu hiện tại vào file `auto_backup_before_restore_<timestamp>.sql.gz`.
   - Nếu quá trình khôi phục gặp lỗi giữa chừng, hệ thống sẽ tự động dùng file sao lưu nhanh này để khôi phục lại hiện trạng dữ liệu nhằm tránh mất mát dữ liệu hiện tại. Kết quả của tiến trình phục hồi sẽ được ghi lại trong log là `FAILED` kèm theo `error_message` chi tiết.
4. **Quyền hạn truy cập của Người dùng khác:**
   - Trường hợp **Quản lý hợp tác xã (VT-02)** gửi request yêu cầu khôi phục dữ liệu hoặc truy cập bất kỳ API nào liên quan đến backup, Spring Security sẽ ngay lập tức chặn ở tầng Filter và trả về `403 Forbidden` mà không cần đi vào logic nghiệp vụ của Controller.

---

## Related Endpoints
- `/api/v1/auth/login` (Dùng để lấy JWT Token của tài khoản ADMIN)
- `/actuator/health` (Dùng để kiểm tra trạng thái hoạt động của Service trong thời gian bảo trì)
