API: Nhận thông báo

NCL-08-CN-005 — Epic NCL-08: Cảnh báo, thu hồi lô và lịch sử hoạt động

Nhánh git: feature/notification-inbox

1. Thông tin chung

Mục tiêu

Cho phép người dùng — đặc biệt là Người ghi sự kiện — nhận và theo dõi thông báo về việc cần làm và các cảnh báo liên quan phát sinh trong chuỗi, để xử lý kịp thời. Khi có sự kiện nghiệp vụ phát sinh (cảnh báo bất thường, thu hồi lô, mời tham gia tổ chức, chứng nhận sắp hết hạn...), hệ thống tạo bản ghi Notification cho (các) người dùng liên quan, hiển thị trong hộp thông báo, và cho phép đánh dấu đã đọc để lưu lại lịch sử.

Nhật ký này phục vụ:

•  Cho phép người dùng xem danh sách thông báo (việc cần làm và cảnh báo) liên quan đến tài khoản của mình.

•  Đảm bảo thông báo được tạo và hiển thị kịp thời ngay khi có sự kiện/cảnh báo phát sinh trong chuỗi (QTN-07).

•  Hiển thị đúng trạng thái khi người dùng chưa có thông báo nào (hộp thông báo rỗng).

•  Cho phép người dùng đánh dấu thông báo đã đọc và lưu lại trạng thái/lịch sử đọc.

•  Đảm bảo người dùng chỉ nhận và thao tác trên thông báo thuộc về chính mình.

2. Endpoint

GET /api/v1/notifications

Lấy danh sách thông báo của người dùng đang đăng nhập, sắp xếp theo thời điểm tạo mới nhất, hỗ trợ phân trang và lọc theo trạng thái đọc.

GET /api/v1/notifications/unread-count

Lấy số lượng thông báo chưa đọc của người dùng đang đăng nhập, dùng để hiển thị badge trên hộp thông báo.

Không có tham số đầu vào ngoài thông tin xác thực (token) của người dùng.

PATCH /api/v1/notifications/{notificationId}/read

Đánh dấu một thông báo cụ thể là đã đọc.

Ghi chú: việc xác định loại sự kiện nào phát sinh thông báo (ALERT, TASK, INFO) và cơ chế tạo/phân phối thông báo tới người dùng liên quan được xử lý nội bộ ở phía các service nghiệp vụ (ChainEvent, Alert, Recall, Invitation...); các endpoint trong tài liệu này chỉ xử lý việc người dùng nhận, xem và đánh dấu đã đọc thông báo.

3. Điều kiện

Người dùng:

•  Phải đăng nhập (áp dụng cho mọi vai trò nội bộ VT-01 → VT-05; không áp dụng cho người tiêu dùng ẩn danh tra cứu công khai).

•  Chỉ được xem danh sách thông báo và đánh dấu đã đọc trên các thông báo thuộc về chính mình (Notification.user_id = id người dùng hiện tại).

•  Nếu thao tác đánh dấu đã đọc trên thông báo không thuộc về mình, hệ thống từ chối (TC-04).

Điều kiện về thông báo:

•  Thông báo (Notification) phải tồn tại khi thực hiện đánh dấu đã đọc; nếu không tồn tại, hệ thống trả lỗi không tìm thấy.

•  Nếu người dùng chưa có thông báo nào, hệ thống trả về danh sách rỗng kèm trạng thái thành công, không phải lỗi (TC-02).

•  Một thông báo chỉ được đánh dấu đã đọc một lần; nếu đã is_read = true, thao tác đánh dấu lại không gây lỗi nhưng không cập nhật lại read_at.

4. Business Rules

Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi dự kiến trong NotificationServiceImpl.

4.1 Xác định loại thông báo cần phát (CV-01)

Hệ thống phân loại thông báo theo trường type khi phát sinh:

•  ALERT — cảnh báo cần chú ý ngay: tem quét bất thường (SCAN_ANOMALY), lô hàng bị thu hồi (RECALL_SHIPMENT), chứng nhận sắp/đã hết hạn (CERT_EXPIRY)...

•  TASK — việc cần xử lý: lời mời tham gia tổ chức (Invitation), lô sản xuất chờ phê duyệt (ProductionLot ở trạng thái PENDING)...

•  INFO — thông tin chung không yêu cầu hành động ngay.

4.2 Tạo và gửi thông báo (TC-01, CV-02)

Khi một sự kiện nghiệp vụ phát sinh thông báo (ChainEvent, Alert, Recall, Invitation...), hệ thống tạo (các) bản ghi Notification tương ứng với mỗi user_id liên quan, gồm type, title, content, is_read = false, created_at = thời điểm hiện tại. NotificationService phân phối đồng thời tới tất cả người dùng liên quan (theo vai trò, theo tổ chức sở hữu, hoặc theo người tham gia sự kiện cụ thể), đảm bảo độ trễ tạo thông báo trong phạm vi cho phép của QTN-07.

4.3 Hiển thị danh sách thông báo (TC-01, TC-02, CV-03)

Khi gọi GET /api/v1/notifications, hệ thống truy vấn toàn bộ Notification có user_id = người dùng hiện tại, sắp xếp created_at giảm dần, áp dụng phân trang và lọc theo isRead nếu có. Nếu người dùng chưa có thông báo nào, hệ thống trả về data là danh sách rỗng ([]) cùng success = true, không trả lỗi (TC-02).

4.4 Đếm số thông báo chưa đọc (CV-03)

Khi gọi GET /api/v1/notifications/unread-count, hệ thống đếm số Notification có user_id = người dùng hiện tại và is_read = false, trả về unreadCount.

4.5 Kiểm tra quyền đánh dấu đã đọc (TC-04)

Khi gọi PATCH /api/v1/notifications/{notificationId}/read, hệ thống tìm Notification theo id; nếu không tồn tại, ném BusinessException:

"Không tìm thấy thông báo."

Nếu tồn tại nhưng Notification.user_id khác với người dùng hiện tại, hệ thống ném BusinessException:

"Bạn không có quyền thao tác trên thông báo này."

4.6 Đánh dấu đã đọc và lưu lịch sử (TC-03, CV-04)

Nếu hợp lệ và is_read đang là false, hệ thống cập nhật is_read = true, read_at = thời điểm hiện tại trong một transaction. Trạng thái đã đọc được lưu lại làm cơ sở cho lịch sử thông báo của người dùng; nếu thông báo đã ở trạng thái đã đọc từ trước, hệ thống trả về trạng thái hiện tại mà không ghi đè read_at.

5. Response DTO

public class NotificationResponse {

private UUID id;

private String type;          // ALERT, TASK, INFO

private String title;

private String content;

private Boolean isRead;

private LocalDateTime readAt;

private LocalDateTime createdAt;

}

public class NotificationListResponse {

private List<NotificationResponse> items;

private int page;

private int size;

private long totalElements;

}

public class UnreadCountResponse {

private long unreadCount;

}

public class MarkReadResponse {

private UUID id;

private Boolean isRead;

private LocalDateTime readAt;

}

Ghi chú: totalElements cho biết tổng số thông báo phù hợp bộ lọc, phục vụ hiển thị phân trang trên hộp thông báo.

6. Response

Ví dụ request

GET http://localhost:8080/api/v1/notifications?page=0&size=20

HTTP 200 OK — danh sách thông báo (TC-01)

{

"success": true,

"status": 200,

"data": {

"items": [

{

"id": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",

"type": "ALERT",

"title": "Lô hàng đã bị thu hồi",

"content": "Lô hàng SP-0021 vừa được thu hồi do phát hiện dư lượng thuốc bảo vệ thực vật.",

"isRead": false,

"readAt": null,

"createdAt": "2026-07-31T02:10:05Z"

}

],

"page": 0,

"size": 20,

"totalElements": 1

},

"timestamp": "2026-07-31T02:15:00.001000000Z"

}

HTTP 200 OK — chưa có thông báo (TC-02)

{

"success": true,

"status": 200,

"data": {

"items": [],

"page": 0,

"size": 20,

"totalElements": 0

},

"timestamp": "2026-07-31T02:16:00.001000000Z"

}

HTTP 200 OK — đánh dấu đã đọc thành công (TC-03)

{

"success": true,

"status": 200,

"data": {

"id": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",

"isRead": true,

"readAt": "2026-07-31T02:20:00Z"

},

"timestamp": "2026-07-31T02:20:00.001000000Z"

}

HTTP 200 OK — số thông báo chưa đọc

{

"success": true,

"status": 200,

"data": { "unreadCount": 3 },

"timestamp": "2026-07-31T02:21:00.001000000Z"

}

7. Error Response

403 Forbidden — không có quyền thao tác (TC-04)

{

"success": false,

"status": 403,

"message": "Bạn không có quyền thao tác trên thông báo này."

}

404 Not Found — thông báo không tồn tại

{

"success": false,

"status": 404,

"message": "Không tìm thấy thông báo."

}

401 Unauthorized — chưa đăng nhập

{

"success": false,

"status": 401,

"message": "Bạn cần đăng nhập để xem thông báo."

}

8. Backend xử lý

Luồng tạo và gửi thông báo

Sự kiện nghiệp vụ phát sinh (ChainEvent / Alert / Recall / Invitation...)

▼

Xác định loại thông báo (ALERT / TASK / INFO) và danh sách người nhận liên quan

▼

Tạo bản ghi Notification cho từng người nhận (is_read = false)

▼

Người dùng thấy thông báo trong hộp thông báo (badge chưa đọc)

Luồng GET /api/v1/notifications

Xác thực người dùng đang đăng nhập

▼

Truy vấn Notification theo user_id, áp dụng lọc isRead và phân trang

▼

Danh sách rỗng → trả data.items = [] (TC-02)

▼

Trả Response (200) kèm danh sách và tổng số bản ghi

Luồng PATCH /api/v1/notifications/{notificationId}/read

Kiểm tra Notification tồn tại theo notificationId → 404 nếu không có

▼

Kiểm tra Notification.user_id = người dùng hiện tại → 403 nếu khác (TC-04)

▼

Cập nhật is_read = true, read_at = thời điểm hiện tại (TC-03)

▼

Trả Response (200) với trạng thái đã đọc mới nhất

9. Repository

NotificationRepository

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

Page<Notification> findByUserIdOrderByCreatedAtDesc(

UUID userId, Pageable pageable);

Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(

UUID userId, Boolean isRead, Pageable pageable);

long countByUserIdAndIsReadFalse(UUID userId);

Optional<Notification> findById(UUID id);

@Modifying

@Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +

"WHERE n.id = :id")

int markAsRead(@Param("id") UUID id, @Param("readAt") LocalDateTime readAt);

}

Ghi chú: việc tạo Notification khi có sự kiện nghiệp vụ phát sinh (save/saveAll) được gọi từ các service nguồn (ChainEventService, AlertService, RecallService, InvitationService...) thông qua NotificationService dùng chung, đảm bảo mọi luồng phát thông báo tuân theo cùng một quy tắc phân loại và phân phối (mục 4.1, 4.2).

10. Phạm vi của Story

Bao gồm

•  Nhận thông báo khi có sự kiện/cảnh báo phát sinh liên quan đến người dùng.

•  Xem danh sách thông báo của chính mình, có phân trang và lọc theo trạng thái đọc.

•  Xem số lượng thông báo chưa đọc (badge).

•  Đánh dấu một thông báo là đã đọc và lưu lại trạng thái/lịch sử đọc.

•  Hiển thị trạng thái phù hợp khi hộp thông báo chưa có dữ liệu.

Không bao gồm

•  Cấu hình kênh gửi thông báo (email, SMS, push notification) — chưa thuộc phạm vi story này.

•  Cho phép người dùng tuỳ chỉnh loại thông báo muốn nhận — chưa thuộc phạm vi story này.

•  Xoá thông báo khỏi hộp thông báo.

•  Logic xác định chi tiết từng loại sự kiện phát sinh cảnh báo (thuộc các story nguồn: NCL-08-CN-001, NCL-08-CN-003...); story này chỉ xử lý việc nhận, hiển thị và đánh dấu đã đọc.

•  Đánh dấu đã đọc hàng loạt nhiều thông báo cùng lúc — giả định mỗi lần chỉ đánh dấu một thông báo.

11. User Story liên quan

NCL-08-CN-005 — Nhận thông báo

Là Người ghi sự kiện, tôi muốn nhận thông báo về việc cần làm và cảnh báo liên quan, để xử lý kịp thời trong chuỗi.

Độ ưu tiên: Bắt buộc | Phụ trách: Thành viên ba | Trạng thái: Chưa thực hiện | Tham chiếu: QTN-07

12. Danh sách công việc

Chu kỳ áp dụng: Chu kỳ số ba.

13. Test Cases

TC-01: Luồng thành công

TC-02: Dữ liệu rỗng

TC-03: Lưu lịch sử

TC-04: Không có quyền