API: Cảnh báo tem quét bất thường

NCL-08-CN-001 — Epic NCL-08: Cảnh báo, thu hồi lô và lịch sử hoạt động

Nhánh git: feature/scan-anomaly-alert

1. Thông tin chung

Mục tiêu

Cho phép hệ thống tự động theo dõi lượt quét mã truy xuất (TraceCode) theo thời gian và vị trí, phát hiện dấu hiệu tem bị nhân bản hoặc quét bất thường ở nhiều nơi khác nhau, tự động tạo cảnh báo (Alert) và gửi thông báo (Notification) cho Quản trị viên nền tảng cùng Quản lý hợp tác xã liên quan, đồng thời cho phép Quản trị viên xem, xử lý và lưu lại lịch sử các cảnh báo.

Nhật ký này phục vụ:

Phát hiện sớm dấu hiệu nhân bản tem / tem giả thông qua phân tích lượt quét (ScanLog).

Phân biệt rõ giữa quét bất thường (nhiều vị trí địa lý khác nhau) và quét hợp lệ (nhiều lượt nhưng cùng một vị trí, ví dụ tại điểm bán).

Gửi thông báo kịp thời cho Quản trị viên nền tảng và Quản lý hợp tác xã sở hữu lô hàng liên quan.

Ghi nhận lịch sử mỗi lần cảnh báo được tạo và xử lý, phục vụ truy vết và kiểm toán sau này.

2. Endpoint

GET /api/v1/alerts

Xem danh sách cảnh báo tem quét bất thường.

PATCH /api/v1/alerts/{alertId}/resolve

Đánh dấu một cảnh báo đã được xử lý và ghi lịch sử xử lý vào AuditLog.

Ghi chú: việc phát hiện bất thường và tạo cảnh báo được thực hiện tự động ở tầng service ngay sau khi một lượt quét (ScanLog) mới được ghi nhận; không có endpoint riêng để client chủ động kích hoạt việc phát hiện.

3. Điều kiện

Người dùng:

Phải đăng nhập và có vai trò VT-01 (Quản trị viên nền tảng) để xem và xử lý toàn bộ cảnh báo trên hệ thống.

Quản lý hợp tác xã (VT-02) chỉ được xem và xử lý các cảnh báo liên quan đến tổ chức của mình (thông qua organizationId).

Nếu người dùng không có vai trò phù hợp, hệ thống từ chối truy cập (TC-05).

Phát hiện bất thường:

Mỗi lượt quét mã (ScanLog) được ghi nhận kèm vị trí (location) và thời điểm quét (scanned_at).

Nếu số lượt quét ở các vị trí khác nhau vượt ngưỡng cấu hình trong một cửa sổ thời gian xác định, mã được đánh dấu nghi ngờ và hệ thống tạo cảnh báo (TC-01).

Nếu các lượt quét diễn ra nhiều lần nhưng cùng một vị trí (trong bán kính cho phép), hệ thống không đánh dấu bất thường (TC-02).

Khi cảnh báo được tạo, hệ thống gửi thông báo tới Quản trị viên nền tảng và Quản lý hợp tác xã sở hữu lô hàng liên quan (TC-03).

Mỗi cảnh báo được tạo phải lưu lại lịch sử (TC-04).

4. Business Rules

Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi dự kiến trong ScanAnomalyDetectionServiceImpl.

4.1 Ghi nhận lượt quét và kích hoạt kiểm tra (CV-01, CV-02)

Sau khi một bản ghi ScanLog mới được lưu thành công (thuộc chức năng tra cứu công khai), hệ thống kích hoạt sự kiện onScanRecorded, truyền vào trace_code_id vừa được quét để tiến hành kiểm tra ngưỡng bất thường.

4.2 Kiểm tra ngưỡng bất thường (TC-01, CV-01, CV-02)

Hệ thống lấy N lượt quét gần nhất (cấu hình, mặc định N = 10) của cùng trace_code_id trong cửa sổ thời gian T (cấu hình, mặc định 24 giờ). Nếu số vị trí quét khác nhau — được xem là khác nhau khi khoảng cách giữa hai vị trí vượt bán kính cho phép R (cấu hình, mặc định 5km) — lớn hơn hoặc bằng ngưỡng K (cấu hình, mặc định 3), mã được xác định là bất thường.

4.3 Loại trừ trường hợp cùng vị trí (TC-02)

Nếu toàn bộ các lượt quét trong cửa sổ thời gian nằm trong cùng bán kính cho phép R, hệ thống không tạo cảnh báo. Trường hợp này được xem là hành vi quét hợp lệ, ví dụ nhiều người tiêu dùng quét tem tại cùng một điểm bán hoặc trưng bày sản phẩm.

4.4 Tạo cảnh báo và gửi thông báo (TC-01, TC-03, CV-03)

Khi xác định mã bị quét bất thường, hệ thống thực hiện:

Tạo bản ghi Alert với type = SCAN_ANOMALY, related_entity_type = "TraceCode", related_entity_id = trace_code_id.

Tính severity: MEDIUM nếu số vị trí vượt ngưỡng từ 1–2 lần, HIGH nếu vượt ngưỡng từ 3 lần trở lên.

Lưu details (JSON) gồm danh sách vị trí, thời gian quét và tổng số lượt quét trong cửa sổ thời gian.

Tạo Notification cho toàn bộ Quản trị viên nền tảng và Quản lý hợp tác xã sở hữu Shipment tương ứng với trace_code_id.

4.5 Lưu lịch sử cảnh báo (TC-04)

Ngay sau khi tạo Alert thành công, hệ thống ghi một bản ghi AuditLog với action = CREATE_SCAN_ANOMALY_ALERT, resource_type = "Alert", resource_id là id của Alert vừa tạo, new_values chứa {trace_code_id, locations, scan_count, severity}.

4.6 Xử lý cảnh báo (TC-05, TC-06)

Khi gọi endpoint PATCH /api/v1/alerts/{alertId}/resolve, hệ thống kiểm tra vai trò người dùng (VT-01, hoặc VT-02 nếu cảnh báo thuộc tổ chức của họ). Nếu không đủ quyền, hệ thống ném BusinessException:

"Bạn không có quyền xử lý cảnh báo này."

Nếu hợp lệ, hệ thống cập nhật Alert.status = RESOLVED, resolved_at, resolved_by, đồng thời ghi một bản ghi AuditLog với action = RESOLVE_SCAN_ANOMALY_ALERT.

5. Response DTO

public class AlertResponse {

private UUID id;

private String type;

private String relatedEntityType;

private UUID relatedEntityId;

private String severity;

private AlertDetails details;

private String status;

private LocalDateTime createdAt;

private LocalDateTime resolvedAt;

private UUID resolvedBy;

}

public class AlertDetails {

private List<ScanPoint> locations;

private Integer scanCount;

private Integer thresholdConfigured;

}

public class ScanPoint {

private Double latitude;

private Double longitude;

private LocalDateTime scannedAt;

}

public class AlertListResponse {

private List<AlertResponse> content;

private Integer totalElements;

private Integer totalPages;

private Integer page;

private Integer size;

}

public class ResolveAlertRequest {

private String resolutionNote;

}

public class ResolveAlertResponse {

private UUID id;

private String status;

private LocalDateTime resolvedAt;

private UUID resolvedBy;

private UUID auditLogId;

}

Ghi chú: khi status = ALL và không có cảnh báo nào phù hợp, content trả về danh sách rỗng, totalElements = 0.

6. Response

Ví dụ request

GET http://localhost:8080/api/v1/alerts?status=PENDING&fromDate=2026-07-01&toDate=2026-07-31

HTTP 200 OK — có cảnh báo (TC-01)

{

"success": true,

"status": 200,

"data": {

"content": [

{

"id": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",

"type": "SCAN_ANOMALY",

"relatedEntityType": "TraceCode",

"relatedEntityId": "9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f",

"severity": "HIGH",

"details": {

"locations": [

{ "latitude": 21.0285, "longitude": 105.8542, "scannedAt": "2026-07-30T08:12:00Z" },

{ "latitude": 10.7769, "longitude": 106.7009, "scannedAt": "2026-07-30T08:15:00Z" },

{ "latitude": 16.0544, "longitude": 108.2022, "scannedAt": "2026-07-30T08:20:00Z" }

],

"scanCount": 3,

"thresholdConfigured": 3

},

"status": "PENDING",

"createdAt": "2026-07-30T08:20:05Z",

"resolvedAt": null,

"resolvedBy": null

}

],

"totalElements": 1,

"totalPages": 1,

"page": 0,

"size": 20

},

"timestamp": "2026-07-31T02:05:12.551123400Z"

}

HTTP 200 OK — không có cảnh báo phù hợp

{

"success": true,

"status": 200,

"data": {

"content": [],

"totalElements": 0,

"totalPages": 0,

"page": 0,

"size": 20

},

"timestamp": "2026-07-31T02:05:12.551123400Z"

}

HTTP 200 OK — xử lý cảnh báo thành công

{

"success": true,

"status": 200,

"data": {

"id": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",

"status": "RESOLVED",

"resolvedAt": "2026-07-31T02:10:00Z",

"resolvedBy": "7f6e5d4c-3333-4a2a-9f3d-1a2b3c4d5e6f",

"auditLogId": "b3f1a2e0-6c1a-4e2a-9f3d-1a2b3c4d5e6f"

},

"timestamp": "2026-07-31T02:10:00.001000000Z"

}

7. Error Response

403 Forbidden — không có quyền (TC-05)

{

"success": false,

"status": 403,

"message": "Bạn không có quyền xử lý cảnh báo này."

}

404 Not Found — cảnh báo không tồn tại

{

"success": false,

"status": 404,

"message": "Không tìm thấy cảnh báo."

}

400 Bad Request — cảnh báo đã được xử lý (TC-06)

{

"success": false,

"status": 400,

"message": "Cảnh báo này đã được xử lý trước đó."

}

8. Backend xử lý

ScanLog mới được ghi nhận (từ chức năng tra cứu công khai)

│

▼

Kích hoạt onScanRecorded(trace_code_id)

│

▼

Lấy N lượt quét gần nhất trong cửa sổ thời gian T

│

▼

Số vị trí khác nhau (> bán kính R) >= ngưỡng K ?

│                              │

Có                             Không

│                              │

▼                              ▼

Tạo Alert (SCAN_ANOMALY)      Không tạo cảnh báo (TC-02)

│

▼

Gửi Notification (Quản trị viên nền tảng + Quản lý HTX liên quan)

│

▼

Ghi AuditLog (CREATE_SCAN_ANOMALY_ALERT)

----

GET/PATCH /api/v1/alerts(/{alertId}/resolve)

│

▼

Kiểm tra Role = VT-01 hoặc VT-02 (phạm vi tổ chức) -> 403 nếu sai vai trò

│

▼

(GET) Lọc theo status/fromDate/toDate/organizationId -> trả danh sách

│

▼

(PATCH) Kiểm tra Alert tồn tại -> 404 nếu không có

│

▼

Kiểm tra Alert.status != RESOLVED -> 400 nếu đã xử lý

│

▼

Cập nhật status = RESOLVED + ghi AuditLog (RESOLVE_SCAN_ANOMALY_ALERT)

│

▼

Trả Response (200)

9. Repository

ScanLogRepository

public interface ScanLogRepository extends JpaRepository<ScanLog, UUID> {

@Query("SELECT s FROM ScanLog s WHERE s.traceCodeId = :traceCodeId " +

"AND s.scannedAt >= :sinceTime ORDER BY s.scannedAt DESC")

List<ScanLog> findRecentByTraceCodeId(

@Param("traceCodeId") UUID traceCodeId,

@Param("sinceTime") LocalDateTime sinceTime);

}

AlertRepository

public interface AlertRepository extends JpaRepository<Alert, UUID> {

Page<Alert> findByTypeAndStatus(String type, String status, Pageable pageable);

@Query("SELECT a FROM Alert a WHERE a.type = :type " +

"AND a.createdAt BETWEEN :fromDate AND :toDate")

Page<Alert> findByTypeAndCreatedAtBetween(

@Param("type") String type,

@Param("fromDate") LocalDateTime fromDate,

@Param("toDate") LocalDateTime toDate,

Pageable pageable);

}

NotificationRepository

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

List<Notification> saveAll(Iterable<Notification> notifications);

}

AuditLogRepository

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

AuditLog save(AuditLog auditLog);

}

Ghi chú: việc tính khoảng cách giữa hai vị trí (Haversine) và xác định ngưỡng bất thường được thực hiện ở tầng service (ScanAnomalyDetectionServiceImpl), không đặt trong tầng repository.

10. Phạm vi của Story

Bao gồm

Tự động phát hiện lượt quét mã bất thường dựa trên vị trí và thời gian.

Phân biệt quét bất thường với quét hợp lệ cùng vị trí.

Tự động tạo cảnh báo (Alert) và gửi thông báo (Notification) cho Quản trị viên nền tảng và Quản lý hợp tác xã liên quan.

Xem danh sách cảnh báo tem quét bất thường, có bộ lọc theo trạng thái và khoảng thời gian.

Xử lý (đánh dấu đã xem xét) cảnh báo và lưu lịch sử xử lý (AuditLog).

Kiểm soát quyền truy cập theo vai trò VT-01 (toàn hệ thống) và VT-02 (phạm vi tổ chức).

Không bao gồm

Thu hồi lô hàng (Recall) khi phát hiện tem giả — thuộc story khác trong Epic NCL-08.

Phát hiện sự kiện sai lô (ChainEvent bất thường) — thuộc story khác trong Epic NCL-08.

Thống kê, biểu đồ tổng hợp lượt quét (ScanLog dashboard) — thuộc chức năng khác.

Cấu hình ngưỡng (N, T, R, K) qua giao diện quản trị — giả định các giá trị này được cấu hình sẵn ở tầng hệ thống, không thuộc phạm vi story này.

11. User Story liên quan

NCL-08-CN-001 — Cảnh báo tem quét bất thường

Là Quản trị viên nền tảng, tôi muốn nhận cảnh báo khi một mã bị quét bất thường ở nhiều nơi, để kịp thời phát hiện tem giả.

Độ ưu tiên: Bắt buộc | Phụ trách: Thành viên bốn | Trạng thái: Chưa thực hiện | Tham chiếu: QTN-10

12. Danh sách công việc

Chu kỳ áp dụng: Chu kỳ số bốn.

13. Test Cases

TC-01: Luồng thành công

TC-02: Ngoại lệ

TC-03: Gửi thông báo

TC-04: Lưu lịch sử

TC-05: Không có quyền

TC-06: Xử lý cảnh báo đã đóng