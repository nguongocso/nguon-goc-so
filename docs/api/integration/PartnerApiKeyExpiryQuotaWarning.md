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
| Sắp chạm hạn mức | `call_count` trong ngày hôm nay (bảng `partner_api_key_daily_usage`) `>= ceil(rateLimitPerHour × 0.8)` | 1 thông báo/ngày/khóa, gửi ngay tại lượt gọi chạm ngưỡng |
| Đối soát hạn mức (job) | Mỗi giờ quét usage hôm nay **chưa** gửi cảnh báo mà đã vượt ngưỡng | Gửi bù đúng 1 thông báo/ngày/khóa (bù khi restart / nhiều instance / bộ đếm vượt ngưỡng) |
| Bỏ qua | `status = REVOKED/EXPIRED` | Không quét, không cảnh báo (TC-03) |
| Chống trùng | Hạn mức: claim nguyên tử trên `partner_api_key_daily_usage.warning_sent_at` (`UPDATE ... WHERE warning_sent_at IS NULL`). Hết hạn: thông báo cùng `entityId` trong ngày | Không tạo thêm (TC-04), an toàn khi nhiều instance (TC-06) |

Cấu hình (`application.properties`):

```properties
app.apikey.expiry-warning-days=7
app.apikey.quota-warning-ratio=0.8
app.apikey.quota-scan-cron=0 30 * * * ?
```

Ghi chú vận hành:

- **Usage theo ngày được lưu ở DB**, không còn ở bộ nhớ tạm (JVM) ⇒ cảnh báo hạn mức và mục tương ứng trên `/alerts` **không mất khi khởi động lại backend**; nhiều instance dùng chung một nguồn đếm nên không gửi trùng (TC-06).
- **Rate limit theo giờ (QTN-20) không đổi**: vẫn đếm theo cửa sổ giờ và trả `429` khi vượt `rateLimitPerHour`. Lượt bị `429` **không** tính vào usage ngày (giữ nguyên cơ chế "chặn trước – đếm sau" hiện hành).
- Job đối soát mặc định **mỗi giờ** (phút 30) vì `01:00` là thời điểm **sang ngày mới** (usage hôm nay = 0): nếu chỉ quét hằng ngày đúng `01:00` sẽ không phát hiện được usage của chính ngày hôm đó.

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
        "partnerName": "Công ty TNHH Nông sản Bình Minh",
        "keyPrefix": "nks_live_710exp01",
        "rateLimitPerHour": 100,
        "expiresAt": "2026-09-20T23:17:34",
        "status": "ACTIVE",
        "isTest": false,
        "totalCalls": 128,
        "failedCalls": 2,
        "usedCallsToday": 8,
        "quotaWarningThreshold": 80,
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

- **Trường mới (NCL-12-CN-005):** `usedCallsToday` = số lượt gọi **hôm nay** của khóa (đọc từ bảng `partner_api_key_daily_usage`), `quotaWarningThreshold` = số lượt chạm ngưỡng cảnh báo (`ceil(rateLimitPerHour × app.apikey.quota-warning-ratio)`). FE dùng 2 trường này để hiển thị cột "Lượt gọi hôm nay" và badge "sắp chạm hạn mức" mà **không** tự hardcode tỷ lệ 0.8.

> Ghi chú: `status` trong list vẫn giữ ánh xạ ảo — key `ACTIVE` nhưng đã quá `expiresAt` được trả `EXPIRED` (không persist tại lúc đọc; persist do scheduler đảm nhiệm).

### 2.2. GET /api/v1/alerts/aggregate (thêm 2 nguồn realtime, không persist)

Bổ sung 2 loại cảnh báo tính realtime từ `partner_api_keys` (tự đóng khi gia hạn/thu hồi, không cần resolve tay):

| `type` mới | `typeName` | `severity` | `actionUrl` |
|---|---|---|---|
| `API_KEY_EXPIRING` | Khóa truy cập sắp hết hạn | `MEDIUM` (sắp hết hạn), `HIGH` (đã hết hạn nhưng DB chưa kịp persist) | `/integration/api-keys` |
| `API_KEY_QUOTA_WARNING` | Khóa truy cập sắp chạm hạn mức | `MEDIUM` | `/integration/api-keys` |

- **Quyền/phạm vi:** giữ nguyên QTN-01 (`VT-01` toàn nền tảng + cột tổ chức, `VT-02` chỉ org mình).
- Ví dụ item:

```json
{
  "type": "API_KEY_EXPIRING",
  "typeName": "Khóa truy cập sắp hết hạn",
  "severity": "MEDIUM",
  "title": "Khóa truy cập sắp hết hạn",
  "message": "Khóa của đối tác \"Công ty TNHH Nông sản Bình Minh\" còn 5 ngày (hết hạn 20/09/2026).",
  "relatedEntityType": "PARTNER_API_KEY",
  "relatedEntityId": "00000000-0000-0000-0000-001200000001",
  "relatedEntityName": "nks_live_710exp01",
  "actionUrl": "/integration/api-keys",
  "status": "OPEN"
}
```

Ví dụ item cảnh báo hạn mức:

```json
{
  "type": "API_KEY_QUOTA_WARNING",
  "typeName": "Khóa truy cập sắp chạm hạn mức",
  "severity": "MEDIUM",
  "title": "Khóa truy cập sắp chạm hạn mức",
  "message": "Khóa của đối tác \"Hợp tác xã Cà phê Tân Cương\" đã dùng 8/10 lượt gọi trong ngày hôm nay (đạt 80%, ngưỡng cảnh báo 80%).",
  "relatedEntityType": "PARTNER_API_KEY",
  "relatedEntityId": "00000000-0000-0000-0000-001200000002",
  "relatedEntityName": "nks_test_710qta02",
  "actionUrl": "/integration/api-keys",
  "status": "OPEN"
}
```

### 2.3. Thông báo đẩy (dùng hạ tầng NCL-08-CN-005, không endpoint mới)

- Kênh duy nhất: `GET /api/v1/notifications`, `GET /api/v1/notifications/unread-count`, `PATCH /api/v1/notifications/{id}/read` (giữ nguyên).
- Payload: `type = ALERT`, `entityId = apiKeyId`, `isRead = false`. FE bấm vào mở popup chi tiết + nút `Xem khóa → /integration/api-keys` (không tạo route/trang mới).
- Mẫu tiêu đề/nội dung:
  - Hết hạn: `Khóa truy cập sắp hết hạn` / `Khóa truy cập của đối tác "<partnerName>" sẽ hết hạn sau <n> ngày (vào <dd/MM/yyyy HH:mm>). Vui lòng gia hạn để đối tác không bị gián đoạn kết nối.`
  - Hạn mức: `Khóa truy cập sắp chạm hạn mức` / `Khóa truy cập của đối tác "<partnerName>" đã dùng <used>/<limit> lượt gọi trong ngày hôm nay (đạt <percent>%, ngưỡng cảnh báo <thresholdPercent>%). Vui lòng nâng hạn mức hoặc điều tiết tần suất gọi.`
- Hạn mức gửi **1 lần/ngày/khóa**; nếu lượt chạm ngưỡng xảy ra khi job/hệ thống vừa khởi động lại, job đối soát sẽ gửi bù nhưng vẫn không tạo trùng.

---

## 3. Ánh xạ Acceptance Criteria

- **TC-01:** key còn 5 ngày → sau lần quét → VT-02 có notification + mục `API_KEY_EXPIRING` trên `/alerts` kèm lối tắt.
- **TC-02:** key 10 lượt/giờ, gọi 8 lượt (tổng ngày chạm `ceil(10 × 0.8) = 8`) → notification `API_KEY_QUOTA_WARNING` gửi ngay tại lượt thứ 8; lượt 11 trong cùng giờ → `429` như cũ (QTN-20 không đổi). Kịch bản gốc trong tài liệu nghiệp vụ ghi "85% hạn mức trong giờ": khi tổng lượt trong ngày đã vượt ngưỡng 80% thì cảnh báo đã được gửi (tại mốc 80%), nên kết quả quan sát được vẫn đúng.
- **TC-03:** key `REVOKED` → quét bỏ qua, không notification, không item aggregate.
- **TC-04:** quét/gọi lại trong cùng ngày → không trùng (hạn mức: cờ `warning_sent_at`; hết hạn: `entityId` + khoảng thời gian trong ngày).
- **TC-05 (đối soát):** khóa đã vượt ngưỡng nhưng chưa gửi cảnh báo (ví dụ backend vừa khởi động lại) → job đối soát gửi bù **đúng 1** thông báo và mục `API_KEY_QUOTA_WARNING` vẫn hiển thị trên `/alerts`.
- **TC-06 (nhiều instance):** hai instance cùng ghi nhận lượt chạm ngưỡng → chỉ **1** thông báo nhờ claim nguyên tử ở DB.

---

## 4. Tương thích ngược

- FE cũ đọc `data.content` vẫn đúng; chỉ bổ sung `page/size/totalElements/totalPages` ngang hàng (trước đây nằm trong `data.page`, nay dời lên top-level theo contract này — FE NCL-12-CN-005 đọc top-level).
- Không thêm endpoint, không đổi payload tạo/thu hồi khóa. Bổ sung 2 trường **chỉ đọc** ở response danh sách (`usedCallsToday`, `quotaWarningThreshold`) — FE cũ bỏ qua an toàn.

---

## 5. Dữ liệu & Migration

- Migration mới: `backend/src/main/resources/db/migration/schema/V20260916085531__create_partner_api_key_daily_usage.sql` (version dạng timestamp theo `docs/agent/06-flyway-migration-convention.md`).
- Bảng `partner_api_key_daily_usage` — độ mịn: **một dòng cho mỗi khóa × ngày**:

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | `CHAR(36)` | Khóa chính |
| `api_key_id` | `CHAR(36)` | FK → `partner_api_keys(id)` (`ON DELETE CASCADE`) |
| `usage_date` | `DATE` | Ngày nghiệp vụ theo `app.timezone` (mặc định `Asia/Ho_Chi_Minh`) |
| `call_count` | `INT` | Số lượt gọi đã xác thực **thành công** trong ngày |
| `warning_sent_at` | `DATETIME` | Mốc gửi cảnh báo hạn mức (`NULL` = chưa gửi) — dùng làm cờ claim chống trùng |
| `created_at`, `updated_at` | `DATETIME` | Dấu vết thời gian |
| Ràng buộc | — | `UNIQUE (api_key_id, usage_date)` + index `(usage_date, warning_sent_at)` |

- Bảng này **không** expose qua endpoint nào; chỉ phục vụ tính ngưỡng cảnh báo, hiển thị "lượt gọi hôm nay" và job đối soát.

---

## 6. Endpoint gia hạn và nâng hạn mức (NCL-12-CN-005)

### PATCH /api/v1/organization/api-keys/{id}/expiry

- Body: `{"expiresAt":"2026-12-31T23:59:59"}`
- Active → cập nhật thời hạn
- Expired → cập nhật thời hạn + `status=ACTIVE`
- Revoked → từ chối
- `expiresAt` phải `> now` (`@Future`)

### PATCH /api/v1/organization/api-keys/{id}/quota

- Body: `{"rateLimitPerHour":200}`
- `rateLimitPerHour` phải `> 0` và `> current`
- Revoked → từ chối
