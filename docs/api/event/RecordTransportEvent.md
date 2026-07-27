# API: Ghi sự kiện vận chuyển

*NCL-05-CN-002 — Epic NCL-05: Ghi sự kiện chuỗi cung ứng*

*Nhánh git: feature/record-transport-event*

## 1. Thông tin chung

**Mục tiêu**

Cho phép Người ghi sự kiện quét mã truy xuất (TraceCode) của một lô hàng (Shipment) đã kích hoạt tem, nhập thông tin vận chuyển (điểm đi, điểm đến, thời gian), để hệ thống bổ sung một sự kiện vận chuyển (ChainEvent) vào dòng sự kiện của lô hàng, phản ánh đúng hành trình di chuyển thực tế.

**Nhật ký này phục vụ:**

- Ghi nhận đầy đủ hành trình vận chuyển của lô hàng trong chuỗi cung ứng.
- Đảm bảo sự kiện vận chuyển chỉ được thêm vào lô hàng còn hiệu lực (đã kích hoạt, chưa bị thu hồi).
- Chuẩn bị dữ liệu dòng sự kiện phục vụ tra cứu công khai nguồn gốc lô hàng ở các chức năng tiếp theo.

## 2. Endpoint

**POST /api/v1/chain-events/transport**

**Request Body**

```json
{
  "codeValue": "HX00000029",
  "fromLocation": "Xã Long Cốc, huyện Tân Sơn, Phú Thọ",
  "toLocation": "Kho trung chuyển Việt Trì, Phú Thọ",
  "transportTime": "2026-07-24T09:00:00"
}
```

**Ghi chú tham số**

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| codeValue | string | Có | Mã truy xuất (QR) của lô hàng cần ghi sự kiện vận chuyển. |
| fromLocation | string | Có | Điểm xuất phát của chuyến vận chuyển. |
| toLocation | string | Có | Điểm đến của chuyến vận chuyển. |
| transportTime | datetime | Có | Thời điểm diễn ra vận chuyển thực tế (ghi vào recorded_at). |

Ví dụ đường dẫn xem dòng sự kiện của lô hàng sau khi ghi:

```
GET http://localhost:8080/api/v1/shipments/{shipmentId}/chain-events
```

## 3. Điều kiện

**Người dùng phải:**

- Đăng nhập thành công.
- Có role VT-03 (Người ghi sự kiện).

**Lô hàng (xác định qua mã truy xuất) phải:**

- Tồn tại trong hệ thống (mã truy xuất hợp lệ).
- Đang ở trạng thái ACTIVATED (đã kích hoạt tem, chưa bị thu hồi).

## 4. Business Rules

Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi dự kiến trong `ChainEventServiceImpl.recordTransportEvent()`.

**4.1 Kiểm tra Role**

Chỉ người dùng có role VT-03 (Người ghi sự kiện) được phép ghi sự kiện vận chuyển. Đây là bước kiểm tra đầu tiên, thực hiện trước khi tìm mã truy xuất.

Nếu không đúng role:

> "Bạn không có quyền ghi sự kiện vận chuyển."

**4.2 Kiểm tra tồn tại mã truy xuất (TC-03)**

Hệ thống tìm TraceCode theo codeValue. Nếu không tồn tại, hệ thống ném BusinessException:

> "Mã lô hàng không tồn tại."

**4.3 Xác định Lô hàng (Shipment) tương ứng**

Từ TraceCode tìm được, hệ thống xác định Shipment liên kết (thông qua TraceCode.shipment_id).

**4.4 Kiểm tra trạng thái Lô hàng (TC-02)**

Lô hàng phải đang ở trạng thái ACTIVATED. Nếu lô hàng đã bị thu hồi (RECALLED), hệ thống chặn thao tác và báo lỗi sai trạng thái:

> "Lô hàng đã bị thu hồi, không thể ghi sự kiện vận chuyển."

Nếu lô hàng chưa được kích hoạt (đang ở DRAFT hoặc CODE_PRINTED), hệ thống cũng chặn thao tác và báo:

> "Lô hàng chưa được kích hoạt, không thể ghi sự kiện vận chuyển."

**4.5 Tạo sự kiện vận chuyển (ChainEvent) (TC-01)**

Hệ thống tạo bản ghi ChainEvent với:

- event_type = TRANSPORT
- shipment_id = id của Shipment xác định ở bước 4.3
- event_data (JSON) = { fromLocation, toLocation }
- recorded_at = transportTime (thời điểm sự kiện xảy ra do người dùng nhập)
- recorded_by = currentUser
- is_correction = false

**4.6 Lưu lịch sử ghi nhận (TC-04)**

Hệ thống lưu created_at (thời điểm ghi vào hệ thống) cùng recorded_by để phục vụ truy vết nhật ký hoạt động. Sự kiện vận chuyển được bổ sung vào dòng sự kiện (timeline) của lô hàng, sắp xếp theo recorded_at.

## 5. Response DTO

```java
public class ChainEventResponse {
    private UUID id;
    private UUID shipmentId;
    private String eventType;
    private TransportEventData eventData;
    private LocalDateTime recordedAt;
    private String recordedByName;
    private LocalDateTime createdAt;
}

public class TransportEventData {
    private String fromLocation;
    private String toLocation;
}
```

Ghi chú: dữ liệu ChainEvent được map ở tầng service sau khi giao dịch ghi sự kiện hoàn tất; event_data được deserialize từ JSON sang TransportEventData tương ứng với event_type = TRANSPORT.

## 6. Response

**Ví dụ request**

```
POST http://localhost:8080/api/v1/chain-events/transport
Content-Type: application/json

{
  "codeValue": "HX00000029",
  "fromLocation": "Xã Long Cốc, huyện Tân Sơn, Phú Thọ",
  "toLocation": "Kho trung chuyển Việt Trì, Phú Thọ",
  "transportTime": "2026-07-24T09:00:00"
}
```

**HTTP 201 Created**

**Response**

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "3f9a2b1c-7d5e-4a3f-9c2b-1e6f8a4d5c7b",
    "shipmentId": "ecde1d21-18e3-437a-9695-ffb4d2a4c23a",
    "eventType": "TRANSPORT",
    "eventData": {
      "fromLocation": "Xã Long Cốc, huyện Tân Sơn, Phú Thọ",
      "toLocation": "Kho trung chuyển Việt Trì, Phú Thọ"
    },
    "recordedAt": "2026-07-24T09:00:00",
    "recordedByName": "Nguyễn Văn An",
    "createdAt": "2026-07-24T09:05:12.5461200"
  },
  "timestamp": "2026-07-24T02:05:12.551123400Z"
}
```

## 7. Error Response

**400 Bad Request**

Trường hợp thiếu trường bắt buộc (codeValue, fromLocation, toLocation, transportTime):

```json
{
  "success": false,
  "status": 400,
  "message": "Vui lòng nhập đầy đủ thông tin sự kiện vận chuyển."
}
```

**403 Forbidden**

Trường hợp sai role (không phải VT-03):

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền ghi sự kiện vận chuyển."
}
```

**404 Not Found**

```json
{
  "success": false,
  "status": 404,
  "message": "Mã lô hàng không tồn tại."
}
```

**409 Conflict — lô hàng đã thu hồi (TC-02)**

```json
{
  "success": false,
  "status": 409,
  "message": "Lô hàng đã bị thu hồi, không thể ghi sự kiện vận chuyển."
}
```

**409 Conflict — lô hàng chưa kích hoạt**

```json
{
  "success": false,
  "status": 409,
  "message": "Lô hàng chưa được kích hoạt, không thể ghi sự kiện vận chuyển."
}
```

## 8. Backend xử lý

```
Client
  │
  ▼
POST /api/v1/chain-events/transport { codeValue, fromLocation, toLocation, transportTime }
  │
  ▼
Lấy currentUser (SecurityContext)
  │
  ▼
Kiểm tra Role (VT-03) -> 403 nếu sai
  │
  ▼
Tìm TraceCode theo codeValue -> 404 nếu không có
  │
  ▼
Xác định Shipment liên kết từ TraceCode
  │
  ▼
Kiểm tra trạng thái Shipment = ACTIVATED -> 409 nếu đã thu hồi hoặc chưa kích hoạt
  │
  ▼
Tạo ChainEvent (event_type = TRANSPORT, event_data = {fromLocation, toLocation})
  │
  ▼
Lưu recorded_by, recorded_at, created_at
  │
  ▼
Map sang ChainEventResponse & Trả Response (201)
```

## 9. Repository

**TraceCodeRepository**

```java
public interface TraceCodeRepository extends JpaRepository<TraceCode, UUID> {
    Optional<TraceCode> findByCodeValue(String codeValue);
}
```

**ShipmentRepository**

```java
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findById(UUID id);
}
```

**ChainEventRepository**

```java
public interface ChainEventRepository extends JpaRepository<ChainEvent, UUID> {
    List<ChainEvent> findByShipmentIdOrderByRecordedAtAsc(UUID shipmentId);
}
```

Ghi chú: cột shipment_id trên bảng ChainEvent có ràng buộc khóa ngoại tới Shipment; findByShipmentIdOrderByRecordedAtAsc phục vụ hiển thị dòng sự kiện (timeline) của lô hàng theo đúng thứ tự thời gian xảy ra.

## 10. Phạm vi của Story

**Bao gồm**

- Quét mã truy xuất (TraceCode) để xác định lô hàng cần ghi sự kiện.
- Kiểm tra Role (VT-03 — Người ghi sự kiện).
- Kiểm tra trạng thái lô hàng (phải đã kích hoạt, chưa bị thu hồi).
- Ghi nhận sự kiện vận chuyển (ChainEvent) với điểm đi, điểm đến, thời gian.
- Bổ sung sự kiện vào dòng sự kiện (timeline) của lô hàng.

**Không bao gồm**

- Kích hoạt tem (TraceCode.status → ACTIVE) — thuộc chức năng khác.
- Thu hồi lô hàng (Shipment.status → RECALLED).
- Ghi sự kiện thu hoạch, đóng gói, thu mua — thuộc các story khác trong Epic NCL-05.
- Đính chính sự kiện (is_correction = true) — thuộc chức năng khác.
- Tra cứu công khai dòng sự kiện theo mã truy xuất.

## 11. User Story liên quan

**NCL-05-CN-002 — Ghi sự kiện vận chuyển**

Là Người ghi sự kiện, tôi muốn quét mã lô hàng và ghi sự kiện vận chuyển, để hành trình phản ánh khâu di chuyển thực tế.

*Độ ưu tiên: Bắt buộc | Phụ trách: Thành viên ba | Trạng thái: Chưa thực hiện | Tham chiếu: QTN-05*

## 12. Danh sách công việc

*Chu kỳ áp dụng: Chu kỳ số ba.*

| Mã công việc | Tên công việc | Loại | Phụ trách | Trạng thái |
| --- | --- | --- | --- | --- |
| NCL-05-CN-002-CV-01 | Thiết kế màn hình quét và ghi | Thiết kế giao diện | Thành viên hai | Chưa thực hiện |
| NCL-05-CN-002-CV-02 | Phát triển quét mã lô hàng | Phát triển phần giao diện | Thành viên hai | Chưa thực hiện |
| NCL-05-CN-002-CV-03 | Phát triển ghi sự kiện vận chuyển | Phát triển phần máy chủ | Thành viên ba | Chưa thực hiện |
| NCL-05-CN-002-CV-04 | Kiểm tra gắn đúng lô | Kiểm thử | Thành viên năm | Chưa thực hiện |

## 13. Test Cases

**TC-01: Luồng thành công**

| Mục | Nội dung |
| --- | --- |
| Điều kiện đầu vào | Lô hàng còn hiệu lực (đã kích hoạt, chưa bị thu hồi). |
| Hành động | Người ghi ghi sự kiện vận chuyển. |
| Kết quả mong đợi | Sự kiện vận chuyển được ghi nhận. |
| Dữ liệu liên quan | Mã lô, điểm đi, điểm đến, thời gian. |
| Mức độ ưu tiên | Cao |

**TC-02: Sai trạng thái**

| Mục | Nội dung |
| --- | --- |
| Điều kiện đầu vào | Lô hàng đã thu hồi. |
| Hành động | Người ghi ghi sự kiện vận chuyển. |
| Kết quả mong đợi | Hệ thống chặn và báo lô đã thu hồi. |
| Dữ liệu liên quan | Trạng thái lô hàng. |
| Mức độ ưu tiên | Cao |

**TC-03: Dữ liệu không hợp lệ**

| Mục | Nội dung |
| --- | --- |
| Điều kiện đầu vào | Mã quét không tồn tại. |
| Hành động | Người ghi ghi sự kiện vận chuyển. |
| Kết quả mong đợi | Hệ thống báo mã không hợp lệ. |
| Dữ liệu liên quan | Mã lô hàng. |
| Mức độ ưu tiên | Cao |

**TC-04: Lưu lịch sử**

| Mục | Nội dung |
| --- | --- |
| Điều kiện đầu vào | Sự kiện được ghi. |
| Hành động | Hệ thống ghi nhận. |
| Kết quả mong đợi | Lưu người ghi và thời điểm. |
| Dữ liệu liên quan | Nhật ký sự kiện. |
| Mức độ ưu tiên | Trung bình |