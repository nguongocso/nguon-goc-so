# API Docs – Bảng điều khiển mức độ sử dụng nền tảng theo tổ chức (NCL-07-CN-008)

**Tên nhánh:** `feature/NCL-07-CN-008-organization-usage`

Dashboard dành cho Quản trị viên nền tảng (VT-01) theo dõi mức độ sử dụng
của từng tổ chức: hợp tác xã nào đang dùng thật, hợp tác xã nào bỏ dở và cần hỗ trợ.

---

## 1. Lấy dữ liệu mức độ sử dụng theo tổ chức

### Thông tin API

| Thuộc tính   | Giá trị                                      |
|--------------|----------------------------------------------|
| **Method**   | `GET`                                        |
| **Endpoint** | `/api/v1/reports/organization-usage`         |
| **Quyền**    | `VT-01` (chỉ Quản trị viên nền tảng)         |

* `VT-01`: được phép truy cập, xem toàn hệ thống hoặc lọc một tổ chức.
* Mọi vai trò khác (`VT-02`, `VT-03`, `VT-04`, `VT-05`): `403 Forbidden`.
  Không tái sử dụng phân quyền của Production Lot Dashboard
  (API đó cho phép `VT-02`).

---

### Request

**Query Parameters:**

| Parameter        | Kiểu   | Bắt buộc | Mặc định         | Mô tả                                                        |
|------------------|--------|----------|------------------|--------------------------------------------------------------|
| `startDate`      | Date   | Không    | 30 ngày gần nhất | Định dạng `yyyy-MM-dd`. Ngày bắt đầu kỳ hiện tại.            |
| `endDate`        | Date   | Không    | Hôm nay          | Định dạng `yyyy-MM-dd`. Ngày kết thúc kỳ hiện tại.           |
| `organizationId` | UUID   | Không    | Tất cả           | Lọc một tổ chức cụ thể. ID không tồn tại trả danh sách rỗng. |

**Ngữ nghĩa ngày (Date semantics):**

* Kỳ hiện tại: `[startDate 00:00:00, endDate 23:59:59]`
  (múi giờ nghiệp vụ `Asia/Ho_Chi_Minh`, thống nhất với backend/database).
* Kỳ trước có độ dài tương đương kỳ hiện tại và kết thúc vào ngày liền
  trước kỳ hiện tại. Ví dụ kỳ hiện tại `01/09 -> 30/09` (30 ngày) thì kỳ
  trước là `02/08 -> 31/08` (30 ngày). Kỳ tùy chỉnh 10 ngày
  `10/08 -> 19/08` thì kỳ trước là `31/07 -> 09/08`.
* `startDate > endDate` trả `400` với message tiếng Việt.

---

### Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "startDate": "2026-09-01",
    "endDate": "2026-09-30",
    "previousStartDate": "2026-08-02",
    "previousEndDate": "2026-08-31",
    "totalOrganizations": 3,
    "items": [
      {
        "organizationId": "a9f8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4d",
        "organizationCode": "HTX001",
        "organizationName": "Hợp tác xã Chè Tân Cương",
        "organizationType": "COOPERATIVE",
        "organizationStatus": "ACTIVE",
        "createdAt": "2026-01-10T08:00:00",
        "hasData": true,
        "lastActivityAt": "2026-09-28T15:30:00",
        "needsSupport": false,
        "productionLots": { "current": 10, "previous": 8, "change": 2, "changePercent": 25.0 },
        "farmLogs": { "current": 35, "previous": 30, "change": 5, "changePercent": 16.67 },
        "chainEvents": { "current": 20, "previous": 22, "change": -2, "changePercent": -9.09 },
        "activatedLabels": { "current": 500, "previous": 0, "change": 500, "changePercent": null },
        "publicLookups": { "current": 120, "previous": 100, "change": 20, "changePercent": 20.0 },
        "activeUsers": { "current": 5, "previous": 4, "change": 1, "changePercent": 25.0 }
      }
    ]
  },
  "timestamp": "2026-09-14T03:00:00Z"
}
```

**Response schema:**

| Trường | Kiểu | Mô tả |
|---|---|---|
| `startDate` / `endDate` | Date | Kỳ hiện tại đã chuẩn hóa (đã áp dụng mặc định khi không truyền). |
| `previousStartDate` / `previousEndDate` | Date | Kỳ trước tính từ kỳ hiện tại. |
| `totalOrganizations` | Integer | Số tổ chức trong `items`. |
| `items[].organizationId/Code/Name/Type/Status` | — | Định danh tổ chức. |
| `items[].createdAt` | DateTime | Thời điểm tổ chức được tạo (phân biệt "chưa tồn tại trong kỳ"). |
| `items[].hasData` | Boolean | `false` = "Chưa có dữ liệu" cho kỳ được chọn. |
| `items[].lastActivityAt` | DateTime/null | Hoạt động gần nhất từ mọi nguồn thực tế. |
| `items[].needsSupport` | Boolean | `true` = "Cần liên hệ hỗ trợ" (không hoạt động 30 ngày). |
| `items[].<metric>.current` | Long | Giá trị kỳ hiện tại. |
| `items[].<metric>.previous` | Long | Giá trị kỳ trước. |
| `items[].<metric>.change` | Long | `current - previous`. |
| `items[].<metric>.changePercent` | Double/null | `(change / previous) * 100`, `null` khi `previous == 0`. |

---

### 6 chỉ số — nguồn số liệu (không đoán data source)

| Chỉ số | Entity / Bảng | Quan hệ tổ chức | Mốc thời gian | Điều kiện tính |
|---|---|---|---|---|
| `productionLots` — Số lô sản xuất tạo mới | `ProductionLot` / `production_lot` | Trực tiếp `organization_id` | `created_at` (không dùng `planting_date`) | Mọi bản ghi tạo trong kỳ |
| `farmLogs` — Số mục nhật ký | `FarmLog` / `farm_logs` | Gián tiếp qua `production_lot.organization_id` | `created_at` | Mọi mục ghi trong kỳ |
| `chainEvents` — Số sự kiện chuỗi | `ChainEvent` / `chain_events` | Tổ chức sở hữu lô hàng; sự kiện chưa gắn lô hàng thì theo tổ chức đã ghi | `created_at` | `is_correction = false` |
| `activatedLabels` — Số tem kích hoạt | `TraceCode` / `trace_codes` | Gián tiếp qua `shipment.organization_id` | `activated_at` (không lọc `status` hiện tại vì tem còn chuyển sang SUSPECT/LOCKED/RECALLED sau kích hoạt) | `activated_at` trong kỳ |
| `publicLookups` — Số lượt tra cứu công khai | `TraceCodeScanLog` / `trace_code_scan_logs` | Gián tiếp qua tem → lô hàng → tổ chức | `scanned_at` | Mọi lượt quét trong kỳ (gồm cả quét bất thường) |
| `activeUsers` — Số người dùng hoạt động | `ActivityLog` / `activity_logs` | Trực tiếp `organization_id` | `created_at` | `COUNT(DISTINCT user_id)` — người dùng có hoạt động thật, không phải tổng số tài khoản |

---

### Hành vi `previous = 0`

* `previous == 0`, `current > 0`: `change = current`, `changePercent = null`.
  Frontend hiển thị giá trị kèm mũi tên lên và `+100.0%` (quy ước: tăng từ con số 0 = tăng 100%). Không trả `Infinity`/`NaN`.
* `current == 0`, `previous == 0`: `change = 0`, `changePercent = null`.
  Frontend hiển thị `− 0.0%` (không tăng không giảm).
* `hasData == false`: Frontend hiển thị `—` (chưa có dữ liệu kỳ nào).

```json
"activatedLabels": { "current": 500, "previous": 0, "change": 500, "changePercent": null }
```

---

### Response `200 OK` – Tổ chức chưa có dữ liệu (AC-04)

Tổ chức mới tạo trong ngày nhưng kỳ được chọn không có hoạt động (hoặc tổ
chức được tạo sau kỳ được chọn) không hiển thị như "0 activity" sai ngữ
nghĩa mà có `hasData = false`. Frontend hiển thị "Chưa có dữ liệu",
phân biệt với tổ chức đã tồn tại nhưng không hoạt động.

```json
{
  "success": true,
  "status": 200,
  "data": {
    "startDate": "2026-09-01",
    "endDate": "2026-09-30",
    "previousStartDate": "2026-08-02",
    "previousEndDate": "2026-08-31",
    "totalOrganizations": 1,
    "items": [
      {
        "organizationId": "b2c8e7d6-c5b4-a3f2-e1d0-9c8b7a6b5c4e",
        "organizationCode": "HTX099",
        "organizationName": "Hợp tác xã mới thành lập",
        "organizationType": "COOPERATIVE",
        "organizationStatus": "ACTIVE",
        "createdAt": "2026-09-14T08:00:00",
        "hasData": false,
        "lastActivityAt": null,
        "needsSupport": true,
        "productionLots": { "current": 0, "previous": 0, "change": 0, "changePercent": null },
        "farmLogs": { "current": 0, "previous": 0, "change": 0, "changePercent": null },
        "chainEvents": { "current": 0, "previous": 0, "change": 0, "changePercent": null },
        "activatedLabels": { "current": 0, "previous": 0, "change": 0, "changePercent": null },
        "publicLookups": { "current": 0, "previous": 0, "change": 0, "changePercent": null },
        "activeUsers": { "current": 0, "previous": 0, "change": 0, "changePercent": null }
      }
    ]
  },
  "timestamp": "2026-09-14T03:00:00Z"
}
```

---

### Ngữ nghĩa `lastActivityAt` / `needsSupport` (AC-02)

* `lastActivityAt` = max của: lô mới nhất (`production_lot.created_at`),
  nhật ký mới nhất (`farm_logs.created_at`), sự kiện mới nhất
  (`chain_events.created_at`), tem kích hoạt mới nhất
  (`trace_codes.activated_at`), tra cứu mới nhất
  (`trace_code_scan_logs.scanned_at`), hoạt động mới nhất
  (`activity_logs.created_at`).
* `needsSupport = true` khi `lastActivityAt == null` (chưa từng có hoạt
  động) hoặc hoạt động gần nhất đã từ đủ 30 ngày trở lên
  (so với thời điểm hiện tại theo múi giờ `Asia/Ho_Chi_Minh`).
* Frontend hiển thị cảnh báo "Cần liên hệ hỗ trợ" cho tổ chức này.

---

### Lỗi thường gặp

#### 1. Vai trò khác VT-01 (AC-03)

**HTTP Status: 403 Forbidden**

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện chức năng này",
  "errors": "ACCESS_DENIED",
  "path": "/api/v1/reports/organization-usage",
  "timestamp": "2026-09-14T03:05:00Z"
}
```

#### 2. Chưa đăng nhập hoặc token hết hạn

**HTTP Status: 403 Forbidden** (theo convention anonymous của hệ thống —
request không có JWT hợp lệ bị từ chối ở tầng bảo mật).

#### 3. Khoảng thời gian không hợp lệ

**HTTP Status: 400 Bad Request**

```json
{
  "success": false,
  "status": 400,
  "message": "Khoảng thời gian không hợp lệ: từ ngày phải trước hoặc bằng đến ngày.",
  "path": "/api/v1/reports/organization-usage",
  "timestamp": "2026-09-14T03:05:00Z"
}
```

---

## 2. Xuất báo cáo mức độ sử dụng theo kỳ

### Thông tin API

| Thuộc tính   | Giá trị                                      |
|--------------|----------------------------------------------|
| **Method**   | `GET`                                        |
| **Endpoint** | `/api/v1/reports/organization-usage/export`  |
| **Quyền**    | `VT-01` (vai trò khác: `403 Forbidden`)      |
| **Định dạng**| CSV (UTF-8 có BOM, mở đúng tiếng Việt trong Excel) |

**Query Parameters:** `startDate`, `endDate`, `organizationId` (giống API dashboard).

**Response `200 OK`:** file đính kèm
`Bao_cao_muc_do_su_dung_yyyyMMdd.csv` (`Content-Type: text/csv`),
mỗi tổ chức một dòng gồm: mã/tên/loại/trạng thái/ngày tạo tổ chức, kỳ
hiện tại, kỳ trước, 6 chỉ số (hiện tại/kỳ trước/thay đổi/% thay đổi),
hoạt động gần nhất, trạng thái cần hỗ trợ, trạng thái dữ liệu.

---

## 3. Quy tắc nghiệp vụ (Business Rules)

* **Phân quyền VT-01 duy nhất:** chỉ Quản trị viên nền tảng được truy cập
  dashboard và export. Backend enforce bằng
  `@PreAuthorize("hasRole('VT-01')")` ở cấp controller; frontend chỉ ẩn
  menu/route, không phải lớp bảo mật duy nhất.
* **So sánh kỳ:** kỳ trước luôn có độ dài tương đương kỳ hiện tại, không
  hard-code "tháng trước".
* **Không hoạt động 30 ngày:** tổ chức không có bất kỳ hoạt động nào
  (tổng hợp từ cả 6 nguồn) trong 30 ngày gần nhất được đánh dấu
  `needsSupport = true` ("Cần liên hệ hỗ trợ").
* **Tổ chức mới/chưa có dữ liệu:** `hasData = false` khi không có hoạt
  động nào trong cả hai kỳ (hoặc tổ chức được tạo sau kỳ được chọn);
  frontend hiển thị "Chưa có dữ liệu".

---

## 4. Tiêu chí nghiệm thu (Acceptance Criteria)

* **AC-01:** Dashboard hiển thị đủ 6 chỉ số cho từng tổ chức, mỗi chỉ số có
  current/previous/change (không chỉ current value).
* **AC-02:** Tổ chức không có hoạt động nào trong 30 ngày được đánh dấu
  "Cần liên hệ hỗ trợ" dựa trên `lastActivityAt` tổng hợp.
* **AC-03:** VT-01 truy cập được; VT-02 trả HTTP 403 (backend enforce).
* **AC-04:** Tổ chức mới/chưa có dữ liệu trong kỳ hiển thị "Chưa có dữ liệu",
  không hiển thị sai ngữ nghĩa như "0 activity".
