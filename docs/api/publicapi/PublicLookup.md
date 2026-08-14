# API: Tra cứu công khai khi quét mã



NCL-06-CN-001 — Epic NCL-06: Tra cứu công khai cho người tiêu dùng

Nhánh git: feature/public-trace-lookup


## 1. Thông tin chung

Mục tiêu

Cho phép Người tiêu dùng tra cứu quét mã truy xuất (TraceCode) in trên sản phẩm để xem trang công khai hiển thị thông tin lô hàng (Shipment) và dòng sự kiện (ChainEvent) đã được ghi nhận, giúp người mua yên tâm về nguồn gốc trước khi mua, mà không cần đăng nhập.

Nhật ký này phục vụ:

Nâng niềm tin và giá trị nông sản thông qua minh bạch hành trình sản phẩm.

Cho phép người tiêu dùng xem hành trình công khai của lô hàng ngay khi quét mã, không cần tài khoản.

Đảm bảo trang công khai chỉ hiển thị dữ liệu đã được phép công bố, không lộ dữ liệu nội bộ.

Cảnh báo rõ ràng khi lô hàng đã bị thu hồi, bảo vệ người tiêu dùng.


## 2. Endpoint


```http
GET /api/v1/public/trace/{codeValue}
```

Ghi chú tham số

Endpoint không yêu cầu đăng nhập (public), chỉ hỗ trợ xem, không có thao tác ghi hay sửa dữ liệu (theo QTN-12).


## 3. Điều kiện

Người dùng:

Không yêu cầu đăng nhập, không yêu cầu vai trò (public endpoint).

Lô hàng (xác định qua mã truy xuất) phải:

Tồn tại trong hệ thống (mã truy xuất hợp lệ).

Tem (TraceCode) đã ở trạng thái ACTIVATED (đã kích hoạt).

Nếu lô hàng đã bị thu hồi (RECALLED), trang công khai vẫn hiển thị nhưng kèm cảnh báo thu hồi rõ ràng, không chặn truy cập.


## 4. Business Rules

Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi dự kiến trong PublicTraceServiceImpl.getPublicTrace().


## 4.1 Kiểm tra tồn tại mã truy xuất (TC-02)

Hệ thống tìm TraceCode theo codeValue. Nếu không tồn tại, hệ thống ném BusinessException:

"Mã lô hàng không tồn tại."


## 4.2 Kiểm tra trạng thái tem (TC-03)

Tem (TraceCode) phải đang ở trạng thái ACTIVATED. Nếu tem chưa được kích hoạt (DRAFT hoặc CODE_PRINTED), hệ thống chặn hiển thị hành trình và báo:

"Tem chưa có hiệu lực, chưa thể tra cứu hành trình."


## 4.3 Xác định Lô hàng (Shipment) tương ứng

Từ TraceCode tìm được, hệ thống xác định Shipment liên kết (thông qua TraceCode.shipment_id).


## 4.4 Kiểm tra trạng thái thu hồi của Lô hàng (TC-04)

Nếu Shipment.status = RECALLED, hệ thống vẫn trả về trang công khai cùng dòng sự kiện, nhưng gắn cờ cảnh báo:

"Lô hàng này đã bị thu hồi. Vui lòng ngừng sử dụng và liên hệ nhà cung cấp."


## 4.5 Lấy dòng sự kiện (ChainEvent) của lô hàng

Hệ thống truy vấn toàn bộ ChainEvent theo shipment_id, sắp xếp theo recorded_at tăng dần, để dựng dòng thời gian (timeline) hành trình lô hàng.


## 4.6 Kiểm soát chỉ hiển thị công khai (CV-04)

Trước khi trả response, hệ thống lọc và chỉ ánh xạ các trường được phép công bố công khai (tên sản phẩm, mã lô, địa điểm, thời gian sự kiện). Các trường nội bộ như recorded_by (nội bộ), created_at hệ thống, thông tin tài khoản người ghi sự kiện... không được đưa vào response công khai.


## 5. Response DTO

public class PublicTraceResponse {
private String codeValue;
private String productName;
private String shipmentCode;
private String shipmentStatus;
private Boolean recalled;
private String recallMessage;
private List<PublicChainEventItem> events;
}

public class PublicChainEventItem {
private String eventType;
private Map<String, Object> eventData;
private LocalDateTime recordedAt;
}

Ghi chú: eventData chỉ chứa các trường công khai tương ứng với từng event_type (ví dụ TRANSPORT: fromLocation, toLocation). Danh sách events được sắp xếp theo recordedAt tăng dần để hiển thị đúng thứ tự hành trình.


## 6. Response

Ví dụ request


```http
GET http://localhost:8080/api/v1/public/trace/HX00000029
```

HTTP 200 OK — lô hàng còn hiệu lực

Response

```json
{
"success": true,
"status": 200,
"data": {
"codeValue": "HX00000029",
"productName": "Chè Long Cốc",
"shipmentCode": "LH-2026-0029",
"shipmentStatus": "ACTIVATED",
"recalled": false,
"recallMessage": null,
"events": [
{
"eventType": "TRANSPORT",
"eventData": {
"fromLocation": "Xã Long Cốc, huyện Tân Sơn, Phú Thọ",
"toLocation": "Kho trung chuyển Việt Trì, Phú Thọ"
},
"recordedAt": "2026-07-24T09:00:00"
}
]
},
"timestamp": "2026-07-28T02:05:12.551123400Z"
}

HTTP 200 OK — lô hàng đã thu hồi (TC-04)

```json
{
"success": true,
"status": 200,
"data": {
"codeValue": "HX00000029",
"productName": "Chè Long Cốc",
"shipmentCode": "LH-2026-0029",
"shipmentStatus": "RECALLED",
"recalled": true,
"recallMessage": "Lô hàng này đã bị thu hồi. Vui lòng ngừng sử dụng và liên hệ nhà cung cấp.",
"events": [ ... ]
},
"timestamp": "2026-07-28T02:05:12.551123400Z"
}


## 7. Error Response

404 Not Found — mã không tồn tại (TC-02)

```json
{
"success": false,
"status": 404,
"message": "Mã lô hàng không tồn tại."
}

409 Conflict — tem chưa kích hoạt (TC-03)

```json
{
"success": false,
"status": 409,
"message": "Tem chưa có hiệu lực, chưa thể tra cứu hành trình."
}


## 8. Backend xử lý

Client
│
▼
GET /api/v1/public/trace/{codeValue}
│
▼
Tìm TraceCode theo codeValue -> 404 nếu không có
│
▼
Kiểm tra trạng thái Tem = ACTIVATED -> 409 nếu chưa kích hoạt
│
▼
Xác định Shipment liên kết từ TraceCode
│
▼
Kiểm tra Shipment.status = RECALLED -> gắn cờ recalled + recallMessage nếu có
│
▼
Lấy danh sách ChainEvent theo shipment_id, sắp xếp theo recorded_at
│
▼
Lọc, chỉ map các trường công khai (CV-04) sang PublicTraceResponse
│
▼
Trả Response (200)


## 9. Repository

TraceCodeRepository

public interface TraceCodeRepository extends JpaRepository<TraceCode, UUID> {
Optional<TraceCode> findByCodeValue(String codeValue);
}

ShipmentRepository

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
Optional<Shipment> findById(UUID id);
}

ChainEventRepository

public interface ChainEventRepository extends JpaRepository<ChainEvent, UUID> {
List<ChainEvent> findByShipmentIdOrderByRecordedAtAsc(UUID shipmentId);
}

Ghi chú: tái sử dụng findByShipmentIdOrderByRecordedAtAsc đã có từ chức năng ghi sự kiện, phục vụ dựng dòng thời gian (timeline) công khai theo đúng thứ tự thời gian xảy ra.

10. Phạm vi của Story

Bao gồm

Quét mã truy xuất (TraceCode) để xem trang tra cứu công khai.

Hiển thị thông tin lô hàng và dòng sự kiện (timeline) đã kích hoạt tem.

Cảnh báo rõ ràng khi lô hàng đã bị thu hồi.

Chỉ hiển thị công khai ở chế độ xem, không đăng nhập, không sửa dữ liệu (QTN-12).

Không bao gồm

Gửi phản ánh sản phẩm — thuộc chức năng khác trong Epic NCL-06 (NCL-06-CN-003).

Kích hoạt tem (TraceCode.status → ACTIVE) — thuộc chức năng khác.

Thu hồi lô hàng (Shipment.status → RECALLED) — thuộc chức năng khác.

Ghi các sự kiện chuỗi cung ứng (thu hoạch, đóng gói, thu mua, vận chuyển) — thuộc các story khác.

11. User Story liên quan

NCL-06-CN-001 — Tra cứu công khai khi quét mã

Là Người tiêu dùng tra cứu, tôi muốn quét mã trên sản phẩm để xem hành trình của lô hàng, để yên tâm về nguồn gốc trước khi mua.

Độ ưu tiên: Bắt buộc | Phụ trách: Thành viên ba | Trạng thái: Chưa thực hiện | Tham chiếu: QTN-12

12. Danh sách công việc

Chu kỳ áp dụng: Chu kỳ số bốn.

13. Test Cases

TC-01: Luồng thành công

TC-02: Dữ liệu không hợp lệ

TC-03: Sai trạng thái

TC-04: Ngoại lệ