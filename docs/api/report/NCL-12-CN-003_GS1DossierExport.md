# 📘 API Docs: Xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1
## NCL-12-CN-003

---

## 1. Thông tin chung

| Thuộc tính | Giá trị |
|---|---|
| **User Story** | NCL-12-CN-003 - Xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1 |
| **Epic** | NCL-12 - Cổng dữ liệu và hồ sơ theo lược đồ chuẩn |
| **Git Branch** | `feature/NCL-12-CN-003-gs1-dossier-export` |
| **Vai trò được phép** | VT-02, VT-04 |
| **API Docs** | `docs/api/report/GS1DossierExport.md` |
| **HTTP Method** | `GET` |
| **Mục đích** | Xuất hồ sơ truy xuất của Shipment theo lược đồ GS1 mô phỏng |
| **GS1 Compliance** | Không phải chứng nhận hoặc triển khai tuân thủ chính thức GS1 |
| **Database Migration** | Không cần |

### Mô tả

Tính năng cho phép người dùng có quyền VT-02 hoặc VT-04
xuất hồ sơ truy xuất của một `Shipment` theo một lược đồ dữ liệu
mô phỏng hướng chuẩn GS1.

Hồ sơ xuất bao gồm:

- Thông tin Shipment.
- Danh sách `ChainEvent`.
- Thông tin actor ghi nhận sự kiện.
- Thời điểm sự kiện.
- Địa điểm sự kiện nếu dữ liệu tồn tại.
- Dữ liệu chi tiết của sự kiện.
- Bảng ánh xạ giữa dữ liệu hệ thống và schema mô phỏng GS1.
- Các cảnh báo dữ liệu thiếu hoặc không đầy đủ.
- Thông tin thời điểm và người thực hiện export.

Đây là **schema mô phỏng phục vụ mục đích học tập/demo**, không được
tuyên bố là GS1 Digital Link, EPCIS hoặc một chứng nhận GS1 compliance.

---

# 2. Phạm vi và nguyên tắc triển khai

## 2.1. Phạm vi

Feature này chỉ thực hiện:

```text
Shipment
   ↓
ChainEvent
   ↓
Mapping
   ↓
GS1 simulated dossier
   ↓
JSON / XML
````

Đây là chức năng **READ-ONLY + EXPORT** đối với dữ liệu nghiệp vụ.

## 2.2. Không được thay đổi dữ liệu nghiệp vụ

Trong quá trình export:

* Không thay đổi `Shipment`.
* Không thay đổi `ChainEvent`.
* Không thay đổi `ShipmentStatus`.
* Không tạo `ChainEvent`.
* Không tạo event type mới.
* Không tạo dữ liệu giả để hoàn thiện hồ sơ.
* Không cập nhật số lượng Shipment.
* Không cập nhật trạng thái Shipment.
* Không thay đổi dữ liệu sản xuất.
* Không thay đổi dữ liệu PROCUREMENT.
* Không thay đổi dữ liệu TRANSPORT.

Nếu dữ liệu cần cho schema mô phỏng không tồn tại trong hệ thống:

```text
→ để null / bỏ trường tùy DTO
→ hoặc ghi warning
```

Không được tự suy diễn hoặc tự tạo dữ liệu.

---

# 3. Endpoint

## 3.1. Export dossier

```http
GET /api/v1/shipments/{shipmentId}/export-gs1-dossier
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

#### JSON

```http
GET /api/v1/shipments/{shipmentId}/export-gs1-dossier?format=json
```

Response:

```http
Content-Type: application/json
```

#### XML

```http
GET /api/v1/shipments/{shipmentId}/export-gs1-dossier?format=xml
```

Response:

```http
Content-Type: application/xml
```

---

# 4. Authorization

Endpoint yêu cầu authentication.

Các role được phép:

```text
VT-02
VT-04
```

Các role khác không được phép export.

### Authorization flow

```text
Request
  ↓
Authentication
  ↓
Role check
  ├── VT-02 / VT-04 → tiếp tục
  └── role khác → 403
  ↓
Kiểm tra Shipment
  ↓
Kiểm tra quyền truy cập Shipment / Organization
  ↓
Kiểm tra QTN-11
  ↓
Export
```

Không cho phép frontend truyền role để bypass authorization.

Authorization phải dựa trên authenticated principal và
cơ chế security hiện có của project.

---

# 5. Điều kiện xuất hồ sơ — QTN-11

Shipment chỉ được export khi đáp ứng điều kiện nghiệp vụ
được project định nghĩa cho QTN-11.

## 5.1. Trạng thái Shipment

Shipment phải ở trạng thái được phép export theo domain hiện tại.

Các giá trị status phải được lấy từ enum/domain hiện có.

> Không tự tạo `CLOSED`, `PACKAGED` hoặc bất kỳ status mới nào nếu
> status đó không tồn tại trong domain hiện tại.

Nếu implementation hiện tại của project xác định:

```text
CLOSED
PACKAGED
```

là các trạng thái hợp lệ thì sử dụng đúng các giá trị đó.

## 5.2. Required farming records

Hồ sơ phải có đầy đủ các chứng từ/sự kiện bắt buộc:

```text
PLANTING
FERTILIZING
PESTICIDE
HARVESTING
```

Các event type phải được đối chiếu với enum/domain hiện tại.

> Không tạo event type mới chỉ để phục vụ feature export.

## 5.3. Shipment không có ChainEvent

Nếu Shipment không có bất kỳ `ChainEvent` nào:

```http
400 Bad Request
```

Không tạo hồ sơ rỗng.

---

# 6. Event mapping

Mỗi `ChainEvent` được ánh xạ thành một `GS1Event`.

Schema mô phỏng sử dụng bốn chiều:

```text
who
when
where
why
```

## 6.1. who

Đại diện cho actor thực hiện/ghi nhận sự kiện.

Mapping:

```text
ChainEvent.recordedBy.fullName
        ↓
GS1Event.recordedBy
```

`recordedBy` phải lấy từ dữ liệu thực tế trong hệ thống.

Không tự tạo tên actor.

---

## 6.2. when

Đại diện cho thời điểm ghi nhận event.

Mapping:

```text
ChainEvent.recordedAt
        ↓
GS1Event.recordedAt
```

Không sử dụng client-provided timestamp cho `exportedAt`.

---

## 6.3. where

Đại diện cho địa điểm event nếu domain hiện tại có thông tin.

Mapping:

```text
ChainEvent.location.latitude
        ↓
GS1Event.location.latitude

ChainEvent.location.longitude
        ↓
GS1Event.location.longitude

ChainEvent.location.address
        ↓
GS1Event.location.address
```

### Quan trọng

Implementation phải kiểm tra entity/domain thực tế trước khi sử dụng
các field trên.

Nếu `ChainEvent` hiện tại không có `location.address`, không được:

* thêm column;
* tạo migration;
* thay đổi entity schema chỉ vì feature này.

Trường không tồn tại phải được xử lý bằng `null` hoặc warning.

---

## 6.4. why

Đại diện cho lý do/ngữ nghĩa của event.

Trong schema mô phỏng:

```text
why = eventType + eventData
```

Mapping:

```text
ChainEvent.eventType
ChainEvent.eventData
        ↓
GS1Event.details
```

`eventData` phải được sử dụng từ dữ liệu thực tế hiện có.

Không tự suy diễn thêm business data.

---

# 7. Event location handling

Location không phải lúc nào cũng tồn tại đối với mọi event.

Ví dụ:

```json
{
  "eventType": "TRANSPORT",
  "location": null,
  "details": {
    "fromLocation": "Thái Nguyên",
    "toLocation": "Hà Nội"
  }
}
```

Trường hợp này vẫn được export.

Hệ thống phải thêm warning:

```json
{
  "eventId": "...",
  "field": "location",
  "message": "Sự kiện thiếu thông tin vị trí"
}
```

### Quy tắc

```text
Có location
    → mapping bình thường

Không có location
    → vẫn export
    → location = null
    → thêm warning

Không có address nhưng có coordinates
    → export coordinates
    → address = null
    → warning nếu cần

Không có bất kỳ location data nào
    → location = null
    → warning
```

Không được tự suy ra coordinates từ tên địa điểm.

Ví dụ:

```text
"Thái Nguyên"
```

không được tự động chuyển thành latitude/longitude.

---

# 8. Response — JSON

## 8.1. Thành công — TC-01

HTTP:

```http
200 OK
Content-Type: application/json
```

Ví dụ:

```json
{
  "success": true,
  "status": 200,
  "data": {
    "shipment": {
      "id": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",
      "name": "Lô chè Tân Cương T8/2026",
      "codeValue": "TANCUONG00000059",
      "productCategory": "Chè",
      "totalQuantity": 500.0,
      "unit": "kg",
      "status": "PACKAGED",
      "organization": {
        "id": "7f6e5d4c-3333-4a2a-9f3d-1a2b3c4d5e6f",
        "name": "HTX Chè Tân Cương",
        "code": "HTX-TC"
      }
    },
    "events": [
      {
        "eventId": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "HARVESTING",
        "eventTypeLabel": "Thu hoạch",
        "recordedAt": "2026-08-11T10:00:00Z",
        "recordedBy": "Nguyễn Văn A",
        "location": {
          "latitude": 21.0285,
          "longitude": 105.8542,
          "address": "Xã Tân Cương, Thái Nguyên"
        },
        "details": {
          "quantity": 500.0,
          "harvestDate": "2026-08-11",
          "productionLotName": "Lô chè Tân Cương T8/2026"
        }
      },
      {
        "eventId": "b2c3d4e5-2222-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "PACKAGING",
        "eventTypeLabel": "Đóng gói",
        "recordedAt": "2026-08-11T10:30:00Z",
        "recordedBy": "Nguyễn Văn A",
        "location": {
          "latitude": 21.0285,
          "longitude": 105.8542,
          "address": "Xã Tân Cương, Thái Nguyên"
        },
        "details": {
          "packagingSpecification": "Túi 500g",
          "packagingDate": "2026-08-11",
          "productionLotName": "Lô chè Tân Cương T8/2026"
        }
      },
      {
        "eventId": "c3d4e5f6-3333-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "TRANSPORT",
        "eventTypeLabel": "Vận chuyển",
        "recordedAt": "2026-08-11T11:00:00Z",
        "recordedBy": "Nguyễn Văn B",
        "location": null,
        "details": {
          "fromLocation": "Thái Nguyên",
          "toLocation": "Hà Nội"
        }
      },
      {
        "eventId": "d4e5f6a7-4444-4a2a-9f3d-1a2b3c4d5e6f",
        "eventType": "PROCUREMENT",
        "eventTypeLabel": "Thu mua",
        "recordedAt": "2026-08-11T11:30:00Z",
        "recordedBy": "Nguyễn Văn C",
        "location": {
          "latitude": 21.0285,
          "longitude": 105.8542,
          "address": "Hà Nội"
        },
        "details": {
          "receivedQuantity": 500.0,
          "shipmentName": "Lô chè xuất Trung"
        }
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
      "ChainEvent.eventData": "eventData",
      "Shipment.name": "shipmentName",
      "Shipment.totalQuantity": "declaredQuantity",
      "Shipment.status": "shipmentStatus"
    },
    "warnings": [],
    "exportedAt": "2026-08-12T10:00:00Z",
    "exportedBy": "Nguyễn Văn C",
    "schemaVersion": "1.0.0",
    "schemaDescription": "Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ GS1"
  },
  "timestamp": "2026-08-12T10:00:01.123Z"
}
```

---

# 9. `includeMapping`

## `includeMapping=true`

Mặc định:

```http
GET /api/v1/shipments/{id}/export-gs1-dossier?includeMapping=true
```

Response có:

```json
{
  "mapping": {
    "ChainEvent.id": "eventIdentifier"
  }
}
```

## `includeMapping=false`

```http
GET /api/v1/shipments/{id}/export-gs1-dossier?includeMapping=false
```

Response:

```json
{
  "mapping": null
}
```

Không ảnh hưởng đến `events`.

---

# 10. Warnings

Warnings không làm request thất bại.

Ví dụ:

```json
{
  "warnings": [
    {
      "eventId": "c3d4e5f6-3333-4a2a-9f3d-1a2b3c4d5e6f",
      "field": "location",
      "message": "Sự kiện vận chuyển thiếu thông tin vị trí"
    }
  ]
}
```

Quy tắc:

```text
Business data hợp lệ nhưng thiếu metadata
        ↓
200 OK + warnings
```

Không sử dụng warning để che giấu lỗi nghiệp vụ nghiêm trọng.

---

# 11. XML Response

Khi:

```text
format=xml
```

response:

```http
200 OK
Content-Type: application/xml
```

Ví dụ cấu trúc:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gs1Dossier>
    <shipment>
        <id>9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f</id>
        <name>Lô chè Tân Cương T8/2026</name>
        <codeValue>TANCUONG00000059</codeValue>
        <status>PACKAGED</status>
    </shipment>

    <events>
        <event>
            <eventId>a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f</eventId>
            <eventType>HARVESTING</eventType>
            <recordedAt>2026-08-11T10:00:00Z</recordedAt>
            <recordedBy>Nguyễn Văn A</recordedBy>
        </event>
    </events>

    <warnings />

    <exportedAt>2026-08-12T10:00:00Z</exportedAt>
    <exportedBy>Nguyễn Văn C</exportedBy>

    <schemaVersion>1.0.0</schemaVersion>
</gs1Dossier>
```

### XML constraint

JSON và XML phải biểu diễn **cùng một tập dữ liệu và cùng business semantics**.

Không được để JSON có field mà XML hoàn toàn bỏ qua nếu field đó
thuộc schema export.

---

# 12. Error Responses

## 12.1. Shipment không tồn tại — TC-04

```http
404 Not Found
```

```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy lô hàng.",
  "timestamp": "2026-08-12T10:00:01.123Z"
}
```

---

## 12.2. Không có quyền — TC-03

```http
403 Forbidden
```

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền xuất hồ sơ theo lược đồ cho lô này.",
  "timestamp": "2026-08-12T10:00:01.123Z"
}
```

Áp dụng cho:

* user không có VT-02;
* user không có VT-04;
* user không thuộc organization có quyền truy cập Shipment.

---

## 12.3. Shipment không đủ điều kiện — QTN-11

```http
400 Bad Request
```

```json
{
  "success": false,
  "status": 400,
  "message": "Lô chưa đủ điều kiện xuất hồ sơ.",
  "data": {
    "missingConditions": [
      "Lô chưa đạt trạng thái cho phép xuất hồ sơ",
      "Thiếu chứng từ PLANTING",
      "Thiếu chứng từ FERTILIZING",
      "Thiếu chứng từ PESTICIDE",
      "Thiếu chứng từ HARVESTING"
    ]
  },
  "timestamp": "2026-08-12T10:00:01.123Z"
}
```

Danh sách `missingConditions` chỉ chứa những điều kiện thực sự thiếu.

---

## 12.4. Shipment không có event

```http
400 Bad Request
```

```json
{
  "success": false,
  "status": 400,
  "message": "Lô chưa có sự kiện nào để xuất hồ sơ.",
  "timestamp": "2026-08-12T10:00:01.123Z"
}
```

---

## 12.5. Format không hợp lệ

Nếu:

```text
format=pdf
```

thì:

```http
400 Bad Request
```

```json
{
  "success": false,
  "status": 400,
  "message": "Định dạng xuất không được hỗ trợ. Chỉ hỗ trợ json hoặc xml.",
  "timestamp": "2026-08-12T10:00:01.123Z"
}
```

---

# 13. Response DTO

## 13.1. GS1DossierExportResponse

```java
public class GS1DossierExportResponse {

    private ShipmentInfo shipment;

    private List<GS1Event> events;

    private Map<String, String> mapping;

    private List<Warning> warnings;

    private LocalDateTime exportedAt;

    private String exportedBy;

    private String schemaVersion;

    private String schemaDescription;
}
```

---

## 13.2. GS1Event

```java
public class GS1Event {

    private UUID eventId;

    private String eventType;

    private String eventTypeLabel;

    private LocalDateTime recordedAt;

    private String recordedBy;

    private EventLocation location;

    private Map<String, Object> details;
}
```

---

## 13.3. EventLocation

```java
public class EventLocation {

    private Double latitude;

    private Double longitude;

    private String address;
}
```

---

## 13.4. Warning

```java
public class Warning {

    private UUID eventId;

    private String field;

    private String message;
}
```

---

## 13.5. Request

Request không có body.

Các tham số được truyền bằng query parameters:

```text
format
includeMapping
```

Nếu project convention yêu cầu Request DTO:

```java
public class GS1DossierExportRequest {

    private String format = "json";

    private Boolean includeMapping = true;
}
```

Không cần tạo request body cho GET.

---

# 14. ShipmentInfo

`ShipmentInfo` phải sử dụng các field đã tồn tại trong domain.

Dữ liệu mô phỏng:

```text
Shipment.id
Shipment.name
Shipment.codeValue
Shipment.productCategory
Shipment.totalQuantity
Shipment.unit
Shipment.status
Shipment.organization
```

Không thêm field vào `Shipment` chỉ để phục vụ export.

Nếu một field không tồn tại trong entity hiện tại:

```text
→ không tạo migration
→ không tạo fake value
→ loại bỏ hoặc null tùy DTO
```

---

# 15. Mapping Table

| System Field                     | GS1 Simulation Field      | Description         |
| -------------------------------- | ------------------------- | ------------------- |
| `ChainEvent.id`                  | `eventIdentifier`         | ID sự kiện          |
| `ChainEvent.eventType`           | `eventTypeCode`           | Loại sự kiện        |
| `ChainEvent.recordedAt`          | `eventDateTime`           | Thời điểm ghi nhận  |
| `ChainEvent.recordedBy.fullName` | `actorName`               | Người ghi nhận      |
| `ChainEvent.location.latitude`   | `eventLocation.latitude`  | Vĩ độ               |
| `ChainEvent.location.longitude`  | `eventLocation.longitude` | Kinh độ             |
| `ChainEvent.location.address`    | `eventLocation.address`   | Địa chỉ             |
| `ChainEvent.eventData`           | `eventData`               | Dữ liệu chi tiết    |
| `Shipment.name`                  | `shipmentName`            | Tên Shipment        |
| `Shipment.totalQuantity`         | `declaredQuantity`        | Số lượng            |
| `Shipment.status`                | `shipmentStatus`          | Trạng thái Shipment |

> Mapping trên là mapping mô phỏng. Không được xem là mapping chính thức
> tới GS1 EPCIS hoặc GS1 Digital Link.

---

# 16. Event ordering

Các event được lấy theo:

```text
recordedAt ASC
```

Ví dụ:

```text
2026-08-11T10:00
        ↓
2026-08-11T10:30
        ↓
2026-08-11T11:00
        ↓
2026-08-11T11:30
```

Nếu hai event có cùng `recordedAt`, implementation phải sử dụng
thứ tự ổn định dựa trên field hiện có trong domain.

Không được random order.

---

# 17. Audit — ActivityLog

Mỗi lần export thành công phải ghi ActivityLog theo cơ chế audit
hiện có của project.

Thông tin tối thiểu:

```text
Action:
GS1_DOSSIER_EXPORT

Target:
Shipment

Target ID:
shipmentId

Actor:
authenticated user

Format:
json / xml
```

`exportedBy`:

```text
authenticated principal
```

`exportedAt`:

```text
server time
```

Không nhận `exportedBy` hoặc `exportedAt` từ client.

Không tạo migration nếu ActivityLog hiện tại đã hỗ trợ các metadata này.

---

# 18. Processing Flow

```text
VT-02 / VT-04
      │
      ▼
GET /api/v1/shipments/{shipmentId}/export-gs1-dossier
      │
      ▼
Authenticate user
      │
      ├── Không authenticated → Security mechanism hiện tại
      │
      ▼
Check role
      │
      ├── Không phải VT-02 / VT-04 → 403
      │
      ▼
Find Shipment
      │
      ├── Không tồn tại → 404
      │
      ▼
Check organization/resource access
      │
      ├── Không có quyền → 403
      │
      ▼
Validate QTN-11
      │
      ├── Không đủ điều kiện → 400
      │
      ▼
Load ChainEvents
      │
      ├── Không có event → 400
      │
      ▼
Sort recordedAt ASC
      │
      ▼
Map ChainEvent → GS1Event
      │
      ├── Có location → map location
      │
      └── Không có location
             ↓
         location=null
             +
          warning
      │
      ▼
Build mapping
      │
      ├── includeMapping=true → mapping
      │
      └── includeMapping=false → mapping=null
      │
      ▼
Build dossier
      │
      ▼
Format
      │
      ├── json → application/json
      │
      └── xml → application/xml
      │
      ▼
Write ActivityLog
      │
      ▼
Return 200
```

---

# 19. Business Rules

| Rule                    | Description                                                             |
| ----------------------- | ----------------------------------------------------------------------- |
| **QTN-11**              | Shipment phải đáp ứng các điều kiện export được định nghĩa trong domain |
| **Authorization**       | Chỉ VT-02 và VT-04 được export                                          |
| **Organization access** | User phải có quyền truy cập Shipment                                    |
| **No events**           | Không có ChainEvent → 400                                               |
| **Event ordering**      | ChainEvent được sắp xếp `recordedAt ASC`                                |
| **Four dimensions**     | Event được ánh xạ theo `who`, `when`, `where`, `why`                    |
| **Missing location**    | Thiếu location không làm export thất bại; tạo warning                   |
| **No fake data**        | Không tự sinh dữ liệu không tồn tại                                     |
| **Read-only**           | Không sửa Shipment/ChainEvent                                           |
| **Mapping**             | `includeMapping=true` mặc định                                          |
| **JSON**                | `format=json`                                                           |
| **XML**                 | `format=xml`                                                            |
| **Audit**               | Export thành công phải ghi ActivityLog                                  |
| **Schema**              | Không thay đổi database schema                                          |
| **GS1**                 | Đây chỉ là schema mô phỏng, không phải GS1 compliance                   |

---

# 20. Test Cases

| TC        | Scenario                                      | Expected                                                 |
| --------- | --------------------------------------------- | -------------------------------------------------------- |
| **TC-01** | Shipment đủ điều kiện và có đầy đủ ChainEvent | `200`, dossier được export                               |
| **TC-02** | Một event thiếu location                      | `200`, `location=null`, warning được tạo                 |
| **TC-03** | User VT-06 / role không được phép             | `403`                                                    |
| **TC-04** | Shipment không tồn tại                        | `404`                                                    |
| **TC-05** | Shipment không đủ QTN-11                      | `400`, liệt kê missing conditions                        |
| **TC-06** | Shipment không có ChainEvent                  | `400`                                                    |
| **TC-07** | `format=json`                                 | `200`, `application/json`                                |
| **TC-08** | `format=xml`                                  | `200`, `application/xml`                                 |
| **TC-09** | `format=pdf`                                  | `400`                                                    |
| **TC-10** | `includeMapping=true`                         | Response có mapping                                      |
| **TC-11** | `includeMapping=false`                        | Response không có mapping                                |
| **TC-12** | ChainEvent được tạo với nhiều timestamp       | Events sắp xếp `recordedAt ASC`                          |
| **TC-13** | Export thành công                             | ActivityLog được tạo                                     |
| **TC-14** | `exportedBy`                                  | Lấy từ authenticated user                                |
| **TC-15** | `exportedAt`                                  | Lấy từ server time                                       |
| **TC-16** | Không có location/address                     | Export thành công + warning                              |
| **TC-17** | Procurement event                             | Export đúng ChainEvent hiện có                           |
| **TC-18** | Transport event                               | Export đúng ChainEvent hiện có, không tự tạo coordinates |

---

# 21. Implementation Constraints

## Bắt buộc

Agent triển khai phải tuân thủ:

1. **Không thay đổi database schema.**
2. **Không tạo Flyway migration.**
3. **Không thay đổi `ChainEvent`.**
4. **Không thay đổi `Shipment`.**
5. **Không thay đổi `ShipmentStatus`.**
6. **Không tạo event type mới.**
7. **Không tạo dữ liệu giả.**
8. **Không tự geocode địa chỉ thành coordinates.**
9. **Không tự suy diễn dữ liệu GS1 chưa tồn tại.**
10. **Không thay đổi logic PROCUREMENT.**
11. **Không thay đổi logic TRANSPORT.**
12. **Không thay đổi logic farming logs.**
13. **Không thay đổi authorization hiện tại ngoài việc thêm endpoint nếu cần.**
14. **Không tạo một authentication/authorization mechanism riêng.**
15. `exportedBy` phải lấy từ authenticated principal.
16. `exportedAt` phải lấy từ server.
17. JSON và XML phải biểu diễn cùng semantic data.
18. Phải sử dụng exception/error response convention hiện có của project.
19. Phải sử dụng ActivityLog/audit mechanism hiện có nếu đã tồn tại.
20. Trước khi tạo service/DTO mới, phải kiểm tra các abstraction tương đương
    đã tồn tại trong project.
21. Không tạo `GS1DossierExportService` nếu `DossierService` hiện tại đã là
    abstraction phù hợp và có thể mở rộng mà không phá vỡ kiến trúc.
22. Không sửa code ngoài phạm vi NCL-12-CN-003.

---

# 22. Recommended Project Structure

Trước khi tạo file mới, kiểm tra structure hiện tại.

Nếu project chưa có abstraction tương đương, structure đề xuất:

```text
backend/src/main/java/vn/nguongocso/

├── report/
│   ├── controller/
│   │   └── DossierController.java
│   │
│   ├── service/
│   │   ├── DossierService.java
│   │   └── DossierServiceImpl.java
│   │
│   └── dto/
│       ├── GS1DossierExportResponse.java
│       ├── GS1Event.java
│       ├── EventLocation.java
│       └── Warning.java
```

Không bắt buộc phải sử dụng đúng structure trên nếu project hiện tại
đã có package convention tương ứng.

---

# 23. Files cần tạo/sửa

## Có thể tạo

```text
GS1DossierExportResponse.java
GS1Event.java
EventLocation.java
Warning.java
```

## Có thể mở rộng

```text
DossierService.java
DossierServiceImpl.java
```

## Controller

Tạo mới hoặc mở rộng controller hiện có tùy structure thực tế:

```text
DossierController.java
```

## Documentation

```text
docs/api/report/GS1DossierExport.md
```

## Migration

```text
KHÔNG CẦN
```

Không tạo migration trừ khi trong quá trình kiểm tra source phát hiện
một yêu cầu bắt buộc của feature nhưng docs này chưa mô tả. Trong trường
hợp đó phải **dừng và báo cáo**, không tự ý thay đổi schema.

---

# 24. Git Branch

```bash
git checkout -b feature/NCL-12-CN-003-gs1-dossier-export
```

Commit đề xuất:

```text
feat: add GS1 dossier export for shipments
```

---

# 25. Definition of Done

Feature được xem là hoàn thành khi:

### Backend

* [ ] Endpoint export tồn tại.
* [ ] Authentication hoạt động.
* [ ] VT-02 được phép.
* [ ] VT-04 được phép.
* [ ] Role khác bị `403`.
* [ ] Shipment không tồn tại → `404`.
* [ ] Shipment không đủ QTN-11 → `400`.
* [ ] Shipment không có ChainEvent → `400`.
* [ ] ChainEvent được sort `recordedAt ASC`.
* [ ] ChainEvent được map thành GS1Event.
* [ ] Missing location tạo warning.
* [ ] Không tạo fake location.
* [ ] `exportedBy` lấy từ authenticated user.
* [ ] `exportedAt` lấy từ server.
* [ ] JSON export hoạt động.
* [ ] XML export hoạt động.
* [ ] `includeMapping=true` hoạt động.
* [ ] `includeMapping=false` hoạt động.
* [ ] ActivityLog được ghi sau export thành công.
* [ ] Không có database migration.
* [ ] Không thay đổi Shipment/ChainEvent business behavior.

### Tests

Ít nhất phải có test cho:

```text
authorization
shipment not found
QTN-11 failure
empty events
successful export
missing location
JSON format
XML format
invalid format
includeMapping
event ordering
audit log
```

### Frontend

Nếu feature hiện tại chỉ cung cấp API backend:

```text
Không cần thay đổi frontend.
```

Nếu product yêu cầu UI export, frontend sẽ được triển khai trong task riêng
hoặc chỉ thực hiện khi được yêu cầu trong scope NCL-12-CN-003.

---

# 26. Final Architecture

```text
                ┌──────────────────────┐
                │      VT-02 / VT-04   │
                └──────────┬───────────┘
                           │
                           ▼
       GET /api/v1/shipments/{id}/export-gs1-dossier
                           │
                           ▼
                  Authentication
                           │
                           ▼
                    Authorization
                           │
                           ▼
                  Shipment lookup
                           │
                           ▼
                     QTN-11 check
                           │
                           ▼
                   Load ChainEvents
                           │
                           ▼
                  Sort recordedAt
                           │
                           ▼
                ┌─────────────────────┐
                │  GS1 Mapping Layer  │
                │                     │
                │ who   ← recordedBy  │
                │ when  ← recordedAt  │
                │ where ← location    │
                │ why   ← eventData   │
                └─────────┬───────────┘
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
       format=json              format=xml
              │                       │
              ▼                       ▼
     application/json        application/xml
              │                       │
              └───────────┬───────────┘
                          ▼
                    ActivityLog
                          │
                          ▼
                       Response
```

---

# 27. Lưu ý về GS1

Feature này sử dụng một **lược đồ mô phỏng lấy cảm hứng từ cách
mô hình hóa event theo các chiều dữ liệu `who / when / where / why`**.

Nó **không phải**:

* chứng nhận GS1;
* GS1 EPCIS implementation;
* GS1 Digital Link implementation;
* chứng nhận interoperability;
* chứng nhận compliance.

`schemaVersion = "1.0.0"` chỉ là version của **simulation schema của
project**, không phải version của một tiêu chuẩn GS1 chính thức.

---

## Author

```text
Author: @hienvanla5
Date: 2026-08-13
Branch: feature/NCL-12-CN-003-gs1-dossier-export
File: docs/api/report/GS1DossierExport.md
```
