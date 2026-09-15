# API: Cảnh báo khóa truy cập sắp hết hạn và sắp chạm hạn mức (NCL-12-CN-005)

> **User Story:** NCL-12-CN-005 — Epic NCL-12 (Cổng dữ liệu và hồ sơ theo lược đồ chuẩn)
> **Quy tắc nghiệp vụ:** QTN-20 (khóa có thời hạn và hạn mức lượt gọi/giờ)
> **Phụ thuộc:** NCL-12-CN-001 (cấp/thu hồi khóa), NCL-08-CN-005 (hộp thông báo), NCL-08-CN-016 (trang cảnh báo tổng hợp)
> **Trạng thái hợp đồng:** Nguồn sự thật API cho NCL-12-CN-005

---

## 1. Tổng quan hành vi

Hệ thống chủ động cảnh báo cho Quản lý HTX (`VT-02`, `VT-01` xem toàn nền tảng) **trước khi** khóa truy cập của đối tác hết hạn hoặc chạm hạn mức, thay vì chỉ báo lỗi khi đối tác đã bị chặn:

| Kích hoạt | Điều kiện (mặc định, cấu hình được) | Hành động |
|---|---|---|
| Sắp hết hạn | `0 < expiresAt - now <= 7 ngày`, `status = ACTIVE` | 1 thông báo/ngày/khóa + mục trên `/alerts` |
| Đã hết hạn | `expiresAt <= now`, `status = ACTIVE` | Persist `status = EXPIRED` (sửa dứt điểm việc key quá hạn vẫn `ACTIVE`) + 1 thông báo |
| Sắp chạm hạn mức | lượt gọi trong giờ hiện tại `>= 80% rateLimitPerHour` | 1 thông báo/giờ/khóa (bắn đúng 1 lần khi chạm ngưỡng) |
| Bỏ qua | `status = REVOKED/EXPIRED` | Không quét, không cảnh báo (TC-03) |
| Chống trùng | Đã có thông báo cùng `entityId` trong ngày (hết hạn) / trong giờ (hạn mức) | Không tạo thêm (TC-04) |

Cấu hình (`application.properties`):

```properties
app.apikey.expiry-warning-days=7
app.apikey.quota-warning-ratio=0.8
```

---

## 2. Endpoint thay đổi

### 2.1. GET /api/v1/organization/api-keys (sửa contract phân trang)

Danh sách khóa của tổ chức hiện tại. **Sửa lỗi thiếu `totalElements/totalPages`**: trước đây trả Spring `Page` dạng rút gọn `{content, page: {...}}` khiến FE luôn đọc `Tổng số = 0`; nay trả DTO tường minh (theo mẫu `AggregateAlertPageResponse`).

- **Quyền:** `VT-01`, `VT-02` (giữ nguyên).
- **Query:** `status` (`ACTIVE`/`REVOKED`/`EXPIRED`), `page`, `size` (giữ nguyên). Sau khi scheduler persist `EXPIRED`, filter `EXPIRED` hoạt động đúng ở tầng DB.
- **Response 200:**

```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "id": "00000000-0000-0000-0000-001200000001",
        "organizationId": "aaa00001-0000-0000-0000-000000000001",
        "partnerName": "Đối tác Test Sắp Hết Hạn (TC-01)",
        "keyPrefix": "nks_live_710exp01",
        "rateLimitPerHour": 100,
        "expiresAt": "2026-09-20T23:17:34",
        "status": "ACTIVE",
        "isTest": false,
        "totalCalls": 0,
        "failedCalls": 0,
        "lastCalledAt": null,
        "createdByName": "Quản lý HTX A",
        "createdAt": "2026-09-15T23:17:34"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 4,
    "totalPages": 1
  }
}
```

> Ghi chú: `status` trong list vẫn giữ ánh xạ ảo — key `ACTIVE` nhưng đã quá `expiresAt` được trả `EXPIRED` (không persist tại lúc đọc; persist do scheduler đảm nhiệm).

### 2.2. GET /api/v1/alerts/aggregate (thêm 2 nguồn realtime, không persist)

Bổ sung 2 loại cảnh báo tính realtime từ `partner_api_keys` (tự đóng khi gia hạn/thu hồi, không cần resolve tay):

| `type` mới | `typeName` | `severity` | `actionUrl` |
|---|---|---|---|
| `API_KEY_EXPIRING` | Khóa truy cập sắp hết hạn | `MEDIUM` | `/integration/api-keys` |
| `API_KEY_QUOTA_WARNING` | Khóa truy cập sắp chạm hạn mức | `MEDIUM` (`HIGH` khi đã vượt 100% và bị 429) | `/integration/api-keys` |

- **Quyền/phạm vi:** giữ nguyên QTN-01 (`VT-01` toàn nền tảng + cột tổ chức, `VT-02` chỉ org mình).
- Ví dụ item:

```json
{
  "type": "API_KEY_EXPIRING",
  "typeName": "Khóa truy cập sắp hết hạn",
  "severity": "MEDIUM",
  "title": "Khóa truy cập sắp hết hạn",
  "message": "Khóa của đối tác \"Đối tác Test Sắp Hết Hạn (TC-01)\" còn 5 ngày (hết hạn 20/09/2026).",
  "relatedEntityType": "PARTNER_API_KEY",
  "relatedEntityId": "00000000-0000-0000-0000-001200000001",
  "relatedEntityName": "nks_live_710exp01",
  "actionUrl": "/integration/api-keys",
  "status": "OPEN"
}
```

### 2.3. Thông báo đẩy (dùng hạ tầng NCL-08-CN-005, không endpoint mới)

- Kênh duy nhất: `GET /api/v1/notifications`, `GET /api/v1/notifications/unread-count`, `PATCH /api/v1/notifications/{id}/read` (giữ nguyên).
- Payload: `type = ALERT`, `entityId = apiKeyId`, `isRead = false`. FE bấm vào mở popup chi tiết + nút `Xem khóa → /integration/api-keys` (không tạo route/trang mới).
- Mẫu tiêu đề/nội dung:
  - Hết hạn: `Khóa truy cập sắp hết hạn` / `Khóa truy cập của đối tác "<partnerName>" sẽ hết hạn vào <dd/MM/yyyy HH:mm>. Vui lòng gia hạn để đối tác không bị gián đoạn kết nối. Xem chi tiết tại Quản trị khóa truy cập.`
  - Hạn mức: `Khóa truy cập sắp chạm hạn mức` / `Khóa của đối tác "<partnerName>" đã dùng <used>/<limit> lượt gọi trong giờ hiện tại (đạt 80%). Vui lòng nâng hạn mức hoặc chờ sang giờ tiếp theo.`

---

## 3. Ánh xạ Acceptance Criteria

- **TC-01:** key còn 5 ngày → sau lần quét → VT-02 có notification + mục `API_KEY_EXPIRING` trên `/alerts` kèm lối tắt.
- **TC-02:** key 10 lượt/giờ, gọi 8 lượt → notification `API_KEY_QUOTA_WARNING`; lượt 11 → `429` như cũ (QTN-20 không đổi).
- **TC-03:** key `REVOKED` → quét bỏ qua, không notification, không item aggregate.
- **TC-04:** quét lần 2 cùng ngày/giờ → không trùng (check `entityId` + khoảng thời gian).

---

## 4. Tương thích ngược

- FE cũ đọc `data.content` vẫn đúng; chỉ bổ sung `page/size/totalElements/totalPages` ngang hàng (trước đây nằm trong `data.page`, nay dời lên top-level theo contract này — FE NCL-12-CN-005 đọc top-level).
- Không thêm endpoint, không thêm cột DB, không đổi payload tạo/thu hồi khóa.
