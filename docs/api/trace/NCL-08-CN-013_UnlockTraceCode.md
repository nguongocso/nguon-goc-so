# 📘 API Docs: Mở khóa mã tem sau khi xác minh (NCL-08-CN-013)

## 1. Thông tin chung

| Thuộc tính | Giá trị |
| --- | --- |
| **User Story** | NCL-08-CN-013 - Mở khóa mã tem sau khi xác minh |
| **Epic** | NCL-08 - Cảnh báo, thu hồi lô và lịch sử hoạt động |
| **Git Branch** | `feature/NCL-08-CN-013_unlock-trace-code` |
| **Vai trò thực hiện** | VT-01 - Quản trị viên nền tảng |
| **Endpoint** | `POST /api/v1/admin/trace-codes/{codeValue}/unlock` (hoặc `{traceCodeId}/unlock`) |
| **Phương thức** | `POST` |
| **Bảo mật** | Yêu cầu JWT token, `@PreAuthorize("hasRole('VT-01')")` |

---

## 2. Mô tả nghiệp vụ & Quy tắc (Business Rules)

Story này bổ sung luồng đảo ngược (reverse flow) cho NCL-08-CN-007 (khóa mã tem nghi vấn). Khi một mã tem đã bị khóa được xác minh là hợp lệ / an toàn, Quản trị viên nền tảng (VT-01) có thể mở khóa mã tem.

### Quy tắc nghiệp vụ:
1. **Trạng thái hợp lệ để mở khóa**: Mã tem phải đang ở trạng thái `LOCKED`. Nếu mã không ở trạng thái `LOCKED` (ví dụ: `ACTIVE`, `INACTIVE`, `RECALLED`), hệ thống trả lỗi HTTP 409 Conflict.
2. **Kết luận xác minh bắt buộc (QTN-29)**: Quản trị viên phải cung cấp kết luận xác minh (`conclusion` / `unlockConclusion`) với độ dài tối thiểu 10 ký tự, tối đa 500 ký tự. Bằng chứng (`evidence` / `unlockEvidence`) là tùy chọn (tối đa 500 ký tự).
3. **Quy tắc cùng Quản trị viên (Same Admin Check)**:
   - Nếu quản trị viên thực hiện mở khóa chính là người đã thực hiện khóa mã tem trước đó (`locked_by == currentUser.userId`), yêu cầu kết luận xác minh phải chi tiết hơn (tối thiểu 20 ký tự).
4. **Cập nhật trạng thái**:
   - Trạng thái mã tem chuyển từ `LOCKED` về `ACTIVE`.
   - Lưu thông tin mở khóa: `unlocked_by`, `unlock_conclusion`, `unlock_evidence`, `unlocked_at`, `verification_note` (gán từ conclusion).
   - Đặt lại / giữ vết các trường khóa phục vụ tra cứu.
5. **Gỡ cảnh báo công khai**:
   - Trang tra cứu công khai (`/public/trace/{codeValue}`) gỡ bỏ cảnh báo mã bị khóa (`LockAlert`), hiển thị ghi chú đã xác minh ("Đã xác minh") kèm kết luận xác minh và thời gian xác minh.
6. **Thông báo**:
   - Gửi thông báo đến người quản lý của Hợp tác xã / Doanh nghiệp sở hữu mã tem (`shipment.organization`).
7. **Nhật ký hoạt động (Audit Trail)**:
   - Lưu sự kiện vào `activity_logs` với action `UNLOCK_TRACE_CODE`, ghi nhận người thực hiện, thời gian, mã tem và kết luận.

---

## 3. Chi tiết API Endpoint

### 3.1. Mở khóa mã tem
```http
POST /api/v1/admin/trace-codes/{codeValue}/unlock
```
Hoặc qua UUID:
```http
POST /api/v1/admin/trace-codes/{traceCodeId}/unlock
```

#### Headers:
```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

#### Request Body:
```json
{
  "conclusion": "Đã kiểm tra hóa đơn vận chuyển và camera hành trình, các lượt quét trùng lặp do lỗi thiết bị quét tại điểm phân phối.",
  "evidence": "Biên bản làm việc số 12/BB-XM ngày 30/08/2026 đính kèm."
}
```

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả | Ràng buộc |
| --- | --- | --- | --- | --- |
| `conclusion` | String | Có | Kết luận xác minh | 10 - 500 ký tự (≥ 20 ký tự nếu cùng admin đã khóa) |
| `evidence` | String | Không | Bằng chứng xác minh | Tối đa 500 ký tự |

#### Responses:

##### Success: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Mở khóa mã tem thành công",
  "data": {
    "id": "c1f7b764-7e9b-4e89-b7b5-2cfc80b91e92",
    "codeValue": "NCL0001",
    "status": "ACTIVE",
    "unlockedAt": "2026-08-30T13:00:00",
    "unlockedBy": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "unlockedByName": "Nguyễn Văn Admin",
    "unlockConclusion": "Đã kiểm tra hóa đơn vận chuyển và camera hành trình...",
    "unlockEvidence": "Biên bản làm việc số 12/BB-XM...",
    "verificationNote": "Đã kiểm tra hóa đơn vận chuyển và camera hành trình...",
    "notificationSent": true
  },
  "timestamp": "2026-08-30T13:00:00.000Z"
}
```

##### Error: `400 Bad Request` (Validation Failed)
```json
{
  "success": false,
  "status": 400,
  "message": "Kết luận xác minh phải từ 10 đến 500 ký tự (tối thiểu 20 ký tự nếu cùng người khóa)",
  "timestamp": "2026-08-30T13:00:00.000Z"
}
```

##### Error: `403 Forbidden` (Không có quyền VT-01)
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện thao tác này",
  "timestamp": "2026-08-30T13:00:00.000Z"
}
```

##### Error: `404 Not Found` (Mã tem không tồn tại)
```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy mã tem.",
  "timestamp": "2026-08-30T13:00:00.000Z"
}
```

##### Error: `409 Conflict` (Mã tem không ở trạng thái LOCKED)
```json
{
  "success": false,
  "status": 409,
  "message": "Mã tem không ở trạng thái bị khóa.",
  "timestamp": "2026-08-30T13:00:00.000Z"
}
```

---

## 4. Ảnh hưởng đến Public Trace API

### Tra cứu công khai (`GET /api/v1/public/trace/{codeValue}`)

Khi mã tem ở trạng thái `ACTIVE` và có `unlockConclusion` / `verificationNote`:
- Trường `locked` trả về `false`.
- Bổ sung trường `verificationNote` và `unlockedAt` trong response `PublicTraceResponse`.
- Giao diện người dùng hiển thị hộp thông tin xác minh (Verified Banner) màu xanh lá thay cho cảnh báo khóa màu đỏ.
