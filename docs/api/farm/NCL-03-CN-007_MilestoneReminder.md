# API: Nhắc lịch ghi nhật ký theo mốc canh tác bắt buộc (NCL-03-CN-007)

**Epic NCL-03: Nhật ký canh tác và chứng từ**  
**User Story: NCL-03-CN-007** — Nhắc lịch ghi nhật ký theo mốc canh tác bắt buộc  
**Quy tắc liên quan:** QTN-06, QTN-07  
**User Story liên quan:** NCL-09-CN-011 (Cấu hình mốc canh tác bắt buộc), NCL-08-CN-005 (Nhận thông báo)

---

## 1. Thông tin chung

### 1.1 Mục tiêu
Cung cấp cơ chế chủ động nhắc việc ghi nhật ký canh tác cho Người ghi sự kiện khi một mốc canh tác bắt buộc của lô sản xuất chưa được ghi nhật ký và đã quá hạn dự kiến tính từ ngày gieo trồng. 

Cơ chế này biến một quy tắc chặn (QTN-06, NCL-03-CN-004 — chặn đóng gói khi thiếu nhật ký ở cuối chuỗi) thành một quy tắc hỗ trợ chủ động, giúp người ghi sự kiện bổ sung nhật ký kịp thời ngoài đồng, đảm bảo lô hàng không bị chặn đóng gói vì thiếu bằng chứng vào phút chót.

### 1.2 Nguyên tắc nghiệp vụ
1. **Lô thuộc diện quét:** Chỉ quét các lô đang trong giai đoạn canh tác có trạng thái `APPROVED` (Đã duyệt) và có `plantingDate` (ngày gieo trồng). **Tuyệt đối không nhắc** với lô đã hủy (`CANCELLED`), đã thu hồi (`RECALLED`), đã đóng gói (`PACKAGED`) hoặc các trạng thái không canh tác khác (`DRAFT`, `PENDING`, `CLOSED`, `DISPOSED`).
2. **Xác định mốc thiếu:** Sử dụng danh sách mốc bắt buộc (`isMandatory = true`) áp dụng cho loại nông sản (`productCategory`) và bộ tiêu chuẩn (`standards`) gắn cho lô, đối chiếu với các nhật ký canh tác hiệu lực (`FarmLog` không bị đính chính `isCorrected = false`) theo `activityType`.
3. **Xác định quá hạn:**
   - Ngày dự kiến: `expectedDate = plantingDate + expectedDaysFromPlanting`.
   - Mốc bị coi là quá hạn khi: `today > expectedDate`.
   - Số ngày quá hạn: `overdueDays = today - expectedDate` (ngày).
4. **Người nhận nhắc việc:** Thành viên được phân công phụ trách lô (bản ghi `lot_assignments` có `active = true`). Trường hợp chưa có phân công cụ thể, gửi cho người tạo lô (`createdBy`).
5. **Cơ chế thông báo:** Tái sử dụng cơ chế thông báo của NCL-08-CN-005 (`Notification` loại `TASK`), tiêu đề rõ ràng, nội dung hiển thị tên lô, mốc còn thiếu và số ngày quá hạn:  
   *Ví dụ: "Lô Lúa ST25 - Vụ Đông Xuân thiếu mốc Bón phân đợt 1 quá hạn 3 ngày."*
6. **Chống tạo nhắc việc trùng:** Không tạo nhắc trùng trong cùng ngày cho cùng một mốc trên cùng một lô (`reminder_date = today`). Khi quét lần hai hoặc nhiều lần trong cùng ngày, hệ thống bỏ qua các mốc đã được tạo nhắc việc trong ngày.
7. **Tự động đóng nhắc việc:** Khi người ghi nhập nhật ký canh tác mới (`FarmLog`) cho mốc đó (khớp `activityType` của mốc trên lô), toàn bộ nhắc việc đang mở (`OPEN`) cho mốc đó tự động chuyển sang trạng thái đã hoàn thành (`COMPLETED`), ghi nhận thời điểm hoàn thành `completedAt`.

---

## 2. Endpoints

### 2.1 Kích hoạt quét mốc quá hạn và tạo nhắc việc
- **URL:** `/api/v1/milestone-reminders/scan`
- **Method:** `POST`
- **Authentication:** Bearer Token (JWT)
- **Authorization:** `VT-01` (Quản trị viên nền tảng), `VT-02` (Quản lý hợp tác xã)
- **Mô tả:** Chạy quy trình quét mốc canh tác bắt buộc quá hạn trên toàn hệ thống (hoặc theo tổ chức của VT-02), tạo bản ghi nhắc việc và thông báo tương ứng. Endpoint này cũng được gọi tự động theo lịch trình hằng ngày (`@Scheduled` lúc 02:00 AM).

#### Request Body
Không có (empty body).

#### Response 200 OK
```json
{
  "success": true,
  "status": 200,
  "message": "Quét mốc canh tác quá hạn thành công.",
  "data": {
    "scannedLotsCount": 5,
    "remindersCreatedCount": 2,
    "message": "Đã quét 5 lô sản xuất, tạo mới 2 nhắc việc quá hạn."
  },
  "timestamp": "2026-09-08T02:00:05.123Z"
}
```

---

### 2.2 Lấy danh sách nhắc việc canh tác
- **URL:** `/api/v1/milestone-reminders`
- **Method:** `GET`
- **Authentication:** Bearer Token (JWT)
- **Authorization:** `VT-01`, `VT-02`, `VT-03`
- **Query Parameters:**
  - `status` (String, optional): Lọc theo trạng thái `OPEN` hoặc `COMPLETED`. Mặc định lấy tất cả.
  - `lotId` (UUID, optional): Lọc theo ID lô sản xuất.
  - `page` (int, default: 0): Trang.
  - `size` (int, default: 20): Số lượng bản ghi mỗi trang.

#### Phạm vi dữ liệu theo vai trò:
- `VT-01`: Xem tất cả nhắc việc trong hệ thống.
- `VT-02`: Xem tất cả nhắc việc của các lô thuộc tổ chức của mình.
- `VT-03`: Xem các nhắc việc được phân công cho chính mình.

#### Response 200 OK
```json
{
  "success": true,
  "status": 200,
  "data": {
    "items": [
      {
        "id": "c1a2b3c4-1111-4444-8888-abcdef123456",
        "lotId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "lotName": "Lô Lúa ST25 - Đợt 1",
        "milestoneId": 12,
        "milestoneName": "Bón phân đợt 1",
        "activityType": "FERTILIZING",
        "overdueDays": 3,
        "expectedDate": "2026-09-05",
        "status": "OPEN",
        "reminderDate": "2026-09-08",
        "completedAt": null,
        "createdAt": "2026-09-08T02:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-09-08T08:00:00Z"
}
```

---

### 2.3 Lấy danh sách nhắc việc đang mở của người dùng hiện tại (Dành cho Mobile & Dashboard)
- **URL:** `/api/v1/milestone-reminders/my-active`
- **Method:** `GET`
- **Authentication:** Bearer Token (JWT)
- **Authorization:** `VT-02`, `VT-03`
- **Mô tả:** Trả về danh sách các nhắc việc đang mở (`status = OPEN`) được phân công cho người dùng hiện tại (hoặc thuộc tổ chức nếu là quản lý), sắp xếp theo số ngày quá hạn giảm dần (ưu tiên việc gấp).

#### Response 200 OK
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "c1a2b3c4-1111-4444-8888-abcdef123456",
      "lotId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "lotName": "Lô Lúa ST25 - Đợt 1",
      "milestoneId": 12,
      "milestoneName": "Bón phân đợt 1",
      "activityType": "FERTILIZING",
      "overdueDays": 3,
      "expectedDate": "2026-09-05",
      "status": "OPEN",
      "reminderDate": "2026-09-08",
      "completedAt": null,
      "createdAt": "2026-09-08T02:00:00Z"
    }
  ],
  "timestamp": "2026-09-08T08:05:00Z"
}
```

---

## 3. Data Structure

### 3.1 Bảng cơ sở dữ liệu `milestone_reminders`
| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | `CHAR(36)` | PK | UUID định danh nhắc việc |
| `lot_id` | `CHAR(36)` | FK `production_lot(id)`, NOT NULL | Lô sản xuất liên quan |
| `milestone_id` | `BIGINT` | FK `cultivation_milestone(id)`, NOT NULL | Mốc canh tác bị quá hạn |
| `user_id` | `CHAR(36)` | FK `users(user_id)`, NOT NULL | Người nhận nhắc việc |
| `notification_id` | `CHAR(36)` | FK `notifications(id)`, NULL | Bản ghi thông báo liên kết (NCL-08-CN-005) |
| `overdue_days` | `INT` | NOT NULL | Số ngày quá hạn tại thời điểm nhắc |
| `reminder_date` | `DATE` | NOT NULL | Ngày phát nhắc việc (phục vụ chống trùng lặp trong ngày) |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'OPEN'` | Trạng thái: `OPEN`, `COMPLETED` |
| `completed_at` | `DATETIME(6)` | NULL | Thời điểm tự đóng khi đã ghi nhật ký |
| `created_at` | `DATETIME(6)` | NOT NULL | Thời điểm tạo bản ghi |
| `updated_at` | `DATETIME(6)` | NULL | Thời điểm cập nhật cuối cùng |

Ràng buộc toàn vẹn duy nhất:  
`uk_lot_milestone_date_user (lot_id, milestone_id, reminder_date, user_id)` — Đảm bảo không tạo nhắc việc trùng trong cùng một ngày cho cùng một mốc.

---

## 4. Error Responses

### 4.1 401 Unauthorized
```json
{
  "success": false,
  "status": 401,
  "message": "Bạn cần đăng nhập để thực hiện thao tác này."
}
```

### 4.2 403 Forbidden
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện thao tác này."
}
```
