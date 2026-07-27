# API: Xem dòng sự kiện truy xuất

*NCL-05-CN-005 — Epic NCL-05: Ghi sự kiện chuỗi cung ứng*

*Nhánh git: feature/view-chain-event-timeline*

## 1. Thông tin chung

**Mục tiêu**

Cho phép Quản lý hợp tác xã (role VT-02) mở một lô hàng (Shipment) thuộc tổ chức mình và xem toàn bộ dòng sự kiện (ChainEvent) của lô hàng đó, sắp xếp theo đúng thứ tự thời gian xảy ra, kèm thông tin người ghi, để kiểm tra hành trình đầy đủ của lô hàng trước khi xuất hồ sơ hoặc phục vụ tra cứu nội bộ.

**Nhật ký này phục vụ:**

- Cho phép người quản lý rà soát hành trình lô hàng liền mạch, từ thu hoạch, vận chuyển, đóng gói đến thu mua.
- Đảm bảo dòng sự kiện hiển thị đầy đủ, đúng thứ tự thời gian, kể cả khi lô hàng chưa phát sinh sự kiện nào.
- Đảm bảo tính minh bạch: hiển thị cả sự kiện gốc và sự kiện đính chính (is_correction) trong cùng dòng thời gian.
- Giới hạn quyền xem: chỉ người dùng thuộc tổ chức sở hữu lô hàng mới được truy cập dòng sự kiện của lô hàng đó.

## 2. Endpoint

**GET /api/v1/shipments/{shipmentId}/chain-events**

**Path Parameter**

| Trường | Kiểu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| shipmentId | UUID | Có | Id của lô hàng (Shipment) cần xem dòng sự kiện. |

**Ví dụ đường dẫn:**

```
GET http://localhost:8080/api/v1/shipments/ecde1d21-18e3-437a-9695-ffb4d2a4c23a/chain-events
```

## 3. Điều kiện

**Người dùng phải:**

- Đăng nhập thành công.
- Có role VT-02 (Quản lý hợp tác xã). Đây là vai trò duy nhất được phép xem dòng sự kiện truy xuất ở story này.
- Thuộc tổ chức sở hữu lô hàng (thông qua OrganizationUser).

**Lô hàng (xác định qua shipmentId) phải:**

- Tồn tại trong hệ thống.

## 4. Business Rules

Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi dự kiến trong `ChainEventServiceImpl.getShipmentTimeline()`.

**4.1 Kiểm tra Role**

Chỉ người dùng có role VT-02 (Quản lý hợp tác xã) được phép xem dòng sự kiện truy xuất. Đây là bước kiểm tra đầu tiên, thực hiện trước khi tìm lô hàng.

Nếu không đúng role:

> "Bạn không có quyền xem dòng sự kiện truy xuất."

**4.2 Kiểm tra tồn tại lô hàng**

Hệ thống tìm Shipment theo shipmentId. Nếu không tồn tại, hệ thống ném BusinessException:

> "Lô hàng không tồn tại."

**4.3 Kiểm tra quyền truy cập theo tổ chức (TC-03)**

Hệ thống so sánh organization_id của currentUser (qua OrganizationUser) với organization_id của Shipment tìm được. Nếu không trùng khớp (lô hàng thuộc tổ chức khác), hệ thống từ chối truy cập:

> "Bạn không có quyền xem dòng sự kiện của lô hàng này."

**4.4 Truy vấn dòng sự kiện (TC-01, TC-02)**

Hệ thống gọi `ChainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId)` để lấy toàn bộ ChainEvent thuộc lô hàng, sắp xếp tăng dần theo recorded_at.

Nếu danh sách rỗng (lô hàng chưa phát sinh sự kiện nào), hệ thống trả về mảng dữ liệu rỗng; phía client hiển thị trạng thái "Chưa có sự kiện", không coi là lỗi.

**4.5 Hiển thị sự kiện đính chính (TC-04)**

Danh sách trả về bao gồm cả sự kiện gốc và sự kiện đính chính (is_correction = true, liên kết qua parent_event_id). Sự kiện đính chính không tách riêng thành danh sách khác mà được sắp xếp xen kẽ đúng theo recorded_at, cùng dòng thời gian với sự kiện gốc.

**4.6 Ánh xạ tên người ghi**

Với mỗi ChainEvent, hệ thống lấy tên người dùng tương ứng với recorded_by để ánh xạ sang recordedByName, phục vụ hiển thị "dòng sự kiện kèm người ghi" trên giao diện.

**4.7 Ánh xạ tọa độ vị trí**

Trường location (kiểu geometry) của sự kiện, nếu có, được tách thành hai trường latitude và longitude tương ứng trong response.

## 5. Response DTO

```java
package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.event.enums.ChainEventType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO phản hồi chuỗi sự kiện cung ứng.
 *
 * @author Team WEB 1
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
public class ChainEventResponse {
    private UUID id;
    private UUID shipmentId;
    private ChainEventType eventType;
    private Map<String, Object> eventData;
    private Double latitude;
    private Double longitude;
    private LocalDateTime recordedAt;
    private String recordedByName;
    private LocalDateTime createdAt;
}
```

Ghi chú: khác với API ghi nhận sự kiện vận chuyển (eventData được deserialize sang một kiểu cụ thể như TransportEventData), API xem dòng sự kiện trả về eventData ở dạng `Map<String, Object>` vì một lô hàng có thể có nhiều loại sự kiện khác nhau (HARVEST, TRANSPORT, PACKAGING, PROCUREMENT, CORRECTION), mỗi loại có cấu trúc dữ liệu riêng.

## 6. Response

**Ví dụ request**

```
GET http://localhost:8080/api/v1/shipments/ecde1d21-18e3-437a-9695-ffb4d2a4c23a/chain-events
```

**HTTP 200 OK**

**Response**

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "a1b2c3d4-1111-4a3f-9c2b-1e6f8a4d5c7b",
      "shipmentId": "ecde1d21-18e3-437a-9695-ffb4d2a4c23a",
      "eventType": "HARVEST",
      "eventData": { "note": "Thu hoạch đợt 1" },
      "latitude": 21.267800,
      "longitude": 105.223500,
      "recordedAt": "2026-07-20T07:00:00",
      "recordedByName": "Trần Thị Bình",
      "createdAt": "2026-07-20T07:05:03.1120000"
    },
    {
      "id": "3f9a2b1c-7d5e-4a3f-9c2b-1e6f8a4d5c7b",
      "shipmentId": "ecde1d21-18e3-437a-9695-ffb4d2a4c23a",
      "eventType": "TRANSPORT",
      "eventData": {
        "fromLocation": "Xã Long Cốc, huyện Tân Sơn, Phú Thọ",
        "toLocation": "Kho trung chuyển Việt Trì, Phú Thọ"
      },
      "latitude": null,
      "longitude": null,
      "recordedAt": "2026-07-24T09:00:00",
      "recordedByName": "Nguyễn Văn An",
      "createdAt": "2026-07-24T09:05:12.5461200"
    }
  ],
  "timestamp": "2026-07-27T02:10:00.551123400Z"
}
```

Ghi chú: nếu lô hàng chưa có sự kiện nào (TC-02), trường data trả về là mảng rỗng (`[]`), status vẫn là 200 OK.

## 7. Error Response

**401 Unauthorized**

```json
{
  "success": false,
  "status": 401,
  "message": "Vui lòng đăng nhập để xem dòng sự kiện."
}
```

**403 Forbidden — sai role (không phải VT-02)**

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền xem dòng sự kiện truy xuất."
}
```

**403 Forbidden — lô hàng thuộc tổ chức khác (TC-03)**

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền xem dòng sự kiện của lô hàng này."
}
```

**404 Not Found**

```json
{
  "success": false,
  "status": 404,
  "message": "Lô hàng không tồn tại."
}
```

## 8. Backend xử lý

```
Client
  │
  ▼
GET /api/v1/shipments/{shipmentId}/chain-events
  │
  ▼
Lấy currentUser (SecurityContext)
  │
  ▼
Kiểm tra Role (VT-02) -> 403 nếu sai
  │
  ▼
Tìm Shipment theo shipmentId -> 404 nếu không có
  │
  ▼
So sánh organization_id của currentUser với Shipment -> 403 nếu khác tổ chức
  │
  ▼
ChainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId)
  │
  ▼
Map từng ChainEvent -> ChainEventResponse (kèm recordedByName, latitude/longitude)
  │
  ▼
Trả Response (200), data = danh sách (có thể rỗng)
```

## 9. Repository

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

**OrganizationUserRepository**

```java
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, UUID> {
    Optional<OrganizationUser> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);
}
```

Ghi chú: `findByShipmentIdOrderByRecordedAtAsc` trả về toàn bộ ChainEvent của lô hàng theo đúng thứ tự thời gian xảy ra (recorded_at), bao gồm cả sự kiện gốc và sự kiện đính chính, phục vụ trực tiếp việc hiển thị dòng sự kiện (timeline).

## 10. Phạm vi của Story

**Bao gồm**

- Truy vấn và hiển thị dòng sự kiện (timeline) của một lô hàng theo đúng thứ tự thời gian xảy ra.
- Kiểm tra quyền truy cập: chỉ tổ chức sở hữu lô hàng mới được xem dòng sự kiện.
- Hiển thị đầy đủ cả sự kiện gốc và sự kiện đính chính trong cùng dòng thời gian.
- Hiển thị thông tin người ghi (recordedByName) kèm mỗi sự kiện.
- Xử lý trường hợp lô hàng chưa có sự kiện nào (trả về danh sách rỗng).

**Không bao gồm**

- Ghi nhận sự kiện mới (thu hoạch, vận chuyển, đóng gói, thu mua) — thuộc các story khác trong Epic NCL-05.
- Đính chính sự kiện (is_correction = true) — thuộc chức năng khác.
- Tra cứu công khai dòng sự kiện theo mã truy xuất (QR) dành cho người tiêu dùng.
- Phân trang, lọc theo loại sự kiện hoặc xuất hồ sơ (export) — nằm ngoài phạm vi CV-03.

## 11. User Story liên quan

**NCL-05-CN-005 — Xem dòng sự kiện truy xuất**

Là Quản lý hợp tác xã, tôi muốn xem dòng sự kiện của một lô hàng theo thời gian, để kiểm tra hành trình đầy đủ trước khi xuất hồ sơ.

*Độ ưu tiên: Bắt buộc | Phụ trách: Thành viên hai | Trạng thái: Chưa thực hiện | Tham chiếu: QTN-08*

## 12. Danh sách công việc

*Chu kỳ áp dụng: Chu kỳ số ba.*

| Mã công việc | Tên công việc | Loại | Phụ trách | Trạng thái |
| --- | --- | --- | --- | --- |
| NCL-05-CN-005-CV-01 | Thiết kế màn hình xem dòng sự kiện | Thiết kế giao diện | Thành viên hai | Chưa thực hiện |
| NCL-05-CN-005-CV-02 | Phát triển giao diện xem dòng sự kiện | Phát triển phần giao diện | Thành viên hai | Chưa thực hiện |
| NCL-05-CN-005-CV-03 | Tối ưu truy vấn sự kiện | Phát triển phần máy chủ | Thành viên bốn | Chưa thực hiện |
| NCL-05-CN-005-CV-04 | Kiểm tra thứ tự và đính chính | Kiểm thử | Thành viên năm | Chưa thực hiện |

## 13. Test Cases

**TC-01: Luồng thành công**

| Mục | Nội dung |
| --- | --- |
| Điều kiện đầu vào | Lô hàng có sự kiện. |
| Hành động | Người quản lý xem dòng sự kiện. |
| Kết quả mong đợi | Dòng sự kiện hiển thị theo thời gian. |
| Dữ liệu liên quan | Danh sách sự kiện, người ghi. |
| Mức độ ưu tiên | Cao |

**TC-02: Dữ liệu rỗng**

| Mục | Nội dung |
| --- | --- |
| Điều kiện đầu vào | Lô hàng chưa có sự kiện. |
| Hành động | Người quản lý xem dòng sự kiện. |
| Kết quả mong đợi | Hệ thống hiển thị trạng thái chưa có sự kiện. |
| Dữ liệu liên quan | Danh sách sự kiện. |
| Mức độ ưu tiên | Cao |

**TC-03: Không có quyền**

| Mục | Nội dung |
| --- | --- |
| Điều kiện đầu vào | Lô của tổ chức khác. |
| Hành động | Người dùng xem dòng sự kiện. |
| Kết quả mong đợi | Hệ thống từ chối truy cập. |
| Dữ liệu liên quan | Tổ chức của lô. |
| Mức độ ưu tiên | Cao |

**TC-04: Ngoại lệ**

| Mục | Nội dung |
| --- | --- |
| Điều kiện đầu vào | Có sự kiện đính chính. |
| Hành động | Người quản lý xem dòng sự kiện. |
| Kết quả mong đợi | Hệ thống hiển thị cả sự kiện gốc và đính chính. |
| Dữ liệu liên quan | Sự kiện đính chính. |
| Mức độ ưu tiên | Cao |