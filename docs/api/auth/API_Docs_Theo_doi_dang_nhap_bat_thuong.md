**📄 API Docs — Theo dõi đăng nhập bất thường**



**Tên nhánh: ****feature/NCL-01-CN-005-login-anomaly-detection**



**User Story: ****NCL-01-CN-005**



**Epic: ****NCL-01 — Quản lý tài khoản và phân quyền đa tổ chức**



**Quy tắc: ****QTN-01 — Bảo vệ dữ liệu truy xuất khỏi việc bị sửa bằng tài khoản bị chiếm quyền**



**Phụ thuộc: ****NCL-01-CN-001 (Đăng nhập, tạo tổ chức, cấp quyền thành viên)**



## 1. Thông tin chung



### Mục tiêu



Cho phép phát hiện sớm tài khoản bị chiếm quyền cho MỌI tài khoản trong hệ thống (không riêng tài khoản quản trị), và cho phép quản trị viên nền tảng cùng các vai trò quản lý tổ chức khóa/mở khóa tài khoản nghi vấn trong phạm vi phụ trách của mình — đồng thời hiển thị cảnh báo trực quan ngay trong hộp thông báo có sẵn của người dùng, không cần xây dựng kênh thông báo riêng.



### Yêu cầu nghiệp vụ



Toàn bộ logic đăng nhập bất thường được đặt trong package auth/ hiện có (không tách package security/ riêng) — vì bản chất đây là một phần mở rộng trực tiếp của luồng xác thực (AuthService.login()).



Đối tượng bị giám sát: mọi tài khoản đăng nhập vào hệ thống, không phân biệt vai trò — tức áp dụng đồng nhất cho VT-01 (Quản trị viên nền tảng), VT-02 (Quản lý tổ chức), VT-03, VT-04, VT-05 (các vai trò còn lại trong tổ chức). Mọi lần đăng nhập của tài khoản thuộc bất kỳ vai trò nào đều được ghi log và đưa vào đánh giá bất thường.



Một tài khoản được đánh dấu bất thường khi: (1) sai mật khẩu liên tiếp ≥ 5 lần trong cửa sổ 2 phút, hoặc (2) đăng nhập từ một quốc gia (GeoIP, mức country code) chưa từng ghi nhận thành công cho tài khoản đó.



Quyền xem/thao tác áp dụng theo đúng mô hình phân quyền đã dùng cho các API khác trong hệ thống (như activity-logs): VT-01 xem và thao tác trên toàn nền tảng; các vai trò quản lý tổ chức (VT-02 và tương đương) xem/thao tác trong phạm vi tổ chức của chính họ — organizationId luôn lấy từ token, không nhận từ client.



khóa là vô thời hạn: Admin (VT-01 hoặc quản lý tổ chức có quyền) khóa → tài khoản giữ trạng thái LOCKED cho đến khi được mở khóa thủ công, không có cơ chế tự hết hạn.



Khi phát hiện bất thường hoặc khi khóa/mở khóa tài khoản, hệ thống KHÔNG xây kênh thông báo mới mà tái sử dụng nguyên trạng NotificationService/NotificationController đã có (GET /api/v1/notifications, GET /api/v1/notifications/unread-count, PATCH /api/v1/notifications/{id}/read) để người nhận thấy cảnh báo ngay trong hộp thông báo hiện có của họ.



## 2. Vị trí làm việc tại cây thư mục Backend



*Thay đổi so với thiết kế trước: gộp toàn bộ vào package auth/ đã có, không tạo package security/ riêng.*



| src/main/java/vn/nguongocso/<br>├── auth/                                  # Package ĐÃ CÓ — bổ sung trực tiếp vào đây<br>│   ├── controller/<br>│   │   ├── AuthController.java             # đã có, không đổi<br>│   │   └── LoginMonitoringController.java  # <-- mới: login-history, login-anomalies, lock, unlock<br>│   ├── dto/<br>│   │   ├── request/<br>│   │   │   └── LockAccountRequest.java              # <-- mới<br>│   │   └── response/<br>│   │       ├── LoginHistoryResponse.java            # <-- mới<br>│   │       ├── LoginAnomalyResponse.java            # <-- mới<br>│   │       └── AccountLockResponse.java             # <-- mới<br>│   ├── entity/<br>│   │   ├── User.java                       # đã có, không đổi cấu trúc<br>│   │   ├── LoginAttempt.java                # <-- mới<br>│   │   ├── LoginAnomaly.java                 # <-- mới<br>│   │   └── AccountLock.java                  # <-- mới<br>│   ├── enums/<br>│   │   ├── UserStatus.java                   # đã có — bổ sung giá trị LOCKED<br>│   │   ├── LoginResult.java                # <-- mới: SUCCESS, FAILED<br>│   │   ├── AnomalyReasonCode.java           # <-- mới: REPEATED_FAILED_LOGIN,<br>│   │   │                                    #          UNUSUAL_COUNTRY<br>│   │   ├── AnomalyStatus.java               # <-- mới: OPEN, ACCOUNT_LOCKED,<br>│   │   │                                    #          DISMISSED<br>│   │   └── AccountLockStatus.java           # <-- mới: LOCKED, UNLOCKED<br>│   ├── repository/<br>│   │   ├── UserRepository.java              # đã có, không đổi<br>│   │   ├── LoginAttemptRepository.java       # <-- mới<br>│   │   ├── LoginAnomalyRepository.java       # <-- mới<br>│   │   └── AccountLockRepository.java        # <-- mới<br>│   └── service/<br>│       ├── AuthService.java                 # bổ sung: ghi LoginAttempt +<br>│       │                                     #          gọi evaluate() trong login()<br>│       ├── LoginAnomalyDetectionService.java # <-- mới<br>│       ├── AccountLockService.java           # <-- mới<br>│       └── impl/<br>│           ├── LoginAnomalyDetectionServiceImpl.java  # gọi Notification<br>│           │                                          # khi phát hiện<br>│           └── AccountLockServiceImpl.java            # gọi Notification<br>│                                                      # khi khóa/mở khóa<br>├── organization/                    # Package đã có — không đổi (activity-logs)<br>└── notification/                    # Package đã có — TÁI SỬ DỤNG NGUYÊN TRẠNG<br>├── controller/NotificationController.java  # không sửa<br>└── service/NotificationService.java         # auth.service gọi trực tiếp |

| --- |



Lưu ý: auth.service (LoginAnomalyDetectionServiceImpl, AccountLockServiceImpl) phụ thuộc (dependency) vào notification.service.NotificationService — chiều phụ thuộc một chiều auth → notification, không có chiều ngược lại, tránh phụ thuộc vòng giữa hai package.



## 3. Cơ sở dữ liệu (Migration)



### 3.1. Bảng mới: login_attempts



| **Cột** | **Kiểu** | **Ràng buộc** | **Mô tả** |

| --- | --- | --- | --- |

| id | CHAR(36) | PK | Khoá chính |

| user_id | CHAR(36) | FK → User.id, nullable | Null nếu username không khớp tài khoản nào |

| username_input | VARCHAR | not null | Username được nhập, giữ lại kể cả khi sai |

| result | ENUM | not null | SUCCESS, FAILED |

| ip_address | VARCHAR | not null | Địa chỉ mạng nguồn của request đăng nhập |

| country_code | VARCHAR(2) | nullable | Mã quốc gia suy ra từ ip_address qua GeoIP |

| is_new_country | BOOLEAN | not null, default false | true nếu đây là quốc gia chưa từng ghi nhận SUCCESS cho tài khoản |

| created_at | DATETIME | not null | Thời điểm đăng nhập — áp dụng cho tài khoản mọi vai trò VT-01..VT-05 |



### 3.2. Bảng mới: login_anomalies



| **Cột** | **Kiểu** | **Ràng buộc** | **Mô tả** |

| --- | --- | --- | --- |

| id | CHAR(36) | PK | Khoá chính |

| user_id | CHAR(36) | FK → User.id, not null | Tài khoản nghi vấn — bất kỳ vai trò nào |

| organization_id | CHAR(36) | FK → Organization.id, not null | Dùng để lọc theo phạm vi tổ chức |

| reason_code | ENUM | not null | REPEATED_FAILED_LOGIN, UNUSUAL_COUNTRY |

| attempt_count | INT | nullable | Số lần sai liên tiếp (khi reason_code = REPEATED_FAILED_LOGIN) |

| ip_address | VARCHAR | not null | IP tại thời điểm phát hiện |

| country_code | VARCHAR(2) | nullable | Quốc gia tại thời điểm phát hiện |

| detected_at | DATETIME | not null | Thời điểm hệ thống đánh dấu bất thường |

| status | ENUM | not null, default OPEN | OPEN, ACCOUNT_LOCKED, DISMISSED |

| notification_id | CHAR(36) | FK → notifications.id, nullable | Bản ghi thông báo đã tạo qua NotificationService cho sự kiện này |



### 3.3. Bảng mới: account_locks



Lưu vòng đời từng lần khoá/mở khoá tạm — phục vụ hiển thị trạng thái hiện tại và lịch sử; hành động vẫn được ghi song song vào activity_logs theo TC-04.



| **Cột** | **Kiểu** | **Ràng buộc** | **Mô tả** |

| --- | --- | --- | --- |

| id | CHAR(36) | PK | Khoá chính |

| user_id | CHAR(36) | FK → User.id, not null | Tài khoản bị khoá — bất kỳ vai trò nào |

| anomaly_id | CHAR(36) | FK → login_anomalies.id, nullable | Bản ghi bất thường dẫn tới khoá (nếu có) |

| locked_by | CHAR(36) | FK → User.id, not null | Người thực hiện khoá (VT-01 hoặc quản lý tổ chức có quyền) |

| lock_reason | VARCHAR(500) | nullable | Ghi chú của người thực hiện |

| locked_at | DATETIME | not null | Thời điểm khoá |

| unlocked_by | CHAR(36) | FK → User.id, nullable | Người thực hiện mở khoá |

| unlocked_at | DATETIME | nullable | Thời điểm mở khoá — null nghĩa là đang còn khoá |

| status | ENUM | not null, default LOCKED | LOCKED, UNLOCKED |



### 3.4. Bổ sung giá trị cho enum UserStatus (bảng users)



Cột status hiện có của bảng users bổ sung giá trị LOCKED (bên cạnh ACTIVE, INACTIVE), áp dụng chung cho tài khoản mọi vai trò VT-01..VT-05. Khi status = LOCKED, đăng nhập bị từ chối ngay ở bước xác thực username/password. Không có cột thời hạn khoá vì luồng đã chốt là khoá vô thời hạn cho đến khi được mở khoá thủ công.



## 4. Phân quyền áp dụng (VT-01 – VT-05)



*Vai trò VT-03, VT-04, VT-05 chưa được mô tả chi tiết trong dữ liệu backlog đã cung cấp; bảng dưới đây dùng giả định hợp lý (Người ghi sự kiện, Đơn vị vận chuyển, Đơn vị kiểm nghiệm) chỉ để minh hoạ nguyên tắc phân quyền — cần đối chiếu lại với danh sách vai trò chính thức trước khi code.*



| **Vai trò** | **Bị giám sát đăng nhập (login_attempts)** | **Xem login-history / login-anomalies** | **Khoá / mở khoá tài khoản** |

| --- | --- | --- | --- |

| VT-01 — Quản trị viên nền tảng | Có | Toàn nền tảng (mọi tổ chức) | Mọi tài khoản, mọi tổ chức |

| VT-02 — Quản lý tổ chức (HTX) | Có | Chỉ trong tổ chức mình | Tài khoản thuộc tổ chức mình |

| VT-03 — Người ghi sự kiện (giả định) | Có | Không có quyền xem (403) | Không có quyền |

| VT-04 — Đơn vị vận chuyển (giả định) | Có | Không có quyền xem (403) | Không có quyền |

| VT-05 — Đơn vị kiểm nghiệm (giả định) | Có | Không có quyền xem (403) | Không có quyền |



*Nguyên tắc dùng chung cho mọi vai trò: cột "Bị giám sát đăng nhập" luôn là Có cho tất cả — vì mục tiêu là phát hiện chiếm quyền tài khoản bất kể vai trò gì. Cột "Xem"/"Khoá" áp dụng đúng theo mô hình phân quyền tổ chức đã dùng ở API activity-logs: chỉ vai trò có phạm vi quản lý (VT-01 toàn nền tảng, VT-02 trong tổ chức) mới thao tác được; các vai trò tác nghiệp (VT-03..VT-05) chỉ là đối tượng bị giám sát, không có quyền truy cập màn hình giám sát.*



## 5. API Endpoints



### 5.1. Lấy lịch sử đăng nhập



**Method: ****GET**



**Endpoint: ****/api/v1/auth/security/login-history**



**Quyền: ****VT-01 (toàn nền tảng, lọc được organizationId) · VT-02 (chỉ tổ chức mình, organizationId luôn lấy từ token)**



Trả về lịch sử các lần đăng nhập (thành công và thất bại) của tài khoản thuộc mọi vai trò, tách biệt với API Lịch sử hoạt động (/api/v1/organizations/activity-logs).



**Query Parameters**



| **Parameter** | **Kiểu** | **Bắt buộc** | **Mô tả** |

| --- | --- | --- | --- |

| page | int | Không | >= 0, mặc định 0 |

| size | int | Không | > 0, mặc định 10 |

| userId | UUID | Không | Lọc theo 1 tài khoản bất kỳ (VT-01 không giới hạn; VT-02 chỉ trong tổ chức mình) |

| result | String | Không | SUCCESS hoặc FAILED |

| organizationId | UUID | Không | Chỉ có hiệu lực với VT-01; VT-02 luôn bị ghi đè bằng organizationId trong token |

| startDate / endDate | String | Không | Định dạng yyyy-MM-dd |



**Response 200 OK**



| {<br>"success": true,<br>"status": 200,<br>"data": {<br>"items": [<br>{<br>"id": "9b1a2c3d-1122-4a3f-9c2b-1e6f8a4d5c7b",<br>"userId": "3c907154-1b15-46c8-bc4a-93df383a8b27",<br>"usernameInput": "field_staff01",<br>"roleCode": "VT-03",<br>"result": "FAILED",<br>"ipAddress": "203.0.113.5",<br>"countryCode": "VN",<br>"isNewCountry": false,<br>"createdAt": "2026-08-10T09:12:03Z"<br>}<br>],<br>"page": 0, "size": 10, "totalElements": 1, "totalPages": 1,<br>"first": true, "last": true<br>},<br>"timestamp": "2026-08-10T09:15:00Z"<br>} |

| --- |



### 5.2. Lấy danh sách đăng nhập bất thường



**Method: ****GET**



**Endpoint: ****/api/v1/auth/security/login-anomalies**



**Quyền: ****VT-01 (toàn nền tảng) · VT-02 (chỉ tổ chức mình — NCL-01-CN-005-TC-03)**



Đối tượng của các bản ghi trả về có thể là tài khoản thuộc bất kỳ vai trò nào (VT-01..VT-05); trường roleCode trong response cho biết vai trò của tài khoản nghi vấn.



**Query Parameters**



| **Parameter** | **Kiểu** | **Bắt buộc** | **Mô tả** |

| --- | --- | --- | --- |

| page | int | Không | >= 0, mặc định 0 |

| size | int | Không | > 0, mặc định 10 |

| status | String | Không | OPEN, ACCOUNT_LOCKED, DISMISSED |

| reasonCode | String | Không | REPEATED_FAILED_LOGIN, UNUSUAL_COUNTRY |

| organizationId | UUID | Không | Chỉ có hiệu lực với VT-01; VT-02 luôn bị ghi đè bằng organizationId trong token |



**Response 200 OK**



| {<br>"success": true,<br>"status": 200,<br>"data": {<br>"items": [<br>{<br>"id": "a71f4e2b-0021-4a3f-9c2b-1e6f8a4d5c7b",<br>"userId": "3c907154-1b15-46c8-bc4a-93df383a8b27",<br>"username": "field_staff01",<br>"fullName": "Trần Văn B",<br>"roleCode": "VT-03",<br>"organizationId": "b1e0c9a2-0011-4a3f-9c2b-1e6f8a4d5c7b",<br>"organizationName": "HTX Cát Tường",<br>"reasonCode": "REPEATED_FAILED_LOGIN",<br>"attemptCount": 5,<br>"ipAddress": "203.0.113.5",<br>"countryCode": "VN",<br>"detectedAt": "2026-08-10T09:12:10Z",<br>"status": "OPEN",<br>"notificationId": "c92e1a4f-..."<br>}<br>],<br>"page": 0, "size": 10, "totalElements": 1, "totalPages": 1,<br>"first": true, "last": true<br>},<br>"timestamp": "2026-08-10T09:15:00Z"<br>} |

| --- |



### 5.3. Khoá tạm tài khoản nghi vấn



**Method: ****PATCH**



**Endpoint: ****/api/v1/auth/security/accounts/{accountId}/lock**



**Quyền: ****VT-01 (mọi tài khoản) · VT-02 (chỉ tài khoản thuộc tổ chức mình, kể cả tài khoản VT-03/04/05 trong tổ chức đó)**



**Path Parameters**



| **Parameter** | **Kiểu** | **Bắt buộc** | **Mô tả** |

| --- | --- | --- | --- |

| accountId | UUID | Có | Phải tồn tại và đang ở trạng thái ACTIVE |



**Request Body**



| **Trường** | **Kiểu** | **Bắt buộc** | **Mô tả** |

| --- | --- | --- | --- |

| anomalyId | UUID | Không | Liên kết bản ghi bất thường dẫn tới khoá, nếu có |

| reason | String | Không | Tối đa 500 ký tự, ghi chú của người thực hiện |



**Response 200 OK**



| {<br>"success": true,<br>"status": 200,<br>"data": {<br>"accountId": "3c907154-1b15-46c8-bc4a-93df383a8b27",<br>"status": "LOCKED",<br>"lockedBy": "manager_coop1",<br>"lockedAt": "2026-08-10T09:20:00Z",<br>"reason": "5 lần sai mật khẩu trong 2 phút, đăng nhập từ quốc gia lạ",<br>"notificationSent": true<br>},<br>"timestamp": "2026-08-10T09:20:00Z"<br>} |

| --- |



**Response 409 Conflict — đã bị khoá trước đó**



| {<br>"success": false,<br>"status": 409,<br>"message": "Tài khoản đã bị khóa trước đó"<br>} |

| --- |



**Response 403 Forbidden — VT-02 cố khoá tài khoản ngoài tổ chức mình, hoặc vai trò không có quyền**



| {<br>"success": false,<br>"status": 403,<br>"message": "Bạn không có quyền khóa tài khoản này"<br>} |

| --- |



### 5.4. Mở khoá tài khoản



**Method: ****PATCH**



**Endpoint: ****/api/v1/auth/security/accounts/{accountId}/unlock**



**Quyền: ****VT-01 (mọi tài khoản) · VT-02 (chỉ tài khoản thuộc tổ chức mình)**



Tài khoản LOCKED giữ nguyên trạng thái vô thời hạn cho đến khi API này được gọi — không có cơ chế tự động hết hạn khoá.



**Response 200 OK**



| {<br>"success": true,<br>"status": 200,<br>"data": {<br>"accountId": "3c907154-1b15-46c8-bc4a-93df383a8b27",<br>"status": "ACTIVE",<br>"unlockedBy": "manager_coop1",<br>"unlockedAt": "2026-08-12T08:00:00Z",<br>"notificationSent": true<br>},<br>"timestamp": "2026-08-12T08:00:00Z"<br>} |

| --- |



**Response 409 Conflict — tài khoản hiện không ở trạng thái khoá**



| {<br>"success": false,<br>"status": 409,<br>"message": "Tài khoản hiện không ở trạng thái bị khóa"<br>} |

| --- |



## 6. Tích hợp với API Thông báo (Notification) — dùng lại nguyên trạng



Không tạo endpoint thông báo mới. Ba API đã có trong NotificationController tiếp tục dùng nguyên trạng, chỉ khác là nay có thêm các bản ghi thông báo mới do auth.service tạo ra qua NotificationService:



| **Endpoint có sẵn** | **Vai trò dùng cho story này** |

| --- | --- |

| GET /api/v1/notifications?isRead=false | Người nhận (VT-01 toàn nền tảng, hoặc VT-02 của tổ chức liên quan) mở hộp thông báo, thấy cảnh báo LOGIN_ANOMALY_DETECTED mới nhất trước tiên |

| GET /api/v1/notifications/unread-count | Hiển thị số huy hiệu (badge) trên biểu tượng chuông — tăng ngay khi có bất thường mới được phát hiện, hoặc khi tài khoản của người dùng bị khoá/mở khoá |

| PATCH /api/v1/notifications/{notificationId}/read | Đánh dấu đã xem sau khi người dùng bấm vào thông báo và được điều hướng tới màn hình chi tiết bất thường tương ứng (referenceId = anomalyId) |



**Luồng gọi nội bộ (không phải REST call, mà là gọi thẳng bean NotificationService từ auth.service):**



| // Trong LoginAnomalyDetectionServiceImpl, sau khi lưu bản ghi login_anomalies<br>notificationService.create(<br>NotificationCreateCommand.builder()<br>.recipientUserIds(platformAdminAndScopedOrgManagerIds)<br>.type("LOGIN_ANOMALY_DETECTED")<br>.referenceType("LOGIN_ANOMALY")<br>.referenceId(anomaly.getId())<br>.message(buildAnomalyMessage(anomaly)) // vd: "Tài khoản field_staff01 đăng nhập bất thường"<br>.build());<br>// Trong AccountLockServiceImpl, sau khi khoá / mở khoá thành công<br>notificationService.create(<br>NotificationCreateCommand.builder()<br>.recipientUserIds(List.of(lockedAccount.getUserId()))<br>.type("ACCOUNT_LOCKED") // hoặc "ACCOUNT_UNLOCKED"<br>.referenceType("ACCOUNT_LOCK")<br>.referenceId(accountLock.getId())<br>.message("Tài khoản của bạn đã bị khóa do phát hiện đăng nhập bất thường")<br>.build()); |

| --- |



**Bổ sung tối thiểu cho notification/ để nhận 2 loại sự kiện mới, KHÔNG đổi API bề mặt của NotificationController:**



Thêm giá trị LOGIN_ANOMALY_DETECTED, ACCOUNT_LOCKED, ACCOUNT_UNLOCKED vào enum NotificationType hiện có của notification/.



NotificationService cần một phương thức nội bộ (không phải REST endpoint public) để các service khác — ở đây là auth.service — gọi tạo thông báo hàng loạt cho nhiều recipientUserIds; nếu NotificationService hiện tại chỉ hỗ trợ tạo cho 1 người nhận, bổ sung overload nhận List<UUID>.



NotificationResponse (đã có) trả thêm referenceType/referenceId nếu chưa có, để frontend biết điều hướng tới đâu khi người dùng bấm vào thông báo loại LOGIN_ANOMALY_DETECTED / ACCOUNT_LOCKED.



## 7. Business Rules (QTN-01 và các quy tắc liên quan)



Cách ly dữ liệu (QTN-01): organizationId của VT-02 luôn lấy từ token cho cả 2 API GET; VT-01 không bị giới hạn.



Giám sát đăng nhập áp dụng cho tài khoản mọi vai trò (VT-01–VT-05); quyền xem/khoá/mở khoá chỉ giới hạn ở VT-01 và VT-02 theo phạm vi tổ chức, như mô tả ở mục 4.



Điều kiện đánh dấu bất thường: sai mật khẩu liên tiếp ≥ 5 lần trong cửa sổ 2 phút, HOẶC đăng nhập từ quốc gia (GeoIP, mức country code) chưa từng ghi nhận SUCCESS cho tài khoản.



Khoá/mở khoá là thao tác thủ công hai chiều — không có job tự động mở khoá theo thời gian.



Ngay khi khoá, mọi access token hiện có của tài khoản bị vô hiệu hoá lập tức.



Mỗi lần phát hiện bất thường hoặc khoá/mở khoá tài khoản đều tạo một thông báo qua NotificationService hiện có — không thông báo nào bỏ qua bước này, đảm bảo tính trực quan theo yêu cầu.



Hành động khoá/mở khoá vẫn ghi vào activity_logs kèm actor (TC-04), song song với bản ghi account_locks.



Trường hợp không có bản ghi thỏa bộ lọc: trả về items rỗng [] kèm 200 OK.



Danh sách trả về sắp xếp giảm dần theo thời gian (detectedAt DESC / createdAt DESC).



## 8. Repository Methods



**LoginAttemptRepository (mới)**



| Page<LoginAttempt> findByUser_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);<br>List<LoginAttempt> findTop5ByUser_UserIdAndResultOrderByCreatedAtDesc(<br>UUID userId, LoginResult result);<br>boolean existsByUser_UserIdAndResultAndCountryCode(<br>UUID userId, LoginResult result, String countryCode); |

| --- |



**LoginAnomalyRepository (mới)**



| Page<LoginAnomaly> findByOrganization_OrganizationIdOrderByDetectedAtDesc(<br>UUID organizationId, Pageable pageable);<br>Page<LoginAnomaly> findAllByOrderByDetectedAtDesc(Pageable pageable); |

| --- |



**AccountLockRepository (mới)**



| Optional<AccountLock> findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(<br>UUID userId, AccountLockStatus status); |

| --- |



## 9. DTOs



**LockAccountRequest**



| public class LockAccountRequest {<br>private UUID anomalyId;<br>@Size(max = 500)<br>private String reason;<br>} |

| --- |



**LoginAnomalyResponse**



| public class LoginAnomalyResponse {<br>private UUID id;<br>private UUID userId;<br>private String username;<br>private String fullName;<br>private String roleCode;        // VT-01..VT-05 — vai trò của tài khoản nghi vấn<br>private UUID organizationId;<br>private String organizationName;<br>private String reasonCode;      // REPEATED_FAILED_LOGIN, UNUSUAL_COUNTRY<br>private Integer attemptCount;<br>private String ipAddress;<br>private String countryCode;<br>private OffsetDateTime detectedAt;<br>private String status;          // OPEN, ACCOUNT_LOCKED, DISMISSED<br>private UUID notificationId;<br>} |

| --- |



**AccountLockResponse**



| public class AccountLockResponse {<br>private UUID accountId;<br>private String status;          // LOCKED, ACTIVE<br>private String lockedBy;<br>private OffsetDateTime lockedAt;<br>private String unlockedBy;<br>private OffsetDateTime unlockedAt;<br>private String reason;<br>private Boolean notificationSent;<br>} |

| --- |



## 10. Ghi chú Frontend



Màn hình "Theo dõi đăng nhập bất thường" là trang riêng, tách khỏi "Lịch sử hoạt động"; hiển thị được cho VT-01 (toàn nền tảng) và VT-02 (tổ chức mình), có cột roleCode để phân biệt tài khoản nghi vấn thuộc vai trò nào.



Biểu tượng chuông thông báo hiện có (dùng chung toàn hệ thống) tự động hiện badge khi có bất thường mới hoặc khi tài khoản của người dùng bị khoá/mở khoá — không cần xây UI thông báo riêng.



Khi người dùng bấm vào thông báo loại LOGIN_ANOMALY_DETECTED, điều hướng tới màn hình chi tiết bất thường theo referenceId; khi bấm thông báo loại ACCOUNT_LOCKED, hiển thị màn hình thông tin tài khoản bị khoá kèm lý do.



Nút "Khoá tạm" / "Mở khoá" chỉ hiện ra với người dùng có quyền (VT-01, hoặc VT-02 khi đang xem tài khoản trong tổ chức của mình).



Không hiển thị đếm ngược/thời hạn khoá ở đâu trên UI vì khoá là vô thời hạn theo thiết kế.



## 11. Kế hoạch triển khai (Backend)



| **Công việc** | **Package** | **File** |

| --- | --- | --- |

| Chốt ngưỡng và cách nhận biết vị trí lạ (CV-01) | auth (phân tích) | Tài liệu quy tắc: 5 lần/2 phút, GeoIP quốc gia |

| Phát triển ghi nhật ký đăng nhập (CV-02) | auth.entity, auth.repository | LoginAttempt.java, LoginAttemptRepository.java |

| Phát triển đánh giá và tạo bất thường + gọi Notification | auth.service.impl, notification.service | LoginAnomalyDetectionServiceImpl.java |

| Bổ sung điểm gọi vào luồng login hiện có | auth.service | AuthService.java (login()) |

| Phát triển màn hình theo dõi và khoá/mở khoá (CV-03) | auth.controller, frontend | LoginMonitoringController.java, màn hình danh sách bất thường |

| Phát triển khoá/mở khoá + vô hiệu hoá phiên + gọi Notification | auth.service.impl, notification.service | AccountLockServiceImpl.java |

| Bổ sung NotificationType mới + overload gửi nhiều người nhận | notification.enums, notification.service | NotificationType.java, NotificationServiceImpl.java |

| Rà soát an toàn phiên đăng nhập (CV-04) | auth (bảo mật) | Kiểm tra JwtTokenProvider / danh sách token bị thu hồi |

| Tạo migration script | resources/db/migration | V18__add_login_anomaly_tables.sql |

| Kiểm thử phát hiện đăng nhập bất thường (CV-05) | test | LoginMonitoringControllerTest.java |



### Ánh xạ Test Case ↔ Xử lý backend



| **Test Case** | **Kịch bản** | **Xử lý tương ứng** |

| --- | --- | --- |

| NCL-01-CN-005-TC-01 | 5 lần sai liên tiếp trong 2 phút | LoginAnomalyDetectionServiceImpl tạo login_anomalies (REPEATED_FAILED_LOGIN) + gọi NotificationService tạo thông báo cho VT-01/VT-02 liên quan |

| NCL-01-CN-005-TC-02 | Admin khoá tạm tài khoản nghi vấn | PATCH 5.3 trả 200, status=LOCKED, vô hiệu hoá access token, gọi NotificationService gửi thông báo ACCOUNT_LOCKED cho chủ tài khoản |

| NCL-01-CN-005-TC-03 | Quản lý HTX mở danh sách bất thường toàn nền tảng | GET 5.2 tự lọc theo organizationId trong token của VT-02, bỏ qua tham số organizationId truyền lên |

| NCL-01-CN-005-TC-04 | Admin khoá tạm một tài khoản | Ghi vào activity_logs kèm actor, đồng thời tạo bản ghi account_locks (status=LOCKED) |
