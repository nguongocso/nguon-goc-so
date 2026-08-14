# 📘 API Docs: Xuất hồ sơ truy xuất theo lược đồ mô phỏng chuẩn GS1
## NCL-12-CN-003

---

## 1. Thông tin chung

| Thuộc tính | Giá trị |
|---|---|
| **User Story** | NCL-12-CN-003 - Xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1 |
| **Epic** | NCL-12 - Cổng dữ liệu và hồ sơ theo lược đồ chuẩn |
| **Git Branch** | `feature/NCL-12-CN-003_export_dossier_gs1_schema` |
| **Vai trò được phép** | VT-02 (Quản lý HTX), VT-04 (Doanh nghiệp thu mua) |
| **HTTP Method** | `GET` |
| **Mục đích** | Xuất hồ sơ truy xuất của Shipment theo lược đồ GS1 mô phỏng |
| **GS1 Compliance** | Không phải chứng nhận hoặc triển khai tuân thủ chính thức GS1 |
| **Database Migration** | Không cần |

### Mô tả

Cho phép VT-02 / VT-04 xuất hồ sơ truy xuất của một `Shipment` theo lược đồ dữ liệu mô phỏng hướng chuẩn GS1. Hồ sơ bao gồm: thông tin Shipment, danh sách `ChainEvent` (sorted `recordedAt ASC`), bảng ánh xạ schema (`includeMapping`), cảnh báo dữ liệu thiếu, và thông tin `exportedAt` / `exportedBy`.

> **Lưu ý:** Đây là **schema mô phỏng phục vụ học tập/demo**, không phải GS1 Digital Link, EPCIS hoặc chứng nhận GS1 compliance.

---

## 2. Endpoint

```http
GET /api/v1/shipments/{shipmentId}/dossier/gs1
```

### Path Parameter

| Parameter    | Type | Required | Description                    |
| ------------ | ---- | -------: | ------------------------------ |
| `shipmentId` | UUID |      Yes | ID của Shipment cần xuất hồ sơ |

### Query Parameters

| Parameter        | Type    | Required | Default | Description                             |
| ---------------- | ------- | -------: | ------- | --------------------------------------- |
| `format`         | string  |       No | `json`  | Định dạng xuất: `json` hoặc `xml`       |
| `includeMapping` | boolean |       No | `true`  | Có bao gồm bảng ánh xạ schema hay không |

### Ví dụ

```http
GET /api/v1/shipments/{shipmentId}/dossier/gs1?format=json      # → application/json
GET /api/v1/shipments/{shipmentId}/dossier/gs1?format=xml       # → application/xml
```

---

## 3. Authorization

Endpoint yêu cầu authentication (JWT ACCESS).

Role được phép: `VT-02`, `VT-04`. Các role khác bị từ chối `403`.

Authorization dựa trên authenticated principal và cơ chế security hiện có; không cho phép frontend truyền role để bypass.

### Authorization flow

```text
Request → Authentication → Role check (VT-02/VT-04)
  ├── role khác → 403
  → Find Shipment ├── không tồn tại → 404
  → validateDossierAccess (scope tổ chức)
  → checkEligibility (QTN-11)
  → Kiểm tra Shipment có ChainEvent không ├── không có → 400
  → Export GS1 dossier (JSON/XML) → ActivityLog → 200
```

---

## 4. Điều kiện xuất hồ sơ — QTN-11

Tái sử dụng `checkEligibility` của `dossier/check`:
- Lô sản xuất `status` = `CLOSED` hoặc `PACKAGED`.
- Có ít nhất 1 chứng từ đính kèm cho mỗi hoạt động: `PLANTING`, `FERTILIZING`, `PESTICIDE`, `HARVESTING`.

Nếu Shipment không có `ChainEvent` nào → `400` (không tạo hồ sơ rỗng).

---

## 5. Event mapping — bốn chiều

```text
who   ← ChainEvent.recordedBy.fullName
when  ← ChainEvent.recordedAt
where ← ChainEvent.location (JTS Point → latitude/longitude)
why/what ← ChainEvent.eventType + ChainEvent.eventData (details)
```

### Bảng ánh xạ chuẩn (mapping table)

| System Field                     | GS1 Simulation Field       | Ghi chú |
| -------------------------------- | -------------------------- | ------- |
| `ChainEvent.id`                  | `eventIdentifier`          | ID sự kiện |
| `ChainEvent.eventType`           | `eventTypeCode`            | Loại sự kiện (enum hiện có) |
| `ChainEvent.recordedAt`          | `eventDateTime`            | Thời điểm ghi nhận |
| `ChainEvent.recordedBy.fullName` | `actorName`                | Người ghi nhận (who) |
| `ChainEvent.location.getY()`     | `eventLocation.latitude`   | Vĩ độ |
| `ChainEvent.location.getX()`     | `eventLocation.longitude`  | Kinh độ |
| `ChainEvent.location.address`    | `eventLocation.address`    | Không lưu trong domain → null + warning |
| `ChainEvent.eventData`           | `details`                  | Dữ liệu chi tiết (why/what) |
| `Shipment.name`                  | `shipmentName`             | Tên Shipment |
| `Shipment.totalQuantity`         | `declaredQuantity`         | Số lượng (long) |
| `Shipment.status`                | `shipmentStatus`           | Trạng thái Shipment |
| `TraceCode.codeValue` (list)     | `codeValues`               | Mã truy xuất (best effort) |

> Mapping mô phỏng, không phải mapping chính thức tới GS1 EPCIS / GS1 Digital Link.

### Event location handling
- Có toạ độ → xuất `latitude`/`longitude`.
- Không có `address` → `address = null`, thêm warning.
- Không có location → `location = null`, thêm warning.
- Không tự geocode tên địa điểm thành toạ độ.

---

## 6. Response — JSON (thành công)

```http
200 OK
Content-Type: application/json
```

```json
{
  "success": true,
  "status": 200,
  "data": {
    "shipment": {
      "id": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
      "name": "Lô chè Tân Cương T8/2026",
      "codeValues": ["TANCUONG00000059"],
      "productCategory": "Chè",
      "totalQuantity": 500,
      "unit": "kg",
      "status": "PACKAGED",
      "organization": { "id": "...", "name": "HTX Chè Tân Cương", "code": "HTX-TC" }
    },
    "events": [
      {
        "eventId": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "HARVEST",
        "eventTypeLabel": "Thu hoạch",
        "recordedAt": "2026-08-11T10:00:00",
        "recordedBy": "Nguyễn Văn A",
        "location": { "latitude": 21.0285, "longitude": 105.8542, "address": null },
        "details": { "productionLotId": "...", "productionLotName": "Lô chè Tân Cương T8/2026", "quantity": 500.0, "harvestDate": "2026-08-11" }
      }
    ],
    "mapping": {
      "ChainEvent.id": "eventIdentifier",
      "ChainEvent.eventType": "eventTypeCode",
      "ChainEvent.recordedAt": "eventDateTime",
      "ChainEvent.recordedBy.fullName": "actorName",
      "ChainEvent.location.latitude": "eventLocation.latitude",
      "ChainEvent.location.longitude": "eventLocation.longitude",
      "ChainEvent.location.address": "eventLocation.address",
      "ChainEvent.eventData": "details",
      "Shipment.name": "shipmentName",
      "Shipment.totalQuantity": "declaredQuantity",
      "Shipment.status": "shipmentStatus",
      "TraceCode.codeValue": "codeValues"
    },
    "warnings": [
      { "eventId": "c3d4e5f6-3333-4a2a-9f3d-1a2b3c4d5e6f", "field": "location", "message": "Sự kiện thiếu thông tin vị trí" }
    ],
    "exportedAt": "2026-08-12T10:00:00",
    "exportedBy": "Nguyễn Văn C",
    "schemaVersion": "1.0.0",
    "schemaDescription": "Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ GS1"
  },
  "timestamp": "2026-08-12T10:00:01.123Z"
}
```

---

## 7. Response — XML

Khi `format=xml`, response là root `<gs1Dossier>` (không bọc `ApiResult`) mang cùng semantic data:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gs1Dossier>
  <shipment>
    <id>9c8b7a6f-...</id>
    <name>Lô chè Tân Cương T8/2026</name>
    <codeValues><codeValue>TANCUONG00000059</codeValue></codeValues>
    <productCategory>Chè</productCategory>
    <totalQuantity>500</totalQuantity>
    <unit>kg</unit>
    <status>PACKAGED</status>
    <organization><id>...</id><name>HTX Chè Tân Cương</name><code>HTX-TC</code></organization>
  </shipment>
  <events>
    <event>
      <eventId>a1b2c3d4-...</eventId>
      <eventType>HARVEST</eventType>
      <eventTypeLabel>Thu hoạch</eventTypeLabel>
      <recordedAt>2026-08-11T10:00:00</recordedAt>
      <recordedBy>Nguyễn Văn A</recordedBy>
      <location><latitude>21.0285</latitude><longitude>105.8542</longitude><address/></location>
    </event>
  </events>
  <mapping>...</mapping>
  <warnings>
    <warning><eventId>c3d4e5f6-...</eventId><field>location</field><message>Sự kiện thiếu thông tin vị trí</message></warning>
  </warnings>
  <exportedAt>2026-08-12T10:00:00</exportedAt>
  <exportedBy>Nguyễn Văn C</exportedBy>
  <schemaVersion>1.0.0</schemaVersion>
  <schemaDescription>Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ GS1</schemaDescription>
</gs1Dossier>
```

> JSON và XML biểu diễn **cùng một tập dữ liệu và cùng business semantics**.

---

## 8. Error Responses

### 8.1. Shipment không tồn tại — TC-04 (`404`)
```json
{
  "success": false, "status": 404,
  "message": "Không tìm thấy thông tin lô hàng.",
  "path": "/api/v1/shipments/{id}/dossier/gs1", "timestamp": "..."
}
```

### 8.2. Không có quyền — TC-03 (`403`, role hoặc organization)
```json
{
  "success": false, "status": 403,
  "message": "Từ chối thao tác: Bạn không có quyền xem hoặc xuất hồ sơ cho lô hàng này.",
  "path": "/api/v1/shipments/{id}/dossier/gs1", "timestamp": "..."
}
```

### 8.3. Không đủ QTN-11 — TC-05 (`400`)
```json
{
  "success": false, "status": 400,
  "message": "Không đủ điều kiện xuất hồ sơ truy xuất: Lô hàng chưa hoàn tất hoặc thiếu chứng từ bắt buộc.",
  "errors": ["Thiếu chứng từ bón phân (FERTILIZING)"],
  "path": "/api/v1/shipments/{id}/dossier/gs1", "timestamp": "..."
}
```

### 8.4. Không có event — TC-06 (`400`)
```json
{
  "success": false, "status": 400,
  "message": "Lô chưa có sự kiện nào để xuất hồ sơ.",
  "path": "/api/v1/shipments/{id}/dossier/gs1", "timestamp": "..."
}
```

### 8.5. Format không hợp lệ — TC-09 (`400`, vd `format=pdf`)
```json
{
  "success": false, "status": 400,
  "message": "Định dạng xuất không được hỗ trợ. Chỉ hỗ trợ json hoặc xml.",
  "path": "/api/v1/shipments/{id}/dossier/gs1", "timestamp": "..."
}
```

---

## 9. DTO — Backend

### `Gs1DossierExportResponse`
```java
@JacksonXmlRootElement(localName = "gs1Dossier")
public class Gs1DossierExportResponse {
    private Gs1ShipmentInfo shipment;
    private List<Gs1Event> events;
    private Map<String, String> mapping;
    private List<Gs1Warning> warnings;
    private LocalDateTime exportedAt;
    private String exportedBy;
    private String schemaVersion;
    private String schemaDescription;
}
```

### `Gs1Event`
```java
public class Gs1Event {
    private UUID eventId;
    private String eventType;
    private String eventTypeLabel;
    private LocalDateTime recordedAt;
    private String recordedBy;
    private Gs1EventLocation location;
    private Map<String, Object> details;
}
```

### `Gs1EventLocation`
```java
public class Gs1EventLocation {
    private Double latitude;    // ChainEvent.location.getY()
    private Double longitude;   // ChainEvent.location.getX()
    private String address;     // null (không tồn tại trong domain)
}
```

### `Gs1Warning`
```java
public class Gs1Warning {
    private UUID eventId;
    private String field;
    private String message;
}
```

---

## 10. `includeMapping`

- `true` (mặc định): response có `mapping`.
- `false`: `mapping = null`. Không ảnh hưởng đến `events`.

---

## 11. Warnings & Audit

- Warnings không làm request thất bại (HTTP vẫn `200`).
- `exportedBy` = authenticated principal; `exportedAt` = server time (không nhận từ client).
- Mỗi lần export thành công ghi `ActivityLogEvent`:
  ```text
  Action: GS1_DOSSIER_EXPORT
  Target: Shipment
  Target ID: shipmentId
  Actor: authenticated user
  ```

---

## 12. Event ordering

Các event lấy theo `recordedAt ASC` (`chainEventRepository.findByShipment_IdOrderByRecordedAtAsc`).

---

## 13. Business Rules

| Rule | Description |
|---|---|
| QTN-11 | Shipment phải đáp ứng điều kiện export (`checkEligibility`) |
| Authorization | Chỉ VT-02 / VT-04 được export |
| Organization access | User phải có quyền truy cập Shipment (`validateDossierAccess`) |
| No events | Không có ChainEvent → 400 |
| Event ordering | ChainEvent sort `recordedAt ASC` |
| Four dimensions | Event ánh xạ `who / when / where / why` |
| Missing location | Thiếu location không làm export thất bại; tạo warning |
| No fake data | Không tự sinh dữ liệu (address, geocode) |
| Read-only | Không sửa Shipment/ChainEvent |
| Mapping | `includeMapping=true` mặc định |
| JSON / XML | `format=json` (default) / `format=xml` |
| Audit | Export thành công ghi ActivityLog (`GS1_DOSSIER_EXPORT`) |
| Schema | Không thay đổi database schema; không tạo migration |
| GS1 | Chỉ là schema mô phỏng, không phải GS1 compliance |

---

## 14. Files thay đổi / tạo mới

### Backend — tạo mới
```text
report/dto/response/Gs1DossierExportResponse.java
report/dto/response/Gs1Event.java
report/dto/response/Gs1EventLocation.java
report/dto/response/Gs1ShipmentInfo.java
report/dto/response/Gs1Warning.java
```

### Backend — mở rộng
```text
report/service/DossierService.java
report/service/impl/DossierServiceImpl.java
report/controller/DossierController.java
```

### Frontend
```text
src/api/dossierApi.ts
src/config/roleAccess.ts
src/pages/shipment/ShipmentList.tsx (VT-02 trigger)
src/components/shipment/ProcurementShipmentList.tsx (VT-04 trigger)
```

### Tài liệu
```text
docs/api/report/NCL-12-CN-003_ExportTraceabilityDossierGS1.md
```

### Migration
```text
KHÔNG CẦN
```

---

## 15. Test Cases

| TC | Scenario | Expected |
|---|---|---|
| TC-01 | Shipment đủ điều kiện + có ChainEvent | 200, dossier export |
| TC-02 | Một event thiếu location | 200, location=null, warning |
| TC-03 | Role không được phép (VT-01/03/05) | 403 |
| TC-04 | Shipment không tồn tại | 404 |
| TC-05 | Shipment không đủ QTN-11 | 400, liệt kê missing |
| TC-06 | Shipment không có ChainEvent | 400 |
| TC-07 | format=json | 200, application/json |
| TC-08 | format=xml | 200, application/xml |
| TC-09 | format=pdf | 400 |
| TC-10 | includeMapping=true | Response có mapping |
| TC-11 | includeMapping=false | Response không có mapping |
| TC-12 | ChainEvent nhiều timestamp | Events sort recordedAt ASC |
| TC-13 | Export thành công | ActivityLog tạo (`GS1_DOSSIER_EXPORT`) |
| TC-14 | exportedBy | Lấy từ authenticated user |
| TC-15 | exportedAt | Lấy từ server time |
| TC-16 | Không có location/address | Export thành công + warning |
| TC-17 | Procurement event | Export đúng ChainEvent hiện có |
| TC-18 | Transport event | Export đúng ChainEvent, không tự tạo coordinates |

---

## 16. Implementation Constraints

1. Không thay đổi database schema.
2. Không tạo Flyway migration.
3. Không thay đổi `ChainEvent`.
4. Không thay đổi `Shipment`.
5. Không tạo event type mới.
6. Không tạo dữ liệu giả.
7. Không tự geocode địa chỉ thành coordinates.
8. Không thay đổi logic PROCUREMENT / TRANSPORT / farming logs.
9. Không thay đổi authorization hiện tại ngoài việc thêm endpoint.
10. `exportedBy` lấy từ authenticated principal; `exportedAt` lấy từ server.
11. JSON và XML biểu diễn cùng semantic data.
12. Sử dụng exception/error response convention hiện có.
13. Sử dụng ActivityLog/audit mechanism hiện có.
14. Tái sử dụng `DossierService.checkEligibility` và `validateDossierAccess`.

---

## 17. Definition of Done

- [ ] Endpoint `GET /api/v1/shipments/{id}/dossier/gs1` tồn tại.
- [ ] VT-02 được phép; VT-04 được phép; role khác bị 403.
- [ ] Shipment không tồn tại → 404.
- [ ] Shipment không đủ QTN-11 → 400.
- [ ] Shipment không có ChainEvent → 400.
- [ ] ChainEvent sort `recordedAt ASC`, map thành Gs1Event (who/when/where/why).
- [ ] Missing location → warning; không tạo fake location/address.
- [ ] `exportedBy` từ authenticated user; `exportedAt` từ server.
- [ ] JSON export hoạt động; XML export hoạt động.
- [ ] `includeMapping=true` / `false` hoạt động.
- [ ] ActivityLog được ghi sau export thành công.
- [ ] Không có database migration.
- [ ] Không thay đổi Shipment/ChainEvent business behavior.
- [ ] API `exportGs1Dossier` trong `dossierApi.ts`.
- [ ] Role permission `gs1DossierExport` (`VT-02`, `VT-04`).
- [ ] Nút "Xuất hồ sơ GS1" trên `ShipmentList` (VT-02).
- [ ] Nút "Xuất hồ sơ GS1" trên `ProcurementShipmentList` (VT-04).

---

## Author

```text
Task: NCL-12-CN-003
Branch: feature/NCL-12-CN-003_export_dossier_gs1_schema
```
