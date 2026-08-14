API: Thu hồi lô

NCL-08-CN-003 — Epic NCL-08: Cảnh báo, thu hồi lô và lịch sử hoạt động

Nhánh git: feature/shipment-recall

1. Thông tin chung

Mục tiêu

Cho phép Quản lý hợp tác xã thu hồi một lô hàng (Shipment) khi phát hiện vấn đề về an toàn hoặc chất lượng. Khi lô được thu hồi, hệ thống chuyển trạng thái lô hàng và toàn bộ mã truy xuất (TraceCode) liên quan sang RECALLED, bật cảnh báo công khai trên trang tra cứu để bảo vệ người tiêu dùng, đồng thời gửi thông báo cho các bên liên quan và lưu lại lịch sử thao tác (AuditLog) để phục vụ truy vết.

Nhật ký này phục vụ:

•  Cho phép Quản lý hợp tác xã chủ động thu hồi lô hàng thuộc tổ chức của mình khi phát hiện sự cố an toàn.

•  Đảm bảo mọi mã truy xuất (TraceCode) của lô bị thu hồi đều hiển thị cảnh báo công khai, không thể ẩn (QTN-09).

•  Ngăn chặn thao tác thu hồi trái phép trên lô hàng của tổ chức khác.

•  Gửi thông báo kịp thời cho các bên liên quan khi lô bị thu hồi.

•  Ghi nhận lịch sử mỗi lần thu hồi vào AuditLog, phục vụ truy vết và kiểm toán sau này.

2. Endpoint

POST /api/v1/shipments/{shipmentId}/recall

Thu hồi một lô hàng đang hiệu lực.

GET /api/v1/shipments/{shipmentId}/recall

Xem thông tin thu hồi hiện tại của một lô hàng (nếu có).

Ghi chú: việc hiển thị cảnh báo thu hồi trên trang tra cứu công khai (dành cho người tiêu dùng) thuộc phạm vi story NCL-06-CN-001; endpoint trong tài liệu này chỉ xử lý thao tác thu hồi và cập nhật trạng thái nguồn dữ liệu mà trang tra cứu sử dụng.

3. Điều kiện

Người dùng:

•  Phải đăng nhập và có vai trò VT-02 (Quản lý hợp tác xã) và thuộc tổ chức sở hữu Shipment (thông qua Shipment.organization_id) để thực hiện thu hồi.

•  Quản trị viên nền tảng (VT-01) có thể thu hồi thay khi được ủy quyền xử lý sự cố liên tổ chức.

•  Nếu người dùng không thuộc tổ chức sở hữu lô hàng và không phải VT-01, hệ thống từ chối thao tác (TC-02).

Điều kiện về lô hàng:

•  Lô hàng (Shipment) phải đang ở trạng thái hiệu lực (khác RECALLED) tại thời điểm thu hồi (TC-01).

•  Nếu lô hàng đã ở trạng thái RECALLED trước đó, hệ thống từ chối và báo lỗi lô đã thu hồi (TC-03).

•  Lý do thu hồi (reason) là bắt buộc và không được để trống.

4. Business Rules

Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi dự kiến trong ShipmentRecallServiceImpl.

4.1 Kiểm tra quyền thu hồi (TC-02, CV-05)

Khi gọi POST /api/v1/shipments/{shipmentId}/recall, hệ thống kiểm tra vai trò người dùng. Nếu là VT-02, hệ thống đối chiếu organization_id của người dùng với Shipment.organization_id; nếu không khớp và người dùng không phải VT-01, hệ thống ném BusinessException:

"Bạn không có quyền thu hồi lô hàng này."

4.2 Kiểm tra trạng thái lô hàng (TC-03, CV-03)

Hệ thống kiểm tra Shipment.status. Nếu status = RECALLED, hệ thống ném BusinessException:

"Lô hàng này đã được thu hồi trước đó."

4.3 Thực hiện thu hồi (TC-01, CV-03)

Nếu hợp lệ, hệ thống thực hiện trong một transaction:

•  Tạo bản ghi Recall với shipment_id, reason, recalled_by = id người thực hiện, recalled_at = thời điểm hiện tại, status = ACTIVE.

•  Cập nhật Shipment.status = RECALLED.

•  Cập nhật toàn bộ TraceCode có shipment_id tương ứng sang status = RECALLED.

4.4 Kích hoạt cảnh báo công khai (CV-04, QTN-09)

Sau khi cập nhật trạng thái, trang tra cứu công khai (dựa trên TraceCode.status hoặc Shipment.status) sẽ hiển thị cảnh báo thu hồi ngay khi người tiêu dùng quét mã. Hệ thống không cung cấp cơ chế ẩn hoặc gỡ cảnh báo này (QTN-09: không cho ẩn cảnh báo thu hồi).

4.5 Gửi thông báo (TC-04, CV-03)

Ngay sau khi thu hồi thành công, hệ thống tạo Notification (type = ALERT) gửi tới: Quản trị viên nền tảng (toàn bộ VT-01), các thành viên khác của tổ chức sở hữu lô hàng, và Doanh nghiệp thu mua có ChainEvent liên quan đến shipment_id (nếu có).

4.6 Lưu lịch sử thu hồi (CV-03)

Ngay sau khi tạo Recall thành công, hệ thống ghi một bản ghi AuditLog với action = RECALL_SHIPMENT, resource_type = "Shipment", resource_id là id của Shipment, old_values chứa {status: trạng thái cũ}, new_values chứa {status: "RECALLED", reason, recall_id}.

5. Response DTO

public class RecallRequest {

private String reason;

}

public class RecallResponse {

private UUID id;

private UUID shipmentId;

private String reason;

private UUID recalledBy;

private LocalDateTime recalledAt;

private String status;

private String shipmentStatus;

private Integer traceCodesUpdated;

private UUID auditLogId;

}

public class RecallInfoResponse {

private UUID shipmentId;

private Boolean recalled;

private String reason;

private LocalDateTime recalledAt;

}

Ghi chú: traceCodesUpdated cho biết số lượng TraceCode đã được chuyển sang trạng thái RECALLED cùng lô hàng.

6. Response

Ví dụ request

POST http://localhost:8080/api/v1/shipments/9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f/recall

{

"reason": "Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép"

}

HTTP 200 OK — thu hồi thành công (TC-01)

{

"success": true,

"status": 200,

"data": {

"id": "b3f1a2e0-6c1a-4e2a-9f3d-1a2b3c4d5e6f",

"shipmentId": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",

"reason": "Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép",

"recalledBy": "7f6e5d4c-3333-4a2a-9f3d-1a2b3c4d5e6f",

"recalledAt": "2026-07-31T02:10:00Z",

"status": "ACTIVE",

"shipmentStatus": "RECALLED",

"traceCodesUpdated": 480,

"auditLogId": "c4e2b3f1-7d2b-4e2a-9f3d-1a2b3c4d5e6f"

},

"timestamp": "2026-07-31T02:10:00.001000000Z"

}

HTTP 200 OK — xem thông tin thu hồi (đã bị thu hồi)

{

"success": true,

"status": 200,

"data": {

"shipmentId": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",

"recalled": true,

"reason": "Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép",

"recalledAt": "2026-07-31T02:10:00Z"

},

"timestamp": "2026-07-31T02:15:00.001000000Z"

}

7. Error Response

403 Forbidden — không có quyền (TC-02)

{

"success": false,

"status": 403,

"message": "Bạn không có quyền thu hồi lô hàng này."

}

400 Bad Request — lô hàng đã thu hồi trước đó (TC-03)

{

"success": false,

"status": 400,

"message": "Lô hàng này đã được thu hồi trước đó."

}

404 Not Found — lô hàng không tồn tại

{

"success": false,

"status": 404,

"message": "Không tìm thấy lô hàng."

}

400 Bad Request — thiếu lý do thu hồi

{

"success": false,

"status": 400,

"message": "Lý do thu hồi không được để trống."

}

8. Backend xử lý

POST /api/v1/shipments/{shipmentId}/recall

│

▼

Kiểm tra Role = VT-01, hoặc VT-02 cùng organization_id với Shipment

-> 403 nếu sai vai trò / khác tổ chức (TC-02)

│

▼

Kiểm tra Shipment tồn tại -> 404 nếu không có

│

▼

Kiểm tra Shipment.status != RECALLED -> 400 nếu đã thu hồi (TC-03)

│

▼

Tạo bản ghi Recall (reason, recalled_by, recalled_at, status=ACTIVE)

│

▼

Cập nhật Shipment.status = RECALLED

│

▼

Cập nhật toàn bộ TraceCode liên quan -> status = RECALLED

│

▼

Gửi Notification (Quản trị viên nền tảng + tổ chức sở hữu + bên liên quan) (TC-04)

│

▼

Ghi AuditLog (action = RECALL_SHIPMENT)

│

▼

Trả Response (200)

---

Trang tra cứu công khai (quét TraceCode)

│

▼

Kiểm tra TraceCode.status hoặc Shipment.status = RECALLED

│

▼

Hiển thị cảnh báo thu hồi công khai, không cho ẩn (QTN-09)

9. Repository

ShipmentRepository

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

Optional<Shipment> findById(UUID id);

}

RecallRepository

public interface RecallRepository extends JpaRepository<Recall, UUID> {

@Query("SELECT r FROM Recall r WHERE r.shipmentId = :shipmentId " +

"ORDER BY r.recalledAt DESC")

Optional<Recall> findLatestByShipmentId(

@Param("shipmentId") UUID shipmentId);

}

TraceCodeRepository

public interface TraceCodeRepository extends JpaRepository<TraceCode, UUID> {

@Modifying

@Query("UPDATE TraceCode t SET t.status = 'RECALLED' " +

"WHERE t.shipmentId = :shipmentId")

int updateStatusToRecalledByShipmentId(

@Param("shipmentId") UUID shipmentId);

}

NotificationRepository

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

List<Notification> saveAll(Iterable<Notification> notifications);

}

AuditLogRepository

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

AuditLog save(AuditLog auditLog);

}

Ghi chú: việc cập nhật hàng loạt TraceCode sang RECALLED được thực hiện trong cùng transaction với việc tạo Recall và cập nhật Shipment, đảm bảo tính nhất quán dữ liệu (ShipmentRecallServiceImpl).

10. Phạm vi của Story

Bao gồm

•  Thu hồi lô hàng (Shipment) đang hiệu lực theo yêu cầu của Quản lý hợp tác xã sở hữu lô.

•  Kiểm soát quyền theo tổ chức: chỉ tổ chức sở hữu lô (hoặc VT-01) mới được thu hồi.

•  Cập nhật trạng thái Shipment và toàn bộ TraceCode liên quan sang RECALLED.

•  Kích hoạt điều kiện để trang tra cứu công khai hiển thị cảnh báo thu hồi (QTN-09).

•  Gửi thông báo (Notification) cho các bên liên quan khi lô bị thu hồi.

•  Lưu lịch sử thao tác thu hồi vào AuditLog.

Không bao gồm

•  Giao diện và logic hiển thị cảnh báo trên trang tra cứu công khai — thuộc story NCL-06-CN-001.

•  Hủy/khôi phục trạng thái thu hồi (un-recall) — chưa thuộc phạm vi story này.

•  Cảnh báo tem quét bất thường (SCAN_ANOMALY) — thuộc story NCL-08-CN-001.

•  Phát hiện sự kiện sai lô (ChainEvent bất thường) — thuộc story khác trong Epic NCL-08.

•  Thu hồi hàng loạt nhiều lô cùng lúc — giả định mỗi lần chỉ thu hồi một lô.

11. User Story liên quan

NCL-08-CN-003 — Thu hồi lô

Là Quản lý hợp tác xã, tôi muốn thu hồi một lô khi phát hiện vấn đề an toàn, để cảnh báo người tiêu dùng và dừng lưu thông lô đó.

Độ ưu tiên: Bắt buộc | Phụ trách: Thành viên bốn | Trạng thái: Chưa thực hiện | Tham chiếu: QTN-09

12. Danh sách công việc

Chu kỳ áp dụng: Chu kỳ số bốn.

13. Test Cases

TC-01: Luồng thành công

TC-02: Không có quyền

TC-03: Sai trạng thái

TC-04: Gửi thông báo